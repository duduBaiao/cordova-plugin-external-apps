package com.nostalgictouch.cordova.plugins;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class ExternalApps extends CordovaPlugin {
    
    protected static final String TAG = ExternalApps.class.getSimpleName();
    
    private String LIST = "list";
    private String CAN_OPEN = "canOpen";
    private String OPEN = "open";
    private String CHOOSE_AND_OPEN = "chooseAndOpen";
    
    private String NAVIGATION = "navigation";
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (LIST.equals(action)) {            
            String kind = args.getString(0);
            JSONObject parameters = args.getJSONObject(1);
            
            list(kind, parameters, callbackContext);
            return true;
        }
        else if (CAN_OPEN.equals(action)) {
            String url = args.getString(0);

            canOpen(url, callbackContext);
            return true;
        }
        else if (OPEN.equals(action)) {
            String url = args.getString(0);
            JSONObject intentInfo = args.getJSONObject(1);
            
            if (open(url, intentInfo)) {
                callbackContext.success();
            }
            else {
                callbackContext.error("Can't open the external app!");
            }

            return true;
        }
        else if (CHOOSE_AND_OPEN.equals(action)) {
            String title = args.getString(0);
            String uri = args.getString(1);
            String mimeType = args.getString(2);

            chooseAndOpen(title, uri, mimeType, callbackContext);
            return true;
        }

        return false;
    }

    private void canOpen(String url, CallbackContext callbackContext) {
        Intent intentCheck = intentForUrl(url);
        PackageManager packageManager = cordova.getActivity().getPackageManager();
        List<ResolveInfo> appsList = packageManager.queryIntentActivities(intentCheck, 0);

        try {
            if (appsList.size() > 0) {
                ResolveInfo resolveInfo = appsList.get(0);

                JSONObject intentInfo = new JSONObject();
                intentInfo.put("packageName", resolveInfo.activityInfo.packageName);
                intentInfo.put("className", resolveInfo.activityInfo.name);

                callbackContext.success(intentInfo);
            }
            else {
                callbackContext.error("");
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "canOpen: invalid json!");

            callbackContext.error("");
        }
    }

    private void list(final String kind, final JSONObject parameters, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject result = new JSONObject();
                    JSONArray apps = new JSONArray();
                    
                    if (NAVIGATION.equals(kind)) {
                        loadNavigationApps(apps, parameters);
                    }
                    
                    result.put("apps", apps);

                    callbackContext.success(result);
                }
                catch (JSONException e) {
                    Log.e(TAG, "list: invalid json!");

                    callbackContext.error("Invalid parameters!");
                }
            }
         });            
    }
    
    private Intent intentForUrl(String uriStr) {
        Uri uri = Uri.parse(uriStr);
        
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        
        return intent;
    }
    
    private void loadNavigationApps(JSONArray apps, JSONObject parameters) {
        List<String> navBlackList = Arrays.asList(new String[]{"Internet"});
        
        String srcLatitude, srcLongitude, destLatitude, destLongitude;
        
        try {
            JSONObject source = parameters.getJSONObject("source");
            
            srcLatitude = source.getString("latitude");
            srcLongitude = source.getString("longitude");
            
            JSONObject destination = parameters.getJSONObject("destination");
            
            destLatitude = destination.getString("latitude");
            destLongitude = destination.getString("longitude");
        }
        catch (JSONException e) {
            Log.e(TAG, "loadNavigationApps: invalid json!");

            return;
        }
        
        this.loadAppsForUrl(apps, "google.navigation:q=" + destLatitude + "," + destLongitude, navBlackList);
        
        this.loadAppsForUrl(apps, "waze://?ll=" + destLatitude + "," + destLongitude + "&navigate=yes", navBlackList);

        if (apps.length() == 0) {
            this.loadAppsForUrl(apps, "http://maps.google.com/maps?f=d&saddr=" + srcLatitude + "," + srcLongitude + "&daddr=" + destLatitude + "," + destLongitude + "&navigate=yes", navBlackList);
        }
    }
    
    private void loadAppsForUrl(JSONArray apps, String url, List<String> blackList) {
        Intent intentCheck = intentForUrl(url);
        PackageManager packageManager = cordova.getActivity().getPackageManager();
        List<ResolveInfo> appsList = packageManager.queryIntentActivities(intentCheck, 0);
        
        for (ResolveInfo resolveInfo : appsList) {
            String title = resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager).toString();
            
            if (blackList.contains(title)) {
                continue;
            }
            
            try {                
                boolean alreadyAdded = false;
                
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject addedApp = apps.getJSONObject(i);
                    
                    if (addedApp.get("title").equals(title)) {
                        alreadyAdded = true;
                        break;
                    }
                }
                
                if (!alreadyAdded) {
                    JSONObject app = new JSONObject();
                    
                    app.put("title", title);
                    app.put("url", url);
                    
                    JSONObject intentInfo = new JSONObject();
                    intentInfo.put("packageName", resolveInfo.activityInfo.packageName);
                    intentInfo.put("className", resolveInfo.activityInfo.name);
                    
                    app.put("intentInfo", intentInfo);
                    
                    apps.put(app);
                }
            }
            catch (JSONException e) {
                Log.e(TAG, "loadAppsForUrl: invalid json!");
            }
        }
    }
    
    private boolean open(String url, JSONObject intentInfo) {
        String packageName, className;
        
        try {
            packageName = intentInfo.getString("packageName");
            className = intentInfo.getString("className");
        }
        catch (JSONException e) {
            Log.e(TAG, "open: invalid json!");

            return false;
        }
        
        Intent applicationIntent = intentForUrl(url);
        
        applicationIntent.setClassName(packageName, className);
        applicationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        cordova.getActivity().startActivity(applicationIntent);
        
        return true;
    }

    private void chooseAndOpen(String title, String uri, String mimeType, CallbackContext callbackContext) {
        Intent intentCheck = new Intent(Intent.ACTION_VIEW);

        if (mimeType.length() > 0) {
            intentCheck.setDataAndType(Uri.parse(uri), mimeType);
        }
        else {
            intentCheck.setData(Uri.parse(uri));
        }

        intentCheck.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PackageManager packageManager = cordova.getActivity().getPackageManager();
        List<ResolveInfo> appsList = packageManager.queryIntentActivities(intentCheck, 0);

        if (appsList.size() > 0) {
            Intent chooserIntent = Intent.createChooser(intentCheck, title);

            this.cordova.startActivityForResult(this, chooserIntent, 100);

            callbackContext.success();
        }
        else {
            callbackContext.error("No apps found!");
        }
    }
}
