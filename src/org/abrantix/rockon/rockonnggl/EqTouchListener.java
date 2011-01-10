package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class EqTouchListener implements OnTouchListener{

	protected static final String TAG = "EqTouchListener";
	
	GestureDetector mGestureDetector;
	View			mView;
	
	public EqTouchListener(Context ctx) {
		mGestureDetector = new GestureDetector(ctx, mGestureListener); // does not read multitouch
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mView = v;
	    mGestureDetector.onTouchEvent(event);
	    if(event.getAction()==MotionEvent.ACTION_UP)
	    	((EqView)mView).finalizeMovement(event.getX(), event.getY());
	    mView = null;
	    return true;
	}

	android.view.GestureDetector.OnGestureListener mGestureListener = new android.view.GestureDetector.OnGestureListener() {
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			Log.i(TAG, "SingleTapUp");
			return false;
		}
		
		@Override
		public void onShowPress(MotionEvent e) {
			Log.i(TAG, "ShowPress");	
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY) {
			((EqView)mView).adjustBandGain(distanceX, distanceY);
			Log.i(TAG, "Scroll "+e1.getX()+","+e1.getY()+" --- "+e2.getX()+","+e2.getY());
			return false;
		}
		
		@Override
		public void onLongPress(MotionEvent e) {
			Log.i(TAG, "LongPress");
			
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			Log.i(TAG, "Fling "+e1.getX()+","+e1.getY()+" --- "+e2.getX()+","+e2.getY());
			return false;
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			Log.i(TAG, "Down");
			int bandIdx = ((EqView)mView).setMovingBandByXY(e.getX(), e.getY());
			Log.i(TAG, "Band: "+bandIdx);
			return false;
		}
	};
}
