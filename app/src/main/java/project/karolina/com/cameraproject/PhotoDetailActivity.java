package project.karolina.com.cameraproject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import project.karolina.com.cameraproject.adapter.ImageAdapter;
import project.karolina.com.cameraproject.entity.Image;
import project.karolina.com.cameraproject.helper.LocaleHelper;

public class PhotoDetailActivity extends AppCompatActivity {

    private static final String TAG = "PhotoDetailActivity";

    private final int REQUEST_CAMERA_ACTIVITY = 202;
    public static final String PHOTO_DETAIL_FOLDER_NAME = "project.karolina.com.cameraproject.PhotoDetailActivity.FOLDER_NAME";
    public static final String PHOTO_DETAIL_CLICKED_SIDE = "project.karolina.com.cameraproject.PhotoDetailActivity.CLICKED_SIDE";

    private String name;

    private List<Image> leftPhotoImageList = new ArrayList<>();
    private List<Image> rightPhotoImageList = new ArrayList<>();
    private RecyclerView.Adapter leftPhotoListAdapter, rightPhotoListAdapter;

    private Animator animator;
    private int shortAnimationDuration;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        Log.d(TAG, "onCreate: initialization photo detail activity");
        Intent intent = getIntent();
        name = intent.getStringExtra(PHOTO_DETAIL_FOLDER_NAME);
        Log.d(TAG, "folder opened: " + name);
        TextView nameValue = findViewById(R.id.photo_detail_name_value);
        nameValue.setText(name.substring(name.indexOf("_")+1).replaceAll("_", " "));
        ImageButton leftAddButton = findViewById(R.id.photo_detail_left_add_button);

        String root = Environment.getExternalStorageDirectory().toString();
        final String folderPath = root + "/" + HomeActivity.APPLICATION_FOLDER_NAME + "/" + name + "/";

        leftAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent camera = new Intent(PhotoDetailActivity.this, CameraActivity.class);
                camera.putExtra(PHOTO_DETAIL_CLICKED_SIDE, Side.LEFT.ordinal());
                camera.putExtra(PHOTO_DETAIL_FOLDER_NAME, folderPath + HomeActivity.FOLDER_LEFT_NAME);
                PhotoDetailActivity.this.startActivityForResult(camera, REQUEST_CAMERA_ACTIVITY);
            }
        });
        ImageButton rightAddButton = findViewById(R.id.photo_detail_right_add_button);
        rightAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent camera = new Intent(PhotoDetailActivity.this, CameraActivity.class);
                camera.putExtra(PHOTO_DETAIL_CLICKED_SIDE, Side.RIGHT.ordinal());
                camera.putExtra(PHOTO_DETAIL_FOLDER_NAME, folderPath + HomeActivity.FOLDER_RIGHT_NAME);
                PhotoDetailActivity.this.startActivityForResult(camera, REQUEST_CAMERA_ACTIVITY);
            }
        });

        RecyclerView.LayoutManager leftPhotoLayoutManager = new LinearLayoutManager(this);
        leftPhotoListAdapter = new ImageAdapter(PhotoDetailActivity.this, leftPhotoImageList, name, Side.LEFT);
        RecyclerView leftPhotoList = findViewById(R.id.photo_detail_left_photo_list);
        leftPhotoList.setHasFixedSize(true);
        leftPhotoList.setLayoutManager(leftPhotoLayoutManager);
        leftPhotoList.setAdapter(leftPhotoListAdapter);
        RecyclerView.LayoutManager rightPhotoLayoutManager = new LinearLayoutManager(this);
        rightPhotoListAdapter = new ImageAdapter(PhotoDetailActivity.this, rightPhotoImageList, name, Side.RIGHT);
        RecyclerView rightPhotoList = findViewById(R.id.photo_detail_right_photo_list);
        rightPhotoList.setHasFixedSize(true);
        rightPhotoList.setLayoutManager(rightPhotoLayoutManager);
        rightPhotoList.setAdapter(rightPhotoListAdapter);
        initImageList(Side.LEFT);
        initImageList(Side.RIGHT);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        shortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public enum Side {
        LEFT, RIGHT
    }

    public void initImageList(Side side) {
        List<Image> images = side == Side.LEFT ? leftPhotoImageList : rightPhotoImageList;
        images.clear();
        String root = Environment.getExternalStorageDirectory().toString();
        String folderPath = root + "/" + HomeActivity.APPLICATION_FOLDER_NAME + "/" + name + "/";
        folderPath += side == Side.LEFT ? HomeActivity.FOLDER_LEFT_NAME : HomeActivity.FOLDER_RIGHT_NAME;
        File directory = new File(folderPath);
        if(directory.isDirectory()) {
            for(File file : directory.listFiles()) {
                Log.d(TAG, "getImageList: file name: " + file.getName());
                images.add(new Image(file.getName()));
            }
        }
        if(side == Side.LEFT)
            leftPhotoListAdapter.notifyDataSetChanged();
        else
            rightPhotoListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "onActivityResult: returned from activity");
        if(requestCode == REQUEST_CAMERA_ACTIVITY && resultCode == RESULT_OK) {
            if(data != null && data.hasExtra(CameraActivity.RESULT_CAMERA_SIDE)) {
                Log.i(TAG, "onActivityResult: retrieving result from camera");
                Side clickedSide = Side.values()[data.getIntExtra(CameraActivity.RESULT_CAMERA_SIDE, -1)];
                initImageList(clickedSide);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // https://developer.android.com/training/animation/zoom#java

    public void zoomImageFromThumb(final View viewThumbnail, Bitmap image) {
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
