
package com.markiv.images.ui.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.view.CollapsibleActionView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.markiv.images.R;

/**
 * @author vikrambd
 * @since 2/1/15
 */
public class SearchAutoCompleteTextView extends AutoCompleteTextView implements
        CollapsibleActionView {
    private boolean justCleared = false;
    private OnClearListener onClearListener;
    private final Drawable imgClearButton = getResources().getDrawable(
            R.drawable.abc_ic_clear_mtrl_alpha);

    public SearchAutoCompleteTextView(Context context) {
        super(context);
        init();
    }

    /* Required methods, not used in this implementation */
    public SearchAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {
        // Set the bounds of the button
        this.setCompoundDrawablesWithIntrinsicBounds(null, null, imgClearButton, null);

        // if the clear button is pressed, fire up the handler. Otherwise do nothing
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (getCompoundDrawables()[2] == null)
                    return false;

                if (event.getAction() != MotionEvent.ACTION_UP)
                    return false;

                if (event.getX() > getWidth() - getPaddingRight()
                        - imgClearButton.getIntrinsicWidth()) {
                    if(onClearListener != null) {
                        onClearListener.onClear();
                    }
                    justCleared = true;
                }
                return false;
            }
        });
    }

    public void setOnClearListener(final OnClearListener clearListener) {
        this.onClearListener = clearListener;
    }

    public void hideClearButton() {
        this.setCompoundDrawables(null, null, null, null);
    }

    public void showClearButton() {
        this.setCompoundDrawablesWithIntrinsicBounds(null, null, imgClearButton, null);
    }

    @Override
    public void onActionViewExpanded() {
    }

    @Override
    public void onActionViewCollapsed() {
    }

    public static interface OnClearListener {
        void onClear();
    }
}
