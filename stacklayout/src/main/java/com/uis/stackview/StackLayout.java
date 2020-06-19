package com.uis.stackview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 卡片层叠视图，支持随手势滑动动画，自动轮播动画
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup implements ValueAnimator.AnimatorUpdateListener{

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
    public final static int MODEL_TOP = 3;
    public final static int MODEL_BOTTOM = 4;
    public final static int MODEL_NONE = 0;

    /** 层叠之间间距 */
    int stackSpace = 10;
    /** 层叠视图边距 */
    int stackEdge = 0;
    /** 层叠递减倍数 >0*/
    float stackZoomX = 1f;
    int stackPadX = 0;
    /** 层叠缩放比例 >0*/
    float stackZoomY = 0.9f;
    /** 层叠显示数量 */
    int stackSize = 3;
    /** 自动轮播 */
    boolean stackLooper = false;
    /** 1 ->left, 2 ->right */
    int stackEdgeModel = MODEL_LEFT;


    /** 局部变量 */
    private int downX, downY;
    private float lastX;
    private int mMaximumVelocity;
    private StackAdapter adapter;
    private VelocityTracker mVelocity;


    private long clickMills = System.currentTimeMillis();
    private ScheduledThreadPoolExecutor executor;
    /** 判定为滑动的阈值，单位是像素 */
    private int mTouchSlop;
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
    private int mDuration = 500;
    private int mDelay = 3000;

    public StackLayout(Context context) {
        this(context, null);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray type = context.obtainStyledAttributes(attrs, R.styleable.StackLayout);
        stackSpace =  (int)type.getDimension(R.styleable.StackLayout_stackSpace, stackSpace);
        stackEdge = (int)type.getDimension(R.styleable.StackLayout_stackEdge,stackEdge);
        stackZoomX = type.getFloat(R.styleable.StackLayout_stackZoomX, stackZoomX);
        stackPadX = (int)type.getDimension(R.styleable.StackLayout_stackPadX,stackPadX);
        stackZoomY = type.getFloat(R.styleable.StackLayout_stackZoomY, stackZoomY);
        stackSize = type.getInteger(R.styleable.StackLayout_stackSize,stackSize);
        stackLooper = type.getBoolean(R.styleable.StackLayout_stackLooper,stackLooper);
        stackEdgeModel = type.getInteger(R.styleable.StackLayout_stackEdgeModel,stackEdgeModel);
        type.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        mTouchSlop = configuration.getScaledTouchSlop();
        mAnimator.setDurations(mDuration);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = MeasureSpec.EXACTLY != MeasureSpec.getMode(heightMeasureSpec) ? width/2
                : getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        if(!isInEditMode() && (adapter == null || adapter.getItemCount() == 0)){
            height = 0;
        }
        setMeasuredDimension(width,height);
        if(adapter != null) {
            measureChild(width,height);
        }
    }

    int getItemCount(){
        int cnt = 0;
        if(getAdapter() != null){
            cnt = getAdapter().getItemCount();
        }
        return cnt;
    }

    void measureChild(int width,int height){
        if(getItemCount() > 0 ){
            int size = getRealStackSize();
            float stackSpaces = 0;
            everyHeight = height - getPaddingTop() - getPaddingBottom();
            originX.add(stackEdge);
            int padX = stackPadX;
            if(padX*(size-1) > stackSpace){
                padX = 0;
            }
            for (int i = 1; i < size; i++) {
                int mstackSpace = (int)((stackSpace-padX*(size-1-i))*Math.pow(stackZoomX,(size-1-i)));
                stackSpaces += mstackSpace;
                originX.add(originX.get(i - 1) + mstackSpace);
            }
            everyWidth = width - getPaddingLeft() - getPaddingRight() - (int)stackSpaces - 2 * stackEdge;
            mMaxDistance = everyWidth / 3;
            int childSize = getChildCount();
            if(childSize <= 0) {
                needRelayout = true;
                for (int i = childSize; i < size; i++) {
                    View view = getStackView();
                    addView(view);
                    getAdapter().onBindView(view, size - 1 - i);
                }
                displayPosition = 0;
                getAdapter().onItemDisplay(displayPosition);
            }
        }
    }

    void setAutoPlay(boolean looper){
        if(looper) {
            if(executor == null || executor.isShutdown()){
                executor = new ScheduledThreadPoolExecutor(1);
            }
            if(executor.getQueue().size() <= 0 && getItemCount() > 1) {
                executor.scheduleWithFixedDelay(new AutoRunnable(this), 2000, mDelay, TimeUnit.MILLISECONDS);
            }
        }else {
            if(executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }

        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(adapter != null) {
            layoutChild();
        }
    }

    void layoutChild(){
        if(needRelayout) {
            needRelayout = false;
            int childSize = getChildCount();
            int stackSize = getRealStackSize();
            for (int i = 0; i < childSize; i++) {
                int top, bottom, left, right, pivot;
                View view = getChildAt(i);
                top = getPaddingTop();
                bottom = everyHeight + top;
                if(i < stackSize) {
                    int x = originX.get(i);
                    if (stackEdgeModel == MODEL_LEFT) {
                        left = x;
                        right = everyWidth + left;
                        pivot = stackEdge;
                    } else {
                        right = getWidth() - x;
                        left = right - everyWidth;
                        pivot = getWidth() - stackEdge;
                    }
                    view.setPivotX(pivot);
                    view.setPivotY(everyHeight/2f);
                    if(!mAnimator.isRunning() ||  0 == i){
                        float scale = (float) Math.pow(stackZoomY,stackSize-1-i);
                        view.setScaleY(scale);
                        view.setTranslationX(0);
                        view.layout(left, top, right, bottom);
                    }
                }else if(!isTopRemove && !mAnimator.isRunning()){//顶层加入
                    if (stackEdgeModel == MODEL_LEFT) {
                        left = getWidth() - stackEdge;
                        right = everyWidth + left;
                    }else{
                        right = stackEdge;
                        left = right - everyWidth;
                    }
                    view.layout(left, top, right, bottom);
                }
            }
        }
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
        mAnimator.cancelAnimator();
    }

    public int getRealStackSize(){
        return adapter == null ? 0 : Math.min(adapter.getItemCount(), stackSize);
    }

    public void setStackSize(int size){
        stackSize = size;
    }

    public void setStackEdgeModel(int model){
        stackEdgeModel = model;
    }

    public void setStackLooper(boolean looper){
        stackLooper = looper;
        setAutoPlay(looper);
    }

    public void setAdapter(StackAdapter adapter) {
        this.adapter = adapter;
        notifyDataChanged();
    }

    public StackAdapter getAdapter(){
        return adapter;
    }

    public void notifyDataChanged(){
        removeAllViews();
        weakViews.clear();
    }

    void onItemClicked(){
        getAdapter().onItemClicked(displayPosition);
    }

    int getPosition(){
        return displayPosition;
    }

    public void setPosition(int position){
        StackLayout.StackAdapter adapter = getAdapter();
        if(displayPosition != position && adapter != null && adapter.getItemCount() > 0){
            position %= adapter.getItemCount();
            displayPosition = position;
            for(int child = getChildCount(),i = child-1; i >= 0; i--){
                adapter.onBindView(getChildAt(i),position++%adapter.getItemCount());
            }
            adapter.onItemDisplay(displayPosition);
        }
    }

    public int getSelectedPosition(){
        return getPosition();
    }

    public void setDuration(int duration){
        mDuration = duration;
        mAnimator.setDurations(mDuration);
    }

    /**
     * looper delay
     * @param mills 1000 is 1 second
     */
    public void setLooperDelay(int mills){
        mDelay = mills;
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
                //fixed inner view touch
            case MotionEvent.ACTION_MOVE:
                return canScroll(downX,downY,event.getX(),event.getY());
                default:
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(adapter == null || adapter.getItemCount() == 0){
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }
        log("action:"+event.getAction()+",downX="+downX+",lastX="+event.getX()+",downY="+downY+",lastY="+event.getY());
        mVelocity.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                fingerTouchDown();
                break;
            case MotionEvent.ACTION_MOVE:
                if(adapter != null && adapter.getItemCount() > 1) {
                    int currentX = (int) event.getX();
                    if (canScroll(downX, downY, event.getX(), event.getY())) {
                        int dx = (int) (currentX - lastX);
                        executeScroll(dx);
                    }
                    lastX = currentX;
                }
                break;
            case MotionEvent.ACTION_UP:
                handleItemClicked();
            case MotionEvent.ACTION_CANCEL:
                if(adapter != null && adapter.getItemCount() > 1) {
                    mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocity = (int) mVelocity.getXVelocity();
                    releaseScroll(velocity);
                    recycleVelocityTracker();
                }
                default:
        }
        return  true;
    }

    private void handleItemClicked(){
        if(adapter != null && downX == (int)lastX){
            if(filterClick()) {
                onItemClicked();
            }
        }
    }

    public void requestParentDisallowInterceptTouchEvent() {
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

    boolean filterClick(){
        long nowMills = System.currentTimeMillis();
        if(nowMills - clickMills > 500){
            clickMills = nowMills;
            return true;
        }
        return false;
    }

    private View getStackView(){
        View view = null;
        for (WeakReference<View> item ;weakViews.size() > 0;){
            item = weakViews.removeLast();
            view = item.get();
            if(view != null){
                view.setTranslationX(0);
                view.setScaleY(1f);
                break;
            }
        }
        if (view == null && getAdapter() != null){
            view = getAdapter().onCreateView(this);
            view.measure(View.MeasureSpec.makeMeasureSpec(everyWidth,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(everyHeight,View.MeasureSpec.EXACTLY));
        }
        return view;
    }

    private void removeStackView(View view){
        if(view != null ) {
            removeView(view);
            weakViews.add(new WeakReference<>(view));
        }
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
                requestParentDisallowInterceptTouchEvent();
                enableScroll = true;
            } else if (yDistance > xDistance && yDistance > mTouchSlop) {
                // 垂直滑动
            }
        }
        return enableScroll && !mAnimator.isRunning();
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
        if(getChildCount() > 0 && !mAnimator.isRunning()) {
            View view;
            //判断滑动方向
            if(MODEL_NONE == swipModel){
                swipModel = dx < 0 ? MODEL_LEFT : MODEL_RIGHT;
                isTopRemove = stackEdgeModel != swipModel;
                if(isTopRemove){
                    view = getChildAt(getChildCount()-1);
                }else{
                    view = addTopView();
                }
                mAnimator.setAnimatorView(view);
            }else{
                view = mAnimator.mAnimatorView;
            }
            int tx = (int)(1.0f*everyWidth/getWidth() * dx);
            view.setTranslationX(view.getTranslationX() + tx);
            scaleTransChild(tx);
        }
    }

    void scaleTransChild(int dx){
        int cnt = getChildCount();
        int stackSize = getRealStackSize();
        int size = Math.min(cnt,stackSize)-(cnt==stackSize ? 1:0);
        int first = cnt > stackSize ? 1 : 0;
        int index = 0;
        for (int i = first; i < size; i++) {
            View v = getChildAt(i);
            int sign = MODEL_LEFT == stackEdgeModel ? 1 : -1;
            float rate = 1f * dx / getWidth();
            int indexX =  index;
            int tx =  originX.get( indexX+1) - originX.get(indexX);
            v.setTranslationX(v.getTranslationX() + rate * tx);
            int indexY = size - index;
            double scaley = Math.pow(stackZoomY, indexY - 1 -first);
            double scale = Math.pow(stackZoomY, indexY-first);
            v.setScaleY(Math.min(v.getScaleY() + (float) (sign * rate * (scaley - scale)),(float) scaley));
            index++;
        }
    }

    void releaseScroll(int velocity){
        isFingerTouch = false;
        enableScroll = false;
        //手指离开屏幕重置轮播状态
        setAutoPlay(stackLooper);
        float transX = mAnimator.getTranslationX();
        if(getAdapter() != null && Math.abs(transX) > 0 && !mAnimator.isRunning()) {
            //滑动速度大于限定或者滑动距离大于宽度一部分，移出当前视图可视范围
            int sign = MODEL_LEFT == swipModel ? -1 : 1;
            /** 加入防抖动 */
            if( (velocity*sign>=0 || Math.abs(velocity)<3*mTouchSlop) && (Math.abs(velocity) >= everyWidth|| Math.abs(transX) > mMaxDistance)){
                if(isTopRemove){
                    addBottomView();
                    transX = getWidth()*sign - transX;
                    mAnimator.startAnimator(true,false,transX,this);
                }else{
                    removeBottomView();
                    transX = everyWidth*sign - transX;
                    //addTop宽度补偿达到平滑过度
                    int offset   = (getWidth()+stackEdge-everyWidth)*sign;
                    scaleTransChild(offset);
                    mAnimator.startAnimator(false,false,transX,this);
                }
            }else {
                //取反恢复原样
                transX *= -1;
                mAnimator.startAnimator(!isTopRemove,false,transX,this);
            }
            swipModel = MODEL_NONE;
        }
    }

    /** 加入底层 */
    private void addBottomView(){
        int cnt = getItemCount();
        int stackSize = getRealStackSize();
        if(getChildCount() < stackSize+1) {
            displayPosition += 1;
            displayPosition %= cnt;
            int index = (stackSize - 1 + displayPosition) % cnt;
            View view = getStackView();
            getAdapter().onBindView(view, index);
            addView(view, 0);
            needRelayout = true;
            getAdapter().onItemDisplay(displayPosition);
        }
    }

    /** 加入顶层 */
    private View addTopView(){
        int cnt = getItemCount();
        int position = displayPosition - 1;
        if(position < 0){
            position += cnt;
        }
        View view = getStackView();
        getAdapter().onBindView(view, position);
        addView(view);
        needRelayout = true;
        return view;
    }

    /** 移除底层 */
    private void removeBottomView(){
        int cnt = getItemCount();
        displayPosition -= 1;
        if(displayPosition < 0){
            displayPosition += cnt;
        }
        View view = getChildAt(0);
        removeStackView(view);
        getAdapter().onItemDisplay(displayPosition);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float animateValue = (float) animation.getAnimatedValue();
        float lastValue = mAnimator.getTranslationX();
        float fraction = animation.getAnimatedFraction();
        mAnimator.setTranslationX(animateValue);
        scaleTransChild((int)(mAnimator.getTranslationX()-lastValue));
        if (fraction >= 1.0f) {
            mAnimator.endAnimator(this);
            requestLayout();
            needRelayout = true;
        } else if(fraction > 0.2 && mAnimator.needAddBottomView()){
            addBottomView();
        }
    }

    static class AutoRunnable implements Runnable{
        WeakReference<StackLayout> weakHelper;

        AutoRunnable(StackLayout stack) {
            weakHelper = new WeakReference<>(stack);
        }

        @Override
        public void run() {
            try {
                StackLayout helper = weakHelper.get();
                if (helper != null && helper.getItemCount() > 1 && helper.getChildCount() > 0) {
                    StackLayout stack = helper;
                    int[] points = new int[2];
                    stack.getLocationInWindow(points);
                    if (points[1] < Resources.getSystem().getDisplayMetrics().heightPixels) {
                        stack.getHandler().post(new AutoScrollRunnable(helper));
                    }
                }
            }catch (Throwable ex){
                ex.printStackTrace();
            }
        }
    }

    void autoScroll(){
        if(getChildCount() > 0 && !isFingerTouch && !mAnimator.isRunning()) {
            mAnimator.setAnimatorView(getChildAt(getChildCount() - 1));
            int transX = (stackEdgeModel == MODEL_LEFT ? 1 : -1)*getWidth();
            isTopRemove = true;
            mAnimator.startAnimator(true,true,transX,this);
        }
    }

    static class AutoScrollRunnable implements Runnable{
        WeakReference<StackLayout> weakHelper;

        AutoScrollRunnable(StackLayout weakHelper) {
            this.weakHelper = new WeakReference<>(weakHelper);
        }

        @Override
        public void run() {
            StackLayout helper = weakHelper.get();
            if(helper != null) {
                helper.autoScroll();
            }
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
        private int duration;
        private int mWidth;

        public ReleaseAnimator() {
            mWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        }

        void setDurations(int duration){
            this.duration = duration;
        }

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

        void startAnimator(boolean needRemoveTop,boolean needAddBottom,float transX, ValueAnimator.AnimatorUpdateListener listener){
            if(animator == null) {
                int mills = needAddBottom ? duration : (int) (1.2 * duration * Math.abs(transX) / mWidth);
                needAddBottomView = needAddBottom;
                needRemoveTopView = needRemoveTop;
                animator = ValueAnimator.ofFloat(0f, transX).setDuration(mills);
                animator.setInterpolator(needAddBottom ? interpolatorAuto : interpolator);
                animator.addUpdateListener(listener);
                animator.start();
            }
        }

        void cancelAnimator(){
            if(animator != null){
                animator.removeAllUpdateListeners();
                animator.cancel();
                endAnimator(null);
            }
        }

        void endAnimator(StackLayout helper){
            if(needRemoveTopView && helper != null) {
                helper.removeStackView(mAnimatorView);
            }
            animator.removeAllUpdateListeners();
            needRemoveTopView = false;
            lastValue = 0f;
            mAnimatorView = null;
            animator = null;
        }
    }

    public static abstract class StackAdapter{

        public abstract View onCreateView(ViewGroup parent);

        public abstract void onBindView(View view, int position);

        public abstract int getItemCount();

        public int getItemViewType(int position){
            return 0;
        }

        public void onItemDisplay(int position) {

        }

        public void onItemClicked(int position){

        }
    }

    static void log(String msg){
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        Log.e("StackLayout", String.format("%1$s:%2$s(%3$s):%4$s", element.getClassName(),element.getMethodName(), element.getLineNumber(), msg));
    }
}