package info.guardianproject.securereaderinterface.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 16-03-03.
 */
public class YoutubeMediaContentView extends WebView implements SeekBar.OnSeekBarChangeListener {

    public static final boolean LOGGING = false;
    public static final String LOGTAG = "YoutubeMediaContentView";

    private MediaContent mMediaContent;
    private boolean mUseProxy;
    private String mUserAgent;

    private View mBtnPlay;
    private View mBtnPause;
    private boolean mIsPlayerLoaded;
    private boolean mIsPlaying;
    private boolean mIsPaused;

    private View mMediaController;
    private SeekBar mSeekbar;
    private ProgressBar mLoadIndicator;
    private boolean mIsTrackingThumb;

    public YoutubeMediaContentView(Context context) {
        super(context);
        init(context);
    }

    @SuppressLint("NewApi")
    public YoutubeMediaContentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public YoutubeMediaContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public YoutubeMediaContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public YoutubeMediaContentView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init(context);
    }

    private void init(Context context) {
        mUseProxy = App.getInstance().socialReader.useProxy();

        getSettings().setPluginState(WebSettings.PluginState.ON);
        getSettings().setLoadsImagesAutomatically(true);
        getSettings().setAppCacheEnabled(true);
        getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setAllowUniversalAccessFromFileURLs(true);
        setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setWebChromeClient(new WebChromeClient());
        addJavascriptInterface(new WebAppInterface(context), "Android");

        mUserAgent = getSettings().getUserAgentString();

        mMediaController = createMediaController();
        if (mMediaController != null) {
            mSeekbar = (SeekBar)mMediaController.findViewById(R.id.seekbar);
            if (mSeekbar != null) {
                mSeekbar.setOnSeekBarChangeListener(this);
                mSeekbar.setVisibility(View.GONE);
                mSeekbar.setMax(0);
                mSeekbar.setProgress(0);
                mSeekbar.setSecondaryProgress(0);
            }
            mLoadIndicator = (ProgressBar)mMediaController.findViewById(R.id.progressBar);
            if (mLoadIndicator != null)
                mLoadIndicator.setVisibility(View.VISIBLE); // Until the frame is loaded
        }
    }

    protected View createMediaController() {
        return null;
    }

    public void setMediaContent(MediaContent mediaContent) {
        mMediaContent = mediaContent;

        Item parentItem = App.getInstance().socialReader.getItemFromId(mediaContent.getItemDatabaseId());
        String videoId = "";
        if (parentItem == null || !parentItem.getGuid().startsWith("yt:video:")) {
            setVisibility(View.GONE);
            return;
        }


        videoId = parentItem.getGuid().substring(9); // Strip the "yt:video:" part to get video ID

        String playerPageSource = null;
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.youtube_player);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            playerPageSource = new String(b);
        } catch (Exception e) {
        }

        if (playerPageSource != null) {
            playerPageSource = playerPageSource.replace("%VIDEOID%", videoId);
            playerPageSource = playerPageSource.replace("%WIDTH%", "100%");
            playerPageSource = playerPageSource.replace("%HEIGHT%", "100%");
            setWebViewClient(new WebViewClient() {

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (mUseProxy && !request.getUrl().getScheme().equals("data")) {
                        return createProxiedResponse(request.getUrl().toString(), request.getMethod(), request.getRequestHeaders());
                    }
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (mUseProxy && !url.startsWith("data:")) {
                        return createProxiedResponse(url, "get", null);
                    }
                    return super.shouldInterceptRequest(view, url);
                }
            });
            loadDataWithBaseURL("https://www.youtube.com", playerPageSource, "text/html", "utf-8", null);
        }
    }


    WebResourceResponse createProxiedResponse(String uri, String method, Map<String, String> headers) {
        try {
            if (LOGGING)
                Log.v(LOGTAG, "Reqest for URL: " + uri);

            if (!method.toLowerCase().equals("get"))
                throw new Exception("Can only handle get request at this time");

            SocialReader socialReader = App.getInstance().socialReader;

            Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(socialReader.getProxyHost(), 8118));

            URL url = new URL(uri);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);
            if (headers != null) {
                for (String header : headers.keySet()) {
                    urlConnection.addRequestProperty(header, headers.get(header));
                }
            } else {
                urlConnection.setRequestProperty("Host", Uri.parse(uri).getHost());
                urlConnection.setRequestProperty("User-Agent", mUserAgent);
            }

            if (LOGGING)
                Log.v(LOGTAG, "Status line: " + urlConnection.getResponseMessage());

            HashMap<String, String> mapHeaders = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> entry : urlConnection.getHeaderFields().entrySet()) {
                if (TextUtils.isEmpty(entry.getKey()))
                    continue;
                mapHeaders.put(entry.getKey(), TextUtils.join(",", entry.getValue()));
                if (LOGGING)
                    Log.v(LOGTAG, String.format("Response header: %s -> %s", entry.getKey(), TextUtils.join(",", entry.getValue())));
            }

            String mimeType = "application/octet-stream";
            String enc = "utf-8";

            if (urlConnection.getContentType() != null) {
                String[] parts = urlConnection.getContentType().split(";");
                mimeType = parts[0];
                if (parts.length > 1) {
                    String charset = parts[1].trim();
                    if (charset.startsWith("charset="))
                        enc = charset.substring(8);
                }
            }
            if (LOGGING)
                Log.v(LOGTAG, "Mime " + mimeType + " enc " + enc);
            if (Build.VERSION.SDK_INT >= 21)
                return new WebResourceResponse(mimeType, enc, urlConnection.getResponseCode(), urlConnection.getResponseMessage(), mapHeaders, urlConnection.getInputStream());
            else
                return new WebResourceResponse(mimeType, enc, urlConnection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new WebResourceResponse("text/text", "utf-8", null);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != View.VISIBLE) {
            pause();
        }
    }

    public void setStartAutomatically(boolean startAutomatically) {
    }


    public void setPlayButton(View view) {
        if (mBtnPlay != null)
            mBtnPlay.setOnClickListener(null);
        mBtnPlay = view;
        if (mBtnPlay != null) {
            mBtnPlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    play();
                }
            });
            mBtnPlay.setVisibility((mIsPlaying || !mIsPlayerLoaded) ? View.GONE : View.VISIBLE);
        }
    }

    public void setPauseButton(View view) {
        if (mBtnPause != null)
            mBtnPause.setOnClickListener(null);
        mBtnPause = view;
        if (mBtnPause != null) {
            mBtnPause.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    pause();
                }
            });
            mBtnPause.setVisibility(mIsPlaying ? View.VISIBLE : View.GONE);
        }
    }

    public void play() {
        if (mIsPlayerLoaded) {
            if (LOGGING)
                Log.d(LOGTAG, "Calling play()");
            loadUrl("javascript:play();");
        }
    }

    public void pause() {
        if (mIsPlayerLoaded) {
            if (LOGGING)
                Log.d(LOGTAG, "Calling pause()");
            loadUrl("javascript:pause();");
        }
    }

    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void onYoutubeFrameLoaded() {
        }

        @JavascriptInterface
        public void onYoutubeFrameReady() {
            if (LOGGING)
                Log.d(LOGTAG, "Frame is ready");
            mIsPlayerLoaded = true;
            post(new Runnable() {
                @Override
                public void run() {
                    if (mBtnPlay != null)
                        mBtnPlay.setVisibility(View.VISIBLE);
                    if (mLoadIndicator != null)
                        mLoadIndicator.setVisibility(View.GONE);
                }
            });
        }

        @JavascriptInterface
        public void onStateChange(final int state) {
            post(new Runnable() {
                @Override
                public void run() {
                    onReceivedStateChange(state);
                }
            });
        }

        @JavascriptInterface
        public void onProgress(final int progress, final int max, final float fractionLoaded) {
            post(new Runnable() {
                @Override
                public void run() {
                    onReceivedProgress(progress, max, fractionLoaded);
                }
            });
        }

        @JavascriptInterface
        public void isCueing(final boolean cueing) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (LOGGING)
                        Log.d(LOGTAG, "isCueing: " + cueing);
                    if (mLoadIndicator != null && cueing) {
                        mLoadIndicator.setVisibility(VISIBLE);
                    }
                    if (mSeekbar != null) {
                        mSeekbar.setVisibility(View.VISIBLE);
                    }
                    if (cueing) {
                        if (mBtnPlay != null)
                            mBtnPlay.setVisibility(cueing ? View.GONE : View.VISIBLE);
                        if (mBtnPause != null)
                            mBtnPause.setVisibility(cueing ? View.VISIBLE : View.GONE);
                    }
                    if (!cueing) {
                        play();
                    }
                }
            });
        }
    }

    private Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            try
            {
                if (!mIsTrackingThumb && mSeekbar != null)
                    loadUrl("javascript:updateProgress();");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (mIsPlaying || mIsPaused)
                postDelayed(mUpdateProgressRunnable, 500);
        }
    };

    private void startProgressThread() {
        removeCallbacks(mUpdateProgressRunnable);
        post(mUpdateProgressRunnable);
    }

    private void onReceivedStateChange(int state) {
        if (LOGGING)
            Log.d(LOGTAG, "State is " + state);
        if (state == 1) {
            if (!mIsPlaying) {
                this.bringToFront();
                if (mMediaController != null)
                    mMediaController.bringToFront();
                if (mLoadIndicator != null)
                    mLoadIndicator.setVisibility(GONE);
                startProgressThread();
                mIsPlaying = true;
            }
            if (mBtnPlay != null)
                mBtnPlay.setVisibility(View.GONE);
            if (mBtnPause != null)
                mBtnPause.setVisibility(View.VISIBLE);
        } else if (state == 5) {
        }
        else if (state != 3 && state != -1) {
            mIsPlaying = false;
            if (mLoadIndicator != null) {
                mLoadIndicator.setVisibility(GONE);
            }
            if (mBtnPlay != null)
                mBtnPlay.setVisibility(View.VISIBLE);
            if (mBtnPause != null)
                mBtnPause.setVisibility(View.GONE);
        }
        mIsPaused = (state == 2);
    }

    private void onReceivedProgress(int progress, int max, float fractionLoaded) {
        if (mSeekbar != null) {
            mSeekbar.setMax(max);
            mSeekbar.setProgress(progress);
            mSeekbar.setSecondaryProgress((int)(fractionLoaded * mSeekbar.getMax()));
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        mIsTrackingThumb = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        loadUrl("javascript:seekTo(" + mSeekbar.getProgress() + ");");
        mIsTrackingThumb = false;
    }
}
