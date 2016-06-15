package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import info.guardianproject.securereaderinterface.uiutil.FontManager;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.R;

public class CustomFontTextView extends TextView
{
	private CustomFontTextViewHelper mHelper;
	private boolean mNoTransform;

	public CustomFontTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public CustomFontTextView(Context context)
	{
		this(context, null, 0);
	}

	public CustomFontTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mHelper = new CustomFontTextViewHelper(this, attrs);
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView, defStyle, 0);
			if (a != null) {
				mNoTransform = a.getBoolean(R.styleable.CustomFontTextView_no_transform, false);
				a.recycle();
			}
		}
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		super.setText(mNoTransform ? text : FontManager.transformText(this, text), type);
	}

	@Override
	public void setTextAppearance(Context context, int resid)
	{
		super.setTextAppearance(context, resid);
		mHelper.setTextAppearance(resid);
	}
}
