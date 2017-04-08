package com.jagsaund.rxuploader.sample.model;

import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import com.jagsaund.rxuploader.sample.model.data.DataModel;
import com.jagsaund.rxuploader.sample.model.data.DataModelType;

public class UploadPhotoList extends SortedList<DataModel> {
    public UploadPhotoList(@NonNull final RecyclerView.Adapter adapter) {
        super(DataModel.class, new SortedList.Callback<DataModel>() {

            @Override
            public void onInserted(int position, int count) {
                adapter.notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                adapter.notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                adapter.notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public int compare(@NonNull DataModel o1, @NonNull DataModel o2) {
                // upload jobs are ordered first
                if (o1.type() != o2.type()) {
                    if (o1.type() == DataModelType.UPLOAD) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                return o1.id().compareTo(o2.id());
            }

            @Override
            public void onChanged(int position, int count) {
                adapter.notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(@NonNull DataModel oldItem,
                    @NonNull DataModel newItem) {
                return oldItem.hashCode() == newItem.hashCode();
            }

            @Override
            public boolean areItemsTheSame(@NonNull DataModel item1, @NonNull DataModel item2) {
                return item1.type() == item2.type() && item1.id().equals(item2.id());
            }
        });
    }
}
