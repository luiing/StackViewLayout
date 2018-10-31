package com.stone.pile.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.stone.pile.R;
import com.stone.pile.entity.ItemEntity;
import com.stone.pile.util.Utils;
import com.stone.pile.widget.FadeTransitionImageView;
import com.stone.pile.widget.HorizontalTransitionLayout;
import com.stone.pile.widget.VerticalTransitionLayout;
import com.uis.stackview.StackViewLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by xmuSistone on 2017/5/12.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.bt_web).setOnClickListener(this);
        findViewById(R.id.bt_app).setOnClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new StackAdapter());
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