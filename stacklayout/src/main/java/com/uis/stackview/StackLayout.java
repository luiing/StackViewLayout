package com.uis.stackview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

/** 卡片层叠视图，支持随手势滑动动画，自动轮播动画
 * @author  uis 2018/10/30
 */

public class StackLayout extends ViewGroup{

    public final static int MODEL_LEFT = 1;
    public final static int MODEL_RIGHT = 2;
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
    private StackHelper stackHelper;

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
        stackHelper = new StackHelper(configuration.getScaledTouchSlop());
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
            stackHelper.measureChild(width,height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(adapter != null) {
            stackHelper.layoutChild();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        stackHelper.bindLayout(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stackHelper.unbindLayout();
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
        stackHelper.setAutoPlay(looper);
    }

    public void setAdapter(StackAdapter adapter) {
        this.adapter = adapter;
        notifyDataChanged();
    }

    public StackAdapter getAdapter(){
        return adapter;
    }

    public void notifyDataChanged(){
        removeAllViewsInLayout();
        stackHelper.notifyDataChanged();
    }

    public void setPosition(int position){
        stackHelper.setPosition(position);
    }

    public int getSelectedPosition(){
        return stackHelper.getPosition();
    }

    public void setDuration(int duration){
        stackHelper.setDuration(duration);
    }

    /**
     * looper delay
     * @param mills 1000 is 1 second
     */
    public void setLooperDelay(int mills){
        stackHelper.setLooperDelay(mills);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
//        stackHelper.log("act="+event.getActionMasked());
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
                return stackHelper.canScroll(downX,downY,event.getX(),event.getY());
                default:
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        stackHelper.log("act="+event.getActionMasked());
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
                stackHelper.fingerTouchDown();
                break;
            case MotionEvent.ACTION_MOVE:
                if(adapter != null && adapter.getItemCount() > 1) {
                    if (stackHelper.canScroll(downX, downY, event.getX(), event.getY())) {
                        int currentX = (int) event.getX();
                        int dx = (int) (currentX - lastX);
                        stackHelper.executeScroll(dx);
                        lastX = currentX;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handleItemClicked();
            case MotionEvent.ACTION_CANCEL:
                if(adapter != null && adapter.getItemCount() > 1) {
                    mVelocity.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocity = (int) mVelocity.getXVelocity();
                    stackHelper.releaseScroll(velocity);
                    recycleVelocityTracker();
                }
                default:
        }
        return  true;
    }

    private void handleItemClicked(){
        if(adapter != null && downX == (int)lastX){
            if(stackHelper.filterClick()) {
                stackHelper.onItemClicked();
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

    public static abstract class StackAdapter{

        public abstract View onCreateView(ViewGroup parent);

        public abstract void onBindView(View view, int index);

        public abstract int getItemCount();

        public void onItemDisplay(int position) {
        }

        public void onItemClicked(int position){

        }
    }
}