package com.uis.stackview.demo.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.common.logging.FLog;
import com.facebook.common.logging.LoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.gson.Gson;
import com.uis.stackview.StackLayout;
import com.uis.stackview.demo.R;
import com.uis.stackview.demo.entity.ItemEntity;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by xmuSistone on 2017/5/12.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RecyclerView recyclerView;
    private StackLayout stackViewLayout;
    List<ItemEntity> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StackAdapter());
        //stackViewLayout.setStackLooper(true);
        stackViewLayout.setPosition(10);
        stackViewLayout.setAdapter(new StackLayout.StackAdapter() {
            @Override
            public View onCreateView(ViewGroup parent) {
                return LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fresco_layout,null);
            }

            @Override
            public void onBindView(View view, int position) {
                StackAdapter.ViewHolder viewHolder = (StackAdapter.ViewHolder) view.getTag();
                if (viewHolder == null) {
                    viewHolder = new StackAdapter.ViewHolder();
                    viewHolder.dv = view.findViewById(R.id.imageView);
                    view.setTag(viewHolder);
                }
                Log.e("xx","binderVH: " + position + ",data: " + new Gson().toJson(dataList.get(position)));
                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(Uri.parse(dataList.get(position).getCoverImageUrl()))
                        .setTapToRetryEnabled(true)
                        .setOldController(viewHolder.dv.getController())
                        .build();
                viewHolder.dv.setController(controller);
            }

            @Override
            public int getItemCount() {
                return dataList.size();
            }

            @Override
            public void onItemDisplay(int position) {
                Log.e("xx","display = " + position);
            }

            @Override
            public void onItemClicked(int position) {
                Log.e("xx","clicked = " + position);
                stackViewLayout.setStackLooper(false);
                stackViewLayout.setPosition(position+3);
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