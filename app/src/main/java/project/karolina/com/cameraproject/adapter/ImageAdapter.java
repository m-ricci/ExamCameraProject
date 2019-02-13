package project.karolina.com.cameraproject.adapter;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.io.File;
import java.util.List;

import project.karolina.com.cameraproject.HandPhotoPreviewActivity;
import project.karolina.com.cameraproject.HomeActivity;
import project.karolina.com.cameraproject.PhotoDetailActivity;
import project.karolina.com.cameraproject.R;
import project.karolina.com.cameraproject.entity.Image;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private static final String TAG = "FolderAdapter";

    private final List<Image> images;
    private final PhotoDetailActivity photoDetailActivity;
    private final PhotoDetailActivity.Side side;
    private final String folderPath;

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageButton imageView;
        ImageButton deleteButton;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_list_row_photo);
            deleteButton = itemView.findViewById(R.id.image_list_row_button_delete);
        }
    }

    public ImageAdapter(PhotoDetailActivity photoDetailActivity, List<Image> images, String folderName, PhotoDetailActivity.Side side) {
        this.photoDetailActivity = photoDetailActivity;
        this.images = images;
        this.side = side;
        String root = Environment.getExternalStorageDirectory().toString();
        String path = root + "/" + HomeActivity.APPLICATION_FOLDER_NAME + "/" + folderName + "/";
        this.folderPath = path + (side == PhotoDetailActivity.Side.LEFT ? HomeActivity.FOLDER_LEFT_NAME : HomeActivity.FOLDER_RIGHT_NAME) + "/";
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_list_row, viewGroup, false);
        return new ImageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ImageViewHolder imageViewHolder, int i) {
        final Image image = images.get(i);
        int THUMBSIZE = 256;
        imageViewHolder.imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(folderPath + image.getPath()), THUMBSIZE, THUMBSIZE));
        imageViewHolder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(photoDetailActivity, HandPhotoPreviewActivity.class);
                intent.putExtra(HandPhotoPreviewActivity.FILE_NAME, folderPath + image.getPath());
                photoDetailActivity.startActivity(intent);
            }
        });
        imageViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: clicked delete for image: " + folderPath + image.getPath());
                File file = new File(folderPath + image.getPath());
                Log.d(TAG, "onClick: image deleted with result: " + file.delete());
                photoDetailActivity.initImageList(side);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
