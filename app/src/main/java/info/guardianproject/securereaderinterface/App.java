package info.guardianproject.securereaderinterface;
		
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader.SocialReaderLockListener;
import info.guardianproject.securereader.SyncService;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbackListener;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.widgets.CustomFontButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontEditText;
import info.guardianproject.securereaderinterface.widgets.CustomFontRadioButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontTextView;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReporter;

import info.guardianproject.securereaderinterface.widgets.MirroringImageView;
import info.guardianproject.zt.SplashActivity;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tinymission.rss.Feed;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class App extends MultiDexApplication implements OnSharedPreferenceChangeListener, SocialReaderLockListener, SocialReader.SocialReaderFeedPreprocessor
{
	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;
	
	public static final boolean UI_ENABLE_POPULAR_ITEMS = false;
			
	public static final boolean UI_ENABLE_COMMENTS = false;
	public static final boolean UI_ENABLE_TAGS = false;
	public static final boolean UI_ENABLE_POST_LOGIN = false;
	public static final boolean UI_ENABLE_REPORTER = false;
	public static final boolean UI_ENABLE_CHAT = false;
	public static final boolean UI_ENABLE_LANGUAGE_CHOICE = true;
	
	public static final String EXIT_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.exit.action";
	public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.setuilanguage.action";
	public static final String WIPE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.wipe.action";
	public static final String LOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.lock.action";
	public static final String UNLOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.unlock.action";
	public static final String RADIOPLAYER_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.radioplayer.action";
	public static final String SYNC_STATUS_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.sync.action";

	public static final String FRAGMENT_TAG_RECEIVE_SHARE = "FragmentReceiveShare";
	public static final String FRAGMENT_TAG_SEND_BT_SHARE = "FragmentSendBTShare";

	// the following url is what is contained in the m3u link here:
	// http://www.internet-radio.com/servers/tools/playlistgenerator/?u=http://uk1.internet-radio.com:8034/live.m3u&t=.m3u";
	// we might need to download the m3u every time and use the contained url in case that changes
	public static final String RZ_RADIO_URI = "http://uk1.internet-radio.com:8034/live";

	private static App m_singleton;

	public static Context m_context;
	public static SettingsUI m_settings;

	public SocialReader socialReader;
	public SocialReporter socialReporter;
	private ProxyMediaStreamServer proxyMediaStreamServer;

	private String mCurrentLanguage;
	private FeedFilterType mCurrentFeedFilterType = FeedFilterType.ALL_FEEDS;
	private Feed mCurrentFeed = null;

	private boolean mIsWiping = false;

	public static final int RADIO_PLAYER_IDLE = 0;
	public static final int RADIO_PLAYER_LOADING = 1;
	public static final int RADIO_PLAYER_PLAYING = 2;
	public static final int RADIO_PLAYER_ERROR = 3;
	private MediaPlayer mPlayer;
	private int mRadioPlayerStatus = RADIO_PLAYER_IDLE;

	@Override
	public void onCreate()
	{
		m_singleton = this;
		m_context = this;
		m_settings = new SettingsUI(m_context);

		mCurrentFeed = new Feed(1, null, null);
		mCurrentFeedFilterType = FeedFilterType.SINGLE_FEED;

		// Currently hardcode Farsi
		m_settings.setUiLanguage(Settings.UiLanguage.Farsi);

		applyUiLanguage(false);
		super.onCreate();

		socialReader = SocialReader.getInstance(this.getApplicationContext());
		socialReader.setLockListener(this);
		socialReader.setFeedPreprocessor(this);
		socialReader.setSyncServiceListener(new SyncService.SyncServiceListener()
		{
			@Override
			public void syncEvent(SyncService.SyncTask syncTask)
			{
				if (LOGGING)
					Log.v(LOGTAG, "Got a syncEvent");
				if (syncTask.type == SyncService.SyncTask.TYPE_FEED) {
					if (getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS ||
							(getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED && getCurrentFeed() != null && syncTask.feed != null && getCurrentFeed().getDatabaseId() == syncTask.feed.getDatabaseId())) {
						Log.v(LOGTAG, "Sync event for current feed: " + syncTask.status);
						Intent broadcast = new Intent(App.SYNC_STATUS_BROADCAST_ACTION);
						broadcast.putExtra("feed", syncTask.feed == null ? 0 : syncTask.feed.getDatabaseId());
						broadcast.putExtra("status", syncTask.status);
						LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(broadcast);

						if (syncTask.feed != null)
							updateCurrentFeed(syncTask.feed);
					}
				}
			}
		});
		socialReporter = new SocialReporter(socialReader);
		//applyPassphraseTimeout();
		
		m_settings.registerChangeListener(this);
		
		mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
		UICallbacks.getInstance().addListener(new UICallbackListener()
		{
			@Override
			public void onFeedSelect(FeedFilterType type, long feedId, Object source)
			{
				Feed feed = null;
				if (type == FeedFilterType.SINGLE_FEED)
				{
					feed = getFeedById(feedId);
				}
				mCurrentFeedFilterType = type;
				mCurrentFeed = feed;
			}
		});
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	public static Context getContext()
	{
		return m_context;
	}

	public static App getInstance()
	{
		return m_singleton;
	}

	public static SettingsUI getSettings()
	{
		return m_settings;
	}

	private Bitmap mTransitionBitmap;

	private LockScreenActivity mLockScreen;

	public Bitmap getTransitionBitmap()
	{
		return mTransitionBitmap;
	}

	public void putTransitionBitmap(Bitmap bitmap)
	{
		mTransitionBitmap = bitmap;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (!mIsWiping) {
			if (key.equals(Settings.KEY_PROXY_TYPE)) {
				stopRadioPlayer();
			} else if (key.equals(Settings.KEY_UI_LANGUAGE)) {
				applyUiLanguage(true);
			} else if (key.equals(Settings.KEY_PASSPHRASE_TIMEOUT)) {
				applyPassphraseTimeout();
			}
		}
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		applyUiLanguage(false);
	}

	@SuppressLint("NewApi")
	private void applyUiLanguage(boolean sendNotifications)
	{
		// Update language!
		//
		String language = m_settings.uiLanguageCode();
		if (language.equals(mCurrentLanguage))
			return;
		mCurrentLanguage = language;
		setCurrentLanguageInConfig(m_context);

		// Notify activities (if any)
		if (sendNotifications)
			LocalBroadcastManager.getInstance(m_context).sendBroadcastSync(new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
	}

	public void setCurrentLanguageInConfig(Context context)
	{
		Configuration config = new Configuration();
		String language = m_settings.uiLanguageCode();
		Locale loc = new Locale(language);
		if (Build.VERSION.SDK_INT >= 17)
			config.setLocale(loc);
		else
			config.locale = loc;
		Locale.setDefault(loc);
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
	}

	private void applyPassphraseTimeout()
	{
		socialReader.setCacheWordTimeout(m_settings.passphraseTimeout());
	}

	public void wipe(Context context, int wipeMethod)
	{
		mIsWiping = true;
		socialReader.doWipe(wipeMethod);
		m_settings.resetSettings();
		mLastResumed = null;
		mLockScreen = null;

		// Notify activities (if any)
		LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.WIPE_BROADCAST_ACTION));

		if (wipeMethod == SocialReader.DATA_WIPE)
		{
			Intent intent = new Intent(m_context, SplashActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		mIsWiping = false;
	}

	public boolean isWiping()
	{
		return mIsWiping;
	}

	public static View createView(String name, Context context, AttributeSet attrs)
	{
		View returnView = null;

		int id = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "id", -1);
		if (Build.VERSION.SDK_INT < 11)
		{
			// Older devices don't support setting the "android:alertDialogTheme" in styles.xml
			int idParent = Resources.getSystem().getIdentifier("parentPanel", "id", "android");
			if (id == idParent)
				context.setTheme(R.style.ModalDialogTheme);
		}
		
		if (name.equals("TextView") || name.endsWith("DialogTitle"))
		{
			return new CustomFontTextView(context, attrs);
		}
		else if (name.equals("Button"))
		{
			View view = null;
			if (id == android.R.id.button1) // Positive button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonPositiveTheme), attrs);
			else if (id == android.R.id.button2) // Negative button
				view = new CustomFontButton(new ContextThemeWrapper(context, R.style.ModalAlertDialogButtonNegativeTheme), attrs);
			else
				view = new CustomFontButton(context, attrs);		
			return view;
		}
		else if (name.equals("RadioButton"))
		{
			return new CustomFontRadioButton(context, attrs);
		}
		else if (name.equals("EditText"))
		{
			return new CustomFontEditText(context, attrs);
		}
/*		else if (name.equals("ImageView"))
		{
			TypedArray a = context.obtainStyledAttributes(attrs, new int[] { R.attr.doNotMirror });
			boolean dontMirror = false;
			if (a != null)
				dontMirror = a.getBoolean(0, false);
			a.recycle();
			if (dontMirror)
				returnView = new ImageView(context, attrs);
			else
				returnView = new MirroringImageView(context, attrs);
		}*/
		// API 17 still has some trouble with handling RTL layouts automatically.
		else if (name.equals("RelativeLayout") && Build.VERSION.SDK_INT == 17 && getInstance().isRTL()) {
			if (returnView == null)
				returnView = new RelativeLayout(context, attrs);
			RelativeLayout relativeLayout = (RelativeLayout) returnView;
			relativeLayout.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
				@Override
				public void onChildViewAdded(View parent, View child) {
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) child.getLayoutParams();
					if (lp != null) {
						int[] rules = lp.getRules();
						if (rules[RelativeLayout.START_OF] != 0) {
							lp.removeRule(RelativeLayout.LEFT_OF);
						}
						if (rules[RelativeLayout.END_OF] != 0) {
							lp.removeRule(RelativeLayout.RIGHT_OF);
						}
						if (rules[RelativeLayout.ALIGN_PARENT_START] != 0) {
							lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						if (rules[RelativeLayout.ALIGN_PARENT_END] != 0) {
							lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
					}
				}

				@Override
				public void onChildViewRemoved(View parent, View child) {
				}
			});
		}
		return returnView;
	}

	public boolean isRTL()
	{
		if (Build.VERSION.SDK_INT >= 17)
		{
			return (getBaseContext().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
		}
		else
		{
			// Handle old devices by looking at current language
			Configuration config = getBaseContext().getResources().getConfiguration();
			if (config.locale != null)
			{
				String language = config.locale.getLanguage();
				if (language.startsWith("ar") || language.startsWith("fa"))
					return true;
			}
			return false;
		}
	}

	private int mnResumed = 0;
	private Activity mLastResumed;
	private boolean mIsLocked = true;
	
	public void onActivityPause(Activity activity)
	{
		mnResumed--;
		if (mnResumed == 0)
			socialReader.onPause();
		if (mLastResumed == activity)
			mLastResumed = null;
	}

	public void onActivityResume(Activity activity)
	{
		mLastResumed = activity;
		mnResumed++;
		if (mnResumed == 1)
			socialReader.onResume();
		showLockScreenIfLocked();
	}
	
	public boolean isActivityLocked()
	{
		return mIsLocked;
	}
	
	private void showLockScreenIfLocked()
	{
		if (mIsLocked && mLastResumed != null && mLockScreen == null && !mIsWiping)
		{
			Intent intent = new Intent(App.this, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra("originalIntent", mLastResumed.getIntent());
			mLastResumed.startActivity(intent);
			mLastResumed.overridePendingTransition(0, 0);
			mLastResumed = null;
		}
	}
	
	@Override
	public void onLocked()
	{
		mIsLocked = true;
		showLockScreenIfLocked();
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(LOCKED_BROADCAST_ACTION));
	}

	@Override
	public void onUnlocked()
	{
		mIsLocked = false;
		if (mLockScreen != null)
			mLockScreen.onUnlocked();
		mLockScreen = null;
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(UNLOCKED_BROADCAST_ACTION));
	}

	public void onLockScreenResumed(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = lockScreenActivity;
	}

	public void onLockScreenPaused(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = null;
	}
	
	private Feed getFeedById(long idFeed)
	{
		ArrayList<Feed> items = socialReader.getSubscribedFeedsList();
		for (Feed feed : items)
		{
			if (feed.getDatabaseId() == idFeed)
				return feed;
		}
		return null;
	}
	
	public FeedFilterType getCurrentFeedFilterType()
	{
		return mCurrentFeedFilterType;
	}
	
	public Feed getCurrentFeed()
	{
		return mCurrentFeed;
	}
	
	public long getCurrentFeedId()
	{
		if (getCurrentFeed() != null)
			return getCurrentFeed().getDatabaseId();
		return 0;
	}

	/**
	 * Update the current feed property. Why is this needed? Because if the feed was just
	 * updated from the network a new Feed object will have been created and we want to
	 * pick up changes to the network pull date (and possibly other changes) here.
	 * @param feed
	 */
	public void updateCurrentFeed(Feed feed)
	{
		if (mCurrentFeed != null && mCurrentFeed.getDatabaseId() == feed.getDatabaseId())
			mCurrentFeed = feed;
	}

	public static InputStream XsltTransform(InputStream stylesheet, InputStream data, long expiryDateLimit) {
		String html = "";

		try {
			Source xmlSource = new StreamSource(data);
			Source xsltSource = new StreamSource(stylesheet);

			StringWriter writer = new StringWriter();
			Result result = new StreamResult(writer);
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer(xsltSource);
			transformer.setParameter("expiryDateLimit", expiryDateLimit);
			transformer.transform(xmlSource, result);

			html = writer.toString();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ByteArrayInputStream(html.getBytes());
	}

	@Override
	public String onGetFeedURL(Feed feed) {
		return null;
	}

	private long getExpirationDateAsLong() {
		long ret = 0;
		if (m_settings.articleExpiration() != Settings.ArticleExpiration.Never) {
			Date expirationDate = new Date(System.currentTimeMillis() - m_settings.articleExpirationMillis());
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.setTime(expirationDate);
			ret = c.get(Calendar.YEAR) * 10000000000L;
			ret += (c.get(Calendar.MONTH) + 1) * 100000000L;
			ret += c.get(Calendar.DAY_OF_MONTH) * 1000000;
			ret += c.get(Calendar.HOUR_OF_DAY) * 10000;
			ret += c.get(Calendar.MINUTE) * 100;
			ret += c.get(Calendar.SECOND);
		}
		return ret;
	}

	@Override
	public InputStream onFeedDownloaded(Feed feed, InputStream content, Map<String, String> headers) {
		if (App.isAudioFeed(feed)) {
			return XsltTransform(m_context.getResources().openRawResource(R.raw.audio_feed_transform), content, getExpirationDateAsLong());
		} else if (App.isVideoFeed(feed)) {
			return XsltTransform(m_context.getResources().openRawResource(R.raw.youtube_feed_transform), content, 0);
		}
		return null;
	}

	public static boolean isAudioFeed(Feed feed) {
		return feed.getFeedURL().contains("://feeds.soundcloud.com");
	}

	public static boolean isVideoFeed(Feed feed) {
		return feed.getFeedURL().contains("://www.youtube.com/feeds/videos.xml");
	}

	public static boolean isGeneralFeed(Feed feed) {
		return feed.getFeedURL().contains("://radiozamaneh.com/feed");
	}

	public ProxyMediaStreamServer getProxyMediaStreamServer() {
		if (proxyMediaStreamServer == null || !proxyMediaStreamServer.isAlive()) {
			if (proxyMediaStreamServer != null) // If we have a dead server, call stop before we replace it
				proxyMediaStreamServer.stop();
			proxyMediaStreamServer = ProxyMediaStreamServer.createMediaServer();
		}
		return proxyMediaStreamServer;
	}

	public int getRadioPlayerStatus() {
		return mRadioPlayerStatus;
	}

	public void toggleRadioPlayer() {
		//if not playing
		if (mRadioPlayerStatus == RADIO_PLAYER_IDLE || mRadioPlayerStatus == RADIO_PLAYER_ERROR) {
			if (mPlayer != null) {
				mPlayer.reset();
				mPlayer.release();
			}
			startRadioPlayer();
		} else {
			stopRadioPlayer();
		}
	}

	private void startRadioPlayer() {
		try {
			boolean useProxy = App.getInstance().socialReader.useProxy();
			if (!useProxy || App.getInstance().getProxyMediaStreamServer() != null) {
				mRadioPlayerStatus = RADIO_PLAYER_LOADING;
				updateRadioPlayerUI();
				mPlayer = new MediaPlayer();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					public void onPrepared(MediaPlayer mp) {
						mp.start();
						mRadioPlayerStatus = RADIO_PLAYER_PLAYING;
						updateRadioPlayerUI();
					}
				});
				mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer mp, int what, int extra) {
						if (LOGGING)
							Log.d(LOGTAG, "MediaPlayer onError: " + what + " " + extra);
						mRadioPlayerStatus = RADIO_PLAYER_ERROR;
						updateRadioPlayerUI();
						return false;
					}
				});
				mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						mRadioPlayerStatus = RADIO_PLAYER_IDLE;
						updateRadioPlayerUI();
					}
				});
				mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
					@Override
					public void onBufferingUpdate(MediaPlayer mp, int percent) {
						if (LOGGING)
							Log.d(LOGTAG, String.format("Percent is %d", percent));
					}
				});
				if (useProxy)
					mPlayer.setDataSource(this, Uri.parse(App.getInstance().getProxyMediaStreamServer().getProxyUrlForUrl(RZ_RADIO_URI)));
				else
					mPlayer.setDataSource(this, Uri.parse(RZ_RADIO_URI));
				mPlayer.prepareAsync();
			}
		} catch (IllegalStateException e) {
			Log.d(LOGTAG, "IllegalStateException: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			Log.d(LOGTAG, "IllegalArgumentException: " + e.getMessage());
		} catch (SecurityException e) {
			Log.d(LOGTAG, "SecurityException: " + e.getMessage());
		} catch (Exception e) {
			Log.d(LOGTAG, "Exception: " + e.getMessage());
		}
	}

	private void stopRadioPlayer() {
		if (mPlayer != null) {
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		mRadioPlayerStatus = RADIO_PLAYER_IDLE;
		updateRadioPlayerUI();
	}

	private void updateRadioPlayerUI() {
		Intent intent = new Intent(App.RADIOPLAYER_BROADCAST_ACTION);
		intent.putExtra("status", mRadioPlayerStatus);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
