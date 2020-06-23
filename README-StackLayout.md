# StackLayout

**层叠View支持手势左右滑动，自动轮播，过渡动画**

1.onMeasure通过StackAdapter适配器取到itemView加入到StackLayaout容器
  
  onMeasure() through stackAdapter add view to StackLayout

2.onLayout取到childView按照层叠布局

  onLayout() get child view layout stack ui

3.onInterceptTouchEvent处理手势支持子View及Velocity

  onInterceptTouchEvent() support child view gesture

4.onTouchEvent处理手势，释放后播放动画平滑过渡

  onTouchEvent() swipe animation,when release recover animation

### Use
    implementation 'com.uis:stacklayout:0.5.0'

*Name*| *Descript*|*Value*
  -----|--------|---
stackSpace|间距(space)|默认值(default)：10dp
stackEdge|边界距离(edge)|默认值(default)：10dp
stackZoomX|x方向缩放(x zoom)| 0<x<=1,1表示等间距，默认值(default)：1
stackPadX|x方向偏移(x padding)|表示偏移间距,默认值：0
stackPadX|PadX*(Size-1) < Space|PadX优先级高于ZoomX
stackZoomY|y方向缩放(y zoom)| 0<y<=1,1表示和顶层等高度，默认值：0.9
stackLooper|自动轮播(looper)|false/true
stackSize|层叠数量(stack size)|3
stackEdgeModel|层叠位置(stack model)|left/right
   
```
    <?xml version="1.0" encoding="utf-8"?>
    <com.uis.stackview.StackLayout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:stack="http://schemas.android.com/apk/res-auto"
        android:id="@+id/stacklayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        stack:stackSpace="5dp"
        stack:stackEdge="20dp"
        stack:stackZoomX="0.1"
        stack:stackZoomY="0.1"
        stack:stackLooper = "false"
        stack:stackSize = "5"
        stack:stackEdgeModel = "left">
    </com.uis.stackview.StackLayout>
```

```
        stackViewLayout.setStackLooper(true);
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
        stackViewLayout.setPosition(10);//指定位置
```

### License

    Copyright 2018, uis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
