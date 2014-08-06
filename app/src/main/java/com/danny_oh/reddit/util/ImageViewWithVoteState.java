package com.danny_oh.reddit.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

import com.danny_oh.reddit.R;

/**
 * Created by danny on 7/24/14.
 *
 * A custom ImageView with custom states
 */
public class ImageViewWithVoteState extends ImageView implements Checkable {
    private static final int[] STATE_VOTED = {R.attr.submission_vote_state};
    private static final int[] CHECKED_STATE = {android.R.attr.state_checked};

    private boolean mVoted = false;

    private boolean mChecked = false;

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean b) {
        this.mChecked = b;
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    public ImageViewWithVoteState(Context context) {
        super(context);
    }

    public ImageViewWithVoteState(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewWithVoteState(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        // ann 1 extra space to drawable state

        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);

        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE);
        }

        if (mVoted) {
            mergeDrawableStates(drawableState, STATE_VOTED);
        }

        return drawableState;
    }

    public void setStateVoted(boolean voted) {
        if (this.mVoted != voted) {
            this.mVoted = voted;

            refreshDrawableState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
