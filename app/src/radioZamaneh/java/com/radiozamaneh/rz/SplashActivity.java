package com.radiozamaneh.rz;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.MainActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.security.GeneralSecurityException;
import java.util.UUID;

public class SplashActivity extends Activity implements ICacheWordSubscriber
{
	protected int _splashTime = 4000; // time to display the splash screen in ms
	private CacheWordHandler mCacheWord;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		findViewById(R.id.rlRoot).setAlpha(0);
		AnimationHelpers.fadeIn(findViewById(R.id.rlRoot), 500, 0, false, new FadeInFadeOutListener() {
			@Override
			public void onFadeInStarted(View view) {

			}

			@Override
			public void onFadeInEnded(View view) {
				mCacheWord = new CacheWordHandler(SplashActivity.this);
				mCacheWord.connectToService();
			}

			@Override
			public void onFadeOutStarted(View view) {

			}

			@Override
			public void onFadeOutEnded(View view) {

			}
		});
		Thread splashTread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					int waited = 0;
					while (mCacheWord == null || mCacheWord.isLocked() || waited < _splashTime)
					{
						sleep(100);
						waited += 100;
					}
				}
				catch (InterruptedException e)
				{
					// do nothing
				}
				finally
				{
					close();
				}
			}
		};
		splashTread.start();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		if (mCacheWord != null)
			mCacheWord.connectToService();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (mCacheWord != null)
			mCacheWord.disconnectFromService();
	}

	private void close()
	{
		findViewById(R.id.rlRoot).post(new Runnable() {
			@Override
			public void run() {
				getWindow().setBackgroundDrawableResource(R.drawable.background_splash);
				Intent i = new Intent(SplashActivity.this, MainActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(i);
				finish();
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
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
}
