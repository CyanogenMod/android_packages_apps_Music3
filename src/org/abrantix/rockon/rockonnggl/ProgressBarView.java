package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.AvoidXfermode.Mode;
import android.graphics.Paint.Style;
import android.graphics.Path.FillType;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ProgressBarView extends View{

	private static final String TAG = "ProgressBarView";

	public ProgressBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	int		mDuration = 100000;
	int		mProgress = 0;
	int		mWidth = 0;
	int 	mHeight = 0;
	int		mDotPixelRadius = 1;
	
	Paint	mDotPaint;
	Paint	mBarOverlayPaint;
	Paint	mTouchOverlayPaint;
	RectF	mDotOvalArea;
	RectF	mBarOvalArea;
	
	Bitmap	mProgressBitmap = null;
	int		mFractionPlayedInColumns = 0;
	
	boolean	mTouching = false;
	double	mTouchingLastTimestamp = 0;
	float	mTouchingX = 0;
	float	mTouchingY = 0;
	int		mOverlayTimeout = 1000;
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	public void setTouchCoords(float x, float y){
		mTouchingX = x;
		mTouchingY = y;
		mTouchingLastTimestamp = System.currentTimeMillis();
//		invalidate();
	}
	
	/**
	 * 
	 * @param touching
	 */
	public void setTouching(boolean touching){
		mTouching = touching;
		mTouchingLastTimestamp = System.currentTimeMillis();
		invalidate();
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public int getCoordSeekPosition(float x, float y){
		return (int) (x/mWidth * mDuration);
	}
	
	/**
	 * 
	 * @param duration
	 */
	public void setDuration(int duration, boolean refresh){
		mDuration = duration;
		if(refresh)
			this.invalidate();
	}
	
	/**
	 * 
	 * @param progress
	 */
	public void setProgress(int progress, boolean refresh){
		mProgress = progress;
		if(refresh)
			this.invalidate();
	}
	
	/**
	 * 
	 */
	public void refresh(){
		this.invalidate();
	}
	
	
	/**
	 * overlay paint
	 * @param width
	 * @param height
	 * @return
	 */
	private Paint setupOverlayPaint(int width, int height){
		Paint paint = new Paint();

		LinearGradient lGradHoriz =
			new LinearGradient(
					0, 0, 
					width, 0, 
					new int[]
					{
							Color.argb(255, 0, 0, 0),
							Color.argb(64, 0, 0, 0),
							Color.argb(16, 0, 0, 0),
							Color.argb(64, 0, 0, 0),
							Color.argb(255, 0, 0, 0)
					}, 
					new float[]
					{
							0.f,
							0.1f,
							0.5f,
							0.9f,
							1.f
					},
					TileMode.REPEAT);
		
		LinearGradient lGradVert =
			new LinearGradient(
					0, 0, 
					0, height, 
					new int[]
					{
							Color.argb(255, 0, 0, 0),
							Color.argb(64, 0, 0, 0),
							Color.argb(16, 0, 0, 0),
							Color.argb(64, 0, 0, 0),
							Color.argb(255, 0, 0, 0)
					}, 
					new float[]
					{
							0.f,
							0.15f,
							0.5f,
							0.85f,
							1.f
					}, 
					TileMode.REPEAT);
		
		
		ComposeShader cShader = 
			new ComposeShader(
					lGradHoriz, 
					lGradVert, 
					PorterDuff.Mode.SRC_OVER);
		
		paint.setShader(cShader);
		
		paint.setAntiAlias(true);
		paint.setDither(true);
		
		return paint;
	}
	
	private Paint setupTouchOverlayPaint(int width, int height){
		Paint paint = new Paint();

		int		TOUCH_OVERLAY_RADIUS = 32;
		
		RadialGradient radialGradient = 
			new RadialGradient(
					TOUCH_OVERLAY_RADIUS, // x center coord
					TOUCH_OVERLAY_RADIUS, // y center coord
					TOUCH_OVERLAY_RADIUS, // radius
					new int[]
					{
							Color.argb(127, 255, 255, 255),
							Color.argb(64, 255, 255, 255),
							Color.argb(0, 255, 255, 255)
					}, 
					new float[]
					{
							0.f,
							0.5f,
							1.f
					},
					TileMode.CLAMP);
		
		paint.setShader(radialGradient);
		
		paint.setAntiAlias(true);
		paint.setDither(true);
		
		return paint;		
	}
	
	/**
	 * dot paint
	 * @return
	 */
	private Paint setupDotPaint(){
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(1.f);
		DashPathEffect d = new DashPathEffect(new float[]{3,3}, 0.f);
		paint.setPathEffect(d);
		return paint;
	}
	
	@Override
	protected void onSizeChanged(
			int width,
			int height,
			int oldWidth,
			int oldHeight)
	{
//		Log.i(TAG, "Size changed!");
		
		mWidth = width;
		mHeight = height;
		
		mDotPaint = 
			setupDotPaint();
		mBarOverlayPaint = 
			setupOverlayPaint(
				width, 
				height);
		mTouchOverlayPaint =
			setupTouchOverlayPaint(
				width,
				height);
		
		mDotOvalArea =
			new RectF(
					0, 
					0, 
					mDotPixelRadius*2+1,
					mDotPixelRadius*2+1);
		mBarOvalArea = 
			new RectF(
					0, 
					0, 
					width, 
					height);
		
		if(mBarOvalArea == null)
			Log.i(TAG, "Oval Area is NULL");
		
		mProgressBitmap = 
			Bitmap.createBitmap(
					width, 
					height, 
					Bitmap.Config.ARGB_8888);
		
		this.invalidate();
	}
	
	/* optimization */
	int		fractionPlayedInColumns;
	@Override
	protected void onDraw(Canvas canvas){
		if(mWidth > 0 && mHeight > 0 &&
			mBarOvalArea != null && mBarOverlayPaint != null)
		{
//			Log.i(TAG, "Drawing Progress Bar");
			
			
			fractionPlayedInColumns = 
				Math.round(
						(float)
						((float)mProgress/(float)mDuration
						*
						mWidth/(mDotPixelRadius*2+1)))
				/2;
			
			if(fractionPlayedInColumns != mFractionPlayedInColumns)
			{
				mFractionPlayedInColumns = fractionPlayedInColumns;
				generateProgressBitmap(fractionPlayedInColumns, mProgressBitmap);
			}
			
			canvas.drawBitmap(mProgressBitmap, 0, 0, mBarOverlayPaint);
			
			/* draw touch overlay */
			if(mTouching || System.currentTimeMillis() - mTouchingLastTimestamp < mOverlayTimeout){
				canvas.save();
				canvas.translate(
						Math.max(
								Math.min(mTouchingX-32, mWidth-64),
								0),
						Math.max(
								Math.min(mTouchingY-32, mHeight-64),
								0));
				mTouchOverlayPaint.setAlpha(
						(int) 
						(255 * (mOverlayTimeout - System.currentTimeMillis() + mTouchingLastTimestamp) / mOverlayTimeout));
				canvas.drawCircle(
						32, 
						32, 
						32, // FIXME: should be a constant... 
						mTouchOverlayPaint);
				canvas.restore();
//				if(!mTouching)
					invalidate();
					Log.i(TAG, "drawing touch");
			}			
		}
	}
	
	/* optimization */
	Canvas 	progressCanvas = new Canvas();
	Path 	progressPath = new Path();
	private void generateProgressBitmap(int columns, Bitmap bitmap){

		bitmap.eraseColor(Color.TRANSPARENT);
		
		progressCanvas.setBitmap(bitmap);
		
		progressCanvas.save();
		
		/* columns of dots */
		for(int i=0; i<columns;i++){

			progressCanvas.restore();
			progressCanvas.save();
			
			progressCanvas.translate(
						2 * i * (mDotPixelRadius*2+1), 
						0);
				
			progressPath.reset();
			progressPath.lineTo(0, mHeight);
			progressPath.setFillType(FillType.INVERSE_WINDING);
			
			progressCanvas.drawPath(progressPath, mDotPaint);

		}
		
		progressCanvas.restore();
					
		progressCanvas.drawRect(mBarOvalArea, mBarOverlayPaint);
	}
	
}