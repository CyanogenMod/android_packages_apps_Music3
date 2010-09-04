package org.abrantix.rockon.rockonnggl;

import java.io.File;

import org.abrantix.rockon.rockonnggl.R;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class AlbumArtProcessor{

	private final String TAG = "AlbumArtProcessor";
	
	private Context	mContext;
	private Thread	mWorkerThread;
	private Handler	mUiProgressHandler;
	private int 	mTheme;
	
	AlbumArtProcessor(Context context, Handler uiProgressHandler, int theme){
		this.mContext = context;
		this.mUiProgressHandler = uiProgressHandler;
		this.mTheme = theme;
		createAlbumArtDirectories();
	}

	
	public boolean	processAlbumArt(){
		mWorkerThread = new 
			Thread(){				
				public void run(){
					try{
						processAlbumArtThreadedWork();
					} catch(Exception e){
						e.printStackTrace();
						return;
					}
				}
			};
		mWorkerThread.start();
		return true;
	}
	
	public void	processAlbumArtThreadedWork(){
		/* create album cursor */
		Cursor albumCursor = new CursorUtils(mContext)
			.getAlbumListFromPlaylist(
					Constants.PLAYLIST_ALL, 
					PreferenceManager.
						getDefaultSharedPreferences(mContext).
						getBoolean(
								mContext.
								getString(
										R.string.preference_key_prefer_artist_sorting), 
										true));
		       
		/* Give feedback to the user */
		updateUi(mContext.getResources()
				.getString(R.string.art_download_progress_initial_message));
		
		/* Our Processor */
		ImageProcessor	imgProc = new ImageProcessor(mTheme);
		 
    	/* Loop through the albums */
		String	artistName;
		String	albumName;
		String	albumKey;
		Integer	albumId;
		byte[]	tmpBuf = 
			new byte[4*Constants.ALBUM_ART_TEXTURE_SIZE*Constants.ALBUM_ART_TEXTURE_SIZE];
		Bitmap	tmpBm = 
			Bitmap.createBitmap(
				Constants.ALBUM_ART_TEXTURE_SIZE, 
				Constants.ALBUM_ART_TEXTURE_SIZE, 
				Bitmap.Config.RGB_565);
		
		for(int i=0; i<albumCursor.getCount(); i++){
			/** check if we need to stop this thread */
    		if(Thread.currentThread().isInterrupted())
    			return;
    		
    		/** move cursor to current position */
    		albumCursor.moveToPosition(i);
    		
    		/** initialize vars */
    		artistName = 
    			albumCursor.getString(
    				albumCursor.getColumnIndex(
    						MediaStore.Audio.Albums.ARTIST));
    		albumName = 
    			albumCursor.getString(
        				albumCursor.getColumnIndex(
        						MediaStore.Audio.Albums.ALBUM));
    		albumKey = 
    			albumCursor.getString(
        				albumCursor.getColumnIndex(
        						MediaStore.Audio.Albums.ALBUM_KEY));
    		albumId = 
    			albumCursor.getInt(
        				albumCursor.getColumnIndex(
        						MediaStore.Audio.Albums._ID));
    		
    		Log.i(TAG, artistName+" - "+albumName);
    		
    		/** ui feedback */
    		updateUi((i+1) + " / "+albumCursor.getCount()+"\n"+
    				artistName + "\n"+
    				albumName);
    		
    		/** generate art */
    		AlbumArtUtils.
    			processAndSaveSmallAlbumCoverInSdCard(
    					tmpBm, 
    					tmpBuf, 
    					RockOnFileUtils.validateFileName(albumId.toString()),
    					imgProc);
    	}
    	
    	closeUi();
	}
	
	public void stopAlbumArt(){
		Log.i(TAG, "Stopping worker thread");
		if(mWorkerThread != null)
			mWorkerThread.interrupt(); // XXX - it is not instantaneous
	}
	

	private void createAlbumArtDirectories(){
        File albumArtDirectory = new File(Constants.ROCKON_ALBUM_ART_PATH);
    	albumArtDirectory.mkdirs();
		
        File albumSmallArtDirectory = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH);
		albumSmallArtDirectory.mkdirs();
	}
	
	private void closeUi(){
		Bundle data = new Bundle();
		Message msg = new Message();
		data.clear();
		data.putString(
				Constants.ALBUM_ART_PROCESSING_UI_UPDATE_DONE_IPC_MSG, 
				"");
		msg.setData(data);
		msg.what = 0;
		mUiProgressHandler.removeMessages(0);
		mUiProgressHandler.sendMessage(msg);
		
		/**
		 * XXX: this is an hack -- 
		 * we need to do this here in case the process has been taken to the background
		 */
//		Log.i(TAG, "XXX: HACKALERT");
		Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		edit.putInt(Constants.prefkey_mTheme, mTheme);
		if(mTheme == Constants.THEME_HALFTONE)
			edit.putBoolean(Constants.prefkey_mThemeHalfToneDone, true);
		edit.commit();
	}
	
	private void updateUi(String message){
		/* Give feedback to the user */
		Bundle data = new Bundle();
		Message msg = new Message();
		data.clear();
		data.putString(
				Constants.ALBUM_ART_PROCESSING_UI_UPDATE_IPC_MSG,
				message);
		msg.setData(data);
		msg.what = 0;
		mUiProgressHandler.removeMessages(0);
		mUiProgressHandler.sendMessage(msg);
	}
}