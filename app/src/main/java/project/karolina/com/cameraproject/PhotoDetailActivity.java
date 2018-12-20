package project.karolina.com.cameraproject;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;

import project.karolina.com.cameraproject.helper.LocaleHelper;

public class PhotoDetailActivity extends AppCompatActivity {

    private static final String TAG = "PhotoDetailActivity";
    public static final String EXTRA_IS_NEW = "project.karolina.com.cameraproject.PhotoDetailActivity.IS_NEW";
    public static final String EXTRA_NAME = "project.karolina.com.cameraproject.PhotoDetailActivity.NAME";

    public static final String APPLICATION_FOLDER_NAME = "hand_picture_app_folder";
    public static final int RESULT_OK = -1;
    public static final int RESULT_CANCEL = 0;

    private final int REQUEST_IMAGE_CAPTURE = 201;

    private static final String STATE_NAME = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_NAME";
    private static final String STATE_ORIGINAL_NAME = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_ORIGINAL_NAME";
    private static final String STATE_LEFT_PHOTO = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_LEFT_PHOTO";
    private static final String STATE_RIGHT_PHOTO = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_RIGHT_PHOTO";
    private static final String STATE_SIDE = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_SIDE";
    private static final String STATE_RELOADED = "project.karolina.com.cameraproject.PhotoDetailActivity.STATE_RELOADED";

    private Animator animator;
    private int shortAnimationDuration;

    private String name;
    private View layout;

    private Bitmap leftPhoto, rightPhoto;
    private Side clickedSide;

    EditText photoDetailNameInput;
    ImageView photoDetailLeftPhotoPreview, photoDetailRightPhotoPreview;
    Button photoDetailLeftPhotoChangeButton, photoDetailRightPhotoChangeButton, photoDetailSaveButton, photoDetailCancelButton;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);
        Log.i(TAG, "onCreate: initialization photo detail activity");

        layout = findViewById(R.id.photo_detail_container_layout);
        photoDetailNameInput = findViewById(R.id.photo_detail_name_input);
        photoDetailLeftPhotoPreview = findViewById(R.id.photo_detail_left_photo_preview);
        photoDetailRightPhotoPreview = findViewById(R.id.photo_detail_right_photo_preview);
        photoDetailLeftPhotoChangeButton = findViewById(R.id.photo_detail_left_photo_change_button);
        photoDetailRightPhotoChangeButton = findViewById(R.id.photo_detail_right_photo_change_button);
        photoDetailSaveButton = findViewById(R.id.photo_detail_save_button);
        photoDetailCancelButton = findViewById(R.id.photo_detail_cancel_button);

        photoDetailLeftPhotoChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: left change clicked");
                dispatchTakePictureIntent(Side.LEFT);
            }
        });
        photoDetailRightPhotoChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: right change clicked");
                dispatchTakePictureIntent(Side.RIGHT);
            }
        });
        photoDetailSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: save clicked");
                save();
            }
        });
        photoDetailCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: cancel clicked");
                cancel();
            }
        });

        // load the screen
        if(!(savedInstanceState != null && savedInstanceState.getBoolean(STATE_RELOADED, false))) {
            Intent intent = getIntent();
            String name = intent.getStringExtra(EXTRA_NAME);
            Bitmap leftPhoto = null;
            Bitmap rightPhoto = null;
            if(name != null) {
                leftPhoto = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + APPLICATION_FOLDER_NAME + "/" + name + "/left.jpg");
                rightPhoto = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + APPLICATION_FOLDER_NAME + "/" + name + "/right.jpg");
            }
            init(name, name, leftPhoto, rightPhoto, -1);
        }

        photoDetailLeftPhotoPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(leftPhoto != null)
                    zoomImageFromThumb(photoDetailLeftPhotoPreview, leftPhoto);
            }
        });
        photoDetailRightPhotoPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(rightPhoto != null)
                    zoomImageFromThumb(photoDetailRightPhotoPreview, rightPhoto);
            }
        });
        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    /*
    https://stackoverflow.com/questions/24503968/camera-intent-not-returning-to-calling-activity
     */

    private void dispatchTakePictureIntent(Side side) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.i(TAG, "dispatchTakePictureIntent: verify if it is possible to start the camera");
        if(takePictureIntent.resolveActivity(getPackageManager()) != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            Log.i(TAG, "dispatchTakePictureIntent: it is possible to start the camera");
            clickedSide = side;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "temp-picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "temp-picture-description");
            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void cancel() {
        Intent intent = new Intent();
        setResult(RESULT_CANCEL, intent);

        finish();
    }

    /*
    https://stackoverflow.com/questions/14053338/save-bitmap-in-android-as-jpeg-in-external-storage-in-a-folder
     */

    private void save() {
        Log.i(TAG, "save: saving data");
        String newName = photoDetailNameInput.getText().toString();
        if(newName == null || newName.isEmpty()) {
            Log.w(TAG, "save: name is mandatory", null);
            Snackbar.make(layout, getString(R.string.str_snackbar_mandatory_name), Snackbar.LENGTH_LONG).show();
            return;
        }
        if(leftPhoto == null || rightPhoto == null) {
            Log.w(TAG, "save: photos are mandatory", null);
            Snackbar.make(layout, getString(R.string.str_snackbar_mandatory_photos), Snackbar.LENGTH_LONG).show();
            return;
        }
        Log.i(TAG, "save: name: " + newName);
        Log.i(TAG, "save: left: " + leftPhoto);
        Log.i(TAG, "save: right: " + rightPhoto);

        String root = Environment.getExternalStorageDirectory().toString();
        String pathName = newName.replaceAll(" ", "_");

        File dir = new File(root + "/" + APPLICATION_FOLDER_NAME + "/" + pathName);
        Log.i(TAG, "save: directory path: " + dir.getPath());

        if(name == null || !name.equals(pathName)) {
            // check if the folder already exists
            if(dir != null && dir.isDirectory()) {
                Snackbar.make(layout, getString(R.string.str_snackbar_directory_exists), Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        if(name != null && !name.equals(pathName)) {
            Log.i(TAG, "save: delete previous directory because no more needed");
            File oldDir = new File(root + "/" + APPLICATION_FOLDER_NAME + "/" + name);
            // delete previous folder
            if(oldDir != null) {
                if(oldDir.isDirectory() && oldDir.listFiles() != null) {
                    for(File file : oldDir.listFiles()) {
                        file.delete();
                    }
                    oldDir.delete();
                }
            }
        }

        dir.mkdirs();
        File leftPhotoFile = new File(dir, "left.jpg");
        File rightPhotoFile = new File(dir, "right.jpg");

        if(leftPhotoFile.exists())
            leftPhotoFile.delete();
        if(rightPhotoFile.exists())
            rightPhotoFile.delete();

        try {
            FileOutputStream outLeftPhoto = new FileOutputStream(leftPhotoFile);
            leftPhoto.compress(Bitmap.CompressFormat.JPEG, 100, outLeftPhoto);
            FileOutputStream outRightPhoto = new FileOutputStream(rightPhotoFile);
            rightPhoto.compress(Bitmap.CompressFormat.JPEG, 100, outRightPhoto);
            outLeftPhoto.flush();
            outRightPhoto.flush();
            outLeftPhoto.close();
            outRightPhoto.close();

            Intent intent = new Intent();
            setResult(RESULT_OK, intent);

            finish();
        } catch (Exception e) {
            Log.e(TAG, "save: unable to save photos", e);
            Snackbar.make(layout, getString(R.string.str_snackbar_unable_save_photos), Snackbar.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "onActivityResult: returned from activity");
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.i(TAG, "onActivityResult: returned from camera");
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePathColumn, null, null, null);
            if(cursor == null) {
                Log.e(TAG, "onActivityResult: impossible to get the image", null);
                return;
            }
            cursor.moveToLast();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filePath = cursor.getString(columnIndex);
            File source = new File(filePath);
            cursor.close();
            if(source.exists()) {
                Log.i(TAG, "onActivityResult: path: " + filePath);
                switch (clickedSide) {
                    case LEFT:
                        leftPhoto = BitmapFactory.decodeFile(filePath);
                        photoDetailLeftPhotoPreview.setImageBitmap(leftPhoto);
                        break;
                    case RIGHT:
                        rightPhoto = BitmapFactory.decodeFile(filePath);
                        photoDetailRightPhotoPreview.setImageBitmap(rightPhoto);
                        break;
                }
                source.delete();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public enum Side {
        LEFT, RIGHT
    }

    @Override
    protected void onPause() {
        super.onPause();
        // saving the application state
        Log.i(TAG, "onPause: saving application state");

    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO: fix issue with saving photos temporary
        /*
        Log.i(TAG, "onSaveInstanceState: saving instance state");
        if(name != null)
            outState.putString(STATE_ORIGINAL_NAME, name);
        outState.putString(STATE_NAME, photoDetailNameInput.getText().toString().replaceAll(" ", "_"));
        if(leftPhoto != null)
            outState.putParcelable(STATE_LEFT_PHOTO, leftPhoto);
        if(rightPhoto != null)
            outState.putParcelable(STATE_RIGHT_PHOTO, rightPhoto);
        if(clickedSide != null)
            outState.putInt(STATE_SIDE, clickedSide.ordinal());
        outState.putBoolean(STATE_RELOADED, true);
        */
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // TODO: fix issue with saving photos temporary
        /*
        Log.i(TAG, "onRestoreInstanceState: restoring instance state");
        Bitmap loadedLeftPhoto = savedInstanceState.getParcelable(STATE_LEFT_PHOTO);
        Bitmap loadedRightPhoto = savedInstanceState.getParcelable(STATE_RIGHT_PHOTO);
        init(
                savedInstanceState.getString(STATE_ORIGINAL_NAME),
                savedInstanceState.getString(STATE_NAME),
                loadedLeftPhoto,
                loadedRightPhoto,
                savedInstanceState.getInt(STATE_SIDE, -1)
        );
        */
    }

    private void init(String originalName, String name, Bitmap leftPhoto, Bitmap rightPhoto, int side) {
        this.name = originalName;
        if(name != null)
            photoDetailNameInput.setText(name.replaceAll("_", " "));
        this.leftPhoto = leftPhoto;
        this.rightPhoto = rightPhoto;
        if(side > -1)
            this.clickedSide = Side.values()[side];
        // restore pictures in the image views
        if(leftPhoto != null)
            photoDetailLeftPhotoPreview.setImageBitmap(leftPhoto);
        if(rightPhoto != null)
            photoDetailRightPhotoPreview.setImageBitmap(rightPhoto);
    }

    /*
    https://developer.android.com/training/animation/zoom#java
     */

    private void zoomImageFromThumb(final View viewThumbnail, Bitmap image) {
        if(animator != null)
            animator.cancel();
        final ImageView expandedImageView = findViewById(R.id.photo_detail_expanded_image);
        expandedImageView.setImageBitmap(image);
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();
        viewThumbnail.getGlobalVisibleRect(startBounds);
        findViewById(R.id.photo_detail_layout).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);
        float startScale;
        if((float)finalBounds.width()/finalBounds.height() > (float)startBounds.width()/startBounds.height()) {
            startScale = (float)startBounds.width()/startBounds.height();
            float startWidth = startScale*finalBounds.width();
            float deltaWidth = (startWidth-startBounds.width())/2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            startScale = (float)startBounds.width()/finalBounds.width();
            float startHeight = startScale*finalBounds.height();
            float deltaHeight = (startHeight-startBounds.height())/2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }
        viewThumbnail.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }
        });
        set.start();
        animator = set;
        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(animator != null)
                    animator.cancel();
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                set.setDuration(shortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        viewThumbnail.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        animator = null;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewThumbnail.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        animator = null;
                    }
                });
                set.start();
                animator = set;
            }
        });
    }

}
