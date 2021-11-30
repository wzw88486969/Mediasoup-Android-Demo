package com.example.lib;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class AHMediaStreamingManager {
    public AHMediaStreamingManager(Context ctx, GLSurfaceView view, AHAVCodecType encodingType) {
    }

    public synchronized boolean startStreaming() {
        return true;
    }

    public synchronized boolean resume() {
        return true;
    }

    public void pause() {

    }

    public boolean stopStreaming() {
        return true;
    }

    public boolean switchCamera() {
        return true;
    }
    public void destroy() {

    }
    public boolean turnLightOn() {
        return true;
    }
    public boolean turnLightOff() {
        return true;
    }
    public void mute(boolean enable) {
    }
    public void setNativeLoggingEnabled(boolean enabled) {

    }
    public final void setSurfaceTextureCallback(AHSurfaceTextureCallback callback) {

    }
    public final void setStreamingPreviewCallback(AHStreamingPreviewCallback callback) {
    }

    public void setStreamingProfile(AHStreamingProfile profile) {

    }
    public void setAutoRefreshOverlay(boolean enable) {

    }
    public boolean prepare(AHStreamingProfile profile) {
        return this.prepare((AHCameraStreamingSetting)null, profile);
    }

    public boolean prepare(AHCameraStreamingSetting setting, AHStreamingProfile profile) {
        return this.prepare(setting, (AHMicrophoneStreamingSetting)null, profile);
    }

    public boolean prepare(AHCameraStreamingSetting camSetting, AHMicrophoneStreamingSetting microphoneSetting, AHWatermarkSetting wmSetting, AHStreamingProfile profile) {
        return this.prepare(camSetting, microphoneSetting, wmSetting, profile, (AHPreviewAppearance)null);
    }

    public boolean prepare(AHCameraStreamingSetting camSetting, AHMicrophoneStreamingSetting microphoneSetting, AHStreamingProfile profile) {
        return this.prepare(camSetting, microphoneSetting, (AHWatermarkSetting)null, profile, (AHPreviewAppearance)null);
    }

    public boolean prepare(AHCameraStreamingSetting camSetting, AHMicrophoneStreamingSetting microphoneSetting, AHWatermarkSetting wmSetting, AHStreamingProfile profile, AHPreviewAppearance previewAppearance) {
        return true;
    }
}
