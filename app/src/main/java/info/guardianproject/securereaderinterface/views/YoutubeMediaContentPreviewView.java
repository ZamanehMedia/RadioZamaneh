package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.graphics.Bitmap;
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

import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ProxyMediaStreamServer;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class YoutubeMediaContentPreviewView extends FrameLayout implements MediaContentPreviewView, PlayableMediaContentPreviewView
{
	public static final String LOGTAG = "YoutubeMediaContentPreviewView";
	public static final boolean LOGGING = false;

	private MediaContent mMediaContent;
	private File mMediaFile;
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
	private YoutubeMediaContentView mMediaPlayView;

	public YoutubeMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public YoutubeMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public YoutubeMediaContentPreviewView(Context context)
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
		if (mMediaFile != null && mRealBitmap == null && mSetImageThread == null)
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
					Bitmap bmp = UIHelpers.scaleToMaxGLSize(getContext(), mMediaFile, mMediaContent.getWidth(), mMediaContent.getHeight(), w, h);

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
		if (mMediaContent != null && mRealBitmap == null)
			return mMediaContent.getHeight();
		return super.getSuggestedMinimumHeight();
	}

	@Override
	protected int getSuggestedMinimumWidth()
	{
		if (mMediaContent != null && mRealBitmap == null)
			return mMediaContent.getWidth();
		return super.getSuggestedMinimumWidth();
	}

	@Override
	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mIsUpdate = (mediaContent == mMediaContent && mMediaFile != null && mMediaFile.equals(mediaFile));
		mMediaContent = mediaContent;
		mMediaFile = mediaFile;
		mUseThisThread = useThisThread;
		if (mMediaFile == null)
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
			if (mMediaContent != null) {
				mBtnLoading.setVisibility(View.GONE);
				if (mMediaPlayView == null) {

					final View viewController = LayoutInflater.from(getContext()).inflate(R.layout.audio_view_controller, this, false);

					mMediaPlayView = new YoutubeMediaContentView(getContext()) {
						@Override
						protected View createMediaController() {
							return viewController;
						}
					};
					mMediaPlayView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
					addView(mMediaPlayView, this.indexOfChild(mImageView) + 1);
					addView(viewController);

					mMediaPlayView.setStartAutomatically(false);
					mMediaPlayView.setMediaControls(mBtnPlay, mBtnPause, mBtnLoading, mMediaStatusView);
					ProxyMediaStreamServer proxyMediaServer = App.getInstance().getProxyMediaStreamServer();
					if (proxyMediaServer != null) {
						mMediaPlayView.setMediaContent(mMediaContent);
					}
				} else {
					mMediaPlayView.updateMediaControls();
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
			mMediaPlayView.setMediaControls(mBtnPlay, mBtnPause, mBtnLoading, mMediaStatusView);
		}
		updateControls();
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.VISIBLE)
			updateControls();
	}
}