/******************************************************************************
 * Copyright 2015-2016 Befrest
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.resana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class BefrestConnection extends Handler {
    private static final String TAG = ResanaLog.TAG_PREF + "BefrestConnection";

    PowerManager.WakeLock connectWakelock;
    public static final String connectWakeLockName = "befrstconnectwakelock";

    Looper mLooper;
    Context appContext;

    Class<?> pushService;

    protected WebSocketReader mReader;
    protected WebSocketWriter mWriter;
    protected HandlerThread mWriterThread;
    protected Socket mTransportChannel;
    private String mWsScheme;
    private String mWsHost;
    private int mWsPort;
    private String mWsPath;
    private String mWsQuery;
    private String[] mWsSubprotocols;
    private List<KeyValue> mWsHeaders;
    private WebSocket.ConnectionHandler mWsHandler;
    protected WebSocketOptions mOptions;

    private boolean refreshRequested;

    private List<String> pendingResanaAcks;

    private Runnable disconnectIfWebSocketHandshakeTimeOut = new Runnable() {
        @Override
        public void run() {
            disconnectAndNotify(WebSocketConnectionHandler.CLOSE_HANDSHAKE_TIME_OUT, "Server Handshake Not Received After " + SERVER_HANDSHAKE_TIMEOUT + "ms");
        }
    };
    private static final int SERVER_HANDSHAKE_TIMEOUT = 7 * 1000;

    //pinging variables and constants
    private static final int[] PING_INTERVAL = {120 * 1000, 300 * 1000, 480 * 1000};
    private static final int PING_TIMEOUT = 5 * 1000;
    private static final String PING_DATA_PREFIX = String.valueOf((int) (Math.random() * 9999));
    private int currentPingId = 0;
    private int prevSuccessfulPings;
    private long lastPingSetTime; //last time a ping was set to be sent delayed

    private boolean restartInProgress;

    private Runnable sendPing = new Runnable() {
        @Override
        public void run() {
            if (appContext != null && !AdService.isDestroied)
                BefrestImpl.startService(appContext, pushService, AdService.PING);
        }
    };

    private Runnable releaseConnectWakeLock = new Runnable() {
        @Override
        public void run() {
            releaseConnectWakeLockIfNeeded();
        }
    };

    private Runnable restart = new Runnable() {
        @Override
        public void run() {
            disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CONNECTION_NOT_RESPONDING, "connection did not respond to ping message after " + PING_TIMEOUT + "ms");
        }
    };

    private void sendPing() {
        if (mWriter != null) {
            ResanaLog.d(TAG, "Sending Ping ...");
            postDelayed(restart, PING_TIMEOUT);
            restartInProgress = true;
            currentPingId = (currentPingId + 1) % 5;
            String payload = PING_DATA_PREFIX + currentPingId;
            mWriter.forward(new WebSocketMessage.Ping(payload.getBytes(Charset.defaultCharset())));
        } else ResanaLog.e(TAG, "could not send ping! writer is null");
    }

    private void onPong(String pongData) {
        boolean isValid = isValidPong(pongData);
        ResanaLog.d(TAG, "onPong(" + pongData + ") " + (isValid ? "valid" : "invalid!"));
        if (!isValid) return;
        cancelUpcommingRestart();
        prevSuccessfulPings++;
        setNextPingToSendInFuture();
        notifyConnectionRefreshedIfNeeded();
    }

    private boolean isValidPong(String pongData) {
        return (PING_DATA_PREFIX + currentPingId).equals(pongData);
    }

    private void cancelFuturePing() {
        ResanaLog.v(TAG, "cancelFuturePing()");
        removeCallbacks(sendPing);
        cancelKeepPingingAlarm();
    }

    private void cancelUpcommingRestart() {
        ResanaLog.v(TAG, "cancelUpcommingRestart()");
        removeCallbacks(restart);
        restartInProgress = false;
    }

    private int getPingInterval() {
        return PING_INTERVAL[prevSuccessfulPings < PING_INTERVAL.length ? prevSuccessfulPings : PING_INTERVAL.length - 1];
    }

    private void revisePinging() {
        if (restartInProgress || System.currentTimeMillis() - lastPingSetTime < getPingInterval() / 2)
            return;
        prevSuccessfulPings++;
        setNextPingToSendInFuture();
        ResanaLog.v(TAG, "resana Pinging Revised");
    }

    private void setNextPingToSendInFuture() {
        setNextPingToSendInFuture(getPingInterval());
    }

    private void setNextPingToSendInFuture(int interval) {
        ResanaLog.v(TAG, "setNextPingToSendInFuture()  interval : " + interval);
        lastPingSetTime = System.currentTimeMillis();
        postDelayed(sendPing, interval);
        setKeepPingingAlarm(interval);
    }

    public BefrestConnection(Context context, Looper looper, WebSocket.ConnectionHandler wsHandler, String url, List<KeyValue> headers) {
        super(looper);
        this.mLooper = looper;
        this.mWsHandler = wsHandler;
        this.appContext = context.getApplicationContext();
        parseWebsocketUri(url, headers);
        pushService = ((BefrestInvocHandler) Proxy.getInvocationHandler(BefrestFactory.getInternalInstance(appContext))).obj.pushService;
        loadPendingResanaAcks();
    }

    public void setKeepPingingAlarm(int pingDelay) {
        int delay = (pingDelay * 2) + 60000;
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, pushService).putExtra(AdService.KEEP_PINGING, true);
        PendingIntent pi = PendingIntent.getService(appContext, BefrestImpl.KEEP_PINGING_ALARM_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = SystemClock.elapsedRealtime() + delay;
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pi);
        ResanaLog.d(TAG, "KeepPinging alarm set for " + delay + " ms");
    }

    public void cancelKeepPingingAlarm() {
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, pushService).putExtra(AdService.KEEP_PINGING, true);
        PendingIntent pi = PendingIntent.getService(appContext, BefrestImpl.KEEP_PINGING_ALARM_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (pi == null) {
            // ReportCrash ?
        } else {
            alarmMgr.cancel(pi);
            pi.cancel();
        }
        ResanaLog.d(TAG, "KeepPinging alarm canceled");
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg.obj instanceof WebSocketMessage.Message) {
                //msg from reader and writer
                handleMsgFromReaderWriter(((WebSocketMessage.Message) msg.obj));
            } else if (msg.obj instanceof BefrestEvent) {
                //msg from push service
                handleBefrestEvent(((BefrestEvent) msg.obj));
            }
        } catch (Throwable t) {
            ResanaLog.e(TAG, "unExpected Exception!");
            // ReportCrash ?
            throw t;
        }
    }

    public void forward(Object message) {
        Message msg = obtainMessage();
        msg.obj = message;
        sendMessage(msg);
    }

    private void sendResanaAck(String msg) {
        pendingResanaAcks.add(msg);
        persistPendingResanaAcks();
        sendNextPendingResanaAck();
    }

    private void sendNextPendingResanaAck() {
        if (pendingResanaAcks.size() > 0) {
            if (sendTextMessage(new WebSocketMessage.ResanaAckMessage(pendingResanaAcks.get(0)))) {
                pendingResanaAcks.remove(0);
                persistPendingResanaAcks();
            }
        }
    }

    private void persistPendingResanaAcks() {
        final Iterator<String> iterator = pendingResanaAcks.iterator();
        String s = "";
        while (iterator.hasNext()) {
            s += iterator.next();
            if (iterator.hasNext())
                s += "@";
        }
        ResanaPreferences.saveString(appContext, ResanaPreferences.PREF_PENDING_RESANA_ACKS, s);
    }

    private void loadPendingResanaAcks() {
        pendingResanaAcks = new ArrayList<>();
        final String s = ResanaPreferences.getPrefs(appContext)
                .getString(ResanaPreferences.PREF_PENDING_RESANA_ACKS, "");
        for (String s1 : s.split("@")) {
            if (!s1.isEmpty())
                pendingResanaAcks.add(s1);
        }
    }

    private boolean sendTextMessage(WebSocketMessage.TextMessage msg) {
        try {
            if (mWriter != null) {
                mWriter.forward(msg);
                ResanaLog.d(TAG, "sending resana ack success :: " + msg.mPayload);
                return true;
            }
        } catch (Exception e) {
            ResanaLog.w(TAG, "problem in sending message\n" + e.getMessage());
        }
        ResanaLog.w(TAG, "sending resana ack failure :: " + msg.mPayload);
        return false;
    }

    public void handleMsgFromReaderWriter(WebSocketMessage.Message msg) {
        ResanaLog.d(TAG, "handleMsgFromReaderWriter: " + msg);
        if (msg instanceof WebSocketMessage.ResanaAckMessage) {
            if (((WebSocketMessage.ResanaAckMessage) msg).sentSuccessfully) {
                sendNextPendingResanaAck();
            } else {
                pendingResanaAcks.add(((WebSocketMessage.ResanaAckMessage) msg).mPayload);
                persistPendingResanaAcks();
            }
        } else if (msg instanceof WebSocketMessage.TextMessage) {
            WebSocketMessage.TextMessage textMessage = (WebSocketMessage.TextMessage) msg;
            revisePinging();
            ResanaLog.v(TAG, "rawMsg: " + textMessage.mPayload);
            if ((android.os.Build.MANUFACTURER.equals("samsung") || android.os.Build.MANUFACTURER.equals("Samsung"))
                    && Build.VERSION.SDK_INT == 21)//todo find a way for solving gson problem for samsungs 5.0.x
                return;
            Ad bmsg = new Ad(textMessage.mPayload);
            if (bmsg.isCorrupted)
                return;
            mWsHandler.onBefrestMessage(bmsg);
        } else if (msg instanceof WebSocketMessage.RawTextMessage) {

            WebSocketMessage.RawTextMessage rawTextMessage = (WebSocketMessage.RawTextMessage) msg;

            mWsHandler.onRawTextMessage(rawTextMessage.mPayload);

        } else if (msg instanceof WebSocketMessage.BinaryMessage) {

            WebSocketMessage.BinaryMessage binaryMessage = (WebSocketMessage.BinaryMessage) msg;
            mWsHandler.onBinaryMessage(binaryMessage.mPayload);

        } else if (msg instanceof WebSocketMessage.Ping) {

            WebSocketMessage.Ping ping = (WebSocketMessage.Ping) msg;
            ResanaLog.v(TAG, "WebSockets Ping received");

            // reply with Pong
            WebSocketMessage.Pong pong = new WebSocketMessage.Pong();
            pong.mPayload = ping.mPayload;
            if (mWriter != null) mWriter.forward(pong);

        } else if (msg instanceof WebSocketMessage.Pong) {
            WebSocketMessage.Pong pong = (WebSocketMessage.Pong) msg;
            onPong(new String(pong.mPayload, Charset.defaultCharset()));

        } else if (msg instanceof WebSocketMessage.Close) {

            WebSocketMessage.Close close = (WebSocketMessage.Close) msg;
            ResanaLog.v(TAG, "WebSockets Close received (" + close.mCode + " - " + close.mReason + ")");
            final int closeCode = (close.mCode == 1000) ? WebSocket.ConnectionHandler.CLOSE_NORMAL : WebSocket.ConnectionHandler.CLOSE_CONNECTION_LOST;
            disconnectAndNotify(closeCode, close.mReason);
        } else if (msg instanceof WebSocketMessage.ServerHandshake) {

            WebSocketMessage.ServerHandshake serverHandshake = (WebSocketMessage.ServerHandshake) msg;
            ResanaLog.v(TAG, "opening handshake received");
            removeCallbacks(disconnectIfWebSocketHandshakeTimeOut);

            if (serverHandshake.mSuccess) {
                try {
                    mWsHandler.onOpen();
                } catch (Exception e) {
                    // if (e instanceof NullPointerException) => Nu(Nazdika#1021)
                    // ReportCrash ?
                    ResanaLog.e(TAG, e);
                    Befrest b = BefrestFactory.getInstance(appContext);
                    b.stop();
                    b.start();
                }
                postDelayed(releaseConnectWakeLock, 2000);
                notifyConnectionRefreshedIfNeeded();
                prevSuccessfulPings = 0;
                setNextPingToSendInFuture();

            } else {
                ResanaLog.w(TAG, "could not call onOpen() .. serverHandshake was not successful");
            }

        } else if (msg instanceof WebSocketMessage.ConnectionLost) {
            @SuppressWarnings("unused")
            WebSocketMessage.ConnectionLost connnectionLost = (WebSocketMessage.ConnectionLost) msg;

            disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CONNECTION_LOST, "WebSockets connection lost");
        } else if (msg instanceof WebSocketMessage.ProtocolViolation) {

            @SuppressWarnings("unused")
            WebSocketMessage.ProtocolViolation protocolViolation = (WebSocketMessage.ProtocolViolation) msg;
            disconnectAndNotify(WebSocketConnectionHandler.CLOSE_PROTOCOL_ERROR, "WebSockets protocol violation");
        } else if (msg instanceof WebSocketMessage.Error) {

            WebSocketMessage.Error error = (WebSocketMessage.Error) msg;
            ResanaLog.e(TAG, error.mException);
            disconnectAndNotify(WebSocketConnectionHandler.CLOSE_INTERNAL_ERROR, "WebSockets internal error (" + error.mException.toString() + ")");

        } else if (msg instanceof WebSocketMessage.ServerError) {

            WebSocketMessage.ServerError error = (WebSocketMessage.ServerError) msg;
            int errCode = WebSocketConnectionHandler.CLOSE_SERVER_ERROR;
            if (error.mStatusCode == 401)
                errCode = WebSocketConnectionHandler.CLOSE_UNAUTHORIZED;
            disconnectAndNotify(errCode, "Server error " + error.mStatusCode + " (" + error.mStatusMessage + ")");
        }
    }

    private void disconnectAndNotify(int code, String reason) {
        ResanaLog.v(TAG, "disconnectAndNotify:" + code + " , " + reason);
        disconnect();
        mWsHandler.onClose(code, reason);
        releaseConnectWakeLockIfNeeded();
    }

    public void handleBefrestEvent(BefrestEvent e) {
        switch (e.type) {
            case CONNECT:
                connect();
                break;
            case DISCONNECT:
                disconnect();
                break;
            case STOP:
                mLooper.quit();
                break;
            case REFRESH:
                refresh();
                break;
            case PING:
                sendPing();
                break;
            case SEND_MESSAGE:
                sendTextMessage(new WebSocketMessage.TextMessage(e.data));
                break;
            case SEND_RESANA_ACK:
                sendResanaAck(e.data);
                break;
        }
    }

    private void refresh() {
        refreshRequested = true;
        if (isConnected()) {
//            prevSuccessfulPings = 0; seems illogical
            cancelFuturePing();
            cancelUpcommingRestart();
            setNextPingToSendInFuture(0);
        } else {
            ResanaLog.v(TAG, "refresh received when socket is not connected. will connect...");
            connect();
        }
    }

    private void notifyConnectionRefreshedIfNeeded() {
        if (refreshRequested) {
            refreshRequested = false;
            mWsHandler.onConnectionRefreshed();
        }
    }

    public void connect() {
        ResanaLog.v(TAG, "--------------------------connect()_START--------------------");
        if (isConnected()) {
            ResanaLog.v(TAG, "already connected!");
        } else if (appContext != null && !BefrestImpl.Util.isConnectedToInternet(appContext)) {
            ResanaLog.v(TAG, "no internet connection!");
        } else {
            acquireConnectWakeLockIfPossible();
            waitABit();
            try {
                mTransportChannel = createSocket();
                if (isConnected()) {
                    createReader();
                    createWriter();
                    startWebSocketHandshake();
                    postDelayed(disconnectIfWebSocketHandshakeTimeOut, SERVER_HANDSHAKE_TIMEOUT);
                } else {
                    disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CANNOT_CONNECT, "Could not connect to WebSocket server");
                }
            } catch (IOException e) {
                ResanaLog.e(TAG, e);
                disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CANNOT_CONNECT, e.getMessage());
            } catch (Exception ex) {
                ResanaLog.e(TAG, ex);
                disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CANNOT_CONNECT, ex.getMessage());
            } catch (AssertionError e) {
                if (isAndroidGetsocknameError(e))
                    disconnectAndNotify(WebSocketConnectionHandler.CLOSE_CANNOT_CONNECT, e.getMessage());
                else
                    throw e;
            }
        }
        ResanaLog.v(TAG, "--------------------------connect()_END--------------------");
    }

    private void waitABit() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startWebSocketHandshake() {
        WebSocketMessage.ClientHandshake hs = new WebSocketMessage.ClientHandshake(
                mWsHost + ":" + mWsPort);
        hs.mPath = mWsPath;
        hs.mQuery = mWsQuery;
        hs.mSubprotocols = mWsSubprotocols;
        hs.mHeaderList = mWsHeaders;
        mWriter.forward(hs);
    }

    private Socket createSocket() throws IOException {
        Socket soc;
        if (mWsScheme.equals("wss")) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//                SSLSocket secSoc = (SSLSocket) factory.createSocket(mWsHost, mWsPort);
            SSLSocket secSoc = (SSLSocket) factory.createSocket();
            secSoc.setUseClientMode(true);
            secSoc.connect(new InetSocketAddress(mWsHost, mWsPort), mOptions.getSocketConnectTimeout());
//                secSoc.setSoTimeout(mOptions.getSocketReceiveTimeout());
            secSoc.setTcpNoDelay(mOptions.getTcpNoDelay());
            secSoc.addHandshakeCompletedListener(new HandshakeCompletedListener() {
                public void handshakeCompleted(HandshakeCompletedEvent event) {
                    ResanaLog.d(TAG, "ssl handshake completed");
                }
            });
            soc = secSoc;
        } else
            soc = new Socket(mWsHost, mWsPort);
        return soc;
    }

    /**
     * Create WebSockets background writer.
     */
    protected void createWriter() {
        mWriterThread = new HandlerThread("WebSocketWriter");
        mWriterThread.start();
        mWriter = new WebSocketWriter(mWriterThread.getLooper(), this, mTransportChannel, mOptions, appContext);
        ResanaLog.v(TAG, "WS writer created and started");
    }


    /**
     * Create WebSockets background reader.
     */
    protected void createReader() {
        mReader = new WebSocketReader(this, mTransportChannel, mOptions, "WebSocketReader", appContext);
        mReader.start();
        ResanaLog.v(TAG, "WS reader created and started");
    }


    private void parseWebsocketUri(String wsUri, List<KeyValue> headers) {
        try {
            URI mWsUri = new URI(wsUri);

            mWsScheme = mWsUri.getScheme();

            if (mWsUri.getPort() == -1) {
                if (mWsScheme.equals("ws")) mWsPort = 80;
                else mWsPort = 443;
            } else mWsPort = mWsUri.getPort();
            mWsHost = mWsUri.getHost();

            if (mWsUri.getRawPath() == null || mWsUri.getRawPath().equals("")) mWsPath = "/";
            else mWsPath = mWsUri.getRawPath();

            if (mWsUri.getRawQuery() == null || mWsUri.getRawQuery().equals(""))
                mWsQuery = null;
            else mWsQuery = mWsUri.getRawQuery();
        } catch (URISyntaxException e) {
            //should not come here
        }
        mWsSubprotocols = null;
        mWsHeaders = headers;
        mOptions = new WebSocketOptions();
    }

    private void disconnect() {
        ResanaLog.v(TAG, "--------------------------disconnect()_START--------------------");
        removeCallbacks(disconnectIfWebSocketHandshakeTimeOut);
        cancelFuturePing();
        cancelUpcommingRestart();
        if (mReader != null) {
            mReader.quit();
        } else ResanaLog.v(TAG, "mReader was null");
        if (mWriter != null) {
            mWriter.forward(new WebSocketMessage.Quit());
        } else
            ResanaLog.v(TAG, "mWriter was null");
        try {
            if (mTransportChannel != null) {
                try {
                    mTransportChannel.close();
                    ResanaLog.v(TAG, "mTranslateChannel closed");
                } catch (IOException e) {
                    ResanaLog.e(TAG, e);
                } catch (AssertionError e) {
                    if (isAndroidGetsocknameError(e))
                        ResanaLog.e(TAG, e);
                    else throw e;
                }
            } else {
                ResanaLog.v(TAG, "mTransportChannel was NULL");
            }
            if (mWriterThread != null) {
                mWriterThread.join(1000);
                ResanaLog.v(TAG, "mWriterThread joined");
            }
            if (mReader != null) {
                mReader.join(1000);
                ResanaLog.v(TAG, "mReader joined");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mReader = null;
        mWriter = null;
        mWriterThread = null;
        mTransportChannel = null;
        switchProtocol();
        ResanaLog.v(TAG, "--------------------------disconnect()_END--------------------");
    }

    /**
     * switch between ws and wss to ensure
     * that client will finally connect
     **/
    private void switchProtocol() {
        if (mWsScheme.equals("ws")) {
            mWsScheme = "wss";
            mWsPort = 443;
        } else {
            mWsScheme = "ws";
            mWsPort = 80;
        }
    }

    public boolean isConnected() {
        boolean res = mTransportChannel != null && mTransportChannel.isConnected() && !mTransportChannel.isClosed();
        return res;
    }

    private void acquireConnectWakeLockIfPossible() {
        removeCallbacks(releaseConnectWakeLock);
        if (BefrestInternal.Util.isWakeLockPermissionGranted(appContext)) {
            if (connectWakelock != null) {
                if (connectWakelock.isHeld())
                    return;
            } else {
                PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                connectWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, connectWakeLockName);
                connectWakelock.setReferenceCounted(false);
            }
            connectWakelock.acquire();
            ResanaLog.v(TAG, "connectWakeLock acquired.");
        } else
            ResanaLog.d(TAG, "could not acquire connect wakelock. (permission not granted)");
    }

    private void releaseConnectWakeLockIfNeeded() {
        if (BefrestInternal.Util.isWakeLockPermissionGranted(appContext)) {
            if (connectWakelock != null && connectWakelock.isHeld()) {
                connectWakelock.release();
                ResanaLog.v(TAG, "connectWakeLock released manually");
            }
        }
    }

    /**
     * Returns true if {@code e} is due to a firmware bug fixed after Android 4.2.2.
     * https://code.google.com/p/android/issues/detail?id=54072
     */
    static boolean isAndroidGetsocknameError(AssertionError e) {
        return e.getCause() != null && e.getMessage() != null
                && e.getMessage().contains("getsockname failed");
    }
}
