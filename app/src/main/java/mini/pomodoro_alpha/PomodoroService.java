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
    private long msElapsed;

    private final String ELAPSE_MESSAGE = " | ";

    // Inherited Methods

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long timeRemainingInMS = intent.getLongExtra("msRemaining",0);
        msElapsed = intent.getLongExtra("msElapsed",msElapsed);
        String timeFromActivity = timer.getTimeString(timeRemainingInMS / 1000); // FIXME: get rid of these / 1000s
        String timeSecElapsed = timer.getTimeString(msElapsed / 1000);
        String message = timeFromActivity + ELAPSE_MESSAGE + timeSecElapsed;
        startForeground(1, createNotification(message));

        timer = new PomodoroTimer(timeRemainingInMS,1000) {
            long initialMsRemaining = timeRemainingInMS;
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                if (!isBreakActive) {
                    msElapsed += (initialMsRemaining - timeInMS);
                }
                initialMsRemaining = timeInMS;
                sendBroadcast();
                updateNotification(getTimeString(timer.getMsRemaining() / 1000)
                        + ELAPSE_MESSAGE + timer.getTimeString(msElapsed / 1000));
            }
            @Override
            public void onFinish() {
                String message = "Timer finished. ";
                if (isBreakActive) {
                    message += "Study time!";
                } else {
                    message += "Break time!";
                }
                sendBroadcast();
                updateNotification(message);
                super.onFinish();
                createAlarmSound();
            }
        };

        numPomodoros = intent.getIntExtra("numPomodoros",0);
        isBreakActive = intent.getBooleanExtra("isBreakActive",false);
        isPauseActive = intent.getBooleanExtra("isPauseActive",false);

        if (!isPauseActive) {
            timer.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    // Notification Methods

    public Notification createNotification(String text) {
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
        Notification notification = createNotification(text);
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(1, notification);
    }

    public void sendBroadcast() {
        Log.i("deleteLater","Sending broadcast: " + timer.getMsRemaining() + " | " + msElapsed);
        Intent intentLocal = new Intent();
        intentLocal.setAction("Counter");
        intentLocal.putExtra("isSessionActive",true);
        intentLocal.putExtra("timeRemaining",timer.getMsRemaining());
        intentLocal.putExtra("isPauseActive",isPauseActive);
        intentLocal.putExtra("isBreakActive",isBreakActive);
        intentLocal.putExtra("numPomodoros",numPomodoros);
        intentLocal.putExtra("msElapsed",msElapsed);
        sendBroadcast(intentLocal);
    }

    // Notification Sound Methods

    public void createAlarmSound() {
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null) { alert =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        ringtone.play();
    }

}
