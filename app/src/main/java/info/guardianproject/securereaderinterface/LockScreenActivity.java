package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;

import java.security.GeneralSecurityException;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class LockScreenActivity extends Activity implements LockScreenCallbacks, ICacheWordSubscriber
{
    private static final String LOGTAG = "LockScreenActivity";
	public static final boolean LOGGING = false;
	
	private CacheWordHandler mCacheWord;
	private View mRootView;
	private LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = LayoutInflater.from(this);
		mInflater = inflater.cloneInContext(this);
		LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			 getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
			 WindowManager.LayoutParams.FLAG_SECURE);
		}
		//setContentView(R.layout.lock_screen_return);
		mCacheWord = new CacheWordHandler(this);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		App.getInstance().onLockScreenResumed(this);
        mCacheWord.connectToService();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		App.getInstance().onLockScreenPaused(this);
        mCacheWord.disconnectFromService();
	}

	@Override
	public boolean isInternalActivityOpened()
	{
		return false;
	}

	@Override
	public void setContentView(int layoutResID) 
	{
		mRootView = LayoutInflater.from(this).inflate(layoutResID, null);
		super.setContentView(mRootView);
	}

	private Bitmap takeSnapshot(View view)
	{
		if (view.getWidth() == 0 || view.getHeight() == 0)
			return null;

		view.setDrawingCacheEnabled(true);
		Bitmap bmp = view.getDrawingCache();
		Bitmap bitmap = Bitmap.createBitmap(bmp, 0, 0, view.getWidth(), view.getHeight()).copy(bmp.getConfig(), false);
		view.setDrawingCacheEnabled(false);
		return bitmap;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			// Back from lock screen means quit app. So send a kill signal to
			// any open activity and finish!	
			LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private String getCWPassword() {

		String passphrase = App.getSettings().launchPassphrase();
		if (TextUtils.isEmpty(passphrase)) {
			passphrase = UUID.randomUUID().toString();
			App.getSettings().setLaunchPassphrase(passphrase);
		}
		return passphrase;
	}

	@Override
	public void onCacheWordUninitialized() {
		try {
			if (!App.getInstance().isWiping()) {
				mCacheWord.setPassphrase(getCWPassword().toCharArray());
			}
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCacheWordLocked() {
		try {
			if (!App.getInstance().isWiping()) {
				mCacheWord.setPassphrase(getCWPassword().toCharArray());
			}
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

    @Override
    public void onCacheWordOpened() {
	}
      
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			if (mInflater != null)
				return mInflater;
		}
		return super.getSystemService(name);
	}
	
	public void onUnlocked()
	{
      App.getSettings().setCurrentNumberOfPasswordAttempts(0);

      Intent intent = (Intent) getIntent().getParcelableExtra("originalIntent");
      if (intent == null)
      	intent = new Intent(this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

      //Bitmap snap = takeSnapshot(mRootView);
      //App.getInstance().putTransitionBitmap(snap);

      startActivity(intent);
      finish();
      LockScreenActivity.this.overridePendingTransition(0, 0);
	}
}
