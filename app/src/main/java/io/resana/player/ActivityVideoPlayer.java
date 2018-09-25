package io.resana.player;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.MediaController;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.resana.Resana;
import io.resana.SubtitleAdView;

public class ActivityVideoPlayer extends Activity implements IVLCVout.Callback, MediaController.MediaPlayerControl {
    public final static String TAG = "ActivityVideoPlayer";
    private String mFilePath;

    // display surface
    @Bind(R.id.surface)
    SurfaceView mSurface;
    @Bind(R.id.adView)
    SubtitleAdView adView;
    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;

    MediaController mediaController;
    private Resana resana;

    /*************
     * Activity
     *************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        ButterKnife.bind(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Receive path to play from intent
        Intent intent = getIntent();
        mFilePath = intent.getStringExtra(MainActivity.VIDEO_PATH_KEY);
        if (mFilePath == null)
            mFilePath = intent.getData().getPath();

        Log.d(TAG, "Playing back " + mFilePath);

        holder = mSurface.getHolder();
//        holder.addCallback(this);
//        holder.setFormat(ImageFormat.YV12);
        holder.setFormat(PixelFormat.RGBA_8888);
        holder.setKeepScreenOn(true);

        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(findViewById(R.id.root));
//        adView.setup(new String[]{"category"}, new AdViewController() {
//        adView.setup(ResanaInternal.create(this, null), new AdViewController() {
//            @Override
//            public int getCurrentPosition() {
//                return ActivityVideoPlayer.this.getCurrentPosition();
//            }
//
//            @Override
//            public int getDuration() {
//                return ActivityVideoPlayer.this.getDuration();
//            }
//        });
        resana = Resana.create(this, null, Resana.LOG_LEVEL_VERBOSE);
        adView.setup(this, resana, null);
//        adView.setSkipRepetitiveAds(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mediaController.show();
        return super.onTouchEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        adView.pause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        releasePlayer();
        resana.release();
    }

    /*************
     * Surface
     *************/
    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if (holder == null || mSurface == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /*************
     * Player
     *************/

    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
//                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
//                        0);
//                toast.show();
            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
//            options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            options.add("--androidwindow-chroma");
//            options.add("YV12");
            options.add("RV32");

            libvlc = new LibVLC(options);
//            libvlc.setOnHardwareAccelerationError(this);
            holder.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, media);
            m.setHWDecoderEnabled(false, false);
            mMediaPlayer.setMedia(m);
//            mMediaPlayer.play();
            start();
        } catch (Exception e) {
            libvlc = null;
            mMediaPlayer = null;
            e.printStackTrace();
//            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this cleaner
    private void releasePlayer() {
        Log.d(TAG, "releasePlayer: ");
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();

        libvlc.release();
        libvlc = null;
        mMediaPlayer = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /*************
     * Events
     *************/

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    /**************
     * IVLCVout.Callback
     ***************/
    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
//        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    //media player control
    @Override
    public void start() {
        if (mMediaPlayer == null)
            createPlayer(mFilePath);
        else {
            mMediaPlayer.play();
            adView.play();
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null)
            mMediaPlayer.pause();
        adView.pause();
    }

    @Override
    public int getDuration() {
        if (mMediaPlayer != null)
            return (int) mMediaPlayer.getLength();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mMediaPlayer != null)
            return (int) (mMediaPlayer.getPosition() * mMediaPlayer.getLength());
        else
            return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (mMediaPlayer != null)
            mMediaPlayer.setPosition((float) pos / getDuration());
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 100;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return mMediaPlayer == null || mMediaPlayer.isSeekable();
    }

    @Override
    public boolean canSeekForward() {
        return mMediaPlayer == null || mMediaPlayer.isSeekable();
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<ActivityVideoPlayer> mOwner;

        public MyPlayerListener(ActivityVideoPlayer owner) {
            mOwner = new WeakReference<ActivityVideoPlayer>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
//            Log.d(TAG, "onEvent: " + event.type);
//            ActivityVideoPlayer activity = mOwner.get();
//            if (activity != null)
//                switch (event.type) {
//                    case MediaPlayer.Event.EndReached:
//                        activity.releasePlayer();
//                        activity.adView.pause();
//                        break;
//                    case MediaPlayer.Event.Playing:
//                        activity.adView.play();
//                        break;
//                    case MediaPlayer.Event.Paused:
//                    case MediaPlayer.Event.Stopped:
//                        activity.adView.pause();
//                        break;
//                    default:
//                        break;
//                }
        }
    }

}

//right
//05-31 11:51:41.361 23897-6107/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea68d78] core window: looking for vout window module matching "any": 1 candidates
//        05-31 11:51:41.361 23897-6107/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea68d78] core window: no vout window modules matched
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea15db8] core video output: Opening vout display wrapper
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: looking for vout display module matching "any": 3 candidates
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer W/VLC: [0000007f5b47beb8] android_window vout display: Could not initialize NativeWindow Priv API.
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: using opaque
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer E/VLC: [0000007f5b47beb8] android_window vout display: can't get Subtitles Surface
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: VoutDisplayEvent 'fullscreen' 1
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: using vout display module "android_window"
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: PoolAlloc: request 26 frames
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: PoolAlloc: got 31 frames
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer E/VLC: [0000007f5b47beb8] core vout display: Failed to change zoom
//        05-31 11:51:41.361 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: change source crop/aspect

//        05-31 11:51:41.441 23897-6119/imani.hojjat.oddrunplayer D/VLC: [0000007f3eaaf738] core generic: reusing provided vout
//        05-31 11:51:41.441 23897-6108/imani.hojjat.oddrunplayer E/VLC: [0000007f5b47beb8] core vout display: Failed to change zoom
//        05-31 11:51:41.441 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: change source crop/aspect
//        05-31 11:51:41.491 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: SOURCE  sz 640x368, of (0,0), vsz 640x368, 4cc ANOP, sar 1:1, msk r0x0 g0x0 b0x0
//        05-31 11:51:41.491 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: CROPPED sz 640x368, of (0,0), vsz 640x368, 4cc ANOP, sar 1:1, msk r0x0 g0x0 b0x0
//        05-31 11:51:41.491 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] android_window vout display: change source crop/aspect
//        05-31 11:51:42.371 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: auto hiding mouse cursor


//        05-31 11:52:03.411 23897-6107/imani.hojjat.oddrunplayer D/VLC: [0000007f3eaaf738] core generic: saving a free vout
//        05-31 11:52:03.411 23897-6107/imani.hojjat.oddrunplayer D/VLC: [0000007f3eaaf738] core generic: reusing provided vout
//        05-31 11:52:03.421 23897-23897/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea15db8] core video output: destroying useless vout
//        05-31 11:52:03.421 23897-6108/imani.hojjat.oddrunplayer D/VLC: [0000007f5b47beb8] core vout display: removing module "android_window"


//problematic
//05-31 11:53:08.841 23897-8990/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea685f8] core window: looking for vout window module matching "any": 1 candidates
//        05-31 11:53:08.841 23897-8990/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea685f8] core window: no vout window modules matched
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea158b8] core video output: Opening vout display wrapper
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] core vout display: looking for vout display module matching "any": 3 candidates
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer W/VLC: [0000007f3e9b36b8] android_window vout display: Could not initialize NativeWindow Priv API.
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] android_window vout display: using opaque
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer E/VLC: [0000007f3e9b36b8] android_window vout display: can't get Subtitles Surface
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] core vout display: VoutDisplayEvent 'fullscreen' 1
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] core vout display: using vout display module "android_window"
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] android_window vout display: PoolAlloc: request 20 frames
//        05-31 11:53:08.841 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] android_window vout display: PoolAlloc: got 31 frames
//        05-31 11:53:08.851 23897-8991/imani.hojjat.oddrunplayer E/VLC: [0000007f3e9b36b8] core vout display: Failed to change zoom
//        05-31 11:53:08.851 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] android_window vout display: change source crop/aspect

//        05-31 11:53:09.851 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] core vout display: auto hiding mouse cursor

//        05-31 11:53:16.711 23897-8990/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9f2138] core generic: saving a free vout
//        05-31 11:53:16.711 23897-8990/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9f2138] core generic: reusing provided vout
//        05-31 11:53:16.721 23897-23897/imani.hojjat.oddrunplayer D/VLC: [0000007f3ea158b8] core video output: destroying useless vout
//        05-31 11:53:16.721 23897-8991/imani.hojjat.oddrunplayer D/VLC: [0000007f3e9b36b8] core vout display: removing module "android_window"