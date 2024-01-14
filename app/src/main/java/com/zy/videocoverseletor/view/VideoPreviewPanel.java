package com.zy.videocoverseletor.view;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import static com.zy.videocoverseletor.data.AVEngine.VideoState.VideoStatus.SEEK;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.zy.videocoverseletor.R;
import com.zy.videocoverseletor.data.VideoData;
import com.zy.videocoverseletor.data.AVEngine;
import com.zy.videocoverseletor.utils.ScreenUtils;
import com.zy.videocoverseletor.utils.VideoUtil;

import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;

/**
 * 视频预览面板
 */
public class VideoPreviewPanel extends RelativeLayout {
    private RecyclerView mThumbPreview;//缩略图预览
    private ClipView mClipView;
    private ImageView mSplitView;
    private Size mLayoutSize = new Size(0, 0);
    private int mCacheScrollX = 0;
    private AVEngine.VideoState mVideoState;
    private List<ViewType> mInfoList = new LinkedList<>();
    private int mThumbSize = 60;
//    RecyclerViewCornerRadius radiusItemDecoration;

    public VideoPreviewPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChildes();
        bindViewTreeCallback();
    }

    private void initChildes() {
        //new
        mThumbSize = compatSize(mThumbSize);
        mThumbPreview = new RecyclerView(getContext());
        mThumbPreview.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        mThumbPreview.setAdapter(new ThumbAdapter());
        mClipView = new ClipView(getContext());
        mSplitView = new ImageView(getContext());
        mSplitView.setScaleType(ImageView.ScaleType.FIT_XY);
        mSplitView.setImageResource(R.drawable.drawable_video_split);

        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, compatSize(120)));
        addView(relativeLayout);

        //设置相关属性
        relativeLayout.addView(mThumbPreview, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mThumbSize));
        relativeLayout.addView(mClipView, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, mThumbSize + 2 * ClipView.LINE_WIDTH));
        relativeLayout.addView(mSplitView, new LayoutParams( compatSize(3), ViewGroup.LayoutParams.MATCH_PARENT));
        ((LayoutParams) mClipView.getLayoutParams()).addRule(CENTER_VERTICAL);
        ((LayoutParams) mClipView.getLayoutParams()).addRule(ALIGN_PARENT_LEFT);
        ((LayoutParams) mSplitView.getLayoutParams()).addRule(CENTER_IN_PARENT);
        ((LayoutParams) mThumbPreview.getLayoutParams()).addRule(CENTER_IN_PARENT);

        //绑定回调
        mClipView.setClipCallback(new ClipView.ClipCallback() {
            @Override
            public void onClip(Rect src, Rect dst) {
                if (mClipCallback != null) {
                    mClipCallback.onClip(src, dst);
                }
                AVEngine.getVideoEngine().updateEdit(mVideoState.editComponent, false);
                updateClip();
            }
        });

        mThumbPreview.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AVEngine.getVideoEngine().seek(true);//进入seek模式
                    if(callback != null){
                        callback.onScrollStart();
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    AVEngine.getVideoEngine().seek(false);//退出seek模式，处于暂停状态
                    if(callback != null){
                        callback.onScrollEnd();
                    }
                }
                updateCorrectScrollX();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateCorrectScrollX();
                updateClip();
                if (recyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE &&
                        AVEngine.getVideoEngine().getVideoState().status == SEEK) {
                    long position = (long) (1000000.f / mThumbSize * mCacheScrollX);
                    if(callback != null){
                        callback.onScrolled(position);
                        freshUI();
                    }
                    AVEngine.getVideoEngine().seek(position);
                }
            }
        });
    }

    OnScrollCallback callback;

    public void setScrollCallback(OnScrollCallback callback) {
        this.callback = callback;
    }

    public interface OnScrollCallback {
        void onScrolled(long position);

        void onScrollStart();

        void onScrollEnd();
    }

    private void updateCorrectScrollX() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mThumbPreview.getLayoutManager();
        int firstPos = layoutManager.findFirstVisibleItemPosition();
        if (firstPos == 0) {
            mCacheScrollX = -layoutManager.findViewByPosition(0).getLeft();
        } else {
            int totalScroll = mLayoutSize.getWidth() / 2;
            for (int i = 1; i < firstPos; i++) {
                totalScroll += mInfoList.get(i).duration / 1000000.f * mThumbSize;
            }
            totalScroll += -layoutManager.findViewByPosition(firstPos).getLeft();
            mCacheScrollX = totalScroll;
        }
    }

    private void bindViewTreeCallback() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLayoutSize = new Size(getMeasuredWidth(), getMeasuredHeight());
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateData(mVideoState);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private int compatSize(int size) {
        return (int) (size * getContext().getResources().getDisplayMetrics().density);
    }

    public void updateData(AVEngine.VideoState videoState) {
        if (videoState == null) return;
        mVideoState = videoState;
        //处理预览缩略等相关数据
        mInfoList.clear();
        mInfoList.add(new ViewType(TYPE_HEAD_FOOT));
        long currentPts = 0;
        while (currentPts < videoState.durationUS) {
            VideoData avVideo = mVideoState.videoComponent;
            mInfoList.add(new ViewType(TYPE_THUMB));
            mInfoList.get(mInfoList.size() - 1).component = avVideo;
            //计算正确的pts，针对单个文件
            long correctFilePts = currentPts - avVideo.getEngineStartTime() + avVideo.getClipStartTime();
            mInfoList.get(mInfoList.size() - 1).imgPath = VideoUtil.getThumbJpg(getContext(), avVideo.getPath(), correctFilePts);
            //处理开头不满1s
            if (correctFilePts % 1000000 != 0) {
                mInfoList.get(mInfoList.size() - 1).duration = rightTime(correctFilePts) - correctFilePts;
                mInfoList.get(mInfoList.size() - 1).clip = 1;
                currentPts += mInfoList.get(mInfoList.size() - 1).duration;
                continue;
            }
            //处理最后一帧不满1s
            if (currentPts + 1000000 > avVideo.getEngineEndTime()) {
                mInfoList.get(mInfoList.size() - 1).duration = avVideo.getEngineEndTime() - currentPts;
                mInfoList.get(mInfoList.size() - 1).clip = 2;
                currentPts = avVideo.getEngineEndTime();
                continue;
            }
            //正常
            mInfoList.get(mInfoList.size() - 1).duration = 1000000;
            mInfoList.get(mInfoList.size() - 1).clip = 0;
            currentPts += 1000000;
        }
        mInfoList.add(new ViewType(TYPE_HEAD_FOOT));
        mThumbPreview.getAdapter().notifyDataSetChanged();
    }

    public static long rightTime(long time) {
        return (time + 1000000) / 1000000 * 1000000;
    }

    public void updateScroll(boolean isSmooth) {
        if (mVideoState == null) return;
        boolean needScroll = mVideoState.status == AVEngine.VideoState.VideoStatus.START
                || (mVideoState.status == AVEngine.VideoState.VideoStatus.PAUSE && mThumbPreview.getScrollState() == SCROLL_STATE_IDLE
        );
        if (needScroll) {
            int correctScrollX = (int) (mThumbSize / 1000000.f * AVEngine.getVideoEngine().getMainClock());
            if (isSmooth) {
                mThumbPreview.smoothScrollBy(correctScrollX - mCacheScrollX, 0);
            } else {
                mThumbPreview.scrollBy(correctScrollX - mCacheScrollX, 0);
            }
        }
    }

    TextView mTimeInfo;

    public void setmTimeInfo(TextView mTimeInfo) {
        this.mTimeInfo = mTimeInfo;
    }

    private void freshUI() {
        ((Activity)getContext()).getWindow().getDecorView().post(() -> {
            AVEngine.VideoState mVideoState = AVEngine.getVideoEngine().getVideoState();
            if (mVideoState != null) {
                long positionInMS = (AVEngine.getVideoEngine().getMainClock() + 999) / 1000;
                long durationInMS = (mVideoState.videoDuration + 999) / 1000;
                mTimeInfo.setText(String.format("%02d:%02d:%03d / %02d:%02d:%03d",
                        positionInMS / 1000 / 60 % 60, positionInMS / 1000 % 60, positionInMS % 1000,
                        durationInMS / 1000 / 60 % 60, durationInMS / 1000 % 60, durationInMS % 1000));
//                mPlayBtn.setImageResource(mVideoState.status == START ? R.drawable.icon_video_pause : R.drawable.icon_video_play);
                updateScroll(true);
            }
        });
    }

    public void updateClip() {
        if (mVideoState.isEdit) {
            updateCorrectScrollX();
            int eStartTime = (int) (mVideoState.editComponent.getEngineStartTime() / 1000000.f * mThumbSize);
            LayoutParams layoutParams = (LayoutParams) mClipView.getLayoutParams();
            layoutParams.width = (int) (mVideoState.editComponent.getEngineDuration() / 1000000.f * mThumbSize) + 2 * ClipView.DRAG_BTN_WIDTH;
            layoutParams.height = mThumbSize + 2 * ClipView.LINE_WIDTH;
            layoutParams.leftMargin = eStartTime + mLayoutSize.getWidth() / 2 -
                    ClipView.DRAG_BTN_WIDTH - mCacheScrollX;
            mClipView.requestLayout();
        }
        mClipView.setVisibility(mVideoState.isEdit ? VISIBLE : GONE);
    }

    private ClipView.ClipCallback mClipCallback;
    public void setClipCallback(ClipView.ClipCallback clipCallback) {
        mClipCallback = clipCallback;
    }

    private static class ViewType {
        public int type;
        public String imgPath;
        public VideoData component;
        public long duration;
        public int clip;

        public ViewType(int type) {
            this.type = type;
        }
    }

    private static final int TYPE_HEAD_FOOT = 0;
    private static final int TYPE_THUMB = 1;

    private static class ThumbViewHolder extends RecyclerView.ViewHolder {

        public ThumbViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private class ThumbAdapter extends RecyclerView.Adapter<ThumbViewHolder> {

        @NonNull
        @Override
        public ThumbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView view = new ImageView(parent.getContext());
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setLayoutParams(new RecyclerView.LayoutParams(viewType == TYPE_HEAD_FOOT ?
                    mLayoutSize.getWidth() / 2 : mThumbSize, mThumbSize));
            return new ThumbViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ThumbViewHolder holder, int position) {
            final int position1 = holder.getAdapterPosition();
            ViewType viewType = mInfoList.get(position);
            if (viewType.type == TYPE_THUMB) {
                holder.itemView.getLayoutParams().width = (int) (viewType.duration / 1000000.f * mThumbSize);
                if (viewType.duration < 1000000.f) {
                    Glide.with(getContext())
                            .load(viewType.imgPath)
                            .transform(new BitmapTransformation() {
                                @Override
                                public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

                                }

                                @Override
                                protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
                                    if (toTransform.getWidth() == holder.itemView.getLayoutParams().width) {
                                        return toTransform;
                                    }
                                    if (viewType.clip == 2) {
                                        return Bitmap.createBitmap(toTransform, 0, 0,
                                                (int) (toTransform.getWidth() * viewType.duration / 1000000.f), toTransform.getHeight());
                                    } else {
                                        return Bitmap.createBitmap(toTransform, (int) (toTransform.getWidth() * (1000000.f - viewType.duration) / 1000000.f), 0,
                                                (int) (toTransform.getWidth() * viewType.duration / 1000000.f), toTransform.getHeight());
                                    }
                                }
                            }).into((ImageView) holder.itemView);
                } else {
                    Glide.with(getContext())
                            .load(viewType.imgPath)
                            .into((ImageView) holder.itemView);
                }
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AVEngine.getVideoEngine().updateEdit(mInfoList.get(position1).component, !mVideoState.isEdit);
                        updateClip();
                    }
                });
            } else {
                holder.itemView.getLayoutParams().width = mLayoutSize.getWidth() / 2;
                Glide.with(getContext())
                        .load("")
                        .into((ImageView) holder.itemView);
            }
        }

        @Override
        public int getItemCount() {
            return mInfoList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mInfoList.get(position).type;
        }
    }
}
