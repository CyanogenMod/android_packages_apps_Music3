package org.abrantix.rockon.rockonnggl;

import android.database.Cursor;
import android.provider.MediaStore;

public class ContentProviderUnifier{
	
	/**
	 * Some tables (e.g. MediaStore.Audio.Playlist.Members) of the 
	 * internal content provider have the audio id field
	 * in different columns
	 * @param cursor
	 * @return
	 */
	static long	getAudioIdFromUnknownCursor(Cursor cursor){
		/* Playlist.Members */
		if(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID) != -1){
			return 
				cursor.getLong(
						cursor.getColumnIndexOrThrow(
								MediaStore.Audio.Playlists.Members.AUDIO_ID));
		} 
		/* Audio.Media / Genres.Members/... */
		else {
			return 
				cursor.getLong(
						cursor.getColumnIndexOrThrow(
								MediaStore.Audio.Media._ID));
		}
	}
	
	/**
	 * 
	 * @param cursor
	 * @return
	 */
	static String getAudioNameFromUnknownCursor(Cursor cursor)
	{
		/* Playlist.Members */
		if(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.TITLE) != -1){
			return 
				cursor.getString(
						cursor.getColumnIndexOrThrow(
								MediaStore.Audio.Playlists.Members.TITLE));
		} 
		/* Audio.Media / Genres.Members/... */
		else {
			return 
				cursor.getString(
						cursor.getColumnIndexOrThrow(
								MediaStore.Audio.Media.TITLE));
		}
	}
}