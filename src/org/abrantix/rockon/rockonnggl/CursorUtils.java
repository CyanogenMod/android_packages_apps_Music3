package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.sql.SQLData;
import java.util.LinkedList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;
import android.widget.Toast;

public class CursorUtils{

	private final String TAG = "CursorUtils";
	Context ctx;
	
	/**
	 * Constructor
	 * @param ctx
	 */
	CursorUtils(Context ctx){
		this.ctx = ctx;
	}

	/**
	 * Create Album Cursor
	 * @param playlistId
	 * @return
	 */
//	Cursor getAlbumFromAlbumId(long albumId, boolean preferArtistSorting){
	Cursor getAlbumFromAlbumId(long albumId){
		ContentResolver resolver = ctx.getContentResolver();
		Cursor c = null;
		if(DirectoryFilter.usesExternalStorage()) {
			c = resolver.query(
					MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					Constants.albumProjection, 
					MediaStore.Audio.Albums._ID + " = "+albumId, 
					null, 
					Constants.albumAlphabeticalSortOrder);
		} else {
			c = resolver.query(
					MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,
					Constants.albumProjection, 
					MediaStore.Audio.Albums._ID + " = "+albumId, 
					null, 
					Constants.albumAlphabeticalSortOrder);
		}
		return c;
		
//			return resolver.query(
//				MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//				Constants.albumProjection, 
//				MediaStore.Audio.Albums._ID + " = "+albumId, 
//				null, 
//				Constants.albumAlphabeticalSortOrder);
	}
	
	/**
	 * 
	 * @param artistId
	 * @param playlistId
	 * @return
	 */
	Cursor getAlbumListFromArtistId(long artistId, int playlistId)
	{
		switch(playlistId)
		{
		case Constants.PLAYLIST_ALL:
			return getAlbumListFromArtistId(artistId);
		}
		return null;
	}
	
	/**
	 * 
	 * @param artistId
	 * @return
	 */
	Cursor getAlbumListFromArtistId(long artistId)
	{
		ContentResolver resolver = ctx.getContentResolver();
		Cursor c = null;
		if(DirectoryFilter.usesExternalStorage()) {
			c = resolver.query(
					MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), 
					Constants.albumProjection, 
					null, 
					null, 
					Constants.artistAlbumsYearSortOrder);
		} else {
			c = resolver.query(
					MediaStore.Audio.Artists.Albums.getContentUri("internal", artistId), 
					Constants.albumProjection, 
					null, 
					null, 
					Constants.artistAlbumsYearSortOrder);
		}
		return c;
		
//		return resolver.query(
//				MediaStore.Audio.Artists.Albums.getContentUri("external", artistId), 
//				Constants.albumProjection, 
//				null, 
//				null, 
//				Constants.artistAlbumsYearSortOrder);
////				MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
	}
	
	/**
	 * 
	 * @param playlistId
	 * @return
	 */
	String test;
	Cursor getAlbumListFromPlaylist(int playlistId, boolean preferArtistSorting){
		
		/** ALL ALBUMS **/
		if(Constants.PLAYLIST_ALL == playlistId)
		{
			ContentResolver resolver = ctx.getContentResolver();
			Cursor c = null;
			if(DirectoryFilter.usesExternalStorage()) {
				if(preferArtistSorting) {
					c = resolver.query(
							MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
							Constants.albumProjection, 
							null, 
							null, 
							Constants.albumAlphabeticalSortOrderByArtist);
				} else {
					c = resolver.query(
							MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
							Constants.albumProjection, 
							null, 
							null, 
							Constants.albumAlphabeticalSortOrder);
				}
			} else {
				if(preferArtistSorting) {
					c = resolver.query(
							MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,
							Constants.albumProjection, 
							null, 
							null, 
							Constants.albumAlphabeticalSortOrderByArtist);
				} else {
					c = resolver.query(
							MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,
							Constants.albumProjection, 
							null, 
							null, 
							Constants.albumAlphabeticalSortOrder);
				}
			}
			return c;
		}
				
//				return resolver.query(
//					MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//					Constants.albumProjection, 
//					null, 
//					null, 
//					Constants.albumAlphabeticalSortOrder);
//			}
//		} 
//		/** 'NORMAL' PLAYLISTS */
//		else if(playlistId >= 0)
//		{
//			double start = System.currentTimeMillis();
//			ContentResolver resolver = ctx.getContentResolver();
//			/* get songs of this playlist */
//			Cursor playlistSongsCursor =
//				resolver.query(
//					MediaStore.Audio.Playlists.Members.getContentUri(
//							"external", 
//							playlistId),
//					Constants.playlistMembersProjection,
//					MediaStore.Audio.Playlists.Members.IS_MUSIC+"=1", // Filter Ringtones and other funny stuff
//					null,
//					Constants.playlistMembersAlbumSorting);
//			
//			/* get the albums in this playlist */
//			if(playlistSongsCursor != null)
//			{
//				LinkedList<Long>	albumList = new LinkedList<Long>();
//				Long				albumId;
//				for(int i=0; i<playlistSongsCursor.getCount(); i++){
//					playlistSongsCursor.moveToPosition(i);
//					albumId = 
//						playlistSongsCursor.getLong(
//								playlistSongsCursor.getColumnIndexOrThrow(
//										MediaStore.Audio.Playlists.Members.ALBUM_ID));
//					// small optimization -- check if albumId is different from the last 
//					if(!albumList.contains(albumId))
//						albumList.add(albumId);
//				}
//				playlistSongsCursor.close();
//				/* this playlist has no songs? */
//				if(albumList.size() <= 0)
//					return null;
//				/* create the selection string for querying the album contentprovider */
//				String	selection = null;
//				for(int i=0; i<albumList.size(); i++){
//					if(i==0){
//						selection = 
//							MediaStore.Audio.Albums._ID +
//							" = "+
//							albumList.get(i).toString();
//					} else {
//						selection += 
//							" OR "+
//							MediaStore.Audio.Albums._ID+
//							" = "+
//							albumList.get(i).toString();
//					}
//				}
//				/* query the album contentprovider */
//				Cursor albumCursor = null;
//				if(preferArtistSorting)
//					albumCursor =  resolver.query(
//							MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//							Constants.albumProjection, 
//							selection, 
//							null, 
//							Constants.albumAlphabeticalSortOrderByArtist);
//				else
//					albumCursor =  resolver.query(
//						MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//						Constants.albumProjection, 
//						selection, 
//						null, 
//						Constants.albumAlphabeticalSortOrder);
//				Log.i(TAG, " + "+(System.currentTimeMillis()-start));
//				return albumCursor;
//			} else {
//				return null;
//			}
//		}
//		/** GENRE PLAYLISTS*/
//		else if(playlistId <= Constants.PLAYLIST_GENRE_OFFSET &&
//				playlistId >= Constants.PLAYLIST_GENRE_OFFSET - Constants.PLAYLIST_GENRE_RANGE)
//		{
//			double start = System.currentTimeMillis();
//			ContentResolver resolver = ctx.getContentResolver();
//			/* get songs of this playlist */
//			Cursor genreSongsCursor =
//				resolver.query(
//					MediaStore.Audio.Genres.Members.getContentUri(
//							"external", 
//							Math.abs(playlistId - Constants.PLAYLIST_GENRE_OFFSET)),
//					Constants.genreMemberProjection,
//					MediaStore.Audio.Genres.Members.IS_MUSIC+"=1",
//					null,
////					null);
//					Constants.genreMembersAlbumSorting);
//			if(genreSongsCursor != null)
//			{
//				/* get the albums in this playlist */
//				LinkedList<Long>	albumList = new LinkedList<Long>();
//				Long				albumId;
//				for(int i=0; i<genreSongsCursor.getCount(); i++){
//					genreSongsCursor.moveToPosition(i);
//					albumId = 
//						genreSongsCursor.getLong(
//								genreSongsCursor.getColumnIndexOrThrow(
//										MediaStore.Audio.Genres.Members.ALBUM_ID));
//					// small optimization -- check if albumId is different from the last 
//					Log.i(TAG, String.valueOf(
//							genreSongsCursor.getLong(
//									genreSongsCursor.getColumnIndexOrThrow(
//											MediaStore.Audio.Genres.Members.ALBUM_ID))));
//					if(!albumList.contains(albumId))
//						albumList.add(albumId);
//				}
//				genreSongsCursor.close();
//				/* this genre has no songs? */
//				if(albumList.size() <= 0)
//					return null;
//				/* create the selection string for querying the album contentprovider */
//				String	selection = null;
//				for(int i=0; i<albumList.size(); i++){
//					if(i==0){
//						selection = 
//							MediaStore.Audio.Albums._ID +
//							" = "+
//							albumList.get(i).toString();
//					} else {
//						selection += 
//							" OR "+
//							MediaStore.Audio.Albums._ID+
//							" = "+
//							albumList.get(i).toString();
//					}
//				}
//				Log.i(TAG, "SELECT: "+selection);
//				/* query the album contentprovider */
//				Cursor albumCursor = null;
//				if(preferArtistSorting)
//					albumCursor =  resolver.query(
//							MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//							Constants.albumProjection, 
//							selection, 
//							null, 
//							Constants.albumAlphabeticalSortOrderByArtist);
//				else
//					albumCursor =  resolver.query(
//						MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//						Constants.albumProjection, 
//						selection, 
//						null, 
//						Constants.albumAlphabeticalSortOrder);
//				Log.i(TAG, " + "+(System.currentTimeMillis()-start));
//				return albumCursor;
//			}
//			else
//			{
//				// TODO: reset playlist to play all
//				return null;
//			}
		/** NOT RECOGNIZED */
		else {
			return null;
		}
	}
	
	Cursor getArtistListFromPlaylist(int playlistId)
	{
		/** ALL ALBUMS **/
		if(Constants.PLAYLIST_ALL == playlistId)
		{
			ContentResolver resolver = ctx.getContentResolver();
			Cursor c = null;
			if(DirectoryFilter.usesExternalStorage()) {
				c = resolver.query(
						MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
						Constants.artistProjection, 
						null, 
						null, 
						Constants.artistAlphabeticalSortOrder);
			} else {
				c = resolver.query(
						MediaStore.Audio.Artists.INTERNAL_CONTENT_URI,
						Constants.artistProjection, 
						null, 
						null, 
						Constants.artistAlphabeticalSortOrder);
			}
			return c;
						
//			return resolver.query(
//					MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
//					Constants.artistProjection, 
//					null, 
//					null, 
//					Constants.artistAlphabeticalSortOrder);
		} 
		else
		{
			return null;
		}
	}
	
	/**
	 * getGenres
	 * @return
	 */
	Cursor	getGenres(){
		try{
			Cursor c = null;
			if(DirectoryFilter.usesExternalStorage()) {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
						Constants.genreProjection,
						null,
						null,
						Constants.genreAlphabeticalSorting);
			} else {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Genres.INTERNAL_CONTENT_URI,
						Constants.genreProjection,
						null,
						null,
						Constants.genreAlphabeticalSorting);
			}
			return c;
		} catch(SQLiteException e) {
			e.printStackTrace();
			return null;
		}
		
//		Cursor genreList = ctx.getContentResolver().query(
//		MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
//		Constants.genreProjection,
//		null,
//		null,
//		Constants.genreAlphabeticalSorting);

//		return genreList;
	}
	
	/**
	 * getPlaylists
	 * @return
	 */
	Cursor	getPlaylists(){
		try{
			Cursor c = null;
			if(DirectoryFilter.usesExternalStorage()) {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
						Constants.playlistProjection,
						null,
						null,
						Constants.playlistAlphabeticalSorting);
			} else {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI,
						Constants.playlistProjection,
						null,
						null,
						Constants.playlistAlphabeticalSorting);
			}
			return c;
		} catch(SQLiteException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * All songs ordered by the title
	 * @return
	 */
	Cursor getAllSongsListOrderedBySongTitle()
	{
		return getSongListOrderedBySongTitle(null);
	}
	
	/**
	 * songs list ordered by song title with a given constraint
	 * @param constraint
	 * @return
	 */
	Cursor getSongListOrderedBySongTitle(String constraint)
	{
		Cursor c = null;
		if(DirectoryFilter.usesExternalStorage()) {
			String sqlDirFilter = DirectoryFilter.getFolderSqlStatement(ctx, DirectoryFilter.EXTERNAL_STORAGE);
			if(sqlDirFilter != null) {
				if(constraint != null) {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							Constants.songProjection,
							constraint + " AND (" + sqlDirFilter + ")",
							null,
							Constants.songListTitleSorting);
				} else {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							Constants.songProjection,
							sqlDirFilter,
							null,
							Constants.songListTitleSorting);
				}
			} else {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						Constants.songProjection,
						constraint,
						null,
						Constants.songListTitleSorting);
			}
		} else {
			String sqlDirFilter = DirectoryFilter.getFolderSqlStatement(ctx, DirectoryFilter.INTERNAL_STORAGE);
			if(sqlDirFilter != null) {
				if(constraint != null) {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
							Constants.songProjection,
							constraint + " AND (" + sqlDirFilter + ")",
							null,
							Constants.songListTitleSorting);
				} else {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
							Constants.songProjection,
							sqlDirFilter,
							null,
							Constants.songListTitleSorting);
				}
			} else {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
						Constants.songProjection,
						constraint,
						null,
						Constants.songListTitleSorting);
			}
		}
		return c;
	}
	
	
	/**
	 * Create song list cursor
	 * from albumId
	 * and playlistId
	 */
	Cursor	getSongListCursorFromAlbumId(long albumId, int playlistId){
		return
			getSongsFromPlaylistWithConstraint(
				playlistId, 
				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId+
					" AND "+
					MediaStore.Audio.Media.IS_MUSIC + "=1",
				true);
//		ContentResolver resolver = ctx.getContentResolver();
//		Cursor			songList = resolver.query(
//				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//				Constants.songProjection,
//				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId,
//				null,
//				Constants.songListNumericalSorting);
//		// TODO: verify that all songs belong to the desired playlist
//		return songList;	
	}
	
	/**
	 * Create song list cursor
	 * from albumId
	 * and playlistId
	 */
	Cursor	getSongListCursorFromAlbumAndArtistId(long albumId, long artistId, int playlistId){
		return
			getSongsFromPlaylistWithConstraint(
				playlistId, 
				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId+
					" AND "+
					MediaStore.Audio.Media.ARTIST_ID + " = " + artistId+
					" AND "+
					MediaStore.Audio.Media.IS_MUSIC + "=1",
				true);
//		ContentResolver resolver = ctx.getContentResolver();
//		Cursor			songList = resolver.query(
//				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//				Constants.songProjection,
//				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId,
//				null,
//				Constants.songListNumericalSorting);
//		// TODO: verify that all songs belong to the desired playlist
//		return songList;
		
	}
	
	/**
	 * Create song list cursor
	 * from artistId
	 */
	Cursor	getSongListCursorFromArtistId(long artistId, int playlistId){
		return
			getSongsFromPlaylistWithConstraint(
				playlistId, 
					MediaStore.Audio.Media.ARTIST_ID + " = " + artistId+
					" AND "+
					MediaStore.Audio.Media.IS_MUSIC + "=1",
				true);
//		ContentResolver resolver = ctx.getContentResolver();
//		Cursor			songList = resolver.query(
//				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//				Constants.songProjection,
//				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId,
//				null,
//				Constants.songListNumericalSorting);
//		// TODO: verify that all songs belong to the desired playlist
//		return songList;
		
	}
	
	/**
	 * Create song list cursor
	 * from artist name
	 */
	Cursor	getSongListCursorFromArtistName(String artistName, int playlistId){
		return
			getSongsFromPlaylistWithConstraint(
				playlistId, 
				MediaStore.Audio.Media.ARTIST + " = '" + artistName+ "'" +
					" AND "+
					MediaStore.Audio.Media.IS_MUSIC + "=1",
				true);
//		ContentResolver resolver = ctx.getContentResolver();
//		Cursor			songList = resolver.query(
//				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//				Constants.songProjection,
//				MediaStore.Audio.Media.ALBUM_ID + " = " + albumId,
//				null,
//				Constants.songListNumericalSorting);
//		// TODO: verify that all songs belong to the desired playlist
//		return songList;	
	}
	
	Cursor	getAllSongsFromPlaylist(int playlistId){
		return getSongsFromPlaylistWithConstraint(playlistId, MediaStore.Audio.Media.IS_MUSIC + "=1", true);
//		switch(playlistId){
//		case Constants.PLAYLIST_ALL:
//			Cursor songList = ctx.getContentResolver().query(
//					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//					Constants.songProjection,
//					null,
//					null,
//					Constants.songListAlbumAndNumericalSorting);
//			return songList;
//		}
//		return null;
	}
	
	
	Cursor	getSongsFromPlaylistWithConstraint(int playlistId, String constraint, boolean usePlaylistSorting){
		String sqlDirFilter = null;
		if(DirectoryFilter.usesExternalStorage())
			sqlDirFilter = DirectoryFilter.getFolderSqlStatement(ctx, DirectoryFilter.EXTERNAL_STORAGE);
		else
			sqlDirFilter = DirectoryFilter.getFolderSqlStatement(ctx, DirectoryFilter.INTERNAL_STORAGE);
		String finalConstraint = null;
		if(sqlDirFilter != null){
			if(constraint != null)
				finalConstraint = constraint  + " AND (" + sqlDirFilter + ")";
			else
				finalConstraint = sqlDirFilter;
		} else {
			finalConstraint = constraint;
		}
		Cursor c = null;
		/* ALL SONGS */
		if(playlistId == Constants.PLAYLIST_ALL)
		{
			if(DirectoryFilter.usesExternalStorage()) {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
						Constants.songProjection,
						finalConstraint,
						null,
						Constants.songListAlbumAndNumericalSorting);
			} else {
				c = ctx.getContentResolver().query(
						MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
						Constants.songProjection,
						finalConstraint,
						null,
						Constants.songListAlbumAndNumericalSorting);
			}
			return c;
		}
		/* 'NORMAL' PLAYLISTS */
		else if(playlistId >= 0)
		{
			String sorting = null;
			if(usePlaylistSorting)
				sorting = Constants.songListPlaylistSorting;
			else
				sorting = Constants.songListAlbumAndNumericalSorting;
			
			try {
				String lastConstraint;
				if(finalConstraint != null)
					lastConstraint = finalConstraint
									+ " AND "
									+ MediaStore.Audio.Playlists.Members.IS_MUSIC + "=1";
				else
					lastConstraint = MediaStore.Audio.Playlists.Members.IS_MUSIC + "=1";
				if(DirectoryFilter.usesExternalStorage()) {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Playlists.Members.getContentUri(
									"external", 
									playlistId),
							Constants.playlistMembersProjection,
							lastConstraint,
							null,
							sorting);
				} else {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Playlists.Members.getContentUri(
									"internal", 
									playlistId),
							Constants.playlistMembersProjection,
							lastConstraint,
							null,
							sorting);
				}
			} catch(SQLiteException e) {
				e.printStackTrace();
			}
			return c;	
		}
		/* GENRE PLAYLIST */
		else if(playlistId <= Constants.PLAYLIST_GENRE_OFFSET &&
				playlistId > Constants.PLAYLIST_GENRE_OFFSET - Constants.PLAYLIST_GENRE_RANGE)
		{
			String lastConstraint;
			if(finalConstraint != null)
				lastConstraint = finalConstraint
								+ " AND "
								+ MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";
			else
				lastConstraint = MediaStore.Audio.Genres.Members.IS_MUSIC + "=1";
			try {
				if(DirectoryFilter.usesExternalStorage()) {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Genres.Members.getContentUri(
									"external", 
									Math.abs(playlistId - Constants.PLAYLIST_GENRE_OFFSET)),
							Constants.genreMemberProjection,
							lastConstraint,
							null,
							Constants.songListAlbumAndNumericalSorting);
				} else {
					c = ctx.getContentResolver().query(
							MediaStore.Audio.Genres.Members.getContentUri(
									"internal", 
									Math.abs(playlistId - Constants.PLAYLIST_GENRE_OFFSET)),
							Constants.genreMemberProjection,
							lastConstraint,
							null,
							Constants.songListAlbumAndNumericalSorting);
				}
			} catch(SQLiteException e) {
				e.printStackTrace();
			}
			
			return c;
		}
		return null;
	}
	
	/**
	 * getSongListCursorFromSongList
	 * @param list
	 * @param position
	 * @return
	 */
	Cursor	getSongListCursorFromSongList(long[] list, int position, int limit){
		if(list != null && list.length > 0)
		{
			Cursor[]	cursor = 
				new Cursor[Math.min(list.length-position, position + limit + 1)];
			boolean external = DirectoryFilter.usesExternalStorage(); 
			String	constraint;
			for(int i = position; i < Math.min(list.length, position + limit + 1) ; i++){
				Log.i(TAG, ""+list[i]);
	//			if(i != position)
	//				constraint += " OR ";
				constraint = 
					MediaStore.Audio.Media._ID+
					"="+
					String.valueOf(list[i]);
				if(external) {
					cursor[i-position] = 
						ctx.getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
							Constants.songProjection, 
							constraint, 
							null, 
							null);
				} else {
					cursor[i-position] = 
						ctx.getContentResolver().query(
							MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
							Constants.songProjection, 
							constraint, 
							null, 
							null);
				}
			}
			MergeCursor playQueueCursor = null;
			if(cursor != null)
				playQueueCursor = new MergeCursor(cursor);
			return playQueueCursor;
		} else {
			return null;
		}
	}
	
	/**
	 * getNextAudioId
	 * @param audioId
	 * @param shuffle
	 * @param playlistId
	 * @return
	 */
	long	getNextPrevAudioId(int direction, long audioId, int albumId, int shuffle, int playlistId){
		long nextAudioId = -1;
		if(shuffle == Constants.SHUFFLE_NORMAL || shuffle == Constants.SHUFFLE_AUTO){
			/* it is the same whether is next or previous */
			Cursor cursor = getAllSongsFromPlaylist(playlistId);
			int retries = 0;
			nextAudioId = audioId;
			while(nextAudioId == audioId && retries <= 20){
				cursor.moveToPosition((int) (Math.random() * (cursor.getCount()-1)));
				nextAudioId =
					ContentProviderUnifier.
						getAudioIdFromUnknownCursor(cursor);
			}
			if(retries == 20)
				nextAudioId = -1;
			cursor.close();
		} else if(shuffle == Constants.SHUFFLE_NONE){
			Cursor cursor = getSongListCursorFromAlbumId(albumId, playlistId);
			int i = 0;
			/* find our track number */
			for(i=0; i<cursor.getCount(); i++){
				cursor.moveToPosition(i);
				if(audioId == 
					ContentProviderUnifier.
						getAudioIdFromUnknownCursor(cursor))
					break;
			}
			if(direction == Constants.FIND_NEXT){
				/* not the end of the album */
				if(i<cursor.getCount()-1){
					cursor.moveToPosition(i+1);
					nextAudioId = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(cursor);
					cursor.close();
				}
				/* last song of the album - get the first from the next album */
				else{
					cursor.close();
					Cursor newAlbumSongCursor = 
						getSongListCursorFromAlbumId(
							getNextPrevAlbumId(
									direction,
									albumId, 
									playlistId, 
									shuffle), 
							playlistId);
					newAlbumSongCursor.moveToFirst();
					nextAudioId = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(newAlbumSongCursor);
					newAlbumSongCursor.close();
				
				}
			} else if(direction == Constants.FIND_PREV){
				/* not the first song of the album */
				if(i > 0){
					cursor.moveToPosition(i-1);
					nextAudioId = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(cursor);
					cursor.close();
				}
				/* first song of the album - get the last from the previous album */
				else{
					cursor.close();
					Cursor newAlbumSongCursor = 
						getSongListCursorFromAlbumId(
							getNextPrevAlbumId(
									direction,
									albumId, 
									playlistId, 
									shuffle), 
							playlistId);
					newAlbumSongCursor.moveToLast();
					nextAudioId = 
						ContentProviderUnifier.
							getAudioIdFromUnknownCursor(newAlbumSongCursor);
					newAlbumSongCursor.close();
				
				}

			}
				
			
		}
		return nextAudioId;
	}

	/**
	 * get the next album id
	 * @param albumId
	 * @param playlistId
	 * @param shuffle
	 * @return
	 */
	long getNextPrevAlbumId(int direction, long albumId, int playlistId, int shuffle){
		long nextAlbumId = -1;
		if(shuffle == Constants.SHUFFLE_NONE){
			Log.i(TAG, "Fetching next album - SHUFFLE_NONE");
			Cursor cursor = getAlbumListFromPlaylist(playlistId, true);
			int i = 0;
			for(i=0; i<cursor.getCount(); i++){
				cursor.moveToPosition(i);
				if(albumId == 
					cursor.getLong(
							cursor.getColumnIndexOrThrow(
									MediaStore.Audio.Albums._ID)))
					break;
			}
			if(direction == Constants.FIND_NEXT){
				if(i<cursor.getCount()-1)
					cursor.moveToPosition(i+1);
				else // last album return to first
					cursor.moveToPosition(0);
			} 
			else if(direction == Constants.FIND_PREV){
				if(i>0)
					cursor.moveToPosition(i-1);
				else
					cursor.moveToLast();
			}
				
			nextAlbumId = 
				cursor.getLong(
						cursor.getColumnIndexOrThrow(
								MediaStore.Audio.Albums._ID));
			cursor.close();
		} 
		/* direction does not matter in shuffle mode */
		else if(shuffle == Constants.SHUFFLE_NORMAL || shuffle == Constants.SHUFFLE_AUTO){
			Cursor cursor = getAlbumListFromPlaylist(playlistId, true);
			nextAlbumId = albumId;
			int retries = 0;
			while(nextAlbumId == albumId && retries < 10){
				cursor.moveToPosition((int) (Math.random() * (cursor.getCount()-1)));
				nextAlbumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
				retries++;
			}
			cursor.close();
		}
		return nextAlbumId;
	}
	
	/**
	 * create a playlist entry
	 * @param name
	 * @return
	 */
	public boolean createPlaylist(String name)
	{
		try
		{
			ContentValues values = new ContentValues();
			values.put(
					MediaStore.Audio.Playlists.NAME,
					name);
			ContentResolver res = ctx.getContentResolver();
			if(DirectoryFilter.usesExternalStorage()) {
				res.insert(
					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, 
					values);
			} else {
				res.insert(
						MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, 
						values);
			}
//			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
//				res.insert(
//					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, 
//					values);
//			else 
//				res.insert(
//					MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, 
//					values);
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 
	 * @param songId
	 * @return
	 */
	public boolean deleteSong(int songId)
	{
		try
		{
			long[] songIdList = {songId};
			deleteTracks(songIdList);
			return true;
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * XXX Borrowed from Android open source project
	 * Removes several tracks
	 * @param list
	 */
    public void deleteTracks(long [] list) {
        
        String [] cols = new String [] { 
        		MediaStore.Audio.Media._ID, 
                MediaStore.Audio.Media.DATA, 
                MediaStore.Audio.Media.ALBUM_ID };
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < list.length; i++) {
            where.append(list[i]);
            if (i < list.length - 1) {
                where.append(",");
            }
        }
        where.append(")");
        
        Cursor c = null;
        boolean external = DirectoryFilter.usesExternalStorage(); 
        if(external) {
        	c = ctx.getContentResolver().query(
            		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
            		cols,
                    where.toString(), 
                    null, 
                    null);
        } else {
        	c = ctx.getContentResolver().query(
            		MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
            		cols,
                    where.toString(), 
                    null, 
                    null); 
        }

        if (c != null) 
        {
            // step 1: remove selected tracks from the current playlist, as well
            // as from the album art cache
            c.moveToFirst();
            while (! c.isAfterLast()) {
                // remove from current playlist
                long id = c.getLong(0);
//                    sService.removeTrack(id);
                // remove from album art cache
                long artIndex = c.getLong(2);
//                    synchronized(sArtCache) {
//                        sArtCache.remove(artIndex);
//                    }
                c.moveToNext();
            }
            
            // step 2: remove selected tracks from the database
            if(external)
            	ctx.getContentResolver().delete(
            		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
            		where.toString(), 
            		null);
            else
            	ctx.getContentResolver().delete(
            		MediaStore.Audio.Media.INTERNAL_CONTENT_URI, 
            		where.toString(), 
            		null);

            // step 3: remove files from card
            c.moveToFirst();
            while (! c.isAfterLast()) {
                String name = c.getString(1);
                File f = new File(name);
                try {  // File.delete can throw a security exception
                    if (!f.delete()) {
                        // I'm not sure if we'd ever get here (deletion would
                        // have to fail, but no exception thrown)
                        Log.e(TAG, "Failed to delete file " + name);
                    }
                    c.moveToNext();
                } catch (SecurityException ex) {
                    c.moveToNext();
                }
            }
            c.close();
        }

        // We deleted a number of tracks, which could affect any number of things
        // in the media content domain, so update everything.
        ctx.getContentResolver().notifyChange(Uri.parse("content://media"), null);
    }

	
	/**
	 * 
	 * @param playlistId
	 * @return
	 */
	public boolean deletePlaylist(int playlistId)
	{
		if(clearPlaylist(playlistId))
		{
			try{
				if(DirectoryFilter.usesExternalStorage())
					ctx.getContentResolver().delete(
		        		MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, 
		        		MediaStore.Audio.Playlists._ID + "=" + playlistId, 
		        		null);
				else
					ctx.getContentResolver().delete(
		        		MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, 
		        		MediaStore.Audio.Playlists._ID + "=" + playlistId, 
		        		null);
				return true;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
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
	public boolean clearPlaylist(int playlistId)
	{
		try
		{
			if(DirectoryFilter.usesExternalStorage()) {
				Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
		        ctx.getContentResolver().delete(uri, null, null);
			} else {
		        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("internal", playlistId);
		        ctx.getContentResolver().delete(uri, null, null);
			}
	        return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 
	 * @param genreId
	 * @return
	 */
	public boolean deleteGenre(int genreId)
	{
		clearGenre(genreId); // this does not work -- cannot alter mp3?
		try{
			if(DirectoryFilter.usesExternalStorage())
				ctx.getContentResolver().delete(
	        		MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, 
	        		MediaStore.Audio.Media._ID + "=" + genreId, 
	        		null);
			else
				ctx.getContentResolver().delete(
	        		MediaStore.Audio.Genres.INTERNAL_CONTENT_URI, 
	        		MediaStore.Audio.Media._ID + "=" + genreId, 
	        		null);
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 
	 * @param genreId
	 * @return
	 */
	public boolean clearGenre(int genreId)
	{
		try
		{
			if(DirectoryFilter.usesExternalStorage()) {
				Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId);
		        ctx.getContentResolver().delete(uri, null, null);
			} else {
		        Uri uri = MediaStore.Audio.Genres.Members.getContentUri("internal", genreId);
		        ctx.getContentResolver().delete(uri, null, null);
			}
	        return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * getPlaylistId
	 * @param playlistName
	 * @return
	 */
	public long getPlaylistIdFromName(String playlistName)
	{
		ContentResolver res = ctx.getContentResolver();
		Cursor c = null;
		if(DirectoryFilter.usesExternalStorage()) {
			c = res.query(
					MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, 
					Constants.playlistProjection, 
					MediaStore.Audio.Playlists.NAME + "='" + playlistName + "'", 
					null, 
					null);
		} else {
			c = res.query(
					MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, 
					Constants.playlistProjection, 
					MediaStore.Audio.Playlists.NAME + "='" + playlistName + "'", 
					null, 
					null);
		}
//		try{
		c.moveToFirst();
		return c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
//		} catch(Exception e) {
//			return -1;
//		}
	}
	
	/**
	 * add songs to a playlist
	 * @param playlistName
	 * @param songIds
	 * @return
	 */
	public boolean addSongsToPlaylist(long playlistId, long[] ids)
	{
        if (ids == null) {
            // this shouldn't happen (the menuitems shouldn't be visible
            // unless the selected item represents something playable
            Log.e(TAG, "ListSelection null");
            return false;
        } else {
            int size = ids.length;
            ContentValues values [] = new ContentValues[size];
            ContentResolver resolver = ctx.getContentResolver();
            // need to determine the number of items currently in the playlist,
            // so the play_order field can be maintained.
            String[] cols = new String[] {
                    "count(*)"
            };
            Cursor cur;
            Uri uri;
            if(DirectoryFilter.usesExternalStorage()) {
	            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
	            cur = resolver.query(uri, cols, null, null, null);
            } else {
            	uri = MediaStore.Audio.Playlists.Members.getContentUri("internal", playlistId);
            	cur = resolver.query(uri, cols, null, null, null);
            }
            cur.moveToFirst();
            int base = cur.getInt(0);
            cur.close();

            for (int i = 0; i < size; i++) {
                values[i] = new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + i));
                values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, ids[i]);
            }
            resolver.bulkInsert(uri, values);
            return true;
        }
	}
	
	/**
	 * 
	 * @param artistCursor
	 * @param artistAlbumHelperArray
	 * @param playlistId
	 * @return
	 */
	int oArtistColumnIndex;
	int oAlbumIdColumnIndex = -1;
	public boolean fillArtistAlbumHelperArray(
			Cursor artistCursor, 
			ArtistAlbumHelper[] artistAlbumHelperArray)
	{
		/** Sanity check */
		if(!artistCursor.isClosed() &&
			artistCursor.getCount() == artistAlbumHelperArray.length)
		{
			Cursor albumCursor;
			oArtistColumnIndex = artistCursor.getColumnIndex(MediaStore.Audio.Artists._ID);
			for(int i=0; i<artistCursor.getCount(); i++)
			{
				try
				{
					artistCursor.moveToPosition(i);
					if(artistAlbumHelperArray[i] == null)
						artistAlbumHelperArray[i] = new ArtistAlbumHelper();
					artistAlbumHelperArray[i].artistId = artistCursor.getString(oArtistColumnIndex);
					// sometimes we obtain an artistId of -1;
					albumCursor = getAlbumListFromArtistId(artistCursor.getLong(oArtistColumnIndex));
				}
				catch(IllegalStateException e)
				{
					e.printStackTrace();
					albumCursor = null;
				}
				catch(CursorIndexOutOfBoundsException e)
				{
					e.printStackTrace();
					albumCursor = null;
				}
				if(albumCursor != null)
				{
					if(albumCursor.getCount() > 0)
					{
						if(oAlbumIdColumnIndex == -1 )
							oAlbumIdColumnIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
						albumCursor.moveToFirst();
						artistAlbumHelperArray[i].albumId = albumCursor.getString(oAlbumIdColumnIndex);
//						Log.i(TAG, "Album Id:"+artistAlbumHelperArray[i].albumId);
					}
					else
					{
						artistAlbumHelperArray[i].albumId = "";
					}
					albumCursor.close();
				}
				else
				{
					artistAlbumHelperArray[i].albumId = "";	
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}
	
}