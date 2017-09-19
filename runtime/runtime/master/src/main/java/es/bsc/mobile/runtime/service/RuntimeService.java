package es.bsc.mobile.runtime.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import es.bsc.mobile.monitor.MonitorActivity;
import es.bsc.mobile.runtime.Configuration;

import es.bsc.mobile.runtime.types.CEI;

public class RuntimeService extends Service {

    private static final String LOGGER_TAG = "Runtime.Service";
    private final RuntimeServiceImpl mBinder = new RuntimeServiceImpl();

    @Override
    public void onCreate() {
        try {
            Intent intent = new Intent(this, MonitorActivity.class);
            PendingIntent penInt = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("Mobile Runtime Service")
                    .setContentText("MRS is currently runnig")
                    .setContentIntent(penInt);

            startForeground(10101010, mBuilder.getNotification());
        } catch (Exception e) {
            Log.e(LOGGER_TAG, "Error building the runtime notification", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Configuration.load(this);
        synchronized (this) {
            CEI cei = (CEI) intent.getExtras().get("CEI");
            mBinder.start(cei);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (RuntimeServiceItf.Stub) mBinder;
    }
}
