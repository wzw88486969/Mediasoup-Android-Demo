package com.example.mediasoupdemo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lib.PeerConnectionUtils;
import com.example.lib.Room;

import org.webrtc.SurfaceViewRenderer;

public class MainActivity extends AppCompatActivity {

    private Room room;
    private SurfaceViewRenderer mRemoteVideoView;
    private SurfaceViewRenderer mLocalVideoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mLocalVideoView = findViewById(R.id.local_surfaceView);
        mRemoteVideoView = findViewById(R.id.remote_surfaceView);

        mLocalVideoView.init(PeerConnectionUtils.getEglContext(), null);
        mRemoteVideoView.init(PeerConnectionUtils.getEglContext(), null);
    }

    public void publish(View v) {
        room = new Room(this,
                "7chpagjs",
                "7chpagjs110",
                mLocalVideoView,
                mRemoteVideoView
        );
    }

    public void end(View v) {
        room.close();
        room = null;
    }
}