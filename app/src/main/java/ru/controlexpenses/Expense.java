package ru.controlexpenses;
public class Expense {
 public long id; public String date,merchant,category,project,note; public double amount;
 public Expense(long i,String d,String m,String c,String p,String n,double a){id=i;date=d;merchant=m;category=c;project=p;note=n;amount=a;}
}