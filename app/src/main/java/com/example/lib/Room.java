package com.example.lib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class Room {
    private static final String TAG = "Room";
    // Local cam mediasoup Producer.
    private Producer mCamProducer;
    // Closed flag.
    private volatile boolean mClosed;
    // PeerConnection util.
    private PeerConnectionUtils mPeerConnectionUtils;
    // mediasoup Transport for sending.
    private SendTransport mSendTransport;
    // mediasoup Transport for receiving.
    private RecvTransport mRecvTransport;
    // jobs worker handler.
    private Handler mWorkHandler;
    // main looper handler.
    private Handler mMainHandler;
    private Device mMediasoupDevice;
    // mProtoo-client Protoo instance.
    // local Video Track for cam.
    private VideoTrack mLocalVideoTrack;
    // Local Audio Track for mic.
    private AudioTrack mLocalAudioTrack;
    // Local mic mediasoup Producer.
    private Producer mMicProducer;
    private Protoo mProtoo;
    private String mProtooUrl = "";
    private String roomId, peerId;
    private ConsumerTransportListener consumerTransportListener;
    private SurfaceViewRenderer remoteRenderer;
    private SurfaceViewRenderer localRenderer;
    private Context mContext;
    private static boolean isInitMediasoupClient = false;

    private ArrayList<PeerHolder> peerList = new ArrayList<>();

    public static class PeerHolder {
        String peerId;
        ArrayList<Consumer> list = new ArrayList<>();
    }

    public ArrayList<PeerHolder> getConsumerArrayList() {
        return peerList;
    }

    public Room(Context ctx, String roomId, String peerId, SurfaceViewRenderer localVideoView, SurfaceViewRenderer remoteVideoView) {
        mContext = ctx;
        if (isInitMediasoupClient ==  false) {
            MediasoupClient.initialize(mContext.getApplicationContext());
            isInitMediasoupClient = true;
        }

        this.remoteRenderer = remoteVideoView;
        this.localRenderer = localVideoView;
        //this.consumerTransportListener = consumerTransportListener;
        this.roomId = roomId;
        this.peerId = peerId;
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mProtooUrl = "wss://10.168.11.43:4443/?roomId=" + roomId + "&peerId=" + peerId;

        mWorkHandler.post(
                () -> {
                    WebSocketTransport transport = new WebSocketTransport(mProtooUrl);
                    mProtoo = new Protoo(transport, peerListener);
                });
        mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());
    }

    private void joinImpl() throws MediasoupException, ProtooException, JSONException {
        mMediasoupDevice = new Device();
        String routerRtpCapabilities = mProtoo.syncRequest("getRouterRtpCapabilities");
        Log.e(TAG,"Capabilities: " + routerRtpCapabilities);
        mMediasoupDevice.load(routerRtpCapabilities);
        String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();

        createSendTransport();
        createRecvTransport();

        JSONObject deviceInfo = new JSONObject();
        deviceInfo.put("flag", "123");
        deviceInfo.put("name", "android");
        deviceInfo.put("version", "1.0.1");

        String joinResponse =
                mProtoo.syncRequest(
                        "join",
                        req -> {
                            try {
                                req.put("displayName", "Tanay");
                                req.put("device", deviceInfo);
                                req.put("rtpCapabilities", new JSONObject(rtpCapabilities));
                                // TODO (HaiyangWu): add sctpCapabilities
                                req.put("sctpCapabilities", "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });

        mWorkHandler.post(this::enableMic);
        mWorkHandler.post(this::enableCam);
    }

    @WorkerThread
    private void createSendTransport() throws ProtooException, JSONException, MediasoupException {
        Log.e(TAG,"createSendTransport()");
        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        (req -> {
                            try {
                                req.put("forceTcp", false);
                                req.put("producing", true);
                                req.put("consuming", false);
                                // TODO: sctpCapabilities
                                req.put("sctpCapabilities", "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }));
        JSONObject info = new JSONObject(res);

        //Logger.d(TAG, "device#createSendTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mSendTransport = mMediasoupDevice.createSendTransport(sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
    }

    @WorkerThread
    private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
        Log.e(TAG,"createRecvTransport()");

        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        req -> {
                            try {
                                req.put("forceTcp", false);
                                req.put("producing", false);
                                req.put("consuming", true);
                                // TODO (HaiyangWu): add sctpCapabilities
                                req.put("sctpCapabilities", "");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        });
        JSONObject info = new JSONObject(res);
        Log.e(TAG,"device#createRecvTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mRecvTransport =
                mMediasoupDevice.createRecvTransport(
                        recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, null);
    }

    private SendTransport.Listener sendTransportListener =
            new SendTransport.Listener() {

                //private String listenerTAG = TAG + "_SendTrans";

                @Override
                public String onProduce(
                        Transport transport, String kind, String rtpParameters, String appData) {
                    /*if (mClosed) {
                        return "";
                    }*/
                    //Logger.d(listenerTAG, "onProduce() ");
                    String producerId =
                            fetchProduceId(
                                    req -> {
                                        try {
                                            req.put("transportId", transport.getId());
                                            req.put("kind", kind);
                                            req.put("rtpParameters", new JSONObject(rtpParameters));
                                            req.put("appData", appData);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                    });
                    Log.e(TAG,"producerId: " + producerId);
                    return producerId;
                }

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    /*if (mClosed) {
                        return;
                    }*/
                    //Logger.d(listenerTAG + "_send", "onConnect()");

                    mProtoo.request(
                            "connectWebRtcTransport",
                            req -> {
                                try {
                                    req.put("transportId", transport.getId());
                                    req.put("dtlsParameters", new JSONObject(dtlsParameters));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            })
                            .subscribe(
                                    d -> Log.e(TAG,"connectWebRtcTransport res: " + d),
                                    t -> Log.e(TAG,"connectWebRtcTransport for mSendTransport failed" + t));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Log.e(TAG,"onConnectionStateChange: " + connectionState);
                }
            };

    private String fetchProduceId(Protoo.RequestGenerator generator) {
        Log.e(TAG,"fetchProduceId:()");
        try {
            String response = mProtoo.syncRequest("produce", generator);
            return new JSONObject(response).optString("id");
        } catch (ProtooException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"send produce request failed " + e);
            return "";
        }
    }

    private RecvTransport.Listener recvTransportListener =
            new RecvTransport.Listener() {

                //private String listenerTAG = TAG + "_RecvTrans";

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                   /* if (mClosed) {
                        return;
                    }*/
                    Log.e(TAG,"RecvTransport, onConnect()");
                    mProtoo.request(
                            "connectWebRtcTransport",
                            req -> {
                                try {
                                    req.put("transportId", transport.getId());
                                    req.put("dtlsParameters", new JSONObject(dtlsParameters));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            })
                            .subscribe(
                                    d -> Log.e(TAG,"connectWebRtcTransport res: " + d),
                                    t -> Log.e(TAG,"connectWebRtcTransport for mRecvTransport failed " + t));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Log.e(TAG,"onConnectionStateChange: " + connectionState);
                    /*if (connectionState.equalsIgnoreCase("completed")) {
                        mMainHandler.post(() -> consumerTransportListener.onTransportConnected());
                    }*/
                }
            };

    @WorkerThread
    private void enableCamImpl() {
        Log.e(TAG,"enableCamImpl()");
        try {
            if (mCamProducer != null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Log.e(TAG,"enableCam() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce("video")) {
                Log.e(TAG,"enableCam() | cannot produce video");
                return;
            }
            if (mSendTransport == null) {
                Log.e(TAG,"enableCam() | mSendTransport doesn't ready");
                return;
            }

            if (mLocalVideoTrack == null) {
                mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext.getApplicationContext(), "cam");
                mLocalVideoTrack.setEnabled(true);
                mLocalVideoTrack.addSink(localRenderer);
            }
            mCamProducer =
                    mSendTransport.produce(
                            producer -> {
                                Log.e(TAG,"onTransportClose(), camProducer");
                                if (mCamProducer != null) {
                                    //mStore.removeProducer(mCamProducer.getId());
                                    mCamProducer = null;
                                }
                            },
                            mLocalVideoTrack,
                            null,
                            null);
            //mStore.addProducer(mCamProducer);
        } catch (MediasoupException e) {
            e.printStackTrace();
            Log.e(TAG,"enableWebcam() | failed: " + e);
            //mStore.addNotify("error", "Error enabling webcam: " + e.getMessage());
            /*if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
            }*/
        }
    }

    private Protoo.Listener peerListener =
            new Protoo.Listener() {
                @Override
                public void onOpen() {
                    mWorkHandler.post(() -> {
                        try {
                            joinImpl();
                        } catch (MediasoupException | ProtooException | JSONException e) {
                            e.printStackTrace();
                        }
                    });

                }

                @Override
                public void onFail() {
  /*                  mWorkHandler.post(
                            () -> {
                                mStore.addNotify("error", "WebSocket connection failed");
                                mStore.setRoomState(ConnectionState.CONNECTING);
                            });*/
                }

                @Override
                public void onRequest(
                        @androidx.annotation.NonNull Message.Request request, @androidx.annotation.NonNull Protoo.ServerRequestHandler handler) {
                    //Logger.d(TAG, "onRequest() " + request.getData().toString());
                    mWorkHandler.post(
                            () -> {
                                try {
                                    switch (request.getMethod()) {
                                        case "newConsumer": {
                                            Log.e(TAG,"newConsumer");
                                            onNewConsumer(request, handler);
                                            break;
                                        }
                                        case "newDataConsumer": {
                                            Log.e(TAG,"newDataConsumer");
                                            //onNewDataConsumer(request, handler);
                                            break;
                                        }
                                        default: {
                                            handler.reject(403, "unknown protoo request.method " + request.getMethod());
                                            //Logger.w(TAG, "unknown protoo request.method " + request.getMethod());
                                        }
                                    }
                                } catch (Exception e) {
                                    //Logger.e(TAG, "handleRequestError.", e);
                                }
                            });
                }

                @Override
                public void onNotification(@androidx.annotation.NonNull Message.Notification notification) {
                    /*Logger.d(
                            TAG,
                            "onNotification() "
                                    + notification.getMethod()
                                    + ", "
                                    + notification.getData().toString());*/
                    mWorkHandler.post(
                            () -> {
                                try {
                                    //handleNotification(notification);
                                } catch (Exception e) {
                                    //Logger.e(TAG, "handleNotification error.", e);
                                }
                            });
                }

                @Override
                public void onDisconnected() {
                    mWorkHandler.post(
                            () -> {
                                //mStore.addNotify("error", "WebSocket disconnected");
                                //mStore.setRoomState(ConnectionState.CONNECTING);

                                // Close All Transports created by device.
                                // All will reCreated After ReJoin.
                                //disposeTransportDevice();
                            });
                }

                @Override
                public void onClose() {
                    /*if (mClosed) {
                        return;
                    }
                    mWorkHandler.post(
                            () -> {
                                if (mClosed) {
                                    return;
                                }
                                close();
                            });*/
                }
            };

    public void close() {
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        Log.e(TAG,"close()");

        mWorkHandler.post(
                () -> {
                    // Close mProtoo Protoo
                    if (mProtoo != null) {
                        mProtoo.close();
                        mProtoo = null;
                    }

                    // dispose all transport and device.
                    disposeTransportDevice();

                    // dispose audio track.
                    if (mLocalAudioTrack != null) {
                        mLocalAudioTrack.setEnabled(false);
                        mLocalAudioTrack.dispose();
                        mLocalAudioTrack = null;
                    }

                    // dispose video track.
                    if (mLocalVideoTrack != null) {
                        mLocalVideoTrack.setEnabled(false);
                        mLocalVideoTrack.dispose();
                        mLocalVideoTrack = null;
                    }

                    // dispose peerConnection.
                    mPeerConnectionUtils.dispose();

                    // quit worker handler thread.
                    mWorkHandler.getLooper().quit();
                });

        // dispose request.
        //mCompositeDisposable.dispose();

        // Set room state.
        //mStore.setRoomState(ConnectionState.CLOSED);
    }

    @WorkerThread
    private void enableMicImpl() {
        Log.e(TAG,"enableMicImpl()");
        try {
            if (mMicProducer != null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Log.e(TAG,"enableMic() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce("audio")) {
                Log.e(TAG,"enableMic() | cannot produce audio");
                return;
            }
            if (mSendTransport == null) {
                Log.e(TAG,"enableMic() | mSendTransport doesn't ready");
                return;
            }
            if (mLocalAudioTrack == null) {
                mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext.getApplicationContext(), "mic");
                mLocalAudioTrack.setEnabled(true);
            }
            mMicProducer =
                    mSendTransport.produce(
                            producer -> {
                                Log.e(TAG,"onTransportClose(), micProducer");
                                if (mMicProducer != null) {
                                    //mStore.removeProducer(mMicProducer.getId());
                                    mMicProducer = null;
                                }
                            },
                            mLocalAudioTrack,
                            null,
                            null);
            //mStore.addProducer(mMicProducer);
        } catch (MediasoupException e) {
            e.printStackTrace();
            Log.e(TAG,"enableMic() | failed: " + e);
            //mStore.addNotify("error", "Error enabling microphone: " + e.getMessage());
            if (mLocalAudioTrack != null) {
                mLocalAudioTrack.setEnabled(false);
            }
        }
    }

    //@Async
    public void enableMic() {
        Log.e(TAG,"enableMic()");
        mWorkHandler.post(this::enableMicImpl);
    }

    //@Async
    public void disableMic() {
        Log.e(TAG,"disableMic()");
        mWorkHandler.post(this::disableMicImpl);
    }

    //@Async
    public void muteMic() {
        Log.e(TAG,"muteMic()");
        mWorkHandler.post(this::muteMicImpl);
    }

    //@Async
    public void unmuteMic() {
        Log.e(TAG,"unmuteMic()");
        mWorkHandler.post(this::unmuteMicImpl);
    }

    //@Async
    public void enableCam() {
        Log.e(TAG,"enableCam()");
        //mStore.setCamInProgress(true);
        mWorkHandler.post(
                () -> {
                    enableCamImpl();
                    //mStore.setCamInProgress(false);
                });
    }

    //@Async
    public void disableCam() {
        Log.e(TAG,"disableCam()");
        mWorkHandler.post(this::disableCamImpl);
    }

    @WorkerThread
    private void disableMicImpl() {
        Log.e(TAG,"disableMicImpl()");
        if (mMicProducer == null) {
            return;
        }

        mMicProducer.close();
        //mStore.removeProducer(mMicProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> {
                try {
                    req.put("producerId", mMicProducer.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (ProtooException e) {
            e.printStackTrace();
            //mStore.addNotify("error", "Error closing server-side mic Producer: " + e.getMessage());
        }
        mMicProducer = null;
    }

    @WorkerThread
    private void muteMicImpl() {
        Log.e(TAG,"muteMicImpl()");
        mMicProducer.pause();

        try {
            mProtoo.syncRequest("pauseProducer", req -> {
                try {
                    req.put("producerId", mMicProducer.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            //mStore.setProducerPaused(mMicProducer.getId());
        } catch (ProtooException e) {
            e.printStackTrace();
            Log.e(TAG,"muteMic() | failed: " + e);
            //mStore.addNotify("error", "Error pausing server-side mic Producer: " + e.getMessage());
        }
    }

    @WorkerThread
    private void unmuteMicImpl() {
        Log.e(TAG,"unmuteMicImpl()");
        mMicProducer.resume();

        try {
            mProtoo.syncRequest(
                    "resumeProducer", req -> {
                        try {
                            req.put("producerId", mMicProducer.getId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
            //mStore.setProducerResumed(mMicProducer.getId());
        } catch (ProtooException e) {
            e.printStackTrace();
            Log.e(TAG,"unmuteMic() | failed: " + e);
            //mStore.addNotify("error", "Error resuming server-side mic Producer: " + e.getMessage());
        }
    }

    @WorkerThread
    private void disableCamImpl() {
        Log.e(TAG,"disableCamImpl()");
        if (mCamProducer == null) {
            return;
        }
        mCamProducer.close();
        //mStore.removeProducer(mCamProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> {
                try {
                    req.put("producerId", mCamProducer.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (ProtooException e) {
            e.printStackTrace();
            //mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.getMessage());
        }
        mCamProducer = null;
    }

    @WorkerThread
    private void disposeTransportDevice() {
        Log.e(TAG,"disposeTransportDevice()");
        // Close mediasoup Transports.
        if (mSendTransport != null) {
            mSendTransport.close();
            mSendTransport.dispose();
            mSendTransport = null;
        }

        if (mRecvTransport != null) {
            mRecvTransport.close();
            mRecvTransport.dispose();
            mRecvTransport = null;
        }

        // dispose device.
        if (mMediasoupDevice != null) {
            mMediasoupDevice.dispose();
            mMediasoupDevice = null;
        }
    }

    private void onNewConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {

        try {
            JSONObject data = request.getData();
            String peerId = data.optString("peerId");
            String producerId = data.optString("producerId");
            String id = data.optString("id");
            String kind = data.optString("kind");
            String rtpParameters = data.optString("rtpParameters");
            String type = data.optString("type");
            String appData = data.optString("appData");
            boolean producerPaused = data.optBoolean("producerPaused");

            Consumer consumer =
                    mRecvTransport.consume(
                            c -> {
                                //mConsumers.remove(c.getId());
                                Log.e(TAG,"onTransportClose for consume");
                            },
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            appData);

            //mConsumers.put(consumer.getId(), new ConsumerHolder(peerId, consumer));
            //mStore.addConsumer(peerId, type, consumer, producerPaused);

            // We are ready. Answer the protoo request so the server will
            // resume this Consumer (which was paused for now if video).
            handler.accept();

            if (kind.equalsIgnoreCase("video")) {
                ((VideoTrack) consumer.getTrack()).addSink(remoteRenderer);
            }

            /*boolean isFound = false;
            for (int i = 0; i < peerList.size(); i++) {
                if (peerList.get(i).peerId.equalsIgnoreCase(peerId)){
                    peerList.get(i).list.add(consumer);
                    isFound = true;
                }
            }

            if (!isFound){
                PeerHolder holder = new PeerHolder();
                holder.peerId = peerId;
                holder.list.add(consumer);

                peerList.add(holder);
            }*/

            // If audio-only mode is enabled, pause it.
            /*if ("video".equals(consumer.getKind()) && mStore.getMe().getValue().isAudioOnly()) {
                pauseConsumer(consumer);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"\"newConsumer\" request failed: " + e);
            //mStore.addNotify("error", "Error creating a Consumer: " + e.getMessage());
        }
    }

    public void changeToHd() {
        if (mPeerConnectionUtils != null) {
            mPeerConnectionUtils.changeToHd();
        }
    }

    public void changeToCustom(int width, int height) {
        if (mPeerConnectionUtils != null) {
            mPeerConnectionUtils.changeToCustom(width, height);
        }
    }

    public interface ConsumerTransportListener {
        void onTransportConnected();
    }
}
