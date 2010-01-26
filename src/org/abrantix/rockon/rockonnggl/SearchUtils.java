package org.abrantix.rockon.rockonnggl;

import android.util.Log;

public class SearchUtils{
	
	static boolean nameIsSimilarEnough(String artistNameFromSearch, String artistNameFiltered){
		/*
		 * First check if the separate words of our name 
		 * are contained in the result from the search
		 */
		// TODO:
		
		/* Then, do a blind search */
		int COMPARISON_SEGMENT_LENGTH = 3;
		int matchedSegments = 0;
		for(int i=0; i < artistNameFiltered.length() - COMPARISON_SEGMENT_LENGTH; i++){
			if(artistNameFromSearch.contains(artistNameFiltered.substring(i, i+COMPARISON_SEGMENT_LENGTH))){
				matchedSegments++;
			}
//			Log.i("SIMILARITY", artistNameFiltered.substring(i, i+COMPARISON_SEGMENT_LENGTH) +" in "+artistNameFromSearch);
		}
		Log.i("SIMILARITY", artistNameFromSearch+" ? "+artistNameFiltered);
		Log.i("SIMILARITY", ((double)matchedSegments)/(artistNameFiltered.length() - COMPARISON_SEGMENT_LENGTH)+
				" > "+Constants.SIMILARITY_THRESHOLD);
		if(((double)matchedSegments)/(artistNameFiltered.length() - COMPARISON_SEGMENT_LENGTH) 
				> Constants.SIMILARITY_THRESHOLD)
			return true;
		else
			return false;
	}
	
	
	/*******************************
	 * 
	 * filterString
	 * 
	 *******************************/
	static public String filterString(String original){
		String filtered = original;
		
		try{
			/* Remove anything within () or []*/
			int init = original.indexOf('(');
			int stop = original.indexOf(')', init);
			if(init != -1 && stop != -1){
				String addInfo= original.substring(init, stop+1);
				filtered = original.substring(0, init) +
							original.substring(stop+1, original.length());
			}
			init = filtered.indexOf('[');
			stop = filtered.indexOf(']', init);
			if(init != -1 && stop != -1){
				String addInfo= filtered.substring(init, stop+1);
				filtered = filtered.substring(0, init) +
							filtered.substring(stop+1, filtered.length());
			}
			
			/* Remove common album name garbage */
			filtered = filtered.replace("CD1", "");
			filtered = filtered.replace("CD2", "");
			filtered = filtered.replace("cd1", "");
			filtered = filtered.replace("cd2", "");
			
			/* Remove strange characters */
			filtered = filtered.replace(',', ' ');
			filtered = filtered.replace('.', ' ');
			filtered = filtered.replace('+', ' ');
			filtered = filtered.replace('/', ' ');
			filtered = filtered.replace('<', ' ');
			filtered = filtered.replace('>', ' ');
			filtered = filtered.replace('?', ' ');
			filtered = filtered.replace('|', ' ');
			filtered = filtered.replace('#', ' ');
			filtered = filtered.replace('&', ' ');
			filtered = filtered.replace('%', ' ');
			
			Log.i("filter", filtered);
	
			return filtered;
		} catch (StringIndexOutOfBoundsException e){
			e.printStackTrace();
			return original;
		}
	}
}