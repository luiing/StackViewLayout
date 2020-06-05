package com.uis.stackviewlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 卡片层叠视图，支持随手势滑动动画，自动轮播
 * @author  uis 2020/6/1
 */

public class StackViewLayout extends ViewGroup{

    public final static int MODEL_LEFT = 0x01;
    public final static int MODEL_RIGHT = 0x02;
    public final static int MODEL_TOP = 0x04;
    public final static int MODEL_BOTTOM = 0x08;

    private  int stackSize = 3;
    private float aspectRatio = 0;
    private boolean autoPlay = true;
    private int stackModel = MODEL_RIGHT;
    private int edge = 0;
    private int paddingX = 10;
    private int offsetX = 0;
    private int paddingY = 10;
    private int offsetY = 2;

    private float initX,lastX,initY, lastY;
    private int current;
    private VelocityTracker mVelocity;
    private int mMaximumVelocity;
    private int mTouchSlop;
    private Scroller mScroller;
    private ScheduledThreadPoolExecutor executor;
    private StackViewAdapter adapter;
    private int mDuration = 600;
    private int mDelay = 3000;
    private int childWidthMeasure,childHeightMeasure;

    private int dragModel;
    private boolean mIsDragged = false;
    private boolean mIsFling = false;

    private List<Integer> xSpace = new ArrayList<>(stackSize);
    private List<Integer> ySpace = new ArrayList<>(stackSize);

    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public StackViewLayout(Context context) {
        this(context, null);
    }

    public StackViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray type = context.obtainStyledAttributes(attrs, R.styleable.StackViewLayout);
        stackSize = type.getInteger(R.styleable.StackViewLayout_stackSize,stackSize);
        aspectRatio = type.getFloat(R.styleable.StackViewLayout_stackAspectRatio,aspectRatio);
        autoPlay = type.getBoolean(R.styleable.StackViewLayout_stackAutoPlay,autoPlay);
        stackModel = type.getInteger(R.styleable.StackViewLayout_stackModel,stackModel);
        edge = (int)type.getDimension(R.styleable.StackViewLayout_stackEdge,edge);
        paddingX = (int)type.getDimension(R.styleable.StackViewLayout_stackPaddingX,paddingX);
        offsetX = (int)type.getDimension(R.styleable.StackViewLayout_stackOffsetX,offsetX);
        paddingY = (int)type.getDimension(R.styleable.StackViewLayout_stackPaddingY,paddingY);
        offsetY = (int)type.getDimension(R.styleable.StackViewLayout_stackOffsetY,offsetY);
        type.recycle();

        boolean isLeft = MODEL_LEFT == stackModel;
        for(int i=0,size=stackSize;i<size;i++){
            int revert = size-1-i;
            int dx = isLeft ? getSeriesSum(paddingX-revert*offsetX,offsetX,i):getSeriesSum(paddingX,offsetX,revert);
            int dy = getSeriesSum(paddingY,offsetY,revert);
            xSpace.add(dx);
            ySpace.add(dy);
        }

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();
        mScroller = new Scroller(context,sInterpolator);
        if(isInEditMode()){
            adapter = new StackViewAdapter() {
                @Override
                public View onCreateView(ViewGroup parent, int viewType) {
                    return new View(parent.getContext());
                }

                @Override
                public void onBindView(View view, int position) {
                    view.setBackgroundColor(Color.LTGRAY);
                }

                @Override
                public int getItemCount() {
                    return 10;
                }
            };
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoPlay();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoPlay(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(width/2,heightMeasureSpec);
        childWidthMeasure = width-2*edge-getSeriesSum(paddingX,offsetX,getOffsetXSize()-1)-getPaddingLeft()-getPaddingRight();
        if(aspectRatio > 0){
            height = (int)(1f*childWidthMeasure/aspectRatio)+getPaddingTop()+getPaddingBottom();
        }
        childHeightMeasure = height;
        setMeasuredDimension(width,height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int size = getOffsetXSize();
        for (int i = getChildCount(); i<size+2 && size>0; i++) {
            int cnt = adapter.getItemCount();
            int position = (current+size-i+cnt)%cnt;
            int viewType = adapter.getItemViewType(position);
            View child = adapter.onCreateView(this, viewType);
            addView(child);
            adapter.onBindView(child,position);
            adapter.onPageSelected(current);
        }
        layoutChildView();
    }

    private void layoutChildView(){
        int count = getChildCount();
        for(int i=0;i<count;i++){
            int left=getPaddingLeft(),right,top=getPaddingTop(),bottom;
            View child = getChildAt(i);
            if(i==count-1){
                boolean isLeft = MODEL_LEFT == stackModel;
                left += isLeft ? getMeasuredWidth():-childWidthMeasure;
            }else {
                int index = Math.max(0, i - 1);
                left += edge + xSpace.get(index);
                top += ySpace.get(index);
            }
            bottom = childHeightMeasure - top;
            right = left + childWidthMeasure;
            setChildMeasureDimension(child,right-left,bottom-top);
            child.layout(left,top,right,bottom);
        }
    }

    private void setChildMeasureDimension(View child,int w,int h){
        child.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    }

    private int getOffsetXSize(){
        return null==adapter ? 0 : Math.min(adapter.getItemCount(),stackSize);
    }

    /** 等差数列求和*/
    private int getSeriesSum(int start,int ratio,int size){
        return size*(2*start-(size-1)*ratio)/2;
    }

    @Override
    public void computeScroll() {
        if(!mScroller.isFinished() && mScroller.computeScrollOffset()){
            int dx = mScroller.getCurrX();
            scrollDx(dx);
            ViewCompat.postInvalidateOnAnimation(this);
        }else{
            nextPage();
        }
    }

    private void scrollDx(int dx){
        int x = dx*childWidthMeasure/getMeasuredWidth();
        int count = getChildCount();
        View child;
        int left;
        if( 0 == (dragModel&stackModel)){
            int i = count-2;
            child = getChildAt(i);
            left = getPaddingLeft()+edge+xSpace.get(i-1)+x;
        }else{
            int i = count-1;
            child = getChildAt(i);
            left = getPaddingLeft()+(MODEL_LEFT == stackModel?getMeasuredWidth():-childWidthMeasure)+x;
        }
        int right = left + childWidthMeasure;
        int top = getPaddingTop();
        int bottom = childHeightMeasure-top;
        child.layout(left,top,right,bottom);
    }

    private void resetTouch(){
        mScroller.abortAnimation();
        stopAutoPlay(false);
    }

    private int getSign(){
        return MODEL_LEFT == stackModel?1:-1;
    }

    private void scrollVelocity(int velocity){
        int count = getChildCount();
        View child;
        int dx;
        int sign;
        if( 0 == (dragModel&stackModel)) {
            int i = count - 2;
            child = getChildAt(i);
            dx = getPaddingLeft()+edge+xSpace.get(i-1)-child.getLeft();
            sign = getSign();
        }else{
            int i = count-1;
            child = getChildAt(i);
            dx = getPaddingLeft()+(MODEL_LEFT == stackModel?getMeasuredWidth():-childWidthMeasure)-child.getLeft();
            sign = -getSign();
        }
        if(velocity*sign>getMeasuredWidth() || -dx*sign>0.45*childWidthMeasure){
            int dnx = dx+sign*(getMeasuredWidth()+edge);
            startScroll(-dx,dnx);
            mIsFling = true;
        }else {
            startScroll(-dx, dx);
        }
        startAutoPlay();
    }

    private void autoScroll(){
        if(mScroller.isFinished() && adapter!=null && adapter.getItemCount()>1){
            dragModel = MODEL_LEFT==stackModel ? MODEL_RIGHT:MODEL_LEFT;
            scrollVelocity(2*getMeasuredWidth()*getSign());
        }
    }

    private void nextPage(){
        if(mIsFling) {
            mIsFling = false;
            int cnt = adapter.getItemCount();
            if (0 == (dragModel & stackModel)){
                current = (current+1)%cnt;
                int position = (current+stackSize)%cnt;
                int viewType = adapter.getItemViewType(position);
                View child = adapter.onCreateView(this, viewType);
                addView(child,0);
                removeViewAt(getChildCount() - 1);
                adapter.onBindView(child,position);
                adapter.onPageSelected(current);
            } else {
                current = (current-1+cnt)%cnt;
                removeViewAt(0);
            }
        }
    }

    private void startScroll(int begin,int dx){
        int mills = Math.max(mDuration*Math.abs(dx)/getMeasuredWidth(),mDuration/2);
        mScroller.startScroll(begin,0,dx,0,mills);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void startAutoPlay(){
        if(autoPlay) {
            if (executor == null || executor.isShutdown()) {
                executor = new ScheduledThreadPoolExecutor(1);
            }
            if (executor.getQueue().size() <= 0) {
                executor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        autoScroll();
                    }
                }, mDelay, mDelay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void stopAutoPlay(boolean force){
        if(force) {
            if(executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
                executor = null;
            }
        }else{
            if(executor != null){
                executor.getQueue().clear();
            }
        }
    }

    public void setAutoPlay(boolean autoPlay){
        this.autoPlay = autoPlay;
        if(autoPlay){
            startAutoPlay();
        }else{
            stopAutoPlay(false);
        }
    }

    public void setAdapter(StackViewAdapter adapter) {
        this.adapter = adapter;
        requestLayout();
    }

    public StackViewAdapter getAdapter(){
        return adapter;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        log(" action="+ev.getAction()+",x="+ev.getX()+",y="+ev.getY()+",slop="+mTouchSlop);
        if(adapter == null || adapter.getItemCount() <= 1){
            return false;
        }
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                resetTouch();
                mIsDragged = false;
                initVelocityTracker();
                lastX = initX = ev.getX();
                lastY = initY = ev.getY();
                break;
                //fixed inner view touch
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - lastX;
                float xDistance = Math.abs(dx);
                float yDistance = Math.abs(lastY - ev.getY());
                if (xDistance > mTouchSlop && xDistance/2 > yDistance) {
                    //水平滑动，需要拦截 在RecyclerView中需要禁止父类拦截
                    requestDisallowInterceptTouchEvent(true);
                    dragModel = dx > 0 ? MODEL_RIGHT : MODEL_LEFT;
                    lastX =  dx > 0 ? initX+mTouchSlop : initX-mTouchSlop;
                    mIsDragged = true;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        log(" action="+ev.getAction()+",x="+ev.getX()+",y="+ev.getY());
        if(adapter == null || adapter.getItemCount() <= 1){
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            return false;
        }
        mVelocity.addMovement(ev);
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if(!mIsDragged) {
                    float dx = ev.getX()-lastX;
                    float xDistance = Math.abs(dx);
                    float yDistance = Math.abs(ev.getY() - lastY);
                    if (xDistance > yDistance && xDistance > mTouchSlop) {
                        requestDisallowInterceptTouchEvent(true);
                        mIsDragged = true;
                        dragModel = dx > 0 ? MODEL_RIGHT : MODEL_LEFT;
                        lastX =  dx > 0 ? initX+mTouchSlop : initX-mTouchSlop;
                    }
                }
                if(mIsDragged){
                    scrollDx((int)(ev.getX()-lastX));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) mVelocity.getXVelocity();
                scrollVelocity(velocity);
                recycleVelocityTracker();
        }
        return  true;
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

    public static abstract class StackViewAdapter{

        public abstract View onCreateView(ViewGroup parent,int viewType);

        public abstract void onBindView(View view, int position);

        public abstract int getItemCount();

        public int getItemViewType(int position){ return 0; }

        public void onPageSelected(int position) { }
    }

    void log(String msg){
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        Log.e("StackLayout", String.format("%1$s:%2$s(%3$s):%4$s", element.getClassName(),element.getMethodName(), element.getLineNumber(), msg));
    }
}