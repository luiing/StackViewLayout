package com.uis.stackview;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.uis.stackview.StackLayout.MODEL_LEFT;
import static com.uis.stackview.StackLayout.MODEL_NONE;
import static com.uis.stackview.StackLayout.MODEL_RIGHT;

/** 布局处理和滚动动画
 * @author uis 2018/11/03
 */

final class StackHelper implements ValueAnimator.AnimatorUpdateListener{

    private long clickMills = System.currentTimeMillis();
    private ScheduledThreadPoolExecutor executor;
    /** 判定为滑动的阈值，单位是像素 */
    private final int mTouchSlop;
    private int swipModel = MODEL_NONE;
    /** true:顶层将移除，false:顶层将加新*/
    private boolean isTopRemove;
    private LinkedList<WeakReference<View>> weakViews = new LinkedList<>();
    private List<Integer> originX = new ArrayList<>();
    private ReleaseAnimator mAnimator = new ReleaseAnimator();
    private int everyWidth;
    private int everyHeight;
    private int mMaxDistance;
    private boolean needRelayout = true;
    private boolean isFingerTouch = false;
    private boolean enableScroll = false;
    private int displayPosition = 0;
    private StackLayout layout;
    private boolean debug = false;

    StackHelper(int touchSlop) {
        mTouchSlop = touchSlop;
    }

    void bindLayout(StackLayout layout) {
        if(this.layout == null) {
            this.layout = layout;
        }
        setAutoPlay(this.layout.stackLooper);
    }

    void unbindLayout() {
        setAutoPlay(false);
    }

    void measureChild(int width,int height){
        //log("measue...");
        if(layout != null && layout.getAdapter() != null && layout.getAdapter().getItemCount() > 0 ){
            int size = layout.getRealStackSize();
            if(originX.isEmpty()) {
                float stackSpaces = 0;
                everyHeight = height - layout.getPaddingTop() - layout.getPaddingBottom();
                originX.add(layout.stackEdge);
                for (int i = 1; i < size; i++) {
                    int stackSpace = (int)(layout.stackSpace /(1+layout.stackZoomX*(size-1-i)));
                    stackSpaces += stackSpace;
                    originX.add(originX.get(i - 1) + stackSpace);
                }
                everyWidth = width - layout.getPaddingLeft() - layout.getPaddingRight() - (int)stackSpaces - 2 * layout.stackEdge;
                mMaxDistance = everyWidth / 3;
            }
            int childSize = layout.getChildCount();
            if(childSize <= 0) {
                needRelayout = true;
                for (int i = childSize; i < size; i++) {
                    View view = getStackView();
                    layout.addView(view);
                    layout.getAdapter().onBindView(view, size - 1 - i);
                }
                displayPosition = 0;
                layout.getAdapter().onItemDisplay(displayPosition);
            }
        }
    }

    void layoutChild(){
        //log("layout..."+layout.getChildCount());
        if(needRelayout && layout != null) {
            needRelayout = false;
            int childSize = layout.getChildCount();
            int stackSize = layout.stackSize;
            for (int i = 0; i < childSize; i++) {
                int top, bottom, left, right, pivot;
                View view = layout.getChildAt(i);
                top = layout.getPaddingTop();
                bottom = everyHeight + top;
                if(i < stackSize) {
                    int x = originX.get(i);
                    if (layout.stackEdgeModel == MODEL_LEFT) {
                        left = x;
                        right = everyWidth + left;
                        pivot = layout.stackEdge;
                    } else {
                        right = layout.getWidth() - x;
                        left = right - everyWidth;
                        pivot = layout.getWidth() - layout.stackEdge;
                    }
                    view.setPivotX(pivot);
                    view.setPivotY(everyHeight / 2);
                    view.layout(left, top, right, bottom);
                    view.setScaleY(getChildScale(i, layout.stackZoomY));
                    if(view.getTranslationX() != 0f){
                        if(i+1 < stackSize){
                            view.setTranslationX(0);
                        }else if(i+1 == stackSize && childSize == stackSize){//恢复stackSize时，补偿距离
                            float distance = layout.stackEdgeModel == MODEL_RIGHT ? -everyWidth : everyWidth;
                            view.setTranslationX(view.getTranslationX()+distance);
                        }
                    }
                }else if(!isTopRemove){//顶层加入
                    if (layout.stackEdgeModel == MODEL_LEFT) {
                        left = layout.getWidth() - layout.stackEdge;
                        right = everyWidth + left;
                    }else{
                        right = layout.stackEdge;
                        left = right - everyWidth;
                    }
                    view.layout(left, top, right, bottom);
                }
            }
        }
    }

    void onItemClicked(){
        if(layout != null){
            layout.getAdapter().onItemClicked(displayPosition);
        }
    }

    void notifyDataChanged(){
        if(layout != null){
            layout.requestLayout();
        }
    }

    void setPosition(int position){
        StackLayout.StackAdapter adapter = layout == null ? null : layout.getAdapter();
        if(displayPosition != position && adapter != null && adapter.getItemCount() > 0){
            position %= adapter.getItemCount();
            displayPosition = position;
            for(int child = layout.getChildCount(),i = child-1; i >= 0; i--){
                adapter.onBindView(layout.getChildAt(i),position++%adapter.getItemCount());
            }
            adapter.onItemDisplay(displayPosition);
        }
    }

    private View getStackView(){
        View view = null;
        for (WeakReference<View> item ;weakViews.size() > 0;){
            item = weakViews.removeLast();
            view = item.get();
            if(view != null){
                break;
            }
        }
        if (view == null && layout != null && layout.getAdapter() != null){
            FrameLayout frame = new FrameLayout(layout.getContext());
            View createView = layout.getAdapter().onCreateView(layout);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(everyWidth, everyHeight);
            frame.addView(createView,params);
            frame.measure(everyWidth,everyHeight);
            view = frame;
        }
        return view;
    }

    private void removeStackView(View view){
        if(view != null && layout != null) {
            layout.removeView(view);
            view.setTranslationX(0);
            view.setScaleY(1f);
            weakViews.add(new WeakReference<>(view));
        }
    }

    private float getChildScale(int index,float zoom) {
        int rate = layout.getRealStackSize() -1 - index;
        return (float) Math.pow(1.0f - zoom,rate);
    }

    /**
     * 在StackLayout是否能水平滑动
     * @return true:拦截滑动，false：放行
     */
    boolean canScroll(float downX, float downY, float x, float y){
        if (!enableScroll) {
            float xDistance = Math.abs(downX - x);
            float yDistance = Math.abs(downY - y);
            if (xDistance > yDistance && xDistance > mTouchSlop) {
                //水平滑动，需要拦截 在RecyclerView中需要禁止父类拦截
                layout.requestParentDisallowInterceptTouchEvent();
                enableScroll = true;
            } else if (yDistance > xDistance && yDistance > mTouchSlop) {
                // 垂直滑动
            }
        }
        return enableScroll;
    }

    void fingerTouchDown(){
        isFingerTouch = true;
        //手指在屏幕停止轮播
        if(executor != null){
            executor.getQueue().clear();
        }
    }

    void executeScroll(int dx) {
        //dx<0 left, dx>0 right
        if(layout.getChildCount() > 0 && !mAnimator.isRunning()) {
            View view;
            //判断滑动方向
            if(MODEL_NONE == swipModel){
                swipModel = dx < 0 ? MODEL_LEFT : MODEL_RIGHT;
                isTopRemove = layout.stackEdgeModel != swipModel;
                if(isTopRemove){
                    view = layout.getChildAt(layout.getChildCount()-1);
                }else{
                    view = addTopView();
                }
                mAnimator.setAnimatorView(view);
            }else{
                view = mAnimator.mAnimatorView;
            }
            view.setTranslationX(view.getTranslationX() + 1.0f*everyWidth/layout.getWidth() * dx);
        }
    }

    void releaseScroll(int velocity){
        isFingerTouch = false;
        enableScroll = false;
        //手指离开屏幕重置轮播状态
        setAutoPlay(layout.stackLooper);
        float transX = mAnimator.getTranslationX();
        if(layout.getAdapter() != null && Math.abs(transX) > 0 && !mAnimator.isRunning()) {
            //滑动速度大于限定或者滑动距离大于宽度一部分，移出当前视图可视范围
            if(Math.abs(velocity) >= everyWidth || Math.abs(transX) > mMaxDistance){
                int sign = MODEL_LEFT == swipModel ? -1 : 1;
                if(isTopRemove){
                    addBottomView();
                    transX = layout.getWidth()*sign - transX;
                    mAnimator.startAnimator(true,false,transX,mMaxDistance,this);
                }else{
                    removeBottomView();
                    transX = everyWidth*sign - transX;
                    mAnimator.startAnimator(false,false,transX,mMaxDistance,this);
                }
            }else {
                //取反恢复原样
                transX *= -1;
                mAnimator.startAnimator(!isTopRemove,false,transX,mMaxDistance,this);
            }
            swipModel = MODEL_NONE;
        }
    }

    void autoScroll(){
        if(layout.getChildCount() > 0 && !isFingerTouch && !mAnimator.isRunning()) {
            mAnimator.setAnimatorView(layout.getChildAt(layout.getChildCount() - 1));
            int transX = (layout.stackEdgeModel == MODEL_LEFT ? 1 : -1)*layout.getWidth();
            isTopRemove = true;
            mAnimator.startAnimator(true,true,transX,mMaxDistance,this);
        }
    }

    void setAutoPlay(boolean looper){
        if(looper) {
            if(executor == null || executor.isShutdown()){
                executor = new ScheduledThreadPoolExecutor(2);
            }
            if(executor.getQueue().size() <= 0) {
                executor.scheduleWithFixedDelay(new AutoRunnable(this), 2, 3, TimeUnit.SECONDS);
            }
        }else {
            if(executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    /** 加入底层 */
    private void addBottomView(){
        int cnt = layout.getAdapter().getItemCount();
        displayPosition += 1;
        displayPosition %= cnt;
        int index = (layout.stackSize - 1 + displayPosition) % cnt;
        View view = getStackView();
        layout.getAdapter().onBindView(view, index);
        needRelayout = true;
        layout.addView(view, 0);
        layout.getAdapter().onItemDisplay(displayPosition);
    }

    /** 加入顶层 */
    private View addTopView(){
        int cnt = layout.getAdapter().getItemCount();
        int position = displayPosition - 1;
        if(position < 0){
            position += cnt;
        }
        View view = getStackView();
        layout.getAdapter().onBindView(view, position);
        needRelayout = true;
        layout.addView(view);
        return view;
    }

    /** 移除底层 */
    private void removeBottomView(){
        int cnt = layout.getAdapter().getItemCount();
        displayPosition -= 1;
        if(displayPosition < 0){
            displayPosition += cnt;
        }
        needRelayout = true;
        View view = layout.getChildAt(0);
        removeStackView(view);
        layout.getAdapter().onItemDisplay(displayPosition);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float animateValue = (float) animation.getAnimatedValue();
        float fraction = animation.getAnimatedFraction();
        mAnimator.setTranslationX(animateValue);
        if (fraction >= 1.0f) {
            mAnimator.endAnimator(this);
        } else if(fraction > 0.2 && mAnimator.needAddBottomView()){
            addBottomView();
        }
    }

    static class ReleaseAnimator{
        private View mAnimatorView;
        private float lastValue = 0f;
        private ValueAnimator animator;
        private Interpolator interpolator = new DecelerateInterpolator(1.2f);
        private Interpolator interpolatorAuto = new LinearInterpolator();
        private boolean needRemoveTopView = false;
        private boolean needAddBottomView;

        void setAnimatorView(View view){
            mAnimatorView = view;
        }

        boolean isRunning(){
            return animator != null && animator.isRunning();
        }

        boolean needAddBottomView(){
            if(needAddBottomView){
                needAddBottomView = false;
                return true;
            }
            return false;
        }

        float getTranslationX(){
            return mAnimatorView == null ? 0f : mAnimatorView.getTranslationX();
        }

        void setTranslationX(float value){
            if(mAnimatorView != null){
                float trans = mAnimatorView.getTranslationX()+value-lastValue;
                mAnimatorView.setTranslationX(trans);
            }
            lastValue = value;
        }

        void startAnimator(boolean needRemoveTop,boolean needAddBottom,float transX, int distance, ValueAnimator.AnimatorUpdateListener listener){
            int duration = needAddBottom ? 400 : (int)(Math.abs(transX)/distance*100);
            needAddBottomView = needAddBottom;
            needRemoveTopView = needRemoveTop;
            animator = ValueAnimator.ofFloat(0f, transX).setDuration(duration);
            animator.setInterpolator(needAddBottom ? interpolatorAuto : interpolator);
            animator.addUpdateListener(listener);
            animator.start();
        }

        void endAnimator(StackHelper helper){
            if(needRemoveTopView) {
                helper.removeStackView(mAnimatorView);
            }
            animator.removeAllUpdateListeners();
            needRemoveTopView = false;
            lastValue = 0f;
            mAnimatorView = null;
            animator = null;
        }
    }

    static class AutoRunnable implements Runnable{
        static Handler mHandler = new Handler(Looper.getMainLooper());
        StackHelper helper;

        AutoRunnable(StackHelper stack) {
            this.helper = stack;
        }

        @Override
        public void run() {
            if(helper.layout != null && helper.layout.getAdapter() != null && helper.layout.getChildCount() > 0){
                final StackLayout stack = helper.layout;
                int[] points = new int[2];
                stack.getLocationInWindow(points);
                if(points[1] < Resources.getSystem().getDisplayMetrics().heightPixels){
                    mHandler.post(new AutoScrollRunnable(helper));
                }
            }
        }
    }

    static class AutoScrollRunnable implements Runnable{
        WeakReference<StackHelper> weakHelper;

        AutoScrollRunnable(StackHelper weakHelper) {
            this.weakHelper = new WeakReference<>(weakHelper);
        }

        @Override
        public void run() {
            StackHelper helper = weakHelper.get();
            if(helper != null) {
                helper.autoScroll();
            }
        }
    }

    boolean filterClick(){
        long nowMills = System.currentTimeMillis();
        if(nowMills - clickMills > 500){
            clickMills = nowMills;
            return true;
        }
        return false;
    }

    void log(String msg){
        if(debug) {
            StackTraceElement element = Thread.currentThread().getStackTrace()[3];
            Log.e("StackLayout", String.format("%1$s:%2$s(%3$s):%4$s", element.getClassName(),
                    element.getMethodName(), element.getLineNumber(), msg));
        }
    }
}
