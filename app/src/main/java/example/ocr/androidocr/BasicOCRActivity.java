package example.ocr.androidocr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class BasicOCRActivity extends AppCompatActivity {

    private CameraDevice mCameraDevice;

    private String mCameraId;

    private TextureView mCameraPreviewView;

    private CameraManager mCameraManager;

    private Size mStreamSize;

    private Handler mHandler;

    private HandlerThread mHandlerThread;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createSessions();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
            System.out.println("error in camera state");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraPreviewView = findViewById(R.id.camera_preview);

        openBackgroundThread();

        requestPermissions();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestPermissions()) {
            if (mCameraPreviewView.isAvailable()) {
                configureCamera();
                openCamera();
            } else {
                mCameraPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                        configureCamera();
                        openCamera();
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            onResume();
        }
    }

    private void configureCamera() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (mCameraManager != null) {
            try {
                for (String id : mCameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        mCameraId = id;
                        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (map != null) {
                            mStreamSize = map.getOutputSizes(SurfaceTexture.class)[0];
                        }
                    }
                }
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void openCamera() {
        if (mCameraManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                    return;
                }
                mCameraManager.openCamera(mCameraId, mStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void openBackgroundThread() {
        mHandlerThread = new HandlerThread("basic ocr");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void createSessions() {
        try {
            SurfaceTexture texture = mCameraPreviewView.getSurfaceTexture();
            texture.setDefaultBufferSize(mCameraPreviewView.getWidth(), mCameraPreviewView.getHeight());
            Surface previewSurface = new Surface(texture);

            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    System.out.println("config failed");
                }
            }, mHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
            return false;
        }
        return true;
    }
}
