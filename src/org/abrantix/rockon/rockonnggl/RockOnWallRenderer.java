/*
 * 
 */

package org.abrantix.rockon.rockonnggl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class RockOnWallRenderer extends RockOnRenderer implements GLSurfaceView.Renderer{

	final String TAG = "RockOnWallRenderer";
	private boolean mStopThreads = false;
	
	/** renderer mode */
	public int getType()
	{
		return Constants.RENDERER_WALL;
	}
	
	public void renderNow(){
//		if(!mIsRendering)
			mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
	
	public void stopRender()
	{
		mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
    public RockOnWallRenderer(
    		Context context, 
    		Handler requestRenderHandler, 
    		int theme,
    		int browseCat) 
    {
        mContext = context;
        mRequestRenderHandler = requestRenderHandler;
        mTheme = theme;
        mBrowseCat = browseCat;
        
    	initNonGlVars(context, mBrowseCat, false);
    }
    
    public void changePlaylist(int playlistId){
    	mPositionY = 0;
    	mTargetPositionY = 0;
    	mPositionX = 0;
    	mTargetPositionX = 0;
    	initNonGlVars(mContext, mBrowseCat, true);
    	this.triggerPositionUpdate();
    }
    
    private void initNonGlVars(Context context, int browseCat, boolean force){
    	
    	/** init album cursor **/
    	if(mCursor == null || force){
    		final CursorUtils cursorUtils = new CursorUtils(context);
    		if (browseCat == Constants.BROWSECAT_ARTIST)
    		{
    			Cursor helperCursor = 
    				cursorUtils.getArtistListFromPlaylist(Constants.PLAYLIST_ALL);
    			mCursor = helperCursor;
    			/**
    			 * Lets get the album ids on this artist cursor
    			 */
    			Thread helperThread = new Thread()
    			{
    				public void run()
    				{
		    			if(mCursor != null && mCursor.getCount() >0)
		    			{
		    				mArtistAlbumHelper = new ArtistAlbumHelper[mCursor.getCount()];
		    				cursorUtils.fillArtistAlbumHelperArray(mCursor, mArtistAlbumHelper);
		    				// trigger some update
		    				if(!mStopThreads)
		    					forceTextureUpdateOnNextDraw();
		    			}
		//    			Log.i(TAG, "+ "+(System.currentTimeMillis()-t));
    				}
    			};
    			helperThread.start();    		}
    		else // ALBUM
    		{
    			mPreferArtistSorting = 
    				PreferenceManager.getDefaultSharedPreferences(mContext).
    					getBoolean(
    						mContext.getString(R.string.preference_key_prefer_artist_sorting),
    						true);
	    		Cursor helperCursor = 
	    			cursorUtils.getAlbumListFromPlaylist(
	    				PreferenceManager.getDefaultSharedPreferences(mContext).
	    					getInt(
	    							Constants.prefkey_mPlaylistId,
	    							Constants.PLAYLIST_ALL),
	    				mPreferArtistSorting);
	    		mCursor = helperCursor;
    		}
    	}
    	
    	/** init dimensions */
    	mBitmapWidth = Constants.ALBUM_ART_TEXTURE_SIZE;
    	mBitmapHeight = Constants.ALBUM_ART_TEXTURE_SIZE;
    	
    	/** albumNavUtils */
    	mNavItemUtils = new NavItemUtils(mBitmapWidth, mBitmapHeight, mContext);
    	
    	/** init cover bitmap cache */
    	for(int i = 0; i < mCacheSize; i++){
    		NavItem n = new NavItem();
        	n.index = -999;
    		n.cover = Bitmap.createBitmap(
    				mBitmapWidth, 
    				mBitmapHeight, 
    				Bitmap.Config.RGB_565);
    		n.label = Bitmap.createBitmap(
    				mBitmapWidth,
    				mBitmapHeight/4,
    				Bitmap.Config.ARGB_8888);
    		mNavItem[i] = n;
    	}
    	mColorComponentBuffer = new byte[4*mBitmapWidth*mBitmapHeight];
    	
    }

    public	Cursor getAlbumCursor(){
    	return mCursor;
    }
    
    public int[] getConfigSpec() {
//        if (mTranslucentBackground) {
                // We want a depth buffer and an alpha buffer
                int[] configSpec = {
                        EGL10.EGL_RED_SIZE,      8,
                        EGL10.EGL_GREEN_SIZE,    8,
                        EGL10.EGL_BLUE_SIZE,     8,
                        EGL10.EGL_ALPHA_SIZE,    8,
                        EGL10.EGL_DEPTH_SIZE,   16,
                        EGL10.EGL_NONE
                };
                return configSpec;
//            } else {
//                // We want a depth buffer, don't care about the
//                // details of the color buffer.
//                int[] configSpec = {
//                        EGL10.EGL_DEPTH_SIZE,   16,
//                        EGL10.EGL_NONE
//                };
//                return configSpec;
//            }
    }
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//    	Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    	/*
    	 *	Is Mipmapping supported 
    	 */
    	String extensions = gl.glGetString(GL10.GL_EXTENSIONS);
//      String[] extensionArray = extensions.split(" ");
//      for(int i = 0; i<extensionArray.length; i++)
//      	Log.i(TAG, extensionArray[i]);
		  if(extensions != null && extensions.contains("generate_mipmap"))
//			  ||
//		  	Integer.valueOf(Build.VERSION.SDK) >= 7)
		  {
		  	mSupportMipmapGeneration = true;
		  }
		  else
		  {
		  	mSupportMipmapGeneration = false;
		  }
    	
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(
        		GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

        gl.glClearColor(0.f, 0.f, 0.f, 0);
//        gl.glClearColor(.5f, .5f, .5f, 1);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        /*
         * Create our textures. This has to be done each time the
         * surface is created.
         */
        // album covers - vertical
        gl.glGenTextures(mTextureId.length, mTextureId, 0);
        // album labels
        gl.glGenTextures(mTextureLabelId.length, mTextureLabelId, 0);
        // album labels
//        gl.glGenTextures(mTextureAlphabetId.length, mTextureAlphabetId, 0);
        
        mRockOnCover = new RockOnCover();
//        mAlbumLabelGlText = new AlbumLabelGlText(mTextureLabelId[0]);
        
        
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);
        gl.glEnable(GL10.GL_DEPTH_TEST);
//        gl.glEnable(GL10.GL_LINE_SMOOTH);
//        gl.glEnable(GL10.GL_LINE_SMOOTH_HINT);
//        gl.glEnable(GL10.GL_BLEND);
//        gl.glBlendFunc(
//        		GL10.GL_ONE, 
//        		GL10.GL_ONE_MINUS_SRC_ALPHA);
//        		GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        
        gl.glTexEnvx(
        		GL10.GL_TEXTURE_ENV, 
        		GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_MODULATE);

        /** FOG */
        gl.glEnable(GL10.GL_FOG);
        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
//        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_EXP); // GL_EXP2 doesnt show anything
        gl.glFogf(GL10.GL_FOG_START, -mEyeNormal[2]-1.f);
        gl.glFogf(GL10.GL_FOG_END, -mEyeNormal[2]+3.f);
//        float[] fogColor = {.5f,.5f,.5f, 1.f};
//        gl.glFogfv(GL10.GL_FOG_COLOR, FloatBuffer.wrap(fogColor));
        gl.glHint(GL10.GL_FOG_HINT, GL10.GL_NICEST);
    }

    public int getItemDimension(){
    	return (int) (mHeight * .4f);
    }
    
    /* optmization */
    float	distanceToRotationLimits = 0.f;
    float	logisticFuncResult = 0.f;
    public void onDrawFrame(GL10 gl) {
    	  
    	
    	/** Calculate new position */
    	if(!updatePosition(false)){
    		
    	} 
    	
        /*
         * Usually, the first thing one might want to do is to clear
         * the screen. The most efficient way of doing this is to use
         * glClear().
         */

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        /*
         * Upon browsing category change we want to clear the screen
         * until the changes have been applied
         */
        if(mIsChangingCat)
        {
//        	mIsChangingCat = false; // redundant
//        	stopRender();
        	return;
        }
        
        /*
         * Now we're ready to draw some 3D objects
         */
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        /* 
         * Check if we are doing a click animation
         * 	- it changes the eye spot of the camera
         */
        if(mClickAnimation){
        	logisticFuncResult = 
        		(float) 
        		(2*
				(1/(1+Math.pow(Math.E, -(float)this.mClickAnimationStep/(float)this.MAX_CLICK_ANIMATION_STEPS * 6)))
				-
				1);
        	/* camera eye update */
        	mEyeX = 
        		(float) 
        		(mEyeInitialX 
        		+ 
        		(mEyeTargetX - mEyeInitialX) 
        		* 
        		logisticFuncResult);
        	mEyeY = 
        		(float) 
        		(mEyeInitialY 
        		+ 
        		(mEyeTargetY - mEyeInitialY) 
        		* 
        		logisticFuncResult);
        	mEyeZ = 
        		(float) 
        		(mEyeInitialZ 
        		+ 
        		(mEyeTargetZ - mEyeInitialZ) 
        		* 
        		logisticFuncResult);

        	/* camera center update */
        	mCenterX = 
        		(float) 
        		(mCenterInitialX 
        		+ 
        		(mCenterTargetX - mCenterInitialX) 
        		* 
        		logisticFuncResult);
        	mCenterY = 
        		(float) 
        		(mCenterInitialY 
        		+ 
        		(mCenterTargetY - mCenterInitialY) 
        		* 
        		logisticFuncResult);
        	mCenterZ = 
        		(float) 
        		(mCenterInitialZ 
        		+ 
        		(mCenterTargetZ - mCenterInitialZ) 
        		* 
        		logisticFuncResult);

//        	Log.i(TAG, "growth: "+ logisticFuncResult);
        	
//        	Log.i(TAG, "X: "+mEyeX+" - "+mEyeTargetX);
//        	Log.i(TAG, "Y: "+mEyeY+" - "+mEyeTargetY);
//        	Log.i(TAG, "Z: "+mEyeZ+" - "+mEyeTargetZ);
        	
        	if(this.mClickAnimationStep == this.MAX_CLICK_ANIMATION_STEPS)
        	{
        		mEyeX = mEyeTargetX;
        		mEyeY = mEyeTargetY;
        		mEyeZ = mEyeTargetZ;
        		
        		mCenterX = mCenterTargetX;
        		mCenterY = mCenterTargetY;
        		mCenterZ = mCenterTargetZ;
        		
        		mClickAnimation = false;
        	} else {
        		mClickAnimationStep ++;
        	}
        }
//        else
//        {
//            /* move camera when scrolling cube in Y axis */
//    	 	if(mTargetPositionY > mPositionY)
//        	{
//        		distanceToRotationLimits = 
//            			1.f * 
//            			Math.min(
//            					mPositionY - mRotationInitialPositionY, 
//            					mTargetPositionY - mPositionY);
//        		
//                mCenterY = distanceToRotationLimits;
//        	} 
//        	else if(mTargetPositionY < mPositionY)
//        	{
//        		distanceToRotationLimits = 
//        			1.f * 
//        			Math.min(
//        					mRotationInitialPositionY - mPositionY, 
//        					mPositionY - mTargetPositionY);
//    		
//        		mCenterY = -distanceToRotationLimits;
//        	} 
//        	else
//        	{
//        		mCenterY = 0;
//        	}
////	            // adjust the fog
////	            gl.glFogf(
////	            		GL10.GL_FOG_START, 
////	            		3.5f + .5f * distanceToRotationLimits);
////	            gl.glFogf(
////	            		GL10.GL_FOG_END, 
////	            		4.5f + distanceToRotationLimits);
//        }
  
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        
        /* update textures if needed -- whenever we cross one album */
    	try
    	{
    		texturesUpdated = updateTextures(gl);
    	}
    	catch(IllegalArgumentException e)
    	{
    		e.printStackTrace();
    		texturesUpdated = false;
    		return;
    	} 
        /* set the fog distance */
        gl.glFogf(GL10.GL_FOG_START, -mEyeZ-1.f);
        gl.glFogf(GL10.GL_FOG_END, -mEyeZ+11.f);
//        gl.glDisable(GL10.GL_FOG);

        positionYTmp = mPositionY;
        flooredPositionYTmp = flooredPositionY;
        /* draw each cover */
        for(int i = 0; i<mCacheSize; i++)
        {
        	gl.glLoadIdentity();
        	GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, mCenterX, mCenterY, mCenterZ, 0f, -1.0f, 0.0f);
        	
        	// poor variable name -- dont mind it
        	deltaToCenter = mNavItem[i].index - flooredPositionYTmp * 2;
        	// make it all positive
        	deltaToCenter += mCacheSize/2 - 1; // (-4) negative numbers go bad with integer divisions
        	
        	// optimizations, optimizations
//        	if(deltaToCenter < 0 || deltaToCenter > 11)
//        		continue;
        	
        	/* place the covers */
        	gl.glTranslatef(
        			-1.f + i%2 * 2.f,  // we just dont need to use delta center here because the navigator always moves by 2 positions (1 row)
        			-4.f + deltaToCenter/2 * 2.f, 
        			0);
        	gl.glTranslatef(
        			0, 
        			-(positionYTmp-flooredPositionYTmp) * 2.f, 
        			0);

        	mRockOnCover.setTextureId(mTextureId[i]);
        	mRockOnCover.draw(gl);
        }
        
        if(mTargetPositionY == mPositionY && 
        	!mClickAnimation &&
        	!texturesUpdated)
        {
//        	Log.i(TAG, "positions are not final!");
//        	Log.i(TAG, "mTargetPositionY: "+mTargetPositionY+" mPositionY: "+mPositionY);
//        	Log.i(TAG, "mTargetPositionX: "+mTargetPositionX+" mPositionX: "+mPositionX);
//        	Log.i(TAG, "mClickAnimation: "+mClickAnimation);
//        	Log.i(TAG, "texturesUpdated: "+texturesUpdated);
        	stopRender();	
        }
        else
        {
        	renderNow();
        }
    }

    @Override
    public float getScrollPosition()
    {
    	return (mPositionY * 2) /mCursor.getCount();
    }
    
    /**
     * 
     */
    @Override
    public void setCurrentTargetYByProgress(float progress)
    {
    	mTargetPositionY = Math.min(
    			Math.round(progress * mCursor.getCount() * .5f),
    			mCursor.getCount() * .5f - 1);
//    	mPositionY = mTargetPositionY;
    	if(Math.abs(mPositionY - mTargetPositionY) > 5)
    		mPositionY = 
    			mTargetPositionY -
    			Math.signum(mTargetPositionY - mPositionY) * 5.f;
    	renderNow();
    }
    
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
        
        mWidth = w;
        mHeight = h;

        /*
        * Set our projection matrix. This doesn't have to be done
        * each time we draw, but usually a new projection needs to
        * be set when the viewport is resized.
        */

        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 20);

    }

    /* optimization */
    float offsetY;
    int rowFromY;
    int columnFromX;
    /**
     * 
     * @param x
     * @param y
     * @return
     */
    int	getPositionFromScreenCoordinates(float x, float y)
    {
    	offsetY = mPositionY - flooredPositionY;
    	rowFromY = (int) ((y / (mHeight*.25f))+offsetY);
    	if(rowFromY == 2) rowFromY--;
    	else if(rowFromY == 3) rowFromY--;
    	
    	columnFromX = (int) (x / (mWidth*.5f));
    	
    	return 
    		flooredPositionY*2 - 2
    		+
    		2 * rowFromY
    		+
    		columnFromX;
    }
    
    int getVerifiedPositionFromScreenCoordinates(float x, float y)
    {
    	return
    		Math.min(
	    		Math.max(
	    			getPositionFromScreenCoordinates(x,y),
	    			0),
	    		mCursor.getCount()-1);
    }
    
    /* optimization */
    int[] rowAndColumn = new int[2];
    /**
     * 
     * @param x
     * @param y
     * @return
     */
    int[]	getRowAndColumnFromScreenCoordinates(float x, float y)
    {
    	rowFromY = (int) (y / (mHeight*.25f));
    	if(rowFromY == 2) rowFromY--;
    	else if(rowFromY == 3) rowFromY--;
    	
    	columnFromX = (int) (x / (mWidth*.5f));
    	
    	rowAndColumn[0] = columnFromX;
    	rowAndColumn[1] = rowFromY;
    	return rowAndColumn;
    }
    
    /**
     * start animating a click
     */
    void showClickAnimation(float x, float y){
    	this.mClickAnimation = true;
    	mClickAnimationStep = 0;

    	this.mEyeTargetX = mEyeClicked[0];
    	this.mEyeTargetY = mEyeClicked[1];
    	this.mEyeTargetZ = mEyeClicked[2];
    	
    	this.mEyeInitialX = this.mEyeX;
    	this.mEyeInitialY = this.mEyeY;
    	this.mEyeInitialZ = this.mEyeZ;
    	
    	this.mCenterTargetX = -1 + 2*getRowAndColumnFromScreenCoordinates(x, y)[0];
    	this.mCenterTargetY = -2 + 2*getRowAndColumnFromScreenCoordinates(x, y)[1]; // duplicated effort -- FIXME
    	this.mCenterTargetZ = 0;
    	
    	this.mCenterInitialX = this.mCenterX;
    	this.mCenterInitialY = this.mCenterY;
    	this.mCenterInitialZ = this.mCenterZ;
    	
    	pTimestamp = System.currentTimeMillis();
    	this.renderNow();
    }
    
    /**
     * reverse a click animation
     */
    void reverseClickAnimation(){
    	this.mClickAnimation = true;
    	mClickAnimationStep = 0;

    	this.mEyeTargetX = mEyeNormal[0];
    	this.mEyeTargetY = mEyeNormal[1];
    	this.mEyeTargetZ = mEyeNormal[2];
    	
    	this.mEyeInitialX = this.mEyeX;
    	this.mEyeInitialY = this.mEyeY;
    	this.mEyeInitialZ = this.mEyeZ;
    	
    	this.mCenterTargetX = 0;
    	this.mCenterTargetY = 0;
    	this.mCenterTargetZ = 0;
    	
    	this.mCenterInitialX = this.mCenterX;
    	this.mCenterInitialY = this.mCenterY;
    	this.mCenterInitialZ = this.mCenterZ;
    	    	
    	pTimestamp = System.currentTimeMillis();
    	this.renderNow();
    }
    
    int getClickActionDelay(){
    	return 2 * Constants.CLICK_ACTION_DELAY;
    }
    
    void forceTextureUpdateOnNextDraw(){
    	mForceTextureUpdate = true;
    	this.renderNow();
    }
    
    int		albumIndexTmp;
//    int		alphabetIndexTmp;
    int		lastInitial = -1;
    int		charTmp;
    boolean changed;
    private boolean updateTextures(GL10 gl){
    	changed = false;
		if(mCursor != null && !mCursor.isClosed())
		{
//			Log.i(TAG, " ++ updating textures");
	    	/* Album Cover textures in vertical scrolling */
    		for(int i = 0; i < mCacheSize; i++){
    			// we try to minimize cache reshuffling to the max
    			albumIndexTmp = 
    				(int)Math.floor((flooredPositionY * 2) / mCacheSize) * mCacheSize
    				+ i;
    			if(albumIndexTmp < 
    					flooredPositionY * 2  
        					- 4)
    			{
    				albumIndexTmp += mCacheSize;
    			} 
    			// should never happen
    			else if(albumIndexTmp >=
    					flooredPositionY * 2  
    						- 4 
    						+ mCacheSize)
    			{
    				albumIndexTmp -= mCacheSize;
    			}
    				
	    		
//    			Log.i(TAG, 
//	    			"albumIndexTmp: "+albumIndexTmp+
//	    			" flooredPositionY: "+flooredPositionY+
//	    			" mPositionY: "+mPositionY);
	    		
	    		if(setupAlbumTextures(gl, i, albumIndexTmp, mForceTextureUpdate))
	    			changed = true;
	    	}
	    	
	    	if(mForceTextureUpdate)
	    		mForceTextureUpdate = false;
    	}
    	return changed;
    }
    
    Bitmap undefined = Bitmap.createBitmap(
			Constants.REASONABLE_ALBUM_ART_SIZE, 
			Constants.REASONABLE_ALBUM_ART_SIZE, 
			Bitmap.Config.RGB_565);
    
    /* optimization */
    int 			cacheIdxTmp;
    NavItem 	albumNavItemTmp;
    boolean			reusedCached = false;
    /**
     * fills the albumNavItem structure with the cover bitmap, labels, etc
     * @param gl
     * @param cacheIndex
     * @param navIndex
     * @param force
     * @return
     */
    private boolean setupAlbumTextures(GL10 gl, int cacheIndex, int navIndex, boolean force){
    	/** texture needs update? */
    	if(mNavItem[cacheIndex].index != navIndex || force){
//    		Log.i(TAG, "albumIndex: "+albumIndex+"/"+mAlbumCursor.getCount()+" flooredPositionY: "+flooredPositionY+" mPositionY: "+mPositionY);
    		
//    		reusedCached = false;
//    		/** Check if we can reuse an element already in cache */
//    		for (cacheIdxTmp = 0; cacheIdxTmp < mCacheSize; cacheIdxTmp++)
//    		{
//    			if(mAlbumNavItem[cacheIdxTmp].index == albumIndex)
//    			{
//    				// swap the elements in the cache
//    				albumNavItemTmp = mAlbumNavItem[cacheIndex];
//    				mAlbumNavItem[cacheIndex] = mAlbumNavItem[cacheIdxTmp];
//    				mAlbumNavItem[cacheIdxTmp] = albumNavItemTmp;
//    				// signal that we dont want to run the code ahead
//    				reusedCached = true;
//    			}
//    		}
//    		
//    		if(!reusedCached)
//    		{
	    		/** Update cache item */
	    		mNavItem[cacheIndex].index = navIndex;
	    		if(navIndex < 0 || navIndex >= mCursor.getCount())
	    		{
	//    			Log.i(TAG, "BM failed, index oob");
	    			mNavItem[cacheIndex].albumName = "";
	    			mNavItem[cacheIndex].artistName = "";
	    			mNavItem[cacheIndex].cover = undefined;
	    			if(!mNavItem[cacheIndex].cover.isRecycled())
	    				mNavItem[cacheIndex].cover.eraseColor(Color.argb(255, 0, 0, 0));
	    		} 
	    		else 
	    		{
	    			switch(mBrowseCat)
	    			{
	    			case Constants.BROWSECAT_ALBUM:	
			    		if(!mNavItemUtils.fillAlbumInfo(
			    				mCursor, 
			    				mNavItem[cacheIndex], 
			    				navIndex))
			    		{
		//	    			Log.i(TAG, "Info Failed");
			    			mNavItem[cacheIndex].albumName = null;
			    			mNavItem[cacheIndex].artistName = null;
			    			mNavItem[cacheIndex].albumKey = null;
			    		}
			    		if(!mNavItemUtils.fillAlbumBitmap(
			    				mNavItem[cacheIndex], 
			    				mBitmapWidth, 
			    				mBitmapHeight, 
			    				mColorComponentBuffer,
			    				mTheme))
			    		{
		//	    			Log.i(TAG, "BM failed, error loading bm");
			    			try
			    			{
				    			mNavItemUtils.fillAlbumUnknownBitmap(
				    					mNavItem[cacheIndex], 
				    					mContext.getResources(), 
				    					mNavItem[cacheIndex].cover.getWidth(), 
				    					mNavItem[cacheIndex].cover.getHeight(), 
				    					mColorComponentBuffer, 
				    					mTheme);
			    			}
			    			catch(NullPointerException e)
			    			{
			    				e.printStackTrace();
			    			}
			    		}
			    		break;
	    			case Constants.BROWSECAT_ARTIST:
	    				if(navIndex < mArtistAlbumHelper.length)
	    				{
		    				if(!mNavItemUtils.fillArtistInfo(
				    				mCursor, 
				    				mNavItem[cacheIndex],
				    				mArtistAlbumHelper[navIndex],
				    				navIndex))
				    		{
				    			mNavItem[cacheIndex].artistId = null;
				    			mNavItem[cacheIndex].artistName = null;
				    			mNavItem[cacheIndex].nAlbumsFromArtist = 0;
				    			mNavItem[cacheIndex].nSongsFromArtist = 0;
				    		}
				    		if(!mNavItemUtils.fillArtistBitmap(
				    				mNavItem[cacheIndex],
				    				mArtistAlbumHelper[navIndex],
				    				mBitmapWidth, 
				    				mBitmapHeight, 
				    				mColorComponentBuffer,
				    				mTheme))
				    		{
				    			mNavItemUtils.fillAlbumUnknownBitmap(
				    					mNavItem[cacheIndex], 
				    					mContext.getResources(), 
				    					mNavItem[cacheIndex].cover.getWidth(), 
				    					mNavItem[cacheIndex].cover.getHeight(), 
				    					mColorComponentBuffer, 
				    					mTheme);	
				    		}
	//			    		if(!mNavItemUtils.fillArtistLabel(
	//			    				mNavItem[cacheIndex],
	//			    				mBitmapWidth,
	//			    				mBitmapHeight/4))
	//			    		{
	//			    			if(!mNavItem[cacheIndex].label.isRecycled())
	//			    				mNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
	//			    		}
	    				} else {
	    		    		mNavItem[cacheIndex].index = -1;
	    				}
	    				break;
	    			}
	    		}
//    		}
    		    		
	    	/** bind new texture */
    		bindTexture(gl, mNavItem[cacheIndex].cover, mTextureId[cacheIndex]);
    		bindTexture(gl, mNavItem[cacheIndex].label, mTextureLabelId[cacheIndex]);
    		
    		return true;
    	} else  {
    		return false;
    	}
    	
    }
    
    private void bindTexture(GL10 gl, Bitmap bitmap, int textureId){
    	/** MIPMAPPING requires this */
        gl.glFlush();

    	/** bind new texture */
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
    	
        if(mSupportMipmapGeneration) 
        {
        	gl.glTexParameterf(
            		GL10.GL_TEXTURE_2D, 
            		GL10.GL_TEXTURE_MIN_FILTER,
            		GL10.GL_LINEAR_MIPMAP_LINEAR);
//                    GL10.GL_NEAREST);
//            		GL10.GL_LINEAR);
        	gl.glTexParameterf(
        			GL11.GL_TEXTURE_2D, 
        			GL11.GL_GENERATE_MIPMAP, 
        			GL11.GL_TRUE);
        }
        else
        {
        	gl.glTexParameterf(
            		GL10.GL_TEXTURE_2D, 
            		GL10.GL_TEXTURE_MIN_FILTER,
//            		GL10.GL_LINEAR_MIPMAP_LINEAR);
//                    GL10.GL_NEAREST);
            		GL10.GL_LINEAR);
        }
        gl.glTexParameterf(
        		GL10.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_LINEAR);

        gl.glTexParameterf(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE);
        
//        gl.glTexParameterf(
//        		GL10.GL_TEXTURE_2D, 
//        		GL10.GL_TEXTURE_COLOR_BORDER,
//                GL10.GL_NICEST);

        gl.glTexEnvf(
        		GL10.GL_TEXTURE_ENV, 
        		GL10.GL_TEXTURE_ENV_MODE,
        		GL10.GL_MODULATE);
//        		GL10.GL_DECAL);
//        		GL10.GL_MODULATE);
//        		GL10.GL_BLEND);
//                GL10.GL_REPLACE);


        if(bitmap != null)
        	GLUtils.texImage2D(
        			GL10.GL_TEXTURE_2D, 
        			0, 
        			bitmap, 
        			0);

    }
    
    float getPositionX(){
    	return mPositionX;
    }

    float getTargetPositionX(){
    	return mTargetPositionX;
    }

    float getPositionY(){
    	return mPositionY;
    }

    float getTargetPositionY(){
    	return mTargetPositionY;
    }
    
    void saveRotationInitialPosition(){
    	mRotationInitialPositionY = mPositionY;
//    	mRotationInitialPositionX = mPositionX;
    }
    
    public void triggerPositionUpdate(){
    	updatePosition(true);
    	this.renderNow();
    }
    
    /* optimization */
    double itvlFromLastRender;
    float WALL_MIN_SCROLL;
    float WALL_SMOOTH;
    float WALL_MAX_SCROLL;
    private boolean updatePosition(boolean force){
    	WALL_MIN_SCROLL = 0.25f * Constants.MIN_SCROLL;
    	WALL_SMOOTH = 1.f * Constants.SCROLL_SPEED_SMOOTHNESS;
    	WALL_MAX_SCROLL = 1.f * Constants.MAX_SCROLL;
    
    	/** time independence */
    	itvlFromLastRender = 
    		Math.min(
    				System.currentTimeMillis() - pTimestamp,
    				100) // 100 ms is the biggest 'jump' we allow
    		*
    		.001;
    	if(updateFraction > 0 && updateFraction < .05f)
	    	updateFraction = 
	    		Constants.CPU_SMOOTHNESS * itvlFromLastRender
		    		+
		    		(1-Constants.CPU_SMOOTHNESS) * updateFraction;
    	else
    		updateFraction = itvlFromLastRender;
    	
		/** save state **/
		pTimestamp = System.currentTimeMillis();
	
    	/** optimization calculation*/
		flooredPositionY = (int) Math.floor(mPositionY);
		positionOffsetY = mPositionY - flooredPositionY;
		

    	
   		/** 
   		 * New Y pivot
   		 */
	   	if(mTargetPositionY > mPositionY)
			mPositionY +=
				Math.min(
					Math.min(
						Math.max(
								updateFraction
									* WALL_SMOOTH * (mTargetPositionY-mPositionY), 
								updateFraction 
									* .05f * WALL_MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * WALL_MAX_SCROLL * 3.f); // XXX *4.f is a HACK
		else if(mTargetPositionY < mPositionY)
			mPositionY	 += 
				Math.max(
					Math.max(
						Math.min(
							updateFraction
								* WALL_SMOOTH * (mTargetPositionY-mPositionY), 
							updateFraction 
								* .05f * -WALL_MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * -WALL_MAX_SCROLL * 3.f); // XXX *4.f is a HACK

		/** are we outside the limits of the album list?*/
    	if(mCursor != null){
    		/** Y checks */
    		if(mPositionY <= 0)
	    		mTargetPositionY = Math.min(1, (mCursor.getCount()-1)/2);
	    	else if(mPositionY >= (mCursor.getCount() - 1)/2)
	    		mTargetPositionY = (mCursor.getCount() - 1)/2 - 1;
	    	
//	    	/** are we done? */
//	    	if(mTargetPositionY == (float)mPositionY){
//	    		/* check limits */
//	    		if(mPositionY < 0)
//	    			mTargetPositionY = 0;
//	    		else if(mPositionY > mAlbumCursor.getCount() - 1)
//	    			mTargetPositionY = mAlbumCursor.getCount() - 1;
//	    		/* yes, we are done scrolling */
//	    		else if(!force)
//	    			return false;
//	    	}
    	}
    	
		
		return true;
    }

    /** stop scroll on touch */
    public void	stopScrollOnTouch()
    {
		if(mTargetPositionY > mPositionY)
			mTargetPositionY = (float) Math.ceil(mPositionY);
		else
			mTargetPositionY = (float) Math.floor(mPositionY);
	}

    /** scroll on touch move */
    public void scrollOnTouchMove(float px, int direction)
    {
    	switch(direction)
    	{
    	case Constants.SCROLL_MODE_VERTICAL:
    		try
    		{
	    		mTargetPositionY = mPositionY - px/(mHeight*.5f);
				/* make we dont exceed the cube limits */
	    		if(mTargetPositionY <= -1 + Constants.MAX_POSITION_OVERSHOOT)
					mTargetPositionY = -1 + Constants.MAX_POSITION_OVERSHOOT;
				else if(mTargetPositionY >= (mCursor.getCount() - 1)/2 - 1 + Constants.MAX_POSITION_OVERSHOOT)
					mTargetPositionY = (mCursor.getCount() - 1)/2 - 1 + Constants.MAX_POSITION_OVERSHOOT;
				
				mPositionY = mTargetPositionY;
	    		return;
    		}
    		catch(NullPointerException e)
    		{
    			e.printStackTrace();
    			return;
    		}
    	}
    }
    
    /** inertial scroll on touch end */
    public void	inertialScrollOnTouchEnd(float scrollSpeed, int direction)
    {
    	switch(direction)
    	{
    	case Constants.SCROLL_MODE_VERTICAL:
//    		/* make the movement harder for lower rotations */
//    		if(Math.abs(scrollSpeed/(mHeight*.5f)) < Constants.MAX_LOW_SPEED)
//    		{
//    			mTargetPositionY = 
//    				Math.round(
//    						mPositionY
//    						+
//    						0.5f * Math.signum(scrollSpeed/(mHeight*.5f)) // needs to be .5f because of the rounding...
//    				);
//    		} 
//    		/* full speed ahead */
//    		else
//    		{
    			mTargetPositionY = 
    				Math.round(
    						mPositionY
    						+
    						1.5 * Constants.SCROLL_SPEED_BOOST
    						*
    						scrollSpeed/(mHeight*.5f)
    				);
//    		}
    		/* small optimization to avoid weird moves on the edges */
    		if(mTargetPositionY == 0)
    			mTargetPositionY = -1;
    		else if(mTargetPositionY == Math.floor(getAlbumCount()*.5f))
    			mTargetPositionY = (float) Math.floor(getAlbumCount()*.5f)+1;		
    		return;
    	}
    }
    
    /** is the cube spinning */
    boolean isSpinning(){
    	// TODO: also check X scrolling
    	if(isSpinningX() ||
    		isSpinningY())
    	{
    		return true;
    	} 
    	else 
    	{
    		return false;
    	}
    }
    
    /** is the cube spinning */
    boolean isSpinningX(){
    	return false; // there is no X scrolling in Wall view
    }   
    
    /** is the cube spinning */
    boolean isSpinningY(){
    	// TODO: also check X scrolling
    	if(mTargetPositionY != mPositionY)
    	{
    		return true;
    	} 
    	else 
    	{
    		return false;
    	}
    }
    
    /** get album count */
    public int	getAlbumCount(){
    	if(mCursor == null)
    		return -1;
    	else
    		return mCursor.getCount();
    }
    
    /** get the current position */
    int	getShownPosition(float x, float y){
    	return getPositionFromScreenCoordinates(x, y);
    }
    
    /* optimization */
    int cursorIdxValidation;
    /** get the current Album Id */
    synchronized int getShownElementId(float x, float y){
    	if(mTargetPositionY != mPositionY ||
    		mCursor == null ||
    		mCursor.isClosed() ||
			/**
			 * FIXME: this is a quick cursor overflow bugfix, unverified
			 */
    		getPositionFromScreenCoordinates(x, y) > mCursor.getCount() - 1 ||
    		getPositionFromScreenCoordinates(x, y) < 0)
//    		(int) mPositionY > mAlbumCursor.getCount() - 1)
    	{
//    		Log.i(TAG, "Target was not reached yet: "+mTargetPosition+" - "+mPosition);
    		return -1;
    	}
    	else{
    		try
    		{
	    		int tmpIndex = mCursor.getPosition();
	    		
	    		mCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
	
	    		int id = -1;
	    		if(mBrowseCat == Constants.BROWSECAT_ALBUM)
		    		id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Albums._ID));
	    		else if(mBrowseCat == Constants.BROWSECAT_ARTIST)
	    			id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Artists._ID));
	    		mCursor.moveToPosition(tmpIndex);
	    		return id;
    		}
    		catch(NullPointerException e)
    		{
    			e.printStackTrace();
    			return -1;
    		}
    		catch(CursorIndexOutOfBoundsException e)
    		{
    			e.printStackTrace();
    			return -1;
    		}
    	}
    }
    
    synchronized int getElementId(int position){
    	if(mCursor == null ||
    		mCursor.isClosed() ||
			/**
			 * FIXME: this is a quick cursor overflow bugfix, unverified
			 */
    		position > mCursor.getCount() - 1 ||
    		position < 0)
//    		(int) mPositionY > mAlbumCursor.getCount() - 1)
    	{
//    		Log.i(TAG, "Target was not reached yet: "+mTargetPosition+" - "+mPosition);
    		return -1;
    	}
    	else{
    		try
    		{
	    		int tmpIndex = mCursor.getPosition();
	    		
	    		mCursor.moveToPosition(position);
	
	    		int id = -1;
	    		if(mBrowseCat == Constants.BROWSECAT_ALBUM)
		    		id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Albums._ID));
	    		else if(mBrowseCat == Constants.BROWSECAT_ARTIST)
	    			id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Artists._ID));
	    		mCursor.moveToPosition(tmpIndex);
	    		return id;
    		}
    		catch(NullPointerException e)
    		{
    			e.printStackTrace();
    			return -1;
    		}
    		catch(CursorIndexOutOfBoundsException e)
    		{
    			e.printStackTrace();
    			return -1;
    		}
    	}
    }
    
    /** get the current Album Name */
    String getShownAlbumName(float x, float y){
    	if(mTargetPositionY != mPositionY)
    		return null;
    	else{
    		int tmpIndex = mCursor.getPosition();
    		mCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
    		String albumName = mCursor.getString(
    				mCursor.getColumnIndexOrThrow(
    						MediaStore.Audio.Albums.ALBUM));
    		mCursor.moveToPosition(tmpIndex);
    		return albumName;
    	}	
    }
    
    String getAlbumName(int position){
    	int tmpIndex = mCursor.getPosition();
		mCursor.moveToPosition(position);
		String albumName = mCursor.getString(
				mCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ALBUM));
		mCursor.moveToPosition(tmpIndex);
		return albumName;
    }
    
    /** get the current Album Name */
    String getShownAlbumArtistName(float x, float y){
    	if(mTargetPositionY != mPositionY)
    		return null;
    	else{
    		int tmpIndex = mCursor.getPosition();
    		mCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
    		String artistName = mCursor.getString(
    				mCursor.getColumnIndexOrThrow(
    						MediaStore.Audio.Albums.ARTIST));
    		mCursor.moveToPosition(tmpIndex);
    		return artistName;
    	}
    }
    
    String getAlbumArtistName(int position){
		int tmpIndex = mCursor.getPosition();
		mCursor.moveToPosition(position);
		String artistName = mCursor.getString(
				mCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ARTIST));
		mCursor.moveToPosition(tmpIndex);
		return artistName;
    }
    
    /** get the shown song name */
    String getShownSongName(float x, float y)
    {
    	return null;
    }

    String getSongName(int position)
    {
    	return null;
    }
    
    /** move navigator to the specified album Id */
    synchronized int setCurrentByAlbumId(long albumId){

    	if(albumId >= 0)
    	{
	    	if(mCursor != null){
		    	for(int i = 0; i < mCursor.getCount(); i++){
		    		mCursor.moveToPosition(i);
		    		try
		    		{
			    		if(mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)) == albumId){
			    			mTargetPositionY = i/2;
			    			mPositionY = 
			    				mTargetPositionY - 
			    				Math.signum(mTargetPositionY - mPositionY)
			    				*
			    				Math.min(
			    					Math.abs(mTargetPositionY-mPositionY), 
			    					5.5f);
			    			// TODO: trigger rotation
			    			this.renderNow();
			    			return i;
			    		}
		    		}
		    		catch(CursorIndexOutOfBoundsException e)
		    		{
		    			e.printStackTrace();
		    			return -1;
		    		}
		    	}
		    	return -1;
	    	} else {
	    		return -1;
	    	}
    	} else {
    		return -1;
    	}
    }
    
    /**
     * 
     */
    synchronized int setCurrentByArtistId(long artistId)
    {
    	if(artistId >= 0)
    	{
	    	if(mCursor != null){
		    	for(int i = 0; i < mCursor.getCount(); i++){
		    		mCursor.moveToPosition(i);
		    		try
		    		{
			    		if(mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)) == artistId){
			    			mTargetPositionY = i/2;
			    			mPositionY = 
			    				mTargetPositionY - 
			    				Math.signum(mTargetPositionY - mPositionY)
			    				*
			    				Math.min(
			    					Math.abs(mTargetPositionY-mPositionY), 
			    					5.5f);
			    			// TODO: trigger rotation
			    			this.renderNow();
			    			return i;
			    		}
		    		}
		    		catch(CursorIndexOutOfBoundsException e)
		    		{
		    			e.printStackTrace();
		    			return -1;
		    		}
		    	}
		    	return -1;
	    	} else {
	    		return -1;
	    	}
    	} else {
    		return -1;
    	}
    }
    
    // XXX - this should never be called 
    /** move navigator to the specified audio Id */
    synchronized int setCurrentBySongId(long songId)
    {
    	return -1;
    }
    
    /**
     * 
     */
    synchronized public void changeBrowseCat(int browseCat)
    {
    	if(browseCat != mBrowseCat)
    	{
    		mIsChangingCat = true;
    		this.renderNow();
    		mCursor.close();
    		initNonGlVars(mContext, browseCat, true);
    		mBrowseCat = browseCat;
    		mPositionY = -5.f;
    		mIsChangingCat = false;
    		System.gc();
    		this.renderNow(); // also redundant
    	}
    }
    
    /**
     * 
     * @return
     */
    public int getBrowseCat()
    {
    	return mBrowseCat;	
    }
    
    /** 
     * Recycle cached bitmaps
     */
    public void clearCache()
    {
    	mStopThreads = true;
    	for(int i=0; i<mNavItem.length; i++)
    	{
    		try
    		{
	    		mNavItem[i].cover.recycle();
	    		mNavItem[i].label.recycle();
    		}
    		catch(Exception e)
    		{
    			e.printStackTrace();
    		}
    	}
    }
    
    /**
     * Class members
     */
//    private int 				mBrowseCat;
    private int 				mTheme;
    private	int					mCacheSize = 10; // 2 covers at the center row and then 2 more rows up and 2 more rows down
    private Context 			mContext;
//    private Handler			mRequestRenderHandler;
    private RockOnCover			mRockOnCover;
    private int[] 				mTextureId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private int[] 				mTextureLabelId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
//    private int[] 			mTextureAlphabetId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private	int					mScrollMode = Constants.SCROLL_MODE_VERTICAL;
    public	boolean				mClickAnimation = false;
//    private	Cursor				mCursor = null;
//    private AlphabetNavItem[]	mAlphabetNavItem = new AlphabetNavItem[mCacheSize];
    private NavItem[]			mNavItem = new NavItem[mCacheSize];
    private NavItemUtils		mNavItemUtils;
    private ArtistAlbumHelper[]	mArtistAlbumHelper;
    private	int					mBitmapWidth;
    private int 				mBitmapHeight;
    private byte[]				mColorComponentBuffer;
    private	boolean				mForceTextureUpdate = false;
    private int					mWidth = 0;
    private int					mHeight = 0;
    private boolean				mIsChangingCat = false;

    private float[]		mEyeNormal = 
    {
    		0.f,
    		0.f,
    		-6.25f
    };
    private float[]		mEyeClicked = 
    {
    		0.f, // XX dont care
    		0.f, // XX dont care
    		-4.0f
    };
    private float		mEyeX = mEyeNormal[0];
    private float		mEyeY = mEyeNormal[1];
    private float		mEyeZ = mEyeNormal[2];
    private float		mEyeTargetX = mEyeNormal[0];
    private float		mEyeTargetY = mEyeNormal[1];
    private float		mEyeTargetZ = mEyeNormal[2];
    private float		mEyeInitialX = mEyeX;
    private float		mEyeInitialY = mEyeY;
    private float		mEyeInitialZ = mEyeZ;
    private float		mCenterX = 0;
    private float		mCenterY = 0;
    private float		mCenterZ = 0;
    private float		mCenterTargetX = 0;
    private float		mCenterTargetY = 0;
    private float		mCenterTargetZ = 0;
    private float		mCenterInitialX = 0;
    private float		mCenterInitialY = 0;
    private float		mCenterInitialZ = 0;
    private float		MAX_CLICK_ANIMATION_STEPS = 25;
    private float		mClickAnimationStep = 0;
    
    // GL extensions
    private boolean		mSupportMipmapGeneration;
    
    
    /** 
     * optimization vars 
     */
    private double 	pTimestamp;
    private int		flooredPositionX;
    private int		flooredPositionY;
    private float	positionOffsetX;
    private float	positionOffsetY;
    private float	rotationAngleX;
    private float	rotationAngleY;
    private boolean	texturesUpdated;
    private double	updateFraction;
    private int		tmpTextureIdx;
    private int		deltaToCenter;
    private float	positionYTmp;
    private int		flooredPositionYTmp;
}


class RockOnCover {
	
	private final String TAG = "RockOnCover";
	
    public RockOnCover() {
//  public RockOnCover(int[] textureId, int[] textureAlphabetId) {
    	/**
    	 * cover coordinates
    	 */
    	float[] coords = {
        		// X, Y, Z
        		-1.f, 1.f, 0.f,
        		1.f, 1.f, 0.f, 
        		1.f, -1.f, 0.f,
        		-1.f, -1.f, 0.f
        };
        
        /**
         * Texture coordinates
         */
        float[] textCoords = {
	        		0.f, 1.f,
	        		1.f, 1.f,
	        		1.f, 0.f,
	        		0.f, 0.f
        };
        
    	/**
    	 * Generate our openGL buffers with the 
    	 * vertice and texture coordinates 
    	 * and drawing indexes
    	 * VERTICAL 
    	 */
        // Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4); // verts * ncoords * bytes per vert??
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();

        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
        
        for (int i = 0; i < VERTS; i++) {
            for(int j = 0; j < 3; j++) {
            	mFVertexBuffer.put(coords[i*3+j]);
            }
        }

        mTexBuffer.put(textCoords);

        for(int i = 0; i < VERTS; i++) {
            mIndexBuffer.put((short) i);
        }

        mFVertexBuffer.position(0);
        mTexBuffer.position(0);
        mIndexBuffer.position(0);

	}

    /* optimization */
//    int	x;
//    int y;
    public void draw(GL10 gl) {
    	
    	/**
    	 * Vertical scrolling, only draw covers
    	 */
		gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);
        gl.glTexParameterx(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_REPEAT);
        gl.glTexParameterx(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_REPEAT);
    	
        gl.glFrontFace(GL10.GL_CCW);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
        gl.glDrawElements(
//        		GL10.GL_TRIANGLE_STRIP,
        		GL10.GL_TRIANGLE_FAN,
//        		GL10.GL_LINE_LOOP,
        		VERTS,
                GL10.GL_UNSIGNED_SHORT,
                mIndexBuffer);
    }

    public void setTextureId(int textId){
    	mTextureId = textId;
    }

    /* number of vertices */
    private final static int 	VERTS = 4;
	private final int 			pointsPerFace = 4;
    
	/* vertical scrolling buffers */
	private FloatBuffer mFVertexBuffer;
    private FloatBuffer mTexBuffer;
    private ShortBuffer mIndexBuffer;
    
    /** our 4 vertical face textures */
    public	int 		mTextureId;
}

//class AlbumLabelGlText{
//	
//	public void setTexture(int textureId){
//		mTextureId = textureId;
//	}
//	
//    public AlbumLabelGlText(int textureId) {
//
//    	/** 
//    	 * Save the texture ids
//    	 */
//    	mTextureId = textureId;
//    	
//    	/** 
//    	 * Our cube coordinate declaration
//    	 */ 
//    	ArrayList<float[]> faceCoordsArray = new ArrayList<float[]>(mCubeFaces);
//    	
//    	// FACE 0
//        float[] coords0 = {
//        		// X, Y, Z
//        		-1.f, .25f, 0.f,
//        		1.f, .25f, 0.f,
//        		1.f, -.25f, 0.f, 
//        		-1.f, -.25f, 0.f
//        };
//        faceCoordsArray.add(coords0);
//        
////        // FACE 1
////        float[] coords1 = {
////        		// X, Y, Z
////        		-1.f, 1.1f, -.5f,
////        		1.f, 1.1f, -5f,
////        		1.f, 1.1f, -1.f,
////        		-1.f, 1.1f, -1.f
////        };
////        faceCoordsArray.add(coords1);
////        
////        // FACE 2
////        float[] coords2 = {
////                // X, Y, Z
////        		-1.f, .5f, 1.1f,
////        		1.f, .5f, 1.1f,
////        		1.f, 1.f, 1.1f,
////        		-1.f, 1.f, 1.1f
////        };
////        faceCoordsArray.add(coords2);
////        
////        // FACE 3
////        float[] coords3 = {
////        		// X, Y, Z
////        		-1.f, -1.1f, .5f,
////        		1.f, -1.1f, .5f,
////         		1.f, -1.1f, 1.f,
////        		-1.f, -1.1f, 1.f
////        };
////        faceCoordsArray.add(coords3);
////        
//        // General texture coords
//        float[] textCoords = {
//	        		0.f, 1.f,
//	        		1.f, 1.f,
//	        		1.f, 0.f,
//	        		0.f, 0.f
//        };
//        
////    	/**
////    	 * Generate our openGL buffers with the 
////    	 * vertice and texture coordinates 
////    	 * and drawing indexes
////    	 */
////    	for(int k = 0; k < mCubeFaces; k++){
////	        // Buffers to be passed to gl*Pointer() functions
////	        // must be direct, i.e., they must be placed on the
////	        // native heap where the garbage collector cannot
////	        // move them.
////	        //
////	        // Buffers with multi-byte datatypes (e.g., short, int, float)
////	        // must have their byte order set to native order
//	
//	        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4); // verts * ncoords * bytes per vert??
//	        vbb.order(ByteOrder.nativeOrder());
//	        mFVertexBuffer = vbb.asFloatBuffer();
//	
//	        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
//	        tbb.order(ByteOrder.nativeOrder());
//	        mTexBuffer = tbb.asFloatBuffer();
//	
//	        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
//	        ibb.order(ByteOrder.nativeOrder());
//	        mIndexBuffer = ibb.asShortBuffer();
//	
//	        float[] coords = faceCoordsArray.get(0);
//	        
//	        for (int i = 0; i < VERTS; i++) {
//	            for(int j = 0; j < 3; j++) {
//	            	mFVertexBuffer.put(coords[i*3+j]);
//	            }
//	        }
//	
//	        
////	        for (int i = 0; i < VERTS; i++) {
////	            for(int j = 0; j < 2; j++) {
////	                mTexBuffer[k].put(coords[i*3+j]);
////	            }
////	        }
//	        mTexBuffer.put(textCoords);
//	        
//	
//	        for(int i = 0; i < VERTS; i++) {
//	            mIndexBuffer.put((short) i);
//	        }
//	
//	        mFVertexBuffer.position(0);
//	        mTexBuffer.position(0);
//	        mIndexBuffer.position(0);
////    	}
//    }
//
//    public void draw(GL10 gl) {
//    	
////    	for(int i = 0; i < mTextureId.length; i++){
//    		gl.glActiveTexture(GL10.GL_TEXTURE0);
//	        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);
//	        gl.glTexParameterx(
//	        		GL10.GL_TEXTURE_2D, 
//	        		GL10.GL_TEXTURE_WRAP_S,
//	                GL10.GL_REPEAT);
//	        gl.glTexParameterx(
//	        		GL10.GL_TEXTURE_2D, 
//	        		GL10.GL_TEXTURE_WRAP_T,
//	                GL10.GL_REPEAT);
//	    	
//	        gl.glFrontFace(GL10.GL_CCW);
//	        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);
//	        gl.glEnable(GL10.GL_TEXTURE_2D);
//	        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
//	        gl.glDrawElements(
//	//        		GL10.GL_TRIANGLE_STRIP,
//	        		GL10.GL_TRIANGLE_FAN,
//	//        		GL10.GL_LINE_LOOP,
//	        		VERTS,
//	                GL10.GL_UNSIGNED_SHORT,
//	                mIndexBuffer);
////    	}
//    }
//
//    private final static int VERTS = 4;
//	private final int mCubeFaces = 4;
//	private final int pointsPerFace = 4;
//    
//    private FloatBuffer mFVertexBuffer;
//    private FloatBuffer mTexBuffer;
//    private ShortBuffer mIndexBuffer;
//    
//    /** our texture id */
//    public	int mTextureId;
//}
