package mini.pomodoro_delta;

import static mini.pomodoro_delta.TimerService.formatTimeString;
import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * TODO:
 * - SharedPreferences for PAUSE system to keep track of msRemaining and totalMsElapsed upon
 *   closing out of app (Keep in Main Activity; purely for UI purposes)
 *
 * - SharedPreferences for saving STATS (new activity)
 *
 * - Note: as long as a timer runs, timer service is running constantly.
 *
 * - Implement what to do when time runs out in Timer Service (sound, next action, etc. -->
 *   on Finish)
 *
 * - Implement what to do on long break
 */

public class MainActivity extends AppCompatActivity {
    private Alarm alarm;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Button btnSession;
    private Button btnBegin;
    private Button btnPause;
    private Button txtElapsed;
    private Button btnAlarm;
    private TextView txtTimer;
    private TextView txtStatus;

    // session stats
    private long msRemaining;
    private long msElapsed;
    private long totalMsElapsed;
    private int numPomodoros;
    private boolean isSession;
    private boolean isStudy;
    private boolean isPaused;

    // global stats
    private static long allTimeTotalMsElapsed;
    private static long allTimeMaxMsElapsed;
    private static int allTimeNumSessions;

    private static boolean isTimerFinished;
    private final long MS_STUDY_TIME = 25 * 60 * 1000;
    private final long MS_BREAK_TIME = 5 * 50 * 1000;
    private final long MS_LONG_BREAK_TIME = 15 * 60 * 1000;

    private BroadcastReceiver broadcastReceiver;

    // inherited
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedPreferences = getPreferences(MODE_PRIVATE);
        alarm = new Alarm(getApplicationContext());

        btnSession = findViewById(R.id.btn_session);
        btnBegin = findViewById(R.id.btn_begin);
        btnPause = findViewById(R.id.btn_pause);
        btnAlarm = findViewById(R.id.btn_alarm);
        txtTimer = findViewById(R.id.txt_timer);
        txtStatus = findViewById(R.id.txt_status);
        txtElapsed = findViewById(R.id.txt_elapsed);

        getUIData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v("Verbose","Broadcast received: " + isTimerFinished);
                    msRemaining = intent.getLongExtra("msRemaining", 0);
                    msElapsed = intent.getLongExtra("msElapsed", 0);
                    updateTimerUI(msRemaining,txtTimer,"");
                    updateTimerUI(totalMsElapsed + msElapsed,txtElapsed,"Time Elapsed: ");
                    if (isTimerFinished) {
                        alarm.playAlarmSound();
                        resumeUI();
                    }
                }
            };
        }
        IntentFilter intentFilter = new IntentFilter("TIMER_SERVICE_DATA_INTENT");
        registerReceiver(broadcastReceiver, intentFilter);

        resumeUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        recordUIData();
    }

    // buttons
    public void clickSession(View v) {
        isSession = !isSession;
        if (isSession) {
            txtStatus.setText(R.string.txt_status_session_active);
            btnSession.setText(R.string.btn_end_session);
            updateButtonUI(true,btnBegin,R.color.orange);
        } else {
            totalMsElapsed += msElapsed; // FIXME
            stopServiceTimer();
            resetUI();
            collectStats();
            alarm.stopAlarmSound();
            msElapsed = 0;
            totalMsElapsed = 0;
            msRemaining = 0;
            numPomodoros = 0;
            isStudy = false;
            isPaused = false;
            isSession = false;
        }
    }

    public void clickBegin(View v) {
        isStudy = !isStudy;
        if (isStudy) {
            numPomodoros++;
            startServiceTimer(MS_STUDY_TIME);
            if (numPomodoros % 4 == 0 && numPomodoros != 0) {
                btnBegin.setText(R.string.btn_bgn_long_break);
            } else { btnBegin.setText(R.string.btn_bgn_break); }
            txtStatus.setText(R.string.txt_status_study);
        } else {
            if (numPomodoros % 4 == 0 && numPomodoros != 0) {
                startServiceTimer(MS_LONG_BREAK_TIME);
                txtStatus.setText(R.string.txt_status_long_break);
            } else {
                startServiceTimer(MS_BREAK_TIME);
                txtStatus.setText(R.string.txt_status_break);
            }
            btnBegin.setText(R.string.btn_bgn_study);
        }
        updateButtonUI(true,btnPause,R.color.red);
        updateButtonUI(false,btnBegin,R.color.orange);
    }

    public void clickPause(View v) {
        isPaused = !isPaused;
        int status = isStudy ? R.string.txt_status_study : R.string.txt_status_break;
        if (isPaused) {
            totalMsElapsed += msElapsed;
            stopServiceTimer();

            btnPause.setText(R.string.btn_resume_timer);
            txtStatus.setText(R.string.txt_status_paused);
            txtTimer.setTextColor(getResources().getColor(R.color.light_yellow));
        } else {
            startServiceTimer(msRemaining);

            btnPause.setText(R.string.btn_pause_timer);
            txtStatus.setText(status);
            txtTimer.setTextColor(getResources().getColor(R.color.white));
        }
    }

    public void clickStats(View v) {
        openStatsActivity();
    }

    // UI handling
    public void recordUIData() {
        editor = sharedPreferences.edit();
        editor.putLong("msRemaining",msRemaining);
        editor.putLong("totalMsElapsed",totalMsElapsed);
        editor.putInt("numPomodoros",numPomodoros);
        editor.putBoolean("isSession",isSession);
        editor.putBoolean("isStudy",isStudy);
        editor.putBoolean("isPaused",isPaused);

        editor.putLong("allTimeTotalMsElapsed",allTimeTotalMsElapsed);
        editor.putLong("allTimeMaxMsElapsed",allTimeMaxMsElapsed);
        editor.putInt("allTimeNumSessions",allTimeNumSessions);

        editor.apply();
    }

    public void getUIData() {
        if (sharedPreferences != null) {
            msRemaining = sharedPreferences.getLong("msRemaining",0);
            totalMsElapsed = sharedPreferences.getLong("totalMsElapsed",0);
            numPomodoros = sharedPreferences.getInt("numPomodoros",0);
            isSession = sharedPreferences.getBoolean("isSession",false);
            isStudy = sharedPreferences.getBoolean("isStudy",false);
            isPaused = sharedPreferences.getBoolean("isPaused",false);

            allTimeTotalMsElapsed = sharedPreferences.getLong("allTimeTotalMsElapsed",allTimeTotalMsElapsed);
            allTimeMaxMsElapsed = sharedPreferences.getLong("allTimeMaxMsElapsed",allTimeMaxMsElapsed);
            allTimeNumSessions = sharedPreferences.getInt("allTimeNumSessions",allTimeNumSessions);

        } else { // FIXME check redundancy
            msRemaining = 0;
            totalMsElapsed = 0;
            numPomodoros = 0;
            isSession = false;
            isStudy = false;
            isPaused = false;
        }
    }

    private void resumeUI() { // must be after shared preferences
        if (isSession && !(txtStatus.getText().equals("Current Status:\nSession Active"))) {
            btnSession.setText(R.string.btn_end_session);
            updateButtonUI(isTimerFinished,btnBegin,R.color.orange);
            updateButtonUI(!isTimerFinished,btnPause,R.color.red);
            if (isTimerFinished) { // timer finished

                getDataFromTimerFinish();
                txtStatus.setText(isStudy ? R.string.txt_status_study : R.string.txt_status_break);
                if (!isStudy && (numPomodoros % 4 == 0)) {
                    txtStatus.setText(R.string.txt_status_long_break);
                }
                btnBegin.setText(!isStudy ? R.string.btn_bgn_study : R.string.btn_bgn_break);
                if (isStudy && (numPomodoros % 4 == 0)) {
                    btnBegin.setText(R.string.btn_bgn_long_break);
                }
                updateTimerUI(totalMsElapsed,txtElapsed,"Time Elapsed: ");
            } else {
                btnPause.setText(isPaused ? R.string.btn_resume_timer : R.string.btn_pause_timer);
                txtTimer.setTextColor(getResources().getColor(
                        isPaused ? R.color.light_yellow : R.color.white));
                if (isPaused) {
                    txtStatus.setText(R.string.txt_status_paused);
                } else {
                    txtStatus.setText(isStudy ? R.string.txt_status_study : R.string.txt_status_break);
                    if (!isStudy && (numPomodoros % 4 == 0)) {
                        txtStatus.setText(R.string.txt_status_long_break);
                    }
                }
                if (msRemaining == 0) {
                    updateButtonUI(true,btnBegin,R.color.orange); // FIXME combine if statements + clean
                    updateButtonUI(false,btnPause,R.color.red);
                }
                updateTimerUI(totalMsElapsed + msElapsed,txtElapsed,"Time Elapsed: ");
            }
            updateTimerUI(msRemaining,txtTimer,"");
        }
        updateAlarmButtonUI(Alarm.getIsAlarmOn());
    }

    private void resetUI() {
        btnSession.setText(R.string.btn_bgn_session);
        btnBegin.setText(R.string.btn_bgn_study);
        btnPause.setText(R.string.btn_pause_timer);
        txtElapsed.setText(R.string.txt_status_default_elapsed);
        txtStatus.setText(R.string.txt_status_session_inactive);
        txtTimer.setText(R.string.txt_default_study_time);
        txtTimer.setTextColor(getResources().getColor(R.color.white));
        updateButtonUI(false,btnBegin,R.color.orange);
        updateButtonUI(false,btnPause,R.color.red);
    }

    private void updateButtonUI(boolean enable, Button btn, int btnColor) {
        if (enable) {
            btn.setEnabled(true);
            btn.setBackgroundColor(getResources().getColor(btnColor));
            btn.setTextColor(getResources().getColor(R.color.white));
        } else {
            btn.setEnabled(false);
            btn.setBackgroundColor(getResources().getColor(R.color.background_lighter_grey));
            btn.setTextColor(getResources().getColor(R.color.light_grey));
        }
    }

    private void updateTimerUI(long ms, View UI, String prefix) {
        String time = prefix + formatTimeString(ms);
        ((TextView) UI).setText(time);
    }

    // service interaction

    private void startServiceTimer(long msInitialTime){
        Intent intentToServiceStart = new Intent(MainActivity.this,TimerService.class);
        intentToServiceStart.putExtra("msInitialTime",msInitialTime);
        intentToServiceStart.putExtra("isStudy",isStudy);
        startService(intentToServiceStart);
    }

    private void stopServiceTimer(){
        try {
            Intent intentToServiceStop = new Intent(MainActivity.this,TimerService.class);
            stopService(intentToServiceStop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDataFromTimerFinish() {
        if (isTimerFinished) {
            if (isStudy) { totalMsElapsed += msElapsed + msRemaining; }
            msElapsed = 0;
            msRemaining = 0;
            stopServiceTimer();
            isTimerFinished = !isTimerFinished;
        }
    }

    public static void setIsTimerFinished(boolean b) {
        isTimerFinished = b;
    }

    // stats interaction
    private void collectStats() {
        Log.i("test","collecting stats" + totalMsElapsed);
        if (totalMsElapsed > (60 * 1000)) { // session longer than 1 min
            allTimeNumSessions++;
            allTimeTotalMsElapsed += totalMsElapsed;
            if (totalMsElapsed > allTimeMaxMsElapsed) {
                allTimeMaxMsElapsed = totalMsElapsed;
                txtStatus.setText(R.string.txt_personal_best);
            }
        } else {
            txtStatus.setText(R.string.txt_session_not_recorded);
        }
    }

    private void openStatsActivity() {
        Intent statsIntent = new Intent(this,StatsActivity.class);
        Log.i("test",""+allTimeNumSessions);
        statsIntent.putExtra("allTimeTotalMsElapsed", allTimeTotalMsElapsed);
        statsIntent.putExtra("allTimeMaxMsElapsed", allTimeMaxMsElapsed);
        statsIntent.putExtra("allTimeNumSessions", allTimeNumSessions);
        startActivity(statsIntent);
    }

    // sound interaction

    private void updateAlarmButtonUI(boolean enable) {
        txtElapsed.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
        btnAlarm.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        btnAlarm.setEnabled(true);
    }

    public void clickAlarm(View v) {
        alarm.stopAlarmSound();
        updateAlarmButtonUI(false);
    }

}