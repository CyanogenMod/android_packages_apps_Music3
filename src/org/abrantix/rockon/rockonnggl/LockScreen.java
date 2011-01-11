package org.abrantix.rockon.rockonnggl;

import java.util.Calendar;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.KeyguardManager.OnKeyguardExitResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.inputmethodservice.Keyboard.Key;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class LockScreen extends Activity{

	protected static final String TAG = "LockScreen";

	public IRockOnNextGenService mService;

	KeyguardLock 	keyGuardLock;
	boolean			mUnlocked = false;
	boolean			mExplicitUnlock = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, "CREATE");
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(
        		//WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,   
        		WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        		0);
		
        setupWindow();
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
    	attachBroadcastReceivers();
		Log.i(TAG, "START");
	}
	
	@Override
	public void onRestart()
	{
		super.onStart();
		Log.i(TAG, "RESTART");
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		Log.i(TAG, "RESUME");
        
        keyGuardLock = 
    		((KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE)).
    			newKeyguardLock(TAG);
		
//		if(mService != null)
//			unlockPhone();
//		else
//			lockPhone();

        if(mService == null)
        	connectToService();
        
        try
        {
	        if(mService != null && mService.isPlaying())
	        {
//	        	WakeLock wakeLock =
//	        		((PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE))
//	        		.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "CUBED");
//	    		wakeLock.acquire(10);
	    		
	    		setupWindow();
	    		unlockPhone();	    			
	    		showLockScreen();	    		
	        }
	        else if(mService != null && !mService.isPlaying())
	        {
	    		lockPhone();
	    		finish();
	        }
        }
        catch(RemoteException e)
        {
        	e.printStackTrace();
        	finish();
        }
    }
	
	@Override
	public void onPause()
	{
		super.onPause();
		Log.i(TAG, "PAUSE");

//		else
			lockPhone();
		
//		finish();
			
		if(mExplicitUnlock)
			mExplicitUnlock = false;
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		Log.i(TAG, "STOP");
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG, "DESTROY");
			
		try
		{
			if(mServiceConnection != null)
				unbindService(mServiceConnection);
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
//			lockPhone();
			return true;
//		case KeyEvent.KEYCODE_HOME:
//			lockPhone();
//			finish();
//			break;
			
		default:
			break;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	public void connectToService()
	{
		if(bindService(
				new Intent(getApplicationContext(), RockOnNextGenService.class),
				mServiceConnection, 
				0))
		{
//			Log.i(TAG, "Service binding successful");
		}
		else
		{
//			Log.i(TAG, "Service binding unsuccessful");
			/** Service is probably not running */
			finish();
		}
	}
	
	public void setupWindow()
	{		
		/**
		 * 2.0 only
		 */
		if(Integer.parseInt(Build.VERSION.SDK) >= 5) // 5
		{
			try {
				getWindow().addFlags(
					WindowManager.LayoutParams.class.getField("FLAG_SHOW_WHEN_LOCKED").getInt(null)
					| WindowManager.LayoutParams.class.getField("FLAG_DISMISS_KEYGUARD").getInt(null)
					| WindowManager.LayoutParams.class.getField("FLAG_SHOW_WALLPAPER").getInt(null));
//				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//              | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
////              | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//      		| WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
////              | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON); 
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/**
		 * 1.5+ 
		 * http://www.anddev.org/viewtopic.php?p=32367
		 * http://code.google.com/p/mylockforandroid/source/browse/trunk/myLockcupcake/src/i4nc4mp/myLock/cupcake/ManageKeyguard.java?spec=svn282&r=282
		 */
        
//    	/**
//    	 *  Blur&Dim the BG
//    	 */
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
//               WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, 
//       		WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//		WindowManager.LayoutParams params = getWindow().getAttributes();
//		params.dimAmount = 0.8f;
//		getWindow().setAttributes(params);
		
		/**
		 * Show the Home Wallpaper
		 */
	  	if(Integer.valueOf(Build.VERSION.SDK) >= 7)
	  	{
//	  		WallpaperManager wManager = WallpaperManager.getInstance(this);
//			findViewById(R.id.lock_screen_main).
//				setBackgroundDrawable(wManager.getFastDrawable());
	  	}
	  	else
	  	{
	  		// doo nothing;
	  	}
       
	}
	
	public void lockPhone()
	{   
		Log.i(TAG, "Locking phone!");
		Log.i(TAG, "Keyguard is: " + ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode());
		if(Integer.valueOf(Build.VERSION.SDK) < 5)
		{
			Log.i(TAG, "mUnlocked: "+mUnlocked);
			Log.i(TAG, "mExplicitUnlock: "+mExplicitUnlock);
			if(!mExplicitUnlock && mUnlocked)
			{
//				if(!((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode())
					keyGuardLock.reenableKeyguard();
			}

			mUnlocked= false;
			mLockHandler.sendEmptyMessageDelayed(0, 50);
		}

	}
	
	/**
	 * 
	 */
	Handler mLockHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			  if (((KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE)).
					  inKeyguardRestrictedInputMode())
			  {
				    Log.v(TAG,"--Trying to exit keyguard securely");
				    ((KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE)).
				    	exitKeyguardSecurely(
				    			new OnKeyguardExitResult() 
							    {
							      public void onKeyguardExitResult(boolean success) 
							      {
							        keyGuardLock.reenableKeyguard();
							        //this call ensures the keyguard comes back at screen off
							        //without this call, all future disable calls will be blocked
							        //for not following the lockscreen rules
							        //in other words reenable immediately restores a paused lockscreen
							        //but only queues restore for next screen off if a secure exit has been done already
							        if (success) {
							          Log.v(TAG,"--Keyguard exited securely");
//							          callback.LaunchOnKeyguardExitSuccess();
							        } else {
							          Log.v(TAG,"--Keyguard exit failed");
							        }
							      }
							    }
				    		);
			  } else {
//				    callback.LaunchOnKeyguardExitSuccess();
			  }
		}
	};
	
	public void unlockPhone()
	{
    	/**
    	 * Unlock Phone
    	 */
		Log.i(TAG, "Unlocking phone");
		Log.i(TAG, "Keyguard is: " + ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode());
		if(Integer.valueOf(Build.VERSION.SDK) < 5)
		{	
			keyGuardLock.disableKeyguard();
			mUnlocked = true;
		}
	}
	
	public void showLockScreen()
	{
		setContentView(R.layout.lock_screen_layout);
		updateFields();
		attachListeners();
	}
	
	private void attachListeners()
	{
		findViewById(R.id.control_next_lock).setOnClickListener(mLockClickListener);
		findViewById(R.id.control_play_lock).setOnClickListener(mLockClickListener);
		findViewById(R.id.control_prev_lock).setOnClickListener(mLockClickListener);
		findViewById(R.id.lock_screen_main).setOnClickListener(mLockClickListener);
		findViewById(R.id.lock_screen_unlock_button).setOnClickListener(mLockClickListener);
	}
	
	private void updateFields()
	{
		if(mService != null)
		{
			try
			{
				// set date
				Calendar c = Calendar.getInstance();
				String hour;
				String min;
				if(DateFormat.is24HourFormat(getApplicationContext()))
					hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
				else
					hour = String.valueOf(c.get(Calendar.HOUR));
				int minInt = c.get(Calendar.MINUTE);
				if(minInt<10) 
					min = '0'+String.valueOf(minInt);
				else
					min = String.valueOf(minInt);
				((TextView)findViewById(R.id.lock_screen_current_time)).
					setText(hour+":"+min);
				
				// set trackName
				((TextView)findViewById(R.id.lock_song)).
					setText(mService.getTrackName());
				// set artistName
				((TextView)findViewById(R.id.lock_artist)).
					setText(mService.getArtistName());
				// set albumName
				((TextView)findViewById(R.id.lock_album)).
					setText(mService.getAlbumName());
				// set play button
				if(mService.isPlaying())
					((ImageButton)findViewById(R.id.control_play_lock)).
							setImageDrawable(getResources().getDrawable(R.drawable.ic_appwidget_music_pause));
				else
					((ImageButton)findViewById(R.id.control_play_lock)).
						setImageDrawable(getResources().getDrawable(R.drawable.ic_appwidget_music_play));
			}
			catch(RemoteException e)
			{
				e.printStackTrace();
			}
		}
	}
		
	
	OnClickListener mLockClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.control_next_lock)
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				doNext();
				updateFields();
			}
			else if(v.getId() == R.id.control_play_lock)
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				doPlayPause();
				updateFields();
			}
			else if(v.getId() == R.id.control_prev_lock)
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				doPrevious();
				updateFields();
			}
			else if(v.getId() == R.id.lock_screen_main)
			{
//				lockPhone();
//				finish();
			}
			else if(v.getId() == R.id.lock_screen_unlock_button)
			{
//				unlockPhone();
				mExplicitUnlock = true;
				finish();
			}
		}
	};
	
	private void doNext()
	{
		try
		{
			mService.next();	
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
	private void doPlayPause()
	{
		try
		{
			if(mService.isPlaying())
				mService.pause();
			else
				mService.play();
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
	private void doPrevious()
	{
		try
		{
			mService.prev();
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}

    /**
     * attachBroadcastReceivers
     */
    private void attachBroadcastReceivers(){
    	/* service play status update */
        IntentFilter f = new IntentFilter();
        f.addAction(Constants.PLAYSTATE_CHANGED);
        f.addAction(Constants.META_CHANGED);
        f.addAction(Constants.PLAYBACK_COMPLETE);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
            	//don't need to worry about the type of intent - all the registered types have the same handling.
            	updateFields();
            } catch(Exception e){
            	e.printStackTrace();
            }
        }
    };
	
	ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service disconnected");
			mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connected");
            mService = IRockOnNextGenService.Stub.asInterface(service);
            try
            {
	            if(mService.isPlaying())
	            {
	    			Log.i(TAG, "Service is playing");
		            unlockPhone();
					showLockScreen();
	            }
	            else
	            {
	    			Log.i(TAG, "Service not playing");
	    			lockPhone();
	            	finish();
	            }
            }
            catch(RemoteException e)
            {
            	e.printStackTrace();
            }
		}
	};
	
}
