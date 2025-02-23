/*

* Copyright (C) 2015 Author <dictfb#gmail.com>

*

* Licensed under the Apache License, Version 2.0 (the "License");

* you may not use this file except in compliance with the License.

* You may obtain a copy of the License at

*

* http://www.apache.org/licenses/LICENSE-2.0

*

* Unless required by applicable law or agreed to in writing, software

* distributed under the License is distributed on an "AS IS" BASIS,

* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

* See the License for the specific language governing permissions and

* limitations under the License.

*/

package matrixcri.in.videoview;

import android.content.Context;

import android.content.res.TypedArray;

import android.os.Handler;

import android.os.Message;

import android.util.AttributeSet;

import android.view.KeyEvent;

import android.view.LayoutInflater;

import android.view.MotionEvent;

import android.view.View;

import android.view.ViewGroup;

import android.widget.FrameLayout;

import android.widget.ImageButton;

import android.widget.ProgressBar;

import android.widget.SeekBar;

import android.widget.SeekBar.OnSeekBarChangeListener;

import android.widget.TextView;

import java.util.Formatter;

import java.util.Locale;

public class MatrixMediaController extends FrameLayout {

    private MatrixMediaController.MediaPlayerControl mPlayer;

    private Context mContext;

    private ProgressBar mProgress;

    private TextView mEndTime, mCurrentTime;

    private TextView mTitle;

    private boolean mShowing = true;

    private boolean mDragging;

    private boolean mScalable = false;

    private boolean mIsFullScreen = false;

    private static final int sDefaultTimeout = 3000;

    private static final int STATE_PLAYING = 1;

    private static final int STATE_PAUSE = 2;

    private static final int STATE_LOADING = 3;

    private static final int STATE_ERROR = 4;

    private static final int STATE_COMPLETE = 5;

    private int mState = STATE_LOADING;

    private static final int FADE_OUT = 1;

    private static final int SHOW_PROGRESS = 2;

    private static final int SHOW_LOADING = 3;

    private static final int HIDE_LOADING = 4;

    private static final int SHOW_ERROR = 5;

    private static final int HIDE_ERROR = 6;

    private static final int SHOW_COMPLETE = 7;

    private static final int HIDE_COMPLETE = 8;

    StringBuilder mFormatBuilder;

    Formatter mFormatter;

    private ImageButton mTurnButton;

    private ImageButton mScaleButton;

    private View mBackButton;

    private ViewGroup loadingLayout;

    private ViewGroup errorLayout;

    private View mTitleLayout;

    private View mControlLayout;

    private View mCenterPlayButton;

    public MatrixMediaController(Context context, AttributeSet attrs) {

        super(context, attrs);

        mContext = context;

        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.MatrixMediaController);

        mScalable = a.getBoolean(R.styleable.MatrixMediaController_mvv_scalable, false);

        a.recycle();

        init(context);

    }

    public MatrixMediaController(Context context) {

        super(context);

        init(context);

    }

    private void init(Context context) {

        mContext = context;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View viewRoot = inflater.inflate(R.layout.matrix_video_view_player_controller, this);

        viewRoot.setOnTouchListener(mTouchListener);

        initControllerView(viewRoot);

    }

    private void initControllerView(View v) {

        mTitleLayout = v.findViewById(R.id.title_part);

        mControlLayout = v.findViewById(R.id.control_layout);

        loadingLayout = (ViewGroup) v.findViewById(R.id.loading_layout);

        errorLayout = (ViewGroup) v.findViewById(R.id.error_layout);

        mTurnButton = (ImageButton) v.findViewById(R.id.turn_button);

        mScaleButton = (ImageButton) v.findViewById(R.id.scale_button);

        mCenterPlayButton = v.findViewById(R.id.center_play_btn);

        mBackButton = v.findViewById(R.id.back_btn);

        if (mTurnButton != null) {

            mTurnButton.requestFocus();

            mTurnButton.setOnClickListener(mPauseListener);

        }

        if (mScalable) {

            if (mScaleButton != null) {

                mScaleButton.setVisibility(VISIBLE);

                mScaleButton.setOnClickListener(mScaleListener);

            }

        } else {

            if (mScaleButton != null) {

                mScaleButton.setVisibility(GONE);

            }

        }

        if (mCenterPlayButton != null) {

            mCenterPlayButton.setOnClickListener(mCenterPlayListener);

        }

        if (mBackButton != null) {

            mBackButton.setOnClickListener(mBackListener);

        }

        View bar = v.findViewById(R.id.seekbar);

        mProgress = (ProgressBar) bar;

        if (mProgress != null) {

            if (mProgress instanceof SeekBar) {

                SeekBar seeker = (SeekBar) mProgress;

                seeker.setOnSeekBarChangeListener(mSeekListener);

            }

            mProgress.setMax(1000);

        }

        mEndTime = (TextView) v.findViewById(R.id.duration);

        mCurrentTime = (TextView) v.findViewById(R.id.has_played);

        mTitle = (TextView) v.findViewById(R.id.title);

        mFormatBuilder = new StringBuilder();

        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    }

    public void setMediaPlayer(MediaPlayerControl player) {

        mPlayer = player;

        updatePausePlay();

    }

    public void show() {

        show(sDefaultTimeout);

    }

    private void disableUnsupportedButtons() {

        try {

            if (mTurnButton != null && mPlayer != null && !mPlayer.canPause()) {

                mTurnButton.setEnabled(false);

            }

        } catch (IncompatibleClassChangeError ex) {

        }

    }

    public void show(int timeout) {

        if (!mShowing) {

            setProgress();

            if (mTurnButton != null) {

                mTurnButton.requestFocus();

            }

            disableUnsupportedButtons();

            mShowing = true;

        }

        updatePausePlay();

        updateBackButton();

        if (getVisibility() != VISIBLE) {

            setVisibility(VISIBLE);

        }

        if (mTitleLayout.getVisibility() != VISIBLE) {

            mTitleLayout.setVisibility(VISIBLE);

        }

        if (mControlLayout.getVisibility() != VISIBLE) {

            mControlLayout.setVisibility(VISIBLE);

        }

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);

        if (timeout != 0) {

            mHandler.removeMessages(FADE_OUT);

            mHandler.sendMessageDelayed(msg, timeout);

        }

    }

    public boolean isShowing() {

        return mShowing;

    }

    public void hide() {

        if (mShowing) {

            mHandler.removeMessages(SHOW_PROGRESS);

            mTitleLayout.setVisibility(GONE);

            mControlLayout.setVisibility(GONE);

            mShowing = false;

        }

    }

    private Handler mHandler = new Handler() {

        @Override

        public void handleMessage(Message msg) {

            int pos;

            switch (msg.what) {

                case FADE_OUT: 

                    hide();

                    break;

                case SHOW_PROGRESS: 

                    pos = setProgress();

                    if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) {

                        msg = obtainMessage(SHOW_PROGRESS);

                        sendMessageDelayed(msg, 1000 - (pos % 1000));

                    }

                    break;

                case SHOW_LOADING: 

                    show();

                    showCenterView(R.id.loading_layout);

                    break;

                case SHOW_COMPLETE: 

                    showCenterView(R.id.center_play_btn);

                    break;

                case SHOW_ERROR: 

                    show();

                    showCenterView(R.id.error_layout);

                    break;

                case HIDE_LOADING: 

                case HIDE_ERROR:

                case HIDE_COMPLETE: 

                    hide();

                    hideCenterView();

                    break;

            }

        }

    };

    private void showCenterView(int resId) {

        if (resId == R.id.loading_layout) {

            if (loadingLayout.getVisibility() != VISIBLE) {

                loadingLayout.setVisibility(VISIBLE);

            }

            if (mCenterPlayButton.getVisibility() == VISIBLE) {

                mCenterPlayButton.setVisibility(GONE);

            }

            if (errorLayout.getVisibility() == VISIBLE) {

                errorLayout.setVisibility(GONE);

            }

        } else if (resId == R.id.center_play_btn) {

            if (mCenterPlayButton.getVisibility() != VISIBLE) {

                mCenterPlayButton.setVisibility(VISIBLE);

            }

            if (loadingLayout.getVisibility() == VISIBLE) {

                loadingLayout.setVisibility(GONE);

            }

            if (errorLayout.getVisibility() == VISIBLE) {

                errorLayout.setVisibility(GONE);

            }

        } else if (resId == R.id.error_layout) {

            if (errorLayout.getVisibility() != VISIBLE) {

                errorLayout.setVisibility(VISIBLE);

            }

            if (mCenterPlayButton.getVisibility() == VISIBLE) {

                mCenterPlayButton.setVisibility(GONE);

            }

            if (loadingLayout.getVisibility() == VISIBLE) {

                loadingLayout.setVisibility(GONE);

            }

        }

    }

    private void hideCenterView() {

        if (mCenterPlayButton.getVisibility() == VISIBLE) {

            mCenterPlayButton.setVisibility(GONE);

        }

        if (errorLayout.getVisibility() == VISIBLE) {

            errorLayout.setVisibility(GONE);

        }

        if (loadingLayout.getVisibility() == VISIBLE) {

            loadingLayout.setVisibility(GONE);

        }

    }

    public void reset() {

        mCurrentTime.setText("00:00");

        mEndTime.setText("00:00");

        mProgress.setProgress(0);

        mTurnButton.setImageResource(R.drawable.matrix_player_player_btn);

        setVisibility(View.VISIBLE);

        hideLoading();

    }

    private String stringForTime(int timeMs) {

        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;

        int minutes = (totalSeconds / 60) % 60;

        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);

        if (hours > 0) {

            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();

        } else {

            return mFormatter.format("%02d:%02d", minutes, seconds).toString();

        }

    }

    private int setProgress() {

        if (mPlayer == null || mDragging) {

            return 0;

        }

        int position = mPlayer.getCurrentPosition();

        int duration = mPlayer.getDuration();

        if (mProgress != null) {

            if (duration > 0) {

             

                long pos = 1000L * position / duration;

                mProgress.setProgress((int) pos);

            }

            int percent = mPlayer.getBufferPercentage();

            mProgress.setSecondaryProgress(percent * 10);

        }

        if (mEndTime != null)

            mEndTime.setText(stringForTime(duration));

        if (mCurrentTime != null)

            mCurrentTime.setText(stringForTime(position));

        return position;

    }

    @Override

    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                show(0);

                handled = false;

                break;

            case MotionEvent.ACTION_UP:

                if (!handled) {

                    handled = false;

                    show(sDefaultTimeout);

                }

                break;

            case MotionEvent.ACTION_CANCEL:

                hide();

                break;

            default:

                break;

        }

        return true;

    }

    boolean handled = false;

 

    private OnTouchListener mTouchListener = new OnTouchListener() {

        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                if (mShowing) {

                    hide();

                    handled = true;

                    return true;

                }

            }

            return false;

        }

    };

    @Override

    public boolean onTrackballEvent(MotionEvent ev) {

        show(sDefaultTimeout);

        return false;

    }

    @Override

    public boolean dispatchKeyEvent(KeyEvent event) {

        int keyCode = event.getKeyCode();

        final boolean uniqueDown = event.getRepeatCount() == 0

                && event.getAction() == KeyEvent.ACTION_DOWN;

        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK

                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE

                || keyCode == KeyEvent.KEYCODE_SPACE) {

            if (uniqueDown) {

                doPauseResume();

                show(sDefaultTimeout);

                if (mTurnButton != null) {

                    mTurnButton.requestFocus();

                }

            }

            return true;

        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {

            if (uniqueDown && !mPlayer.isPlaying()) {

                mPlayer.start();

                updatePausePlay();

                show(sDefaultTimeout);

            }

            return true;

        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP

                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {

            if (uniqueDown && mPlayer.isPlaying()) {

                mPlayer.pause();

                updatePausePlay();

                show(sDefaultTimeout);

            }

            return true;

        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

                || keyCode == KeyEvent.KEYCODE_VOLUME_UP

                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE

                || keyCode == KeyEvent.KEYCODE_CAMERA) {

          

            return super.dispatchKeyEvent(event);

        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {

            if (uniqueDown) {

                hide();

            }

            return true;

        }

        show(sDefaultTimeout);

        return super.dispatchKeyEvent(event);

    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (mPlayer != null) {

                doPauseResume();

                show(sDefaultTimeout);

            }

        }

    };

    private View.OnClickListener mScaleListener = new View.OnClickListener() {

        public void onClick(View v) {

            mIsFullScreen = !mIsFullScreen;

            updateScaleButton();

            updateBackButton();

            mPlayer.setFullscreen(mIsFullScreen);

        }

    };

    //仅全屏时才有返回按钮

    private View.OnClickListener mBackListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (mIsFullScreen) {

                mIsFullScreen = false;

                updateScaleButton();

                updateBackButton();

                mPlayer.setFullscreen(false);

            }

        }

    };

    private View.OnClickListener mCenterPlayListener = new View.OnClickListener() {

        public void onClick(View v) {

            hideCenterView();

            mPlayer.start();

        }

    };

    private void updatePausePlay() {

        if (mPlayer != null && mPlayer.isPlaying()) {

            mTurnButton.setImageResource(R.drawable.matrix_stop_btn);

        } else {

            mTurnButton.setImageResource(R.drawable.matrix_player_player_btn);

        }

    }

    void updateScaleButton() {

        if (mIsFullScreen) {

            mScaleButton.setImageResource(R.drawable.matrix_star_zoom_in);

        } else {

            mScaleButton.setImageResource(R.drawable.matrix_player_scale_btn);

        }

    }

    void toggleButtons(boolean isFullScreen) {

        mIsFullScreen = isFullScreen;

        updateScaleButton();

        updateBackButton();

    }

    void updateBackButton() {

        mBackButton.setVisibility(mIsFullScreen ? View.VISIBLE : View.INVISIBLE);

    }

    boolean isFullScreen() {

        return mIsFullScreen;

    }

    private void doPauseResume() {

        if (mPlayer.isPlaying()) {

            mPlayer.pause();

        } else {

            mPlayer.start();

        }

        updatePausePlay();

    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

        int newPosition = 0;

        boolean change = false;

        public void onStartTrackingTouch(SeekBar bar) {

            if (mPlayer == null) {

                return;

            }

            show(3600000);

            mDragging = true;

            mHandler.removeMessages(SHOW_PROGRESS);

        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {

            if (mPlayer == null || !fromuser) {

                return;

            }

            long duration = mPlayer.getDuration();

            long newposition = (duration * progress) / 1000L;

            newPosition = (int) newposition;

            change = true;

        }

        public void onStopTrackingTouch(SeekBar bar) {

            if (mPlayer == null) {

                return;

            }

            if (change) {

                mPlayer.seekTo(newPosition);

                if (mCurrentTime != null) {

                    mCurrentTime.setText(stringForTime(newPosition));

                }

            }

            mDragging = false;

            setProgress();

            updatePausePlay();

            show(sDefaultTimeout);

            mShowing = true;

            mHandler.sendEmptyMessage(SHOW_PROGRESS);

        }

    };

    @Override

    public void setEnabled(boolean enabled) {

        if (mTurnButton != null) {

            mTurnButton.setEnabled(enabled);

        }

        if (mProgress != null) {

            mProgress.setEnabled(enabled);

        }

        if (mScalable) {

            mScaleButton.setEnabled(enabled);

        }

        mBackButton.setEnabled(true);

    }

    public void showLoading() {

        mHandler.sendEmptyMessage(SHOW_LOADING);

    }

    public void hideLoading() {

        mHandler.sendEmptyMessage(HIDE_LOADING);

    }

    public void showError() {

        mHandler.sendEmptyMessage(SHOW_ERROR);

    }

    public void hideError() {

        mHandler.sendEmptyMessage(HIDE_ERROR);

    }

    public void showComplete() {

        mHandler.sendEmptyMessage(SHOW_COMPLETE);

    }

    public void hideComplete() {

        mHandler.sendEmptyMessage(HIDE_COMPLETE);

    }

    public void setTitle(String titile) {

        mTitle.setText(titile);

    }

    public void setOnErrorView(int resId) {

        errorLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(mContext);

        inflater.inflate(resId, errorLayout, true);

    }

    public void setOnErrorView(View onErrorView) {

        errorLayout.removeAllViews();

        errorLayout.addView(onErrorView);

    }

    public void setOnLoadingView(int resId) {

        loadingLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(mContext);

        inflater.inflate(resId, loadingLayout, true);

    }

    public void setOnLoadingView(View onLoadingView) {

        loadingLayout.removeAllViews();

        loadingLayout.addView(onLoadingView);

    }

    public void setOnErrorViewClick(View.OnClickListener onClickListener) {

        errorLayout.setOnClickListener(onClickListener);

    }

    public interface MediaPlayerControl {

        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        void closePlayer();

        void setFullscreen(boolean fullscreen);

    

        void setFullscreen(boolean fullscreen, int screenOrientation);

    }

}
