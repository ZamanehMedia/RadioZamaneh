package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.adapters.ShareSpinnerAdapter;
import info.guardianproject.securereaderinterface.widgets.compat.Spinner;
import info.guardianproject.securereaderinterface.R;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class ActionProviderShare extends ActionProvider
{
	private final Context mContext;
	private Spinner mSpinner;
	private ShareSpinnerAdapter mAdapter;
	private boolean mIsForFeedShare;
	private Feed mFeed;
	private Item mItem;

	public ActionProviderShare(Context context, boolean isForFeedShare)
	{
		super(context);
		mContext = context;
		mIsForFeedShare = isForFeedShare;
	}

	@Override
	public View onCreateActionView()
	{
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = inflater.inflate(R.layout.actionbar_spinner_share, null);
		view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		mSpinner = (Spinner) view.findViewById(R.id.spinnerShare);
		mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				ShareSpinnerAdapter adapter = (ShareSpinnerAdapter) parent.getAdapter();
				Intent shareIntent = adapter.getIntentAtPosition(position);
				if (shareIntent != null)
				{
					mContext.startActivity(shareIntent);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
		updateOrCreateAdapter();
		return view;
	}

	@Override
	public boolean hasSubMenu()
	{
		return false;
	}

	private void updateOrCreateAdapter()
	{
		if (mSpinner != null)
		{
			if (mAdapter == null)
			{
				mAdapter = new ShareSpinnerAdapter(mSpinner, mContext, mIsForFeedShare ? R.string.feed_share_popup_title : R.string.story_item_share_popup_title, R.layout.actionbar_spinner_share_item);
				mSpinner.setAdapter(mAdapter);
			}
			mAdapter.clear();
			if (mIsForFeedShare && mFeed != null) {
				Intent shareIntent = App.getInstance().socialReader.getShareIntent(mFeed);
				//mAdapter.addSecureBTShareResolver(shareIntent);
				mAdapter.addIntentResolvers(shareIntent);
			} else if (!mIsForFeedShare && mItem != null) {
				Intent shareIntent = App.getInstance().socialReader.getShareIntent(mItem);
				//mAdapter.addSecureBTShareResolver(shareIntent);
				mAdapter.addIntentResolvers(shareIntent);
			}
			mAdapter.notifyDataSetChanged();
		}
	}
	
	public void setFeed(Feed feed) 
	{
		mFeed = feed;
		updateOrCreateAdapter();
	}

	public void setItem(Item item) {
		mItem = item;
		updateOrCreateAdapter();
	}
}