package org.abrantix.rockon.rockonnggl;

import java.io.File;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

public class ClearAlbumArtDialogPreference extends DialogPreference{

	private static final String TAG = null;

	public ClearAlbumArtDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ClearAlbumArtDialogPreference(Context context, AttributeSet attrs, int i) {
		super(context, attrs, i);
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		switch(which)
		{
		case DialogInterface.BUTTON_POSITIVE:
			clearAlbumArtDirectory();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			// do nothing
			break;
		}	
	}
	
	/**
	 * 
	 */
	public void clearAlbumArtDirectory()
	{
		File f = new File(Constants.ROCKON_ALBUM_ART_PATH);
		File[] fileList = f.listFiles();
		try
		{
			for(int i=0; i<fileList.length; i++)
			{
				if(fileList[i].getAbsolutePath() != Constants.ROCKON_SMALL_ALBUM_ART_PATH)
					fileList[i].delete();
//				Log.i(TAG, fileList[i].getAbsolutePath());
			}
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
		}
		
		
		f = new File(Constants.ROCKON_SMALL_ALBUM_ART_PATH);
		fileList = f.listFiles();
		try
		{
			for(int i=0; i<fileList.length; i++)
			{
				fileList[i].delete();
//				Log.i(TAG, fileList[i].getAbsolutePath());
			}
		}
		catch(NullPointerException e)
		{
			e.printStackTrace();
		}
	}

}
