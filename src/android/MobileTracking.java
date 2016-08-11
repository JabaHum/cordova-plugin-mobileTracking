import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;

import javax.security.auth.callback.Callback;

public class MobileTracking extends CordovaPlugin {
CallbackContext callbackContext ;
public static final String TAG = "debug";
CallbackContext context;
public static final String GPS = Manifest.permission.ACCESS_FINE_LOCATION;
public static final int REQ_CODE = 0;
private static final String PERMISSION_DENIED_ERROR =
        "User refused to give permissions for reading call log";


/**
* Constructor.
*/
public MobileTracking() {}

/*
public void initialize(CordovaInterface cordova, CordovaWebView webView) {
  super.initialize(cordova, webView);
  Log.v(TAG,"Init CoolPlugin");
}

*/
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
  Log.d(TAG, "We are entering execute");
  context = callbackContext;
  if (action.equals("getCurrentPosition")) {
    if(cordova.hasPermission(GPS)){
      Log.i(TAG, "has permission ");

      return true;
    }
    else {
      getGPSPermission(REQ_CODE);

    }
    return true;
    }
  return false;  // Returning false results in a "MethodNotFound" error.
}

protected void getGPSPermission(int requestCode)
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
            this.context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
            return;
        }
    }
    switch(requestCode)
    {
        case REQ_CODE:
        Log.i(TAG, "works ");

            break;
    }
}

}
