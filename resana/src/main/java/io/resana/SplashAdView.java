package io.resana;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import static io.resana.FileManager.FileSpec;

public class SplashAdView extends FrameLayout implements View.OnClickListener, LandingView.Delegate {
    private static final String TAG = ResanaLog.TAG_PREF + "SplashAdView";
    private ImageView image;
    private ImageView label;
    private View progress;
    private Resana resana;

    private Bitmap imageBitmap;
    private Bitmap labelBitmap;

    private AdViewCustomTranslateAnimation progressAnim;

    Ad currAd;

    private Delegate delegate;
    private Activity activity;
    private static final int LOAD_AD_TIME_OUT = 3 * 1000;
    public static final int DEFAULT_SPLASH_DURATION = 3 * 1000;
    private int splashDuration = DEFAULT_SPLASH_DURATION;
    private long lastShowAdRequestTime = -1;

    private WeakRunnable<SplashAdView> prepareSplashTimedOut = new PrepareSplashTimedOut(this);

    Handler handler = new Handler();

    private Dialog landDialog;
    private LandingView landView;

    public static abstract class Delegate {
        public abstract void onFinish();

        public abstract void onFailure(String reason);

        public abstract void closeVideo();

        public void handleTelegramAction(String action) {
        }

        public String getTelegramChatData() {
            return null;
        }
    }

    @Override
    public void closeLanding() {
        dismissLanding();
        progressAnim.resume();
    }

    @Override
    public void landingActionClicked() {
        landingClicked();
        dismissLanding();
        performAction(true);
    }

    public SplashAdView(Context context) {
        super(context);
        init();
    }

    public SplashAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SplashAdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setShowProgress(boolean show) {
        progress.setAlpha(show ? 1 : 0);
    }

    private void init() {
        setOnClickListener(this);
        FrameLayout container = new FrameLayout(getContext());
        image = new ImageView(getContext());
        image.setOnClickListener(this);
        image.setAdjustViewBounds(true);
        container.addView(image, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        label = new ImageView(getContext());
        label.setAdjustViewBounds(true);
        label.setMaxWidth(AdViewUtil.getResanaLabelMaxWidth());
        container.addView(label, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        progress = new View(getContext());
        container.addView(progress, new LayoutParams(LayoutParams.MATCH_PARENT, AdViewUtil.dpToPx(getContext(), 5), Gravity.BOTTOM));

        addView(container, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        setVisibility(GONE);
    }

    @Override
    public void onClick(View v) {
        if (!allowClick())
            return;
        adClicked();
        if (currAd.hasLanding())
            openLanding();
        else
            performAction(false);
    }

    private void adClicked() {
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            resanaInternal.onSplashClicked(currAd);
    }

    private void landingClicked() {
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            resanaInternal.onSplashLandingClicked(currAd);
    }

    private void performAction(boolean fromLanding) {
        ResanaLog.d(TAG, "performAction() fromLanding = [" + fromLanding + "]");
        final Intent intentAction = AdViewUtil.getAdActionIntent(currAd);
        if (intentAction != null) {
            handleIntentAction(intentAction);
            return;
        }
        if (fromLanding)
            progressAnim.resume();
    }

    private void handleIntentAction(Intent intent) {
        cancelProgressAnim();
        onFinish();
        delegate.closeVideo();
        try {
            getContext().startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void cancelProgressAnim() {
        if (progressAnim != null)
            progressAnim.cancel();
        progressAnim = null;
    }

    private void openLanding() {
        progressAnim.pause();
        landView = new LandingView(getContext());
        landView.setDelegate(this);
        landView.setData(currAd);
        landDialog = new Dialog(activity);
        landDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        landDialog.setCancelable(false);
        landDialog.setContentView(landView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        landDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        try {
            landDialog.show();
        } catch (Exception ignored) {
            onFinish();
        }
    }

    private boolean allowClick() {
        return resana != null && resana.instance != null &&
                (currAd.hasLanding()
                        || currAd.data.intent != null
                        || currAd.data.link != null);
    }

    public void setup(Activity activity, Resana resana, Delegate delegate) {
        if (activity == null)
            throw new IllegalArgumentException("Activity object can't be null");
        if (resana == null)
            throw new IllegalArgumentException("Resana object can't be null");
        if (delegate == null)
            throw new IllegalArgumentException("Delegate object can't be null");
        if (resana.instance == null)
            throw new RuntimeException("resana object is released!");
        this.resana = resana;
        this.delegate = delegate;
        this.activity = activity;
    }

    public boolean isShowingAd() {
        if (currAd == null)
            return false;
        if (landDialog != null)
            return true;
        //even if view goes to a bad state this will
        //return false in a few seconds
        return System.currentTimeMillis() - lastShowAdRequestTime < splashDuration + LOAD_AD_TIME_OUT;
    }

    public void showSplash() {
        ResanaLog.v(TAG, "showSplash");
        handler.postDelayed(prepareSplashTimedOut, LOAD_AD_TIME_OUT);
        lastShowAdRequestTime = System.currentTimeMillis();
        ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal == null) {
            ResanaLog.w(TAG, "The resana reference object provided in setup phase is released!");
            onFail("The resana object provided in setup phase is released!");
            return;
        }
        resanaInternal.attachSplashViewer(this);
    }

    void startShowingAd(Ad ad) {
        ResanaLog.v(TAG, "startSplashAd");
        this.currAd = ad;
        setBackgroundColor(ad.getBackgroundColor());
        progress.setBackgroundColor(Color.YELLOW);
        if (ad.getDuration() > 0)
            splashDuration = ad.getDuration();
        handler.removeCallbacks(prepareSplashTimedOut);
        loadSplashImageFromFile();
        setLabel();
    }

    private void setLabel() {
        if ("none".equals(currAd.getLabelUrl(getContext())))
            label.setVisibility(GONE);
        else {
            FileManager.getInstance(getContext()).loadBitmapFromFile(new FileSpec(currAd.getLabelFileName()), new LabelBitmapLoadedDelegate(this));
        }
    }

    private void labelBitmapLoaded(boolean success, Object... bitmaps) {
        if (!success)
            return;
        labelBitmap = (Bitmap) bitmaps[0];
        label.setImageBitmap(labelBitmap);
    }

    void loadSplashImageFromFile() {
        FileManager.getInstance(getContext()).loadBitmapFromFile(new FileSpec(currAd.getSplashImageFileName()), new ImageBitmapLoadedDelegate(this));
    }

    private void splashImageLoaded(Bitmap b) {
        imageBitmap = b;
        image.setImageBitmap(b);
        startAnimation();
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null) {
            resanaInternal.onSplashRendered(currAd);
        }
    }

    private void startAnimation() {
        ResanaLog.v(TAG, "startAnimation");
        setVisibility(VISIBLE);
        setAlpha(0);
        AdViewUtil.runOnceWhenViewWasDrawn(this, new Runnable() {
            @Override
            public void run() {
                progressAnim = new AdViewCustomTranslateAnimation(-progress.getWidth() - 100, 0, 0, 0);
                progressAnim.setFillAfter(true);
                progressAnim.setDuration(splashDuration);
                progressAnim.setInterpolator(new LinearInterpolator());
                progressAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        setAlpha(1);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        onFinish();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                progress.startAnimation(progressAnim);
            }
        });
        invalidate();
        requestLayout();
    }

    private void releaseSplash() {
        if (currAd != null) {
            final ResanaInternal resanaInternal = resana.instance;
            if (resanaInternal != null)
                resanaInternal.releaseSplash(currAd);
        }
    }

    void splashAdDidNotLoadInTime() {
        ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            resanaInternal.detachSplashViewer(this);
        onFail("Splash Initiation Timed Out");
    }

    void cancelShowingAd(String reason) {
        onFail(reason);
    }

    private void onFail(String reason) {
        handler.removeCallbacks(prepareSplashTimedOut);
        releaseSplash();
        currAd = null;
        lastShowAdRequestTime = -1;
        ResanaLog.i(TAG, "Failed to show Splash. reason: " + reason);
        delegate.onFailure(reason);
    }

    private void onFinish() {
        ResanaLog.i(TAG, "Splash Ad rendered successfully");
        setVisibility(GONE);
        releaseSplash();
        currAd = null;
        lastShowAdRequestTime = -1;
        delegate.onFinish();
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

    private void dismissLanding() {
        if (landDialog == null)
            return;
        try {
            landDialog.dismiss();
        } catch (Exception ignored) {
        }
        landView.freeBitmapResources();
        landDialog = null;
        landView = null;
    }

    private static class PrepareSplashTimedOut extends WeakRunnable<SplashAdView> {

        PrepareSplashTimedOut(SplashAdView ref) {
            super(ref);
        }

        @Override
        void run(SplashAdView object) {
            object.splashAdDidNotLoadInTime();
        }
    }

    private static class LabelBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<SplashAdView> viewRef;

        LabelBitmapLoadedDelegate(SplashAdView view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SplashAdView view = viewRef.get();
            if (view != null)
                view.labelBitmapLoaded(success, args);
        }
    }

    private static class ImageBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<SplashAdView> viewRef;

        ImageBitmapLoadedDelegate(SplashAdView view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... bitmaps) {
            final SplashAdView view = viewRef.get();
            if (view != null)
                if (success)
                    view.splashImageLoaded((Bitmap) bitmaps[0]);
                else
                    view.onFail("Could Not Load Splash Image From Storage.");
        }
    }
}