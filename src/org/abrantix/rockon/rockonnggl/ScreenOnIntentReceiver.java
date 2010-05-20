/*
 * code adapted from the stock music app
 */

package org.abrantix.rockon.rockonnggl;

//import android.bluetooth.BluetoothA2dp;
import org.abrantix.rockon.rockonnggl.R;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * 
 */
public class ScreenOnIntentReceiver extends BroadcastReceiver {

	private static final String TAG = "ScreenOnIntentReceiver";
	
//    private static final int MSG_LONGPRESS_TIMEOUT = 1;
//    private static final int LONG_PRESS_DELAY = 1000;
//
//    private static long mLastClickTime = 0;
//    private static boolean mDown = false;
//    private static boolean mLaunched = false;

//    private static Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case MSG_LONGPRESS_TIMEOUT:
//                    if (!mLaunched) {
//                        Context context = (Context)msg.obj;
//                        Intent i = new Intent();
//                        i.putExtra("autoshuffle", "true");
//                        i.setClass(context, RockOnNextGenGL.class);
//                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                        context.startActivity(i);
//                        mLaunched = true;
//                    }
//                    break;
//            }
//        }
//    };
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i(TAG, intent.getAction());
    	
    	/**
    	 *  are we using the lockscreen? 
    	 */
    	if(!PreferenceManager.getDefaultSharedPreferences(context).
    			getBoolean(
    					context.getString(
    							R.string.preference_key_lock_screen), 
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
    	 * Start our Lock Screen App
    	 */
    	Intent i = new Intent(
    			context, 
    			LockScreen.class);
    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	context.startActivity(i);
    	
    }
    
//    Handler mLockScreenLauncherHandler = new Handler()
//    {
//    	@Override
//    	public void handleMessage(Message msg)
//    	{
//
//    	}
//    };
}
