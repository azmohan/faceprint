package com.mediatek.HobbyDB;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class CustomHobbyProvider extends ContentProvider {
	
	 private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);   
	    private static final int CB = 1;   
	    private static final int CBS = 2;   
	    static{   
	        MATCHER.addURI("com.android.settings.customhobbyprovider", "customhobby/#", CB);   
	        MATCHER.addURI("com.android.settings.customhobbyprovider", "customhobbys", CBS);//#号为通配符   
	    }      
	    private CustomHobbyService mService;
	@Override  
	public boolean onCreate() {  
	    // TODO Auto-generated method stub  
		mService=new CustomHobbyService(getContext());
	    return true;  
	}  
	    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		 switch (MATCHER.match(uri)){
		 	case CB:
//		 		mService.delete(Integer.parseInt(selectionArgs[0].toString()), Integer.parseInt(selectionArgs[1].toString()));
		 		mService.deleteTableData();
		 		break;
		 	 default:
		            throw new RuntimeException("unknown uri " + uri.toString());
		 }
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		 switch (MATCHER.match(uri)){
		 	case CB:
		 		if (mService.isExistData(Integer.parseInt((String)values.get(CustomHobbyUtils.PARENT_TITLE)),Integer.parseInt((String)values.get(CustomHobbyUtils.CONTENT)))) {
		 			mService.update(Integer.parseInt((String)values.get(CustomHobbyUtils.PARENT_TITLE)),Integer.parseInt((String)values.get(CustomHobbyUtils.CONTENT)));
				}else{
					mService.insert(Integer.parseInt((String)values.get(CustomHobbyUtils.PARENT_TITLE)),Integer.parseInt((String)values.get(CustomHobbyUtils.CONTENT)),
			 				(String)values.get(CustomHobbyUtils.LINK),1,(String)values.get( CustomHobbyUtils.COMMENT));
				}
		 		break;
		 	 default:
		            throw new RuntimeException("unknown uri " + uri.toString());
		 }
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		 switch (MATCHER.match(uri)){
		 	case CB:
		 		mService.update(Integer.parseInt((String)values.get(CustomHobbyUtils.PARENT_TITLE)),Integer.parseInt((String)values.get(CustomHobbyUtils.CONTENT)));
		 		break;
		 	 default:
		            throw new RuntimeException("unknown uri " + uri.toString());
		 }
		return 0;
	}

}
