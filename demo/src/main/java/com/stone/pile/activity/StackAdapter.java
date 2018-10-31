package com.stone.pile.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.stone.pile.R;
import com.stone.pile.entity.ItemEntity;
import com.uis.stackview.StackViewLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StackAdapter extends RecyclerView.Adapter<StackAdapter.StackVH> {

    ArrayList<ItemEntity> dataList;

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
        return 20;
    }

    private ArrayList<ItemEntity> initDataList(Context context) {
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

    static class ViewHolder {
        ImageView imageView;
    }

    static class StackVH extends RecyclerView.ViewHolder{
        StackViewLayout stackLayout;
        public StackVH(boolean left,ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(
                    left ? R.layout.stack_left : R.layout.stack_right,parent,false));
            stackLayout = itemView.findViewById(R.id.stacklayout);
        }

        public void binderVH(final ArrayList<ItemEntity> dataList){
            stackLayout.setAdapter(new StackViewLayout.StackViewAdapter() {
                @Override
                public int getLayoutId() {
                    return R.layout.item_layout;
                }

                @Override
                public void bindView(View view, int position) {
                    ViewHolder viewHolder = (ViewHolder) view.getTag();
                    if (viewHolder == null) {
                        viewHolder = new ViewHolder();
                        viewHolder.imageView = (ImageView) view.findViewById(R.id.imageView);
                        view.setTag(viewHolder);
                    }

                    Glide.with(itemView.getContext())
                            .load(dataList.get(position).getCoverImageUrl()).into(viewHolder.imageView);
                }

                @Override
                public int getItemCount() {
                    return dataList.size();
                }

                @Override
                public void displaying(int position) {

                }

                @Override
                public void onItemClick(View view, int position) {
                    super.onItemClick(view, position);
                    Toast.makeText(view.getContext(),"click:"+position,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
