package com.google.android.apps.nexuslauncher.superg;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;


public abstract class BaseGContainerView extends FrameLayout implements View.OnClickListener {
    private static final String TEXT_ASSIST = "com.google.android.googlequicksearchbox.TEXT_ASSIST";

    private final ArgbEvaluator mArgbEvaluator = new ArgbEvaluator(); //mArgbEvaluator
    private ObjectAnimator mObjectAnimator;
    protected View mQsbView;
    private float mQsbButtonElevation;
    protected QsbConnector mConnectorView;
    private final Interpolator mADInterpolator = new AccelerateDecelerateInterpolator();
    private ObjectAnimator mElevationAnimator; //bJ
    protected boolean qsbHidden;
    private int mQsbViewId = 0;
    protected final Launcher mLauncher;
    private boolean mWindowHasFocus;

    protected abstract int getQsbView(boolean withMic);

    public BaseGContainerView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        mLauncher = Launcher.getLauncher(paramContext);
    }

    public void applyOpaPreference() {
        int qsbViewId = getQsbView(false);
        if (qsbViewId != mQsbViewId) {
            mQsbViewId = qsbViewId;
            if (mQsbView != null) {
                removeView(mQsbView);
            }
            mQsbView = LayoutInflater.from(getContext()).inflate(mQsbViewId, this, false);
            mQsbButtonElevation = (float) getResources().getDimensionPixelSize(R.dimen.qsb_button_elevation);
            addView(mQsbView);
            mObjectAnimator = ObjectAnimator.ofFloat(mQsbView, "elevation", 0f, mQsbButtonElevation).setDuration(200L);
            mObjectAnimator.setInterpolator(mADInterpolator);
            if (qsbHidden) {
                hideQsbImmediately();
            }
            mQsbView.setOnClickListener(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyOpaPreference();
        applyMinusOnePreference();
        applyVisibility();
    }

    private void applyMinusOnePreference() { //bh
        if (mConnectorView != null) {
            removeView(mConnectorView);
            mConnectorView = null;
        }

        if (!mLauncher.useVerticalBarLayout()) {
            mConnectorView = (QsbConnector) mLauncher.getLayoutInflater().inflate(R.layout.qsb_connector, this, false);
            addView(mConnectorView, 0);
        }
    }

    public void onClick(View paramView) {
        getContext().sendOrderedBroadcast(getPillAnimationIntent("com.google.nexuslauncher.FAST_TEXT_SEARCH"),
                null,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (getResultCode() == 0) {
                            startQsbActivity(BaseGContainerView.TEXT_ASSIST);
                        } else {
                            loadWindowFocus();
                        }
                    }
                },
                null,
                0,
                null,
                null);
    }

    private Intent getPillAnimationIntent(String action) {
        int[] qsbLocation = new int[2];
        mQsbView.getLocationOnScreen(qsbLocation);

        Rect rect = new Rect(qsbLocation[0],
                qsbLocation[1],
                qsbLocation[0] + mQsbView.getWidth(),
                qsbLocation[1] + mQsbView.getHeight());

        Intent intent = new Intent(action);
        setGoogleAnimationStart(rect, intent);
        intent.setSourceBounds(rect);
        return intent.putExtra("source_round_left", true)
                .putExtra("source_round_right", true)
                .putExtra("source_logo_offset", midLocation(findViewById(R.id.g_icon), rect))
                .setPackage("com.google.android.googlequicksearchbox")
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private Point midLocation(View view, Rect rect) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        Point point = new Point();
        point.x = (location[0] - rect.left) + (view.getWidth() / 2);
        point.y = (location[1] - rect.top) + (view.getHeight() / 2);
        return point;
    }

    protected void setGoogleAnimationStart(Rect rect, Intent intent) {
    }

    private void loadWindowFocus() {
        if (hasWindowFocus()) {
            mWindowHasFocus = true;
        } else {
            hideQsbImmediately();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean newWindowHasFocus) {
        super.onWindowFocusChanged(newWindowHasFocus);
        if (!newWindowHasFocus && mWindowHasFocus) {
            hideQsbImmediately();
        } else if (newWindowHasFocus && !mWindowHasFocus) {
            changeVisibility(true);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int paramInt) {
        super.onWindowVisibilityChanged(paramInt);
        changeVisibility(false);
    }

    private void hideQsbImmediately() { //bb
        mWindowHasFocus = false;
        qsbHidden = true;
        if (mQsbView != null) {
            mQsbView.setAlpha(0f);
            if (mElevationAnimator != null && mElevationAnimator.isRunning()) {
                mElevationAnimator.end();
            }
        }
        if (mConnectorView != null) {
            if (mObjectAnimator != null && mObjectAnimator.isRunning()) {
                mObjectAnimator.end();
            }
            mConnectorView.setAlpha(0f);
        }
    }

    private void changeVisibility(boolean makeVisible) { //bc
        mWindowHasFocus = false;
        if (qsbHidden) {
            qsbHidden = false;
            if (mQsbView != null) {
                mQsbView.setAlpha(1f);
                if (mElevationAnimator != null) {
                    mElevationAnimator.start();
                    if (!makeVisible) {
                        mElevationAnimator.end();
                    }
                }
            }
            if (mConnectorView != null) {
                mConnectorView.setAlpha(1f);
                mConnectorView.changeVisibility(makeVisible);
            }
        }
    }

    private void startQsbActivity(String action) {
        Context context = getContext();
        try {
            context.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setPackage("com.google.android.googlequicksearchbox"));
        } catch (ActivityNotFoundException ignored) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")),
                        mLauncher.getActivityLaunchOptions(mQsbView));
            } catch (ActivityNotFoundException ignored2) {
            }
        }
    }

    private void applyVisibility() {
        int visibility = View.VISIBLE;

        if (mQsbView != null) {
            mQsbView.setVisibility(visibility);
        }
        if (mConnectorView != null) {
            mConnectorView.setVisibility(visibility);
        }
    }
}