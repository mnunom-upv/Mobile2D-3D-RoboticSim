package com.robotsimulator.eq2;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSWebSocketClient extends WebSocketClient {
    private static final String TAG = "ROSWebSocketClient";
    private final Activity activity;
    private final TextView txtDecision;
    private ConnectionCallback connectionCallback;

    public interface ConnectionCallback {
        void onConnectionSuccess();
        void onConnectionError(Exception e);
        void onConnectionClosed();
    }

    public ROSWebSocketClient(String serverUri, Activity activity, TextView txtDecision)
            throws URISyntaxException {
        super(new URI(serverUri));
        this.activity = activity;
        this.txtDecision = txtDecision;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        Log.d(TAG, "Connection established");

        // Subscribe to the decisions topic
        String subscribeMessage = "{ \"op\": \"subscribe\", \"topic\": \"/decisiones_pub\", \"type\": \"std_msgs/String\" }";
        send(subscribeMessage);

        if (connectionCallback != null) {
            activity.runOnUiThread(() -> connectionCallback.onConnectionSuccess());
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Received message: " + message);

        activity.runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(message);

                if (json.has("msg")) {
                    JSONObject msg = json.getJSONObject("msg");
                    if (msg.has("data")) {
                        String decisionText = msg.getString("data");
                        txtDecision.setText("Decision: " + decisionText);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing message: " + e.getMessage());
            }
        });
    }

    public void publishCommand(String command) {
        try {
            String message = String.format(
                    "{ \"op\": \"publish\", " +
                            "\"topic\": \"/decisiones_pub\", " +
                            "\"msg\": { \"data\": \"%s\" } }",
                    command
            );
            send(message);
            Log.d(TAG, "Published command: " + command);
        } catch (Exception e) {
            Log.e(TAG, "Error publishing command: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Connection closed: " + reason);
        if (connectionCallback != null) {
            activity.runOnUiThread(() -> connectionCallback.onConnectionClosed());
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "WebSocket error: " + ex.getMessage());
        if (connectionCallback != null) {
            activity.runOnUiThread(() -> connectionCallback.onConnectionError(ex));
        }
    }

    public boolean isConnected() {
        return getConnection() != null && getConnection().isOpen();
    }
}