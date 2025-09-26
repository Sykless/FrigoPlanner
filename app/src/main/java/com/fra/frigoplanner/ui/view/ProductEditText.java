package com.fra.frigoplanner.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import java.util.List;

public class ProductEditText extends AppCompatAutoCompleteTextView {

    private boolean focused = false;
    private boolean showCandidates = false;
    private OnBackPressedListener backPressedListener;

    public ProductEditText(Context context) {
        super(context);
    }

    public ProductEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProductEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void setShowCandidates(boolean show) {
        this.showCandidates = show;
    }

    public void clearEditText(View itemView)
    {
        // Hide ProductDico dropdown
        this.dismissDropDown();

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);

        // Give focus to main layout
        this.clearFocus();
        itemView.requestFocus();
    }

    public void setCandidates(List<String> candidates) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                candidates
        );
        setAdapter(adapter);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (backPressedListener != null) {
                backPressedListener.onBackPressed();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean enoughToFilter() {
        return showCandidates && this.focused && super.enoughToFilter();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.backPressedListener = listener;
    }

    public interface OnBackPressedListener {
        void onBackPressed();
    }
}
