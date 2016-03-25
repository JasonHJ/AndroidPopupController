package com.vilyever.popupcontroller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.vilyever.popupcontroller.animation.AnimationPerformer;
import com.vilyever.popupcontroller.animation.OnAnimationStateChangeListener;
import com.vilyever.popupcontroller.listener.OnViewAppearStateChangeListener;
import com.vilyever.popupcontroller.listener.OnViewAttachStateChangeListener;
import com.vilyever.popupcontroller.listener.OnViewLayoutChangeListener;

import java.util.ArrayList;

/**
 * ViewController
 * AndroidPopupController <com.vilyever.popupcontroller>
 * Created by vilyever on 2016/3/1.
 * Feature:
 * 视图控制器
 */
public class ViewController {
    final ViewController self = this;


    /* Constructors */
    public ViewController(Context context) {
        setContext(context);
    }

    public ViewController(Context context, int layout) {
        // For generate LayoutParams
        FrameLayout wrapperFrameLayout = new FrameLayout(context);

        View view = LayoutInflater.from(wrapperFrameLayout.getContext())
                                  .inflate(layout, wrapperFrameLayout, false);
        setView(view);
    }

    public ViewController(View view) {
        setView(view);
    }


    /* Public Methods */
    /** {@link #attachToActivity(Activity, boolean)} */
    public <T extends ViewController> T attachToActivity(Activity activity) {
        return attachToActivity(activity, false);
    }

    /**
     * 添加当前controller的view到一个activity的根视图上
     * @param activity
     * @return
     */
    public <T extends ViewController> T attachToActivity(Activity activity, boolean keepMargin) {
        return attachToParent((ViewGroup) activity.getWindow().getDecorView(), keepMargin);
    }

    /** {@link #attachToParent(ViewGroup, boolean)} */
    public <T extends ViewController> T attachToParent(ViewGroup parent) {
        return attachToParent(parent, false);
    }

    /**
     * 添加当前controller的view到一个父view上
     * @param parent 父view
     * @param keepMargin 若当前view的layoutParams有margin属性，是否保持不变
     */
    public <T extends ViewController> T attachToParent(ViewGroup parent, boolean keepMargin) {
        if (getDetachAnimationPerformer() != null) {
            getDetachAnimationPerformer().onAnimationCancel(getView());
        }

        if (getView().getParent() != parent) {
            ViewGroup.MarginLayoutParams preLayoutParams = null;
            if (getView().getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                preLayoutParams = (ViewGroup.MarginLayoutParams) getView().getLayoutParams();
            }

            detachFromParent();

            onViewWillAttachToParent(parent);

            parent.addView(getView());

            if (keepMargin) {
                if (preLayoutParams != null && getView().getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getView().getLayoutParams();
                    layoutParams.setMargins(preLayoutParams.leftMargin, preLayoutParams.topMargin, preLayoutParams.rightMargin, preLayoutParams.bottomMargin);
                }
            }

            setAttachedToParent(true);

            onViewAttachedToParent(parent);
        }

        return (T) this;
    }

    /** {@link #detachFromParent(boolean)} */
    public <T extends ViewController> T detachFromParent() {
        return detachFromParent(true);
    }

    /**
     * 将当前controller的view从父view移除
     * 注意：如果detach动画{@link #detachAnimationPerformer}不为空且animated为true，即detach动画可用，在动画结束前，根视图的parent不会移除当前根视图
     * 注意：如果parent是{@link android.widget.LinearLayout}时，在detach动画未完成前向该parent添加视图会导致显示问题
     * @param animated 是否启用detach动画
     */
    public <T extends ViewController> T detachFromParent(boolean animated) {
        if (animated && isDisappearing()) {
            return (T) this;
        }

        if (getAttachAnimationPerformer() != null) {
            getAttachAnimationPerformer().onAnimationCancel(getView());
        }

        if (getView().getParent() != null) {
            ViewGroup parent = (ViewGroup) getView().getParent();

            onViewWillDetachFromParent(parent);

            onViewWillDisappear();

            if (animated && getDetachAnimationPerformer() != null) {
                getDetachAnimationPerformer().onAnimation(getView(), new OnAnimationStateChangeListener() {
                    @Override
                    public void onAnimationStart() {
                        setDisappearing(true);
                        onDetachAnimationStart();
                    }

                    @Override
                    public void onAnimationEnd() {
                        setDisappearing(false);
                        onDetachAnimationEnd();

                        ViewGroup parent = (ViewGroup) getView().getParent();

                        parent.removeView(getView());

                        setAttachedToParent(false);

                        onViewDetachedFromParent(parent);
                    }

                    @Override
                    public void onAnimationCancel() {
                        setDisappearing(false);
                        onDetachAnimationCancel();
                    }
                });
            }
            else {
                if (getDetachAnimationPerformer() != null) {
                    getDetachAnimationPerformer().onAnimationCancel(getView());
                }

                parent.removeView(getView());

                setAttachedToParent(false);

                onViewDetachedFromParent(parent);
            }
        }
        return (T) this;
    }

    /**
     * 替代controller进入parent显示
     * @param controller 被替代的controller
     */
    public void replaceController(@NonNull ViewController controller) {
        if (controller.isAttachedToParent()) {
            if (controller.getView().getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) controller.getView().getParent();
                parent.removeView(controller.getView());
                attachToParent(parent);
            }
        }
    }

    /**
     * 添加根视图{@link #view}的layout change监听
     * @param listener listener
     * @return this
     */
    public ViewController registerOnViewLayoutChangeListener(OnViewLayoutChangeListener listener) {
        if (!getOnViewLayoutChangeListeners().contains(listener)) {
            getOnViewLayoutChangeListeners().add(listener);
        }
        return this;
    }

    /**
     * 移除根视图{@link #view}的layout change监听
     * @param listener listener
     * @return this
     */
    public ViewController removeOnViewLayoutChangeListener(OnViewLayoutChangeListener listener) {
        getOnViewLayoutChangeListeners().remove(listener);
        return this;
    }

    /**
     * 添加根视图{@link #view}的attach state监听
     * @param listener listener
     * @return this
     */
    public ViewController registerOnViewAttachStateChangeListener(OnViewAttachStateChangeListener listener) {
        if (!getOnViewAttachStateChangeListeners().contains(listener)) {
            getOnViewAttachStateChangeListeners().add(listener);
        }
        return this;
    }

    /**
     * 移除根视图{@link #view}的attach state监听
     * @param listener listener
     * @return this
     */
    public ViewController removeOnViewAttachStateChangeListener(OnViewAttachStateChangeListener listener) {
        getOnViewAttachStateChangeListeners().remove(listener);
        return this;
    }

    /**
     * 添加根视图{@link #view}的appear state监听
     * @param listener listener
     * @return this
     */
    public ViewController registerOnViewAppearStateChangeListener(OnViewAppearStateChangeListener listener) {
        if (!getOnViewAppearStateChangeListeners().contains(listener)) {
            getOnViewAppearStateChangeListeners().add(listener);
        }
        return this;
    }

    /**
     * 移除根视图{@link #view}的appear state监听
     * @param listener listener
     * @return this
     */
    public ViewController removeOnViewAppearStateChangeListener(OnViewAppearStateChangeListener listener) {
        getOnViewAppearStateChangeListeners().remove(listener);
        return this;
    }


    /* Properties */
    private Context context;
    protected <T extends ViewController> T setContext(Context context) {
        this.context = context;
        return (T) this;
    }
    public Context getContext() {
        return context;
    }

    /**
     * controller的根视图
     * 注意：如果controller是由{@link #ViewController(Context, int)}生成的，此时的根视图view存在一个包裹它的FrameLayout
     * 这是由于如果没有一个view用于初始化layout，layout的根视图将无法生成LayoutParams
     * 如果要对LayoutParams，请注意其类型
     */
    private View view;
    protected <T extends ViewController> T setView(View view) {
        if (this.view != null) {
            this.view.removeOnLayoutChangeListener(getInternalOnLayoutChangeListener());
            this.view.removeOnAttachStateChangeListener(getInternalOnAttachStateChangeListener());
        }

        this.view = view;
        setContext(view.getContext());

        view.addOnLayoutChangeListener(getInternalOnLayoutChangeListener());
        view.addOnAttachStateChangeListener(getInternalOnAttachStateChangeListener());

        return (T) this;
    }
    public View getView() {
        return view;
    }

    /**
     * attach动画提供
     */
    private AnimationPerformer attachAnimationPerformer;
    public ViewController setAttachAnimationPerformer(AnimationPerformer attachAnimationPerformer) {
        this.attachAnimationPerformer = attachAnimationPerformer;
        return this;
    }
    public AnimationPerformer getAttachAnimationPerformer() {
        return attachAnimationPerformer;
    }

    /**
     * detach动画提供
     */
    private AnimationPerformer detachAnimationPerformer;
    public ViewController setDetachAnimationPerformer(AnimationPerformer detachAnimationPerformer) {
        this.detachAnimationPerformer = detachAnimationPerformer;
        return this;
    }
    public AnimationPerformer getDetachAnimationPerformer() {
        return detachAnimationPerformer;
    }

    /**
     * OnLayoutChangeListener
     * 用于当前根视图{@link #view}
     */
    private View.OnLayoutChangeListener internalOnLayoutChangeListener;
    private View.OnLayoutChangeListener getInternalOnLayoutChangeListener() {
        if (internalOnLayoutChangeListener == null) {
            internalOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    Rect frame = new Rect(left, top, right, bottom);
                    Rect oldFrame = new Rect(oldLeft, oldTop, oldRight, oldBottom);

                    onViewLayoutChange(frame, oldFrame);
                }
            };
        }
        return internalOnLayoutChangeListener;
    }

    /**
     * OnAttachStateChangeListener
     * 用于当前根视图{@link #view}
     */
    private View.OnAttachStateChangeListener internalOnAttachStateChangeListener;
    protected View.OnAttachStateChangeListener getInternalOnAttachStateChangeListener() {
        if (internalOnAttachStateChangeListener == null) {
            internalOnAttachStateChangeListener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // 由于不禁止从外部直接addChild，这种状况下将不会有关于attach的回调
                    setAttachedToParent(true);

                    onViewWillAppear();

                    if (getAttachAnimationPerformer() != null) {
                        getAttachAnimationPerformer().onAnimation(getView(), new OnAnimationStateChangeListener() {
                            @Override
                            public void onAnimationStart() {
                                setAppearing(true);
                                onAttachAnimationStart();
                            }

                            @Override
                            public void onAnimationEnd() {
                                setAppearing(false);
                                onAttachAnimationEnd();

                                setAppeared(true);
                                onViewAppeared();
                            }

                            @Override
                            public void onAnimationCancel() {
                                setAppearing(false);
                                onAttachAnimationCancel();
                            }
                        });
                    }
                    else {
                        getView().post(new Runnable() {
                            @Override
                            public void run() {
                                setAppeared(true);
                                onViewAppeared();
                            }
                        });
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    setAppeared(false);
                    onViewDisappeared();
                }
            };
        }
        return internalOnAttachStateChangeListener;
    }

    /**
     * OnViewLayoutChangeListener合集
     */
    private ArrayList<OnViewLayoutChangeListener> onViewLayoutChangeListeners;
    protected ArrayList<OnViewLayoutChangeListener> getOnViewLayoutChangeListeners() {
        if (onViewLayoutChangeListeners == null) {
            onViewLayoutChangeListeners = new ArrayList<OnViewLayoutChangeListener>();
        }
        return onViewLayoutChangeListeners;
    }

    /**
     * OnViewAttachStateChangeListener合集
     */
    private ArrayList<OnViewAttachStateChangeListener> onViewAttachStateChangeListeners;
    protected ArrayList<OnViewAttachStateChangeListener> getOnViewAttachStateChangeListeners() {
        if (onViewAttachStateChangeListeners == null) {
            onViewAttachStateChangeListeners = new ArrayList<OnViewAttachStateChangeListener>();
        }
        return onViewAttachStateChangeListeners;
    }

    /**
     * OnViewAppearStateChangeListener合集
     */
    private ArrayList<OnViewAppearStateChangeListener> onViewAppearStateChangeListeners;
    protected ArrayList<OnViewAppearStateChangeListener> getOnViewAppearStateChangeListeners() {
        if (onViewAppearStateChangeListeners == null) {
            onViewAppearStateChangeListeners = new ArrayList<OnViewAppearStateChangeListener>();
        }
        return onViewAppearStateChangeListeners;
    }

    /**
     * 是否添加到了某个parent上
     */
    private boolean attachedToParent;
    protected ViewController setAttachedToParent(boolean attachedToParent) {
        this.attachedToParent = attachedToParent;
        return this;
    }
    public boolean isAttachedToParent() {
        return attachedToParent;
    }

    /**
     * 是否显示在window上
     * 注意：true表明此时根视图{@link #view}的宽高等参数已经计算完成
     */
    private boolean appeared;
    protected ViewController setAppeared(boolean appeared) {
        this.appeared = appeared;
        return this;
    }
    public boolean isAppeared() {
        return appeared;
    }

    /**
     * 是否正在进行attach动画
     */
    private boolean appearing;
    protected ViewController setAppearing(boolean appearing) {
        this.appearing = appearing;
        return this;
    }
    public boolean isAppearing() {
        return appearing;
    }

    /**
     * 是否正在进行detach动画
     */
    private boolean disappearing;
    protected ViewController setDisappearing(boolean disappearing) {
        this.disappearing = disappearing;
        return this;
    }
    public boolean isDisappearing() {
        return disappearing;
    }

    /**
     * 是否显示到window上过
     */
    private boolean alreadyAppeared = false;
    protected ViewController setAlreadyAppeared(boolean alreadyAppeared) {
        this.alreadyAppeared = alreadyAppeared;
        return this;
    }
    protected boolean isAlreadyAppeared() {
        return this.alreadyAppeared;
    }
    
    /* Overrides */
     
     
    /* Delegates */


    /* Protected Methods */

    /**
     * 根视图{@link #view}的Layout发生改变
     * @param frame 当前frame
     * @param oldFrame 改变前的frame
     */
    @CallSuper
    protected void onViewLayoutChange(Rect frame, Rect oldFrame) {

        ArrayList<OnViewLayoutChangeListener> copyList =
                (ArrayList<OnViewLayoutChangeListener>)getOnViewLayoutChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewLayoutChange(this, frame, oldFrame);
        }
    }

    /**
     * 根视图{@link #view}将要添加到parent上
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * @param parent 将要添加到的parent
     */
    @CallSuper
    protected void onViewWillAttachToParent(ViewGroup parent) {

        ArrayList<OnViewAttachStateChangeListener> copyList =
                (ArrayList<OnViewAttachStateChangeListener>)getOnViewAttachStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewWillAttachToParent(this, parent);
        }
    }

    /**
     * 根视图{@link #view}已经添加到parent上
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * @param parent 已经添加到的parent，即根视图{@link #view}当前的parent
     */
    @CallSuper
    protected void onViewAttachedToParent(ViewGroup parent) {

        ArrayList<OnViewAttachStateChangeListener> copyList =
                (ArrayList<OnViewAttachStateChangeListener>)getOnViewAttachStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewAttachedToParent(this, parent);
        }
    }

    /**
     * 根视图{@link #view}将要从当前parent上移除
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * @param parent 根视图{@link #view}当前的parent
     */
    @CallSuper
    protected void onViewWillDetachFromParent(ViewGroup parent) {

        // 在根视图{@link #view}将要脱离window时，若子view持有焦点可能导致键盘不隐藏，故强制隐藏键盘
        if (getView().hasFocus()) {
            getView().clearFocus();
            InputMethodManager imm = (InputMethodManager) getView().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        ArrayList<OnViewAttachStateChangeListener> copyList =
                (ArrayList<OnViewAttachStateChangeListener>)getOnViewAttachStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewWillDetachFromParent(this, parent);
        }
    }

    /**
     * 根视图{@link #view}已经从先前的parent上移除
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * @param parent 根视图{@link #view}先前的parent
     */
    @CallSuper
    protected void onViewDetachedFromParent(ViewGroup parent) {

        ArrayList<OnViewAttachStateChangeListener> copyList =
                (ArrayList<OnViewAttachStateChangeListener>)getOnViewAttachStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewDetachedFromParent(this, parent);
        }
    }

    /**
     * 根视图{@link #view}将要显示在window上
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * 注意：此时根视图{@link #view}已经添加在了window下，只是还未计算宽高等参数
     * 注意：Appear与根视图{@link #view}的{@link View#setVisibility(int)}无关
     */
    @CallSuper
    protected void onViewWillAppear() {

        ArrayList<OnViewAppearStateChangeListener> copyList =
                (ArrayList<OnViewAppearStateChangeListener>)getOnViewAppearStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewWillAppear(this);
        }
    }

    /**
     * 根视图{@link #view}已经显示在window上
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * 注意：此时可以对根视图{@link #view}直接获取getWidth()等
     * 注意：Appear与根视图{@link #view}的{@link View#setVisibility(int)}无关
     */
    @CallSuper
    protected void onViewAppeared() {

        if (!isAlreadyAppeared()) {
            setAlreadyAppeared(true);
            onViewFirstAppeared();
        }

        ArrayList<OnViewAppearStateChangeListener> copyList =
                (ArrayList<OnViewAppearStateChangeListener>)getOnViewAppearStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewAppeared(this);
        }
    }

    /**
     * 第一次显示到window上时的回调
     */
    @CallSuper
    protected void onViewFirstAppeared() {

    }

    /**
     * 根视图{@link #view}将从window上被移除
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * 注意：Disappear与根视图{@link #view}的{@link View#setVisibility(int)}无关
     */
    @CallSuper
    protected void onViewWillDisappear() {

        ArrayList<OnViewAppearStateChangeListener> copyList =
                (ArrayList<OnViewAppearStateChangeListener>)getOnViewAppearStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewWillDisappear(this);
        }
    }

    /**
     * 根视图{@link #view}已经从window上被移除
     * 注意：【Attach、Detach】与【Appear、Disappear】无特定顺序关系
     * 若要处理类似WillDisappear时的事件，通常可以在{@link #onViewWillDetachFromParent(ViewGroup)}中完成
     * 注意：Disappear与根视图{@link #view}的{@link View#setVisibility(int)}无关
     */
    @CallSuper
    protected void onViewDisappeared() {

        ArrayList<OnViewAppearStateChangeListener> copyList =
                (ArrayList<OnViewAppearStateChangeListener>)getOnViewAppearStateChangeListeners().clone();
        int size = copyList.size();
        for (int i = 0; i < size; ++i) {
            copyList.get(i).onViewDisappeared(this);
        }
    }

    /**
     * 根视图{@link #view}attach显示动画开始
     */
    @CallSuper
    protected void onAttachAnimationStart() {

    }

    /**
     * 根视图{@link #view}attach显示动画结束
     */
    @CallSuper
    protected void onAttachAnimationEnd() {

    }

    /**
     * 根视图{@link #view}attach显示动画取消
     */
    @CallSuper
    protected void onAttachAnimationCancel() {

    }

    /**
     * 根视图{@link #view}detach显示动画开始
     */
    @CallSuper
    protected void onDetachAnimationStart() {

    }

    /**
     * 根视图{@link #view}detach显示动画结束
     */
    @CallSuper
    protected void onDetachAnimationEnd() {

    }

    /**
     * 根视图{@link #view}detach显示动画取消
     */
    @CallSuper
    protected void onDetachAnimationCancel() {

    }

    /**
     * findViewById便捷封装
     * @param id 控件id
     * @param <T> 控件类型
     * @return 控件实例
     */
    protected  <T extends View> T findViewById(int id) {
        return (T) getView().findViewById(id);
    }

    /* Private Methods */
}