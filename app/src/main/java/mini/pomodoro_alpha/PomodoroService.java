package mini.pomodoro_alpha;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import static mini.pomodoro_alpha.App.CHANNEL_ID;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class PomodoroService extends Service {
    private PomodoroTimer timer;
    private boolean isPauseActive;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("SHINOGI","Service created.");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        Log.i("SHINOGI","Destroying service.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timeRemainingInMS = intent.getLongExtra("msRemaining",0);
        String timeFromActivity = timer.getTimeString(timeRemainingInMS / 1000); // FIXME: get rid of these / 1000s
        startForeground(1, createNotification(timeFromActivity));

        timer = new PomodoroTimer(timeRemainingInMS,1000) {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                sendBroadcast();
                updateNotification(getTimeString(timer.getMsRemaining() / 1000));
                Log.i("SHINOGI","Sending broadcast: " + timer.getMsRemaining());

            }
        };

        isPauseActive = intent.getBooleanExtra("isPauseActive",false);
        if (!isPauseActive) { timer.start(); }

        Log.i("SHINOGI","Starting service and thus timer.");
        Log.i("SHINOGI","Time remaining: " + timeRemainingInMS);

        return super.onStartCommand(intent, flags, startId);
    }


    public Notification createNotification(String text) {
        Log.i("SHINOGI","Creating notification");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,notificationIntent,0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nya, Time Remaining")
                .setContentText(text)
                .setSmallIcon(R.drawable.timer_icon)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();
    }

    public void updateNotification(String text) {
        Notification notification = createNotification(text);
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(1, notification);
    }

    public void sendBroadcast() {
        Intent intentLocal = new Intent();
        intentLocal.setAction("Counter");
        intentLocal.putExtra("timeRemaining",timer.getMsRemaining());
        intentLocal.putExtra("isPauseActive",isPauseActive);
        sendBroadcast(intentLocal);
    }


}
