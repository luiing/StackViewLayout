package com.uis.stackview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.stone.pile.libs.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xmuSistone on 2017/5/12. com.stone.pile.libs.PileLayout
 * @author  uis 2018/10/30
 */

public class StackViewLayout extends ViewGroup implements View.OnClickListener,ViewTreeObserver.OnGlobalLayoutListener {

    private final int mMaximumVelocity;

    /** 层叠之间间距 */
    private int stackSpace = 30;
    private int stackEdge = 0;
    /** 层叠缩放比例 */
    private float stackZoom = 0.2f;
     /** 显示数量 */

    /** 层叠显示数量 */
    private int stackSize = 3;
    private boolean stackLooper = false;
    /** 1 ->left, 2 ->right */
    private int stackEdgeModel = 1;

    private int everyWidth;
    private int everyHeight;
    private int offsetIndex = 0;

    private List<Integer> originX = new ArrayList<>();

    // 拖拽相关
    private static final int MODE_IDLE = 0;
    private static final int MODE_HORIZONTAL = 1;
    private static final int MODE_VERTICAL = 2;
    //速度阀值
    private static final int VELOCITY_THRESHOLD = 200;
    private int scrollMode;
    private int downX, downY;
    private float lastX;
    // 判定为滑动的阈值，单位是像素
    private final int mTouchSlop;

    private float animateValue;
    private ObjectAnimator animator;
    private Interpolator interpolator = new DecelerateInterpolator(1.6f);
    private StackViewAdapter adapter;
    private boolean hasSetAdapter = false;
    private FrameLayout animatingView;
    private VelocityTracker mVelocityTracker;

    public StackViewLayout(Context context) {
        this(context, null);
    }

    public StackViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void onClick(View v) {
        if (null != adapter) {
            int position = offsetIndex;
            if (position >= 0 && position < adapter.getItemCount()) {
                adapter.onItemClick(((FrameLayout) v).getChildAt(0), position);
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        if (getHeight() > 0 && null != adapter && !hasSetAdapter) {
            setAdapter(adapter);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.e("xx","onMeasure....");
        //计算宽度，高度设置为宽度的比例
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = MeasureSpec.EXACTLY != MeasureSpec.getMode(heightMeasureSpec) ?
                width/2 + getPaddingTop() + getPaddingBottom() : getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width,height);
        if(adapter != null && adapter.getItemCount() > 0 && originX.isEmpty()){
            int realSize = getRealStackSize();
            everyWidth = width - getPaddingLeft() - getPaddingRight() - stackSpace *(realSize-1) - 2*stackEdge;
            everyHeight = height;
            originX.add(stackEdge);
            originX.add(stackEdge);
            for(int i = 1; i < realSize; i++){
                originX.add(originX.get(i) + stackSpace);
            }
            originX.add(width-1);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e("xx","onLayout....");
        for (int i = 0,size = getChildCount(); i < size; i++) {
            View itemView = getChildAt(i);
            itemView.measure(everyWidth,everyHeight);

            int x = originX.get(i);
            int left,right,pivot;
            if(stackEdgeModel == 1) {
                left = x;
                right = everyWidth + x;
                pivot = stackEdge;
            }else{
                right = getWidth() - x;
                left = right - everyWidth;
                pivot = getWidth() - stackEdge;
            }
            int top = 0;
            int bottom = top + everyHeight;
            itemView.setPivotX(pivot);
            itemView.setPivotY(everyHeight / 2);
            itemView.layout(left, top, right, bottom);
            if(i < size - 2) {
                adjustScale(i,itemView);
            }
        }
    }

    private int getRealStackSize(){
        return adapter.getItemCount() < stackSize ? adapter.getItemCount() : stackSize;
    }

    private void initAdapterView(){
        Log.e("xx","initAdapterview start....");
        if (adapter != null && getChildCount() == 0 && adapter.getItemCount() > 0) {
            hasSetAdapter = true;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0,size = getRealStackSize() + 2; i < size; i++) {
                FrameLayout frameLayout = new FrameLayout(getContext());
                View view = inflater.inflate(adapter.getLayoutId(), null);
                FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(everyWidth, everyHeight);
                frameLayout.addView(view, lp1);
                frameLayout.setOnClickListener(this);
                addView(frameLayout);
            }

        }
    }

    private void adjustScale(int index,View itemView) {
        int rate = getRealStackSize() - (index <= 1 ? 1: index);
        float scale = (float) Math.pow(1.0f - stackZoom,rate);
        itemView.setScaleX(1.0f - stackZoom);
        itemView.setScaleY(scale);
    }

    /**
     * 绑定Adapter
     */
    public void setAdapter(StackViewAdapter adapter) {
        Log.e("xx","setAdapter....");
        this.adapter = adapter;
        // ViewdoBindAdapter尚未渲染出来的时候，不做适配
        if ( everyWidth > 0 && everyHeight > 0) {
            doBindAdapter();
        }
    }


    /**
     * 真正绑定Adapter
     */
    private void doBindAdapter() {
        if(adapter != null && !hasSetAdapter){
            initAdapterView();
            doBindAdapterData();
        }
    }

    private void doBindAdapterData(){
        int size = getRealStackSize();
        FrameLayout frameLayout;
        for (int i = 0; i < size + 2; i++) {
            frameLayout = (FrameLayout) getChildAt(i);
            adapter.bindView(frameLayout.getChildAt(0),getRealIndex(size,i));//size-1-i
            //Log.e("xx","doBind....");
        }
    }

    private int getRealIndex(int size,int index){
        int count = adapter.getItemCount();
        int real;
        if(0 == index){//底层
            real = count > size ? size : 0;
        }else if(size+1 == index){//顶层
            real = count - 1;
        }else{//中间层
            real = size - index;
        }
        return real;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // 决策是否需要拦截
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                downY = (int) event.getY();
                lastX = event.getX();
                scrollMode = MODE_IDLE;
                if (null != animator) {
                    animator.cancel();
                }
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(event);
                animatingView = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (scrollMode == MODE_IDLE) {
                    float xDistance = Math.abs(downX - event.getX());
                    float yDistance = Math.abs(downY - event.getY());
                    if (xDistance > yDistance && xDistance > mTouchSlop) {
                        // 水平滑动，需要拦截
                        scrollMode = MODE_HORIZONTAL;
                        return true;
                    } else if (yDistance > xDistance && yDistance > mTouchSlop) {
                        // 垂直滑动
                        scrollMode = MODE_VERTICAL;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                recycleVelocityTracker();
                // ACTION_UP还能拦截，说明手指没滑动，只是一个click事件，同样需要snap到特定位置
                //onRelease(event.getX(), 0);
                break;
                default:
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int currentX = (int) event.getX();
                int dx = (int) (currentX - lastX);
                requireScrollChange(dx);
                lastX = currentX;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) velocityTracker.getXVelocity();
                recycleVelocityTracker();

                //onRelease(event.getX(), velocity);
                break;
            default:
        }
        return true;
    }

    private void onRelease(float eventX, int velocityX) {

//        animator = ObjectAnimator.ofFloat(this, "animateValue", animatingView.getLeft(), 0);
//        animator.setInterpolator(interpolator);
//        animator.setDuration(300).start();
    }

    private void requireScrollChange(int dx) {
        if(getChildCount() == 0){
            return;
        }
        //dx<0 left, dx>0 right
        for (int i = 0,size = 2 + getRealStackSize(); i < size; i++) {
            Log.e("x",size+",position="+i+",dx="+dx);
            View itemView = getChildAt(i);
            itemView.offsetLeftAndRight(dx);
            //adapter.bindView(itemView,getRealIndex(size-2,i));
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    /**
     * 属性动画，请勿删除
     */
    public void setAnimateValue(float animateValue) {
        // 当前应该在的位置
        this.animateValue = animateValue;
        int dx = Math.round(animateValue - animatingView.getLeft());
        requireScrollChange(dx);
    }

    public float getAnimateValue() {
        return animateValue;
    }

    /**
     * 适配器
     */
    public static abstract class StackViewAdapter {

        /**
         * layout文件ID，调用者必须实现
         */
        public abstract int getLayoutId();

        /**
         * item数量，调用者必须实现
         */
        public abstract int getItemCount();

        /**
         * View与数据绑定回调，可重载
         */
        public void bindView(View view, int index) {
        }

        /**
         * 正在展示的回调，可重载
         */
        public void displaying(int position) {
        }

        /**
         * item点击，可重载
         */
        public void onItemClick(View view, int position) {
        }
    }
}