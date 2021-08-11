package mini.pomodoro_delta;

import static mini.pomodoro_delta.TimerService.formatTimeString;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class StatsActivity extends AppCompatActivity {
    private TextView statMsElapsed;
    private TextView statNumSessions;
    private TextView statAvgMs;
    private TextView statMaxMs;

    private long allTimeTotalMsElapsed;
    private long allTimeMaxMsElapsed;
    private int allTimeNumSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        statMsElapsed = findViewById(R.id.stat_msElapsed);
        statNumSessions = findViewById(R.id.stat_numSessions);
        statAvgMs = findViewById(R.id.stat_avgMS);
        statMaxMs = findViewById(R.id.stat_maxMS);

        Intent intentFromMain = getIntent();
        allTimeTotalMsElapsed = intentFromMain.getLongExtra("allTimeTotalMsElapsed",allTimeTotalMsElapsed);
        allTimeMaxMsElapsed = intentFromMain.getLongExtra("allTimeMaxMsElapsed",allTimeMaxMsElapsed);
        allTimeNumSessions = intentFromMain.getIntExtra("allTimeNumSessions",allTimeNumSessions);

        updateStats();
    }

    private void updateStats() {
        statMsElapsed.setText(formatTimeString(allTimeTotalMsElapsed));
        statNumSessions.setText(String.valueOf(allTimeNumSessions));
        statMaxMs.setText(formatTimeString(allTimeMaxMsElapsed));

        long avgMs = 0;
        if (allTimeNumSessions > 0) {
            avgMs = allTimeTotalMsElapsed / allTimeNumSessions;
        }
        statAvgMs.setText(formatTimeString(avgMs));
    }

    public void clickBack(View v) {
        finish();
    }
}