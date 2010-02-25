package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;


public class ThemeChangeClickListener implements OnClickListener{
    
	final	String	TAG = "AlbumArtDownloadOkClickListener";
	
	private Context				mContext;
	private int					mTheme;
	private boolean				mProcessing = false;
	private boolean				mInTheBackground = false;
	private ProgressDialog		mProgressDialog;
	private Handler				mReloadUiHandler;
	private AlbumArtProcessor	mAlbumArtProcessor;
	
	ThemeChangeClickListener(Context context, int theme, Handler reloadUiHandler){
		this.mContext = context;
		this.mTheme = theme;
		this.mReloadUiHandler = reloadUiHandler;
	}
	
	public boolean isProcessing(){
		return mProcessing;
	}
	
	public boolean isInTheBackground(){
		return mInTheBackground;
	}
	
	public int getTheme()
	{
		return mTheme;
	}
	
	public void changeTheme(int theme, boolean autostart)
	{
		// TODO: stop processing thread
		mTheme = theme;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		mArtProcessingTrigger.sendEmptyMessageDelayed(0, 250);
	}
	
	/* Handler for triggering art download */
	public Handler mArtProcessingTrigger = new Handler(){
		@Override
		public void handleMessage(Message msg){
			mProcessing = true;
			
			/* show a progress dialog to give feedback to the user */
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle(R.string.art_processing_progress_title);
			mProgressDialog.setMessage(mContext.getResources().getString(
					R.string.art_processing_progress_initial_message));
			mProgressDialog.setButton(
					mContext.getString(R.string.art_processing_go_background), 
					goTotheBackgroundClickListener);
			mProgressDialog.setButton2(
					mContext.getString(R.string.art_processing_cancel), 
					cancelProcessingClickListener);
			mProgressDialog.show();
			
			Log.i(TAG, "Creating Album Art Processor");
			
			/* start fetching album art */
			mAlbumArtProcessor = new AlbumArtProcessor(mContext, mArtProcessingUpdateHandler, mTheme);
			
			Log.i(TAG, "Importing art!");
	
			mAlbumArtProcessor.processAlbumArt();
		}
	};
	
	
	// Handler to receive messages from the AlbumArtDownloader class thread
	public Handler	mArtProcessingUpdateHandler = new Handler(){
		public void handleMessage(Message msg){
			if(mProgressDialog.isShowing()){
				// Last Message ?
				if(msg.getData()
						.getString(Constants.ALBUM_ART_PROCESSING_UI_UPDATE_DONE_IPC_MSG)
						!= null)
				{
					try
					{
						mProgressDialog.dismiss();
					} 
					catch (IllegalArgumentException e) // based on bug reports
					{
						e.printStackTrace();
					}
					stopArtProcessing();
					// save in preferences
					Log.i(TAG, "XXXXXXXXXXXXXXXXXXXXXXXXXX");
					Log.i(TAG, "Finishing Image Processing");
					Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
					edit.putInt(Constants.prefkey_mTheme, mTheme);
					if(mTheme == Constants.THEME_HALFTONE)
						edit.putBoolean(Constants.prefkey_mThemeHalfToneDone, true);
					edit.commit();
					// reload ui
					mReloadUiHandler.sendEmptyMessage(mTheme);
				}
				// Still going
				else
					mProgressDialog.setMessage(
						msg.getData()
							.getString(Constants.ALBUM_ART_PROCESSING_UI_UPDATE_IPC_MSG));
			}
		}
	};
	
	// function for resuming download after a rotation/app close

	
	private OnClickListener cancelProcessingClickListener = new OnClickListener(){

		@Override
		public void onClick(DialogInterface dialog, int which) {
			stopArtProcessing();
		}
		
	};
	
	private OnClickListener goTotheBackgroundClickListener = new OnClickListener(){

		@Override
		public void onClick(DialogInterface dialog, int which) {
			mInTheBackground = true;
		}
		
	};
	
	public void stopArtProcessing(){
		mAlbumArtProcessor.stopAlbumArt();
		mProcessing = false;
		try
		{
			mProgressDialog.dismiss();
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
	}
	
}