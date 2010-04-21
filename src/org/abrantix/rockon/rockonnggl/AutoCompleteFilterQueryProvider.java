package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.FilterQueryProvider;

class AutoCompleteFilterQueryProvider implements FilterQueryProvider{

	Context	mContext;
	int 	mPlaylistId;
	
	AutoCompleteFilterQueryProvider(Context context, int playlistId){
		mPlaylistId = playlistId;
		mContext = context;
	}
	
	@Override
	public Cursor runQuery(CharSequence constraint) {
		String whereClause = 
			MediaStore.Audio.Media.TITLE+" LIKE '%"+constraint+"%'"
			+" OR "+
			MediaStore.Audio.Media.ARTIST+" LIKE '%"+constraint+"%'";
		Log.i("SEARCH", whereClause);

		/* cursor */
		Cursor songsCursor = new CursorUtils(mContext.getApplicationContext()).
		getSongsFromPlaylistWithConstraint(mPlaylistId, whereClause, false);
		return songsCursor;
	}
}