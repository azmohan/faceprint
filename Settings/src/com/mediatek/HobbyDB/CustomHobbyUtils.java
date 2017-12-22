package com.mediatek.HobbyDB;

import android.content.ContentValues;

public class CustomHobbyUtils {
	public static final int DB_VERSION = 1;
	// Database name
	public static final String DB_NAME = "Custom_Hobby_DB";
	// Database version number
	public static final String TABLE_NAME = "Custom_Hobby_Table";

	/* The database table fields */

	// primary key
	public static final String ID = "_id";
	
	public static final String  PARENT_TITLE= "parent_title";
	public static final String CONTENT="content";
	public static final String LINK= "link";
	public static final String NUM = "num";
	public static final String  COMMENT= "comment";
	

	public static ContentValues getValues(int  id,int  parent_title,int  content,String link,int num,String comment) {
		ContentValues custom_hobby = new ContentValues();
		custom_hobby = new ContentValues();
		custom_hobby.put(CustomHobbyUtils.ID, id);
		custom_hobby.put(CustomHobbyUtils.PARENT_TITLE, parent_title);
		custom_hobby.put(CustomHobbyUtils.CONTENT, content);
		custom_hobby.put(CustomHobbyUtils.LINK, link);
		custom_hobby.put(CustomHobbyUtils.NUM, num);
		custom_hobby.put(CustomHobbyUtils.COMMENT, comment);
		return custom_hobby;

	}
}
