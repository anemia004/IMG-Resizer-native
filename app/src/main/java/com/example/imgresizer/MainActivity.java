package com.example.imgresizer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1;

    private ImageView imgOriginal, imgProcessed;
    private TextView tvOriginalInfo, tvStatus;
    private EditText etWidth, etHeight, etTargetKB;
    private CheckBox cbAspectRatio;
    private RadioGroup rgMode;
    private Spinner spFormat;
    private SeekBar sbQuality;
    private Button btnPick, btnResize, btnCompress, btnDownload;
    private View layoutDimensions, layoutFileSize, layoutQuality, windowBody;
    private Button btnMinimize, btnMaximize, btnClose;

    private Bitmap originalBitmap = null;
    private Bitmap processedBitmap = null;
    private int originalWidth, originalHeight;
    private Uri originalUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        imgOriginal = findViewById(R.id.imgOriginal);
        imgProcessed = findViewById(R.id.imgProcessed);
        tvOriginalInfo = findViewById(R.id.tvOriginalInfo);
        tvStatus = findViewById(R.id.tvStatus);
        etWidth = findViewById(R.id.etWidth);
        etHeight = findViewById(R.id.etHeight);
        etTargetKB = findViewById(R.id.etTargetKB);
        cbAspectRatio = findViewById(R.id.cbAspectRatio);
        rgMode = findViewById(R.id.rgMode);
        spFormat = findViewById(R.id.spFormat);
        sbQuality = findViewById(R.id.sbQuality);
        btnPick = findViewById(R.id.btnPickImage);
        btnResize = findViewById(R.id.btnResize);
        btnCompress = findViewById(R.id.btnCompress);
        btnDownload = findViewById(R.id.btnDownload);
        layoutDimensions = findViewById(R.id.layoutDimensions);
        layoutFileSize = findViewById(R.id.layoutFileSize);
        layoutQuality = findViewById(R.id.layoutQuality);
        windowBody = findViewById(R.id.window_body);
        btnMinimize = findViewById(R.id.btn_minimize);
        btnMaximize = findViewById(R.id.btn_maximize);
        btnClose = findViewById(R.id.btn_close);

        // Window button actions
        btnMinimize.setOnClickListener(v -> {
            windowBody.setVisibility(windowBody.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });
        btnMaximize.setOnClickListener(v -> {
            if (getWindow().getDecorView().getSystemUiVisibility() == 0) {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        });
        btnClose.setOnClickListener(v -> clearAll());

        // Pick image – no permission required
        btnPick.setOnClickListener(v -> openImagePicker());

        // Mode switch
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            layoutDimensions.setVisibility(checkedId == R.id.rbDimensions ? View.VISIBLE : View.GONE);
            layoutFileSize.setVisibility(checkedId == R.id.rbFileSize ? View.VISIBLE : View.GONE);
        });

        // Resize button
        btnResize.setOnClickListener(v -> resizeImage());

        // Compress button
        btnCompress.setOnClickListener(v -> compressToFileSize());

        // Download button
        btnDownload.setOnClickListener(v -> saveImage());

        // Format spinner
        spFormat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                boolean show = pos != 0; // not PNG
                layoutQuality.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                originalUri = uri;
                loadOriginalImage(uri);
            }
        }
    }

    private void loadOriginalImage(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("img_orig_", ".jpg", getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();

            long fileSize = tempFile.length();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(tempFile.getAbsolutePath(), opts);
            originalWidth = opts.outWidth;
            originalHeight = opts.outHeight;

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, 1024, 1024);
            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), opts);
            originalBitmap = bitmap;
            imgOriginal.setImageBitmap(bitmap);

            tvOriginalInfo.setText(String.format("Original: %d × %d | %.1f KB",
                    originalWidth, originalHeight, fileSize / 1024.0));
            tvStatus.setText("Image loaded.");

            etWidth.setText(String.valueOf(originalWidth));
            etHeight.setText(String.valueOf(originalHeight));

            processedBitmap = null;
            imgProcessed.setImageBitmap(null);
            btnDownload.setEnabled(false);
            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void resizeImage() {
        if (originalBitmap == null) { Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show(); return; }
        try {
            int targetW = Integer.parseInt(etWidth.getText().toString());
            int targetH = Integer.parseInt(etHeight.getText().toString());
            if (cbAspectRatio.isChecked()) {
                float ratio = (float) originalWidth / originalHeight;
                if (targetW != originalWidth) {
                    targetH = Math.round(targetW / ratio);
                    etHeight.setText(String.valueOf(targetH));
                } else if (targetH != originalHeight) {
                    targetW = Math.round(targetH * ratio);
                    etWidth.setText(String.valueOf(targetW));
                }
            }
            Bitmap resized = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true);
            processedBitmap = resized;
            imgProcessed.setImageBitmap(resized);
            btnDownload.setEnabled(true);
            tvStatus.setText(String.format("Resized to %d×%d", targetW, targetH));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid dimensions", Toast.LENGTH_SHORT).show();
        }
    }

    private void compressToFileSize() {
        if (originalBitmap == null) { Toast.makeText(this, "No image", Toast.LENGTH_SHORT).show(); return; }
        try {
            int targetKB = Integer.parseInt(etTargetKB.getText().toString());
            int targetBytes = targetKB * 1024;
            int fmtIndex = spFormat.getSelectedItemPosition();
            Bitmap.CompressFormat format;
            if (fmtIndex == 1) format = Bitmap.CompressFormat.JPEG;
            else if (fmtIndex == 2) format = Bitmap.CompressFormat.WEBP;
            else {
                Toast.makeText(this, "PNG is lossless – cannot target size", Toast.LENGTH_SHORT).show();
                return;
            }

            int w = originalWidth, h = originalHeight;
            long originalFileSize = getFileSize(originalUri);
            if (originalFileSize > 0 && originalFileSize > targetBytes) {
                double ratio = (double) targetBytes / originalFileSize;
                double scale = Math.sqrt(ratio) * 0.85;
                scale = Math.max(0.01, Math.min(1, scale));
                w = Math.max(1, (int)(originalWidth * scale));
                h = (int)Math.round(w / ((double)originalWidth / originalHeight));
                if (h < 1) { h = 1; w = (int)Math.round(h * ((double)originalWidth / originalHeight)); }
            }

            Bitmap bestBitmap = null;
            int bestQuality = 0;
            for (int attempt = 0; attempt < 3; attempt++) {
                Bitmap scaled = Bitmap.createScaledBitmap(originalBitmap, w, h, true);
                int lo = 10, hi = 100, bestQ = 0;
                Bitmap bestB = null;
                for (int i = 0; i < 8; i++) {
                    int mid = (lo + hi) / 2;
                    byte[] bytes = bitmapToByteArray(scaled, format, mid);
                    if (bytes.length <= targetBytes) {
                        if (bestB != null) bestB.recycle();
                        bestB = scaled.copy(scaled.getConfig(), false);
                        bestQ = mid;
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                }
                scaled.recycle();
                if (bestB != null) {
                    bestBitmap = bestB;
                    bestQuality = bestQ;
                    break;
                }
                w = Math.max(1, (int)(w * 0.8));
                h = (int)Math.round(w / ((double)originalWidth / originalHeight));
                if (h < 1) { h = 1; w = (int)Math.round(h * ((double)originalWidth / originalHeight)); }
            }

            if (bestBitmap != null) {
                processedBitmap = bestBitmap;
                imgProcessed.setImageBitmap(bestBitmap);
                btnDownload.setEnabled(true);
                int finalSize = bitmapToByteArray(bestBitmap, format, bestQuality).length;
                tvStatus.setText(String.format("Compressed to %.1f KB (q=%d%%, %d×%d)",
                        finalSize / 1024.0, bestQuality, bestBitmap.getWidth(), bestBitmap.getHeight()));
            } else {
                Toast.makeText(this, "Could not reach target size", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid target KB", Toast.LENGTH_SHORT).show();
        }
    }

    private long getFileSize(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            return in.available();
        } catch (Exception e) { return 0; }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        bitmap.compress(format, quality, out);
        return out.toByteArray();
    }

    private void saveImage() {
        if (processedBitmap == null) { Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show(); return; }
        try {
            int fmtIndex = spFormat.getSelectedItemPosition();
            Bitmap.CompressFormat format;
            String ext;
            if (fmtIndex == 0) { format = Bitmap.CompressFormat.PNG; ext = "png"; }
            else if (fmtIndex == 1) { format = Bitmap.CompressFormat.JPEG; ext = "jpg"; }
            else { format = Bitmap.CompressFormat.WEBP; ext = "webp"; }

            int quality = sbQuality.getProgress();
            if (format == Bitmap.CompressFormat.PNG) quality = 100;
            String fileName = "resized_" + System.currentTimeMillis() + "." + ext;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "image/" + ext);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    processedBitmap.compress(format, quality, out);
                    out.close();
                    Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File file = new File(downloadsDir, fileName);
                FileOutputStream out = new FileOutputStream(file);
                processedBitmap.compress(format, quality, out);
                out.close();
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), fileName, null);
                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAll() {
        originalBitmap = null;
        processedBitmap = null;
        originalUri = null;
        imgOriginal.setImageBitmap(null);
        imgProcessed.setImageBitmap(null);
        tvOriginalInfo.setText("No image loaded.");
        tvStatus.setText("Ready.");
        etWidth.setText("800");
        etHeight.setText("600");
        etTargetKB.setText("100");
        btnDownload.setEnabled(false);
        File cacheDir = getCacheDir();
        if (cacheDir != null) {
            for (File f : cacheDir.listFiles()) f.delete();
        }
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
    }
}
