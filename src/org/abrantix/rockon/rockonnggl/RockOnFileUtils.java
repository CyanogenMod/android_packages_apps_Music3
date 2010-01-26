package org.abrantix.rockon.rockonnggl;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

public class RockOnFileUtils{
	
	/**
	 * Constructor
	 */
	RockOnFileUtils(){
		
	}
	
	/**
	 * sanitize the filename
	 * @param fileName
	 * @return
	 */
	static String validateFileName(String fileName){
		if(fileName == null)
			return null;
		fileName = fileName.replace('/', '_');
		fileName = fileName.replace('<', '_');
		fileName = fileName.replace('>', '_');
		fileName = fileName.replace(':', '_');
		fileName = fileName.replace('\'', '_');
		fileName = fileName.replace('?', '_');
		fileName = fileName.replace('"', '_');
		fileName = fileName.replace('|', '_');
		fileName = fileName.replace('(', '_');
		fileName = fileName.replace(')', '_');
		fileName = fileName.replace('[', '_');
		fileName = fileName.replace(']', '_');
		fileName = fileName.replaceAll("%", "");
		return fileName;
	}
	
}