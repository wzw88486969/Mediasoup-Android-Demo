package com.example.mediasoupdemo;

import androidx.annotation.WorkerThread;

import org.json.JSONObject;
import org.protoojs.droid.ProtooException;

import io.reactivex.Observable;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Protoo extends org.protoojs.droid.Peer {

    private static final String TAG = "Protoo";

    interface RequestGenerator {
        void request(JSONObject req);
    }

    public Protoo(@androidx.annotation.NonNull WebSocketTransport transport, @androidx.annotation.NonNull Listener listener) {
        super(transport, listener);
    }

    public Observable<String> request(String method) {
        return request(method, new JSONObject());
    }

    public Observable<String> request(String method, @androidx.annotation.NonNull RequestGenerator generator) {
        JSONObject req = new JSONObject();
        generator.request(req);
        return request(method, req);
    }

    private Observable<String> request(String method, @androidx.annotation.NonNull JSONObject data) {
        AppData.log("Request Method: " + method);
        AppData.log("request(), method: " + method);
        return Observable.create(
                emitter ->
                        request(
                                method,
                                data,
                                new ClientRequestHandler() {
                                    @Override
                                    public void resolve(String data) {
                                        if (!emitter.isDisposed()) {
                                            emitter.onNext(data);
                                        }
                                    }

                                    @Override
                                    public void reject(long error, String errorReason) {
                                        if (!emitter.isDisposed()) {
                                            emitter.onError(new ProtooException(error, errorReason));
                                        }
                                    }
                                }));
    }

    @WorkerThread
    public String syncRequest(String method) throws ProtooException {
        return syncRequest(method, new JSONObject());
    }

    @WorkerThread
    public String syncRequest(String method, @androidx.annotation.NonNull RequestGenerator generator)
            throws ProtooException {
        JSONObject req = new JSONObject();
        generator.request(req);
        return syncRequest(method, req);
    }

    @WorkerThread
    private String syncRequest(String method, @androidx.annotation.NonNull JSONObject data) throws ProtooException {
        AppData.log( "syncRequest(), method: " + method);

        try {
            return request(method, data).blockingFirst();
        } catch (Throwable throwable) {
            throw new ProtooException(-1, throwable.getMessage());
        }
    }
}
