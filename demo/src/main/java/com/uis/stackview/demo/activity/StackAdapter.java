package com.uis.stackview.demo.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.facebook.drawee.view.SimpleDraweeView;
import com.uis.stackview.demo.R;
import com.uis.stackview.demo.entity.ItemEntity;
import com.uis.stackview.StackLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StackAdapter extends RecyclerView.Adapter<StackAdapter.StackVH> {

    List<ItemEntity> dataList;

    @NonNull
    @Override
    public StackAdapter.StackVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(dataList == null){
            dataList = initDataList(parent.getContext());
        }
        return new StackVH(0 == viewType,parent);
    }

    @Override
    public void onBindViewHolder(@NonNull StackAdapter.StackVH holder, int position) {
        holder.binderVH(dataList);
    }

    @Override
    public int getItemViewType(int position) {
        return position % 2;
    }

    @Override
    public int getItemCount() {
        return 10;
    }

    public static ArrayList<ItemEntity> initDataList(Context context) {
        ArrayList<ItemEntity> dataList = new ArrayList<ItemEntity>();
        try {
            InputStream in = context.getAssets().open("preset.config");
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            String jsonStr = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray jsonArray = jsonObject.optJSONArray("result");
            if (null != jsonArray) {
                int len = jsonArray.length();
                //for (int j = 0; j < 3; j++) {
                for (int i = 0; i < len; i++) {
                    JSONObject itemJsonObject = jsonArray.getJSONObject(i);
                    ItemEntity itemEntity = new ItemEntity(itemJsonObject);
                    dataList.add(itemEntity);
                }
                //}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataList;
    }

    public static class ViewHolder {
        ImageView imageView;
        SimpleDraweeView dv;
    }

    static class StackVH extends RecyclerView.ViewHolder{
        StackLayout stackLayout;
        List<ItemEntity> stackData = new ArrayList<>();
        StackLayout.StackAdapter adapter = new StackLayout.StackAdapter() {

            @Override
            public View onCreateView(ViewGroup parent) {
                return LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout,parent,false);
            }

            @Override
            public void onBindView(View view, int position) {
                ImageView imageView = view.findViewById(R.id.imageView);
                try{
                    Glide.with(view.getContext()).load(stackData.get(position).getMapImageUrl()).into(imageView);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            @Override
            public int getItemCount() {
                return stackData.size();
            }

            @Override
            public void onItemDisplay(int position) {

            }
        };

        public StackVH(boolean left,ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    left ? R.layout.stack_left : R.layout.stack_right,parent,false));
            stackLayout = itemView.findViewById(R.id.stacklayout);
        }

        public void binderVH(final List<ItemEntity> dataList){
            stackData = dataList;
            if(stackLayout.getAdapter() == null) {
                stackLayout.setAdapter(adapter);
            }else{
                stackLayout.notifyDataChanged();
            }
        }
    }
}
