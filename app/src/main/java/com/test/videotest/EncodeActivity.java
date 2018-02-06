package com.test.videotest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import static android.content.ContentValues.TAG;

/**
 * 本地编码
 */
public class EncodeActivity extends Activity  implements SurfaceHolder.Callback,Camera.PreviewCallback {

    private SurfaceView mSurfaceView;


    AvcEncoder avcCodec;
    private static int yuvqueuesize = 10;

    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);

    private String TAG=EncodeActivity.class.getSimpleName();

    public Camera m_camera;
    SurfaceView m_prevewview;
    SurfaceHolder m_surfaceHolder;
    int width = 1280;
    int height = 720;
    int framerate = 15;
    int bitrate = 125000;
    //int bitRate = camera.getFpsRange()[1] * currentSize.width * currentSize.height / 15;

    byte[] h264 = new byte[width * height * 3 / 2];

    private FileOutputStream file = null;
    private String filename = "camera.h264";
    private int byteOffset = 0;
    private long lastTime = 0;

    private String mImagePath;
    private final static int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImagePath = Environment.getExternalStorageDirectory().getPath() + "/avcCodec/";
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode);

        Log.d("ws", "width=" + width + ", height=" + height + ", framerate=" + framerate + ", bitrate=" + bitrate);
        //初始化编码器，在有的机器上失败 fuck!
        try {
            avcCodec = new AvcEncoder(width, height, framerate, bitrate);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }

        m_prevewview = (SurfaceView) findViewById(R.id.surfaceview_encode);
        m_surfaceHolder = m_prevewview.getHolder();
        m_surfaceHolder.setFixedSize(width, height);
        m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_surfaceHolder.addCallback((SurfaceHolder.Callback) this);


    }

    private void checkPermission() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA};

        boolean need_permission = ContextCompat.checkSelfPermission(this, permissions[0]) ==
                PackageManager.PERMISSION_DENIED;
        if (need_permission) {
            Log.e(TAG, "requestPermissions()");
            //申请相机以及录音权限
            ActivityCompat.requestPermissions(this,
                    permissions,
                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        }

    }


    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

    }

    @SuppressLint("NewApi")
    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            m_camera = Camera.open();
            m_camera.setPreviewDisplay(m_surfaceHolder);
            Camera.Parameters parameters = m_camera.getParameters();
            parameters.setPreviewSize(width, height);
            parameters.setPictureSize(width, height);
            parameters.setPreviewFormat(ImageFormat.YV12);
            parameters.set("rotation", 90);
            //parameters.set("orientation", "portrait");
            m_camera.setParameters(parameters);
            m_camera.setDisplayOrientation(90);
            m_camera.setPreviewCallback((Camera.PreviewCallback) this);
            m_camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("LSH",e.toString());
        }


        try {
            File fileFolder = new File(mImagePath);
            if (!fileFolder.exists())
                fileFolder.mkdirs();
            File files = new File(mImagePath, filename);
            if (!files.exists()) {
                Log.e(TAG, "file create success ");
                files.createNewFile();
            }
            file = new FileOutputStream(files);
            Log.e(TAG, "file save success ");
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        m_camera.setPreviewCallback(null);
        m_camera.release();
        m_camera = null;
        avcCodec.close();
        try {
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.d("Fuck", "File close error");
            e.printStackTrace();
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        long newTime = System.currentTimeMillis();
        long diff = newTime - lastTime;
        lastTime = newTime;
        //把摄像头的数据传给编码器
        int ret = avcCodec.offerEncoder(data, h264);

        if (ret > 0) {
            try {
                byte[] length_bytes = intToBytes(ret);
                file.write(length_bytes);
                file.write(h264, 0, ret);

            } catch (IOException e) {
                Log.d("ws", "exception: " + e.toString());
            }
        }

    }

    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }
}
