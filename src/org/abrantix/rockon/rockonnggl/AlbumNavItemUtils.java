package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.provider.MediaStore;
import android.util.Log;


public class AlbumNavItemUtils{
	final String TAG = "AlbumNavItemUtils";
	
	
	/** Code optimization */
	String 	albumCoverPath;
	String 	artistName;
	String 	albumName;
	String 	path;
	File	albumCoverFile;
	Canvas 	canvas;
	RectF	labelRectf;
	Paint	labelBgPaint;
	Paint	labelAlbumPaint;
	Paint	labelArtistPaint;
	Paint	labelAlbumBoringPaint;
	Paint	labelArtistBoringPaint;
	
	public AlbumNavItemUtils(int width, int height) {
		canvas = new Canvas();
		labelRectf = new RectF(0,0,width,height/4);
		
		labelBgPaint = new Paint();
		labelBgPaint.setColor(Color.parseColor("#00000000"));
		labelBgPaint.setAntiAlias(true);
		labelBgPaint.setStyle(Style.FILL_AND_STROKE);
		labelBgPaint.setStrokeWidth(0); // 0 = hairline
		
		labelAlbumPaint = new Paint();
		labelAlbumPaint.setColor(Color.parseColor("#ffffffff"));
		labelAlbumPaint.setStrokeWidth(1.33f);
		labelAlbumPaint.setAntiAlias(true);
		labelAlbumPaint.setSubpixelText(true);
		labelAlbumPaint.setStyle(Style.FILL_AND_STROKE);
		labelAlbumPaint.setTextAlign(Align.CENTER);
		
		labelArtistPaint = new Paint();
		labelArtistPaint.setColor(Color.parseColor("#ffaaaaaa"));
		labelArtistPaint.setStrokeWidth(1.1f);
		labelArtistPaint.setAntiAlias(true);
		labelArtistPaint.setSubpixelText(true);
		labelArtistPaint.setStyle(Style.FILL_AND_STROKE);
		labelArtistPaint.setTextAlign(Align.CENTER);
		
		labelAlbumBoringPaint = new Paint();
		labelAlbumBoringPaint.setColor(Color.parseColor("#ffaaaaaa"));
		labelAlbumBoringPaint.setStrokeWidth(1.33f);
		labelAlbumBoringPaint.setAntiAlias(true);
		labelAlbumBoringPaint.setSubpixelText(true);
		labelAlbumBoringPaint.setStyle(Style.FILL_AND_STROKE);
		labelAlbumBoringPaint.setTextAlign(Align.LEFT);
		
		labelArtistBoringPaint = new Paint();
		labelArtistBoringPaint.setColor(Color.parseColor("#ffffffff"));
		labelArtistBoringPaint.setStrokeWidth(1.1f);
		labelArtistBoringPaint.setAntiAlias(true);
		labelArtistBoringPaint.setSubpixelText(true);
		labelArtistBoringPaint.setStyle(Style.FILL_AND_STROKE);
		labelArtistBoringPaint.setTextAlign(Align.LEFT);
	}
	
	/**
	 * fill album bitmap
	 * @param bitmap
	 * @param position
	 * @return
	 */
	boolean fillAlbumBitmap(
			AlbumNavItem albumNavItem, 
			int width, 
			int height, 
			byte[] colorComponent,
			int theme)
	{
		try{
			/** Sanity check */
	    	if(albumNavItem.cover.getWidth() != width || 
					albumNavItem.cover.getHeight() != height){
				Log.i(TAG, " - reading pixels from file failed");
	    		return false;
	    	}
	    	
	    	albumCoverPath = null;
				
	    	/** Get the path to the album art */
	    	switch(theme)
	    	{
	    	case Constants.THEME_NORMAL:
	    		path = Constants.ROCKON_SMALL_ALBUM_ART_PATH+
    				RockOnFileUtils.validateFileName(albumNavItem.albumId);
	    		break;
	    	case Constants.THEME_HALFTONE:
	    		path = Constants.ROCKON_SMALL_ALBUM_ART_PATH+
    				RockOnFileUtils.validateFileName(albumNavItem.albumId)+
    				Constants.THEME_HALF_TONE_FILE_EXT;
	    		break;
	    	case Constants.THEME_EARTHQUAKE:
	    		path = Constants.ROCKON_SMALL_ALBUM_ART_PATH+
    				RockOnFileUtils.validateFileName(albumNavItem.albumId)+
    				Constants.THEME_EARTHQUAKE_FILE_EXT;
	    		break;
	    	default:
	    		path = Constants.ROCKON_SMALL_ALBUM_ART_PATH+
					RockOnFileUtils.validateFileName(albumNavItem.albumId);
	    		break;
	    	}
	    
			/** Access the file */
			albumCoverFile = new File(path);
			if(albumCoverFile.exists() && albumCoverFile.length() > 0){
				albumCoverPath = path;
			} else {
//				Log.i(TAG, " - album cover bmp file has a problem "+path);
				return false;
			}
			
			/** Read File and fill bitmap */
			try {
				FileInputStream albumCoverFileInputStream = new FileInputStream(albumCoverFile);
				albumCoverFileInputStream.read(colorComponent, 0, colorComponent.length);
				albumNavItem.cover.copyPixelsFromBuffer(ByteBuffer.wrap(colorComponent));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * fill alphabet bitmap
	 * @param bitmap
	 * @param letter
	 * @return
	 */
	boolean fillAlphabetBitmap(
			AlphabetNavItem	alphaNavItem, 
			int 			width, 
			int 			height){
		
		try{
			/** Sanity check */
	    	if(alphaNavItem.letterBitmap.getWidth() != width || 
					alphaNavItem.letterBitmap.getHeight() != height)
	    	{
				Log.i(TAG, " - reading pixels from file failed");
	    		return false;
	    	}

	    	/** Create bitmap */
	    	alphaNavItem.letterBitmap.eraseColor(Color.parseColor("#00000000"));
	    	canvas.setBitmap(alphaNavItem.letterBitmap);
	    	if(alphaNavItem.letter >= 'a')
	    	{
		    	labelAlbumPaint.setTextSize(.60f * height);
		    	canvas.drawText(
		    			String.valueOf((char)alphaNavItem.letter), 
		    			.5f * width, 
		    			(.5f+.2f) * height, // -.2f is half of the font size so that we can center it
		    			labelAlbumPaint);
	    	} 
	    	else if(alphaNavItem.letter == 'a' - 1)
	    	{
		    	labelAlbumPaint.setTextSize(.3f * height);
		    	canvas.drawText(
		    			"123", 
		    			.5f * width, 
		    			(.5f+.0f) * height, // -.2f is half of the font size so that we can center it
		    			labelAlbumPaint);
		    	canvas.drawText(
		    			"_>?", 
		    			.5f * width, 
		    			(.5f+.32f) * height, // -.2f is half of the font size so that we can center it
		    			labelAlbumPaint);
		    }	
			return true;
		} catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * fillAlbumLabel
	 * @param albumNavItem
	 * @param width
	 * @param height
	 * @return
	 */
	boolean fillAlbumLabel(
			AlbumNavItem albumNavItem,
			int width,
			int height){
		/** Sanity check */
    	if(albumNavItem.label.getWidth() != width || 
				albumNavItem.label.getHeight() != height)
    	{
			Log.i(TAG, " - reading pixels from file failed");
    		return false;
    	}
    	/** Create bitmap */
    	albumNavItem.label.eraseColor(Color.argb(0, 0, 0, 0));
    	canvas.setBitmap(albumNavItem.label);
    	canvas.drawRoundRect(labelRectf, height/8, height/8, labelBgPaint);
    	labelAlbumPaint.setTextSize(.28f * height);
    	labelArtistPaint.setTextSize(.24f * height);
    	if(albumNavItem.albumName != null){
	    	canvas.drawText(
	    			albumNavItem.albumName.substring(
	    					0, 
	    					labelAlbumPaint.breakText(
	    							albumNavItem.albumName, 
	    							false, 
	    							width*0.95f, 
	    							null)), 
	    			.5f * width, 
	    			.5f * height,
	    			labelAlbumPaint);
    	}
    	if(albumNavItem.artistName != null){
	    	canvas.drawText(
	    			albumNavItem.artistName.substring(
	    					0, 
	    					labelArtistPaint.breakText(
	    							albumNavItem.artistName, 
	    							false, 
	    							width*.95f, 
	    							null)),
					.5f * width, 
					.9f * height, 
					labelArtistPaint);
    	}
    	
		return true;
	}
	
	/**
	 * fillAlbumBoringLabel
	 * @param albumNavItem
	 * @param width
	 * @param height
	 * @return
	 */
	boolean fillAlbumBoringLabel(
			AlbumNavItem albumNavItem,
			int width,
			int height)
	{
		/** Sanity check */
    	if(albumNavItem.label.getWidth() != width || 
				albumNavItem.label.getHeight() != height)
    	{
			Log.i(TAG, " - reading pixels from file failed");
    		return false;
    	}
    	/** Create bitmap */
    	albumNavItem.label.eraseColor(Color.argb(0, 0, 0, 0));
    	canvas.setBitmap(albumNavItem.label);
//    	canvas.drawRoundRect(labelRectf, height/8, height/8, labelBgPaint);
    	labelAlbumBoringPaint.setTextSize(.24f * height);
    	labelArtistBoringPaint.setTextSize(.48f * height);
    	if(albumNavItem.artistName != null){
	    	canvas.drawText(
	    			albumNavItem.artistName.substring(
	    					0, 
	    					labelArtistBoringPaint.breakText(
	    							albumNavItem.artistName, 
	    							false, 
	    							width*.95f, 
	    							null)),
					0.f * width, 
					.5f * height, 
					labelArtistBoringPaint);
    	}
    	if(albumNavItem.albumName != null){
	    	canvas.drawText(
	    			albumNavItem.albumName.substring(
	    					0, 
	    					labelAlbumBoringPaint.breakText(
	    							albumNavItem.albumName, 
	    							false, 
	    							width*0.95f, 
	    							null)), 
	    			0.f * width, 
	    			.9f * height,
	    			labelAlbumBoringPaint);
    	}

		return true;
	}
	
	/**
	 * fillAlbumInfo
	 * @param albumNavItem
	 * @param position
	 * @return
	 */
	boolean fillAlbumInfo(Cursor albumCursor, AlbumNavItem albumNavItem, int position){
		
		
    	/** Sanity check */
    	if(position < 0 || position >= albumCursor.getCount()){
			Log.i(TAG, " - reading pixels from file failed");
    		return false;
    	}
    	
		/** move cursor */ 
		albumCursor.moveToPosition(position);

		/** get album info */
		albumNavItem.artistName = albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ARTIST));
		albumNavItem.albumName = albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ALBUM));
    	albumNavItem.albumKey = albumCursor.getString(
    			albumCursor.getColumnIndexOrThrow(
    					MediaStore.Audio.Albums.ALBUM_KEY));
    	albumNavItem.albumId = String.valueOf(
    			albumCursor.getInt(
    			albumCursor.getColumnIndexOrThrow(
    					MediaStore.Audio.Albums._ID)));
		
//    	Log.i(TAG, albumNavItem.albumId+" - "+albumNavItem.artistName+" "+albumNavItem.albumName);
    	
		return true;

	}
}