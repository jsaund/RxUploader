package com.jagsaund.rxuploader.sample;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.jagsaund.rxuploader.sample.model.UploadPhotoList;
import com.jagsaund.rxuploader.sample.model.data.DataModel;
import java.util.List;

public class UploadRecyclerAdapter extends RecyclerView.Adapter<UploadViewHolder> {
    @NonNull private final UploadPhotoList photoList;

    public UploadRecyclerAdapter() {
        photoList = new UploadPhotoList(this);
    }

    @NonNull
    @Override
    public UploadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return UploadViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull UploadViewHolder holder, int position) {
        final DataModel m = getItemAt(position);
        if (m == null) {
            return;
        }
        holder.bind(m);
    }

    @Override
    public int getItemViewType(int position) {
        final DataModel m = getItemAt(position);
        if (m == null) {
            return RecyclerView.INVALID_TYPE;
        }
        return m.type();
    }

    @Override
    public long getItemId(int position) {
        final DataModel m = getItemAt(position);
        if (m == null) {
            return RecyclerView.NO_ID;
        }
        return m.id().hashCode();
    }

    @Nullable
    public DataModel getItemAt(int position) {
        if (position == RecyclerView.NO_POSITION || position < 0 || position >= getItemCount()) {
            return null;
        }
        return photoList.get(position);
    }

    public void addAll(@NonNull List<DataModel> data) {
        photoList.addAll(data);
    }

    public void add(@NonNull DataModel data) {
        photoList.add(data);
    }

    public void remove(@NonNull DataModel data) {
        photoList.remove(data);
    }

    public void removeItemAt(int index) {
        photoList.removeItemAt(index);
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }
}
