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
import android.os.Bundle;
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



public class AlbumCursorAdapter extends SimpleCursorAdapter{
	private final String		TAG = "AlbumCursorAdapter";
	
	private Cursor 				mAlbumCursor;
	private int					mArtistId;
	private String				mArtistName;
    private Context 			mContext;
    private	Handler				mAlbumItemSelectedHandler;
    private	DialogInterface		mDialogInterface;
    public int					mViewWidth;
    public int					mAlbumInitialPosition;
    private String[]			mFrom;
    private int[]				mTo;
    
    public AlbumCursorAdapter(Context context, 
    							int layout, 
    							Cursor c,
    							int	artistId,
    							String artistName,
    							String[] from,
    							int[] to,
    							Handler clickHandler) 
    {
        super(context, layout, c, from, to);
        this.mAlbumCursor = c;
        this.mArtistId = artistId;
        this.mArtistName = artistName;
        this.mContext = context;
        this.mFrom = from;
        this.mTo = to;
        this.mAlbumInitialPosition = c.getPosition();
        this.mAlbumItemSelectedHandler = clickHandler;
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
    	view.setOnClickListener(mAlbumListAlbumClick);
    	view.setOnLongClickListener(mAlbumListAlbumLongClick);
    	
    	for(int i=0; i<mFrom.length; i++){
    		TextView textField = (TextView)
    			view.findViewById(mTo[i]);
	    	textField.setText(cursor.getString(
					cursor.getColumnIndex(
							mFrom[i])));
    	}
    }
    
    OnClickListener	mAlbumListAlbumClick = new OnClickListener(){
		@Override
		public void onClick(View songLayout) {
			try
			{
				/* 
				 * Check album position 
				 */
				int position = ((ListView) songLayout.getParent()).
						getPositionForView(songLayout);
				mAlbumCursor.moveToPosition(position);
				Bundle b = new Bundle();
				b.putInt(
						"artistId", 
						mArtistId);
				b.putInt(
						"albumId", 
						mAlbumCursor.getInt(
								mAlbumCursor.getColumnIndexOrThrow(
										MediaStore.Audio.Albums._ID)));
				b.putString(
						"artistName", 
						mArtistName);
				b.putString(
						"albumName", 
						mAlbumCursor.getString(
								mAlbumCursor.getColumnIndexOrThrow(
										MediaStore.Audio.Albums.ALBUM)));
				Message msg = new Message();
				msg.setData(b);
				mAlbumItemSelectedHandler.sendMessageDelayed(
						msg, 
						Constants.CLICK_ACTION_DELAY);
				mDialogInterface.dismiss();
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		}
    };
    
    OnLongClickListener	mAlbumListAlbumLongClick = new OnLongClickListener(){
		@Override
		public boolean onLongClick(View songLayout) {
			// TODO:
			// Queue the whole album
//			/* Check song position */
//			int position = ((ListView) songLayout.getParent()).
//					getPositionForView(songLayout);
//			mAlbumCursor.moveToPosition(position);
//			int songId = (int)
//				ContentProviderUnifier.
//					getAudioIdFromUnknownCursor(mAlbumCursor);
////				mSongCursor.getInt(
////					mSongCursor.getColumnIndexOrThrow(
////							MediaStore.Audio.Media._ID));
//			/* tell the handler to put it in the end of the list */
//			Message msg = new Message();
//			msg.arg1 = songId;
//			msg.arg2 = Constants.LAST;
//			mAlbumItemSelectedHandler.sendMessageDelayed(
//					msg, 
//					Constants.CLICK_ACTION_DELAY);
//			mDialogInterface.dismiss();
////				((Filex) context).
////								songListView.getPositionForView(songTextView);
//			
//			Log.i(TAG, "Song "+position+" LONG clicked - ADAPTER");
			
			return true;
		}
    };
}