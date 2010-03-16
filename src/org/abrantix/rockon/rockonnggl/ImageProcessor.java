package org.abrantix.rockon.rockonnggl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class ImageProcessor
{
	/** Globals */
	int 	mTheme;
	Paint	mHalfToneWhitePaint;
	Paint	mHalfToneBlackPaint;
	
	/** optimization */
	float	blockAverageIntensity;
	int 	color;
	
	public ImageProcessor(int theme)
	{
		mTheme = theme;
		
		initPaint();

	}
	
	private void initPaint()
	{
		switch(mTheme)
		{
		case Constants.THEME_HALFTONE:
			initHalfTonePaint();
			return;
		case Constants.THEME_EARTHQUAKE:
			initEarthquakePaint();
			return;
		}
	}
	
	private void initHalfTonePaint()
	{
		mHalfToneWhitePaint = new Paint();
		mHalfToneWhitePaint.setColor(Color.WHITE);
		mHalfToneWhitePaint.setAntiAlias(true);
		
		mHalfToneBlackPaint = new Paint();
		mHalfToneBlackPaint.setColor(Color.BLACK);
		mHalfToneBlackPaint.setAntiAlias(true);
	}
	
	private void initEarthquakePaint()
	{
//		mHalfToneWhitePaint = new Paint();
//		mHalfToneWhitePaint.setColor(Color.WHITE);
//		mHalfToneWhitePaint.setAntiAlias(true);
//		
//		mHalfToneBlackPaint = new Paint();
//		mHalfToneBlackPaint.setColor(Color.BLACK);
//		mHalfToneBlackPaint.setAntiAlias(true);
	}
	
	public Bitmap process(Bitmap bm)
	{
		switch(mTheme)
		{
		case Constants.THEME_NORMAL:
			return bm;
		case Constants.THEME_HALFTONE:
			return processHalfTone(bm);		
		case Constants.THEME_EARTHQUAKE:
			return processEarthquake(bm);
		}
		
		return null;
	}
	
	public Bitmap processHalfTone(Bitmap bm)
	{
		int		origBmSize = bm.getWidth();
//		Bitmap bmTmp = Bitmap.createBitmap(bm);
		Bitmap 	bmTmp = Bitmap.createScaledBitmap(
				bm, 
				Constants.THEME_HALF_TONE_PROC_RESOLUTION, 
				Constants.THEME_HALF_TONE_PROC_RESOLUTION, 
				true);
		bm = Bitmap.createBitmap(bmTmp);
		Canvas c = new Canvas();
		c.setBitmap(bm);
		
		int blockSize = bm.getWidth() / Constants.THEME_HALF_TONE_BLOCK_COUNT;
		for(int i=0; i<Constants.THEME_HALF_TONE_BLOCK_COUNT; i++)
		{
			for(int j=0; j<Constants.THEME_HALF_TONE_BLOCK_COUNT; j++)
			{
				/** get the area tone intensity */
				blockAverageIntensity = 0;
				for(int k=0; k<blockSize; k++)
				{
					for(int l=0; l<blockSize; l++)
					{
						color = 
							bmTmp.getPixel(
									i*blockSize + k, 
									j*blockSize + l);
						blockAverageIntensity += Color.red(color);
						blockAverageIntensity += Color.green(color);
						blockAverageIntensity += Color.blue(color);
					}
				}
				blockAverageIntensity /= blockSize * blockSize * 3;
				blockAverageIntensity /= 256;
				
				/** clear up this bitmap area */
				c.drawRect(
						i*blockSize, 
						j*blockSize, 
						(i+1)*blockSize, 
						(j+1)*blockSize, 
						mHalfToneBlackPaint);
				
				/** draw our half tone */
				c.drawCircle(
						i*blockSize + blockSize/2, 
						j*blockSize + blockSize/2, 
//						Math.round(blockSize/2 * blockAverageIntensity), 
//						1 + Math.round(blockSize * blockAverageIntensity), 
						1 + (Math.round(blockSize/2 - 1)) * blockAverageIntensity, 
//						Math.max(1, Math.round((blockSize/2) * blockAverageIntensity)), 
//						Math.round(blockSize * blockAverageIntensity), 
						mHalfToneWhitePaint);
			}
		}
		
		return Bitmap.createScaledBitmap(bm, origBmSize, origBmSize, true);
	}
	
	public Bitmap processEarthquake(Bitmap bm)
	{
		Bitmap bmTmp = Bitmap.createBitmap(bm);
		bm = Bitmap.createScaledBitmap(
				bmTmp,
//				bmTmp.getWidth(), 
//				bmTmp.getHeight(),
				bmTmp.getWidth() + Constants.THEME_EARTHQUAKE_RANDOM_AMOUNT, 
				bmTmp.getHeight() + Constants.THEME_EARTHQUAKE_RANDOM_AMOUNT,
				false);
		
		Rect rSrc = new Rect();
		Rect rDst = new Rect();
		
		bm.eraseColor(Color.TRANSPARENT);
		Canvas c = new Canvas();
		c.setBitmap(bm);
		
		int offset;
		int blockSize = bmTmp.getHeight() / Constants.THEME_EARTHQUAKE_BLOCK_COUNT;
		
		for(int i=0; i<(bmTmp.getHeight()-1)/blockSize; i++)
		{
			
			offset = (int) (Math.random() * Constants.THEME_EARTHQUAKE_RANDOM_AMOUNT);
//			offset = 2 * Math.abs((i % Constants.THEME_EARTHQUAKE_RANDOM_AMOUNT) - Constants.THEME_EARTHQUAKE_RANDOM_AMOUNT/2);
			
			/** source bitmap area */
			rSrc.top = i*blockSize;
			rSrc.bottom = i*blockSize+blockSize;
			rSrc.left = 0;
			rSrc.right = bmTmp.getWidth()-1;
			
			/** destination bitmap area */
			rDst.top = i*blockSize;
			rDst.bottom = i*blockSize+blockSize;
			rDst.left = offset;
			rDst.right = bmTmp.getWidth() - 1 + offset;
			
			c.drawBitmap(
					bmTmp, 
					rSrc, 
					rDst, 
					null);
		}
		
		return Bitmap.createScaledBitmap(
				bm, 
				bmTmp.getWidth(), 
				bmTmp.getHeight(), 
				true);
	}
	
	public String getThemeFileExt()
	{
		switch(mTheme)
		{
		case Constants.THEME_NORMAL:
			return "";
		case Constants.THEME_HALFTONE:
			return Constants.THEME_HALF_TONE_FILE_EXT;
		case Constants.THEME_EARTHQUAKE:
			return Constants.THEME_EARTHQUAKE_FILE_EXT;
		}
		return null;
	}
}