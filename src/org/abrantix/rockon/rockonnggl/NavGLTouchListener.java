package org.abrantix.rockon.rockonnggl;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.provider.MediaStore;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class NavGLTouchListener implements OnTouchListener{
    
	final	String	TAG = "NavGLTouchListener";
	
	int		mItemDimension;

	float	mDownX = 0.f;
	float	mDownY = 0.f;
	double	mDownTimestamp = 0;
	
	float 	lastX = 0.f;
	float 	lastY = 0.f;
	double 	lastTimestamp = 0;
	
	float	mScrollingSpeed = 0.f;
		
	boolean	mScrolling = false;
	boolean mScrollingX = false;
	boolean mScrollingY = false;
	boolean mLongClick = false; // filter repeating longclick requests
	
//	RockOnCubeRenderer 	mRenderer = null;
	RockOnRenderer 		mRenderer = null;
	Handler				mTimeoutHandler = null;
	
	Handler	mClickHandler = null;

	public void setRenderer(RockOnRenderer renderer){
		this.mRenderer = (RockOnRenderer) renderer;
	}
	
	public void setTimeoutHandler(Handler timeoutHandler){
		this.mTimeoutHandler = timeoutHandler;
		mTimeoutHandler.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT);
	}
	
	public void setClickHandler(Handler clickHandler){
		mClickHandler = clickHandler;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!mClickHandler.hasMessages(0)){
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mTimeoutHandler.removeMessages(0);
				mDownX = event.getX();
				mDownY = event.getY();
				mDownTimestamp = System.currentTimeMillis();
				
				lastX = mDownX;
				lastY = mDownY;
				mScrollingSpeed = 0.f;
				
				mItemDimension = mRenderer.getItemDimension(); 
					
//					(int) (Math.min(v.getWidth(), v.getHeight()) * 0.8f);
				
				// this might be not necessary
				if(mRenderer.mTargetPositionY > mRenderer.mPositionY)
					mRenderer.mTargetPositionY = (float) Math.ceil(mRenderer.mPositionY);
				else
					mRenderer.mTargetPositionY = (float) Math.floor(mRenderer.mPositionY);
	//			lastTimestamp = System.currentTimeMillis();
				mRenderer.saveRotationInitialPosition();
				mRenderer.renderNow();
				return true;
			case MotionEvent.ACTION_MOVE:
				if(!mScrolling){
					/* check if this is a long press */
					if(System.currentTimeMillis() - mDownTimestamp > 
						Constants.MIN_LONG_CLICK_DURATION)
					{
						if(mClickHandler != null && 
								!mLongClick &&
								!mClickHandler.hasMessages(Constants.SINGLE_CLICK) &&
								!mClickHandler.hasMessages(Constants.LONG_CLICK))
						{
							mClickHandler.removeCallbacksAndMessages(null);
							mClickHandler.sendEmptyMessageDelayed(
									Constants.LONG_CLICK, 
									Constants.CLICK_ACTION_DELAY);
							mRenderer.showClickAnimation();
							mLongClick = true;
						}
						return true;
					} 
					/* if not, then check if we started moving for real */
					else {
						if(Math.abs(event.getY() - mDownY) > 
							Constants.MIN_SCROLL_TOUCH_MOVE * mItemDimension &&
							!mRenderer.isSpinningX())
						{
							mScrolling = true;
							mScrollingY = true;
						} 
						else if(Math.abs(event.getX() - mDownX) > 
							Constants.MIN_SCROLL_TOUCH_MOVE * mItemDimension &&
							!mRenderer.isSpinningY())
						{
							mScrolling = true;
							mScrollingX = true;
						}
						else {
							return true;
						}
					}
				}
				
				if(mScrollingY){
					mRenderer.mTargetPositionY = mRenderer.mPositionY - (event.getY() - lastY)/mItemDimension;
					mRenderer.mPositionY = mRenderer.mTargetPositionY - 0.002f;
					mScrollingSpeed = 
						(float) 
						(-0.75 * (event.getY() - lastY)/mItemDimension
						+
						(1-0.75) * mScrollingSpeed);
				} else if(mScrollingX){
					mRenderer.mTargetPositionX = mRenderer.mPositionX - (event.getX() - lastX)/mItemDimension;
					mRenderer.mPositionX = mRenderer.mTargetPositionX - 0.002f;
					mScrollingSpeed = 
						(float) 
						(0.75 * (event.getX() - lastX)/mItemDimension
						+
						(1-0.75) * mScrollingSpeed);
				}
				

				lastY = event.getY();
				lastX = event.getX();
	//			Log.i(TAG, " - scrolling speed is "+scrollingSpeed);
	//			lastTimestamp = System.currentTimeMillis();
//				Log.i(TAG, "trigger rendering");
				mRenderer.renderNow();
				return true;
			case MotionEvent.ACTION_UP:

				/* is this a click? */
				if(!mScrolling &&
					// could also verify the Y axis but i like it how it goes back while scrolling
					!mRenderer.isSpinningX()) 
				{
					// we also check if the ymove is big enough 
					// reason: when yscrolling the mscrolling is not recorded
					if(System.currentTimeMillis() - mDownTimestamp < Constants.MAX_CLICK_DOWNTIME &&
						Math.abs(event.getY() - mDownY) <	// condition unnecessary? 
							Constants.MIN_SCROLL_TOUCH_MOVE * mItemDimension )
					{
						if(mClickHandler != null && 
							!mLongClick &&
							!mClickHandler.hasMessages(Constants.SINGLE_CLICK) &&
							!mClickHandler.hasMessages(Constants.LONG_CLICK))
						{
							mClickHandler.removeCallbacksAndMessages(null);
							mClickHandler.sendEmptyMessageDelayed(
									Constants.SINGLE_CLICK, 
									Constants.CLICK_ACTION_DELAY);
							mRenderer.showClickAnimation();
						}
						return false;
					} 
//					else { // long click
//						if(mClickHandler != null && 
//								!mLongClick &&
//								!mClickHandler.hasMessages(Constants.SINGLE_CLICK) &&
//								!mClickHandler.hasMessages(Constants.LONG_CLICK))
//						{
//							mClickHandler.removeCallbacksAndMessages(null);
//							mClickHandler.sendEmptyMessageDelayed(
//									Constants.LONG_CLICK, 
//									Constants.CLICK_ACTION_DELAY);
//							mRenderer.showClickAnimation();
//							mLongClick = true;
//						}
//						return false;
//					}
				}
				
				mTimeoutHandler.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT);
				
				if(mScrollingY){
					/* make the movement harder for lower rotations */
					if(Math.abs(mScrollingSpeed) < Constants.MAX_LOW_SPEED)
					{
						mRenderer.mTargetPositionY = 
							Math.round(
									mRenderer.mPositionY
									+
									0.5f * Math.signum(mScrollingSpeed) // needs to be .5f because of the rounding...
							);
					} 
//					else if(Math.abs(mScrollingSpeed) < 0.16f)
//					{
//						mRenderer.mTargetPositionY = 
//							Math.round(
//									mRenderer.mPositionY
//									+
//									2.f * Math.signum(mScrollingSpeed)
//							);
//					} 
					/* full speed ahead */
					else
					{
						mRenderer.mTargetPositionY = 
							Math.round(
									mRenderer.mPositionY
									+
									Constants.SCROLL_SPEED_BOOST
									*
									mScrollingSpeed
							);
					}
					/* small optimization to avoid weird moves on the edges */
					if(mRenderer.mTargetPositionY == -1)
						mRenderer.mTargetPositionY = -2;
					else if(mRenderer.mTargetPositionY == mRenderer.getAlbumCount())
						mRenderer.mTargetPositionY = mRenderer.getAlbumCount() + 1;
				} else if(mScrollingX){
					mRenderer.mTargetPositionX = Math.round(
							mRenderer.mPositionX
							-
							(Constants.SCROLL_SPEED_BOOST * mScrollingSpeed));
					/* small optimization to avoid weird moves on the edges */
//					if(mRenderer.mTargetPositionX == -1)
//						mRenderer.mTargetPositionX = -2;
//					else if(mRenderer.mTargetPositionX == 24)
//						mRenderer.mTargetPositionX = 24 + 1;
				}
				

				
				/* end touch movement */
				mScrolling = false;
				mScrollingX = false;
				mScrollingY = false;
				mLongClick = false;

				/* render */
				mRenderer.renderNow();
				return true;
			}
			return false;
		} else {
			return true;
		}
	}
}