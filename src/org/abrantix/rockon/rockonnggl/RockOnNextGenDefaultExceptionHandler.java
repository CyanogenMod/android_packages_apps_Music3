package org.abrantix.rockon.rockonnggl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import org.abrantix.rockon.rockonnggl.R;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.sax.StartElementListener;
import android.util.Log;

	class RockOnNextGenDefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

		private UncaughtExceptionHandler oldDefaultExceptionHandler;
		private final String TAG = "RockOnNextGenDefaultExceptionHandler";
		private Context context;
	
		RockOnNextGenDefaultExceptionHandler(Context context) 
		{
//			Log.d(TAG, "Default Exception Handler=" + Thread.getDefaultUncaughtExceptionHandler());
			oldDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			
			Thread.setDefaultUncaughtExceptionHandler(this);
			
			this.context = context;
		}

		public void destroy(){
			this.context = null;
			Thread.setDefaultUncaughtExceptionHandler(null);
		}
		
		public void uncaughtException(Thread t, Throwable e) {
			if(e.getClass().equals(OutOfMemoryError.class))
			{
				android.os.Process.killProcess(android.os.Process.myPid());						
				return;
			}
			if(e.getMessage().contains("eglMakeCurrent failed"))
			{
				android.os.Process.killProcess(android.os.Process.myPid());						
				return;
			}
			if(e.getMessage().contains("eglSwapBuffers failed"))
			{
				android.os.Process.killProcess(android.os.Process.myPid());						
				return;
			}
			if(e.getMessage().contains("createContext failed"))
			{
				android.os.Process.killProcess(android.os.Process.myPid());						
				return;
			}
			
            PackageManager manager = context.getPackageManager();
            PackageInfo info;
            try{
            	info = manager.getPackageInfo(context.getPackageName(), 0);
            }catch(Exception e1){
            	info = new PackageInfo();
            	info.versionName = "0.0.0";
            }
			
			StringBuilder message = new StringBuilder(
					context.getString(R.string.bug_report_intro_part_one)+
					info.versionName+" "+
					context.getString(R.string.bug_report_intro_part_two)
					);
			message.append(String.format("-- Android Version: sdk=%s, release=%s, inc=%s\n",
				Build.VERSION.SDK, Build.VERSION.RELEASE, Build.VERSION.INCREMENTAL));
			
			Runtime rt = Runtime.getRuntime();
			message.append(String.format("-- Memory free: %4.2fMB total: %4.2fMB max: %4.2fMB",
					rt.freeMemory() / 1024 / 1024.0, 
					rt.totalMemory() / 1024 / 1024.0,
					rt.maxMemory() / 1024 / 1024.0));
			message.append(String.format("-- Thread State: %s\n", t.getState()));
			
			// Add stacktrace

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			
			e.printStackTrace(pw);
			pw.close();
			
			message.append("-- Stacktrace:\n");
			message.append(sw.getBuffer());
			
			String messageBody = message.toString();
			
			// ignore certain exceptions
//			if (Pattern.compile("CacheManager.java:391").matcher(messageBody).find())
//				return;

			// Prepare Mail
	
			final Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			sendIntent.setType("message/rfc822");
			sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "filipe.abrantes@gmail.com" });
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, 
					"3 BugReport ["+info.versionName+"]: " + e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);
//			Log.e(TAG, "Exception handled. Email activity should be initiated now.");
	
			// Send Mail
	
			new Thread(new Runnable() {
				public void run() {
					context.startActivity(sendIntent);
					//sendBroadcast(sendIntent);
				}
			}).start();
	
//			Log.e(TAG, "Exception handled. Email should be sent by now.");
	
			// Use default exception mechanism
	
			if (oldDefaultExceptionHandler != null)
				oldDefaultExceptionHandler.uncaughtException(t, e);
		}

	}