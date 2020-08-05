# StackViewLayout
    1.支持布局预览、宽高比设置、viewType布局
    2.支持自动播放、左右布局
    3.支持缩放滑动、连续滑动

#### Captures
![效果图](/images/image_normal.jpg) ![效果图](/images/image_left.jpg) ![效果图](/images/image_right.jpg)

### Use
    implementation 'com.uis:stackviewlayout:0.1.1'

### [老版本StackLayout](README-StackLayout.md)

*Name*| *Descript*|*Value*
  -----|--------|---
stackEdgeModel|层叠位置(stack model)|left/right
stackEdge|边界距离(edge)|默认值(default)：10dp
stackPaddingX|x方向偏移距离|10dp
stackOffsetX|x方向偏移因子|2dp
stackPaddingY|y方向偏移距离|10dp
stackOffsetY|y方向偏移因子|2dp
stackAutoPlay|自动轮播(looper)|true
stackDelay|自动轮播延时时间mills|3000
stackDuration|自动轮播播放时间mills|600
stackSize|层叠数量(stack size)|3
stackAspectRatio|顶层宽高比,宽度须有值|0

### Version
    v0.1.0->初始版本,滑动利用Scroller,支持手动滑动和自动轮播，支持多种布局
    v0.1.1->fixed 点击后自动轮播失效,增加轮播延时、轮播时间设置

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
