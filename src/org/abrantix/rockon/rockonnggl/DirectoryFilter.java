package org.abrantix.rockon.rockonnggl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

public class DirectoryFilter {

	private static final String TAG = "DirectoryFilter";

	public final static int EXTERNAL_STORAGE = 0;
	public final static int INTERNAL_STORAGE = 1;
	
	static public String getInternalStorageRoot(Context ctx) {
		File f = new File("/emmc");
		if(f.exists())
			return f.getAbsolutePath();
		else {
			f = new File("/mnt/emmc");
			if(f.exists())
				return f.getAbsolutePath();
			else
				return ctx.getFilesDir().getParentFile().getParentFile().getParentFile().getAbsolutePath();
		}
	}
	
	static public boolean usesExternalStorage() {
		return (getStorageType() == EXTERNAL_STORAGE);
	}
	
	static public int getStorageType() {
		try{
			File f = new File(Constants.ROCKON_STORAGE_TYPE_FILENAME);
			BufferedReader br = new BufferedReader(new FileReader(f), 64);
			String type = br.readLine();
			if(type != null)
				return Integer.parseInt(type);
			else
				return EXTERNAL_STORAGE;
		} catch(Exception e) {
			e.printStackTrace();
			return EXTERNAL_STORAGE;
		}
	}
	
	static public String getStorageReadableSize(Context ctx, int type) {
		long bytes = getStorageSize(ctx, type);
		return formatStorageReadableSize(bytes);
	}
	
	static public long getStorageSize(Context ctx, int type) {
		String path = null;
		switch(type) {
		case EXTERNAL_STORAGE:
			path = Environment.getExternalStorageDirectory().getPath();
			break;
		case INTERNAL_STORAGE:
			path = getInternalStorageRoot(ctx);
			break;
		}
        StatFs stat = new StatFs(path);
        long bytesCount = (long)stat.getBlockSize() *(long)stat.getBlockCount();
        return bytesCount;
	}
	
	static public String getStorageReadableAvailable(Context ctx, int type) {
		long bytes = getStorageAvailable(ctx, type);
		return formatStorageReadableSize(bytes);
	}
	
	static public long getStorageAvailable(Context ctx, int type) {
		String path = null;
		switch(type) {
		case EXTERNAL_STORAGE:
			path = Environment.getExternalStorageDirectory().getPath();
			break;
		case INTERNAL_STORAGE:
			path = getInternalStorageRoot(ctx);
			break;
		}
        StatFs stat = new StatFs(path);
        long bytesCount = (long)stat.getBlockSize() *(long)stat.getAvailableBlocks();
        return bytesCount;
    }
	
	static public String formatStorageReadableSize(long bytes) {
		long divider;
		String unit;
		if(bytes > 1073741824) {
			divider = 1073741824;
			unit = "GB";	
		} else if(bytes > 1048576) {
			divider = 1048576;
			unit = "MB";
		} else {
			divider = 1024;
			unit = "KB";
		}
		if(bytes/divider >= 10) {
			return (int)(bytes/divider) + " " + unit;
		} else {
			return (int)((bytes*10)/divider)/10.f + " " + unit;
		}
	}
	
	static void setStorageType(int type) {
		try{
			File f = new File(Constants.ROCKON_STORAGE_TYPE_FILENAME);
			if(!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(f), 64);
			String typeString = String.valueOf(type);
			bw.write(typeString);
			bw.close();
		} catch(Exception e) {
			e.printStackTrace();	
		}
	}
	
	static public File[] getExternalDirectories() {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return null;
		} else {
			File[] dirs = Environment.getExternalStorageDirectory().listFiles(
					new FileFilter() {
						
						@Override
						public boolean accept(File pathname) {
							if(pathname.isDirectory())
								return true;
							else
								return false;
						}
					});
			Arrays.sort(dirs, mFileNameComparator);
			return dirs;
		}
	}
	
	static public File[] getInternalDirectories(Context ctx) {
		File internalRoot = new File(getInternalStorageRoot(ctx));
		File[] dirs = internalRoot.listFiles(
				new FileFilter() {
					
					@Override
					public boolean accept(File pathname) {
						if(pathname.isDirectory() && !pathname.getName().equals("mnt") && !pathname.getName().equals("sdcard"))
							return true;
						else
							return false;
					}
				});
		if(dirs != null)
			Arrays.sort(dirs, mFileNameComparator);
		return dirs;
	}
			
	static public String getFolderSqlStatement(Context ctx, int storageType) {
////		String stmt = PreferenceManager.getDefaultSharedPreferences(
////				ctx.getApplicationContext()).getString(
////				ctx.getString(R.string.preference_key_folder), null);
		String[] stmtArray = getSelectedDirNames(storageType);
		if(stmtArray != null) {
			String stmt = "";
			for (int i = 0; i < stmtArray.length; i++) {
				if (i > 0) {
					stmt += " OR ";
				}
				if(storageType == EXTERNAL_STORAGE) {
					stmt += (MediaStore.Audio.Media.DATA
							+ " like \"" //'%"
							+ Environment.getExternalStorageDirectory().getAbsolutePath() 
							+ "/" + stmtArray[i] + "%\"");
				} else {
					stmt += (MediaStore.Audio.Media.DATA
							+ " like \"" //'%"
							+ getInternalStorageRoot(ctx) 
							+ "/" + stmtArray[i] + "%\"");
				}
//				Log.i(TAG, i + " --- " + stmt);
			}
			return stmt;
		} else {
			return null;
		}
	}

	static public String[] getSelectedDirNames(int storageType) {
		String dirsString = getDirectoriesString(storageType);
		if(dirsString == null || dirsString.equals(""))
			return null;
		else
			return dirsString.split(";");
	}
	
	static public String[] getSelectedExternalDirNames() {
		String directories = getDirectoriesString(EXTERNAL_STORAGE);
		if(directories != null && !directories.equals("")) {
			String[] selected = directories.split(";");
			Arrays.sort(selected, mNameComparator);
			return selected;
		} else {
			return null;
		}
	}
	
	static public String[] getSelectedInternalDirNames() {
		String directories = getDirectoriesString(INTERNAL_STORAGE);
		if(directories != null && !directories.equals("")) {
			String[] selected = directories.split(";");
			Arrays.sort(selected, mNameComparator);
			return selected;
		} else {
			return null;
		}
	}
	
	static private String getDirectoriesString(int type) {
		try {
			File f = null;
			switch(type) {
			case EXTERNAL_STORAGE:
				f = new File(Constants.ROCKON_EXTERNAL_DIRECTORIES_FILENAME);
				break;
			case INTERNAL_STORAGE:
				f = new File(Constants.ROCKON_INTERNAL_DIRECTORIES_FILENAME);
				break;
			}
			Log.i(TAG, f.getAbsolutePath());
			BufferedReader br = new BufferedReader(new FileReader(f), 256);
			String dirs = br.readLine();
//			if(dirs != null)
//				Log.i(TAG, dirs);
			return dirs;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static public boolean saveDirectories(int storageType, String[] selected, boolean all) {
		try {
			StringBuilder dirs = new StringBuilder("");
			if(!all) {	
				for(int i=0; i<selected.length; i++) {
					if(i>0)
						dirs.append(";");
					dirs.append(selected[i]);
				}
			}
			Log.i(TAG, dirs.toString());
			File f = null;
			switch(storageType) {
			case EXTERNAL_STORAGE:
				f = new File(Constants.ROCKON_EXTERNAL_DIRECTORIES_FILENAME);
				break;
			case INTERNAL_STORAGE:
				f = new File(Constants.ROCKON_INTERNAL_DIRECTORIES_FILENAME);
				break;
			}
			Log.i(TAG, f.getAbsolutePath());
			if(!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(f), 256);
			bw.write(dirs.toString());
			bw.close();
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	final static public Comparator<File> mFileNameComparator = new Comparator<File>() {

		@Override
		public int compare(File object1, File object2) {
			return object1.getName().compareToIgnoreCase(object2.getName());
		}
	};
	
	final static public Comparator<String> mNameComparator = new Comparator<String>() {

		@Override
		public int compare(String object1, String object2) {
			return object1.compareToIgnoreCase(object2);
		}
	};

}
