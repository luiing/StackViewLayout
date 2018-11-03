package com.uis.stackview;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
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

    private static long clickMills = System.currentTimeMillis();
    private ScheduledThreadPoolExecutor executor;
    /** 判定为滑动的阈值，单位是像素 */
    private final int mTouchSlop;
    private int swipModel = MODEL_NONE;
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

    StackHelper(int touchSlop) {
        mTouchSlop = touchSlop;
    }

    void bindLayout(StackLayout layout) {
        //log("bindLayout...");
        if(this.layout == null) {
            this.layout = layout;
        }
        setAutoPlay(this.layout.stackLooper);
    }

    void unbindLayout() {
        //log("unbindLayout...");
        setAutoPlay(false);
    }

    void measureChild(int width,int height){
        log("measue...");
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
        log("layout..."+layout.getChildCount());
        if(needRelayout && layout != null) {
            needRelayout = false;
            //log("childCount="+layout.getChildCount());
            for (int i = 0, size = layout.getChildCount(); i < size && i < layout.stackSize; i++) {
                View view = layout.getChildAt(i);
                int x = originX.get(i);
                int top,bottom,left, right, pivot;
                if (layout.stackEdgeModel == MODEL_LEFT) {
                    left = x;
                    right = everyWidth + x;
                    pivot = layout.stackEdge;
                } else {
                    right = layout.getWidth() - x;
                    left = right - everyWidth;
                    pivot = layout.getWidth() - layout.stackEdge;
                }
                top = layout.getPaddingTop();
                bottom = everyHeight + top;
                left += layout.getPaddingLeft();
                view.setPivotX(pivot);
                view.setPivotY(everyHeight / 2);
                view.layout(left, top, right, bottom);
                if (i < size - 1) {
                    view.setScaleY(getChildScale(i,layout.stackZoomY));
                } else {
                    view.setScaleY(1f);
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
            weakViews.add(new WeakReference<View>(view));
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
            //判断滑动方向
            if(swipModel == MODEL_NONE){
                swipModel = dx < 0 ? MODEL_LEFT : MODEL_RIGHT;
            }
            for (int size = layout.getChildCount(), i = size - 1; i < size; i++) {
                View itemView = layout.getChildAt(i);
                itemView.setTranslationX(itemView.getTranslationX() + 0.8f * dx);
                StackHelper.log("dx = " + dx + ",view transX = " + itemView.getTranslationX());
                mAnimator.setAnimatorView(itemView);
            }
        }
    }

    void releaseScroll(int velocity){
        isFingerTouch = false;
        enableScroll = false;
        //手指离开屏幕重置轮播状态
        setAutoPlay(layout.stackLooper);
        float transX = mAnimator.getTranslationX();
        if(layout.getAdapter() != null && Math.abs(transX) > 0 && !mAnimator.isRunning()) {
            StackHelper.log("end = " + transX);
            //滑动速度大于限定或者滑动距离大于宽度一部分，移出当前视图可视范围
            boolean recover = true;
            if(Math.abs(velocity) >= everyWidth || Math.abs(transX) > mMaxDistance){
                if(MODEL_LEFT == swipModel){
                    if(isRightMoveOutLeft()){
                        addBottomView();
                        recover = false;
                    }else{
                        addTopView();
                    }
                }else if(MODEL_RIGHT == swipModel){
                    if(isLeftMoveOutRight()){
                        addBottomView();
                        recover = false;
                    }else{
                        addTopView();
                    }
                }
                if(!recover) {
                    transX = layout.getWidth() * Math.signum(transX) - transX;
                }
            }
            if(recover){//取反恢复原样
                transX *= -1;
            }
            StackHelper.log("velocity = " + velocity+",end = " + transX + ",distance="+mMaxDistance+ ",needRemoveTopView="+!recover +
                    ",recover=" + recover + ",swipModel =" + swipModel + ",edgeModel=" + layout.stackEdgeModel);
            mAnimator.startAnimator(!recover,false,transX,mMaxDistance,this);
            swipModel = MODEL_NONE;
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

    void autoScroll(){
        if(layout.getChildCount() > 0 && !isFingerTouch && !mAnimator.isRunning()) {
            mAnimator.setAnimatorView(layout.getChildAt(layout.getChildCount() - 1));
            int transX = (layout.stackEdgeModel == MODEL_LEFT ? 1 : -1)*layout.getWidth();
            mAnimator.startAnimator(true,true,transX,mMaxDistance,this);
        }
    }

    private boolean isLeftMoveOutRight(){
        return layout.stackEdgeModel == MODEL_LEFT && swipModel == MODEL_RIGHT;
    }

    private boolean isRightMoveOutLeft(){
        return layout.stackEdgeModel == MODEL_RIGHT && swipModel == MODEL_LEFT;
    }

    /** 移除顶层 */
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
    private void addTopView(){

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
                mAnimatorView.setTranslationX(mAnimatorView.getTranslationX()+value-lastValue);
            }
            lastValue = value;
        }

        void startAnimator(boolean needRemove,boolean needAdd,float transX, int distance, ValueAnimator.AnimatorUpdateListener listener){
            int duration = needAdd ? 500 : (int)(Math.abs(transX)/distance*150);
            needAddBottomView = needAdd;
            needRemoveTopView = needRemove;
            animator = ValueAnimator.ofFloat(0f, transX).setDuration(duration);
            animator.setInterpolator(needAdd ? interpolatorAuto : interpolator);
            animator.addUpdateListener(listener);
            animator.start();
        }

        void endAnimator(StackHelper helper){
            if(needRemoveTopView) {
                helper.removeStackView(mAnimatorView);
                needRemoveTopView = false;
            }
            animator.removeAllUpdateListeners();
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

    static boolean filterClick(){
        long nowMills = System.currentTimeMillis();
        if(nowMills - clickMills > 500){
            clickMills = nowMills;
            return true;
        }
        return false;
    }

    static void log(String msg){
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        Log.e("StackLayout",String.format("%1$s:%2$s(%3$s):%4$s",element.getClassName(),
                element.getMethodName(),element.getLineNumber(),msg));
    }
}
