package mini.pomodoro_delta;

import static mini.pomodoro_delta.App.CHANNEL_ID;
import static mini.pomodoro_delta.MainActivity.setIsTimerFinished;

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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class TimerService extends Service {
    private Ringtone ringtone;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private CountDownTimer timer;
    private long msRemaining;
    private long previousMsRemaining;
    private long msElapsed;
    private boolean isStudy;

    // inherited
    @Override
    public void onDestroy() {
        Log.v("Verbose","onDestroy SERVICE called");
        super.onDestroy();

        if (timer != null) { timer.cancel(); }
        turnOffWakeLock();
        stopAlarmSound();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("Verbose","onStartCommand SERVICE called");
        msRemaining = intent.getLongExtra("msInitialTime",0);
        isStudy = intent.getBooleanExtra("isStudy",false);
        if (!intent.getBooleanExtra("isPaused",false)) {
            createTimer(msRemaining);
            startTimer();
        }

        startForeground(1, createNotification(formatTimeString(msRemaining)));
        turnOnWakeLock();

        return super.onStartCommand(intent, flags, startId);
    }

    // timer
    public void createTimer(long ms) {
        if (timer != null) { timer.cancel(); }
        setIsTimerFinished(false);
        previousMsRemaining = ms;
        timer = new CountDownTimer(ms,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                msRemaining = millisUntilFinished;
                if (isStudy) {
                    msElapsed += (previousMsRemaining - msRemaining);
                }
                previousMsRemaining = msRemaining;
                sendBroadcast();
                updateNotification(formatTimeString(msRemaining));
            }

            @Override
            public void onFinish() {
                setIsTimerFinished(true);
                sendBroadcast();
                updateNotification("Timer finished!");
                playAlarmSound();
            }
        };
    }

    public void startTimer() {
        if (timer != null) {
            timer.start();
        } else {
            Log.e("Error","Attempted to start timer when timer does not exist.");
        }
    }

    public static String formatTimeString(long ms) {
        String time;

        long sec = ms / 1000;
        int hrsElapsed = (int) (sec / 60) / 60;
        int minElapsed = (int) (sec / 60) % 60;
        int secElapsed = (int) sec % 60;

        if (hrsElapsed == 0) {
            time = String.format(Locale.ENGLISH,"%02d:%02d", minElapsed, secElapsed);
        } else {
            time = String.format(Locale.ENGLISH,"%02d:%02d:%02d", hrsElapsed, minElapsed, secElapsed);
        }

        return time;
    }

    // broadcast interaction
    public Notification createNotification(String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,notificationIntent,0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Time Remaining")
                .setContentText(message)
                .setSmallIcon(R.drawable.timer_icon)
                .setColor(getResources().getColor(R.color.teal))
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build();
    }

    public void updateNotification(String message) {
        Notification notification = createNotification(message);
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(1, notification);
    }

    private void sendBroadcast() {
        Log.v("Verbose","Sending Broadcast from Service | isStudy: " + isStudy
                + " | msRemaining: " + msRemaining
                + " | msElapsed: " + msElapsed);

        Intent intentLocal = new Intent("TIMER_SERVICE_DATA_INTENT");
        intentLocal.putExtra("msRemaining",msRemaining);
        intentLocal.putExtra("msElapsed",msElapsed);
        sendBroadcast(intentLocal);
    }

    // wake lock interaction
    public void turnOnWakeLock() {
        if (wakeLock != null) { wakeLock.release(); }
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "TimerService::WakelockTag");
        wakeLock.acquire(msRemaining);
        Log.i("wakeLock","turning on");
    }

    public void turnOffWakeLock() {
        if (wakeLock != null) {
            wakeLock.release();
            Log.i("wakeLock","turning off");
        }
    }

    // sound interaction
    private void playAlarmSound() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                vibrator.vibrate(1000);
            }

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        try {
            ringtone.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}