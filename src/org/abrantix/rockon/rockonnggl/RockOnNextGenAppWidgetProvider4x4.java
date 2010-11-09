/*
 * adapted from the stock music app
 */

package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class RockOnNextGenAppWidgetProvider4x4 extends AppWidgetProvider {
    static final String TAG = "RockOnNextGenAppWidgetProvider4x4";
    
    private boolean 			mPlaying = false;
    private int					mShuffleMode = Constants.SHUFFLE_NONE;
    private int					mRepeatMode = Constants.REPEAT_NONE;
    public static final String 	CMDAPPWIDGETUPDATE = "appwidgetupdate";
    
    static final ComponentName THIS_APPWIDGET =
        new ComponentName(
        		Constants.WIDGET_COMPONENT_PACKAGE,
                Constants.WIDGET_COMPONENT_4x4);
    
    private static RockOnNextGenAppWidgetProvider4x4 sInstance;
    
    static synchronized RockOnNextGenAppWidgetProvider4x4 getInstance() {
        if (sInstance == null) {
            sInstance = new RockOnNextGenAppWidgetProvider4x4();
        }
        return sInstance;
    }

    private Bitmap	albumCover = null;
    private Bitmap	albumCoverTmp = null;
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(Constants.SERVICECMD);
        updateIntent.putExtra(Constants.CMDNAME,
                RockOnNextGenAppWidgetProvider4x4.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
        
        // Create our bitmap cache
        albumCover = Bitmap.createBitmap(
        		Constants.ALBUM_ART_TEXTURE_SIZE, 
        		Constants.ALBUM_ART_TEXTURE_SIZE, 
        		Config.RGB_565);
    }
    
    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = 
        	new RemoteViews(
        			context.getPackageName(), 
        			R.layout.album_appwidget_4x4);
        
//        views.setViewVisibility(R.id.title, View.GONE);
//        views.setTextViewText(R.id.artist, res.getText(R.string.emptyplaylist));

        linkButtons(
        		context, 
        		views, 
        		false /* not playing */,
        		Constants.SHUFFLE_NONE,
        		Constants.REPEAT_NONE);
        pushUpdate(
        		context, 
        		appWidgetIds, 
        		views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
//        if (appWidgetIds != null) {
//            gm.updateAppWidget(appWidgetIds, views);
//        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
//        }
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(RockOnNextGenService service, String what) {
        if (hasInstances(service)) {
            if (Constants.PLAYBACK_COMPLETE.equals(what) ||
                    Constants.META_CHANGED.equals(what) ||
                    Constants.PLAYSTATE_CHANGED.equals(what) ||
                    Constants.PLAYMODE_CHANGED.equals(what)) 
            {
                performUpdate(service, null);
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(RockOnNextGenService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget_4x4);
        
        /* Set the right album cover */
        Bitmap wickedCover = 
        	WidgetCoverUtils.getWidgetCoverBitmap(
				String.valueOf(service.getAlbumId()),
				String.valueOf(service.getArtistName()),
				String.valueOf(service.getTrackName()),
				Constants.REASONABLE_ALBUM_ART_SIZE,
				Constants.REASONABLE_ALBUM_ART_SIZE);
        if(wickedCover != null){
        	views.setImageViewBitmap(
        			R.id.widget_album_cover, 
        			wickedCover);
        }
        
//        CharSequence titleName = service.getTrackName();
//        CharSequence artistName = service.getArtistName();
//        CharSequence errorState = null;
        
//        // Format title string with track number, or show SD card message
//        String status = Environment.getExternalStorageState();
//        if (status.equals(Environment.MEDIA_SHARED) ||
//                status.equals(Environment.MEDIA_UNMOUNTED)) {
//            errorState = res.getText(R.string.sdcard_busy_title);
//        } else if (status.equals(Environment.MEDIA_REMOVED)) {
//            errorState = res.getText(R.string.sdcard_missing_title);
//        } else if (titleName == null) {
//            errorState = res.getText(R.string.emptyplaylist);
//        }
//        
//        if (errorState != null) {
//            // Show error state to user
//            views.setViewVisibility(R.id.title, View.GONE);
//            views.setTextViewText(R.id.artist, errorState);
//            
//        } else {
//            // No error, so show normal titles
//            views.setViewVisibility(R.id.title, View.VISIBLE);
//            views.setTextViewText(R.id.title, titleName);
//            views.setTextViewText(R.id.artist, artistName);
//        }
        
        // Set correct drawable for pause state
        mPlaying = service.isPlaying();
        if (mPlaying) {
            views.setImageViewResource(R.id.control_play, R.drawable.pause_selector);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.play_selector);
        }
        
        // Set correct drawable for shuffle state
        mShuffleMode = service.getShuffleMode();
        if (mShuffleMode == Constants.SHUFFLE_NONE) {
            views.setImageViewResource(R.id.control_shuffle, R.drawable.shuffle_none_selector);
        } else {
            views.setImageViewResource(R.id.control_shuffle, R.drawable.shuffle_selector);
        }
        
        // Set correct drawable for repeat state
        mRepeatMode = service.getRepeatMode();
        if (mRepeatMode == Constants.REPEAT_NONE) {
            views.setImageViewResource(R.id.control_repeat, R.drawable.repeat_none_selector);
        } else if(mRepeatMode == Constants.REPEAT_CURRENT){
            views.setImageViewResource(R.id.control_repeat, R.drawable.repeat_current_selector);
        } else {
        	views.setImageViewResource(R.id.control_repeat, R.drawable.repeat_all_selector);
        }

        // Link actions buttons to intents
        linkButtons(service, views, mPlaying, mShuffleMode, mRepeatMode);
        
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivity},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(
    		Context context, 
    		RemoteViews views, 
    		boolean playerActive, 
    		int shuffleMode, 
    		int repeatMode) 
    {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, RockOnNextGenService.class);
        
        /* TAP ALBUM COVER = OPEN APP */
        if (playerActive) {
            intent = new Intent(context, RockOnNextGenGL.class);
            pendingIntent = PendingIntent.getActivity(
            		context,
                    0 /* no requestCode */, 
                    intent, 
                    0 /* no flags */);
            views.setOnClickPendingIntent(R.id.widget_album_cover, pendingIntent);
        } else {
        	/* code is duplicated for now but we might 
        	 * want to change this in the future 
        	 */
            intent = new Intent(context, RockOnNextGenGL.class);
            pendingIntent = PendingIntent.getActivity(
            		context,
                    0 /* no requestCode */, 
                    intent, 
                    0 /* no flags */);
            views.setOnClickPendingIntent(R.id.widget_album_cover, pendingIntent);
        }
        
        /* PLAY/PAUSE */
        intent = new Intent(Constants.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(
        		context,
                0 /* no requestCode */, 
                intent, 
                0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        /* NEXT */
        intent = new Intent(Constants.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(
        		context,
                0 /* no requestCode */, 
                intent, 
                0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
        
        /* PREVIOUS */
        intent = new Intent(Constants.PREVIOUS_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(
        		context,
                0 /* no requestCode */, 
                intent, 
                0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_previous, pendingIntent);
        
        /* TOGGLE SHUFFLE */
        if(shuffleMode == Constants.SHUFFLE_NONE)
        	intent = new Intent(Constants.SHUFFLE_NORMAL_ACTION);
        else
        	intent = new Intent(Constants.SHUFFLE_NONE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(
        		context,
                0 /* no requestCode */, 
                intent, 
                0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_shuffle, pendingIntent);
        
        /* TOGGLE REPEAT */
        if(repeatMode == Constants.REPEAT_NONE)
        	intent = new Intent(Constants.REPEAT_CURRENT_ACTION);
        else
        	intent = new Intent(Constants.REPEAT_NONE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(
        		context,
                0 /* no requestCode */, 
                intent, 
                0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_repeat, pendingIntent);
    }
}
