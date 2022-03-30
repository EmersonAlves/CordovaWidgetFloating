module.exports.open = function ({url, userId, driverId, token, urlLogo}, successCallback, errorCallback) {
    cordova.exec(() => {
            successCallback()
        }, () => {
            errorCallback()
        }, "FloatingWidget", "open",
        [{url, userId, driverId, token,urlLogo}]);
};

module.exports.close = function (successCallback, errorCallback) {
    cordova.exec(() => {
        successCallback()
    }, () => {
        errorCallback()
    }, "FloatingWidget", "close", []);
}

module.exports.getPermission = function (successCallback, errorCallback) {
    cordova.exec(successCallback,errorCallback, "FloatingWidget", "getPermission", []);
}

module.exports.getPermissionLocation = function (successCallback, errorCallback) {
    cordova.exec(successCallback,errorCallback, "FloatingWidget", "getPermissionLocation", []);
}

module.exports.askPermissionLocation = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "FloatingWidget", "askPermissionLocation", []);
}


module.exports.checkSystemOverlayPermission = function (callback) {
    cordova.exec(callback, ()=>{}, "FloatingWidget", "checkSystemOverlayPermission", []);
}