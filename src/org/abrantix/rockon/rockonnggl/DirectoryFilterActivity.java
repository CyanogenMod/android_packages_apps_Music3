package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.abrantix.rockon.rockonnggl.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class DirectoryFilterActivity extends Activity{
	protected static final String TAG = "DirectoryFilterActivity";
	File[]				mAllDirs;
	ArrayList<String> 	mSelectedDirs;
	boolean		mUsesExternal = true;
	
	DirectoryFilterAdapter mDirectoryFilterAdapter;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.directory_filter_layout);
        
        attachListeners();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	loadContent();
    }
    
    public void attachListeners()
    {
    	findViewById(R.id.selectall_toggle).setOnClickListener(mGlobalClickListener);
    	findViewById(R.id.selectnone_toggle).setOnClickListener(mGlobalClickListener);
    }
    
    public void loadContent() {
    	mUsesExternal = DirectoryFilter.usesExternalStorage();
    	TextView tv = (TextView)findViewById(R.id.directory_filter_header_text);
    	if(mUsesExternal)
    		tv.setText(R.string.preference_title_storage_type_external);
    	else
    		tv.setText(R.string.preference_title_storage_type_internal);
    	
    	if(mUsesExternal) {
    		mAllDirs = DirectoryFilter.getExternalDirectories();
    		try {
    			// XXX - inefficient?
    			mSelectedDirs = new ArrayList<String>(Arrays.asList(DirectoryFilter.getSelectedExternalDirNames()));
    		} catch(NullPointerException e) {
    			e.printStackTrace();
    		}
    	} else {
    		mAllDirs = DirectoryFilter.getInternalDirectories(getApplicationContext());
    		try{
    			// XXX - inefficient?
    			mSelectedDirs = new ArrayList<String>(Arrays.asList(DirectoryFilter.getSelectedInternalDirNames()));
    		} catch(NullPointerException e) {
    			e.printStackTrace();
    		}
    	}
    	if(mSelectedDirs == null || mSelectedDirs.size() == 0) {
			fillAllDirsAsSelected();
		}
    	
    	mDirectoryFilterAdapter = new DirectoryFilterAdapter(getApplicationContext(), mAllDirs, mSelectedDirs);
    	ListView lv = (ListView)findViewById(R.id.directory_list);
    	lv.setCacheColorHint(Color.TRANSPARENT);
    	lv.setAdapter(mDirectoryFilterAdapter);
    	lv.setOnItemClickListener(mDirectoryClickListener);
    }
    
    private void fillAllDirsAsSelected() {
    	if(mSelectedDirs != null)
    		mSelectedDirs.clear();
    	else
    		mSelectedDirs = new ArrayList<String>();
    	if(mAllDirs != null) {
	    	for(File f : mAllDirs) {
	    		mSelectedDirs.add(f.getName());
	    	}
    	}
    }
    
//    public void reloadSelected() {
//    	mAllDirs = DirectoryFilter.getSdCardDirectories();
//    	mSelectedDirs = DirectoryFilter.getSelectedDirNames();
//    	
//    	mDirectoryFilterAdapter.setData(mAllDirs, mSelectedDirs);
//    	ListView lv = (ListView)findViewById(R.id.directory_list);
//    	lv.invalidateViews();
//    	
//    	//loadAllNoneButtons();
//    }
    
    private OnItemClickListener mDirectoryClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> arg0, 
				View arg1, 
				int pos,
				long arg3) 
		{
			Log.i(TAG, "CLICKED");
			File tappedFile = mAllDirs[pos];
			int idx = Collections.binarySearch(mSelectedDirs, tappedFile.getName(), DirectoryFilter.mNameComparator);
			if(idx >= 0) {
				mSelectedDirs.remove(idx);
			} else {
				mSelectedDirs.add(tappedFile.getName());
				Collections.sort(mSelectedDirs, DirectoryFilter.mNameComparator);
			}
			saveSelected();			
	    	ListView lv = (ListView)findViewById(R.id.directory_list);
	    	lv.invalidateViews();
		}
	};
    
	private void saveSelected() {
		String[] selected = new String[mSelectedDirs.size()]; 
		mSelectedDirs.toArray(selected);
		if(mUsesExternal)
			DirectoryFilter.saveDirectories(DirectoryFilter.EXTERNAL_STORAGE, selected, (mSelectedDirs.size() == mAllDirs.length));
		else
			DirectoryFilter.saveDirectories(DirectoryFilter.INTERNAL_STORAGE, selected, (mSelectedDirs.size() == mAllDirs.length));
	}
	
    private OnClickListener mGlobalClickListener= new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(v.equals(findViewById(R.id.selectall_toggle)))
			{
				fillAllDirsAsSelected();
				saveSelected();			
		    	ListView lv = (ListView)findViewById(R.id.directory_list);
		    	lv.invalidateViews();
			}
			else if(v.equals(findViewById(R.id.selectnone_toggle)))
			{
				mSelectedDirs.clear();
				saveSelected();	
		    	ListView lv = (ListView)findViewById(R.id.directory_list);
		    	lv.invalidateViews();
			}
		}
	};
}
