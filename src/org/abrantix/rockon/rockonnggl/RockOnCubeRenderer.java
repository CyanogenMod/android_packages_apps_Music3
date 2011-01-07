/*
 * 
 */

package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.cm.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class RockOnCubeRenderer extends RockOnRenderer implements GLSurfaceView.Renderer{

	final String TAG = "RockOnCubeRenderer";
	boolean mStopThreads = false;	
	
	/** renderer mode */
	public int getType()
	{
		return Constants.RENDERER_CUBE;
	}
	
//	public boolean needsRender()
//	{
//        if(mTargetPositionX != mPositionX ||
//            	mPositionX != 0 ||
//            	mTargetPositionY != mPositionY || 
//            	mClickAnimation ||
//            	texturesUpdated)
//        {
//            return true;
//        }
//        else
//        {
//        	Log.i(TAG, "mTargetY: "+mTargetPositionY+" mPositionY: "+mPositionY);
//        	return false;    	
//        }
//    }
	
    public RockOnCubeRenderer(
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
    			/**
    			 * We use two cursor in order to avoid multi thread problems
    			 * when changing browsing category
    			 */
    			Cursor helperCursor = 
    				cursorUtils.getArtistListFromPlaylist(Constants.PLAYLIST_ALL);
    			mCursor = helperCursor;
    			// Let's add the audio id to the artist list so we can get art
//    			double t = System.currentTimeMillis();
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
    			helperThread.start();
    		}
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
//    		mNavItem[i] = new NavItem();
//        	mNavItem[i].index = -1;
//    		mNavItem[i].cover = Bitmap.createBitmap(
//    				mBitmapWidth, 
//    				mBitmapHeight, 
//    				Bitmap.Config.RGB_565);
//    		mNavItem[i].label = Bitmap.createBitmap(
//    				mBitmapWidth,
//    				mBitmapHeight/4,
//    				Bitmap.Config.ARGB_8888);
    	}
        mColorComponentBuffer = new byte[4*mBitmapWidth*mBitmapHeight];
    	
    	/** init alphabet bitmap cache */
        for(int i = 0; i < mCacheSize; i++){
    		mAlphabetNavItem[i] = new AlphabetNavItem();
    		mAlphabetNavItem[i].letter = -1;
    		mAlphabetNavItem[i].letterBitmap = Bitmap.createBitmap(
    				mBitmapWidth, 
    				mBitmapHeight, 
    				Bitmap.Config.ARGB_8888);
    	}
        
//        /**
//         * debug
//         */
//        try
//        {
//	        for(int i = 0; i < 50; i++)
//	        {
//	        	Log.i(TAG, "b: "+i);
//	        	b[i] = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565);
//	        }
//        }
//        catch(OutOfMemoryError err)
//        {
//        	err.printStackTrace();
//			
//        	Intent i = new Intent(context, RockOnNextGenService.class);
//			i.setAction(Constants.SERVICECMD);
//            i.putExtra(Constants.CMDNAME, Constants.CMDSAVE);
//            context.startService(i);
//            
////        	android.os.Process.killProcess(android.os.Process.myPid());
//        }
    }

//    Bitmap[] b = new Bitmap[1000];

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
        gl.glEnable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_DEPTH_TEST);
//        gl.glDisable(GL10.GL_BLEND);
//        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);
        
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        /*
         * Create our textures. This has to be done each time the
         * surface is created.
         */
        // album covers - vertical
        gl.glGenTextures(mTextureId.length, mTextureId, 0);
        // album labels
        gl.glGenTextures(mTextureLabelId.length, mTextureLabelId, 0);
        // album labels
        gl.glGenTextures(mTextureAlphabetId.length, mTextureAlphabetId, 0);

        mRockOnCube = new RockOnCube(mTextureId, mTextureAlphabetId);
        mAlbumLabelGlText = new AlbumLabelGlText(mTextureLabelId[0]);
        
        
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
//        gl.glDisable(GL10.GL_DITHER);
//        gl.glEnable(GL10.GL_DEPTH_TEST);
//        gl.glEnable(GL10.GL_LINE_SMOOTH);
//        gl.glEnable(GL10.GL_LINE_SMOOTH_HINT);
//        gl.glEnable(GL10.GL_BLEND);
//        gl.glBlendFunc(
//        		GL10.GL_ONE, 
//        		GL10.GL_ONE_MINUS_SRC_ALPHA);
//        		GL10.GL_ONE_MINUS_SRC_ALPHA);
//        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
        
        gl.glTexEnvx(
        		GL10.GL_TEXTURE_ENV, 
        		GL10.GL_TEXTURE_ENV_MODE,
                GL10.GL_MODULATE);

//        /**
//         * LIGHT
//         */
//        float[] lightAmbient= { 
//        		1.f, 
//        		1.f, 
//        		1.f, 
//        		1.f };				
//        float[] lightDiffuse= { 
//        		1.f, 
//        		1.f, 
//        		1.f, 
//        		1.f };				
//        float[] lightPosition= { 
//        		0.f, 
//        		-4.5f, 
//        		2.5f, 
//        		3.0f };				
//        
//        gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_AMBIENT, FloatBuffer.wrap(lightAmbient));
//        gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_AMBIENT, FloatBuffer.wrap(lightAmbient));
//        gl.glLightfv(GL10.GL_LIGHT3, GL10.GL_AMBIENT, FloatBuffer.wrap(lightAmbient));
////        gl.glLightfv(GL10.GL_LIGHT4, GL10.GL_AMBIENT, FloatBuffer.wrap(lightAmbient));
////        gl.glLightfv(GL10.GL_LIGHT5, GL10.GL_AMBIENT, FloatBuffer.wrap(lightAmbient));
//        
//        gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT3, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT4, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT5, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT6, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        gl.glLightfv(GL10.GL_LIGHT7, GL10.GL_DIFFUSE, FloatBuffer.wrap(lightDiffuse));
//        
//        gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT2, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT3, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT4, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT5, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT6, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        gl.glLightfv(GL10.GL_LIGHT7, GL10.GL_POSITION, FloatBuffer.wrap(lightPosition));
//        
//        gl.glEnable(GL10.GL_LIGHT1);
//        gl.glEnable(GL10.GL_LIGHT2);
//        gl.glEnable(GL10.GL_LIGHT3);
//        gl.glEnable(GL10.GL_LIGHT4);
//        gl.glEnable(GL10.GL_LIGHT5);
//        gl.glEnable(GL10.GL_LIGHT6);
//        gl.glEnable(GL10.GL_LIGHT7);
        
//        gl.glEnable(GL10.GL_LIGHTING);
        
        /** 
         * FOG 
         */
        gl.glEnable(GL10.GL_FOG);
        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
//        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_EXP); // GL_EXP2 doesnt show anything
        gl.glFogf(GL10.GL_FOG_START, 4.5f);
        gl.glFogf(GL10.GL_FOG_END, 5.5f);
//        float[] fogColor = {.5f,.5f,.5f, 1.f};
//        gl.glFogfv(GL10.GL_FOG_COLOR, FloatBuffer.wrap(fogColor));
        gl.glHint(GL10.GL_FOG_HINT, GL10.GL_NICEST);
    }

    public int getItemDimension(){
    	return (int)(mHeight * .8f);
    }
    
    /* optimization */
    float	distanceToRotationLimits = 0.f;
    double	frameStartTime ;
    double	lastSecond = 0;
    double	fps = 0;
    boolean drawOneMoreTime = false;
    public void onDrawFrame(GL10 gl) {
    	  
    	frameStartTime = System.currentTimeMillis();
    	mIsRendering = true;
    	
    	/** Calculate new position */
    	if(!updatePosition(false)){
    		
    	} 
    	
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
  
    	mRockOnCube.setHorizontalIndex((int) mPositionX, mPositionX);
    	mRockOnCube.setVerticalIndex((int) mPositionY, mPositionY);
    	
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
        	/* camera eye update */
        	mEyeX += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetX-mEyeX);
        	mEyeY += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetY-mEyeY);
        	mEyeZ += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetZ-mEyeZ);
        	/* minimum movements */
        	if(Math.abs(mEyeTargetX - mEyeX) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction 
        			< updateFraction * Constants.MIN_SCROLL)
        		mEyeX +=  
        			Math.signum(mEyeTargetX - mEyeX) * 
        				Math.min(
        					Math.abs(mEyeTargetX - mEyeX),
        					updateFraction * Constants.MIN_SCROLL);
        	if(Math.abs(mEyeTargetY - mEyeY) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction 
        			< updateFraction * Constants.MIN_SCROLL)
        		mEyeY += 
        			Math.signum(mEyeTargetY - mEyeY) * 
        				Math.min(
        						Math.abs(mEyeTargetY - mEyeY),
        						updateFraction * Constants.MIN_SCROLL);
        	if(Math.abs(mEyeTargetZ - mEyeZ) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction 
        			< updateFraction * Constants.MIN_SCROLL)
        		mEyeZ += 
        			Math.signum(mEyeTargetZ - mEyeZ) * 
        				Math.min(
        						Math.abs(mEyeTargetZ - mEyeZ),
        						updateFraction * Constants.MIN_SCROLL);
        	/* end of animation */
//        	Log.i(TAG, "X: "+mEyeX+" - "+mEyeTargetX);
//        	Log.i(TAG, "Y: "+mEyeY+" - "+mEyeTargetY);
//        	Log.i(TAG, "Z: "+mEyeZ+" - "+mEyeTargetZ);
        	if(mEyeX == mEyeTargetX && mEyeY == mEyeTargetY && mEyeZ == mEyeTargetZ &&
        			!isSpinning())
        		mClickAnimation = false;
        }
        else
        {
            /* move camera when scrolling cube in Y axis */
        	if(mPositionY != mTargetPositionY)
        	{
        		distanceToRotationLimits = 
        			Math.max(
        				Math.min(
	            			.5f 
	            			* 
	            			(Math.min(
	            					Math.abs(mPositionY - mTargetPositionY),
	            					Math.abs(mPositionY - mRotationInitialPositionY))
	            					-1),
	            			1.5f),
	            		0.f);
	            			
        		// adjust our 'eye'
	            mEyeZ = 
	            	-5.f 
	            	- 
	            	distanceToRotationLimits;
	            // adjust the fog
	            gl.glFogf(
	            		GL10.GL_FOG_START, 
	            		4.5f + .5f * distanceToRotationLimits);
	            gl.glFogf(
	            		GL10.GL_FOG_END, 
	            		5.5f + distanceToRotationLimits);
        	}
        }
        
//        gl.glDisable(GL10.GL_FOG);
        
    	GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, 0f, 0f, 0f, 0f, -1.0f, 0.0f);
        
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY); // LIGHTING
        
        /* Center (or at least compensate) in the X axis */
        gl.glTranslatef(.1f, 0.f, 0.f);
        
        /* Calculate rotations */
        if(mPositionX != 0)
	        rotationAngleX = 
	        	((flooredPositionX % mCacheSize) + positionOffsetX)
	        	/
	        	4.f
	        	*
	        	360.f;
        else
        	rotationAngleX = 0;
        
        rotationAngleY = 
        	((flooredPositionY % mCacheSize) + positionOffsetY)
        	/
        	(float) mCacheSize
        	*
        	360.f;
                
        if(mScrollMode == Constants.SCROLL_MODE_VERTICAL){
        	gl.glRotatef(rotationAngleX, 0.f, 1.f, 0.f);
        	gl.glRotatef(-rotationAngleY, 1.f, 0.f, 0.f);
        }
                
    	/* 
    	 * ??
    	 */
//        if(texturesUpdated)
//	        gl.glTexEnvx(
//	        		GL10.GL_TEXTURE_ENV, 
//	        		GL10.GL_TEXTURE_ENV_MODE,
//	                GL10.GL_MODULATE);	        
        
//        if(!texturesUpdated){	// avoids a strange flickering...
//        	mRockOnCube.draw(gl);
        
        /* draw cube */
    	mRockOnCube.draw(gl);
//    	if(rotationAngleY != 0)
//    	{
//    		gl.glDisable(GL10.GL_BLEND);
//    		gl.glRotatef(2.f, 1.f, 0.f, 0.f);
//    		mRockOnCube.draw(gl);
//    		gl.glRotatef(2.f, 1.f, 0.f, 0.f);
//    		mRockOnCube.draw(gl);
//    		gl.glRotatef(2.f, 1.f, 0.f, 0.f);
//    		mRockOnCube.draw(gl);
//    		gl.glRotatef(2.f, 1.f, 0.f, 0.f);
//    		mRockOnCube.draw(gl);
//    		gl.glEnable(GL10.GL_BLEND);  		
//    	}
    	
    	/* draw label */
    	if(Math.abs(mPositionY - mTargetPositionY) < .5f &&
    		mPositionX > -1.f &&
    		mPositionX < 1.f)
    	{
        	gl.glLoadIdentity();
            GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, 0f, 0f, 0f, 0f, -1.0f, 0.0f);
            gl.glTranslatef(.1f, -1.55f, -1.f);
            // X rotation
            gl.glRotatef(rotationAngleX, 0.f, 1.f, 0.f);
            // Y rotation
	        gl.glRotatef((.5f - Math.abs(positionOffsetY-0.5f))*2*90.f, 1.f, 0.f, 0.f);
	        tmpTextureIdx = (int)Math.round(mPositionY%mTextureId.length);
        	if(tmpTextureIdx >= mTextureId.length || 
        		tmpTextureIdx < 0)
            	mAlbumLabelGlText.setTexture(mTextureLabelId[0]);
            else
            	mAlbumLabelGlText.setTexture(mTextureLabelId[tmpTextureIdx]);
        	mAlbumLabelGlText.draw(gl);
        }
    	
    	mIsRendering = false;
    	
        if(mPositionX == 0 &&
        	positionOffsetY == 0 &&
        	positionOffsetX == 0 &&
        	mPositionX % 1 == 0 &&
        	mPositionY % 1 == 0 &&
        	!mClickAnimation &&
        	!texturesUpdated)
        {
        	stopRender();
        }
        else
        {
//        	// avoid showing the scrollbar when doing click animations
//        	if(!mClickAnimation)
        		renderNow();
        }
        
//        Log.i(TAG, "XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
//        Log.i(TAG, "mTargetPositionX: "+mTargetPositionX+" mPositionX: "+mPositionX);
//        Log.i(TAG, "mTargetPositionY: "+mTargetPositionY+" mPositionY: "+mPositionY);
//        Log.i(TAG, "mEyeX: "+mEyeX+" mEyeY: "+mEyeY+" mEyeZ: "+mEyeZ);
//        Log.i(TAG, "mEyeTargetX: "+mEyeTargetX+" mEyeTargetY: "+mEyeTargetY+" mEyeTargetZ: "+mEyeTargetZ);
//        Log.i(TAG, "mClickAnimation: "+mClickAnimation);
//        Log.i(TAG, "texturesUpdated: "+texturesUpdated);
        
//      Log.i(TAG, ""+(System.currentTimeMillis() - frameStartTime));
//        fps++;
//        if(System.currentTimeMillis()-lastSecond > 1000)
//        {
//        	Log.i(TAG, "XXXXXXXXXXXXXXXXXX");
//        	Log.i(TAG, "fps: "+fps);
//        	fps=0;
//        	lastSecond = System.currentTimeMillis();
//        }
    }

    @Override
    public float getScrollPosition()
    {
    	return mPositionY/mCursor.getCount();
    }
    
    /**
     * 
     */
    @Override
    public void setCurrentTargetYByProgress(float progress)
    {
    	mTargetPositionY = Math.min(
    			Math.round(progress * mCursor.getCount()),
    			mCursor.getCount()-1);
//    	mPositionY = mTargetPositionY;
    	if(Math.abs(mPositionY - mTargetPositionY) > 5)
    		mPositionY = 
    			mTargetPositionY -
    			Math.signum(mTargetPositionY - mPositionY) * 5.f;
    	renderNow();
    }
    
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        mHeight = h;
        mWidth = w;
        
        /* if the screen is big make the cube a little bit smaller */
        if(mWidth > 320 && mHeight > mWidth)
        {
        	mViewportTop = .20f * (mWidth-320)/2 * h/w;
        	mViewportHeight = (mWidth - .20f * (mWidth - 320)) * h/w;
        	gl.glViewport(
        			(int) (.20f * (mWidth-320)/2), 						// x
        			(int) (mViewportTop),				// y 
        			(int) (mWidth - .20f * (mWidth - 320)), 			// width
        			(int) (mViewportHeight));	// height
        }
        else
        {
        	mViewportTop = 0.f;
        	mViewportHeight = h;
        	gl.glViewport(0, 0, w, h);
        }
        
        
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

    void showClickAnimation(float x, float y){
    	this.mClickAnimation = true;
    	// this should be in the constants -- too lazy
    	this.mEyeTargetX = .75f;
    	this.mEyeTargetY = -2.f;
    	this.mEyeTargetZ = -5.75f;
    	pTimestamp = System.currentTimeMillis();
    	this.renderNow();
    }
    
    void reverseClickAnimation(){
    	this.mClickAnimation = true;
    	// this should be in the constants -- too lazy
    	this.mEyeTargetX = .75f;
    	this.mEyeTargetY = -2.f;
    	this.mEyeTargetZ = -5.f;
    	pTimestamp = System.currentTimeMillis();
    	this.renderNow();
    }
    
    int getClickActionDelay(){
    	return Constants.CLICK_ACTION_DELAY;
    }
    
    void forceTextureUpdateOnNextDraw(){
    	mForceTextureUpdate = true;
    	this.renderNow();
    }
    
    int		cursorIndexTmp;
    int		alphabetIndexTmp;
    int		lastInitial = -1;
    int		charTmp;
    boolean changed;
    private boolean updateTextures(GL10 gl){
    	changed = false;
    	if(mCursor != null && !mCursor.isClosed())
    	{
	    	/* Album Cover textures in vertical scrolling */
    		for(int i = 0; i < mCacheSize; i++){
	    		cursorIndexTmp = (int) (Math.floor((float) flooredPositionY / (float) mCacheSize) * mCacheSize + i);
	    		if(cursorIndexTmp - flooredPositionY > 2){
	    			cursorIndexTmp -= mCacheSize;
	    		} else if (cursorIndexTmp - flooredPositionY < -1){
	    			cursorIndexTmp += mCacheSize;
	    		}
	    		
	//    		Log.i(TAG, 
//	    			"albumIndexTmp: "+albumIndexTmp+
//	    			" flooredPosition: "+flooredPosition+
//	    			" mPosition: "+mPosition);
	    		
	    		if(setupAlbumTextures(gl, i, cursorIndexTmp, mForceTextureUpdate))
	    			changed = true;
	    	}
	    	
    		/* Alphabetical textures in horizontal scrolling of the cube */
    		if(mPositionX != 0){
				for(int i=0; i<mTextureAlphabetId.length; i++){    	    		
					try
	    			{
						if(lastInitial == -1){
	    					/** 
	    					 * FIXME: quick fix for bug, untested, unchecked
	    					 */
	    					if(flooredPositionY > mCursor.getCount() - 1)
							{
	    						mCursor.moveToLast();
	    						Log.i(TAG, "Renderer album cursor overflow XXX FIXME");
							} 
	    					else if(flooredPositionY < 0)
	    					{
	    						mCursor.moveToFirst();
	    						Log.i(TAG, "Renderer album cursor overflow XXX FIXME");
	    					}
	    					else
							{
								mCursor.moveToPosition(flooredPositionY);
							}
	    					
	    					/*
	    					 * Get the first meaningful initial of the current position
	    					 */
	    			    	lastInitial = getFirstLetterLowerCaseAbstractFromCategory(
	    			    			mBrowseCat, 
	    			    			mCursor, 
	    			    			oCharArrayBuffer);
	    				}
	    				
	//    				Log.i(TAG, "flooredX: "+flooredPositionX);
	//    				Log.i(TAG, "lastInitial: "+(char)lastInitial);
	//    				Log.i(TAG, "flooredPositionX: "+flooredPositionX+" (int)/mCacheSize: "+(int)((flooredPositionX)/mCacheSize));
	//    				Log.i(TAG, "flooredPositionX: "+flooredPositionX+" %mCacheSize: "+(flooredPositionX)%mCacheSize);
	    				
	    				charTmp = 
	    					lastInitial + 
	    					(int)((flooredPositionX)/mCacheSize)*mCacheSize + 
	    					i;
		    				
		    			/**
		    			 * Transition special cases
		    			 */
		    			/* cache turn point when going forward in the alphabet */
	    				if(flooredPositionX%mCacheSize == mCacheSize - 1 &&
		    				i == 0)
		    			{
		    				charTmp += mCacheSize;
		    			}
		    			
		    			/* negative scrolling (going back in the alphabet) */
		    			if(flooredPositionX%mCacheSize < 0 &&
		    				i != 0)
		    			{
		    				charTmp -= mCacheSize;
		    			}
	
	
	    	    		if(setupAlphabetTextures(gl, i, charTmp, mForceTextureUpdate))
	    	    			changed = true;
	    	    		
	    	    		/* DEBUG CODE */
	//    				undefined.eraseColor(Color.CYAN);
	//    	    		bindTexture(gl, undefined, mTextureAlphabetId[i]);
	    			}
	    			catch(NullPointerException e)
	    			{
	    				e.printStackTrace();
	    			}
	    			catch(IllegalStateException e) {
	    				e.printStackTrace();
	    			}
    			}
    		}
	    	
	    	if(mForceTextureUpdate)
	    		mForceTextureUpdate = false;
    	}
    	return changed;
    }
    
//    Bitmap undefined = BitmapFactory.decodeResource(
//    		mContext.getResources(), 
//    		R.drawable.unknown_256); 
//    	Bitmap.createBitmap(
//			Constants.REASONABLE_ALBUM_ART_SIZE, 
//			Constants.REASONABLE_ALBUM_ART_SIZE, 
//			Bitmap.Config.RGB_565);
//    
    private boolean setupAlbumTextures(GL10 gl, int cacheIndex, int navIndex, boolean force){
    	/** texture needs update? */
    	if(mNavItem[cacheIndex].index != navIndex || force){
	    	
//    		Log.i(TAG, "albumIndexTmp: "+albumIndexTmp+" flooredPosition: "+flooredPosition+" mPosition: "+mPosition);

    		/** Update cache item */
    		mNavItem[cacheIndex].index = navIndex;
    		if(navIndex < 0 || navIndex >= mCursor.getCount())
    		{
    			mNavItem[cacheIndex].albumName = "";
    			mNavItem[cacheIndex].artistName = "";
//    			mAlbumNavItemUtils.fillAlbumUnknownBitmap(
//    					mAlbumNavItem[cacheIndex], 
//    					mContext.getResources(), 
//    					mAlbumNavItem[cacheIndex].cover.getWidth(), 
//    					mAlbumNavItem[cacheIndex].cover.getHeight(), 
//    					mColorComponentBuffer, 
//    					mTheme);
    			try
    			{
	    			if(!mNavItem[cacheIndex].cover.isRecycled())
	    				mNavItem[cacheIndex].cover.eraseColor(Color.argb(127, 122, 122, 0));
	    			// we cannot change the bitmap reference of the item
	    			// we need to write to the existing reference
	    			if(!mNavItem[cacheIndex].label.isRecycled())
	    				mNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
    			}
    			catch(NullPointerException e)
    			{
    				// cover and/or label are still being created
    				e.printStackTrace();
    			}
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
		    			mNavItemUtils.fillAlbumUnknownBitmap(
		    					mNavItem[cacheIndex], 
		    					mContext.getResources(), 
		    					mNavItem[cacheIndex].cover.getWidth(), 
		    					mNavItem[cacheIndex].cover.getHeight(), 
		    					mColorComponentBuffer, 
		    					mTheme);	
		    		}
		    		if(!mNavItemUtils.fillAlbumLabel(
		    				mNavItem[cacheIndex],
		    				mBitmapWidth,
		    				mBitmapHeight/4))
		    		{
		    			if(!mNavItem[cacheIndex].label.isRecycled())
		    				mNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
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
			    		if(!mNavItemUtils.fillArtistLabel(
			    				mNavItem[cacheIndex],
			    				mBitmapWidth,
			    				mBitmapHeight/4))
			    		{
			    			if(!mNavItem[cacheIndex].label.isRecycled())
			    				mNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
			    		}
    				} else {
    					mNavItem[cacheIndex].index = -1;
    				}
    				break;
    			}
    		}
    		
//    		Log.i(TAG, "cacheIndex: "+cacheIndex+"");
    		
	    	/** bind new texture */
    		bindTexture(gl, mNavItem[cacheIndex].cover, mTextureId[cacheIndex]);
    		bindTexture(gl, mNavItem[cacheIndex].label, mTextureLabelId[cacheIndex]);
    		
    		return true;
    	} else  {
    		return false;
    	}
    	
    }
    
    private boolean setupAlphabetTextures(GL10 gl, int cacheIndex, int letter, boolean force){
//		Log.i(TAG, "letter: "+(char)letter);

    	/** precheck albumNavItem */
    	if(mAlphabetNavItem[cacheIndex].letter != letter || force){
	    	
//    		Log.i(TAG, " + letter: "+(char)letter);

    		/** Update cache item */
    		mAlphabetNavItem[cacheIndex].letter = letter;
    		if(letter < 'a'-1 || letter > 'z') // 24?????
    		{
        		Log.i(TAG, " + letter failed: "+(char)letter);
    			mAlphabetNavItem[cacheIndex].letterBitmap.eraseColor(Color.argb(127, 122, 122, 0));
    		} 
    		else 
    		{
	    		if(!mNavItemUtils.fillAlphabetBitmap(
	    				mAlphabetNavItem[cacheIndex], 
	    				mBitmapWidth, 
	    				mBitmapHeight))
	    		{
	        		Log.i(TAG, " + letter failed to create bitmap: "+(char)letter);
	        		if(!mAlphabetNavItem[cacheIndex].letterBitmap.isRecycled())
	        			mAlphabetNavItem[cacheIndex].letterBitmap.eraseColor(Color.argb(127, 122, 122, 0));
	
	    		}
    		}
    		
//    		Log.i(TAG, "cacheIndex: "+cacheIndex+"");
    		
	    	/** bind new texture */
    		bindTexture(gl, mAlphabetNavItem[cacheIndex].letterBitmap, mTextureAlphabetId[cacheIndex]);
    		
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

        gl.glTexParameterf(
        		GL10.GL_TEXTURE_2D,
                GL10.GL_TEXTURE_MAG_FILTER,
//        		GL10.GL_LINEAR_MIPMAP_LINEAR);
        		GL10.GL_LINEAR);
        
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
//        		GL10.GL_MODULATE);
//        		GL10.GL_DECAL);
        		GL10.GL_MODULATE);
//        		GL10.GL_BLEND);
//                GL10.GL_REPLACE);

//        gl.glColor4f(1.0f,1.0f,1.0f,0.5f);
        
        gl.glBlendFunc(
        		GL10.GL_SRC_ALPHA, 
        		GL10.GL_ONE);
        


        if(bitmap != null)
        {
        	GLUtils.texImage2D(
        			GL10.GL_TEXTURE_2D, 
        			0, 
        			bitmap, 
        			0);
        }	
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
    float CUBE_MIN_SCROLL;
    float CUBE_SMOOTH;
    float CUBE_MAX_SCROLL;
    private boolean updatePosition(boolean force){
    	CUBE_MIN_SCROLL = 0.5f * Constants.MIN_SCROLL;
    	CUBE_SMOOTH = 1.2f * Constants.SCROLL_SPEED_SMOOTHNESS;
    	CUBE_MAX_SCROLL = 0.9f * Constants.MAX_SCROLL;
    	
    	/** time independence */
    	itvlFromLastRender = 
    		Math.min(
    				System.currentTimeMillis() - pTimestamp,
    				100) // 100 ms is the biggest 'jump' we allow
    		*
    		.001;
    	
    	/** save state **/
		pTimestamp = System.currentTimeMillis();
	
    	/** are we starting a movement -- yes? forget about the past */
    	if(updateFraction > 0 && updateFraction < .05f)
	    	updateFraction = 
	    		Constants.CPU_SMOOTHNESS * itvlFromLastRender
		    		+
		    		(1-Constants.CPU_SMOOTHNESS) * updateFraction;
    	else
    		updateFraction = itvlFromLastRender;
    	    	
    	/** 
    	 * New X pivot 
    	 */
    	if(mTargetPositionX > mPositionX)
			mPositionX +=
				Math.min(
					Math.min(
						Math.max(
								updateFraction * 
									CUBE_SMOOTH * (mTargetPositionX-mPositionX), 
								updateFraction * 
									CUBE_MIN_SCROLL)
						, mTargetPositionX-mPositionX)
					, updateFraction * CUBE_MAX_SCROLL);
		else if(mTargetPositionX < mPositionX)
			mPositionX	 += 
				Math.max(
					Math.max(
						Math.min(
							updateFraction * 
								CUBE_SMOOTH * (mTargetPositionX-mPositionX), 
							updateFraction * 
								-CUBE_MIN_SCROLL)
						, mTargetPositionX-mPositionX)
					, updateFraction * -CUBE_MAX_SCROLL);
		/**
		 * Finished scrolling X
		 */
		else if(mPositionX != 0 && mTargetPositionX % 1 == 0){
//			Log.i(TAG, "cleaning up after X rotation");
			
			mPositionY = findAlbumPositionAfterAlphabeticalScroll();
			mTargetPositionY = mPositionY;
			
			mPositionX = 0;
			mTargetPositionX = 0;
			
			lastInitial = -1;
			// need some more cleanup
		}
    	/**
    	 * Hmmm, double rotation -- strange -- FIXME
    	 */
    	if(mTargetPositionY != mPositionY &&
    		mTargetPositionX != 0)
    	{
    		lastInitial = -1;
    	}
		
//    	Log.i(TAG, "mTargetPosition: "+(mTargetPositionX % 1));
    	

    	/** optimization calculation*/
		flooredPositionX = (int) Math.floor(mPositionX);
		flooredPositionY = (int) Math.floor(mPositionY);
		
		positionOffsetX = mPositionX - flooredPositionX;
		positionOffsetY = mPositionY - flooredPositionY;
		

    	
   		/** 
   		 * New Y pivot
   		 */
    	// avoid rotations from a very far away point 
    	if(mCursor != null &&
    		mPositionY >= mCursor.getCount() - 1 + Constants.MAX_POSITION_OVERSHOOT + 2)
    		mPositionY = mCursor.getCount() - 1;

    	//		position += speedFactor * speed;
    	if(mTargetPositionY > mPositionY)
			mPositionY +=
				Math.min(
					Math.min(
						Math.max(
								updateFraction
									* CUBE_SMOOTH * (mTargetPositionY-mPositionY), 
								updateFraction 
									* CUBE_MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * CUBE_MAX_SCROLL);
		else if(mTargetPositionY < mPositionY)
			mPositionY	 += 
				Math.max(
					Math.max(
						Math.min(
							updateFraction
								* CUBE_SMOOTH * (mTargetPositionY-mPositionY), 
							updateFraction 
								* -CUBE_MIN_SCROLL)
						, mTargetPositionY-mPositionY)
					, updateFraction * -CUBE_MAX_SCROLL);

		/** are we outside the limits of the album list?*/
    	if(mCursor != null){
    		/** X checks */
    		if(lastInitial != -1 &&
    			lastInitial + mPositionX <= 'a' - 1 - Constants.MAX_POSITION_OVERSHOOT)
    		{
//    			Log.i(TAG, "lastInitial: "+(char)lastInitial+" mPositionX: "+mPositionX+" current: "+(char)(lastInitial + mPositionX));
	    		mTargetPositionX = 'a' - 1 - lastInitial;
    		}
	    	else if(lastInitial + mPositionX >= 'z' + Constants.MAX_POSITION_OVERSHOOT)
	    		mTargetPositionX = 'z' - lastInitial;
    		// TODO: are we done?
    		
    		/** Y checks */
    		if(mPositionY <= -Constants.MAX_POSITION_OVERSHOOT)
	    		mTargetPositionY = 0;
	    	else if(mPositionY >= mCursor.getCount() - 1 + Constants.MAX_POSITION_OVERSHOOT)
	    		mTargetPositionY = mCursor.getCount() - 1;
    	}
    	
		
		return true;
    }
    
    /**
     * 
     * @param cat
     * @param c
     * @param cBuf
     * @return
     */
    char	oTmpC;
    private char getFirstLetterLowerCaseAbstractFromCategory(int cat, Cursor c, CharArrayBuffer cBuf)
    {
    	try{
	    	switch(mBrowseCat)
			{
			case Constants.BROWSECAT_ALBUM:
				if(mPreferArtistSorting)
				{
					c.copyStringToBuffer(
							c.getColumnIndex(MediaStore.Audio.Albums.ARTIST),
							cBuf);
					oTmpC = cBuf.data[0];
				}
				else
				{
					c.copyStringToBuffer(
							c.getColumnIndex(MediaStore.Audio.Albums.ALBUM),
							cBuf);
					oTmpC = this.filterTheFunnyStuff(cBuf);
				}
				break;
			case Constants.BROWSECAT_ARTIST:
				c.copyStringToBuffer(
						c.getColumnIndex(MediaStore.Audio.Artists.ARTIST),
						cBuf);
				oTmpC = 
					this.filterTheFunnyStuff(cBuf);	    						
				break;
			}
	    	/*
			 * make it lower case
			 */
			if(oTmpC >= 'A' && oTmpC <= 'Z')
				oTmpC += 32;
			
			/*
			 * if it is outside the alphabet 
			 * 	make it the '0' of the alphabet
			 */
			if(oTmpC < 'a')
				oTmpC = 'a' - 1;
			if(oTmpC > 'z')
				oTmpC = 'z';
			
			return oTmpC;
    	} catch(CursorIndexOutOfBoundsException e) {
    		e.printStackTrace();
    		return 'a';
    	}
    }
    
    /* optimization */
    char lastLetter;
    char newLetter;
    int	 letterIdx;
    /**
     * returns the index of the album cursor after alphabetical scroll
     * @return
     */
    private int findAlbumPositionAfterAlphabeticalScroll(){
    	if(mCursor != null &&
    		(int)mPositionY >= 0 && 
    		(int)mPositionY < mCursor.getCount())
    	{
        	try
        	{
        		/*
        		 * Supposedly the initial position of the rotation
        		 */
		    	mCursor.moveToPosition((int)mPositionY);
		    	
		    	/*
		    	 * Find the initial position
		    	 */
		    	lastLetter = getFirstLetterLowerCaseAbstractFromCategory(
		    			mBrowseCat, 
		    			mCursor, 
		    			oCharArrayBuffer);
		    	
		    	/*
		    	 * Calculate the final letter
		    	 */
		    	newLetter = (char) (lastLetter + mPositionX);
		    	
		    	/*
		    	 * Search for the new cursor index
		    	 */
		    	if(mPositionX > 0){
		    		for(letterIdx = (int)mPositionY; letterIdx<mCursor.getCount(); letterIdx++){
		    			mCursor.moveToPosition(letterIdx);
		    			if(getFirstLetterLowerCaseAbstractFromCategory(
				    			mBrowseCat, 
				    			mCursor, 
				    			oCharArrayBuffer)
		    				>= newLetter)
		    			{
		    				break;
		    			}
		    		}
		    	} else {
		    		for(letterIdx = (int)mPositionY; letterIdx>=0; letterIdx--){
		    			mCursor.moveToPosition(letterIdx);
		    			if(getFirstLetterLowerCaseAbstractFromCategory(
				    			mBrowseCat, 
				    			mCursor, 
				    			oCharArrayBuffer)
		    				<= newLetter)
		    			{
		    				break;
		    			}
		    		}
		    	}
		    	
		    	/*
		    	 * Return
		    	 */
		    	return letterIdx;
	    	}
	    	catch(StaleDataException e)
	    	{
	    		// cursor must be changing
	    		e.printStackTrace();
	    		return 'a';
	    	}
	    	catch(NullPointerException e)
	    	{
	    		// cursor must be changing
	    		e.printStackTrace();
	    		return 'a';
	    	}
    	} else {
    		return 'a';
    	}

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
			mTargetPositionY = mPositionY - px/(mHeight*.75f);
			/* make we dont exceed the cube limits */
			if(mTargetPositionY <= -Constants.MAX_POSITION_OVERSHOOT)
				mTargetPositionY = -Constants.MAX_POSITION_OVERSHOOT;
			else if(mTargetPositionY >= mCursor.getCount() - 1 + Constants.MAX_POSITION_OVERSHOOT)
				mTargetPositionY = mCursor.getCount() - 1 + Constants.MAX_POSITION_OVERSHOOT;
			/* update position */
			mPositionY = mTargetPositionY;
    		return;
    	case Constants.SCROLL_MODE_HORIZONTAL:
    		mTargetPositionX = mPositionX - px/(mWidth*.9f);
			mPositionX = mTargetPositionX;
    		return;
    	}
    }
    
    /** inertial scroll on touch end */
    public void	inertialScrollOnTouchEnd(float scrollSpeed, int direction)
    {
    	switch(direction)
    	{
    	case Constants.SCROLL_MODE_VERTICAL:
    		/* make the movement harder for lower rotations */
    		if(Math.abs(scrollSpeed/(mHeight*.6f)) < Constants.MAX_LOW_SPEED)
    		{
    			mTargetPositionY = 
    				Math.round(
    						mPositionY
    						+
    						0.5f * Math.signum(scrollSpeed/(mHeight*.5f)) // needs to be .5f because of the rounding...
    				);
    		} 
    		/* full speed ahead */
    		else
    		{
    			mTargetPositionY = 
    				Math.round(
    						mPositionY
    						+
    						1.33f * Constants.SCROLL_SPEED_BOOST
    						*
    						scrollSpeed/(mHeight*.5f)
    				);
    		}
    		
    		/* limit move to X covers */
    		if(Math.abs(mTargetPositionY - mPositionY) > 12)
    			mTargetPositionY = 
    				Math.round(mPositionY) +
    				Math.signum(scrollSpeed) * 12;
    		
    		/* small optimization to avoid weird moves on the edges */
    		if(mTargetPositionY == -1)
    			mTargetPositionY = -2;
    		else if(mTargetPositionY == getAlbumCount())
    			mTargetPositionY = getAlbumCount() + 1;		
    		return;
    	case Constants.SCROLL_MODE_HORIZONTAL:
			mTargetPositionX = Math.round(
					mPositionX
					-
					(2.0f * Constants.SCROLL_SPEED_BOOST * scrollSpeed/(mWidth*.9f)));
			/* small optimization to avoid weird moves on the edges */
		//	if(mRenderer.mTargetPositionX == -1)
		//		mRenderer.mTargetPositionX = -2;
		//	else if(mRenderer.mTargetPositionX == 24)
		//		mRenderer.mTargetPositionX = 24 + 1;
    		return;
    	}
    }
    
    
    /** is the cube spinning */
    boolean isSpinning(){
    	// TODO: also check X scrolling
    	if(mTargetPositionY != mPositionY ||
    		mTargetPositionX != mPositionX)
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
    	// TODO: also check X scrolling
    	if(mTargetPositionX != mPositionX)
    	{
    		return true;
    	} 
    	else 
    	{
    		return false;
    	}
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
    	if(mTargetPositionY > mPositionY)
    		return (int) (mPositionY + 1);
    	else
    		return (int) mPositionY;
    }
    
    /** get the current Album Id */
    int getShownElementId(float x, float y){
    	try{
	    	if(mTargetPositionY != mPositionY ||
	    		mCursor == null ||
	    		mCursor.isClosed() ||
				/**
				 * FIXME: this is a quick cursor overflow bugfix, unverified
				 */
	    		(int) mPositionY > mCursor.getCount() - 1 ||
	    		(int) mPositionY < 0)
	    	{
	//    		Log.i(TAG, "Target was not reached yet: "+mTargetPosition+" - "+mPosition);
	    		return -1;
	    	}
	    	else{
	    		int tmpIndex = mCursor.getPosition();
	    		mCursor.moveToPosition((int) mPositionY);
	    		int id = -1;
	    		if(mBrowseCat == Constants.BROWSECAT_ALBUM)
	    		{
		    		id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Albums._ID));
	    		}
	    		else if(mBrowseCat == Constants.BROWSECAT_ARTIST)
	    		{
	    			id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Artists._ID));
	    		}
	    		mCursor.moveToPosition(tmpIndex);
	    		return id;
	    	}
    	} catch(CursorIndexOutOfBoundsException e) {
    		e.printStackTrace();
    		return -1;
    	}
    }
    
    /** get the current Album Id */
    int getElementId(int position){
    	try{
	    	if(mCursor == null ||
	    		mCursor.isClosed() ||
				/**
				 * FIXME: this is a quick cursor overflow bugfix, unverified
				 */
	    		position > mCursor.getCount() - 1 ||
	    		position < 0)
	    	{
	//    		Log.i(TAG, "Target was not reached yet: "+mTargetPosition+" - "+mPosition);
	    		return -1;
	    	}
	    	else{
	    		int tmpIndex = mCursor.getPosition();
	    		mCursor.moveToPosition(position);
	    		int id = -1;
	    		if(mBrowseCat == Constants.BROWSECAT_ALBUM)
	    		{
		    		id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Albums._ID));
	    		}
	    		else if(mBrowseCat == Constants.BROWSECAT_ARTIST)
	    		{
	    			id = mCursor.getInt(
		    				mCursor.getColumnIndexOrThrow(
		    						MediaStore.Audio.Artists._ID));
	    		}
	    		mCursor.moveToPosition(tmpIndex);
	    		return id;
	    	}
    	} catch(CursorIndexOutOfBoundsException e) {
    		e.printStackTrace();
    		return -1;
    	}
    }

    /** get the current Album Name */
    String getShownAlbumName(float x, float y){
    	if(mTargetPositionY != mPositionY)
    		return null;
    	else{
    		int tmpIndex = mCursor.getPosition();
    		mCursor.moveToPosition((int) mPositionY);
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
    		mCursor.moveToPosition((int) mPositionY);
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
		    		try
		    		{
			    		mCursor.moveToPosition(i);
			    		if(mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)) == albumId){
			    			mTargetPositionY = i;
			    			mPositionY = 
			    				i - 
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
    
    /** move navigator to the specified artist Id */
    synchronized int setCurrentByArtistId(long artistId)
    {
    	if(artistId >= 0)
    	{
	    	if(mCursor != null){
		    	for(int i = 0; i < mCursor.getCount(); i++){
		    		try
		    		{
			    		mCursor.moveToPosition(i);
			    		if(mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)) == artistId){
			    			mTargetPositionY = i;
			    			mPositionY = 
			    				i - 
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
		    		catch(IllegalStateException e) {
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
    		// can only change this after the GL vars have been set
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
    	for(int i=0; i<mAlphabetNavItem.length; i++)
    	{
    		try
    		{
    			mAlphabetNavItem[i].letterBitmap.recycle();
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
//    private int					mBrowseCat;
    private int					mTheme;
    private	int					mCacheSize = 4;
    private Context 			mContext;
//    private Handler				mRequestRenderHandler; // part of the abstract class
    private RockOnCube 			mRockOnCube;
    private AlbumLabelGlText	mAlbumLabelGlText;
    private int[] 				mTextureId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private int[] 				mTextureLabelId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private int[] 				mTextureAlphabetId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
    private	int					mScrollMode = Constants.SCROLL_MODE_VERTICAL;
    public	boolean				mClickAnimation = false;
//    private	Cursor				mCursor = null;
    private AlphabetNavItem[]	mAlphabetNavItem = new AlphabetNavItem[mCacheSize];
    private NavItem[]			mNavItem = new NavItem[mCacheSize];
    private NavItemUtils		mNavItemUtils;
    private ArtistAlbumHelper[]	mArtistAlbumHelper;
    private	int					mBitmapWidth;
    private int 				mBitmapHeight;
    private byte[]				mColorComponentBuffer;
    private	boolean				mForceTextureUpdate = false;
    private int					mHeight = 0;
    private int					mWidth = 0;
    private boolean				mIsRendering = false;
    private boolean				mIsChangingCat = false;
    
//    public	float		mPositionX = 0.f;
//    public	float		mTargetPositionX = 0.f;
//    public	float		mPositionY = 0.f;
//    public	float		mTargetPositionY = -1.f;
    private float		mEyeX = 0.75f;
    private float		mEyeY = -2.f;
    private float		mEyeZ = -5.f;
    private float		mEyeTargetX = 0.75f;
    private float		mEyeTargetY = 0.f;
    private float		mEyeTargetZ = -5.f;
    
    
    // GL Extensions support
    private boolean		mSupportMipmapGeneration;
    
    // Fake 3D
    private boolean		mRightEye = false;
    
    
    /** 
     * optimization vars 
     */
    private double 			pTimestamp;
    private int				flooredPositionX;
    private int				flooredPositionY;
    private float			positionOffsetX;
    private float			positionOffsetY;
    private float			rotationAngleX;
    private float			rotationAngleY;
    private boolean			texturesUpdated;
    private double			updateFraction;
    private int				tmpTextureIdx;
    private CharArrayBuffer	oCharArrayBuffer = new CharArrayBuffer(100);
}





class RockOnCube {
	
	private final String TAG = "RockOnCube";
	
    public RockOnCube(int[] textureId, int[] textureAlphabetId) {

    	/** 
    	 * Save the texture ids
    	 */
    	mTextureId = textureId;
    	mTextureIdHorizontal = textureAlphabetId;
    	
    	/** 
    	 * Our cube coordinate declaration
    	 * we have 4 sets of 4 faces *only*
    	 * 
    	 * @ each instant only one of the sets
    	 *  is being drawn
    	 * when transitioning, 2 sets may be drawn
    	 */ 
    	ArrayList<float[]> faceCoordsArray = new ArrayList<float[]>(4);
    	ArrayList<float[]> faceNormArray = new ArrayList<float[]>(4);
//    	for(int i=0; i<mCubeFaces; i++){
//    		faceCoordsArray.add(new ArrayList<float[]>(mCubeFaces));
//    	}
    	
    	/**
    	 * 1ST FACE SET
    	 */
    	// FACE 0
        float[] coords0 = {
        		// X, Y, Z
        		-1.f, 1.f, -1.f,
        		1.f, 1.f, -1.f, 
        		1.f, -1.f, -1.f,
        		-1.f, -1.f, -1.f
        };
        faceCoordsArray.add(coords0);
        float[] norm0 = {
        	0.f, 0.f, -1.f	
        };
        faceNormArray.add(norm0);
        
        // FACE 1
        float[] coords1 = {
        		// X, Y, Z
        		-1.f, 1.f, 1.f,
        		1.f, 1.f, 1.f,
        		1.f, 1.f, -1.f,
        		-1.f, 1.f, -1.f
        };
        faceCoordsArray.add(coords1);
        float[] norm1 = {
            	0.f, 1.f, 0.f	
            };
        faceNormArray.add(norm1);
            
        // FACE 2
        float[] coords2 = {
                // X, Y, Z
        		-1.f, -1.f, 1.f,
        		1.f, -1.f, 1.f,
        		1.f, 1.f, 1.f,
        		-1.f, 1.f, 1.f
        };
        faceCoordsArray.add(coords2);
        float[] norm2 = {
            	0.f, 0.f, 1.f	
            };
        faceNormArray.add(norm2);
            
        
        // FACE 3
        float[] coords3 = {
        		// X, Y, Z
        		-1.f, -1.f, -1.f,
        		1.f, -1.f, -1.f,
        		1.f, -1.f, 1.f,
        		-1.f, -1.f, 1.f
        };
        faceCoordsArray.add(coords3);
        float[] norm3 = {
            	0.f, -1.f, 0.f	
            };
        faceNormArray.add(norm3);
        
        /**
         * Alphabet faces
         */
        float[] coordsAlphabet = {
        		// 1ST SET OF 4
        		// 1 - X, Y, Z
        		-1,1,-1,
        		1,1,-1,
        		1,-1,-1,
        		-1,-1,-1,
        		// 2 - X, Y, Z
        		1,1,-1,
        		1,1,1,
        		1,-1,1,
        		1,-1,-1,
        		// 3 - X, Y, Z
        		1,1,1,
        		-1,1,1,
        		-1,-1,1,
        		1,-1,1,
        		// 4 - X, Y, Z
        		-1,1,1,
        		-1,1,-1,
        		-1,-1,-1,
        		-1,-1,1,
        		// 2ND SET OF 4
        		// 1 - X, Y, Z
        		-1,1,1,
        		1,1,1,
        		1,1,-1,
        		-1,1,-1,
        		// 2 - X, Y, Z
        		1,1,1,
        		1,-1,1,
        		1,-1,-1,
        		1,1,-1,
        		// 3 - X, Y, Z
        		1,-1,1,
        		-1,-1,1,
        		-1,-1,-1,
        		1,-1,-1,
        		// 4 - X, Y, Z
        		-1,-1,1,
        		-1,1,1,
        		-1,1,-1,
        		-1,-1,-1,
        		// 3RD SET OF 4
        		// 1 - X, Y, Z
        		-1,-1,1,
        		1,-1,1,
        		1,1,1,
        		-1,1,1,
        		// 2 - X, Y, Z
        		1,-1,1,
        		1,-1,-1,
        		1,1,-1,
        		1,1,1,
        		// 3 - X, Y, Z
        		1,-1,-1,
        		-1,-1,-1,
        		-1,1,-1,
        		1,1,-1,
        		// 4 - X, Y, Z
        		-1,-1,-1,
        		-1,-1,1,
        		-1,1,1,
        		-1,1,-1,
        		// 4TH SET OF 4
        		// 1 - X, Y, Z
        		-1,-1,-1,
        		1,-1,-1,
        		1,-1,1,
        		-1,-1,1,
        		// 2 - X, Y, Z
        		1,-1,-1,
        		1,1,-1,
        		1,1,1,
        		1,-1,1,
        		// 3 - X, Y, Z
        		1,1,-1,
        		-1,1,-1,
        		-1,1,1,
        		1,1,1,
        		// 4 - X, Y, Z
        		-1,1,-1,
        		-1,-1,-1,
        		-1,-1,1,
        		-1,1,1,
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
//        for(int l = 0; l < mCubeFaces; l++){
    	for(int k = 0; k < mCubeFaces; k++){
	        // Buffers to be passed to gl*Pointer() functions
	        // must be direct, i.e., they must be placed on the
	        // native heap where the garbage collector cannot
	        // move them.
	        //
	        // Buffers with multi-byte datatypes (e.g., short, int, float)
	        // must have their byte order set to native order
	
	        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4); // verts * ncoords * bytes per vert??
	        vbb.order(ByteOrder.nativeOrder());
	        mFVertexBuffer[k] = vbb.asFloatBuffer();
	
	        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
	        tbb.order(ByteOrder.nativeOrder());
	        mTexBuffer[k] = tbb.asFloatBuffer();
	        
	        ByteBuffer nbb = ByteBuffer.allocateDirect(3 * 4);
	        nbb.order(ByteOrder.nativeOrder());
	        mNormalBuffer[k] = nbb.asFloatBuffer();
	        
	        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
	        ibb.order(ByteOrder.nativeOrder());
	        mIndexBuffer[k] = ibb.asShortBuffer();
	
	        float[] coords = faceCoordsArray.get(k);
	        
	        for (int i = 0; i < VERTS; i++) {
	            for(int j = 0; j < 3; j++) {
	            	mFVertexBuffer[k].put(coords[i*3+j]);
	            }
	        }
	        
	        for(int i = 0; i < 3; i++)
	        	mNormalBuffer[k].put(faceNormArray.get(k)[i]);
	        
	        mTexBuffer[k].put(textCoords);
	        
	
	        for(int i = 0; i < VERTS; i++) {
	            mIndexBuffer[k].put((short) i);
	        }
	
	        mFVertexBuffer[k].position(0);
	        mTexBuffer[k].position(0);
	        mNormalBuffer[k].position(0);
	        mIndexBuffer[k].position(0);
    	}

    	   
    	/**
    	 * Generate our openGL buffers with the 
    	 * vertice and texture coordinates 
    	 * and drawing indexes
    	 * HORIZONTAL 
    	 */
        for(int l = 0; l < mCubeFaces; l++){
			for(int k = 0; k < mCubeFaces; k++){
		        // Buffers to be passed to gl*Pointer() functions
		        // must be direct, i.e., they must be placed on the
		        // native heap where the garbage collector cannot
		        // move them.
		        //
		        // Buffers with multi-byte datatypes (e.g., short, int, float)
		        // must have their byte order set to native order
		
		        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4); // verts * ncoords * bytes per vert??
		        vbb.order(ByteOrder.nativeOrder());
		        mFVertexBufferHorizontal[l][k] = vbb.asFloatBuffer();
		
		        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
		        tbb.order(ByteOrder.nativeOrder());
		        mTexBufferHorizontal[l][k] = tbb.asFloatBuffer();
		
		        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
		        ibb.order(ByteOrder.nativeOrder());
		        mIndexBufferHorizontal[l][k] = ibb.asShortBuffer();
	
		        for (int i = 0; i < VERTS; i++) {
		            for(int j = 0; j < 3; j++) {
//		            	mFVertexBufferHorizontal[l][k].put(coords[i*3+j]);
		            	mFVertexBufferHorizontal[l][k].put(coordsAlphabet[l*VERTS*3*mCubeFaces+k*VERTS*3+i*3+j]);
		            }
		        }
		
		        
		//        for (int i = 0; i < VERTS; i++) {
		//            for(int j = 0; j < 2; j++) {
		//                mTexBuffer[k].put(coords[i*3+j]);
		//            }
		//        }
		        mTexBufferHorizontal[l][k].put(textCoords);
		        
		
		        for(int i = 0; i < VERTS; i++) {
		            mIndexBufferHorizontal[l][k].put((short) i);
		        }
		
		        mFVertexBufferHorizontal[l][k].position(0);
		        mTexBufferHorizontal[l][k].position(0);
		        mIndexBufferHorizontal[l][k].position(0);
			}
	    }
	}

    /* optimization */
//    int	x;
    int y;
    public void draw(GL10 gl) {
    	
    	/**
    	 * Vertical scrolling, only draw covers
    	 */
//    	if(horizontalIndexFloat < 1 && horizontalIndexFloat > -1){
    	if(horizontalIndexFloat == 0){
    		   	for(int i = 0; i < mTextureId.length; i++){
		    		gl.glActiveTexture(GL10.GL_TEXTURE0);
			        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId[i]);
			        gl.glTexParameterx(
			        		GL10.GL_TEXTURE_2D, 
			        		GL10.GL_TEXTURE_WRAP_S,
			                GL10.GL_REPEAT);
			        gl.glTexParameterx(
			        		GL10.GL_TEXTURE_2D, 
			        		GL10.GL_TEXTURE_WRAP_T,
			                GL10.GL_REPEAT);
			    	
			        gl.glFrontFace(GL10.GL_CCW);
			        gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer[i]);
			        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer[i]);
			        gl.glEnable(GL10.GL_TEXTURE_2D);
			        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer[i]);
			        gl.glDrawElements(
			//        		GL10.GL_TRIANGLE_STRIP,
			        		GL10.GL_TRIANGLE_FAN,
			//        		GL10.GL_LINE_LOOP,
			        		VERTS,
			                GL10.GL_UNSIGNED_SHORT,
			                mIndexBuffer[i]);
	    	}
    	}
    	/**
    	 * Horizontal scrolling - draw alphabetical indexes if needed
    	 */
    	y = verticalIndex % mCubeFaces;
    	if(y<0)
    		y=0;
    	if(horizontalIndexFloat != 0){ // should be a floating point otherwise we only get indexes after one face has rotated
    		for(int i = 0; i < 4; i++){
//	    		Log.i(TAG, "drawing alphabet "+i);
	    		gl.glActiveTexture(GL10.GL_TEXTURE0);
		        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIdHorizontal[i]);
		        gl.glTexParameterx(
		        		GL10.GL_TEXTURE_2D, 
		        		GL10.GL_TEXTURE_WRAP_S,
		                GL10.GL_REPEAT);
		        gl.glTexParameterx(
		        		GL10.GL_TEXTURE_2D, 
		        		GL10.GL_TEXTURE_WRAP_T,
		                GL10.GL_REPEAT);
		    	
		        gl.glFrontFace(GL10.GL_CCW);
//		        gl.glNormalPointer(type, stride, pointer);
		        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBufferHorizontal[y][i]);
		        gl.glEnable(GL10.GL_TEXTURE_2D);
		        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBufferHorizontal[y][i]);
		        gl.glDrawElements(
		//        		GL10.GL_TRIANGLE_STRIP,
		        		GL10.GL_TRIANGLE_FAN,
		//        		GL10.GL_LINE_LOOP,
		        		VERTS,
		                GL10.GL_UNSIGNED_SHORT,
		                mIndexBufferHorizontal[y][i]);
	    	}
//	    	}
    	}
    }

    public void setHorizontalIndex(int idx, float idxF){
//    	horizontalIndex = idx;
    	horizontalIndexFloat = idxF;
    }
    
    public void setVerticalIndex(int idx, float idxF){
    	verticalIndex = idx;
//    	verticalIndexFloat = idxF;
    }
    
    private final static int 	VERTS = 4;
	private final int 			mCubeFaces = 4;
	private final int 			pointsPerFace = 4;
//	private int					horizontalIndex = 0;
	private float				horizontalIndexFloat = 0;
	private int					verticalIndex = 0;
//	private float				verticalIndexFloat = 0;
    
	/* vertical scrolling buffers */
	private FloatBuffer mFVertexBuffer[] = new FloatBuffer[mCubeFaces];
    private FloatBuffer mTexBuffer[] = new FloatBuffer[mCubeFaces];
    private FloatBuffer mNormalBuffer[] = new FloatBuffer[mCubeFaces];
    private ShortBuffer mIndexBuffer[] = new ShortBuffer[mCubeFaces];
    
    /* horizontal scrolling buffers */
    private FloatBuffer mFVertexBufferHorizontal[][] = new FloatBuffer[mCubeFaces][mCubeFaces];
    private FloatBuffer mTexBufferHorizontal[][] = new FloatBuffer[mCubeFaces][mCubeFaces];
    private ShortBuffer mIndexBufferHorizontal[][] = new ShortBuffer[mCubeFaces][mCubeFaces];
    
    /** our 4 vertical face textures */
    public	int[] mTextureId;
    
    /** our 4 horizontal face textures */
    public	int[] mTextureIdHorizontal;
    
}

class AlbumLabelGlText{
	
	public void setTexture(int textureId){
		mTextureId = textureId;
	}
	
    public AlbumLabelGlText(int textureId) {

    	/** 
    	 * Save the texture ids
    	 */
    	mTextureId = textureId;
    	
    	/** 
    	 * Our cube coordinate declaration
    	 */ 
    	ArrayList<float[]> faceCoordsArray = new ArrayList<float[]>(mCubeFaces);
    	
    	// FACE 0
        float[] coords0 = {
        		// X, Y, Z
        		-1.f, .25f, 0.f,
        		1.f, .25f, 0.f,
        		1.f, -.25f, 0.f, 
        		-1.f, -.25f, 0.f
        };
        faceCoordsArray.add(coords0);
        
//        // FACE 1
//        float[] coords1 = {
//        		// X, Y, Z
//        		-1.f, 1.1f, -.5f,
//        		1.f, 1.1f, -5f,
//        		1.f, 1.1f, -1.f,
//        		-1.f, 1.1f, -1.f
//        };
//        faceCoordsArray.add(coords1);
//        
//        // FACE 2
//        float[] coords2 = {
//                // X, Y, Z
//        		-1.f, .5f, 1.1f,
//        		1.f, .5f, 1.1f,
//        		1.f, 1.f, 1.1f,
//        		-1.f, 1.f, 1.1f
//        };
//        faceCoordsArray.add(coords2);
//        
//        // FACE 3
//        float[] coords3 = {
//        		// X, Y, Z
//        		-1.f, -1.1f, .5f,
//        		1.f, -1.1f, .5f,
//         		1.f, -1.1f, 1.f,
//        		-1.f, -1.1f, 1.f
//        };
//        faceCoordsArray.add(coords3);
//        
        // General texture coords
        float[] textCoords = {
	        		0.f, 1.f,
	        		1.f, 1.f,
	        		1.f, 0.f,
	        		0.f, 0.f
        };
        
//    	/**
//    	 * Generate our openGL buffers with the 
//    	 * vertice and texture coordinates 
//    	 * and drawing indexes
//    	 */
//    	for(int k = 0; k < mCubeFaces; k++){
//	        // Buffers to be passed to gl*Pointer() functions
//	        // must be direct, i.e., they must be placed on the
//	        // native heap where the garbage collector cannot
//	        // move them.
//	        //
//	        // Buffers with multi-byte datatypes (e.g., short, int, float)
//	        // must have their byte order set to native order
	
	        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4); // verts * ncoords * bytes per vert??
	        vbb.order(ByteOrder.nativeOrder());
	        mFVertexBuffer = vbb.asFloatBuffer();
	
	        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
	        tbb.order(ByteOrder.nativeOrder());
	        mTexBuffer = tbb.asFloatBuffer();
	
	        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
	        ibb.order(ByteOrder.nativeOrder());
	        mIndexBuffer = ibb.asShortBuffer();
	
	        float[] coords = faceCoordsArray.get(0);
	        
	        for (int i = 0; i < VERTS; i++) {
	            for(int j = 0; j < 3; j++) {
	            	mFVertexBuffer.put(coords[i*3+j]);
	            }
	        }
	
	        
//	        for (int i = 0; i < VERTS; i++) {
//	            for(int j = 0; j < 2; j++) {
//	                mTexBuffer[k].put(coords[i*3+j]);
//	            }
//	        }
	        mTexBuffer.put(textCoords);
	        
	
	        for(int i = 0; i < VERTS; i++) {
	            mIndexBuffer.put((short) i);
	        }
	
	        mFVertexBuffer.position(0);
	        mTexBuffer.position(0);
	        mIndexBuffer.position(0);
//    	}
    }

    public void draw(GL10 gl) {
    	
//    	for(int i = 0; i < mTextureId.length; i++){
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
//    	}
    }

    private final static int VERTS = 4;
	private final int mCubeFaces = 4;
	private final int pointsPerFace = 4;
    
    private FloatBuffer mFVertexBuffer;
    private FloatBuffer mTexBuffer;
    private ShortBuffer mIndexBuffer;
    
    /** our texture id */
    public	int mTextureId;
}
