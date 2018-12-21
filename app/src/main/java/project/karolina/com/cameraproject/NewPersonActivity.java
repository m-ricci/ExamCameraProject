package project.karolina.com.cameraproject;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;

import project.karolina.com.cameraproject.helper.LocaleHelper;

public class NewPersonActivity extends AppCompatActivity {

    private static final String TAG = "NewPersonActivity";

    private EditText name;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_person);
        name = findViewById(R.id.new_person_name_input);
        Button save = findViewById(R.id.new_person_save_button);
        Button cancel = findViewById(R.id.new_person_cancel_button);
        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // check if the name is valid
                String value = name.getText().toString();
                if(value.isEmpty()) {
                    Snackbar.make(findViewById(R.id.new_person_layout), getString(R.string.str_snackbar_mandatory_name), Snackbar.LENGTH_LONG).show();
                } else {
                    // create the folder
                    String root = Environment.getExternalStorageDirectory().toString();
                    String pathName = value.replaceAll(" ", "_");
                    Long timestamp = System.currentTimeMillis()/1000;
                    String folderName = timestamp.toString() + "_" + pathName;
                    String folderPath = root + "/" + HomeActivity.APPLICATION_FOLDER_NAME + "/" + folderName;
                    File dir = new File(folderPath);
                    Log.d(TAG, "onClick: main folder created with result: " + dir.mkdirs());
                    dir = new File(folderPath + "/" + HomeActivity.FOLDER_LEFT_NAME);
                    Log.d(TAG, "onClick: left hand folder created with result: " + dir.mkdir());
                    dir = new File(folderPath + "/" + HomeActivity.FOLDER_RIGHT_NAME);
                    Log.d(TAG, "onClick: right hand folder created with result: " + dir.mkdir());
                    // return the result to the home
                    Intent intent = new Intent();
                    intent.putExtra(HomeActivity.ACTIVITY_RESULT_FOLDER_NAME, folderName);
                    setResult(HomeActivity.ACTIVITY_RESULT_OK, intent);
                    finish();
                }
            }

        });
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                setResult(HomeActivity.ACTIVITY_RESULT_CANCEL, intent);
                finish();
            }

        });

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
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

}
