package org.abrantix.rockon.rockonnggl;

import java.io.File;

import org.abrantix.rockon.rockonnggl.R;

import android.content.Context;
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

public class AlbumArtImporter{

	private final String TAG = "AlbumArtImporter";
	
	private Context	mContext;
	private Thread	mWorkerThread;
	private Handler	mUiProgressHandler;
	private boolean	mGetFromInet;
	
	AlbumArtImporter(
			Context context, 
			Handler uiProgressHandler,
			boolean	getFromInet)
	{
		this.mContext = context;
		this.mUiProgressHandler = uiProgressHandler;
		this.mGetFromInet = getFromInet;
		createAlbumArtDirectories();
		try {
			/* Check for Internet Connection (Through whichever interface) */
			ConnectivityManager connManager = (ConnectivityManager) 
				mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = connManager.getActiveNetworkInfo();
			/******* EMULATOR HACK - false condition needs to be removed *****/
			//if (false && (netInfo == null || !netInfo.isConnected())){
			if (mGetFromInet && (netInfo == null || netInfo.isConnected() == false))
			{
				updateUi(mContext.getResources().getString(R.string.art_download_no_inet));
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public boolean	getAlbumArt(){
		mWorkerThread = new 
			Thread(){				
				public void run(){
					try{
						getAlbumArtThreadedWork();
					} catch(Exception e){
						e.printStackTrace();
						return;
					}
				}
			};
		mWorkerThread.start();
		return true;
	}
	
	public void	getAlbumArtThreadedWork(){
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
		
		/* Our 'Fetchers' */
		FreeCoversNetFetcher freeCoversNetFetcher = new FreeCoversNetFetcher();
		GoogleImagesFetcher googleImagesFetcher = new GoogleImagesFetcher();
//		TODO: LastFMFetcher freeCoversNetFetcher = new LastFMFetcherFetcher();
		 
    	/* Loop through the albums */
		String	embeddedArtPath;
		Bitmap	embeddedArt;
		Bitmap	internetArt;
		String	artistName;
		String	albumName;
		String	albumKey;
		Integer	albumId;
    	for(int i=0; i<albumCursor.getCount(); i++)
    	{
			/** check if we need to stop this thread */
    		if(mGetFromInet && Thread.currentThread().isInterrupted())
    			return;
    		
    		/** move cursor to current position */
    		albumCursor.moveToPosition(i);
    		
    		/** initialize vars */
    		embeddedArt = null;
    		internetArt = null;
    		embeddedArtPath =
    			albumCursor.getString(
        				albumCursor.getColumnIndex(
        						MediaStore.Audio.Albums.ALBUM_ART));
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
    		
    		/** do we want the embedded even if it is low res? */
    		boolean	useAlwaysEmbedded = 
    			PreferenceManager.getDefaultSharedPreferences(mContext).
        			getBoolean(
        					mContext.getString(
        							R.string.preference_key_embedded_album_art), 
        					true);
    		
    		int		artSize = 
    			AlbumArtUtils.getImageSize(
        				AlbumArtUtils.getAlbumArtPath(
        	    				embeddedArtPath, 
        	    				RockOnFileUtils.validateFileName(albumId.toString())));
    		
    		/** check if image (embedded or downloaded) exists and is big enough */
    		if(
    				mGetFromInet
    				&&
    				(
	    				(
	    						!useAlwaysEmbedded &&
	    						artSize < Constants.REASONABLE_ALBUM_ART_SIZE
	    				)
	    				||
	    				(
	    						useAlwaysEmbedded &&
	    						artSize <= 0
	    				)
	    			)
    			)
    		{
    			if(!artistName.equals("<unknown>")){
	        		/** try to fetch it from freecovers.net */
	        		internetArt = freeCoversNetFetcher.fetch(
	        				SearchUtils.filterString(artistName), 
	        				SearchUtils.filterString(albumName));
	    			if(internetArt == null || 
	    					internetArt.getWidth() < Constants.REASONABLE_ALBUM_ART_SIZE ||
	    					internetArt.getHeight() < Constants.REASONABLE_ALBUM_ART_SIZE){
		        		/** try to fetch it from google */
		        		 internetArt = googleImagesFetcher.fetch(
		        				 SearchUtils.filterString(artistName),
		        				 SearchUtils.filterString(albumName));
	        			if(internetArt == null || 
	        					internetArt.getWidth() < Constants.REASONABLE_ALBUM_ART_SIZE ||
	        					internetArt.getHeight() < Constants.REASONABLE_ALBUM_ART_SIZE){
	        				/** try to fetch it from Last.FM */
	        				// TODO: internetArt = lastFmFetcher.fetch...
	        			}
	    			}
	
	    		/** Create 'normal' album cover */
	    			if(internetArt != null){
	//    				AlbumArtUtils.saveAlbumCoverInSdCard(internetArt, FileUtils.validateFileName(URLEncoder.encode(albumKey)));
	    				AlbumArtUtils.saveAlbumCoverInSdCard(internetArt, RockOnFileUtils.validateFileName(albumId.toString()));
	        		}
    			}
    		}
    		
    		/** check if small image exists */
    		// TODO: dont always recreate the small art
    		
    		/** create small art */
    		if(internetArt != null)
    		{ // fresh art download
    			AlbumArtUtils.saveSmallAlbumCoverInSdCard(
    					internetArt, 
    					RockOnFileUtils.validateFileName(albumId.toString()));
    			internetArt.recycle();
    		} else if(embeddedArtPath != null){
    			embeddedArt = BitmapFactory.decodeFile(embeddedArtPath);
    			if(embeddedArt != null)
    			{
    				AlbumArtUtils.saveSmallAlbumCoverInSdCard(
    						embeddedArt, 
    						RockOnFileUtils.validateFileName(albumId.toString()));
    				embeddedArt.recycle();
    			} else {
    				// TODO ::::::: --- needs treatment
    			}
    		}
    	}
    	
    	closeUi();
	}
	
	public void stopAlbumArt()
	{
		Log.i(TAG, "Stopping worker thread");
		if(mWorkerThread != null)
			mWorkerThread.interrupt(); // XXX - it is not instantaneous
	}
	

	private void createAlbumArtDirectories()
	{
        File albumArtDirectory = new File(Constants.ROCKON_ALBUM_ART_PATH);
    	albumArtDirectory.mkdirs();
		
        File albumSmallArtDirectory = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH);
		albumSmallArtDirectory.mkdirs();
	}
	
	private void closeUi()
	{
		if(mUiProgressHandler != null)
		{
			Bundle data = new Bundle();
			Message msg = new Message();
			data.clear();
			data.putString(
					Constants.ALBUM_ART_DOWNLOAD_UI_UPDATE_DONE_IPC_MSG, 
					"");
			msg.setData(data);
			msg.what = 0;
			mUiProgressHandler.removeMessages(0);
			mUiProgressHandler.sendMessage(msg);
		}
	}
	
	private void updateUi(String message)
	{
		if(mUiProgressHandler != null)
		{
			/* Give feedback to the user */
			Bundle data = new Bundle();
			Message msg = new Message();
			data.clear();
			data.putString(
					Constants.ALBUM_ART_DOWNLOAD_UI_UPDATE_IPC_MSG,
					message);
			msg.setData(data);
			msg.what = 0;
			mUiProgressHandler.removeMessages(0);
			mUiProgressHandler.sendMessage(msg);
		}
	}
}