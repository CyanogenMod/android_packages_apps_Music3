package org.abrantix.rockon.rockonnggl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class EqualizerWrapper {
	
	private static final String TAG = "EqualierWrapper";
	Object	mEqualizer = null;
	EqSettings mSettings;
	boolean	mEnabled = false;
	
	public EqualizerWrapper(EqSettings settings) {
		mSettings = settings;
	}
	
	public void enable(int priority, int audioSession) {
//	public EqualizerWrapper(int priority, int audioSession) {
		Class c;
		try {
			if(mEqualizer == null) {
				// new Equalizer(priority, audioSessionId)
				c = Class.forName("android.media.audiofx.Equalizer");
				Constructor constructor = c.getConstructor(new Class[]{int.class, int.class});
				mEqualizer = constructor.newInstance(new Object[]{priority, audioSession});
			}
			// setEnabled
			Method m = mEqualizer.getClass().getMethod("setEnabled", new Class[]{boolean.class});
			m.invoke(mEqualizer, true);
			
			// EqSettings update
			mSettings.setEnabled();
			if(mSettings.isBogus()) {
				Log.i(TAG, "Saved equalizer settings was bogus!");
				readPropertiesFromEq();
			} else {
				setProperties(mSettings);
			}
			mEnabled = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
	}
	
	public void disable() {
		// setEnabled
		try {
			Method m = mEqualizer.getClass().getMethod("setEnabled", new Class[]{boolean.class});
			m.invoke(mEqualizer, false);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		// my own stuff
		mEnabled = false;
		mSettings.setDisabled();
		// HOW TO DISABLE THIS? Just make everything flat?
		// ... yeah, making everything flat
		for(short i=0; i<getNumberOfBands(); i++) {
			setBandLevel(i, (short) (getBandLevelRange()[0] + (getBandLevelRange()[1]-getBandLevelRange()[0])/2), true);
		}
	}
	
	static public boolean isSupported() {
		try {
			Class.forName("android.media.audiofx.Equalizer");
			return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isEnabled() {
		return mEnabled;
	}
	
	public short 	getBand(int frequency) {
		return -1;
	}
	
	public int[] 	getBandFreqRange(short band) {
		return new int[]{0, 20000000};
	}
	
	public short 	getBandLevel(short band) {
		if(mEqualizer != null && mEnabled) {
			try {
				Method m = mEqualizer.getClass().getMethod("getBandLevel", new Class[]{short.class});
				return (Short) m.invoke(mEqualizer, new Object[]{band});
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return -1;
		} else {
			return (short) mSettings.mBandLevels[band];
		}
	}
	
	public short[] 	getBandLevelRange() {
		if(mEqualizer != null && mEnabled) {
			try {
				Method m = mEqualizer.getClass().getMethod("getBandLevelRange", new Class[]{});
				return (short[]) m.invoke(mEqualizer, new Object[]{});
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return new short[]{0,255};
		} else {
			int[] levelRange = mSettings.getLevelRangeInArray();
			short[] levelRangeShort = new short[levelRange.length];
			for(int i=0; i<levelRange.length; i++) {
				levelRangeShort[i] = (short) levelRange[i];
			}
			return levelRangeShort;
		}
		
	}
	
	public int 	getCenterFreq(short band) {
		if(mEqualizer != null && mEnabled) {
			try {
				Method m = mEqualizer.getClass().getMethod("getCenterFreq", new Class[]{short.class});
				return (Integer) m.invoke(mEqualizer, new Object[]{band});
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return -1;
		} else {
			return mSettings.mBandHz[band];
		}
	}
	
	public short 	getCurrentPreset() {
		return -1;
	}
	
	public short 	getNumberOfBands() {
		if(mEqualizer != null && mEnabled) {
			try {
				Method m = mEqualizer.getClass().getMethod("getNumberOfBands", new Class[]{});
				return (Short) m.invoke(mEqualizer, new Object[]{});
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return -1;
		} else {
			return (short) mSettings.mNumBands;
		}
	}
	
	public short 	getNumberOfPresets() {
		return -1;
	}
	
	public String 	getPresetName(short preset) {
		return null;
	}
	
	public void 	setBandLevel(short band, short level) {
		setBandLevel(band, level, false);
	}

	private void 	setBandLevel(short band, short level, boolean keepSettings) {
		if(mEqualizer != null) {
			try {
				Method m = mEqualizer.getClass().getMethod("setBandLevel", new Class[]{short.class, short.class});
				m.invoke(mEqualizer, new Object[]{band, level});
				if(!keepSettings)
					mSettings.mBandLevels[band] = level;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// nothing
		}
	}
	
	public void 	usePreset(short preset) {
		
	}
	
	public String getSettings() throws IOException {
		try {
			return mSettings.writeTo64String();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public void setProperties(EqSettings settings) {
		// Set each band level
		for(short i=0; i<settings.mNumBands; i++) {
			setBandLevel(i, (short)settings.mBandLevels[i]);
			Log.i(TAG, "Setting band "+i+"+ from cache: "+(short)settings.mBandLevels[i]);
		}
		
		// Set the preset
		// TODO:
		
		// ...
	}
	
	private void readPropertiesFromEq() {
		// num bands
		mSettings.mNumBands = getNumberOfBands();
		// band center freqs
		mSettings.mBandHz = new int[getNumberOfBands()];
		for(short i=0; i<getNumberOfBands(); i++) {
			mSettings.mBandHz[i] = getCenterFreq(i);
		}
		// band levels
		mSettings.mBandLevels = new int[getNumberOfBands()];
		for(short i=0; i<getNumberOfBands(); i++) {
			mSettings.mBandLevels[i] = getBandLevel(i);
		}
		// level range
		mSettings.setLevelRange(getBandLevelRange());
		// current preset 
		mSettings.mCurrentPreset = getCurrentPreset();
		// preset names
		if(getNumberOfPresets() > 0) {
			mSettings.mPresetNames = new String[getNumberOfPresets()];
			for(short i=0; i<getNumberOfPresets(); i++) {
				mSettings.mPresetNames[i] = getPresetName(i);
			}
		}
	}
	
//	public void 	setParameterListener(Equalizer.OnParameterChangeListener listener)
//	void 	setProperties(Equalizer.Settings settings)
//	Equalizer.Settings 	getProperties()
	
}
