package com.example.textrecoginition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private ImageView imageView;
    private Bitmap bitmap;
    private FirebaseVisionImage firebaseVisionImage;
    private FirebaseVisionTextRecognizer textRecognizer;
    private TextView resultDisplayTV;
    GraphicOverlay mGraphicOverlay;
    private Button chooseImage, searchText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Getting ID's
        imageView = findViewById(R.id.text_image);
        resultDisplayTV = findViewById(R.id.results_tv);
        chooseImage = findViewById(R.id.add_img_btn);
        searchText = findViewById(R.id.search_text_btn);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);



        // Choose Img
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    } else {

                        bringImagePicker();
                    }
                } else {

                    bringImagePicker();
                }
            }
        });


        // Search Text
        searchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultDisplayTV.setText("");

                // Getting bitmap from Imageview
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                bitmap = drawable.getBitmap();

                runTextRecoginition();
            }
        });

    }

    private void runTextRecoginition() {

        firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        // Running Model on Bitmap
        textRecognizer.processImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {

                        chooseImage.setEnabled(true);
                        searchText.setEnabled(true);

                        processTextRecognitionResult(firebaseVisionText);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                chooseImage.setEnabled(false);
                searchText.setEnabled(false);
                resultDisplayTV.setText("Error : "+e.getMessage());
            }
        });


    }

    private void processTextRecognitionResult(FirebaseVisionText firebaseVisionText) {

        String resultText = firebaseVisionText.getText();

        Log.d(TAG+"Result Text", resultText);
        resultDisplayTV.setText("Result Text: "+resultText);

        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        if(blocks.size() == 0){
            resultDisplayTV.setText("No Text Found");
            return;
        }
        mGraphicOverlay.clear();
        for(int i=0; i<blocks.size(); i++){
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for(int j=0; j<lines.size(); j++){
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for(int k=0; k<elements.size(); k++){
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);
                }
            }
        }
    }

    private void bringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1)
                .setCropShape(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? CropImageView.CropShape.RECTANGLE : CropImageView.CropShape.OVAL)
                .start(MainActivity.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                resultDisplayTV.setText("");
                mGraphicOverlay.clear();
                imageView.setImageURI(result.getUri());


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                resultDisplayTV.setText("Error Loading Image: "+error);

            }
        }
    }

}
