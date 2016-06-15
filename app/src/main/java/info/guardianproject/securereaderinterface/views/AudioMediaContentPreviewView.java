package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ProxyMediaStreamServer;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.iocipher.File;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

public class AudioMediaContentPreviewView extends FrameLayout implements MediaContentPreviewView, PlayableMediaContentPreviewView
{
	public static final String LOGTAG = "AudioMediaContentPreviewView";
	public static final boolean LOGGING = false;

	private MediaContent mMediaContent;
	private MediaContent mThumbnailMediaContent;
	private File mThumbnailMediaFile;
	private Bitmap mRealBitmap;
	private Thread mSetImageThread;
	private Handler mHandler;
	private boolean mUseThisThread;
	private boolean mIsUpdate; // true if this view has already shown this content previously

	private ImageView mImageView;

	private View mPlayPauseView;
	private View mBtnPlay;
	private View mBtnPause;
	private View mBtnLoading;
	private TextView mMediaStatusView;
	private VideoMediaContentView mMediaPlayView;
	private MediaViewCollection.MediaContentLoadInfo mLoadInfo;

	public AudioMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public AudioMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public AudioMediaContentPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.audio_preview_view, this);
		mImageView = (ImageView) findViewById(R.id.image);
		mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		setBitmapIfDownloaded();
		super.onLayout(changed, left, top, right, bottom);
	}

	public void setImageBitmap(Bitmap bm)
	{
		// If we are setting the image from a different thread, make sure to
		// fade it in.
		// If we, however, set it from this thread (as we do when closing the
		// full screen mode
		// view) we want it to show immediately!
		if (bm != null && !mUseThisThread && !mIsUpdate)
			AnimationHelpers.fadeOut(mImageView, 0, 0, false);
		mImageView.setImageBitmap(bm);
		if (bm != null && !mUseThisThread && !mIsUpdate)
			AnimationHelpers.fadeIn(mImageView, 500, 0, false);
		else if (bm != null)
			AnimationHelpers.fadeIn(mImageView, 0, 0, false);
	}

	public void recycle()
	{
		setImageBitmap(null);
		if (mRealBitmap != null)
		{
			mRealBitmap.recycle();
			mRealBitmap = null;
		}
	}

	private synchronized void setBitmapIfDownloaded()
	{
		if (mThumbnailMediaFile != null && mRealBitmap == null && mSetImageThread == null)
		{
			if (mHandler == null && !mUseThisThread)
				mHandler = new Handler();

			Runnable setImageRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					int w = getWidth();
					int h = getHeight();
					Bitmap bmp = UIHelpers.scaleToMaxGLSize(getContext(), mThumbnailMediaFile, mThumbnailMediaContent.getWidth(), mThumbnailMediaContent.getHeight(), w, h);

					Runnable doSetImageRunnable = new Runnable()
					{
						private Bitmap mBitmap;

						@Override
						public void run()
						{
							mRealBitmap = mBitmap;
							setImageBitmap(mRealBitmap);
						}

						private Runnable init(Bitmap bitmap)
						{
							mBitmap = bitmap;
							return this;
						}

					}.init(bmp);

					if (mUseThisThread)
						doSetImageRunnable.run();
					else
						mHandler.post(doSetImageRunnable);
				}
			};

			if (mUseThisThread)
			{
				setImageRunnable.run();
			}
			else
			{
				mSetImageThread = new Thread(setImageRunnable);
				mSetImageThread.start();
			}
		}
	}

	@Override
	protected int getSuggestedMinimumHeight()
	{
		if (mThumbnailMediaContent != null && mRealBitmap == null)
			return mThumbnailMediaContent.getHeight();
		return super.getSuggestedMinimumHeight();
	}

	@Override
	protected int getSuggestedMinimumWidth()
	{
		if (mThumbnailMediaContent != null && mRealBitmap == null)
			return mThumbnailMediaContent.getWidth();
		return super.getSuggestedMinimumWidth();
	}

	public void setThumbnailMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mIsUpdate = (mediaContent == mThumbnailMediaContent && mThumbnailMediaFile != null && mThumbnailMediaFile.equals(mediaFile));
		mThumbnailMediaContent = mediaContent;
		mThumbnailMediaFile = mediaFile;
		mUseThisThread = useThisThread;
		if (mThumbnailMediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}

		int w = getWidth();
		int h = getHeight();
		if (w > 0 && h > 0)
		{
			setBitmapIfDownloaded();
		}
	}

	@Override
	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mMediaContent = mediaContent;
		updateControls();
	}

	@Override
	public MediaContent getMediaContent()
	{
		return mMediaContent;
	}

	private void updateControls() {
		if (mPlayPauseView != null) {
			mPlayPauseView.setVisibility(View.VISIBLE);
			if (mLoadInfo != null && mLoadInfo.isLoading()) {
				mBtnPlay.setVisibility(View.GONE);
				mBtnPause.setVisibility(View.GONE);
				mBtnLoading.setVisibility(View.VISIBLE);
				mMediaStatusView.setText(R.string.media_status_loading);
			} else if (mMediaContent != null) {
				mBtnLoading.setVisibility(View.GONE);
				if (mMediaPlayView == null) {

					final View audioViewController = LayoutInflater.from(getContext()).inflate(R.layout.audio_view_controller, this, false);

					mMediaPlayView = new VideoMediaContentView(getContext()) {
						@Override
						protected View createMediaController() {
							return audioViewController;
						}

						@Override
						protected void setupMediaController(View controllerView) {
						}
					};
					mMediaPlayView.setLayoutParams(new AudioMediaContentPreviewView.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
					addView(mMediaPlayView, this.indexOfChild(mImageView) + 1);
					addView(audioViewController);

					mMediaPlayView.setStartAutomatically(false);
					mMediaPlayView.setAlwaysShowController(true);
					if (mBtnPlay != null)
						mMediaPlayView.setPlayButton(mBtnPlay);
					if (mBtnPause != null)
						mMediaPlayView.setPauseButton(mBtnPause);
					if (mMediaStatusView != null)
						mMediaPlayView.setStatusView(mMediaStatusView);
					ProxyMediaStreamServer proxyMediaServer = App.getInstance().getProxyMediaStreamServer();
					if (proxyMediaServer != null) {
						mMediaPlayView.setContentUri(Uri.parse(proxyMediaServer.getProxyUrlForMediaContent(mMediaContent)));
					}
				}
			}
		}
	}

	public void pauseIfPlaying() {
		if (mMediaPlayView != null)
			mMediaPlayView.pause();
	}

	public void setPlayPauseView(View playPauseView, TextView mediaStatusView) {
		mPlayPauseView = playPauseView;
		mBtnPlay = mPlayPauseView.findViewById(R.id.btnPlay);
		mBtnPause = mPlayPauseView.findViewById(R.id.btnPause);
		mBtnLoading = mPlayPauseView.findViewById(R.id.btnLoading);
		mMediaStatusView = mediaStatusView;
		if (mMediaPlayView != null) {
			mMediaPlayView.setPlayButton(mBtnPlay);
			mMediaPlayView.setPauseButton(mBtnPause);
		}
		updateControls();
	}

	public void setLoadInfo(MediaViewCollection.MediaContentLoadInfo loadInfo) {
		mLoadInfo = loadInfo;
	}
}