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

//public abstract class RockOnRenderer implements GLSurfaceView.Renderer{
public abstract class RockOnRenderer{

	final String TAG = this.toString();
	
	abstract public void renderNow();
	
//    public RockOnRenderer(Context context, Handler requestRenderHandler) {
//        mContext = context;
//        mRequestRenderHandler = requestRenderHandler;
//        
//    	initNonGlVars(context, false);
//    }
    
    abstract public void changePlaylist(int playlistId);
    
//    abstract public void initNonGlVars(Context context, boolean force);

    abstract public	Cursor getAlbumCursor();
    
    abstract public int getAlbumCount();
    
    abstract public int getItemDimension();
//    {
//    	return mAlbumCursor;
//    }
//    
//    public int[] getConfigSpec() {
////        if (mTranslucentBackground) {
//                // We want a depth buffer and an alpha buffer
//                int[] configSpec = {
//                        EGL10.EGL_RED_SIZE,      8,
//                        EGL10.EGL_GREEN_SIZE,    8,
//                        EGL10.EGL_BLUE_SIZE,     8,
//                        EGL10.EGL_ALPHA_SIZE,    8,
//                        EGL10.EGL_DEPTH_SIZE,   16,
//                        EGL10.EGL_NONE
//                };
//                return configSpec;
////            } else {
////                // We want a depth buffer, don't care about the
////                // details of the color buffer.
////                int[] configSpec = {
////                        EGL10.EGL_DEPTH_SIZE,   16,
////                        EGL10.EGL_NONE
////                };
////                return configSpec;
////            }
//    }
    
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
////    	Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
//
//    	
//        /*
//         * By default, OpenGL enables features that improve quality
//         * but reduce performance. One might want to tweak that
//         * especially on software renderer.
//         */
//        gl.glDisable(GL10.GL_DITHER);
//
//        /*
//         * Some one-time OpenGL initialization can be made here
//         * probably based on features of this particular context
//         */
//        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
//                GL10.GL_FASTEST);
//
//        gl.glClearColor(0.f, 0.f, 0.f, 0);
////        gl.glClearColor(.5f, .5f, .5f, 1);
//        gl.glShadeModel(GL10.GL_SMOOTH);
//        gl.glEnable(GL10.GL_DEPTH_TEST);
//        gl.glEnable(GL10.GL_TEXTURE_2D);
//
//        /*
//         * Create our textures. This has to be done each time the
//         * surface is created.
//         */
//        // album covers - vertical
//        gl.glGenTextures(mTextureId.length, mTextureId, 0);
//        // album labels
//        gl.glGenTextures(mTextureLabelId.length, mTextureLabelId, 0);
//        // album labels
//        gl.glGenTextures(mTextureAlphabetId.length, mTextureAlphabetId, 0);
//
//        mRockOnCube = new RockOnCube(mTextureId, mTextureAlphabetId);
//        mAlbumLabelGlText = new AlbumLabelGlText(mTextureLabelId[0]);
//        
//        
//        /*
//         * By default, OpenGL enables features that improve quality
//         * but reduce performance. One might want to tweak that
//         * especially on software renderer.
//         */
//        gl.glDisable(GL10.GL_DITHER);
//        gl.glEnable(GL10.GL_DEPTH_TEST);
////        gl.glEnable(GL10.GL_LINE_SMOOTH);
////        gl.glEnable(GL10.GL_LINE_SMOOTH_HINT);
////        gl.glEnable(GL10.GL_BLEND);
////        gl.glBlendFunc(
////        		GL10.GL_ONE, 
////        		GL10.GL_ONE_MINUS_SRC_ALPHA);
////        		GL10.GL_ONE_MINUS_SRC_ALPHA);
////        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
//        
//        gl.glTexEnvx(
//        		GL10.GL_TEXTURE_ENV, 
//        		GL10.GL_TEXTURE_ENV_MODE,
//                GL10.GL_MODULATE);
//
//        /** FOG */
//        gl.glEnable(GL10.GL_FOG);
//        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_LINEAR);
////        gl.glFogx(GL10.GL_FOG_MODE, GL10.GL_EXP); // GL_EXP2 doesnt show anything
//        gl.glFogf(GL10.GL_FOG_START, 4.5f);
//        gl.glFogf(GL10.GL_FOG_END, 5.5f);
////        float[] fogColor = {.5f,.5f,.5f, 1.f};
////        gl.glFogfv(GL10.GL_FOG_COLOR, FloatBuffer.wrap(fogColor));
//        gl.glHint(GL10.GL_FOG_HINT, GL10.GL_NICEST);
//    }
//
//    public void onDrawFrame(GL10 gl) {
//    	  
//    	
//    	/** Calculate new position */
//    	if(!updatePosition(false)){
//    		
//    	} 
//  
//    	mRockOnCube.setHorizontalIndex((int) mPositionX, mPositionX);
//    	mRockOnCube.setVerticalIndex((int) mPositionY, mPositionY);
//    	
//        /*
//         * Usually, the first thing one might want to do is to clear
//         * the screen. The most efficient way of doing this is to use
//         * glClear().
//         */
//
//        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//
//        /*
//         * Now we're ready to draw some 3D objects
//         */
//
//        gl.glMatrixMode(GL10.GL_MODELVIEW);
//        gl.glLoadIdentity();
//
//        /* 
//         * Check if we are doing a click animation
//         * 	- it changes the eye spot of the camera
//         */
//        if(mClickAnimation){
//        	/* camera eye update */
//        	mEyeX += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetX-mEyeX);
//        	mEyeY += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetY-mEyeY);
//        	mEyeZ += updateFraction * Constants.SCROLL_SPEED_SMOOTHNESS * (mEyeTargetZ-mEyeZ);
//        	/* minimum movements */
//        	if(Math.abs(mEyeTargetX - mEyeX) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction < Constants.MIN_SCROLL)
//        		mEyeX +=  
//        			Math.signum(mEyeTargetX - mEyeX) * 
//        				Math.min(
//        					Math.abs(mEyeTargetX - mEyeX),
//        					Constants.MIN_SCROLL);
//        	if(Math.abs(mEyeTargetY - mEyeY) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction < Constants.MIN_SCROLL)
//        		mEyeY += 
//        			Math.signum(mEyeTargetY - mEyeY) * 
//        				Math.min(
//        						Math.abs(mEyeTargetY - mEyeY),
//        						Constants.MIN_SCROLL);
//        	if(Math.abs(mEyeTargetZ - mEyeZ) * Constants.SCROLL_SPEED_SMOOTHNESS * updateFraction < Constants.MIN_SCROLL)
//        		mEyeZ += 
//        			Math.signum(mEyeTargetZ - mEyeZ) * 
//        				Math.min(
//        						Math.abs(mEyeTargetZ - mEyeZ),
//        						Constants.MIN_SCROLL);
//        	/* end of animation */
////        	Log.i(TAG, "X: "+mEyeX+" - "+mEyeTargetX);
////        	Log.i(TAG, "Y: "+mEyeY+" - "+mEyeTargetY);
////        	Log.i(TAG, "Z: "+mEyeZ+" - "+mEyeTargetZ);
//        	if(mEyeX == mEyeTargetX && mEyeY == mEyeTargetY && mEyeZ == mEyeTargetZ)
//        		mClickAnimation = false;
//        }
//        GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, 0f, 0f, 0f, 0f, -1.0f, 0.0f);
//
//        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
//        
//        /* Center (or at least compensate) in the X axis */
//        gl.glTranslatef(.1f, 0.f, 0.f);
//        
//        /* Calculate rotations */
//        if(mPositionX != 0)
//	        rotationAngleX = 
//	        	((flooredPositionX % mCacheSize) + positionOffsetX)
//	        	/
//	        	4.f
//	        	*
//	        	360.f;
//        else
//        	rotationAngleX = 0;
//        
//        rotationAngleY = 
//        	((flooredPositionY % mCacheSize) + positionOffsetY)
//        	/
//        	(float) mCacheSize
//        	*
//        	360.f;
//                
//        if(mScrollMode == Constants.SCROLL_MODE_VERTICAL){
//        	gl.glRotatef(rotationAngleX, 0.f, 1.f, 0.f);
//        	gl.glRotatef(-rotationAngleY, 1.f, 0.f, 0.f);
//        }
//        
//        /* update textures if needed -- whenever we cross one album */
//        texturesUpdated = updateTextures(gl);
//                
//    	/* 
//    	 * ??
//    	 */
////        if(texturesUpdated)
////	        gl.glTexEnvx(
////	        		GL10.GL_TEXTURE_ENV, 
////	        		GL10.GL_TEXTURE_ENV_MODE,
////	                GL10.GL_MODULATE);	        
//        
////        if(!texturesUpdated){	// avoids a strange flickering...
////        	mRockOnCube.draw(gl);
//        
//        /* draw cube */
//    	mRockOnCube.draw(gl);
//    	
//    	/* draw label */
//    	if(Math.abs(mPositionY - mTargetPositionY) < .5f &&
//    		mPositionX > -1.f &&
//    		mPositionX < 1.f)
//    	{
//        	gl.glLoadIdentity();
//            GLU.gluLookAt(gl, mEyeX, mEyeY, mEyeZ, 0f, 0f, 0f, 0f, -1.0f, 0.0f);
//            gl.glTranslatef(.1f, -1.55f, -1.f);
//            // X rotation
//            gl.glRotatef(rotationAngleX, 0.f, 1.f, 0.f);
//            // Y rotation
//	        gl.glRotatef((.5f - Math.abs(positionOffsetY-0.5f))*2*90.f, 1.f, 0.f, 0.f);
//        	if((int)Math.round(mPositionY%mTextureId.length) >= mTextureId.length || 
//            	(int)Math.round(mPositionY%mTextureId.length) < 0)
//            	mAlbumLabelGlText.setTexture(mTextureLabelId[0]);
//            else
//            	mAlbumLabelGlText.setTexture(mTextureLabelId[(int)Math.round(mPositionY%mTextureId.length)]);
//        	mAlbumLabelGlText.draw(gl);
////        	}
//        }
//        if(mTargetPositionX != mPositionX ||
//        	mPositionX != 0 ||
//        	mTargetPositionY != mPositionY || 
//        	mClickAnimation ||
//        	texturesUpdated)
//        	renderNow();
//    }

//    public void onSurfaceChanged(GL10 gl, int w, int h) {
//        gl.glViewport(0, 0, w, h);
//
//        /*
//        * Set our projection matrix. This doesn't have to be done
//        * each time we draw, but usually a new projection needs to
//        * be set when the viewport is resized.
//        */
//
//        float ratio = (float) w / h;
//        gl.glMatrixMode(GL10.GL_PROJECTION);
//        gl.glLoadIdentity();
//        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 20);
//
//    }

    abstract void showClickAnimation();
    
    abstract void reverseClickAnimation();
    
    abstract void forceTextureUpdateOnNextDraw();
    
//    int		albumIndexTmp;
//    int		alphabetIndexTmp;
//    int		lastInitial = -1;
//    int		charTmp;
//    boolean changed;
//    private boolean updateTextures(GL10 gl){
//    	changed = false;
//    	if(mAlbumCursor != null){
//	    	/* Album Cover textures in vertical scrolling */
//    		for(int i = 0; i < mCacheSize; i++){
//	    		albumIndexTmp = (int) (Math.floor((float) flooredPositionY / (float) mCacheSize) * mCacheSize + i);
//	    		if(albumIndexTmp - flooredPositionY > 2){
//	    			albumIndexTmp -= mCacheSize;
//	    		} else if (albumIndexTmp - flooredPositionY < -1){
//	    			albumIndexTmp += mCacheSize;
//	    		}
//	    		
//	//    		Log.i(TAG, 
////	    			"albumIndexTmp: "+albumIndexTmp+
////	    			" flooredPosition: "+flooredPosition+
////	    			" mPosition: "+mPosition);
//	    		
//	    		if(setupAlbumTextures(gl, i, albumIndexTmp, mForceTextureUpdate))
//	    			changed = true;
//	    	}
//	    	
//    		/* Alphabetical textures in horizontal scrolling of the cube */
//    		if(mPositionX != 0){
//    			for(int i=0; i<mTextureAlphabetId.length; i++){    	    		
//    				if(lastInitial == -1){
//    					mAlbumCursor.moveToPosition(flooredPositionY);
//    					lastInitial = 
//    						mAlbumCursor.
//    							getString(
//    									mAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).
//    							toLowerCase().
//    								charAt(0);
//    					if(lastInitial < 'a')
//    						lastInitial = 'a' - 1;
//    				}
//    				
////    				Log.i(TAG, "flooredX: "+flooredPositionX);
////    				Log.i(TAG, "lastInitial: "+(char)lastInitial);
////    				Log.i(TAG, "flooredPositionX: "+flooredPositionX+" (int)/mCacheSize: "+(int)((flooredPositionX)/mCacheSize));
////    				Log.i(TAG, "flooredPositionX: "+flooredPositionX+" %mCacheSize: "+(flooredPositionX)%mCacheSize);
//    				
//    				charTmp = 
//    					lastInitial + 
//    					(int)((flooredPositionX)/mCacheSize)*mCacheSize + 
//    					i;
//	    				
//	    			/**
//	    			 * Transition special cases
//	    			 */
//	    			/* cache turn point when going forward in the alphabet */
//    				if(flooredPositionX%mCacheSize == mCacheSize - 1 &&
//	    				i == 0)
//	    			{
//	    				charTmp += mCacheSize;
//	    			}
//	    			
//	    			/* negative scrolling (going back in the alphabet) */
//	    			if(flooredPositionX%mCacheSize < 0 &&
//	    				i != 0)
//	    			{
//	    				charTmp -= mCacheSize;
//	    			}
//
//
//    	    		if(setupAlphabetTextures(gl, i, charTmp, mForceTextureUpdate))
//    	    			changed = true;
//    	    		
//    	    		/* DEBUG CODE */
////    				undefined.eraseColor(Color.CYAN);
////    	    		bindTexture(gl, undefined, mTextureAlphabetId[i]);
//    			}
//    		}
//	    	
//	    	if(mForceTextureUpdate)
//	    		mForceTextureUpdate = false;
//    	}
//    	return changed;
//    }
    
//    Bitmap undefined = Bitmap.createBitmap(
//			Constants.REASONABLE_ALBUM_ART_SIZE, 
//			Constants.REASONABLE_ALBUM_ART_SIZE, 
//			Bitmap.Config.RGB_565);
    
//    private boolean setupAlbumTextures(GL10 gl, int cacheIndex, int albumIndex, boolean force){
//    	/** texture needs update? */
//    	if(mAlbumNavItem[cacheIndex].index != albumIndex || force){
//	    	
////    		Log.i(TAG, "albumIndexTmp: "+albumIndexTmp+" flooredPosition: "+flooredPosition+" mPosition: "+mPosition);
//
//    		/** Update cache item */
//    		mAlbumNavItem[cacheIndex].index = albumIndex;
//    		if(albumIndex < 0 || albumIndex >= mAlbumCursor.getCount())
//    		{
//    			mAlbumNavItem[cacheIndex].albumName = "";
//    			mAlbumNavItem[cacheIndex].artistName = "";
//    			mAlbumNavItem[cacheIndex].cover = undefined;
//    			mAlbumNavItem[cacheIndex].cover.eraseColor(Color.argb(127, 122, 122, 0));
//    			// we cannot change the bitmap reference of the item
//    			// we need to write to the existing reference
//    			mAlbumNavItemUtils.fillAlbumLabel(
//    					mAlbumNavItem[cacheIndex],
//    					mBitmapWidth, 
//    					mBitmapHeight/4);
//    		} 
//    		else 
//    		{
//	    		if(!mAlbumNavItemUtils.fillAlbumInfo(
//	    				mAlbumCursor, 
//	    				mAlbumNavItem[cacheIndex], 
//	    				albumIndex))
//	    		{
//	    			mAlbumNavItem[cacheIndex].albumName = null;
//	    			mAlbumNavItem[cacheIndex].artistName = null;
//	    			mAlbumNavItem[cacheIndex].albumKey = null;
//	    		}
//	    		if(!mAlbumNavItemUtils.fillAlbumBitmap(
//	    				mAlbumNavItem[cacheIndex], 
//	    				mBitmapWidth, 
//	    				mBitmapHeight, 
//	    				mColorComponentBuffer))
//	    		{
//	//    			mAlbumNavItem[cacheIndex].cover = null;
//	//    			mAlbumNavItem[cacheIndex].cover = 
//	//    				Bitmap.createBitmap(
//	//    						mBitmapWidth, 
//	//    						mBitmapHeight, 
//	//    						Bitmap.Config.RGB_565);
//	    			mAlbumNavItem[cacheIndex].cover = undefined;
//	    			mAlbumNavItem[cacheIndex].cover.eraseColor(Color.argb(127, 0, 255, 0));
//	
//	    		}
//	    		if(Math.abs(mTargetPositionY - mPositionY) < 3 ||
//	    				mPositionY < 3){ // avoid unnecessary processing
//		    		if(!mAlbumNavItemUtils.fillAlbumLabel(
//		    				mAlbumNavItem[cacheIndex],
//		    				mBitmapWidth,
//		    				mBitmapHeight/4))
//		    		{
//	//	    			mAlbumNavItem[cacheIndex].label = undefined;
//	//	    			mAlbumNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
//		    		}
//	    		}
//    		}
//    		
////    		Log.i(TAG, "cacheIndex: "+cacheIndex+"");
//    		
//	    	/** bind new texture */
//    		bindTexture(gl, mAlbumNavItem[cacheIndex].cover, mTextureId[cacheIndex]);
//    		bindTexture(gl, mAlbumNavItem[cacheIndex].label, mTextureLabelId[cacheIndex]);
//    		
//    		return true;
//    	} else  {
//    		return false;
//    	}
//    	
//    }
    
//    private boolean setupAlphabetTextures(GL10 gl, int cacheIndex, int letter, boolean force){
////		Log.i(TAG, "letter: "+(char)letter);
//
//    	/** precheck albumNavItem */
//    	if(mAlphabetNavItem[cacheIndex].letter != letter || force){
//	    	
////    		Log.i(TAG, " + letter: "+(char)letter);
//
//    		/** Update cache item */
//    		mAlphabetNavItem[cacheIndex].letter = letter;
//    		if(letter < 'a'-1 || letter > 'z') // 24?????
//    		{
//        		Log.i(TAG, " + letter failed: "+(char)letter);
//    			mAlphabetNavItem[cacheIndex].letterBitmap = undefined;
//    			mAlphabetNavItem[cacheIndex].letterBitmap.eraseColor(Color.argb(127, 122, 122, 0));
//    		} 
//    		else 
//    		{
//	    		if(!mAlbumNavItemUtils.fillAlphabetBitmap(
//	    				mAlphabetNavItem[cacheIndex], 
//	    				mBitmapWidth, 
//	    				mBitmapHeight))
//	    		{
//	        		Log.i(TAG, " + letter failed to create bitmap: "+(char)letter);
//	    			mAlphabetNavItem[cacheIndex].letterBitmap = undefined;
//	    			mAlphabetNavItem[cacheIndex].letterBitmap.eraseColor(Color.argb(127, 122, 122, 0));
//	
//	    		}
////	    		if(Math.abs(mTargetPositionY - mPositionY) < 3 ||
////	    				mPositionY < 3){ // avoid unnecessary processing
////		    		if(!mAlbumNavItemUtils.fillAlbumLabel(
////		    				mAlbumNavItem[cacheIndex],
////		    				mBitmapWidth,
////		    				mBitmapHeight/4))
////		    		{
////	//	    			mAlbumNavItem[cacheIndex].label = undefined;
////	//	    			mAlbumNavItem[cacheIndex].label.eraseColor(Color.argb(0, 0, 0, 0));
////		    		}
////	    		}
//    		}
//    		
////    		Log.i(TAG, "cacheIndex: "+cacheIndex+"");
//    		
//	    	/** bind new texture */
//    		bindTexture(gl, mAlphabetNavItem[cacheIndex].letterBitmap, mTextureAlphabetId[cacheIndex]);
//    		
//    		return true;
//    	} else  {
//    		return false;
//    	}
//    	
//    }
    
//    private void bindTexture(GL10 gl, Bitmap bitmap, int textureId){
//    	/** bind new texture */
//        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
//    	
//        gl.glTexParameterf(
//        		GL10.GL_TEXTURE_2D, 
//        		GL10.GL_TEXTURE_MIN_FILTER,
//                GL10.GL_LINEAR);
//        gl.glTexParameterf(
//        		GL10.GL_TEXTURE_2D,
//                GL10.GL_TEXTURE_MAG_FILTER,
//                GL10.GL_LINEAR);
//
//        gl.glTexParameterf(
//        		GL10.GL_TEXTURE_2D, 
//        		GL10.GL_TEXTURE_WRAP_S,
//                GL10.GL_CLAMP_TO_EDGE);
//        gl.glTexParameterf(
//        		GL10.GL_TEXTURE_2D, 
//        		GL10.GL_TEXTURE_WRAP_T,
//                GL10.GL_CLAMP_TO_EDGE);
//        
////        gl.glTexParameterf(
////        		GL10.GL_TEXTURE_2D, 
////        		GL10.GL_TEXTURE_COLOR_BORDER,
////                GL10.GL_NICEST);
//
//        gl.glTexEnvf(
//        		GL10.GL_TEXTURE_ENV, 
//        		GL10.GL_TEXTURE_ENV_MODE,
//        		GL10.GL_MODULATE);
////        		GL10.GL_DECAL);
////        		GL10.GL_MODULATE);
////        		GL10.GL_BLEND);
////                GL10.GL_REPLACE);
//
//
//        if(bitmap != null)
//        	GLUtils.texImage2D(
//        			GL10.GL_TEXTURE_2D, 
//        			0, 
//        			bitmap, 
//        			0);
//
//    }
    
    abstract void	triggerPositionUpdate();
    
    abstract float	getPositionX();

    abstract float	getTargetPositionX();
    
    abstract float	getPositionY();
    
    abstract float	getTargetPositionY();
    
    abstract void	saveRotationInitialPosition();

//    private boolean updatePosition(boolean force){
//    	
////    	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//    	
////    	updateFraction = 
////    		Math.min(
////    			(System.currentTimeMillis() - pTimestamp)/Constants.FRAME_DURATION_STD,
////    			Constants.FRAME_JUMP_MAX);
//    	updateFraction = .5;
//    	
////        Log.i(TAG, " + "+ (System.currentTimeMillis() - pTimestamp));
//
////    	Log.i(TAG, "framesPerSec: "+ 1000/(System.currentTimeMillis() - pTimestamp));
//    	
//    	/** 
//    	 * New X pivot 
//    	 */
//    	if(mTargetPositionX > mPositionX)
//			mPositionX +=
//				Math.min(
//					Math.min(
//						Math.max(
//								updateFraction * 
//									Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionX-mPositionX), 
//								updateFraction * 
//									Constants.MIN_SCROLL)
//						, mTargetPositionX-mPositionX)
//					, Constants.MAX_SCROLL);
//		else if(mTargetPositionX < mPositionX)
//			mPositionX	 += 
//				Math.max(
//					Math.max(
//						Math.min(
//							updateFraction * 
//								Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionX-mPositionX), 
//							updateFraction * 
//								-Constants.MIN_SCROLL)
//						, mTargetPositionX-mPositionX)
//					, -Constants.MAX_SCROLL);
//		/**
//		 * Finished scrolling X
//		 */
//		else if(mPositionX != 0 && mTargetPositionX % 1 == 0){
////			Log.i(TAG, "cleaning up after X rotation");
//			
//			mPositionY = findAlbumPositionAfterAlphabeticalScroll();
//			mTargetPositionY = mPositionY;
//			
//			mPositionX = 0;
//			mTargetPositionX = 0;
//			
//			lastInitial = -1;
//			// need some more cleanup
//		}
//    	/**
//    	 * Hmmm, double rotation -- strange -- FIXME
//    	 */
//    	if(mTargetPositionY != mPositionY &&
//    		mTargetPositionX != 0)
//    	{
//    		lastInitial = -1;
//    	}
//		
////    	Log.i(TAG, "mTargetPosition: "+(mTargetPositionX % 1));
//    	
//    	
//   		/** 
//   		 * New Y pivot
//   		 */
////		position += speedFactor * speed;
//    	if(mTargetPositionY > mPositionY)
//			mPositionY +=
//				Math.min(
//					Math.min(
//						Math.max(
//								updateFraction
//									* Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionY-mPositionY), 
//								updateFraction 
//									* Constants.MIN_SCROLL)
//						, mTargetPositionY-mPositionY)
//					, Constants.MAX_SCROLL);
//		else if(mTargetPositionY < mPositionY)
//			mPositionY	 += 
//				Math.max(
//					Math.max(
//						Math.min(
//							updateFraction
//								* Constants.SCROLL_SPEED_SMOOTHNESS * (mTargetPositionY-mPositionY), 
//							updateFraction 
//								* -Constants.MIN_SCROLL)
//						, mTargetPositionY-mPositionY)
//					, -Constants.MAX_SCROLL);
//
//		
//    	
//		/** save state **/
//		pTimestamp = System.currentTimeMillis();
//		
//		/** optimization calculation*/
//		flooredPositionX = (int) Math.floor(mPositionX);
//		flooredPositionY = (int) Math.floor(mPositionY);
//		
//		positionOffsetX = mPositionX - flooredPositionX;
//		positionOffsetY = mPositionY - flooredPositionY;
//		
//		/** are we outside the limits of the album list?*/
//    	if(mAlbumCursor != null){
//    		/** X checks */
//    		if(lastInitial != -1 &&
//    			lastInitial + mPositionX <= 'a' - 1 - Constants.MAX_POSITION_OVERSHOOT)
//    		{
////    			Log.i(TAG, "lastInitial: "+(char)lastInitial+" mPositionX: "+mPositionX+" current: "+(char)(lastInitial + mPositionX));
//	    		mTargetPositionX = 'a' - 1 - lastInitial;
//    		}
//	    	else if(lastInitial + mPositionX >= 'z' + Constants.MAX_POSITION_OVERSHOOT)
//	    		mTargetPositionX = 'z' - lastInitial;
//    		// TODO: are we done?
//    		
//    		/** Y checks */
//    		if(mPositionY <= -Constants.MAX_POSITION_OVERSHOOT)
//	    		mTargetPositionY = 0;
//	    	else if(mPositionY >= mAlbumCursor.getCount() - 1 + Constants.MAX_POSITION_OVERSHOOT)
//	    		mTargetPositionY = mAlbumCursor.getCount() - 1;
//	    	
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
//	    	
//	    	
//	    	
//	    	
//    	}
//    	
//		
//		return true;
//    }
    
//    /* optimization */
//    char lastLetter;
//    char newLetter;
//    int	 letterIdx;
//    /**
//     * returns the index of the album cursor after alphabetical scroll
//     * @return
//     */
//    private int findAlbumPositionAfterAlphabeticalScroll(){
//    	if((int)mPositionY >= 0 && (int)mPositionY < mAlbumCursor.getCount())
//    	{
//	    	mAlbumCursor.moveToPosition((int)mPositionY);
//	    	lastLetter = 
//	    		mAlbumCursor.getString(
//	    				mAlbumCursor.getColumnIndexOrThrow(
//	    						MediaStore.Audio.Albums.ARTIST)).
//	    				toLowerCase().charAt(0);
//	    	newLetter = (char) (lastLetter + mPositionX);
//	    	if(mPositionX > 0){
//	    		for(letterIdx = (int)mPositionY; letterIdx<mAlbumCursor.getCount(); letterIdx++){
//	    			mAlbumCursor.moveToPosition(letterIdx);
//	    			if(mAlbumCursor.getString(
//	    					mAlbumCursor.getColumnIndexOrThrow(
//	    							MediaStore.Audio.Albums.ARTIST)).
//	    					toLowerCase().charAt(0)
//	    					>= newLetter)
//	    			{
//	    				break;
//	    			}
//	    		}
//	    	} else {
//	    		for(letterIdx = (int)mPositionY; letterIdx>=0; letterIdx--){
//	    			mAlbumCursor.moveToPosition(letterIdx);
//	    			if(mAlbumCursor.getString(
//	    					mAlbumCursor.getColumnIndexOrThrow(
//	    							MediaStore.Audio.Albums.ARTIST)).
//	    					toLowerCase().charAt(0)
//	    					<= newLetter)
//	    			{
//	    				break;
//	    			}
//	    		}
//	    	}
//	    	return letterIdx;
//    	} else {
//    		return 'a';
//    	}
//    }
    
    /** is the cube spinning */
    abstract boolean isSpinning();
    
    /** is the cube spinning */
    abstract boolean isSpinningX();
    
    /** is the cube spinning */
    abstract boolean isSpinningY();
    
    /** get album count */
//    abstract int	getAlbumCount();
    
    /** get the current position */
    abstract int getShownPosition();
    
    /** get the current Album Id */
    abstract int getShownAlbumId();
    
    /** get the current Album Name */
    abstract String getShownAlbumName();
    
    /** get the current Album Name */
    abstract String getShownAlbumArtistName();
    
    /** move navigator to the specified album Id */
    abstract int setCurrentByAlbumId(long albumId);
    
//    /**
//     * Class members
//     */
//    private	int					mCacheSize = 4;
//    private Context 			mContext;
//    private Handler				mRequestRenderHandler;
//    private RockOnCube 			mRockOnCube;
//    private AlbumLabelGlText	mAlbumLabelGlText;
//    private int[] 				mTextureId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
//    private int[] 				mTextureLabelId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
//    private int[] 				mTextureAlphabetId = new int[mCacheSize]; // the number of textures must be equal to the number of faces of our shape
//    private	int					mScrollMode = Constants.SCROLL_MODE_VERTICAL;
//    public	boolean				mClickAnimation = false;
//    private	Cursor				mAlbumCursor = null;
//    private AlphabetNavItem[]	mAlphabetNavItem = new AlphabetNavItem[mCacheSize];
//    private AlbumNavItem[]		mAlbumNavItem = new AlbumNavItem[mCacheSize];
//    private AlbumNavItemUtils	mAlbumNavItemUtils;
//    private	int					mBitmapWidth;
//    private int 				mBitmapHeight;
//    private byte[]				mColorComponentBuffer;
//    private	boolean				mForceTextureUpdate = false;
//
    public	float		mPositionX = 0.f;
    public	float		mTargetPositionX = 0.f;
    public	float		mPositionY = 0.f;
    public	float		mTargetPositionY = -1.f;
    public	float		mRotationInitialPositionY = 0.f;
//    private float		mEyeX = 0.75f;
//    private float		mEyeY = -2.f;
//    private float		mEyeZ = -5.f;
//    private float		mEyeTargetX = 0.75f;
//    private float		mEyeTargetY = 0.f;
//    private float		mEyeTargetZ = -5.f;
    
    
//    /** 
//     * optimization vars 
//     */
//    private double 	pTimestamp;
//    private int		flooredPositionX;
//    private int		flooredPositionY;
//    private float	positionOffsetX;
//    private float	positionOffsetY;
//    private float	rotationAngleX;
//    private float	rotationAngleY;
//    private boolean	texturesUpdated;
//    private double	updateFraction;
    

}


