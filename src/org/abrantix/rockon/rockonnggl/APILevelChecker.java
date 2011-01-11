package org.abrantix.rockon.rockonnggl;

import java.lang.reflect.Field;
import android.os.Build;
import android.view.View;

public class APILevelChecker {
	//Singleton pattern implementation taken from Android Developers Blog: 
	//http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
	
	//Reflection-based API Level Int check, with package-level "3" minimum, taken from "Android 1" blog entry:
	//http://doandroids.com/blogs/2010/5/8/backwards-compatibility/

	public int SDK_INT;

	// Private constructor prevents instantiation from other classes
	private APILevelChecker() {
		try {
		    // works for level 4 and up
		    Field SDK_INT_field = Build.VERSION.class.getField("SDK_INT");
		    SDK_INT = (Integer) SDK_INT_field.get(null);
		} catch (NoSuchFieldException e) {
		    // Must be level 3 (since the app doesn't support lower levels)
			SDK_INT=3;
		} catch (IllegalAccessException e) {
			//Shouldn't happen, let's assume 0 is a suitable invalid value, 
			// we don't want to add a throws clause.
			SDK_INT=0;
		}
	}

	/**
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder { 
		private static final APILevelChecker INSTANCE = new APILevelChecker();
	}

	public static APILevelChecker getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public void hapticFeedback(View v)
	{
		if (SDK_INT >= 5) {
			try {
				Class c = Class.forName("HapticFeedbackConstants");
				int virtualKey;
				virtualKey = c.getField("VIRTUAL_KEY").getInt(null);
				v.performHapticFeedback(virtualKey);
//				v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
