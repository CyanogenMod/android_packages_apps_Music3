package org.abrantix.rockon.rockonnggl;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

class SongOptionClickListener implements android.content.DialogInterface.OnClickListener{
	private final String TAG = "PlaylistOptionClickListener";
	
	int					mSongId;
	Handler				mSongOptionClickHandler;
	
	public SongOptionClickListener(
			int		songId,
			Handler songOptionClickHandler) 
	{
		mSongOptionClickHandler = songOptionClickHandler;
		mSongId = songId;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		Message msg = new Message();
		msg.what = which;
		msg.arg1 = mSongId;
		mSongOptionClickHandler.sendMessage(msg); 
		dialog.dismiss();
	}
}
