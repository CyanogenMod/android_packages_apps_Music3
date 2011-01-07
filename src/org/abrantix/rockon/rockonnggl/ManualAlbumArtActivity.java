package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.cm.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ManualAlbumArtActivity extends Activity{
	static long 			mAlbumId = -1;
	GridView				mChooserGrid;
	ManualArtChooserAdapter	mChooserAdapter;
	String					MANUAL_SEARCH = "manualSearch";
	boolean					mManualSearch = false;
	static String			mManualArtist;
	static String			mManualAlbum;
	IRockOnNextGenService	mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		if(savedInstanceState != null)
			mManualSearch = savedInstanceState.getBoolean(MANUAL_SEARCH);
		
		Intent intent = getIntent();
		mAlbumId = intent.getLongExtra("albumId", -1);
		if(mAlbumId == -1){
			showNoAlbumSpecifiedError();
		} else {
			showAlbumChooser();
			attachListeners();
		}
		
		Intent i = new Intent(this, RockOnNextGenService.class);
    	startService(i);
    	bindService(i, mServiceConnection, BIND_AUTO_CREATE);
    }
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
	    @Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {
	        try{ 
	        	mService = IRockOnNextGenService.Stub.asInterface(obj);
	        	mService.trackPage(Constants.ANALYTICS_MANUAL_ART_PAGE);
	        } catch(RemoteException e) {
	        	e.printStackTrace();
	        }
	    }

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
	};
	 
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
		if(mService != null)
			unbindService(mServiceConnection);
		super.onDestroy();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstance)
	{
		savedInstance.putBoolean(MANUAL_SEARCH, mManualSearch);
	}
	
//	  @Override
//	  public void onAttachedToWindow() {
//	    super.onAttachedToWindow();
//	    Window window = getWindow();
//	    // Eliminates color banding
//	    window.setFormat(PixelFormat.RGBA_8888);
//	  }
	  
	
	public void attachListeners()
	{
		findViewById(R.id.manual_input_enable_button)
		.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						findViewById(R.id.manual_input_sub_layout).setVisibility(View.VISIBLE);
						findViewById(R.id.manual_input_go_button).setVisibility(View.VISIBLE);
						findViewById(R.id.manual_input_enable_button).setVisibility(View.GONE);
					}
				});
		
		findViewById(R.id.manual_input_go_button)
		.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						mManualSearch = true;
						mManualAlbum = 
							((EditText)findViewById(R.id.manual_input_album))
							.getText()
							.toString();
						mManualArtist =
							((EditText)findViewById(R.id.manual_input_artist))
							.getText()
							.toString();
						mChooserAdapter.stopAndClean();
						showAlbumChooser();
					}
				});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			if(mManualSearch)
			{
				mManualSearch = false;
				findViewById(R.id.manual_input_sub_layout).setVisibility(View.GONE);
				findViewById(R.id.manual_input_go_button).setVisibility(View.GONE);
				findViewById(R.id.manual_input_enable_button).setVisibility(View.VISIBLE);
				mChooserAdapter.stopAndClean();
				showAlbumChooser();
				return true;
			}
			else
				break;
		}
		return super.onKeyDown(keyCode, event);
	}
	  
	public void showNoAlbumSpecifiedError(){
		AlertDialog.Builder aD = new AlertDialog.Builder(this);
		aD.setTitle(getString(R.string.manual_albumart_error));
		aD.setMessage(getString(R.string.manual_albumart_no_album_specified));
		aD.show();
	}
	
	private void showAlbumChooser(){
		/*
		 * Set the content view if it isnt already set
		 */
		if(findViewById(R.id.manual_art_grid) == null)
		{
			setContentView(
				((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
						inflate(R.layout.manual_art_chooser, null));
		}
		
		/*
		 * Choose whether to show manual art button or edittexts
		 */
		if(mManualSearch)
		{
			findViewById(R.id.manual_input_sub_layout).setVisibility(View.VISIBLE);
			findViewById(R.id.manual_input_go_button).setVisibility(View.VISIBLE);
			findViewById(R.id.manual_input_enable_button).setVisibility(View.GONE);
			
			/*
			 * Restore album and artist values
			 */
			((EditText)findViewById(R.id.manual_input_album)).setText(mManualAlbum);
			((EditText)findViewById(R.id.manual_input_artist)).setText(mManualArtist);
		}
		
		
		/*
		 * Trigger cover fetching in the bg
		 */
		Cursor albumCursor = new CursorUtils(this).getAlbumFromAlbumId(mAlbumId);
		if(albumCursor.getCount() < 1){
			showNoAlbumSpecifiedError();
			return;
		}
		albumCursor.moveToFirst();
		String embeddedArt = 
			albumCursor.getString(
				albumCursor.getColumnIndexOrThrow(
						MediaStore.Audio.Albums.ALBUM_ART));
		String artist = null;
		String album = null;
		if(mManualSearch)
		{
			artist = mManualArtist;
			album = mManualAlbum;
		}
		else
		{
			artist= 
				albumCursor.getString(
						albumCursor.getColumnIndexOrThrow(
								MediaStore.Audio.Albums.ARTIST));
			album = 
				albumCursor.getString(
						albumCursor.getColumnIndexOrThrow(
								MediaStore.Audio.Albums.ALBUM));
		}
		
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