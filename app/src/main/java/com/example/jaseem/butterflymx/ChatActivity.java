package com.example.jaseem.butterflymx;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.chat2.OutgoingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Jaseem on 4/12/18.
 */

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = ChatActivity.class.getSimpleName();

    private static String userJID, userPassword, userIsLoggedIn, isUserDisconnected, userChatsKey = null;

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor spEditor;
    private ChatManager chatManager;
    private static Handler uiHandler;
    private BroadcastReceiver receiver;
    private Gson gson;

    private ArrayList<MessageModel> messagesList = new ArrayList<>();
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private static AbstractXMPPConnection connection;
    private Button bLogout, bPos, bNeg;
    private TextView tvConnectionStatus, tvNoMessages;
    private FloatingActionButton fabCompose;
    private Dialog composeDialog;
    private EditText etMessageRecipient, etMessageText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Set cache keys
        userJID = getString(R.string.spUserJIDKey);
        userPassword = getString(R.string.spUserPasswordKey);
        isUserDisconnected = getString(R.string.spIsUserDisconnectedKey);
        userIsLoggedIn = getString(R.string.spUserIsLoggedInKey);
        userChatsKey = getString(R.string.spUserChatsKey);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this);
        spEditor = sharedPreferences.edit();

        uiHandler = new Handler(Looper.getMainLooper());
        bLogout = findViewById(R.id.bLogout);
        bLogout.setOnClickListener(this);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvNoMessages = findViewById(R.id.tvNoMessages);
        fabCompose = findViewById(R.id.fabCompose);
        fabCompose.setOnClickListener(this);
        rvChat = findViewById(R.id.rvChat);
        chatAdapter = new ChatAdapter(messagesList);

        //Check user connection status
        if (!sharedPreferences.getBoolean(isUserDisconnected, true)) {
            checkConnection(((SmackClient)getApplication()).getStatus());
        }

        //Create network listener
        IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager
                        .getActiveNetworkInfo();

                // Check internet connection state
                if (networkInfo != null && networkInfo.isConnected()) {
                    if (!connection.isConnected()) {
                        checkConnection("DISCONNECTED");
                    } else {
                        Log.d(TAG, "Connected to internet.");
                        checkConnection("CONNECTED");
                    }
                } else {
                    Log.d(TAG, "Disconnected from internet");
                    checkConnection("ERROR");
                }
            }
        };

        //Register listener
        ChatActivity.this.registerReceiver(receiver, intentFilter);

        //Set recycler view layout and attach adapter
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rvChat.setLayoutManager(layoutManager);
        rvChat.setItemAnimator(new DefaultItemAnimator());

        init();
    }

    //Load cached messages, and set chat incoming listeners
    public void init() {

        //Retrieve cached messages
        gson = new Gson();
        String json = sharedPreferences.getString(userChatsKey, "");
        try {
            messagesList.clear();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                MessageModel model = new MessageModel();
                JSONObject object = jsonArray.getJSONObject(i);
                model.setMessageSender(object.getString("from"));
                model.setMessageText(object.getString("body"));
                messagesList.add(model);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (messagesList.size() > 1) {
                        tvNoMessages.setVisibility(View.GONE);
                    }
                    chatAdapter.notifyDataSetChanged();
                }
            });
        } catch (JSONException jse) {
            Log.e(TAG, "Error restoring chats.");
        }

        //Get the saved SmackClient connection
        connection = ((SmackClient)getApplication()).getConnection();

        //Attach incoming message listeners to update UI upon message received
        chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener(new IncomingChatMessageListener() {
            @Override
            public void newIncomingMessage(EntityBareJid from, final Message message, Chat chat) {
                final MessageModel messageModel = new MessageModel();
                messageModel.setMessageSender(from.toString());
                messageModel.setMessageText(message.getBody());
                Date now = new Date();
                Long timestamp = now.getTime()/1000;
                messageModel.setTimestamp(timestamp);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messagesList.add(messageModel);
                        sortChats();
                        if (messagesList.size() > 1) {
                            tvNoMessages.setVisibility(View.GONE);
                        }
                        chatAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        //Attach outgoing message listener
        chatManager.addOutgoingListener(new OutgoingChatMessageListener() {
            @Override
            public void newOutgoingMessage(EntityBareJid to, Message message, Chat chat) {
                final MessageModel messageModel = new MessageModel();
                messageModel.setMessageSender(to.toString());
                messageModel.setMessageText(message.getBody());
                Date now = new Date();
                Long timestamp = now.getTime()/1000;
                messageModel.setTimestamp(timestamp);
            }
        });

        if (rvChat.getAdapter() == null) {
            rvChat.setAdapter(chatAdapter);
        }
    }

    //Runs on ui thread of activity
    public static void runOnUI(Runnable runnable) {
        uiHandler.post(runnable);
    }

    //Check current internet connection and handles conneciton status display
    public void checkConnection(String status) {
        switch (status) {
            case "CONNECTED":
                tvConnectionStatus.setText(getString(R.string.status_connected));
                tvConnectionStatus.setTextColor(getResources().getColor(R.color.green));
                break;
            case "ERROR":
                Log.e(TAG, "Connection error");
                tvConnectionStatus.setText(getString(R.string.status_disconnected));
                tvConnectionStatus.setTextColor(getResources().getColor(R.color.red));
                break;
            case "CONNECTING":
                Log.e(TAG, "Connecting...");
                tvConnectionStatus.setText(getString(R.string.status_connecting));
                tvConnectionStatus.setTextColor(getResources().getColor(R.color.yellow));
            case "DISCONNECTED":
                Log.e(TAG, "Disconnected");
                sortChats();
                String json = gson.toJson(messagesList);
                spEditor.putString(userChatsKey, json);
                spEditor.apply();
                tvConnectionStatus.setText(getString(R.string.status_disconnected));
                tvConnectionStatus.setTextColor(getResources().getColor(R.color.red));
                try {
                    tvConnectionStatus.setText(getString(R.string.status_connecting));
                    tvConnectionStatus.setTextColor(getResources().getColor(R.color.yellow));
                    new ReconnectTask(ChatActivity.this, sharedPreferences.getString(userJID, ""), sharedPreferences.getString(userPassword, "")).execute();
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
                break;
            case "DISCONNECTING":
                break;
            default:
                break;
        }
    }

    //Send message pop-up dialog
    public void composeDialog() {
        composeDialog = new Dialog(ChatActivity.this);
        composeDialog.setContentView(R.layout.layout_compose_dialog);

        bPos = composeDialog.findViewById(R.id.bDialogPositive);
        bNeg = composeDialog.findViewById(R.id.bDialogNegative);
        TextView tvDialogTitle = composeDialog.findViewById(R.id.tvDialogTitle);
        etMessageRecipient = composeDialog.findViewById(R.id.etMessageRecipient);
        etMessageText = composeDialog.findViewById(R.id.etMessageText);

        tvDialogTitle.setText(this.getString(R.string.compose_title));
        bPos.setText(this.getString(R.string.compose_send));
        bNeg.setText(this.getString(R.string.compose_cancel));
        bPos.setEnabled(true);
        bNeg.setEnabled(true);
        bPos.setOnClickListener(this);
        bNeg.setOnClickListener(this);

        composeDialog.show();
    }

    //Check that fields are not empty
    public boolean checkFields() {
        return (etMessageRecipient.getText().toString().trim().length() > 0 && etMessageText.getText().toString().trim().length() > 0);
    }

    //OnClick functions
    @Override
    public void onClick(View view) {
        if (view == bLogout) {
            if (connection.isConnected()) {
                ChatActivity.this.unregisterReceiver(receiver);
                sortChats();
                String json = gson.toJson(messagesList);
                spEditor.putString(userChatsKey, json);
                spEditor.putBoolean(userIsLoggedIn, false);
                spEditor.putBoolean(isUserDisconnected, true);
                spEditor.apply();
                if (spEditor.commit()) {
                    ((SmackClient)getApplication()).disconnect();
                    Intent loginIntent = new Intent(ChatActivity.this, LoginActivity.class);
                    ChatActivity.this.startActivity(loginIntent);
                    ChatActivity.this.finish();
                }
            }
        }

        if (view == fabCompose) {
            if (connection.isConnected()) {
                composeDialog();
            } else {
                Toast.makeText(this, "Try again after regaining connection!", Toast.LENGTH_SHORT).show();
            }
        }

        if (view == bPos) {
            if (checkFields()) {
                new SendTask(ChatActivity.this, connection, etMessageRecipient.getText().toString().trim(), etMessageText.getText().toString().trim()).execute();

                if (composeDialog.isShowing()) {
                    composeDialog.dismiss();
                }
            } else {
                Toast.makeText(this, "Please make sure all fields are filled.", Toast.LENGTH_SHORT).show();
            }
        }

        if (view == bNeg) {
            composeDialog.dismiss();
        }
    }

    //Sort chats by time received/sent
    public void sortChats() {
        Collections.sort(messagesList, new Comparator<MessageModel>() {
            @Override
            public int compare(MessageModel m0, MessageModel m1) {
                if (m0.getTimestamp() < m1.getTimestamp()) {
                    return 1;
                } else if (m1.getTimestamp() < m0.getTimestamp()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    //Save to cache when paused
    @Override
    protected void onPause() {
        sortChats();
        String json = gson.toJson(messagesList);
        spEditor.putString(userChatsKey, json);
        spEditor.apply();
        super.onPause();
    }

    //Save to cache when stopped
    @Override
    protected void onStop() {
        sortChats();
        String json = gson.toJson(messagesList);
        spEditor.putString(userChatsKey, json);
        spEditor.apply();
        super.onStop();
    }

    //Save user state when activity is destroyed
    @Override
    protected void onDestroy() {
        spEditor.putBoolean(isUserDisconnected, true);
        spEditor.apply();
        super.onDestroy();
    }

    //Async task to send messages to a user, runs on separate thread
    private static class SendTask extends AsyncTask<String, Void, Void> {

        private WeakReference<ChatActivity> chatActivityWeakReference;
        private String recipient;
        private String messageText;
        private EntityBareJid recipientJID;
        AbstractXMPPConnection conn;

        private SendTask(ChatActivity context, AbstractXMPPConnection conn1, String recipient, String messageText) {
            this.recipient = recipient;
            this.messageText = messageText;
            this.conn = conn1;
            chatActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... strings) {
            final ChatActivity chatActivity = chatActivityWeakReference.get();
            try {
                recipientJID = JidCreate.entityBareFrom(recipient + "@404.city");
                Chat chat = chatActivity.chatManager.chatWith(recipientJID);
                chat.send(messageText);
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            final ChatActivity chatActivity = chatActivityWeakReference.get();
            MessageModel messageModel = new MessageModel();
            messageModel.setMessageSender(sharedPreferences.getString(userJID, "") + "@404.city");
            messageModel.setMessageText(messageText);
            Date now = new Date();
            Long timestamp = now.getTime()/1000;
            messageModel.setTimestamp(timestamp);
            chatActivity.messagesList.add(messageModel);
            runOnUI(new Runnable() {
                @Override
                public void run() {
                    chatActivity.sortChats();
                    if (chatActivity.messagesList.size() > 1) {
                        chatActivity.tvNoMessages.setVisibility(View.GONE);
                    }
                    chatActivity.chatAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    //Async task to reconnect user to Smack service
    private static class ReconnectTask extends AsyncTask<String, Void, Void> {
        private WeakReference<ChatActivity> chatActivityWeakReference;
        String username, password = null;

        private ReconnectTask(ChatActivity context, String username, String password) {
            this.username = username;
            this.password = password;
            chatActivityWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... strings) {
            final ChatActivity chatActivity = chatActivityWeakReference.get();
            try {
                connection = ((SmackClient)chatActivity.getApplication()).initializeClient(chatActivity, username, password);
                connection.connect().login();
            } catch (Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            final ChatActivity chatActivity = chatActivityWeakReference.get();
            super.onPostExecute(aVoid);
            if (connection.isConnected() && connection.isAuthenticated()) {
                spEditor.putBoolean(userIsLoggedIn, true);
                spEditor.putBoolean(isUserDisconnected, false);
                spEditor.apply();
                chatActivity.init();
                chatActivity.checkConnection("CONNECTED");
            }
        }
    }
}
