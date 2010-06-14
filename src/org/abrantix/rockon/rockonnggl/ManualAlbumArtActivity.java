package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ManualAlbumArtActivity extends Activity{
	static long 			mAlbumId = -1;
	GridView				mChooserGrid;
	ManualArtChooserAdapter	mChooserAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Intent intent = getIntent();
		mAlbumId = intent.getLongExtra("albumId", -1);
		if(mAlbumId == -1){
			showNoAlbumSpecifiedError();
		} else {
			showAlbumChooser();
		}
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(mChooserAdapter != null)
			mChooserAdapter.stopAndClean();
		super.onDestroy();
	}
	
//	  @Override
//	  public void onAttachedToWindow() {
//	    super.onAttachedToWindow();
//	    Window window = getWindow();
//	    // Eliminates color banding
//	    window.setFormat(PixelFormat.RGBA_8888);
//	  }
	  
	  
	public void showNoAlbumSpecifiedError(){
		AlertDialog.Builder aD = new AlertDialog.Builder(this);
		aD.setTitle(getString(R.string.manual_albumart_error));
		aD.setMessage(getString(R.string.manual_albumart_no_album_specified));
		aD.show();
	}
	
	private void showAlbumChooser(){
		Cursor albumCursor = new CursorUtils(this).getAlbumFromAlbumId(mAlbumId);
		if(albumCursor.getCount() < 1){
			showNoAlbumSpecifiedError();
			return;
		}
		albumCursor.moveToFirst();
		String artist = 
			albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ARTIST));
		String album = 
			albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ALBUM));
		String embeddedArt = 
			albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ALBUM_ART));
		
		setContentView(
				((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
						inflate(R.layout.manual_art_chooser, null));
		mChooserGrid = (GridView) findViewById(R.id.manual_art_grid);
		mChooserAdapter = new ManualArtChooserAdapter(
				this, 
				artist, 
				album, 
				embeddedArt, 
				mAlbumId,
				updateUiHandler);
		mChooserGrid.setAdapter(mChooserAdapter);
		mChooserGrid.setOnItemClickListener(mAlbumGridClickListener);
		
		albumCursor.close();
	}
	
	OnItemClickListener mAlbumGridClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(
				AdapterView<?> arg0, 
				View arg1, 
				int arg2,
				long arg3) 
		{
			if(((ManualArtChooserAdapter) arg0.getAdapter()).covers[arg2] != null)
			{
				AlbumArtUtils.saveAlbumCoverInSdCard(
						((ManualArtChooserAdapter) arg0.getAdapter()).covers[arg2],
						String.valueOf(mAlbumId),
						true); 	// forces resolution above minimum to avoid being 
								// overriden the next time we search for covers
				AlbumArtUtils.saveSmallAlbumCoverInSdCard(
						((ManualArtChooserAdapter) arg0.getAdapter()).covers[arg2],
						String.valueOf(mAlbumId));
				Toast.makeText(
						getApplicationContext(), 
						R.string.album_art_changed, 
						Toast.LENGTH_SHORT)
					.show();

			}
		}
	
	};
	
	Handler updateUiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			mChooserGrid.invalidateViews();
		}
	};
}