package com.zy.videocoverseletor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.zy.videocoverseletor.utils.ScreenUtils;

/**
 * 裁剪View
 */
public class ClipView extends View {
    private static final String TAG = ClipView.class.getSimpleName();
    public static final int mPaintSize = 16;
    public static final int DRAG_BTN_WIDTH = 6;
    public static final int LINE_WIDTH = 6;
    private static final int CORNER_RADIO = 4;
    private boolean mIsDrag = false;
    private boolean mDragLeft = false;
    private boolean mDragRight = false;
    private boolean mIsEdit = false;
    private Paint mPaint;
    private Rect[] mRect;
    private PointF mDownPoint = new PointF();
    float topLeftRadius = ScreenUtils.dip2px(CORNER_RADIO);
    float topRightRadius = ScreenUtils.dip2px(CORNER_RADIO);
    float bottomLeftRadius = ScreenUtils.dip2px(CORNER_RADIO);
    float bottomRightRadius = ScreenUtils.dip2px(CORNER_RADIO);
    private ClipCallback mClipCallback;

    public ClipView(Context context) {
        super(context);
        path = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xffFFffff);
        mPaint.setTextSize(mPaintSize);
        mRect = new Rect[6];
        for (int i = 0; i < 6; i++) {
            mRect[i] = new Rect();
        }
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!mIsEdit) {
                            mIsEdit = true;
                            freshRoi();
                            return false;
                        }
                        mDragLeft = mRect[2].contains((int) event.getX(), (int) event.getY());
                        mDragRight = mRect[3].contains((int) event.getX(), (int) event.getY());
                        mDownPoint.x = event.getX();
                        mDownPoint.y = event.getY();
                        mIsDrag = mDragLeft || mDragRight;
                        if (mIsDrag) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        } else {
                            return false;
                        }
                        Log.d(TAG, "ACTION_DOWN" + event.toString());
                    case MotionEvent.ACTION_MOVE:
                        if (mIsDrag) {
                            if (mDragLeft) {
                                mRect[2].left = Math.max(0, (int) event.getX());
                                mRect[2].right = mRect[2].left + DRAG_BTN_WIDTH;
                                mRect[0].left = mRect[1].left = (int) event.getX();
                            } else if (mDragRight) {
                                mRect[3].left = Math.min((int) event.getX(), getLayoutParams().width - DRAG_BTN_WIDTH);
                                mRect[3].right = mRect[3].left + DRAG_BTN_WIDTH;
                                mRect[0].right = mRect[1].right = (int) event.getX();
                            }
                        }
                        Log.d(TAG, "ACTION_MOVE" + event.toString());
                        break;
                    case MotionEvent.ACTION_UP:
                        mIsDrag = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                        if (event.getEventTime() - event.getDownTime() > 100) {
                            mIsEdit = !mIsEdit;
                            callbackIfNeed();
                            Log.d(TAG, "ACTION_UP" + event.toString());
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        getParent().requestDisallowInterceptTouchEvent(false);
                        mIsDrag = false;
                        if (event.getEventTime() - event.getDownTime() < 100) {
                            mIsEdit = !mIsEdit;
//                            callbackIfNeed();
                            Log.d(TAG, "ACTION_CANCEL" + event.toString());
                        }
                        break;
                }
                post(() -> invalidate());
                return true;
            }
        });
    }

    public void freshRoi() {
        int width = getLayoutParams().width;
        int height = getLayoutParams().height;

        mRect[0].left = DRAG_BTN_WIDTH;
        mRect[0].top = 0;
        mRect[0].right = width - DRAG_BTN_WIDTH;
        mRect[0].bottom = mRect[0].top + LINE_WIDTH;

        mRect[1].left = 0;
        mRect[1].top = height - LINE_WIDTH;
        mRect[1].right = width - DRAG_BTN_WIDTH;
        mRect[1].bottom = mRect[1].top + LINE_WIDTH;

        mRect[2].left = 0;
        mRect[2].top = 0;
        mRect[2].right = DRAG_BTN_WIDTH;
        mRect[2].bottom = height;

        mRect[3].left = width - DRAG_BTN_WIDTH;
        mRect[3].top = 0;
        mRect[3].right = width;
        mRect[3].bottom = height;
    }

    public interface ClipCallback {
        void onClip(Rect src, Rect dst);
    }

    private void callbackIfNeed() {
        if (mClipCallback != null) {
            mRect[4].left = 0;
            mRect[4].right = getLayoutParams().width - 2 * DRAG_BTN_WIDTH;
            mRect[5].left = mRect[2].right - DRAG_BTN_WIDTH;
            mRect[5].right = mRect[3].left - DRAG_BTN_WIDTH;
            mClipCallback.onClip(mRect[4], mRect[5]);
        }
    }

    public void setClipCallback(ClipCallback clipCallback) {
        mClipCallback = clipCallback;
    }

    private Path path;

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mIsEdit) {
            freshRoi();
        }
        path.reset();
        RectF rectF = new RectF(mRect[2].left, mRect[2].top, mRect[3].right, mRect[3].bottom);
        path.addRoundRect(rectF, new float[]{
                topLeftRadius, topLeftRadius,
                topRightRadius, topRightRadius,
                bottomLeftRadius, bottomLeftRadius,
                bottomRightRadius, bottomRightRadius
        }, Path.Direction.CCW);
        canvas.clipRect(rectF);
        canvas.clipPath(path, Region.Op.REPLACE);

        canvas.drawRect(mRect[0], mPaint);//上
        canvas.drawRect(mRect[1], mPaint);//下
        canvas.drawRect(mRect[2], mPaint);//左
        canvas.drawRect(mRect[3], mPaint);//右


    }
}
