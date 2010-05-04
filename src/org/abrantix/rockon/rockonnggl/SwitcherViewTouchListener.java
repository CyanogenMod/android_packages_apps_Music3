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

public class SwitcherViewTouchListener implements OnTouchListener{
    
	final	String	TAG = "SwitcherViewTouchListener";
	
//	int		mItemDimension;

	float	mDownX = 0.f;
	float	mDownY = 0.f;
	double	mDownTimestamp = 0;
	
	float 	lastX = 0.f;
	float 	lastY = 0.f;
	double 	lastTimestamp = 0;	
	
	AlbumArtistSwitcherView mSwitcherView;
	
	Message	mMsg;
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
			
			mSwitcherView = (AlbumArtistSwitcherView) v;
			
			switch(event.getAction()){
			
			case MotionEvent.ACTION_DOWN:
				/**
				 * detect movement in the lower part of the view
				 */
				if(event.getY() < v.getHeight()/1.66f)
					return false;
				/**
				 * and only on the sides
				 */
				if(event.getX() > v.getWidth() * .2f && event.getX() < v.getWidth() * .8f)
					return false;
				
				mDownX = event.getX();
				mDownY = event.getY();
				mDownTimestamp = System.currentTimeMillis();
				lastTimestamp = mDownTimestamp;
				
				lastX = mDownX;
				lastY = mDownY;
				
				mSwitcherView.setTouching(
						true, 
						event.getX(), 
						event.getY());
				
				// AVOID EVENT FLOODING XXX
				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//
				
				return true;
				
			case MotionEvent.ACTION_MOVE:
//				/**
//				 *  LONG PRESS 
//				 */
//				if(System.currentTimeMillis() - mDownTimestamp > 
//					Constants.MIN_LONG_CLICK_DURATION)
//				{
//				} 
//				/**
//				 * MOVE
//				 */
//				else 
//				{
					mSwitcherView.changePosition(
							event.getX() - lastX,
							event.getX(), 
							event.getY());
//				}

				/**
				 * SAVE STATE
				 */
				lastY = event.getY();
				lastX = event.getX();
				lastTimestamp = System.currentTimeMillis();

				return true;
				
			case MotionEvent.ACTION_UP:
				mSwitcherView.setTouching(
						false, 
						event.getX(), 
						event.getY());
				/**
				 * RESET STATE 
				 */
				
				return true;

			} 
			return false;
	}
}