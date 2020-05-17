package com.uis.stackview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** 卡片层叠视图，支持随手势滑动动画，自动轮播动画
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup{

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
    public final static int MODEL_TOP = 3;
    public final static int MODEL_BOTTOM = 4;

    private  int stackSize = 3;
    private float aspectRatio = 0;
    private boolean autoPlay = true;
    private boolean enableGesture = true;
    private int stackModel = MODEL_RIGHT;
    private int edge = 0;
    private int paddingX = 10;
    private int offsetX = 0;
    private int paddingY = 10;
    private int offsetY = 2;

    private int startX, startY;
    private VelocityTracker mVelocity;
    private int mMaximumVelocity;
    private int mTouchSlop;
    private Scroller mScroller;
    private ScheduledThreadPoolExecutor executor;
    private StackAdapter adapter;

    public StackLayout(Context context) {
        this(context, null);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray type = context.obtainStyledAttributes(attrs, R.styleable.StackLayout);
        stackSize = type.getInteger(R.styleable.StackLayout_stackSize,stackSize);
        aspectRatio = type.getFloat(R.styleable.StackLayout_stackAspectRatio,aspectRatio);
        autoPlay = type.getBoolean(R.styleable.StackLayout_stackAutoPlay,autoPlay);
        enableGesture = type.getBoolean(R.styleable.StackLayout_stackEnableGesture,enableGesture);
        stackModel = type.getInteger(R.styleable.StackLayout_stackModel,stackModel);
        edge = (int)type.getDimension(R.styleable.StackLayout_stackEdge,edge);
        paddingX = (int)type.getDimension(R.styleable.StackLayout_stackPaddingX,paddingX);
        offsetX = (int)type.getDimension(R.styleable.StackLayout_stackOffsetX,offsetX);
        paddingY = (int)type.getDimension(R.styleable.StackLayout_stackPaddingY,paddingY);
        offsetY = (int)type.getDimension(R.styleable.StackLayout_stackOffsetY,offsetY);
        type.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();
        mScroller = new Scroller(context);
        if(isInEditMode()){

        }
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
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    void layoutChildren(){

    }

    @Override
    public void computeScroll() {

    }

    void startScroll(){

    }

    void endScroll(){

    }

    void resetTouch(){

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setAdapter(StackAdapter adapter) {
        this.adapter = adapter;
        requestLayout();
    }

    public StackAdapter getAdapter(){
        return adapter;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initVelocityTracker();
                startX = (int) event.getX();
                startY = (int) event.getY();

                break;
                //fixed inner view touch
            case MotionEvent.ACTION_MOVE:

                default:
        }
        return true;//super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(adapter == null || adapter.getItemCount() == 0){
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }
        mVelocity.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:
                if(adapter != null && adapter.getItemCount() > 1) {
                    int currentX = (int) event.getX();
                }
                break;
            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                if(adapter != null && adapter.getItemCount() > 1) {
                    mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocity = (int) mVelocity.getXVelocity();

                    recycleVelocityTracker();
                }
                default:
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

    public static abstract class StackAdapter{

        public abstract View onCreateView(ViewGroup parent,int viewType);

        public abstract void onBindView(View view, int position);

        public abstract int getItemCount();

        public int getItemViewType(int position){
            return 0;
        }

        public void onItemDisplay(int position) { }
    }

    void log(String msg){
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        Log.e("StackLayout", String.format("%1$s:%2$s(%3$s):%4$s", element.getClassName(),element.getMethodName(), element.getLineNumber(), msg));
    }
}