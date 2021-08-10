package mini.pomodoro_delta;

import static mini.pomodoro_delta.App.CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class TimerService extends Service {
    private CountDownTimer timer;
    private long msRemaining;
    private long previousMsRemaining;
    private long msElapsed;
    private boolean isStudy;

    @Override
    public void onDestroy() {
        Log.v("Verbose","onDestroy SERVICE called");
        super.onDestroy();
        if (timer != null) { timer.cancel(); }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("Verbose","Starting service.");
        msRemaining = intent.getLongExtra("msInitialTime",0);
        isStudy = intent.getBooleanExtra("isStudy",false);
        if (!intent.getBooleanExtra("isPaused",false)) {
            createTimer(msRemaining);
            startTimer();
        }

        String message = formatTimeString(msRemaining) + " | " + formatTimeString(msElapsed);
        startForeground(1, createNotification(message));

        return super.onStartCommand(intent, flags, startId);
    }

    // timer methods

    public void createTimer(long ms) {
        if (timer != null) { timer.cancel(); }

        previousMsRemaining = ms;
        timer = new CountDownTimer(ms,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.v("Verbose","On tick: " + msRemaining + " " + msElapsed);
                msRemaining = millisUntilFinished;
                if (isStudy) {
                    msElapsed += (previousMsRemaining - msRemaining);
                }
                previousMsRemaining = msRemaining;
                sendBroadcast();
                updateNotification(
                        formatTimeString(msRemaining) + " | " + formatTimeString(msElapsed));
            }

            @Override
            public void onFinish() {
                Log.v("Verbose","Timer finished.");
                sendBroadcast();
                updateNotification("Timer finished.");
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
            time = String.format("%02d:%02d", minElapsed, secElapsed);
        } else {
            time = String.format("%02d:%02d:%02d", hrsElapsed, minElapsed, secElapsed);
        }

        return time;
    }

    // broadcast methods

    //private void startForegroundNotification() {}

    public Notification createNotification(String message) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0,notificationIntent,0);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Time Remaining | Elapsed")
                .setContentText(message)
                .setSmallIcon(R.drawable.timer_icon)
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
        Log.v("Verbose","Sending broadcast: " + msRemaining + " " + msElapsed);
        Intent intentLocal = new Intent("TIMER_SERVICE_DATA_INTENT");
        intentLocal.putExtra("msRemaining",msRemaining);
        intentLocal.putExtra("msElapsed",msElapsed);
        sendBroadcast(intentLocal);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}