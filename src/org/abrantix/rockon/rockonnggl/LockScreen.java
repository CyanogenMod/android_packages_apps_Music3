package org.abrantix.rockon.rockonnggl;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.inputmethodservice.Keyboard.Key;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
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
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		Log.i(TAG, "CREATE");
		
        keyGuardLock = 
    		((KeyguardManager)getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE)).
    			newKeyguardLock(TAG);
        
        setupWindow();
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
//		Log.i(TAG, "START");
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
//		Log.i(TAG, "RESUME");
        
		if(mService != null)
			unlockPhone();
		else
			lockPhone();

        if(mService == null)
        	connectToService();
        
        try
        {
	        if(mService != null && mService.isPlaying())
	        	showLockScreen();
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
//		Log.i(TAG, "PAUSE");

//		lockPhone();
//		finish();
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
//		Log.i(TAG, "STOP");
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
//		Log.i(TAG, "DESTROY");
			
		try
		{
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
			lockPhone();
			break;
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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
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
//		Log.i(TAG, "Locking phone!");
		if(Integer.valueOf(Build.VERSION.SDK) >= 7 || mUnlocked)
		{
	    	keyGuardLock.reenableKeyguard();
			mUnlocked = false;
		}

	}
	
	public void unlockPhone()
	{
    	/**
    	 * Unlock Phone
    	 */
//		Log.i(TAG, "Unlocking phone");
		keyGuardLock.disableKeyguard();
		mUnlocked = true;
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
	}
	
	private void updateFields()
	{
		if(mService != null)
		{
			try
			{
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
				doNext();
				updateFields();
			}
			else if(v.getId() == R.id.control_play_lock)
			{
				doPlayPause();
				updateFields();
			}
			else if(v.getId() == R.id.control_prev_lock)
			{
				doPrevious();
				updateFields();
			}
			else if(v.getId() == R.id.lock_screen_main)
			{
				lockPhone();
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
