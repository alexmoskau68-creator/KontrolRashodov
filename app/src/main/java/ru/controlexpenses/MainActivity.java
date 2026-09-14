package ru.controlexpenses;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_CAMERA = 10;
    static final int REQ_GALLERY = 11;
    static final int REQ_RECEIPT = 12;
    static final int REQ_CAMERA_PERMISSION = 13;

    DB db;
    Uri cameraUri;

    String[] cats = {"Продукты","Кафе и рестораны","Транспорт","Дом","Здоровье","Развлечения","Одежда","Связь","Другое"};
    String[] currencies = {"RUB ₽ — Российский рубль","BYN Br — Белорусский рубль","KZT ₸ — Казахстанский тенге","UAH ₴ — Украинская гривна","EUR € — Евро","USD $ — Доллар США","GBP £ — Фунт стерлингов"};

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        db = new DB(this);
        findViewById(R.id.expenses).setOnClickListener(v -> expenses());
        findViewById(R.id.projects).setOnClickListener(v -> projects());
        findViewById(R.id.reports).setOnClickListener(v -> reports());
        findViewById(R.id.currencyTop).setOnClickListener(v -> currencyInfo());
        expenses();
    }

    String code(String s) { return s.substring(0, 3); }
    String sym(String c) {
        if ("RUB".equals(c)) return "₽";
        if ("BYN".equals(c)) return "Br";
        if ("KZT".equals(c)) return "₸";
        if ("UAH".equals(c)) return "₴";
        if ("EUR".equals(c)) return "€";
        if ("USD".equals(c)) return "$";
        return "£";
    }

    TextView tv(String s, int z, boolean bold) {
        TextView x = new TextView(this);
        x.setText(s); x.setTextSize(z); x.setTextColor(Color.rgb(28,42,54));
        x.setPadding(16,10,16,10); if (bold) x.setTypeface(null,1); return x;
    }
    Button bt(String s) { Button x = new Button(this); x.setText(s); x.setAllCaps(false); return x; }
    void screen(View v) { FrameLayout f = findViewById(R.id.content); f.removeAllViews(); f.addView(v); }
    String today() { return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()); }
    String money(double n) { return String.format(Locale.getDefault(),"%.2f",n).replace('.',','); }

    void expenses() {
        ScrollView s = new ScrollView(this);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL); l.setPadding(12,12,12,12);
        l.addView(tv("Расходы",14,false));
        for (String[] x : db.byCur()) l.addView(tv(x[0] + "   " + x[1] + " " + sym(x[0]),25,true));
        l.addView(tv("Добавьте покупку по чеку или вручную",15,false));
        Button add = bt("＋  Добавить расход"); add.setOnClickListener(v -> addDialog()); l.addView(add);

        for (Expense e : db.all()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL); card.setPadding(10,8,10,8); card.setBackgroundColor(Color.WHITE);
            card.addView(tv(e.merchant.isEmpty() ? "Расход" : e.merchant,18,true));
            card.addView(tv(e.date + "   " + e.category,14,false));
            card.addView(tv(money(e.amount) + " " + sym(e.currency),21,true));
            if (e.note != null && !e.note.isEmpty()) card.addView(tv(e.note,13,false));
            Button ed = bt("Открыть и изменить"); ed.setOnClickListener(v -> editDialog(e)); card.addView(ed);
            l.addView(card); l.addView(tv("",4,false));
        }
        s.addView(l); screen(s);
    }

    void addDialog() {
        String[] methods = {"Сканировать чек камерой","Выбрать чек из галереи","Ввести вручную","Ввести голосом"};
        new AlertDialog.Builder(this).setTitle("Добавить расход").setItems(methods,(d,w) -> {
            if (w == 0) openCamera();
            else if (w == 1) openGallery();
            else editDialog(null);
        }).show();
    }

    void openCamera() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "receipts");
            dir.mkdirs();
            File f = new File(dir, "receipt_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, "ru.controlexpenses.fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) { Toast.makeText(this, "Не удалось открыть камеру: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    void openGallery() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_GALLERY);
    }

    void openReceipt(Uri uri) {
        Intent i = new Intent(this, ReceiptActivity.class);
        i.putExtra("image_uri", uri.toString());
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, REQ_RECEIPT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_CAMERA_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) openCamera();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQ_CAMERA && cameraUri != null) {
            openReceipt(cameraUri);
        } else if (requestCode == REQ_GALLERY && data != null && data.getData() != null) {
            Uri u = data.getData();
            try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            openReceipt(u);
        } else if (requestCode == REQ_RECEIPT && data != null) {
            String shop = data.getStringExtra("shop");
            String date = data.getStringExtra("date");
            String currency = data.getStringExtra("currency");
            String items = data.getStringExtra("items");
            double total = data.getDoubleExtra("total", 0);
            if (total > 0) {
                db.add(new Expense(0, date == null ? today() : date, shop == null ? "" : shop, "Продукты", "", items == null ? "" : items, total, currency == null ? "RUB" : currency));
                Toast.makeText(this, "Чек сохранён", Toast.LENGTH_SHORT).show();
                expenses();
            }
        }
    }

    void currencyInfo() {
        new AlertDialog.Builder(this).setTitle("Валюты покупок")
                .setMessage("Валюта сохраняется отдельно у каждой покупки.\n\nRUB ₽ — Российский рубль\nBYN Br — Белорусский рубль\nKZT ₸ — Казахстанский тенге\nUAH ₴ — Украинская гривна\nEUR € — Евро\nUSD $ — Доллар США\nGBP £ — Фунт стерлингов\n\nРазные валюты не складываются без пересчёта.")
                .setPositiveButton("Понятно",null).show();
    }

    void editDialog(Expense e) {
        boolean n = e == null;
        if (n) e = new Expense(0,today(),"","Другое","","",0,"RUB");
        LinearLayout l = new LinearLayout(this); l.setPadding(12,4,12,4); l.setOrientation(LinearLayout.VERTICAL);
        EditText date = f("Дата покупки",e.date), shop = f("Магазин / получатель",e.merchant), amount = f("Фактически уплачено",e.amount==0?"":money(e.amount)), note = f("Комментарий",e.note);
        Spinner cur = new Spinner(this); cur.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,currencies));
        int ci = 0; for (int i=0;i<currencies.length;i++) if (code(currencies[i]).equals(e.currency)) ci=i; cur.setSelection(ci);
        Spinner cat = new Spinner(this); cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,cats)); cat.setSelection(Math.max(0,Arrays.asList(cats).indexOf(e.category)));
        l.addView(date); l.addView(shop); l.addView(amount); l.addView(tv("Валюта покупки",14,true)); l.addView(cur); l.addView(cat); l.addView(note);
        Expense z = e;
        new AlertDialog.Builder(this).setTitle(n?"Новый расход":"Покупка").setView(l).setPositiveButton("Сохранить",(d,w)->{
            z.date=date.getText().toString(); z.merchant=shop.getText().toString(); z.amount=parse(amount.getText().toString());
            z.currency=code(String.valueOf(cur.getSelectedItem())); z.category=String.valueOf(cat.getSelectedItem()); z.note=note.getText().toString();
            if(n) db.add(z); else db.update(z); expenses();
        }).setNegativeButton("Отмена",null).show();
    }

    EditText f(String h,String v){ EditText x=new EditText(this); x.setHint(h); x.setText(v); return x; }
    double parse(String s){ try{return Double.parseDouble(s.replace(" ","").replace(',','.'));}catch(Exception e){return 0;} }

    void projects(){ LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(14,12,14,12);l.addView(tv("Проекты",25,true));Button a=bt("＋ Новый проект");a.setOnClickListener(v->{EditText e=f("Название проекта","");new AlertDialog.Builder(this).setTitle("Новый проект").setView(e).setPositiveButton("Создать",(d,w)->{db.project(e.getText().toString());projects();}).setNegativeButton("Отмена",null).show();});l.addView(a);for(String p:db.projects())l.addView(tv(p,18,true));screen(l); }
    void reports(){ ScrollView s=new ScrollView(this);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(14,12,14,12);l.addView(tv("Отчёты",25,true));l.addView(tv("Итоги по валютам",17,false));for(String[] x:db.byCur())l.addView(tv(x[0]+" — "+x[1]+" "+sym(x[0]),20,true));l.addView(tv("Расходы разделены по валютам, чтобы не складывать разные валюты без курса.",14,false));s.addView(l);screen(s); }
}
