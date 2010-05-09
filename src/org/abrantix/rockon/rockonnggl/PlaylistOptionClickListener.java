package org.abrantix.rockon.rockonnggl;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

class PlaylistOptionClickListener implements android.content.DialogInterface.OnClickListener{
	private final String TAG = "PlaylistOptionClickListener";
	
	int					mPlaylistId;
	Handler				mPlaylistOptionClickHandler;
	
	public PlaylistOptionClickListener(
			int		playlistId,
			Handler playlistOptionClickHandler) 
	{
		mPlaylistOptionClickHandler = playlistOptionClickHandler;
		mPlaylistId = playlistId;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		
		Message msg = new Message();
		msg.what = which;
		msg.arg1 = mPlaylistId;
		mPlaylistOptionClickHandler.sendMessage(msg); 
		dialog.dismiss();
	}
}
