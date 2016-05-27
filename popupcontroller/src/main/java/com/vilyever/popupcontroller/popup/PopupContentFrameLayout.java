package com.vilyever.popupcontroller.popup;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.vilyever.popupcontroller.animation.AnimationPerformer;
import com.vilyever.popupcontroller.animation.OnAnimationStateChangeListener;
import com.vilyever.resource.Resource;
import com.vilyever.unitconversion.DimenConverter;

/**
 * PopupWindowBackgroundFrameLayout
 * Created by vilyever on 2016/5/26.
 * Feature:
 */
public class PopupContentFrameLayout extends FrameLayout implements View.OnAttachStateChangeListener {
    final PopupContentFrameLayout self = this;
    
    
    /* Constructors */
    public PopupContentFrameLayout(Context context) {
        super(context);
        internalInit();
    }
    
    public PopupContentFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public PopupContentFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        internalInit();
    }
    
    /* Public Methods */
    public void addPopupView(PopupTargetBackgroundView child) {
        child.addOnAttachStateChangeListener(this);
        addView(child);
    }

    public void removePopupView(final PopupTargetBackgroundView child, final RemoveDelegate delegate) {
        if (equals(child.getParent())) {
            if (getDismissPopupAnimationPerformer() != null) {
                getDismissPopupAnimationPerformer().onAnimation(child, new OnAnimationStateChangeListener() {
                    @Override
                    public void onAnimationStart() {

                    }

                    @Override
                    public void onAnimationEnd() {
                        child.removeOnAttachStateChangeListener(self);
                        self.removeView(child);
                        delegate.onRemoveAnimationEnd(self);
                    }

                    @Override
                    public void onAnimationCancel() {

                    }
                });
            }
            else {
                child.removeOnAttachStateChangeListener(this);
                removeView(child);
                delegate.onRemoveAnimationEnd(this);
            }
        }
    }
    
    /* Properties */
    /**
     * 显示popup动画
     */
    private AnimationPerformer showPopupAnimationPerformer;
    public PopupContentFrameLayout setShowPopupAnimationPerformer(AnimationPerformer showPopupAnimationPerformer) {
        this.showPopupAnimationPerformer = showPopupAnimationPerformer;
//        this.showPopupAnimationPerformer = null;
        return this;
    }
    public AnimationPerformer getShowPopupAnimationPerformer() {
        return this.showPopupAnimationPerformer;
    }
    
    /**
     * 隐藏popup动画
     */
    private AnimationPerformer dismissPopupAnimationPerformer;
    public PopupContentFrameLayout setDismissPopupAnimationPerformer(AnimationPerformer dismissPopupAnimationPerformer) {
        this.dismissPopupAnimationPerformer = dismissPopupAnimationPerformer;
//        this.dismissPopupAnimationPerformer = null;
        return this;
    }
    public AnimationPerformer getDismissPopupAnimationPerformer() {
        return this.dismissPopupAnimationPerformer;
    }

    private PopupTargetBackgroundView focusingBackgroundView;
    protected PopupContentFrameLayout setFocusingBackgroundView(PopupTargetBackgroundView focusingBackgroundView) {
        this.focusingBackgroundView = focusingBackgroundView;
        return this;
    }
    protected PopupTargetBackgroundView getFocusingBackgroundView() {
        return this.focusingBackgroundView;
    }
    
    /* Overrides */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            internalLayoutChildrenOffset();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int[] location = new int[2];
        getLocationOnScreen(location);
        int screenX = location[0];
        int screenY = location[1];

        int expectedHeight = Resource.getDisplayMetrics().heightPixels - screenY;

        if (h != expectedHeight) {
            View focusView = findFocus();

            if (focusView != null) {
                View parent = (View) focusView.getParent();
                while (parent != null) {
                    if (parent instanceof PopupTargetBackgroundView) {
                        if (parent == getFocusingBackgroundView()) {
                            return;
                        }

                        if (getFocusingBackgroundView() != null) {
                            internalLayoutChildrenOffset(getFocusingBackgroundView(), -screenY);
                            setFocusingBackgroundView((PopupTargetBackgroundView) parent);
                        }
                        break;
                    }

                    parent = (View) parent.getParent();
                }

                if (getFocusingBackgroundView() != null) {
                    int[] focusingBackgroundViewLocation = new int[2];
                    getFocusingBackgroundView().getLocationOnScreen(focusingBackgroundViewLocation);
                    int focusingBackgroundViewScreenX = focusingBackgroundViewLocation[0];
                    int focusingBackgroundViewScreenY = focusingBackgroundViewLocation[1];

                    int[] focusViewLocation = new int[2];
                    focusView.getLocationOnScreen(focusViewLocation);
                    int focusViewScreenX = focusViewLocation[0];
                    int focusViewScreenY = focusViewLocation[1];

                    int offset = screenY + h - (focusViewScreenY + focusView.getHeight()) - DimenConverter.dpToPixel(8);

                    getFocusingBackgroundView().setY(getFocusingBackgroundView().getWindowY() + offset);
                }
            }

        }
        else {
            if (getFocusingBackgroundView() != null) {
                internalLayoutChildrenOffset(getFocusingBackgroundView(), -screenY);
            }
            else {
                internalLayoutChildrenOffset(-screenY);
            }
        }
    }

    /* Delegates */
    @Override
    public void onViewAttachedToWindow(View v) {
        if (getShowPopupAnimationPerformer() != null) {
            getShowPopupAnimationPerformer().onAnimation(v, null);
        }
    }

    @Override
    public void onViewDetachedFromWindow(View v) {

    }
    
    /* Private Methods */
    private void internalInit() {
        setWillNotDraw(false);

        setFocusableInTouchMode(true);

        setPadding(0, 0, 0, -Resource.getDisplayMetrics().heightPixels);

        setClipChildren(false);
        setClipToPadding(false);
    }

    private void internalLayoutChildrenOffset() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        int screenX = location[0];
        int screenY = location[1];

        internalLayoutChildrenOffset(-screenY);
    }

    private void internalLayoutChildrenOffset(int offsetY) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof PopupTargetBackgroundView && child != getFocusingBackgroundView()) {
                PopupTargetBackgroundView v = (PopupTargetBackgroundView) child;
                internalLayoutChildrenOffset(v, offsetY);
            }
        }
    }

    private void internalLayoutChildrenOffset(PopupTargetBackgroundView child, int offsetY) {
        child.setX(child.getWindowX());
        child.setY(child.getWindowY() + offsetY);
    }

    /* Interfaces */
    public interface RemoveDelegate {
        void onRemoveAnimationEnd(PopupContentFrameLayout frameLayout);
    }
}