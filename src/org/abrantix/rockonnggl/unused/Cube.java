/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.abrantix.rockonnggl.unused;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

/**
 * A vertex shaded cube.
 */
class Cube
{
    public Cube()
    {
        int one = 0x10000;
        int vertices[] = {
                -one, -one, -one,
                one, -one, -one,
                one,  one, -one,
                -one,  one, -one,
                -one, -one,  one,
                one, -one,  one,
                one,  one,  one,
                -one,  one,  one,
        };

        int colors[] = {
                0,    0,    0,  one,
                one,    0,    0,  one,
                one,  one,    0,  one,
                0,  one,    0,  one,
                0,    0,  one,  one,
                one,    0,  one,  one,
                one,  one,  one,  one,
                0,  one,  one,  one,
        };

        int texVertices[] = {
        		0, 0,
        		0, 1,
        		1, 1,
        		1, 0, 
        		-one, -one, 
        		one, -one,
        		one, one, 
        		-one, one
        };
        
        byte indices[] = {
                0, 4, 5,    0, 5, 1,
                1, 5, 6,    1, 6, 2,
                2, 6, 7,    2, 7, 3,
                3, 7, 4,    3, 4, 0,
                4, 7, 6,    4, 6, 5,
                3, 0, 1,    3, 1, 2
        };

        // Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asIntBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
        cbb.order(ByteOrder.nativeOrder());
        mColorBuffer = cbb.asIntBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(texVertices.length*4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asIntBuffer();
        mTexBuffer.put(texVertices);
        mTexBuffer.position(0);
        
        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
                
    }
    
    Bitmap bm = null;
    public void loadTextures(GL10 gl){
    	
    	/** should be the cycle using the position */
    	
    	if(bm == null){
	    	FileInputStream albumCoverFileInputStream;
			try {
				albumCoverFileInputStream = new FileInputStream("/sdcard/albumthumbs/RockOn/small/Ingrid Michaelson - Be OK");
				bm = BitmapFactory.decodeStream(albumCoverFileInputStream);

				/** load texture */
				gl.glBindTexture(
						gl.GL_TEXTURE_2D, 
						99 // our texture ID
				);
				GLUtils.texImage2D(
							gl.GL_TEXTURE_2D, 
							0, // detail level
							bm, //bitmap
							0 // border
				);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

    	}
		
    }
    
    public void draw(GL10 gl)
    {
    	gl.glEnable(GL10.GL_TEXTURE_2D);
    	
    	loadTextures(gl);
    
    
    	
        gl.glFrontFace(gl.GL_CCW);
        gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);

        
        gl.glActiveTexture(GL10.GL_TEXTURE0);
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, 99);
    	gl.glTexCoordPointer(2, gl.GL_FIXED, 0, mTexBuffer);

//        gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
        gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE, mIndexBuffer);
    }

    public	float		position = 0;	
    private IntBuffer	mTexBuffer;
    private IntBuffer   mVertexBuffer;
    private IntBuffer   mColorBuffer;
    private ByteBuffer  mIndexBuffer;
}
