package de.appplant.cordova.plugin.background;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.StrictMode;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;


public class CellLocationController{
public Context context;
public ArrayList<Cell> threeCells;
public static final String TAG = "debug";
public final int MY_PERMISSIONS_REQUEST_CORSE = 1;
double myLatitude, myLongitude,signalStrength;

private XmlPullParserFactory xmlFactoryObject;

  public void run(TelephonyManager telephonyManager,Context context)
  {
    this.context = context;
        ArrayList<Cell> test;
        int i=0;

        int numberOfTower = telephonyManager.getAllCellInfo().size();
        Log.i(TAG,"number of cell tower "+numberOfTower);
        if(numberOfTower>2){
            test = chooseCells(telephonyManager);
            OpenCellID[] openCellIDs = new OpenCellID[test.size()];
            i=0;
            for (OpenCellID openCellID : openCellIDs){
                openCellID = new OpenCellID();
                openCellID.setMcc("" + test.get(i).getMcc());
                openCellID.setMnc("" + test.get(i).getMnc());
                openCellID.setCallID(test.get(i).getCid());
                openCellID.setCallLac(test.get(i).getLac());
                try {
                    test.get(i).setLocation(openCellID.findLocation());
                }catch (Exception e) {
                    e.printStackTrace();
                }
                i++;
            }
            i = 0;
            for (Cell cell : test) {
                Log.i(TAG, "cell: " + i + cell.getLocation().address);
                if(i==0){
                    test.get(0).setDifferenceLatitude(0);
                    test.get(0).setDifferenceLongitude(0);
                }
                else{
                    cell.setDifferenceLongitude(Math.abs(test.get(0).getLocation().getLongitude()-cell.getLocation().getLongitude()));
                    cell.setDifferenceLatitude(Math.abs(test.get(0).getLocation().getLatitude()-cell.getLocation().getLatitude()));
                }
                i++;
            }
            ArrayList<Cell> cellsToPerformTriangulationOn;
            Collections.sort(test);
            if(test.size()>2){
                cellsToPerformTriangulationOn=new ArrayList<Cell>(3);
                cellsToPerformTriangulationOn.add(test.get(0));
                cellsToPerformTriangulationOn.add(test.get(1));
                cellsToPerformTriangulationOn.add(test.get(2));
            }
            else if(test.size()==2){
                cellsToPerformTriangulationOn=new ArrayList<Cell>(2);
                cellsToPerformTriangulationOn.add(test.get(0));
                cellsToPerformTriangulationOn.add(test.get(1));
            }
            else {
                cellsToPerformTriangulationOn=new ArrayList<Cell>(1);
                cellsToPerformTriangulationOn.add(test.get(0));
            }

            Location result = triangulation(cellsToPerformTriangulationOn);
            Log.i(TAG, "run: "+(result.getAddress()));
            Log.i(TAG, "run2: "+"latitude:"+result.getLatitude()+"longitude:"+result.getLongitude());
            Log.i(TAG, "result: " + result.getAddress());
            Log.i(TAG, "longitude: " + result.getLongitude());
            Log.i(TAG, "latitude: " + result.getLatitude());
        }
  }
  public String getAddressFromCoordinates(double latitude, double longitude){
        Geocoder geocoder;
        List<Address> addresses=null;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
        return "you are in : "+address+" ,"+state+" ,"+country;
    }

  public XmlPullParser xmlParser(InputStream inputStream){
        try {
            xmlFactoryObject = XmlPullParserFactory.newInstance();
            xmlFactoryObject.setNamespaceAware(true);
            XmlPullParser myParser = xmlFactoryObject.newPullParser();
            myParser.setInput(inputStream, null);
            return myParser;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

  public double[] parseXml(XmlPullParser myParser){
        double[] coordinates = new double[2];
        if(myParser==null){
            Log.i("debug","parser null");
        }
        int event;
        String text=null;

        try {
            event = myParser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                String name=myParser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:

                        break;

                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(name.equals("cell")){
                            coordinates[0]=Double.parseDouble(myParser.getAttributeValue(0));
                            coordinates[1]=Double.parseDouble(myParser.getAttributeValue(1));
                            coordinates[2]=Double.parseDouble(myParser.getAttributeValue(6));
                        }
                        break;
                }
                event = myParser.next();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return coordinates;
    }

  public Location triangulation(ArrayList<Cell> cells){
        int numberOfCells=cells.size();
        double resLatitude = 0,resLongitude = 0,sumOfSignals = 0;

        Log.i(TAG, "size of cells to perform triangulation on: "+numberOfCells);
        for (Cell cell : cells){
            sumOfSignals+=cell.getSignalStrength();
        }
        double [] weights = new double[numberOfCells];
        for (int i = 0; i < numberOfCells; i++) {
            weights[i]=cells.get(i).getSignalStrength()/sumOfSignals;
            resLatitude+=(weights[i]*cells.get(i).getLocation().getLatitude());
            resLongitude+=(weights[i]*cells.get(i).getLocation().getLongitude());
        }
        return new Location(resLongitude,resLatitude);
    }

  public  ArrayList<Cell> chooseCells(TelephonyManager telephonyManager){
        ArrayList<Cell> cells = new ArrayList<Cell>(telephonyManager.getAllCellInfo().size());
        int i=0;
        try {
            for (CellInfo info : telephonyManager.getAllCellInfo()) {
                if (isValidCellInfo(info)) {
                    if (info instanceof CellInfoGsm) {
                        cells.add(new Cell());
                        CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        cells.get(i).setCid(((CellInfoGsm) info).getCellIdentity().getCid());
                        cells.get(i).setLac(((CellInfoGsm) info).getCellIdentity().getLac());
                        cells.get(i).setMcc(((CellInfoGsm) info).getCellIdentity().getMcc());
                        cells.get(i).setMnc(((CellInfoGsm) info).getCellIdentity().getMnc());
                        cells.get(i).setSignalStrength(gsm.getDbm());
                    } else if (info instanceof CellInfoCdma) {
                    } else if (info instanceof CellInfoLte) {
                    } else if (info instanceof CellInfoWcdma) {
                    } else {
                        Log.i("debug", "problem ");
                    }
                    i++;
                }
            }
        }catch (Exception e){
            Log.i(TAG, "exception!! ");
        }
        return cells;
    }

  public boolean isValidCellInfo(CellInfo cellInfo){
        if (cellInfo instanceof CellInfoGsm) {
            if (((CellInfoGsm) cellInfo).getCellIdentity().getCid()==Integer.MAX_VALUE||((CellInfoGsm) cellInfo).getCellIdentity().getCid()==-1){
                return false;
            }
        }else if(cellInfo instanceof  CellInfoWcdma){
            if (((CellInfoWcdma) cellInfo).getCellIdentity().getCid()==Integer.MAX_VALUE){
                return false;
            }
        }else if (cellInfo instanceof CellInfoCdma){
            if (((CellInfoCdma) cellInfo).getCellIdentity().getBasestationId()==Integer.MAX_VALUE){
                return false;
            }
        }else if (cellInfo instanceof CellInfoLte){
            if (((CellInfoLte) cellInfo).getCellIdentity().getCi()==Integer.MAX_VALUE){
                return false;
            }
        } else;
        return true;
    }
  class Cell implements Comparable{
        private int mnc;
        private int mcc;
        private int lac;
        private int cid;
        private int signalStrength;
        private Location location;
        private double differenceLatitude;
        private double differenceLongitude;

        public double getDifferenceLatitude() {
            return differenceLatitude;
        }

        public void setDifferenceLatitude(double differenceLatitude) {
            this.differenceLatitude = differenceLatitude;
        }

        public double getDifferenceLongitude() {
            return differenceLongitude;
        }

        public void setDifferenceLongitude(double differenceLongitude) {
            this.differenceLongitude = differenceLongitude;
        }

        public int getSignalStrength() {
            return signalStrength;
        }

        public void setSignalStrength(int signalStrength) {
            this.signalStrength = signalStrength;
        }

        public int getMnc() {
            return mnc;
        }

        public void setMnc(int mnc) {
            this.mnc = mnc;
        }

        public int getMcc() {
            return mcc;
        }

        public void setMcc(int mcc) {
            this.mcc = mcc;
        }

        public int getLac() {
            return lac;
        }

        public void setLac(int lac) {
            this.lac = lac;
        }

        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        @Override
        public int compareTo(Object cell) {

            /* For Ascending order*/
            double tmp = (this.getDifferenceLatitude()+this.getDifferenceLongitude())-(((Cell)cell).getDifferenceLatitude()+((Cell)cell).getDifferenceLongitude());
            if(tmp>0)return 1;
            if(tmp<0)return -1;
            return 0;
        }
    }

   class OpenCellID {
        String mcc; //Mobile Country Code
        String mnc; //mobile network code
        String cellid; //Cell ID
        String lac; //Location Area Code
        Boolean error;
        String strURLSent;

        public Boolean isError(){
            return error;
        }

        public void setMcc(String value){
            mcc = value;
        }

        public void setMnc(String value){
            mnc = value;
        }

        public void setCallID(int value){
            cellid = String.valueOf(value);
        }

        public void setCallLac(int value){
            lac = String.valueOf(value);
        }

        public void groupURLSent(){
            strURLSent =
                    "http://www.opencellid.org/cell/get?key=ccd8d849-d708-42a3-ae8a-f5a4ee9d666c&mcc=" + mcc
                            +"&mnc=" + mnc
                            +"&cellid=" + cellid
                            +"&lac=" + lac
                            +"&fmt=txt";
        }

        public String getstrURLSent(){
            return strURLSent;
        }

        public Location findLocation() throws Exception {
            double[] coordinates = new double[3];
            String location ;
            groupURLSent();
            int len = 500;
            InputStream is = null;
            try {
                URL url = new URL(getstrURLSent());
                HttpURLConnection client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("GET");
                is = client.getInputStream();
                //parse xml here
                XmlPullParser xmlPullParser = xmlParser(is);
                coordinates= parseXml(xmlPullParser);
                location = getAddressFromCoordinates(coordinates[0],coordinates[1]);
            }finally {
                if (is != null) {
                    is.close();
                }
            }
            return new Location(location,coordinates[0],coordinates[1]);
        }
    }
  class Location {
        private String address;
        private double longitude;
        private double latitude;

        public Location(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.address=getAddressFromCoordinates(latitude,longitude);
        }

        public Location(String address, double latitude , double longitude ) {
            this.address = address;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(int longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(int latitude) {
            this.latitude = latitude;
        }
    }
}
