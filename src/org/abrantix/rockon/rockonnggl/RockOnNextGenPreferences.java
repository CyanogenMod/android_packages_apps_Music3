package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.WindowManager;

public class RockOnNextGenPreferences extends PreferenceActivity{
	
	protected static final String TAG = "RockOnNextGenPreferences";

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
	}
	
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
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_screen))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_lock_screen), false));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_lock_screen))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
		
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
					getBoolean(getString(R.string.preference_key_controls_on_bottom), false));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_controls_on_bottom))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
	
		/* Prefer artist sorting */
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_prefer_artist_sorting))).
			setChecked(
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
					getBoolean(getString(R.string.preference_key_prefer_artist_sorting), true));
		((CheckBoxPreference)findPreference(getString(R.string.preference_key_prefer_artist_sorting))).
			setOnPreferenceChangeListener(booleanPreferenceChangeListener);
	
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
	
}