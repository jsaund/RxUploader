package com.jagsaund.rxuploader.sample;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.RequestManager;
import com.jagsaund.rxuploader.job.Status;
import com.jagsaund.rxuploader.job.StatusType;
import com.jagsaund.rxuploader.sample.model.data.DataModel;
import com.jagsaund.rxuploader.sample.model.data.DataModelType;
import com.jagsaund.rxuploader.sample.model.data.PhotoDataModel;
import com.jagsaund.rxuploader.sample.model.data.UploadDataModel;

public class UploadViewHolder extends RecyclerView.ViewHolder {
    @NonNull
    public static UploadViewHolder create(@NonNull ViewGroup parent,
            @NonNull RequestManager glide) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View itemView = inflater.inflate(R.layout.upload_item, parent, false);
        final ImageView thumbnail = (ImageView) itemView.findViewById(R.id.image_thumbnail);
        final TextView filename = (TextView) itemView.findViewById(R.id.text_filename);
        final TextView status = (TextView) itemView.findViewById(R.id.text_status);
        return new UploadViewHolder(itemView, thumbnail, filename, status, glide);
    }

    @NonNull private final ImageView thumbnail;
    @NonNull private final TextView filename;
    @NonNull private final TextView status;
    @NonNull private final RequestManager glide;

    @VisibleForTesting
    UploadViewHolder(@NonNull View itemView, @NonNull ImageView thumbnail,
            @NonNull TextView filename, @NonNull TextView status, @NonNull RequestManager glide) {
        super(itemView);
        this.thumbnail = thumbnail;
        this.filename = filename;
        this.status = status;
        this.glide = glide;
    }

    public void bind(@NonNull DataModel dataModel) {
        @DataModelType final int type = dataModel.type();
        switch (type) {
            case DataModelType.PHOTO: {
                status.setVisibility(View.GONE);

                final PhotoDataModel photoDataModel = (PhotoDataModel) dataModel;
                glide.load(photoDataModel.getUrl()).into(thumbnail);
                filename.setText(photoDataModel.getName());
                break;
            }
            case DataModelType.UPLOAD: {
                status.setVisibility(View.VISIBLE);

                final UploadDataModel uploadDataModel = (UploadDataModel) dataModel;
                final Status s = uploadDataModel.getStatus();
                filename.setText(uploadDataModel.getName());
                thumbnail.setImageDrawable(new ColorDrawable(Color.DKGRAY));

                final StatusType statusType = s.statusType();
                switch (statusType) {
                    case QUEUED: {
                        status.setText("queued");
                        status.setTextColor(Color.GRAY);
                        break;
                    }
                    case SENDING: {
                        status.setText(String.valueOf(s.progress()) + "%");
                        status.setTextColor(Color.DKGRAY);
                        break;
                    }
                    case COMPLETED: {
                        status.setText("uploaded");
                        status.setTextColor(Color.GREEN);
                        break;
                    }
                    case FAILED: {
                        status.setText("failed");
                        status.setTextColor(Color.RED);
                        break;
                    }
                    default: {
                        status.setText("???");
                        status.setTextColor(Color.RED);
                    }
                }
                break;
            }
        }
    }
}
