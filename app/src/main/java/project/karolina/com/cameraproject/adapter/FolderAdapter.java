package project.karolina.com.cameraproject.adapter;

import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import project.karolina.com.cameraproject.HomeActivity;
import project.karolina.com.cameraproject.PhotoDetailActivity;
import project.karolina.com.cameraproject.R;
import project.karolina.com.cameraproject.entity.Folder;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private static final String TAG = "FolderAdapter";

    private final List<Folder> folders;
    private final HomeActivity homeActivity;

    static class FolderViewHolder extends RecyclerView.ViewHolder {

        TextView folderTitle;
        Button editButton, deleteButton;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderTitle = itemView.findViewById(R.id.folder_list_row_title);
            editButton = itemView.findViewById(R.id.folder_list_row_button_edit);
            deleteButton = itemView.findViewById(R.id.folder_list_row_button_delete);
        }
    }

    public FolderAdapter(HomeActivity homeActivity, List<Folder> folders) {
        this.homeActivity = homeActivity;
        this.folders = folders;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.folder_list_row, viewGroup, false);
        return new FolderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder folderViewHolder, int i) {
        final Folder folder = folders.get(i);
        folderViewHolder.folderTitle.setText(folder.getName().replaceAll("_", " "));
        folderViewHolder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: clicked edit for folder: " + folder.getName());
                Intent intent = new Intent(homeActivity, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_IS_NEW, false);
                intent.putExtra(PhotoDetailActivity.EXTRA_NAME, folder.getName());
                homeActivity.startActivityForResult(intent, HomeActivity.REQUEST_DETAIL_PHOTO);
            }
        });
        folderViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: clicked delete for folder: " + folder.getName());
                File directory = new File(Environment.getExternalStorageDirectory().toString() + "/" + PhotoDetailActivity.APPLICATION_FOLDER_NAME + "/" + folder.getName());
                if(directory != null && directory.isDirectory()) {
                    if(directory.listFiles() != null) {
                        for (File file : directory.listFiles()) {
                            file.delete();
                        }
                    }
                    directory.delete();
                }
                homeActivity.prepareFolderList();
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }
}
