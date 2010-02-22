package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.os.Build;
import android.util.Log;

class WidgetCoverUtils{
	static final String TAG = "WidgetCoverUtils";
	
	static Bitmap getWidgetCoverBitmap(
			String albumId, 
			String artistName,
			String trackName,
			int width, 
			int height){
		/** 
		 * THIS CODE IS SOMEWHAT DUPLICATED HERE
		 * NEEDS BETTER/CENTRALIZED FUNCTIONS FOR THIS
		 * ... IN ANY CASE DO NOT TAKE THIS CODE AS STABLE
		 *  
		 * 		-- CHANGE AT WILL --
		 * 		-- MOST IMPORTANTLY --
		 * 		-- DO NOT CHANGE OTHER CODE TO MATCH THIS -- 
		 * 
		 */
    	String path = 
//    		Constants.ROCKON_ALBUM_ART_PATH+
//			RockOnFileUtils.validateFileName(albumId);
			Constants.ROCKON_SMALL_ALBUM_ART_PATH+
			RockOnFileUtils.validateFileName(albumId);
    	
		/** Access the file */
    	String albumCoverPath;
		File albumCoverFile = new File(path);
		if(albumCoverFile.exists() && albumCoverFile.length() > 0){
			albumCoverPath = path;
		} else {
			Log.i(TAG, " - album cover bmp file has a problem "+path);
			albumCoverPath = null;
//			return null;
		}
		
		Bitmap cover = null;
		/** Read File and fill bitmap */
		if(albumCoverPath != null){
			try {
				byte[] colorComponent = new byte[4*width*height];
				FileInputStream albumCoverFileInputStream = new FileInputStream(albumCoverFile);
				albumCoverFileInputStream.read(colorComponent, 0, colorComponent.length);
				cover = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				cover.copyPixelsFromBuffer(ByteBuffer.wrap(colorComponent));
//				cover = BitmapFactory.decodeStream(new FileInputStream(albumCoverFile));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		
		/** Failed reading the bitmap, load the default */
		if(cover == null){
			// load the unknown album
			// TODO: should bitmapfactory.decoderesource - createscaledbitmap
			cover = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			cover.eraseColor(Color.argb(128, 0, 0, 0));
			Log.i(TAG, "Cover was not created");
		}
		
		Bitmap wickedBitmap = createWickedBitmapFromCover(cover, artistName, trackName);
		return wickedBitmap;
			
	}
	
	/* optimization */
	static int	textWidth;
	static int	stringIdx;
	private static Bitmap createWickedBitmapFromCover(
			Bitmap cover,
			String artistName,
			String trackName)
	{
		
		final int	textOffset = 54;
		
		Bitmap wickedBitmap = Bitmap.createBitmap(
				(int)(cover.getWidth()*1.05), 
				(int)(cover.getHeight()*1.05 + textOffset), 
				Bitmap.Config.ARGB_8888);
		
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setFilterBitmap(true);
//		paint.setColor(Color.TRANSPARENT);
		
		Canvas canvas = new Canvas();
		canvas.setBitmap(wickedBitmap);
		
		if(Integer.valueOf(Build.VERSION.SDK) < 4)
			canvas.translate(.5f*wickedBitmap.getWidth(), .5f*wickedBitmap.getHeight());
		else
			canvas.translate(.5f*wickedBitmap.getWidth(), .5f*wickedBitmap.getHeight() + .5f*textOffset);

		// Translate stuff for the 3d effect
		Camera cam = new Camera();
		cam.translate(0.f, 0.f, cover.getHeight()*.0001f);
		cam.translate(-.5f*cover.getWidth(), .5f*cover.getHeight(), 0.f);
		cam.rotateX(-20.f);
		cam.rotateY(-0.f);
		cam.applyToCanvas(canvas);
		
		
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.TRANSPARENT);
		paint.setShadowLayer(
				5f, 
				2.f, 
				2.f, 
				Color.argb(
						200, 
						30, 
						30, 
						30));
		/* draw the shade */
		canvas.drawRoundRect(
				new RectF(
						0, 
						2,
						cover.getWidth()-0, 
						cover.getHeight()-2), 
				cover.getWidth()/16, 
				cover.getHeight()/16, 
				paint);
		
		paint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
		paint.setColor(Color.BLACK);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setShader(
				new BitmapShader(
						cover, 
						TileMode.REPEAT, 
						TileMode.REPEAT));
		/* draw the bitmap */
		canvas.drawRoundRect(
				new RectF(
						4, 
						4,
						cover.getWidth()-4, 
						cover.getHeight()-4), 
				cover.getWidth()/32, 
				cover.getHeight()/32, 
				paint);
		
		/* draw label */
		if((artistName != null || trackName != null) && Integer.valueOf(Build.VERSION.SDK) >= 4)
		{
			Log.i(TAG, "artist: "+ artistName + " trackname: "+trackName + " versionInt: "+Build.VERSION.SDK);
			/* reposition*/
			canvas.translate(0, - textOffset);
			
			/* draw text background */
			paint.reset();
			paint.setAntiAlias(true);
			paint.setColor(Color.argb(208, 32, 32, 32));
			paint.setStyle(Style.FILL_AND_STROKE);

			/**
			 * Verify if text fits in the given space
			 */
			// need to set the font here otherwise we cannot measure text
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setTextAlign(Align.CENTER);
			paint.setTextSize(22.f);
			stringIdx = paint.breakText(
					trackName, 
					true, 
					cover.getWidth(), 
					null);
			if(stringIdx < trackName.length())
			{
				trackName = trackName.substring(
						0, 
						stringIdx-3);
				trackName += "...";
			} else {
				trackName = trackName.substring(
						0, 
						stringIdx);
			}
			textWidth = (int)paint.measureText(trackName);
			
			/* measure artistName */
//			paint.setTypeface(Typeface.DEFAULT);
			paint.setTextSize(18.f);
			stringIdx = paint.breakText(
					artistName, 
					true, 
					cover.getWidth(), 
					null);
			if(stringIdx < artistName.length())
			{
				artistName = artistName.substring(
						0, 
						stringIdx-3);
				artistName += "...";
			} else {
				artistName = artistName.substring(
						0, 
						stringIdx);
			}
			textWidth = (int)
				Math.max(
						paint.measureText(artistName), 
						textWidth);
			
			/* some margin for the label */
			textWidth += 12;
				
//			Log.i(TAG, "textwidth: "+ textWidth + " cover.width: "+cover.getWidth()+" wickedBm.width: "+wickedBitmap.getWidth());
			
			canvas.drawRoundRect(
					new RectF(
						(cover.getWidth() - textWidth) / 2,
//						0,
						0,
						cover.getWidth() - (cover.getWidth()-textWidth) / 2,
//						cover.getWidth(),
						56),
					12,
					12,
					paint);	
			
			/* write track name */
//			paint.reset();
//			paint.setAntiAlias(true);
			paint.setColor(Color.argb(255, 255, 255, 255));
			paint.setStyle(Style.FILL_AND_STROKE);
			paint.setTypeface(Typeface.DEFAULT_BOLD);
//			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setTextAlign(Align.CENTER);
			paint.setTextSize(22.f);
			canvas.drawText(trackName, wickedBitmap.getWidth()/2-6, 24, paint);
			
			/* write artist name */
			paint.setColor(Color.argb(255, 200, 200, 200));
//			paint.setTypeface(Typeface.DEFAULT);
			paint.setTextSize(18.f);
			canvas.drawText(artistName, wickedBitmap.getWidth()/2-6, 48, paint);
		}
		return wickedBitmap;
	}
}