# StackViewLayout
**A swipe ViewGroup that supports left and right slide.**

**层叠View支持手势左右滑动，自动轮播，过渡动画**

1.onMeasure通过StackAdapter适配器取到itemView加入到StackLayaout容器
  
  onMeasure() through stackAdapter add view to StackLayout

2.onLayout取到childView按照层叠布局

  onLayout() get child view layout stack ui

3.onInterceptTouchEvent处理手势支持子View及Velocity

  onInterceptTouchEvent() support child view gesture

4.onTouchEvent处理手势，释放后播放动画平滑过渡

  onTouchEvent() swipe animation,when release recover animation

### Captures

![效果图](/pic/pic001.jpeg)
![效果图](/pic/demo20.gif)

![尺寸说明](/pic/biaozhu.png)

<li>注释：此图解释参数意义，展示效果不太精确，图片真实宽度为**上层橙色**

### Use
    implementation 'com.uis:stacklayout:0.3.4'

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

### Version
*Version*| *Descript*|*Fixed*
----|----|----
0.0.1|自动轮播，滑动从顶部移除，整体上浮|support auto looper and animation
0.0.2|滑动从顶层加入，整体下沉|fixed child view clicked event
0.1.0|zoomX,zoomY呈等比数列|modify attribute
0.1.1|只有一条数据时|fixed adapter itemSize=1
0.1.2|增加动画、轮播时间设置，获取当前选中位置|add animation,looper time
0.2.0|只有一个元素，不支持轮播和滑动|only one child,can't swipe
0.2.1|减少child层级,见child.measure()|child.measure() opt
0.3.0|增加联动效果（缩放+平移）|support whole animation
0.3.1|联动动画平滑过度|fixed animation smooth
0.3.2|联动动画去抖动及adapter数据更新会多出层|opt animation shake
0.3.3|adapter数据更新ui展示错误|opt adapter changed display
0.3.4|滑动促发item点击事件|fixed item clicked event

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
