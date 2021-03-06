package niry.mygrandsons;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Receive the boot-complete action and set the wakeup alarm
 */
public class BootReceiver extends BroadcastReceiver {
    public final int TIME_INTERVAL= 900000;


    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent pollIntent= new Intent(context, WhatsApiService.class);
            pollIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_WAKE_AND_POLL);
            PendingIntent operation = PendingIntent.getService(context, 0, pollIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+TIME_INTERVAL, TIME_INTERVAL, operation);
        }
    }
}
