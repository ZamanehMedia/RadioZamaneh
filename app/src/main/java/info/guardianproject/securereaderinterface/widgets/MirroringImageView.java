package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class MirroringImageView extends ImageView
{
	public MirroringImageView(Context context)
	{
		super(context);
	}

	public MirroringImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public MirroringImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	public MirroringImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void setImageDrawable(Drawable drawable)
	{
		if (drawable != null && App.getInstance().isRTL())
		{
			drawable = UIHelpers.getMirroringDrawable(getContext(), drawable);
		}
		super.setImageDrawable(drawable);
	}

	@Override
	public void setImageResource(int resId)
	{
		super.setImageResource(resId);
		if (super.getDrawable() != null && App.getInstance().isRTL())
		{
			Drawable d = UIHelpers.getMirroringDrawable(getContext(), super.getDrawable());
			super.setImageDrawable(d);
		}
	}

}
