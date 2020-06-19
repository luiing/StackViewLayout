# StackViewLayout
#### Captures

![效果图]()

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
