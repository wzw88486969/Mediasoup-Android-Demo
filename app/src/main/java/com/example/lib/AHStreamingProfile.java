package com.example.lib;

public class AHStreamingProfile {
    public void AHStreamingProfile(){
    }
    public void setPublishUrl(String url){

    }

    public static enum H264Profile {
        BASELINE,
        MAIN,
        HIGH;

        private H264Profile() {
        }
    }

    public static enum EncoderRCModes {
        QUALITY_PRIORITY,
        BITRATE_PRIORITY;

        private EncoderRCModes() {
        }
    }

    public static class VideoEncodingSize {
        public int level;
        public int width;
        public int height;

        public VideoEncodingSize(int level, int w, int h) {
            this.level = level;
            this.width = w;
            this.height = h;
        }
    }

    public static class AudioProfile {
        public int sampleRate;
        public int reqBitrate;
        public int channelNumber;

        public AudioProfile(int sampleRate, int bitrate) {
            this.sampleRate = sampleRate;
            this.reqBitrate = bitrate;
            this.channelNumber = 1;
        }

        public String toString() {
            return "AudioProfile: [" + this.sampleRate + "Hz," + this.reqBitrate + "bps]";
        }
    }

    public static class VideoProfile {
        public int reqFps;
        public int reqBitrate;
        public int maxKeyFrameInterval;
        public AHStreamingProfile.H264Profile h264Profile;
        public boolean avcc;

        public VideoProfile(int fps, int bitrate) {
            this.reqFps = fps;
            this.reqBitrate = bitrate;
            this.maxKeyFrameInterval = fps * 3;
            this.h264Profile = AHStreamingProfile.H264Profile.BASELINE;
            this.avcc = true;
        }

        public VideoProfile(int fps, int bitrate, int maxKeyFrameInterval) {
            this.reqFps = fps;
            this.reqBitrate = bitrate;
            this.maxKeyFrameInterval = maxKeyFrameInterval;
            this.h264Profile = AHStreamingProfile.H264Profile.BASELINE;
            this.avcc = true;
        }

        public VideoProfile(int fps, int bitrate, int maxKeyFrameInterval, AHStreamingProfile.H264Profile profile) {
            this.reqFps = fps;
            this.reqBitrate = bitrate;
            this.maxKeyFrameInterval = maxKeyFrameInterval;
            this.h264Profile = profile;
            this.avcc = true;
        }

        public VideoProfile(int fps, int bitrate, int maxKeyFrameInterval, boolean avcc) {
            this.reqFps = fps;
            this.reqBitrate = bitrate;
            this.maxKeyFrameInterval = maxKeyFrameInterval;
            this.h264Profile = AHStreamingProfile.H264Profile.BASELINE;
            this.avcc = avcc;
        }

        public String toString() {
            return "VideoProfile: [" + this.reqFps + "fps," + this.reqBitrate + "bps, GOP:" + this.maxKeyFrameInterval + "," + this.h264Profile + ",avcc=" + this.avcc + "]";
        }

        public AHStreamingProfile.H264Profile getH264Profile() {
            return this.h264Profile;
        }
    }

    public static class AVProfile {
        private AHStreamingProfile.VideoProfile mVideoProfile;
        private AHStreamingProfile.AudioProfile mAudioProfile;

        public AVProfile(AHStreamingProfile.VideoProfile vProfile, AHStreamingProfile.AudioProfile aProfile) {
            this.mVideoProfile = vProfile;
            this.mAudioProfile = aProfile;
        }
    }
}
