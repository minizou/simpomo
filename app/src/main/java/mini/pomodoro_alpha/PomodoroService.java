package mini.pomodoro_alpha;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import static mini.pomodoro_alpha.App.CHANNEL_ID;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class PomodoroService extends Service {
    private PomodoroTimer timer;
    private Ringtone ringtone;
    private boolean isPauseActive;
    private boolean isBreakActive;
    private int numPomodoros;
    private long secElapsed;

    private final String ELAPSE_MESSAGE = " | ";

    // Inherited Methods

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("SHINOGI","Service created.");
        Log.i("KEIJI","SERVICE onCreate: " + secElapsed);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i("KEIJI","SERVICE onDestroy: " + secElapsed);
        super.onDestroy();
        timer.cancel();
        // FIXME: DELETE 1 SEC RIGHT BEFORE LAST BROADCAST.
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        Log.i("SHINOGI","Destroying service.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SHINOGI","OnStartCommand (SERVICE)");
        Log.i("KEIJI","SERVICE onStartCommand: " + secElapsed);
        long timeRemainingInMS = intent.getLongExtra("msRemaining",0);
        secElapsed = intent.getLongExtra("secElapsed",secElapsed);
        String timeFromActivity = timer.getTimeString(timeRemainingInMS / 1000); // FIXME: get rid of these / 1000s
        String timeSecElapsed = timer.getTimeString(secElapsed);
        String message = timeFromActivity + ELAPSE_MESSAGE + timeSecElapsed;
        startForeground(1, createNotification(message));
        Log.i("KEIJI","SERVICE onStartCommand (notif initialized): " + secElapsed);

        timer = new PomodoroTimer(timeRemainingInMS,1000) {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                secElapsed++; // FIXME: secElapsed not correctly working
                sendBroadcast();
                updateNotification(getTimeString(timer.getMsRemaining() / 1000)
                        + ELAPSE_MESSAGE + timer.getTimeString(secElapsed));
                // updateNotification(getTimeString(timer.getMsRemaining() / 1000));
                Log.i("SHINOGI","Sending broadcast: " + timer.getMsRemaining() + " | " + secElapsed);
                // Log.i("KEIJI","SERVICE onTick: " + secElapsed);
            }

            @Override
            public void onFinish() {
                Log.i("KEIJI","SERVICE onFinish: " + secElapsed);
                super.onFinish();
                createAlarmSound();
            }
        };
        Log.i("KEIJI","SERVICE onStartCommand (timer initialized): " + secElapsed);

        numPomodoros = intent.getIntExtra("numPomodoros",0);
        isBreakActive = intent.getBooleanExtra("isBreakActive",false);
        isPauseActive = intent.getBooleanExtra("isPauseActive",false);
        if (!isPauseActive) {
            timer.start();
        }

        Log.i("SHINOGI","Starting service and thus timer.");
        Log.i("SHINOGI","Time remaining: " + timeRemainingInMS);

        Log.i("MISHIMA","onStartCommand (SERVICE), secElapsed--: " + secElapsed);
        secElapsed--; // FIXME: NEW

        return super.onStartCommand(intent, flags, startId);
    }

    // Notification Methods

    public Notification createNotification(String text) {
        Log.i("KEIJI","SERVICE createNotification: " + secElapsed);
        // Log.i("SHINOGI","Creating notification");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,notificationIntent,0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Time Remaining | Elapsed")
                .setContentText(text)
                .setSmallIcon(R.drawable.timer_icon)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();
    }

    public void updateNotification(String text) {
        Log.i("KEIJI","SERVICE updateNotification: " + secElapsed);
        Notification notification = createNotification(text);
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(1, notification);
    }

    public void sendBroadcast() {
        //Log.i("KEIJI","SERVICE sendBroadcast: " + secElapsed);
        //Log.i("SHINOGI","Sending broadcast (SERVICE)");
        Intent intentLocal = new Intent();
        intentLocal.setAction("Counter");
        intentLocal.putExtra("timeRemaining",timer.getMsRemaining());
        intentLocal.putExtra("isPauseActive",isPauseActive);
        intentLocal.putExtra("isBreakActive",isBreakActive);
        intentLocal.putExtra("numPomodoros",numPomodoros);
        intentLocal.putExtra("secElapsed",secElapsed);
        sendBroadcast(intentLocal);
    }

    // Notification Sound Methods

    public void createAlarmSound() {
        Log.i("KEIJI","SERVICE createAlarmSound: " + secElapsed);
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null) { alert =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        ringtone.play();
    }

}
