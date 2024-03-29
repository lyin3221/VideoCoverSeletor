package com.zy.videocoverseletor.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.UCropView;
import com.zy.videocoverseletor.R;
import com.zy.videocoverseletor.data.VideoData;
import com.zy.videocoverseletor.data.AVEngine;
import com.zy.videocoverseletor.utils.VideoUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class SelectCoverView extends RelativeLayout {
    private UCropView uCropView;
    private GestureCropImageView cropImageView;
    private OverlayView overlayView;
    private String mVideoPath = "/storage/emulated/0/DCIM/Camera/6676.mp4";
//    private String mVideoPath = "https://cache.bydauto.com.cn/dilink_user_upload_dev/xxl/202312/29/1358147gOchfEJ.mp4";
//    private String mVideoPath = "https://cache.bydauto.com.cn/dilink_user_upload/xxl/202306/25/141345hDYJs0ge.mp4";

//        private String mVideoPath = "http://cdnxdc.tanzi88.com/XDC/dvideo/2018/02/29/056bf3fabc41a1c1257ea7f69b5ee787.mp4";
    private VideoPlayer videoPlayer;
    private TextureView mTextureView;

    private VideoPreviewPanel mVideoPreViewPanel;
    private AVEngine mAVEngine;
    private TextView mTimeInfo;

    public SelectCoverView(Context context) {
        super(context);
        initViews();
    }

    public SelectCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public SelectCoverView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews();
    }

    private void initViews() {
        inflate(getContext(), R.layout.video_selector_layout, this);
        loadVideoData();

        mTimeInfo = findViewById(R.id.text_duration11);
        mTextureView = findViewById(R.id.tv_Video_view);
        mVideoPreViewPanel = findViewById(R.id.rl_video_preview_panel);
        mVideoPreViewPanel.setScrollCallback(new VideoPreviewPanel.OnScrollCallback() {
            @Override
            public void onScrolled(long position) {
                videoPlayer.seekTo(position/1000);
            }

            @Override
            public void onScrollStart() {
                mTextureView.setVisibility(View.VISIBLE);
                Bitmap bitmap = Bitmap.createBitmap(mTextureView.getWidth(), mTextureView.getHeight(),
                        Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.TRANSPARENT);
                cropImageView.setImageBitmap(bitmap);
            }

            @Override
            public void onScrollEnd() {
                mTextureView.setVisibility(View.GONE);
                cropImageView.setImageBitmap(mTextureView.getBitmap());
                cropImageView.zoomOutImage(1);
                float centerX = overlayView.getCropViewRect().centerX();
                float centerY = overlayView.getCropViewRect().centerY();
                float currentX = cropImageView.mCurrentImageCenter[0];
                float currentY = cropImageView.mCurrentImageCenter[1];
                cropImageView.postTranslate(centerX - currentX, centerY - currentY);
            }
        });
        mVideoPreViewPanel.setmTimeInfo(mTimeInfo);
        mAVEngine = AVEngine.getVideoEngine();
        videoPlayer = new VideoPlayer();
        videoPlayer.setTextureView(mTextureView);

        uCropView = findViewById(R.id.ucv_ucrop);
        overlayView = uCropView.getOverlayView();
        overlayView.setShowCropFrame(true);
        overlayView.setShowCropGrid(false);
        overlayView.setPadding(0,0,0,0);
        cropImageView = uCropView.getCropImageView();
        cropImageView.setRotateEnabled(false);
        cropImageView.setAdjustViewBounds(true);
        cropImageView.setTargetAspectRatio(1.25f);
        cropImageView.setPadding(0,0,0,0);

        bindViewTreeCallback();
    }

    private void bindViewTreeCallback() {
        if(mTextureView == null) return;
        mTextureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Size videoOriginalSize = new Size(mTextureView.getMeasuredWidth(), mTextureView.getMeasuredHeight());
                addVideoData(videoOriginalSize);
                playVideo(mVideoPath);
            }
        });
    }

    private void addVideoData(Size size){
        VideoData avComponent = new VideoData(mVideoPath);
        mAVEngine.setmSurfaceViewSize(size);
        mAVEngine.addComponent(avComponent, new AVEngine.EngineCallback() {
            @Override
            public void onCallback(Object[] args1) {
                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mVideoPreViewPanel.updateData(mAVEngine.getVideoState());
                    }
                });
                mAVEngine.setCanvasType("original", new AVEngine.EngineCallback() {
                    @Override
                    public void onCallback(Object... args1) {
                        mTextureView.post(new Runnable() {
                            @Override
                            public void run() {
                                Size size = (Size) args1[0];
                                mTextureView.getLayoutParams().width = size.getWidth();
                                mTextureView.getLayoutParams().height = size.getHeight();
                                Bitmap bitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(),
                                        Bitmap.Config.ARGB_8888);
                                bitmap.eraseColor(Color.TRANSPARENT);
                                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) uCropView.getLayoutParams();
                                layoutParams.width = size.getWidth();
                                layoutParams.height = size.getHeight();
                                uCropView.setLayoutParams(layoutParams);
                                try {
                                    Uri outputUri = Uri.fromFile(new File(getContext().getCacheDir(), "test.png"));
                                    Uri inputUri = Uri.parse(MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, null,null));
                                    cropImageView.setImageUri(inputUri, outputUri);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
//                                mTextureView.requestLayout();
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadVideoData(){
        LinkedList<VideoUtil.FileEntry> mFiles = new LinkedList<>();
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            try {
                mediaMetadataRetriever.setDataSource(mVideoPath, new HashMap<>());
//                mediaMetadataRetriever.setDataSource(mp4.getAbsolutePath());
                if (Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)) > 0) {
                    VideoUtil.FileEntry fileEntry = new VideoUtil.FileEntry();
                    fileEntry.duration = Integer.parseInt(mediaMetadataRetriever.extractMetadata
                            (MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
                    fileEntry.thumb = mediaMetadataRetriever.getFrameAtTime(0);
                    fileEntry.width = Integer.parseInt(mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    fileEntry.height = Integer.parseInt(mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    fileEntry.path = mVideoPath;
                    fileEntry.adjustPath = VideoUtil.getAdjustGopVideoPath(getContext(), fileEntry.path);
                    mFiles.add(fileEntry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mediaMetadataRetriever.release();
            }
        VideoUtil.processVideo(getContext(), mFiles, new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                return false;
            }
        });
    }

    private void playVideo(final String videoUrl) {
        videoPlayer.reset();
        videoPlayer.setOnStateChangeListener(new VideoPlayer.OnStateChangeListener() {
            @Override
            public void onPrepared() {
                videoPlayer.seekTo(0);
            }

            @Override
            public void onReset() {
            }

            @Override
            public void onRenderingStart() {
            }

            @Override
            public void onProgressUpdate(float per) {
            }

            @Override
            public void onPause() {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onComplete() {
            }
        });
        videoPlayer.setDataSource(videoUrl);
        videoPlayer.prepare();
    }

    private Bitmap outupBitmap;
    public void cropVideoCover() {
        cropImageView.cropAndSaveImage(Bitmap.CompressFormat.PNG, 100, new BitmapCropCallback() {
            @Override
            public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                try {
                    outupBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), resultUri);
                    if(onCorpImgListener != null){
                        onCorpImgListener.OnCrop(outupBitmap);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCropFailure(@NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private OnCorpImgListener onCorpImgListener;

    public void setOnCorpImgListener(OnCorpImgListener onCorpImgListener) {
        this.onCorpImgListener = onCorpImgListener;
    }

    public interface OnCorpImgListener{
        void OnCrop(Bitmap bitmap);
    }

}
