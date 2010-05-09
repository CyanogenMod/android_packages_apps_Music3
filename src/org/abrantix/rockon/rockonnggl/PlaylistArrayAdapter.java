package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;



public class PlaylistArrayAdapter extends ArrayAdapter{
	private final String		TAG = "PlaylistCursorAdapter";
	
	private ArrayList<Playlist> mPlaylistArray;
    private Context 			mContext;
    private	Handler				mPlaylistItemSelectedHandler;
    private	DialogInterface		mDialogInterface;
    private int					mLayoutId;
    public int					mViewWidth;
    public int					mPlaylistInitialPosition;
    private String[]			mFrom;
    private int[]				mTo;
    private int					mResultLimit;
    private int					mExtraResults;
    
    public PlaylistArrayAdapter(Context context, 
    							int layout, 
    							ArrayList<Playlist> playlistArray,
    							String[] from,
    							int[] to,
    							int	limit,	
    							int	extraResults,
    							Handler clickHandler) 
    {
    	super(context, layout, to[0], playlistArray);
        this.mPlaylistArray = playlistArray;
        this.mContext = context;
        this.mFrom = from;
        this.mTo = to;
        this.mLayoutId = layout;
        this.mPlaylistInitialPosition = 0; // TODO: something
        this.mResultLimit = limit;
        this.mExtraResults = extraResults;
        this.mPlaylistItemSelectedHandler = clickHandler;
    }

    public void setDialogInterface(DialogInterface dialogInterface){
    	this.mDialogInterface = dialogInterface;
    }
    
//    /* (non-Javadoc)
//     * This is where you actually create the item view of the list
//     */
//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//    	/* set click listener */
//    	view.setOnClickListener(mSongListSongClick);
//    	view.setOnLongClickListener(mSongListSongLongClick);
//    	
//    	if(cursor.getPosition() < mResultLimit)
//    	{
//    		view.setClickable(true);
//	    	for(int i=0; i<mFrom.length; i++){
//	    		TextView textField = (TextView)
//	    			view.findViewById(mTo[i]);
//	    		if(mFrom[i] == MediaStore.Audio.Media.DURATION){
//	    	    	try{
//	    	    		double duration = new Double (
//	    	    				cursor.getString(
//	    	    						cursor.getColumnIndex(
//	    	    								mFrom[i])));
//	    		    	double minutes = Math.floor(duration / 1000 / 60);
//	    		    	double seconds = duration / 1000 % 60;
//	    		    	if(seconds > 10)
//	    		    		textField.
//	    		    			setText(
//	    		    					String.valueOf((int)minutes)+
//	    		    					":"+
//	    		    					String.valueOf((int)seconds));
//	    		    	else
//	    		    		textField.
//	    		    			setText(
//	    		    					String.valueOf((int)minutes)+
//	    		    					":0"+
//	    		    					String.valueOf((int)seconds));
//	    	    	} catch (Exception e){
//	    	    		e.printStackTrace();
//	    	    		textField.setText("-:--");
//	    	    	}
//	    		} else {
//	    	    	textField.setText(cursor.getString(
//	    					cursor.getColumnIndex(
//	    							mFrom[i])));
//	    		}
//	    	}
//    	}
//    	else
//    	{
//    		view.setClickable(false);
//    		for(int i=0; i<mFrom.length; i++)
//    		{
//    			switch(i)
//    			{
//    			case 0:
//	    			((TextView)view.findViewById(mTo[0])).setText(
//	    					"+ " + mExtraResults + " " + mContext.getString(R.string.song_list_more_results));
//	    			break;
//    			case 1:
//    				((TextView)view.findViewById(mTo[1])).setText("");
//    				break;
//    			case 2:
//    				((TextView)view.findViewById(mTo[2])).setText("");
//    				break;
//    			}
//    		}
//    	}
//    }
    
    /* (non-Javadoc)
     * This is where you actually create the item view of the list
     */
    @Override
    public View getView(int position, View  convertView, ViewGroup  parent) {
    	if(convertView == null)
    	{
    		convertView = LayoutInflater.from(mContext).inflate(mLayoutId, null);
    	}
    	
    	/* set click listener */
    	convertView.setOnClickListener(mSongListSongClick);
    	convertView.setOnLongClickListener(mSongListSongLongClick);
    	
    	if(position < mResultLimit)
    	{
    		convertView.setClickable(true);
	    	for(int i=0; i<mFrom.length; i++){
	    		TextView textField = (TextView)
	    			convertView.findViewById(mTo[i]);
	    	    textField.setText(mPlaylistArray.get(position).get(mFrom[i]));
	    	}
    	}
    	else
    	{	
    		/**
    		 * should never happen
    		 */
    		convertView.setClickable(false);
	    	for(int i=0; i<mFrom.length; i++)
	    	{
	    		TextView textField = (TextView)
	    			convertView.findViewById(mTo[i]);
	    		textField.setText("+");
	    	}
    	}
    	
    	return convertView;
    }
    
    
    OnClickListener	mSongListSongClick = new OnClickListener(){
		@Override
		public void onClick(View songLayout) {
			try
			{
				/* 
				 * Check playlist position 
				 */
				int position = ((ListView) songLayout.getParent()).
						getPositionForView(songLayout);
				if(position < mResultLimit)
				{
					Message msg = new Message();
					msg.what = Constants.SINGLE_CLICK;
					msg.arg1 = Integer.parseInt(mPlaylistArray.get(position).get(Constants.PLAYLIST_ID_KEY));
					msg.obj = mPlaylistArray.get(position).get(Constants.PLAYLIST_NAME_KEY);
					mPlaylistItemSelectedHandler.sendMessageDelayed(
							msg, 
							Constants.CLICK_ACTION_DELAY);
					mDialogInterface.dismiss();
					}
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		}
    };
    
    OnLongClickListener	mSongListSongLongClick = new OnLongClickListener(){
		@Override
		public boolean onLongClick(View songLayout) {
			try
			{
				/* 
				 * Check playlist position 
				 */
				int position = ((ListView) songLayout.getParent()).
						getPositionForView(songLayout);
				if(position < mResultLimit)
				{
					Message msg = new Message();
					msg.what = Constants.LONG_CLICK;
					msg.arg1 = Integer.parseInt(mPlaylistArray.get(position).get(Constants.PLAYLIST_ID_KEY));
					msg.obj = mPlaylistArray.get(position).get(Constants.PLAYLIST_NAME_KEY);
					mPlaylistItemSelectedHandler.sendMessageDelayed(
							msg, 
							Constants.CLICK_ACTION_DELAY);
					mDialogInterface.dismiss();
				}
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
			return true;
		}
    };
}