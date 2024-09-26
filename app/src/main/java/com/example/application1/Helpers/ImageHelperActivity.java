package com.example.application1.Helpers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.application1.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImageHelperActivity extends AppCompatActivity {
    public final static int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 1064;
    public final static int REQUEST_READ_EXTERNAL_STORAGE = 2031;
    private static final int CAMERA_PERMISSION_CODE = 100;

    File photoFile;

    private ImageView inputImageView;
    private TextView outputTextView;

    private ImageLabeler imageLabeler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_helper);

        inputImageView = findViewById(R.id.inputImageView);
        outputTextView = findViewById(R.id.outputTextView);

        imageLabeler = ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build());

        // Check for read storage permission
        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_READ_EXTERNAL_STORAGE);
        }

        // Check for camera permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the camera
                Log.d("Naman", "Camera permission granted.");
            } else {
                // Permission denied
                Log.d("Naman", "Camera permission denied.");
                outputTextView.setText("Camera permission denied");
            }
        }

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Naman", "Read storage permission granted.");
            } else {
                Log.d("Naman", "Read storage permission denied.");
                outputTextView.setText("Read storage permission denied");
            }
        }
    }

    public void onPickImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        startActivityForResult(intent, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    public void onStartCamera(View view) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            photoFile = createPhotoFile();
            Uri fileUri = FileProvider.getUriForFile(this, "com.example.application1", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(intent, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            // Permission not granted
            Log.d("Naman", "Camera permission not granted");
            outputTextView.setText("Please grant camera permission");
        }
    }


    private File createPhotoFile() {
        File photoFileDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ML_IMAGE_HELPER");
        if (!photoFileDir.exists()) {
            photoFileDir.mkdir();
        }
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(photoFileDir.getPath() + File.separator + name);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE) {
                Uri uri = data.getData();
                Bitmap bitmap = loadFromUri(uri);
                inputImageView.setImageBitmap(bitmap);
                runClassification(bitmap);

            }
            if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                inputImageView.setImageBitmap(bitmap);
                runClassification(bitmap);

            }
        }
    }

    private Bitmap loadFromUri(Uri uri) {
        Bitmap bitmap = null;
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
            bitmap = ImageDecoder.decodeBitmap(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void runClassification(Bitmap bitmap) {
        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        imageLabeler.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(List<ImageLabel> imageLabels) {
                if (!imageLabels.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    for (ImageLabel label : imageLabels) {
                        builder.append(label.getText()).append(" : ").append(label.getConfidence()).append("\n");

                    }
                    outputTextView.setText(builder.toString());


                } else {
                    outputTextView.setText("Could not Classify");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

    }
}