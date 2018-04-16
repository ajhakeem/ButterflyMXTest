package com.example.jaseem.butterflymx;

import android.support.annotation.Nullable;

/**
 * Created by Jaseem on 4/12/18.
 */

public interface LoginCallback {

    void onSuccess(@Nullable String message);
    void onFailure(String message);

}
