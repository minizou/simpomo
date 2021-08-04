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
import android.os.Bundle;
import android.os.PowerManager;
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
    private long msRemaining;
    private long secElapsed;

    // Constants
    private final long STUDY_TIME_MS = 25 * 60 * 1000;
    private final long STUDY_TIME_SEC = STUDY_TIME_MS / 1000;
    private final long BREAK_TIME_MS = 5 * 60 * 1000;
    private final long LONG_BREAK_TIME_MS = 15 * 60 * 1000;

    // Inherited Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("Counter");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                msRemaining = intent.getLongExtra("timeRemaining",msRemaining);
                isPauseActive = intent.getBooleanExtra("isPauseActive",isPauseActive);
                isBreakActive = intent.getBooleanExtra("isBreakActive",isBreakActive);
                numPomodoros = intent.getIntExtra("numPomodoros",numPomodoros);
                secElapsed = intent.getLongExtra("secElapsed",secElapsed);
                saveServiceData();
            }
        };

        registerReceiver(broadcastReceiver, intentFilter);

        restartUI();
        stopPomodoroService();
        getServiceData();
    }

    @Override
    protected void onResume() {
        if (isSessionActive) {
            stopPomodoroService();
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (Exception e) { }
            if (!isPauseActive) { secElapsed--; } // #fixlater --> replacing secElapsed with Initial Amount - secRemaining
        }

        if (msRemaining != 0) {
            restoreSession(); // TODO: new, fixme so that it's not just for msRemaining = 0
        }

        if (isPauseActive && !isBreakActive) {
            secElapsed++; // #fixlater
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
        Log.i("SHINOGI","startPomodoroService() (MAIN)");

        if (pomodoroTimer != null) { // TODO: new, added to fix timer running as service ran
            Log.i("SHINOGI","Cancelled timer in startPomodoroService");
            pomodoroTimer.cancel();
        }

        Intent intentPomodoroService = new Intent(this,PomodoroService.class);
        intentPomodoroService.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // FIXME: prevents multiple duplicates of service?
        intentPomodoroService.putExtra("msRemaining",pomodoroTimer.getMsRemaining());
        intentPomodoroService.putExtra("secElapsed",secElapsed);
        intentPomodoroService.putExtra("isPauseActive",isPauseActive);
        intentPomodoroService.putExtra("isBreakActive",isBreakActive);
        intentPomodoroService.putExtra("numPomodoros",numPomodoros);
        startService(intentPomodoroService);

    }

    private void stopPomodoroService() {
        Log.i("SHINOGI","stopPomodoroService (MAIN)");
        try {
            Intent intentPomodoroServiceStop = new Intent(this,PomodoroService.class);
            stopService(intentPomodoroServiceStop);
        } catch (Exception e) { }
    }

    private void saveServiceData() {
        // Log.i("SHINOGI","saveServiceData (MAIN)");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isSessionActive) {
            Log.i("SHINOGI","Shared data applied: " + msRemaining + " | " + isPauseActive + " | " + isBreakActive);
        } else {
            msRemaining = 0;
            isPauseActive = false;
            isBreakActive = false;
            Log.i("SHINOGI","Clearing data: " +  msRemaining + " | " + isPauseActive + " | " + isBreakActive);
        }
        editor.putLong("msRemaining",msRemaining);
        editor.putLong("secElapsed",secElapsed);
        editor.putBoolean("isPauseActive",isPauseActive);
        editor.putBoolean("isBreakActive",isBreakActive);
        editor.putInt("numPomodoros",numPomodoros);

        editor.apply();
    }

    private void getServiceData() {
        Log.i("SHINOGI","getServiceData() (MAIN)");
        Log.i("SHINOGI","Attempting to retrieve service data.");
        if (sharedPreferences != null) {
            msRemaining = sharedPreferences.getLong("msRemaining",msRemaining);
            secElapsed = sharedPreferences.getLong("secElapsed",secElapsed);
            isPauseActive = sharedPreferences.getBoolean("isPauseActive",isPauseActive);
            isBreakActive = sharedPreferences.getBoolean("isBreakActive",isBreakActive);
            numPomodoros = sharedPreferences.getInt("numPomodoros",numPomodoros);
            Log.i("SHINOGI","Service data obtained: " + msRemaining);
        } else { // FIXME: check [else] if it is redundant
            msRemaining = 0;
            numPomodoros = 0;
            secElapsed = 0;
            isBreakActive = false;
            isPauseActive = false;
        }
    }

    // UI methods, continued

    public void restartUI() {
        if (pomodoroTimer != null) {
            Log.i("SHINOGI","Timer cancelled.");
            pomodoroTimer.cancel();
        }
        Log.i("SHINOGI","UI restarted.");
        txtStatus.setText(R.string.intro_message);
        txtTimer.setTextColor(getResources().getColor(R.color.text_white));
        txtTimer.setText(PomodoroTimer.getTimeString(STUDY_TIME_SEC));
        txtTimeElapsed.setText(PomodoroTimer.getTimeString(secElapsed));
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
        Log.i("SHINOGI","Updating Timer UI.");
        long secRemaining = msRemaining / 1000;
        txtTimer.setText(PomodoroTimer.getTimeString(secRemaining));
        txtTimeElapsed.setText(PomodoroTimer.getTimeString(secElapsed));
    }

    public void changeButtonUI(Button b, boolean enabled, int color) {
        Log.i("SHINOGI","Changing Button UI.");
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
        Log.i("SHINOGI","Switching button availability :" + isBreakActive);
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
        Log.i("SHINOGI","Creating Timer.");
        pomodoroTimer = new PomodoroTimer(TIME_IN_MS,1000) {
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                msRemaining = timeInMS;
                if (!isBreakActive) {
                    Log.i("MISHIMA","createTimer, secElapsed++: " + secElapsed);
                    secElapsed++;
                }
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                Log.i("SHINOGI","Timer has finished.");
                super.onFinish();
                if (!isBreakActive) { numPomodoros++; }
                isBreakActive = !isBreakActive;
                updateStatusUI();
                // startAlarm(); TODO
            }
        };
    }

    public void startTimer() {
        isPauseActive = false;
        Log.i("SHINOGI","Starting timer.");
        if (pomodoroTimer != null) {
            pomodoroTimer.start();
        }
    }

    public void beginSession(View v) {
        Log.i("SHINOGI","Begin Session pressed.");
        if (isSessionActive) {
            numPomodoros = 0;
            secElapsed = 0;
            restartUI();
        } else {
            btnSession.setText(R.string.end_session_message);
            beginStudying(btnBeginStudying);
        }
        isSessionActive = !isSessionActive;
    }

    public void pauseResumeTimer(View v) {
        Log.i("SHINOGI","Pause button pressed: " + isPauseActive);
        System.out.println("Beginning: " + isPauseActive);
        if (!isPauseActive) { // pause not on, so it pauses for you
            msRemaining = pomodoroTimer.getMsRemaining();
            pomodoroTimer.cancel();
            if (!isBreakActive) {
                Log.i("MISHIMA","pauseResumeTimer, secElapsed--");
                secElapsed--;
            }

            txtTimer.setTextColor(getResources().getColor(R.color.text_yellow));
            btnPause.setText(R.string.resume_message);
            isPauseActive = !isPauseActive;
        } else {
            createTimer(msRemaining); // startTimer requires time in ms
            startTimer();
            txtTimer.setTextColor(getResources().getColor(R.color.text_white));
            btnPause.setText(R.string.pause_message);
        }
        System.out.println("Here: " + isPauseActive);
    }

    public void beginStudying(View v) {
        Log.i("SHINOGI","Begin studying button pressed.");
        msRemaining = STUDY_TIME_MS; // TODO: just added
        createTimer(msRemaining); // TODO: below this is potentially redundant, call restoreStudying
        startTimer();
        txtStatus.setText(R.string.study_time_message);
        changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);
    }

    public void beginBreak(View v) {
        Log.i("SHINOGI","Begin break button pressed.");
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

    /*
    public void stopAlarmSound(View v) {
        ringtone.stop();

        changeButtonUI(btnStopAlarm,false,R.color.pomodoro_green);
    }

    public void startAlarm() {
        Log.i("KEIJI","SERVICE createAlarmSound: " + secElapsed);
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null) { alert =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        ringtone.play();

        changeButtonUI(btnStopAlarm,true,R.color.pomodoro_green);
    }
     */
}