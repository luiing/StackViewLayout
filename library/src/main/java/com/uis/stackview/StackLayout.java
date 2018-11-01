package com.uis.stackview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.Scroller;

import com.stone.pile.libs.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xmuSistone on 2017/5/12. com.stone.pile.libs.PileLayout
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup implements ViewTreeObserver.OnGlobalLayoutListener {

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;

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


    private int everyWidth;
    private int everyHeight;

    private List<Integer> originX = new ArrayList<>();

    private int downX, downY;
    private float lastX;
    private boolean swipEnable = false;
    // 判定为滑动的阈值，单位是像素
    private final int mTouchSlop;

    private float animateValue;
    private ObjectAnimator animator;
    private Interpolator interpolator = new DecelerateInterpolator(1.6f);
    private StackAdapter adapter;
    private boolean hasSetAdapter = false;
    private FrameLayout animatingView;
    private VelocityTracker mVelocity;


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
        getViewTreeObserver().addOnGlobalLayoutListener(this);
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
            originX.add(width);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e("xx","onLayout....");
        for (int i = 0, size = getChildCount(); i < size; i++) {
            View itemView = getChildAt(i);
            itemView.measure(everyWidth, everyHeight);
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
            if (i < size - 2) {
                adjustScale(i, itemView);
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
            for (int i = 0,size = getRealStackSize() + 2; i < size; i++) {
                FrameLayout frameLayout = new FrameLayout(getContext());
                View view = adapter.onCreateView(frameLayout);
                FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(everyWidth, everyHeight);
                frameLayout.addView(view, lp1);
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
    public void setAdapter(StackAdapter adapter) {
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
            adapter.onBindView(frameLayout.getChildAt(0),getRealIndex(size,i));//size-1-i
        }
    }

    private int getRealIndex(int size,int index){
        int count = adapter.getItemCount();
        int real;
        //底层
        if(0 == index){
            real = count > size ? size : 0;
        }//顶层
        else if(size+1 == index){
            real = count - 1;
        }//中间层
        else{
            real = size - index;
        }
        return real;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // 决策是否需要拦截
        int action = event.getActionMasked();
        Log.e("xx","onInterceptTouchEvent="+action);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                downY = (int) event.getY();
                lastX = event.getX();
                if (null != animator) {
                    animator.cancel();
                }
                animatingView = null;
                break;
            case MotionEvent.ACTION_MOVE:
                return isSwipEnable(event);
                default:
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e("xx","onTouchEvent="+event.getActionMasked());
        int action = event.getActionMasked();
        switch (action) {
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
                default:
                    swipEnable = false;

        }

        return  true;
    }

    /**
     * 在StackLayout是否能水平滑动
     * @param event
     * @return
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
        for (int i = 3,size = 2 + getRealStackSize(); i < size; i++) {
            View itemView = getChildAt(i);
            itemView.setTranslationX(itemView.getTranslationX() + dx);
        }
    }

    private void onRelease(float eventX, int velocityX) {

        animator = ObjectAnimator.ofFloat(this, "animateValue", animatingView.getLeft(), 0);
        animator.setInterpolator(interpolator);
        animator.setDuration(300).start();
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

    public void setAnimateValue(float animateValue) {
        // 当前应该在的位置
        this.animateValue = animateValue;
        int dx = Math.round(animateValue - animatingView.getLeft());
        computeScroll(dx);
    }

    public float getAnimateValue() {
        return animateValue;
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