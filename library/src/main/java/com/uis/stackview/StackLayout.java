package com.uis.stackview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup implements ValueAnimator.AnimatorUpdateListener{

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
    public final static int MODEL_NONE = 0;
    private final static String TAG = "StackLayout";
    /** 层叠之间间距 */
    private int stackSpace = 10;
    /** 层叠视图边距 */
    private int stackEdge = 0;
    /** 层叠缩放比例 */
    private float stackZoom = 0.1f;
    /** 层叠显示数量 */
    private int stackSize = 3;
    /** 自动轮播 */
    private boolean stackLooper = false;
    /** 1 ->left, 2 ->right */
    private int stackEdgeModel = MODEL_LEFT;
    private int swipModel = MODEL_NONE;

    private int everyWidth;
    private int everyHeight;

    private List<Integer> originX = new ArrayList<>();

    private int downX, downY;
    private float lastX;
    private boolean swipEnable = false;

    private View animatingView;
    private ValueAnimator animator;
    private float animateLastValue;
    private Interpolator interpolator = new DecelerateInterpolator(1.2f);

    private ScheduledThreadPoolExecutor executor;
    private StackAdapter adapter;
    private int displayItemPosition = 0;
    private LinkedList<View> views = new LinkedList<>();
    /** 判定为滑动的阈值，单位是像素 */
    private final int mTouchSlop;
    private VelocityTracker mVelocity;
    private int mMaximumVelocity;
    private int mMaxDistance;

    private boolean needRelayout = true;
    private boolean isAnimRunning = false;
    private boolean isFingerTouch = false;
    private boolean autoPlay = false;
    private boolean canRemoveTopView = false;
    private static long clickMills = System.currentTimeMillis();


    public StackLayout(Context context) {
        this(context, null);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray type = context.obtainStyledAttributes(attrs, R.styleable.stackview);
        stackSpace =  (int)type.getDimension(R.styleable.stackview_stackSpace, stackSpace);
        stackEdge = (int)type.getDimension(R.styleable.stackview_stackEdge,stackEdge);
        stackZoom = type.getFloat(R.styleable.stackview_stackZoom, stackZoom);
        stackSize = type.getInteger(R.styleable.stackview_stackSize,stackSize);
        stackLooper = type.getBoolean(R.styleable.stackview_stackLooper,stackLooper);
        stackEdgeModel = type.getInteger(R.styleable.stackview_stackEdgeModel,stackEdgeModel);
        type.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    public void setStackSize(int size){
        stackSize = size;
    }

    public void setStackEdgeModel(int model){
        stackEdgeModel = model;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setAutoPlay(stackLooper);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setAutoPlay(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        log("onMeasure..." + (adapter == null?"null":adapter.getItemCount()) + ",child="+getChildCount());
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = MeasureSpec.EXACTLY != MeasureSpec.getMode(heightMeasureSpec) ?
                width/2 + getPaddingTop() + getPaddingBottom() :
                getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width,height);
        if(adapter != null && adapter.getItemCount() > 0 ){
            int size = getRealStackSize();
            if(originX.isEmpty()) {
                int stackSpaces = 0;
                everyWidth = width - getPaddingLeft() - getPaddingRight() - stackSpace * (size - 1) - 2 * stackEdge;
                everyHeight = height;
                originX.add(stackEdge);
                for (int i = 1; i < size; i++) {
                    originX.add(originX.get(i - 1) + stackSpace);
                }
                mMaxDistance = everyWidth / 3;
            }
            int childSize = getChildCount();
            if(childSize < size) {
                needRelayout = true;
                for (int i = childSize; i < size; i++) {
                    View view = getStackView();
                    addView(view);
                    adapter.onBindView(view, size - 1 - i);
                }
                log("childSize = " + getChildCount());
                displayItemPosition = 0;
                adapter.onItemDisplay(displayItemPosition);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        log("onLayout....needRelayout=" + needRelayout);
        if(needRelayout) {
            log("onLayout....");
            needRelayout = false;
            for (int i = 0, size = getChildCount(); i < size && i < stackSize; i++) {
                View view = getChildAt(i);
                int x = originX.get(i);
                int left, right, pivot;
                if (stackEdgeModel == MODEL_LEFT) {
                    left = x;
                    right = everyWidth + x;
                    pivot = stackEdge;
                } else {
                    right = getWidth() - x;
                    left = right - everyWidth;
                    pivot = getWidth() - stackEdge;
                }
                view.setPivotX(pivot);
                view.setPivotY(everyHeight / 2);
                view.layout(left, 0, right, everyHeight);
                if (i < size - 1) {
                    setChildScale(i, view);
                } else {
                    view.setScaleY(1f);
                }
            }
        }
    }

    private int getRealStackSize(){
        return adapter.getItemCount() < stackSize ? adapter.getItemCount() : stackSize;
    }

    private View getStackView(){
        View view = null;
        if(views.size() > 0){
            view = views.removeLast();
            view.setTranslationX(0);
        }else if (adapter != null  && adapter.getItemCount() > 0){
            FrameLayout frame = new FrameLayout(getContext());
            View createView = adapter.onCreateView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(everyWidth, everyHeight);
            frame.addView(createView,params);
            view = frame;
            view.measure(everyWidth,everyHeight);
        }
        return view;
    }

    private void removeStackView(View view){
        removeView(view);
        views.add(view);
    }

    private void setChildScale(int index,View itemView) {
        int rate = getRealStackSize() -1 - index;
        float scale = (float) Math.pow(1.0f - stackZoom,rate);
        itemView.setScaleY(scale);
    }

    /**
     * 绑定Adapter
     */
    public void setAdapter(StackAdapter adapter) {
        this.adapter = adapter;
        removeAllViewsInLayout();
        //invalidate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initVelocityTracker();
                downX = (int) event.getX();
                downY = (int) event.getY();
                lastX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                return isSwipEnable(event);
                default:
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocity.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isFingerTouch = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if(isSwipEnable(event)) {
                    int currentX = (int) event.getX();
                    int dx = (int) (currentX - lastX);
                    executeScroll(dx);
                    lastX = currentX;
                }
                break;
            case MotionEvent.ACTION_UP:
                if(downX == (int)lastX && adapter != null){
                    if(filterClick()) {
                        adapter.onItemClicked(displayItemPosition);
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) mVelocity.getXVelocity();
                releaseScroll(velocity);
                isFingerTouch = false;
                swipEnable = false;
                recycleVelocityTracker();
                default:
        }
        return  true;
    }

    /**
     * 在StackLayout是否能水平滑动
     * @param event
     * @return true:拦截滑动，false：放行
     */
    private boolean isSwipEnable(MotionEvent event){
        if (!swipEnable) {
            float xDistance = Math.abs(downX - event.getX());
            float yDistance = Math.abs(downY - event.getY());
            if (xDistance > yDistance && xDistance > mTouchSlop) {
                // 水平滑动，需要拦截
                swipEnable = true;
                //在RecyclerView中需要禁止父类拦截
                requestParentDisallowInterceptTouchEvent();
                return true;
            } else if (yDistance > xDistance && yDistance > mTouchSlop) {
                // 垂直滑动
            }
        }
        return swipEnable;
    }

    public void executeScroll(int dx) {
        //dx<0 left, dx>0 right
        if(!isAnimRunning && getChildCount() > 0) {
            //判断滑动方向
            if(swipModel == MODEL_NONE){
                swipModel = dx < 0 ? MODEL_LEFT : MODEL_RIGHT;
            }
            for (int size = getChildCount(), i = size - 1; i < size; i++) {
                View itemView = getChildAt(i);
                itemView.setTranslationX(itemView.getTranslationX() + 0.8f * dx);
                log("dx = " + dx + ",view transX = " + itemView.getTranslationX());
                animatingView = itemView;
            }
        }
    }

    private void releaseScroll(int velocity){
        if(swipEnable && !isAnimRunning && animatingView != null && adapter != null) {
            float transX = animatingView.getTranslationX();
            log("end = " + transX);
            //滑动速度大于限定或者滑动距离大于宽度一部分，移出当前视图可视范围
            boolean recover = true;
            if(Math.abs(velocity) >= everyWidth || Math.abs(transX) > mMaxDistance){
                if(velocity < 0){
                    if(isRightMoveOut()){
                        moveOutTopView();
                        recover = false;
                    }else{
                        addIntoTopView();
                    }
                }else{
                    if(isLeftMoveOut()){
                        moveOutTopView();
                        recover = false;
                    }else{
                        addIntoTopView();
                    }
                }
                if(!recover) {
                    transX = getWidth() * Math.signum(transX) - transX;
                }
                canRemoveTopView = !recover;
            }
            if(recover){//取反恢复原样
                transX *= -1;
                canRemoveTopView = false;
            }
            log("velocity = " + velocity+",end = " + transX + ",canRemoveTopView="+canRemoveTopView +
                    ",recover=" + recover + ",swipModel =" + swipModel + ",edgeModel=" + stackEdgeModel);
            startEndAnimator(transX);
        }
    }

    private void autoScroll(){
        if(getChildCount() > 0 && !isFingerTouch) {
            swipModel = stackEdgeModel == MODEL_LEFT ? MODEL_RIGHT : MODEL_LEFT;
            animatingView = getChildAt(getChildCount() - 1);
            canRemoveTopView = true;
            autoPlay = true;
            int sign = stackEdgeModel == MODEL_LEFT ? 1 : -1;
            startEndAnimator( sign * getWidth());
        }
    }

    private void startEndAnimator(float transX){
        animateLastValue = 0;
        animator = ValueAnimator.ofFloat(0f, transX);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(this);
        int duration = (int)(Math.abs(transX)/mMaxDistance*120);
        animator.setDuration(duration).start();
    }

    private boolean isLeftMoveOut(){
        return stackEdgeModel == MODEL_LEFT && swipModel == MODEL_RIGHT;
    }

    private boolean isRightMoveOut(){
        return stackEdgeModel == MODEL_RIGHT && swipModel == MODEL_LEFT;
    }

    /** 移除顶层 */
    private void moveOutTopView(){
        int cnt = adapter.getItemCount();
        displayItemPosition += 1;
        displayItemPosition %= cnt;
        int index = (stackSize - 1 + displayItemPosition) % cnt;
        View view = getStackView();
        adapter.onBindView(view, index);
        needRelayout = true;
        addView(view, 0);
        adapter.onItemDisplay(displayItemPosition);
    }

    /** 加入顶层 */
    private void addIntoTopView(){

    }

    public void setStackLooper(boolean looper){
        stackLooper = looper;
        setAutoPlay(looper);
    }

    private void setAutoPlay(boolean looper){
        if(looper) {
            if(executor == null || executor.isShutdown()){
                executor = new ScheduledThreadPoolExecutor(2);
            }
            if(executor.getActiveCount() == 0) {
                executor.scheduleWithFixedDelay(new AutoRunnable(this), 2, 3, TimeUnit.SECONDS);
            }
        }else {
            if(executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        isAnimRunning = true;
        float animateValue = (float) animation.getAnimatedValue();
        animatingView.setTranslationX(animatingView.getTranslationX() + animateValue - animateLastValue);
        animateLastValue = animateValue;
        float fraction = animation.getAnimatedFraction();
        if(fraction > 0.4 && autoPlay){
            autoPlay = false;
            moveOutTopView();
        }
        if (fraction >= 1.0f) {
            animator.removeUpdateListener(this);
            if (canRemoveTopView) {
                removeStackView(animatingView);
            }
            canRemoveTopView = false;
            swipModel = MODEL_NONE;
            animatingView = null;
            animator = null;
            isAnimRunning = false;
        }
    }

    private void requestParentDisallowInterceptTouchEvent() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void initVelocityTracker() {
        if (mVelocity == null) {
            mVelocity = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocity != null) {
            mVelocity.recycle();
            mVelocity = null;
        }
    }

    private static class AutoRunnable implements Runnable{
        static Handler mHandler = new Handler(Looper.getMainLooper());
        WeakReference<StackLayout> stackLayout;

        public AutoRunnable(StackLayout stackLayout) {
            this.stackLayout = new WeakReference<>(stackLayout);
        }

        @Override
        public void run() {
            if(stackLayout != null && stackLayout.get() != null){
                final StackLayout stack = stackLayout.get();
                if(stack != null && stack.adapter != null && stack.getChildCount() > 0){
                    int[] points = new int[2];
                    stack.getLocationInWindow(points);
                    if(points[1] < Resources.getSystem().getDisplayMetrics().heightPixels){
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                stack.autoScroll();
                            }
                        });
                    }
                }
            }
        }
    }

    public static abstract class StackAdapter<T> {

        public abstract View onCreateView(ViewGroup parent);

        public abstract void onBindView(View view, int index);

        public int getItemCount(){
            return 0;
        }

        public void onItemDisplay(int position) {
        }

        public void onItemClicked(int position){

        }
    }

    public static boolean filterClick(){
        long nowMills = System.currentTimeMillis();
        if(nowMills - clickMills > 500){
            clickMills = nowMills;
            return true;
        }
        return false;
    }

    protected static void log(String msg){
        StackTraceElement element = Thread.currentThread().getStackTrace()[4];
        Log.e(TAG,String.format("%1$s:%2$s(%3$s):%4$s",element.getClassName(),
            element.getMethodName(),element.getLineNumber(),msg));
    }
}