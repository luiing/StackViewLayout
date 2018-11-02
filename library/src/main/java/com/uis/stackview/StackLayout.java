package com.uis.stackview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup implements ViewTreeObserver.OnGlobalLayoutListener,
        ValueAnimator.AnimatorUpdateListener {

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
    public final static int MODEL_NONE = 0;

    /** 层叠之间间距 */
    private int stackSpace = 30;
    /** 层叠视图边距 */
    private int stackEdge = 0;
    /** 层叠缩放比例 */
    private float stackZoom = 0.2f;
    /** 层叠显示数量 */
    private int stackSize = 3;
    private boolean stackLooper = false;
    /** 1 ->left, 2 ->right */
    private int stackEdgeModel = MODEL_LEFT;
    private int swipModel = MODEL_NONE;

    private int everyWidth;
    private int everyHeight;

    private List<Integer> originX = new ArrayList<>();
    private int originXstart;
    private int originXend;

    private int downX, downY;
    private float lastX;
    private boolean swipEnable = false;
    // 判定为滑动的阈值，单位是像素
    private final int mTouchSlop;

    private View animatingView;
    private ValueAnimator animator;
    private Interpolator interpolator = new DecelerateInterpolator(1.6f);

    private StackAdapter adapter;
    private boolean hasSetAdapter = false;
    private int displayItemPosition = 0;
    private LinkedList<View> views = new LinkedList<>();
    boolean isLayout = false;

    private VelocityTracker mVelocity;
    private int mMaximumVelocity;
    private  int VELOCITY_THRESHOLD = 200;
    private int mMaxDistance;
    private float animateLastValue;
    private boolean isAnimRunning = false;


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
        VELOCITY_THRESHOLD = (int)(getResources().getDisplayMetrics().density * VELOCITY_THRESHOLD);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //views.clear();
    }

    @Override
    public void onGlobalLayout() {
        if (getHeight() > 0 && null != adapter && !hasSetAdapter) {
            setAdapter(adapter);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.e("xx","onMeasure...");
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = MeasureSpec.EXACTLY != MeasureSpec.getMode(heightMeasureSpec) ?
                width/2 + getPaddingTop() + getPaddingBottom() : getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width,height);
        if(adapter != null && adapter.getItemCount() > 0 && originX.isEmpty()){
            int realSize = getRealStackSize();
            everyWidth = width - getPaddingLeft() - getPaddingRight() - stackSpace *(realSize-1) - 2*stackEdge;
            everyHeight = height;
            originXstart = stackEdge;
            originX.add(stackEdge);
            for(int i = 1; i < realSize; i++){
                originX.add(originX.get(i-1) + stackSpace);
            }
            originXend = width;
            mMaxDistance = everyWidth/3;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e("xx","onLayout...."+getChildCount()+",stackSize="+stackSize);
        if(!hasSetAdapter || isLayout){
            return;
        }
        Log.e("xx","onLayout....start stackSize="+stackSize+",total="+getChildCount());
        isLayout = true;
        for (int i = 0, size = getChildCount(); i < size && i < stackSize; i++) {
            View itemView = getChildAt(i);
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
            int top = 0;
            int bottom = top + everyHeight;
            itemView.setPivotX(pivot);
            itemView.setPivotY(everyHeight / 2);
            itemView.layout(left, top, right, bottom);
            if (i < size - 1) {
                adjustScale(i, itemView);
            }else{
                itemView.setScaleY(1f);
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
            FrameLayout frameLayout = new FrameLayout(getContext());
            View createView = adapter.onCreateView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(everyWidth, everyHeight);
            frameLayout.addView(createView,params);
            view = frameLayout;
        }
        return view;
    }

    private void removeStackView(View view){
        removeView(view);
        views.add(view);

    }

    private void adjustScale(int index,View itemView) {
        int rate = getRealStackSize() -1 - index;
        float scale = (float) Math.pow(1.0f - stackZoom,rate);
        itemView.setScaleY(scale);
    }

    /**
     * 绑定Adapter
     */
    public void setAdapter(StackAdapter adapter) {
        Log.e("xx","setAdapter....");
        this.adapter = adapter;
        if ( everyWidth > 0 && everyHeight > 0 && adapter != null && !hasSetAdapter) {
            hasSetAdapter = true;
            for (int i = 0,size = getRealStackSize(); i < size; i++){
                View view = getStackView();
                addView(view);
                adapter.onBindView(view,size-1-i);//size-1-i
            }
            displayItemPosition = 0;
            adapter.onItemDisplay(displayItemPosition);
        }
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

                break;
            case MotionEvent.ACTION_MOVE:
                if(isSwipEnable(event)) {
                    int currentX = (int) event.getX();
                    int dx = (int) (currentX - lastX);
                    computeScroll(dx);
                    lastX = currentX;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                swipEnable = false;
                mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) mVelocity.getXVelocity();
                endScroll(velocity);
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
                requestParentDisallowInterceptTouchEvent(true);
                return true;
            } else if (yDistance > xDistance && yDistance > mTouchSlop) {
                // 垂直滑动
            }
        }
        return swipEnable;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    public void computeScroll(int dx) {
        //dx<0 left, dx>0 right
        if(!isAnimRunning) {
            //判断滑动方向
            if(swipModel == MODEL_NONE){
                swipModel = dx < 0 ? MODEL_LEFT : MODEL_RIGHT;
            }
            for (int size = getChildCount(), i = size - 1; i < size; i++) {
                View itemView = getChildAt(i);
                itemView.setTranslationX(itemView.getTranslationX() + 0.8f * dx);
                Log.e("xx", "dx = " + dx + ",view transX = " + itemView.getTranslationX());
                animatingView = itemView;
            }
        }
    }

    private boolean enableNext = false;

    public void endScroll(int velocity){
        if(!isAnimRunning && animatingView != null) {
            float transX = animatingView.getTranslationX();
            Log.e("xx","end = " + transX);
            //滑动速度大于限定或者滑动距离大于宽度一部分，移出当前视图可视范围
            boolean recover = true;
            if(Math.abs(velocity) >= everyWidth || Math.abs(transX) > mMaxDistance){
                if(velocity < 0){
                    if(isRightMoveOut()){
                        moveOut();
                        recover = false;
                    }else{
                        entryInto();
                    }
                }else{
                    if(isLeftMoveOut()){
                        moveOut();
                        recover = false;
                    }else{
                        entryInto();

                    }
                }
                if(!recover) {
                    transX = getWidth() * Math.signum(transX) - animatingView.getTranslationX();
                }
                enableNext = !recover;
            }
            if(recover){//取反恢复原样
                transX *= -1;
                enableNext = false;
            }
            Log.e("xx","velocity = " + velocity+",end = " + transX + ",next="+enableNext + ",recover=" + recover);
            animateLastValue = 0;
            animator = ValueAnimator.ofFloat(0f, transX);
            animator.setInterpolator(interpolator);
            animator.addUpdateListener(this);
            animator.setDuration(200).start();
        }
    }

    private boolean isLeftMoveOut(){
        return stackEdgeModel == MODEL_LEFT && swipModel == MODEL_RIGHT;
    }

    private boolean isRightMoveOut(){
        return stackEdgeModel == MODEL_RIGHT && swipModel == MODEL_LEFT;
    }

    //从内移出
    private void moveOut(){
        {

            {

                int cnt = adapter.getItemCount();
                displayItemPosition += 1;
                displayItemPosition %= cnt;
                int index = (stackSize - 1 + displayItemPosition) % cnt;
                View view = getStackView();
                adapter.onBindView(view, index);
                isLayout = false;
                addView(view, 0);
                Log.e("xx", "addView....");

            }
        }
    }

    //从外进入
    private void entryInto(){

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        isAnimRunning = true;
        float animateValue = (float)animation.getAnimatedValue();
        animatingView.setTranslationX(animatingView.getTranslationX() + animateValue - animateLastValue);
        animateLastValue = animateValue;
        if(animation.getAnimatedFraction() >= 1.0f){
            animator.removeUpdateListener(this);
            if(enableNext){
                removeStackView(animatingView);
            }
            enableNext = false;
            swipModel = MODEL_NONE;
            animatingView = null;
            isAnimRunning = false;
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

    public static abstract class StackAdapter {

        public abstract View onCreateView(ViewGroup parent);

        public abstract void onBindView(View view, int index);

        public int getItemCount(){
            return 0;
        }

        public void onItemDisplay(int position) {
        }
    }
}