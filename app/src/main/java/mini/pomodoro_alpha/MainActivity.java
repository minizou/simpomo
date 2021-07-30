package mini.pomodoro_alpha;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private PomodoroTimer pomodoroTimer;
    BroadcastReceiver broadcastReceiver;
    SharedPreferences sharedPreferences;

    private TextView txtTimer;
    private TextView txtStatus;
    private Button btnSession;
    private Button txtTimeElapsed;
    private Button btnBeginStudying;
    private Button btnBeginBreak;
    private Button btnPause;

    private boolean isBreakActive;
    private boolean isPauseActive;
    private boolean isSessionActive;
    private int numPomodoros;
    private long msRemaining;
    private long secElapsed;

    private final long STUDY_TIME_MS = 1 * 20 * 1000;
    private final long STUDY_TIME_SEC = STUDY_TIME_MS / 1000;
    private final long BREAK_TIME_MS = 1 * 30 * 1000;
    private final long LONG_BREAK_TIME_MS = 1 * 70 * 1000;

    // Inherited Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("SHINOGI","OnCreate, creating Main Activity.");

        // initializing UI objects
        txtTimer = findViewById(R.id.txt_timer);
        txtTimeElapsed = findViewById(R.id.txt_time_elapsed);
        txtStatus = findViewById(R.id.txt_status);
        btnBeginStudying = findViewById(R.id.btn_begin_studying);
        btnBeginBreak = findViewById(R.id.btn_begin_break);
        btnPause = findViewById(R.id.btn_pause);
        btnSession = findViewById(R.id.btn_session);
        sharedPreferences = getPreferences(MODE_PRIVATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("Counter");

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                msRemaining = intent.getLongExtra("timeRemaining",msRemaining);
                isPauseActive = intent.getBooleanExtra("isPauseActive",isPauseActive);
                saveServiceData();
                Log.i("SHINOGI","Broadcast received by Main Activity: " + msRemaining);
            }
        };

        Log.i("SHINOGI","Broadcast Receiver REGISTERED.");
        registerReceiver(broadcastReceiver, intentFilter);

        restartUI();

        try {
            stopService(new Intent(MainActivity.this, PomodoroService.class));
        } catch (Exception e) { }

        getServiceData();
    }

    @Override
    protected void onResume() {
        Log.i("SHINOGI","onResume (MAIN)");
        if (isSessionActive) {
            stopPomodoroService();
            Log.i("SHINOGI","Broadcast Receiver UNREGISTERED.");
            unregisterReceiver(broadcastReceiver);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("SHINOGI","onPause (MAIN)");
        if (isSessionActive) {
            startPomodoroService();
        }
        saveServiceData();
        super.onPause();
    }

    // Service Methods

    private void startPomodoroService() {
        Log.i("SHINOGI","Calling startPomodoroService.");
        Intent intentPomodoroService = new Intent(this,PomodoroService.class);
        intentPomodoroService.putExtra("msRemaining",pomodoroTimer.getMsRemaining());
        intentPomodoroService.putExtra("isPauseActive",isPauseActive);
        startService(intentPomodoroService);
    }

    private void stopPomodoroService() {
        Intent intentPomodoroServiceStop = new Intent(this,PomodoroService.class);
        stopService(intentPomodoroServiceStop);
    }

    private void saveServiceData() {
        Log.i("SHINOGI","Sending shared preference data");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isSessionActive) {
            Log.i("SHINOGI","Shared data applied: " + msRemaining + " | " + isPauseActive);
        } else {
            msRemaining = 0;
            isPauseActive = false;
            Log.i("SHINOGI","Clearing data: " +  msRemaining + " | " + isPauseActive);
        }
        editor.putLong("msRemaining",msRemaining);
        editor.putBoolean("isPauseActive",isPauseActive);
        editor.apply();
    }

    private void getServiceData() {
        Log.i("SHINOGI","Attempting to retrieve service data.");
        if (sharedPreferences != null) {
            msRemaining = sharedPreferences.getLong("msRemaining",msRemaining);
            isPauseActive = sharedPreferences.getBoolean("isPauseActive",isPauseActive);
            Log.i("SHINOGI","Service data obtained: " + msRemaining);
        } else { // FIXME: check [else] if it is redundant
            msRemaining = 0;
            isPauseActive = false;
        }
    }

    // UI methods

    public void restartUI() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        Log.i("SHINOGI","UI initialized");
        txtStatus.setText(R.string.intro_message);
        txtTimer.setText(pomodoroTimer.getTimeString(STUDY_TIME_SEC));
        btnPause.setText(R.string.pause_message);
        btnBeginBreak.setText(R.string.begin_break_message);
        btnSession.setText(R.string.begin_session_message);
        changeButtonUI(btnSession,true,R.color.pomodoro_green);
        changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
        changeButtonUI(btnPause,false,R.color.pomodoro_red);
    }

    public void updateTimerUI() {
        msRemaining = pomodoroTimer.getMsRemaining();
        long secRemaining = msRemaining / 1000;
        txtTimer.setText(pomodoroTimer.getTimeString(secRemaining));
        txtTimeElapsed.setText(pomodoroTimer.getTimeString(secElapsed));
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

    public void startTimer(long TIME_IN_MS) {
        // Log.i("SHINOGI","Starting Timer in Main Activity.");
        pomodoroTimer = new PomodoroTimer(TIME_IN_MS,1000) {
            @Override
            public void onTick(long timeInMS) {
                super.onTick(timeInMS);
                if (!isBreakActive) { secElapsed++; }
                updateTimerUI();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                if (!isBreakActive) { numPomodoros++; }
                isBreakActive = !isBreakActive;
                updateStatusUI();
            }
        };
        pomodoroTimer.start();
    }

    public void beginSession(View v) {
        // Log.i("SHINOGI","Begin Session pressed.");
        if (isSessionActive) {
            restartUI();
        } else {
            btnSession.setText(R.string.end_session_message);
            beginStudying(btnBeginStudying);
        }
        isSessionActive = !isSessionActive;
    }

    public void pauseResumeTimer(View v) {
        // Log.i("SHINOGI","Pause button pressed.");
        System.out.println("Beginning: " + isPauseActive);
        if (!isPauseActive) {
            msRemaining = pomodoroTimer.getMsRemaining();
            pomodoroTimer.cancel();
            if (!isBreakActive) { secElapsed--; }

            txtTimer.setTextColor(getResources().getColor(R.color.text_yellow));
            btnPause.setText(R.string.resume_message);
        } else {
            startTimer(msRemaining); // startTimer requires time in ms
            txtTimer.setTextColor(getResources().getColor(R.color.text_white));
            btnPause.setText(R.string.pause_message);
        }
        isPauseActive = !isPauseActive;
        System.out.println("Here: " + isPauseActive);
    }

    public void beginStudying(View v) {
        // Log.i("SHINOGI","Begin studying button pressed.");
        startTimer(STUDY_TIME_MS);
        txtStatus.setText(R.string.study_time_message);
        changeButtonUI(btnBeginStudying,false,R.color.pomodoro_teal);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);
    }

    public void beginBreak(View v) {
        // Log.i("SHINOGI","Begin break button pressed.");
        if (numPomodoros % 4 == 0) {
            startTimer(LONG_BREAK_TIME_MS);
            txtStatus.setText(R.string.long_break_time_message);
        } else {
            startTimer(BREAK_TIME_MS);
            txtStatus.setText(R.string.break_time_message);
            btnBeginBreak.setText(R.string.begin_break_message);
        }
        changeButtonUI(btnBeginBreak,false,R.color.pomodoro_orange);
        changeButtonUI(btnPause,true,R.color.pomodoro_red);
    }
}