package com.zy.videocoverseletor.data;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Size;

import java.io.IOException;

public class VideoData {
    private long engineStartTime;//引擎相关
    private long engineEndTime;//引擎相关
    private long clipStartTime;//文件相关的
    private long clipEndTime;//文件相关
    private long duration;//视频duration,不可改变
    private long position;
    private boolean isOpen;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private int frameRate = 0;
    private String path;
    private Size videoSize;

    public VideoData(String path) {
        this.engineStartTime = -1;
        this.engineEndTime = -1;
        this.clipStartTime = -1;
        this.clipEndTime = -1;
        this.position = Long.MAX_VALUE;
        this.isOpen = false;
        this.path = path;
        this.videoSize = new Size(0, 0);
    }
    public int getFrameRate() {
        return frameRate;
    }
    public Size getVideoSize() {
        return videoSize;
    }
    public void setVideoSize(Size videoSize) {
        this.videoSize = videoSize;
    }
    public String getPath() {
        return path;
    }
    public long getEngineStartTime() {
        return engineStartTime;
    }

    public void setEngineStartTime(long engineStartTime) {
        this.engineStartTime = engineStartTime;
    }

    public long getEngineEndTime() {
        return engineEndTime;
    }

    public void setEngineEndTime(long engineEndTime) {
        this.engineEndTime = engineEndTime;
    }

    public long getClipStartTime() {
        return clipStartTime;
    }

    public void setClipStartTime(long clipStartTime) {
        this.clipStartTime = clipStartTime;
    }

    public long getClipEndTime() {
        return clipEndTime;
    }

    public void setClipEndTime(long clipEndTime) {
        this.clipEndTime = clipEndTime;
    }

    public long getEngineDuration() {
        return engineEndTime - engineStartTime;
    }

    public long getClipDuration() {
        return clipEndTime - clipStartTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void markOpen(boolean open) {
        isOpen = open;
    }

    public boolean isValid(long position) {
        return position >= engineStartTime && position < engineEndTime;
    }

    public void open() {
        if (isOpen()) return;
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(getPath());
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                if (mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).contains("video")) {
                    mediaFormat = mediaExtractor.getTrackFormat(i);
                    mediaExtractor.selectTrack(i);
                    frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                    setVideoSize(new Size(mediaFormat.getInteger(MediaFormat.KEY_WIDTH), mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)));
                    long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    setClipStartTime(0);
                    setClipEndTime(duration);
                    setDuration(duration);
                    break;
                }
            }
            setEngineEndTime(getEngineStartTime() + getDuration());
            markOpen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
