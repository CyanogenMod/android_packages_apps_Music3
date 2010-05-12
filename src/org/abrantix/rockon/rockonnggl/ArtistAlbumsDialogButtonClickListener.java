package org.abrantix.rockon.rockonnggl;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

public class ArtistAlbumsDialogButtonClickListener implements android.content.DialogInterface.OnClickListener{

	long			mArtistId;
	Handler 		mArtistAlbumAlbumListDialogOverallOptionsHandler;
	RockOnRenderer	mRockOnRenderer;
	
	public ArtistAlbumsDialogButtonClickListener(
			long				artistId,
			Handler 		artistAlbumsDialogOptionHandler,
			RockOnRenderer 	rockonRenderer) 
	{
		mArtistId = artistId;
		mArtistAlbumAlbumListDialogOverallOptionsHandler = artistAlbumsDialogOptionHandler;
		mRockOnRenderer = rockonRenderer;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		Message msg = new Message();
		msg.what = 0;
		msg.obj = new Long(mArtistId);
		switch(which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			msg.arg1 = Constants.NOW;
			break;
		case DialogInterface.BUTTON_NEUTRAL:
			msg.arg1 = Constants.LAST;
			break;
		}
//		mRockOnCubeRenderer.reverseClickAnimation();
		mRockOnRenderer.reverseClickAnimation();
		mArtistAlbumAlbumListDialogOverallOptionsHandler.sendMessageDelayed(msg, Constants.CLICK_ACTION_DELAY);		
	}
}
