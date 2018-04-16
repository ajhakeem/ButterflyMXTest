package com.example.jaseem.butterflymx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LoginActivity.class.getName();
    private static String userJID, userPassword, userIsLoggedIn, isUserDisconnected, userIsRemembered = null;

    private AbstractXMPPConnection connection;

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor spEditor;

    private EditText etLoginUsername, etLoginPassword;
    private Button bLogin;
    private CheckBox cbRemember;
    private ProgressBar pbLoginProgress;
    private CheckConnection checkConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        spEditor = sharedPreferences.edit();

        userJID = getString(R.string.spUserJIDKey);
        userPassword = getString(R.string.spUserPasswordKey);
        isUserDisconnected = getString(R.string.spIsUserDisconnectedKey);
        userIsRemembered = getString(R.string.spIsUserRememberedKey);
        userIsLoggedIn = getString(R.string.spUserIsLoggedInKey);
        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        bLogin = findViewById(R.id.bLogin);
        bLogin.setOnClickListener(this);
        cbRemember = findViewById(R.id.cbRememberMe);
        cbRemember.setChecked(false);
        pbLoginProgress = findViewById(R.id.pbLoginProgress);
        checkConnection = new CheckConnection(LoginActivity.this);

        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    //Set remember checkbox listener and preload user info if saved
    public void initUI() {
        cbRemember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked) {
                    spEditor.putBoolean(userIsRemembered, false);
                    spEditor.remove(userJID);
                    spEditor.remove(userPassword);
                    spEditor.apply();
                }
            }
        });
        if (sharedPreferences.getBoolean(userIsRemembered, false)) {
            cbRemember.setChecked(true);
            etLoginUsername.setText(sharedPreferences.getString(userJID, "").trim());
            etLoginPassword.setText(sharedPreferences.getString(userPassword, "").trim());
        }
    }

    @Override
    public void onClick(View view) {
        if (view == bLogin) {
            //If fields are not empty, continue
            if (checkFields()) {
                pbLoginProgress.setVisibility(View.VISIBLE);

                String username = etLoginUsername.getText().toString().trim();
                String password = etLoginPassword.getText().toString().trim();

                //If user currently has internet connection
                if (checkConnection.isConnected()) {
                    new LoginTask(LoginActivity.this, username, password).execute();
                } else {
                    pbLoginProgress.setVisibility(View.GONE);
                    Toast.makeText(this, "Check your connection and try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Check fields and try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Check that fields are not empty
    public boolean checkFields() {
        String checkUser = etLoginUsername.getText().toString().trim();
        String checkPass = etLoginPassword.getText().toString().trim();

        return (checkUser.length() > 0 && checkPass.length() > 0);
    }

    //Do login task on separate thread to prevent UI block
    private static class LoginTask extends AsyncTask<String, Void, Void> {
        WeakReference<LoginActivity> loginActivityWeakReference;
        String username, password = null;

        public LoginTask(LoginActivity context, String username, String password) {
            this.username = username;
            this.password = password;
            loginActivityWeakReference = new WeakReference<LoginActivity>(context);
        }

        @Override
        protected Void doInBackground(String... strings) {
            LoginActivity loginActivity = loginActivityWeakReference.get();
            try {
                loginActivity.connection = ((SmackClient)loginActivity.getApplication()).initializeClient(loginActivity, username, password);
                loginActivity.connection.connect().login();
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            LoginActivity loginActivity = loginActivityWeakReference.get();

            if (loginActivity.connection.isConnected() && !loginActivity.connection.isAuthenticated()) {
                try {
                    if (loginActivity.pbLoginProgress.getVisibility() == View.VISIBLE) {
                        loginActivity.pbLoginProgress.setVisibility(View.GONE);
                        Toast.makeText(loginActivity, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NullPointerException npe) {
                    Log.d(TAG, "Progress bar npe : " + npe);
                }
            }

            if (loginActivity.connection.isConnected() && loginActivity.connection.isAuthenticated()) {
                loginActivity.pbLoginProgress.setVisibility(View.GONE);
                loginActivity.spEditor.putString(userJID, username);
                loginActivity.spEditor.putString(userPassword, password);
                loginActivity.spEditor.putBoolean(userIsLoggedIn, true);
                loginActivity.spEditor.putBoolean(isUserDisconnected, false);

                if (loginActivity.cbRemember.isChecked()) {
                    loginActivity.spEditor.putBoolean(userIsRemembered, true);
                } else {
                    loginActivity.spEditor.putBoolean(userIsRemembered, false);
                }
                loginActivity.spEditor.apply();

                Intent chatIntent = new Intent(loginActivity, ChatActivity.class);
                loginActivity.startActivity(chatIntent);
                loginActivity.finish();
            }
        }
    }
}
