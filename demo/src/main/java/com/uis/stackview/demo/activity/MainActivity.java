package com.uis.stackview.demo.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.common.logging.FLog;
import com.facebook.common.logging.LoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.gson.Gson;
import com.uis.stackview.StackLayout;
import com.uis.stackview.demo.R;
import com.uis.stackview.demo.entity.ItemEntity;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.WindowManager;
import androidx.viewpager.widget.ViewPager;

/**
 * Created by xmuSistone on 2017/5/12.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RecyclerView recyclerView;
    private StackLayout stackViewLayout;
    List<ItemEntity> dataList;

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
        if(!Fresco.hasBeenInitialized()) {
            ImagePipelineConfig config = ImagePipelineConfig.newBuilder(getApplicationContext())
                    .setDiskCacheEnabled(true)
                    .setDownsampleEnabled(true)
                    .build();
            Fresco.initialize(getApplicationContext(), config);
        }
        recyclerView = findViewById(R.id.recyclerView);
        stackViewLayout = findViewById(R.id.stacklayout);
        findViewById(R.id.bt_web).setOnClickListener(this);
        findViewById(R.id.bt_app).setOnClickListener(this);

        dataList = StackAdapter.initDataList(this);
        ViewPager viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(dataList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(adapter.getRealSize());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StackAdapter());
        //stackViewLayout.setStackLooper(true);
        //stackViewLayout.setPosition(10);
        stackViewLayout.setAdapter(new StackLayout.StackAdapter() {
            @Override
            public View onCreateView(ViewGroup parent) {
                return LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fresco_layout,null);
            }

            @Override
            public void onBindView(View view, int position) {
                SimpleDraweeView dv = view.findViewById(R.id.imageView);
                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(Uri.parse(dataList.get(position).getMapImageUrl()))
                        .setTapToRetryEnabled(true)
                        .setOldController(dv.getController())
                        .build();
                dv.setController(controller);
            }

            @Override
            public int getItemCount() {
                return 5;//dataList.size();
            }

            @Override
            public void onItemDisplay(int position) {
                Log.e("xx","display = " + position);
            }

            @Override
            public void onItemClicked(int position) {
                Log.e("xx","clicked = " + position);
                //stackViewLayout.setStackLooper(false);
                //stackViewLayout.setPosition(position+3);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_app:
                updateApp();
                break;
            case R.id.bt_web:
                updateWeb();
                break;
                default:
        }
    }

    void updateWeb(){//"http://app.qq.com/#id=detail&appid=1104844480");//
        Uri uri = Uri.parse(String.format("http://a.app.qq.com/o/simple.jsp?pkgname=%s","cn.com.bailian.bailianmobile"));
        Intent it = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(it);
    }

    void updateApp(){
        try {
            Uri uri = Uri.parse(String.format("market://details?id=%s", "cn.com.bailian.bailianmobile"));
            Intent it = new Intent(Intent.ACTION_VIEW, uri);
            it.setPackage("com.tencent.android.qqdownloader");//com.tencent.android.qqdownloader com.huawei.appmarket
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
        }catch (Exception ex){
            ex.printStackTrace();
            Toast.makeText(this,"应用宝未安装",Toast.LENGTH_SHORT).show();
        }
    }
}