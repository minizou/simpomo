package mini.pomodoro_alpha;

import android.os.CountDownTimer;

public class PomodoroTimer extends CountDownTimer {
    private long msRemaining;
    private long studySecElapsed;

    public PomodoroTimer(long timeInMS, long countDownInterval) {
        super(timeInMS, countDownInterval);
        msRemaining = timeInMS;
    }

    @Override
    public void onTick(long timeInMS) {
        msRemaining = timeInMS;
    }

    @Override
    public void onFinish() {
    }

    // basic getters & setters
    public long getStudySecElapsed() {
        return studySecElapsed;
    }
    public long getMsRemaining() {
        return msRemaining;
    }
    public void incrementStudySecElapsed() { studySecElapsed++; }
    public void decrementStudySecElapsed() { studySecElapsed--; }

    // static methods
    public static String getTimeString(long sec) {
        int hrsElapsed = (int) (sec / 60) / 60;
        int minElapsed = (int) (sec / 60) % 60;
        int secElapsed = (int) sec % 60;

        if (hrsElapsed == 0) {
            return String.format("%02d:%02d", minElapsed, secElapsed);
        } else {
            return String.format("%02d:%02d:%02d", hrsElapsed, minElapsed, secElapsed);
        }
    }
}
