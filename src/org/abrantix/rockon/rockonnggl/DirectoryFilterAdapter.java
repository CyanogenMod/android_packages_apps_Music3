package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class DirectoryFilterAdapter extends BaseAdapter{
	Context 		mContext;
	File[] 			mAllDirs;
	List<String> 	mSelectedDirs;
		
	String 			oDirName;
	CheckedTextView oDirTextView;
	CheckBox		oDirCheckBox;
	
	public DirectoryFilterAdapter(Context ctx, File[] allDirs, List<String> selectedDirs) {
		mContext = ctx;
		mAllDirs = allDirs;
		mSelectedDirs = selectedDirs;
	}
	
	public void setData(File[] allDirs, List<String> selectedDirs) {
		mAllDirs = allDirs;
		mSelectedDirs = selectedDirs;
	}
	
	@Override
	public int getCount() {
		if(mAllDirs != null)
			return mAllDirs.length;
		else 
			return 0;
	}

	@Override
	public Object getItem(int arg0) {
		if(mAllDirs != null)
			return mAllDirs[arg0];
		else return null;
	}

	@Override
	public long getItemId(int arg0) {
//		return mAllDirs[arg0].getName().hashCode();
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup viewParent) {
		if(convertView == null) {
			convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_multiple_choice, null);
		}

		oDirTextView = (CheckedTextView)convertView.findViewById(android.R.id.text1);
//		oDirCheckBox = (CheckBox)((ViewGroup)convertView).getChildAt(2); 
//
		oDirName = mAllDirs[position].getName();
		oDirTextView.setText(oDirName);
		if(mSelectedDirs == null || Collections.binarySearch(mSelectedDirs, oDirName, DirectoryFilter.mNameComparator) >= 0 ) { 
			oDirTextView.setChecked(true);
//			oDirCheckBox.setChecked(true);
		} else {
			oDirTextView.setChecked(false);
//			oDirCheckBox.setChecked(false);
		}
		
		return convertView;
	}

	

}
