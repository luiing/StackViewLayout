# StackViewLayout
**An swip StackViewLayout,support left and right.**

**层叠随手势滑动，带轮播自定义ViewGroup**

### Captures

![效果图](/pic/pic001.jpeg)
![效果图](/pic/demo20.gif)

### Use
    implementation 'com.uis:stacklayout:0.1.0'

*Name*| *Descript*|*Value*
  -----|--------|---
stackSpace|间距|默认值：10dp
stackEdge|边界距离|默认值：10dp
stackZoomX|x方向缩放| 0<x<=1,1表示等间距，默认值：1
stackPadX|x方向偏移|表示偏移间距,默认值：0
stackPadX|PadX*(Size-1) < Space|PadX优先级高于ZoomX
stackZoomY|y方向缩放| 0<y<=1,1表示和顶层等高度，默认值：0.9
stackLooper|自动轮播|false/true
stackSize|层叠数量|3
stackEdgeModel|层叠位置|left/right
   
```Xml
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

```Java
        stackViewLayout.setStackLooper(true);
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
        stackViewLayout.setPosition(10);
```

### Version
*Version*| *Descript*|*Fixed*
----|----|----
0.0.1|自动轮播，滑动从顶部移除，整体上浮|初始版本
0.0.2|滑动从顶层加入，整体下沉|内部view点击事件
0.1.0|zoomX,zoomY呈等比数列|更改属性

### Thanks

[AndroidPileLayout](https://github.com/xmuSistone/AndroidPileLayout)
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
