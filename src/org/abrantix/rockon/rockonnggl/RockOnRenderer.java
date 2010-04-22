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
	Handler	mRequestRenderHandler;
	
	public void renderNow(){
		mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	}
	
	public void stopRender()
	{
		mRequestRenderHandler.sendEmptyMessage(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	abstract public void clearCache();
    
	abstract public void changeBrowseCat(int browseCat);
	
	abstract public int	 getBrowseCat();
	
    abstract public void changePlaylist(int playlistId);
    
    abstract public	Cursor getAlbumCursor();
    
    abstract public int getAlbumCount();
    
    abstract public int getItemDimension();

    abstract void showClickAnimation(float x, float y);
    
    abstract void reverseClickAnimation();
    
    abstract int  getClickActionDelay();
    
    abstract void forceTextureUpdateOnNextDraw();
    
    abstract void	triggerPositionUpdate();
    
    abstract float	getPositionX();

    abstract float	getTargetPositionX();
    
    abstract float	getPositionY();
    
    abstract float	getTargetPositionY();
    
    abstract void	saveRotationInitialPosition();
    
    /** stop scrolling on touch down */
    abstract void	stopScrollOnTouch();
    
    /** scroll on touch */
    abstract void 	scrollOnTouchMove(float px, int direction);
    
    /** trigger inertial scroll on touch end */
    abstract void	inertialScrollOnTouchEnd(float scrollSpeed, int direction);
    
    /** is the cube spinning */
    abstract boolean isSpinning();
    
    /** is the cube spinning */
    abstract boolean isSpinningX();
    
    /** is the cube spinning */
    abstract boolean isSpinningY();
    
    /** get album count */
//    abstract int	getAlbumCount();
    
    /** get the current position */
    abstract int getShownPosition(float x, float y);
    
    /** get the current Album/Artist/... Id */
    abstract int getShownElementId(float x, float y);
    
    /** get the current Album Name */
    abstract String getShownAlbumName(float x, float y);
    
    /** get the current Album Name */
    abstract String getShownAlbumArtistName(float x, float y);
    
    /** move navigator to the specified album Id */
    abstract int setCurrentByAlbumId(long albumId);

    /** move navigator to the specified artist Id */
    abstract int setCurrentByArtistId(long artistId);

    //
    public	float		mPositionX = 0.f;
    public	float		mTargetPositionX = 0.f;
    public	float		mPositionY = 0.f;
    public	float		mTargetPositionY = -1.f;
    public	float		mRotationInitialPositionY = 0.f;
    
    public	boolean		mIsChangingCat = false;

    

}


