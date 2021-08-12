package mini.pomodoro_delta;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Alarm {
    protected Context context;
    private static Ringtone ringtone;
    private static boolean isAlarmOn;
    private static Vibrator vibrator;

    public Alarm(Context context){
        this.context = context;
    }

    public void playAlarmSound() {
        try {
            vibrate();
            if (!isAlarmOn) { // does not redo alarm if already on
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                ringtone = RingtoneManager.getRingtone(context, notification);
                ringtone.play();
                isAlarmOn = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAlarmSound() {
        try {
            ringtone.stop();
            isAlarmOn = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void vibrate() {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(1000);
        }
    }

    public static boolean getIsAlarmOn() {
        return isAlarmOn;
    }
}
