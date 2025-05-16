package com.robotsimulator.eq3.aut;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSWebSocketClient extends WebSocketClient {
    private static final String TAG = "ROSWebSocket";
    private final Activity activity;
    private final TextView txtDecision;
    private GameRenderer renderer;  // Referencia al renderer

    public ROSWebSocketClient(String serverUri, Activity activity, TextView txtDecision, GameRenderer renderer)
            throws URISyntaxException {
        super(new URI(serverUri));
        this.activity = activity;
        this.txtDecision = txtDecision;
        this.renderer = renderer;
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
        Log.d(TAG, "Connection established");
        if (connectionCallback != null) {
            connectionCallback.onConnectionSuccess();
        }

        // Suscripción al tópico de decisiones
        String subscribeDecision = "{ \"op\": \"subscribe\", \"topic\": \"/decisiones_pub\" }";
        send(subscribeDecision);
    }

    @Override
    public void onMessage(final String message) {
        activity.runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(message);

                // Verificar que sea un mensaje de tipo mensaje
                if (json.has("op") && json.getString("op").equals("publish")) {
                    String topic = json.getString("topic");

                    if (topic.equals("/decisiones_pub")) {
                        JSONObject msg = json.getJSONObject("msg");
                        String decisionText = msg.getString("data");
                        Log.d(TAG, "Decisión recibida: " + decisionText);

                        // Actualizar el texto en la UI
                        txtDecision.setText("Decision: " + decisionText);

                        // Actualizar el renderer con la nueva decisión
                        if (renderer != null) {
                            renderer.updateDecision(decisionText);
                            Log.d(TAG, "Decisión enviada al renderer: " + decisionText);
                        } else {
                            Log.e(TAG, "Renderer no inicializado");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar mensaje: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public boolean isConnected() {
        return this.getConnection() != null && this.getConnection().isOpen();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Conexión cerrada: " + reason);
        if (connectionCallback != null) {
            activity.runOnUiThread(() -> connectionCallback.onConnectionClosed());
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "Error: " + ex.getMessage());
        if (connectionCallback != null) {
            activity.runOnUiThread(() -> connectionCallback.onConnectionError(ex));
        }
    }
}