/*
 * code adapted from the stock music app
 */

package org.abrantix.rockon.rockonnggl;

//import android.bluetooth.BluetoothA2dp;
import org.abrantix.rockon.rockonnggl.R;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

/**
 * 
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {

	private static final String TAG = "MediaButtonIntentReceiver";
	
    private static final int MSG_LONGPRESS_TIMEOUT = 1;
    private static final int LONG_PRESS_DELAY = 1000;

    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LONGPRESS_TIMEOUT:
                    if (!mLaunched) {
                        Context context = (Context)msg.obj;
                        Intent i = new Intent();
                        i.putExtra("autoshuffle", "true");
                        i.setClass(context, RockOnNextGenGL.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        mLaunched = true;
                    }
                    break;
            }
        }
    };
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i(TAG, intent.getAction());
    	
    	/**
    	 *  are we using headset buttons? 
    	 */
    	if(!PreferenceManager.getDefaultSharedPreferences(context).
    			getBoolean(
    					context.getString(
    							R.string.preference_key_use_headset_buttons), 
    					true))
    	{
    		return;
    	}
    	
    	/**
    	 * Ignore if a call is in progress or the phone is ringing
    	 */
    	if(((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState()
    			== TelephonyManager.CALL_STATE_RINGING 
    			||
    		((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getCallState()
    			== TelephonyManager.CALL_STATE_OFFHOOK)
    	{
    		return;
    	}
    
    	/**
    	 *  So the user has chosen to use the headset in ^3 
    	 *  and no call is active or incoming
    	 *   - do our magic 
    	 */
        String intentAction = intent.getAction();
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
            Intent i = new Intent(context, RockOnNextGenService.class);
            i.setAction(Constants.SERVICECMD);
            i.putExtra(Constants.CMDNAME, Constants.CMDPAUSE);
            context.startService(i);
        } else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = (KeyEvent)
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();

            // single quick press: pause/resume. 
            // double press: next track
            // long press: start auto-shuffle mode.
            
            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    command = Constants.CMDSTOP;
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    command = Constants.CMDTOGGLEPAUSE;
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    command = Constants.CMDNEXT;
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    command = Constants.CMDPREVIOUS;
                    break;
            }

            if (command != null) {
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mDown) {
                        if (Constants.CMDTOGGLEPAUSE.equals(command)
                                && mLastClickTime != 0 
                                && eventtime - mLastClickTime > LONG_PRESS_DELAY) {
                            mHandler.sendMessage(
                                    mHandler.obtainMessage(MSG_LONGPRESS_TIMEOUT, context));
                        }
                    } else {
                        // if this isn't a repeat event

                        // The service may or may not be running, but we need to send it
                        // a command.
                        Intent i = new Intent(context, RockOnNextGenService.class);
                        i.setAction(Constants.SERVICECMD);
                        if (keycode == KeyEvent.KEYCODE_HEADSETHOOK &&
                                eventtime - mLastClickTime < 300) {
                        	Log.i(TAG, "Sending NEXT command");
                            i.putExtra(Constants.CMDNAME, Constants.CMDNEXT);
                            context.startService(i);
                            mLastClickTime = 0;
                        } else {
                        	Log.i(TAG, "Sending other command: "+command);
                            i.putExtra(Constants.CMDNAME, command);
                            context.startService(i);
                            mLastClickTime = eventtime;
                        }

                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    mHandler.removeMessages(MSG_LONGPRESS_TIMEOUT);
                    mDown = false;
                }
                abortBroadcast();
            }
        }
    }
}
