package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class EqView extends View {
	private static final String TAG = "EqView";
	
	private final static int MODE_DIFF = 0;
	private final static int MODE_ABS = 1;
	
	private final static float ALL_AROUND_MARGIN = 6.f;
	private final static float LABEL_SQUARE_SIZE = 24.f;
	private final static float OPTION_BUTTONS_SECTION_SIZE = 0.f;
	private final static float EQ_INNER_PADDING = 16.f;
	
	private final static int LIGHT_OVERLAY = Color.argb(192, 224, 224, 224);
	private final static int DARK_OVERLAY = Color.argb(192, 64, 64, 64);
	
	private final static int RED_LIGHT = Color.argb(255, 250, 128, 64);
	private final static int RED_DARK = Color.argb(255, 234, 0, 0);
	private final static int BLUE_LIGHT = Color.argb(255, 64, 128, 250);
	private final static int BLUE_DARK = Color.argb(255, 0, 0, 234);
	
	private final static int GRAY_LIGHT = Color.argb(255, 140, 140, 140);
	private final static int GRAY_DARK = Color.argb(255, 70, 70, 70);

	EqualizerActivity mEqActivity;
	
	int mWidth;
	int mHeight;
	
	int mMode = MODE_ABS;
	
	int mMovingBand = -1;
	
	float	mDpi = 1.5f; // Needs proper initialization;
	
	boolean mIsLaidOut = false;
	
	EqSettings	mEqSettings;
	Paint		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DEV_KERN_TEXT_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
//	Paint		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.DEV_KERN_TEXT_FLAG);
	Path 		mEqAreaPath = new Path();
	Path 		mEqContourPath = new Path();
	
	Bitmap			mHandle;
	LinearGradient	mEqAreaGradientOn;
	LinearGradient	mEqAreaGradientOff;
	
	float[][]		mBandXYs;
	float[]			mLevelXs;
	
	public EqView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mEqActivity = (EqualizerActivity)context;
	}

	int oIdxTouch;
	float oXTouch;
	float oYTouch; 
	float oDistance;
	float oMinDistance;
	int oCloserBand;
	public int setMovingBandByXY(float x, float y) {
		oMinDistance = 1000000000;
		for(oIdxTouch=0; oIdxTouch<mEqSettings.mNumBands; oIdxTouch++) {
			oXTouch = mBandXYs[oIdxTouch][0];
			oYTouch = mBandXYs[oIdxTouch][1];
			oDistance = Math.abs(x-oXTouch)+Math.abs(y-oYTouch);
			if(oDistance<oMinDistance) {
				oCloserBand = oIdxTouch;
				oMinDistance = oDistance;
			}
		}
		if(oMinDistance<64.f)
			mMovingBand = oCloserBand;
		else
			mMovingBand = -1;
		return mMovingBand;
	}
	
	float oNewLevelX;
	float oNewLevelInPercent;
	int   oNewLevel;
	public float adjustBandGain(float scrollx, float scrollY) {
		if(mEqSettings.isEnabled()) {
			mBandXYs[mMovingBand][0] -= scrollx;
			mBandXYs[mMovingBand][0] = Math.min(getFinalEqX()-EQ_INNER_PADDING, mBandXYs[mMovingBand][0]);
			mBandXYs[mMovingBand][0] = Math.max(getInitialEqX()+EQ_INNER_PADDING, mBandXYs[mMovingBand][0]);
			
			oNewLevelX = mBandXYs[mMovingBand][0];
			oNewLevelInPercent = (oNewLevelX-(getInitialEqX()+EQ_INNER_PADDING))/(getFinalEqX()-getInitialEqX()-2*EQ_INNER_PADDING);
			oNewLevel = mEqSettings.getClosestGain(oNewLevelInPercent);
			mEqSettings.mBandLevels[mMovingBand] = oNewLevel;
			
			invalidate();
			return mBandXYs[mMovingBand][0];
		} else {
			return 0;
		}
		
	}
	
	public void finalizeMovement(float x, float y) {
		// Commit changes to service
		if(mMovingBand>=0)
			mEqActivity.commitBandLevel(mMovingBand, mEqSettings.mBandLevels[mMovingBand]);
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
		if(mEqSettings!=null)
			calculateBandAndLevelXYs();
		setStuff();
		mIsLaidOut = true;
	}
	
	public void setEqSettings(EqSettings settings) {
		mEqSettings = settings;
		if(mIsLaidOut)
			calculateBandAndLevelXYs();
	}
	
	private void calculateBandAndLevelXYs() {
		oLevelRangeInX = getLevelRangeX();  
		oLowerLevelX = getInitialEqX() + EQ_INNER_PADDING;
		oBandRangeInY = getBandRangeY();
		oFirstBandY = getInitialEqY() + EQ_INNER_PADDING;
		
		mBandXYs = new float[mEqSettings.mNumBands][2];
		for(oIdx=0; oIdx<mEqSettings.mNumBands; oIdx++) {
			oX = oLowerLevelX + mEqSettings.getBandLevelInPercent(oIdx)*oLevelRangeInX;
			oY = oFirstBandY + oIdx/(float)(mEqSettings.mNumBands-1)*oBandRangeInY;
			mBandXYs[oIdx][0] = oX;
			mBandXYs[oIdx][1] = oY;
		}
		
		mLevelXs = new float[mEqSettings.getHumanLevelRange()];
		for(oIdx=0; oIdx<mEqSettings.getHumanLevelRange(); oIdx++) {
			oX = oLowerLevelX + mEqSettings.getHumanLevelInPercent(oIdx)*oLevelRangeInX;
			mLevelXs[oIdx] = oX;
		}

	}
	
	private void setStuff() {
		switch(mMode) {
		case MODE_ABS:
			mEqAreaGradientOn = new LinearGradient(
					getInitialEqX(), getInitialEqY(), 
					getFinalEqX(), getInitialEqY(), 
					new int[]{RED_LIGHT, RED_DARK}, new float[]{0.f, 1.f}, 
					TileMode.CLAMP);
			mEqAreaGradientOff = new LinearGradient(
					getInitialEqX(), getInitialEqY(), 
					getFinalEqX(), getInitialEqY(), 
					new int[]{GRAY_DARK, GRAY_LIGHT}, new float[]{0.f, 1.f}, 
					TileMode.CLAMP);
			break;
		case MODE_DIFF:
			mEqAreaGradientOn = new LinearGradient(
					getInitialEqX(), getInitialEqY(), 
					getFinalEqX(), getInitialEqY(), 
					new int[]{BLUE_DARK, BLUE_LIGHT, Color.TRANSPARENT, RED_LIGHT, RED_DARK}, new float[]{0.f, 0.475f, .5f, 0.525f, 1.f}, 
					TileMode.CLAMP);
			mEqAreaGradientOff = new LinearGradient(
					getInitialEqX(), getInitialEqY(), 
					getFinalEqX(), getInitialEqY(), 
					new int[]{GRAY_DARK, GRAY_LIGHT, Color.TRANSPARENT, GRAY_LIGHT, GRAY_DARK}, new float[]{0.f, 0.475f, .5f, 0.525f, 1.f}, 
					TileMode.CLAMP);
			break;
		}
	}
	
	private Bitmap createHandle() {
		Bitmap bm = BitmapFactory.decodeResource(mEqActivity.getResources(), R.drawable.eq_knob); 
//			Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
		return bm;
	}
	
	private float getInitialEqX() {
		return ALL_AROUND_MARGIN + LABEL_SQUARE_SIZE;
	}
	
	private float getInitialEqY() {
		return ALL_AROUND_MARGIN + LABEL_SQUARE_SIZE;
	}
	
	private float getFinalEqX() {
		return mWidth-ALL_AROUND_MARGIN-LABEL_SQUARE_SIZE;
	}
	
	private float getFinalEqY() {
		return mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE;
	}

	private float getLevelRangeX() {
		return getFinalEqX()-getInitialEqX()-2*EQ_INNER_PADDING;
	}
	
	private float getBandRangeY() {
		return getFinalEqY()-getInitialEqY()-2*EQ_INNER_PADDING;
	}
	
	private float getEqMiddleLevelX() {
		return getInitialEqX() + (getFinalEqX()-getInitialEqX())*.5f;
	}
	@Override
	protected void onDraw(Canvas c)
	{
		if(mIsLaidOut && mEqSettings != null)
		{
			drawEqBox(c);
			drawEqGrid(c);
			drawEqArea(c);
			drawEqOverlayPoints(c);
			drawEqLabels(c);
//			drawPresets(c);
//			drawFx(c);
		}
	}
	
	private void drawEqBox(Canvas c) {
//		mPaint.setColor(DARK_OVERLAY);
//		mPaint.setStrokeWidth(.2f);
//		c.drawLine(getInitialEqX(), getInitialEqY(), mWidth-ALL_AROUND_MARGIN-2*LABEL_SQUARE_SIZE, ALL_AROUND_MARGIN+LABEL_SQUARE_SIZE, mPaint);
//		c.drawLine(getInitialEqX(), getInitialEqY(), ALL_AROUND_MARGIN+LABEL_SQUARE_SIZE, mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE, mPaint);
//		mPaint.setColor(Color.BLUE);
//		c.drawRect(getInitialEqX()-LABEL_SQUARE_SIZE, getInitialEqY()-LABEL_SQUARE_SIZE, getFinalEqX()+LABEL_SQUARE_SIZE, getFinalEqY()+LABEL_SQUARE_SIZE+OPTION_BUTTONS_SECTION_SIZE, mPaint);
//		mPaint.setColor(Color.CYAN);
//		c.drawRect(getInitialEqX()-LABEL_SQUARE_SIZE, getInitialEqY()-LABEL_SQUARE_SIZE, getFinalEqX()+LABEL_SQUARE_SIZE, getFinalEqY()+LABEL_SQUARE_SIZE, mPaint);
//		mPaint.setColor(Color.MAGENTA);
//		c.drawRect(getInitialEqX(), getInitialEqY(), getFinalEqX(), getFinalEqY(), mPaint);
//		mPaint.setColor(Color.YELLOW);
//		c.drawRect(getInitialEqX()+EQ_INNER_PADDING, getInitialEqY()+EQ_INNER_PADDING, getFinalEqX()-EQ_INNER_PADDING, getFinalEqY()-EQ_INNER_PADDING, mPaint);
		
	}
	
	private void drawEqGrid(Canvas c) {
		mPaint.setStrokeCap(Cap.ROUND);
		mPaint.setStrokeWidth(.15f);
		for(oIdx=0; oIdx<mEqSettings.mNumBands; oIdx++) {
			oY = mBandXYs[oIdx][1];
			mPaint.setColor(DARK_OVERLAY);
			c.drawLine(getInitialEqX(), oY, getFinalEqX(), oY, mPaint);
			mPaint.setColor(LIGHT_OVERLAY);
			c.drawLine(getInitialEqX(), oY+1.f/mDpi, getFinalEqX(), oY+1.f/mDpi, mPaint);
		}
		for(oIdx=0; oIdx<mEqSettings.getHumanLevelRange(); oIdx++) {
			oX = mLevelXs[oIdx];
			mPaint.setColor(DARK_OVERLAY);
			c.drawLine(oX, getInitialEqY(), oX, getFinalEqY(), mPaint);
			mPaint.setColor(LIGHT_OVERLAY);
			c.drawLine(oX+1.f/mDpi, getInitialEqY(), oX+1.f/mDpi, getFinalEqY(), mPaint);
		}
		mPaint.setStrokeWidth(1.f);
	}
	
	int oIdx;
	float oX;
	float oY;
	float oLastX;
	float oLastY;
	float oLevelRangeInX;
	float oLowerLevelX;
	float oBandRangeInY;
	float oFirstBandY;
	private void drawEqArea(Canvas c) {
//		oLevelRangeInX = getLevelRangeX();  
//		oLowerLevelX = getInitialEqX() + EQ_INNER_PADDING;
//		oBandRangeInY = getBandRangeY();
//		oFirstBandY = getInitialEqY() + EQ_INNER_PADDING;
		mPaint.setColor(LIGHT_OVERLAY);
		mEqAreaPath.reset();
		mEqContourPath.reset();
		switch(mMode) {
		case MODE_ABS:
			mEqAreaPath.moveTo(getInitialEqX(), getInitialEqY());
			break;
		case MODE_DIFF:
			mEqAreaPath.moveTo(getEqMiddleLevelX(), getInitialEqY());
			break;
		}
		
		for(oIdx=0; oIdx<mEqSettings.mNumBands; oIdx++) {
			oX = mBandXYs[oIdx][0];
			oY = mBandXYs[oIdx][1];
			if(oIdx==0) {
				mEqAreaPath.lineTo(oX, oFirstBandY-EQ_INNER_PADDING);
				mEqContourPath.moveTo(oX, oFirstBandY-EQ_INNER_PADDING);
				oLastX = oX;
				oLastY = oFirstBandY-EQ_INNER_PADDING;
			}
			mEqAreaPath.cubicTo(
					oLastX, (oLastY+oY)*.5f, 
					oX, (oLastY+oY)*.5f, 
					oX, oY);
			mEqContourPath.cubicTo(
					oLastX, (oLastY+oY)*.5f, 
					oX, (oLastY+oY)*.5f, 
					oX, oY);
			oLastX = oX;
			oLastY = oY;
		}
		// connect to the side
		mEqAreaPath.lineTo(oLastX, mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE);
		mEqContourPath.lineTo(oLastX, mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE);
		// go to the origin
		switch(mMode) {
		case MODE_ABS:
			mEqAreaPath.lineTo(ALL_AROUND_MARGIN+LABEL_SQUARE_SIZE, mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE);	
			break;
		case MODE_DIFF:
			mEqAreaPath.lineTo(ALL_AROUND_MARGIN+LABEL_SQUARE_SIZE+EQ_INNER_PADDING+oLevelRangeInX*.5f, mHeight-ALL_AROUND_MARGIN-OPTION_BUTTONS_SECTION_SIZE-LABEL_SQUARE_SIZE);
			break;
		}
		// close the area
		mEqAreaPath.close();
		if(mEqSettings.isEnabled())
			mPaint.setShader(mEqAreaGradientOn);
		else
			mPaint.setShader(mEqAreaGradientOff);
		c.drawPath(mEqAreaPath, mPaint);
		mPaint.setShader(null);
		
		mPaint.setStrokeWidth(3.5f);
		mPaint.setStyle(Style.STROKE);
		if(mEqSettings.isEnabled())
			mPaint.setColor(RED_LIGHT);
		else
			mPaint.setColor(LIGHT_OVERLAY);
		c.drawPath(mEqContourPath, mPaint);
		mPaint.setStrokeWidth(1.f);
		mPaint.setStyle(Style.FILL_AND_STROKE);
		mPaint.setColor(LIGHT_OVERLAY);		
	}
	
	private void drawEqOverlayPoints(Canvas c) {
		if(mHandle == null)
			mHandle = createHandle();
		for(oIdx=0; oIdx<mEqSettings.mNumBands; oIdx++) {
			oX = mBandXYs[oIdx][0] - mHandle.getWidth()/2;
			oY = mBandXYs[oIdx][1] - mHandle.getHeight()/2;
			c.drawBitmap(mHandle, oX, oY, mPaint);
		}
	}
	
	int oHz;
	String oLabel;
	private void drawEqLabels(Canvas c) {
		mPaint.setStrokeWidth(.2f);
		mPaint.setTextAlign(Align.CENTER);
		mPaint.setTextSize(LABEL_SQUARE_SIZE*.4f);
		for(oIdx=0; oIdx<mEqSettings.mNumBands; oIdx++) {
			oX = getInitialEqX() - LABEL_SQUARE_SIZE*.5f;
			oY = mBandXYs[oIdx][1] + LABEL_SQUARE_SIZE*.4f*.5f*.75f;
			oHz = mEqSettings.mBandHz[oIdx];
			if(oHz>=1000000)
				oLabel = String.valueOf(oHz/1000000)+"kHz";
			else
				oLabel = String.valueOf(oHz/1000)+"Hz";
			
			c.save();
			c.translate(oX, oY);
			c.rotate(-90.f, 0, -LABEL_SQUARE_SIZE*.4f*.5f*.75f*.5f);
			c.drawText(oLabel, 0, 0, mPaint);
			c.restore();
			
			oX = getFinalEqX() + LABEL_SQUARE_SIZE*.5f;
			c.save();
			c.translate(oX, oY);
			c.rotate(+90.f, 0, -LABEL_SQUARE_SIZE*.4f*.5f*.75f*.5f);
			c.drawText(oLabel, 0, 0, mPaint);
			c.restore();
		}
		for(oIdx=0; oIdx<mEqSettings.getHumanLevelRange(); oIdx++) {
			oX = mLevelXs[oIdx];
			oY = getInitialEqY() - LABEL_SQUARE_SIZE*.5f + LABEL_SQUARE_SIZE*.4f*.5f*.75f;
			oLabel = mEqSettings.getHumanLevel(oIdx);
			
			c.save();
			c.translate(oX, oY);
			c.drawText(oLabel, 0, 0, mPaint);
			c.restore();
			
			oY = getFinalEqY() + LABEL_SQUARE_SIZE*.5f + LABEL_SQUARE_SIZE*.4f*.5f*.75f;
			c.save();
			c.translate(oX, oY);
			c.rotate(-180.f, 0, -LABEL_SQUARE_SIZE*.4f*.5f*.75f*.5f);
			c.drawText(oLabel, 0, 0, mPaint);
			c.restore();
		}
	}
	
//	private void drawPresets(Canvas c) {
//		
//	}
//	
//	private void drawFx(Canvas c) {
//		
//	}
}
