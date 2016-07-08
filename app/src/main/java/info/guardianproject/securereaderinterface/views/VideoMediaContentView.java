package info.guardianproject.securereaderinterface.views;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import info.guardianproject.securereaderinterface.models.OnMediaOrientationListener;
import info.guardianproject.securereaderinterface.R;

public class VideoMediaContentView extends FrameLayout implements OnErrorListener, OnPreparedListener, OnCompletionListener, OnSeekBarChangeListener
{
	protected VideoView mVideoView;
	private MediaController mMediaController;
	private MediaPlayer mMP;
	private View mControllerView;
	private View mViewLoading;
	private View mBtnPlay;
	private View mBtnPause;
	private TextView mMediaStatusView;
	private SeekBar mSeekbar;
	private boolean mIsTrackingThumb;
	private OnMediaOrientationListener mOrientationListener;
	private boolean mAlwaysShowController;
	private boolean mStartAutomatically;

	public VideoMediaContentView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public VideoMediaContentView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public VideoMediaContentView(Context context)
	{
		super(context);
		initView(context);
	}

	protected int getMediaViewId() {
		return R.layout.video_view;
	}

	private void initView(Context context)
	{
		mStartAutomatically = true;
		View.inflate(context, getMediaViewId(), this);

		mVideoView = (VideoView) findViewById(R.id.content);
		mViewLoading = findViewById(R.id.frameLoading);

		mControllerView = createMediaController();

		if (!isInEditMode())
		{
			mViewLoading.setVisibility(View.INVISIBLE);
			setupMediaController(mControllerView);

			mVideoView.setOnErrorListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnCompletionListener(this);
		}

		View btnCollapse = mControllerView.findViewById(R.id.btnCollapse);
		if (btnCollapse != null) {
			btnCollapse.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					((Activity) v.getContext()).finish();
				}
			});
		}

		setMediaControls(mControllerView.findViewById(R.id.btnPlay),
				mControllerView.findViewById(R.id.btnPause), null, null);

		mSeekbar = (SeekBar) mControllerView.findViewById(R.id.seekbar);
		if (mSeekbar != null) {
			mSeekbar.setMax(0);
			mSeekbar.setProgress(0);
			mSeekbar.setSecondaryProgress(0);
		}
	}

	protected View createMediaController() {
		return LayoutInflater.from(getContext()).inflate(R.layout.video_view_controller, this, false);
	}

	protected void setupMediaController(View controllerView) {
			mMediaController = new InternalMediaController(getContext());
			mMediaController.setAnchorView(mVideoView);
			mVideoView.setMediaController(mMediaController);
	}

	public void setStartAutomatically(boolean startAutomatically) {
		this.mStartAutomatically = startAutomatically;
	}

	public void setAlwaysShowController(boolean alwaysShowController) {
		this.mAlwaysShowController = alwaysShowController;
	}

	public void setMediaControls(View btnPlay, View btnPause, View btnLoading, TextView mediaStatusView) {
		if (mBtnPlay != null)
			mBtnPlay.setOnClickListener(null);
		mBtnPlay = btnPlay;
		if (mBtnPlay != null) {
			mBtnPlay.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					play();
				}
			});
		}

		if (mBtnPause != null)
			mBtnPause.setOnClickListener(null);
		mBtnPause = btnPause;
		if (mBtnPause != null) {
			mBtnPause.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pause();
				}
			});
		}

		mMediaStatusView = mediaStatusView;
		updateMediaControls();
	}

	public void updateMediaControls() {
		if (mBtnPlay != null)
			mBtnPlay.setVisibility(mVideoView.isPlaying() ? View.GONE : View.VISIBLE);
		if (mBtnPause != null)
			mBtnPause.setVisibility(mVideoView.isPlaying() ? View.VISIBLE : View.GONE);
	}

	public void play() {
		mVideoView.start();
		startProgressThread();
		updateMediaControls();
	}

	public void pause() {
		mVideoView.pause();
		updateMediaControls();
	}

	public void setContentUri(Uri uri)
	{
		mVideoView.setVideoURI(uri);
		mViewLoading.setVisibility(View.VISIBLE);
	}

	/**
	 * Sets a listener that will be notified when media has been downloaded and
	 * it is known whether this media is in landscape or portrait mode.
	 * 
	 * @param listener
	 */
	public void setOnMediaOrientationListener(OnMediaOrientationListener listener)
	{
		this.mOrientationListener = listener;
	}

	private class InternalMediaController extends MediaController
	{
		public InternalMediaController(Context context)
		{
			super(context);
		}

		@Override
		public void setAnchorView(View view)
		{
			super.setAnchorView(view);
			this.removeAllViews();
			addView(mControllerView);
		}

		@Override
		public void hide() {
			if (!mAlwaysShowController) {
				super.hide();
			}
		}
	}

	public MediaPlayer getMediaPlayer()
	{
		return mMP;
	}

	@Override
	public void onPrepared(final MediaPlayer mp)
	{
		if (mOrientationListener != null)
		{
			boolean isPortrait = mp.getVideoHeight() > mp.getVideoWidth();
			mOrientationListener.onMediaOrientation(this, isPortrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		mMP = mp;
		mViewLoading.setVisibility(View.GONE);
		if (mMediaController != null)
			mMediaController.show();
		mSeekbar.setOnSeekBarChangeListener(this);
		mSeekbar.setMax(mp.getDuration());
		if (mMediaStatusView != null)
			mMediaStatusView.setText(getResources().getString(R.string.media_status_minutes, (int) (mp.getDuration() / 60000)));
		if (mStartAutomatically) {
			play();
		}
	}

	private void startProgressThread() {
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					while (mMP != null && mMP.isPlaying() && mMP.getCurrentPosition() < mMP.getDuration())
					{
						if (!mIsTrackingThumb)
							mSeekbar.setProgress(mMP.getCurrentPosition());
						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		// TODO - show error
		mViewLoading.setVisibility(View.GONE);
		mBtnPlay.setVisibility(View.VISIBLE);
		mBtnPause.setVisibility(View.INVISIBLE);
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp)
	{
		mBtnPlay.setVisibility(View.VISIBLE);
		mBtnPause.setVisibility(View.INVISIBLE);
		mVideoView.seekTo(0);
		mSeekbar.setProgress(0);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		if (mIsTrackingThumb && mMediaController != null)
			mMediaController.show();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
		mIsTrackingThumb = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		if (getMediaPlayer() != null)
			getMediaPlayer().seekTo(seekBar.getProgress());
		mIsTrackingThumb = false;
	}
}