package io.resana;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

public class NativeLandingView extends Dialog {

    private Context context;
    private NativeAd nativeAd;
    private LandingView landingView;


    private LandingView.Delegate delegate;

    NativeLandingView(Context context, NativeAd nativeAd) {
        super(context);
        this.context = context;
        this.nativeAd = nativeAd;
        init();
    }

    private void init() {
        if (nativeAd != null) {
            landingView = new LandingView(context);
            landingView.setDelegate(delegate);
            landingView.setData(nativeAd);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } catch (Exception ignored) {
        }

        setCancelable(true);
        setContentView(landingView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setDelegate(LandingView.Delegate delegate) {
        this.delegate = delegate;
        landingView.setDelegate(delegate);
    }
}
