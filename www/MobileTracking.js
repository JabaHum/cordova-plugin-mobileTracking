var exec = cordova.require('cordova/exec');

var MobileTracking = {
    PROVIDER: {
        BEST: 0,
        GPS: 1,
        NETWORK: 2,
        CELL: 3,
    },
    ACCURACY: {
        //A constant indicating a finer location accuracy requirement
        FINE: 1,
        //A constant indicating an approximate accuracy requirement
        COARSE: 2,
    },
    POWER: {
        //A constant indicating a high power requirement.
        HIGH: 3,
        //A constant indicating a low power requirement.
        LOW: 1,
        //A constant indicating a medium power requirement.
        MEDIUM: 2,
    },
    COST: {
        ALLOWED: 1,
        NOTALLOWED: 2,
    },
    getCurrentPosition: function(success, fail, args) {

        var param = {
            provider: this.PROVIDER.NETWORK,
            accuracy: this.ACCURACY.HIGH,
            power: this.POWER.HIGH,
            minTime:1000,
        };

        exec(success, fail, "MobileTracking", "getCurrentPosition", [param]);
    },

    watchPosition: function(success, fail, args) {

        exec(success, fail, "MobileTracking", "watchPosition", []);

    },

    clearWatch: function(success, fail, args) {

        exec(success, fail, "MobileTracking", "clearWatch", []);
    }

};

module.exports = MobileTracking;
