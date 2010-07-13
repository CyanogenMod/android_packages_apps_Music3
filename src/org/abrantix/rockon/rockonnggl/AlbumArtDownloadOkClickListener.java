package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class AlbumArtDownloadOkClickListener implements OnClickListener{
    
	final	String	TAG = "AlbumArtDownloadOkClickListener";
	
	private Context				mContext;
	private boolean				mDownloading = false;
	private ProgressDialog		mProgressDialog;
	private AlbumArtImporter	mAlbumArtImporter;
	
	AlbumArtDownloadOkClickListener(Context context){
		this.mContext = context;
	}
	
	public boolean isDownloading(){
		return mDownloading;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(which == DialogInterface.BUTTON_POSITIVE)
		{
			Log.i(TAG, "Positive button");
			mArtDownloadTrigger.sendEmptyMessageDelayed(Constants.GET_INET_ART_TOO, 500);
		}
		else
		{
			Log.i(TAG, "Negative button");
			mArtDownloadTrigger.sendEmptyMessageDelayed(Constants.GET_LOCAL_ART_ONLY, 500);
		}
	}
	
	/* Handler for triggering art download */
	public Handler mArtDownloadTrigger = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == Constants.GET_INET_ART_TOO)
			{
				mDownloading = true;
				
				/* show a progress dialog to give feedback to the user */
				mProgressDialog = new ProgressDialog(mContext);
				mProgressDialog.setTitle(R.string.art_download_progress_title);
				mProgressDialog.setMessage(mContext.getResources().getString(
						R.string.art_download_progress_initial_message));
				mProgressDialog.setButton(
						mContext.getString(R.string.art_download_cancel), 
						cancelDownloadClickListener);
				mProgressDialog.show();
								
				/* start fetching album art */
				mAlbumArtImporter = new AlbumArtImporter(mContext, mArtDownloadUpdateHandler, true);
				mAlbumArtImporter.getAlbumArt();							
			}
			else
			{
				/* start fetching album art */
				mAlbumArtImporter = new AlbumArtImporter(mContext, null, false);
				mAlbumArtImporter.getAlbumArt();
			}
		}
	};
	
	
	// Handler to receive messages from the AlbumArtDownloader class thread
	public Handler	mArtDownloadUpdateHandler = new Handler(){
		public void handleMessage(Message msg){
			if(mProgressDialog.isShowing()){
				// Last Message ?
				if(msg.getData()
						.getString(Constants.ALBUM_ART_DOWNLOAD_UI_UPDATE_DONE_IPC_MSG)
						!= null)
				{
					try{
						mProgressDialog.dismiss();
					} catch(IllegalArgumentException e) {
						e.printStackTrace();
					}
					stopArtDownload();
				}
				// Still going
				else
					mProgressDialog.setMessage(
						msg.getData()
							.getString(Constants.ALBUM_ART_DOWNLOAD_UI_UPDATE_IPC_MSG));
			}
		}
	};
	
	// function for resuming download after a rotation/app close

	
	private OnClickListener cancelDownloadClickListener = new OnClickListener(){

		@Override
		public void onClick(DialogInterface dialog, int which) {
			stopArtDownload();
		}
		
	};
	
	public void stopArtDownload(){
		mAlbumArtImporter.stopAlbumArt();
		try{
			mProgressDialog.dismiss();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		mDownloading = false;
	}
	
}