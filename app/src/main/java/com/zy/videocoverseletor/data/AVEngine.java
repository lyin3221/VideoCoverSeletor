package com.zy.videocoverseletor.data;

import android.util.Log;
import android.util.Size;

/**
 * 视频
 */
public class AVEngine {
    private static AVEngine gAVEngine;
    private VideoState mVideoState;
    private Size mSurfaceViewSize = new Size(1080, 350);

    public static AVEngine getVideoEngine() {
        if (gAVEngine == null) {
            synchronized (AVEngine.class) {
                if (gAVEngine == null) {
                    gAVEngine = new AVEngine();
                }
            }
        }
        return gAVEngine;
    }

    public interface EngineCallback {
        void onCallback(Object... args1);
    }

    private AVEngine() {
        mVideoState = new VideoState();
    }

    private static class Clock {
        public long lastUpdate = -1;
        public long delta = -1;
    }

    //视频核心信息类
    public static class VideoState {
        public enum VideoStatus {
            INIT,
            START,
            PAUSE,
            SEEK,
        }

        public long seekPositionUS;
        public long videoDuration;
        public long durationUS;//视频总时长 us
        public boolean isEdit;//编辑组件状态
        public Size canvasSize;//surface size
        public VideoStatus status;//播放状态
        public Clock extClock;
        public VideoData editComponent;
        public VideoData videoComponent;//视频轨道
        //onDrawFrame方法用到的
        public VideoState() {
            reset();
        }

        public void reset() {
            extClock = new Clock();
            durationUS = 0;
            videoDuration = 0;
            isEdit = false;
            seekPositionUS = Long.MAX_VALUE;
            status = VideoStatus.INIT;
            videoComponent = null;
        }
    }

    public long getCurrentTimeUs() {
        return System.nanoTime() / 1000;
    }

    public long getClock(Clock clock) {
        if (clock.lastUpdate == -1) {
            return 0;
        }
        if (mVideoState.status == VideoState.VideoStatus.START) {
            return getCurrentTimeUs() + clock.delta;
        }
        return clock.lastUpdate;
    }

    public long getMainClock() {
        long currentClk = getClock(mVideoState.extClock);
        if (currentClk > mVideoState.durationUS) {
            setMainClock(mVideoState.durationUS);
            return mVideoState.durationUS;
        }
        return currentClk;
    }

    public void setMainClock(long time) {
        mVideoState.extClock.lastUpdate = time;
        mVideoState.extClock.delta = mVideoState.extClock.lastUpdate - getCurrentTimeUs();
    }

    public void setmSurfaceViewSize(Size mSurfaceViewSize) {
        this.mSurfaceViewSize = mSurfaceViewSize;
    }

    private Size calCanvasSize(String text, Size videoSize) {
        int width = mSurfaceViewSize.getWidth();
        int height = mSurfaceViewSize.getHeight();
        if (text.equalsIgnoreCase("original")) {
            height = (int) (width * videoSize.getHeight() * 1.0f /
                    videoSize.getWidth());
            if (height > mSurfaceViewSize.getHeight()) {
                height = mSurfaceViewSize.getHeight();
                width = (int) (height * videoSize.getWidth() * 1.0f / videoSize.getHeight());
            }
        } else if (text.equalsIgnoreCase("4:3")) {
            height = (int) (width * 3.0f / 4.0f);
            if (height > mSurfaceViewSize.getHeight()) {
                height = mSurfaceViewSize.getHeight();
                width = (int) (height * 4.f / 3.f);
            }
        } else if (text.equalsIgnoreCase("3:4")) {
            width = (int) (height * 3.f / 4.0f);
        } else if (text.equalsIgnoreCase("1:1")) {
            width = height;
        } else if (text.equalsIgnoreCase("16:9")) {
            height = (int) (width * 9.0f / 16.0f);
            if (height > mSurfaceViewSize.getHeight()) {
                height = mSurfaceViewSize.getHeight();
                width = (int) (height * 16.f / 9.f);
            }
        } else if (text.equalsIgnoreCase("9:16")) {
            width = (int) (height * 9.0f / 16.0f);
        } else if (text.equalsIgnoreCase("centerCrop")){
            float bi1 = width * 1.0f / videoSize.getWidth();
            float bi2 = height * 1.0f / videoSize.getHeight();
            if(bi1 > bi2){
                //宽的比率大于高的比例，高等于屏幕高度，宽等于
                height = (int) (width * videoSize.getHeight() * 1.0f / videoSize.getWidth());
            }else{
                width = (int) (height * videoSize.getWidth() * 1.0f / videoSize.getHeight());
            }
            Log.e("","");
        }
        return new Size(width, height);
    }

    /**
     * 设置画布大小
     */
    public void setCanvasType(String type, EngineCallback engineCallback) {
        if (type == null) return;
        if (mVideoState.videoComponent == null) {
            return;
        }
        Size video0Size = mVideoState.videoComponent.getVideoSize();
        Size targetSize = calCanvasSize(type, video0Size);
        mVideoState.canvasSize = targetSize;
        if (engineCallback != null) {
            engineCallback.onCallback(targetSize);
        }
    }

    public void updateEdit(VideoData avComponent, boolean isEdit) {
        mVideoState.isEdit = isEdit;
        mVideoState.editComponent = avComponent;
    }

    /**
     * 添加组件
     *
     * @param avComponent
     */
    public void addComponent(VideoData avComponent, EngineCallback engineCallback) {
        VideoData component = avComponent;
        if (component.getEngineStartTime() == -1) {
            component.setEngineStartTime(getMainClock());
        }
        component.open();
        mVideoState.videoComponent = component;
        reCalculate();
        if (engineCallback != null) {
            engineCallback.onCallback("");
        }
    }

    /**
     * 计算总时长US.
     */
    private void reCalculate() {
        mVideoState.videoDuration = 0;
        mVideoState.videoDuration = Math.max(mVideoState.videoComponent.getEngineEndTime(), mVideoState.videoDuration);
        mVideoState.durationUS = mVideoState.videoDuration;
        if (getMainClock() > mVideoState.durationUS) {
            setMainClock(mVideoState.durationUS);
        }
    }

    private static final int SEEK_ENTER = -1;
    private static final int SEEK_EXIT = -2;

    public void seek(boolean status) {
        seek((status ? SEEK_ENTER : SEEK_EXIT));
    }

    public void seek(long position) {
        long args = position;
        if (args == SEEK_EXIT && mVideoState.status == VideoState.VideoStatus.SEEK) {
            setMainClock(getMainClock());
            mVideoState.status = VideoState.VideoStatus.PAUSE;
            return;
        }

        if (args == SEEK_ENTER) {//进入Seek模式
            mVideoState.status = VideoState.VideoStatus.SEEK;
            return;
        }

        if (mVideoState.status == VideoState.VideoStatus.SEEK) {
            long seekPositionUS = position;
            if (seekPositionUS < 0 || seekPositionUS > mVideoState.durationUS) {
                return;
            }
            mVideoState.seekPositionUS = position;
            setMainClock(mVideoState.seekPositionUS);
        }
    }

    public VideoState getVideoState() {
        return mVideoState;
    }

}
