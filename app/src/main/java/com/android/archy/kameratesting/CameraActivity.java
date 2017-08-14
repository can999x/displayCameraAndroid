package com.android.archy.kameratesting;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

//implements SurfaceHolder.callback Harus ada surfaceCreated, surfaceChange, surfaceDestroyed
//implements SurfaceHolder.callback must have surfaceCreated, surfaceChange, surfaceDestroyed
public class CameraActivity extends Activity implements SurfaceHolder.Callback{

    private Camera cameraHardware;
    private SurfaceView cameraSurfaceView;
    private SurfaceHolder surfaceHolder;
    private int cameraId =0;        //nanti untuk cek depan atau belakang       //checking front camera or back camera
    private boolean isPreviewRunning=false;     //cek, apakah kamera sudah pernah di nyalakan //check camera that was ever used
    private int heightCamera;
    private int widthCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initView();
        initData();
    }

    private void initView()
    {
        cameraSurfaceView = (SurfaceView) findViewById(R.id.camera_surface_view);
        surfaceHolder = cameraSurfaceView.getHolder();
        if (cameraHardware== null) {
            cameraHardware = getCamera(cameraId);
        }else
        {
            //jika tidak ada kamera
            //if there is no camera
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
    }

    private Camera getCamera(int id)
    {
        Camera camera = null;
        try{
            //kamera siap dipakai
            //camera ready to be used
            camera = Camera.open();
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
