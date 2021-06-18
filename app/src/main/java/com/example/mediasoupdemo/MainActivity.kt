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
    private lateinit var surfaceView: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.init(PeerConnectionUtils.getEglContext(), null)
        room = Room("qwerty", "qwerty", surfaceView)
    }

    fun publish(view: View) {
        room?.enableCam()
        room?.enableMic()
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