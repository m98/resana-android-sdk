package io.resana;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

public class GIFView extends View {

    Context context;

    private Movie movie;
    private long movieStart;
    private float mScale;
    private int mMeasuredMovieWidth;
    private int mMeasuredMovieHeight;
    private float mLeft;
    private float mTop;
    private boolean mVisible;

    GIFView(Context context) {
        super(context);
        this.context = context;
    }

    GIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    GIFView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    void setGif(Movie gif) {
        this.movie = gif;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        /*
         * Calculate left / top for drawing in center
         */
        mLeft = (getWidth() - mMeasuredMovieWidth) / 2f;
        mTop = (getHeight() - mMeasuredMovieHeight) / 2f;

        mVisible = getVisibility() == View.VISIBLE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (movie != null) {
            int movieWidth = movie.width();
            int movieHeight = movie.height();

            /*
             * Calculate horizontal scaling
             */
            float scaleH = 1f;
            int measureModeWidth = MeasureSpec.getMode(widthMeasureSpec);

            if (measureModeWidth != MeasureSpec.UNSPECIFIED) {
                int maximumWidth = MeasureSpec.getSize(widthMeasureSpec);
                if (movieWidth > maximumWidth) {
                    scaleH = (float) movieWidth / (float) maximumWidth;
                }
            }

            /*
             * calculate vertical scaling
             */
            float scaleW = 1f;
            int measureModeHeight = MeasureSpec.getMode(heightMeasureSpec);

            if (measureModeHeight != MeasureSpec.UNSPECIFIED) {
                int maximumHeight = MeasureSpec.getSize(heightMeasureSpec);
                if (movieHeight > maximumHeight) {
                    scaleW = (float) movieHeight / (float) maximumHeight;
                }
            }

            /*
             * calculate overall scale
             */
            mScale = 1f / Math.max(scaleH, scaleW);

            mMeasuredMovieWidth = (int) (movieWidth * mScale);
            mMeasuredMovieHeight = (int) (movieHeight * mScale);

            setMeasuredDimension(mMeasuredMovieWidth, mMeasuredMovieHeight);

        } else {
            /*
             * No movie set, just set minimum available size.
             */
            setMeasuredDimension(getSuggestedMinimumWidth(), getSuggestedMinimumHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();
        if (movieStart == 0)
            movieStart = now;
        if (movie != null) {
            int relTime = (int) ((now - movieStart) % movie.duration());
            movie.setTime(relTime);
            canvas.scale(mScale, mScale);
            movie.draw(canvas, mLeft / mScale, mTop / mScale);
            this.invalidate();
        }
    }
}
