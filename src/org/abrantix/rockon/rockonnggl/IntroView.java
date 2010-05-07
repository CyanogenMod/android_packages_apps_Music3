package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class IntroView extends View{

//	private class FadingImagePixel
//	{
//		int	alpha = 0;
//		int	increaseSpeed = 1;
//		boolean bright = false;
//	}
	
	int						mHeight = 0;
	int						mWidth = 0;
	Bitmap					mLogoBitmap;
	Fader					mFader = new Fader();
	Handler					mPassIntroHandler;
	
	public void setIntroBitmap(Bitmap b)
	{
		mLogoBitmap = b;
	}
	
	public void setDoneHandler(Handler h)
	{
		mPassIntroHandler = h;
	}
	
	public IntroView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void  onSizeChanged  (int w, int h, int oldw, int oldh)
	{
		mWidth = w;
		mHeight = h;
	}
	
//	/**
//	 * 
//	 * @param b
//	 */
//	private void fillLogoBitmap(Bitmap b)
//	{
//		Paint p = new Paint();
//		p.setAntiAlias(true);
//		p.setDither(true);
//		p.setTextAlign(Align.CENTER);
//		p.setTextSize(b.getHeight() * 1.f);
//		p.setColor(Color.WHITE);
//		
//		Canvas c = new Canvas();
//		c.setBitmap(b);
//		c.drawText("3", b.getWidth() * .5f, b.getHeight() * 0.f * .5f, p);
//	}
	
	
	@Override
	public void onDraw(Canvas canvas)
	{
		if(mLogoBitmap != null)
		{
			canvas.translate(
					(mWidth - mLogoBitmap.getWidth()) * .5f,
					(mHeight - mLogoBitmap.getHeight()) * .5f); 
			mFader.step();
			canvas.drawBitmap(mLogoBitmap, new Matrix(), mFader.mFaderPaint);
		}
		
		if(mFader.done())
		{
			if(mPassIntroHandler != null)
				mPassIntroHandler.sendEmptyMessage(0);
		}
		else
		{
			invalidate();
		}
	}
	
	
	private class Fader
	{
		private static final String TAG = "Fader";
		
		final int FADE_STATE_INIT = 0;
		final int FADE_STATE_IN = 1;
		final int FADE_STATE_SHOWING = 2;
		final int FADE_STATE_OUT = 3;
		final int FADE_STATE_DONE = 4; // needed?
		
		public void Fader()
		{
			mFaderPaint.setAntiAlias(true);
			mFaderPaint.setDither(true);
		}
		
		Paint 	mFaderPaint = new Paint();
		int		mAlpha = 0;
		final	int FADE_IN_DURATION = 500; // ms
		final	int FADE_OUT_DURATION = 500; // ms
		final	int SHOW_DURATION = 1500; // ms
		
		double	mLastStepTime = 0;
		double	mShowStartTime = 0;
		int 	mState = FADE_STATE_INIT;
		
		// set the paint opacity, switch state, update variables
		public void step()
		{
			switch (mState) {
			case FADE_STATE_INIT:
				mAlpha = 1;
				mState = FADE_STATE_IN;
				break;
			case FADE_STATE_IN:
				mAlpha += 
					(System.currentTimeMillis() - mLastStepTime)/FADE_IN_DURATION
					*
					255;
				if(mAlpha >= 255)
				{
					mAlpha = 255;
					mState = FADE_STATE_SHOWING;
					mShowStartTime = System.currentTimeMillis();
				}
				break;
			case FADE_STATE_SHOWING:
				if(System.currentTimeMillis() - mShowStartTime > SHOW_DURATION)
				{
					mState = FADE_STATE_OUT;
				}
				break;
			case FADE_STATE_OUT:
				mAlpha -= 
					(System.currentTimeMillis() - mLastStepTime)/FADE_OUT_DURATION
					*
					255;
				if(mAlpha <= 0)
				{
					mAlpha = 0;
					mState = FADE_STATE_DONE;
				}
				break;
			default:
				break;
			}
			
//			Log.i(TAG, "alpha: "+mAlpha);
			mFaderPaint.setAlpha(mAlpha);
			mLastStepTime = System.currentTimeMillis();
		}
		
		public boolean done()
		{
			if(mState == FADE_STATE_DONE)
				return true;
			else
				return false;
		}
	}
}
