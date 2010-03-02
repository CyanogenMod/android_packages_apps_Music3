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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class RockOnBoringRenderer extends RockOnRenderer implements GLSurfaceView.Renderer{

	final String TAG = "RockOnBoringRenderer";
	
	public void renderNow(){
//		if(!mIsRendering)
			mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
	
	public void stopRender()
	{
		mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
    public RockOnBoringRenderer(Context context, Handler requestRenderHandler, int theme) 
    {
        mContext = context;
        mRequestRenderHandler = requestRenderHandler;
        mTheme = theme;
        
        initCursorVars(context, false);
    }
    
    public void changePlaylist(int playlistId){
    	mPositionY = 0;
    	mTargetPositionY = 0;
    	mPositionX = 0;
    	mTargetPositionX = 0;
    	initCursorVars(mContext, true);
    	initCacheVars(true);
    	this.triggerPositionUpdate();
    }
    
    private void initCursorVars(Context context, boolean force){
    	/** init album cursor **/
    	if(mAlbumCursor == null || force){
    		CursorUtils cursorUtils = new CursorUtils(context);
    		mAlbumCursor = 
    			cursorUtils.getAlbumListFromPlaylist(
    				PreferenceManager.getDefaultSharedPreferences(mContext).
    					getInt(
    							Constants.prefkey_mPlaylistId,
    							Constants.PLAYLIST_ALL));
    	}
    }

    private void initCacheVars(boolean force){
       	/** init dimensions */
    	mBitmapWidth = 
    		Constants.ALBUM_ART_TEXTURE_SIZE * 
    		(int) Math.ceil((float)mWidth / (float)Constants.ALBUM_ART_TEXTURE_SIZE);
    	mBitmapHeight = 
    		Constants.ALBUM_ART_TEXTURE_SIZE * 
    		(int) Math.ceil((float)mWidth / (float)Constants.ALBUM_ART_TEXTURE_SIZE);
//    	mBitmapWidth = 
//    		Constants.ALBUM_ART_TEXTURE_SIZE * 
//    		(int) Math.ceil((float)mWidth / 320.f);
//    	mBitmapHeight = 
//    		Constants.ALBUM_ART_TEXTURE_SIZE * 
//    		(int) Math.ceil((float)mWidth / 320.f);
    	
    	mTextRatio = 4;
   	
    	/** albumNavUtils */
    	mAlbumNavItemUtils = new AlbumNavItemUtils(mBitmapWidth, mBitmapHeight);
    	
    	/** init cover bitmap cache */
    	for(int i = 0; i < mCacheSize; i++){
    		mAlbumNavItem[i] = new AlbumNavItem();
        	mAlbumNavItem[i].index = -1;
//    		mAlbumNavItem[i].cover = Bitmap.createBitmap(
//    				mBitmapWidth, 
//    				mBitmapHeight, 
//    				Bitmap.Config.RGB_565);
    		mAlbumNavItem[i].label = Bitmap.createBitmap(
    				mBitmapWidth,
    				mBitmapHeight/mTextRatio,
    				Bitmap.Config.ARGB_8888);
    	}
//    	mColorComponentBuffer = new byte[4*mBitmapWidth*(mBitmapHeight/4)];
    	
    }

    public	Cursor getAlbumCursor(){
    	return mAlbumCursor;
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
//        gl.glGenTextures(mTextureId.length, mTextureId, 0);
        // album labels
        gl.glGenTextures(mTextureLabelId.length, mTextureLabelId, 0);
        // album labels
//        gl.glGenTextures(mTextureAlphabetId.length, mTextureAlphabetId, 0);
        
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
        texturesUpdated = updateTextures(gl);
         
        /* set the fog distance */
        gl.glFogf(GL10.GL_FOG_START, -mEyeNormal[2]);
        gl.glFogf(GL10.GL_FOG_END, -mEyeClicked[2]-mEyeClicked[0]-5.f);

        positionYTmp = mPositionY;
        flooredPositionYTmp = flooredPositionY;
        /* draw each cover */
        for(int i = 0; i<mCacheSize; i++)
        {
        	gl.glLoadIdentity();
                	     
        	if(mAlbumNavItem[i].index != mClickedPosition)
        	{
        		GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, mCenterX, mCenterY, mCenterZ, 0f, -1.0f, 0.0f);
        	}
        	else
        	{
        		GLU.gluLookAt(gl, mEyeNormal[0], mEyeNormal[1], mEyeNormal[2], mCenterX, mCenterY, mCenterZ, 0f, -1.0f, 0.0f);
        	}
        	
        	// poor variable name -- dont mind it
        	deltaToCenter = mAlbumNavItem[i].index - flooredPositionYTmp;
        	// make it all positive
        	deltaToCenter += mCacheSize/2 - 1; // (-4) negative numbers go bad with integer divisions
        	
        	// optimizations, optimizations
//        	if(deltaToCenter < 0 || deltaToCenter > 11)
//        		continue;
        	
        	/* place the covers */
        	// Star wars effect
//        	gl.glRotatef(60.f, 1.f, 0.f, 0.f);

        	gl.glTranslatef(
        			0,  // we just dont need to use delta center here because the navigator always moves by 2 positions (1 row)
        			-4.f*2.f/mTextRatio + deltaToCenter * 2.f/mTextRatio, 
        			0);
        	gl.glTranslatef(
        			0, 
        			-(positionYTmp-flooredPositionYTmp) * 2.f/mTextRatio, 
        			0);

//        	gl.glRotatef(15.f, 1.f, 0.f, 0.f);
        	
        	if(mRockOnAlbumTextLabel != null)
        	{
	        	mRockOnAlbumTextLabel.setTextureId(mTextureLabelId[i]);
	        	mRockOnAlbumTextLabel.draw(gl);
        	}
        	
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
        
        /**
         * Initialize some variables
         */
    	initCacheVars(false);
    	
    	mRockOnAlbumTextLabel = new RockOnAlbumTextLabel(mTextRatio);        
        
    }

    /* optimization */
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
    	y += (mPositionY - flooredPositionY) * (mWidth * 1.f/mTextRatio);
    	if(y >= mHeight/2 - mWidth * .1f)
    		rowFromY = (int) ((y - (mHeight/2 - mWidth * .5f/mTextRatio)) / (mWidth * 1.f/mTextRatio));
    	else
    		rowFromY = -1 - (int) (((mHeight/2 - mWidth * .5f/mTextRatio) - y) / (mWidth * 1.f/mTextRatio));
    	
    	if(true || mPositionY == mTargetPositionY)
	    	return 
	    		flooredPositionY + rowFromY;
    	else
    		return flooredPositionY;
    }
    
    int getVerifiedPositionFromScreenCoordinates(float x, float y)
    {
    	return
    		Math.min(
	    		Math.max(
	    			getPositionFromScreenCoordinates(x,y),
	    			0),
	    		mAlbumCursor.getCount()-1);
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
    	if(y >= mHeight/2)
    		rowFromY = (int) ((y - (mHeight/2)) / (mWidth * 1.f/mTextRatio));
    	else
    		rowFromY = -1 - (int) (((mHeight/2) - y) / (mWidth * 1.f/mTextRatio));
    		
    	columnFromX = 0;
    	
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
    	
//    	this.mCenterTargetX = -1 + 2*getRowAndColumnFromScreenCoordinates(x, y)[0];
//    	this.mCenterTargetY = -2 + 2*getRowAndColumnFromScreenCoordinates(x, y)[1]; // duplicated effort -- FIXME
//    	this.mCenterTargetZ = 0;
//    	
//    	this.mCenterInitialX = this.mCenterX;
//    	this.mCenterInitialY = this.mCenterY;
//    	this.mCenterInitialZ = this.mCenterZ;
    	
    	this.mClickedPosition = getVerifiedPositionFromScreenCoordinates(x, y);
    	
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
    	
//    	this.mCenterTargetX = 0;
//    	this.mCenterTargetY = 0;
//    	this.mCenterTargetZ = 0;
//    	
//    	this.mCenterInitialX = this.mCenterX;
//    	this.mCenterInitialY = this.mCenterY;
//    	this.mCenterInitialZ = this.mCenterZ;
    	
//    	this.mClickedPosition = -1000;
    	    	
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
		if(mAlbumCursor != null){
//			Log.i(TAG, " ++ updating textures");
	    	/* Album Cover textures in vertical scrolling */
    		for(int i = 0; i < mCacheSize; i++){
    			// we try to minimize cache reshuffling to the max
    			albumIndexTmp = 
    				(int)Math.floor(flooredPositionY / mCacheSize) * mCacheSize
    				+ i;
    			if(albumIndexTmp < 
    					flooredPositionY  
        					- 4)
    			{
    				albumIndexTmp += mCacheSize;
    			} 
    			// should never happen
    			else if(albumIndexTmp >=
    					flooredPositionY 
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
    AlbumNavItem 	albumNavItemTmp;
    boolean			reusedCached = false;
    /**
     * fills the albumNavItem structure with the cover bitmap, labels, etc
     * @param gl
     * @param cacheIndex
     * @param albumIndex
     * @param force
     * @return
     */
    private boolean setupAlbumTextures(GL10 gl, int cacheIndex, int albumIndex, boolean force){
    	/** texture needs update? */
    	if(mAlbumNavItem[cacheIndex].index != albumIndex || force){
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
	    		mAlbumNavItem[cacheIndex].index = albumIndex;
	    		if(albumIndex < 0 || albumIndex >= mAlbumCursor.getCount())
	    		{
	//    			Log.i(TAG, "BM failed, index oob");
	    			mAlbumNavItem[cacheIndex].albumName = "";
	    			mAlbumNavItem[cacheIndex].artistName = "";
	    			mAlbumNavItem[cacheIndex].cover = undefined;
	    			mAlbumNavItem[cacheIndex].cover.eraseColor(Color.argb(255, 0, 0, 0));
	    			mAlbumNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
	    		} 
	    		else 
	    		{
		    		if(!mAlbumNavItemUtils.fillAlbumInfo(
		    				mAlbumCursor, 
		    				mAlbumNavItem[cacheIndex], 
		    				albumIndex))
		    		{
	//	    			Log.i(TAG, "Info Failed");
		    			mAlbumNavItem[cacheIndex].albumName = null;
		    			mAlbumNavItem[cacheIndex].artistName = null;
		    			mAlbumNavItem[cacheIndex].albumKey = null;
		    		}
		    		if(!mAlbumNavItemUtils.fillAlbumBoringLabel(
		    				mAlbumNavItem[cacheIndex],
		    				mAlbumNavItem[cacheIndex].label.getWidth(),
		    				mAlbumNavItem[cacheIndex].label.getHeight()))
		    		{
		    			mAlbumNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
		    		}
	    		}
//    		}
    		    		
	    	/** bind new texture */
//    		bindTexture(gl, mAlbumNavItem[cacheIndex].cover, mTextureId[cacheIndex]);
    		bindTexture(gl, mAlbumNavItem[cacheIndex].label, mTextureLabelId[cacheIndex]);
    		
    		return true;
    	} else  {
    		return false;
    	}
    	
    }
    
    private void bindTexture(GL10 gl, Bitmap bitmap, int textureId){
    	/** bind new texture */
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
    	
        gl.glTexParameterf(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_LINEAR);
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
    private boolean updatePosition(boolean force){
    	
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
									* Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionY-mPositionY), 
								updateFraction 
									* .5f * Constants.MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * Constants.MAX_SCROLL * 111112.f); // XXX *4.f is a HACK
		else if(mTargetPositionY < mPositionY)
			mPositionY	 += 
				Math.max(
					Math.max(
						Math.min(
							updateFraction
								* Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionY-mPositionY), 
							updateFraction 
								* .5f * -Constants.MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * -Constants.MAX_SCROLL * 11111112.f); // XXX *4.f is a HACK

		/** are we outside the limits of the album list?*/
    	if(mAlbumCursor != null){
    		/** Y checks */
    		if(mPositionY <= 0)
	    		mTargetPositionY = Math.min(1, (mAlbumCursor.getCount()-1));
	    	else if(mPositionY >= (mAlbumCursor.getCount() - 1))
	    		mTargetPositionY = (mAlbumCursor.getCount() - 1) - 1;
	    	
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
			mTargetPositionY = mPositionY - px/(mHeight*1.f/mTextRatio);
			/* make we dont exceed the cube limits */
			if(mTargetPositionY <= -1 + Constants.MAX_POSITION_OVERSHOOT)
				mTargetPositionY = -1 + Constants.MAX_POSITION_OVERSHOOT;
			else if(mTargetPositionY >= (mAlbumCursor.getCount() - 1) - 1 + Constants.MAX_POSITION_OVERSHOOT)
				mTargetPositionY = (mAlbumCursor.getCount() - 1) - 1 + Constants.MAX_POSITION_OVERSHOOT;
			
			mPositionY = mTargetPositionY;
    		return;
    	}
    }
    
    /** inertial scroll on touch end */
    public void	inertialScrollOnTouchEnd(float scrollSpeed, int direction)
    {
    	switch(direction)
    	{
    	case Constants.SCROLL_MODE_VERTICAL:
			mTargetPositionY = 
				Math.round(
						mPositionY
						+
						Constants.SCROLL_SPEED_BOOST
						*
						scrollSpeed/(mHeight*1.f/mTextRatio)
				);
		
    		/* small optimization to avoid weird moves on the edges */
    		if(mTargetPositionY == 0)
    			mTargetPositionY = -2;
    		else if(mTargetPositionY == getAlbumCount())
    			mTargetPositionY = getAlbumCount() + 1;		
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
    	if(mAlbumCursor == null)
    		return -1;
    	else
    		return mAlbumCursor.getCount();
    }
    
    /** get the current position */
    int	getShownPosition(float x, float y){
    	return getPositionFromScreenCoordinates(x, y);
    }
    
    /* optimization */
    int cursorIdxValidation;
    /** get the current Album Id */
    int getShownAlbumId(float x, float y){
    	if(mTargetPositionY != mPositionY ||
    		mAlbumCursor == null ||
			/**
			 * FIXME: this is a quick cursor overflow bugfix, unverified
			 */
    		getPositionFromScreenCoordinates(x, y) > mAlbumCursor.getCount() - 1)
//    		(int) mPositionY > mAlbumCursor.getCount() - 1)
    	{
//    		Log.i(TAG, "Target was not reached yet: "+mTargetPosition+" - "+mPosition);
    		return -1;
    	}
    	else{
    		int tmpIndex = mAlbumCursor.getPosition();
    		
    		// validate idx -- bug report
    		mAlbumCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
//    		cursorIdxValidation = getPositionFromScreenCoordinates(x, y);
//    		if(cursorIdxValidation < 0)
//    			mAlbumCursor.moveToFirst();
//    		else if(cursorIdxValidation >= mAlbumCursor.getCount())
//    			mAlbumCursor.moveToLast();
//    		else
//    			mAlbumCursor.moveToPosition(cursorIdxValidation);

    		int albumId = mAlbumCursor.getInt(
    				mAlbumCursor.getColumnIndexOrThrow(
    						MediaStore.Audio.Albums._ID));
    		mAlbumCursor.moveToPosition(tmpIndex);
    		return albumId;
    	}
    }
    
    /** get the current Album Name */
    String getShownAlbumName(float x, float y){
    	if(mTargetPositionY != mPositionY)
    		return null;
    	else{
    		int tmpIndex = mAlbumCursor.getPosition();
    		mAlbumCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
    		String albumName = mAlbumCursor.getString(
    				mAlbumCursor.getColumnIndexOrThrow(
    						MediaStore.Audio.Albums.ALBUM));
    		mAlbumCursor.moveToPosition(tmpIndex);
    		return albumName;
    	}	
    }
    
    /** get the current Album Name */
    String getShownAlbumArtistName(float x, float y){
    	if(mTargetPositionY != mPositionY)
    		return null;
    	else{
    		int tmpIndex = mAlbumCursor.getPosition();
    		mAlbumCursor.moveToPosition(getVerifiedPositionFromScreenCoordinates(x,y));
    		String artistName = mAlbumCursor.getString(
    				mAlbumCursor.getColumnIndexOrThrow(
    						MediaStore.Audio.Albums.ARTIST));
    		mAlbumCursor.moveToPosition(tmpIndex);
    		return artistName;
    	}
    }
    
    /** move navigator to the specified album Id */
    int setCurrentByAlbumId(long albumId){

    	if(albumId >= 0)
    	{
	    	if(mAlbumCursor != null){
		    	for(int i = 0; i < mAlbumCursor.getCount()-1; i++){
		    		mAlbumCursor.moveToPosition(i);
		    		if(mAlbumCursor.getLong(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)) == albumId){
		    			mTargetPositionY = i;
		    			mPositionY = 
		    				mTargetPositionY - 
		    				Math.signum(mTargetPositionY - mPositionY)
		    				*
		    				Math.min(
		    					Math.abs(mTargetPositionY-mPositionY), 
		    					10.f);
		    			// TODO: trigger rotation
		    			this.renderNow();
		    			return i;
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
     * Class members
     */
    private int 					mTheme;
    private	int						mCacheSize = 10; // 2 covers at the center row and then 2 more rows up and 2 more rows down
    private Context 				mContext;
//    private Handler				mRequestRenderHandler;
    private RockOnAlbumTextLabel	mRockOnAlbumTextLabel;
    private int[] 					mTextureId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private int[] 					mTextureLabelId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
//    private int[] 				mTextureAlphabetId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private	int						mScrollMode = Constants.SCROLL_MODE_VERTICAL;
    public	boolean					mClickAnimation = false;
    private	Cursor					mAlbumCursor = null;
//    private AlphabetNavItem[]		mAlphabetNavItem = new AlphabetNavItem[mCacheSize];
    private AlbumNavItem[]			mAlbumNavItem = new AlbumNavItem[mCacheSize];
    private AlbumNavItemUtils		mAlbumNavItemUtils;
    private	int						mBitmapWidth;
    private int 					mBitmapHeight;
    private	boolean					mForceTextureUpdate = false;
    private int						mWidth = 0;
    private int						mHeight = 0;
    private int						mTextRatio;

    private float[]		mEyeNormal = 
    {
    		0.f,
    		0.f,
    		-4.f
    };
    private float[]		mEyeClicked = 
    {
    		-12.f, // XX dont care
    		0.f, // XX dont care
    		-4.f
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
    private int			mClickedPosition;
    
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


class RockOnAlbumTextLabel {
	
	private final String TAG = "RockOnAlbumTextLabel";
	
    public RockOnAlbumTextLabel(int mTextRatio) {
//  public RockOnCover(int[] textureId, int[] textureAlphabetId) {
    	/**
    	 * cover coordinates
    	 */
    	float[] coords = {
        		// X, Y, Z
        		-1.f, 1.f/mTextRatio, 0.f,
        		1.f, 1.f/mTextRatio, 0.f, 
        		1.f, -1.f/mTextRatio, 0.f,
        		-1.f, -1.f/mTextRatio, 0.f
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
	    // FOR THE G1 For Some reason
//        float[] textCoords = {
//        		0.f, .80f,
//        		1.f, .80f,
//        		1.f, .10f,
//        		0.f, .10f
//        };
        
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
                GL10.GL_CLAMP_TO_EDGE);
//        		GL10.GL_REPEAT);
        gl.glTexParameterx(
        		GL10.GL_TEXTURE_2D, 
        		GL10.GL_TEXTURE_WRAP_T,
        		GL10.GL_CLAMP_TO_EDGE);
//        		GL10.GL_REPEAT);
    	
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

