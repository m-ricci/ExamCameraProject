package project.karolina.com.cameraproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    public static final String RESULT_CAMERA_BITMAP = "project.karolina.com.cameraproject.CameraActivity.RESULT_CAMERA_BITMAP";
    public static final String RESULT_CAMERA_SIDE = "project.karolina.com.cameraproject.CameraActivity.RESULT_CAMERA_SIDE";

    private static final int CAMERA_REQUEST_CODE = 301;
    private CameraManager manager;
    private int cameraFacing;
    private TextureView.SurfaceTextureListener listener;
    private Size previewSize;
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback;
    private CameraDevice cameraDevice;
    private TextureView textureView;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest captureRequest;
    private PhotoDetailActivity.Side clickedSide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.i(TAG, "onCreate: initialization camera page");

        clickedSide = PhotoDetailActivity.Side.values()[getIntent().getIntExtra(PhotoDetailActivity.PHOTO_DETAIL_CLICKED_SIDE, -1)];
        textureView = findViewById(R.id.camera_texture_view);
        FloatingActionButton cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                textureView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
                Intent result = new Intent(CameraActivity.this, PhotoDetailActivity.class);result.putExtra(RESULT_CAMERA_BITMAP, stream.toByteArray());
                result.putExtra(RESULT_CAMERA_SIDE, clickedSide.ordinal());
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

        // place imageView with hand placeholder
        ImageView fingerPlaceholder = findViewById(R.id.finger_placeholder);
        fingerPlaceholder.setImageDrawable(getDrawable(
                clickedSide == PhotoDetailActivity.Side.LEFT
                    ? R.drawable.left_hand_placeholder
                    : R.drawable.right_hand_placeholder
        ));

        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                CameraActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                cameraDevice.close();
                CameraActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                cameraDevice.close();
                CameraActivity.this.cameraDevice = null;
            }
        };

        ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFacing = CameraCharacteristics.LENS_FACING_BACK;
        listener = new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                setUpCamera();
                openCamera();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if(cameraDevice == null)
                                return;
                            try {
                                captureRequest = captureRequestBuilder.build();
                                CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                                CameraActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);
                            } catch(CameraAccessException e) {
                                Log.e(TAG, "onConfigured: unable to interact with camera", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch(CameraAccessException e) {
            Log.e(TAG, "createPreviewSession: unable to interact with camera", e);
        }
    }

    private void setUpCamera() {
        try {
            for(String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                Integer localCameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if(localCameraFacing != null && localCameraFacing == cameraFacing) {
                    StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP );
                    if(streamConfigurationMap != null) {
                        previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                        this.cameraId = cameraId;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "setUpCamera: unable to interact with camera using camera manager", e);
        }
    }

    private void openCamera() {
        try {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: unable to open the camera", e);
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("camera_background_thread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if(textureView.isAvailable()) {
            setUpCamera();
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(listener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        if(cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if(cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void closeBackgroundThread() {
        if(backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

}
