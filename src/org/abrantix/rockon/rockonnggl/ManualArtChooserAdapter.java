package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ManualArtChooserAdapter extends BaseAdapter{

	final String	TAG = "ManualArtChooserAdapter";
	
	final int 	NUMBER_OF_GSEARCH_ITEMS = 8;
	final int 	NUMBER_OF_ITEMS = 1 + 1 + NUMBER_OF_GSEARCH_ITEMS; // embedded + freecovers + gsearch
	
	Bitmap[]	covers = new Bitmap[NUMBER_OF_ITEMS];
	Bitmap		loadingBitmap = Bitmap.createBitmap(
					Constants.REASONABLE_ALBUM_ART_SIZE, 
					Constants.REASONABLE_ALBUM_ART_SIZE, 
					Bitmap.Config.RGB_565);
	
	String		mArtist;
	String 		mAlbum;
	String		mEmbeddedArt;
	long		mAlbumId;

	Context 	mContext;
	Thread		mWorkerThread;
	Handler		mUpdateHandler;
	
	public ManualArtChooserAdapter(
			Context context,
			String artist, 
			String album, 
			String embeddedArt,
			long albumId,
			Handler	handler) 
	{
		mContext = context;
		mArtist = artist;
		mAlbum = album;
		mEmbeddedArt = embeddedArt;
		mAlbumId = albumId;
		mUpdateHandler = handler;
		if(mEmbeddedArt != null){
			covers[0]  = BitmapFactory.decodeFile(embeddedArt);
		}
		else
		{
			covers[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.unknown_256);
		}
		triggerAlbumArtFetchingInADifferentThread();
	}
	
	@Override
	public int getCount() {
		return NUMBER_OF_ITEMS;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View itemView;
		if(convertView != null)
			itemView = convertView;
		else
			itemView = 
				((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
					inflate(R.layout.art_grid_item, null);

		// image and image properties
		if(covers[position]!=null){
			BitmapDrawable bmD = new BitmapDrawable(covers[position]);
			bmD.setDither(true);
			bmD.setAntiAlias(true);
//			bmD.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			((ImageView)itemView.findViewById(R.id.art_grid_item_image))
				.setImageDrawable(bmD);
			((TextView)itemView.findViewById(R.id.art_grid_item_res))
				.setText(covers[position].getWidth()+"x"+covers[position].getHeight());
		}
		else{
			((ImageView)itemView.findViewById(R.id.art_grid_item_image))
				.setImageBitmap(loadingBitmap);
			if(position == 0)
				((TextView)itemView.findViewById(R.id.art_grid_item_res))
					.setText(mContext.getString(R.string.manual_albumart_resolution_unavailable));
			else
				((TextView)itemView.findViewById(R.id.art_grid_item_res))
					.setText(mContext.getString(R.string.manual_albumart_resolution_loading));
		}

		// art source
		if(position == 0)
			((TextView)itemView.findViewById(R.id.art_grid_item_source))
				.setText(
					mContext.getString(R.string.manual_albumart_source)+
					": "+
					mContext.getString(R.string.manual_albumart_source_local));
		else
			((TextView)itemView.findViewById(R.id.art_grid_item_source))
				.setText(
					mContext.getString(R.string.manual_albumart_source)+
					": "+
					mContext.getString(R.string.manual_albumart_source_internet));

	
		Log.i(TAG, "loading: "+position);
		
		return itemView;
	}
	
	public void stopAndClean(){
		mWorkerThread.interrupt();
		mGoogleImageHandler.removeCallbacksAndMessages(null);
		mUpdateHandler.removeCallbacksAndMessages(null);
	}
	
	private void triggerAlbumArtFetchingInADifferentThread(){
		mWorkerThread = new Thread(){
			@Override
			public void run(){
				// get from free covers
				FreeCoversNetFetcher freeCoversFetcher = new FreeCoversNetFetcher();
				Bitmap bm = freeCoversFetcher.fetch(mArtist, mAlbum);
				if(bm != null){
					covers[1] = bm;
					mUpdateHandler.sendEmptyMessage(0);
				}
				else
				{
					covers[1] = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.unknown_256);
					mUpdateHandler.sendEmptyMessage(0);
				}
				if(Thread.currentThread().isInterrupted())
					return;
				// Google search
				GoogleImagesFetcher gfetcher = new GoogleImagesFetcher();
				gfetcher.fetch(
						mArtist, 
						mAlbum, 
						NUMBER_OF_GSEARCH_ITEMS, 
						mGoogleImageHandler);
//				for(int i = 0; i<NUMBER_OF_GSEARCH_ITEMS; i++){
//					if(this.currentThread().isInterrupted())
//						return;
//					Bitmap gBm = gfetcher.fetch(mArtist, mAlbum);
//					if(gBm != null){
//						covers[2+i] = gBm;
//						mUpdateHandler.sendEmptyMessage(0);
//					}
//					break;
//				}
				// get from google
				// TODO: fetch images
			}
		};
		mWorkerThread.start();
	}
	
	Handler mGoogleImageHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			int index = msg.what;
			Bitmap bm = (Bitmap) msg.obj;
			covers[2+index] = bm;
			mUpdateHandler.sendEmptyMessage(0);
		}
	};
}