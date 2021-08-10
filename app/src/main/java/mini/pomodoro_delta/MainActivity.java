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
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Button btnSession;
    private Button btnBegin;
    private Button btnPause;
    private Button txtElapsed;
    private TextView txtTimer;
    private TextView txtStatus;

    private long msRemaining;
    private long msElapsed;
    private long totalMsElapsed;
    private boolean isSession;
    private boolean isStudy;
    private boolean isPaused;

    private final long MS_STUDY_TIME = 1 * 50 * 1000;
    private final long MS_BREAK_TIME = 5 * 60 * 1000;

    private BroadcastReceiver broadcastReceiver;

    // inherited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        System.out.println("onCreate | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        sharedPreferences = getPreferences(MODE_PRIVATE);

        btnSession = findViewById(R.id.btn_session);
        btnBegin = findViewById(R.id.btn_begin);
        btnPause = findViewById(R.id.btn_pause);
        txtTimer = findViewById(R.id.txt_timer);
        txtStatus = findViewById(R.id.txt_status);
        txtElapsed = findViewById(R.id.txt_elapsed);

        getUIData();
        System.out.println("OnCreate: " + isSession + " | " + isStudy + " | " + isPaused + " | "  + msRemaining + " | "  + msElapsed + " | "  + totalMsElapsed);
        resumeUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("onResume | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    msRemaining = intent.getLongExtra("msRemaining", 0);
                    msElapsed = intent.getLongExtra("msElapsed", 0);
                    updateTimerUI(msRemaining,txtTimer,"");
                    updateTimerUI(totalMsElapsed + msElapsed,txtElapsed,"Time Elapsed: ");
                    Log.v("Verbose","Broadcast Received: " + msRemaining + " " + msElapsed);
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
        System.out.println("onPause | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        recordUIData();
    }
    // UI

    public void clickSession(View v) {
        isSession = !isSession;
        if (isSession) {
            txtStatus.setText("Current Status:\nSession Active");
            btnSession.setText(R.string.txt_end_session);
            updateButtonUI(true,btnBegin,R.color.orange);
        } else {
            stopServiceTimer();
            totalMsElapsed = 0;
            msRemaining = 0;
            isStudy = false;
            isPaused = false;
            isSession = false;
            resetUI();
        }
        System.out.println("clickSession | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    public void clickBegin(View v) {
        isStudy = !isStudy;
        if (isStudy) {
            startServiceTimer(MS_STUDY_TIME);
            btnBegin.setText(R.string.txt_bgn_break);
            txtStatus.setText(R.string.txt_status_study);
        } else {
            startServiceTimer(MS_BREAK_TIME);
            btnBegin.setText(R.string.btn_bgn_study);
            txtStatus.setText(R.string.txt_status_break);
        }
        updateButtonUI(true,btnPause,R.color.red);
        updateButtonUI(false,btnBegin,R.color.orange);
        System.out.println("clcikBegin | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    public void clickPause(View v) {
        isPaused = !isPaused;
        int status = isStudy ? R.string.txt_status_study : R.string.txt_status_break;
        if (isPaused) {
            totalMsElapsed += msElapsed;
            stopServiceTimer();

            btnPause.setText(R.string.txt_resume_timer);
            txtStatus.setText(R.string.txt_status_paused);
            txtTimer.setTextColor(getResources().getColor(R.color.light_yellow));
        } else {
            startServiceTimer(msRemaining);

            btnPause.setText(R.string.btn_pause_timer);
            txtStatus.setText(status);
            txtTimer.setTextColor(getResources().getColor(R.color.white));
        }
        System.out.println("clickPause | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    public void recordUIData() {
        editor = sharedPreferences.edit();
        editor.putLong("msRemaining",msRemaining);
        editor.putLong("totalMsElapsed",totalMsElapsed);
        editor.putBoolean("isSession",isSession);
        editor.putBoolean("isStudy",isStudy);
        editor.putBoolean("isPaused",isPaused);
        editor.apply();

        System.out.println("recordUIData | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        System.out.println("sharedPrefs | isSession: "
                + sharedPreferences.getBoolean("isSession",false)
                + " isStudy: " + sharedPreferences.getBoolean("isStudy",false)
                + " isPaused: " + sharedPreferences.getBoolean("isPaused",false)
                + " msRemaining: " + sharedPreferences.getLong("msRemaining",0)
        );
    }

    public void getUIData() {
        if (sharedPreferences != null) {
            msRemaining = sharedPreferences.getLong("msRemaining",0);
            totalMsElapsed = sharedPreferences.getLong("totalMsElapsed",0);
            isSession = sharedPreferences.getBoolean("isSession",false);
            isStudy = sharedPreferences.getBoolean("isStudy",false);
            isPaused = sharedPreferences.getBoolean("isPaused",false);
        } else { // FIXME check redundancy
            msRemaining = 0;
            totalMsElapsed = 0;
            isSession = false;
            isStudy = false;
            isPaused = false;
        }
        System.out.println("getUIData | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    private void resumeUI() { // must be after shared preferences
        System.out.println("resumeUI | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        if (isSession) {
            if (msRemaining > 1000) {
                updateButtonUI(true,btnPause,R.color.red);
                if (isPaused) {
                    updateTimerUI(totalMsElapsed + msElapsed,txtElapsed,"Time Elapsed: ");
                    btnPause.setText(R.string.txt_resume_timer);
                    txtStatus.setText(R.string.txt_status_paused);
                    txtTimer.setTextColor(getResources().getColor(R.color.light_yellow));
                }
            } else {
                isStudy = !isStudy;
                updateButtonUI(true,btnBegin,R.color.orange);
                btnBegin.setText(isStudy ? R.string.btn_bgn_study : R.string.txt_bgn_break);
            }
            btnSession.setText(R.string.txt_end_session);
            txtStatus.setText(isStudy ? R.string.txt_status_study : R.string.txt_status_break);
            updateTimerUI(msRemaining,txtTimer,"");
        }
    }

    private void resetUI() {
        btnSession.setText(R.string.btn_bgn_session);
        btnBegin.setText(R.string.btn_bgn_study);
        btnPause.setText(R.string.btn_pause_timer);
        txtElapsed.setText(R.string.txt_time_elapsed_zero);
        txtStatus.setText(R.string.txt_status_session_inactive);
        txtTimer.setText(R.string.txt_study_time);
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
        System.out.println("updateButtonUI | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    private void updateTimerUI(long ms, View UI, String prefix) {
        String time = prefix + formatTimeString(ms);
        ((TextView) UI).setText(time);
        System.out.println("updateTimerUI | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    // service interaction

    public void startServiceTimer(long msInitialTime){
        Intent intentToServiceStart = new Intent(MainActivity.this,TimerService.class);
        intentToServiceStart.putExtra("msInitialTime",msInitialTime);
        intentToServiceStart.putExtra("isStudy",isStudy);
        startService(intentToServiceStart);
        System.out.println("startServiceTimer | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
    }

    public void stopServiceTimer(){
        try {
            Intent intentToServiceStop = new Intent(MainActivity.this,TimerService.class);
            stopService(intentToServiceStop);
            System.out.println("stopServiceTimer | isSession: " + isSession + " isStudy: " + isStudy + " isPaused: " + isPaused + " msRemaining: " + msRemaining);
        } catch (Exception e) {
            Log.e("Error","Cannot stop service.");
        }
    }
}