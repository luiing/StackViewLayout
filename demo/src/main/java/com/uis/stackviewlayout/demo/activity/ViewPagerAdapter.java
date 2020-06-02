package com.uis.stackviewlayout.demo.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.uis.stackview.demo.R;
import com.uis.stackviewlayout.demo.entity.ItemEntity;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class ViewPagerAdapter extends PagerAdapter {
    List<ItemEntity> dataList;
    LinkedList<View> views = new LinkedList<View>();
    int MAX = 1000;
    int real = 0;

    public ViewPagerAdapter(List<ItemEntity> dataList) {
        this.dataList = dataList;
        real = dataList.size();
    }

    public int getRealSize(){
        return MAX/2 - MAX/2%real;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        position %= real;
        View v;
        if(views.size() > 0){
            v = views.removeLast();
        }else {
            v = LayoutInflater.from(container.getContext()).
                    inflate(R.layout.item_fresco_pager, container, false);
        }
        SimpleDraweeView dv = (SimpleDraweeView)v;
        dv.setImageURI(dataList.get(position).getMapImageUrl());
        container.addView(v);
        return v;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View v = (View)object;
        container.removeView(v);
        views.add(v);
    }

    @Override
    public int getCount() {
        return MAX;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
