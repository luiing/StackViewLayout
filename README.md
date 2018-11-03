# StackViewLayout
**An swip StackViewLayout,support left and right.**

**层叠随手势滑动，带轮播自定义ViewGroup**

### Captures

![效果图](/pic/pic001.png)
![效果图](/pic/demo20.gif)

### how to use
    `implementation 'com.uis:stacklayout:0.0.1'`

*Name*| *Descript*|*Value*
  -----|--------|---
stackSpace|间距|10dp
stackEdge|边界距离|10dp
stackZoomX|x方向缩放| 0.0-2.0
stackZoomY|y方向缩放|0.0-0.2
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

### Thanks

[AndroidPileLayout](https://github.com/xmuSistone/AndroidPileLayout)
## License

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
