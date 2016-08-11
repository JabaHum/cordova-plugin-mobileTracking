
var exec = cordova.require('cordova/exec');

var MobileTracking = {

  getCurrentPosition: function(success, fail, args) {

          exec(success, fail, "MobileTracking", "getCurrentPosition", []);
      },

      watchPosition: function(success, fail, args) {

          exec(success, fail, "MobileTracking", "watchPosition", []);

      },

      clearWatch: function(success, fail, args) {

          exec(success, fail, "MobileTracking", "clearWatch", []);
      }

};

module.exports = MobileTracking;
