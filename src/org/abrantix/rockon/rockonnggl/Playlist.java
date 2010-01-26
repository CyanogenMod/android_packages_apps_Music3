package org.abrantix.rockon.rockonnggl;

import java.util.HashMap;

class Playlist extends HashMap<String, String>{
	int		mId;
	String	mName;
	
	public Playlist() {
		
	}
	
	public Playlist(int id, String name){
		mId = id;
		mName = name;
		// HashMap methods
		this.put(Constants.PLAYLIST_ID_KEY, String.valueOf(id));
		this.put(Constants.PLAYLIST_NAME_KEY, name);
	}
}