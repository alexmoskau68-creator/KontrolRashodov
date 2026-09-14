package ru.controlexpenses;
public class Expense {
 public long id; public String date,merchant,category,project,note,currency; public double amount;
 public Expense(long i,String d,String m,String c,String p,String n,double a){this(i,d,m,c,p,n,a,"RUB");}
 public Expense(long i,String d,String m,String c,String p,String n,double a,String cur){id=i;date=d;merchant=m;category=c;project=p;note=n;amount=a;currency=cur==null||cur.isEmpty()?"RUB":cur;}
}