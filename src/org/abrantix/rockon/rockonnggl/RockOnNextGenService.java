/*
 * Adapted from the Android stock Music app
 */

package org.abrantix.rockon.rockonnggl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OptionalDataException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.jar.Pack200.Unpacker;

import org.abrantix.rockon.rockonnggl.IRockOnNextGenService;
import org.abrantix.rockon.rockonnggl.R;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * Provides "background" audio playback capabilities, allowing the
 * user to switch between activities without stopping playback.
 */
public class RockOnNextGenService extends Service {

    private static final String TAG = "RockOnNextGenService";

    public static final int PLAYBACKSERVICE_STATUS = 1;

    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FADEIN = 4;
    private static final int MAX_HISTORY_SIZE = 100;
    
    private static boolean 	mLock = false;
    private static String	mScrobblerName = "none";
    
    private int mRockOnShuffleMode = Constants.SHUFFLE_NONE;
    private int mRockOnRepeatMode = Constants.REPEAT_NONE;
    private int mPlaylistId = Constants.PLAYLIST_UNKNOWN;
    
    private boolean mUseAnalytics = true;
    GoogleAnalyticsTracker	mAnalytics;
    
    private EqualizerWrapper	mEqualizerWrapper;
    
    private MultiPlayer mPlayer;
    private String mFileToPlay;
    private int mShuffleMode = Constants.SHUFFLE_NONE;
    private int mRepeatMode = Constants.REPEAT_NONE;
    private int mMediaMountedCount = 0;
    private long [] mAutoShuffleList = null;
    private boolean mOneShot; //??
    private long [] mPlayList = null;
    private int mPlayListLen = 0;
    private Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);
    private Cursor mCursor;
    private int mPlayPos = -1;
    private final Shuffler mRand = new Shuffler();
    private int mOpenFailedCounter = 0;
    String[] mCursorCols = new String[] {
            "audio._id AS _id",             // index must match IDCOLIDX below
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID
    };
    private final static int IDCOLIDX = 0;
    private BroadcastReceiver mUnmountReceiver = null;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    
    private SharedPreferences mPreferences;
    // We use this to distinguish between different cards when saving/restoring playlists.
    // This will have to change if we want to support multiple simultaneous cards.
    private int mCardId;
    
    private RockOnNextGenAppWidgetProvider mAppWidgetProvider = 
    	RockOnNextGenAppWidgetProvider.getInstance();
    private RockOnNextGenAppWidgetProvider3x3 mAppWidgetProvider3x3 = 
    	RockOnNextGenAppWidgetProvider3x3.getInstance();
    private RockOnNextGenAppWidgetProvider4x4 mAppWidgetProvider4x4 = 
    	RockOnNextGenAppWidgetProvider4x4.getInstance();
    private RockOnNextGenAppWidgetProvider4x1 mAppWidgetProvider4x1 = 
    	RockOnNextGenAppWidgetProvider4x1.getInstance();
    
    private ScreenOnIntentReceiver mScreenOnReceiver;
    
    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000; 

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0) {
                    mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (getAudioId() >= 0);
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (getAudioId() >= 0);
                pause();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // start playing again
                if (mResumeAfterCall) {
                    // resume playback only if music was playing
                    // when the call was answered
                    startAndFadeIn();
                    mResumeAfterCall = false;
                }
            }
        }
    };
    
    private void startAndFadeIn() {
        mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
    }
    
    private Handler mMediaplayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADEIN:
                    if (!isPlaying()) {
                        mCurrentVolume = 0f;
                        mPlayer.setVolume(mCurrentVolume);
                        play();
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume += 0.01f;
                        if (mCurrentVolume < 1.0f) {
                            mMediaplayerHandler.sendEmptyMessageDelayed(FADEIN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        mPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case SERVER_DIED:
                    if (mIsSupposedToBePlaying) {
                        next(true);
                    } else {
                        // the server died when we were idle, so just
                        // reopen the same song (it will start again
                        // from the beginning though when the user
                        // restarts)
                        openCurrent();
                    }
                    break;
                case TRACK_ENDED:
                    if (mRepeatMode == Constants.REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else if (!mOneShot) {
                        next(false);
                    } else {
                        notifyChange(Constants.PLAYBACK_COMPLETE);
                        mIsSupposedToBePlaying = false;
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            Log.i(TAG, cmd);
            if (Constants.CMDNEXT.equals(cmd) || Constants.NEXT_ACTION.equals(action)) {
                next(true);
            } else if (Constants.CMDPREVIOUS.equals(cmd) || Constants.PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (Constants.CMDTOGGLEPAUSE.equals(cmd) || Constants.TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
            } else if (Constants.CMDPAUSE.equals(cmd) || Constants.PAUSE_ACTION.equals(action)) {
                pause();
            } else if (Constants.CMDSTOP.equals(cmd)) {
                pause();
                seek(0);
            } else if (Constants.CMDSEEKFWD.equals(cmd)) {
            	long value = intent.getLongExtra(Constants.CMDSEEKAMOUNT, 0);
            	seek(position() + value); 	
            } else if (Constants.CMDSEEKBACK.equals(cmd)) {
            	long value = intent.getLongExtra(Constants.CMDSEEKAMOUNT, 0);
            	seek(position() - value); 	
            } else if (RockOnNextGenAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to refresh a set of specific widgets, probably
                // because they were just added.
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if(mAppWidgetProvider != null)
                	mAppWidgetProvider.performUpdate(RockOnNextGenService.this, appWidgetIds);
                if(mAppWidgetProvider3x3 != null)
                	mAppWidgetProvider3x3.performUpdate(RockOnNextGenService.this, appWidgetIds);
                if(mAppWidgetProvider4x4 != null)
                	mAppWidgetProvider4x4.performUpdate(RockOnNextGenService.this, appWidgetIds);
                if(mAppWidgetProvider4x1 != null)
                	mAppWidgetProvider4x1.performUpdate(RockOnNextGenService.this, appWidgetIds);  
            }
        }
    };

    public RockOnNextGenService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mUseAnalytics = getResources().getBoolean(R.bool.config_isMarketVersion);
        if (mUseAnalytics)
    	{
        	mAnalytics = GoogleAnalyticsTracker.getInstance();
	        mAnalytics.start("UA-20349033-2", 6*60*60 /* every 6 hours */, this);
	//        mAnalytics.start("UA-20349033-2", this);
    	}

        Log.i(TAG, "SERVICE onCreate");
        
//        mPreferences = getSharedPreferences("Music", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        mPreferences = getSharedPreferences("CubedMusic", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE); 
        //PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        
        mCardId = 0; // should be the serial number of the sd card
//        mCardId = FileUtils.getFatVolumeId(Environment.getExternalStorageDirectory().getPath());
        
        registerExternalStorageListener();
        registerScreenOnReceiver();

        // Needs to be done in this thread, since otherwise ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);
        
        // Reload Equalizer + Sound FX
        if(EqualizerWrapper.isSupported())
        	loadEqualizerWrapper();

        reloadQueue();
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(Constants.SERVICECMD);
        commandFilter.addAction(Constants.TOGGLEPAUSE_ACTION);
        commandFilter.addAction(Constants.PAUSE_ACTION);
        commandFilter.addAction(Constants.NEXT_ACTION);
        commandFilter.addAction(Constants.PREVIOUS_ACTION);
//        commandFilter.addAction(Constants.SHUFFLE_AUTO_ACTION);
//        commandFilter.addAction(Constants.SHUFFLE_NONE_ACTION);
//        commandFilter.addAction(Constants.REPEAT_CURRENT_ACTION);
//        commandFilter.addAction(Constants.REPEAT_NONE_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);
        
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }
    
    @Override
    public void onDestroy() {
        
        // Check that we're not being destroyed while something is still playing.
        if (isPlaying()) {
            Log.i(TAG, "Service being destroyed while still playing.");
        }
        // release all MediaPlayer resources, including the native player and wakelocks
        mPlayer.release();
        mPlayer = null;
        
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, 0);

        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        
        unregisterScreenOnReceiver();
       
        if(mAnalytics != null)
        	mAnalytics.stop();
        mWakeLock.release();
        super.onDestroy();
    }
    
    private void loadEqualizerWrapper() {
    	if(mEqualizerWrapper == null) {
    		EqSettings settings = readEqSettingsFromPreferences();
    		mEqualizerWrapper = new EqualizerWrapper(settings);
    	}
    	if(mPreferences.getBoolean(Constants.prefKey_mEqualizerEnabled, false)) {
			try {
				Method m = MediaPlayer.class.getMethod("getAudioSessionId", new Class[]{});
	    		mEqualizerWrapper.enable(0, (Integer) m.invoke(mPlayer.mMediaPlayer, new Object[]{}));
//	    		mEqualizerWrapper.enable(0, mPlayer.mMediaPlayer.getAudioSessionId());
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private EqSettings readEqSettingsFromPreferences() {
    	String eqSettingsString = mPreferences.getString(Constants.prefKey_mEqualizerSettings, null);
    	if(eqSettingsString != null) {
			try {
				return EqSettings.readFrom64String(eqSettingsString);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return new EqSettings();
    	}
		else
    		return new EqSettings();
    }
    
    private final char hexdigits [] = new char [] {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    private void saveQueue(boolean full) {
        if (mOneShot) {
            return;
        }
        Editor ed = mPreferences.edit();
        //long start = System.currentTimeMillis();
        if(mPlayList != null && mPlayList.length > 0)
        	ed.putLong("audioId", mPlayList[0]);
        if (full) {
            StringBuilder q = new StringBuilder();
            
            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            // (saving the full state takes about 40 ms under no-load conditions
            // on the phone)
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        int digit = (int)(n & 0xf);
                        n >>= 4;
                        q.append(hexdigits[digit]);
                    }
                    q.append(";");
                }
            }
            //Log.i("@@@@ service", "created queue string in " + (System.currentTimeMillis() - start) + " ms");
            ed.putString("queue", q.toString());
            ed.putInt("cardid", mCardId);
            if (mShuffleMode != Constants.SHUFFLE_NONE) {
                // In shuffle mode we need to save the history too
                len = mHistory.size();
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            int digit = (n & 0xf);
                            n >>= 4;
                            q.append(hexdigits[digit]);
                        }
                        q.append(";");
                    }
                }
                ed.putString("history", q.toString());
            }
        }
        ed.putInt("curpos", mPlayPos);
        if (mPlayer.isInitialized()) {
            ed.putLong("seekpos", mPlayer.position());
        }
        ed.putInt("repeatmode", mRepeatMode);
        ed.putInt("shufflemode", mShuffleMode);
//        ed.putInt(Constants.prefkey_mPlaylistId, mPlaylistId);
        ed.putInt("rockonrepeatmode", mRockOnRepeatMode);
        ed.putInt("rockonshufflemode", mRockOnShuffleMode);
        ed.commit();
  
        //Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void reloadQueue() {
        String q = null;
                
        // the playlist should always be restored! or should it?
        mPlaylistId = mPreferences.getInt(
        		Constants.prefkey_mPlaylistId, 
        		Constants.PLAYLIST_ALL);
        // sanity check
        if(mPlaylistId == Constants.PLAYLIST_UNKNOWN)
        	mPlaylistId = Constants.PLAYLIST_ALL;
                
        boolean newstyle = false;
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            newstyle = true;
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            // Only restore the saved playlist if the card is still
            // the same one as when the playlist was saved
            q = mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            //Log.i("@@@@ service", "loaded queue: " + q);
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    } else {
                        // bogus playlist data
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;

            int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                // The saved playlist is bogus, discard it
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;
            
            // When reloadQueue is called in response to a card-insertion,
            // we might not be able to query the media provider right away.
            // To deal with this, try querying for the current file, and if
            // that fails, wait a while and try again. If that too fails,
            // assume there is a problem and don't restore the state.
            Cursor crsr = null;
            if(DirectoryFilter.usesExternalStorage()) {
            	crsr = getContentResolver().query(
            		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
            		new String [] {"_id"},
            		"_id=" + mPlayList[mPlayPos], 
            		null, 
            		null); 
            } else {
            	crsr = getContentResolver().query(
                		MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
                		new String [] {"_id"},
                		"_id=" + mPlayList[mPlayPos], 
                		null, 
                		null);
            }
//            	MusicUtils.query(this,
//                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                        new String [] {"_id"}, "_id=" + mPlayList[mPlayPos] , null, null);
            if (crsr == null || crsr.getCount() == 0) {
                // wait a bit and try again
                SystemClock.sleep(3000);
                if(DirectoryFilter.usesExternalStorage())
                	crsr = getContentResolver().query(
                       MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mCursorCols, "_id=" + mPlayList[mPlayPos] , null, null);
                else
                	crsr = getContentResolver().query(
                            MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                             mCursorCols, "_id=" + mPlayList[mPlayPos] , null, null);
            }
            if (crsr != null) {
                crsr.close();
            }

            // Make sure we don't auto-skip to the next song, since that
            // also starts playback. What could happen in that case is:
            // - music is paused
            // - go to UMS and delete some files, including the currently playing one
            // - come back from UMS
            // (time passes)
            // - music app is killed for some reason (out of memory)
            // - music service is restarted, service restores state, doesn't find
            //   the "current" file, goes to the next and: playback starts on its
            //   own, potentially at some random inconvenient time.
            mOpenFailedCounter = 20;
            mQuietMode = true;
            openCurrent();
            mQuietMode = false;
            
            if (!mPlayer.isInitialized()) {
                // couldn't restore the saved state
                mPlayListLen = 0;
                return;
            }
            
            long seekpos = mPreferences.getLong("seekpos", 0);
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
            
            int repmode = mPreferences.getInt("repeatmode", Constants.REPEAT_NONE);
            if (repmode != Constants.REPEAT_ALL && repmode != Constants.REPEAT_CURRENT) {
                repmode = Constants.REPEAT_NONE;
            }
            mRepeatMode = repmode;

            // we passed this to the beginning of the method
//            mPlaylistId = mPreferences.getInt(
//            		Constants.prefkey_mPlaylistId, 
//            		Constants.PLAYLIST_ALL);
            
            int rockonrepmode = mPreferences.getInt("rockonrepeatmode", Constants.REPEAT_NONE);
            if (rockonrepmode != Constants.REPEAT_ALL && rockonrepmode != Constants.REPEAT_CURRENT) {
                rockonrepmode = Constants.REPEAT_NONE;
            }
            mRockOnRepeatMode = rockonrepmode;
            
            int shufmode = mPreferences.getInt("shufflemode", Constants.SHUFFLE_NONE);
            if (shufmode != Constants.SHUFFLE_AUTO && shufmode != Constants.SHUFFLE_NORMAL) {
                shufmode = Constants.SHUFFLE_NONE;
            }
            if (shufmode != Constants.SHUFFLE_NONE) {
                // in shuffle mode we need to restore the history too
                q = mPreferences.getString("history", "");
                qlen = q != null ? q.length() : 0;
                if (qlen > 1) {
                    plen = 0;
                    n = 0;
                    shift = 0;
                    mHistory.clear();
                    for (int i = 0; i < qlen; i++) {
                        char c = q.charAt(i);
                        if (c == ';') {
                            if (n >= mPlayListLen) {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            mHistory.add(n);
                            n = 0;
                            shift = 0;
                        } else {
                            if (c >= '0' && c <= '9') {
                                n += ((c - '0') << shift);
                            } else if (c >= 'a' && c <= 'f') {
                                n += ((10 + c - 'a') << shift);
                            } else {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            shift += 4;
                        }
                    }
                }
            }
            if (shufmode == Constants.SHUFFLE_AUTO) {
            	shufmode = Constants.SHUFFLE_NORMAL;
//                if (! makeAutoShuffleList()) {
//                    shufmode = Constants.SHUFFLE_NONE;
//                }
            }
            mShuffleMode = shufmode;
            
            int rockonshufmode = mPreferences.getInt("rockonshufflemode", Constants.SHUFFLE_NONE);
            if (rockonshufmode != Constants.SHUFFLE_AUTO && rockonshufmode != Constants.SHUFFLE_NORMAL) {
                rockonshufmode = Constants.SHUFFLE_NONE;
            }
            if (rockonshufmode != Constants.SHUFFLE_NONE) {
                // in shuffle mode we need to restore the history too
                q = mPreferences.getString("history", "");
                qlen = q != null ? q.length() : 0;
                if (qlen > 1) {
                    plen = 0;
                    n = 0;
                    shift = 0;
                    mHistory.clear();
                    for (int i = 0; i < qlen; i++) {
                        char c = q.charAt(i);
                        if (c == ';') {
                            if (n >= mPlayListLen) {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            mHistory.add(n);
                            n = 0;
                            shift = 0;
                        } else {
                            if (c >= '0' && c <= '9') {
                                n += ((c - '0') << shift);
                            } else if (c >= 'a' && c <= 'f') {
                                n += ((10 + c - 'a') << shift);
                            } else {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            shift += 4;
                        }
                    }
                }
            }
            if (rockonshufmode == Constants.SHUFFLE_AUTO) {
            	rockonshufmode = Constants.SHUFFLE_NORMAL;
//                if (! makeAutoShuffleList()) {
//                    rockonshufmode = Constants.SHUFFLE_NONE;
//                }
            }
            mRockOnShuffleMode = rockonshufmode;
        }
    }
    
    ArrayList<Double> mBindingTimes = new ArrayList<Double>();
    @Override
    public IBinder onBind(Intent intent) {
    	if(mAnalytics != null) {
	    	double start = System.currentTimeMillis();
	    	mBindingTimes.add(new Double(System.currentTimeMillis()));
//        	mAnalytics.trackPageView("/Bind");
            Log.i(TAG, "Time spent analysing: "+(System.currentTimeMillis()-start));
    	}
	    mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
    	if(mAnalytics != null) {
	    	double start = System.currentTimeMillis();
	    	mBindingTimes.add(new Double(System.currentTimeMillis()));
//	    	mAnalytics.trackPageView("/Rebind");
	    	Log.i(TAG, "Time spent analysing: "+(System.currentTimeMillis()-start));
    	}
    	mDelayedStopHandler.removeCallbacksAndMessages(null);
    	mServiceInUse = true;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	/**********************
    	 * ANALYTICS
    	 **********************/
    	if(mAnalytics != null) {
//	    	mAnalytics.trackPageView("/Unbind");
	    	if(mBindingTimes.size() > 0) {
		    	int duration = (int) (System.currentTimeMillis() - mBindingTimes.get(mBindingTimes.size()-1));
		    	mBindingTimes.remove(mBindingTimes.size()-1);
		    	mAnalytics.trackEvent("Duration", "Time bound", "From UI?", duration);
//		    	mAnalytics.dispatch();
	    	}
    	}
    	/**********************/
    	
        mServiceInUse = false;

        // Take a snapshot of the current playlist
        saveQueue(true);

        if (isPlaying() || mResumeAfterCall) {
            // something is currently playing, or will be playing once 
            // an in-progress call ends, so don't stop the service now.
            return true;
        }
        
        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
        if (mPlayListLen > 0  || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }
        
        // No active playlist, OK to stop the service right now
        setForeground(false);
        stopSelf(mServiceStartId);
        return true;
    }
    
    @Override
    public void onStart(Intent intent, int startId){
    	this.onStartCommand(intent, 0, startId);
    }
    
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        Log.i(TAG, "Receiving intent in service");
        
        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra(Constants.CMDNAME);

            Log.i(TAG, "Receiving start command: "+cmd);
            
            if (Constants.CMDNEXT.equals(cmd) || Constants.NEXT_ACTION.equals(action)) {
                next(true);
            } else if (Constants.CMDPREVIOUS.equals(cmd) || Constants.PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (Constants.CMDTOGGLEPAUSE.equals(cmd) || Constants.TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
            } else if (Constants.CMDPAUSE.equals(cmd) || Constants.PAUSE_ACTION.equals(action)) {
                pause();
            } else if (Constants.CMDSTOP.equals(cmd)) {
                pause();
                seek(0);
            } else if(Constants.SHUFFLE_NORMAL_ACTION.equals(action)){
            	this.setShuffleMode(Constants.SHUFFLE_NORMAL);
            } else if(Constants.SHUFFLE_NONE_ACTION.equals(action)){
            	this.setShuffleMode(Constants.SHUFFLE_NONE);
            } else if(Constants.REPEAT_CURRENT_ACTION.equals(action)){
            	this.setRepeatMode(Constants.REPEAT_CURRENT);
            } else if(Constants.REPEAT_NONE_ACTION.equals(action)){
            	this.setRepeatMode(Constants.REPEAT_NONE);
            }
        }
        
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        
        return Constants.START_STICKY;
    }
    
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || mResumeAfterCall || mServiceInUse
                    || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            saveQueue(true);
//            stopForegroundReflected();
            stopSelf(mServiceStartId);
        }
    };
    
    private void stopForegroundReflected() {
    	try{
        	Method m = RockOnNextGenService.class.getMethod(
        			"stopForeground", 
        			new Class[]{
        					boolean.class});
        	m.invoke(this, true);
        	//stopForeground(true);
        } catch(Exception e){
        	e.printStackTrace();
        	// XXX - deprecated
        	setForeground(false);
        }
    }
    
    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(Constants.QUEUE_CHANGED);
        notifyChange(Constants.META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
     * The intent will call closeExternalStorageFiles() if the external media
     * is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        mOneShot = true; // This makes us not save the state again later,
                                         // which would be wrong because the song ids and
                                         // card id might not match. 
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mMediaMountedCount++;
                        mCardId = 0;
//                        mCardId = FileUtils.getFatVolumeId(intent.getData().getPath());
                        reloadQueue();
                        notifyChange(Constants.QUEUE_CHANGED);
                        notifyChange(Constants.META_CHANGED);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.android.music.metachanged"
     * "com.android.music.queuechanged",
     * "com.android.music.playbackcomplete"
     * "com.android.music.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {
        
        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getArtistName());
        i.putExtra("album",getAlbumName());
        i.putExtra("track", getTrackName());
        sendBroadcast(i);
        
        if (what.equals(Constants.QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        
        // Share this notification directly with our widgets
        if(mAppWidgetProvider != null)
        	mAppWidgetProvider.notifyChange(this, what);
        if(mAppWidgetProvider3x3 != null)
        	mAppWidgetProvider3x3.notifyChange(this, what);
        if(mAppWidgetProvider4x4 != null)
        	mAppWidgetProvider4x4.notifyChange(this, what);
        if(mAppWidgetProvider4x1 != null)
        	mAppWidgetProvider4x1.notifyChange(this, what);
        
    }
    
    private void sendScrobbleBroadcast(int state) {
    	// check that state is a valid state
    	if (state != Constants.SCROBBLE_PLAYSTATE_START &&
    		state != Constants.SCROBBLE_PLAYSTATE_RESUME &&
    		state != Constants.SCROBBLE_PLAYSTATE_PAUSE &&
    		state != Constants.SCROBBLE_PLAYSTATE_COMPLETE) {
    		Log.e(TAG, "Trying to send scrobble with invalid state: " + state);
    		return;
    	}
    	
//    	String who = PreferenceManager.getDefaultSharedPreferences(this).getString(
//    			getString(R.string.preference_scrobble_list_key), 
//    			getString(R.string.preference_scrobble_value_dont));
    	
    	// check if scrobbling is enabled, and to whom we should send the broadcast
    	if (mScrobblerName.equals(getString(R.string.preference_scrobble_value_sls))) 
    	{
    		sendScrobbleBroadcastSLS(state);
    	} 
    	else if (mScrobblerName.equals(getString(R.string.preference_scrobble_value_sd))) 
    	{
    		sendScrobbleBroadcastSD(state);
    	}
    }
    
    private void sendScrobbleBroadcastSLS(int state) {
    	Log.d(TAG, "Sending scrobble broadcast to SLS");
    	Intent i = new Intent(Constants.SCROBBLE_SLS_API);
    	
    	i.putExtra("app-name", "RockOn NextGen"); // TODO: what is the name of this app?
    	i.putExtra("app-package", "org.abrantix.rockon.rockonnggl");
    	
    	i.putExtra("state", state);
    	
    	i.putExtra("artist", getArtistName());
        i.putExtra("album",getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("duration", (int)(duration()/1000));
        
        sendBroadcast(i);
    }
    
    private void sendScrobbleBroadcastSD(int state) {
    	Log.d(TAG, "Sending scrobble broadcast to SD");
    	Intent i = new Intent(Constants.SCROBBLE_SD_API);
    	
    	boolean playing = false;
    	if (state == Constants.SCROBBLE_PLAYSTATE_START ||
    		state == Constants.SCROBBLE_PLAYSTATE_RESUME) {
    		playing = true;
    	}
    	i.putExtra("playing", playing);
    	
    	i.putExtra("id", (int)getAudioId());
    	
    	sendBroadcast(i);
    }

    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            long [] newlist = new long[size * 2];
            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = mPlayList[i];
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }
    
//    // add to the current point of the playing list
//    private void insertToPlayList(long[] list, int position){
//    	int addlen = list.length;
//    }
    
    // insert the list of songs at the specified position in the playlist
    private void addToPlayList(long [] list, int position) 
    {
//    	Log.i(TAG, "===============================");
//    	Log.i(TAG, "list.length: "+list.length+" position: "+position);
//    	Log.i(TAG, "mPlaylistLen: "+mPlayListLen+" mPlaylist.length: "+mPlayList.length);
        int addlen = list.length;
        if (position < 0) { // overwrite
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }
        
        // move part of list after insertion point
        int tailsize = mPlayListLen - position;
        for (int i = tailsize ; i > 0 ; i--) {
            mPlayList[position + i + addlen] = mPlayList[position + i]; 
            // TODO: need to update history??????????
        }
        
        // copy list into playlist
        for (int i = 0; i < addlen; i++) {
            mPlayList[position + i] = list[i];
        }
        mPlayListLen += addlen;
    }
    
    /**
     * Appends a list of tracks to the current playlist.
     * If nothing is playing currently, playback will be started at
     * the first track.
     * If the action is NOW, playback will switch to the first of
     * the new tracks immediately.
     * @param list The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueue(long [] list, int action) {
        synchronized(this) {
        	Log.i(TAG, "yep, action: "+action);

            if (action == Constants.NEXT && mPlayPos + 1 < mPlayListLen) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(Constants.QUEUE_CHANGED);
            } else {
                // action == LAST || action == NOW || mPlayPos + 1 == mPlayListLen
                if(action == Constants.NOW)
                	addToPlayList(list, mPlayPos + 1);
                else if(action == Constants.LAST)
                	addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(Constants.QUEUE_CHANGED);
                if (action == Constants.NOW) {
//                	mPlayPos = mPlayListLen - list.length;
                	Log.i(TAG, "yep, opening: "+mPlayPos);
                	mPlayPos = mPlayPos + 1;
                    openCurrent();
                    play();
                    notifyChange(Constants.META_CHANGED);
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrent();
                play();
                notifyChange(Constants.META_CHANGED);
            }
        }
    }

    /**
     * Replaces the current playlist with a new list,
     * and prepares for starting playback at the specified
     * position in the list, or a random position if the
     * specified position is 0.
     * @param list The new list of tracks.
     */
    public void open(long [] list, int position) {
        synchronized (this) {
            if (mShuffleMode == Constants.SHUFFLE_AUTO) {
                mShuffleMode = Constants.SHUFFLE_NORMAL;
            }
            long oldId = getAudioId();
            int listlength = list.length;
            boolean newlist = true;
            if (mPlayListLen == listlength) {
                // possible fast path: list might be the same
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlayList[i]) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(Constants.QUEUE_CHANGED);
            }
            int oldpos = mPlayPos;
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = mRand.nextInt(mPlayListLen);
            }
            mHistory.clear();

            saveBookmarkIfNeeded();
            openCurrent();
            if (oldId != getAudioId()) {
                notifyChange(Constants.META_CHANGED);
            }
        }
    }
    
    /**
     * Moves the item at index1 to index2.
     * @param index1
     * @param index2
     */
    public void moveQueueItem(int index1, int index2) {
        synchronized (this) {
            if (index1 >= mPlayListLen) {
                index1 = mPlayListLen - 1;
            }
            if (index2 >= mPlayListLen) {
                index2 = mPlayListLen - 1;
            }
            if (index1 < index2) {
                long tmp = mPlayList[index1];
                for (int i = index1; i < index2; i++) {
                    mPlayList[i] = mPlayList[i+1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index1 && mPlayPos <= index2) {
                        mPlayPos--;
                }
            } else if (index2 < index1) {
                long tmp = mPlayList[index1];
                for (int i = index1; i > index2; i--) {
                    mPlayList[i] = mPlayList[i-1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index2 && mPlayPos <= index1) {
                        mPlayPos++;
                }
            }
            notifyChange(Constants.QUEUE_CHANGED);
        }
    }

    /**
     * Returns the current play list
     * @return An array of integers containing the IDs of the tracks in the play list
     */
    public long [] getQueue() {
        synchronized (this) {
            int len = mPlayListLen;
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mPlayList[i];
            }
            return list;
        }
    }
    
    public long [] getOutstandingQueue(){
    	synchronized (this) {
    		long [] list;
    		if(mPlayListLen <= 0){
    			list = null;
//				Log.i(TAG, "list.length: "+list.length+" mPlayPos: "+mPlayPos+" mPlayListLen: "+mPlayListLen);
    		} else if(mShuffleMode == Constants.SHUFFLE_NONE){
    			list = new long[mPlayListLen - mPlayPos + 1];
    			for(int i = mPlayPos; i<mPlayListLen; i++){
    				list[i-mPlayPos] = mPlayList[i];
    			}
    		} else {
    			// check which elements on the playlist have not been played yet
    			int numUnplayed = mPlayListLen;
    			long[] tmplist = new long[mPlayListLen];
    			for(int i=0; i<mPlayListLen; i++){
    				tmplist[i] = mPlayList[i];
    				if(mPlayList[i]<0)
    					numUnplayed--;
    			}
    			for(int i=0; i<mHistory.size(); i++){
    				if(mHistory.get(i).intValue() < tmplist.length)
    				{
	    				if(tmplist[mHistory.get(i).intValue()] >= 0){
	    					// always list the currently playing song
	    					if(mHistory.get(i).intValue() != mPlayPos){ 
		    					tmplist[mHistory.get(i).intValue()] = -1;
		    					numUnplayed--;
	    					}
	    				}
    				}
    			}
    			// pass the unplayed item to our result list
    			list = new long[numUnplayed];
    			// put the currently playing song on top
    			list[0] = mPlayList[mPlayPos];
    			int cnt = 1;
    			for(int i=0; i<mPlayListLen; i++){
    				if(tmplist[i]>=0 && i != mPlayPos){ // currently playing song already added
    					Log.i(TAG, "numUnPlayed: "+numUnplayed + "cnt: "+cnt);
    					list[cnt] = tmplist[i];
    					cnt++;
    				}
    			}
    		}
    		return list;
		}
    }

    private void openCurrent() {
        synchronized (this) {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }
            if (mPlayListLen == 0) {
                return;
            }
            stop(false);

            String id = String.valueOf(mPlayList[mPlayPos]);
            
            boolean external = DirectoryFilter.usesExternalStorage();
            if(external)
            	mCursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mCursorCols, "_id=" + id , null, null);
            else
            	mCursor = getContentResolver().query(
                        MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                        mCursorCols, "_id=" + id , null, null);
            if (mCursor != null && mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                if(external)
                	open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id, false);
                else
                	open(MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "/" + id, false);
                // go to bookmark if needed
//                if (isPodcast()) {
//                    long bookmark = getBookmark();
//                    // Start playing a little bit before the bookmark,
//                    // so it's easier to get back in to the narrative.
//                    seek(bookmark - 5000);
//                }
            }
        }
    }

    public void openAsync(String path) {
        synchronized (this) {
            if (path == null) {
                return;
            }
            
            mRepeatMode = Constants.REPEAT_NONE;
            ensurePlayListCapacity(1);
            mPlayListLen = 1;
            mPlayPos = -1;
            
            mFileToPlay = path;
            mCursor = null;
            mPlayer.setDataSourceAsync(mFileToPlay);
            mOneShot = true;
        }
    }
    
    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     * @param oneshot when set to true, playback will stop after this file completes, instead
     * of moving on to the next track in the list 
     */
    public void open(String path, boolean oneshot) {
        synchronized (this) {
            if (path == null) {
                return;
            }
            
            if (oneshot) {
                mRepeatMode = Constants.REPEAT_NONE;
                ensurePlayListCapacity(1);
                mPlayListLen = 1;
                mPlayPos = -1;
            }
            
            // if mCursor is null, try to associate path with a database cursor
            if (mCursor == null) {

                ContentResolver resolver = getContentResolver();
                Uri uri;
                String where;
                String selectionArgs[];
                if (path.startsWith("content://media/")) {
                    uri = Uri.parse(path);
                    where = null;
                    selectionArgs = null;
                } else {
                   uri = MediaStore.Audio.Media.getContentUriForPath(path);
                   where = MediaStore.Audio.Media.DATA + "=?";
                   selectionArgs = new String[] { path };
                }
                
                try {
                    mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
                    if  (mCursor != null) {
                        if (mCursor.getCount() == 0) {
                            mCursor.close();
                            mCursor = null;
                        } else {
                            mCursor.moveToNext();
                            ensurePlayListCapacity(1);
                            mPlayListLen = 1;
                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
                            mPlayPos = 0;
                        }
                    }
                } catch (UnsupportedOperationException ex) {
                }
            }
            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            mOneShot = oneshot;
            if (! mPlayer.isInitialized()) {
                stop(true);
                if (mOpenFailedCounter++ < 10 &&  mPlayListLen > 1) {
                    // beware: this ends up being recursive because next() calls open() again.
                    next(false);
                }
                if (! mPlayer.isInitialized() && mOpenFailedCounter != 0) {
                    // need to make sure we only shows this once
                    mOpenFailedCounter = 0;
                    if (!mQuietMode) {
//                        Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                mOpenFailedCounter = 0;
            }
        }
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        if (mPlayer.isInitialized()) {
            // if we are at the end of the song, go to the next song first
            long duration = mPlayer.duration();
            if (mRepeatMode != Constants.REPEAT_CURRENT && 
            	duration > 2000 &&
            	mPlayer.position() >= duration - 2000) {
                next(true);
            }

            mPlayer.start();

            RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
            views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
            if (getAudioId() < 0) {
                // streaming
                views.setTextViewText(R.id.trackname, getPath());
                views.setTextViewText(R.id.artistalbum, null);
            } else {
                String artist = getArtistName();
                views.setTextViewText(R.id.trackname, getTrackName());
//                if (artist == null || artist.equals(MediaFile.UNKNOWN_STRING)) {
               	if (artist == null) {
                    artist = getString(R.string.unknown_artist_name);
                }
                String album = getAlbumName();
//                if (album == null || album.equals(MediaFile.UNKNOWN_STRING)) {
                if (album == null) {
                    album = getString(R.string.unknown_album_name);
                }
                views.setTextViewText(
                		R.id.artistalbum, 
                		artist+"\n"+album);
//                views.setTextViewText(R.id.artistalbum,
//                        getString(R.string.notification_artist_album, artist, album)
//                        );
            }
            
            Notification status = new Notification();
            status.contentView = views;
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
            status.contentIntent = 
            	PendingIntent.getActivity(
            		this, 
            		0,
                    new Intent(getString(Constants.MAIN_ACTIVITY_INTENT)),
                    0);
//            		new Intent("com.android.music.PLAYBACK_VIEWER"), 0);
            
            startForegroundReflected(PLAYBACKSERVICE_STATUS, status);
//            try{
//            	RockOnNextGenService.class.getMethod(
//            			"startForeground", 
//            			new Class[]{
//            					int.class, 
//            					Notification.class});
//              startForeground(PLAYBACKSERVICE_STATUS, status);
//            } catch(Exception e){
//            	e.printStackTrace();
//            	// XXX - deprecated
//            	setForeground(true);
//            	NotificationManager notificationManager = (NotificationManager) 
//            		getSystemService(Context.NOTIFICATION_SERVICE);
//            	notificationManager.notify(Constants.PLAY_NOTIFICATION_ID, status);	
//            }
            if (!mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = true;
                notifyChange(Constants.PLAYSTATE_CHANGED);
                // it's difficult to say if we
                sendScrobbleBroadcast(Constants.SCROBBLE_PLAYSTATE_RESUME);
            }

        } 
//        else if (mPlayListLen <= 0) {
//            // This is mostly so that if you press 'play' on a bluetooth headset
//            // without every having played anything before, it will still play
//            // something.
//            setShuffleMode(Constants.SHUFFLE_AUTO);
//        }
    }
    
    private void startForegroundReflected(int id, Notification notification) {
    	try{
        	Method m = RockOnNextGenService.class.getMethod(
        			"startForeground", 
        			new Class[]{
        					int.class, 
        					Notification.class});
        	m.invoke(this, id, notification);
        } catch(Exception e){
        	e.printStackTrace();
        	// XXX - deprecated
        	setForeground(true);
        	NotificationManager notificationManager = (NotificationManager) 
        		getSystemService(Context.NOTIFICATION_SERVICE);
        	notificationManager.notify(Constants.PLAY_NOTIFICATION_ID, notification);	
        }
    }
    
    private void stop(boolean remove_status_icon) {
        if (mPlayer.isInitialized()) {
            mPlayer.stop();
        }
        mFileToPlay = null;
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (remove_status_icon) {
            gotoIdleState();
        } else {
//            stopForeground(false);
        }
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
        }
        
        sendScrobbleBroadcast(Constants.SCROBBLE_PLAYSTATE_COMPLETE);
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized(this) {
            if (isPlaying()) {
                mPlayer.pause();
                gotoIdleState();
                mIsSupposedToBePlaying = false;
                notifyChange(Constants.PLAYSTATE_CHANGED);
                sendScrobbleBroadcast(Constants.SCROBBLE_PLAYSTATE_PAUSE);
                saveBookmarkIfNeeded();
            }
        }
    }

    /** Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /*
      Desired behavior for prev/next/shuffle:

      - NEXT will move to the next track in the list when not shuffling, and to
        a track randomly picked from the not-yet-played tracks when shuffling.
        If all tracks have already been played, pick from the full set, but
        avoid picking the previously played track if possible.
      - when shuffling, PREV will go to the previously played track. Hitting PREV
        again will go to the track played before that, etc. When the start of the
        history has been reached, PREV is a no-op.
        When not shuffling, PREV will go to the sequentially previous track (the
        difference with the shuffle-case is mainly that when not shuffling, the
        user can back up to tracks that are not in the history).

        Example:
        When playing an album with 10 tracks from the start, and enabling shuffle
        while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
        the final play order might be 1-2-3-4-5-8-10-6-9-7.
        When hitting 'prev' 8 times while playing track 7 in this example, the
        user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next',
        a random track will be picked again. If at any time user disables shuffling
        the next/previous track will be picked in sequential order again.
     */

    public void prev() {
        synchronized (this) {
            if (mOneShot || mRepeatMode == Constants.REPEAT_CURRENT) {
                // we were playing a specific file not part of a playlist, so there is no 'previous'
                seek(0);
                play();
                return;
            }
//            if (mShuffleMode == Constants.SHUFFLE_NORMAL) {
        	if (mShuffleMode == Constants.SHUFFLE_NORMAL || mShuffleMode == Constants.SHUFFLE_AUTO) {
                 // go to previously-played track and remove it from the history
                int histsize = mHistory.size();
                if (histsize == 0) {
                    // go get some random song?
//                	Log.i(TAG, "History size is zero: "+histsize);
                    return;
                } 
                else
                {	
	                Integer pos = mHistory.remove(histsize - 1);
	                mPlayPos = pos.intValue();
                }
//                Log.i(TAG, "PREVIOUS - playPos: "+mPlayPos);
            } else { // not  shuffling
            	if (mPlayPos > 0) 
            	{
                    mPlayPos--;
                } 
            	else // mPlayPos = 0 
            	{
                    CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
            		long audioId = cursorUtils.getNextPrevAudioId(
            				Constants.FIND_PREV,
            				this.getAudioId(), 
            				(int) this.getAlbumId(), 
            				mShuffleMode, 
            				mPlaylistId);
            		// shift the playlist 1 spot up
            		ensurePlayListCapacity(mPlayListLen+2);
            		for(int i = mPlayListLen-1; i >= 0;i--){
//            			Log.i(TAG, "PLLen: "+mPlayListLen+" i: "+i+" i+1: "+(i+1));
            			mPlayList[i+1] = mPlayList[i];
            		}
            		// add this to history? - or the one that is playing?
            		// -- NO
            		// set the new song to play at pos=0 and increase list size
            		mPlayListLen++;
            		mPlayList[0] = audioId;
            		mPlayPos = 0;
//                    mPlayPos = mPlayListLen - 1;
                }
//                Log.i(TAG, "PREV|| pos: "+mPlayPos+" len: "+mPlayListLen);
            }
        	/* FIXME: bug report driven */
        	if(mPlayPos < 0 || mPlayPos >= mPlayListLen)
        	{
        		Log.i(TAG, 
        				"XXX - HACK FIX -" +
        				" mPlayPos: "+mPlayPos+
        				" mPlayList.length: "+mPlayList.length+
        				" mPlayListLen: "+mPlayListLen);
        		return;
        	}
        		
            saveBookmarkIfNeeded();
            stop(false);
            openCurrent();
            // the START scrobble broadcast need to be sent before play() is called
            // as play() sends a RESUME state.
            sendScrobbleBroadcast(Constants.SCROBBLE_PLAYSTATE_START);
            play();
            notifyChange(Constants.META_CHANGED);
        }
    }

    public void next(boolean force) {
        synchronized (this) {
            if (mOneShot || mRepeatMode == Constants.REPEAT_CURRENT) {
                // we were playing a specific file not part of a playlist, so there is no 'next'
                seek(0);
                play();
                return;
            }

            if (mPlayListLen <= 0) {
                return;
            }

            // Store the current file in the history, but keep the history at a
            // reasonable size
            if (mPlayPos >= 0) {
                mHistory.add(Integer.valueOf(mPlayPos));
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.removeElementAt(0);
            }

            if (mShuffleMode == Constants.SHUFFLE_NORMAL) {
                // Pick random next track from the not-yet-played ones
                // TODO: make it work right after adding/removing items in the queue.
            	Log.i(TAG, "next - SHUFFLE_NORMAL - PlayListLen: "+mPlayListLen);
                int numTracks = mPlayListLen;
                int[] tracks = new int[numTracks+1];
                for (int i=0;i<numTracks; i++) {
                    tracks[i] = i;
                }
                // small rockon hack addon
                tracks[numTracks] = -1;
                //

                int numHistory = mHistory.size();
                int numUnplayed = numTracks;
                for (int i=0;i < numHistory; i++) {
                    int idx = mHistory.get(i).intValue();
//                    Log.i(TAG, "Checking history element: "+i+" - "+idx+" - track: "+tracks[idx]);
                    if (idx < numTracks && tracks[idx] >= 0) {
//                    	Log.i(TAG, " -- played already: "+numTracks);
                	    numUnplayed--;
                        tracks[idx] = -1;
                    }
                }

                // 'numUnplayed' now indicates how many tracks have not yet
                // been played, and 'tracks' contains the indices of those
                // tracks.
                if (numUnplayed <=0) {
                	if(mRepeatMode == Constants.REPEAT_ALL) {
	                	mHistory.clear();
	                	numUnplayed = numTracks;
	                	for(int i=0; i<numTracks; i++)
	                		tracks[i] = i;
                	} else if(mRepeatMode == Constants.REPEAT_NONE) {
	                	Log.i(TAG, "SHUFFLE NEXT - all songs in queue have been played!");
//	                    // everything's already been played
//	//                	if (mRepeatMode == Constants.REPEAT_ALL || force) {
//	                	if (mRepeatMode == Constants.REPEAT_ALL) {
//	                        //pick from full set
//	                        numUnplayed = numTracks;
//	                        for (int i=0;i < numTracks; i++) {
//	                            tracks[i] = i;
//	                        }
//	                    } else {
	                    	CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
	                    	long audioId = cursorUtils.getNextPrevAudioId(
	                    			Constants.FIND_NEXT, 
	                    			getAudioId(), 
	                    			(int) getAlbumId(), 
	                    			mShuffleMode, 
	                    			mPlaylistId);
	//                    	Log.i(TAG, "CURRENT audioId: "+ getAudioId() + "NEXT audioId: "+audioId);
	                    	
	                    	// dont grow the playlist size indefinetly
	                    	if(mPlayListLen<Constants.REASONABLE_PLAY_QUEUE_SIZE){
	                    		Log.i(TAG, "List is less than max size, adding one item to the end...");
	                    		mPlayListLen++;
	                    		ensurePlayListCapacity(mPlayListLen);
	                    		mPlayList[mPlayListLen-1] = audioId;
	                        	tracks[numTracks] = mPlayListLen-1;
	                    	} else {
	                        	Log.i(TAG, "List already exceeds the resonable size!");
	                    		// FIXME: this may occasionally give BOGUS result
	                    		// remove the farthest element in the playlist
	                    		for(int i=0; i<mPlayListLen-1; i++)
	                    			mPlayList[i] = mPlayList[i+1];
	                    		mPlayList[mPlayListLen-1] = audioId;
	                    		// add the new song
	                        	tracks[numTracks-1] = mPlayListLen-1;
	                    		// updateHistory
	                    		while(mHistory.removeElement(Integer.valueOf(0))){
	//                    			Log.i(TAG, "removing history element pointing to index zero!");
	                    		}
	                    		for(int i=0; i<mHistory.size(); i++)
	                    			mHistory.set(i, Integer.valueOf(mHistory.get(i).intValue()-1));
	                    	}
	                    	
	                    	numUnplayed = 1;
	                		
	//                       // all done
	//                        gotoIdleState();
	//                        return;
//	                    }
                	}
                }
                int skip = mRand.nextInt(numUnplayed);
                int cnt = -1;
                while (true) {
                    while (tracks[++cnt] < 0)
                        ;
                    skip--;
                    if (skip < 0) {
                        break;
                    }
                }
                mPlayPos = cnt;
            } else if (mShuffleMode == Constants.SHUFFLE_AUTO) {
            	Log.i(TAG, "next - SHUFFLE_AUTO");
            	doAutoShuffleUpdate();
                mPlayPos++;
            } else { // No shuffle
                if (mPlayPos >= mPlayListLen - 1) {
                	// we're at the end of the list
//                	if (mRepeatMode == Constants.REPEAT_NONE && !force) {
                	if (mRepeatMode == Constants.REPEAT_NONE) {
                       	// continuous play not shuffle
//                    	if(true){
                    	CursorUtils cursorUtils = new CursorUtils(getApplicationContext());
                    	long audioId = cursorUtils.getNextPrevAudioId(
                    				Constants.FIND_NEXT,
                    				this.getAudioId(), 
                    				(int) this.getAlbumId(), 
                    				mShuffleMode, 
                    				mPlaylistId);

                    		if(mPlayListLen >= Constants.REASONABLE_PLAY_QUEUE_SIZE){
                    			// shift play queue by one position down
                    			for(int i=0; i<mPlayListLen-1; i++)
                    				mPlayList[i] = mPlayList[i+1];
                    			// update history indexes
                    			for(int i=0; i<mHistory.size(); i++){
                    				mHistory.set(i, mHistory.get(i)-1);
                    				if(mHistory.get(i)<0)
                    					mHistory.remove(i);
                    			}	
                    			// set id in current position
                    			mPlayList[mPlayPos] = audioId; // FIXME: replaces the current song -- which is removed from the queue;
                    		} else {
                    			// lets add the id to the current queue
//                    			if(mPlayPos <= mPlayListLen )
//                    			{
                    				/** XXX - bug report fix */
                    				if(mPlayPos >= mPlayListLen)
                    					mPlayPos = mPlayListLen-1;
                    				/** XXX */
	                    			ensurePlayListCapacity(mPlayListLen+1);
	                    			mPlayList[mPlayPos+1] = audioId;
	                    			mPlayListLen++;
	                    			mPlayPos++;
//                    			}
//                    			else
//                    			{
//                    				Log.i(TAG, "XXX - BugReport fix on service next()");
//	                    			ensurePlayListCapacity(mPlayPos+1);
//	                    			mPlayList[mPlayPos+1] = audioId;
//	                    			mPlayPos++;
//	                    			mPlayListLen = mPlayPos;
//                    			}
                    		}
//                    		long[] list = {audioId};
//                    		addToPlayList(list, );
//                    	} 
//                    	else { // stock app code - does not play continuously
//	                        // all done
//	                        gotoIdleState();
//	                        notifyChange(Constants.PLAYBACK_COMPLETE);
//	                        mIsSupposedToBePlaying = false;
//	                        return;
//                    	}
                    } else if (mRepeatMode == Constants.REPEAT_ALL || force) {
                    	Log.i(TAG, "next - SHUFFLE_NONE - REPEAT_ALL");
                        mPlayPos = 0;
                    }
                } else {
                	Log.i(TAG, "next - not the last position of the queue");
                    mPlayPos++;
                }
            }
            saveBookmarkIfNeeded();
            stop(false);
            openCurrent();
            // the START scrobble broadcast need to be sent before play() is called
            // as play() sends a RESUME state.
            sendScrobbleBroadcast(Constants.SCROBBLE_PLAYSTATE_START);
            play();
            notifyChange(Constants.META_CHANGED);
        }
    }
    
    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        NotificationManager notificationManager = (NotificationManager)
        	getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.PLAY_NOTIFICATION_ID);
        stopForegroundReflected();
//        stopForeground(true);
    }
    
    private void saveBookmarkIfNeeded() {
//        try {
//            if (isPodcast()) {
//                long pos = position();
//                long bookmark = getBookmark();
//                long duration = duration();
//                if ((pos < bookmark && (pos + 10000) > bookmark) ||
//                        (pos > bookmark && (pos - 10000) < bookmark)) {
//                    // The existing bookmark is close to the current
//                    // position, so don't update it.
//                    return;
//                }
//                if (pos < 15000 || (pos + 10000) > duration) {
//                    // if we're near the start or end, clear the bookmark
//                    pos = 0;
//                }
//                
//                // write 'pos' to the bookmark field
//                ContentValues values = new ContentValues();
//                values.put(MediaStore.Audio.Media.BOOKMARK, pos);
//                Uri uri = ContentUris.withAppendedId(
//                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCursor.getLong(IDCOLIDX));
//                getContentResolver().update(uri, values, null, null);
//            }
//        } catch (SQLiteException ex) {
//        }
    }

    // Make sure there are at least 5 items after the currently playing item
    // and no more than 10 items before.
    private void doAutoShuffleUpdate() {
        boolean notify = false;
        // remove old entries
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        // add new entries if needed
        int to_add = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < to_add; i++) {
            // pick something at random from the list
            int idx = mRand.nextInt(mAutoShuffleList.length);
            long which = mAutoShuffleList[idx];
            ensurePlayListCapacity(mPlayListLen + 1);
            mPlayList[mPlayListLen++] = which;
            notify = true;
        }
        if (notify) {
            notifyChange(Constants.QUEUE_CHANGED);
        }
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;
        private Random mRandom = new Random();
        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    };

    private boolean makeAutoShuffleList() {
        ContentResolver res = getContentResolver();
        Cursor c = null;
        try {
        	if(DirectoryFilter.usesExternalStorage())
        		c = res.query(
            		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Media._ID}, 
                    MediaStore.Audio.Media.IS_MUSIC + "=1",
                    null,
                    null);
        	else
            	c = res.query(
                		MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                        new String[] {MediaStore.Audio.Media._ID}, 
                        MediaStore.Audio.Media.IS_MUSIC + "=1",
                        null,
                        null);
        	if (c == null || c.getCount() == 0) {
            	return false;
            }
            int len = c.getCount();
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (RuntimeException ex) {
        	ex.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }
    
    /**
     * Removes the range of tracks specified from the play list. If a file within the range is
     * the file currently being played, playback will move to the next file after the
     * range. 
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(int first, int last) {
        int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(Constants.QUEUE_CHANGED);
        }
        return numremoved;
    }
    
    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) return 0;
            if (first < 0) first = 0;
            if (last >= mPlayListLen) last = mPlayListLen - 1;

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= (last - first + 1);
            }
            
            /** 
        	 * ROCKONNGGL Change
        	 * Remove the tracks being removed from history 
        	 */
            for(int i = first; i<= last; i++)
            {
            	mHistory.removeElement(Integer.valueOf(i));
            }
            /** END */
            
            /**
             * move back the remaining of the queue that is not being deleted
             */
            int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) 
            {
            	/** 
            	 * ROCKONNGGL Change
            	 * Remove (and update) also these tracks from history 
            	 */
            	mHistory.removeElement(Integer.valueOf((int)(first + i)));
            	try
            	{
            		mHistory.set(
            			mHistory.indexOf(Integer.valueOf(last + 1 + i)), 
            			Integer.valueOf(first+i));
            	}
            	catch(ArrayIndexOutOfBoundsException e)
            	{
//            		e.printStackTrace();
            	}
            	/** END */
            	
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            mPlayListLen -= last - first + 1;
            
            if (gotonext) {
                if (mPlayListLen == 0) {
                    stop(true);
                    mPlayPos = -1;
                } else {
                    if (mPlayPos >= mPlayListLen) {
                        mPlayPos = 0;
                    }
                    boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrent();
                    if (wasPlaying) {
                        play();
                    }
                }
            }
            return last - first + 1;
        }
    }
    
    /**
     * Removes all instances of the track with the given id
     * from the playlist.
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlayListLen; i++) {
                if (mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(Constants.QUEUE_CHANGED);
        }
        return numremoved;
    }
    
    public void setShuffleMode(int shufflemode) {
        synchronized(this) {
            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mShuffleMode = shufflemode;
            if (mShuffleMode == Constants.SHUFFLE_AUTO) {
            	// RockOn does not use SHUFFLE_AUTO mode
            	mShuffleMode = Constants.SHUFFLE_NORMAL;
//                if (makeAutoShuffleList()) {
//                    mPlayListLen = 0;
//                    doAutoShuffleUpdate();
//                    mPlayPos = 0;
//                    openCurrent();
//                    play();
//                    notifyChange(Constants.META_CHANGED);
//                    return;
//                } else {
//                    // failed to build a list of files to shuffle
//                    mShuffleMode = Constants.SHUFFLE_NONE;
//                }
            }
            // let our widgets refresh
            if(mShuffleMode == Constants.SHUFFLE_NONE)
            	notifyChange(Constants.PLAYMODE_CHANGED);
            else
            	notifyChange(Constants.PLAYMODE_CHANGED);
            // _IF_ switching back to shuffle none
            // - remove all items in the playlist that
            //	1. have already been played (are in the history)
            //  2. are positioned after the current position
            //
            // _IF_ switching back to shuffle normal
            // - remove all items in the playlist that
            //	1. ??have not been played (are not in the history)?? - doesnt matter if clearing history
            //	2. and are placed _before_ the current position
            
            // - remember to keep the first item in place
            // - clear history? YES
//            trimPlayQueue(mShuffleMode);
            
            saveQueue(false);
        }
    }
    
    /**
     * Unused stuff
     * @param shuffleMode
     */
//    private void trimPlayQueue(int shuffleMode){
//    	synchronized(this) {
//    		/**
//    		 * Trim the play list only if it contains songs
//    		 */
//    		if(mPlayListLen > 0 && mPlayPos < mPlayListLen)
//    		{
//	    		if(shuffleMode == Constants.SHUFFLE_NORMAL){
//	    			// FIXME: this may be wrong
//	        		long[] newlist = new long[mPlayListLen-mPlayPos];
//	        		for(int i=0; i<mPlayListLen-mPlayPos; i++){
//	        			Log.i(TAG, "i: "+i+" oldListLen: "+mPlayListLen+" newListLen: "+(mPlayListLen-mPlayPos));
//	        			newlist[i] = mPlayList[i+mPlayPos];
//	        			
//	        		}
//	        		mPlayList = newlist;
//	        		mPlayListLen = mPlayListLen-mPlayPos;
//	        		mPlayPos = 0;
//	            	mHistory.clear();	
//	        	} else if(shuffleMode == Constants.SHUFFLE_NONE){
//	        		// TODO: test extensively...
//	        		// go through the whole playlist and get all items that are not in history
//	        		long[] unplayedItems = getUnplayedItems();
//	        		// put the current item in the first position
//	        		for(int i=0; i<unplayedItems.length; i++){
//	        			if(unplayedItems[i] == mPlayList[mPlayPos]){
//	        				unplayedItems[i] = unplayedItems[0];
//	        				unplayedItems[0] = mPlayList[mPlayPos];
//	        				break;
//	        			}
//	        		}
//	        		// update the playlist, its length and current position
//	    			Log.i(TAG, "oldListLen: "+mPlayListLen+" newListLen: "+unplayedItems.length);
//	        		mPlayList = unplayedItems;
//	        		mPlayListLen = unplayedItems.length;
//	        		mPlayPos = 0;
//	        		mHistory.clear();
//	        	}
//			}
//    	}
//    }
    
    private long[] getUnplayedItems(){
    	int unplayedCount = mPlayListLen;
    	long[] tmplist = new long[mPlayListLen];
    	for(int i=0; i<mPlayListLen; i++){
    		tmplist[i] = mPlayList[i];
    		// check if this item has been played
    		for(int j=0; j<mHistory.size(); j++){
    			if(mHistory.get(j).intValue() == i){
    				unplayedCount--;
    				tmplist[i] = -1;
    				break;
    			}
    		}
    	}
//    	if(unplayedCount<=0){
//    		long[] unplayedList = new long[1];
//    		unplayedList[0] = mPlayList[mPlayPos];
//    		return unplayedList;
//    	}else{
	    	long[] unplayedList = new long[unplayedCount];
	    	int cnt = 0;
	    	for(int i=0; i<mPlayListLen; i++){
	    		if(tmplist[i]>=0){
	    			Log.i(TAG, "Adding unplayed song n"+cnt+": "+tmplist[i]+" from index: "+i+"|| "+unplayedCount);
	    			unplayedList[cnt] = tmplist[i];
	    			cnt++;
	//    			// sanity check?
	//    			if(cnt>=unplayedCount)
	//    				break;
	    		}  
	    	}
	    	return unplayedList;
//    	}
    }
    
    public int getShuffleMode() {
        return mShuffleMode;
    }
    
    
    public void setRockOnShuffleMode(int shufflemode) {
        synchronized(this) {
            if (mRockOnShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mRockOnShuffleMode = shufflemode;
            if (mRockOnShuffleMode == Constants.SHUFFLE_AUTO) {
            	mRockOnShuffleMode = Constants.SHUFFLE_NORMAL;
//	
//                if (makeAutoShuffleList()) {
//                    mPlayListLen = 0;
//                    doAutoShuffleUpdate();
//                    mPlayPos = 0;
//                    openCurrent();
//                    play();
//                    notifyChange(Constants.META_CHANGED);
//                    return;
//                } else {
//                    // failed to build a list of files to shuffle
//                    mRockOnShuffleMode = Constants.SHUFFLE_NONE;
//                }
            }
            saveQueue(false);
        }
    }
    public int getRockOnShuffleMode() {
        return mRockOnShuffleMode;
    }

    double oLastRepeatChange = System.currentTimeMillis();
    public void setRepeatMode(int repeatmode) {
        synchronized(this) {
        	/********************
        	 * 
        	 * ANALYTICS
        	 * 
        	 ********************/
        	int duration = (int) (System.currentTimeMillis()-oLastRepeatChange);
        	if (mAnalytics != null) {
	        	switch(mRepeatMode) {
	        	case Constants.REPEAT_NONE:
	        		mAnalytics.trackEvent("Duration", "Repeat Mode", "NONE", duration);
	            	break;
	        	case Constants.REPEAT_ALL:
	        		mAnalytics.trackEvent("Duration", "Repeat Mode", "ALL", duration);
	            	break;
	        	case Constants.REPEAT_CURRENT:
	        		mAnalytics.trackEvent("Duration", "Repeat Mode", "CURRENT", duration);
	                break;
	        	}
	        	switch(repeatmode) {
	    		case Constants.REPEAT_NONE:
	        		mAnalytics.trackEvent("User action", "Changed Repeat Mode", "NONE", 0);
	        		break;
	        	case Constants.REPEAT_ALL:
	        		mAnalytics.trackEvent("User action", "Changed Repeat Mode", "ALL", 0);
	        		break;
	        	case Constants.REPEAT_CURRENT:
	        		mAnalytics.trackEvent("User action", "Changed Repeat Mode", "CURRENT", 0);
	        		break;
	        	}
        	}
        	oLastRepeatChange = System.currentTimeMillis();
        	/***********************/
        	
            mRepeatMode = repeatmode;
            // let our widgets refresh
            if(mRepeatMode == Constants.REPEAT_NONE)
            	notifyChange(Constants.PLAYMODE_CHANGED);
            else
            	notifyChange(Constants.PLAYMODE_CHANGED);

            saveQueue(false);
        }
    }
    public int getRepeatMode() {
        return mRepeatMode;
    }
    
    public void setPlaylistId(int playlistId){
    	synchronized(this){
    		if(playlistId != mPlaylistId && 
    			mPlayList != null &&
    			mPlayList.length > 0)
    			this.removeTracks(0, mPlayList.length - 1);
    		mPlaylistId = playlistId;
    	}
    }
    
    public int getPlaylistId(){
    	return mPlaylistId;
    }
    
    public void setRockOnRepeatMode(int repeatmode) {
        synchronized(this) {
            mRockOnRepeatMode = repeatmode;
            saveQueue(false);
        }
    }
    public int getRockOnRepeatMode() {
        return mRockOnRepeatMode;
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if
     * no file is currently playing.
     */
    public String getPath() {
        return mFileToPlay;
    }
    
    /**
     * Returns the rowid of the currently playing file, or -1 if
     * no file is currently playing.
     */
    public long getAudioId() {
        synchronized (this) {
        	if(mPlayList != null && mPlayList.length > 0)
    	   	// XXX Bug Report fix
        	{
	            if (mPlayPos >= 0 && mPlayer.isInitialized()) {
	            	if(mPlayPos < mPlayList.length)
	            		return mPlayList[mPlayPos];
	            	else
	            		return mPlayList[mPlayList.length-1];
	            }
        	}
        }
        return -1;
    }
    
    /**
     * Returns the position in the queue 
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized(this) {
            return mPlayPos;
        }
    }
    
    /**
     * Starts playing the track at the given position in the queue.
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized(this) {
            stop(false);
            mPlayPos = pos;
            openCurrent();
            play();
            notifyChange(Constants.META_CHANGED);
        }
    }

    public String getArtistName() {
        synchronized(this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
        }
    }
    
    public long getArtistId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        }
    }

    public String getTrackName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        }
    }

//    private boolean isPodcast() {
//        synchronized (this) {
//            if (mCursor == null) {
//                return false;
//            }
//            return (mCursor.getInt(PODCASTCOLIDX) > 0);
//        }
//    }
    
//    private long getBookmark() {
//        synchronized (this) {
//            if (mCursor == null) {
//                return 0;
//            }
//            return mCursor.getLong(BOOKMARKCOLIDX);
//        }
//    }
    
    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (mPlayer.isInitialized()) {
            if (pos < 0) pos = 0;
            if (pos > mPlayer.duration()) pos = mPlayer.duration();
            return mPlayer.seek(pos);
        }
        return -1;
    }

    /**
     * 
     * @param scrobbler
     */
    public void setScrobbler(String scrobbler)
    {
    	mScrobblerName = scrobbler;
    }
    
    /**
     * 
     * @param lock
     */
    public void setLockScreen(boolean lock)
    {
    	if(mLock != lock)
    	{
    		mLock = lock;
    		if(mLock)
    		{
	    		Log.i(TAG, "Registering ScreenOn receivers...");
	    		registerScreenOnReceiver();
    		}
    		else
    		{
	    		Log.i(TAG, "Unregistering ScreenOn receivers...");
	    		unregisterScreenOnReceiver();
    		}
    	}
    }
    
    /**
     * 
     */
    public void registerScreenOnReceiver()
    {
    	/**
    	 *  are we using the lockscreen? 
    	 */
//    	if(!PreferenceManager.getDefaultSharedPreferences(this).
//    			getBoolean(
//    					getString(
//    							R.string.preference_key_lock_screen), 
//    					false))
    	if(mLock)
    	{
	    	/**
	    	 * We are, register the receiver
	    	 */
	    	if(mScreenOnReceiver == null)
	    		mScreenOnReceiver = new ScreenOnIntentReceiver();
	    	
	    	registerReceiver(
	    			mScreenOnReceiver,
	    			new IntentFilter("android.intent.action.SCREEN_ON"));
    	}
    }
    
    public void unregisterScreenOnReceiver()
    {
        try
        {
	        if(mScreenOnReceiver != null)
	        	unregisterReceiver(mScreenOnReceiver);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
    }
    
    /**
     * Save state before the app crashes
     * 	so we can restart gracefully upon restart
     */
    public void prepareForCrash()
    {
    	Log.i(TAG, "Yep, service too...");
    }
    
    /**
     * Analytics
     * 
     * @param pageName
     */
    public void trackPage(String pageName) {
    	if(mAnalytics != null) {
    		mAnalytics.trackPageView(pageName);
    	}
    }
    
    /**
     * Analytics
     * 
     * @param cat
     * @param action
     * @param label
     * @param val
     */
    public void trackEvent(String cat, String action, String label, int val) {
    	if(mAnalytics != null) {
    		mAnalytics.trackEvent(cat, action, label, val);
    	}
    }
    
    /**
     * EQ
     * @return
     */
    boolean isEqEnabled() {
    	return mEqualizerWrapper.isEnabled();
    }
    
    /**
     * EQ
     */
    void enableEq() {
//    	mEqualizerWrapper.mSettings.setEnabled();
    	/* A little bit of reflection to maintain compatibility */
		try {
//	    	mEqualizerWrapper.enable(0, mPlayer.mMediaPlayer.getAudioSessionId());
			Method m = MediaPlayer.class.getMethod("getAudioSessionId", new Class[]{});
    		mEqualizerWrapper.enable(0, (Integer) m.invoke(mPlayer.mMediaPlayer, new Object[]{}));
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
    	try {
			mPreferences.edit().putString(Constants.prefKey_mEqualizerSettings, mEqualizerWrapper.getSettings()).commit();
			mPreferences.edit().putBoolean(Constants.prefKey_mEqualizerEnabled, true); // a little dupe
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * EQ
     */
	void disableEq() {
		mEqualizerWrapper.disable();
    	try {
			mPreferences.edit().putString(Constants.prefKey_mEqualizerSettings, mEqualizerWrapper.getSettings()).commit();
			mPreferences.edit().putBoolean(Constants.prefKey_mEqualizerEnabled, false); // a little dupe
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * EQ
	 * @return
	 */
	int[] getEqBandHz() {
		// TODO:
		int numBands = mEqualizerWrapper.getNumberOfBands();
		int[] bandHzs = new int[numBands];
		for(short i=0; i<numBands; i++) {
			bandHzs[i] = mEqualizerWrapper.getCenterFreq(i);
		}
		return bandHzs;
		
//		return new int[]{20000, 100000, 1000000, 5000000, 10000000, 20000000};
	}
	
	/**
	 * EQ
	 * @return
	 */
	int[] getEqBandLevels() {
		int numBands = mEqualizerWrapper.getNumberOfBands();
		int[] bandLevels = new int[numBands];
		for(short i=0; i<numBands; i++) {
			bandLevels[i] = mEqualizerWrapper.getBandLevel(i);
		}
		return bandLevels;
		
//		return new int[]{0, 255, 128, 0, 128, 255};
	}
	
	/**
	 * 
	 * @return
	 */
	int	getEqCurrentPreset() {
		return 0;
	}
	
	/**
	 * EQ
	 * @return
	 */
	String[] getEqPresetNames() {
		return new String[]{"Flat", "Mustard"};
	}
	
	/**
	 * EQ
	 * @return
	 */
	int[] getEqLevelRange() {
		short[] levels = mEqualizerWrapper.getBandLevelRange();
		int[] levelsInt = new int[levels.length];
		for(int i=0; i<levels.length; i++) {
			levelsInt[i] = levels[i];
		}
		return levelsInt;
		
//		return new int[] {0, 255};
//		return new int[] {0, 16, 32, 64, 96, 128, 160, 192, 224, 255};
	}
	
	/**
	 * EQ
	 * @return
	 */
	int	getEqNumBands() {
		return mEqualizerWrapper.getNumberOfBands();
//		return 6;
	}
    
	/**
	 * EQ
	 * @param bandIdx
	 * @param level
	 */
	void setEqBandLevel(int bandIdx, int level) {
		Log.i(TAG, "Setting band: "+bandIdx+" to "+(short)level);
		mEqualizerWrapper.setBandLevel((short)bandIdx, (short) level);
		/* save equalizer settings in the preferences */
		try {
			mPreferences.edit().putString(Constants.prefKey_mEqualizerSettings, mEqualizerWrapper.getSettings()).commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * EQ
	 * @param presetIx
	 */
	void setEqPreset(int presetIx) {
		// TODO:
	}
	
    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class MultiPlayer {
        private MediaPlayer mMediaPlayer = new MediaPlayer();
        private Handler mHandler;
        private boolean mIsInitialized = false;

        public MultiPlayer() {
            mMediaPlayer.setWakeMode(RockOnNextGenService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSourceAsync(String path) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(preparedlistener);
                mMediaPlayer.prepareAsync();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
        }
        
        public void setDataSource(String path) {
            double startTimestamp = System.currentTimeMillis();
        	try {
                mMediaPlayer.reset();
                mMediaPlayer.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    mMediaPlayer.setDataSource(RockOnNextGenService.this, Uri.parse(path));
                } else {
                    mMediaPlayer.setDataSource(path);
                }
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
            Log.i(TAG, "setDataSource took: "+(System.currentTimeMillis()-startTimestamp));
        }
        
        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
        	double startTimestamp = System.currentTimeMillis();
        	if(mStartHandler.hasMessages(0))
        		mStartHandler.removeMessages(0);
        	mStartHandler.sendEmptyMessageDelayed(0, Constants.PLAY_ACTION_DELAY);
//            mMediaPlayer.start();
            Log.i(TAG, "start took: "+(System.currentTimeMillis()-startTimestamp));
        }
        
        public Handler mStartHandler  = new Handler(){
        	@Override
        	public void handleMessage(Message msg){
        		mMediaPlayer.start();
        	}
        };

        public void stop() {
            mStartHandler.removeCallbacksAndMessages(null);
            mMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mMediaPlayer.release();
        }
        
        public void pause() {
            mMediaPlayer.pause();
        }
        
        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
                mWakeLock.acquire(10000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                notifyChange(Constants.ASYNC_OPEN_COMPLETE);
            }
        };
 
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    mIsInitialized = false;
                    mMediaPlayer.release();
                    // Creating a new MediaPlayer and settings its wakemode does not
                    // require the media service, so it's OK to do this now, while the
                    // service is still being restarted
                    mMediaPlayer = new MediaPlayer(); 
                    mMediaPlayer.setWakeMode(RockOnNextGenService.this, PowerManager.PARTIAL_WAKE_LOCK);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                    Log.i(TAG, "MEDIA SERVER ERROR");
                    return true;
                default:
                    break;
                }
                return false;
           }
        };

        public long duration() {
            return mMediaPlayer.getDuration();
        }

        public long position() {
            return mMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mMediaPlayer.setVolume(vol, vol);
        }
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IRockOnNextGenService.Stub {
        WeakReference<RockOnNextGenService> mService;
        
        ServiceStub(RockOnNextGenService service) {
            mService = new WeakReference<RockOnNextGenService>(service);
        }

        public void openFileAsync(String path)
        {
            mService.get().openAsync(path);
        }
        public void openFile(String path, boolean oneShot)
        {
            mService.get().open(path, oneShot);
        }
        public void open(long [] list, int position) {
            mService.get().open(list, position);
        }
        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }
        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }
        public boolean isPlaying() {
            return mService.get().isPlaying();
        }
        public void stop() {
            mService.get().stop();
        }
        public void pause() {
            mService.get().pause();
        }
        public void play() {
            mService.get().play();
        }
        public void prev() {
            mService.get().prev();
        }
        public void next() {
            mService.get().next(true);
        }
        public String getTrackName() {
            return mService.get().getTrackName();
        }
        public String getAlbumName() {
            return mService.get().getAlbumName();
        }
        public long getAlbumId() {
            return mService.get().getAlbumId();
        }
        public String getArtistName() {
            return mService.get().getArtistName();
        }
        public long getArtistId() {
            return mService.get().getArtistId();
        }
        public void enqueue(long [] list , int action) {
            mService.get().enqueue(list, action);
        }
        public long [] getQueue() {
            return mService.get().getQueue();
        }
		public long[] getOutstandingQueue() throws RemoteException {
			return mService.get().getOutstandingQueue();
		}
        public void moveQueueItem(int from, int to) {
            mService.get().moveQueueItem(from, to);
        }
        public String getPath() {
            return mService.get().getPath();
        }
        public long getAudioId() {
            return mService.get().getAudioId();
        }
        public long position() {
            return mService.get().position();
        }
        public long duration() {
            return mService.get().duration();
        }
        public long seek(long pos) {
            return mService.get().seek(pos);
        }
        public void setShuffleMode(int shufflemode) {
            mService.get().setShuffleMode(shufflemode);
        }
        public int getShuffleMode() {
            return mService.get().getShuffleMode();
        }
        public void setRockOnShuffleMode(int shufflemode) {
            mService.get().setRockOnShuffleMode(shufflemode);
        }
        public int getRockOnShuffleMode() {
            return mService.get().getRockOnShuffleMode();
        }
        public int removeTracks(int first, int last) {
            return mService.get().removeTracks(first, last);
        }
        public int removeTrack(long id) {
            return mService.get().removeTrack(id);
        }
        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }
        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }
        public void setRockOnRepeatMode(int repeatmode) {
            mService.get().setRockOnRepeatMode(repeatmode);
        }
        public int getRockOnRepeatMode() {
            return mService.get().getRockOnRepeatMode();
        }
        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }

		public int getPlaylistId() throws RemoteException {
			return mService.get().getPlaylistId();
		}

		public void setPlaylistId(int playlistId) throws RemoteException {
			mService.get().setPlaylistId(playlistId);
		}
		
		public void setScrobbler(String scrobblerName) throws RemoteException {
			mService.get().setScrobbler(scrobblerName);
		}
		
		public void setLockScreen(boolean lock) throws RemoteException {
			mService.get().setLockScreen(lock);
		}
		
		public void prepareForCrash() throws RemoteException {
			mService.get().prepareForCrash();
		}

		@Override
		public void registerScreenOnReceiver() throws RemoteException {
			mService.get().registerScreenOnReceiver();
		}
		
		@Override
		public void unregisterScreenOnReceiver() throws RemoteException {
			mService.get().unregisterScreenOnReceiver();
		}

		@Override 
		public void trackPage(String pageName) throws RemoteException {
			mService.get().trackPage(pageName);
		}
		
		@Override
	    public void trackEvent(String cat, String action, String label, int val) {
			mService.get().trackEvent(cat, action, label, val);
		}

		@Override
		public void disableEq() throws RemoteException {
			mService.get().disableEq();
		}

		@Override
		public void enableEq() throws RemoteException {
			mService.get().enableEq();
		}

		@Override
		public int[] getEqBandHz() throws RemoteException {
			return mService.get().getEqBandHz();
		}

		@Override
		public int[] getEqBandLevels() throws RemoteException {
			return mService.get().getEqBandLevels();
		}

		@Override
		public int getEqCurrentPreset() throws RemoteException {
			return mService.get().getEqCurrentPreset();
		}

		@Override
		public int[] getEqLevelRange() throws RemoteException {
			return mService.get().getEqLevelRange();
		}

		@Override
		public int getEqNumBands() throws RemoteException {
			return mService.get().getEqNumBands();
		}

		@Override
		public String[] getEqPresetNames() throws RemoteException {
			return mService.get().getEqPresetNames();
		}

		@Override
		public boolean isEqEnabled() throws RemoteException {
			return mService.get().isEqEnabled();
		}

		@Override
		public void setEqBandLevel(int bandIdx, int level)
				throws RemoteException {
			mService.get().setEqBandLevel(bandIdx, level);
		}

		@Override
		public void setEqPreset(int presetIdx) throws RemoteException {
			mService.get().setEqPreset(presetIdx);
		}
    
		
    }
    
    private final IBinder mBinder = (IBinder) new ServiceStub(this);
}
