# 正式版运行优化规则

正式版沿用 Baseline Profile 与 ProfileInstaller 安装链路，不需要在应用内增加“强制 dex2oat”入口。系统决定何时进行 AOT 优化；配置写入成功不代表编译完成。AGSL 折射与模糊的 GPU 成本仍需由渲染实现控制。

## 规则来源

- `app/src/main/baseline-prof.txt`：启动种子及应用自有转场、玻璃热点的手写规则。交互热点只标记 H/P，不将整个浏览流程标记为启动代码。不对整个应用或依赖库使用全包通配。
- `app/src/main/baselineProfiles/startup-prof.txt`：手写启动种子，包含 Application、主 Activity 和首页首帧相关类；不包含滚动、玻璃调节或整个视频浏览流程。与采集结果合并使用，不能当作实测完整启动路径。
- `app/src/main/generated/baselineProfiles/`：由插件合并保存实际交互采集的 `baseline-prof.txt` 与 `startup-prof.txt`，独立于手写规则。需要随源码保存供后续发布使用。

正式版和 dev 安装包导出前会检查基线文件及元数据的存在性和文件头；这项检查只能防止文件缺失或明显损坏，不能证明热点覆盖和设备编译状态。

普通发布复用已保存规则，不自动运行采集。显式更新入口为 `scripts/update_baseline_profile.sh`，该脚本会调用 Gradle，只有用户明确要求相关编译/采集工作时才执行。

## 采集场景

`BiliPaiBaselineProfileGenerator` 将启动与交互分开维护：

| 场景 | 操作 |
| --- | --- |
| 启动 | 启动至首页主导航，只将启动路径加入 Startup Profile |
| 常用内容 | 首页、动态、历史、搜索、外观、空间、网页和视频详情 |
| Material 3 原生 | 选择 Material 3、关闭玻璃、开启卡片展开，采集导航、滚动、视频进入与返回 |
| Miuix 原生 | 选择 Miuix，执行同样的原生材质场景 |
| Material 3 玻璃 | 开启玻璃后执行相同场景，包含开关反馈、底栏与转场 |
| Miuix 玻璃 | 选择 Miuix 并开启玻璃，执行相同场景 |

玻璃场景要求 Android 13 或以上；较早系统明确跳过玻璃场景，仍可采集原生路径。保留 `pixel6Api31`，新增 `pixel6Api33` 作为可用的托管设备配置。实际采集所用设备按已有插件配置选择。

采集使用真实设置界面，不向正式版加入测试广播或额外 Intent 参数。使用专用测试安装与中文界面，确保已完成首次启动引导、四个默认导航项可见，首页有可访问的视频内容。采集会修改主题、玻璃与卡片展开设置，请勿直接使用日常使用的安装环境。

手机、大屏和窄分屏需分别在对应窗口环境采集；脚本根据当前窗口导航，不会自动更改分辨率或模拟分屏。关键入口、开关状态、视频详情及返回列表缺失时应终止采集，不能将跳过的路径记作成功。

## 发布边界

新增手写规则可随下一版正式包交付；它们是预编译提示，不是性能收益测量结果。现有安装不会因源码更新而获得这些规则，需要安装携带规则的新版本。实际交互采集的结果要先生成并保存，不能用脚本已经存在来替代采集完成。

参考：[手写规则](https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure)、[插件保存与合并规则](https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles)。

## R8 优化范围

R8 固定为 9.5.28，AGP 继续使用当前 Studio 支持的 9.3.2。水平类合并恢复默认开启，历史记录涉及的视频卡片与文字测量两个文件类由精确类规则隔离；规则不使用成员通配，不阻止这些类的全部静态方法被优化。更新版本不等于已经确认本项目的历史校验错误被修复。

调度兼容规则只保护 `getWmJobScheduler(Context)`、`forNamespace(JobScheduler)` 和原有清理方法边界。API 34 分支、缺失 API 回退以及避免误清理默认命名空间的逻辑保留，同文件的查询与错误信息辅助方法不再整体禁止优化。

R8 官方 9.5 分支中包含 [宽寄存器覆盖修复](https://r8.googlesource.com/r8/+/8db693f4ad2d7b09e3d519a72c67a7f82c3fde8c) 和 [类型传播校验错误修复](https://r8.googlesource.com/r8/+/ab0db3afb22b28424ba8269ef88f9cab58c5462a)。这些修复提供了更新尝试的依据，不能替代本项目的行为确认。
