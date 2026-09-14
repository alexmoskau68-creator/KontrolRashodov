package ru.controlexpenses;

import android.app.*;import android.os.*;import android.view.*;import android.widget.*;import java.text.*;import java.util.*;

public class MainActivity extends Activity{
 DB db; String[] cats={"Продукты","Кафе и рестораны","Транспорт","Дом","Здоровье","Развлечения","Одежда","Связь","Другое"};
 public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);db=new DB(this);
  findViewById(R.id.expenses).setOnClickListener(v->expenses());findViewById(R.id.projects).setOnClickListener(v->projects());findViewById(R.id.reports).setOnClickListener(v->reports());expenses();}
 TextView t(String s,int z,boolean bo){TextView x=new TextView(this);x.setText(s);x.setTextSize(z);x.setPadding(14,10,14,10);if(bo)x.setTypeface(null,1);return x;}
 Button b(String s){Button x=new Button(this);x.setText(s);x.setAllCaps(false);return x;}
 void screen(View v){FrameLayout f=findViewById(R.id.content);f.removeAllViews();f.addView(v);}
 String today(){return new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(new Date());}
 String money(double n){return String.format(Locale.getDefault(),"%.2f",n).replace('.',',');}
 void expenses(){ScrollView s=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(14,8,14,8);
  l.addView(t("Всего расходов: "+money(db.total())+" ₽",23,true));Button add=b("＋ Добавить расход");add.setOnClickListener(v->addDialog());l.addView(add);
  for(Expense e:db.all()){l.addView(t(e.date+" • "+e.merchant+" • "+money(e.amount)+" ₽",17,true));l.addView(t(e.category,14,false));Button ed=b("Изменить");ed.setOnClickListener(v->editDialog(e));l.addView(ed);}
  s.addView(l);screen(s);}
 void addDialog(){new AlertDialog.Builder(this).setTitle("Добавить расход").setItems(new String[]{"Сканировать камерой","Выбрать фото из галереи","Ввести вручную","Ввести голосом"},(d,w)->editDialog(null)).show();}
 void editDialog(Expense e){boolean n=e==null;if(n)e=new Expense(0,today(),"","Другое","","",0);LinearLayout l=new LinearLayout(this);l.setPadding(12,4,12,4);l.setOrientation(LinearLayout.VERTICAL);
  EditText date=f("Дата",e.date),shop=f("Магазин / получатель",e.merchant),amount=f("Сумма",e.amount==0?"":money(e.amount)),note=f("Комментарий",e.note);Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats));cat.setSelection(Math.max(0,Arrays.asList(cats).indexOf(e.category)));
  l.addView(date);l.addView(shop);l.addView(amount);l.addView(cat);l.addView(note);Expense z=e;
  new AlertDialog.Builder(this).setTitle(n?"Новый расход":"Изменить расход").setView(l).setPositiveButton("Сохранить",(d,w)->{z.date=date.getText().toString();z.merchant=shop.getText().toString();z.amount=parse(amount.getText().toString());z.category=String.valueOf(cat.getSelectedItem());z.note=note.getText().toString();if(n)db.add(z);else db.update(z);expenses();}).setNegativeButton("Отмена",null).show();}
 EditText f(String h,String v){EditText x=new EditText(this);x.setHint(h);x.setText(v);return x;}double parse(String s){try{return Double.parseDouble(s.replace(" ","").replace(',','.'));}catch(Exception e){return 0;}}
 void projects(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(14,8,14,8);l.addView(t("Проекты",24,true));Button a=b("＋ Новый проект");a.setOnClickListener(v->{EditText e=f("Название проекта","");new AlertDialog.Builder(this).setTitle("Новый проект").setView(e).setPositiveButton("Создать",(d,w)->{db.project(e.getText().toString());projects();}).setNegativeButton("Отмена",null).show();});l.addView(a);for(String p:db.projects())l.addView(t(p,18,true));screen(l);}
 void reports(){ScrollView s=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(14,8,14,8);l.addView(t("Отчёты",24,true));l.addView(t("Всего: "+money(db.total())+" ₽",22,true));for(String[] x:db.byCat())l.addView(t(x[0]+" — "+x[1]+" ₽",17,true));s.addView(l);screen(s);}
}