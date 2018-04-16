package com.example.jaseem.butterflymx;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * Created by Jaseem on 4/12/18.
 */

public class SmackClient extends Application implements LoginCallback, ConnectionListener {

    public static final String DISCONNECTED = "DISCONNECTED";
    public static final String CONNECTED = "CONNECTED";
    public static final String RECONNECTING = "RECONNECTING";
    public static final String ERROR = "ERROR";

    private Context mContext;
    private AbstractXMPPConnection conn;
    private String connectionStatus;

    public SmackClient() {
    }

    //Initialize client to 404.city public server and attach connecition state listener
    public AbstractXMPPConnection initializeClient(Context context, String username, String password) {
        this.mContext = context;
        XMPPTCPConnectionConfiguration config;
        try {
            config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(username, password)
                    .setConnectTimeout(30000)
                    .setXmppDomain("404.city")
                    .setHost("404.city")
                    .setPort(5222)
                    .setDebuggerEnabled(true)
                    .setKeystoreType(null)
                    .build();
            conn = new XMPPTCPConnection(config);
            conn.addConnectionListener(this);
        } catch (Exception e) {
            onFailure(e.getMessage());
        }

        return conn;
    }

    public AbstractXMPPConnection getConnection() {
        return conn;
    }

    public String getStatus() {
        return connectionStatus;
    }

    //disconnect the current connection
    public void disconnect() {
        if (conn != null) {
            conn.disconnect();
        }
    }


    @Override
    public void connected(XMPPConnection connection) {
        connectionStatus = CONNECTED;
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        connectionStatus = CONNECTED;
    }

    @Override
    public void connectionClosed() {
        connectionStatus = DISCONNECTED;
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        connectionStatus = ERROR;
    }

    @Override
    public void reconnectionSuccessful() {
        connectionStatus = CONNECTED;
    }

    @Override
    public void reconnectingIn(int seconds) {
        connectionStatus = RECONNECTING;
    }

    @Override
    public void reconnectionFailed(Exception e) {
        connectionStatus = ERROR;
    }

    @Override
    public void onSuccess(String message) {
//        Log.d(mContext.getClass().getSimpleName(), "Connected successfully");
    }

    @Override
    public void onFailure(String message) {
        Toast.makeText(mContext, mContext.getString(R.string.connection_failure), Toast.LENGTH_SHORT).show();
    }
}
