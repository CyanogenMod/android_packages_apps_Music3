package org.abrantix.rockon.rockonnggl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;

import android.util.Base64;
import android.util.Log;

public class EqSettings implements Serializable{
	private static final String TAG = null;
	/*short[]*/int[]	mBandLevels = new int[]{127,127,127,127,127,127,127,127}; // EqualizerSettings
	int					mCurrentPreset = -1; // EqualizerSettings
	int					mNumBands = 8; // EqualizerSettings
	
	int[]						mBandHz = new int[]{20000, 100000, 250000, 1000000, 2000000, 5000000, 10000000, 20000000};
	private /*short[]*/int[]	mLevelRange = {0, 255};
	
	String[]			mPresetNames = {""};
	
	private boolean		mEnabled = false;
	
	public boolean isBogus() {
		return (mBandLevels[0] < 0);
	}
	
	public void setDisabled() {
		mEnabled = false;
	}
	
	public void setEnabled() {
		mEnabled = true;
	}
	
	public boolean isEnabled() {
		return mEnabled;
	}
	
	public int getMaxLevel() {
		return mLevelRange[mLevelRange.length-1];
	}
	
	public void setLevelRange(int[] levels) {
		mLevelRange = levels;
	}
	
	public void setLevelRange(short[] levels) {
		mLevelRange = new int[levels.length];
		for(int i=0; i<levels.length; i++) {
			mLevelRange[i] = levels[i];
		}
	}
	
	public int[] getLevelRangeInArray() {
		return mLevelRange;
	}
	
	public int getLevelRange() {
		return mLevelRange[1]-mLevelRange[0];
	}
	
	public int getHumanLevelRange() {
		return 7;
	}
	
	public int getHumanMidLevelIdx() {
//		return (int) Math.floor(getHumanLevelRange()/2);
		return getHumanLevelRange()/2;
	}
	
	int oLevel;
	public String getHumanLevel(int idx) {
		 oLevel = idx-getHumanMidLevelIdx();
		 if(oLevel>0)
			 return "+"+String.valueOf(oLevel);
		 else if(oLevel<0)
			 return String.valueOf(oLevel);
		 else
			 return "0";
	}
	
	public int getBaseLevel() {
		return mLevelRange[0];
	}
	
	public int getTopLevel() {
		return mLevelRange[1];
	}
	
	public float getBandLevelInPercent(int bandIdx) {
		return (mBandLevels[bandIdx]-getBaseLevel())/(float)getLevelRange();
	}
	
	public float getHumanLevelInPercent(int lvlIdx) {
//		return mLevelRange[lvlIdx]/(float)mLevelRange[mLevelRange.length-1];
		return lvlIdx/(float)(getHumanLevelRange()-1);
	}
	
	public int getClosestGain(float gainPercent) {
		return getBaseLevel() + Math.round(gainPercent*(getTopLevel()-getBaseLevel()));
	}
	
	public void setGainFromPercentage(int bandIdx, float gainPercent) {
		mBandLevels[bandIdx] = getBaseLevel() + Math.round(gainPercent*(getTopLevel()-getBaseLevel()));
	}
	
	public String writeTo64String() throws IOException {
	    /** Write the object to a Base64 string. */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(this);
	    oos.close();
	    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT); 
	}
	
	static EqSettings readFrom64String(String s) throws OptionalDataException, ClassNotFoundException, IOException {
		/** Read the object from Base64 string. */
	    byte [] data = Base64.decode(s, Base64.DEFAULT);
	    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
	    Object o  = ois.readObject();
	    ois.close();
	    return (EqSettings)o;
	}
}
