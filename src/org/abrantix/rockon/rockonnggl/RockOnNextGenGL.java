package org.abrantix.rockon.rockonnggl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import org.abrantix.rockon.rockonnggl.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RockOnNextGenGL extends Activity {
	private final String TAG = "RockOnNextGenGL";
	
	/** Global Vars */
//	Context											mContext;
	int												mRendererMode;
	int												mTheme;
	int												mBrowseCatMode;
	GLSurfaceView 									mGlSurfaceView;
//	RockOnCubeRenderer 								mRockOnCubeRenderer;
	RockOnRenderer	 								mRockOnRenderer;
    private RockOnNextGenDefaultExceptionHandler 	mDefaultExceptionHandler; 
    boolean											mIsSdCardPresentAndHasMusic = true;
    String											mNewPlaylistName;
    NavScrollerView									mNavScroller;
    
    /** Dialogs */
    private	AlertDialog.Builder					mPlaylistDialog;
    private	AlertDialog.Builder					mViewModeDialog;
    private	AlertDialog.Builder					mThemeDialog;
    private	AlertDialog.Builder					mSpecificThemeDialog;
	private AlertDialog.Builder					mInstallConcertAppDialog;
    
	/** Initialized vars */
	AlbumArtDownloadOkClickListener		mAlbumArtDownloadOkClickListener = null;
	ThemeChangeClickListener			mThemeChangeClickListener = null;
    private IRockOnNextGenService 		mService = null;
	
	/** State Variables */
	static int			mState = Constants.STATE_INTRO;
	static String		mTrackName = null;
	static String		mArtistName = null;
	static long			mTrackDuration = -1;
	static long			mTrackProgress = -1;
	static float		mNavigatorPositionX = -1;
	static float		mNavigatorTargetPositionX = -1;
	static float		mNavigatorPositionY = -1;
	static float		mNavigatorTargetPositionY = -1;
	static int			mPlaylistId = Constants.PLAYLIST_UNKNOWN;
	
	/*********************************************************
	 * *******************************************************
	 *  
	 *  	USES REFLECTION
	 *  
	 *  Media Button registration for 2.2
	 *  
	 *  ******************************************************/
    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
    private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;


    private static void initializeRemoteControlRegistrationMethods() {
    	   try {
    	      if (mRegisterMediaButtonEventReceiver == null) {
    	         mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
    	               "registerMediaButtonEventReceiver",
    	               new Class[] { ComponentName.class } );
    	      }
    	      if (mUnregisterMediaButtonEventReceiver == null) {
    	         mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
    	               "unregisterMediaButtonEventReceiver",
    	               new Class[] { ComponentName.class } );
    	      }
    	      /* success, this device will take advantage of better remote */
    	      /* control event handling                                    */
    	   } catch (NoSuchMethodException nsme) {
    	      /* failure, still using the legacy behavior, but this app    */
    	      /* is future-proof!                                          */
    	   }
    }
    
    static {
        initializeRemoteControlRegistrationMethods();
    }

    private void registerRemoteControl() {
    	try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e("MyApp", "unexpected " + ie);
        }
    }
    
    private void unregisterRemoteControl() {
        try {
            if (mUnregisterMediaButtonEventReceiver == null) {
                return;
            }
            mUnregisterMediaButtonEventReceiver.invoke(
            		mAudioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);  
        }
    }
    /*********************************************************
     *********************************************************/
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        /****************
//         * DEBUG ONLY
//         ****************/
//        mAnalytics = GoogleAnalyticsTracker.getInstance();
////      mAnalytics.start("UA-20349033-2", 6*60 /* *60 */ /* every 6 hours */, this);
//        mAnalytics.start("UA-20349033-2", this); 
//
//        mAnalytics.trackPageView("/RockOnNextGenGL");
//        mAnalytics.dispatch();
//        mAnalytics.stop();
        
        /* set up our default exception handler */
        mDefaultExceptionHandler = 
        	new RockOnNextGenDefaultExceptionHandler(
        			RockOnNextGenGL.this);

        /* media button reflection variables */
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(
        		getPackageName(),
                MediaButtonIntentReceiver.class.getName());

        /* setup window properties */
        setupWindow();
        
        /**
         * SD card check
         */
        if(false && !isSdCardPresent())
        // FIXME: restart app code not working
//        	&&
//        	(
//        		getIntent().getAction() == null 
//        		||
//	        	(
//	    			getIntent().getAction() != null 
//	    			&&
//	    			!getIntent().getAction().equals(Constants.MAIN_ACTIVITY_IGNORE_SD_CARD))))
        {
        	mIsSdCardPresentAndHasMusic = false;
        	showSdCardNotPresentAlert();
        	return;
        }
        
//        if(!hasMusic())
//        {
//        	showNoMusicAlert();
//        	return;
//        }

        resumeAlbumArtDownload();
        resumeAlbumArtProcessing();
        
        /* some stuff needs to be read from the preferences before we do anything else */
        initializeState();
        connectToService();
        
        switch(mState){
        case Constants.STATE_INTRO:
        	showIntro();
        	break;
        case Constants.STATE_NAVIGATOR:
        	showNavigator();
        	break;
        case Constants.STATE_FULLSCREEN:
        	showFullScreen();
        	break;
        }
    }
    
    /** OnStart */
    public void onStart(){
    	super.onStart();
    	if(mIsSdCardPresentAndHasMusic)
    	{
//	    	Log.i(TAG, "ON START!");
	    	attachListeners();
	    	attachBroadcastReceivers();
	    	// trigger update album to the current playing
	    	mSetNavigatorCurrent.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT-2);
    	}
    	
        /**
         * Donation
         */
        showDonation();
    }
    
    /** OnResume */
    public void onResume(){
    	super.onResume();
//    	Log.i(TAG, "ON RESUME!");
    	if(mIsSdCardPresentAndHasMusic)
    	{
	    	resumeState();
	    	switch(mState){
	    	case Constants.STATE_NAVIGATOR:
	    		mGlSurfaceView.onResume();
	            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	    		mRockOnRenderer.forceTextureUpdateOnNextDraw();
	    		return;
	    	}
    	}
    }
    
    /** OnPause */
    public void onPause(){
    	super.onPause();
//    	Log.i(TAG, "ON PAUSE!");
    	if(mIsSdCardPresentAndHasMusic){
	    	switch(mState){
	    	case Constants.STATE_NAVIGATOR:
	        	/* save Navigator state */
	    		saveNavigatorState();
	        	// XXX - small GlSurfaceView bughackfix
	            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	            android.os.SystemClock.sleep(250);
	            // 
	    		mGlSurfaceView.onPause();
	        	return;
	    	}
    	}
    }
    
    /** OnStop */
    public void onStop(){
    	super.onStop();

    	if(mIsSdCardPresentAndHasMusic)
    	{
	    	/* unregister receivers */
	    	unregisterReceiver(mStatusListener);
	//    	Log.i(TAG, "ON STOP 2!");
	    	/* remove handler calls */
	    	removePendingHandlerMessages();
    	}
    }
    
    /** OnDestroy */
    public void onDestroy(){
    	super.onDestroy();
    	
    	/** Clear renderer cache */
    	if(mRockOnRenderer != null)
    	{
    		mRockOnRenderer.clearCache();
    	}
    	
    	/** LITTLE HACK */ // sometimes onPause/onDestroy may not be called
    	if(mGlSurfaceView != null && 
    		mGlSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_CONTINUOUSLY)
    	{
    		try
    		{
		        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		        android.os.SystemClock.sleep(250);
		    	mGlSurfaceView.onPause();
    		}
    		catch(Exception e)
    		{
    			e.printStackTrace();
    		}
    	}
    	
    	if(mIsSdCardPresentAndHasMusic)
    	{
	//    	Log.i(TAG, "ON DESTROY!");
//	    	/* save navigator state */
//	    	saveNavigatorState();
	    	
	    	/* check if downloading album art */
	    	if(mAlbumArtDownloadOkClickListener != null &&
	    			mAlbumArtDownloadOkClickListener.isDownloading())
	    	{
	    		mAlbumArtDownloadOkClickListener.stopArtDownload();
	    		/* save state */
	    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
	    		editor.putBoolean(getString(R.string.preference_key_downloading_art_state), true);
	    		editor.commit();
	    	}

	    	/* check if processing album art */
	    	if(mThemeChangeClickListener != null &&
	    			mThemeChangeClickListener.isProcessing() &&
	    			!mThemeChangeClickListener.isInTheBackground())
	    	{
	    		mThemeChangeClickListener.stopArtProcessing();
	    		/* save state */
	    		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
	    		editor.putBoolean(Constants.prefkey_mThemeProcessing, true);
	    		editor.putInt(Constants.prefkey_mThemeBeingProcessed, mThemeChangeClickListener.getTheme());
	    		editor.commit();
	    	}
	    
	    	/* Unbind service */
	    	try
	    	{
	    		unbindService(mServiceConnection);
	    	}
	    	catch(Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	catch(Error err)
	    	{
	    		err.printStackTrace();
	    	}
	    	
	    	/* Remove our default exception handler */
	    	mDefaultExceptionHandler.destroy();
	    	
	    	/* is the concert app install dialog showing */
	//    	if(mInstallConcertAppDialog != null)
	//    		mInstallConcertAppDialog.
    	}
    }
   
//    @Override
//    public void onAttachedToWindow() {
//      super.onAttachedToWindow();
//      Window window = getWindow();
//      // Eliminates color banding
//      window.setFormat(PixelFormat.RGBA_8888);
//    }
    
    @Override
    public boolean  onCreateOptionsMenu(Menu menu){
    	super.onCreateOptionsMenu(menu);

    	String[] menuOptionsTitleArray = 
    		getResources().
    			getStringArray(R.array.menu_options_title_array);
    	int[] menuOptionsIdxArray =
    		getResources().
    			getIntArray(R.array.menu_options_index_array);

    	/* create the menu items */
    	for(int i=0; i<menuOptionsTitleArray.length; i++){
//    		/* bypass equalizer if it is not supported */
    		if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_equalizer))) {
    			if(!EqualizerWrapper.isSupported())
    				continue;
    		}
    		menu.add(
    				0, // subgroup 
    				menuOptionsIdxArray[i], // id 
    				menuOptionsIdxArray[i], // order
    				menuOptionsTitleArray[i]); // title
    		/* set the icon */
    		if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_preferences)))
    			menu.getItem(i).setIcon(android.R.drawable.ic_menu_preferences);
    		else if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_playlist_id)))
    			menu.getItem(i).setIcon(R.drawable.ic_mp_current_playlist_btn);
    		else if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_get_art)))
    			menu.getItem(i).setIcon(R.drawable.ic_menu_music_library);
    		else if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_view_mode)))
    			menu.getItem(i).setIcon(android.R.drawable.ic_menu_view);
    		else if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_theme)))
    			menu.getItem(i).setIcon(android.R.drawable.ic_menu_gallery);
    		else if(menuOptionsTitleArray[i].equals(getString(R.string.menu_option_title_concerts)))
    			menu.getItem(i).setIcon(android.R.drawable.ic_menu_today);
    		// missing: releases
    		// missing: equalizer
    	}
    	
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	// TODO: in case we need to change some the options
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	/**
    	 *  Preferences 
    	 */
    	if(item.getTitle().
    		equals(getString(R.string.menu_option_title_preferences)))
    	{
    		startActivityForResult(
    				new Intent(
    						this, 
    						RockOnNextGenPreferences.class), 
    				Constants.PREFERENCE_ACTIVITY_REQUEST_CODE);
    	} 
    	/**
    	 *  Get art 
    	 */
    	else if(item.getTitle().
    			equals(getString(R.string.menu_option_title_get_art)))
    	{
    		showAlbumArtDownloadDialog();
    	}
    	/**
    	 * Concerts
    	 */
    	else if(item.getTitle().
    			equals(getString(R.string.menu_option_title_concerts)))
    	{
    		try{
    			ComponentName cName = 
    				new ComponentName(
						Constants.CONCERT_APP_PACKAGE, 
						Constants.CONCERT_APP_MAIN_ACTIVITY);
				
    			/* is concerts installed? */
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			
    			Intent i = new Intent();
    			i.setComponent(cName);
    			/* start it */
    			startActivity(i);
    			
    		} catch(NameNotFoundException e) {
    			showConcertsRequiresAppInstallDialog();
    		}
    	}
    	/**
    	 * Releases
    	 */
    	else if(item.getTitle().
    			equals(getString(R.string.menu_option_title_releases)))
    	{
    		try{
    			ComponentName cName = 
    				new ComponentName(
						Constants.RELEASES_APP_PACKAGE, 
						Constants.RELEASES_APP_MAIN_ACTIVITY);
				
    			/* is concerts installed? */
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			
    			Intent i = new Intent();
    			i.setComponent(cName);
    			/* start it */
    			startActivity(i);
    			
    		} catch(NameNotFoundException e) {
    			showReleasesRequiresAppInstallDialog();
    		}
    	}
    	/**
    	 *  View Mode 
    	 */
    	else if(item.getTitle().
    		equals(getString(R.string.menu_option_title_view_mode)))
    	{
    		mViewModeDialog = new AlertDialog.Builder(this);
    		mViewModeDialog.setTitle(getString(R.string.menu_option_title_view_mode));
    		mViewModeDialog.setAdapter(
    				new ArrayAdapter<String>(
    						this,
    						android.R.layout.select_dialog_item,
    						android.R.id.text1,
    						getResources().getStringArray(R.array.view_modes)),
    				mRendererChoiceDialogClick);
    		mViewModeDialog.show();
    	}
    	/**
    	 *  Theme 
    	 */
    	else if(item.getTitle().
    		equals(getString(R.string.menu_option_title_theme)))
    	{
    		mThemeDialog = new AlertDialog.Builder(this);
    		mThemeDialog.setTitle(getString(R.string.menu_option_title_theme));
    		mThemeDialog.setAdapter(
    				new ArrayAdapter<String>(
    						this,
    						android.R.layout.select_dialog_item,
    						android.R.id.text1,
    						getResources().getStringArray(R.array.themes)),
    				mThemeChoiceDialogClick);
    		mThemeDialog.show();
    	}
    	/**
    	 *  Equalizer 
    	 */
    	else if(item.getTitle().
        		equals(getString(R.string.menu_option_title_equalizer)))
        {
//    		int priority = 0; // normal
//    		int audioSessionId = 0; // the output mix
//    		EqualizerWrapper eqWrapper = new EqualizerWrapper(priority, audioSessionId);
    		startActivity(new Intent(this, EqualizerActivity.class));
        }
    	/**
    	 *  Playlists 
    	 */
    	else if(item.getTitle().
    		equals(getString(R.string.menu_option_title_playlist_id)))
    	{
    		CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
    		mPlaylistDialog = new AlertDialog.Builder(this);
    		mPlaylistDialog.setTitle(
    				getString(R.string.playlist_dialog_title));
    		mPlaylistDialog.setNegativeButton(
    				getString(R.string.playlist_dialog_cancel), 
    				null);
    		// create adapter
    		ArrayList<Playlist> playlistArray = new ArrayList<Playlist>();
    		
    		// all + 
//    		playlistArray.add(
//    				new Playlist(
//    						Constants.PLAYLIST_ALL, 
//    						getString(R.string.playlist_all_title)));
    		
//    		// recent +
//    		playlistArray.add(
//    				new Playlist(
//    						Constants.PLAYLIST_MOST_RECENT, 
//    						getString(R.string.playlist_most_recent_title)));
    		
//    		// most played +
//    		playlistArray.add(
//    				new Playlist(
//    						Constants.PLAYLIST_MOST_PLAYED, 
//    						getString(R.string.playlist_most_played_title)));
 
    		// 'normal' playlists +
    		Cursor playlistCursor = cursorUtils.getPlaylists();
    		if(playlistCursor != null)
    		{
	    		for(int i=0; i<playlistCursor.getCount(); i++){
	    			playlistCursor.moveToPosition(i);
	    			playlistArray.add(
	    					new Playlist(
	    							(int) playlistCursor.getLong(
	    									playlistCursor.getColumnIndexOrThrow(
	    											MediaStore.Audio.Playlists._ID)),
	    							playlistCursor.getString(
	    									playlistCursor.getColumnIndexOrThrow(
	    											MediaStore.Audio.Playlists.NAME))
	    					));
	    		}
	    		playlistCursor.close();
    		}
    		
    		// genre cursor + 
    		Cursor genreCursor = cursorUtils.getGenres();
    		if(genreCursor != null)
    		{
	    		for (int i=0; i<genreCursor.getCount(); i++){
	    			genreCursor.moveToPosition(i);
	    			playlistArray.add(
	    					new Playlist(
	    							(int) (Constants.PLAYLIST_GENRE_OFFSET 
	    								- genreCursor.getLong(
	    										genreCursor.getColumnIndexOrThrow(
	    												MediaStore.Audio.Genres._ID))),
	    							genreCursor.getString(
	    									genreCursor.getColumnIndexOrThrow(
	    											MediaStore.Audio.Genres.NAME))
	    					));
	    		}
	    		genreCursor.close();
    		}

//    		SimpleAdapter playlistSimpleAdapter = new SimpleAdapter(
//			getApplicationContext(), 
//			playlistArray, 
//			android.R.layout.select_dialog_item, 
//			new String[]{
//				Constants.PLAYLIST_NAME_KEY
//				},
//			new int[]{
//				android.R.id.text1
//				});	
    		// create adapter
    		PlaylistArrayAdapter playlistAdapter = new PlaylistArrayAdapter(
    				getApplicationContext(), 
    				android.R.layout.select_dialog_item, 
    				playlistArray, 
    				new String[]{
    					Constants.PLAYLIST_NAME_KEY
    					},
    				new int[]{
    					android.R.id.text1
    					},
    				10000,
    				0,
    				mPlaylistClickHandler);
    		mPlaylistDialog.setAdapter(
    				playlistAdapter,
    				null);
//    				new PlaylistSelectedClickListener(mPlaylistSelectedHandler, playlistArray));
    		playlistAdapter.setDialogInterface(mPlaylistDialog.show());
    	}
    	/* other options... */
    	return true;
    }
    
    /**
     * 
     */
    Handler mPlaylistClickHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    		case Constants.SINGLE_CLICK:
    			// show song list from playlist
        		showSongPlaylistDialog(msg.arg1, (String)msg.obj);
    			break;
    		case Constants.LONG_CLICK:
    			// show playlist options dialog
    			showPlaylistOptionsDialog(msg.arg1, (String)msg.obj);
    			break;
    		}
    	}
    };
    
    private void showDonation()
    {
    	int appCreateCount = 
    		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    			getInt(Constants.prefkey_mAppCreateCount, 1);
    	appCreateCount++;
    	
    	int appCreateCountForDonation = 
    		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    			getInt(Constants.prefkey_mAppCreateCountForDonation, Constants.DONATION_INITIAL_INTERVAL);

    	boolean hasDonated = 
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
				getBoolean(Constants.prefkey_mAppHasDonated, false);
		
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();

    	if(getResources().getBoolean(R.bool.config_isMarketVersion)
    		&& appCreateCount >= appCreateCountForDonation)
    	{    			
    		if(hasDonated)
    			appCreateCountForDonation += Constants.DONATION_AFTER_HAVING_DONATED_INTERVAL;
    		else
    			appCreateCountForDonation += Constants.DONATION_STANDARD_INTERVAL;

        	editor.putInt(Constants.prefkey_mAppCreateCount, appCreateCount);
    		editor.putInt(Constants.prefkey_mAppCreateCountForDonation, appCreateCountForDonation);
    		
        	editor.commit();
        	
        	int donationAppsInstalled = 0;
        	try{
        		ComponentName cName = 
    				new ComponentName(
						Constants.DONATION_APP_PKG_1, 
						Constants.DONATION_APP_MAIN_ACTIVITY_1);
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			donationAppsInstalled++;
    		} catch(NameNotFoundException e) {
    		}
    		try{
        		ComponentName cName = 
    				new ComponentName(
    						Constants.DONATION_APP_PKG_2, 
    						Constants.DONATION_APP_MAIN_ACTIVITY_2);
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			donationAppsInstalled++;
    		} catch(NameNotFoundException e) {
    		}
    		try{
        		ComponentName cName = 
    				new ComponentName(
    						Constants.DONATION_APP_PKG_3, 
    						Constants.DONATION_APP_MAIN_ACTIVITY_3);
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			donationAppsInstalled++;
    		} catch(NameNotFoundException e) {
    		}
    		try{
        		ComponentName cName = 
    				new ComponentName(
    						Constants.DONATION_APP_PKG_4, 
    						Constants.DONATION_APP_MAIN_ACTIVITY_4);
    			getPackageManager().
    				getActivityInfo(
    						cName,
    						0);
    			donationAppsInstalled++;
    		} catch(NameNotFoundException e) {
    		}
        	
    		if(donationAppsInstalled <= 0) {
		    	Intent i = new Intent(this, DonateActivity.class);
		        startActivity(i);
    		}
    	}
    	else
    	{
        	editor.putInt(Constants.prefkey_mAppCreateCount, appCreateCount);
        	editor.commit();
    	}
    	
    	/*
    	 * Create dummy file to show other apps
    	 * that a donation has been made 
    	 */
    	if(hasDonated)
    	{
    		File f = new File(Constants.ROCKON_DONATION_PATH);
    		if(!f.exists())
    		{
    			f.mkdirs();
    			try {
					f.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    	else
    	{
    		// TODO: delete the donation file (necessary?)
    	}
    }
    
    /**
     * ask the user if he wants to install the new app or not
     */
    private void showConcertsRequiresAppInstallDialog()
    {
    	mInstallConcertAppDialog = new AlertDialog.Builder(this);
    	mInstallConcertAppDialog.setTitle(getString(R.string.concert_app_install_dialog_title));
    	mInstallConcertAppDialog.setMessage(getString(R.string.concert_app_install_dialog_message));
    	mInstallConcertAppDialog.setPositiveButton(
    			getString(R.string.concert_app_install_dialog_ok), 
    			mConcertAppInstallClickListener);
    	mInstallConcertAppDialog.setNegativeButton(
    			getString(R.string.concert_app_install_dialog_cancel), 
    			null);
    	mInstallConcertAppDialog.show();
    }
    
    /**
     * ask the user if he wants to install the new app or not
     */
    private void showReleasesRequiresAppInstallDialog()
    {
    	mInstallConcertAppDialog = new AlertDialog.Builder(this);
    	mInstallConcertAppDialog.setTitle(getString(R.string.releases_app_install_dialog_title));
    	mInstallConcertAppDialog.setMessage(getString(R.string.releases_app_install_dialog_message));
    	mInstallConcertAppDialog.setPositiveButton(
    			getString(R.string.releases_app_install_dialog_ok), 
    			mReleasesAppInstallClickListener);
    	mInstallConcertAppDialog.setNegativeButton(
    			getString(R.string.releases_app_install_dialog_cancel), 
    			null);
    	mInstallConcertAppDialog.show();
    }
    
    DialogInterface.OnClickListener mConcertAppInstallClickListener = 
    	new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try
				{
					Intent i = new Intent(Intent.ACTION_VIEW, 
					Uri.parse("market://search?q=pname:"+Constants.CONCERT_APP_PACKAGE));
					startActivity(i);					
				}
				catch(ActivityNotFoundException e)
				{
					e.printStackTrace();
					Toast.makeText(
							RockOnNextGenGL.this, 
							R.string.concert_app_install_no_market_app_msg, 
							Toast.LENGTH_LONG)
						.show();
					
				}
			}
		};
    
		DialogInterface.OnClickListener mReleasesAppInstallClickListener = 
	    	new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try
					{
						Intent i = new Intent(Intent.ACTION_VIEW, 
						Uri.parse("market://search?q=pname:"+Constants.RELEASES_APP_PACKAGE));
						startActivity(i);					
					}
					catch(ActivityNotFoundException e)
					{
						e.printStackTrace();
						Toast.makeText(
								RockOnNextGenGL.this, 
								R.string.releases_app_install_no_market_app_msg, 
								Toast.LENGTH_LONG)
							.show();
						
					}
				}
			};
	    
	private DialogInterface.OnClickListener mRendererChoiceDialogClick = 
		new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String[] rendererArray = getResources().getStringArray(R.array.view_modes);
				if(rendererArray[which].equals(getString(R.string.view_mode_cube)))
				{
					// save in preferences
					Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					edit.putInt(Constants.prefkey_mRendererMode, Constants.RENDERER_CUBE);
					edit.commit();
					// reload views
					mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.RENDERER_CUBE);
				}
				else if(rendererArray[which].equals(getString(R.string.view_mode_wall)))
				{
					// save in preferences
					Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					edit.putInt(Constants.prefkey_mRendererMode, Constants.RENDERER_WALL);
					edit.commit();
					// reload views
					mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.RENDERER_WALL);
				}
				else if(rendererArray[which].equals(getString(R.string.view_mode_boring)))
				{
					// save in preferences
					Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					edit.putInt(Constants.prefkey_mRendererMode, Constants.RENDERER_BORING);
					edit.commit();
					// reload views
					mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.RENDERER_BORING);
				}
				else if(rendererArray[which].equals(getString(R.string.view_mode_morph)))
				{
					// save in preferences
					Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
					edit.putInt(Constants.prefkey_mRendererMode, Constants.RENDERER_MORPH);
					edit.commit();
					// reload views
					mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.RENDERER_MORPH);
				}
			}
		};
			
		private DialogInterface.OnClickListener mThemeChoiceDialogClick = 
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String[] themeArray = getResources().getStringArray(R.array.themes);
					if(themeArray[which].equals(getString(R.string.theme_normal)))
					{
						// save in preferences
						Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
						edit.putInt(Constants.prefkey_mTheme, Constants.THEME_NORMAL);
						edit.commit();
						// reload views
						mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.THEME_NORMAL);
					}
					else if(themeArray[which].equals(getString(R.string.theme_halftone)))
					{
						/**
						 * Create our image processing manager
						 */
						if(mThemeChangeClickListener == null)
							mThemeChangeClickListener = 
								new ThemeChangeClickListener(
										RockOnNextGenGL.this, 
										Constants.THEME_HALFTONE,
										mLoadNewViewModeOrTheme);
						else
							mThemeChangeClickListener.
								changeTheme(Constants.THEME_HALFTONE, false);
						
						/**
						 * Check preferences to see if art was already processed
						 */
						if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
								getBoolean(Constants.prefkey_mThemeHalfToneDone, false))
						{
							mSpecificThemeDialog = new AlertDialog.Builder(RockOnNextGenGL.this);
							mSpecificThemeDialog.setTitle(R.string.half_tone_dialog_title);
							mSpecificThemeDialog.setMessage(R.string.half_tone_dialog_message);
							mSpecificThemeDialog.setPositiveButton(
									R.string.half_tone_dialog_yes, 
									mThemeChangeClickListener);
							mSpecificThemeDialog.setNegativeButton(R.string.half_tone_dialog_no, null);
							mSpecificThemeDialog.show();
						}
						else
						{
							mThemeChangeClickListener.mArtProcessingTrigger.sendEmptyMessage(0);
//							// save in preferences
//							Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
//							edit.putInt(Constants.prefkey_mTheme, Constants.THEME_HALFTONE);
//							edit.commit();
//							// reload views
//							mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.THEME_HALFTONE);
						}
					}
					else if(themeArray[which].equals(getString(R.string.theme_earthquake)))
					{
						/**
						 * Create our image processing manager
						 */
						if(mThemeChangeClickListener == null)
							mThemeChangeClickListener = 
								new ThemeChangeClickListener(
										RockOnNextGenGL.this, 
										Constants.THEME_EARTHQUAKE,
										mLoadNewViewModeOrTheme);
						else
							mThemeChangeClickListener.
								changeTheme(Constants.THEME_EARTHQUAKE, false);
						
						/**
						 * Check preferences to see if art was already processed
						 */
						if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
								getBoolean(Constants.prefkey_mThemeEarthquakeDone, false))
						{
							mSpecificThemeDialog = new AlertDialog.Builder(RockOnNextGenGL.this);
							mSpecificThemeDialog.setTitle(R.string.earthquake_dialog_title);
							mSpecificThemeDialog.setMessage(R.string.earthquake_dialog_message);
							mSpecificThemeDialog.setPositiveButton(
									R.string.earthquake_dialog_yes, 
									mThemeChangeClickListener);
							mSpecificThemeDialog.setNegativeButton(R.string.earthquake_dialog_no, null);
							mSpecificThemeDialog.show();
						}
						else
						{
							mThemeChangeClickListener.mArtProcessingTrigger.sendEmptyMessage(0);
//							// save in preferences
//							Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
//							edit.putInt(Constants.prefkey_mTheme, Constants.THEME_EARTHQUAKE);
//							edit.commit();
//							// reload views
//							mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.THEME_EARTHQUAKE);
						}
					}
				}
			};
		
	/**
	 * 		
	 */
	private Handler mLoadNewViewModeOrTheme = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			showNavigator();
			attachListeners();
			resumeNavigatorState();
		}
	};
	
	/**
	 * 
	 */
	private Handler mBrowseCatChanged = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
//			Log.i(TAG, "msg.what: "+msg.what+" mBrowseCatMode: "+mBrowseCatMode);
			if(msg.what != mBrowseCatMode)
			{
				Editor editor = 
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
						edit();
				editor.putInt(Constants.prefkey_mBrowseCatMode, msg.what);
				editor.commit();
				mBrowseCatMode = msg.what;
				/**
				 * new mode is song mode?
				 * 	then we need to always set boring mode
				 *  if not, then we might need to reverse to the 'default' browsing mode
				 */
				boolean changeRenderer = false;
				if(mBrowseCatMode == Constants.BROWSECAT_SONG)
				{
					if(mRockOnRenderer.getType() != Constants.RENDERER_BORING)
					{
						changeRenderer = true;
						mLoadNewViewModeOrTheme.sendEmptyMessage(Constants.RENDERER_BORING);
					}
				}
				else
				{
					if(mRockOnRenderer.getType() != mRendererMode)
					{
						changeRenderer = true;
						mLoadNewViewModeOrTheme.sendEmptyMessage(mRendererMode);
					}
				}
				if(!changeRenderer)
				{
					mRockOnRenderer.changeBrowseCat(mBrowseCatMode);
					try
					{
						switch(mBrowseCatMode)
						{
						case Constants.SWITCHER_CAT_ALBUM:
							mRockOnRenderer.setCurrentByAlbumId(mService.getAlbumId());
							break;
						case Constants.SWITCHER_CAT_ARTIST:
							mRockOnRenderer.setCurrentByArtistId(mService.getArtistId());
							break;
						case Constants.SWITCHER_CAT_SONG:
							mRockOnRenderer.setCurrentBySongId(mService.getAudioId());
							break;
						}
					}
					catch(RemoteException e)
					{
						e.printStackTrace();
					}
					mRockOnRenderer.renderNow();
	//				mLoadNewViewModeOrTheme.sendEmptyMessage(0);
				}
			}
		}
	};
    
//	private boolean prepareSongBrowsingMode()
//	{
//		if(mRendererMode != Constants.RENDERER_BORING)
//		{
//			mRockOnRenderer.clearCache();
//			RockOnBoringRenderer renderer = 
//				new RockOnBoringRenderer(
//						getApplicationContext(), 
//						mRequestRenderHandler, 
//						mTheme, 
//						mBrowseCatMode);
//			mRockOnRenderer = renderer;
//			((GLSurfaceView)findViewById(R.id.cube_surface_view)).
//				setRenderer((Renderer) mRockOnRenderer);
//			return true;
//		}
//		else
//		{
//			return false;
//		}
//	}
	
    /**
     * 
     * @param playlistId
     * @param playlistName
     */
    public void showSongPlaylistDialog(int playlistId, String playlistName)
    {
    	/** Get list of songs */
    	CursorUtils	cursorUtils = new CursorUtils(getApplicationContext());
    	Cursor songCursor = cursorUtils.getAllSongsFromPlaylist(playlistId);
    	/** Show the list to the user */
    	if(songCursor != null && songCursor.getCount() > 0)
    	{
    		startManagingCursor(songCursor);
			songCursor.moveToFirst();
			/** Create dialog */
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
			/* dialog title - album and artist name */
			dialogBuilder.setTitle(playlistName);
			/* show the playlist song list  in a dialog */
			SongCursorAdapter songCursorAdapter = 
				new SongCursorAdapter(
						getApplicationContext(),
						Constants.queueSongListLayoutId, //R.layout.songlist_dialog_item or android.R.layout.select_dialog_item
						songCursor, 
						Constants.queueSongListFrom, 
						Constants.queueSongListTo,
						10000000, // will never reach the limit -- only useful in play queue
						0, // no extra results
						mPlayListItemSelectedHandler);
			dialogBuilder.setAdapter(
						songCursorAdapter,
						null);
			dialogBuilder.setNeutralButton(
					R.string.album_song_list_queue_all, 
					mDialogPlayListClickListener);
			dialogBuilder.setPositiveButton(
					R.string.album_song_list_play_all, 
					mDialogPlayListClickListener);
			
//			dialogBuilder.setNegativeButton(
//					R.string.clear_playlist_dialog_option, 
//					mDialogPlayQueueClickListener);
//			dialogBuilder.setOnCancelListener(mSongAndAlbumDialogCancelListener);
//			dialogBuilder.setPositiveButton(
//					R.string.create_playlist_dialog_option, 
//					mDialogPlayQueueClickListener);
			/* set the selection listener */
			songCursorAdapter.setDialogInterface(dialogBuilder.show());
		} 
    	else 
    	{
			// show Playlist Empty notification
			Toast.
				makeText(
					getApplicationContext(), 
					R.string.playlist_empty_toast, 
					Toast.LENGTH_LONG).
				show();
			/**
			 * it is an empty genre... delete it
			 */
			if(playlistIsGenre(playlistId))
				deletePlaylistOrGenre(playlistId);
		}
    }
    
    /**
     * 
     * @param playlistId
     * @param playlistName
     */
    public void showPlaylistOptionsDialog(int playlistId, String playlistName)
    {
    	// show a new dialog with Add to Queue, Delete
    	AlertDialog.Builder playlistOptsDBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
    	playlistOptsDBuilder.setTitle(playlistName);
    	playlistOptsDBuilder.setAdapter(
    			new ArrayAdapter<String>(
    					getApplicationContext(), 
    					android.R.layout.select_dialog_item, 
    					android.R.id.text1, 
    					getResources().getStringArray(R.array.playlist_options)), 
    			new PlaylistOptionClickListener(
    					playlistId, 
    					mPlaylistOptionSelectedHandler));
    	playlistOptsDBuilder.show();
    }
    
    /**
     * 
     */
    Handler mPlaylistOptionSelectedHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		String playlistOption = getResources().getStringArray(R.array.playlist_options)[msg.what];
    		if(playlistOption.equals(getString(R.string.playlist_option_play_now)))
    		{
    			// play entire playlist
    			queueOrPlayAllSongsFromPlaylist(msg.arg1, Constants.NOW);
 
    		}
    		else if(playlistOption.equals(getString(R.string.playlist_option_add_to_queue)))
    		{
    			// add all playlist song to queue
    			queueOrPlayAllSongsFromPlaylist(msg.arg1, Constants.LAST);
    		}
    		else if(playlistOption.equals(getString(R.string.playlist_option_delete)))
    		{
    			// show confirmation dialog
    			AlertDialog.Builder deletePlaylistConfirmationDialog = 
    				new AlertDialog.Builder(RockOnNextGenGL.this);
    			deletePlaylistConfirmationDialog.setTitle(
    					R.string.playlist_delete_confirm_title);
    			deletePlaylistConfirmationDialog.setMessage(
    					R.string.playlist_delete_message);
    			final int playlistId = msg.arg1;
    			deletePlaylistConfirmationDialog.setPositiveButton(
    					R.string.playlist_delete_positive_button, 
    					new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
				    			deletePlaylistOrGenre(playlistId);								
							}
						});
    			deletePlaylistConfirmationDialog.setNegativeButton(
    					R.string.playlist_delete_negative_button, 
    					null);
    			deletePlaylistConfirmationDialog.show();
    		}	
    	}
    };
    
    /**
     * 
     * @param playlistId
     */
    private void queueOrPlayAllSongsFromPlaylist(int playlistId, int nowOrQueue)
    {
			CursorUtils cUtils = new CursorUtils(getApplicationContext());
			Cursor cursor = cUtils.getAllSongsFromPlaylist(playlistId);
			if(cursor != null && cursor.getCount() > 0)
			{
				long[] songVector = new long[cursor.getCount()];
				for(int i = 0; i<cursor.getCount(); i++){
					cursor.moveToPosition(i);
					// The _ID field has a different meaning in playlist content providers
					//		-- if it is a playlist we need to fetch the AUDIO_ID field
					songVector[i] = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(cursor);
//					Log.i(TAG, "i: "+i+"id: "+songVector[i]);
				}   
				try{
					if(mService != null)
						{
							if(nowOrQueue == Constants.NOW)
								mService.removeTracks(0, mService.getQueue().length -1);
							
							mService.enqueue(songVector, nowOrQueue);
//							mService.enqueue(songVector, Constants.LAST);
						}
				}catch(Exception e){
					e.printStackTrace();
					Toast.makeText(
							getApplicationContext(), 
							R.string.generic_error_toast, 
							Toast.LENGTH_SHORT)
						.show();
				}
			}
			else
			{
				Toast.makeText(
					getApplicationContext(), 
					R.string.playlist_empty_toast, 
					Toast.LENGTH_SHORT)
				.show();
				/**
				 * it is an empty genre... delete it
				 */
				if(playlistIsGenre(playlistId))
					deletePlaylistOrGenre(playlistId);
			}
			System.gc();
    }
    
    /**
     * 
     * @param playlistId
     * @return
     */
    private boolean playlistIsGenre(int playlistId)
    {
    	if(playlistId < Constants.PLAYLIST_GENRE_OFFSET &&
    			playlistId > Constants.PLAYLIST_GENRE_OFFSET - Constants.PLAYLIST_GENRE_RANGE)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    /**
     * 
     * @param playlistId
     * @return
     */
    private int playlistIdToGenreId(int playlistId)
    {
    	return -(playlistId - Constants.PLAYLIST_GENRE_OFFSET);
    }
    
    /**
     * 
     * @param playlistId
     */
    private void deletePlaylistOrGenre(int playlistId)
    {
    	CursorUtils cUtils = new CursorUtils(getApplicationContext());
    	boolean success = false;
    	/**
    	 * GENRE playlists
    	 */
    	if(playlistIsGenre(playlistId))
    	{
    		if(cUtils.deleteGenre(playlistIdToGenreId(playlistId)))
        		success = true;
    	}
    	/**
    	 * 'standard' playlists
    	 */
    	else
    	{
        	if(cUtils.deletePlaylist(playlistId))
        		success = true;
    	}
    	
    	/**
    	 * UI feedback
    	 */
    	if(success)
		{
			Toast.makeText(
					getApplicationContext(), 
					R.string.playlist_deleted_toast, 
					Toast.LENGTH_SHORT)
				.show();
		}
    	else
    	{
			Toast.makeText(
					getApplicationContext(), 
					R.string.generic_error_toast, 
					Toast.LENGTH_SHORT)
				.show();
    	}
    }
    
    /**
     * 
     * @param songId
     */
    private void deleteSong(int songId)
    {
    	CursorUtils cUtils = new CursorUtils(getApplicationContext());
    	boolean success = false;
    	
    	/**
    	 * Remove song from service
    	 */
    	try
    	{
    		mService.removeTrack(songId);
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
    	/**
    	 * Delete song
    	 */
    	if(cUtils.deleteSong(songId))
        		success = true;
    	
    	/**
    	 * UI feedback
    	 */
    	if(success)
		{
			Toast.makeText(
					getApplicationContext(), 
					R.string.song_deleted_toast, 
					Toast.LENGTH_SHORT)
				.show();
		}
    	else
    	{
			Toast.makeText(
					getApplicationContext(), 
					R.string.generic_error_toast, 
					Toast.LENGTH_SHORT)
				.show();
    	}
    }
    
    /**
     * 	
     */
	Handler mPlayListItemSelectedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			Log.i(TAG, "XXXXX: "+msg.arg1 + " - " + msg.arg2);
			
			try{
				long[] list = {msg.arg1};
				if(msg.arg2 == Constants.LONG_CLICK){
//					mService.removeTrack(msg.arg1);
					mService.enqueue(list, Constants.LAST);
					Toast.makeText(
							RockOnNextGenGL.this, 
							R.string.song_added_to_playlist, 
							Toast.LENGTH_SHORT).
						show();
				} else if(msg.arg2 == Constants.SINGLE_CLICK){
					if(mService.isPlaying() && getQueueOnClickPreference(getApplicationContext())) {
						mService.enqueue(list, Constants.LAST);
						Toast.makeText(
								RockOnNextGenGL.this, 
								R.string.song_added_to_playlist, 
								Toast.LENGTH_SHORT).
							show();
					} else {
						mService.removeTracks(0, mService.getQueue().length -1);
						mService.enqueue(list, Constants.NOW);
					}
//					if(!mService.isPlaying()){
//						
//					} else {
//						
//					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
    };
    	
    boolean getQueueOnClickPreference(Context ctx) {
    	return 
    		PreferenceManager
    		.getDefaultSharedPreferences(ctx)
    		.getBoolean(getString(R.string.preference_key_queue_on_click), false);
    }
    
    /**
     * 
     */
	DialogInterface.OnClickListener mDialogPlayListClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			/** Get the dialog adapter */
			SongCursorAdapter songCursorAdapter = (SongCursorAdapter) 
				((AlertDialog)dialog).getListView().getAdapter();
			/** Get cursor from adapter and construct audio_id vector */
			if(songCursorAdapter != null)
			{
				Cursor	cursor = songCursorAdapter.getCursor();
				long[] songVector = new long[cursor.getCount()];
				for(int i = 0; i<cursor.getCount(); i++){
					cursor.moveToPosition(i);
					// The _ID field has a different meaning in playlist content providers
					//		-- if it is a playlist we need to fetch the AUDIO_ID field
					songVector[i] = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(cursor);
//					Log.i(TAG, "i: "+i+"id: "+songVector[i]);
				}
				/** Replace entire queue or add to the tail depending on user input */
				switch(which)
				{
				/** 
				 * Positive Button 
				 * 
				 * (Play and queue playlist)
				 */
				case DialogInterface.BUTTON_POSITIVE:
					try{
						if(mService != null)
						{
							mService.removeTracks(0, mService.getQueue().length-1);
							mService.enqueue(songVector, Constants.NOW);
						}
					}catch(Exception e){
						e.printStackTrace();
						Toast.makeText(
								getApplicationContext(), 
								R.string.generic_error_toast, 
								Toast.LENGTH_SHORT)
							.show();
					}
					break;
				/**
				 * Neutral button
				 * 
				 * (Queue playlist)
				 */
				case DialogInterface.BUTTON_NEUTRAL:
					try{
						if(mService != null)
						{
							mService.enqueue(songVector, Constants.LAST);
							Toast.makeText(
									getApplicationContext(), 
									R.string.playlist_queued_toast, 
									Toast.LENGTH_SHORT)
								.show();
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
						Toast.makeText(
								getApplicationContext(), 
								R.string.generic_error_toast, 
								Toast.LENGTH_SHORT)
							.show();
					}
					break;
				}
			}
		}
	};
	
    /**
     * 
     * @param playlistId
     */
    private void setAndSavePlaylist(int playlistId){
    	mPlaylistId = playlistId;
    	Editor editor = 
    		PreferenceManager.
    			getDefaultSharedPreferences(getApplicationContext()).
    				edit();
    	editor.putInt(
    			Constants.prefkey_mPlaylistId,
    			mPlaylistId);
    	editor.commit();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
    	switch(requestCode){
    	case Constants.PREFERENCE_ACTIVITY_REQUEST_CODE:
//    		Log.i(TAG, "Back from preference activity");
    		// reload preferences :P
    		/* in case the user changed to full screen */
    		// TODO: reload app -- creaty dummy act that starts the main activity and finish this one
    		mLoadNewViewModeOrTheme.sendEmptyMessage(mRendererMode);
    		
    		/**
    		 * Update the preferences in service (we need to do it because they run in different processes)
    		 */
    		if(mService != null)
    			setPreferencesInService();
    						
//    		if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
//    				getBoolean(getString(R.string.preference_key_lock_screen), false))
//    		{
//    			try
//    			{
//    				mService.registerScreenOnReceiver();
//    			}
//    			catch(RemoteException e)
//    			{
//    				e.printStackTrace();
//    			}
//    			catch(NullPointerException e)
//    			{
//    				e.printStackTrace();
//    			}
//    		}
//    		else
//    		{
//    			try
//    			{
//    				mService.unregisterScreenOnReceiver();
//    			}
//    			catch(RemoteException e)
//    			{
//    				e.printStackTrace();
//    			}
//    			catch(NullPointerException e)
//    			{
//    				e.printStackTrace();
//    			}
//    		}
    		
    		break;
    	case Constants.ALBUM_ART_CHOOSER_ACTIVITY_REQUEST_CODE:
    		try
    		{
	//    		mRockOnCubeRenderer.reverseClickAnimation();
	    		mRockOnRenderer.reverseClickAnimation();
	//    		// reloadTextures??
	//    		mRockOnCubeRenderer.forceTextureUpdateOnNextDraw();
	    		mRockOnRenderer.forceTextureUpdateOnNextDraw();
    		}
    		catch(NullPointerException e)
    		{
    			e.printStackTrace();
    		}
    		break;
    	}
    }
    
    /**
     * Service runs in a different process therefor we 
     * need to pass the preferences needed to the service 
     * through its interface
     */
    private void setPreferencesInService()
    {
    	try
    	{
			// service registers the requires receivers
    		Log.i("TAG", "Sending lock screen to service: "+
    				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
		    				getBoolean(getString(R.string.preference_key_lock_screen), false));
			mService.setLockScreen(
					PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
		    				getBoolean(getString(R.string.preference_key_lock_screen), false));
			// last.fm scrobbling
			Log.i("TAG", "Sending scrobbler type to service: "+
					PreferenceManager.getDefaultSharedPreferences(this).getString(
			    			getString(R.string.preference_scrobble_list_key), 
			    			getString(R.string.preference_scrobble_value_dont)));
			mService.setScrobbler(
					PreferenceManager.getDefaultSharedPreferences(this).getString(
			    			getString(R.string.preference_scrobble_list_key), 
			    			getString(R.string.preference_scrobble_value_dont)));
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
    }
    
    /**
     * remove pending Handler Messages
     */
    private void removePendingHandlerMessages(){
    	mPassIntroHandler.removeCallbacksAndMessages(null);
    	mAlbumClickHandler.removeCallbacksAndMessages(null);
    	mSetNavigatorCurrent.removeCallbacksAndMessages(null);
    	mPlayPauseClickHandler.removeCallbacksAndMessages(null);
    	mNextClickHandler.removeCallbacksAndMessages(null);
    	mPreviousClickHandler.removeCallbacksAndMessages(null);
    	mSearchClickHandler.removeCallbacksAndMessages(null);
    	mPlayQueueClickHandler.removeCallbacksAndMessages(null);
    	mSongItemSelectedHandler.removeCallbacksAndMessages(null);
    	mSongSearchClickHandler.removeCallbacksAndMessages(null);
    	mSongListDialogOverallOptionsHandler.removeCallbacksAndMessages(null);
    	mRequestRenderHandler.removeCallbacksAndMessages(null);
    	updateCurrentPlayerStateButtonsFromServiceHandler.removeCallbacksAndMessages(null);
    	mPlaylistClickHandler.removeCallbacksAndMessages(null);
    	mUpdateSongProgressHandler.removeCallbacksAndMessages(null);
    	if(mAlbumArtDownloadOkClickListener != null)
    		mAlbumArtDownloadOkClickListener
				.mArtDownloadTrigger
					.removeCallbacksAndMessages(null);
    }
    
    /**
     * isSdCardPresent
     */
    private boolean isSdCardPresent(){
    	return 
    		android.os.Environment.
    			getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);  
    }
    
    /**
     * showSdCardNotPresentAlert
     */
    private void showSdCardNotPresentAlert(){
    	AlertDialog.Builder noSdCardAlert = new AlertDialog.Builder(this);
    	noSdCardAlert.setTitle(R.string.no_sd_card_dialog_title);
    	noSdCardAlert.setMessage(R.string.no_sd_card_dialog_message);
    	// FIXME: code is unable to restart the app - 
    	// would need another dummy actvity for restarting the app
//    	noSdCardAlert.setPositiveButton(
//    			R.string.no_sd_card_dialog_continue_anyway, 
//    			new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						Intent i = new Intent(RockOnNextGenGL.this, RockOnNextGenGL.class);
//						i.setAction(Constants.MAIN_ACTIVITY_IGNORE_SD_CARD);
//						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//						startActivity(i);
////						finish();
//					}
//				});
    	noSdCardAlert.setNegativeButton(
    			R.string.no_sd_card_dialog_exit,
    			new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
    	// TODO: better to set an ondismiss listener
    	noSdCardAlert.setCancelable(true);
    	noSdCardAlert.show();
    }
    
    /**
     * showSdCardNotPresentAlert
     */
    private void showNoMusicAlert(){
    	AlertDialog.Builder noMusicAlert = new AlertDialog.Builder(this);
    	noMusicAlert.setTitle(R.string.no_music_dialog_title);
    	noMusicAlert.setMessage(R.string.no_music_dialog_message);
    	noMusicAlert.setNegativeButton(
    			R.string.no_music_dialog_exit,
    			new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mPlaylistId = Constants.PLAYLIST_ALL;
						Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
						editor.putInt(Constants.prefkey_mPlaylistId, Constants.PLAYLIST_ALL);
						editor.commit();
						finish();
//						mLoadNewViewModeOrTheme.sendEmptyMessage(mRendererMode);
					}
				});
    	// TODO: better to set an ondismiss listener
//    	noMusicAlert.setCancelable(false);
    	noMusicAlert.show();
    }
//	
    /**
     * SetupWindow
     */
    private void setupWindow(){
    	/*
    	 * Always remove that ugly title bar
    	 */
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	/*
    	 * Has the user chosen to run the app in full screen?
    	 */
    	if(PreferenceManager.
    			getDefaultSharedPreferences(getApplicationContext()).
    				getBoolean(Constants.prefkey_mFullscreen, false))
	    	getWindow().setFlags(
	    			WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	    			WindowManager.LayoutParams.FLAG_FULLSCREEN); 
    	
    	/*
    	 * Has the user forced to be always portrait?
    	 */
    	boolean lockPortrait = 
    		PreferenceManager.
    		getDefaultSharedPreferences(getApplicationContext()).
    		getBoolean(getString(R.string.preference_key_lock_portrait), false);
    	if(lockPortrait)
    		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    
    /**
     * resumeAlbumArtDownload
     */
    private void resumeAlbumArtDownload(){
    	/* resume art download if the application was shut down while downloading art */
    	if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    			.getBoolean(getString(R.string.preference_key_downloading_art_state), false))
    	{
	    	if(mAlbumArtDownloadOkClickListener == null){
				mAlbumArtDownloadOkClickListener = 
					new AlbumArtDownloadOkClickListener(this);
			}
			mAlbumArtDownloadOkClickListener.mArtDownloadTrigger.sendEmptyMessageDelayed(0, 2000);
			Editor editor = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putBoolean(getString(R.string.preference_key_downloading_art_state), false);
			editor.commit();
    	}
    }
    
    /**
     * resumeAlbumArtProcessing
     */
    private void resumeAlbumArtProcessing(){
    	/* resume art processing if the application was shut down while downloading art */
    	if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    			.getBoolean(Constants.prefkey_mThemeProcessing, false))
    	{
			if(mThemeChangeClickListener == null)
				mThemeChangeClickListener = 
					new ThemeChangeClickListener(
							RockOnNextGenGL.this, 
							PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    	    					.getInt(Constants.prefkey_mThemeBeingProcessed, Constants.THEME_NORMAL),
							mLoadNewViewModeOrTheme);
			else
				mThemeChangeClickListener.
					changeTheme(
						PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
	    					.getInt(Constants.prefkey_mThemeBeingProcessed, Constants.THEME_NORMAL),
						false);

    		mThemeChangeClickListener.mArtProcessingTrigger.sendEmptyMessage(0);
			Editor editor = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext()).edit();
			editor.putBoolean(Constants.prefkey_mThemeProcessing, false);
			editor.commit();    		
    	}
    }
    
    /**
     * save navigator state
     */
    private void saveNavigatorState(){
    	
       	Editor editor = PreferenceManager.
		getDefaultSharedPreferences(getApplicationContext()).edit();

    	if(mRockOnRenderer != null){
    		mNavigatorPositionX = mRockOnRenderer.getPositionX();
	    	mNavigatorTargetPositionX = mRockOnRenderer.getTargetPositionX();
	    	mNavigatorPositionY = mRockOnRenderer.getPositionY();
	    	mNavigatorTargetPositionY = mRockOnRenderer.getTargetPositionY();
		   	/* navigator position */
	    	editor.putFloat(Constants.prefkey_mNavigatorPositionX, mNavigatorPositionX);
	    	editor.putFloat(Constants.prefkey_mNavigatorTargetPositionX, mNavigatorTargetPositionX);
	    	editor.putFloat(Constants.prefkey_mNavigatorPositionY, mNavigatorPositionY);
	    	editor.putFloat(Constants.prefkey_mNavigatorTargetPositionY, mNavigatorTargetPositionY);
	    }

    	/* renderer mode */
    	editor.putInt(Constants.prefkey_mRendererMode, mRendererMode);	    		
    	
    	/* app inactivity */
    	editor.putLong(Constants.prefkey_mLastAppUiActionTimestamp, System.currentTimeMillis());
    	editor.putLong(Constants.prefkey_mLastAppActionTimestamp, System.currentTimeMillis());
    	
    	editor.commit();
    	
    }
    
    /**
     * connectToService
     */
    private void connectToService(){
    	Intent intent = new Intent(this, RockOnNextGenService.class);
    	startService(intent);
    	bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }
    
    /** 
     * showIntro
     */
    private void showIntro(){
//    	setContentView(R.layout.intro);
//    	((IntroView)findViewById(R.id.intro_view)).
//    		setIntroBitmap(
//    				BitmapFactory.decodeResource(
//    						getResources(), 
//    						R.drawable.logo_intro));
//    	((IntroView)findViewById(R.id.intro_view)).
//    		setDoneHandler(mPassIntroHandler);
    	mPassIntroHandler.sendEmptyMessage(0);
    }
    
    /**
     * showNavigator
     */
    boolean oBottomControls = false;
    private void showNavigator(){
    	/** Check which layout to use */
    	oBottomControls = 
    		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    			getBoolean(Constants.prefkey_mControlsOnBottom, getResources().getBoolean(R.bool.config_controlsOnBottom));
    	if(!oBottomControls)
    		setContentView(R.layout.navigator_main);
    	else
    		setContentView(R.layout.navigator_main_controls_down);
    	
    	/** Setup our 3d accelerated Surface */
    	mGlSurfaceView = (GLSurfaceView) findViewById(R.id.cube_surface_view);
    	/*************************************************
         * 
         * OPENGL ES HACK FOR GALAXY AND OTHERS
         * 
         *************************************************/
    	mGlSurfaceView.setEGLConfigChooser(
        	     new GLSurfaceView.EGLConfigChooser() {
					@Override
					public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
     	               int[] attributes=new int[]{
       	                    //EGL10.EGL_RED_SIZE,
       	                    //5,
       	                    //EGL10.EGL_BLUE_SIZE,
       	                    //5,
       	                    //EGL10.EGL_GREEN_SIZE,
       	                    //6,
       	                    EGL10.EGL_DEPTH_SIZE,
       	                    16,
       	                    EGL10.EGL_NONE
       	               };
       	               EGLConfig[] configs=new EGLConfig[1];
       	               int[] result=new int[1];
       	               egl.eglChooseConfig(display,attributes,configs,1,result);
       	               return configs[0];
					}
        	     }
        	);
//    	mGlSurfaceView.setEGLConfigChooser(
//    			5, 6, 5, // RGB 
//    			0, 
//    			16, 
//    			0);
        /************************************************
         * HACK END
         ************************************************/
    	
    	/** Setup the appropriate renderer and theme */
        mBrowseCatMode = 
        	PreferenceManager.
        		getDefaultSharedPreferences(getApplicationContext()).
        			getInt(Constants.prefkey_mBrowseCatMode, Constants.BROWSECAT_ALBUM);
        mRendererMode = 
        	PreferenceManager.
        		getDefaultSharedPreferences(getApplicationContext()).
        			getInt(Constants.prefkey_mRendererMode, getResources().getInteger(R.integer.config_defaultRenderer));
        mTheme = 
        	PreferenceManager.
        		getDefaultSharedPreferences(getApplicationContext()).
        			getInt(Constants.prefkey_mTheme, Constants.THEME_NORMAL);
//        			getInt(Constants.prefkey_mBrowseCatMode, Constants.BROWSECAT_ARTIST);
        
        /** Set up category browser */
        AlbumArtistSwitcherView switcher = 
        	(AlbumArtistSwitcherView) findViewById(R.id.player_artist_album_switcher);
        if(switcher != null)
        {
        	switcher.setStateChangeHandler(mBrowseCatChanged);
        }
        
        /** Clear up memory before 
         * setting up caches in the renderers */
        System.gc();
        
        /** XXX: small song browsing hack - force boring mode */
        int rendererTmp = mRendererMode;
        if(mBrowseCatMode == Constants.BROWSECAT_SONG)
        	rendererTmp = Constants.RENDERER_BORING;
        // 
        switch(rendererTmp)
        {
        case Constants.RENDERER_CUBE:
        	RockOnCubeRenderer rockOnCubeRenderer = new RockOnCubeRenderer(
	        		getApplicationContext(),
	        		mRequestRenderHandler,
	        		mTheme,
	        		mBrowseCatMode);
	   		mGlSurfaceView.setRenderer(rockOnCubeRenderer);
	   		mRockOnRenderer = (RockOnRenderer) rockOnCubeRenderer;	
	   		break;
        case Constants.RENDERER_WALL:
        	RockOnWallRenderer rockOnWallRenderer = new RockOnWallRenderer(
	        		getApplicationContext(),
	        		mRequestRenderHandler,
	        		mTheme,
	        		mBrowseCatMode);
	   		mGlSurfaceView.setRenderer(rockOnWallRenderer);
	   		mRockOnRenderer = (RockOnRenderer) rockOnWallRenderer;	
	        break;
        case Constants.RENDERER_BORING:
        	RockOnBoringRenderer rockOnBoringRenderer = new RockOnBoringRenderer(
	        		getApplicationContext(),
	        		mRequestRenderHandler,
	        		mTheme,
	        		mBrowseCatMode);
	   		mGlSurfaceView.setRenderer(rockOnBoringRenderer);
	   		mRockOnRenderer = (RockOnRenderer) rockOnBoringRenderer;	
	        break;
        case Constants.RENDERER_MORPH:
        	RockOnMorphRenderer rockOnRopeRenderer = new RockOnMorphRenderer(
	        		getApplicationContext(),
	        		mRequestRenderHandler,
	        		mTheme,
	        		mBrowseCatMode);
	   		mGlSurfaceView.setRenderer(rockOnRopeRenderer);
	   		mRockOnRenderer = (RockOnRenderer) rockOnRopeRenderer;	
	        break;
        }
    	
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        /** check if we were able to find any music */
    	if(mRockOnRenderer.getAlbumCount() <= 0)
        {
        	mIsSdCardPresentAndHasMusic = false;
   			showNoMusicAlert();
   			return;
        } else {
        	if(mRockOnRenderer.getAlbumCursor() != null)
        		startManagingCursor(mRockOnRenderer.getAlbumCursor());
        }
    	
    	/** Get our NavScrollerView */
    	mNavScroller = (NavScrollerView)findViewById(R.id.player_nav_scroller);
    	mNavScroller.setRenderer(mRockOnRenderer);
    }
    
//    Handler mKillAppAndRestartPlaybackHandler = new Handler()
//    {
//    	public void handleMessage(Message msg)
//    	{
//    		Log.i(TAG, "handling kill process message");
//    	}
//    };
    
    /**
     * handler for updating the surface view
     */
    Handler mRequestRenderHandler = new Handler(){
    	
    	int mScrollSampling = 9;
    	int mScrollCount = 0;
    	
    	public void handleMessage(Message msg){
    		/** update rendering mode */
    		if(mGlSurfaceView.getRenderMode() != msg.what)
    			mGlSurfaceView.setRenderMode(msg.what);
    		
    		/** update scroller */
    		if(mNavScroller != null)
    		{
    			switch(msg.what)
    			{
    			case GLSurfaceView.RENDERMODE_WHEN_DIRTY:
	    			mNavScroller.fadeOut();
    				break;
    			case GLSurfaceView.RENDERMODE_CONTINUOUSLY:
    				if(mRockOnRenderer.isSpinning())
    				{
    					mScrollCount++;
	        			if(mScrollCount == mScrollSampling)
	        			{
	    	    			mNavScroller.updatePosition(mRockOnRenderer.getScrollPosition());
	    	    			mScrollCount = 0;
	        			}
    				}
    				break;
    			}

    		}
    	}
    };
    
    /**
     * showFullScreen
     */
    private void showFullScreen(){
    	
    }
    
    /**
     * Show Search auto complete box
     */
    private void showSearch(){
    	/** inflate our new group of views */
    	ViewGroup searchViewGroup = (ViewGroup) 
    		((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
    			inflate(
    				R.layout.search_layout, 
    				null);
    	/** add it to the current layout */
    	((ViewGroup) findViewById(R.id.the_mother_of_all_views_navigator)).
    			addView(
    					searchViewGroup, 
    					new LayoutParams(
    							LayoutParams.FILL_PARENT, 
    							LayoutParams.FILL_PARENT));
    }
    
    private void prepareSearch(){
    	/** get our autocomplete stuff */
    	setupAutoCompleteSearch(mPlaylistId);
    	((AutoCompleteTextView)findViewById(R.id.search_textview)).
			setOnItemClickListener(mSongSearchClickListener);
//    	((AutoCompleteTextView)findViewById(R.id.search_textview)).
//    		setOnLongClickListener(mSongSearchLongClickListener);
    	findViewById(R.id.search_textview).requestFocus();
    	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
    		showSoftInput(
    				findViewById(R.id.search_textview), 
    				InputMethodManager.SHOW_FORCED);
    }
    
    private void hideSearch(){
    	/* hide also the input keyboard */
    	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
    			hideSoftInputFromWindow(
    					findViewById(R.id.search_textview).getWindowToken(), 
    					0);
    	
    	/* get rid of the search ui */
    	((ViewGroup)findViewById(R.id.search_container)).removeAllViewsInLayout();
    	ViewGroup baseLayout = (ViewGroup) findViewById(R.id.the_mother_of_all_views_navigator);
    	baseLayout.removeView(findViewById(R.id.search_container));
     	
    	
    }
    
    /**
     * resumeState
     */
    private void resumeState(){
    	switch(mState){
        case Constants.STATE_INTRO:
        	// TODO: nothing?
        	return;
        case Constants.STATE_NAVIGATOR:
        	resumeNavigatorState();
        	return;
        case Constants.STATE_FULLSCREEN:
        	// TODO: ??
        	return;
        }	
    }
    
    /**
     * initialize state
     * 	- for now we only need this for the playlist 
     * 		(it is needed to initialize the cube and other stuff)
     */
    private void initializeState(){
    	mPlaylistId = Constants.PLAYLIST_ALL;
    	/** compatibility code */
    	Editor editor = 
    		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    			edit();
    	editor.putInt(Constants.prefkey_mPlaylistId, mPlaylistId);
    	editor.commit();
//    	/* it is only needed for the playlist */
//    	if(mPlaylistId == Constants.PLAYLIST_UNKNOWN)
//    		mPlaylistId = 
//    			PreferenceManager.
//    				getDefaultSharedPreferences(getApplicationContext()).
//    					getInt(Constants.prefkey_mPlaylistId, Constants.PLAYLIST_ALL);
//    	/* sanity check */
//        if(mPlaylistId == Constants.PLAYLIST_UNKNOWN)
//        	mPlaylistId = Constants.PLAYLIST_ALL;
    }
    
    private void resumeNavigatorState(){
    	/* update current song label */
    	updateTrackMetaFromService();
    	setCurrentSongLabels(
    			mTrackName, 
    			mArtistName, 
    			mTrackDuration, 
    			mTrackProgress);
    	updateCurrentPlayerStateButtonsFromServiceHandler
    		.sendEmptyMessage(0);
    
    	Log.i(TAG, "interval from last activity: "+
    			(System.currentTimeMillis()
    			-
    			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    				getLong(Constants.prefkey_mLastAppUiActionTimestamp, 0)));
    	
    	/* set the navigator in the right position */
    	if(System.currentTimeMillis()
    			-
    			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
    				getLong(Constants.prefkey_mLastAppUiActionTimestamp, 0)
    			<
    			Constants.MAX_INACTIVITY_INTERVAL_TO_MAINTAIN_STATE
    	)
    	{
	    	if(mNavigatorPositionY == -1)
	    		mNavigatorPositionY = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
	    			getFloat(Constants.prefkey_mNavigatorPositionY, 0);
	    	if(mNavigatorTargetPositionY == -1)
	    		mNavigatorTargetPositionY = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
	    			getFloat(Constants.prefkey_mNavigatorTargetPositionY, 0);
	    	if(mNavigatorPositionX == -1)
	    		mNavigatorPositionX = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
	    			getFloat(Constants.prefkey_mNavigatorPositionX, 0);
	    	if(mNavigatorTargetPositionX == -1)
	    		mNavigatorTargetPositionX = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
	    			getFloat(Constants.prefkey_mNavigatorTargetPositionX, 0);
	    	setNavigatorPosition(
	    			mNavigatorPositionX, mNavigatorTargetPositionX,
	    			mNavigatorPositionY, mNavigatorTargetPositionY);
    	}
    	else
    	{
//    		mSetNavigatorCurrent.sendEmptyMessage(0);
    		try
    		{
            	updateNavigatorToCurrent(mService);
//            	mRockOnRenderer.setCurrentByAlbumId(mService.getAlbumId());
	    		mNavigatorPositionX = mRockOnRenderer.getPositionX();
	    		mNavigatorPositionY = mRockOnRenderer.getPositionY();
	    		mNavigatorTargetPositionX = mRockOnRenderer.getTargetPositionX();
	    		mNavigatorTargetPositionY = mRockOnRenderer.getTargetPositionY();
    		}
    		catch(Exception e)
    		{
//    			CursorUtils cUtils = new CursorUtils(getApplicationContext());
//    			Cursor c = cUtils.getAlbumListFromPlaylist(mPlaylistId);
//    			c.moveToFirst();
//    			mRockOnRenderer.setCurrentByAlbumId(
//    					c.getLong(
//    							c.getColumnIndexOrThrow(
//    									MediaStore.Audio.Albums._ID)));
//    			c.close();
    			e.printStackTrace();
    		}
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
    
    /** 
     * showAlbumArtDownloadDialog
     */
    private void showAlbumArtDownloadDialog(){
    	
    	if(mAlbumArtDownloadOkClickListener == null)
    		mAlbumArtDownloadOkClickListener = 
    			new AlbumArtDownloadOkClickListener(this);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(getResources().getString(R.string.get_art_dialog_title));
		dialogBuilder.setMessage(getResources().getString(R.string.get_art_dialog_message));
		dialogBuilder.setPositiveButton(
				getResources().getString(R.string.get_art_dialog_yes), 
				mAlbumArtDownloadOkClickListener);
		dialogBuilder.setNegativeButton(
				getResources().getString(R.string.get_art_dialog_no), 
				mAlbumArtDownloadOkClickListener);
		dialogBuilder.setCancelable(false);
		dialogBuilder.show();
    }
    
    /**
     * Attach UI Listeners
     */
    public void attachListeners(){
    	switch(mState){
    	case Constants.STATE_NAVIGATOR:
    		/* Renderer scrolling */
    		if(mGlSurfaceView != null){
	        	NavGLTouchListener 	navGLTouchListener = new NavGLTouchListener();
//	        	navGLTouchListener.setRenderer(mRockOnCubeRenderer);
	        	navGLTouchListener.setRenderer(mRockOnRenderer);
	        	navGLTouchListener.setClickHandler(mAlbumClickHandler);
	        	navGLTouchListener.setTimeoutHandler(mSetNavigatorCurrent);
	        	mGlSurfaceView.setOnTouchListener(navGLTouchListener);
//	        	AlbumClickListener	albumClickListener = 
//	        		new AlbumClickListener(
//	        			this, 
//	        			mRockOnCubeRenderer);
//	        	mGlSurfaceView.setOnClickListener(albumClickListener);
    		}
    		/* NavScroller */
    		if(mNavScroller != null){
	        	NavScrollerTouchListener 	navScrollerTouchListener = new NavScrollerTouchListener();
	        	navScrollerTouchListener.setScrollerHandler(mNavScrollerHandler);
	        	mNavScroller.setOnTouchListener(navScrollerTouchListener);
    		}
    		
    		/* Progress bar seek */
    		ProgressBarTouchListener progressTouchListener = new ProgressBarTouchListener();
    		progressTouchListener.setSeekHandler(mSeekHandler);
    		/* Artist/Album switcher */
    		if(findViewById(R.id.player_artist_album_switcher) != null)
    			findViewById(R.id.player_artist_album_switcher).
    				setOnTouchListener(new SwitcherViewTouchListener());
    		findViewById(R.id.progress_bar).setOnTouchListener(progressTouchListener);
    		/* Play/next/previous button click listener */
    		findViewById(R.id.player_controls_play).setOnClickListener(mPlayPauseClickListener);
    		findViewById(R.id.player_controls_next).setOnClickListener(mNextClickListener);
    		findViewById(R.id.player_controls_previous).setOnClickListener(mPreviousClickListener);
    		/* Repeat and Shuffle Buttons */
    		findViewById(R.id.player_controls_repeat).setOnClickListener(mRepeatClickListener);
    		findViewById(R.id.player_controls_shuffle).setOnClickListener(mShuffleClickListener);
    		/* Search */
    		findViewById(R.id.search_button).setOnClickListener(mSearchClickListener);
    		/* Play Queue */
    		findViewById(R.id.play_queue_button).setOnClickListener(mPlayQueueClickListener);
        	return;
    	}
    }
    
    /**
     * 
     */
    private Handler mNavScrollerHandler = new Handler()
    {
    	float	mTargetPos = 0;
    	
    	@Override
    	public void handleMessage(Message msg)
    	{
    		/**
    		 * Reset the scroll timeout
    		 */
    		if(mSetNavigatorCurrent.hasMessages(0))
    		{
    			mSetNavigatorCurrent.removeCallbacksAndMessages(null);
    			mSetNavigatorCurrent.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT);
    		}
    		
    		/** Nav Scroller will check if the touched area
    		 *  is within the 'grabbable' area 
    		 */
//    		if((mTargetPos = mNavScroller.getPositionFromY(msg.arg2)) > 0)
//    			mRockOnRenderer.setCurrentTargetYByProgress(mTargetPos);
    		if(msg.what == MotionEvent.ACTION_DOWN)
    			mNavScroller.setTouching(true);
    		else if(msg.what == MotionEvent.ACTION_UP)
    		{
    			mNavScroller.setTouching(false);
    			mRockOnRenderer.setCurrentTargetYByProgress(
    					mNavScroller.getCurrentPosition());
    		}
    		
    		mNavScroller.manualScrollToY(msg.arg2);
    		mNavScroller.invalidate();
    	}
    };
    
    
    /**
     * Pass Intro
     */
    Handler	mPassIntroHandler = new Handler(){
    	public void handleMessage(Message msg){
    		mState = Constants.STATE_NAVIGATOR;
    		showNavigator();
    		attachListeners();
    		resumeState();
    		// Check if this is the first boot or new version
    		if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    				.contains(getResources().getString(R.string.preference_key_version)))
    		{
        		showAlbumArtDownloadDialog();
        		Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
    				.edit();
        		editor.putString(getResources().getString(R.string.preference_key_version), "1");
        		editor.commit();
    		}
    	}
    };
    
    /** 
     * Navigator Timeout reset
     */
    Handler	mSetNavigatorCurrent = new Handler(){
    	@Override
    	public void handleMessage(Message msg){
    		try {
//				mRockOnCubeRenderer.setCurrentByAlbumId(mService.getAlbumId());
            	updateNavigatorToCurrent(mService);
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
    	}
    };
    
    /**
     * onKeyDown
     */
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		switch(keyCode){
		/**
		 * BACK when search thing are visible
		 */
		case KeyEvent.KEYCODE_BACK:
			if(findViewById(R.id.search_container) != null)
				hideSearch();
			else{
				super.onKeyDown(keyCode, event);
			}
			return true;
		/**
		 * SEARCH triggers search
		 */
		case KeyEvent.KEYCODE_SEARCH:
			mSearchClickListener.onClick(null);
			return true;
		}
		
    	return false;
    	
    };
    
    /**
     * Player controls Listener and helpers
     */
    OnClickListener mPlayPauseClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mPlayPauseClickHandler.hasMessages(0)){
				APILevelChecker.getInstance().hapticFeedback(v);
				try{
					if(mService.isPlaying()){
						setPlayButton();
						trackSongProgress(Constants.HARD_REFRESH);
					}
					else{
						setPauseButton();
						stopTrackingSongProgress();
					}
				} catch(Exception e){
					e.printStackTrace();
				}
					
				mPlayPauseClickHandler.sendEmptyMessage(0);
//				mPlayPauseClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler	mPlayPauseClickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(mService != null){
				try{
					/* check if service playing something */
					if(mService.isPlaying()){
						mService.pause();
//						Log.i(TAG, "was playing... now pausing...");
						// TODO: stop progress update
						// TODO: change button image
					} else {
						/* There are still songs to be played in the queue */
						if(mService.getQueuePosition() >= mService.getQueue().length ||
								mService.getAudioId() >= 0){
							mService.play(); // or is it resume...
//							Log.i(TAG, "queue has songs... lets play or resume them...");
						} 
						/* The play queue has been fully played */
						else {
//							if(mRockOnCubeRenderer.isSpinning())
							if(mRockOnRenderer.isSpinning())
									return;
							// Queue a song of the currently displayed album
							else{
//								int albumId = mRockOnCubeRenderer.getShownAlbumId();
								int albumId = mRockOnRenderer.getShownElementId(
										findViewById(R.id.cube_surface_view).getWidth()/2,
										findViewById(R.id.cube_surface_view).getHeight()/2);
								Log.i(TAG, "current album: "+albumId);

								if(albumId == -1)
									return;
								else{
									// song list cursor
									CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
									Cursor		songCursor = cursorUtils.getSongListCursorFromAlbumId(
											albumId, 
											mPlaylistId); // TODO: read the actual playlist ID
									songCursor.moveToFirst();
									// queue the first? song
									long[] songIdList = new long[1];
									songIdList[0] = 
										ContentProviderUnifier.
											getAudioIdFromUnknownCursor(songCursor); 
//										songCursor.getLong(
//											songCursor.getColumnIndexOrThrow(
//													MediaStore.Audio.Media._ID));
									Log.i(TAG, "enqueing song: "+songCursor.getString(songCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
									mService.enqueue(songIdList, Constants.NOW); // calls play already
									songCursor.close();
								}
									
							}
						}
					}
				} catch (Exception e){
					e.printStackTrace();
				}
			}else{
				Log.i(TAG, "service interface has not been created");
			}
		}
	};
	
	OnClickListener mNextClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mNextClickHandler.hasMessages(0))
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				mNextClickHandler.sendEmptyMessage(0);
//				mNextClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler	mNextClickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try {
				mService.next();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch(IllegalStateException e) {
				e.printStackTrace();
			}
		}
	};
	
	OnClickListener mPreviousClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mPreviousClickHandler.hasMessages(0))
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				mPreviousClickHandler.sendEmptyMessage(0);
//				mPreviousClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler	mPreviousClickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try {
				mService.prev();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};
	
	OnClickListener mRepeatClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mRepeatClickHandler.hasMessages(0))
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				mRepeatClickHandler.sendEmptyMessage(0);
//				mRepeatClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler mRepeatClickHandler = new Handler(){
		
		@Override
		public void handleMessage(Message msg){
			try{
				if(mService.getRepeatMode() == Constants.REPEAT_CURRENT){
					mService.setRepeatMode(Constants.REPEAT_ALL);
					setRepeatButton(Constants.REPEAT_ALL);
				} else if ( mService.getRepeatMode() == Constants.REPEAT_ALL){
					mService.setRepeatMode(Constants.REPEAT_NONE);
					setRepeatNoneButton();
				} else {
					mService.setRepeatMode(Constants.REPEAT_CURRENT);
					setRepeatButton(Constants.REPEAT_CURRENT);
				}
			}catch(Exception e){
				e.printStackTrace();
			}			
		}
	};
	
	OnClickListener mShuffleClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mShuffleClickHandler.hasMessages(0))
			{
				APILevelChecker.getInstance().hapticFeedback(v);
				mShuffleClickHandler.sendEmptyMessage(0);
//				mShuffleClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler mShuffleClickHandler = new Handler(){
		
		@Override
		public void handleMessage(Message msg){
			try{
				if(mService.getShuffleMode() == Constants.SHUFFLE_AUTO ||
						mService.getShuffleMode() == Constants.SHUFFLE_NORMAL){
					mService.setShuffleMode(Constants.SHUFFLE_NONE);
					setShuffleNoneButton();
				} else {
					mService.setShuffleMode(Constants.SHUFFLE_NORMAL);
					setShuffleButton();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
	};
	
	OnClickListener mSearchClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mSearchClickHandler.hasMessages(0)){
				APILevelChecker.getInstance().hapticFeedback(v);
				if(findViewById(R.id.search_container) ==  null)
					showSearch();
				else
					hideSearch();
				mSearchClickHandler.sendEmptyMessage(0);
//				mSearchClickHandler.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler	mSearchClickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(findViewById(R.id.search_container) !=  null)
				prepareSearch();
		}
	};
	
	OnClickListener mPlayQueueClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!mPlayQueueClickHandler.hasMessages(0)){
				APILevelChecker.getInstance().hapticFeedback(v);
				mPlayQueueClickHandler.sendEmptyMessage(0);
//				mPlayQueueClickHandler.sendEmptyMessageDelayed(
//						0, 
//						Constants.CLICK_ACTION_DELAY);
			}
		}
	};
	
	Handler mPlayQueueClickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try{
				/* song list cursor */
				final int RESULT_LIMIT = 50; // need to limit this to avoid memory exhaustion in MergeCursor
				CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
//				long[] outstandingQueue = mService.getOutstandingQueue();
				long[] outstandingQueue = mService.getQueue();
				Cursor		songCursor = 
					cursorUtils.getSongListCursorFromSongList(
						outstandingQueue,
						0,
						RESULT_LIMIT); // show at most 50 songs in the queue 
				
				// TODO: read the actual playlist ID

				if(songCursor != null){
					startManagingCursor(songCursor);
					songCursor.moveToFirst();				
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
					/* dialog title - album and artist name */
					dialogBuilder.setTitle(
							getString(R.string.play_queue_title));
					/* show the album song list  in a dialog */
					SongCursorAdapter songCursorAdapter = 
						new SongCursorAdapter(
								getApplicationContext(),
								Constants.queueSongListLayoutId, //R.layout.songlist_dialog_item or android.R.layout.select_dialog_item
								songCursor, 
								Constants.queueSongListFrom, 
								Constants.queueSongListTo,
								75, // show at most 75 results
								outstandingQueue.length - RESULT_LIMIT,
								mPlayQueueItemSelectedHandler);
					dialogBuilder.setAdapter(
	 						songCursorAdapter,
	 						null);
//							new SongSelectedClickListener(
//									mSongItemSelectedHandler, 
//									songCursor)); // can be null
					dialogBuilder.setNegativeButton(
							R.string.clear_playlist_dialog_option, 
							mDialogPlayQueueClickListener);
					dialogBuilder.setOnCancelListener(mSongAndAlbumDialogCancelListener);
					dialogBuilder.setPositiveButton(
							R.string.create_playlist_dialog_option, 
							mDialogPlayQueueClickListener);
					/* set the selection listener */
					AlertDialog dialog = dialogBuilder.show();
					songCursorAdapter.setDialogInterface(dialog);
					/* scroll the list view */
//					dialog.getListView().setSelection(mService.getQueuePosition());
					dialog.getListView().setSelectionFromTop(mService.getQueuePosition(), 32);
					
				} else {
					// Play queue is empty
					Toast.makeText(
							getApplicationContext(), 
							R.string.playqueue_empty_toast, 
							Toast.LENGTH_SHORT).show();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	};

	/**
	 * 
	 */
	DialogInterface.OnClickListener mDialogPlayQueueClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch(which)
			{
			/** 
			 * Positive Button 
			 * 
			 * (Save playlist)
			 */
			case DialogInterface.BUTTON_POSITIVE:
				showCreatePlaylistDialog();
				break;
			/**
			 * Negative button
			 * 
			 * (Clear play queue)
			 */
			case DialogInterface.BUTTON_NEGATIVE:
				if(mService != null){
					try{
						mService.removeTracks(
							0,
							mService.getQueue().length-1);
						updateCurrentPlayerStateButtonsFromServiceHandler.sendEmptyMessage(0);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				break;
			}
		}
	};
	
	/**
	 * 
	 */
	private void showCreatePlaylistDialog()
	{
		AlertDialog.Builder aBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
		aBuilder.setTitle(R.string.create_playlist_dialog_title);
		View setPlaylistNameView = 
			((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
				inflate(R.layout.create_playlist_dialog, null);
		final EditText setPlaylistNameEditText = (EditText)
			setPlaylistNameView.findViewById(R.id.new_playlist_name);
		aBuilder.setView(setPlaylistNameView);
		aBuilder.setPositiveButton(
				R.string.create_playlist_dialog_save_option, 
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(setPlaylistNameEditText.getText().toString() != null &&
							setPlaylistNameEditText.getText().toString().length() > 0)
						{
							createPlaylist(setPlaylistNameEditText.getText().toString());
						}
						else
							Toast.makeText(
									RockOnNextGenGL.this, 
									R.string.create_playlist_toaster_name_required, 
									Toast.LENGTH_SHORT).
								show();
					}
				});
		aBuilder.show();
	}
	
	private void createPlaylist(String playlistName)
	{
		try
		{
			CursorUtils cUtils = new CursorUtils(RockOnNextGenGL.this);
			if(!cUtils.createPlaylist(playlistName))
				throw new Exception();
			long playlistId = cUtils.getPlaylistIdFromName(playlistName);
//			cUtils.addSongsToPlaylist(playlistId, mService.getOutstandingQueue());
			cUtils.addSongsToPlaylist(playlistId, mService.getQueue());
			String message = 
//				(mService.getOutstandingQueue().length-1) +
				(mService.getQueue().length) +
				" " + getString(R.string.create_playlist_toaster_added_part_1) +
				" '" + playlistName + "'!"; 
			Toast.makeText(
					RockOnNextGenGL.this, 
					message, 
					Toast.LENGTH_SHORT).
				show();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Toast.makeText(
					RockOnNextGenGL.this, 
					R.string.create_playlist_toaster_failed, 
					Toast.LENGTH_SHORT).
				show();
		}
	}
	
	/**
	 * AlbumClick Handler
	 */
	Handler mAlbumClickHandler = new Handler(){
		int	x;
		int	y;
		int position;
		@Override
		public void handleMessage(Message msg){
			x = msg.arg1;
			y = msg.arg2;
			if(msg.obj != null)
				position = (Integer)msg.obj;
			else
				position = mRockOnRenderer.getShownPosition(x, y);
			if(msg.what == Constants.SINGLE_CLICK){
				if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_ALBUM)
				{
					/* song list cursor */
//					int albumId = mRockOnRenderer.getShownElementId(x, y);
					int albumId = mRockOnRenderer.getElementId(position);
					if(albumId < 0)
					{
						this.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
						return;
					}
					
					showSongListDialogFromAlbum(
							albumId, 
							Constants.NO_SPECIFIC_ARTIST, // no specific artist
							mRockOnRenderer.getAlbumArtistName(position),
							mRockOnRenderer.getAlbumName(position));
//							mRockOnRenderer.getShownAlbumArtistName(x, y),
//							mRockOnRenderer.getShownAlbumName(x, y));
				}
				else if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_ARTIST)
				{
					/* song list cursor */
//					int artistId = mRockOnRenderer.getShownElementId(x, y);
					int artistId = mRockOnRenderer.getElementId(position);
					if(artistId < 0){
						this.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
						return;
					}
					
					showAlbumListDialogFromArtist(
							artistId,
							mRockOnRenderer.getAlbumArtistName(position));
//							mRockOnRenderer.getShownAlbumArtistName(x, y));

				}
				else if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_SONG)
				{
					/* song list cursor */
//					int songId = mRockOnRenderer.getShownElementId(x, y);
					int songId = mRockOnRenderer.getElementId(position);
					Log.i(TAG, "SongId: "+songId);
					if(songId < 0){
						this.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
						return;
					}
					
					/* Play song */
					Message newMsg = new Message();
					newMsg.arg1 = songId;
					newMsg.arg2 = Constants.SINGLE_CLICK;
					mSongItemSelectedHandler.sendMessageDelayed(
							newMsg, 
							Constants.CLICK_ACTION_DELAY);
										
					/* Reverse click animation */
					mRockOnRenderer.reverseClickAnimation();
				}
				
			} else if(msg.what == Constants.LONG_CLICK){
				if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_ALBUM)
				{
					// start album chooser activity
					Intent intent = new Intent(RockOnNextGenGL.this, ManualAlbumArtActivity.class);
//					intent.putExtra("albumId", (long)mRockOnRenderer.getShownElementId(x, y));
					intent.putExtra("albumId", (long)mRockOnRenderer.getElementId(position));
					startActivityForResult(intent, Constants.ALBUM_ART_CHOOSER_ACTIVITY_REQUEST_CODE);
				}
				else if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_ARTIST)
				{
					mRockOnRenderer.reverseClickAnimation();
					Toast.makeText(
							getApplicationContext(), 
							R.string.not_an_album_toast, 
							Toast.LENGTH_SHORT)
						.show();
				}
				else if(mRockOnRenderer.getBrowseCat() == Constants.BROWSECAT_SONG)
				{
					/* song options */
//					int songId = mRockOnRenderer.getShownElementId(x, y);
					int songId = mRockOnRenderer.getElementId(position);
					if(songId < 0){
						this.sendEmptyMessageDelayed(0, Constants.CLICK_ACTION_DELAY);
						return;
					}
					
//					String songName = mRockOnRenderer.getShownSongName(x,y);
					String songName = mRockOnRenderer.getSongName(position);
					
					// Show song options
					showSongOptionsDialog(songId, songName);
				}
			}
			
		}
	};
	
	/**
	 * 
	 * @param albumId
	 * @param artistName
	 * @param albumName
	 */
	private void showSongListDialogFromAlbum(int albumId, int artistId, String artistName, String albumName)
	{
		CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
		Cursor		songCursor = null;
		if(artistId == Constants.NO_SPECIFIC_ARTIST)
		{
			songCursor = cursorUtils.getSongListCursorFromAlbumId(
				albumId, 
				mPlaylistId); 
		}
		else
		{
			songCursor = cursorUtils.getSongListCursorFromAlbumAndArtistId(
				albumId,
				artistId,
				mPlaylistId); 
		}
		if(songCursor != null){
			startManagingCursor(songCursor);
			songCursor.moveToFirst();				
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
			/* dialog title - album and artist name */
			dialogBuilder.setTitle(
					artistName+"\n"+
					albumName);
//					mRockOnRenderer.getShownAlbumArtistName(x, y)+"\n"+
//					mRockOnRenderer.getShownAlbumName(x, y));
			SongCursorAdapter songCursorAdapter = 
				new SongCursorAdapter(
						getApplicationContext(), 
						Constants.albumSongListLayoutId,
						songCursor, 
						Constants.albumSongListFrom, 
						Constants.albumSongListTo,
						1000000, // will never reach limit -- only useful in play queue
						0, // no extra results
						mSongItemSelectedHandler);
			dialogBuilder.setAdapter(
						songCursorAdapter,
						null);
			dialogBuilder.setPositiveButton(getString(R.string.album_song_list_play_all), mSongDialogPlayAllListener);
			dialogBuilder.setNeutralButton(getString(R.string.album_song_list_queue_all), mSongDialogQueueAllListener);
			dialogBuilder.setOnCancelListener(mSongAndAlbumDialogCancelListener);
			/* set the selection listener */
			AlertDialog dialog = dialogBuilder.show();
			songCursorAdapter.setDialogInterface(dialog);
			/* scroll the dialog list to right place */
			try {
				if(albumId == mService.getAlbumId()) {
					long songId = mService.getAudioId();
					int j=-1;
					for(int i=0; i<songCursor.getCount(); i++) {
						songCursor.moveToPosition(i);
						if(songCursor.getLong(songCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)) == songId) {
							j=i;
						}
					}
					if(j>=0) {
						dialog.getListView().setSelectionFromTop(j, 32);
					}
				}
			} catch(RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param artistId
	 * @param artistName
	 */
	private void showAlbumListDialogFromArtist(int artistId, String artistName)
	{
		CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
		Cursor		albumCursor =
			cursorUtils.getAlbumListFromArtistId(
				artistId, 
				mPlaylistId); // TODO: read the actual playlist ID
		
		if(albumCursor != null){
			startManagingCursor(albumCursor);
			albumCursor.moveToFirst();				
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
			/* dialog title - album and artist name */
			if(albumCursor.getCount() == 1)
			{
				dialogBuilder.setTitle(
						artistName+"\n"+
						"("+
						albumCursor.getCount()+" "+getString(R.string.album)+
						")");
			}
			else
			{
				dialogBuilder.setTitle(
						artistName+"\n"+
						"("+
						albumCursor.getCount()+" "+getString(R.string.albums)+
						")");
			}
			AlbumCursorAdapter albumCursorAdapter = 
				new AlbumCursorAdapter(
						getApplicationContext(),
						Constants.artistAlbumListLayoutId,
						albumCursor,
						artistId,
						artistName,
						Constants.artistAlbumListFrom,
						Constants.artistAlbumListTo,
						mAlbumItemSelectedHandler);
			dialogBuilder.setAdapter(
						albumCursorAdapter,
						null);
			ArtistAlbumsDialogButtonClickListener artistAlbumsDialogClickListener = 
				new ArtistAlbumsDialogButtonClickListener(
						artistId, 
						mArtistAlbumListDialogOverallOptionsHandler, 
						mRockOnRenderer);
			dialogBuilder.setPositiveButton(getString(R.string.album_song_list_play_all), artistAlbumsDialogClickListener);
			dialogBuilder.setNeutralButton(getString(R.string.album_song_list_queue_all), artistAlbumsDialogClickListener);
			dialogBuilder.setOnCancelListener(mSongAndAlbumDialogCancelListener);
//			/* set the selection listener */
			albumCursorAdapter.setDialogInterface(dialogBuilder.show());
		}
	}
	
	/**
	 * 
	 */
	Handler mSongItemSelectedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try{
				if(msg.arg2 == Constants.SINGLE_CLICK) 
				{
					if(mService.isPlaying() && getQueueOnClickPreference(getApplicationContext()))
						queueTrack(msg.arg1, Constants.LAST);
					else
						queueTrack(msg.arg1, Constants.NOW);
						
					/* reverse the click animation */
					reverseRendererClickAnimation();	
				}
				else if(msg.arg2 == Constants.LONG_CLICK)
				{
					// show trackoptions dialog 
					showSongOptionsDialog(msg.arg1, (String)msg.obj);
				} 
			} catch(Exception e){
				e.printStackTrace();
				/* reverse the click animation */
				reverseRendererClickAnimation();	
			}
		}
	};
	
	/**
	 * 
	 * @param trackId
	 * @param priority
	 */
	private void queueTrack(int trackId, int priority)
	{
		try
		{
			long[] list = {trackId};
			switch(priority)
			{
			case Constants.NOW:
				mService.removeTracks(0, mService.getQueue().length-1);
				mService.enqueue(list, Constants.NOW);
				setPauseButton();
				setCurrentSongLabels(
						mService.getTrackName(),
						mService.getArtistName(),
						mService.duration(),
						mService.position());
				break;
			case Constants.LAST:
				mService.enqueue(list, Constants.LAST);
				Toast.makeText(
						getApplicationContext(), 
						R.string.song_added_to_playlist, 
						Toast.LENGTH_SHORT).
					show();	
				break;
			}
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
	
    /**
     * 
     * @param songId
     * @param songName
     */
    public void showSongOptionsDialog(int songId, String songName)
    {
    	// show a new dialog with Add to Queue, Delete
    	AlertDialog.Builder songOptsDBuilder = new AlertDialog.Builder(RockOnNextGenGL.this);
    	songOptsDBuilder.setTitle(songName);
    	songOptsDBuilder.setAdapter(
    			new ArrayAdapter<String>(
    					getApplicationContext(), 
    					android.R.layout.select_dialog_item, 
    					android.R.id.text1, 
    					getResources().getStringArray(R.array.song_options)), 
    			new PlaylistOptionClickListener(
    					songId, 
    					mSongOptionSelectedHandler));
    	songOptsDBuilder.setOnCancelListener(
    			new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// Reverse the animation
						mRockOnRenderer.reverseClickAnimation();
					}
    			});
    	songOptsDBuilder.show();
    }
    
    /**
     * 
     */
    Handler mSongOptionSelectedHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
    		String songOption = getResources().getStringArray(R.array.song_options)[msg.what];
    		if(songOption.equals(getString(R.string.song_option_play_now)))
    		{
    			// replace current playlist
    			queueTrack(msg.arg1, Constants.NOW); 
    		}
    		else if(songOption.equals(getString(R.string.song_option_add_to_queue)))
    		{
    			// add all playlist song to queue
    			queueTrack(msg.arg1, Constants.LAST); 
    		}
    		else if(songOption.equals(getString(R.string.song_option_delete)))
    		{
    			// show confirmation dialog
    			// before deleting
    			AlertDialog.Builder deleteSongConfirmationDialog = 
    				new AlertDialog.Builder(RockOnNextGenGL.this);
    			deleteSongConfirmationDialog.setTitle(
    					R.string.song_delete_confirm_title);
    			deleteSongConfirmationDialog.setMessage(
    					R.string.song_delete_message);
    			final int songId = msg.arg1;
    			deleteSongConfirmationDialog.setPositiveButton(
    					R.string.song_delete_positive_button, 
    					new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
				    			deleteSong(songId);								
							}
						});
    			deleteSongConfirmationDialog.setNegativeButton(
    					R.string.song_delete_negative_button, 
    					null);
    			deleteSongConfirmationDialog.show();
    		}	
			reverseRendererClickAnimation();
    	}
    };
    
	Handler mAlbumItemSelectedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try{
				Log.i(TAG, "IO - album slected");
				showSongListDialogFromAlbum(
						msg.getData().getInt("albumId"),
						msg.getData().getInt("artistId"),
						msg.getData().getString("artistName"), 
						msg.getData().getString("albumName"));
//				reverseRendererClickAnimation();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	};
	
	Handler mPlayQueueItemSelectedHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			Log.i(TAG, "NOW/LAST: "+msg.arg2);

			try{				
				long[] list = {msg.arg1};
				if(msg.arg2 == Constants.LONG_CLICK){
					mService.removeTrack(msg.arg1);
					Toast.makeText(
							RockOnNextGenGL.this, 
							R.string.song_removed_from_playlist, 
							Toast.LENGTH_SHORT).
						show();
				} else if(msg.arg2 == Constants.SINGLE_CLICK){
					Log.i(TAG, "NOW/LAST: "+msg.arg2);
					mService.removeTrack(msg.arg1);
					mService.enqueue(list, Constants.NOW);
					// this should all be done on upon the reception of the service intent 
					// notifying the new song
//					setPauseButton();
//					setCurrentSongLabels(
//							mService.getTrackName(),
//							mService.getArtistName(),
//							mService.duration(),
//							mService.position());
				}
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * OnCancelDialogListener
	 */
	OnCancelListener mSongAndAlbumDialogCancelListener = new OnCancelListener() {
		
		@Override
		public void onCancel(DialogInterface dialog) {
			reverseRendererClickAnimation();
			mSetNavigatorCurrent.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT);
		}
	};
	
	/**
	 * OnPositive/NeutralButtonListener
	 */
	android.content.DialogInterface.OnClickListener mSongDialogPlayAllListener = 
		new android.content.DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message msg = new Message();
				msg.what = 0;
				msg.obj = dialog;
				msg.arg1 = Constants.NOW;
//				mRockOnCubeRenderer.reverseClickAnimation();
				mRockOnRenderer.reverseClickAnimation();
				mSongListDialogOverallOptionsHandler.sendMessageDelayed(msg, Constants.CLICK_ACTION_DELAY);				
			}
		
	};
	
	android.content.DialogInterface.OnClickListener mSongDialogQueueAllListener = 
		new android.content.DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message msg = new Message();
				msg.what = 0;
				msg.obj = dialog;
				msg.arg1 = Constants.LAST;
//				mRockOnCubeRenderer.reverseClickAnimation();
				mRockOnRenderer.reverseClickAnimation();
				mSongListDialogOverallOptionsHandler.sendMessageDelayed(msg, Constants.CLICK_ACTION_DELAY);
			}
		
	};
	
	/**
	 * handler
	 */
	Handler mSongListDialogOverallOptionsHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg != null)
			{
				AlertDialog aD = (AlertDialog) msg.obj;
				SongCursorAdapter songCursorAdapter = (SongCursorAdapter) aD.getListView().getAdapter();
				if(songCursorAdapter != null)
				{
					Cursor	cursor = songCursorAdapter.getCursor();
					long[] songVector = new long[cursor.getCount()];
					for(int i = 0; i<cursor.getCount(); i++){
						cursor.moveToPosition(i);
						// The _ID field has a different meaning in playlist content providers
						//		-- if it is a playlist we need to fetch the AUDIO_ID field
						songVector[i] = 
							ContentProviderUnifier.
								getAudioIdFromUnknownCursor(cursor);
		//				// DEBUG TIME
		//				Log.i(TAG, i + " - " + 
		//						String.valueOf(
		//								cursor.getLong(
		//										cursor.getColumnIndexOrThrow(
		//												MediaStore.Audio.Media._ID))));
		//				Log.i(TAG, i + " - " + 
		//						cursor.getString(
		//										cursor.getColumnIndexOrThrow(
		//												MediaStore.Audio.Media.ARTIST)));
		//				Log.i(TAG, i + " - " + 
		//						cursor.getString(
		//										cursor.getColumnIndexOrThrow(
		//												MediaStore.Audio.Media.ALBUM)));
					}
					try{
						if(mService != null)
						{
							if(msg.arg1 == Constants.NOW)
								mService.removeTracks(0, mService.getQueue().length-1);
							mService.enqueue(songVector, msg.arg1);
						}
					}catch(RemoteException e){
						e.printStackTrace();
					}
				}
			}
		}
	};
	
	/**
	 * handler
	 */
	Handler mArtistAlbumListDialogOverallOptionsHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg != null)
			{
				CursorUtils cUtils = new CursorUtils(getApplicationContext());
				Cursor	cursor = cUtils.getSongListCursorFromArtistId((Long)msg.obj, Constants.PLAYLIST_ALL);
				long[] songVector = new long[cursor.getCount()];
				for(int i = 0; i<cursor.getCount(); i++){
					cursor.moveToPosition(i);
					// The _ID field has a different meaning in playlist content providers
					//		-- if it is a playlist we need to fetch the AUDIO_ID field
					songVector[i] = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(cursor);

				}
				try{
					if(mService != null)
					{
						if(msg.arg1 == Constants.NOW)
							mService.removeTracks(0, mService.getQueue().length-1);
						mService.enqueue(songVector, msg.arg1);
					}
				}catch(RemoteException e){
					e.printStackTrace();
				}
				System.gc();
			}
		}
	};
	
	/**
	 * reverse the click animation
	 */
	private void reverseRendererClickAnimation(){
//		mRockOnCubeRenderer.reverseClickAnimation();
		mRockOnRenderer.reverseClickAnimation();
	}

	/**
	 * Song Progress
	 */
	public void trackSongProgress(int refreshCode){
		mUpdateSongProgressHandler.removeCallbacksAndMessages(null);
		mUpdateSongProgressHandler.sendEmptyMessage(refreshCode);
	}
	
	/**
	 * 
	 */
	public void stopTrackingSongProgress(){
		mUpdateSongProgressHandler.removeCallbacksAndMessages(null);
	}
	
	Handler	mUpdateSongProgressHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			try{
//				if(!mRockOnCubeRenderer.isSpinning())
				if(!mRockOnRenderer.isSpinning() || msg.what == Constants.HARD_REFRESH)
				{
					if(msg.what == Constants.HARD_REFRESH)
						((ProgressBarView)findViewById(R.id.progress_bar)).
							setDuration((int) mService.duration(), false);
					((ProgressBarView)findViewById(R.id.progress_bar)).
						setProgress((int) mService.position(), false);
					((ProgressBarView)findViewById(R.id.progress_bar)).
						refresh();
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			if(msg.what == Constants.KEEP_REFRESHING ||
				msg.what == Constants.HARD_REFRESH)
				this.sendEmptyMessageDelayed(Constants.KEEP_REFRESHING, 1000);
		}
	};
	
	/**
	 * 
	 */
	Handler mSeekHandler = new Handler(){
		public void handleMessage(Message msg){
			if(mService != null){
				try{
					if(msg.what <= mService.duration())
						mService.seek(msg.what);
				} catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	};
	
	/**
	 * Controls and Playing Status
	 */
	public void setPlayButton(){
		if(findViewById(R.id.player_controls_play) != null)
			((ImageView)findViewById(R.id.player_controls_play)).
				setImageResource(R.drawable.play); 
	}
	
	/**
	 * 
	 */
	public void setPauseButton(){
		if(findViewById(R.id.player_controls_play) != null)
			((ImageView)findViewById(R.id.player_controls_play)).
				setImageResource(R.drawable.pause);
	}
	
	/**
	 * 
	 */
	public void setShuffleButton(){
		if(findViewById(R.id.player_controls_shuffle) != null)
			((ImageView)findViewById(R.id.player_controls_shuffle)).
				setImageResource(R.drawable.shuffle_selector);
	}
	
	/**
	 * 
	 */
	public void setShuffleNoneButton(){
		if(findViewById(R.id.player_controls_shuffle) != null)
			((ImageView)findViewById(R.id.player_controls_shuffle)).
				setImageResource(R.drawable.shuffle_none_selector);
	}
	
	/**
	 * 
	 * @param repeatCode
	 */
	public void setRepeatButton(int repeatCode){
		if(findViewById(R.id.player_controls_repeat) != null)
		{
			switch(repeatCode)
			{
			case Constants.REPEAT_CURRENT:
				((ImageView)findViewById(R.id.player_controls_repeat)).
					setImageResource(R.drawable.repeat_current_selector);
				break;
			case Constants.REPEAT_ALL:
				((ImageView)findViewById(R.id.player_controls_repeat)).
					setImageResource(R.drawable.repeat_all_selector);
				break;
			}
		}
	}
	
	/**
	 * 
	 */
	public void setRepeatNoneButton(){
		if(findViewById(R.id.player_controls_repeat) != null)
			((ImageView)findViewById(R.id.player_controls_repeat)).
				setImageResource(R.drawable.repeat_none_selector);
	}
	
	/**
	 * 
	 */
	Handler updateCurrentPlayerStateButtonsFromServiceHandler = new Handler(){
		public void handleMessage(Message msg){
			try{
				Log.i(TAG, "Checking Service null");
				if(mService != null){
					Log.i(TAG, "Service is not null");
					setCurrentPlayerStateButtons(
							mService.isPlaying(), 
							mService.getShuffleMode(), 
							mService.getRepeatMode());
				} else {
					this.sendEmptyMessageDelayed(0, 1000);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	};
	
	private void setCurrentPlayerStateButtons(
			boolean isPlaying,
			int		shuffle,
			int		repeat){
    	if(isPlaying){
    		setPauseButton();
    		trackSongProgress(Constants.HARD_REFRESH);
    	}
    	else{
    		setPlayButton();
    		stopTrackingSongProgress();
    	}
    	if(shuffle == Constants.SHUFFLE_NONE)
    		setShuffleNoneButton();
    	else
    		setShuffleButton();
    	if(repeat == Constants.REPEAT_NONE)
    		setRepeatNoneButton();
    	else if(repeat == Constants.REPEAT_CURRENT)
    		setRepeatButton(Constants.REPEAT_CURRENT);
    	else if(repeat == Constants.REPEAT_ALL)
    		setRepeatButton(Constants.REPEAT_ALL);
	}
	
	/**
	 * ??
	 * @param songName
	 * @param artistName
	 * @param songDuration
	 * @param trackProgress
	 */
	public void	setCurrentSongLabels(
			String	songName,
			String	artistName,
			long	songDuration,
			long	trackProgress)
	{
		mTrackName = songName;
		mArtistName = artistName;
		mTrackDuration = songDuration;
		mTrackProgress = trackProgress; // ??
//    	Log.i(TAG, "--track: "+mTrackName);
//    	Log.i(TAG, "--artist: "+mArtistName);
//    	Log.i(TAG, "--duration: "+mTrackDuration);
//    	Log.i(TAG, "--progress: "+mTrackProgress);
		((TextView)findViewById(R.id.song_name)).setText(songName);
		((TextView)findViewById(R.id.artist_name)).setText(artistName);
	}
	
	private boolean updateTrackMetaFromService(){
		try{
			if(mService != null){
				mTrackName = mService.getTrackName();
		    	mArtistName = mService.getArtistName();
		    	mTrackDuration = mService.duration();
		    	mTrackProgress = mService.position();
		    	return true;
			} else {
				return false;
			}
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	/** 
	 * Navigator resume state
	 */
	private void setNavigatorPosition(
			float positionX, 
			float targetPositionX,
			float positionY,
			float targetPositionY)
	{
//		mRockOnCubeRenderer.mPositionX = positionX;
//    	mRockOnCubeRenderer.mTargetPositionX = targetPositionX;
//    	mRockOnCubeRenderer.mPositionY = positionY;
//    	mRockOnCubeRenderer.mTargetPositionY = targetPositionY;
//    	mRockOnCubeRenderer.triggerPositionUpdate();
		mRockOnRenderer.mPositionX = positionX;
    	mRockOnRenderer.mTargetPositionX = targetPositionX;
    	mRockOnRenderer.mPositionY = positionY;
    	mRockOnRenderer.mTargetPositionY = targetPositionY;
    	mRockOnRenderer.triggerPositionUpdate();
	}
	
	/**
	 * Setup the adapter for the autocomplete search box
	 */
	private void setupAutoCompleteSearch(int playlistId){
    	/* cursor */
		Cursor allSongsCursor = new CursorUtils(getApplicationContext()).
    		getAllSongsFromPlaylist(playlistId);
		if(allSongsCursor != null)
			startManagingCursor(allSongsCursor);
    		
		/* adapter */
    	SimpleCursorAdapter songAdapter = new SimpleCursorAdapter(
    			getApplicationContext(),
    			R.layout.simple_dropdown_item_2line,
    			allSongsCursor,
    			new String[] {
    				MediaStore.Audio.Media.TITLE,
					MediaStore.Audio.Media.ARTIST},
    			new int[] {
    				R.id.autotext1, 
    				R.id.autotext2});
    	
    	try
    	{
	    	/* filter */
			AutoCompleteFilterQueryProvider songSearchFilterProvider = 
					new AutoCompleteFilterQueryProvider(
							getApplicationContext(), 
							playlistId);
			
			/* apply filter to view */
			songAdapter.setFilterQueryProvider(songSearchFilterProvider);
	    	/* apply adapter to view */
			((AutoCompleteTextView) findViewById(R.id.search_textview)).
	    			setAdapter(songAdapter);
	    	/* set conversion column of the adapter */
			songAdapter.setStringConversionColumn(
	    			allSongsCursor.getColumnIndexOrThrow(
	    					MediaStore.Audio.Media.TITLE));
    	}
    	catch(NullPointerException e)
    	{
    		e.printStackTrace();
    	}
	}
	
	/**
	 * AutoComplete Item Click
	 */
	OnItemClickListener mSongSearchClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> arg0, 
				View arg1, 
				int arg2,
				long arg3) 
		{
			Cursor	songCursor = ((SimpleCursorAdapter)arg0.getAdapter()).getCursor();
			long trackId = 
				ContentProviderUnifier.
					getAudioIdFromUnknownCursor(songCursor);
			String trackName = 
				songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));

			hideSearch();
			showSongOptionsDialog((int)trackId, trackName);

//			Message msg = new Message();
//			msg.arg1 = (int) trackId;
//			msg.arg2 = Constants.NOW;
//			mSongSearchClickHandler.sendMessageDelayed(msg, Constants.CLICK_ACTION_DELAY);
			songCursor.close();
		}
		
	};
	
	/**
	 * 
	 */
	Handler	mSongSearchClickHandler = new Handler(){
		public void handleMessage(Message msg){
			long[]	list = {
					msg.arg1
			};
			try{
				mService.enqueue(list, msg.arg2);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * 
	 * @param service
	 * @return
	 */
	private boolean updateNavigatorToCurrent(IRockOnNextGenService service)
	{
		try
		{
			switch(mRockOnRenderer.getBrowseCat())
			{
			case Constants.BROWSECAT_ALBUM:
				mRockOnRenderer.setCurrentByAlbumId(service.getAlbumId());
				break;
			case Constants.BROWSECAT_ARTIST:
				mRockOnRenderer.setCurrentByArtistId(service.getArtistId());
				break;		
			case Constants.BROWSECAT_SONG:
				mRockOnRenderer.setCurrentBySongId(service.getAudioId());
				break;
			}
			return true;
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Broadcast Receivers
	 */
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try{
	            if (action.equals(Constants.META_CHANGED)) {
	                // redraw the artist/title info and
	            	updateTrackMetaFromService();
	                setCurrentSongLabels(
	                		mTrackName, 
	                		mArtistName, 
	                		mTrackDuration, 
	                		mTrackProgress);
	            	// set new max for progress bar
	            	// TODO:
	            	if(mService != null && mService.isPlaying())
	            	{
	            		setPauseButton();
	            		trackSongProgress(Constants.HARD_REFRESH);
	            	}
	            	else
	            	{
	            		setPlayButton();
	            		stopTrackingSongProgress();
	            	}
	//                setPauseButtonImage();
	//                queueNextRefresh(1);
//	            	mRockOnCubeRenderer.setCurrentByAlbumId(mService.getAlbumId());
	            	updateNavigatorToCurrent(mService);
	            } else if (action.equals(Constants.PLAYBACK_COMPLETE)) {
	            	if(mService != null && mService.isPlaying()){
	            		setPauseButton();
	            		trackSongProgress(Constants.HARD_REFRESH);
	            	}
	            	else{
	            		setPlayButton();
	            		stopTrackingSongProgress();
	            	}
	//                if (mOneShot) {
	//                    finish();
	//                }
	            } else if (action.equals(Constants.PLAYSTATE_CHANGED)) {
	            	if(mService != null && mService.isPlaying()){
	            		setPauseButton();
	            		trackSongProgress(Constants.HARD_REFRESH);
	            	}
	            	else{
	            		setPlayButton();
	            		stopTrackingSongProgress();
	            	}
	//                setPauseButtonImage();
	            }
            } catch(Exception e){
            	e.printStackTrace();
            }
        }
    };

	/**
	 * Service connection
	 */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = IRockOnNextGenService.Stub.asInterface(obj);
            /* Media Button registration for 2.2+ 
             * - could go into the service
             */
            registerRemoteControl();
            
            try {
            	// need to pass preferences to service (service runs in separate process)
            	setPreferencesInService();
            	mService.trackPage(Constants.ANALYTICS_MAIN_PAGE);
            	
            	/**
            	 * Now let us resume the state
            	 */
                if(mService.getAudioId() >= 0 || mService.getPath() != null){
                	Log.i(TAG, "track: "+mTrackName);
                	Log.i(TAG, "artist: "+mArtistName);
                	Log.i(TAG, "duration: "+mTrackDuration);
                	Log.i(TAG, "progress: "+mTrackProgress);

                	// XXX - ??? DUPLICATED CODE ?
                	resumeState();
                	//updateTrackMetaFromService();
                	//updateCurrentPlayerStateButtonsFromServiceHandler
                	//	.sendEmptyMessage(0);                	
                	return;
                } else {
                	Log.i(TAG, "Not Playing...");
                }
            } 
            catch (RemoteException ex) {
            }
//            // Service is dead or not playing anything. If we got here as part
//            // of a "play this file" Intent, exit. Otherwise go to the Music
//            // app start screen.
//            if (getIntent().getData() == null) {
//                Intent intent = new Intent(Intent.ACTION_MAIN);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.setClass(RockOnNextGenGL.this, RockOnNextGenGL.class);
//                startActivity(intent);
//            }
//            finish();
        }
        
        
        public void onServiceDisconnected(ComponentName classname) {
        }
};

}