
var exec = cordova.require('cordova/exec');

var MobileTracking = {
  TYPE: {
    GPS : 1,
    NETWORK : 2,
    CELL : 3,
  },
  getCurrentPosition: function(success, fail, args) {

    var param = {provider: this.TYPE.GPS};

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
