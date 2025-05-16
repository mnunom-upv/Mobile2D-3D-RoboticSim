package com.robotsimulator.ind;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.robotsimulator.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ROSWebSocketClient extends WebSocketClient {
    private final Activity activity;
    private final TextView txtDistance;
    private final TextView txtCenter;
    private final TextView txtRight;
    private final TextView txtLeft;
    private final TextView txtDecision;
    private final TextView tvSignalStatus;
    private final ImageView imgSignal;

    public ROSWebSocketClient(
            String serverUri, Activity activity,
            TextView txtDistance, TextView txtCenter,
            TextView txtRight, TextView txtLeft, TextView txtDecision,
            TextView tvSignalStatus, ImageView imgSignal)
            throws URISyntaxException {
        super(new URI(serverUri));
        this.activity = activity;
        this.txtDistance = txtDistance;
        this.txtCenter = txtCenter;
        this.txtRight = txtRight;
        this.txtLeft = txtLeft;
        this.txtDecision = txtDecision;
        this.tvSignalStatus = tvSignalStatus;
        this.imgSignal = imgSignal;
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
        String subscribeUltrasonicos = "{ \"op\": \"subscribe\", \"topic\": \"/ultrasonicos_pub\" }";
        String subscribeCenter = "{ \"op\": \"subscribe\", \"topic\": \"/center_tra_pub\" }";
        String subscribeRight = "{ \"op\": \"subscribe\", \"topic\": \"/right_tra_pub\" }";
        String subscribeLeft = "{ \"op\": \"subscribe\", \"topic\": \"/left_tra_pub\" }";
        String subscribeDecision = "{ \"op\": \"subscribe\", \"topic\": \"/decisiones_pub\" }";

        // Suscripciones a tópicos de QR
        String subscribeQrIzq = "{ \"op\": \"subscribe\", \"topic\": \"/deteccionizq\" }";
        String subscribeQrDer = "{ \"op\": \"subscribe\", \"topic\": \"/deteccionder\" }";
        String subscribeQrAlto = "{ \"op\": \"subscribe\", \"topic\": \"/deteccionalto\" }";
        String subscribeQrRecto = "{ \"op\": \"subscribe\", \"topic\": \"/deteccionrecto\" }";

        // Enviar todas las suscripciones
        send(subscribeUltrasonicos);
        send(subscribeCenter);
        send(subscribeRight);
        send(subscribeLeft);
        send(subscribeDecision);
        send(subscribeQrIzq);
        send(subscribeQrDer);
        send(subscribeQrAlto);
        send(subscribeQrRecto);
    }

    @Override
    public void onMessage(final String message) {
        activity.runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(message);
                String topic = json.getString("topic");
                String data = json.getString("msg");

                switch (topic) {
                    case "/ultrasonicos_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            double distance = jsonObject.getDouble("data");
                            txtDistance.setText(String.format("Distancia: %.2f cm", distance));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/center_tra_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            double centerValue = jsonObject.getDouble("data");
                            txtCenter.setText(String.format("Sensor Centro: %.2f", centerValue));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/right_tra_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            double rightValue = jsonObject.getDouble("data");
                            txtRight.setText(String.format("Sensor Derecha: %.2f", rightValue));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/left_tra_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            double leftValue = jsonObject.getDouble("data");
                            txtLeft.setText(String.format("Sensor Izquierda: %.2f", leftValue));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/decisiones_pub":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String decisionText = jsonObject.getString("data");
                            txtDecision.setText("Decisión: " + decisionText);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/deteccionizq":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String detectionMessage = jsonObject.getString("data");
                            if ("Izquierda".equals(detectionMessage)) {
                                showSignal("Izquierda");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/deteccionder":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String detectionMessage = jsonObject.getString("data");
                            if ("Derecha".equals(detectionMessage)) {
                                showSignal("Derecha");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/deteccionalto":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String detectionMessage = jsonObject.getString("data");
                            if ("Alto".equals(detectionMessage)) {
                                showSignal("Alto");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    case "/deteccionrecto":
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            String detectionMessage = jsonObject.getString("data");
                            if ("Recto".equals(detectionMessage)) {
                                showSignal("Recto");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e("ROSWebSocket", "Error processing message: " + e.getMessage());
            }
        });
    }

    private void showSignal(String signalType) {
        // Mostrar la imagen correspondiente
        imgSignal.setVisibility(View.VISIBLE);
        tvSignalStatus.setVisibility(View.GONE);

        // Establecer la imagen según el tipo de señal
        switch (signalType) {
            case "Detenerse":
                imgSignal.setImageResource(R.drawable.stop);
                break;
            case "Izquierda":
                imgSignal.setImageResource(R.drawable.izquierda);
                break;
            case "Derecha":
                imgSignal.setImageResource(R.drawable.derecha);
                break;
            case "Recto":
                imgSignal.setImageResource(R.drawable.recto);
                break;
        }

        // Programar el retorno a "No señalización" después de 3 segundos
        new Handler().postDelayed(() -> {
            tvSignalStatus.setVisibility(View.VISIBLE);
            imgSignal.setVisibility(View.GONE);
            tvSignalStatus.setText("No Signal");
        }, 3000);
    }

    public boolean isConnected() {
        return this.getConnection() != null && this.getConnection().isOpen();
    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
            Log.d("ROSWebSocket", "Connection closed: " + reason);
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