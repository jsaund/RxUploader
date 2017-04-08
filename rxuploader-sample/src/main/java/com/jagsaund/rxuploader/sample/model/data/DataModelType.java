package com.jagsaund.rxuploader.sample.model.data;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.jagsaund.rxuploader.sample.model.data.DataModelType.PHOTO;
import static com.jagsaund.rxuploader.sample.model.data.DataModelType.UPLOAD;

@IntDef({UPLOAD, PHOTO})
@Retention(RetentionPolicy.SOURCE)
public @interface DataModelType {
    int UPLOAD = 1;
    int PHOTO = 2;
}
