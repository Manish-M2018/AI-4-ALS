package com.example.face_det;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.face_det.Fragments.Meal_time.Calls.Call_person1;
import com.example.face_det.Fragments.Meal_time.Calls.Call_person2;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag1;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag2;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag3;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag4;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag5;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag6;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag7;
import com.example.face_det.Fragments.Meal_time.Meal_time.Frag8;
import com.example.face_det.Fragments.Meal_time.Room_related.Room1;
import com.example.face_det.Fragments.Meal_time.Room_related.Room2;
import com.example.face_det.Fragments.Meal_time.Room_related.Room3;
import com.example.face_det.Fragments.Meal_time.Room_related.Room4;
import com.example.face_det.Fragments.Meal_time.Yes_or_no.No;
import com.example.face_det.Fragments.Meal_time.Yes_or_no.Yes;
import com.example.face_det.Helper.GraphicOverlay;
import com.example.face_det.Helper.RectOverlay;
import com.example.face_det.Index_of_phrases.Index0;
import com.example.face_det.Index_of_phrases.Index1;
import com.example.face_det.Index_of_phrases.Index2;
import com.example.face_det.Index_of_phrases.Index3;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    GraphicOverlay graphicOverlay;
    Button btnDetect;
    ViewPager pager;
    TextToSpeech tts;
    TextView text;
    int result;
    String[] items;

    AlertDialog waitingDialog;
    private static final int REQUEST_CODE_FOR_CALL = 1;

    float lefteyeopenprobability,righteyeopenprobability,smileProb;

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(tts != null)
        {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Init view
        cameraView = findViewById(R.id.camera_view);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        btnDetect = findViewById(R.id.btn_detect);
        items = getResources().getStringArray(R.array.Items);

        cameraView.setFacing(CameraKit.Constants.FACING_FRONT);

        waitingDialog = new SpotsDialog.Builder().setContext(this)
                .setMessage("Please Wait")
                .setCancelable(false)
                .build();

        //Speaking out
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS)
                {
                    result = tts.setLanguage(Locale.US);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Feature not supported on your device",Toast.LENGTH_LONG).show();
                }
            }
        });

        //event
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startCamera();
                Timer timer = new Timer();
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        startCamera();
                    }
                }, 2000, 3000);
            }
        });

        List<Fragment> fragments = getFragments();

        MyPageAdapter pageAdapter = new MyPageAdapter(getSupportFragmentManager(), fragments);
        pager = findViewById(R.id.pager);
        pager.setAdapter(pageAdapter);



        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDialog.show();

                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);
                cameraView.stop();

                runFaceDetector(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {
//                File video = cameraKitVideo.getVideoFile();

            }
        });
    }

    public void startCamera() {
        cameraView.start();
        cameraView.captureImage();
        graphicOverlay.clear();

    }

    private List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<>();
        fList.add(new Index0());
        fList.add(new Index1());
        fList.add(new Index2());
        fList.add(new Index3());
        fList.add(new Call_person1());
        fList.add(new Call_person2());
        fList.add(new Frag1());
        fList.add(new Frag2());
        fList.add(new Frag3());
        fList.add(new Frag4());
        fList.add(new Frag5());
        fList.add(new Frag6());
        fList.add(new Frag7());
        fList.add(new Frag8());
        fList.add(new Room1());
        fList.add(new Room2());
        fList.add(new Room3());
        fList.add(new Room4());
        fList.add(new Yes());
        fList.add(new No());

        return fList;
    }

    private class MyPageAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;

        public MyPageAdapter(FragmentManager fm, List<Fragment> fragments ) {
            super(fm);
            this.fragments = fragments;
        }
        @Override
        public Fragment getItem(int position)
        {
            return this.fragments.get(position);
        }

        @Override
        public int getCount()
        {
            return this.fragments.size();
        }
    }

    private void runFaceDetector(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build();

        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {
                        processFaceResult(firebaseVisionFaces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void processFaceResult(List<FirebaseVisionFace> firebaseVisionFaces) {
        int count = 0;
        for(FirebaseVisionFace face: firebaseVisionFaces)
        {
            Rect bounds = face.getBoundingBox();
            //Draw Rectangle
            RectOverlay rect = new RectOverlay(graphicOverlay,bounds);
            graphicOverlay.add(rect);

            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
            if (leftEar != null) {
                FirebaseVisionPoint leftEarPos = leftEar.getPosition();
            }

            // If contour detection was enabled:
            List<FirebaseVisionPoint> leftEyeContour =
                    face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints();
            List<FirebaseVisionPoint> upperLipBottomContour =
                    face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints();

            // If classification was enabled:
            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                smileProb = face.getSmilingProbability();
            }
            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                righteyeopenprobability = face.getRightEyeOpenProbability();
            }
            if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                lefteyeopenprobability = face.getLeftEyeOpenProbability();
            }

            // If face tracking was enabled:
            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
                int id = face.getTrackingId();
            }

            count++;
        }
        waitingDialog.dismiss();
//        Toast.makeText(this,"count: "+count+" left: "+lefteyeopenprobability+" right: "+righteyeopenprobability+" Smileeee "+ smileProb,Toast.LENGTH_LONG).show();
        if (lefteyeopenprobability<0.8 && righteyeopenprobability<0.8) {
            if (pager.getCurrentItem()<=3){
                switch (pager.getCurrentItem()){
                    case 0:
                        pager.setCurrentItem(4);
                        break;
                    case 1:
                        pager.setCurrentItem(6);
                        break;
                    case 2:
                        pager.setCurrentItem(14);
                        break;
                    case 3:
                        pager.setCurrentItem(18);
                        break;
                }
            }else if(pager.getCurrentItem()==4 || pager.getCurrentItem()==5){
                makePhoneCall();
            }else {
                speakOut(items[pager.getCurrentItem()-3]);
            }
        } else if (righteyeopenprobability < 0.8) {
            if (pager.getCurrentItem()+1 == 20) {
                Toast.makeText(getApplicationContext(),"Reached the end of the pages!",Toast.LENGTH_SHORT).show();
            }else {
                pager.setCurrentItem(pager.getCurrentItem() + 1);
            }

        } else if(lefteyeopenprobability < 0.8){
            if (pager.getCurrentItem()-1 == -1) {
                Toast.makeText(getApplicationContext(),"Reached the end of the pages!",Toast.LENGTH_SHORT).show();
            }else {
                pager.setCurrentItem(pager.getCurrentItem() - 1);
            }
        }
    }


    void makePhoneCall() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            //Make a call
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:6360056579"));
            startActivity(callIntent);
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE},REQUEST_CODE_FOR_CALL);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FOR_CALL) {
            if (grantResults.length>0 &&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                makePhoneCall();
            }else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void speakOut(String str) {
        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA){
            Toast.makeText(getApplicationContext(),"Language not supported on your device",Toast.LENGTH_LONG).show();
        }
        else {
            tts.speak(str,TextToSpeech.QUEUE_FLUSH,null);
        }
    }
}
