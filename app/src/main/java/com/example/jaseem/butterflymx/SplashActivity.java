package com.example.jaseem.butterflymx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.lang.ref.WeakReference;

/**
 * Created by Jaseem on 4/12/18.
 */

public class SplashActivity extends AppCompatActivity implements LoginCallback{

    private static final String TAG = SplashActivity.class.getSimpleName();

    private static String userJID, userPassword, userIsLoggedIn, isUserDisconnected = null;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;
    private ImageView ivSplashLogo;
    private TextView tvSplashStatus;
    private CheckConnection checkConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
        spEditor = sharedPreferences.edit();
        ivSplashLogo = findViewById(R.id.ivSplashLogo);
        tvSplashStatus = findViewById(R.id.tvSplashStatus);
        userJID = getString(R.string.spUserJIDKey);
        userPassword = getString(R.string.spUserPasswordKey);
        userIsLoggedIn = getString(R.string.spUserIsLoggedInKey);
        isUserDisconnected = getString(R.string.spIsUserDisconnectedKey);
        checkConnection = new CheckConnection(SplashActivity.this);

        checkAuthentication();
    }

    //Check if user was still logged in when closing app
    public void checkAuthentication() {
        ivSplashLogo.startAnimation(AnimationUtils.loadAnimation(SplashActivity.this, R.anim.fade_in));
        boolean isUserLoggedIn = sharedPreferences.getBoolean(userIsLoggedIn, false);

        //If user was logged in and currently connected to internet, auto-login. Else, send to login page
        if (isUserLoggedIn && checkConnection.isConnected()) {
            tvSplashStatus.setVisibility(View.VISIBLE);
            if (sharedPreferences.getString(userJID, "").length() < 1
                    || sharedPreferences.getString(userPassword, "").length() < 1) {
                Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
                SplashActivity.this.startActivity(loginIntent);
                SplashActivity.this.finish();
            } else {
                new LoginTask(SplashActivity.this, sharedPreferences.getString(userJID, ""), sharedPreferences.getString(userPassword, "")).execute();
            }
        } else {
            Intent loginIntent = new Intent(SplashActivity.this, LoginActivity.class);
            SplashActivity.this.startActivity(loginIntent);
            SplashActivity.this.finish();
        }
    }

    //Welcome toast and send to chat
    @Override
    public void onSuccess(@Nullable String message) {
        spEditor.putBoolean(userIsLoggedIn, true);
        spEditor.putBoolean(isUserDisconnected, false);
        spEditor.apply();
        Intent chatIntent = new Intent(SplashActivity.this, ChatActivity.class);
        SplashActivity.this.startActivity(chatIntent);
        SplashActivity.this.finish();
    }

    @Override
    public void onFailure(String message) {

    }

    //Async task to login user if user was saved on last use
    private static class LoginTask extends AsyncTask<String, Void, Void> {
        WeakReference<SplashActivity> splashActivityWeakReference;
        String username, password = null;

        private LoginTask(SplashActivity context, String username, String password) {
            this.username = username;
            this.password = password;
            splashActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... strings) {
            SplashActivity splashActivity = splashActivityWeakReference.get();
            try {
                AbstractXMPPConnection connection = ((SmackClient)splashActivity.getApplication()).initializeClient(splashActivity, username, password);
                connection.connect().login();
                splashActivity.onSuccess(null);
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
