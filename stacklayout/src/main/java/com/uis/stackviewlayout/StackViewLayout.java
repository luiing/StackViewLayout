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

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
    public final static int MODEL_TOP = 3;
    public final static int MODEL_BOTTOM = 4;

    private  int stackSize = 3;
    private float aspectRatio = 0;
    private boolean autoPlay = true;
    private int stackModel = MODEL_RIGHT;
    private int edge = 0;
    private int paddingX = 10;
    private int offsetX = 0;
    private int paddingY = 10;
    private int offsetY = 2;

    private int startX, startY,current;
    private VelocityTracker mVelocity;
    private int mMaximumVelocity;
    private int mTouchSlop;
    private Scroller mScroller;
    private ScheduledThreadPoolExecutor executor;
    private StackViewAdapter adapter;
    private int mDuration = 500;
    private int mDelay = 3000;
    private int childWidthMeasure,childHeightMeasure;
    private boolean mIsDragged = false;
    private List<Integer> padxArray = new ArrayList<>(stackSize);

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

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();
        mScroller = new Scroller(context);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0,heightMeasureSpec);
        childWidthMeasure = width-2*edge-getSeriesSum(paddingX,offsetX,getOffsetXSize()-1)-getPaddingLeft()-getPaddingRight();
        if(aspectRatio > 0){
            height = (int)(1f*childWidthMeasure/aspectRatio)+getPaddingTop()+getPaddingBottom();
        }
        childHeightMeasure = height;

        setMeasuredDimension(width,height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        addChildView();
        layoutChildView();
    }

    private void layoutChildView(){
        boolean isLeft = MODEL_LEFT == stackModel;
        int count = getChildCount();
        for(int i=0;i<count;i++){
            int left=getPaddingLeft(),right,top=getPaddingTop(),bottom;
            View child = getChildAt(i);

//            int index = count-1-i;//2,1,0
//            int size = MODEL_LEFT == stackModel ? i:index;
//            int start = MODEL_LEFT == stackModel ? paddingX-(index)*offsetX:paddingX;

            if(0 == i){
                int index = count - 3;
                left += edge + getSeriesSum(isLeft ? paddingX-index*offsetX:paddingX, offsetX, isLeft ? i:index);
                right = left + childWidthMeasure;
                top += getSeriesSum(paddingY, offsetY, index);
                bottom = childHeightMeasure - top;
            }else if(count-1 == i){
                left = -childWidthMeasure;
                right = 0;
                bottom = childHeightMeasure - top;
            }else {
                int index = count - 2 - i;
                int size = isLeft ? i-1 : index;
                int start = isLeft ? paddingX - index* offsetX : paddingX;
                left += edge + getSeriesSum(start, offsetX, size);
                right = left + childWidthMeasure;
                top += getSeriesSum(paddingY, offsetY, index);
                bottom = childHeightMeasure - top;
            }
            setChildMeasureDimension(child,right-left,bottom-top);
            child.layout(left,top,right,bottom);
        }
    }

    private void addChildView(){
        int size = getOffsetXSize();
        int cnt = adapter.getItemCount();
        for (int i = getChildCount(); i < size+2; i++) {
            int position = (current+size-i+cnt)%cnt;
            int viewType = adapter.getItemViewType(position);
            View child = adapter.onCreateView(this, viewType);
            addView(child);
            adapter.onBindView(child,position);
            adapter.onPageSelected(current);
        }
    }

    private void addChildPoleView(){

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
            int dy = mScroller.getCurrY();
            scrollTo(0,dy);
            ViewCompat.postInvalidateOnAnimation(this);
        }else{
            endScroll();
        }
    }

    private void scrollDx(int dx){
        int count = getChildCount();
        int i = count-2;
        View child = getChildAt(i);
        int size = MODEL_LEFT == stackModel ? i-1:0;
        int start = paddingX;
        int left = edge+getSeriesSum(start,offsetX,size)+getPaddingLeft()+dx;
        int right = left + childWidthMeasure;
        int top = getPaddingTop();
        int bottom = childHeightMeasure-top;
        child.layout(left,top,right,bottom);
    }

    private void scrollVelocity(int velocity){

    }

    private void startScroll(){
        mScroller.startScroll(getScrollX(),getScrollY(),0,0,mDuration);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void endScroll(){

    }

    private void handleAutoPlay(boolean play){
        if(play) {
            if(executor == null || executor.isShutdown()){
                executor = new ScheduledThreadPoolExecutor(1);
                executor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {

                    }
                }, mDelay, mDelay, TimeUnit.MILLISECONDS);
            }
        }else{
            if(executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    public void setAutoPlay(boolean autoPlay){
        this.autoPlay = autoPlay;
        handleAutoPlay(autoPlay);
    }

    public void setAdapter(StackViewAdapter adapter) {
        this.adapter = adapter;
        requestLayout();
    }

    public StackViewAdapter getAdapter(){
        return adapter;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handleAutoPlay(autoPlay);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handleAutoPlay(false);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        log(" action="+ev.getAction()+",x="+ev.getX()+",y="+ev.getY());
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initVelocityTracker();
                startX = (int) ev.getX();
                startY = (int) ev.getY();
                break;
                //fixed inner view touch
            case MotionEvent.ACTION_MOVE:
                float xDistance = Math.abs(startX - ev.getX());
                float yDistance = Math.abs(startY - ev.getY());
                if (xDistance > yDistance && xDistance > mTouchSlop) {
                    //水平滑动，需要拦截 在RecyclerView中需要禁止父类拦截
                    requestDisallowInterceptTouchEvent(true);
                    mIsDragged = true;
                    return true;
                }
                default:
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        log("action="+ev.getAction()+",x="+ev.getX()+",y="+ev.getY());
        if(adapter == null || adapter.getItemCount() <= 1){
            return false;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            return false;
        }
        mVelocity.addMovement(ev);
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIsDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float xDistance = Math.abs(startX - ev.getX());
                float yDistance = Math.abs(startY - ev.getY());
                if (xDistance > yDistance && xDistance > mTouchSlop) {
                    requestDisallowInterceptTouchEvent(true);
                    mIsDragged = true;
                }
                if(mIsDragged){
                    int currentX = (int) ev.getX();
                    scrollDx(currentX-startX);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) mVelocity.getXVelocity();
                scrollVelocity(velocity);
                recycleVelocityTracker();
                default:
        }
        return  true;
    }

    private void resetTouch(){

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