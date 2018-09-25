package io.resana;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

class LandingView extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = "LandingView";
    Context context;

    ImageView image;
    GIFView gif;
    TextView close;
    ImageView label;

    ProgressBar loading;

    LinearLayout infoContainer;
    TextView hideInfo;
    TextView info;
    boolean canShowInfo;

    Bitmap imageBitmap;
    Bitmap labelBitmap;

    Delegate delegate;

    Ad ad;

    interface Delegate {
        void closeLanding();

        void landingActionClicked();
    }

    public LandingView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        loading = new ProgressBar(getContext());
        loading.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
        loading.setIndeterminate(true);
        loading.setPadding(0, AdViewUtil.dpToPx(getContext(), 40), 0, AdViewUtil.dpToPx(getContext(), 40));
        final LayoutParams lp0 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp0.addRule(CENTER_IN_PARENT);
        addView(loading, lp0);

        image = new ImageView(getContext());
        image.setAdjustViewBounds(true);
        final LayoutParams lp1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp1.addRule(CENTER_IN_PARENT);
        addView(image, lp1);
        image.setOnClickListener(this);

        //todo double check here
        gif = new GIFView(context);
        final LayoutParams lp5 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp5.addRule(CENTER_IN_PARENT); //todo check it. gif is not center of page
        addView(gif, lp5);
        gif.setOnClickListener(this);

        label = new ImageView(getContext());
        label.setAdjustViewBounds(true);
        label.setMaxWidth(AdViewUtil.getResanaLabelMaxWidth());
        label.setOnClickListener(this);
        final LayoutParams lp3 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp3.addRule(ALIGN_PARENT_TOP);
        lp3.addRule(ALIGN_PARENT_LEFT);
        addView(label, lp3);

        close = new TextView(getContext());
        close.setText("بستن");
        close.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundColor(Color.argb(180, 0, 0, 0));
        AdViewUtil.setTypeFace(getContext(), close);
        close.setPadding(AdViewUtil.dpToPx(getContext(), 10), AdViewUtil.dpToPx(getContext(), 5), AdViewUtil.dpToPx(getContext(), 10), AdViewUtil.dpToPx(getContext(), 5));
        close.setOnClickListener(this);
        final LayoutParams lp2 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.addRule(ALIGN_PARENT_TOP);
        lp2.addRule(ALIGN_PARENT_RIGHT);
        addView(close, lp2);

        infoContainer = new LinearLayout(getContext());
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        infoContainer.setBackgroundColor(Color.argb(220, 0, 0, 0));

        final ScrollView sc = new ScrollView(getContext());
        info = new TextView(getContext());
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        info.setTextColor(Color.WHITE);
        AdViewUtil.setTypeFace(getContext(), info);
        final int pad = AdViewUtil.dpToPx(getContext(), 20);
        info.setPadding(pad, pad, pad, pad);
        sc.addView(info);
        infoContainer.addView(sc, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        hideInfo = new TextView(getContext());
        hideInfo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        hideInfo.setTextColor(Color.rgb(43, 209, 255));
        hideInfo.setText("بازگشت");
        hideInfo.setPadding(pad, pad, pad, pad);
        hideInfo.setGravity(Gravity.LEFT);
        hideInfo.setOnClickListener(this);
        AdViewUtil.setTypeFace(getContext(), hideInfo);
        infoContainer.addView(hideInfo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        infoContainer.setVisibility(GONE);
        final LayoutParams lp4 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp4.addRule(CENTER_IN_PARENT);
        addView(infoContainer, lp4);
    }

    void setData(Ad ad) {
        this.ad = ad;
        setImage();
        setLabel();
        setInfo();
    }

    void setData(NativeAd ad) {
        if (ad.getLandingType() == NativeAd.IMAGE) {
            setImage(ad);
        } else if (ad.getLandingType() == NativeAd.GIF) {
            setGif(ad);
        }
        setLabel(ad);
        setInfo(ad);
    }

    void setImage() {
        final String fname = ad.getLandingImageFileName();
        FileManager.getInstance(getContext()).loadBitmapFromFile(new FileManager.FileSpec(fname), new ImageBitmapLoadedDelegate(this));
    }

    void setImage(NativeAd ad) {
        final String fname = ad.getLandingImageFileName();
        FileManager.getInstance(getContext()).loadBitmapFromFile(new FileManager.FileSpec(fname), new ImageBitmapLoadedDelegate(this));
    }

    void setGif(NativeAd ad) {
        final String fname = ad.getLandingImageFileName();
        FileManager.getInstance(getContext()).loadMovieFromFile(new FileManager.FileSpec(fname), new MovieLoadedDelegate(this));
    }

    void imageBitmapLoaded(boolean success, Object... bitmaps) {
        if (success) {
            loading.animate().alpha(0f).setDuration(100);
            image.setAlpha(0f);
            imageBitmap = (Bitmap) bitmaps[0];
            image.setImageBitmap(imageBitmap);
            image.animate().alpha(1f).setDuration(100).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    loading.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else {
            delegate.landingActionClicked();
        }
    }

    void movieLoaded(boolean success, Object... movies) {
        if (success) {
            loading.animate().alpha(0f).setDuration(100);
            gif.setAlpha(0f);
            Movie gifFile = (Movie) movies[0];
            gif.setGif(gifFile);
            gif.animate().alpha(1f).setDuration(100).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    loading.setVisibility(GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    void setLabel() {
        if ("none".equals(ad.getLabelUrl(getContext())))
            label.setVisibility(GONE);
        else
            FileManager.getInstance(getContext()).loadBitmapFromFile(new FileManager.FileSpec(ad.getLabelFileName()), new LabelBitmapLoadedDelegate(this));
    }

    void setLabel(NativeAd ad) {
        if ("none".equals(ad.getLabelUrl()))
            label.setVisibility(GONE);
        else
            FileManager.getInstance(getContext()).loadBitmapFromFile(new FileManager.FileSpec(ad.getLabelFileName()), new LabelBitmapLoadedDelegate(this));
    }

    void labelBitmapLoaded(boolean success, Object... bitmaps) {
        if (!success)
            return;
        label.setAlpha(0f);
        labelBitmap = (Bitmap) bitmaps[0];
        label.setImageBitmap(labelBitmap);
        label.animate().alpha(1f).setDuration(200);
    }

    void setInfo() {
        final String s = ad.getInfoText(getContext());
        if (s != null && !s.isEmpty()) {
            canShowInfo = true;
            info.setText(s);
        }
    }

    void setInfo(NativeAd ad) {
        final String s = ad.getLabelText();
        if (s != null && !s.isEmpty()) {
            canShowInfo = true;
            info.setText(s);
        }
    }

    void freeBitmapResources() {
        image.setImageDrawable(null);
        if (imageBitmap != null) {
            imageBitmap.recycle();
            imageBitmap = null;
        }
        label.setImageDrawable(null);
        if (labelBitmap != null) {
            labelBitmap.recycle();
            labelBitmap = null;
        }
    }

    private void showInfo() {
        final LayoutParams lp = new LayoutParams(image.getWidth(), image.getHeight());
        lp.addRule(CENTER_IN_PARENT);
        infoContainer.setLayoutParams(lp);
        infoContainer.setAlpha(0f);
        infoContainer.animate().alpha(1f).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                infoContainer.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void hideInfo() {
        infoContainer.animate().alpha(0f).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                infoContainer.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onClick(View v) {
        if (close == v)
            delegate.closeLanding();
        else if (label == v && canShowInfo)
            showInfo();
        else if (hideInfo == v)
            hideInfo();
        else
            delegate.landingActionClicked();
    }

    private static class ImageBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<LandingView> viewRef;

        ImageBitmapLoadedDelegate(LandingView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final LandingView view = viewRef.get();
            if (view != null)
                view.imageBitmapLoaded(success, args);
        }
    }

    private static class MovieLoadedDelegate extends FileManager.Delegate {
        WeakReference<LandingView> viewRef;

        MovieLoadedDelegate(LandingView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final LandingView view = viewRef.get();
            if (view != null)
                view.movieLoaded(success, args);
        }
    }

    private static class LabelBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<LandingView> viewRef;

        LabelBitmapLoadedDelegate(LandingView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final LandingView view = viewRef.get();
            if (view != null)
                view.labelBitmapLoaded(success, args);
        }
    }
}
