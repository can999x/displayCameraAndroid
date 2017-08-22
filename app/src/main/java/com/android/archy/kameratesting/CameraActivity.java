package com.android.archy.kameratesting;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

//implements SurfaceHolder.callback Harus ada surfaceCreated, surfaceChange, surfaceDestroyed
//implements SurfaceHolder.callback must have surfaceCreated, surfaceChange, surfaceDestroyed
public class CameraActivity extends Activity implements SurfaceHolder.Callback{

    private Camera cameraHardware;
    private SurfaceView cameraSurfaceView;
    private ImageButton flipImageButton;
    private ImageButton flashLightButton;
    private ImageButton backButton;
    private ImageButton cropButton;
    private ImageButton takePictureImageButton;
    private ImageButton timerButton;
    private TextView delayTimeTextView;
    private View topForScallingView;
    private View bottomForScallingView;
    private LinearLayout bottomMenuButtonLayout;

    private SurfaceHolder surfaceHolder;
    private int cameraId =0;        //nanti untuk cek depan atau belakang       //checking front camera or back camera
    private boolean isPreviewRunning=false;     //cek, apakah kamera sudah pernah di nyalakan //check camera that was ever used
    private int heightCamera;
    private int widthCamera;
    private int flashNumber = 0 ;           //0 = flash off, 1 = flash on, 2 = auto flash
    private int cropValidation =0;          //0 = dont get crop yet, 1 = cropped       this will be used on take picture        //ini akan digunakan saat mengambil kamera
    private int animHeight;             //height of animation (black one)       //tinggi dari animasi (yg hitam)
    float distance = 0;
    private int delayTime;
    private int delayTimeTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
        initData();
        flipButtonClicked();
        flashLightButtonClicked();
        backButtonClicked();
        cropButtonClicked();
        delayTimeButtonClicked();
        takePictureButtonClicked();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraHardware == null) {
            cameraHardware = getCamera(cameraId);
            if (surfaceHolder!= null) {
                startPreviewCamera(cameraHardware, surfaceHolder);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Camera.Parameters parameters = cameraHardware.getParameters();
        int action = event.getAction();


        if(event.getPointerCount()>1)
        {
            if(action == MotionEvent.ACTION_POINTER_DOWN)
            {
                distance = getFingerSpacing(event);
            }else if(action == MotionEvent.ACTION_MOVE && parameters.isZoomSupported())
            {
                //AUTO FOCUS WHEN PINCH
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                handleZoom(event, parameters);
            }
        }else
        {
            if(action == MotionEvent.ACTION_UP)
            {
                //AUTOFOCUS WHEN TAP
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                handleFocus(event,parameters);
            }
        }
        return true;
    }

    private void handleFocus(MotionEvent event, final Camera.Parameters parameters)
    {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);

        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportFocusModes = parameters.getSupportedFocusModes();
        if(supportFocusModes != null && supportFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
        {
            cameraHardware.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                }
            });
        }
    }

    private float getFingerSpacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x*x+y*y);
    }

    private void handleZoom(MotionEvent event,Camera.Parameters parameters)
    {
        int maxZoom = parameters.getMaxZoom();
        int zoom = parameters.getZoom();
        float newDistance = getFingerSpacing(event);
        if(newDistance>distance)
        {
            if(zoom<maxZoom)
                zoom+=2;
        }else if(newDistance<distance)
        {
            if(zoom>0)
                zoom-=2;
        }
        distance = newDistance;
        parameters.setZoom(zoom);
        cameraHardware.setParameters(parameters);
    }

    private void takePictureButtonClicked()
    {
        takePictureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (delayTime == 0) {
                    if (cameraId == 0) {
                        flashLightOnOrOff();
                    }
                    captureImage();
                } else {
                    delayTimeTextView.setVisibility(View.VISIBLE);
                    delayTimeTextView.setText(String.valueOf(delayTime));

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            try {
                                while (delayTime != 0) {
                                    Thread.sleep(1000);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(delayTime==0)
                                            {
                                                delayTimeTextView.setVisibility(View.GONE);
                                              captureImage();
                                            }else {
                                                delayTime--;
                                                delayTimeTextView.setText(String.valueOf(delayTime));
                                            }
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                            }
                        }
                    };

                    t.start();
                }
            }
        });
    }


    private void captureImage()
    {
        //for best quality
        // FOR API 18++
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Camera.Parameters parameters = cameraHardware.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            Camera.Size size = sizes.get(0);
            for (int i = 0; i < sizes.size(); i++) {
                if (sizes.get(i).width > size.width)
                    size = sizes.get(i);
            }
            parameters.setJpegQuality(100);
            parameters.setPictureSize(size.width, size.height);
            parameters.setJpegThumbnailQuality(100);
            parameters.setJpegThumbnailSize(size.width, size.height);
            cameraHardware.setParameters(parameters);
        }

        cameraHardware.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId,info);
                Matrix matrix = new Matrix();
                if(cameraId ==1)
                    matrix.postScale(-1,1);

                if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0) {
                    matrix.postRotate(90);
                }
                if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90) {
                    matrix.postRotate(0);
                }
                if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_180) {
                    matrix.postRotate(270);
                }
                if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270) {
                    matrix.postRotate(180);
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(), bitmap.getHeight(),  matrix, true);
                bitmap = Bitmap.createScaledBitmap(bitmap,bitmap.getWidth(),bitmap.getHeight(),false);
                if(cropValidation==1)
                {
                    animHeight = (bitmap.getHeight() - bitmap.getWidth())/3;
                    bitmap = Bitmap.createBitmap(bitmap,0,animHeight,bitmap.getWidth(),bitmap.getWidth());
                }else
                {
                    bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight());
                }

                String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()+ File.separator+System.currentTimeMillis()+".jpeg";

                File file = new File(path);
                File tempPath = new File(file.getParent());
                if (!tempPath.exists()) {
                    tempPath.mkdirs();
                }
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                        out.flush();
                        out.close();
                    }
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{path}, null, null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(!bitmap.isRecycled()){
                    bitmap.recycle();
                }
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                releaseCamera();
            }
        });

    }

    private void flashLightOnOrOff()
    {
        if (cameraHardware == null) {
            return;
        }
        Camera.Parameters parameters = cameraHardware.getParameters();
        if (parameters == null) {
            return;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }
        String flashMode = parameters.getFlashMode();
        switch (flashNumber) {

            case 0:
                if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
                    // Turn on the flash
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        cameraHardware.setParameters(parameters);
                    }
                }
                    break;
            case 1:
                if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)) {
                    // Turn on the flash
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        cameraHardware.setParameters(parameters);
                    }
                }
                break;
            case 2:
                if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                    // Turn off the flash
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        cameraHardware.setParameters(parameters);
                    }
                }
                break;
        }
    }

    private void cropButtonClicked()
    {
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(cropValidation)
                {
                    case 0:
                        ValueAnimator anim = ValueAnimator.ofInt(0, animHeight);
                        anim.setDuration(300);
                        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                int currentValue = Integer.parseInt(animation.getAnimatedValue().toString());
                                RelativeLayout.LayoutParams Params = new RelativeLayout.LayoutParams(widthCamera, currentValue);
                                topForScallingView.setLayoutParams(Params);

                                RelativeLayout.LayoutParams bottomParams = new RelativeLayout.LayoutParams(widthCamera, currentValue);
                                bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                                bottomForScallingView.setLayoutParams(bottomParams);
                            }

                        });
                        anim.start();
                        topForScallingView.bringToFront();
                        bottomForScallingView.bringToFront();
                        flipImageButton.bringToFront();
                        flashLightButton.bringToFront();
                        bottomMenuButtonLayout.bringToFront();
                        cropValidation++;
                        break;
                    case 1:
                        ValueAnimator animDown = ValueAnimator.ofInt(animHeight,0 );
                        animDown.setDuration(300);
                        animDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                int currentValue = Integer.parseInt(animation.getAnimatedValue().toString());
                                RelativeLayout.LayoutParams Params = new RelativeLayout.LayoutParams(widthCamera, currentValue);
                                topForScallingView.setLayoutParams(Params);

                                RelativeLayout.LayoutParams bottomParams = new RelativeLayout.LayoutParams(widthCamera, currentValue);
                                bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                                bottomForScallingView.setLayoutParams(bottomParams);
                            }

                        });
                        animDown.start();
                        flipImageButton.bringToFront();
                        flashLightButton.bringToFront();
                        cropButton.bringToFront();
                        cropValidation = 0;
                        break;
                }
            }
        });
    }

    private void delayTimeButtonClicked()
    {
        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (delayTime)
                {
                    case 0:
                        delayTime = 3;
                        delayTimeTemp = delayTime;
                        timerButton.setImageResource(R.drawable.ic_timer_3_sec);
                        break;
                    case 3:
                        delayTime = 10;
                        delayTimeTemp = delayTime;
                        timerButton.setImageResource(R.drawable.ic_timer_10_sec);
                        break;
                    case 10:
                        delayTime = 0;
                        delayTimeTemp = delayTime;
                        timerButton.setImageResource(R.drawable.ic_timer_off);
                        break;
                }
            }
        });
    }

    private void backButtonClicked()
    {
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void flashLightButtonClicked()
    {
        flashLightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera.Parameters parameters = cameraHardware.getParameters();
                switch (flashNumber)
                {
                    case 0:
                        //when the flash off (condition first time)
                        //ketika flash mati (kondisi pertama)
                        flashNumber = 1;
                        flashLightButton.setImageResource(R.drawable.ic_flash_on);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);   //if you developing, recommend using torch, but i prefer using FLASH_MODE_ON
                        cameraHardware.setParameters(parameters);
                        break;
                    case 1:
                        //when the flash on
                        //ketika flash hidup
                        flashNumber = 2;
                        flashLightButton.setImageResource(R.drawable.ic_flash_auto);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        cameraHardware.setParameters(parameters);
                        break;
                    case 2:
                        //when set flash auto mode
                        //ketika flashnya auto
                        flashNumber = 0;
                        flashLightButton.setImageResource(R.drawable.ic_flash_off);
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        cameraHardware.setParameters(parameters);
                        break;
                }
            }
        });
    }

    private void flipButtonClicked()
    {
        flipImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //setFunction
                releaseCamera();
                cameraId = (cameraId + 1) % cameraHardware.getNumberOfCameras();
                cameraHardware = getCamera(cameraId);
                if (surfaceHolder != null) {
                    startPreviewCamera(cameraHardware, surfaceHolder);
                }

                //setView
                if(cameraId == 1)
                {
                    //kalau sudah di depan
                    //if camera = front
                    flipImageButton.setImageResource(R.drawable.ic_flip_to_back);
                    flashLightButton.setVisibility(View.GONE);
                }else
                {
                    //kalau sudah di belakang
                    //if camera = back
                    flipImageButton.setImageResource(R.drawable.ic_flip_to_front);
                    flashLightButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void releaseCamera() {
        if (cameraHardware != null) {
            cameraHardware.setPreviewCallback(null);
            cameraHardware.stopPreview();
            cameraHardware.release();
            cameraHardware = null;
        }
    }

    private void initView()
    {
        cameraSurfaceView = (SurfaceView) findViewById(R.id.camera_surface_view);
        flipImageButton = (ImageButton) findViewById(R.id.flip_image_button);
        flashLightButton = (ImageButton) findViewById(R.id.flash_light_button);
        backButton = (ImageButton) findViewById(R.id.back_button);
        cropButton = (ImageButton) findViewById(R.id.crop_button);
        topForScallingView = (View) findViewById(R.id.top_for_scalling_view);
        bottomForScallingView = (View) findViewById(R.id.bottom_for_scalling_view);
        bottomMenuButtonLayout = (LinearLayout) findViewById(R.id.bottom_menu_button_layout);
        takePictureImageButton = (ImageButton) findViewById(R.id.take_picture_image_button);
        timerButton = (ImageButton) findViewById(R.id.timer_button);
        delayTimeTextView = (TextView) findViewById(R.id.delay_time_text_view);


        surfaceHolder = cameraSurfaceView.getHolder();
        if (cameraHardware== null) {
            cameraHardware = getCamera(cameraId);
        }else
        {
            //jika tidak ada kamera
            //if there is no camera
            Toast.makeText(this,"this device doesn't support Camera",Toast.LENGTH_SHORT).show();
        }
        surfaceHolder.addCallback(this);
        //inisiasi kamera
        //initiate camera
    }

    private void initData()
    {
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        widthCamera = dm.widthPixels;
        heightCamera = dm.heightPixels;
        delayTime = 0;

        //tinggi yang warna hitam waktu di klik
        //height of black things that occurs
        animHeight = (heightCamera - widthCamera)/2;
    }

    private Camera getCamera(int id)
    {
        Camera camera = null;
        try{
            //kamera siap dipakai
            //camera ready to be used
            camera = Camera.open(id);           //1 == depan, 0 == belakang              //1 == front, 0 == back
        }catch (Exception e)
        {}
        return camera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startPreviewCamera(cameraHardware,surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    private void startPreviewCamera(Camera cameraHardware, SurfaceHolder holder)
    {
        try
        {
            setUpCamera(cameraHardware);
            cameraHardware.setPreviewDisplay(holder);
            cameraHardware.startPreview();
            isPreviewRunning = true;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private void setUpCamera(Camera cameraHardware)
    {
        Camera.Parameters parameters = cameraHardware.getParameters();
        //disini kita bisa nambahin autofocus, flip camera,flash, dan sebagainya
        //here you can add feature autofocus, flip camera, or flash or whatever you want
        if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        {
            //karna autofocus continous autofocusnya jalan terus, kalau auto focus, cuman sekali
            //because autofocus continous keep focusing, and autofocus only doing one time
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        parameters.setPreviewSize(heightCamera,widthCamera);
//        set default portrait orientation =90
        cameraHardware.setDisplayOrientation(90);
        cameraHardware.setParameters(parameters);
    }
}
