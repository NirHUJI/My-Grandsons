package niry.mygrandsons;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.WhatsApi;

import java.util.Set;

/**
 * This class encapsulates the WhatsApi as a service
 */
public class WhatsApiService extends Service {
    public static final String ACTION = "ACTION";
    public static final String ACTION_KILL= "ACTION_KILL";
    public static final String ACTION_START= "ACTION_START";
    public static final String ACTION_FAST_POLL= "ACTION_FAST_POLL";
    public static final String ACTION_WAKE_AND_POLL= "ACTION_WAKE_AND_POLL";

    private boolean running=true;
    private int TIME_TO_SLEEP= 15000;
    private WhatsApi wa;

    /**
     * Connects to the whatsApp server and login.
     */
    private void connectAndLogin() {

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE);
        String username = preferences.getString(MainActivity.SHARED_PHONE_NUMBER, MainActivity.NOT_EXISTS);
        String identity = MainActivity.IDENTITY;
        String nickname = MainActivity.NICK_NAME;

        try {
            wa = new WhatsApi(username, identity, nickname, getApplicationContext());
            MessageProcessor mp = new MyGrandsonsMessageProcessor(getApplicationContext());
            wa.setNewMessageBind(mp);
            wa.connect();
            //login:
            String password= preferences.getString(MainActivity.SHARED_PASSWORD, MainActivity.NOT_EXISTS);
            wa.loginWithPassword(password);


        } catch (Exception e) {
            //todo
            e.printStackTrace();
        }
    }

    /**
     * Polls one message
     */
    private void pollMessage() {
        //we poll twice- first time for getting the headers and second time for the actual message.
        try {
            wa.pollMessages();
//            Thread.sleep(3000); //sleep to give time to the message poller.
//            wa.pollMessages();
            wa.sendPresence();
        } catch (Exception e) {
           Log.w("custom", "cannot pull");
        }
    }

    /**
     * This method called on WAKE_AND_POLL command and try to redownload failed downloads.
     */
    private void reDownloadFailedFiles(){
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION , MODE_PRIVATE);
        Set<String> set = preferences.getStringSet(MainActivity.WAIT_FOR_DOWNLOAD, null);
        if (set!=null){
            for( String url : set){
                if(url.endsWith(".jpg")){
                   new SaveImageTask(getApplicationContext()).execute(url);
                }
                if(url.endsWith(".mp4")){
            new SaveVideoTask(getApplicationContext()).execute(url);
                }
                if(url.endsWith(".txt")){
                   new SaveVideoTask(getApplicationContext()).execute(url);
                }
            }
        }
        //clean the wait for download set:
        SharedPreferences.Editor ed = preferences.edit();
        ed.putStringSet(MainActivity.WAIT_FOR_DOWNLOAD, null );
        ed.commit();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        final int startIdCopy=startId;
        if (WhatsApiService.ACTION_START.equals(intent.getStringExtra(WhatsApiService.ACTION))) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectAndLogin();
                    //we run in loop until the service will get a kill command and stop the loop:
                    while(running) {
                        pollMessage();
                        try {
                            Thread.sleep(TIME_TO_SLEEP);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    stopSelf();
                }
            }).start();

        }else if(WhatsApiService.ACTION_FAST_POLL.equals(intent.getStringExtra(WhatsApiService.ACTION))){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pollMessage();
                }
            }).start();

        } else if(WhatsApiService.ACTION_KILL.equals(intent.getStringExtra(WhatsApiService.ACTION))){
            //kill the pulling loop:
            this.running=false;
            stopSelf();
        } else if(WhatsApiService.ACTION_WAKE_AND_POLL.equals(intent.getStringExtra(WhatsApiService.ACTION))){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectAndLogin();
                    reDownloadFailedFiles();
                    pollMessage();
                    try {
                        Thread.sleep(TIME_TO_SLEEP);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stopSelf();
                }
            }).start();
        }


        return START_NOT_STICKY;

    }
}
