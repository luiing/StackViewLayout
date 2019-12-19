package com.uis.stackview.demo.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.utils.LogUtils;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.uis.stackview.demo.R;
import com.uis.stackview.demo.entity.ItemEntity;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(Build.VERSION.SDK_INT >= 19){
            if(Build.VERSION.SDK_INT >= 23){
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //只有白色背景需加上此flag
                        |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= 19){
            ViewGroup.LayoutParams params = findViewById(R.id.view).getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.status_height);
        }
        FLog.setMinimumLoggingLevel(FLog.VERBOSE);
        LogUtils.enableLog(false);
        if(!Fresco.hasBeenInitialized()) {
            ImagePipelineConfig config = ImagePipelineConfig.newBuilder(getApplicationContext())
                    .setDiskCacheEnabled(true)
                    .setDownsampleEnabled(true)
                    .build();
            Fresco.initialize(getApplicationContext(), config);
        }

        final List<ItemEntity> dataList = StackAdapter.initDataList(this);
        final StackAdapter stackAdapter = new StackAdapter();
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        final XRefreshView freshView = findViewById(R.id.refreshView);

        freshView.setAutoLoadMore(false);
        freshView.setSilenceLoadMore(false);
        freshView.setXRefreshViewListener(new XRefreshView.SimpleXRefreshListener(){
            @Override
            public void onRefresh(boolean isPullDown) {
                freshView.stopRefresh();
                stackAdapter.dataList = dataList.subList(0,6);
                stackAdapter.notifyDataSetChanged();
            }
        });


        ViewPager viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(dataList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(adapter.getRealSize());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(stackAdapter);
    }
}