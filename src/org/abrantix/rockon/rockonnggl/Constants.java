package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.provider.MediaStore;

public class Constants{

	/** UI States **/
	static final int STATE_INTRO = -1;
	static final int STATE_NAVIGATOR = 0;
	static final int STATE_FULLSCREEN = 1;
	
	/** Browse Categories **/
	static final int BROWSECAT_ARTIST = 0;
	static final int BROWSECAT_ALBUM = 1;
	static final int BROWSECAT_SONG = 2;
	static final int BROWSECAT_GENRE = 3;
	static final int BROWSECAT_PLAYLIST = 4;

	/** Renderer Types */
	static final int RENDERER_CUBE = 0;
	static final int RENDERER_WALL = 1;
	static final int RENDERER_BORING = 2;
	static final int RENDERER_MORPH = 3;
	
	/** Theme Types */
	static final int THEME_NORMAL = 100; // these should not coincide with the Renderer modes
	static final int THEME_HALFTONE = 101;
	static final int THEME_EARTHQUAKE = 102;
	
	/** Half Tone Theme */
	static final int 	THEME_HALF_TONE_PROC_RESOLUTION = 640; 
	static final int 	THEME_HALF_TONE_BLOCK_COUNT = 64; 
	static final String THEME_HALF_TONE_FILE_EXT = ".halftone";

	/** Earthquake Theme */
	static final int	THEME_EARTHQUAKE_BLOCK_COUNT = 256;
	static final int	THEME_EARTHQUAKE_RANDOM_AMOUNT = 16;
	static final String THEME_EARTHQUAKE_FILE_EXT = ".earthquake";
	
	/** Playlist Ids **/
	static final int PLAYLIST_UNKNOWN = -1; // uninitialized variable
	static final int PLAYLIST_ALL = -2;
	static final int PLAYLIST_MOST_RECENT = -3;
	static final int PLAYLIST_MOST_PLAYED = -4;
	static final int PLAYLIST_GENRE_OFFSET = -100000;
	static final int PLAYLIST_GENRE_RANGE = 5000;
	
	/** Specific Constants */
	static final int NO_SPECIFIC_ARTIST = -1;
	
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
		MediaStore.Audio.Albums.ALBUM_ART,
		MediaStore.Audio.Albums.LAST_YEAR
	};
	
	/** Artist Cursor Projection */
	static final String[] artistProjection = 
	{
		MediaStore.Audio.Artists._ID,
		MediaStore.Audio.Artists.ARTIST_KEY,
		MediaStore.Audio.Artists.ARTIST,
		MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
		MediaStore.Audio.Artists.NUMBER_OF_TRACKS
	};
	
	/** Song Cursor Projection **/
	static final String[] songProjection = 
	{
		MediaStore.Audio.Media._ID,
		MediaStore.Audio.Media.ALBUM,
		MediaStore.Audio.Media.ALBUM_ID,
		MediaStore.Audio.Media.ALBUM_KEY,
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
		MediaStore.Audio.Genres.NAME + " COLLATE NOCASE ASC";
	
	/** Playlist Members Alphabetical Album Sorting **/
	static final String playlistMembersAlbumSorting = 
		MediaStore.Audio.Playlists.Members.ALBUM_ID + " ASC";
	
	/** Playlist Alphabetical Sorting **/
	static final String playlistAlphabeticalSorting = 
		MediaStore.Audio.Playlists.NAME + " COLLATE NOCASE ASC";
	
	/** Album Cursor Sorting **/
	static final String albumAlphabeticalSortOrder = 
		MediaStore.Audio.Albums.ALBUM_KEY + " ASC";
	
	static final String albumAlphabeticalSortOrderByArtist = 
		MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC"
		+ ", " + 
		MediaStore.Audio.Albums.LAST_YEAR + " DESC";
	
	/** Artist Albums Cursor Sorting **/
	static final String artistAlbumsYearSortOrder = 
		MediaStore.Audio.Albums.LAST_YEAR + " DESC";

	/** Artist Cursor Sorting **/
	static final String artistAlphabeticalSortOrder = 
		MediaStore.Audio.Artists.ARTIST_KEY + " ASC";
//		MediaStore.Audio.Albums.ARTIST + " COLLATE NOCASE ASC";
	
	/** Song List Sorting **/
	static final String songListNumericalSorting = 
		MediaStore.Audio.Media.TRACK + " ASC";
	static final String songListAlbumAndNumericalSorting = 
//		MediaStore.Audio.Media.YEAR + " DESC " + 
//		", "+
		MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC"+
		", "+
		MediaStore.Audio.Media.TRACK + " ASC";
	static final String songListPlaylistSorting = 
		MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC";
	static final String songListTitleSorting =
//		MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
		MediaStore.Audio.Media.TITLE_KEY + " ASC";

	
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
	
	/** album list adapter to/from */
	static final int	artistAlbumListLayoutId = R.layout.albumlist_dialog_item;
	static final String[] artistAlbumListFrom = {
		MediaStore.Audio.Albums.ALBUM,
		MediaStore.Audio.Albums.LAST_YEAR
	};
	static final int[]	artistAlbumListTo = {
		R.id.albumlist_item_album_name,
		R.id.albumlist_item_album_year
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
	
	/** Inactivity intervals */
	static final double	MAX_INACTIVITY_INTERVAL_TO_MAINTAIN_STATE = 10 * 1000; // 10 secs
	static final double MAX_INACTIVITY_INTERVAL_TO_MAINTAIN_PLAYLIST = 60 * 60 * 12 * 1000; // 12 hours
	
	/** UI scrolling parameters */
	static final double	FRAME_DURATION_STD = 40;
	static final double	FRAME_JUMP_MAX = 10;
	static final float	SCROLL_SPEED_SMOOTHNESS = 2.5f; // as the fraction of the overall animation that should be obtained (per second)
	static final float	CPU_SMOOTHNESS = 0.1f; // as the fraction of the overall animation that should be obtained (per second)
	static final float	MIN_SCROLL = 1.5f; // as the fraction of the cover size (per second)
//	static final float	MAX_SCROLL = 6.f; // as the fraction of the cover size (per second)
	static final float	MAX_SCROLL = 9.f; // as the fraction of the cover size (per second)
	static final float	SCROLL_SPEED_BOOST = 675.f;
//	static final float	MAX_LOW_SPEED = .08f; // mScrollingSpeed...
	static final float	MAX_LOW_SPEED = .0025f; // mScrollingSpeed...
	static final float	MIN_SCROLL_TOUCH_MOVE = 0.05f;
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
	
	/** Album Artist Switcher stuff */
	static final int 	SWITCHER_PERSIST_SWITCH_PERIOD = 750;
	static final int 	SWITCHER_HIGH_PRESENCE_ALPHA = 192;
	static final float 	SWITCHER_MOVEMENT_REQUIRED_TO_SWITCH = .25f;
	static final int 	SWITCHER_LOW_PRESENCE_ALPHA = 0;
	static final float 	SWITCHER_PRESENCE_UPDATE_STEP = .1f; // increment to navigate between 0 and 1
	static final float	SWITCHER_TEXT_RATIO = .66f;
	static final float	SWITCHER_CAT_CIRCLE_RATIO = .05f;
	static final float	SWITCHER_CAT_CIRCLE_SPACING = 1.f;
	static final int	SWITCHER_CAT_ALBUM = BROWSECAT_ALBUM;
	static final int	SWITCHER_CAT_ARTIST = BROWSECAT_ARTIST;
	static final int	SWITCHER_CAT_SONG = BROWSECAT_SONG;
	static final int	SWITCHER_CATEGORY_COUNT = 3; // ALBUMS + ARTIST + SONG 
	static final int	SWITCHER_CAT_ALBUM_STRING_RES = R.string.browse_cat_album;
	static final int	SWITCHER_CAT_ARTIST_STRING_RES = R.string.browse_cat_artists;
	static final int	SWITCHER_CAT_SONG_STRING_RES = R.string.browse_cat_songs;
	
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
	
	/** Art Download Parameters */
	static final int	GET_INET_ART_TOO = 0;
	static final int	GET_LOCAL_ART_ONLY = 1;
	
	/** Inter Process Variables */
	// album fetching thread - ui
	static final String	ALBUM_ART_DOWNLOAD_UI_UPDATE_IPC_MSG = "info";
	static final String	ALBUM_ART_DOWNLOAD_UI_UPDATE_DONE_IPC_MSG = "info_done";
	// art theme processing thread - ui
	static final String	ALBUM_ART_PROCESSING_UI_UPDATE_IPC_MSG = "info";
	static final String	ALBUM_ART_PROCESSING_UI_UPDATE_DONE_IPC_MSG = "info_done";
    
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
    
    static final String CMDSAVE = "save";
    static final String CMDRESTART = "restart";
    
    static final String CMDSEEKFWD = "seekfwd";
    static final String CMDSEEKBACK = "seekback";
    static final String CMDSEEKAMOUNT = "seekamount";
    
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
    
    /* Releases App */
    static final String RELEASES_APP_PACKAGE = "org.abrantix.releases";
    static final String RELEASES_APP_MAIN_ACTIVITY = "org.abrantix.releases.Releases";
    
    /* Widget Stuff */
    static final String WIDGET_COMPONENT_PACKAGE = "org.abrantix.rockon.rockonnggl";
    static final String WIDGET_COMPONENT = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider";
    static final String WIDGET_COMPONENT_3x3 = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider3x3";
    static final String WIDGET_COMPONENT_4x4 = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider4x4";
    static final String WIDGET_COMPONENT_4x1 = "org.abrantix.rockon.rockonnggl.RockOnNextGenAppWidgetProvider4x1";
    
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
    static final String ROCKON_BASE_PATH = "/sdcard/RockOn/";
    static final String ROCKON_DONATION_PATH = ROCKON_BASE_PATH + "donate";
	static final String ROCKON_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOnNg/";
	static final String ROCKON_SMALL_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOnNg/small/";
	static final String ROCKON_UNKNOWN_ART_FILENAME = "_____unknown";
	
	/** Donation Parameters */
	static final int	DONATION_INITIAL_INTERVAL = 8;
	static final int	DONATION_STANDARD_INTERVAL = 30;
	static final int	DONATION_AFTER_HAVING_DONATED_INTERVAL = 10000;
	
	/** Preference keys */ // could be set also in values/preference_strings.xml
	static final String	prefkey_mBrowseCatMode = "mBrowseCatMode";
	static final String	prefkey_mRendererMode = "mRendererMode";
	static final String	prefkey_mTheme = "mTheme";
	static final String	prefkey_mThemeProcessing = "mThemeProcessing";
	static final String	prefkey_mThemeBeingProcessed = "mThemeBeingProcessed";
	static final String	prefkey_mThemeHalfToneDone = "mThemeHalfToneDone";
	static final String	prefkey_mThemeEarthquakeDone = "mThemeEarthquakeDone";
	static final String	prefkey_mNavigatorPositionX = "mNavigatorPositionX";
	static final String prefkey_mNavigatorTargetPositionX = "mNavigatorTargetPositionX";
	static final String	prefkey_mNavigatorPositionY = "mNavigatorPositionY";
	static final String prefkey_mNavigatorTargetPositionY = "mNavigatorTargetPositionY";
	static final String prefkey_mLastAppUiActionTimestamp = "mLastAppUiActionTimestamp"; // everytime the app pauses this is updated
	static final String prefkey_mLastAppActionTimestamp = "mLastAppActionTimestamp"; // includes also playing song in service
	static final String prefkey_mPlaylistId = "mPlaylistId";
	static final String prefkey_mFullscreen = "mFullScreen"; // duplicated in resources... FIXME
	static final String prefkey_mControlsOnBottom = "mControlsOnBottom"; // duplicated in resources... FIXME
	static final String prefkey_mAppCreateCount = "mAppCreateCount";
	static final String prefkey_mAppCreateCountForDonation = "mAppCreateCountForDonation";
	static final String prefkey_mAppHasDonated = "mAppHasDonated";
	
	
}