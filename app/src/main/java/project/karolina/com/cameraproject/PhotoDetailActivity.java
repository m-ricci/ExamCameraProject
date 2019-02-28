package project.karolina.com.cameraproject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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
    public static final String PHOTO_DETAIL_BACKGROUND_TYPE = "project.karolina.com.cameraproject.PhotoDetailActivity.PHOTO_DETAIL_BACKGROUND_TYPE";
    public static final String PHOTO_DETAIL_PHONE_NAME = "project.karolina.com.cameraproject.PhotoDetailActivity.PHOTO_DETAIL_PHONE_NAME";

    private String name;

    private List<Image> leftPhotoImageList = new ArrayList<>();
    private List<Image> rightPhotoImageList = new ArrayList<>();
    private RecyclerView.Adapter leftPhotoListAdapter, rightPhotoListAdapter;

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
                openPhotoBackgroundDecision(Side.LEFT, folderPath + HomeActivity.FOLDER_LEFT_NAME);
            }
        });
        ImageButton rightAddButton = findViewById(R.id.photo_detail_right_add_button);
        rightAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPhotoBackgroundDecision(Side.RIGHT, folderPath + HomeActivity.FOLDER_RIGHT_NAME);
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
    }

    private void openPhotoBackgroundDecision(final Side side, final String folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(PhotoDetailActivity.this);
        builder.setMessage(R.string.str_new_photo_background_selection);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.str_new_photo_background_dark, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityCamera(side, folder, Background.DARK);
            }
        });
        builder.setPositiveButton(R.string.str_new_photo_background_light, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityCamera(side, folder, Background.LIGHT);
            }
        });
        builder.create().show();
    }

    private void startActivityCamera(Side side, String folder, Background background) {
        Intent camera = new Intent(PhotoDetailActivity.this, CameraActivity.class);
        camera.putExtra(PHOTO_DETAIL_CLICKED_SIDE, side.ordinal());
        camera.putExtra(PHOTO_DETAIL_FOLDER_NAME, folder);
        camera.putExtra(PHOTO_DETAIL_BACKGROUND_TYPE, background.getName());
        camera.putExtra(PHOTO_DETAIL_PHONE_NAME, "_" + Build.MANUFACTURER + "_" + Build.MODEL + "_" + Build.VERSION.RELEASE);
        PhotoDetailActivity.this.startActivityForResult(camera, REQUEST_CAMERA_ACTIVITY);
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

    public enum Background {
        DARK("_dark"), LIGHT("_light");

        Background(String name) {
            this.name = name;
        }

        private String name;

        public String getName() {
            return name;
        }

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

}
