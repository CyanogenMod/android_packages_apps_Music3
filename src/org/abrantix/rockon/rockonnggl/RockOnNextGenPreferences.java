package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;

public class RockOnNextGenPreferences extends PreferenceActivity{
	
	protected static final String TAG = "RockOnNextGenPreferences";
	IRockOnNextGenService	mService;

	@Override
	protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
//    	/**
//    	 *  Blur&Dim the BG
//    	 */
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
//                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, 
//        		WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//        WindowManager.LayoutParams params = getWindow().getAttributes();
//        params.dimAmount = 0.625f;
//        getWindow().setAttributes(params);
        
        /**
         * Get preferences layout from xml
         */
        addPreferencesFromResource(R.xml.rockonngglpreferences);
        
        /**
         * Initialize screen
         */
        initPreferences();
        
        /**
         * Connect to service
         */
        Intent i = new Intent(this, RockOnNextGenService.class);
    	startService(i);
    	bindService(i, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDestroy() {
		if(mService != null)
			unbindService(mServiceConnection);
		super.onDestroy();
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
	    @Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {
	        try{
		    	mService = IRockOnNextGenService.Stub.asInterface(obj);
		         mService.trackPage(Constants.ANALYTICS_PREFERENCES_PAGE);
	        } catch(RemoteException e) {
	        	e.printStackTrace();
	        }
	    }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private void initPreferences(){
		/* Use headset */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_use_headset_buttons))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_use_headset_buttons), true));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_use_headset_buttons))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		
		/* Lock Portrait */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_portrait))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_lock_portrait), false));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_portrait))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		
		/* Lock Screen */
		if (getResources().getBoolean(R.bool.config_enableLockScreen)) {
			((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_screen))).
				setChecked(
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
						getBoolean(getString(R.string.preference_key_lock_screen), false));
			((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_screen))).
				setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		} else {
			/* Disable the lock screen option entirely */
			PreferenceCategory general = (PreferenceCategory)findPreference(Constants.prefkey_mParent);
			general.removePreference(findPreference(getString(R.string.preference_key_lock_screen)));
		}
		
		/* Full screen */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_full_screen))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_full_screen), false));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_full_screen))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		
		/* Controls on Bottom */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_controls_on_bottom))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_controls_on_bottom), getResources().getBoolean(R.bool.config_controlsOnBottom)));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_controls_on_bottom))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
	
		/* Queue on click */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_queue_on_click))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_queue_on_click), false));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_queue_on_click))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
	
		/* Prefer artist sorting */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_prefer_artist_sorting))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_prefer_artist_sorting), true));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_prefer_artist_sorting))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
	
		/* Directory filtering */
		((Preference)findPreference(getString(R.string.preference_key_directory_filter))).
			setOnPreferenceClickListener(directoryFilterPreferenceClickListener);

		/* Storage Type */
		fillStorageTypePreferenceText();
		((Preference)findPreference(getString(R.string.preference_key_storage_type))).
			setOnPreferenceClickListener(storageTypePreferenceClickListener);
		
		
		/* Always Embedded Album Art */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_embedded_album_art))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_embedded_album_art), true));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_embedded_album_art))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		
		/* Clear Album Art */
//		((ClearAlbumArtDialogPreference)findPreference(getString(R.string.preference_key_clear_album_art))).
//			setOnPreferenceChangeListener(mClearAlbumArtDialogPreferenceChangeListener);
		
		/* Donate */
		((Preference)findPreference(getString(R.string.preference_key_donate))).
			setOnPreferenceClickListener(donatePreferenceClickListener);
	}
	
	private OnPreferenceChangeListener booleanPreferenceChangeListener = new OnPreferenceChangeListener() {
	
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			if(newValue.getClass().equals(Boolean.class)){
				((CheckBoxPreference)preference).setChecked((Boolean) newValue);
				edit.putBoolean(preference.getKey(), (Boolean) newValue);
			}
			// else if ... other types
			edit.commit();
			return true;
		}
	};
	
	private OnPreferenceClickListener donatePreferenceClickListener = new OnPreferenceClickListener() {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent i = new Intent(getApplicationContext(), DonateActivity.class);
			startActivity(i);
			return true;
		}
	};
	
	private OnPreferenceClickListener directoryFilterPreferenceClickListener = new OnPreferenceClickListener() {
	
		@Override
		public boolean onPreferenceClick(Preference preference) {
			Intent i = new Intent(getApplicationContext(), DirectoryFilterActivity.class);
			startActivity(i);
			return true;
		}
	};
	
	private OnPreferenceClickListener storageTypePreferenceClickListener = new OnPreferenceClickListener() {
		
		@Override
		public boolean onPreferenceClick(Preference preference) {
			AlertDialog.Builder adb = new AlertDialog.Builder(RockOnNextGenPreferences.this);
			adb.setTitle(getString(R.string.preference_storage_dialog_title));
			String[] storageTypes = new String[] {getString(R.string.preference_title_storage_type_external), getString(R.string.preference_title_storage_type_internal)};
			int chosen = 0;
			if(DirectoryFilter.getStorageType() == DirectoryFilter.EXTERNAL_STORAGE)
				chosen = 0;
			else if(DirectoryFilter.getStorageType() == DirectoryFilter.INTERNAL_STORAGE)
				chosen = 1;
			adb.setSingleChoiceItems(storageTypes, chosen, mChosenStorageTypeListener);
			adb.setNegativeButton(getString(R.string.back), null);
			adb.show();
			// show SinglePreferenceDialog
			// add dialog onClickListener to change stuff
			return true;
		}
	};
	
	private void fillStorageTypePreferenceText() {
		Preference pref = ((Preference)findPreference(getString(R.string.preference_key_storage_type)));
		String title = null;
		String summary = null;
		int storageType = DirectoryFilter.getStorageType(); 
		if(storageType == DirectoryFilter.EXTERNAL_STORAGE) {
			title = getString(R.string.preference_title_storage_type_external);
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				summary = DirectoryFilter.getStorageReadableSize(getApplicationContext(), storageType)
				+ " (" + DirectoryFilter.getStorageReadableAvailable(getApplicationContext(), storageType) 
				+ " " + getString(R.string.preference_storage_free)
				+ ").";
				setDirFilter(true);
			} else {
				summary = getString(R.string.preference_storage_media_not_mounted);
				setDirFilter(false);
			}
			Log.i(TAG, Environment.getExternalStorageState());
		} else if (DirectoryFilter.getStorageType() == DirectoryFilter.INTERNAL_STORAGE) {
			title = getString(R.string.preference_title_storage_type_internal);
			summary = DirectoryFilter.getStorageReadableSize(getApplicationContext(), storageType)
			+ " (" + DirectoryFilter.getStorageReadableAvailable(getApplicationContext(), storageType) 
			+ " " + getString(R.string.preference_storage_free)
			+ ").";
			setDirFilter(false);
		}

		pref.setTitle(title);
		pref.setSummary(summary);
	}
	
	private void setDirFilter(boolean enabled) {
		Preference dirPref = ((Preference)findPreference(getString(R.string.preference_key_directory_filter)));
		dirPref.setEnabled(enabled);
		if(enabled) {
			// stuff...
		} else {
			// something else
		}
	}
	
	OnClickListener mChosenStorageTypeListener = new OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch(which) {
				case 0:
					DirectoryFilter.setStorageType(DirectoryFilter.EXTERNAL_STORAGE);
					break;
				case 1:
					DirectoryFilter.setStorageType(DirectoryFilter.INTERNAL_STORAGE);
					break;
			}
			fillStorageTypePreferenceText();
		}
	};
	
}