/******************************************************************************
 * Copyright 2015-2016 ResanaInternal
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.resana;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * subtitle ads are deprecated and will be removed as soon as december 1,2018
 */
@Deprecated
public class SubtitleAdView extends FrameLayout implements View.OnClickListener, LandingView.Delegate {
    private static final String TAG = ResanaLog.TAG_PREF + "SubtitleAdView";
    private static final int WAIT_TIME_BEFORE_SKIP = 5 * 1000;
    private static final int TEXT_SIZE_SMALL = 12;
    private static final int TEXT_SIZE_BIG = 16;

    private long lastWaitStartTime = -1;
    private int lastWaitAmount = -1;

    enum State {
        Idle, ShowingText, ShowingImage, Wait
    }

    boolean skipRepetitiveAds = true;


    Ad currAd;
    Delegate delegate;
    Resana resana;
    private Activity activity;
    State state = State.Idle;
    boolean playing;
    boolean hasShownAdBefore;

    HorizontalScrollView ribbon;
    LinearLayout container;
    TextView text;
    ImageView logo;
    ImageView image;
    FrameLayout skip;
    TextView skipText;
    AdViewCustomTranslateAnimation textAnim;
    AdViewCustomTranslateAnimation imageAnim;

    private boolean landIsOpen;
    private Dialog landDialog;
    private LandingView landView;

    Bitmap imgBitmap;
    Bitmap logoBitmap;

    List<String> renderedAds = new ArrayList<>();

    WeakRunnable endWait = new EndWait(this);

    WeakRunnable showSkip = new ShowSkip(this);

    WeakRunnable sendVideoPlayedReport = new SendVideoPlayedReport(this);

    public static abstract class Delegate {
        /**
         * Returns the current playback position in milliseconds.
         *
         * @return The current playback position in milliseconds.
         */
        public abstract long getCurrentPosition();

        /**
         * Returns the duration of the track in milliseconds.
         *
         * @return The duration of the track in milliseconds.
         */
        public abstract long getDuration();

        public abstract void pauseVideo();

        public abstract void playVideo();

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
        delegate.playVideo();
        play();
    }

    @Override
    public void landingActionClicked() {
        landingClicked();
        dismissLanding();
        performAction(true);
    }

    public SubtitleAdView(Context context) {
        super(context);
        init();
    }

    public SubtitleAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SubtitleAdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ResanaLog.v(TAG, "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ResanaLog.v(TAG, "onDetachedFromWindow");
        removeCallbacks(endWait);
        removeCallbacks(showSkip);
    }

    /**
     * Setup ResanaInternal with needed data to make a connection and receive proper ads.
     *
     * @param resana   resana instance that you have created
     * @param delegate Controller object that provides data about playing video (Nullable)
     */
    public void setup(Activity activity, final Resana resana,
                      Delegate delegate) {
        if (activity == null)
            throw new IllegalArgumentException("Activity object can't be null");
        if (resana == null)
            throw new IllegalArgumentException("Resana object can't be null");
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal == null)
            throw new RuntimeException("resana object is released!");
        this.resana = resana;
        this.delegate = delegate;
        this.activity = activity;
        post(new Runnable() {
            @Override
            public void run() {
                resanaInternal.attachSubtitleViewer(SubtitleAdView.this);
            }
        });
        removeCallbacks(sendVideoPlayedReport);
        postDelayed(sendVideoPlayedReport, 10 * 1000);
    }

    /**
     * Start showing ads.
     */
    public void play() {
        ResanaLog.i(TAG, "Play");
        ResanaLog.v(TAG, "play called in state: " + state);
        playing = true;
        if (state == State.ShowingText) {
            if (textAnim != null) {
                textAnim.resume();
            } else {
                //report anomaly
            }
        } else if (state == State.ShowingImage) {
            if (imageAnim != null) {
                imageAnim.resume();
            } else {
                //report anomaly
            }
        } else if (state == State.Idle) {
            if (hasShownAdBefore)
                startShowingAdIfPossible();
            else {
                startWaiting(getWaitTime(true));
                hasShownAdBefore = true;
            }
        }
    }

    /**
     * Stop showing ads.
     * Pause current ad.
     */
    public void pause() {
        ResanaLog.i(TAG, "Pause");
        playing = false;
        if (state == State.ShowingText) {
            if (textAnim != null) {
                textAnim.pause();
            } else {
                //report anomaly
            }
        } else if (state == State.ShowingImage) {
            if (imageAnim != null) {
                imageAnim.pause();
            } else {
                //report anomaly
            }
        }
    }

    public void stop() {
        ResanaLog.i(TAG, "Stop");
        if (state == State.Wait) {
            removeCallbacks(endWait);
            lastWaitStartTime = lastWaitAmount = -1;
        } else if (state == State.ShowingImage || state == State.ShowingText) {
            removeCallbacks(showSkip);
            endCurrentAd();
        }
        playing = false;
        hasShownAdBefore = false;
        state = State.Idle;
        removeCallbacks(sendVideoPlayedReport);
    }

    public void videoSizeChanged() {
        updateSizes();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        videoSizeChanged();
        ResanaLog.v(TAG, "onConfigurationChanged");
    }

    /**
     * Set if repetitive advertisement are allowed to be shown in current movie.
     *
     * @param skipRepetitiveAds
     */
    public void setSkipRepetitiveAds(boolean skipRepetitiveAds) {
        this.skipRepetitiveAds = skipRepetitiveAds;
    }

    /**
     * Are repetitive advertisements allowed to be shown in current movie?
     *
     * @return true if repetitive advertisements are allowed to be shown in current movie, false otherwise
     */
    public boolean skipRepetitiveAds() {
        return skipRepetitiveAds;
    }

    private void init() {
        int space5dp = AdViewUtil.dpToPx(getContext(), 5);
        int space2dp = AdViewUtil.dpToPx(getContext(), 2);
        int space1dp = AdViewUtil.dpToPx(getContext(), 1);

        image = new ImageView(getContext());
        image.setVisibility(INVISIBLE);
        image.setOnClickListener(SubtitleAdView.this);
        addView(image, getChildCount(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

        container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setHorizontalGravity(Gravity.RIGHT);
        container.setVerticalGravity(Gravity.BOTTOM);
        container.setVisibility(INVISIBLE);

        logo = new ImageView(getContext());
        logo.setOnClickListener(this);
        logo.setAdjustViewBounds(true);
        logo.setPadding(space2dp, space2dp, space2dp, space2dp);
        logo.setVisibility(INVISIBLE);
        container.addView(logo);

        ribbon = new HorizontalScrollView(getContext());
        ribbon.setHorizontalScrollBarEnabled(false);
        ribbon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    SubtitleAdView.this.onClick(v);
                }
                return false;
            }
        });

        text = new TextView(getContext());
        text.setPadding(space5dp, space2dp, space5dp, space2dp);
        AdViewUtil.setTypeFace(getContext(), text);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine();
        text.setClickable(false);
        ribbon.addView(text, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        container.addView(ribbon, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(container, getChildCount(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        skip = new FrameLayout(getContext());
        skip.setOnClickListener(this);
        skipText = new TextView(getContext());
        skipText.setText("Skip");
        skipText.setTextColor(Color.WHITE);
        skipText.setBackgroundColor(Color.GRAY);
        skipText.setPadding(space5dp, space1dp, space5dp, space1dp);
        skip.setVisibility(INVISIBLE);
        skip.addView(skipText, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        addView(skip, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT));

        invalidate();
        requestLayout();

        updateSizes();
    }

    private void showSkip(boolean animate) {
        ResanaLog.v(TAG, "showSkip: ");
        if (isNotSkippable())
            return;
        if (animate) {
            TranslateAnimation anim = new AdViewCustomTranslateAnimation(-skip.getWidth(), 0, 0, 0);
            anim.setDuration(500);
            anim.setFillAfter(true);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    skip.setVisibility(VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            skip.startAnimation(anim);
        } else
            skip.setVisibility(VISIBLE);
    }

    private boolean isNotSkippable() {
        try {
            return (currAd.data.flags & Ad.Flags.SUBTITLE_NOT_SKIPPABLE) != 0;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void updateSizes() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SMALL);
            skipText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SMALL - 1);
        } else {
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_BIG);
            skipText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_BIG - 1);
        }
        AdViewUtil.runOnceWhenViewWasDrawn(ribbon, new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp1 = image.getLayoutParams();
                lp1.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp1.height = ribbon.getHeight();
                image.setLayoutParams(lp1);

                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(logo.getLayoutParams());
                lp2.height = ribbon.getHeight();
                lp2.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                logo.setLayoutParams(lp2);

                ViewGroup.LayoutParams lp3 = skip.getLayoutParams();
                lp3.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp3.height = ribbon.getHeight();
                skip.setLayoutParams(lp3);
            }
        });
        ribbon.invalidate();
        ribbon.requestLayout();
    }


    @Override
    public void onClick(View v) {
        if (v.equals(skip))
            skipCurrentAd();
        else {
            if (!allowClick())
                return;
            adClicked();
            if (currAd.hasLanding())
                openLanding();
            else
                performAction(false);
        }
    }

    private boolean allowClick() {
        return resana != null && resana.instance != null &&
                (currAd.hasLanding()
                        || currAd.data.intent != null
                        || currAd.data.link != null);
    }

    private void openLanding() {
        if (landIsOpen)
            return;
        landIsOpen = true;
        pause();
        delegate.pauseVideo();
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
        }
    }

    private void adClicked() {
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            resanaInternal.onSubtitleClicked(currAd);
    }

    private void landingClicked() {
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            resanaInternal.onSubtitleLandingClicked(currAd);
    }

    private void performAction(boolean fromLanding) {
        ResanaLog.d(TAG, "performAction() fromLanding = [" + fromLanding + "]");
        final Intent intentAction = AdViewUtil.getAdActionIntent(currAd);
        if (intentAction != null) {
            handleIntentAction(intentAction);
            return;
        }
        if (fromLanding) {
            delegate.playVideo();
            play();
        }
    }

    private void handleIntentAction(Intent intent) {
        delegate.closeVideo();
        try {
            getContext().startActivity(intent);
        } catch (Exception ignored) {
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
        landIsOpen = false;
    }

    protected void startShowingAdIfPossible() {
        ResanaLog.v(TAG, "startShowingAdIfPossible. isPlaying:" + playing + " state:" + state);
        if (playing && state == State.Idle) {
            if (!CoolDownHelper.shouldShowSubtitle(getContext())) {
                ResanaLog.v(TAG, "cool down showing subtitle");
                startWaiting(getWaitTime(false));
                return;
            }
            Ad ad = getNextProperAd();
            if (ad != null) {
                currAd = ad;
                ResanaLog.v(TAG, "going to show ad...");
                downloadAndSetImageBitmap();
                showText();
                postDelayed(showSkip, WAIT_TIME_BEFORE_SKIP);
                renderedAds.add(currAd.getOrder());
            } else {
                ResanaLog.v(TAG, "no proper ad available to show");
            }
        }
    }

    private Ad getNextProperAd() {
        long remainedTime = Long.MAX_VALUE;
        if (delegate != null) {
            try {
                remainedTime = delegate.getDuration() - delegate.getCurrentPosition();
            } catch (Exception e) {
                ResanaLog.w(TAG, "Exception while trying to get video duration and currentPosition using provided AdViewController object!", e);
            }
        }
        ResanaLog.v(TAG, "remainedTime=" + remainedTime);
        ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            return resanaInternal.getSubtitleAd(skipRepetitiveAds ? renderedAds : null, remainedTime);
        return null;
    }

    private void showText() {
        state = State.ShowingText;
        ribbon.setBackgroundColor(currAd.getBackgroundColor());
        text.setText(currAd.getSubtitleText());
        text.setTextColor(currAd.getSubtitleTextColor());
        container.setVisibility(VISIBLE);
        AdViewUtil.runOnceWhenViewWasDrawn(text, new Runnable() {
            @Override
            public void run() {
                prepareLogo();
                animateText();
            }
        });
        invalidate();
        requestLayout();
        text.invalidate();
        text.requestLayout();
    }

    private void prepareLogo() {
        if (logoBitmap == null) {
            logo.setVisibility(INVISIBLE);
            downloadAndShowLogo();
        } else {
            logo.setVisibility(VISIBLE);
            logo.setImageBitmap(logoBitmap);
        }
    }

    private void animateText() {
        if (currAd == null) {
            //todo I dont know why this keeps happening.
            return;
        }
        ResanaLog.v(TAG, "animateText: textAnimCurrentTime: " + currAd.textAnimCurrentPlayTime);
        textAnim = new AdViewCustomTranslateAnimation(-text.getWidth(), ribbon.getRight(), 0, 0);
        textAnim.setDuration(AdViewUtil.getNeededTimeForAnimatingText(text));
        textAnim.setFillAfter(true);
        textAnim.setInterpolator(new LinearInterpolator());
        textAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (currAd != null)
                    showImage();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        textAnim.setStartPosition(currAd.textAnimCurrentPlayTime);
        text.startAnimation(textAnim);
        if (!playing) textAnim.pause();
    }

    private void showImage() {
        ResanaLog.v(TAG, "ShowImage");
        state = State.ShowingImage;
        image.setImageBitmap(imgBitmap);
        image.setBackgroundColor(currAd.getBackgroundColor());
        hideText();
        image.setVisibility(VISIBLE);
        imageAnim = new AdViewCustomTranslateAnimation(0, 0, 0, 0);
        if (imgBitmap != null)
            imageAnim.setDuration(5000);
        else
            imageAnim.setDuration(500);
        imageAnim.setStartPosition(currAd.imageShowingTimeElapsed);
        imageAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (currAd != null) {
                    endCurrentAd();
                    startWaiting(getWaitTime(false));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        image.startAnimation(imageAnim);
        if (!playing) imageAnim.pause();
    }

    private void hideText() {
        AlphaAnimation anim = new AlphaAnimation(1, 0);
        anim.setDuration(500);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                container.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        container.startAnimation(anim);
    }

    private void skipCurrentAd() {
        endCurrentAd();
        startWaiting(getWaitTime(false));
    }

    private void endCurrentAd() {
        ResanaLog.v(TAG, "EndCurrentAd");
        state = State.Idle;
        final ResanaInternal resanaInternal = resana.instance;
        if (currAd != null && resanaInternal != null)
            resanaInternal.releaseSubtitle(currAd);
        currAd = null;
        image.setImageDrawable(null);
        logo.setImageDrawable(null);
        if (imgBitmap != null) {
            imgBitmap.recycle();
            imgBitmap = null;
        }
        if (logoBitmap != null) {
            logoBitmap.recycle();
            logoBitmap = null;
        }
        text.clearAnimation();
        container.clearAnimation();
        image.clearAnimation();
        logo.clearAnimation();
        skip.clearAnimation();
        image.setVisibility(INVISIBLE);
        container.setVisibility(INVISIBLE);
        skip.setVisibility(INVISIBLE);
    }

    private void startWaiting(Integer waitTime) {
        ResanaLog.v(TAG, "StartWaiting for " + waitTime + "ms");
        state = State.Wait;
        lastWaitStartTime = System.currentTimeMillis();
        lastWaitAmount = waitTime;
        postDelayed(endWait, waitTime);
    }

    private void endWaiting() {
        ResanaLog.v(TAG, "EndWaiting");
        state = State.Idle;
        lastWaitStartTime = lastWaitAmount = -1;
        startShowingAdIfPossible();
    }

    private int getWaitTime(boolean first) {
        final ResanaInternal resanaInternal = resana.instance;
        if (resanaInternal != null)
            return resanaInternal.getSubtitleWaitTime(first);
        return CoolDownHelper.getDefaultSubtitleWaitTime(first);
    }

    private void downloadAndShowLogo() {
        if (currAd == null) {
            //report anomaly. (this should not happen since fixing Nazdika#6797)
            return;
        }
        if (currAd.getSubtitleLogoUrl() != null)
            FileManager.getInstance(getContext()).loadBitmapFromUrl(currAd.getSubtitleLogoUrl(), new LogoBitmapLoadedDelegate(this));
    }

    private void logoBitmapLoaded(boolean success, Object... bitmaps) {
        if (!success)
            return;
        logoBitmap = (Bitmap) bitmaps[0];
        logo.setImageBitmap(logoBitmap);
        AlphaAnimation anim = new AlphaAnimation(0, 1);
        anim.setDuration(500);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                logo.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        logo.startAnimation(anim);
    }

    private void downloadAndSetImageBitmap() {
        if (currAd.getImgUrl() != null)
            FileManager.getInstance(getContext()).loadBitmapFromUrl(currAd.getImgUrl(), new ImageBitmapLoadedDelegate(this));
    }

    private void imageBitmapLoaded(boolean success, Object... bitmaps) {
        if (!success)
            return;
        imgBitmap = (Bitmap) bitmaps[0];
        image.setImageBitmap(imgBitmap);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        ResanaLog.v(TAG, "onSaveInstanceState: ");
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.playing = this.playing;
        savedState.hasShownAdBefore = this.hasShownAdBefore;
        savedState.state = this.state;
        savedState.currentAd = this.currAd;
        savedState.lastWaitStartTime = lastWaitStartTime;
        savedState.lastWaitAmount = lastWaitAmount;
        savedState.logoBitmap = this.logoBitmap;
        savedState.imgBitmap = this.imgBitmap;
        if (currAd != null) {
            currAd.textAnimCurrentPlayTime =
                    (state == State.ShowingText)
                            ? textAnim.getCurrentPlayTime()
                            : 0;
            currAd.imageShowingTimeElapsed =
                    (state == State.ShowingImage)
                            ? imageAnim.getCurrentPlayTime()
                            : 0;
        }
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        ResanaLog.v(TAG, "onRestoreInstanceState");
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.playing = savedState.playing;
        this.hasShownAdBefore = savedState.hasShownAdBefore;
        this.state = savedState.state;
        this.currAd = savedState.currentAd;
        this.lastWaitStartTime = savedState.lastWaitStartTime;
        this.lastWaitAmount = savedState.lastWaitAmount;
        this.logoBitmap = savedState.logoBitmap;
        this.imgBitmap = savedState.imgBitmap;
        restoreFromSavedState();
    }

    private void restoreFromSavedState() {
        ResanaLog.v(TAG, "restoreFromSavedState:    State == " + state + "     playing == " + playing);
        switch (state) {
            case Idle:
                break;
            case ShowingText:
                showText();
                long t = WAIT_TIME_BEFORE_SKIP - currAd.textAnimCurrentPlayTime;
                if (t > 0)
                    postDelayed(showSkip, t);
                else
                    showSkip(false);
                if (imgBitmap == null)
                    downloadAndSetImageBitmap();
                ResanaLog.v(TAG, "restoreFromSavedState: will showText");
                break;
            case ShowingImage:
                ResanaLog.v(TAG, "restoreFromSavedState: will showImage");
                showSkip(false);
                showImage();
                break;
            case Wait:
                if (lastWaitAmount != -1 && lastWaitStartTime != -1) {
                    long waitTime = lastWaitAmount - (System.currentTimeMillis() - lastWaitStartTime);
                    if (waitTime > 0)
                        startWaiting((int) waitTime);
                    else
                        endWaiting();
                } else {
                    //should not come here
                    endWaiting();
                }
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean playing;
        boolean hasShownAdBefore;
        State state;
        Ad currentAd;
        long lastWaitStartTime;
        int lastWaitAmount;
        Bitmap logoBitmap;
        Bitmap imgBitmap;

        public SavedState(Parcel source) {
            super(source);
            playing = source.readByte() != 0;
            hasShownAdBefore = source.readByte() != 0;
            state = State.values()[source.readInt()];
            currentAd = source.readParcelable(Ad.class.getClassLoader());
            lastWaitStartTime = source.readLong();
            lastWaitAmount = source.readInt();
            try {
                logoBitmap = Bitmap.CREATOR.createFromParcel(source);
                imgBitmap = Bitmap.CREATOR.createFromParcel(source);
            } catch (Exception e) {
                //report anomaly
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (playing ? 1 : 0));
            out.writeByte((byte) (hasShownAdBefore ? 1 : 0));
            out.writeInt(state.ordinal());
            out.writeParcelable(currentAd, flags);
            out.writeLong(lastWaitStartTime);
            out.writeInt(lastWaitAmount);
            out.writeParcelable(logoBitmap, flags);
            out.writeParcelable(imgBitmap, flags);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private static class EndWait extends WeakRunnable<SubtitleAdView> {

        EndWait(SubtitleAdView ref) {
            super(ref);
        }

        @Override
        void run(SubtitleAdView object) {
            object.endWaiting();
        }
    }

    private static class ShowSkip extends WeakRunnable<SubtitleAdView> {

        ShowSkip(SubtitleAdView ref) {
            super(ref);
        }

        @Override
        void run(SubtitleAdView object) {
            object.showSkip(true);
        }
    }

    private static class SendVideoPlayedReport extends WeakRunnable<SubtitleAdView> {

        SendVideoPlayedReport(SubtitleAdView ref) {
            super(ref);
        }

        @Override
        void run(SubtitleAdView object) {
            Long duration = null;
            String tchData = null;
            if (object.delegate != null)
                try {
                    duration = object.delegate.getDuration();
                    tchData = object.delegate.getTelegramChatData();
                } catch (Exception e) {
                    ResanaLog.w(TAG, "Exception while trying to get video durationusing provided AdViewController object!", e);
                }
        }
    }

    private static class LogoBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<SubtitleAdView> viewRef;

        LogoBitmapLoadedDelegate(SubtitleAdView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SubtitleAdView view = viewRef.get();
            if (view != null)
                view.logoBitmapLoaded(success, args);
        }
    }

    private static class ImageBitmapLoadedDelegate extends FileManager.Delegate {
        WeakReference<SubtitleAdView> viewRef;

        ImageBitmapLoadedDelegate(SubtitleAdView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        void onFinish(boolean success, Object... args) {
            final SubtitleAdView view = viewRef.get();
            if (view != null)
                view.imageBitmapLoaded(success, args);
        }
    }
}