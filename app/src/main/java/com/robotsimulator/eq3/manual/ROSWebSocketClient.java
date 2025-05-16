package com.robotsimulator.eq3.manual;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSWebSocketClient extends WebSocketClient {
    private final Activity activity;
    private final TextView txtDecision;

    public ROSWebSocketClient(
            String serverUri, Activity activity, TextView txtDecision)
            throws URISyntaxException {
        super(new URI(serverUri));
        this.activity = activity;
        this.txtDecision = txtDecision;
    }
    public interface ConnectionCallback {
        void onConnectionSuccess();
        void onConnectionError(Exception e);
        void onConnectionClosed();
    }
    private ConnectionCallback connectionCallback;

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        Log.d("ROSWebSocket", "Connection established");
        if (connectionCallback != null) {
            connectionCallback.onConnectionSuccess();
        }

        // Suscripciones a tópicos de sensores
        String subscribeDecision = "{ \"op\": \"subscribe\", \"topic\": \"/decisiones_pub\" }";


        // Enviar todas las suscripciones
        send(subscribeDecision);
    }

    @Override
    public void onMessage(final String message) {
        activity.runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(message);
                String topic = json.getString("topic");
                String data = json.getString("msg");

                switch (topic) {

                    case "/decisiones_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String decisionText = jsonObject.getString("data");
                            txtDecision.setText("Decisión: " + decisionText);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e("ROSWebSocket", "Error al procesar mensaje: " + e.getMessage());
            }
        });
    }



    public boolean isConnected() {
        return this.getConnection() != null && this.getConnection().isOpen();
    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("ROSWebSocket", "Conexión cerrada: " + reason);
        if (connectionCallback != null) {
            connectionCallback.onConnectionClosed();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e("ROSWebSocket", "Error: " + ex.getMessage());
        if (connectionCallback != null) {
            connectionCallback.onConnectionError(ex);
        }
    }
}