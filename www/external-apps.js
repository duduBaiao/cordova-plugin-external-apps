var exec = require('cordova/exec');

const DIALOG_TITLE = "Choose an app to open the file";

var ExternalApps = {

    list: function(kind, parameters, successCallback, errorCallback) {

        cordova.exec(
            successCallback,
            errorCallback,
            'ExternalApps',
            'list',
            [kind, parameters]);
    },

    canOpen: function(url, successCallback, errorCallback) {

        cordova.exec(
            successCallback,
            errorCallback,
            'ExternalApps',
            'canOpen',
            [url]);
    },

    open: function(url, intentInfo, successCallback, errorCallback) {

        cordova.exec(
            successCallback,
            errorCallback,
            'ExternalApps',
            'open',
            [url, intentInfo]);
    },

    chooseAndOpen: function(title, uri, mimeType, successCallback, errorCallback) {

        cordova.exec(
            successCallback,
            errorCallback,
            'ExternalApps',
            'chooseAndOpen',
            [title || DIALOG_TITLE, uri, mimeType || ""]);
    }
};

module.exports = ExternalApps;
