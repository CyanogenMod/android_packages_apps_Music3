package org.abrantix.rockon.rockonnggl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Shader.TileMode;
import android.util.Log;

public class AlbumArtUtils{
	
	static final String TAG = "AlbumArtUtils";
	
	static public String getAlbumArtPath(String embeddedArtPath, String albumKey){
		String 	albumArtPath = null;
		int		maxSize = 0;
		
		/* Check if embedded art is big enough */
		if(embeddedArtPath != null){
	    	BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			Bitmap bmTmp = BitmapFactory.decodeFile(embeddedArtPath, opts);
			maxSize = Math.max(opts.outWidth, opts.outWidth);
			if(maxSize > 0)
				albumArtPath = embeddedArtPath;
			if(bmTmp != null)
				bmTmp.recycle();
		}
		
		/* Check also the (hypothetical downloaded album art) */
		if(albumKey != null){
			String downloadedArtPath = Constants.ROCKON_ALBUM_ART_PATH + albumKey;
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			Bitmap bmTmp = BitmapFactory.decodeFile(downloadedArtPath, opts);
			if(Math.max(opts.outWidth, opts.outWidth) > maxSize &&
					maxSize < Constants.REASONABLE_ALBUM_ART_SIZE)
				albumArtPath = downloadedArtPath;
			if(bmTmp != null)
				bmTmp.recycle();
		}
		
		return albumArtPath;
	}
	
	static public int getImageSize(String albumArtPath){
		if(albumArtPath != null){
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			Bitmap bmTmp = BitmapFactory.decodeFile(albumArtPath, opts);
			if(bmTmp != null)
				bmTmp.recycle();
			return Math.max(opts.outWidth, opts.outWidth);
		} 
		return 0;
	}
	
	static public Bitmap fetchBitmap(String imageURL){
		synchronized (TAG) {
			
			for(int retries = 0; retries < 2; retries++){
				try{
					/* request URL */
					URL coverURL = new URL(imageURL);
			
					Log.i(TAG, "fetchBitmap: "+imageURL);
					Log.i(TAG, "fetchBitmap: "+coverURL.toString());
					
					
					/* connection setup */
					BasicHttpParams params = new BasicHttpParams();
					HttpConnectionParams.setConnectionTimeout(params, 6000);
					HttpConnectionParams.setSoTimeout(params, 10000);
					DefaultHttpClient httpClient = new DefaultHttpClient();	
					httpClient.setParams(params);
			        HttpGet httpGet = new HttpGet(coverURL.toString());
			        HttpResponse response; 
					
					/* fetch content */
					response = httpClient.execute(httpGet);
			        HttpEntity entity = response.getEntity();
//	//	//	        InputStream inputStream = entity.getContent();
//	//	//	        BitmapFactory.Options opts = new BitmapFactory.Options();
//	//	//	        opts.inJustDecodeBounds = true;
//	//	//	        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, opts);
//	//	//	        int sampling = Math.min(opts.outWidth, opts.outHeight)/(2*Constants.REASONABLE_ALBUM_ART_SIZE);
//	//	//	        opts.inJustDecodeBounds = false;
//	//	//	        opts.inSampleSize = sampling;
//	//	//	        bitmap = BitmapFactory.decodeStream(inputStream, null, opts);
//			        Bitmap netBitmap = BitmapFactory.decodeStream(netImageStream);
			        
			        /* get the image and save it to a tmp file */
			        String tmpFilePath = Constants.ROCKON_ALBUM_ART_PATH+"___tmp.jpg";
			        File f=new File(tmpFilePath);
			        InputStream inputStream= entity.getContent();
			        OutputStream out=new FileOutputStream(f);
			        byte buf[]=new byte[1024];
			        int len;
			        while((len=inputStream.read(buf))>=0)
			        	out.write(buf,0,len);
			        out.close();
			        inputStream.close();

			        /* decode the image saved in the tmp file */
			        BitmapFactory.Options opts = new BitmapFactory.Options();
			        opts.inJustDecodeBounds = true;
			        BitmapFactory.decodeFile(tmpFilePath, opts);
			        // sample the bitmap if it is too large 
			        // -- max size of the smallest dimension is 4 * REASONABLE_ART_SIZE 
			        int sampling = (int)	
			        	Math.max(
			        		1,
				        	Math.min(
				        			Math.floor(
				        					opts.outWidth/(2 * Constants.REASONABLE_ALBUM_ART_SIZE)), 
				        			Math.floor(
				        					opts.outHeight/(2 * Constants.REASONABLE_ALBUM_ART_SIZE))));
			        boolean done = false;
			        int		tries = 0;
			        Bitmap 	netBitmap = null;
			        while(!done && tries < 3)
			        {	
			        	try{
			        		Log.i(TAG, "sampling: "+sampling+" wxh:"+opts.outWidth+"x"+opts.outHeight);
			        		opts.inJustDecodeBounds = false;
					        opts.inSampleSize = sampling;
					        opts.inDither = true;
					        netBitmap = BitmapFactory.decodeFile(tmpFilePath, opts);
					        done = true;
			        	} catch (OutOfMemoryError err) {
			        		System.gc();
			        		err.printStackTrace();
			        	}
			        	/* try again with higher sampling */
			        	sampling++;
			        	tries++;
			        }
			        
			        /* correct aspect ratio */
			        if((float)(netBitmap.getWidth())/(float)(netBitmap.getHeight()) > 1.1f){
//			        	try
//			        	{
			        		return Bitmap.createBitmap(
			        				netBitmap, 
				        			netBitmap.getWidth() - netBitmap.getHeight(), 
				        			0, 
				        			netBitmap.getHeight(), 
				        			netBitmap.getHeight());
//			        	} catch(OutOfMemoryError err) {
//			        		err.printStackTrace();
//			        		return null;
//			        	}
			        		
			        } else {
			        	return netBitmap;
			        }
				} catch(Exception e){
					e.printStackTrace();
				}
			}
			return null;
		}
		
	}
	
	static public boolean saveAlbumCoverInSdCard(Bitmap bitmap, String albumKey, boolean force){
		if(bitmap.getWidth() <= Constants.REASONABLE_ALBUM_ART_SIZE ||
			bitmap.getHeight() <= Constants.REASONABLE_ALBUM_ART_SIZE){
			Bitmap upscaledBm = Bitmap.createScaledBitmap(
					bitmap, 
					Constants.REASONABLE_ALBUM_ART_SIZE + 1,
					Constants.REASONABLE_ALBUM_ART_SIZE + 1,
					true);
			saveAlbumCoverInSdCard(upscaledBm, albumKey);
		} else {
			saveAlbumCoverInSdCard(bitmap, albumKey);
		}
		return true;
	}
	
	static public boolean saveAlbumCoverInSdCard(Bitmap bitmap, String albumKey){
		try{
			// TODO: limit bitmap size to 854x854 ????
			File file = new File(Constants.ROCKON_ALBUM_ART_PATH+albumKey);
			if(!file.exists())
				file.createNewFile();
			FileOutputStream fileOutStream = new FileOutputStream(file);
			bitmap.compress(CompressFormat.JPEG, 90, fileOutStream);
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	static public boolean saveSmallAlbumCoverInSdCard(Bitmap bitmap, String albumKey){
		try{
			if(bitmap != null){
				File file = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH+albumKey);
				Log.i(TAG, file.getAbsolutePath());
				if(!file.exists())
					file.createNewFile();
				FileOutputStream fileOutStream = new FileOutputStream(file);
				// TODO: decimation algorithm
//				try{
					Bitmap	smallBitmap = Bitmap.createScaledBitmap(
							bitmap, 
							Constants.ALBUM_ART_TEXTURE_SIZE, 
							Constants.ALBUM_ART_TEXTURE_SIZE, 
							true);
					Bitmap smallBitmapPostProc = smallCoverPostProc(smallBitmap);
					ByteBuffer bitmapBuffer = ByteBuffer.allocate(
							smallBitmapPostProc.getRowBytes() * smallBitmapPostProc.getHeight());
				    smallBitmapPostProc.copyPixelsToBuffer(bitmapBuffer);
				    fileOutStream.write(bitmapBuffer.array());
				    return true;
//				}catch(OutOfMemoryError err){
//					err.printStackTrace();
//					return false;
//				}
			} else {
				return false;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	static public boolean saveSmallUnknownAlbumCoverInSdCard(
			Resources res,
			byte[] colorComponent,
			String path,
			int theme)
	{
		try{
			Log.i(TAG, "saving small unknown album cover");
			
			/** 
			 * SMALL HACK
			 */
			Bitmap unknownBitmap = 
				BitmapFactory.decodeResource(
						res, 
						R.drawable.unknown_256);
			saveSmallAlbumCoverInSdCard(
					unknownBitmap, 
					Constants.ROCKON_UNKNOWN_ART_FILENAME);
			/** */
			
			Bitmap	tmpBm = 
				Bitmap.createBitmap(
					Constants.ALBUM_ART_TEXTURE_SIZE, 
					Constants.ALBUM_ART_TEXTURE_SIZE, 
					Bitmap.Config.RGB_565);
			if(unknownBitmap != null){
				processAndSaveSmallAlbumCoverInSdCard(
						tmpBm,
						colorComponent,
						Constants.ROCKON_UNKNOWN_ART_FILENAME,
						new ImageProcessor(theme));
				return true;
			} else {
				return false;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	static private Bitmap smallCoverPostProc(Bitmap smallBitmap){
		try{
			Bitmap smallBitmapPostProc = 
				Bitmap.createBitmap(
					smallBitmap.getWidth(), 
					smallBitmap.getHeight(),
					Bitmap.Config.RGB_565);
			Canvas 	canvas = new Canvas();
			canvas.setBitmap(smallBitmapPostProc);
			Paint	paint = new Paint();
			paint.setAntiAlias(true);
			Bitmap	bitmapToShade = 
				Bitmap.createBitmap(
						smallBitmap, 
						2, 
						2, 
						smallBitmap.getWidth()-4, 
						smallBitmap.getHeight()-4);
			BitmapShader bmShader = 
				new BitmapShader(
					bitmapToShade, 
					TileMode.CLAMP, 
					TileMode.CLAMP);
			paint.setShader(bmShader);
			canvas.drawRoundRect(
					new RectF(2, 2, smallBitmap.getWidth()-2, smallBitmap.getHeight()-2),
					4,
					4,
					paint);
			return smallBitmapPostProc;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	static public Bitmap processAndSaveSmallAlbumCoverInSdCard(
			Bitmap 			bitmap, 
			byte[] 			buffer, 
			String 			albumKey,
			ImageProcessor 	imgProc)
	{
		Bitmap outputBitmap = null;
		try{
			if(bitmap != null && buffer != null)
			{
				/** Check if file has already been created */
				File fileOut = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH+albumKey+imgProc.getThemeFileExt());
				if(fileOut.exists() && fileOut.length() > 0 &&
					fileOut.length() == bitmap.getHeight() * bitmap.getWidth() * 2) // 2bytes - RGB565 format
				{
					return null;
				}
				/** Read small normal bitmap */
				File fileIn = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH+albumKey);
//				Log.i(TAG, "3 - "+fileIn.getAbsolutePath());
				if(fileIn.exists() && fileIn.length() > 0)
				{
					FileInputStream albumCoverFileInputStream = new FileInputStream(fileIn);
					albumCoverFileInputStream.read(buffer, 0, buffer.length);
					bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(buffer));
					
					/** process bitmap */
					outputBitmap = imgProc.process(bitmap);
					
					/** save small bitmap on SdCard */
					fileOut.createNewFile();
					FileOutputStream fileOutStream = new FileOutputStream(fileOut);
					Bitmap smallBitmapPostProc = smallCoverPostProc(outputBitmap);
					ByteBuffer bitmapBuffer = ByteBuffer.allocate(
							smallBitmapPostProc.getRowBytes() * smallBitmapPostProc.getHeight());
				    smallBitmapPostProc.copyPixelsToBuffer(bitmapBuffer);
				    fileOutStream.write(bitmapBuffer.array());
				}
				return outputBitmap;
			}
			else
			{
				return outputBitmap;
			}
		} 
//		catch(OutOfMemoryError err) 
//		{
//			err.printStackTrace();
//			return outputBitmap;
//		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return outputBitmap;
		}
	}
}