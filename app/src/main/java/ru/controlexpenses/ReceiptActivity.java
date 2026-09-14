package ru.controlexpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptActivity extends Activity {
    private ImageView imageView;
    private TextView status;
    private EditText shopEdit;
    private EditText dateEdit;
    private LinearLayout itemsBox;
    private Spinner currencySpinner;
    private Uri imageUri;
    private final ArrayList<ReceiptItem> items = new ArrayList<>();
    private final String[] currencies = {
            "RUB ₽", "BYN Br", "KZT ₸", "UAH ₴", "EUR €", "USD $", "GBP £"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        String uri = getIntent().getStringExtra("image_uri");
        if (uri != null && !uri.isEmpty()) {
            imageUri = Uri.parse(uri);
            loadImage();
        } else {
            status.setText("Изображение чека не передано");
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xfff3f6f8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(12, 10, 12, 10);
        bar.setBackgroundColor(0xff17324d);
        Button back = button("Назад");
        back.setOnClickListener(v -> finish());
        TextView title = text("Чек", 22, true);
        title.setTextColor(0xffffffff);
        bar.addView(back, new LinearLayout.LayoutParams(-2, -2));
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(14, 12, 14, 20);

        imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(0xffffffff);
        content.addView(imageView, new LinearLayout.LayoutParams(-1, -2));

        status = text("Подготовка чека", 14, false);
        content.addView(status);

        shopEdit = field("Магазин", "");
        dateEdit = field("Дата покупки", today());
        content.addView(shopEdit);
        content.addView(dateEdit);

        content.addView(text("Валюта покупки", 14, true));
        currencySpinner = new Spinner(this);
        currencySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencies));
        content.addView(currencySpinner);

        content.addView(text("Позиции чека", 18, true));
        itemsBox = new LinearLayout(this);
        itemsBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(itemsBox);

        Button recognize = button("Распознать чек");
        recognize.setOnClickListener(v -> runOcr());
        content.addView(recognize);

        Button save = button("Сохранить чек");
        save.setOnClickListener(v -> saveReceipt());
        content.addView(save);

        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void loadImage() {
        try (InputStream in = getContentResolver().openInputStream(imageUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            imageView.setImageBitmap(bitmap);
            status.setText("Чек открыт полностью. Нажмите «Распознать чек».");
        } catch (Exception e) {
            status.setText("Не удалось открыть чек: " + e.getMessage());
        }
    }

    private void runOcr() {
        if (imageUri == null) return;
        status.setText("Распознаю русский текст…");
        new Thread(() -> {
            TessBaseAPI api = null;
            try {
                File base = new File(getFilesDir(), "tesseract");
                File tessdata = new File(base, "tessdata");
                tessdata.mkdirs();
                copyAsset("tessdata/rus.traineddata", new File(tessdata, "rus.traineddata"));
                copyAsset("tessdata/eng.traineddata", new File(tessdata, "eng.traineddata"));

                Bitmap bitmap;
                try (InputStream in = getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(in);
                }

                api = new TessBaseAPI();
                if (!api.init(base.getAbsolutePath(), "rus+eng")) throw new Exception("OCR не запустился");
                api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                api.setImage(bitmap);
                String raw = api.getUTF8Text();
                api.recycle();
                api = null;
                runOnUiThread(() -> parseReceipt(raw == null ? "" : raw));
            } catch (Exception e) {
                if (api != null) api.recycle();
                runOnUiThread(() -> status.setText("Ошибка OCR: " + e.getMessage()));
            }
        }).start();
    }

    private void copyAsset(String name, File target) throws Exception {
        if (target.exists() && target.length() > 100000) return;
        try (InputStream in = getAssets().open(name); FileOutputStream out = new FileOutputStream(target)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
        }
    }

    private void parseReceipt(String raw) {
        items.clear();
        itemsBox.removeAllViews();
        dateEdit.setText(findDate(raw));
        shopEdit.setText(findShop(raw));

        String[] lines = raw.split("\\r?\\n");
        Pattern paidPattern = Pattern.compile("(?i)^(.+?)\\s+(\\d+[,.]\\d{2})\\s*$");
        Pattern calcPattern = Pattern.compile("(?i)^(.+?)\\s+(\\d+[,.]\\d+)\\s*[xх*]\\s*(\\d+[,.]\\d+)\\s*(?:=\\s*)?(\\d+[,.]\\d{2})\\s*$");

        for (String source : lines) {
            String line = source.trim().replaceAll("\\s{2,}", " ");
            if (line.length() < 3 || skip(line)) continue;
            Matcher calc = calcPattern.matcher(line);
            if (calc.matches()) {
                addItem(calc.group(1), number(calc.group(4)));
                continue;
            }
            Matcher paid = paidPattern.matcher(line);
            if (paid.matches() && hasLetters(paid.group(1))) addItem(paid.group(1), number(paid.group(2)));
        }

        status.setText("Распознано позиций: " + items.size() + ". Проверьте данные перед сохранением.");
        renderItems();
    }

    private void addItem(String name, double paid) {
        String clean = name.replaceAll("(?i)\\b(цена|стоимость|сумма)\\b", "").trim();
        if (clean.length() < 2 || paid <= 0) return;
        items.add(new ReceiptItem(clean, paid));
    }

    private void renderItems() {
        itemsBox.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            ReceiptItem item = items.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(10, 8, 10, 8);
            row.setBackgroundColor(0xffffffff);
            EditText name = field("Товар", item.name);
            EditText paid = field("Фактически уплачено", format(item.paid));
            row.addView(name);
            row.addView(paid);
            itemsBox.addView(row);
        }
    }

    private void saveReceipt() {
        if (items.isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Нет распознанных позиций. Сохранить пустой чек нельзя.")
                    .setPositiveButton("Понятно", null).show();
            return;
        }
        for (int i = 0; i < itemsBox.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) itemsBox.getChildAt(i);
            items.get(i).name = ((EditText) row.getChildAt(0)).getText().toString();
            items.get(i).paid = number(((EditText) row.getChildAt(1)).getText().toString());
        }
        double total = 0;
        StringBuilder packed = new StringBuilder();
        for (ReceiptItem item : items) {
            if (item.paid <= 0) continue;
            total += item.paid;
            packed.append(item.name.replace("|", " ")).append("|").append(item.paid).append("\n");
        }
        Intent data = new Intent();
        data.putExtra("shop", shopEdit.getText().toString());
        data.putExtra("date", dateEdit.getText().toString());
        data.putExtra("currency", currencyCode());
        data.putExtra("total", total);
        data.putExtra("items", packed.toString());
        setResult(RESULT_OK, data);
        finish();
    }

    private String currencyCode() {
        String s = String.valueOf(currencySpinner.getSelectedItem());
        return s.length() >= 3 ? s.substring(0, 3) : "RUB";
    }

    private String findDate(String raw) {
        Matcher m = Pattern.compile("(\\d{2}[./-]\\d{2}[./-](?:\\d{2}|\\d{4}))").matcher(raw);
        return m.find() ? m.group(1).replace('/', '.').replace('-', '.') : today();
    }

    private String findShop(String raw) {
        for (String line : raw.split("\\r?\\n")) {
            String s = line.trim();
            if (s.length() >= 3 && s.length() <= 40 && hasLetters(s) && !skip(s)) return s;
        }
        return "";
    }

    private boolean skip(String s) {
        String q = s.toLowerCase(Locale.ROOT);
        return q.contains("итого") || q.contains("кассир") || q.contains("касса") || q.contains("фн ") || q.contains("фд ") || q.contains("фп ") || q.contains("налог") || q.contains("ндс") || q.contains("адрес") || q.contains("к оплате");
    }

    private boolean hasLetters(String s) { return s.matches(".*[A-Za-zА-Яа-яЁё].*"); }
    private double number(String s) { try { return Double.parseDouble(s.replace(" ", "").replace(',', '.')); } catch (Exception e) { return 0; } }
    private String format(double v) { return String.format(Locale.getDefault(), "%.2f", v).replace('.', ','); }
    private String today() { return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()); }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private TextView text(String s, int size, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setPadding(12, 8, 12, 8); if (bold) t.setTypeface(null, 1); return t; }
    private EditText field(String hint, String value) { EditText e = new EditText(this); e.setHint(hint); e.setText(value); return e; }

    static class ReceiptItem {
        String name;
        double paid;
        ReceiptItem(String n, double p) { name = n; paid = p; }
    }
}
