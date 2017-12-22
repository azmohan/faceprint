package com.mediatek.HobbyDB;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class CustomHobbyService {
	private CustomHobbyDB chdb;
	private SQLiteDatabase  db;
	public CustomHobbyService(Context context){
		chdb=new CustomHobbyDB(context);
	}
	
	public void insert(int parent_title,int content,String link,int num,String comment){
		db=chdb.getWritableDatabase();
		String sqlStr="insert into "+CustomHobbyUtils.TABLE_NAME+"(" +CustomHobbyUtils.PARENT_TITLE+
				"," +CustomHobbyUtils.CONTENT+
				"," +CustomHobbyUtils.LINK+
				"," +CustomHobbyUtils.NUM+
				"," +CustomHobbyUtils.COMMENT+
				") values (" +parent_title+
				"," +content+
				",'" +link+
				"'," +num+
				",'" +comment+"'"+
				")";
		db.execSQL(sqlStr);
		db.close();
		Log.i("mylog","insert"+link);
	}
	
	public void setDefaultData(){
		db=chdb.getWritableDatabase();
		String sqlStr="update " +CustomHobbyUtils.TABLE_NAME+
				" set " +CustomHobbyUtils.NUM+"= 0";
		db.execSQL(sqlStr);
		db.close();
	}
	
	public void deleteTableData(){
		db=chdb.getWritableDatabase();
		String sqlStr="delete from " +CustomHobbyUtils.TABLE_NAME;
		db.execSQL(sqlStr);
		db.close();
	}
	
	public void update(int parent_title,int content){
		db=chdb.getWritableDatabase();
		String sqlStr="update " +CustomHobbyUtils.TABLE_NAME+
				" set " +CustomHobbyUtils.NUM+"="+CustomHobbyUtils.NUM+"+1 "+
				" where " + CustomHobbyUtils.PARENT_TITLE +"="+parent_title+
				" and " +CustomHobbyUtils.CONTENT+"="+content;
		db.execSQL(sqlStr);
		db.close();
	}
	
	public void delete(int parent_title,int content){
		db=chdb.getWritableDatabase();
		String sqlStr="delete from " +CustomHobbyUtils.TABLE_NAME+
				" where " + CustomHobbyUtils.PARENT_TITLE +"="+parent_title+
				" and " +CustomHobbyUtils.CONTENT+"="+content;
		db.execSQL(sqlStr);
		db.close();
	}
	
	public List<ContentValues> queryall(){
		db=chdb.getReadableDatabase();
		List<ContentValues>  values=new ArrayList<ContentValues>();
		String sqlStr="select * from "+CustomHobbyUtils.TABLE_NAME+
				" order by " + CustomHobbyUtils.NUM + " DESC  ";
		Cursor cursor=db.rawQuery( sqlStr, null); 
		while(cursor.moveToNext()){  
			values.add(getValue(cursor));
        }
		cursor.close();
		db.close();
		return values;
	}
	
	public List<ContentValues> queryTopThree(){
		db=chdb.getReadableDatabase();
		List<ContentValues>  values=new ArrayList<ContentValues>();
		String sqlStr="select * from "+CustomHobbyUtils.TABLE_NAME+
				" order by " + CustomHobbyUtils.NUM + " DESC limit 0,3  ";
		Cursor cursor=db.rawQuery( sqlStr, null); 
		while(cursor.moveToNext()){  
			values.add(getValue(cursor));
        }  
		cursor.close();
		db.close();
		return values;
	}
	
	public boolean  isExistData(int parent_title,int content){
		db=chdb.getReadableDatabase();
//		ContentValues  value = null;
		int num=0;
		String sqlStr="select  count(*)　 from "+CustomHobbyUtils.TABLE_NAME+
				" where " + CustomHobbyUtils.PARENT_TITLE +"="+parent_title+
				" and " +CustomHobbyUtils.CONTENT+"="+content;
		Cursor cursor=db.rawQuery( sqlStr, null); 
		cursor.moveToFirst();
		num=cursor.getInt(0);
		cursor.close();
		db.close();
		if (num==0) {
			return false;
		}else {
			return true;
		}
	}

	public ContentValues getValue(Cursor cursor){
		int id = cursor.getInt(cursor.getColumnIndex(CustomHobbyUtils.ID));  
		int parent_title = cursor.getInt(cursor.getColumnIndex(CustomHobbyUtils.PARENT_TITLE));  
		int content = cursor.getInt(cursor.getColumnIndex(CustomHobbyUtils.CONTENT));  
		String link=cursor.getString(cursor.getColumnIndex(CustomHobbyUtils.LINK));
		int num = cursor.getInt(cursor.getColumnIndex(CustomHobbyUtils.NUM));  
		String comment=cursor.getString(cursor.getColumnIndex(CustomHobbyUtils.COMMENT));
		Log.i("mylog","==1=="+num);
		return CustomHobbyUtils.getValues(id, parent_title, content, link, num, comment);
	}
	
}
