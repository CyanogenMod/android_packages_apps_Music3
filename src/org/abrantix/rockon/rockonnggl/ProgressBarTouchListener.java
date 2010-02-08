package org.abrantix.rockon.rockonnggl;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class ProgressBarTouchListener implements OnTouchListener{

	private final String TAG = "ProgressBarTouchListener";

	private Handler	mSeekHandler;

	/**
	 * 
	 * @param seekHandler
	 */
	public void setSeekHandler(Handler seekHandler){
		mSeekHandler = seekHandler;
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent event) {

		
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			((ProgressBarView)view).setTouching(true);
			((ProgressBarView)view).setTouchCoords(
					event.getX(), 
					event.getY());
			// AVOID EVENT FLOODING
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
//			Log.i(TAG, "down: "+event.getX()+" _ "+event.getY());
			break;
		case MotionEvent.ACTION_MOVE:
			((ProgressBarView)view).setTouchCoords(
					event.getX(), 
					event.getY());
//			Log.i(TAG, "moving: "+event.getX()+" _ "+event.getY());
			break;
		case MotionEvent.ACTION_UP:
			((ProgressBarView)view).setTouching(false);
			((ProgressBarView)view).setTouchCoords(
					event.getX(), 
					event.getY());
//			Log.i(TAG, "up: "+event.getX()+" _ "+event.getY());
			if(mSeekHandler != null)
				mSeekHandler.sendEmptyMessage(
						((ProgressBarView)view).getCoordSeekPosition(
								event.getX(), 
								event.getY()));
			break;
		}
		
		return true;
	}
	
}