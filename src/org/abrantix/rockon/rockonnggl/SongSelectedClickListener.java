package org.abrantix.rockon.rockonnggl;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

class SongSelectedClickListener implements android.content.DialogInterface.OnClickListener{
	private final String TAG = "SongSelectedClickListener";
	Cursor	mSongListCursor;
	Handler	mSongItemSelectedHandler;
	
	public SongSelectedClickListener(Handler songClickHandler, Cursor songListCursor) {
		mSongItemSelectedHandler = songClickHandler;
		mSongListCursor = songListCursor;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Log.i(TAG, "Item "+which+" clicked!");
		if (true)
			return;
//			Cursor cursor = ((CursorAdapter) arg0.getAdapter()).getCursor();
		mSongListCursor.moveToPosition(which);
		int songId = (int)
			ContentProviderUnifier.
				getAudioIdFromUnknownCursor(mSongListCursor);
//			mSongListCursor.getInt(
//				mSongListCursor.getColumnIndexOrThrow(
//						MediaStore.Audio.Media._ID));
		mSongItemSelectedHandler.sendEmptyMessageDelayed(
				songId, 
				Constants.CLICK_ACTION_DELAY);
		dialog.dismiss();
	}
}
