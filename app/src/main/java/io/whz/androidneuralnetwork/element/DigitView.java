package io.whz.androidneuralnetwork.element;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import io.whz.androidneuralnetwork.R;
import io.whz.androidneuralnetwork.neural.MNISTUtil;

import static android.view.View.MeasureSpec.EXACTLY;

public class DigitView extends View implements View.OnTouchListener {
    private static final int OVERLAY_TIME = 3;
    private static final int DIGIT_SIDE = 28;

    private final Bitmap mDigitBitmap;
    private final Canvas mDigitCanvas;
    private final Matrix mNarrowMatrix;
    private final Matrix mEnlargeMatrix;
    private final Paint mBgPaint;
    private final Paint mFgPaint;
    private final Paint mRicePaint;

    private final Pair mOldPair;
    private final Pair mNewPair;
    private final Path mRicePath;

    {
        mDigitBitmap = Bitmap.createBitmap(DIGIT_SIDE, DIGIT_SIDE, Bitmap.Config.ARGB_8888);
        mDigitCanvas = new Canvas(mDigitBitmap);
        mNarrowMatrix = new Matrix();
        mEnlargeMatrix = new Matrix();

        mBgPaint = new Paint();
        mBgPaint.setColor(Color.WHITE);

        mFgPaint = new Paint();
        mFgPaint.setColor(Color.BLACK);
        mFgPaint.setAntiAlias(true);

        mRicePaint = new Paint();
        mRicePaint.setColor(Color.GRAY);
        mRicePaint.setAlpha(153);
        mRicePaint.setPathEffect(new DashPathEffect(new float[]{5, 5, 5, 5}, 1));
        mRicePaint.setAntiAlias(true);
        mRicePaint.setStyle(Paint.Style.STROKE);
        mRicePaint.setStrokeWidth(3);

        mOldPair = new Pair();
        mNewPair = new Pair();

        mRicePath = new Path();
    }

    public DigitView(Context context) {
        super(context);
    }

    public DigitView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DigitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DigitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int defaultSide = getResources().getDimensionPixelOffset(R.dimen.digit_view_default_side);

        final int tmpW = measureHelper(widthMeasureSpec, defaultSide);
        final int tmpH = measureHelper(heightMeasureSpec, defaultSide);
        final int finalSide = Math.min(tmpW, tmpH);

        prepare(finalSide);
        setMeasuredDimension(finalSide, finalSide);
    }

    private void prepare(float finalSize) {
        final float scale = finalSize / DIGIT_SIDE;

        mEnlargeMatrix.reset();
        mNarrowMatrix.reset();

        mEnlargeMatrix.setScale(scale, scale);
        mEnlargeMatrix.invert(mNarrowMatrix);

        mRicePath.moveTo(0F, 0F);
        mRicePath.lineTo(finalSize, finalSize);

        mRicePath.moveTo(0F, finalSize);
        mRicePath.lineTo(finalSize, 0F);

        final float halfSize = finalSize / 2;

        mRicePath.moveTo(0F, halfSize);
        mRicePath.lineTo(finalSize, halfSize);

        mRicePath.moveTo(halfSize, 0F);
        mRicePath.lineTo(halfSize, finalSize);

        mRicePath.close();
    }

    private int measureHelper(int spec, int defaultSide) {
        final int mode = MeasureSpec.getMode(spec);
        final int size = MeasureSpec.getSize(spec);

        final int res;

        if (mode == EXACTLY) {
            res = Math.max(defaultSide, size);
        } else {
            res = defaultSide;
        }

        return res;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnTouchListener(this);
        reset();
    }

    public void reset() {
        final Rect rect = new Rect(0, 0, mDigitBitmap.getWidth(), mDigitBitmap.getHeight());
        mDigitCanvas.drawRect(rect, mBgPaint);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mDigitBitmap, mEnlargeMatrix, mFgPaint);
        canvas.drawPath(mRicePath, mRicePaint);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleDownAction(motionEvent);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                handleMoveAction(motionEvent);
                return true;

            default:
                return false;
        }
    }

    private void handleMoveAction(@NonNull MotionEvent motionEvent) {
        mOldPair.copy(mNewPair);
        mNewPair.pos(motionEvent.getX(), motionEvent.getY());
        convert(mNewPair);

        drawLine(mOldPair, mNewPair);
        postInvalidate();
    }

    /**
     * In order to deeper the color of the digit
     */
    private void drawLine(@NonNull Pair oldPair, @NonNull Pair newPair) {
        for (int i = 0; i < OVERLAY_TIME; ++i) {
            mDigitCanvas.drawLine(oldPair.getX(), oldPair.getY(),
                    newPair.getX(), newPair.getY(), mFgPaint);
        }
    }

    /**
     * get darkness, 0.0 for white and 1.0 for black pixel
     */
    public io.whz.androidneuralnetwork.matrix.Matrix getDarkness() {
        final int side = DIGIT_SIDE;
        final int[] pixels = new int[side * side];

        mDigitBitmap.getPixels(pixels, 0, side, 0, 0, side, side);

        final double[] doubles = MNISTUtil.convertBitmap2Darkness(pixels);

        return io.whz.androidneuralnetwork.matrix.Matrix.array(doubles, DIGIT_SIDE * DIGIT_SIDE);
    }

    private void handleDownAction(@NonNull MotionEvent motionEvent) {
        mOldPair.reset();

        mNewPair.pos(motionEvent.getX(), motionEvent.getY());
        convert(mNewPair);
    }

    private void convert(@NonNull Pair pair) {
        mNarrowMatrix.mapPoints(pair.getItems());
    }

    private static final class Pair {
        private final float[] items = new float[2];

        void pos(float x, float y) {
            items[0] = x;
            items[1] = y;
        }

        void copy(@NonNull Pair pair) {
            this.items[0] = pair.getX();
            this.items[1] = pair.getY();
        }

        void reset() {
            pos(0F, 0F);
        }

        float[] getItems() {
            return items;
        }

        float getX() {
            return this.items[0];
        }

        float getY() {
            return this.items[1];
        }
    }
}