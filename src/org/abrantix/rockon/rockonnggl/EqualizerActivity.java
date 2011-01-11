package org.abrantix.rockon.rockonnggl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.abrantix.rockon.rockonnggl.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class EqualizerActivity extends Activity{
	private static final String TAG = "EqualizerActivity";
	IRockOnNextGenService	mService;
	EqSettings				mEqSettings;
	EqView					mEqView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
//		if(savedInstanceState != null)
//			mManualSearch = savedInstanceState.getBoolean(MANUAL_SEARCH);	
    }
	 
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		connectToService();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		disconnectService();
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstance)
	{
//		savedInstance.putBoolean(MANUAL_SEARCH, mManualSearch);
	}
	
//	@Override
	public void onAttachedToWindow() {
		// Some reflection for 1.5
		try {
			Method m = Activity.class.getMethod("onAttachedWindow", new Class[]{});
			m.invoke(this, new Object[]{});
//			super.onAttachedToWindow();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	    Window window = getWindow();
	    // Eliminates color banding
	    window.setFormat(PixelFormat.RGBA_8888);
	}  
	
	private void showEqualizer(EqSettings eqSettings) {
		setContentView(R.layout.eq_layout);
		mEqView = (EqView)findViewById(R.id.eq_view);
		mEqView.setEqSettings(eqSettings);
		if(eqSettings.isEnabled()) {
			((TextView)findViewById(R.id.eq_on_off)).setText(R.string.on);
			((TextView)findViewById(R.id.eq_on_off)).setBackgroundResource(R.drawable.eq_on_bg);
		} else {
			((TextView)findViewById(R.id.eq_on_off)).setText(R.string.off);
			((TextView)findViewById(R.id.eq_on_off)).setBackgroundResource(R.drawable.eq_off_bg);
		}
	}
	
	private void showEqualizerError() {
//		AlertDialog.Builder aD = new AlertDialog.Builder(this);
//		aD.setTitle(getString(R.string.manual_albumart_error));
//		aD.setMessage(getString(R.string.manual_albumart_no_album_specified));
//		aD.show();
	}
	
	public void attachListeners()
	{
		findViewById(R.id.eq_view).setOnTouchListener(new EqTouchListener(getApplicationContext()));
		findViewById(R.id.eq_on_off).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mService != null) {
					try {
						if(!mService.isEqEnabled()) 
							mService.enableEq();
						else
							mService.disableEq();
						showEqualizer(readEqSettingsFromService(mService));
						attachListeners();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			// DO STUFF
		}
		return super.onKeyDown(keyCode, event);
	}
	  	
	public void commitBandLevel(int bandIdx, int bandLevel) {
		if(mService != null) {
			try {
				Log.i(TAG, "Commiting: "+bandIdx+" // "+bandLevel);
				mService.setEqBandLevel(bandIdx, bandLevel);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static EqSettings readEqSettingsFromService(IRockOnNextGenService service) {
		EqSettings settings = new EqSettings();
		try{
			if(service.isEqEnabled()) {
				settings.setEnabled();
			} else {
				settings.setDisabled();
			}
			settings.mBandHz = service.getEqBandHz();
			settings.mBandLevels = service.getEqBandLevels();
			settings.mCurrentPreset = service.getEqCurrentPreset();
			settings.setLevelRange(service.getEqLevelRange());
			settings.mNumBands = service.getEqNumBands();
			Log.i(TAG, "Bands: "+settings.mNumBands);
			return settings;
		} catch(RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void connectToService() {
		Intent i = new Intent(this, RockOnNextGenService.class);
    	startService(i);
    	bindService(i, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	private void disconnectService() {
		if(mServiceConnection != null) {
			unbindService(mServiceConnection);
		}
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
	    @Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {
	        try{ 
	        	mService = IRockOnNextGenService.Stub.asInterface(obj);
	        	mService.trackPage(Constants.ANALYTICS_EQUALIZER_PAGE);
	        	mEqSettings = readEqSettingsFromService(mService);
	        	if(mEqSettings != null)
	        		showEqualizer(mEqSettings);
	        	else
	        		showEqualizerError();
	        	attachListeners();
	        } catch(RemoteException e) {
	        	e.printStackTrace();
	        }
	    }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
	};
}