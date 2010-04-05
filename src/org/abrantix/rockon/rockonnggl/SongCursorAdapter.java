package org.abrantix.rockon.rockonnggl;

import java.io.File;
import java.io.IOException;

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
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;



public class SongCursorAdapter extends SimpleCursorAdapter{
	private final String		TAG = "SongCursorAdapter";
	
	private Cursor 				mSongCursor;
    private Context 			mContext;
    private	Handler				mSongItemSelectedHandler;
    private	DialogInterface		mDialogInterface;
    public int					mViewWidth;
    public int					mSongInitialPosition;
    private String[]			mFrom;
    private int[]				mTo;
    
    public SongCursorAdapter(Context context, 
    							int layout, 
    							Cursor c,
    							String[] from,
    							int[] to,
    							Handler clickHandler) 
    {
        super(context, layout, c, from, to);
        this.mSongCursor = c;
        this.mContext = context;
        this.mFrom = from;
        this.mTo = to;
        this.mSongInitialPosition = c.getPosition();
        this.mSongItemSelectedHandler = clickHandler;
    }

    public void setDialogInterface(DialogInterface dialogInterface){
    	this.mDialogInterface = dialogInterface;
    }
    
    /* (non-Javadoc)
     * This is where you actually create the item view of the list
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    	/* set click listener */
    	view.setOnClickListener(mSongListSongClick);
    	view.setOnLongClickListener(mSongListSongLongClick);
    	
    	for(int i=0; i<mFrom.length; i++){
    		TextView textField = (TextView)
    			view.findViewById(mTo[i]);
    		if(mFrom[i] == MediaStore.Audio.Media.DURATION){
    	    	try{
    	    		double duration = new Double (
    	    				cursor.getString(
    	    						cursor.getColumnIndex(
    	    								mFrom[i])));
    		    	double minutes = Math.floor(duration / 1000 / 60);
    		    	double seconds = duration / 1000 % 60;
    		    	if(seconds > 10)
    		    		textField.
    		    			setText(
    		    					String.valueOf((int)minutes)+
    		    					":"+
    		    					String.valueOf((int)seconds));
    		    	else
    		    		textField.
    		    			setText(
    		    					String.valueOf((int)minutes)+
    		    					":0"+
    		    					String.valueOf((int)seconds));
    	    	} catch (Exception e){
    	    		e.printStackTrace();
    	    		textField.setText("-:--");
    	    	}
    		} else {
    	    	textField.setText(cursor.getString(
    					cursor.getColumnIndex(
    							mFrom[i])));
    		}
    	}
    }
    
    OnClickListener	mSongListSongClick = new OnClickListener(){
		@Override
		public void onClick(View songLayout) {
			try
			{
				/* 
				 * Check song position 
				 */
				int position = ((ListView) songLayout.getParent()).
						getPositionForView(songLayout);
				mSongCursor.moveToPosition(position);
				int songId = (int) 
					ContentProviderUnifier.
						getAudioIdFromUnknownCursor(mSongCursor);
				Message msg = new Message();
				msg.arg1 = songId;
				msg.arg2 = Constants.NOW;
				mSongItemSelectedHandler.sendMessageDelayed(
						msg, 
						Constants.CLICK_ACTION_DELAY);
				mDialogInterface.dismiss();
				
//				Log.i(TAG, "Song "+position+" clicked - ADAPTER");
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
			/* Check song position */
			int position = ((ListView) songLayout.getParent()).
					getPositionForView(songLayout);
			mSongCursor.moveToPosition(position);
			int songId = (int)
				ContentProviderUnifier.
					getAudioIdFromUnknownCursor(mSongCursor);
//				mSongCursor.getInt(
//					mSongCursor.getColumnIndexOrThrow(
//							MediaStore.Audio.Media._ID));
			/* tell the handler to put it in the end of the list */
			Message msg = new Message();
			msg.arg1 = songId;
			msg.arg2 = Constants.LAST;
			mSongItemSelectedHandler.sendMessageDelayed(
					msg, 
					Constants.CLICK_ACTION_DELAY);
			mDialogInterface.dismiss();
//				((Filex) context).
//								songListView.getPositionForView(songTextView);
			
			Log.i(TAG, "Song "+position+" LONG clicked - ADAPTER");
			
			return true;
		}
    };
}