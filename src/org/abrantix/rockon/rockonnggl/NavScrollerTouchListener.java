package org.abrantix.rockon.rockonnggl;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class NavScrollerTouchListener implements OnTouchListener{
    
	final	String	TAG = "NavScrollerTouchListener";
			
	Handler	mScrollerHandler = null;
	Message	mMsg;
	
	double	mLastUpdateTimestamp = 0;
	
	public void setScrollerHandler(Handler scrollerHandler){
		mScrollerHandler = scrollerHandler;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(!mScrollerHandler.hasMessages(0)){
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				if(((NavScrollerView)v).isTouchNearHandle(event.getY()))
				{
					if(event.getX() > v.getWidth() * .75f)
					{
						mMsg = new Message();
						mMsg.what = MotionEvent.ACTION_DOWN;
						mMsg.arg1 = (int)event.getX();
						mMsg.arg2 = (int)event.getY();
						mScrollerHandler.sendMessage(mMsg);
						mLastUpdateTimestamp = System.currentTimeMillis();
						
						// AVOID EVENT FLOODING XXX
						try {
							Thread.sleep(16);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return true;
					}
					else
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			case MotionEvent.ACTION_MOVE:
				if(System.currentTimeMillis() - mLastUpdateTimestamp > 20)
				{
					mMsg = new Message();
					mMsg.what = MotionEvent.ACTION_MOVE;
					mMsg.arg1 = (int)event.getX();
					mMsg.arg2 = (int)event.getY();
					mScrollerHandler.sendMessage(mMsg);
					mLastUpdateTimestamp = System.currentTimeMillis();
				}
				return true;
			case MotionEvent.ACTION_UP:
				mMsg = new Message();
				mMsg.what = MotionEvent.ACTION_UP;
				mMsg.arg1 = (int)event.getX();
				mMsg.arg2 = (int)event.getY();
				mScrollerHandler.sendMessage(mMsg);
				mLastUpdateTimestamp = System.currentTimeMillis();
				return true;
			} 
			return false;
		} else {
			Log.i(TAG, "handler has messages!");
			return true;
		}
	}
}