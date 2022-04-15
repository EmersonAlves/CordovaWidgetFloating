module.exports.open = function ({url, userId, driverId, token}, successCallback, errorCallback) {
    cordova.exec(() => {
            successCallback()
        }, () => {
            errorCallback()
        }, "FloatingWidget", "open",
        [{url, userId, driverId, token}]);
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

module.exports.startLocationService = function (data,successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "FloatingWidget", "startLocationService", [data]);
}


module.exports.stopLocationService = function (callback) {
    cordova.exec(callback, ()=>{}, "FloatingWidget", "stopLocationService", []);
}

module.exports.onListenerLocation = function (callback) {
    cordova.exec((data)=> callback(JSON.parse(data)), ()=>{}, "FloatingWidget", "onListenerLocation", []);
}