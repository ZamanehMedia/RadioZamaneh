package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;


public class NestedScrollView extends ScrollView {
    private float downX;
    private float downY;
    private int mScaledTouchSlop;

    public NestedScrollView(Context context) {
        super(context);
        init();
    }

    public NestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NestedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:

                downX = event.getX();
                downY = event.getY();
                super.onInterceptTouchEvent(event);
                break;

            case MotionEvent.ACTION_MOVE:
                try
                {
                    float xDelta = event.getX() - downX;
                    float yDelta = event.getY() - downY;
                    if (Math.abs(yDelta) > Math.abs(xDelta) && Math.abs(yDelta) >= mScaledTouchSlop)
                    {
                        super.onInterceptTouchEvent(event);
                        return true;
                    }
                }
                catch (Exception e)
                {
                    // nothing
                }
                break;
        }
        return false;
    }
}
