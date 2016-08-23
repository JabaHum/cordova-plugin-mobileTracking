
import android.app.Service;
import android.content.Intent;
import android.util.Log;
import android.os.IBinder;
import android.widget.Toast;

public class Background extends Service{
  public static final String TAG = "debug";
  public static int ay7aja(int a,int b){
    return a+b ;
  }

  @Override
      public void onCreate() {
        super.onCreate();

        Log.i(TAG,"on create");
      }
      @Override
      public IBinder onBind(Intent intent) {
        Log.i(TAG,"on bind");

          return null;
      }
      @Override
      public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"on start");

          // If we get killed, after returning from here, restart
          return START_STICKY;
      }
      @Override
      public void onDestroy() {
      }
}
