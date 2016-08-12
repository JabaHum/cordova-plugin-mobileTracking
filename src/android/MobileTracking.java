
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import javax.security.auth.callback.Callback;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import android.os.Bundle;






public class MobileTracking extends CordovaPlugin {

public static final String TAG = "debug";
public static final String GPS = Manifest.permission.ACCESS_FINE_LOCATION;
public static final String NETWORK = Manifest.permission.ACCESS_COARSE_LOCATION;
public static final int GPS_REQ_CODE = 0;
public static final int NETWORK_REQ_CODE = 1;
public static final int MOBILETRACKING_GPS = 1;
public static final int MOBILETRACKING_NETWORK = 2;
public static final int MOBILETRACKING_CELL = 3;
private static final String PERMISSION_DENIED_ERROR = "User refused to give permissions for reading call log";
private static LocationManager locationManager;
private static CordovaInterface cordova;
private static CallbackContext callbackContext;



/**
* Constructor.
*/
public MobileTracking() {}

public void initialize(CordovaInterface cordova, CordovaWebView webView) {
  super.initialize(cordova, webView);
  this.cordova = cordova;

}
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
  Log.d(TAG, "We are entering execute");
  this.callbackContext = callbackContext;

  if (action.equals("getCurrentPosition")) {
    if(args.length()!= 0){
      JSONObject jsonObject = args.getJSONObject(0);
      int provider = jsonObject.getInt("provider");
      switch(provider){
        case MOBILETRACKING_GPS: getPositionGPS(); break;
        case MOBILETRACKING_NETWORK: getPositionNetwork(); break;
        case MOBILETRACKING_CELL: getPositionCell(); break;
      }
      callbackContext.success();
    }
    else {
      // standard algorithm here
    }
    return true;
    }
  return false;  // Returning false results in a "MethodNotFound" error.
}

protected void getGPSPermission(int requestCode)
{
    cordova.requestPermission(this, requestCode, GPS);
}
protected void getNetworkPermission(int requestCode)
{
    cordova.requestPermission(this, requestCode, GPS);
}
public void onRequestPermissionResult(int requestCode, String[] permissions,
                                         int[] grantResults) throws JSONException
{
    for(int r:grantResults)
    {
        if(r == PackageManager.PERMISSION_DENIED)
        {
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
            return;
        }
    }
    switch(requestCode)
    {
        case GPS_REQ_CODE:
        getSinglePositionGPS();

            break;
        case NETWORK_REQ_CODE:
        Log.i(TAG, "network granted");

            break;

    }
}
public void getPositionGPS(){
  if(cordova.hasPermission(GPS)){
    getSinglePositionGPS();
  }else{
    getGPSPermission(GPS_REQ_CODE);
  }
}
public void getPositionNetwork(){
  locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);

  if(cordova.hasPermission(NETWORK)){
    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      Log.i(TAG, "has network permission");

    }

  }else{
    getNetworkPermission(GPS_REQ_CODE);
  }
}
public void getPositionCell(){
  Log.i(TAG, "cell");

}
public void getSinglePositionGPS(){
  locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);

  if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    Log.i(TAG, "can use gps");
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
              Log.i(TAG, location.toString());

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });

  }
  else{
    callbackContext.error("gps is not available");
  }
}

}
