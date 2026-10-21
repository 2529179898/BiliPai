# BiliPai 蓝雪女仆动效

## 关注与取关反馈（2026-10-04）

已按用户确认的开心半身原画接入 `FOLLOW_SUCCESS`：张嘴开心笑、一手贴胸、另一手提裙边，1.2 秒轻欠身、眨眼、手部与发梢轻动，再停留 800ms。身份与风格使用项目精细半身女仆，不采用早期全身 Q 版或加号/爱心标牌。原画及完整提示词见 `blue-snow-maid-follow-happy-reference.png`、`follow-happy-prompt.md`。

基于该角色独立制作 `UNFOLLOW_COMPLETE`：平静闭眼、小闭嘴、双手自然交叠于腰前，1 秒轻点头与收势，再停留 500ms；不套用开心表情，不哭泣、不庆祝，闭眼保留原画睫毛而不重复制作眨眼。使用内置 imagegen 生成 `blue-snow-maid-unfollow-reference.png`，完整最终提示词见 `unfollow-pose-prompt.json`。两段按各自原画测量眼睛、手与发梢蒙版，均有匹配的静态回退。

关注操作通过 `ActionRepository.followUser` 在接口 code=0 后发出一次对应事件，覆盖 UP 空间、视频详情与联合投稿成员关注；直播原有直接接口的成功分支也接入。确认取消、登录缺失、接口失败、列表恢复与拉黑操作不会发出角色反馈。批量取关关闭单项动画，任务结束后仅在有实际成功项时汇总一次，部分失败在详情中保留数量。沿用无 replay 的前台事件宿主与非模态安全角落卡片，旧动画完成回调不能关闭新反馈，后台事件不补播。

十一段资源结构、静态回退、枚举时长一致性检查及 `git diff --check` 通过。浅/深背景抽样关键帧见 `preview-follow_success-frames.png`、`preview-unfollow_complete-frames.png`，确认两种表情/姿态独立、动作终点和蒙版边缘。`render-cleaning-frames.py --state follow_success` / `--state unfollow_complete` 可再生成抽样图；该工具不等同于完整 Lottie 或 Android 渲染器。

未运行编译或 Gradle 测试。应用内连续播放、横竖屏卡片避让、关注分组弹窗与反馈同时出现、快速连续关注/取关及批量部分失败展示尚未验证。

## 下拉刷新女仆反馈已取消（2026-10-04）

按用户要求撤除首页女仆刷新伴随反馈，保留原来的 MD3、Miuix 等刷新指示器与请求逻辑。移除刷新角色组件、枚举与应用内资源，不继续推广到其他页面。清理中、失败及成功等其他女仆状态保持现状。

原画、提示词及抽样关键帧保留作制作记录；Lottie 和静态稿归档为本目录 `refreshing-draft.json` 与 `refreshing-draft-static.png`，不参与应用资源或默认预览生成。`render-cleaning-frames.py --state refreshing` 仅读取此归档稿。

## 失败反馈缩短（2026-10-04）

RETRY 从 2.4 秒调整为 1.5 秒（60fps、90 帧），保留现有担心皱眉、断线检查的独立原画和静态回退。重新安排插头关键帧（15/30/49 帧）、眨眼（42 帧）与断线提示（16/27/40 帧），角色和发梢动作随新的结束帧同步收束；播放器时长同步为 1500ms，不通过截断旧资源缩短。

通用 `ErrorState` 与搜索主体失败状态使用主按钮“重试”；仍支持立即操作，不等待动画完成。搜索已有结果后的分页失败保持文字按钮。错误状态角色默认单次播放，点击角色可明确重播，短视口沿用可滚动布局。

资源结构、关键帧范围和播放器时长一致性检查完成；通过 `render-cleaning-frames.py --state retry` 抽样浅/深背景 0、15、30、42、49、89 帧，预览为 `preview-retry-frames.png`。该工具仅作本地抽样检查，应用内连续播放与按钮实际布局尚未验证；未运行编译或 Gradle 测试。

## 清理中状态（2026-10-04）

新增 `CLEANING`：专注眉眼、小闭嘴、微低头，双手握扫帚；1.6 秒往返扫动和眨眼，起止姿态一致，无完成勾或庆祝元素。清理完成仍使用原来的独立得意姿态。

原画使用内置 imagegen，项目源图为 `blue-snow-maid-cleaning-reference.png`，完整最终提示词见 `cleaning-pose-prompt.json`。原画分层后生成 `bilipai_maid_cleaning.json` 与对应 `bilipai_maid_cleaning_static.png`，不是仅对整张静态图做位移。下半段扫帚以握柄处为轴轻扫，握柄以上与双手保持连接，角色轻微倾身，眼睛独立眨动；蒙版坐标按新姿势重新测量。

缓存清理弹窗在真实任务进行时循环，仅可见前台消耗播放时间；减少动态效果或加载失败保持该状态的静态姿态。成功立即切换现有完成动画，失败沿用原来的错误提示并关闭弹窗，等待动画不触发完成回调。保留现有进度和已清理容量，未知总量使用不定进度。当前清理接口只在开始和结束更新进度，等待期间不虚构增长。弹窗限高并可滚动，短窗口缩小角色。

运行 `generate.py` 更新九段资源和离线预览，`validate.py` 检查资源结构及循环闭合。`render-cleaning-frames.py` 使用 Pillow/NumPy 抽样本段关键帧，生成 `preview-cleaning-frames.png`（浅/深背景，0、24、48、66、72、95 帧）。此抽样工具不是完整 Lottie 渲染器，不能替代应用内播放检查。

本次资源静态检查、深浅背景关键帧人工检查和接入源代码检查已完成；未运行编译、Gradle 测试。浏览器安全策略拒绝打开本地 `file:` 预览，因此本次浏览器连续播放、Android 实际循环暂停恢复与弹窗布局尚未验证。

已接入启动样式选择、手动清理完成、首页与搜索加载失败/断网，以及视频列表、追更合集、缓存列表和搜索结果的空状态；新增收藏成功、缓存下载完成反馈，并替换视频详情默认点赞与三连庆祝效果。

- 欢迎：1 秒，精细半身女仆，睁眼、挥手、发梢和雪花轻动。
- 清理完成：1.2 秒，基于用户后来提供的全身 Q 版参考重新生成。睁着蓝眼睛，得意微笑，双手握扫帚；扫帚扫走缓存方块，眨眼后出现完成勾。
- 暂无内容：1.8 秒，双手捧空收纳盒，闭眼尴尬苦笑，盒子轻摆后停住。
- 搜索无结果：2 秒，拿放大镜托腮思考，角色左眼位于镜片内并被放大，右眼微眯、小 o 嘴；眨眼、镜面微光后停住。
- 重试：1.5 秒，双手检查断开的连接线，担心皱眉、小嘴向下，插头微动、轻眨眼。播放一次，不在错误页面持续循环。
- 收藏成功：1.2 秒，双手抱金色星星，单眼眨眼、闭嘴笑，不露牙齿；星星和手臂轻动，星光亮起。
- 点赞 / 下载完成：共用同一段 1.2 秒动画，一手竖拇指，另一手拿已保存的视频卡，开心庆祝；拇指轻摆，眨眼和完成标记亮起。视频详情默认点赞效果使用此动画，现有自定义皮肤素材仍有优先权。
- 三连成功：1.8 秒，独立全身 Q 版姿势，闭眼笑、双拳举起、小跳落地，点赞、投币、收藏三个图标依次亮起。替换原有三连动画，仅在三个操作全部成功时显示。

用户否定了初版几何简笔画并提供精细参考。纯矢量描摹会损失眼睛和蕾丝的细节，因此最终采用**本地分层插画 Lottie**，而非纯矢量：每段只嵌入一张透明 PNG，通过独立眼睛、手腕/扫帚、发梢、道具图层与矢量效果实现动作。八段资源采用独立姿势，点赞按用户要求复用下载完成资源；只有欢迎招手。没有远程素材或外部字体。

## 预览和再生成

直接打开 `preview.html` 即可离线预览。支持重播、拖动时间轴、深浅背景切换；附带 MIT 授权的 lottie-web 5.12.2 播放器。

使用 Python 3 + Pillow：

```sh
python3 generate.py
python3 validate.py
```

脚本以本目录中的八张最终参考 PNG 为输入，生成 Android raw 资源、静态回退 PNG 及嵌入数据的预览页面。没有生成服务调用，也不编译应用。

品牌动画采用单次播放；空状态与错误状态支持点击角色重播。统一播放器监听 RESUMED 生命周期和根布局可见区域，首页/搜索分页还显式传入当前页条件。隐藏或退到后台时取消播放协程并保留进度；恢复时重置帧时钟、接着播放，完成回调只触发一次。显式 replayKey 或移出组合会建立新的一次播放。Lottie 按 raw 资源缓存解析结果。加载与播放都有超时，播放预算只扣除可见时的耗时；系统减少动态效果时显示最后一帧，资源失败时使用对应的本地静态角色。清理弹窗仍在成功后 2 秒自动关闭，失败不进入成功动画。女仆启动样式保留现有总开关和欢迎协议门槛，旧设置默认保留图标飞出。启动壁纸从系统启动屏真正退出后开始计时，与女仆同时显示。

## 美术生成方式及最终提示词

美术使用 Codex 内置 imagegen 工具生成，透明背景；不使用 API CLI。生成文件已复制进本目录，不依赖工具临时路径。

### 欢迎角色

参考：仓库 `artwork/app-icon/blue_snow_maid_front_foreground_4096.png`，对应用户提供的精细蓝雪女仆图标。

最终提示词：

> Use case: illustration-story. Asset type: production character illustration for BiliPai Lottie. Reference image is the exact character identity and rendering style to preserve: blue snow maid, detailed glossy blue anime eyes, white hair with cyan streaks, lace maid headdress, blue ribbon bows, blue and white maid dress. Create ONE isolated exquisite half-body chibi character of THIS SAME GIRL on genuinely transparent background. No blue circle, no backdrop, no text, no words, no logos. Front-facing, proportionate cute large head but preserve the reference's delicate face and facial expression, beautiful large glossy blue eyes and subtle peach blush, soft friendly smile. Fine clean tapered anime linework, premium mobile game illustration, delicate layered cel shading with small soft highlights; medium intricate lace ruffles, elegant hair strands. Fully visible headdress and all hair tips, cropped neatly at waist with curved silhouette, generous transparent padding. Her left hand rests gracefully near her chest, her right hand is raised clearly to the right of her head in a gentle open-palm greeting, separated from face and hair silhouette. Keep head, hair, dress, and raised hand visually clearly distinguishable for later animation. Beautiful polished original artwork, not primitive geometric shapes, no stick arms, no flat simplistic blob shapes. White/cyan/blue palette matching the reference. Only one character.

### 清理完成角色

参考：用户提供的 `clean-style-reference.jpg`。用户随后明确要求更换表情并重新生成。

最终提示词：

> Use case: illustration-story. Asset type: freshly generated BiliPai cache cleanup animated mascot sprite. Use supplied image ONLY as character identity, chibi proportions and drawing style reference. Generate a NEW polished illustration of the exact same blue snow maid with a CHANGED expression and a broom instead of the megaphone. Keep exact cute full-body super-deformed proportions: very big head, tiny body and legs, long silver white/cyan streaked hair, ruffled maid headdress, cyan blue bows, bright blue white maid dress and white lace apron, tiny navy shoes, thick dark navy confident smooth anime lineart and clean crisp cel shading as the reference. NEW EXPRESSION: beautiful OPEN bright blue glossy anime eyes, relaxed curved brows, softly rosy cheeks, a small closed pleased smile with a hint of pride, conveying 'all clean!'. Absolutely NO squeezed shut > < eyes and NO yelling mouth. She holds a household straw broom naturally with BOTH hands at waist height: visible hands wrapping around a warm tan wooden shaft tilted diagonally down toward viewer LEFT, extending beside her dress to a cute warm golden straw brush head near floor at bottom-left, cyan bristle binding. Clearly connected shaft and brush. No broom across face, shoes and apron clearly visible, face angled very slightly toward viewer. Balanced friendly standing pose with a slight lean toward broom, flowing hair framing body. Whole character and broom fully visible with generous transparent margin. Genuine transparent background, no white background, no blue circle, no scene, no shadow floor, no text/logos/UI/particles/confetti. Only ONE isolated premium chibi mascot artwork, matching the supplied reference art style closely. This is a new regenerated portrait with changed expression, not preserving the shouting pose.

## 验证状态

八段资源已通过 JSON、帧数、递增关键帧、角色/道具图层引用、透明内嵌图片、静态回退和离线预览资源的静态检查；浏览器中检查扫动、眨眼、手部/道具运动、三连图标依次亮起及最终帧和深浅背景。收藏、点赞/下载、三连重播后均停在最终帧，预览无控制台错误或警告。`git diff --check` 通过。新增启动样式解析及启用门槛的 JVM 测试，遵守用户要求未运行 Gradle，应用内集成效果尚未验证。

## 应用内待验证场景

以下行为已实现并做源代码检查，尚未执行应用内验证：

- 角色播放中切到后台或切到另一分页，再返回时从暂停处继续，已完成的不自动重播。
- 点击空/错误状态角色重播一次；操作入口仍执行原有搜索或重试回调。
- 系统减少动态效果时使用最终静态帧；资源缺失/解析失败时用静态 PNG，启动仍能结束。
- 通用视频列表（收藏/历史/稍后再看）、追更合集、缓存列表及所有搜索类别保留原文案与操作入口。
- 搜索结果主体失败使用断线女仆 RETRY，保留“搜索失败”、接口错误信息和“重试”；所有搜索类别共用此分支，仅当前分页播放。搜索无结果仍使用放大镜角色；已有结果后的分页加载失败继续保留列表及页尾重试。
- 原有启动设置、冷启动门槛、壁纸共存与清理失败逻辑继续按前述方案执行。
- 收藏仅在接口确认新增成功后提示；取消、只移除收藏夹和失败不触发。三连内部收藏禁用单项反馈，全部成功才播放三连。
- 下载在文件合并/保存完成、任务变为 COMPLETED 后发出一次提示；同一任务完成事件去重，历史恢复不发事件，后台完成不在回前台时补播。
- 收藏和下载卡片仅在可展示的前台界面收集事件，动画结束后停留 800 毫秒；2600 毫秒兜底关闭。连续事件替换当前卡片，旧回调不能关闭新卡片。默认点赞与三连在减少动态或加载失败时保留静态姿势约 1 秒，再执行对应完成停留。

## 位置与展示时长修订

- 点赞当前锚定点赞图标右上方：女仆中心对齐图标右缘并向右偏 4dp，底边与图标顶部重叠 12dp，按系统安全边缘约束；最大 144dp。仅锚点缺失时沿用右下角回退。动作 1.2 秒后保留最终姿态 0.5 秒，自定义皮肤仍沿用自身资源和播放时间。
- 三连移到内容区右下角，普通窗口最大 220dp、横屏/全屏/短窗口最大 180dp，再按窗口高度缩小；动作 1.8 秒后保留最终姿态 0.6 秒。只在对应庆祝结束后执行已启用的三连跳转；更换视频、较新的庆祝或 5 秒未收到结束回调时取消旧跳转。
- 收藏/下载卡片通过应用导航提供的底部内容预留、系统安全区域、音频播放条和键盘高度共同避让，角色按剩余空间缩小。输入区域挤占到不足 120dp 高度时不展示卡片，仍由超时关闭，不覆盖输入。
- 首页失败、搜索失败/空结果、列表和追更合集的空状态按扣除头部与底部占用的内容区域居中。角色普通窗口 200dp、大屏 220dp、短窗口 128dp，单次播放后保留。
- 启动仍为 1 秒，短窗口缩小启动角色以保留字标空间；清理动作仍为 1.2 秒，成功后 2 秒自动关闭。清理弹窗增加系统安全区域避让、最大高度及内容滚动，短窗口按高度缩小成功角色，普通窗口保留 144dp。

源代码与资源静态检查已完成。新增三连等待完成、旧回调隔离、主动取消和未展示超时不跳转的回归测试源码，并更新位置策略测试；遵守用户要求未运行 Gradle 或这些测试，实际窗口中的遮挡与播放效果尚未验证。失败/空状态在有界的短视口内可滚动，放在已有滚动列表/头部中的无界内容不会再嵌套滚动。

## 状态姿势及表情修订

用户指出通用招手与近似微笑无法区分状态后，使用内置 imagegen 重新制作了断网、空状态、搜索三张独立插画，再按“左眼放到放大镜里”和明显区分表情的反馈修订空状态与搜索。三张姿势的初始完整提示词见 `state-pose-prompts.json`，两张表情修订的完整提示词见 `expression-revision-prompts.json`。最终文件为 `blue-snow-maid-retry-reference.png`、`blue-snow-maid-empty-reference.png`、`blue-snow-maid-search-empty-reference.png`。每种状态都有匹配的静态回退图，静态校验同时检查内嵌图与回退图相同、非欢迎动画没有 Greeting hand 图层、三种状态插画内容互不相同。

用户进一步要求点赞复用点赞姿势、三连单独设计、收藏不露牙齿后，重新生成最终闭嘴收藏图和双拳庆祝三连图。`success-pose-prompts.json` 保留最初探索，收藏初版露牙图已被替换；最终收藏与三连提示词见 `success-revision-prompts.json`。最终输入为 `blue-snow-maid-favorite-reference.png`、`blue-snow-maid-download-reference.png`、`blue-snow-maid-triple-reference.png`。预览截图见 `preview-favorite-final.png` 与 `preview-triple-final.png`，经验约定保存在 [AGENTS.md](AGENTS.md)，后续制作继续遵守。

## 点踩、分享、投币反馈

三个独立姿势采用项目精细半身女仆身份，原画与完整提示词在 `blue-snow-maid-dislike-reference.png`、`blue-snow-maid-share-reference.png`、`blue-snow-maid-coin-reference.png` 和 `video-action-pose-prompts.json`。投币初版金色硬币已替换为用户确认的白色／银白色；三连中的硬币矢量图标同步改白。

| 状态 | 独立动作 | 触发条件 |
| --- | --- | --- |
| 点踩 | 抱臂、小撇嘴，1 秒轻摇头、眨眼，停留 400ms | 接口确认新增点踩；取消与失败不播放 |
| 分享 | 张嘴开心，一手摊掌邀请，1.2 秒手掌、发梢和眨眼，停留 500ms | 链接复制完成、站内好友实际发送成功，或成功打开外部分享界面；不声称外部已送达 |
| 投币 | 握白色硬币、单眼闭嘴笑，1.2 秒握币手与发梢轻动，仅开眼侧眨眼，停留 500ms | 投币接口成功；附带点赞只播投币，三连沿用专属庆祝 |

三者与点赞共用当前 `LikeBurstAnchorRegistry.likeIcon.bounds` 的完整布局计算和单项动画槽，连续操作替换当前角色；事件 ID 在切换视频后仍递增，旧结束回调不能关闭新反馈。分享事件不重放，只由匹配当前 bvid 的前台视频页面收集。打开分享选项、关闭分享面板、未安装外部应用或发送失败不触发。

十四段资源通过 JSON、递增关键帧、时长、蒙版、父层引用、透明内嵌图片与匹配静态回退检查；新增三项各六个采样帧已在深浅背景人工检查，见 `preview-dislike_confirmed-frames.png`、`preview-share_ready-frames.png`、`preview-coin_success-frames.png`。本次使用离线采样，未验证浏览器连续播放或应用内呈现；采样工具不是 Android Lottie 渲染器。添加了旧回调跨视频隔离、取消点踩／投币失败不反馈、附带点赞不叠加动画的回归测试源码，未运行测试或编译。

## 资源体积优化：共享角色图片

14 段应用 Lottie JSON 的 `maid_bitmap` 现在只引用对应静态回退无损 WebP 文件名，`e=0`、`u` 为空。播放器等待解析后，在 IO 协程中读取 `drawable-nodpi` 原图、检查文件名和尺寸，并通过当前 Lottie 6.7.1 的 `LottieImageAsset.setBitmap` 提供图片，再开始播放；缺失或解码失败进入原有静态回退。无需复制一套图片到 assets。图片像素、关键帧、蒙版、角色表情、播放时长与位置均未修改。

`generate.py` 新生成资源同样引用共享 PNG。`package_assets.py` 可转换已有资源并生成包含 14 段动效的离线预览；预览单独内嵌图片，仅用于制作。`render-cleaning-frames.py` 同时支持共享图片及归档的内嵌图片。

| 资源统计 | 优化前 | 优化后 |
| --- | --- | --- |
| JSON | 6,259,862 bytes | 179,972 bytes |
| 静态 PNG | 4,559,991 bytes | 4,559,991 bytes |
| 原始合计 | 10,819,853 bytes | 4,739,963 bytes |
| 同方式 ZIP 压缩模拟 | 9,182,289 bytes | 4,580,755 bytes |

压缩模拟节省 4,601,534 bytes（约 4.60 MB，50.1%）；这是资源 ZIP 估算，不是 APK 实测。完整数字和测量方式见 `size-optimization-report.json`。转换前后全部 PNG 和剔除图片包装字段后的时间轴哈希相同。14 段资源、枚举路径、预览内嵌图均已检查；重新采样的分享与投币帧图哈希与优化前完全相同。未运行编译或 Gradle 测试，应用内共享图片加载、失败回退和播放尚未验证。

## 2026-10-21：无损图片打包

14 张应用状态图使用 `cwebp -lossless -exact -m 4` 编码；包含完全透明像素的 RGBA 字节逐一与原 PNG 比对，512×512 尺寸、蒙版、关键帧和静态回退语义保持不变。原画保留在 artwork，应用仅打包一份 WebP；Lottie `maid_bitmap.p` 与同名静态回退共享图片。`static-image-pixels.json` 保存像素摘要，`validate.py` 校验枚举、时间轴、引用和像素，`package_assets.py` 保持离线预览内嵌同一张 WebP。重新制作需要 Pillow 与 cwebp，生成与抽样工具均读取 `brand-motion` 模块。
