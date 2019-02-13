package project.karolina.com.cameraproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    public static final String RESULT_CAMERA_SIDE = "project.karolina.com.cameraproject.CameraActivity.RESULT_CAMERA_SIDE";

    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String folderName;
    private PhotoDetailActivity.Side side;

    private Handler mBackgroundHandler;
    private static final int REQUEST_CAMERA_PERMISSION = 501;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    private Size imageDimension;
    private HandlerThread mBackgroundThread;

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            Log.i(TAG, "onSurfaceTextureAvailable: -----");
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
            Log.i(TAG, "onSurfaceTextureSizeChanged: -----");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            Log.i(TAG, "onOpened: -----");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "onDisconnected: -----");
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(TAG, "onError: -----");
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: -----");
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        side = PhotoDetailActivity.Side.values()[intent.getIntExtra(PhotoDetailActivity.PHOTO_DETAIL_CLICKED_SIDE, -1)];
        folderName = intent.getStringExtra(PhotoDetailActivity.PHOTO_DETAIL_FOLDER_NAME);

        textureView = findViewById(R.id.camera_texture_view);
        textureView.setSurfaceTextureListener(textureListener);
        FloatingActionButton takePictureButton = findViewById(R.id.camera_button);
        takePictureButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: -----");
                takePicture();
            }

        });

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private void startBackgroundThread() {
        Log.i(TAG, "startBackgroundThread: -----");
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        Log.i(TAG, "stopBackgroundThread: -----");
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackgroundThread: ", e);
        }
    }

    private void takePicture() {
        Log.i(TAG, "takePicture: -----");
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        Log.i(TAG, "takePicture: saving picture");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics;
            if(manager != null)
                characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            else {
                Log.e(TAG, "takePicture: unable to retrieve camera manager", new NullPointerException());
                return;
            }

            Size[] jpegSizes = null;
            StreamConfigurationMap scalerStream = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(scalerStream != null)
                jpegSizes = scalerStream.getOutputSizes(ImageFormat.JPEG);
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Log.d(TAG, "takePicture: photo rotation: " + rotation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            Log.d(TAG, "takePicture: orientation: " + ORIENTATIONS.get(rotation));

            final Long timestamp = System.currentTimeMillis()/1000;
            final File file = new File(folderName, timestamp.toString() + ".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.i(TAG, "onImageAvailable: -----");
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Log.i(TAG, "onImageAvailable: performsaving");
                        save(bytes);
                    } catch (IOException e) {
                        Log.e(TAG, "onImageAvailable: ", e);
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    Log.i(TAG, "save: -----");
                    try (OutputStream output = new FileOutputStream(file)) {
                        Log.i(TAG, "save: file name: " + file.getName());
                        output.write(bytes);
                        Log.i(TAG, "save: file saved");

                        Intent result = new Intent(CameraActivity.this, PhotoDetailActivity.class);
                        result.putExtra(RESULT_CAMERA_SIDE, side.ordinal());
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }
                }

            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i(TAG, "onCaptureCompleted: -----");
                    Toast.makeText(CameraActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }

            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        Log.i(TAG, "onConfigured: -----");
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "onConfigured: ", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }

            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "takePicture: ", e);
        }
    }

    private void createCameraPreview() {
        Log.i(TAG, "createCameraPreview: -----");
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback(){

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    Log.i(TAG, "onConfigured: -----");
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.i(TAG, "onConfigureFailed: -----");
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }

            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreview: ", e);
        }
    }

    private void openCamera() {
        Log.i(TAG, "openCamera: -----");
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.i(TAG, "is camera open");
        try {
            if(manager == null)
                return;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

            // place imageView with hand placeholder
            ImageView fingerPlaceholder = findViewById(R.id.finger_placeholder);
            fingerPlaceholder.setImageDrawable(getDrawable(
                    side == PhotoDetailActivity.Side.LEFT
                            ? R.drawable.left_hand_placeholder
                            : R.drawable.right_hand_placeholder
            ));

        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: ", e);
        }
        Log.i(TAG, "openCamera X");
    }

    private void updatePreview() {
        Log.i(TAG, "updatePreview: -----");
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "updatePreview: ", e);
        }
    }

//    private void closeCamera() {
//        Log.i(TAG, "closeCamera: -----");
//        if (null != cameraDevice) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//        if (null != imageReader) {
//            imageReader.close();
//            imageReader = null;
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: -----");
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: -----");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: -----");
        stopBackgroundThread();
//        closeCamera();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected: -----");
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*

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
                //ByteArrayOutputStream stream = new ByteArrayOutputStream();
                //textureView.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
                Intent result = new Intent(CameraActivity.this, PhotoDetailActivity.class);
                //result.putExtra(RESULT_CAMERA_BITMAP, stream.toByteArray());
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
                Log.i(TAG, "onOpened: ");
                CameraActivity.this.cameraDevice = cameraDevice;
                createPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                Log.i(TAG, "onDisconnected: ");
                cameraDevice.close();
                CameraActivity.this.cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                Log.i(TAG, "onError: ");
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
            Log.i(TAG, "openCamera: ");
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: unable to open the camera", e);
        }
    }

    private void openBackgroundThread() {
        Log.i(TAG, "openBackgroundThread: ");
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
        Log.i(TAG, "onStop: ");
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera: ");
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
        Log.i(TAG, "closeBackgroundThread: ");
        if(backgroundHandler != null) {
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    */

}
