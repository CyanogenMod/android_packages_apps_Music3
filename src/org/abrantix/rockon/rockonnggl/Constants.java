package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.provider.MediaStore;

public class Constants{

	/** UI States **/
	static final int STATE_INTRO = -1;
	static final int STATE_NAVIGATOR = 0;
	static final int STATE_FULLSCREEN = 1;
	
	/** Browse Categories **/
	static final int BROWSECAT_ALBUM = 0;
	static final int BROWSECAT_GENRE = 1;
	static final int BROWSECAT_ARTIST = 2;
	static final int BROWSECAT_PLAYLIST = 3;

	/** Renderer Types */
	static final int RENDERER_CUBE = 0;
	static final int RENDERER_WALL = 1;
	
	/** Playlist Ids **/
	static final int PLAYLIST_UNKNOWN = -1; // uninitialized variable
	static final int PLAYLIST_ALL = -2;
	static final int PLAYLIST_MOST_RECENT = -3;
	static final int PLAYLIST_MOST_PLAYED = -4;
	static final int PLAYLIST_GENRE_OFFSET = -100000;
	static final int PLAYLIST_GENRE_RANGE = 5000;
	
	/** and keys */
	static final String	PLAYLIST_ID_KEY = "playlistId";
	static final String PLAYLIST_NAME_KEY = "playlistName";

	
	/** Album Cursor Projection **/
	static final String[] albumProjection = 
	{
		MediaStore.Audio.Albums._ID,
		MediaStore.Audio.Albums.ALBUM_KEY,
//		MediaStore.Audio.Albums.ALBUM_ID,
		MediaStore.Audio.Albums.ALBUM,
		MediaStore.Audio.Albums.ARTIST,
		MediaStore.Audio.Albums.ALBUM_ART
	};
	
	/** Song Cursor Projection **/
	static final String[] songProjection = 
	{
		MediaStore.Audio.Media._ID,
		MediaStore.Audio.Media.ALBUM,
		MediaStore.Audio.Media.ALBUM_ID,
		MediaStore.Audio.Media.ARTIST,
		MediaStore.Audio.Media.ARTIST_ID,
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.TITLE_KEY,
		MediaStore.Audio.Media.DATA,
		MediaStore.Audio.Media.DISPLAY_NAME,
		MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.IS_MUSIC,
		MediaStore.Audio.Media.TRACK
	};
	
	/** Genres Cursor Projection **/
	static final String[] genreProjection = 
	{
		MediaStore.Audio.Genres._ID,
		MediaStore.Audio.Genres.NAME
	};
	
	/** Genres Cursor Projection */
	static final String[] genreMemberProjection = 
	{
		MediaStore.Audio.Genres.Members._ID,
//		MediaStore.Audio.Genres.Members.AUDIO_ID,
//		MediaStore.Audio.Genres.Members.GENRE_ID,
		MediaStore.Audio.Genres.Members.ALBUM_ID,
		MediaStore.Audio.Genres.Members.ALBUM,
		MediaStore.Audio.Genres.Members.ARTIST,
		MediaStore.Audio.Genres.Members.ARTIST_ID,
		MediaStore.Audio.Genres.Members.TITLE,
		MediaStore.Audio.Genres.Members.TRACK,
		MediaStore.Audio.Genres.Members.DURATION
	};
	
	/** Playlist Cursor Projection **/
	static final String[] playlistProjection = 
	{
		MediaStore.Audio.Playlists._ID,
		MediaStore.Audio.Playlists.NAME
	};
	
	/** Playlist Members Cursor Projection */
	static final String[] playlistMembersProjection = 
	{
		MediaStore.Audio.Playlists.Members._ID,
		MediaStore.Audio.Playlists.Members.PLAYLIST_ID,
//			MediaStore.Audio.Playlists.Members.ALBUM_ART,
		MediaStore.Audio.Playlists.Members.ALBUM_ID,
		MediaStore.Audio.Playlists.Members.ALBUM,
		MediaStore.Audio.Playlists.Members.ARTIST,
		MediaStore.Audio.Playlists.Members.ARTIST_ID,
		MediaStore.Audio.Playlists.Members.AUDIO_ID,
		MediaStore.Audio.Playlists.Members.TITLE,
		MediaStore.Audio.Playlists.Members.TRACK,
		MediaStore.Audio.Playlists.Members.DURATION
	};
	
	/** Genre Member Alphabetical Sorting **/
	static final String genreMembersAlbumSorting = 
		MediaStore.Audio.Genres.Members.ALBUM_ID + " ASC";
	
	/** Genre Alphabetical Sorting **/
	static final String genreAlphabeticalSorting = 
		MediaStore.Audio.Genres.NAME + " ASC";
	
	/** Playlist Members Alphabetical Album Sorting **/
	static final String playlistMembersAlbumSorting = 
		MediaStore.Audio.Playlists.Members.ALBUM_ID + " ASC";
	
	/** Playlist Alphabetical Sorting **/
	static final String playlistAlphabeticalSorting = 
		MediaStore.Audio.Playlists.NAME + " ASC";
	
	/** Album Cursor Sorting **/
	static final String albumAlphabeticalSortOrder = 
		MediaStore.Audio.Albums.ARTIST + " ASC";
	
	/** Song List Sorting **/
	static final String songListNumericalSorting = 
		MediaStore.Audio.Media.TRACK + " ASC";
	static final String songListAlbumAndNumericalSorting = 
		MediaStore.Audio.Media.ALBUM + " ASC " + 
		", "+
		MediaStore.Audio.Media.TRACK + " ASC";
	
	/** song list adapter to/from */
	static final int	albumSongListLayoutId = R.layout.songlist_dialog_item;
	static final String[] albumSongListFrom = {
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.DURATION
	};
	static final int[]	albumSongListTo = {
		R.id.songlist_item_song_name,
		R.id.songlist_item_song_duration
	};
	
	/** song list adapter for current playing lists */
	static final int	queueSongListLayoutId = R.layout.songlist_dialog_item_queue;
	static final String[] queueSongListFrom = {
		MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ARTIST,
		MediaStore.Audio.Media.DURATION
	};
	static final int[]	queueSongListTo = {
		R.id.queue_list_songname,
		R.id.queue_list_artistname,
		R.id.queue_list_songduration
	};
	
//	/** UI Dimension proportions */
//	static final float navItemMinFraction = 0.6f; // minimum fraction of the smallest screen dimension
	
	/** UI scrolling parameters */
//	static final float	MIN_SCROLL_SPEED = 0.015f;
//	static final float	SCROLL_SPEED_DECAY = 0.1f;
	static final double	FRAME_DURATION_STD = 40;
	static final double	FRAME_JUMP_MAX = 10;
	static final float	SCROLL_SPEED_SMOOTHNESS = 0.11f;
	static final float	MIN_SCROLL = 0.04f; // as the fraction of the cover size
	static final float	MAX_SCROLL = 0.13f; // as the fraction of the cover size
	static final float	SCROLL_SPEED_BOOST = 40.f;
	static final float	MAX_LOW_SPEED = .08f; // mScrollingSpeed...
	static final float	MIN_SCROLL_TOUCH_MOVE = 0.085f;
	static final double	MAX_CLICK_DOWNTIME = 1000;
	static final int 	MIN_LONG_CLICK_DURATION = 1000;
	static final int	MAX_POSITION_OVERSHOOT = 1;
	static final int	SCROLLING_RESET_TIMEOUT = 7500;
		
	/** CLICK MODES */
	static final int	SINGLE_CLICK = 0;
	static final int	LONG_CLICK = 1;
	
	/** UI scrolling modes */
	static final int	SCROLL_MODE_VERTICAL = 0;
	static final int	SCROLL_MODE_HORIZONTAL = 1;
	
	/** UI element IDs */
	static final int ALBUM_NAV_VIEW_ID = 0;
	static final int ALBUM_NAV_CONTROLS_ID = 1;
	static final int ALBUM_NAV_INFO_ID = 2;
	
	/** UI general interaction parameters */
	static final int CLICK_ACTION_DELAY = 250;
//	static final int CLICK_ANIMATION_DURATION = 300;
	static final int PLAY_ACTION_DELAY = 750;
	
	/** Album Art Stuff */
	static final int MIN_ALBUM_ART_SIZE = 100;
	static final int REASONABLE_ALBUM_ART_SIZE = 256;
	static final int ALBUM_ART_TEXTURE_SIZE = 256;
	
	/** Search Parameters */
	static final float SIMILARITY_THRESHOLD = 0.66f;
	
	/** Inter Process Variables */
	// album fetching thread - ui
	static final String	ALBUM_ART_DOWNLOAD_UI_UPDATE_IPC_MSG = "info";
	static final String	ALBUM_ART_DOWNLOAD_UI_UPDATE_DONE_IPC_MSG = "info_done";
    
	// Main app - ignore sdcard intent
	// FIXME: restart activity not working
//	static final String	MAIN_ACTIVITY_IGNORE_SD_CARD = "org.abrantix.rockon.rockonnggl.ignoresdcard";
	
	// Service - widget - ui intent ids
	static final int MAIN_ACTIVITY_INTENT = R.string.main_activity_intent;
	static final String PLAYSTATE_CHANGED = "org.abrantix.rockon.rockonnggl.playstatechanged";
    static final String META_CHANGED = "org.abrantix.rockon.rockonnggl.metachanged";
    static final String QUEUE_CHANGED = "org.abrantix.rockon.rockonnggl.queuechanged";
    static final String PLAYBACK_COMPLETE = "org.abrantix.rockon.rockonnggl.playbackcomplete";
    static final String ASYNC_OPEN_COMPLETE = "org.abrantix.rockon.rockonnggl.asyncopencomplete";
	static final String PLAYMODE_CHANGED = "org.abrantix.rockon.rockonnggl.playmodechanged";
    
    // The two scrobblers available on the Android Market
    static final String SCROBBLE_SLS_API = "com.adam.aslfms.notify.playstatechanged";
    static final String SCROBBLE_SD_API  = "net.jjc1138.android.scrobbler.action.MUSIC_STATUS";
    
    static final int	SCROBBLE_PLAYSTATE_START = 0;
    static final int 	SCROBBLE_PLAYSTATE_RESUME = 1;
    static final int 	SCROBBLE_PLAYSTATE_PAUSE = 2;
    static final int 	SCROBBLE_PLAYSTATE_COMPLETE = 3;
    

    // mediabutton - service command ids
    static final String SERVICECMD = "org.abrantix.rockon.rockonnggl.musicservicecommand";
    static final String CMDNAME = "command";
    static final String CMDTOGGLEPAUSE = "togglepause";
    static final String CMDSTOP = "stop";
    static final String CMDPAUSE = "pause";
    static final String CMDPREVIOUS = "previous";
    static final String CMDNEXT = "next";

    static final String TOGGLEPAUSE_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.togglepause";
    static final String PAUSE_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.pause";
    static final String PREVIOUS_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.previous";
    static final String NEXT_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.next";
    static final String SHUFFLE_NORMAL_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.shufflenormal";
    static final String SHUFFLE_NONE_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.shufflenone";
    static final String REPEAT_CURRENT_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.repeatcurrent";
    static final String REPEAT_NONE_ACTION = "org.abrantix.rockon.rockonnggl.musicservicecommand.repeatnone";
	
    static final int NOW = 1;
    static final int NEXT = 2;
    static final int LAST = 3;

    static final int START_STICKY = 1;
    
    /* Concert App */
    static final String CONCERT_APP_PACKAGE = "org.abrantix.rockon.concerts";
    static final String CONCERT_APP_MAIN_ACTIVITY = "org.abrantix.rockon.concerts.Concerts";
    
    /* Widget Stuff */
    static final String WIDGET_COMPONENT_PACKAGE = "org.abrantix.rockon.rockonnggl";
    static final String WIDGET_COMPONENT = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider";
    static final String WIDGET_COMPONENT_3x3 = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider3x3";
    static final String WIDGET_COMPONENT_4x4 = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider4x4";
    
    // Activity request codes
    static final int PREFERENCE_ACTIVITY_REQUEST_CODE = 0;
    static final int ALBUM_ART_CHOOSER_ACTIVITY_REQUEST_CODE = 1;
    
    /** Progress Update Handler Codes */
    static final int HARD_REFRESH = 0;
    static final int KEEP_REFRESHING = 1;
    static final int DO_NOT_REFRESH = 2;
    
    /** Notification Ids */
    static final int PLAY_NOTIFICATION_ID = 0;
    
    /** Play modes */
    static final int SHUFFLE_NONE = 0;
    static final int SHUFFLE_NORMAL = 1;
    static final int SHUFFLE_AUTO = 2;
    
    static final int REPEAT_NONE = 0;
    static final int REPEAT_CURRENT = 1;
    static final int REPEAT_ALL = 2;
    
    static final int FIND_PREV = 0;
    static final int FIND_NEXT = 1;
    
    /** PLAY QUEUE SIZE WHEN NOT SPECIFICALLY DEFINED */
    static final int REASONABLE_PLAY_QUEUE_SIZE = 32;
    
	/** File paths */
	static final String ROCKON_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOnNg/";
	static final String ROCKON_SMALL_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOnNg/small/";
	
	/** Preference keys */ // could be set also in values/preference_strings.xml
	static final String	prefkey_mRendererMode = "mRendererMode";
	static final String	prefkey_mNavigatorPositionX = "mNavigatorPositionX";
	static final String prefkey_mNavigatorTargetPositionX = "mNavigatorTargetPositionX";
	static final String	prefkey_mNavigatorPositionY = "mNavigatorPositionY";
	static final String prefkey_mNavigatorTargetPositionY = "mNavigatorTargetPositionY";
	static final String prefkey_mPlaylistId = "mPlaylistId";
	static final String prefkey_mFullscreen = "mFullScreen"; // duplicated in resources... FIXME
	
	
}