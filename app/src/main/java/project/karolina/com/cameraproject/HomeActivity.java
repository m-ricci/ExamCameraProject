package project.karolina.com.cameraproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import project.karolina.com.cameraproject.adapter.FolderAdapter;
import project.karolina.com.cameraproject.entity.Folder;
import project.karolina.com.cameraproject.helper.LocaleHelper;

public class HomeActivity extends AppCompatActivity {

    public static final String APPLICATION_FOLDER_NAME = "hand_picture_app_folder";
    public static final int ACTIVITY_RESULT_OK = -1;
    public static final int ACTIVITY_RESULT_CANCEL = 0;
    public static final String FOLDER_LEFT_NAME = "left";
    public static final String FOLDER_RIGHT_NAME = "right";
    public static final String ACTIVITY_RESULT_FOLDER_NAME = "ACTIVITY_RESULT_FOLDER_NAME";

    private static final String TAG = "HomeActivity";
    public static final int REQUEST_NEW_PERSON = 101;
    public static final int REQUEST_DETAIL_PHOTO = 102;
    private final int REQUEST_WRITE_PERMISSION = 1;

    private final List<Folder> folders = new ArrayList<>();

    private RecyclerView.Adapter adapter;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.i(TAG, "onCreate: initialization home page");

        RecyclerView recyclerView = findViewById(R.id.home_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FolderAdapter(HomeActivity.this, folders);
        recyclerView.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // explain user why permission in needed
                    Log.d(TAG, "onCreate: show permission request information");
                }
            }
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        } else {
            Log.i(TAG, "onCreate: impossible to verify the permissions because old version used");
            initHome();
        }
    }

    private void initHome() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prepareFolderList();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_WRITE_PERMISSION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: write permission granted");
                    initHome();
                } else {
                    Log.i(TAG, "onRequestPermissionsResult: write permission not granted");
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void prepareFolderList() {
        folders.clear();
        String root = Environment.getExternalStorageDirectory().toString();
        File directory = new File(root + "/" + APPLICATION_FOLDER_NAME);
        if(directory.listFiles() != null) {
            for (File folder : directory.listFiles()) {
                Log.d(TAG,"folder name: " + folder.getName() + ", is directory: " + folder.isDirectory());
                if (folder.isDirectory()) {
                    Log.i(TAG, "prepareFolderList: directory found: " + folder.getName());
                    folders.add(new Folder(folder.getName()));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.i(TAG, "onOptionsItemSelected: menu item clicked");
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        final boolean result;
        switch (id) {
            case R.id.action_new_photo:
                Log.i(TAG, "onOptionsItemSelected: selected to open the new picture activity");
                Intent intent = new Intent(HomeActivity.this, NewPersonActivity.class);
                startActivityForResult(intent, REQUEST_NEW_PERSON);
                result = true;
                break;
            case R.id.action_language_english:
                Log.i(TAG, "onOptionsItemSelected: selected menu item english language");
                LocaleHelper.setLocale(HomeActivity.this, "en");
                recreate();
                result = true;
                break;
            case R.id.action_language_polish:
                Log.i(TAG, "onOptionsItemSelected: selected menu item polish language");
                LocaleHelper.setLocale(HomeActivity.this, "pl");
                recreate();
                result = true;
                break;
            case R.id.action_language_italian:
                Log.i(TAG, "onOptionsItemSelected: selected menu item italian language");
                LocaleHelper.setLocale(HomeActivity.this, "it");
                recreate();
                result = true;
                break;
            default:
                result = super.onOptionsItemSelected(item);

        }
        if (id == R.id.action_new_photo) {
            return true;
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_NEW_PERSON:
                Log.i(TAG, "onActivityResult: returned form detail with result: " + resultCode);
                if(resultCode == ACTIVITY_RESULT_OK) {
                    prepareFolderList();
                    if(data != null) {
                        String folderName = data.getStringExtra(ACTIVITY_RESULT_FOLDER_NAME);
                        Log.i(TAG, "onClick: clicked edit for folder: " + folderName);
                        Intent intent = new Intent(HomeActivity.this, PhotoDetailActivity.class);
                        intent.putExtra(PhotoDetailActivity.PHOTO_DETAIL_FOLDER_NAME, folderName);
                        startActivityForResult(intent, HomeActivity.REQUEST_DETAIL_PHOTO);
                    }
                }
                break;
        }
    }
}
