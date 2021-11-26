package com.example.mediasoupdemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.mediasoup.droid.Consumer
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.*

class MainActivity : AppCompatActivity() {

    private var room: Room? = null
    private lateinit var mRemoteVideoView: SurfaceViewRenderer
    private lateinit var mLocalVideoView: SurfaceViewRenderer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLocalVideoView = findViewById(R.id.local_surfaceView)
        mRemoteVideoView = findViewById(R.id.remote_surfaceView)

        mLocalVideoView.init(PeerConnectionUtils.getEglContext(), null)
        mRemoteVideoView.init(PeerConnectionUtils.getEglContext(), null)

    }

    fun publish(view: View) {
        room = Room("7chpagjs", "7chpagjs110", mLocalVideoView, mRemoteVideoView)
//        Thread.sleep(1000 * 5)
//        room?.enableCam()
//        room?.enableMic()
    }

    fun end(view: View) {
        room?.close()
        room = null
    }

/*    override fun onTransportConnected() {
        Log.e("TANAY", "onTransportConnected")
        val list :ArrayList<Room.PeerHolder> = room.consumerArrayList
        for (data in list){
            if (data.list.get().equals("video", ignoreCase = true)){
                (data.track as VideoTrack).addSink(surfaceView)
            }
        }
    }*/


}