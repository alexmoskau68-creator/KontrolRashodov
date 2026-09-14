package ru.controlexpenses;
import android.content.*;import android.database.*;import android.database.sqlite.*;import java.util.*;
public class DB extends SQLiteOpenHelper{
 public DB(Context c){super(c,"control.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE e(id INTEGER PRIMARY KEY AUTOINCREMENT,date TEXT,merchant TEXT,category TEXT,project TEXT,note TEXT,amount REAL)");d.execSQL("CREATE TABLE p(name TEXT UNIQUE)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 void add(Expense e){ContentValues v=cv(e);getWritableDatabase().insert("e",null,v);}
 void update(Expense e){getWritableDatabase().update("e",cv(e),"id=?",new String[]{""+e.id});}
 ContentValues cv(Expense e){ContentValues v=new ContentValues();v.put("date",e.date);v.put("merchant",e.merchant);v.put("category",e.category);v.put("project",e.project);v.put("note",e.note);v.put("amount",e.amount);return v;}
 double total(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM e",null);c.moveToFirst();double x=c.getDouble(0);c.close();return x;}
 ArrayList<Expense> all(){ArrayList<Expense>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,date,merchant,category,project,note,amount FROM e ORDER BY id DESC",null);while(c.moveToNext())a.add(new Expense(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getDouble(6)));c.close();return a;}
 void project(String s){if(s.trim().isEmpty())return;ContentValues v=new ContentValues();v.put("name",s.trim());getWritableDatabase().insertWithOnConflict("p",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
 ArrayList<String> projects(){ArrayList<String>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT name FROM p ORDER BY name",null);while(c.moveToNext())a.add(c.getString(0));c.close();return a;}
 ArrayList<String[]> byCat(){ArrayList<String[]>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT category,SUM(amount) FROM e GROUP BY category ORDER BY SUM(amount) DESC",null);while(c.moveToNext())a.add(new String[]{c.getString(0),String.format(Locale.getDefault(),"%.2f",c.getDouble(1))});c.close();return a;}
}