package mini.pomodoro_alpha;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/** TODO:
 * - Consider using WakefulBroadcastReceiver or WAKE-LOCK so that service runs and is received while
 *   phone is asleep / timed out (current issue is broadcastreceiver does not run during timeout)
 * - Need to resolve what to do when timer runs to 00:00 in service.
 * - Need to add notification noise at 00:00 in Main Activity
 *
 * Relevant links to explore for next time:
 * https://developer.android.com/training/scheduling/wakelock
 * https://developer.android.com/reference/android/content/BroadcastReceiver
 * https://stackoverflow.com/questions/44149921/broadcast-receiver-not-working-when-app-is-closed
 */

public class MainActivity extends AppCompatActivity {
    // Functionality Objects
    private BroadcastReceiver broadcastReceiver;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private PomodoroTimer pomodoroTimer;
    private Ringtone ringtone;
    private SharedPreferences sharedPreferences;
    private IntentFilter intentFilter;

    // UI Objects
    private Button btnSession;
    private Button btnBeginStudying;
    private Button btnBeginBreak;
    private Button btnPause;
    private Button btnStopAlarm;
    private Button txtTimeElapsed;
    private TextView txtTimer;
    private TextView txtStatus;

    // Session Data
    private boolean isBreakActive;
    private boolean isPauseActive;
    private boolean isSessionActive;
    private int numPomodoros;
    private long initialMsRemaining;
    private long msRemaining;
    private long msElapsed;
    private long msElapsedTotal;

    // Constants
    private final long STUDY_TIME_MS = 5 * 1 * 1000;
    private final long STUDY_TIME_SEC = STUDY_TIME_MS / 1000;
    private final long BREAK_TIME_MS = 3 * 1 * 1000;
    private final long LONG_BREAK_TIME_MS = 10 * 1 * 1000;

    // Inherited Methods

    private void deleteLater() {
        long test = msRemaining / 1000;
        long test2 = msElapsed / 1000;
        // Log.i("deleteLater","msRemaining: " + msRemaining + " | " + test);
        // Log.i("deleteLater","msElapsed: " + msElapsed+ " | " + test2);
        // Log.i("deleteLater","initialMsRemaining: " + initialMsRemaining + " | " + initialMsRemaining);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        deleteLater();

        // initializing UI objects
        sharedPreferences = getPreferences(MODE_PRIVATE);
        btnBeginStudying = findViewById(R.id.btn_begin_studying);
        btnBeginBreak = findViewById(R.id.btn_begin_break);
        btnPause = findViewById(R.id.btn_pause);
        btnSession = findViewById(R.id.btn_session);
        btnStopAlarm = findViewById(R.id.btn_stop_alarm);
        txtTimer = findViewById(R.id.txt_timer);
        txtTimeElapsed = findViewById(R.id.txt_time_elapsed);
        txtStatus = findViewById(R.id.txt_status);

        intentFilter = new IntentFilter();
        intentFilter.addAction("Counter");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                msRemaining = intent.getLongExtra("timeRemaining",msRemaining);
                isSessionActive = intent.getBooleanExtra("isSessionActive",isSessionActive);
                isPauseActive = intent.getBooleanExtra("isPauseActive",isPauseActive);
                isBreakActive = intent.getBooleanExtra("isBreakActive",isBreakActive);
                numPomodoros = intent.getIntExtra("numPomodoros",numPomodoros);
                msElapsed = intent.getLongExtra("msElapsed",msElapsed);
                saveServiceData();
                Log.i("deleteLater","Receiving broadcast: " + msRemaining + " | " + msElapsed);
            }
        };

        restartUI();
        stopPomodoroService();
        getServiceData();
    }

    @Override
    protected void onResume() {
        Log.i("deleteLater","onResume: " + isSessionActive + " | msRemaining: " + msRemaining);
        if (isSessionActive) {
            stopPomodoroService();
            if (msRemaining >= 1000) {
                restoreSession(); // TODO: new, fixme so that it's not just for msRemaining = 0
            } else { // TODO
                if (isBreakActive) {
                    changeButtonUI(btnBeginBreak,true,R.color.pomodoro_orange);
                    changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
                } else {
                    changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
                    changeButtonUI(btnBeginStudying,true,R.color.pomodoro_teal);
                }
                changeButtonUI(btnPause,false,R.color.pomodoro_red);
            }
        }

        if (wakeLock != null) {
            wakeLock.release();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (isSessionActive) {
            startPomodoroService();
            turnOnWakeLock();
        }
        saveServiceData();
        super.onPause();
    }

    // Other Methods

    public void turnOnWakeLock() {
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire(msRemaining);
    }

    public void restoreSession() {
        isSessionActive = true;
        btnSession.setText(R.string.end_session_message);
        updateTimerUI();

        createTimer(msRemaining);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);

        if (isBreakActive) {
            if (numPomodoros % 4 == 0) {
                txtStatus.setText(R.string.long_break_time_message);
            } else {
                txtStatus.setText(R.string.break_time_message);
                btnBeginBreak.setText(R.string.begin_break_message);
            }
            changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
        } else {
            txtStatus.setText(R.string.study_time_message);
            changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        }

        if (!isPauseActive) {
            startTimer();
        } else {
            isPauseActive = false;
            pauseResumeTimer(btnPause);
        }
    }

    // Service Methods

    private void startPomodoroService() {
        if (pomodoroTimer != null) { // TODO: new, added to fix timer running as service ran
            pomodoroTimer.cancel();
        }

        Intent intentPomodoroService = new Intent(this,PomodoroService.class);
        intentPomodoroService.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // FIXME: prevents multiple duplicates of service?
        intentPomodoroService.putExtra("msRemaining",pomodoroTimer.getMsRemaining());
        intentPomodoroService.putExtra("msElapsed",msElapsed);
        intentPomodoroService.putExtra("isPauseActive",isPauseActive);
        intentPomodoroService.putExtra("isBreakActive",isBreakActive);
        intentPomodoroService.putExtra("numPomodoros",numPomodoros);
        intentPomodoroService.putExtra("isSessionActive",isSessionActive); // FIXME redundant
        startService(intentPomodoroService);

    }

    private void stopPomodoroService() {
        try {
            Intent intentPomodoroServiceStop = new Intent(this,PomodoroService.class);
            stopService(intentPomodoroServiceStop);
        } catch (Exception e) { }
    }

    private void saveServiceData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!isSessionActive) {
            msRemaining = 0;
            isPauseActive = false;
            isBreakActive = false;
        }
        editor.putBoolean("isSessionActive",isSessionActive);
        editor.putLong("msRemaining",msRemaining);
        editor.putLong("msElapsed",msElapsed);
        editor.putBoolean("isPauseActive",isPauseActive);
        editor.putBoolean("isBreakActive",isBreakActive);
        editor.putInt("numPomodoros",numPomodoros);

        editor.apply();
    }

    private void getServiceData() {
        if (sharedPreferences != null) {
            isSessionActive = sharedPreferences.getBoolean("isSessionActive",isSessionActive);
            msRemaining = sharedPreferences.getLong("msRemaining",msRemaining);
            msElapsed = sharedPreferences.getLong("msElapsed",msElapsed);
            isPauseActive = sharedPreferences.getBoolean("isPauseActive",isPauseActive);
            isBreakActive = sharedPreferences.getBoolean("isBreakActive",isBreakActive);
            numPomodoros = sharedPreferences.getInt("numPomodoros",numPomodoros);
        } else { // FIXME: check [else] if it is redundant
            msRemaining = 0;
            numPomodoros = 0;
            msElapsed = 0;
            isBreakActive = false;
            isSessionActive = false;
            isPauseActive = false;
        }
    }

    // UI methods, continued

    public void restartUI() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        txtStatus.setText(R.string.intro_message);
        txtTimer.setTextColor(getResources().getColor(R.color.text_white));
        txtTimer.setText(PomodoroTimer.getTimeString(STUDY_TIME_SEC));
        txtTimeElapsed.setText(PomodoroTimer.getTimeString(msElapsed / 1000));
        btnPause.setText(R.string.pause_message);
        btnBeginBreak.setText(R.string.begin_break_message);
        btnSession.setText(R.string.begin_session_message);
        changeButtonUI(btnSession,true,R.color.pomodoro_green);
        changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
        changeButtonUI(btnPause,false,R.color.pomodoro_red);
        changeButtonUI(btnStopAlarm,false,R.color.pomodoro_green);
    }

    public void updateTimerUI() {
        long secRemaining = msRemaining / 1000;
        txtTimer.setText(PomodoroTimer.getTimeString(secRemaining));
        txtTimeElapsed.setText(PomodoroTimer.getTimeString(msElapsed / 1000));
    }

    public void changeButtonUI(Button b, boolean enabled, int color) {
        if (enabled) {
            b.setTextColor(getResources().getColor(R.color.text_white));
            b.setBackgroundColor(getResources().getColor(color));
            b.setEnabled(true);
        } else {
            b.setTextColor(getResources().getColor(R.color.text_darker_grey));
            b.setBackgroundColor(getResources().getColor(R.color.background_lighter_grey));
            b.setEnabled(false);
        }
    }

    public void updateStatusUI() {
        if (!isBreakActive) {
            changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
            changeButtonUI(btnBeginStudying,true,R.color.pomodoro_teal);
        } else {
            if (numPomodoros % 4 == 0) { btnBeginBreak.setText(R.string.begin_long_break_message); }
            changeButtonUI(btnBeginBreak,true,R.color.pomodoro_orange);
            changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        }
        changeButtonUI(btnPause,false,R.color.pomodoro_red);
    }

    // Timer + Button Methods

    public void createTimer(long TIME_IN_MS) {
        deleteLater();

        initialMsRemaining = TIME_IN_MS;
        pomodoroTimer = new PomodoroTimer(TIME_IN_MS,1000) {
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                msRemaining = timeInMS;
                if (!isBreakActive) {
                    msElapsed += (initialMsRemaining - msRemaining);
                }
                initialMsRemaining = msRemaining;
                updateTimerUI();
                deleteLater();
            }
            @Override
            public void onFinish() {
                if (!isBreakActive) {
                    msElapsedTotal += msElapsed;
                    numPomodoros++;
                }
                isBreakActive = !isBreakActive;

                updateStatusUI();
                // startAlarm(); TODO

                super.onFinish();
            }
        };
    }

    public void startTimer() {
        isPauseActive = false;
        if (pomodoroTimer != null) {
            pomodoroTimer.start();
        }
    }

    public void beginSession(View v) {
        if (isSessionActive) {
            numPomodoros = 0;
            msElapsed = 0;
            restartUI();
        } else {
            btnSession.setText(R.string.end_session_message);
            beginStudying(btnBeginStudying);
        }
        isSessionActive = !isSessionActive;
    }

    public void pauseResumeTimer(View v) {
        if (!isPauseActive) { // pause not on, so it pauses for you
            msRemaining = pomodoroTimer.getMsRemaining();
            if (!isBreakActive) { msElapsedTotal += msElapsed; }
            pomodoroTimer.cancel();
            txtTimer.setTextColor(getResources().getColor(R.color.text_yellow));
            btnPause.setText(R.string.resume_message);
            isPauseActive = !isPauseActive;
        } else {
            createTimer(msRemaining); // startTimer requires time in ms
            startTimer();
            txtTimer.setTextColor(getResources().getColor(R.color.text_white));
            btnPause.setText(R.string.pause_message);
        }
    }

    public void beginStudying(View v) {
        msRemaining = STUDY_TIME_MS; // TODO: just added
        createTimer(msRemaining); // TODO: below this is potentially redundant, call restoreStudying
        startTimer();
        txtStatus.setText(R.string.study_time_message);
        changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);
    }

    public void beginBreak(View v) {
        if (numPomodoros % 4 == 0) {
            createTimer(LONG_BREAK_TIME_MS);
            txtStatus.setText(R.string.long_break_time_message);
            btnBeginBreak.setText(R.string.begin_break_message);
        } else {
            createTimer(BREAK_TIME_MS);
            txtStatus.setText(R.string.break_time_message);
        }
        startTimer();
        changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);
    }

    // Sound Methods

    public void stopAlarmSound(View v) {
        ringtone.stop();

        changeButtonUI(btnStopAlarm,false,R.color.pomodoro_green);
    }

    public void startAlarm() {

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(1000);
        }

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null) { alert =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        ringtone.play();

        changeButtonUI(btnStopAlarm,true,R.color.pomodoro_green);
    }
}