package org.abrantix.rockon.rockonnggl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class FreeCoversNetFetcher{

	final String TAG = "FreeCoversNetFetcher";
	final String FREE_COVERS_API_URL = "http://www.freecovers.net/api/search/";
	
	/*
	 * Declare & Initialize some vars
	 */
	SAXParserFactory mSaxParserFactory			= SAXParserFactory.newInstance();
    SAXParser mSaxParser 						= null;
    XMLReader mXmlReader 						= null;
    FreeCoversApiXmlResponseHandler mXmlHandler = new FreeCoversApiXmlResponseHandler();
	
    FreeCoversNetFetcher(){
    	try{
	    	mSaxParser 	= mSaxParserFactory.newSAXParser();
	        mXmlReader 	= mSaxParser.getXMLReader();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
	public Bitmap fetch(String artistName, String albumName){
		
		try{
			URL freeCoversURL = new URL(
					FREE_COVERS_API_URL+
					URLEncoder.encode(artistName)+
					"+"+
					URLEncoder.encode(albumName));
	
			/* connection setup */
			BasicHttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 7500);
			DefaultHttpClient httpClient = new DefaultHttpClient();		
	        HttpGet httpGet = new HttpGet(freeCoversURL.toString());
	        HttpResponse response; 
				        
			/* fetch content */
			response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
			BufferedReader in = 
				new BufferedReader(new InputStreamReader(
						entity.getContent()));
			/* parse xml response */
			mXmlHandler.reset();
			mXmlHandler.setSearchTarget(artistName+" - "+albumName);
			mXmlReader.setContentHandler(mXmlHandler);
			mXmlReader.parse(new InputSource(in));
		
			/* fetch Bitmap */
			Log.i(TAG, "fetching: "+mXmlHandler.albumArtUrl);
//			AlbumArtUtils albumArtUtils = new AlbumArtUtils();
//			Bitmap artBitmap = albumArtUtils.fetchBitmap(mXmlHandler.albumArtUrl);
//			
//			return artBitmap;
			
			// TODO: check is it is front+back -- maybe already inside albumartUtils
			return AlbumArtUtils.fetchBitmap(mXmlHandler.albumArtUrl);
			
			
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
		
}


class FreeCoversApiXmlResponseHandler extends DefaultHandler{
	
	final String TAG = "FreeCoversApiXmlResponseHandler";
	
	final String TITLE_TAG = "title";
	final String NAME_TAG = "name";
	final String TYPE_TAG = "type";
	final String TYPE_FRONT = "front";
	final String PREVIEW_TAG = "preview";
	
	boolean	titleTag = false;
	boolean nameTag = false;
	boolean typeTag = false;
	boolean previewTag = false;
	
	String	albumArtUrl = null;
	String	name = null;
	String	type = null;
	
	String	mSearchTarget = "";
	
	public void setSearchTarget(String searchTarget){
		this.mSearchTarget = searchTarget;
	}
	
	
	@Override
    public void startElement(String namespaceURI, 
    							String localName,
    							String qName,
    							Attributes atts) 
	throws SAXException {
		if(localName.equals(NAME_TAG)){
			nameTag = true;
		} else if(localName.equals(TYPE_TAG)){
			typeTag = true;
		} else if(localName.equals(PREVIEW_TAG)){
			previewTag = true;
		} else if(localName.equals(TITLE_TAG)){
			titleTag = true;
		}
	}
	
	@Override
	public void  endElement  (String uri, 
								String localName, 
								String qName)
	throws SAXException {
		if(localName.equals(NAME_TAG)){
			nameTag = false;
		} else if(localName.equals(TYPE_TAG)){
			typeTag = false;
		} else if(localName.equals(PREVIEW_TAG)){
			previewTag = false;
		} else if(localName.equals(TITLE_TAG)){
			titleTag = false;
		}
	}
	
	 @Override
	 public void characters(char ch[], int start, int length) {
//		 Log.i(TAG, new String(ch, start, length));
		 if(nameTag && 
				 albumArtUrl == null)
			 this.name = new String(ch, start, length);
		 else if(typeTag && 
				 albumArtUrl == null)
			 this.type = new String(ch, start, length);
		 else if(previewTag && 
				 albumArtUrl == null && 
				 type != null && 
				 type.equals(TYPE_FRONT) &&
				 SearchUtils.nameIsSimilarEnough(name, mSearchTarget)){			 
			 // TODO: check name similarity
			 this.albumArtUrl = new String(ch, start, length);
			 Log.i(TAG, name+" - "+albumArtUrl);
		 }
		 
	 }
	 
	 public void reset(){
		 titleTag = false;
		 nameTag = false;
		 typeTag = false;
		 previewTag = false;
			
		 albumArtUrl = null;
		 name = null;
		 type = null;
	 }
}
