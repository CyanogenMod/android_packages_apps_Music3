package org.abrantix.rockon.rockonnggl;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;


class AlbumClickListener implements OnClickListener{
	
	private final String TAG = "AlbumClickListener";
	
	private Context				mContext;
	private	RockOnCubeRenderer	mRenderer;
	
	public AlbumClickListener(Context context, Renderer renderer) {
		mContext = context;
		mRenderer = (RockOnCubeRenderer) renderer;
	}

	@Override
	public void onClick(View v) {
		Log.i(TAG, "album clicked!");
	}
}