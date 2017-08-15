package com.android.archy.kameratesting;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;

//implements SurfaceHolder.callback Harus ada surfaceCreated, surfaceChange, surfaceDestroyed
//implements SurfaceHolder.callback must have surfaceCreated, surfaceChange, surfaceDestroyed
public class CameraActivity extends Activity implements SurfaceHolder.Callback{

    private Camera cameraHardware;
    private SurfaceView cameraSurfaceView;
    private ImageButton flipImageButton;
    private ImageButton flashLightButton;
    private ImageButton backButton;
    private ImageButton cropButton;
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
        heightCamera= dm.heightPixels;

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



        //portrait normal
        if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0)
        {
//            parameters.setPreviewSize(heightCamera, widthCamera);
            cameraHardware.setDisplayOrientation(90);
        }

        //Landscape normal
        if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90)
        {
//            parameters.setPreviewSize(widthCamera, heightCamera);
            cameraHardware.setDisplayOrientation(0);
        }

        //Landscape terbalik
        //inverse Landscape
        if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_180)
        {
//            parameters.setPreviewSize(heightCamera, widthCamera);
            cameraHardware.setDisplayOrientation(0);
        }

        //kalau terbalik portrait
        //inverse Portrait
        if(getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270)
        {
//            parameters.setPreviewSize(widthCamera, heightCamera);
            cameraHardware.setDisplayOrientation(180);
        }
        cameraHardware.setParameters(parameters);
    }
}
