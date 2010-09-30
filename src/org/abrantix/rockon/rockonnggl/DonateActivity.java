package org.abrantix.rockon.rockonnggl;

import org.abrantix.rockon.rockonnggl.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

public class DonateActivity extends Activity{

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.donate_layout);
        
        attachListeners();
    }
    
    public void attachListeners()
    {
    	findViewById(R.id.donate_go_back_layout).setOnClickListener(mGlobalClickListener);
    	findViewById(R.id.donate_market).setOnClickListener(mGlobalClickListener);
    	findViewById(R.id.donate_paypal).setOnClickListener(mGlobalClickListener);
    }
    
    private OnClickListener mGlobalClickListener= new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(v.equals(findViewById(R.id.donate_go_back_layout)))
			{
				goBackToMainApp();
			}
			else if(v.equals(findViewById(R.id.donate_market)))
			{
				searchDonateAppsOnMarket();
			}
			else if(v.equals(findViewById(R.id.donate_paypal)))
			{
				openPaypalDonationPage();
			}
		}
	};
    
	private void saveDonationHistory()
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		editor.putBoolean(Constants.prefkey_mAppHasDonated, true);
		editor.commit();
	}
	
    private void searchDonateAppsOnMarket()
    {
		saveDonationHistory();
		Intent i = new Intent(Intent.ACTION_VIEW, 
				Uri.parse("market://search?&q=Filipe+Abrantes+donate"));
		startActivity(i);
    }
    
    private void openPaypalDonationPage()
    {
		saveDonationHistory();
    	Intent i = new Intent(Intent.ACTION_VIEW, 
    			Uri.parse("http://abrantix.org/cubed-donate.php"));
    	startActivity(i);
    }
    
    private void goBackToMainApp()
    {
    	Intent intent = new Intent(this, RockOnNextGenGL.class);
    	startActivity(intent);
    }
	
}
