/*
 * Adapted from the Android open source Music app
 */

package org.abrantix.rockon.rockonnggl;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class RockOnNextGenAppWidgetProvider4x1 extends AppWidgetProvider {
    static final String TAG = "RockOnNextGenAppWidgetProvider4x1";
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";
    
    static final ComponentName THIS_APPWIDGET =
        new ComponentName(
        		Constants.WIDGET_COMPONENT_PACKAGE,
                Constants.WIDGET_COMPONENT_4x1);
    
    private static RockOnNextGenAppWidgetProvider4x1 sInstance;
    
    static synchronized RockOnNextGenAppWidgetProvider4x1 getInstance() {
        if (sInstance == null) {
            sInstance = new RockOnNextGenAppWidgetProvider4x1();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(Constants.SERVICECMD);
        updateIntent.putExtra(Constants.CMDNAME,
        		RockOnNextGenAppWidgetProvider4x1.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }
    
    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.album_appwidget_4x1);
        
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.widget_app_start));

        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
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
                    Constants.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(RockOnNextGenService service, int[] appWidgetIds) {
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget_4x1);
        
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
        CharSequence errorState = null;
        
        // Format title string with track number, or show SD card message
        String status = Environment.getExternalStorageState();
//        if (status.equals(Environment.MEDIA_SHARED) ||
//                status.equals(Environment.MEDIA_UNMOUNTED)) {
//            errorState = res.getText(R.string.sdcard_busy_title);
//        } else if (status.equals(Environment.MEDIA_REMOVED)) {
//            errorState = res.getText(R.string.sdcard_missing_title);
//        } else
        	if (titleName == null) {
            errorState = res.getText(R.string.widget_app_start);
        }
        
        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
            
        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.artist, artistName);
        }
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }

        // Link actions buttons to intents
        linkButtons(service, views, playing);
        
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivityStarter},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, RockOnNextGenService.class);
        
//        if (playerActive) {
//            intent = new Intent(context, MediaPlaybackActivityStarter.class);
//            pendingIntent = PendingIntent.getActivity(context,
//                    0 /* no requestCode */, intent, 0 /* no flags */);
//            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
//        } else {
//            intent = new Intent(context, MusicBrowserActivity.class);
//            pendingIntent = PendingIntent.getActivity(context,
//                    0 /* no requestCode */, intent, 0 /* no flags */);
//            views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
//        }
        intent = new Intent(context, RockOnNextGenGL.class);
        pendingIntent = PendingIntent.getActivity(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
        
        intent = new Intent(Constants.TOGGLEPAUSE_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);
        
        intent = new Intent(Constants.NEXT_ACTION);
        intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getService(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }
}
