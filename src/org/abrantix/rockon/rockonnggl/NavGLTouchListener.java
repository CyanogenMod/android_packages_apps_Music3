package org.abrantix.rockon.rockonnggl;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class NavGLTouchListener implements OnTouchListener{
    
	final	String	TAG = "NavGLTouchListener";
	
//	int		mItemDimension;

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
	Message	mMsg;

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
//			Log.i(TAG, "onTouch "+event.getAction());
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mTimeoutHandler.removeMessages(0);
				mDownX = event.getX();
				mDownY = event.getY();
				mDownTimestamp = System.currentTimeMillis();
				
				lastX = mDownX;
				lastY = mDownY;
				mScrollingSpeed = 0.f;
				
//				mItemDimension = mRenderer.getItemDimension(); 
				mRenderer.stopScrollOnTouch();
				mRenderer.saveRotationInitialPosition();
				mRenderer.renderNow();
				
				// AVOID EVENT FLOODING XXX
				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//
				
				return true;
			case MotionEvent.ACTION_MOVE:
				if(!mScrolling){
					/**
					 *  LONG PRESS 
					 */
					if(System.currentTimeMillis() - mDownTimestamp > 
						Constants.MIN_LONG_CLICK_DURATION)
					{
						if(mClickHandler != null && 
								!mLongClick &&
								!mClickHandler.hasMessages(Constants.SINGLE_CLICK) &&
								!mClickHandler.hasMessages(Constants.LONG_CLICK))
						{
							mClickHandler.removeCallbacksAndMessages(null);
							mMsg = new Message();
							mMsg.what = Constants.LONG_CLICK;
							mMsg.arg1 = (int)event.getX();
							mMsg.arg2 = (int)event.getY();
							mClickHandler.sendMessageDelayed(
									mMsg,
									mRenderer.getClickActionDelay());
//							mClickHandler.sendEmptyMessageDelayed(
//									Constants.LONG_CLICK,
//									mRenderer.getClickActionDelay());
////									Constants.CLICK_ACTION_DELAY);
							mRenderer.showClickAnimation(event.getX(), event.getY());
							mLongClick = true;
						}
						return true;
					} 
					/**
					 * DID WE START MOVING?
					 */
					else 
					{
						if(Math.abs(event.getY() - mDownY) > 
							Constants.MIN_SCROLL_TOUCH_MOVE * v.getHeight() &&
							!mRenderer.isSpinningX())
						{
							mScrolling = true;
							mScrollingY = true;
						} 
						else if(Math.abs(event.getX() - mDownX) > 
							Constants.MIN_SCROLL_TOUCH_MOVE * v.getWidth() &&
							!mRenderer.isSpinningY())
						{
							mScrolling = true;
							mScrollingX = true;
						}
						else {
//							Log.i(TAG, "not moving yet mDownX: "+mDownX+" X: "+event.getX()+" mDownY: "+mDownY+" Y: "+event.getY());
							return true;
						}
					}
				}
				
				/**
				 * VERTICAL MOVE
				 */
				if(mScrollingY)
				{
					mRenderer.scrollOnTouchMove(event.getY() - lastY, Constants.SCROLL_MODE_VERTICAL);
					
					mScrollingSpeed = 
						(float) 
						(-0.75 * (event.getY() - lastY) // /mItemDimension
						+
						(1-0.75) * mScrollingSpeed);
				}
				/**
				 * HORIZONTAL MOVE
				 */
				else if(mScrollingX)
				{
					mRenderer.scrollOnTouchMove(event.getX() - lastX, Constants.SCROLL_MODE_HORIZONTAL);

					mScrollingSpeed = 
						(float) 
						(0.75 * (event.getX() - lastX) // /mItemDimension
						+
						(1-0.75) * mScrollingSpeed);
				}
				
				/**
				 * SAVE STATE
				 */
				lastY = event.getY();
				lastX = event.getX();

				/**
				 * SHOW MOVEMENT
				 */
				mRenderer.renderNow();
				return true;
			case MotionEvent.ACTION_UP:
				
				/**
				 * WAS A CLICK
				 */
				if(!mScrolling &&
					// could also verify the Y axis but i like it how it goes back while scrolling
					!mRenderer.isSpinningX()) 
				{
					// we also check if the ymove is big enough 
					// reason: when yscrolling the mscrolling is not recorded
					if(System.currentTimeMillis() - mDownTimestamp < Constants.MAX_CLICK_DOWNTIME &&
						Math.abs(event.getY() - mDownY) <	// condition unnecessary? 
							Constants.MIN_SCROLL_TOUCH_MOVE * v.getHeight() )
					{
						if(mClickHandler != null && 
							!mLongClick &&
							!mClickHandler.hasMessages(Constants.SINGLE_CLICK) &&
							!mClickHandler.hasMessages(Constants.LONG_CLICK))
						{
							mClickHandler.removeCallbacksAndMessages(null);
							mMsg = new Message();
							mMsg.what = Constants.SINGLE_CLICK;
							mMsg.arg1 = (int) event.getX();
							mMsg.arg2 = (int) event.getY();
							mClickHandler.sendMessageDelayed(
									mMsg, 
									mRenderer.getClickActionDelay());
//							mClickHandler.sendEmptyMessageDelayed(
//									Constants.SINGLE_CLICK, 
//									mRenderer.getClickActionDelay());
//									Constants.CLICK_ACTION_DELAY);
							mRenderer.showClickAnimation(event.getX(), event.getY());
						}
						return false;
					} 
				}
				
				/**
				 * RESET THE INACTIVITY TIMER
				 */
				mTimeoutHandler.sendEmptyMessageDelayed(0, Constants.SCROLLING_RESET_TIMEOUT);
				
				/**
				 * INERTIAL VERTICAL MOVE
				 */
				if(mScrollingY){
					mRenderer.inertialScrollOnTouchEnd(mScrollingSpeed, Constants.SCROLL_MODE_VERTICAL);
					
//					/* make the movement harder for lower rotations */
//					if(Math.abs(mScrollingSpeed) < Constants.MAX_LOW_SPEED)
//					{
//						mRenderer.mTargetPositionY = 
//							Math.round(
//									mRenderer.mPositionY
//									+
//									0.5f * Math.signum(mScrollingSpeed) // needs to be .5f because of the rounding...
//							);
//					} 
//					
//					/* full speed ahead */
//					else
//					{
//						mRenderer.mTargetPositionY = 
//							Math.round(
//									mRenderer.mPositionY
//									+
//									Constants.SCROLL_SPEED_BOOST
//									*
//									mScrollingSpeed
//							);
//					}
//					/* small optimization to avoid weird moves on the edges */
//					if(mRenderer.mTargetPositionY == -1)
//						mRenderer.mTargetPositionY = -2;
//					else if(mRenderer.mTargetPositionY == mRenderer.getAlbumCount())
//						mRenderer.mTargetPositionY = mRenderer.getAlbumCount() + 1;
				} else if(mScrollingX){
					mRenderer.inertialScrollOnTouchEnd(mScrollingSpeed, Constants.SCROLL_MODE_HORIZONTAL);
//					mRenderer.mTargetPositionX = Math.round(
//							mRenderer.mPositionX
//							-
//							(Constants.SCROLL_SPEED_BOOST * mScrollingSpeed));
//					/* small optimization to avoid weird moves on the edges */
////					if(mRenderer.mTargetPositionX == -1)
////						mRenderer.mTargetPositionX = -2;
////					else if(mRenderer.mTargetPositionX == 24)
////						mRenderer.mTargetPositionX = 24 + 1;
				}
				

				
				/**
				 * RESET STATE 
				 */
				mScrolling = false;
				mScrollingX = false;
				mScrollingY = false;
				mLongClick = false;

				/**
				 * SHOW MOVEMENT
				 */
				mRenderer.renderNow();
				return true;
			} 
			return false;
		} else {
			Log.i(TAG, "handler has messages!");
			return true;
		}
	}
}