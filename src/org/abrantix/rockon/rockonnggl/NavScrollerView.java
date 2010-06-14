package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class NavScrollerView extends View{

	private static final String TAG = "NavScrollerView";

	boolean mInit = false;
	
	RockOnRenderer 	mRockOnRenderer;
	char[]			mLastChar = new char[1];
	char			oLastChar = ' ';
	String			mLetterString = new String(mLastChar);
	boolean			mIsTouching = false;
	
	float	mPosition = 0.f;
	
	int		mTargetAlpha = 0;
	int		mAlpha = 0;
	float	mSmooth = .1f;
	int		mAlphaDelta = 5;
	
	int		mWidth;
	int		mHeight;
	
	float	mMarginVertical = 5.f;
	
	Paint	mScrollBgPaint;
	int[]	mScrollBgColors = 
	{
			Color.argb(100, 127, 127, 127),
			Color.argb(100, 80, 80, 80)
	};
	float[]	mScrollBgColorPositions = 
	{
			0.f,
			1.f
	};
	RectF	mScrollBgRect;
	float	mScrollBgRoundRectRadius;

//	Paint	mScrollBgHintPaint;
//	int[]	mScrollBgHintColors = 
//	{
//			Color.argb(255, 240, 240, 240),
//			Color.argb(255, 192, 192, 192)
//	};

	Paint	mScrollerPaint;
	int[]	mScrollerColors = 
	{
			Color.argb(255, 240, 240, 240),
			Color.argb(255, 127, 127, 127)
	};
	float[]	mScrollerColorPositions = 
	{
			0.f,
			1.f
	};
	RectF	mScrollerRect;
	float	mScrollerRoundRectRadius;
	float	mScrollerHeightFraction = .05f;
	
	Paint	mLetterPaint;
	float	mLetterSize;
	int[]	mLetterColors = 
	{
			Color.argb(255, 32, 32, 32),
			Color.argb(255, 127, 127, 127)
	};
	float[]	mLetterColorPositions = 
	{
			0.f,
			1.f
	};
	Paint	mLetterHintPaint;
	int		mLetterHintColor = Color.WHITE;
	Paint	mLetterShadowPaint;
	int		mLetterShadowColor = Color.argb(255, 146, 185, 240);
	
	Paint	mLetterBgPaint;
	int[]	mLetterBgColors = 
	{
			Color.argb(255, 240, 240, 240),
			Color.argb(255, 127, 127, 127)
	};
	float[]	mLetterBgColorPositions = 
	{
			0.f,
			1.f
	};
	RectF	mLetterBgRect;
	float	mLetterBgRoundRectRadius;
	
	
	
	public NavScrollerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mLastChar[0] = ' ';
//		mLastChar[1] = 0;
		mLetterString = new String(mLastChar);
	}
	
	/**
	 * 
	 * @param renderer
	 */
	public void setRenderer(RockOnRenderer renderer)
	{
		mRockOnRenderer = renderer;
	}
	
	/**
	 * This is called after the view has been inflated
	 * 
	 * 	- that is, when it has been assigned dimensions
	 * 	- if you call it before it will give you width and height = 0
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	
		mWidth = w;
		mHeight = h;
		
		initializeScrollBgPaintStuff();
		
		mInit = true;
	}
	
	private void initializeScrollBgPaintStuff()
	{
		// Bg Paint
		mScrollBgPaint = new Paint();
		mScrollBgPaint.setAntiAlias(true);
		mScrollBgPaint.setShader(
				new LinearGradient(
						.75f * mWidth, 0, 
						.75f * mWidth, mHeight, 
						mScrollBgColors, 
						mScrollBgColorPositions, 
						TileMode.CLAMP));
		mScrollBgPaint.setStyle(Style.STROKE);
		mScrollBgPaint.setStrokeWidth(1.33f);
		
//		mScrollBgHintPaint = new Paint();
//		mScrollBgHintPaint.setAntiAlias(true);
//		mScrollBgHintPaint.setShader(
//				new LinearGradient(
//						.75f * mWidth, 0, 
//						.75f * mWidth, mHeight, 
//						mScrollBgHintColors, 
//						mScrollBgColorPositions, 
//						TileMode.CLAMP));
//		mScrollBgHintPaint.setStyle(Style.STROKE);
//		mScrollBgHintPaint.setStrokeWidth(1.33f);
		
		// Round Rect
		mScrollBgRect = new RectF(
				0.84f * mWidth, mMarginVertical, 
				0.89f * mWidth, mHeight - mMarginVertical);
		mScrollBgRoundRectRadius = mWidth/40.f;
		
		// Scroller Paint
		mScrollerPaint = new Paint();
		mScrollerPaint.setAntiAlias(true);
		mScrollerPaint.setShader(
				new LinearGradient(
						.75f * mWidth, 0, 
						.75f * mWidth, mHeight * mScrollerHeightFraction, 
						mScrollerColors, 
						mScrollerColorPositions, 
						TileMode.CLAMP));
//		mScrollBgPaint.setStyle(Style.STROKE);
//		mScrollBgPaint.setStrokeWidth(1.33f);
		
		// Round Rect
		mScrollerRect = new RectF(
				0.84f * mWidth, mMarginVertical, 
				0.89f * mWidth, mMarginVertical + mScrollerHeightFraction * mHeight);
		mScrollerRoundRectRadius = mWidth/40.f;
		
		// set letter size
		mLetterSize = mWidth * .5f;

		// Letter paint
		mLetterPaint = new Paint();
		mLetterPaint.setAntiAlias(true);
		mLetterPaint.setTextSize(mLetterSize);
		mLetterPaint.setTextAlign(Align.CENTER);
		mLetterPaint.setShader(
				new LinearGradient(
						.25f * mWidth, 0, 
						.25f * mWidth, mLetterSize, 
						mLetterColors, 
						mLetterColorPositions, 
						TileMode.CLAMP));
		
		mLetterHintPaint = new Paint();
		mLetterHintPaint.setAntiAlias(true);
		mLetterHintPaint.setTextSize(mLetterSize);
		mLetterHintPaint.setTextAlign(Align.CENTER);
		mLetterHintPaint.setColor(mLetterHintColor);
		
		mLetterShadowPaint = new Paint();
		mLetterShadowPaint.setAntiAlias(true);
		mLetterShadowPaint.setTextSize(mLetterSize);
		mLetterShadowPaint.setTextAlign(Align.CENTER);
		mLetterShadowPaint.setShadowLayer(
				mLetterSize * .15f, // shadow size
				0.f, 0.f, // x, y offset
				Color.argb(255, 146, 185, 240)); // shadow color
		mLetterPaint.setSubpixelText(true);
		
		// Letter Bg Paint
		mLetterBgPaint = new Paint();
		mLetterBgPaint.setAntiAlias(true);
		mLetterBgPaint.setShader(
				new LinearGradient(
						.3f * mWidth, -mLetterSize * (.3f + .125f), 
						.3f * mWidth, mLetterSize * (.89f - .125f), 
						mLetterBgColors, 
						mLetterBgColorPositions, 
						TileMode.CLAMP));
//		mScrollBgPaint.setStyle(Style.STROKE);
//		mScrollBgPaint.setStrokeWidth(1.33f);
		mLetterBgRect = new RectF(
				-mLetterSize * 0.3f, -mLetterSize * (.3f + .125f), 
				mLetterSize * .89f, mLetterSize * (.89f - .125f));
//				-mLetterSize * 0.25f, -mLetterSize * (.25f + .125f), 
//				mLetterSize * .84f, mLetterSize * (.84f - .125f));
		mLetterBgRoundRectRadius = mWidth/40.f;
		
	}
	
	/**
	 * 
	 * @param isTouching
	 */
	public void setTouching(boolean isTouching)
	{
		mIsTouching = isTouching;
	}
	
	/**
	 * 
	 * @param pos
	 */
	public void updatePosition(float pos)
	{
		if(!mIsTouching)
		{
			setOpaque();
			mPosition = pos;
			invalidate();
		}
	}
	
	/**
	 * 
	 */
	public void fadeOut()
	{
		mTargetAlpha = 0;
		invalidate();
//		Log.i(TAG, "Fading out!");
	}
	
	/**
	 * 
	 * @param y
	 */
	public boolean isTouchNearHandle(float y)
	{
		if(Math.abs(y - mPosition * (mHeight - 2.f*mMarginVertical)) < mHeight * .1f)
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * @param y
	 * @return
	 */
	public float getPositionFromY(int y)
	{
		return (y - mMarginVertical) / (mHeight - 2.f * mMarginVertical);
	}
	
	public float getCurrentPosition()
	{
		return mPosition;
	}
	
	/**
	 * 
	 * @param y
	 */
	public void manualScrollToY(int y)
	{
		setOpaque();
		mPosition = (y - mMarginVertical) / (mHeight - 2.f * mMarginVertical);
		if(mPosition < 0)
			mPosition = 0;
		else if(mPosition > 1)
			mPosition = 1;
		updateLetter(mPosition);
	}
	
	public void setOpaque()
	{
		mTargetAlpha = 255;
		mAlpha = 255;			
	}
	
	/**
	 * 
	 * @param pos
	 */
	private void updateLetter(float pos)
	{
		if(mRockOnRenderer != null)
		{
			if(mLastChar[0] != (oLastChar = mRockOnRenderer.getFirstLetterInPosition(pos)))
			{
				mLastChar[0] = oLastChar;
				mLetterString = new String(mLastChar).toUpperCase();
			}
		}
	}
	
	/**
	 * This is our drawing stuff
	 */
	@Override
	protected void onDraw(Canvas c)
	{
		if(mInit)
		{
			// UpdateAlpha
			mAlpha = updateAlpha();
			
			// Draw bar background
			drawScrollBg(c, mAlpha);
			
			// Draw bar scroller
			drawScroller(c, mAlpha);
			
			// Draw current letter
			if(mIsTouching)
				drawLetter(c, mAlpha);
			
			// NeedsRedraw
			if(shouldRedraw())
				invalidate();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	float oDelta;
	private int updateAlpha()
	{
		oDelta = mSmooth * (mTargetAlpha - mAlpha);
		if(Math.abs(oDelta) < mAlphaDelta)
			oDelta = Math.signum(oDelta) * mAlphaDelta;
		mAlpha += oDelta;
		if(mAlpha < 0)
			mAlpha = 0;
		else if(mAlpha > 255)
			mAlpha = 255;
		return mAlpha;
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean shouldRedraw()
	{
//		Log.i(TAG, "TAlpha: "+ mTargetAlpha + " Alpha: "+mAlpha);
		if(mTargetAlpha != mAlpha)
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * @param c
	 * @param alpha
	 */
	private void drawScrollBg(Canvas c, int alpha)
	{
//		c.save();
//		
//		// hinting
//		c.translate(0.f, 1.33f);
//		c.drawRoundRect(
//				mScrollBgRect, 
//				mScrollBgRoundRectRadius, 
//				mScrollBgRoundRectRadius, 
//				mScrollBgHintPaint);
//		
//		c.restore();
		
		// +real bg
		mScrollBgPaint.setAlpha(alpha);
		c.drawRoundRect(
				mScrollBgRect, 
				mScrollBgRoundRectRadius, 
				mScrollBgRoundRectRadius, 
				mScrollBgPaint);
		

	}
		
	public void drawScroller(Canvas c, int alpha)
	{
		c.save();
	
		mScrollerPaint.setAlpha(alpha);
//		c.translate(0, mMarginVertical + mPosition * (mHeight - 2.f * mMarginVertical));
		c.translate(0, mPosition * (mHeight - 2.f * mMarginVertical - mScrollerHeightFraction * mHeight));
		c.drawRoundRect(
				mScrollerRect, 
				mScrollerRoundRectRadius, 
				mScrollerRoundRectRadius,
				mScrollerPaint);
		
		c.restore();
	}
	
	public void drawLetter(Canvas c, int alpha)
	{
		c.save();
		
		mLetterPaint.setAlpha(alpha);
		mLetterBgPaint.setAlpha(alpha);
		mLetterHintPaint.setAlpha(alpha);
		
		c.translate(
				.15f * mWidth,
				Math.max(
						Math.min(
								mPosition * (mHeight - 2.f * mMarginVertical - mScrollerHeightFraction * mHeight),
								mHeight - mLetterBgRect.height() - mLetterBgRect.top),
						-mLetterBgRect.top)
				);

		c.drawRoundRect(mLetterBgRect, mLetterBgRoundRectRadius, mLetterBgRoundRectRadius, mLetterBgPaint);
		
//		c.drawText("A", 0.f, (mLetterSize + mScrollerHeightFraction * mHeight) * .5f, mLetterShadowPaint);
		c.drawText(mLetterString, mLetterSize * .31f, (mLetterSize * .775f + mScrollerHeightFraction * mHeight) * .5f + 1.f, mLetterHintPaint);
		c.drawText(mLetterString, mLetterSize * .31f, (mLetterSize * .775f + mScrollerHeightFraction * mHeight) * .5f, mLetterPaint);
		
		c.restore();
	}

}
