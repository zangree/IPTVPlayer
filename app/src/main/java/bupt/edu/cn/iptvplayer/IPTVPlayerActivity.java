/*
 * Copyright (C) 2013 yixia.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bupt.edu.cn.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

import java.io.IOException;

import bupt.edu.cn.utils.Global;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;

public class IPTVPlayerActivity extends Activity implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {

    private static final String TAG = IPTVPlayerActivity.class.getName();
    private int videoWidth;
    private int videoHeight;
    private long currentPosition = 0;
    private View controlView;
    private SeekBar seekPosition;
    private MediaPlayer mediaPlayer;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Intent intent;
    private boolean isVideoSizeKnown = false;
    private boolean isVideoReadyToBePlayed = false;
    private boolean visible = false;
    private Handler handler = new Handler();
    private Runnable r = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "setProgress");
            if (getIntent().getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO) {
                seekPosition.setProgress((int) mediaPlayer.getCurrentPosition() / 1000);
                seekPosition.postDelayed(r, 1000);
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!LibsChecker.checkVitamioLibs(this))
            return;
        setContentView(R.layout.activity_iptv_player);
        controlView = findViewById(R.id.control);
        seekPosition = (SeekBar) findViewById(R.id.seekPosition);
        seekPosition.setOnSeekBarChangeListener(new SeekCurrentPosition());
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceView.setOnClickListener(new SurfaceViewClick());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        intent = getIntent();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentPosition = mediaPlayer.getCurrentPosition();
        startVideoPlayback();
    }

    private void playVideo(String path) {
        doCleanUp();
        try {
            Log.v(TAG, path);
            // Create a new media player and set the listeners
            mediaPlayer = new MediaPlayer(this);
            mediaPlayer.setDataSource(path);
            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            handler.post(r);
        } catch (IOException e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
    }

    public void stop(View v) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        isVideoSizeKnown = true;
        videoWidth = width;
        videoHeight = height;
        if (isVideoReadyToBePlayed && isVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        isVideoReadyToBePlayed = true;
        if (isVideoReadyToBePlayed && isVideoSizeKnown) {
            startVideoPlayback();
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");

    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        playVideo(intent.getStringExtra(Global.EXTRA_LINK));

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        Log.d(TAG,"releaseMediaPlayer called");
        if (mediaPlayer != null) {
            if (intent.getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO) {
                seekPosition.removeCallbacks(r);
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void doCleanUp() {
        videoWidth = 0;
        videoHeight = 0;
        isVideoReadyToBePlayed = false;
        isVideoSizeKnown = false;
    }

    private void startVideoPlayback() {
        seekPosition.setMax((int) mediaPlayer.getDuration() / 1000);
        Log.v(TAG, "startVideoPlayback");
        WindowManager windowManager = getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        surfaceHolder.setFixedSize(metrics.widthPixels, videoHeight * metrics.widthPixels / videoWidth); //保持长宽比使其全屏
        if (intent.getIntExtra(Global.MEDIA, 0) == Global.LOCAL_VIDEO)
            if (currentPosition > 0) {
                Log.d(TAG, "continue");
                mediaPlayer.seekTo(currentPosition);
            }
        mediaPlayer.start();
    }


    private class SurfaceViewClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (visible) {
                controlView.setVisibility(View.INVISIBLE);
                visible = false;
            } else {
                controlView.setVisibility(View.VISIBLE);
                visible = true;
            }
        }
    }

    private class SeekCurrentPosition implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mediaPlayer.seekTo(progress * 1000);
                mediaPlayer.start();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
