# 设置语义图标约定

设置项通过 `SettingsIconRole` 指定功能语义，统一由 `rememberSettingsSemanticIcon` 读取本地 VectorDrawable。MD3、Miuix 和液态玻璃共用语义字形，各组件继续负责自己的尺寸、颜色、容器和交互样式。

不同功能不能为了填补图标库缺项而借用同一个角色。增加设置项时，先检查同页实际使用的图标；除了资源名，还应渲染字形检查视觉重复。例如 Material Symbols 的 equalizer 和 bar_chart 形状接近，响度均衡使用 instant_mix 调节杆，声音频谱使用 bar_chart 柱形。

原生字形来自 [Google Material Symbols Outlined](https://github.com/google/material-design-icons/tree/master/symbols/android)，保持默认 24dp、常规线宽和未填充风格，许可证随应用保存于 `app/src/main/assets/licenses/material_symbols.LICENSE`。已有图标能准确表达功能时直接复用资源，无须重复下载。

以下功能使用项目自绘矢量，避免强行套用相近但语义不符的原生图标：

| 功能 | 资源 | 图形含义 |
| --- | --- | --- |
| 评论头像推挤 | `ic_settings_comment_avatar_push_24` | 头像轮廓与向上推移箭头 |
| 保留首页顶部搜索 | `ic_settings_top_search_24` | 页头内的放大镜 |
| 骨架呼吸 | `ic_settings_skeleton_breathing_24` | 加载占位与亮度脉冲 |
| 渐进顶部模糊 | `ic_settings_progressive_top_blur_24` | 顶部边界下逐渐变淡的模糊采样点 |
| 渐进顶部淡出 | `ic_settings_progressive_top_fade_24` | 顶部边界下逐渐变淡的横向色带 |

自绘图标采用 24×24 网格、约 2dp 线宽、相同光学边距，使用白色路径和组件 tint，不写死主题色。方向性返回图标沿用原生资源的 `autoMirrored` 行为；页头、音量和垂直移动等无阅读方向的图形不做水平镜像。

设置图标语义调整属于所有主题共用的内容修正，不应修改共享组件的布局、字体、图标槽位或默认参数。液态玻璃专属的几何与动效调整仍须走材质分支。
