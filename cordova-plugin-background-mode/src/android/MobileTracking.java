package de.appplant.cordova.plugin.background;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.os.Looper;
import android.content.Intent;
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
import android.location.Criteria;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import android.os.Bundle;
import android.telephony.TelephonyManager;


public class MobileTracking extends CordovaPlugin {

    public static final String TAG = "debug";
    public static final String GPS = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String NETWORK = Manifest.permission.ACCESS_COARSE_LOCATION;

    public static final int GPS_REQ_CODE = 0;
    public static final int NETWORK_REQ_CODE = 1;
    public static final int CELL_REQ_CODE = 2;
    public static final int GPS_REQ_CODE_WATCH = 3;

    public static final int MOBILETRACKING_GPS = 1;
    public static final int MOBILETRACKING_NETWORK = 2;
    public static final int MOBILETRACKING_CELL = 3;
    public static final int MOBILETRACKING_BEST = 0;

    public static final int ACCURACY_FINE = 1;
    public static final int ACCURACY_COARSE = 2;

    public static final int POWER_LOW = 1;
    public static final int POWER_MEDIUM = 2;
    public static final int POWER_HIGH = 3;

    public static final int COST_ALLOWED = 1;
    public static final int COST_NOTALLOWED = 2;


    private static final String PERMISSION_DENIED_ERROR = "User refused to give permissions for reading call log";
    private static LocationManager locationManager;
    private static CordovaInterface cordova;
    private static CallbackContext callbackContext;
    private static Context context ;
    public Location bestGpsLocation;
    public Location bestNetworkLocation;
    public int counter=0;
    private double minDistance = 0;
    private long minTime = 0;

    /**
     * Constructor.
     */
    public MobileTracking() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
        context = this.cordova.getActivity().getApplicationContext();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "We are entering execute");
        this.callbackContext = callbackContext;
        if (action.equals("getCurrentPosition")) {
            // Acquire a reference to the system Location Manager
            locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);
            if(args.length()!= 0){
                JSONObject jsonObject = args.getJSONObject(0);
                int provider = jsonObject.getInt("provider");
                if(jsonObject.has("minDistance")){
                minDistance = jsonObject.getDouble("minDistance");
              }else minDistance = 0;
              if(jsonObject.has("minTime")){
              minTime = jsonObject.getLong("minTime");
              }else minTime = 0;
                switch(provider){
                    case MOBILETRACKING_GPS: getPositionGPS(minTime,minDistance); break;
                    case MOBILETRACKING_NETWORK: getPositionNetwork(minTime,minDistance); break;
                    case MOBILETRACKING_CELL: getPositionCell(); break;
                    case MOBILETRACKING_BEST: getPositionFromCriteria(args); break;
                }
            }
            else {
                // standard algorithm here
            }
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            this.callbackContext.sendPluginResult(pluginResult);
            return true;
        }else if (action.equals("watchPosition")){
          Log.d(TAG, "We are entering watchPosition");

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
                getSinglePositionGPS(minTime,minDistance);break;
            case NETWORK_REQ_CODE:
                getSinglePositionNETWORK(minTime,minDistance);break;
            case CELL_REQ_CODE:
                getSinglePositionCELL();break;
            case GPS_REQ_CODE_WATCH:
                watchPositionGPS();break;
        }
    }

    public  void watchPosition(){

      if(cordova.hasPermission(GPS)){
        Log.i("debug","has permissions");

        watchPositionGPS();
      }else{
          getGPSPermission(GPS_REQ_CODE_WATCH);
      }


  Log.i("debug","watchPosition");

    }
    public void watchPositionGPS(){
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        Log.i("debug","watchPositionGPS");
          // Define a listener that responds to location updates
          LocationListener locationListener = new LocationListener() {
              public void onLocationChanged(Location location) {

                  Log.i(TAG, "new "+location);


              }

              public void onStatusChanged(String provider, int status, Bundle extras) {}

              public void onProviderEnabled(String provider) {}

              public void onProviderDisabled(String provider) {}
          };

// Register the listener with the Location Manager to receive location updates
          locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,(float) minDistance, locationListener);
      }
      else{
        Log.i(TAG,"gps  not available");
        callbackContext.error("gps  not available");
      }
    }

    public void getPositionGPS(long minTime, double minDistance){
        if(cordova.hasPermission(GPS)){
            getSinglePositionGPS(minTime,minDistance);
        }else{
            getGPSPermission(GPS_REQ_CODE);
        }
    }
    public void getPositionNetwork(long minTime, double minDistance){
        if(cordova.hasPermission(NETWORK)){
            getSinglePositionNETWORK(minTime,minDistance);
        }else{
            getNetworkPermission(NETWORK_REQ_CODE);
        }
    }
    public void getSinglePositionNETWORK(long minTime, double minDistance){
      counter = 0;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.i(TAG, "can use network");
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {

                    // Called when a new location is found by the network location provider.
                    if(bestNetworkLocation!=null){
                        double deltaAccuracy = bestNetworkLocation.getAccuracy()-location.getAccuracy();
                        if(deltaAccuracy>0){
                            bestNetworkLocation = new Location(location);
                        }
                    }else{
                        bestNetworkLocation = new Location(location);
                    }
                    Log.i(TAG, "new "+location);
                    Log.i(TAG, "##"+counter+"##this...............location: "+bestNetworkLocation);
                    if(counter == 1 || bestNetworkLocation.getAccuracy()<=15){
                        sendLocation(this,bestNetworkLocation);
                    }
                    counter++;
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, (float) minDistance, locationListener);

        }else{
          Log.i(TAG,"network is not available");
          callbackContext.error("network is not available");
        }
    }
    public void getSinglePositionCELL(){
      CellLocationController cellLocationController = new CellLocationController();
      //retrieve a reference to an instance of TelephonyManager
      TelephonyManager telephonyManager = (TelephonyManager)cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
      cellLocationController.run(telephonyManager,context);
    }
    public void getPositionCell(){
        Log.i(TAG, "cell");
        if(cordova.hasPermission(NETWORK)){
          Log.i(TAG, "has cell permission");
          getSinglePositionCELL();
        }else{
            getNetworkPermission(CELL_REQ_CODE);
        }
    }

    public void getPositionFromCriteria(JSONArray args){
        if(args.length()!= 0){
            Criteria criteria = new Criteria();
            try {
                JSONObject jsonObject = args.getJSONObject(0);
                if(jsonObject.has("accuracy")){
                    int accuracy = jsonObject.optInt("accuracy", -1);

                    //int accuracy = jsonObject.getInt("accuracy");
                    switch(accuracy){
                        case ACCURACY_FINE: criteria.setAccuracy(ACCURACY_FINE);break;
                        case ACCURACY_COARSE: criteria.setAccuracy(ACCURACY_COARSE);break;
                        default : break;
                    }
                }
                if(jsonObject.has("power")){

                    //int power = jsonObject.getInt("power");
                    int power = jsonObject.optInt("power", -1);

                    switch(power){
                        case POWER_LOW: criteria.setPowerRequirement(POWER_LOW);break;
                        case POWER_MEDIUM: criteria.setPowerRequirement(POWER_MEDIUM);break;
                        case POWER_HIGH: criteria.setPowerRequirement(POWER_HIGH);break;
                        default : break;
                    }
                }
                if(jsonObject.has("cost")){

                    //int cost = jsonObject.getInt("cost");
                    int cost = jsonObject.optInt("cost", -1);

                    switch(cost){
                        case COST_ALLOWED: criteria.setCostAllowed(true);break;
                        case COST_NOTALLOWED: criteria.setCostAllowed(false);break;
                        default : break;
                    }
                }
                // boolean value indicates that the provider must be available
                String bestProvider = locationManager.getBestProvider(criteria, true);
                Log.i(TAG, "best provider:"+bestProvider);
                if(bestProvider.equals("gps")){
                  getPositionGPS(minTime,minDistance);
                }else if (bestProvider.equals("gps")) {
                  getPositionNetwork(minTime,minDistance);
                }
            } catch (JSONException e) {
                // Do something with the exception
            }
        }

    }

    public void getSinglePositionGPS(long minTime, double minDistance){
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i(TAG, "can use gps");
            // Define a listener that responds to location updates
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    int i=0;
                    counter = 0;
                    // Called when a new location is found by the network location provider.
                    if(bestGpsLocation!=null){
                        double deltaAccuracy = bestGpsLocation.getAccuracy()-location.getAccuracy();
                        if(deltaAccuracy>0){
                            bestGpsLocation = new Location(location);counter++;
                        }
                    }else{
                        bestGpsLocation = new Location(location);counter++;
                    }
                    Log.i(TAG, "new "+location);
                    Log.i(TAG, "##"+counter+"##this...............location: "+bestGpsLocation);
                    if(counter==2 || bestGpsLocation.getAccuracy()<=25){
                        sendLocation(this,bestGpsLocation);
                    }

                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

// Register the listener with the Location Manager to receive location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,(float) minDistance, locationListener);
        }
        else{
          Log.i(TAG,"gps is not available");
          callbackContext.error("gps is not available");
        }
    }
    public void sendLocation(LocationListener locationListener,Location location){
        String parsedLocation = new String();
        PluginResult pluginResult;
        locationManager.removeUpdates(locationListener);
        parsedLocation = ""+location.getLatitude()+" "+location.getLongitude();
        pluginResult = new PluginResult(PluginResult.Status.OK,parsedLocation);
        pluginResult.setKeepCallback(true);
        Log.i(TAG, "sending to successCallback..");
        callbackContext.sendPluginResult(pluginResult);
    }


}
