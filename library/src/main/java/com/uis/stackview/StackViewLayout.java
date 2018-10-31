package com.uis.stackview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
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
            int position = offsetIndex + 2;
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
        //计算宽度，高度设置为宽度的比例
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = MeasureSpec.EXACTLY != MeasureSpec.getMode(heightMeasureSpec) ?
                width/2 + getPaddingTop() + getPaddingBottom() : getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width,height);
        if(adapter != null && adapter.getItemCount() > 0){
            int realSize = getRealStackSize();
            everyWidth = (int) ((width - getPaddingLeft() - getPaddingRight() - stackSpace *(realSize-1) - 2*stackEdge) );
            everyHeight = height;
            originX.clear();
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
        for (int i = 0,size = getChildCount(); i < size; i++) {
            View itemView = getChildAt(i);
            int x = originX.get(i);
            int left,right,pivot;
            if(stackEdgeModel == 1) {
                left = x;
                right = everyWidth + x;
                pivot = stackEdge;
            }else{
                right = getMeasuredWidth() - x;
                left = right - everyWidth;
                pivot = getMeasuredWidth() - stackEdge;
            }
            int top = 0;
            int bottom = top + everyHeight;
            itemView.layout(left, top, right, bottom);
            itemView.setPivotX(pivot);
            itemView.setPivotY(everyHeight / 2);
            if(i < size - 2) {
                adjustScale(i,itemView);
            }
        }
    }

    private int getRealStackSize(){
        return adapter.getItemCount() < stackSize ? adapter.getItemCount() : stackSize;
    }

    private void initAdapterView(){
        if (adapter != null && getChildCount() == 0 && adapter.getItemCount() > 0) {
            hasSetAdapter = true;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0,size = getRealStackSize() + 2; i < size; i++) {
                FrameLayout frameLayout = new FrameLayout(getContext());
                View view = inflater.inflate(adapter.getLayoutId(), null);
                FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                lp1.width = everyWidth;
                lp1.height = everyHeight;
                frameLayout.addView(view, lp1);
                LayoutParams lp2 = new LayoutParams(everyWidth, everyHeight);
                lp2.width = everyWidth;
                lp2.height = everyHeight;
                frameLayout.setLayoutParams(lp2);
                frameLayout.setOnClickListener(this);
                addView(frameLayout);
                frameLayout.measure(everyWidth, everyHeight);
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
            int size = getRealStackSize();
            int count = adapter.getItemCount();
            FrameLayout frameLayout = (FrameLayout) getChildAt(0);
            adapter.bindView(frameLayout.getChildAt(0),getViewIndex(0,0) );//count > size ? size : count-1
            for (int i = 0; i < size + 2; i++) {
                frameLayout = (FrameLayout) getChildAt(i+1);
                adapter.bindView(frameLayout.getChildAt(0),);//size-1-i
            }
            frameLayout = (FrameLayout) getChildAt(size+1);
            adapter.bindView(frameLayout.getChildAt(0), getViewIndex(0,size+1));//count-1
        }
    }

    private int getRealIndex(int index){
        int size = getRealStackSize();
        int count = adapter.getItemCount();
        int real = 0;
        //底层
        if(0 == index){
            real = count - 1;
        //顶层
        }else if(size+1 == index){
            real = count - 1;
        //中间层
        }else{
            real = size - index + offsetIndex ;
        }
        return real;
    }

    /**
     *
     * @param type 1 ->left, 0 ->init, 2 ->right
     * @return
     */
    private int getViewIndex(int type,int index){
        int viewIndex = 0;
        switch (type){
            case 1:
                offsetIndex -= 1;
                break;
            case 2:
                offsetIndex += 1;
                break;
            default:

        }

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
                onRelease(event.getX(), 0);
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
            case MotionEvent.ACTION_DOWN:
                // 此处说明底层没有子View愿意消费Touch事件
                break;

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

                onRelease(event.getX(), velocity);
                break;
            default:
        }
        return true;
    }

    private void onRelease(float eventX, int velocityX) {
        animatingView = (FrameLayout) getChildAt(3);
        animateValue = animatingView.getLeft();
        int tag = Integer.parseInt(animatingView.getTag().toString());

        // 计算目标位置
        int destX = originX.get(3);
        if (velocityX > VELOCITY_THRESHOLD || (animatingView.getLeft() > originX.get(3) + scrollDistanceMax / 2 && velocityX > -VELOCITY_THRESHOLD)) {
            destX = originX.get(4);
            tag--;
        }
        if (tag < 0 || tag >= adapter.getItemCount()) {
            return;
        }

        if (Math.abs(animatingView.getLeft() - destX) < mTouchSlop && Math.abs(eventX - downX) < mTouchSlop) {
            return;
        }

        adapter.displaying(tag);
        animator = ObjectAnimator.ofFloat(this, "animateValue", animatingView.getLeft(), destX);
        animator.setInterpolator(interpolator);
        animator.setDuration(300).start();
    }

    private void requireScrollChange(int dx) {
        if (dx == 0) {
            return;
        }
        int currentPosition = Integer.parseInt(getChildAt(3).getTag().toString());
        if (dx < 0 && currentPosition >= adapter.getItemCount()) {
            return;
        } else if (dx > 0) {
            if (currentPosition <= 0) {
                return;
            } else if (currentPosition == 1) {
                if (getChildAt(3).getLeft() + dx >= originX.get(4)) {
                    dx = originX.get(4) - getChildAt(3).getLeft();
                }
            }
        }
        int num = getChildCount();
        // 1. View循环复用
        FrameLayout firstView = (FrameLayout) getChildAt(0);
        if (dx > 0 && firstView.getLeft() >= originX.get(1)) {
            // 向右滑动，从左边把View补上
            FrameLayout lastView = (FrameLayout) getChildAt(getChildCount() - 1);

            LayoutParams lp = lastView.getLayoutParams();
            removeViewInLayout(lastView);
            addViewInLayout(lastView, 0, lp);

            int tag = Integer.parseInt(lastView.getTag().toString());
            tag -= num;
            lastView.setTag(tag);
            if (tag < 0) {
                lastView.setVisibility(View.INVISIBLE);
            } else {
                lastView.setVisibility(View.VISIBLE);
                adapter.bindView(lastView.getChildAt(0), tag);
            }
        } else if (dx < 0 && firstView.getLeft() <= originX.get(0)) {
            // 向左滑动，从右边把View补上
            LayoutParams lp = firstView.getLayoutParams();
            removeViewInLayout(firstView);
            addViewInLayout(firstView, -1, lp);
            int tag = Integer.parseInt(firstView.getTag().toString());
            tag += num;
            firstView.setTag(tag);
            if (tag >= adapter.getItemCount()) {
                firstView.setVisibility(View.INVISIBLE);
            } else {
                firstView.setVisibility(View.VISIBLE);
                adapter.bindView(firstView.getChildAt(0), tag);
            }
        }

        // 2. 位置修正
        View view3 = getChildAt(3);
        float rate = (float) ((view3.getLeft() + dx) - originX.get(3)) / scrollDistanceMax;
        if (rate < 0) {
            rate = 0;
        }
        int position1 = Math.round(rate * (originX.get(2) - originX.get(1))) + originX.get(1);
        boolean endAnim = false;
        if (position1 >= originX.get(2) && null != animatingView) {
            animator.cancel();
            endAnim = true;
        }
        for (int i = 0; i < num; i++) {
            View itemView = getChildAt(i);
            if (endAnim) {
                itemView.offsetLeftAndRight(originX.get(i + 1) - itemView.getLeft());
            } else if (itemView == animatingView) {
                itemView.offsetLeftAndRight(dx);
            } else {
                int position = Math.round(rate * (originX.get(i + 1) - originX.get(i))) + originX.get(i);
                if (i + 1 < originX.size() && position >= originX.get(i + 1)) {
                    position = originX.get(i + 1);
                }
                itemView.offsetLeftAndRight(position - itemView.getLeft());
            }
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