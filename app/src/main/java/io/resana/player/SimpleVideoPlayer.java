package io.resana.player;

import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import io.resana.Resana;
import io.resana.SplashAdView;
import io.resana.SubtitleAdView;
import io.resana.URAd;

public class SimpleVideoPlayer extends AppCompatActivity {
    private static final String TAG = "SimpleVideoPlayer";
    CustomVideoView videoView;
    Resana resana;
    SubtitleAdView subtitleAd;
    SplashAdView splashAdView;
    ImageButton playPauseBtn;
    private int startFrom;

    boolean waitingForSplashToFinish;

    Handler handler = new Handler();
    Runnable fadePlayPause = new Runnable() {
        @Override
        public void run() {
            playPauseBtn.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_video_player);
        videoView = (CustomVideoView) findViewById(R.id.video);
        // Receive path to play from intent

//        Intent intent = getIntent();
//        String path = intent.getStringExtra(MainActivity.VIDEO_PATH_KEY);
//        if (path == null)
//            path = intent.getData().getPath();

        String path = "android.resource://" + getPackageName() + "/" /* + R.raw.rock*/;

        videoView.setVideoURI(Uri.parse(path));
        playPauseBtn = (ImageButton) findViewById(R.id.playPauseBtn);
        findViewById(R.id.root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayPauseBtn();
            }
        });
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying())
                    videoView.pause();
                else
                    videoView.start();
            }
        });
        videoView.setPlayPauseListener(new CustomVideoView.PlayPauseListener() {
            @Override
            public void onPlay() {
                Log.d(TAG, "onPlay: ");
                if (subtitleAd != null)
                    subtitleAd.play();
                updateplayPauseBtn(false);
            }

            @Override
            public void onPause() {
                Log.d(TAG, "onPause: ");
                if (subtitleAd != null)
                    subtitleAd.pause();
                updateplayPauseBtn(true);
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                subtitleAd.stop();
                updateplayPauseBtn(true);
//                splashAdView.setShowProgress(false);
                splashAdView.showSplash();
            }
        });

        subtitleAd = (SubtitleAdView) findViewById(R.id.ad);
        resana = Resana.create(this, null, Resana.LOG_LEVEL_VERBOSE);
        subtitleAd.setup(this, resana, new SubtitleAdView.Delegate() {
            @Override
            public long getCurrentPosition() {
                return videoView.getCurrentPosition();
            }

            @Override
            public long getDuration() {
                return videoView.getDuration();
            }

            @Override
            public void pauseVideo() {
                videoView.pause();
            }

            @Override
            public void playVideo() {
                videoView.start();
            }

            @Override
            public void closeVideo() {
                finish();
            }
        });

        splashAdView = (SplashAdView) findViewById(R.id.splashAd);
        splashAdView.setup(this, resana, new SplashAdView.Delegate() {
            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish: ");
                waitingForSplashToFinish = false;
                if (videoView != null)
                    videoView.start();
            }

            @Override
            public void onFailure(String reason) {
                Log.d(TAG, "onFailure: reason=" + reason);
                waitingForSplashToFinish = false;
                if (videoView != null)
                    videoView.start();
            }

            @Override
            public void closeVideo() {
                finish();
            }
        });
        waitingForSplashToFinish = true;
        splashAdView.showSplash();
        final URAd urAd = resana.getURAd(null);
        if (urAd != null) {
            ((TextView) findViewById(R.id.urAdText)).setText("text: \"" + urAd.getText() + "\"" + " #visuals:" + urAd.getVisuals().size());
            final TextView image = (TextView) findViewById(R.id.urAdImage);
            final URAd.Visual visual = urAd.getVisuals().get(0);
            if (visual.getWidth() != null) {
                float scale = (float) getWindowManager().getDefaultDisplay().getWidth() / visual.getWidth();
                int width = getWindowManager().getDefaultDisplay().getWidth();
                image.setWidth(width);
                image.setHeight(((int) (visual.getHeight() * scale)));
                image.setText("visual: " + visual.getMimeType() + " " + visual.getUrl() + " " + visual.getUrl() + "*" + visual.getUrl());
            }
            resana.onURAdRendered(urAd.getSecretKey());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        if (resana != null)
            resana.release();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: startFrom=" + startFrom);
        super.onResume();
        if (startFrom > 0) {
            videoView.seekTo(startFrom);
        }
        if (!waitingForSplashToFinish)
            videoView.start();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        startFrom = videoView.getCurrentPosition();
        videoView.pause();
    }

    @Override
    public void onBackPressed() {
        if (splashAdView != null && splashAdView.isShowingAd())
            return;
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged: ");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: ");
        super.onSaveInstanceState(outState);
        outState.putInt("currentPosition", startFrom);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        startFrom = savedInstanceState.getInt("currentPosition");
        Log.d(TAG, "onRestoreInstanceState: " + startFrom);
    }

    private void updateplayPauseBtn(boolean play) {
        if (play)
            playPauseBtn.setImageResource(R.drawable.play);
        else
            playPauseBtn.setImageResource(R.drawable.pause);
        showPlayPauseBtn();
    }

    private void showPlayPauseBtn() {
        playPauseBtn.setVisibility(View.VISIBLE);
        handler.removeCallbacks(fadePlayPause);
        handler.postDelayed(fadePlayPause, 2000);
    }
}
