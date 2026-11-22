# CC：反应堆控制台（cc_rc）开发日志

> 模组 ID：`cc_rc` ｜ Minecraft Forge 1.20.1 ｜ 前置：CC: Tweaked
> 本文档按「目录 / 代码 / 注册物品」三部分组织：**「二、代码」描述模组当前已实现的功能与代码现状**（按模块组织，不按开发期次），目录为索引，方便快速定位源码与实现；每次改动的变更记录见 `临时日志.md`。
> **版本记录（v0.0.7）**：新增 F.A.A.S服务器方块 `server_faas_1/2/3`（水平四方向放置朝向玩家，靠近时持续播放 `server_noise` 环境音效，三种变体共用 `ServerFaasBlock` 方块类，模型来自"模型/FAAS"并转为 Java 格式，另补三张方块掉落表）；版本号由 0.0.6 升至 0.0.7。**上版本记录（v0.0.6）**：新增模组 logo（`模型/logo.png`，256x256，经 mods.toml `logoFile` 声明）；版本号由 0.0.5 升至 0.0.6。**修复**：mods.toml 的 `description` 由多行字符串 `'''...'''` 改为单行 `"..."`，避免第三方启动器（HMCL/PCL2 等）解析 TOML 失败导致 logo 不显示。

---

## 一、目录（索引）

| 编号 | 分类 | 内容 | 位置 |
| --- | --- | --- | --- |
| 1 | 框架 | 模组入口与注册机制 | [二、1 模组框架](#1-模组框架与注册机制) |
| 2 | 框架 | 配置系统（安全按钮 / 圆盘记录仪 / 刷卡机） | [二、2 配置系统 Config](#2-配置系统-config) |
| 3 | 文字显示 | 控制面板文字存储与网络同步 | [二、3.1 文字存储 ConsolePanelBlockEntity](#31-文字存储consolepanelblockentity) |
| 4 | 文字显示 | 通用文字渲染 ConsolePanelBER | [二、3.2 通用文字渲染 ConsolePanelBER](#32-通用文字渲染consolepanelber) |
| 5 | 文字显示 | 命名文字（放置时自定义名称写入） | [二、3.3 命名文字的写入](#33-命名文字的写入setplacedby) |
| 6 | 文字显示 | 数码显示器 / 数字调节器双行文字 | [二、3.4 数码显示器与数字调节器显示](#34-数码显示器与数字调节器显示) |
| 7 | 红石输入 | 指示灯（电平输入 LIT） | [二、4.1 指示灯 PointLampBlock](#41-指示灯pointlampblock) |
| 8 | 红石输入 | 仪表（信号强度 0~15） | [二、4.2 仪表 MeterBlock](#42-仪表meterblock) |
| 9 | 红石输入 | 断路器底部信号自动跳闸 | [二、4.3 断路器红石输入](#43-断路器红石输入) |
| 10 | 红石输入 | 刷卡机定时自动复位 | [二、4.4 刷卡机红石输入](#44-刷卡机红石输入) |
| 11 | 红石输入 | 圆盘记录仪时钟上升沿检测 | [二、4.5 圆盘记录仪时钟（红石输入）](#45-圆盘记录仪时钟红石输入) |
| 12 | 红石输出 | 普通拉杆（继承原版 LeverBlock） | [二、5.1 普通拉杆](#51-普通拉杆) |
| 13 | 红石输出 | 3 挡位拉杆（0/8/15） | [二、5.2 3挡位拉杆](#52-3挡位拉杆consolelever3stageblock) |
| 14 | 红石输出 | 控制台按钮 1~5（按下 15 持续 20tick） | [二、5.3 控制台按钮](#53-控制台按钮consolebuttonblock) |
| 15 | 红石输出 | 安全按钮（三阶段授权输出） | [二、5.4 安全按钮](#54-安全按钮safebuttonblock) |
| 16 | 红石输出 | 断路器 / 刷卡机输出 | [二、5.5 断路器与刷卡机输出](#55-断路器与刷卡机红石输出) |
| 17 | 红石输出 | 连接方向 helper（getConnectedDirection） | [二、5.6 连接方向计算](#56-连接方向计算getconnecteddirection) |
| 18 | 划线 | 圆盘记录仪数据缓冲与采样 | [二、6.1 圆盘记录仪数据缓冲](#61-圆盘记录仪数据缓冲plotterblockentity) |
| 19 | 划线 | 圆盘记录仪趋势图渲染 | [二、6.2 圆盘记录仪渲染 PlotterBER](#62-圆盘记录仪渲染plotterber) |
| 20 | 划线 | 圆盘记录仪时钟触发扫描 | [二、6.3 圆盘记录仪时钟触发](#63-圆盘记录仪时钟触发) |
| 21 | CC 外设 | 数码显示器 / 数字调节器外设 | [二、7 CC-Tweaked 外设](#7-cc-tweaked-外设) |
| 22 | 工具 | 多功能工具（百分比切换 / 模式切换） | [二、8 多功能工具](#8-多功能工具multitoolitem) |
| 23 | 划线 | 数字圆盘记录仪（50点0~100线图 + CC外设） | [二、9 数字圆盘记录仪](#9-数字圆盘记录仪digitalplotter) |
| 24 | 移植 | 旧模组移植物品（冰冻罗非鱼/绿酒/何意味/合一味）与自定义 Tier | [二、10 旧模组移植物品](#10-旧模组移植物品modtooltiers) |
| 25 | 物品 | 描述型物品机制（DescriptionItem/SwordItem/AttackDescriptionItem）与新增月饼/压缩饼干 | [二、11 描述型物品与新增食物](#11-描述型物品与新增食物) |
| 26 | 注册 | 所有注册方块（按方块类归组） | [三、1 方块](#1-方块blocks共-134-个) |
| 27 | 注册 | 所有注册物品（含唱片） | [三、2 物品](#2-物品items共-172-个) |
| 28 | 注册 | 方块实体 / 声音 / 创造标签 / 弹射物实体 | [三、3~6 方块实体·声音·创造标签·弹射物实体](#3-方块实体block-entity-types共-22-个) |
| 29 | 文字显示 | 大号空控制面板（3 倍字号居中文字 + 16x14x2 碰撞箱） | [二、3.2 通用文字渲染 ConsolePanelBER](#32-通用文字渲染consolepanelber) |
| 30 | 物品 | 搬运物品与搬运物品栏（箱装土豆、16 色粗布告示牌与悬挂告示牌 + 自定义方块实体，标签按注册名排序） | [二、12 搬运物品与搬运物品栏](#12-搬运物品与搬运物品栏) |
| 31 | 注册 | 搬运方块：冰箱（27 格容器 + 原版箱子 GUI）、水槽（纯装饰） | [二、13 冰箱与水槽](#13-冰箱与水槽) |
| 32 | 机制 | 自定义配方类型 cc_rc:sink_conversion（数据驱动四向转换） | [二、14 自定义配方类型](#14-自定义配方类型sink_conversion) |
| 33 | 物品/实体 | 包子 bao_zi（右键食用 +4饥饿/+2饱和；左键投掷命中爆炸弹射物，仅伤害实体不破坏方块，投掷冷却 0.5s） | [二、15 包子](#15-包子bao_zi) |
| 34 | 物品 | 法棍 baguette（食物 营养2/饱和度2 堆叠16，主手攻击伤害4 + 击退 ATTACK_KNOCKBACK=3.0，粗体棕色描述"坚如磐石"） | [二、16 法棍](#16-法棍baguette) |
| 35 | 物品 | 撬棍 crowbar（SwordItem 自带横扫，修改器伤害+19 实际总伤害20 攻速慢 暴击2.0x 耐久1024 铁砧打击音效，浅蓝粗体"物理学圣剑"+深蓝"f(x)dx"） | [二、17 撬棍](#17-撬棍crowbar) |
| 36 | 红石 | 红石信号发射器 redstone_sender（六方向，收到信号沿面向方向2~配置距离无线传输给接收器，on/off 贴图）+ 接收器 redstone_receiver（无方向，向相邻方块输出信号强度） | [二、18 红石信号发射器与接收器](#18-红石信号发射器与接收器redstone_sender--redstone_receiver) |
| 37 | 装饰/音效 | F.A.A.S服务器 server_faas_1/2/3（水平四方向放置朝向玩家，贴近持续播放 server_noise 环境音效，三种变体共用方块类） | [二、19 F.A.A.S服务器](#19-faas服务器server_faas) |
| 38 | 物品 | 说明书1 instruction_book_1（原版成书 WrittenBookItem 机制，右键打开书籍界面，固定内容：目录 + 控制面板类/编辑工具/圆盘记录仪/断路器/刷卡机/红石信号收发） | [二、20 说明书1](#20-说明书1instruction_book_1) |
| 39 | 物品/CC | 说明书2 instruction_book_2（原版成书机制，记录 CC 配件外设使用法：数码显示器/数字调节器/数字圆盘记录仪/扩展红石继电器与总线的每个 Lua 函数及参数类型） | [二、21 说明书2](#21-说明书2instruction_book_2) |
| 40 | 物品/武器 | 简易长矛 simple_spear（SwordItem 自带横扫，耐久130 修改器伤害+129 总伤害130 +13攻击范围 ForgeMod.ENTITY_REACH，文字颜色类似附魔金苹果 Rarity.EPIC+附魔微光，手持模型2倍大且y前移3.2，紫色描述"魔女们的秘密武器"） | [二、22 简易长矛](#22-简易长矛simple_spear) |
| 41 | 指令 | /ccrc peripheral set_count <设备种类> <数字>（仅允许设置 ids.json 已记录类型，写文件+反射同步内存）+ get_count（查询）+ list（仅查看 ids.json 内容，不扫描世界）；get_count/set_count 带类型自动补全；旧顶层别名已删除 | [二、23 /ccrc 指令](#23-ccrc-指令ccrccommand) |
| 42 | 红石/物品 | 密码输入器 password_inputer（放置/碰撞箱同控制面板，on/off 两态，破解成功向后方强充能15，1秒后自动关）+ 破解器 password_cracker（手持右键开始破解，20秒可配置，离开5格/切快捷栏即中断重置） | [二、24 密码输入器与破解器](#24-密码输入器与破解器passwordinputerblock--passwordcrackmanager) |
| 43 | 红石/物品 | 核弹按钮 nuke_button（放置/碰撞箱同控制面板，6 状态输出 0/5/7/10/10/15，钥匙推进状态）+ 核弹发射钥匙 1/2 key_1/key_2（右键消耗推进状态） | [二、25 核弹按钮与钥匙](#25-核弹按钮与钥匙nukebuttonblock) |
| 44 | 红石/物品 | 钥匙柜 key_cabinet（水平四向放置，多功能工具右键记录坐标朝向）+ 钥匙分发控制器 key_distributor（六面 key_sender 贴图，方块实体存记录；≤5 信号校验删除失效记录、>5 随机分发钥匙1/2 高亮不消失掉落物） | [二、26 钥匙柜与钥匙分发控制器](#26-钥匙柜与钥匙分发控制器keycabinetblock--keydistributorblock) |
| 45 | 生物/物品 | 错误生物 error_mob（由 QQ 发送的 error.obj 立体化生成的敌对生物实体，仿蠹虫寻路/攻击，无方块交互，不自然生成）+ 刷怪蛋 error_mob_spawn_egg + 变种 error_mob_null/error_mob_warn（null.zip/WARN.zip，仅模型贴图不同）+ 各自刷怪蛋 | [二、27 错误生物](#27-错误生物errormob) |
| 46 | 物品/GUI | 编辑工具 edit_tool（主手右键可显示名称方块打开文字编辑 GUI；副手放置自动打开编辑界面）+ 统一接口 ITextDisplay | [二、28 编辑工具](#28-编辑工具edittool) |
| 47 | 红石/CC外设 | 扩展红石继电器 extended_relay（总线远端红石端口）+ 扩展红石继电器总线 relay_bus（CC 外设 redstone_relay_bus，沿朝向距离搜索继电器读写信号） | [二、29 扩展红石继电器与总线](#29-扩展红石继电器与总线extendedrelayblock--relaybusblock) |
| 48 | 生物/物品 | 盖金蜗牛 gajin（AI 参考原版猪无乘骑机制，金鹰作为食物吸引/繁殖，右键播放 gajin 音效，平常无叫声）+ 刷怪蛋 gajin_spawn_egg + 金鹰 golden_eagle（堆叠 64，蜗牛食物） | [二、30 盖金蜗牛与金鹰](#30-盖金蜗牛与金鹰gajinsnail--goldeneagle) |
| 49 | 物品/音乐 | 唱片音乐 6 张（GitHub issue #1：bit / broken_boy / panic_track / resonance / roller_mobster / sabotage，VLC mono OGG + RecordItem 注册 + 双语 lang） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 50 | CC 外设 | CC 外设无延迟改造（风险最小方案）：knob/display/plotter volatile+dirty+tick 节流广播，relay_bus RelayState 双缓存 + 总线缓存 + Lua 零 Level 访问，修复 Inputs 落盘 | [二、32 CC 外设无延迟改造](#32-cc-外设无延迟改造风险最小方案实施) |
| 51 | 物品/音乐 | 唱片音乐 2 张（friends_wine 朋友的酒 / air 鸟之诗，mp4 源文件经 ffmpeg 提取音频转 mono OGG + RecordItem 注册 + 双语 lang） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 52 | 方块/CC外设 | 数据单元 data_unit（无方向完整方块，专属贴图 top/side；方块实体存名称+空数据列表，编辑工具可改名称，CC 外设读写名称/整个列表/某一位） | [二、33 数据单元](#33-数据单元dataunit) |
| 53 | 方块/CC外设 | 方块探测器 block_detector（六方向放置，专属贴图 top/side/front/back；CC 外设只读探测面向方块的坐标/注册名/模组来源/方块实体数据） | [二、34 方块探测器](#34-方块探测器blockdetector) |
| 54 | 资源整理 | 贴图/模型目录整理：textures/block 与 models/block 顶层散装文件全部归入各方块同名文件夹（含 redstone_receiver/sender、relay_bus、extended_relay 等多贴图方块），所有 JSON 引用同步更新（99 文件） | [二、35 贴图/模型资源目录整理](#35-贴图模型资源目录整理) |
| 55 | 方块/GUI | 控制面板文字样式：染料右键面板染字（16 色，不消耗染料）+ 编辑工具 GUI 新增 B/I/U/S 格式按钮与 16 色块（打开时读取当前样式），EditTextPacket 携带完整样式（颜色/粗体/斜体/下划线/删除线） | [二、36 控制面板文字样式](#36-控制面板文字样式染料染色--编辑工具格式按钮) |
| 56 | 方块 | ~~海报1 poster_1~~（已删除：与 poster_2 贴图完全相同，属第一张重复注册） | [二、37 海报](#37-海报posterblock) |
| 57 | 方块/GUI | 海报 poster（**重构：只注册一个方块**，右键打开 GUI 选图片 + 缩放倍率 + dy/dz 偏移；渲染改 BlockEntityRenderer 直绘四边形，彻底摆脱原版模型元素坐标 ±16/32 限制；旧 poster_2~26 方块/物品/模型/blockstate 全部删除，贴图保留为可选图片，`PosterImages.LIST` 现有 47 条（46 张可选，索引 45 水循环已隐藏）） | [二、37 海报](#37-海报posterblock) |
| 58 | 生物/物品 | 邪恶盖金 evil_gajin（敌对生物，僵尸 AI 不燃烧/不捡装备，100 血 20 攻，距离变速追击，锁定/攻击/击杀/受击全套语音 + RWR 追击循环音，模型暂用盖金蜗牛）+ 刷怪蛋 evil_gajin_spawn_egg | [二、38 邪恶盖金](#38-邪恶盖金evilgajin--evil_gajin) |
| 59 | 生物/渲染 | 错误生物新模型（ERROR/NULL/WARN 像素字牌，来源模型/生物/错误生物/新模型三个 Blockbench zip：纯红占位贴图 + 每面独立 UV json）——先离线转 Java addBox，后改 **Blockbench json 直渲**（ErrorMobJsonRenderer 直接加载 json 渲染，复用 BlockbenchJsonModel，同邪恶盖金方案；删除全部 Java Model 类与模型层注册） | [二、39 错误生物新模型](#39-错误生物新模型errornullwarn-像素字牌) |
| 60 | 生物/渲染 | 邪恶盖金换 Blockbench JSON 直渲模型（gaijin_t58.zip：17 元素 5 旋转，texture_size 32x32）——不再转 Java ModelPart，新增运行时加载器 BlockbenchJsonModel 逐元素逐面绘制（每面独立 UV + 旋转元素绕 origin 旋转），渲染器改为 EntityRenderer 直渲 | [二、40 邪恶盖金 JSON 直渲模型](#40-邪恶盖金-json-直渲模型blockbenchjsonmodel) |
| 61 | 物品/音乐 | 唱片「LEVEL !」music_disc_level（感叹号不入注册名，VLC mono OGG 160k ≈1:59，比较器输出 13 复用 reimu，music_discs 标签兼容唱片机） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 62 | 指令 | /ccrc sound 音频播放指令（`<声音ID> <目标> [音量]`，**目标必选**、音量可选，定向播放给 @p/@a/@e 等，无距离限制无衰减；S2C 纯数据包 PlaySoundPacket id5 + 客户端 CommandSoundInstance 无衰减播放，遵循 PR#2 服务端加载安全模式） | [二、23 /ccrc 指令](#23-ccrc-指令ccrccommand) |
| 63 | 方块/GUI | 控制柜系统总览：`controller_block`（蓝 / 黑变体）拼长方体，外侧唯一 `controller_bus` 总线保存结构状态 / 组件 / 文字 / 绘图 / 变量表 / 程序行 / 配置；`controller_aux` 右键相邻总线打开程序界面；同色连通泛洪 + 长方体完整性 + 每 100 tick 巡检，三态 未成型 / 部分成型 / 完全成型 | [二、41 控制柜系统](#41-控制柜系统consoleblock--consolebusblock--controller) |
| 64 | 方块 | 结构与成型：`main` 标记、总线六向面、成型校验的 5 种失败原因、完全成型禁止挖掘并免疫爆炸 | [二、41.1 结构与成型](#411-结构与成型) |
| 65 | 渲染 | 表面坐标系与渲染：`ConsoleSurface` / `ConsoleFrame` 统一 `(u, v)` 换算，`ConsoleBusBER` 绘制组件模型、读数、文字与绘图 | [二、41.2 表面坐标系与渲染](#412-表面坐标系与渲染) |
| 66 | 方块/GUI | 组件通用机制：放置 / 颜色（贴图白块采样染色）/ 附带文字 / 缩放 / 拖动 | [二、41.3 组件通用机制](#413-组件通用机制放置--颜色--附带文字--缩放--拖动) |
| 67 | 方块/GUI | 组件清单（23 种）：仪表 `controller_meter`、数字屏 `controller_display`、圆盘记录仪 `controller_graph`、指示灯 `controller_lamp`、键盘 `controller_button`、端子、旋钮 / 拉杆开关、各类按钮与旋钮等 | [二、41.4 组件清单](#414-组件清单) |
| 68 | 方块/GUI | 程序系统：变量表、`ProgramIR`、`ProgramExprParser` 文本表达式、`ProgramVM` 字节码栈式执行、程序编辑界面 | [二、41.5 程序系统](#415-程序系统变量表--ir--字节码-vm--表达式--编辑界面) |
| 69 | 兼容 | CC: Tweaked 外设接口：`ConsoleBusPeripheral` 与 `ConsoleCcBridge` 取数 / 写值排队 | [二、41.6 CC: Tweaked 外设接口](#416-cc-tweaked-外设接口) |
| 70 | 数据 | 配置 JSON 保存与加载：`ConsoleConfigJson` 结构与保存 / 加载界面 | [二、41.7 配置 JSON 保存与加载](#417-配置-json-保存与加载) |
| 71 | 物品/GUI | 画图工具 `draw_tool`：右键控制柜表面打开属性界面，24 种预设图案 + 矩形 / 线段 / 箭头，图案存总线方块实体并随 NBT 同步 | [二、41.8 画图工具](#418-画图工具draw_tool) |
| 72 | 方块/渲染 | 斜面控制台 `controller_slope`（默认 / 蓝 / 黑）：把宿主被盖住那一格的组件 / 文字 / 绘图按斜面坐标系重画，颜色须与结构一致 | [二、41.9 斜面控制台](#419-斜面控制台controller_slope) |
| 73 | 方块/GUI | 辅助计算机与界面清单：`controller_aux` 绑定总线，`ConsoleProgramScreen` / `ConsoleConfigScreen` 等界面一览 | [二、41.10 辅助计算机与界面清单](#4110-辅助计算机与界面清单) |
| 74 | 数据 | 关键数据格式：NBT / 网络包 / 配置 JSON 的字段结构 | [二、41.11 关键数据格式](#4111-关键数据格式nbt--网络包--配置-json) |
| 75 | 状态 | 控制柜现状与待办：已完成部分与已知未实现项 | [二、41.12 现状与待办](#4112-现状与待办) |
| 76 | 方块/GUI | 涂鸦文字 `graffiti`：贴墙透明方块，右键墙面放置并打开文字编辑界面，客户端 BER 直接在墙面绘制文字 | [二、42 涂鸦文字](#42-涂鸦文字graffiti) |
| 77 | 方块/GUI | 涂鸦形状 `graffiti_shape`：贴墙透明方块，手持画图工具右键墙面放置并按工具设置初始化，BER 复用控制柜画图渲染器 | [二、43 涂鸦形状](#43-涂鸦形状graffiti_shape) |
| 78 | 实体/GUI | 便签 `note`：贴墙实体，可贴在方块表面任意浮点位置，四行文字各自颜色与格式，左键移除、Shift+右键取回 | [二、44 便签](#44-便签note) |
| 79 | 物品/渲染 | 激光笔 `laser_pointer`：手持时客户端从手部向「视线与碰撞箱表面的交点」绘制红线（最长 16 格、线宽 0.02 格，Config 可调），所有玩家可见，无网络包 | [二、45 激光笔](#45-激光笔laser_pointer) |
| 80 | 方块/GUI | 音频播放器 `audio_player`：客户端循环播放任意已注册音效，四种红石模式（触发 / 反触发 / 脉冲 / 常开），可设范围 / 衰减 / 起始 / 截至 / 间隔（负值重叠）/ 音量（0~500%），界面显示音效时长并带名称补全 | [二、46 音频播放器](#46-音频播放器audio_player) |
| 81 | 音频素材 | 麦卡锡音频 96 条：按「英文类别_英文名」批量注册声音（mechanical / explosion / alarm / running / music），VLC 单声道 64kbps，仅带 `^` 标记的素材按完整循环裁剪 | [三、4 声音](#4-声音sound-events共-134-个) |
| 82 | 方块/GUI | 水循环图 `water_cycle_chart`（独立贴墙物品，固定 2 格宽模型渲染 512×362 低清图；右键打开高清查看界面，5000×3536 高清图仅界面打开时加载、关闭即 `TextureManager.release` 释放显存；海报列表索引 45 保留为隐藏低清条目以兼容旧存档） | [二、48 水循环图](#48-水循环图water_cycle_chart) |

---

## 二、代码

### 1. 模组框架与注册机制

主类 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 通过 6 个 `DeferredRegister` 注册方块、物品、声音、方块实体、实体、创造标签与配方类型，构造函数中还初始化简单网络通道（`ModNetwork.init()`，C2S 投掷包子），并在 `commonSetup` 中注册 CC: Tweaked 外设提供者：

```java
@Mod(CcRc.MODID)
public class CcRc {
    public static final String MODID = "cc_rc";

    public CcRc(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModNetwork.init();
        modEventBus.addListener(this::commonSetup);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
```

客户端在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java#L88-L107) 的 `ClientModEvents` 中注册 9 个方块实体渲染器（BER）与 2 个实体渲染器（`bao_zi` 复用原版 `ThrownItemRenderer`；`error_mob` 使用自定义 `ErrorMobRenderer` + 模型层 `error_mob`）。

**方块注册示例（[ModBlocks.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlocks.java)）：**

```java
public static final RegistryObject<ConsoleLeverBlock> CONSOLE_LEVER_1 = BLOCKS.register("console_lever_1",
        () -> new ConsoleLeverBlock(BlockBehaviour.Properties.of()
                .noCollission().strength(0.5F).sound(SoundType.WOOD)));
```

**物品注册示例（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java)）：**

```java
public static final RegistryObject<Item> CONSOLE_LEVER_1_ITEM = ITEMS.register("console_lever_1",
        () -> new BlockItem(ModBlocks.CONSOLE_LEVER_1.get(), new Item.Properties()));
```

### 2. 配置系统 Config

[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java) 使用 `ForgeConfigSpec`，三个配置段：

- **safe_button**：`timeout_ticks`（默认 200，状态 2 超时回到状态 1）、`signal_ticks`（默认 20，状态 3 信号持续时间）。
- **plotter**：模式 2~9 的数据更新间隔，默认 `5/10/20/50/100/200/500/1000` tick。`getPlotterTickInterval(mode)` 中模式 1 返回 `-1`（被动触发）。
- **card_reader**：`on_ticks`（默认 20，激活后保持输出的时长）；每个等级刷卡机的可识别卡片规则，默认 `A→"A"、B→"AB"、C→"ABC"、D→"ABCD"、E→"ABCDE"`，由 `isCardAccepted(readerLevel, cardLetter)` 判断。
- **password_inputer**：`crack_ticks`（默认 400 = 20 秒，破解密码输入器所需时间）。

### 3. 文字显示

#### 3.1 文字存储（ConsolePanelBlockEntity）

[ConsolePanelBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlockEntity.java) 是所有贴墙/贴面控制类方块实体的基类，存储待渲染文字（`Component text`）与计时（`ticksRemaining`），通过 NBT 持久化并支持网络同步：

```java
public class ConsolePanelBlockEntity extends BlockEntity {
    private Component text = Component.empty();
    private int ticksRemaining = 0;

    public void setText(Component text) { ...; sync(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Text", Component.Serializer.toJson(text));
        tag.putInt("TicksRemaining", ticksRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        text = Component.Serializer.fromJson(tag.getString("Text"));
        ticksRemaining = tag.getInt("TicksRemaining");
    }
    // getUpdateTag / getUpdatePacket / onDataPacket 用于客户端同步
}
```

#### 3.2 通用文字渲染（ConsolePanelBER）

[ConsolePanelBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBER.java) 提供静态方法 `renderText` 与 `pushTextTransform`，所有贴墙方块（拉杆、指示灯、仪表、按钮、圆盘记录仪）共用。核心是根据 `FACING` + `FACE`（WALL/CEILING/FLOOR）计算旋转，再以 `scale(-0.015F, -0.015F, 0.015F)` 把文字缩放为贴近方块表面的像素字：

```java
public static void pushTextTransform(BlockState state, PoseStack poseStack,
                                     float textX, float textY, float textZ) {
    Direction facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
    AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
    poseStack.pushPose();
    poseStack.translate(0.5, 0.5, 0.5);
    if (face == AttachFace.WALL) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.getOpposite().toYRot()));
        poseStack.translate(textX, textY, textZ);
    } else if (face == AttachFace.CEILING) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.translate(textX, textY, textZ);
    } else { // FLOOR
        poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.getOpposite().toYRot() + 180) % 360));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.translate(textX, textY, textZ);
    }
    poseStack.scale(-0.015F, -0.015F, 0.015F);
}
```

文字绘制使用 `Font.drawInBatch`，居中、白色、`POLYGON_OFFSET` 模式避免与方块面 z-fighting：

```java
Font font = Minecraft.getInstance().font;
float width = font.width(text);
font.drawInBatch(text, -width / 2.0F, 0, 0xFFFFFF, false,
        poseStack.last().pose(), bufferSource, Font.DisplayMode.POLYGON_OFFSET,
        0x0F0000, packedLight);
```

默认文字位置常量：`DEFAULT_TEXT_X = 0.0F`、`DEFAULT_TEXT_Y = 0.3F`、`DEFAULT_TEXT_Z = 0.35F`。

`renderText` 提供可指定字号与纵向居中的重载（新增 `scale`、`centerY` 参数），大号空控制面板 [ConsolePanelLargeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelLargeBlock.java) 继承 `ConsolePanelBlock`，仅重写碰撞箱（16 宽 × 14 高 × 2 厚，比空控制面板左右各多 1 像素），渲染时以 `scale = 0.045F`（0.015×3，3 倍字号）且 `centerY = true`（y 方向居中，x 方向本就由 `-width/2.0F` 居中）调用：

```java
if (be.getBlockState().getBlock() instanceof ConsolePanelLargeBlock) {
    renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight,
            0.0F, 0.0F, DEFAULT_TEXT_Z, 0.045F, true);
} else {
    renderText(be.getBlockState(), be.getText(), poseStack, bufferSource, packedLight);
}
```

> **变更记录（大号空控制面板）** 新增方块 `console_panel_large`（模型 `block/console_panel/console_panel_large`，贴图 `empty_console_panel_large`），放置/文字存储逻辑复用空控制面板（同一 `console_panel_be` 方块实体类型），仅碰撞箱与文字渲染（3 倍字号、x/y 居中）不同。

#### 3.3 命名文字的写入（setPlacedBy）

[ConsolePanelBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlock.java) 继承 `FaceAttachedHorizontalDirectionalBlock`，在放置时把物品的自定义名称写入方块实体，实现“用命名牌命名后显示在面板上”：

```java
@Override
public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                        @Nullable LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (stack.hasCustomHoverName()) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ConsolePanelBlockEntity panelBE) {
            panelBE.setText(stack.getHoverName());
        }
    }
}
```

#### 3.4 数码显示器与数字调节器显示

数码显示器 [DigitalDisplayBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayBER.java) 与数字调节器 [DigitalKnobBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobBER.java) 都是完整方块，按水平朝向 `FACING` 旋转后渲染两行文字：

- 第一行（白字）：放置时自定义名称。
- 第二行（橙字，`0xFFF06020`）：数码显示器为状态文字（默认 `"----"`，CC 外设可改）；数字调节器为当前数值。

```java
// DigitalKnobBER 第二行：百分比模式仅改变显示，存储值不变
Component line2 = be.isPercentMode()
        ? Component.literal(String.format(Locale.ROOT, "%.1f%%", be.getValue() / 10.0))
        : Component.literal(Integer.toString(be.getValue()));
```

共同绘制逻辑（`drawLine`）：`translate(0, y, TEXT_Z)` + `scale(-SCALE, -SCALE, SCALE)`，`SCALE = 0.015F`、`TEXT_Z = -0.57F`，居中绘制，`PACKED_LIGHT = 0xF000F0` 全亮。

### 4. 红石输入

#### 4.1 指示灯（PointLampBlock）

[PointLampBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Point_lamp/PointLampBlock.java) 继承 `ConsolePanelBlock`，用 `LIT` 布尔属性显示亮/灭。`neighborChanged` 读取方块是否收到强红石信号并同步 `LIT`，是典型的被动红石输入：

```java
@Override
public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                            BlockPos fromPos, boolean isMoving) {
    if (!level.isClientSide) {
        boolean lit = level.hasNeighborSignal(pos);
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), 3);
        }
    }
}
```

#### 4.2 仪表（MeterBlock）

[MeterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Meter/MeterBlock.java) 继承 `ConsolePanelBlock`，用 `POWER`（0~15 整数）驱动 16 种模型，输入来源为 `level.getBestNeighborSignal(pos)`（邻居最高信号强度），随信号变化实时刷新模型。

#### 4.3 断路器红石输入

[BreakerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Breaker/BreakerBlock.java) 是完整方块，`POWERED` 表示通断。除右键手动切换外，还检测底面信号实现“自动跳闸”：当底面收到红石信号（`level.getSignal(pos.below(), Direction.UP) > 0`）时强制变为断开。

#### 4.4 刷卡机红石输入

[CardReaderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/card_reader/CardReaderBlock.java) 贴墙放置，`use` 时检查手持的是否为 `CardItem` 且通过 `Config.isCardAccepted` 等级校验：接受则 `POWERED = true` 并 `scheduleTick`，等待配置的 `on_ticks` 后由 `tick` 自动复位为 `false`。

> **变更记录（v0.0.5）刷卡机碰撞箱 14x14x2 → 8x14x3**
> 刷卡机碰撞箱由 14×14×2 调整为 8×14×3（与模型元素一致）：宽向（平行墙面）居中 8 格、左右各空 4 格，高 14 格不变，贴墙厚度 3 格。参数区将原 `BOX_LO/BOX_HI`（宽高共用）拆分为独立的 `BOX_W_LO/W_HI`（4~12）与 `BOX_H_LO/H_HI`（1~15），`BOX_THICKNESS` 改为 3；四个方向 SHAPE 均按此缩放。

#### 4.5 圆盘记录仪时钟（红石输入）

[PlotterClockBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterClockBlockEntity.java) 存储上一次信号 `lastSignal`，在 `onNeighborChanged` 中检测**上升沿**（0 → 非 0）后向上扫描 1~8 格触发划线（详见 6.3）：

```java
public void onNeighborChanged() {
    int cur = level.getBestNeighborSignal(worldPosition);
    if (lastSignal == 0 && cur > 0) {   // 上升沿
        triggerAbove();
    }
    setLastSignal(cur);
}
```

### 5. 红石输出

#### 5.1 普通拉杆

[ConsoleLeverBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_lever/ConsoleLeverBlock.java) 直接继承原版 `LeverBlock`（自带红石输出与朝向逻辑），额外实现方块实体与文字显示。

#### 5.2 3挡位拉杆（ConsoleLever3StageBlock）

[ConsoleLever3StageBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_lever/ConsoleLever3StageBlock.java) 用自定义 `STAGE`（0/1/2）替代原版 `POWERED`，右键循环切换，输出强度随挡位变化：

```java
// 输出强度：0 / 8 / 15
@Override
public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
    return switch (state.getValue(STAGE)) {
        case 1 -> 8;
        case 2 -> 15;
        default -> 0;
    };
}

// 直接输出（强充能）：与 getSignal 同强度，仅在连接方向输出（防止向四周泄漏）
@Override
public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
    return getSignal(state, level, pos, direction) > 0
            && getConnectedDirection(state) == direction
            ? getSignal(state, level, pos, direction) : 0;
}
```

> **修复记录（v0.0.8）3挡位拉杆强充能信号恒为 15**：初版 `getDirectSignal` 按"挡位 > 0 → 15"写死，二档（8）对后方强充能时错误输出 15（相邻方块弱信号正常）；改为调用 `getSignal` 按挡位输出（二档 8 / 三档 15），与按钮/刷卡机"强充能同值"模式一致。

挡位切换时需 `onRemove` 更新新旧位置邻居，保证输出实时刷新。

#### 5.3 控制台按钮（ConsoleButtonBlock）

[ConsoleButtonBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Console_button/ConsoleButtonBlock.java) 继承 `ConsolePanelBlock`，`POWERED` 表示按下状态。按下后 `scheduleTick(20)` 自动弹起：

```java
@Override
public InteractionResult use(...) {
    if (!level.isClientSide && !state.getValue(POWERED)) {
        level.setBlock(pos, state.setValue(POWERED, true), 3);
        level.scheduleTick(pos, this, 20);   // 20 tick 后弹起
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
}

@Override
public int getSignal(...)  { return state.getValue(POWERED) ? 15 : 0; }
@Override
public int getDirectSignal(...) {
    return state.getValue(POWERED) && getConnectedDirection(state) == direction ? 15 : 0;
}
```

`console_button_1~5` 五个变体共用本类（仅模型/贴图不同）：1~3 为原版样式；4/5 分别采用"模型/特殊控制台/A、C"的核弹面板按钮（on/off 两态模型，按下时由 blockstate `powered` 切换开/关模型与贴图），转换时 **UV 直接沿用**（源模型 UV 即 Java 0-16 归一化坐标，与贴图内容区精确对应；曾误按 Bedrock 像素坐标缩放导致贴图错乱，已按源文件验算并修正为不缩放），顶/底面做方向翻转。

#### 5.4 安全按钮（SafeButtonBlock）

[SafeButtonBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Safe_button/SafeButtonBlock.java) 实现三阶段授权流程：`STAGE 1 →（潜行+右键）→ 2 →（右键）→ 3`。状态 3 是授权通过，此时输出红石；随后由 `tick` 心跳（配合方块实体 `ticksRemaining`）自动回退：状态 3 持续 `signal_ticks`（默认 20）回到 2，状态 2 持续 `timeout_ticks`（默认 200）回到 1：

```java
// 心跳：每秒排程一次，递减 ticksRemaining
@Override
public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.getBlockEntity(pos) instanceof ConsolePanelBlockEntity be) {
        be.ticksRemaining--;
        // 根据当前 STAGE 在 0 时跳转到下一状态（3→2→1），并更新邻居
        level.setBlock(pos, next, 3);
        level.updateNeighborsAt(pos, this);
    }
    level.scheduleTick(pos, this, 1);   // 持续心跳
}

// 仅状态 3 输出 15
@Override
public int getSignal(...)  { return state.getValue(STAGE) == 3 ? 15 : 0; }
@Override
public int getDirectSignal(...) {
    return state.getValue(STAGE) == 3 && getConnectedDirection(state) == direction ? 15 : 0;
}
```

#### 5.5 断路器与刷卡机红石输出

- **断路器** [BreakerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Breaker/BreakerBlock.java)：`POWERED` 为通时向后方的面与上方输出 15，`getSignal` 在 `getConnectedDirection` 对应的面返回 15，并重写 `onPlace/onRemove` 更新邻居。
- **刷卡机** [CardReaderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/card_reader/CardReaderBlock.java)：`POWERED` 时 `getSignal = 15`，在连接方向 `getDirectSignal = 15`（强充能贴墙方块，使墙另一侧红石线/中继器点亮）。

> **修复记录（v0.0.5）刷卡机无法强充能墙方块**
> - **现象**：刷卡机四周放方块、背后放红石线，没有任何一面被强充能（红石线不亮），但能激活紧邻的红石元件；控制台拉杆/按钮/安全按钮均正常。与 FACING 方向无关。
> - **根因**：Minecraft 红石信号方法（`getSignal`/`getDirectSignal`）的 `direction` 参数是**反向**语义——实际输出到 `direction.getOpposite()` 方向（原版 `LeverBlock#getDirectSignal` Javadoc 明确注明 "directions in redstone signal related methods are backwards"）。旧代码写成 `direction == state.getValue(FACING).getOpposite()`，实际强信号流向 `FACING`（朝外/朝玩家），墙在 `FACING.getOpposite()` 方向，故墙方块永远拿不到强充能，仅剩 `getSignal` 全向 15 能激活紧邻元件。
> - **修复**：改为与参考方块（`ConsoleButtonBlock`/`SafeButtonBlock`/`ConsoleLever3StageBlock`）及原版 `LeverBlock` 一致的写法 `getConnectedDirection(state) == direction`（即 `FACING == direction`），实际强信号流向 `FACING.getOpposite()` = 墙所在方向，正确强充能墙方块与墙后红石线。

#### 5.6 连接方向计算（getConnectedDirection）

“杠杆字节码金标准”——所有贴墙输出型方块共用，把贴面（WALL/CEILING/FLOOR）换算为实际连接方向，供 `getDirectSignal` 判断输出朝向：

```java
protected Direction getConnectedDirection(BlockState state) {
    return switch (state.getValue(FACE)) {
        case CEILING -> Direction.UP;
        case FLOOR  -> Direction.DOWN;
        default     -> state.getValue(FACING);
    };
}
```

### 6. 划线（圆盘记录仪）

#### 6.1 圆盘记录仪数据缓冲（PlotterBlockEntity）

[PlotterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterBlockEntity.java) 继承 `ConsolePanelBlockEntity`，维护长度 24 的环形数据缓冲（值 0~15）。每 `tick()` 按模式间隔右移并采样当前红石信号写入首位：

```java
private static final int DATA_LENGTH = 24;
private int[] data = new int[DATA_LENGTH];
private int mode = 4;          // 1~9，默认4（间隔20tick）
private int tickCounter = ...;

public void tick() {
    int interval = Config.getPlotterTickInterval(mode);
    if (interval <= 0) return;        // 模式1：被动触发，不自动运行
    if (--tickCounter <= 0) {
        tickCounter = interval;
        doSample();
    }
}

public void doSample() {
    System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);  // 整体右移
    data[0] = level.getBestNeighborSignal(worldPosition); // 采样红石强度
    sync();
}
```

模式由多功能工具切换，NBT 保存 `PlotterData / PlotterMode / TickCounter`。

#### 6.2 圆盘记录仪渲染（PlotterBER）

[PlotterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterBER.java) 复用文字渲染管线，用 `█`（FULL BLOCK）字符缩放 1/4 变成 2 像素小方块，红色绘制趋势曲线：

```java
private static final float START_X = 22.0F, START_Y = 48.0F;
private static final float STEP_PIXEL = 2.0F, VALUE_PIXEL = 2.0F;
private static final float SCALE = 0.25F;          // 8px 字符 → 2px 方块
private static final int COLOR_ARGB = 0xFFFF0000;  // 纯红
private static final int PACKED_LIGHT = 0xF000F0;

private void renderPlot(int[] data, PoseStack poseStack, MultiBufferSource bufferSource) {
    poseStack.pushPose();
    poseStack.scale(SCALE, SCALE, SCALE);
    for (int i = 0; i < data.length; i++) {
        int value = Math.max(0, Math.min(15, data[i]));
        if (value <= 0) continue;    // 跳过0值
        float fx = (START_X - i * STEP_PIXEL) * INV_SCALE;        // 向左偏移
        float fy = (START_Y - value * VALUE_PIXEL) * INV_SCALE;   // 向上偏移
        font.drawInBatch(Component.literal("█"), fx, fy, COLOR_ARGB, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                0, PACKED_LIGHT);
    }
    poseStack.popPose();
}
```

绘制前调用 `ConsolePanelBER.pushTextTransform` 进入文字坐标系，先画命名文字再画趋势图。

#### 6.3 圆盘记录仪时钟触发

[PlotterClockBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Plotter/PlotterClockBlockEntity.java) 在检测到红石**上升沿**后，向上扫描 1~8 格，对其中 `mode == 1`（被动触发）的圆盘记录仪执行一次 `doSample()`：

```java
private void triggerAbove() {
    for (int dy = 1; dy <= 8; dy++) {
        BlockEntity be = level.getBlockEntity(base.above(dy));
        if (be instanceof PlotterBlockEntity pbe && pbe.getMode() == 1) {
            pbe.doSample();
        }
    }
}
```

`PlotterClockBlock` 为完整方块，在 `neighborChanged` 中调用实体 `onNeighborChanged()`，NBT 保存 `LastSignal`。

### 7. CC: Tweaked 外设

在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java#L64-L81) 的 `commonSetup` 中通过 `ForgeComputerCraftAPI.registerPeripheralProvider` 注册外设，使数码显示器、数字调节器与数字圆盘记录仪可被 CC 电脑识别：

```java
ForgeComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider() {
    @Override
    public LazyOptional<IPeripheral> getPeripheral(Level world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof DigitalDisplayBlockEntity be)
            return LazyOptional.of(() -> new DigitalDisplayPeripheral(be));
        if (world.getBlockEntity(pos) instanceof DigitalKnobBlockEntity knobBE)
            return LazyOptional.of(() -> new DigitalKnobPeripheral(knobBE));
        if (world.getBlockEntity(pos) instanceof DigitalPlotterBlockEntity plotterBE)
            return LazyOptional.of(() -> new DigitalPlotterPeripheral(plotterBE));
        return LazyOptional.empty();
    }
});
```

- [DigitalDisplayPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayPeripheral.java)：`getType() = "digital_display"`，Lua 接口 `setStatus(text)` / `getStatus()`，修改橙色状态文字。
- [DigitalKnobPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobPeripheral.java)：`getType() = "digital_knob"`，Lua 接口 `setValue(int)` / `getValue()`，读写整数（0~1000，越界钳制；百分比模式不影响 CC 读写值）。

### 8. 多功能工具（MultiToolItem）

[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java) 无耐久不可堆叠，用于切换各类方块的模式：

- **`onItemUseFirst`**：对数字调节器右键切换百分比模式开关（潜行 + 右键重置为开启），返回 `SUCCESS` 拦截原交互。
- **`useOn`**：对圆盘记录仪右键切换模式（1→2→…→9→1，潜行重置为 1），并把新模式的采样间隔以聊天消息告知玩家。
- **`useOn`（控制台结构方块）**：按住右键拖动控制台表面的组件 / 表面文字（详见第 41 节「表面组件拖动」）。

切换后消息键：`message.cc_rc.digital_knob_percent`、`message.cc_rc.plotter_mode`、`message.cc_rc.plotter_mode_passive`。

### 9. 数字圆盘记录仪（DigitalPlotter）

数字圆盘记录仪是完整方块，放置方式同数码显示器（水平四方向，屏幕朝向玩家面对方向），模型使用 Blockbench 制作的自有模型（[digital_plotter.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/block/digital_plotter/digital_plotter.json)，贴图 `textures/block/digital_plotter.png` 64×64）。与圆盘记录仪不同：**没有状态**（不自动采样、无红石触发），通过 CC 电脑读写长度 50 的整数列表（值 0~100，越界自动钳制）。

- [DigitalPlotterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBlock.java)：继承 `Block implements EntityBlock`，仅 `HORIZONTAL_FACING` 状态；`setPlacedBy` 把放置时物品自定义名称写入方块实体作为命名文字。
- [DigitalPlotterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBlockEntity.java)：直接继承 `BlockEntity`，存储 `int[50]` 数据（值 0~100）与命名文字，NBT 保存 `PlotterData / Text`，网络同步三件套。

```java
private static final int DATA_LENGTH = 50;   // 列表长度 50（圆盘记录仪 24）
private static final int MIN_VALUE = 0, MAX_VALUE = 100;

public void push(int value) {                // 写入一个新值：删除最后一位并移位
    value = Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    System.arraycopy(data, 0, data, 1, DATA_LENGTH - 1);
    data[0] = value;
    sync();
}

public boolean setValue(int index, int value) { ... }   // 手动设置某一位，越界不写入
public Integer getValue(int index) { ... }              // 读取某一位，越界返回 null
```

- [DigitalPlotterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterBER.java)：完整方块变换同数码显示器，命名文字（白）一行；趋势线图用 `█` 字符 NORMAL 模式绘制（同圆盘记录仪），点缩放 `PLOT_SCALE = 0.15F`、`STEP_PIXEL = 1.15F`、`VALUE_PIXEL = 0.32F`，50 点以 `START_X = 27.5F`、`START_Y = 23.0F` 居中；命名文字 `NAME_Y = 0.36F`（相对块中心，正数向上），绘图区原点 `PLOT_Y = -0.09F`。
- **朝向修正**：自有模型屏幕位于 −Z（北）侧（数码显示器在 +Z 侧），故 `blockstates/digital_plotter.json` 四朝向 y 旋转在数码显示器基础上整体 +180°（north=180 / south=0 / east=270 / west=90）；BER 沿用 `TEXT_Z=-0.57F`，与旋转 180° 后屏幕正面（模型 z=17 → 世界约 1.06）自动对齐，文字/线条落在屏幕上。
- **Z 分层**：划线平面与命名文字分离，`PLOT_Z = TEXT_Z + 1.0F/16.0F`（向块中心收 1 个模型像素 = 1/16 格），使划线比文字更靠里一层面，避免与文字重叠干扰。
- [DigitalPlotterPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterPeripheral.java)：`getType() = "digital_plotter"`，Lua 接口（索引采用 Lua 惯例 1~50，对应内部 0~49，越界不予读写）：

| Lua 方法 | 说明 |
| --- | --- |
| `push(value)` | 写入一个新值（自动删除最后一位并移位），返回写入后的首位值 |
| `setValue(index, value)` | 手动设置某一位的值，返回是否成功；索引越界返回 false |
| `getValue(index)` | 读取某一位的值；索引越界返回 nil |
| `getList()` | 读取整个列表（长度 50，索引 1~50） |

> 写入值只能为整型；超过 100 按 100 计，小于 0 按 0 计；超出列表范围（<1 或 >50）不予写入或读取。

### 10. 旧模组移植物品（ModToolTiers）

从旧模组（`cdes`，NeoForge 1.21.1 + Create Registrate）移植 5 个物品到本模组（Forge 1.20.1 + `DeferredRegister`）。由于版本不同**未复制代码**，全部按 1.20.1 API 重写：

- [ModToolTiers.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ModToolTiers.java)：按 1.20.1 `Tier` 接口（6 个方法，无 1.21 新增的 `getIncorrectBlocksForDrops`）实现自定义材质 `FROZEN_TILAPIA`（耐久 2、挖掘速度 12、攻击加成 255、挖掘等级 0、附魔能力 0、蓝冰修复）。
- 物品注册见 [ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java#L244-L298)，模型与贴图位于 `assets/cc_rc/models/item/*.json` 与 `textures/item/*.png`。

```java
// 冰冻罗非鱼：1.20.1 用 SwordItem(Tier, 攻击修正, 攻速, Properties)，
// 修改器攻击伤害 = 修正(0) + Tier 加成(255) = +255（实际总伤害 = 基础 1 + 255 = 256）；
// 旧版用属性覆盖实现相同数值
public static final RegistryObject<SwordItem> FROZEN_TILAPIA = ITEMS.register("frozen_tilapia",
        () -> new SwordItem(ModToolTiers.FROZEN_TILAPIA, 0, -2.0F,
                new Item.Properties().stacksTo(1)));

// 冰冻罗非鱼手持模型（特殊设置）：models/item/frozen_tilapia.json 采用 item/handheld 姿态
// （第三人称工具与手臂同平面，rotation [0,-90,55]），保持原版正常大小（0.85/0.68），不放大。

// 沉船绿酒：饮用后剧毒(4)+反胃(3)，各 114514 刻
public static final RegistryObject<Item> GREEN_WINE = ITEMS.register("green_wine",
        () -> new Item(new Item.Properties().stacksTo(16)
                .food(new FoodProperties.Builder()
                        .nutrition(2).saturationMod(0.4F).alwaysEat()
                        .effect(() -> new MobEffectInstance(MobEffects.POISON, 114514, 4), 1.0F)
                        .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 114514, 3), 1.0F)
                        .build())));

// 何意味？：营养11/饱和度4/RARE，食用后同时获得 10 种效果
//（1.20.1 无 TRIAL_OMEN，故比旧版少一种）
public static final RegistryObject<Item> HE_YI_WEI = ITEMS.register("he_yi_wei", () -> new Item(...));

// 合一味：营养30/饱和度0.8/可随时食用
public static final RegistryObject<Item> TASTES_FOOD = ITEMS.register("tastes_food", () -> new Item(...));

// 沉船绿酒桶：普通物品，无特殊功能
public static final RegistryObject<Item> GREEN_WINE_BARREL = ITEMS.register("green_wine_barrel",
        () -> new Item(new Item.Properties()));
```

> 移植要点：① 1.20.1 无 `ItemAttributeModifiers` 组件（1.20.5+ 引入），武器属性改用 `SwordItem` 构造器参数等价实现；② 旧版 `Tier` 的攻击加成 100 在 1.21 中被属性覆盖从未生效，1.20.1 以 Tier 加成 255 + 修正 0 复现实际显示值；③ 旧版 `HE_YI_WEI` 的 `TRIAL_OMEN` 为 1.21 专属效果，1.20.1 不存在，已剔除。

### 11. 描述型物品与新增食物

参考「圆盘记录仪时钟」的悬停描述模式（`DescriptionBlockItem` + 语言 key `item.cc_rc.desc_*`），抽象出三个描述型物品基类，并新增 3 个物品（月饼 × 2、压缩饼干）：

- [DescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DescriptionItem.java)：普通物品 + 描述，构造时可选传入 `ChatFormatting` 颜色为描述文字染色。
- [DescriptionSwordItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DescriptionSwordItem.java)：`SwordItem` + 描述，同样支持可选颜色。
- [AttackDescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/AttackDescriptionItem.java)：普通物品 + 描述 + 主手攻击伤害属性修饰符（非武器、无耐久、可堆叠），用于"五金月饼"。
- 四个描述类（`DescriptionBlockItem` / `DescriptionItem` / `DescriptionSwordItem` / `AttackDescriptionItem`）均继承对应物品基类并在 `appendHoverText` 中追加翻译文本。

```java
// 五金月饼：普通物品注册（非武器、最大堆叠 16、无耐久），
// 修饰符 9.0 + 空手基础 1.0 = 实际攻击 10（工具提示 "+10 Attack Damage"）
public static final RegistryObject<Item> MOON_CAKE_IRON = ITEMS.register("moon_cake_iron",
        () -> new AttackDescriptionItem(new Item.Properties().stacksTo(16)
                        .food(new FoodProperties.Builder()
                                .nutrition(4).saturationMod(2F).alwaysEat()
                                .effect(() -> new MobEffectInstance(MobEffects.WITHER, 40, 1), 1.0F)
                                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4000, 1), 1.0F)
                                .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4000, 0), 1.0F)
                                .build()),
                "item.cc_rc.desc_moon_cake_iron", 9.0D));

// 沉船绿酒：描述文字染色为绿色
public static final RegistryObject<Item> GREEN_WINE = ITEMS.register("green_wine",
        () -> new DescriptionItem(new Item.Properties().stacksTo(16)
                .food(...), "item.cc_rc.desc_green_wine", ChatFormatting.GREEN));
```

新物品清单与数值：

| 物品 ID | 类 | 功能 |
| --- | --- | --- |
| `moon_cake` | `Item` | 五仁月饼：营养 8 / 饱和度 4，可随时食用（饥饿值满也可，类似金苹果），食用后 200 秒（4000 刻）速度 II，堆叠 16 |
| `moon_cake_iron` | `AttackDescriptionItem` | 五金月饼：普通物品（无耐久、堆叠 16），主手攻击伤害 10，营养 4 / 饱和度 2，可随时食用（饥饿值满也可，类似金苹果），食用后 2 秒（40 刻）凋零 II + 200 秒（4000 刻）力量 II 与抗性提升，描述"机加工这一块/." |
| `ship_biscuit` | `DescriptionItem` | 压缩饼干：营养 12 / 饱和度 6，食用后 60 秒（1200 刻）饱和效果 + 20 秒（400 刻）生命恢复，描述"*量大管饱*" |

描述改动（均沿用 `item.cc_rc.desc_*` 语言 key）：

- `digital_knob`：`BlockItem` → `DescriptionBlockItem`，描述"使用多功能工具右键切换显示模式"。
- `frozen_tilapia`：`SwordItem` → `DescriptionSwordItem`，描述"三体宇宙最强武器（bushi）"，颜色 `GOLD`（金色），物品名 `Rarity.RARE`（显示蓝色）。
- `green_wine`：`Item` → `DescriptionItem`，描述"这酒能喝吗？……都绿了"，颜色 `GREEN`。
- `he_yi_wei`：`Item` → `DescriptionItem`，描述"*你想何出怎样的意味？*"，颜色 `DARK_PURPLE`（紫）。

> 数值换算：1 秒 = 20 游戏刻，速度 II = `MOVEMENT_SPEED` 等级 2（amplifier 1），凋零 II = `WITHER` 等级 2，力量 II = `DAMAGE_BOOST` 等级 2，抗性提升未指定等级用 1 级（amplifier 0），生命恢复 1 级（amplifier 0）。

### 12. 搬运物品与搬运物品栏

从外部模组（如农夫乐事）搬运装饰性物品到本模组，模型与贴图**本地化为 `cc_rc` 命名空间**（源资源引用 `farmersdelight:` 命名空间，但项目未依赖该模组）：

- **箱装土豆方块**（`potato_crate`，`Block`）：搬运自农夫乐事，无方向完整方块，木板材质（`MapColor.WOOD` 地图颜色 + `SoundType.WOOD` 木质音效，强度 2.0），仅作装饰展示用。模型复用原版 `cube_bottom_top` 父模型，贴图三张（底/侧/顶）拷贝至 `assets/cc_rc/textures/block/potato_crate/`，模型引用改写为 `cc_rc:block/potato_crate/*`；无方向方块故 blockstate 使用单变体 `""`。
- **箱装土豆物品**（`potato_crate`，`DescriptionBlockItem`）：关联上述方块（方块物品），悬停显示两条带样式描述——第一行"搬运自农夫乐事"（黄色 `YELLOW` + 斜体 `ITALIC`）标识搬运来源，第二行"！？服务器 ？！"（淡蓝 `AQUA` + 粗体 `BOLD`）。为此扩展 `DescriptionBlockItem` 支持传入多个已构建样式的 `Component` 逐行显示。
- **搬运物品栏**（`cc_rc_tab_carried`，标题"CCRC：搬运的物品"）：Forge 按注册名对模组创造标签排序，注册名 `cc_rc_tab_carried` 前缀比主标签 `cc_rc_tab` 更长，按字母序排在其后；图标为箱装土豆，仅收录搬运类物品；箱装土豆与全部粗布告示牌**不加入**主物品栏，只出现在此标签中。
- **16 色粗布告示牌与悬挂式粗布告示牌**（`<颜色>_canvas_sign` 系列，搬运自农夫乐事）：完全沿用原版告示牌机制——每个颜色注册 4 个方块（立式 `StandingSignBlock` / 壁挂 `WallSignBlock` / 天花板悬挂 `CeilingHangingSignBlock` / 壁挂悬挂 `WallHangingSignBlock`），**每种颜色注册各自独立的 `WoodType`** `"cc_rc:canvas_<颜色>"`（[ModWoodTypes.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModWoodTypes.java) 中循环注册 16 个，复用 `BlockSetType.OAK`，顺序与 `ModBlocks.CANVAS_SIGN_COLORS` 一致）。物品为两个：`<颜色>_canvas_sign`（`DescriptionSignItem`，立式/壁挂共用）与 `<颜色>_hanging_canvas_sign`（`DescriptionHangingSignItem`，天花板/壁挂悬挂共用），悬停显示**黄色斜体**描述"搬运自农夫乐事"（复用 key `item.cc_rc.desc_potato_crate_carried`）。模型/贴图本地化为 `cc_rc` 命名空间：blockstate 全部指向 `cc_rc:block/canvas_sign`（仅 particle 纹理，世界渲染由原版 SignRenderer/HangingSignRenderer 完成），物品图标用 `item/generated` + `cc_rc:item/<名>` 贴图（普通与悬挂各 16 张独立图标，均直接拷贝自农夫乐事源 `textures/item/`）。
  - **编辑文字 GUI 背景（关键）**：原版 `HangingSignEditScreen` 的编辑界面背景贴图路径由 `woodType.name()` 加 `textures/gui/hanging_signs/` 前缀拼出——木种 `cc_rc:canvas_<颜色>` 解析为 `cc_rc:textures/gui/hanging_signs/canvas_<颜色>.png`（16×16，blit 到屏幕）。该目录原未提供贴图导致编辑 GUI 紫黑。已按各色生成 16 张背景：以原版 `minecraft:textures/gui/hanging_signs/oak.png` 为模板（16×16，保留链条金属色与透明区域），面板区域按对应染料色（`DyeColor` RGB）重着色并保留明暗层次，生成脚本 `模型/农夫乐事/gen_hanging_gui.ps1`。
  - **告示牌面板贴图（关键）**：原版 `Sheets.createSignMaterial` 用 `entity/signs/<name>.png`、`createHangingSignMaterial` 用 `entity/signs/hanging/<name>.png` 作为材质路径（两者模板不同）。每种颜色注册独立 WoodType 名（如 `"cc_rc:canvas_white"`，命名空间 `cc_rc` + path `canvas_white`）后，材质路径自动解析为各色专属文件，因此必须提供 32 张贴图：
    - 普通式（16 张）：`assets/cc_rc/textures/entity/signs/canvas_<颜色>.png`
    - 悬挂式（16 张）：`assets/cc_rc/textures/entity/signs/hanging/canvas_<颜色>.png`
    32 张贴图均**直接拷贝自农夫乐事源贴图** `模型/农夫乐事/canvas_sign/signs/`（普通式 64×32、悬挂式 64×32，原版 oak 实体贴图 UV 布局），确保 UV 布局与 `SignRenderer.createSignLayer` / `HangingSignRenderer.createHangingSignLayer` 的模型完全匹配；若仅自绘整图边框会与模型 UV 采样区域错位（面板显示错乱），且缺悬挂式贴图会导致紫黑块。
  - **自定义方块实体类型**：原版 `BlockEntityType.SIGN`/`HANGING_SIGN` 的 validBlocks 不含本模组方块，`BlockEntityRenderDispatcher` 渲染前会做 `getType().isValid(blockState)` 校验并跳过渲染（告示牌透明、无文字）。故注册自定义类型 `canvas_sign_be`/`canvas_hanging_sign_be`（[ModBlockEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlockEntities.java)），方块用 4 个自定义子类返回自定义实体，客户端为两类型注册原版 `SignRenderer`/`HangingSignRenderer`（详见「三、3 方块实体」）。模型层与材质仍由原版机制按 `WoodType` 自动生成，无需手动注册。

```java
// 箱装土豆方块：无方向完整方块，木板材质（木质地图颜色/音效），仅作装饰展示
public static final RegistryObject<Block> POTATO_CRATE = BLOCKS.register("potato_crate",
        () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0F)
                .sound(SoundType.WOOD)));

// 箱装土豆物品：方块物品，两行带样式描述（黄斜体来源标识 + 淡蓝粗体装饰语）
public static final RegistryObject<Item> POTATO_CRATE = ITEMS.register("potato_crate",
        () -> new DescriptionBlockItem(ModBlocks.POTATO_CRATE.get(), new Item.Properties(),
                Component.translatable("item.cc_rc.desc_potato_crate_carried").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC),
                Component.translatable("item.cc_rc.desc_potato_crate").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));

// 16 色粗布告示牌：每色 4 个方块（立式/壁挂/天花板悬挂/壁挂悬挂），
// 每种颜色独立的 WoodType "cc_rc:canvas_<颜色>"（ModWoodTypes.CANVAS_TYPES[i]）
// 方块需使用自定义子类（newBlockEntity 返回 CanvasSignBlockEntity，配合自定义方块实体类型
// canvas_sign_be 通过渲染调度器的 isValid 校验，详见三、3 方块实体的说明）
// 例：立式
public static final RegistryObject<Block> WHITE_CANVAS_SIGN = BLOCKS.register("white_canvas_sign",
        () -> new CanvasStandingSignBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .noCollission()
                .strength(1.0F), ModWoodTypes.CANVAS_TYPES[0]));

// 例：物品（立式/壁挂共用 SignItem，黄斜体"搬运自农夫乐事"）
public static final RegistryObject<Item> WHITE_CANVAS_SIGN_ITEM = ITEMS.register("white_canvas_sign",
        () -> new DescriptionSignItem(new Item.Properties(),
                ModBlocks.CANVAS_SIGN_BLOCKS.get(0).get(),
                ModBlocks.CANVAS_WALL_SIGN_BLOCKS.get(0).get(),
                Component.translatable("item.cc_rc.desc_potato_crate_carried").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));

// 搬运物品栏：注册名 cc_rc_tab_carried 按注册名排序位于主标签之后，仅收录搬运类物品，图标为箱装土豆
public static final RegistryObject<CreativeModeTab> CC_RC_CARRIED_TAB = CREATIVE_TABS.register("cc_rc_tab_carried",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.cc_rc_carried"))
                .icon(() -> new ItemStack(ModItems.POTATO_CRATE.get()))
                .displayItems((parameters, output) -> output.accept(ModItems.POTATO_CRATE.get()))
                .build());
```

### 13. 冰箱与水槽

从 Cooking for Blockheads（Cfb）搬运另外两个方块：冰箱、水槽。参考代码（`模型/搬运/冰箱`、`模型/搬运/水槽`）依赖 **Balm API**，本项目不引入 Balm，全部按纯 Forge 原版 API 重写简化：

- **冰箱（`fridge`）**：[FridgeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/fridge/FridgeBlock.java) 为水平四方向完整方块（`FACING`），实现 `EntityBlock` + `use` 右键打开容器菜单；[FridgeBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/fridge/FridgeBlockEntity.java) 继承原版 `RandomizableContainerBlockEntity`（27 格容器，支持掉落物表与自定义命名），GUI **复用原版箱子界面**（`ChestMenu.threeRows`），破坏时 `onRemove` 调用 `Containers.dropContents` 掉落容器内容（与原版箱子一致）。
- **水槽（`sink`）**：[SinkBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/sink/SinkBlock.java) 为水平四方向完整装饰方块（`FACING`），**无方块实体**（参考代码的流体/洗锅边逻辑依赖 Balm 与 Cfb 注册表，已简化掉）。右键手持物品时通过 `RecipeManager` 查询 `cc_rc:sink_conversion` 类型配方执行四向转换：桶→水桶、玻璃瓶→水瓶、水桶→桶、水瓶→玻璃瓶（数据驱动，配方文件在 `data/cc_rc/recipes/`，详见 14 节）。背包已满时不消耗原物品并返回 `FAIL`。
- **放置朝向（修复）**：两方块模型正面（冰箱门 / 水槽柜门+水龙头）位于模型 **-Z 侧**。`getStateForPlacement` 若直接取 `context.getHorizontalDirection()`（玩家朝向），正面会朝向远处、玩家看到的是背面。已改为 **`getHorizontalDirection().getOpposite()`**（同原版熔炉 `FurnaceBlock` 逻辑），使放置后正面朝向玩家。
- **缝隙透视（修复）**：模型非完整 16×16×16 方块（冰箱元素多在 1~15 留缝、水槽元素非满格），但默认遮挡形状（`getOcclusionShape` → 默认返回完整碰撞箱形状）会让渲染器**剔除相邻完整方块朝向本方的面**，从缝隙处透视出虚空/邻块内部。已覆写 `getOcclusionShape` 返回 `Shapes.empty()`（同玻璃做法），使相邻完整方块的面正常渲染，缝隙不再透视。
- **资源**：模型/贴图本地化为 `cc_rc` 命名空间（源引用 `cookingforblockheads:` → `cc_rc:`）。冰箱模型沿用参考的完整静态模型（含门/把手/合页/密封条/层板，`parent: block/block`），贴图 4 张（side/top/back/inside）拷入 `assets/cc_rc/textures/block/fridge/`；水槽模型引用 `minecraft:block/polished_andesite`、`terracotta`、`black_terracotta` 与 `cc_rc:block/sink/sink_metal`（原 `handle` 纹理缺失，改用 `sink_metal`）。blockstate 按 `FACING` 四朝向旋转。
- **数据**：掉落表 `data/cc_rc/loot_tables/blocks/fridge.json`（掉落自身 + 复制方块实体自定义名）、`sink.json`；合成配方 `data/cc_rc/recipes/fridge.json`（箱子 + 铁门）、`sink.json`（铁锭×6 + 陶瓦×6 + 水桶，原 `balm:` 标签改写为原版物品）。

```java
// 冰箱方块：水平四方向完整方块，右键打开容器菜单（服务端），破坏时掉落容器内容
public class FridgeBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // registerDefaultState / createBlockStateDefinition 添加 FACING
    // newBlockEntity -> new FridgeBlockEntity(pos, state)
    // use -> player.openMenu((FridgeBlockEntity) blockEntity)
    // onRemove -> Containers.dropContents(level, pos, (FridgeBlockEntity) blockEntity)
}

// 冰箱方块实体：27 格容器，复用原版箱子 GUI（ChestMenu.threeRows）
public class FridgeBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override public int getContainerSize() { return 27; }
    @Override protected Component getDefaultName() { return Component.translatable("container.cc_rc.fridge"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);   // 原版箱子 3 行界面
    }
}
```

### 14. 自定义配方类型（sink_conversion）

将水槽的四向转换（桶↔水桶、玻璃瓶↔水瓶）做成**数据驱动配方**：由 [`ModRecipes.java`](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModRecipes.java) 注册自定义配方类型 `cc_rc:sink_conversion`（`RecipeType` + `RecipeSerializer` 各一），配方 JSON 放在 `data/cc_rc/recipes/`，水槽右键时经 `RecipeManager` 查询匹配。

- **为什么需要自定义配方类型**：原版 `Ingredient` 不支持 NBT 匹配，而四向转换中的"水瓶"是 `minecraft:potion` + NBT `{Potion:"minecraft:water"}`，输入需要按 NBT 区分"水瓶 vs 其他药水"，输出需要能写出带 NBT 的产物——原版 JSON 配方无法表达，故自研。
- **[SinkConversionRecipe.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/recipe/SinkConversionRecipe.java)**：实现 `Recipe<Container>`。核心是 `matchesItem(ItemStack)`（物品类型 + 输入 NBT 部分匹配——物品标签包含 inputNbt 全部键值即命中）、`getOutputCopy()`（返回含输出 NBT 的产物副本）。
- **[SinkConversionSerializer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/recipe/SinkConversionSerializer.java)**：JSON 格式（SNBT 字符串写 NBT，`TagParser.parseTag` 解析）：

```json
{
  "type": "cc_rc:sink_conversion",
  "input":      { "item": "minecraft:glass_bottle" },
  // "input_nbt":  "{Potion:\"minecraft:water\"}",    可选：输入需包含的 NBT（部分匹配）
  "output":     { "item": "minecraft:potion", "count": 1 },
  "output_nbt": "{Potion:\"minecraft:water\"}"         可选：写入产物的 NBT
}
```

- **SinkBlock.use** 不再硬编码转换：遍历 `recipeManager.getAllRecipesFor(SINK_CONVERSION_TYPE)` 找 `matchesItem(heldItem)` 的第一个匹配配方，取其 `getOutputCopy()` 作为产物，消耗 1 个输入后给予玩家（堆叠为 1 直接替换手持、否则入背包再扣减；背包满返回 `FAIL` 不消耗），并按输入/输出类型播放装/倒水桶（瓶）音效。
- **配方文件（4 个，`data/cc_rc/recipes/`）**：

| 文件 | 转换 |
| --- | --- |
| `sink_bucket_to_water_bucket.json` | 桶 → 水桶 |
| `sink_bottle_to_water_bottle.json` | 玻璃瓶 → 水瓶（输出 NBT：Potion 水） |
| `sink_water_bucket_to_bucket.json` | 水桶 → 桶 |
| `sink_water_bottle_to_bottle.json` | 水瓶（输入 NBT：Potion 水）→ 玻璃瓶 |

### 15. 包子（bao_zi）

包子是「食物 + 投掷炸弹」二合一物品，是本模组第一个**弹射物实体**：

- **物品 [BaoZiItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaoZiItem.java)**：注册 `bao_zi`，最大堆叠 16，`FoodProperties` 营养 4 / 饱和度 2，右键食用；悬停描述"包子雷？"（红色斜体）。加入主创造标签。
- **弹射物实体 [BaoZi.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/BaoZi.java)**：实体类型 `cc_rc:bao_zi`（[ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java) 中 `EntityType.Builder.of(BaoZi::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)`），继承原版 `Snowball`，**飞行逻辑完全沿用雪球**（重力、旋转等）。重写 `onHit(HitResult)`：命中任意实体或方块时调用 `level().explode(this, x, y, z, 4.0F, Level.ExplosionInteraction.NONE)`——`NONE` 交互只造成实体爆炸伤害 + 原版爆炸粒子/音效，不破坏方块、无方块掉落，然后 `discard()`。
  - **投掷构造注意**：不能直接用 `super(level, shooter)`（原版 `Snowball(Level, LivingEntity)` 会硬编码 `EntityType.SNOWBALL`），且 `Snowball` 没有 `(EntityType, LivingEntity, Level)` 构造，故先按 `ModEntities.BAO_ZI.get()` 构造，再手动复刻 `ThrowableProjectile` 便利构造器逻辑（`setPos(shooter.getX(), shooter.getEyeY()-0.1, shooter.getZ())` + `setOwner(shooter)`）。
  - **渲染**：客户端 `ClientModEvents` 为 `bao_zi` 注册原版 `ThrownItemRenderer`（实现 `ItemSupplier`，按 `getDefaultItem()` 兜底显示 `bao_zi` 物品贴图）。
- **左键投掷**：Forge 客户端事件 `InputEvent.InteractionKeyMappingTriggered`（[ClientInputHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ClientInputHandler.java)）——当 `event.isAttack()` 且主手持 `bao_zi` 时 `event.setCanceled(true)` 取消默认攻击，并通过简单网络通道（[ModNetwork.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ModNetwork.java)、[ThrowBaoZiPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ThrowBaoZiPacket.java)）发送 C2S 数据包；服务端校验主手物品后生成弹射物 `shootFromRotation(player, ...初速1.5)` 并消耗 1 个包子（创造模式不消耗）。该事件在 `Minecraft.startAttack` 的 `while(keyAttack.consumeClick())` 循环中触发，每次点击触发一次。
  - **投掷冷却（0.5 秒）**：服务端使用原版物品冷却（`player.getCooldowns()`）作权威校验——发放前 `isOnCooldown` 则忽略，否则 `addCooldown(item, 10)`（常量 `BaoZiItem.THROW_COOLDOWN_TICKS = 10` = 0.5 秒，见 [BaoZiItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaoZiItem.java)），防止连续投掷/作弊；客户端在发送数据包前也检查同一冷却以减少无效发包，且 HUD 会显示冷却动画。

### 16. 法棍（baguette）

法棍是「食物 + 攻击伤害 + 较强击退」的普通物品（无方块关联）：

- **物品 [BaguetteItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/BaguetteItem.java)**：注册 `baguette`，最大堆叠 16，`FoodProperties` 营养 2 / 饱和度 2，右键食用。加入主创造标签。
- **攻击伤害**：仿照 [AttackDescriptionItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/AttackDescriptionItem.java) 的属性修饰符模式，仅主手附加 `Attributes.ATTACK_DAMAGE` 修饰符 3.0（复用原版 `Item.BASE_ATTACK_DAMAGE_UUID`）——修饰符 3.0 + 空手基础 1.0 = **实际攻击 4**（工具提示 "+4 Attack Damage"）。
- **击退属性**：仅主手附加 `Attributes.ATTACK_KNOCKBACK = 3.0`（`Operation.ADDITION`，固定 UUID）。原版击退附魔每级 +1，3.0 等效击退 III，属于较强击退；无武器耐久、无攻速惩罚。
- **描述**：悬停显示"坚如磐石"（**粗体棕色**——棕色非 ChatFormatting 内置色，使用 `Style.EMPTY.withBold(true).withColor(0x835432)` 与原版棕色染料同色）。

### 17. 撬棍（crowbar）

撬棍是铁质剑类武器（[CrowbarItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/CrowbarItem.java)），参照冰冻罗非鱼的自定义 Tier 模式：

- **Tier [ModToolTiers.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ModToolTiers.java)**：新增枚举 `CROWBAR`（耐久 1024、挖掘速度 6、**攻击伤害加成 19**、挖掘等级 2、附魔能力 10、铁锭修复）。
- **武器**：注册 `crowbar`，`CrowbarItem extends SwordItem` 天然**自带横扫攻击**；构造参数伤害修正 0 + Tier 加成 19 = **修改器攻击伤害 +19**（实际总伤害 = 基础 1 + 19 = 20）；攻速修正 **-3.0（攻击速度 1.0，挥动很慢）**；不可堆叠。
- **暴击增强**：内部静态事件类 `CrowbarItem.CritHandler` 监听 Forge 总线 `CriticalHitEvent`——主手持撬棍且为原版暴击（跳跃下落攻击）时 `setDamageModifier(2.0F)`，暴击伤害由原版 1.5 倍提升到 **2.0 倍**（en_US 文案"The Holy Sword of Physics"）。
- **打击音效**：`CritHandler` 另监听 `LivingHurtEvent`，攻击者为主手持撬棍的玩家时，在被击实体位置播放 **原版铁砧落地音效 `SoundEvents.ANVIL_LAND`**（`SoundSource.BLOCKS`，服务端广播，即"哐"的金属撞击声，与铁砧放置 `ANVIL_PLACE` 同源文件）。
- **描述两行**：`"物理学圣剑"`（浅蓝色 `AQUA` + 粗体 `BOLD`）、`"f(x)dx"`（深蓝色 `DARK_BLUE`，无删除线）。
- **手持模型（特殊设置）**：`models/item/crowbar.json` 采用 `item/handheld` 姿态（第三人称为原版手握工具姿态：工具与手臂同一平面，rotation `[0,-90,55]`，而非手持物品的垂直姿态），保持**原版正常大小**（第三人称 `0.85`、第一人称 `0.68`），不放大（仅简易长矛放大）。

### 18. 红石信号发射器与接收器（redstone_sender / redstone_receiver）

红石信号**发射器**与**接收器**组成"无线"红石传输对（[RedstoneSenderBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Redstone_transmit/RedstoneSenderBlock.java) 与 [RedstoneReceiverBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/Redstone_transmit/RedstoneReceiverBlock.java)），贴图来自"模型/红石传输"：

- **发射器 `redstone_sender`**：完整方块，可朝 **x±/y±/z± 六方向**放置（`BlockStateProperties.FACING`），模型 `cube_bottom_top`（顶/底/侧贴图各一套 on/off，侧面贴图按"指向上方放置"绘制）。覆写 Forge 扩展 `canConnectRedstone=true` 连接红石线。`getStateForPlacement` 用 `getNearestLookingDirection().getOpposite()`（**面向玩家**，与原版活塞一致）。
  - `neighborChanged`（服务端）读取 `level.getBestNeighborSignal(pos)` 作为自身收到的信号强度，存入 `POWER`（0~15），**信号由 0 变非 0 切 on 贴图、由非 0 变 0 切 off 贴图**（blockstate `power=1|2|...|15` → on 模型）。
  - 信号变化时沿 `FACING` 方向**从距离 2（不含相邻方块）到配置距离**逐格扫描（`pos.relative(facing, d)`，d 从 2 起），遇 `RedstoneReceiverBlock` 即把其 `POWER` 写成相同强度。最大距离取自 `Config.getRedstoneTransmitRange()`。
- **接收器 `redstone_receiver`**：无方向完整方块（无 FACING），模型 `cube_all`（单贴图 off/on）。覆写 `canConnectRedstone=true` 连接红石线；`isSignalSource=true` 且 `getSignal` 返回 `POWER`，即**向相邻方块发出当前强度的红石信号**（可被红石线、拉杆、机器等识别）。`POWER` 由发射器直接写入，同样由 0↔非 0 切换 on/off 贴图。
- **配置 [Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)**：新增 `redstone_transmit.transmit_range`（默认 **8**，范围 2~64），控制发射器沿面向方向的扫描距离。
- **blockstate**：两个方块均用 `multipart` + **管道 OR 语法**（`power=1|2|...|15`，与原版红石线的 `side|up` 同款写法）实现 0→off / 非 0→on 的模型切换；发射器另按 `FACING` 施加 `x/y` 旋转。
  - > **修复记录（放置后紫黑块）**：`when` 条件里最初写了范围语法 `power=1..15`，但 1.20.1 的 blockstate 条件解析器（`KeyValueCondition`）只支持 `|` 分隔的枚举值，不支持 `..` 范围（范围语法仅限指令参数 `BlockStateParser`），导致 blockstate 整体加载失败、世界内渲染紫黑缺失模型；物品栏模型不经过 blockstate 所以正常。已改为 `1|2|3|4|5|6|7|8|9|10|11|12|13|14|15` 修复。
  - > **修复记录（水平朝向贴图朝上）**：发射器是 `cube_bottom_top`（顶面为发射面），但水平朝向（N/S/E/W）原先只给了 `y` 旋转（绕竖轴自转），顶面仍朝上，看起来"模型方向都对着上方"。已改为给水平朝向加 `x` 旋转：`north → x:90`、`south → x:270`、`east → x:90,y:90`、`west → x:90,y:270`（参照本模组 plotter 的同款 `x` 先于 `y` 组合写法），使顶面发射面指向对应水平方向；`up/down` 保持不变（`up` 不转、`down → x:180`）。

### 19. F.A.A.S服务器（server_faas）

[ServerFaasBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/server_faas/ServerFaasBlock.java) 是三种变体共用的装饰方块类（`server_faas_1/2/3`），模型来源"模型/FAAS/FAAS新"（Blockbench Bedrock 版转为 Java block 模型，含 22.5°/45° 旋转节点，UV 顶/底面已翻转适配），贴图 `textures/block/server_faas/faas_1~3.png`：

- **放置**：继承 `HorizontalDirectionalBlock`，水平四方向（N/S/E/W）放置，`getStateForPlacement` 用 `context.getHorizontalDirection()`（**面向玩家**，未加 `getOpposite()`），与奶龙玩偶一致。
- **音效**：靠近时持续循环播放 `server_noise`。播放逻辑由客户端监听器 [ServerFaasSoundHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ServerFaasSoundHandler.java)（Dist.CLIENT）负责——每 20 tick 扫描玩家周围 16 格内的 FAAS 方块，为每个方块创建 `loop=true` 的 `AbstractTickableSoundInstance`（`FaasLoopingSound`）并交给 SoundManager 无缝循环播放；玩家远离/方块被破坏/离开世界时停止并移除实例。声音文件 `sounds/server_noise.ogg`（当前为约 20 秒长循环环境音 `server_loop_10_whisper.ogg`，loop=true 无缝循环，时长不影响听感）。音效归类 `SoundSource.BLOCKS`（受游戏设置「音效/方块」滑块控制音量，非主音量）；**距离衰减完全手动控制**：音效标记 `relative=false`（世界坐标定位）并关闭引擎衰减，由管理器每 tick 调用 `updateVolume()` 按玩家与音源距离计算线性音量（16 格内 100%→0%，带 lerp 平滑），扫描停止距离 20 格（16 格衰减 + 4 格裕量防边界抖动）。
  - > **修复记录（原音效断续/重叠）**：最初在 `animateTick` 中按概率（`random.nextFloat() < 0.2F`）`playLocalSound` 播放，仿原版营火/火把逻辑——但概率触发导致音效时有时无，且音效较长时会多次触发互相重叠。已改用上述"循环音效实例 + 距离开关"，移除 `animateTick` 覆写与随机播放。
  - > **修复记录（SoundEngine 循环音效不衰减）**：委托 SoundEngine 的 `Attenuation.LINEAR` 实测仍无衰减——循环音效（looping）的音量在 SoundEngine 中**不随玩家移动刷新**（只在低频 tick/播放时按初始距离设定一次）。已改为 `relative=true` + `attenuation=NONE` 关闭引擎衰减，由 `ServerFaasSoundHandler` 每 tick 调用 `updateVolume()` 按玩家与方块距离手动计算线性音量（16 格内 100%→0%，带 lerp 平滑过渡），衰减必定随距离生效；同时把清理实例的停止距离从 24 格收敛到 20 格。`getSource()` 显式返回 `SoundSource.BLOCKS` 保证「音效/方块」滑块可调节音量。
  - > **修复记录（单声道：relative=true 导致只有一边有声音）**：用户反馈"无论玩家面向什么方向，只有一边声道有声音"。**根因**：上一条修复中为关闭引擎衰减把实例标为 `relative=true`（相对播放）——SoundEngine 会把声源位置固定在 listener（玩家）自身，OpenAL 无法按声源相对玩家的方向计算左右声道（pan），方向向量为零 → **声道固定单边**。**修复**：改为 `relative=false`（世界坐标定位，声源=方块中心），OpenAL 按玩家朝向正确计算立体声（正对时双耳都有声音）；引擎衰减仍由 `attenuation=NONE` 关闭（音量完全手动），与 RWR 追击音（EvilGajinRwrSound 同模式）一致。gradlew build BUILD SUCCESSFUL（38s）。
  - > **修复记录（模型替换）**：FAAS 模型由"模型/FAAS"旧版替换为"模型/FAAS/FAAS新"（faas_1/2/3 三组 Blockbench 导出），重新转换生成 `models/block/server_faas/faas_1~3.json`（55/52/49 元素）并更新贴图，blockstate 与 item 模型引用不变。
  - > **修复记录（UV 坐标误缩放）**：FAAS 新模型与按钮 4/5、密码输入器源 JSON 虽标注 Bedrock 格式，但 UV 实为 Java 0-16 归一化坐标（与贴图内容区域验算吻合，如 faas 贴图 512×512 内容 0~378px ↔ UV 11.875×512/16≈380px）；曾误按 `texture_size` 像素缩放导致贴图错位残缺，已统一改为"UV 直接沿用 + 顶/底面方向翻转"并重建全部 9 个模型（faas_1~3、console_button_4/5 × off/on、password_inputer × off/on）。
- **性质**：完整方块（模型元素可超出方块边界渲染），金属音效 `SoundType.METAL`、强度 3.0/6.0、`requiresCorrectToolForDrops`；为三种变体各补一张 `loot_tables/blocks/server_faas_N.json` 使方块可掉落。
  - > **修复记录（循环音效炸响）**：玩家靠近开播时"炸响"、远离一定距离后"连续不断炸响"。两级根因：① 运行时 `server_noise.ogg` 是 **20 秒**文件而素材源 `模型/FAAS/server_noise.ogg` 只有 **8 秒**（历史处理把 8s 素材拼接/拉长成 20s）——循环播放时每到内部拼接点（8s/16s 处）与循环点波形跳变 → 一阵阵爆音；已用素材源重新转码为 **8 秒单声道 44.1k 160kbps**（`-ac 1 -ar 44100 -c:a libvorbis -b:a 160k`），循环点即素材原子状态；② `ServerFaasSoundHandler.FaasLoopingSound` 构造器 `volume` **固定 = BASE_VOLUME(1.0)**，玩家在 16 格扫描边缘进入范围也满音量开播、且 16~20 格边界反复进出会反复重建音效 → 每次进入/重建"炸响"；已改为**构造时按玩家与方块实际距离初始化音量**（`clamp(1 - dist/16)`，边缘≈0，开播即正确响度，updateVolume 再平滑收敛）。gradlew build BUILD SUCCESSFUL（38s）。
  - > **修复记录（循环点音量起伏）**：炸响消除后长时间听仍有"衔接处降低又升高"。**最终根因**：素材源 `模型/FAAS/server_noise.ogg` **开头 0~0.4s 有 fade-in**（100ms RMS -26→-13dB）**、结尾 7.8~8.0s 有 fade-out**（-14→-26dB）——循环从"尾 -11.5dB"跳到"头 -18dB 再爬升"，每 8 秒一次"降低又升高"。**最终修复**：ffmpeg 裁剪**纯净中段 `-ss 0.5 -t 7.2`**（避开 fade-in 0.5s 与 fade-out 0.3s，7.2s），`volume=-1.5dB` 留削波余量、mono 160kbps——首尾 100ms RMS **-12.79 vs -12.43dB（差 0.36dB）**，循环点能量无缝；0.5s 段波动 -11.9~-13.7dB（噪声自然起伏，无 fade 低谷/高峰）；峰值 -0.27dB 无削波。**代码层**：反编译 SoundEngine `m_120326_`(tick) 确认**每 tick 调用 `m_120324_` 重算音量**（读取实例 volume 字段）——`updateVolume` 由 `Mth.lerp(0.2F,...)` 平滑改为**绝对设置**（`volume = clamp(1-dist/16)*BASE_VOLUME`），静止时音量字节级恒定、移动严格跟随距离，杜绝缓变残留。**弃用**：dynaudnorm 帧增益（噪声上产生泵动、首尾补偿不足）、acrossfade 交叉淡化（FFmpeg 4.2 崩溃）、PCM 交叉混合（fade 区能量凹）。gradlew build BUILD SUCCESSFUL（28s）。

### 20. 说明书1（instruction_book_1）

[InstructionBookItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBookItem.java) 采用**原版成书（WrittenBookItem）**样式注册的说明书物品（直接继承 `WrittenBookItem`）：

- **机制**：继承原版成书类；**右键打开书籍界面的逻辑已移交给客户端事件监听器 [InstructionBookClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/InstructionBookClientHandler.java)**（`Dist.CLIENT` 注册，监听 `PlayerInteractEvent.RightClickItem`，仅处理 `LogicalSide.CLIENT` 侧，直接 `Minecraft.setScreen(new BookViewScreen(...))` 打开）；贴图使用 `textures/item/instruction_book_1.png`（来源"模型/其他物品/instruction_book_1.png"），模型 `models/item/instruction_book_1.json`（`item/generated` + layer0）。
- **内容注入**：`createBook()` 方法构造带完整成书 NBT 的 ItemStack——`title="说明书1"`、`author="海鸥的红石"`、`resolved=true`、`generation=0`、`pages`（每页为 `Component.Serializer.toJson(Component.literal(...))` 生成的 JSON 文本组件字符串，支持 `\n` 换行）。创造标签 `output.accept(ModItems.INSTRUCTION_BOOK_1.get().createBook())` 发放带内容的书本。
- **页面结构**（共 8 页）：封面 → 目录 → 控制面板类 → 编辑工具（控制面板类续）→ 圆盘记录仪 → 断路器 → 刷卡机 → 红石信号发射/接收器。
- **注册**：`ModItems.INSTRUCTION_BOOK_1`（`instruction_book_1`，堆叠 1）；lang 中英"说明书1 / Instruction Book 1"。
  - > **修复记录（右键无法打开）**：原版打开成书的链路为「服务端 `ServerPlayer.openItemGui` 发送 `ClientboundOpenBookPacket` → 客户端 `ClientPacketListener.handleOpenBook` 打开界面」，两处均用 `itemstack.is(Items.WRITTEN_BOOK)` **精确匹配原版成书物品**，自定义 WrittenBookItem 子类永远无法触发。已改为由客户端事件监听器 `InstructionBookClientHandler`（`Dist.CLIENT`）在右键物品时直接 `Minecraft.setScreen` 打开 `BookViewScreen`。
  - > **修复记录（服务端 ClassNotFoundException 崩溃）**：最初把 `Minecraft.getInstance().setScreen(new BookViewScreen(...))` 直接写在 `use()` 方法内，导致专用服务器（Mohist forgeserver）加载物品类时因引用客户端类 `net.minecraft.client.gui.screens.inventory.BookViewScreen` 而 `ClassNotFoundException` 崩溃（`ModItems` 注册阶段即失败）。已改为：`use()` 中**不再引用任何 `net.minecraft.client.*` 类**（只返回 `sidedSuccess`），打开界面的逻辑全部移入 `Dist.CLIENT` 的 `InstructionBookClientHandler` 事件监听，服务端不加载该客户端类。
  - > **修复记录（作者）**：成书 `author` 由 "CCRC" 改为 "海鸥的红石"。

### 21. 说明书2（instruction_book_2）

[InstructionBook2Item.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBook2Item.java) 与说明书1同机制（继承 `WrittenBookItem`、覆写 `use()` 客户端打开 `BookViewScreen`），记录 **CC: Tweaked 配件外设的使用方法**：

- **页面结构**（共 15 页）：封面 → 目录 → 数码显示器（2 页）→ 数字调节器（2 页）→ 数字圆盘记录仪（3 页）→ 扩展红石继电器/总线（3 页）→ 方块探测器（3 页）。内容与五个外设类 [DigitalDisplayPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_display/DigitalDisplayPeripheral.java)、[DigitalKnobPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_knob/DigitalKnobPeripheral.java)、[DigitalPlotterPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/digital_plotter/DigitalPlotterPeripheral.java)、[ExtendedRelayBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBusPeripheral.java)、[BlockDetectorPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/block_detector/BlockDetectorPeripheral.java) 中的 `@LuaFunction` 一一对应，精确记录每个函数名、参数类型与返回值：
  - **数码显示器** `digital_display`：`setStatus(text: string) -> string`（设置橙色状态文字）、`getStatus() -> string`（读取当前文字）。
  - **数字调节器** `digital_knob`：`setValue(value: int) -> int`（设置整数 0~1000，越界钳制，返回钳制后值）、`getValue() -> int`（读取当前值）。
  - **数字圆盘记录仪** `digital_plotter`：`push(value: int) -> int`（写入新值并移位，0~100 钳制）、`setValue(index: int, value: int) -> boolean`（设置第 index 位，索引越界返回 false）、`getValue(index: int) -> int | nil`（读取第 index 位，越界返回 nil）、`getList() -> table`（读取整表 50 元素）。索引采用 Lua 惯例 1~50。
  - **扩展红石继电器/总线** `redstone_relay_bus`：`isRelay(distance) -> boolean`（判断该处是否为继电器）、`setOutput(distance, side, on)`（布尔输出 15/0）、`getOutput(distance, side) -> boolean`、`setAnalogOutput(distance, side, value)`（模拟输出 0~15 越界报错）、`getAnalogOutput(distance, side) -> int`、`getInput(distance, side) -> boolean`、`getAnalogInput(distance, side) -> int`。distance 紧贴=1，最大 `relay_bus.max_distance`（默认 16，范围 1~64）；side 以**继电器自身朝向**为基准（top/bottom/left/right/front/back）。
  - **方块探测器** `block_detector`（2026-09-15 新增）：`getFacing() -> string`（探测方向 north/south/west/east/up/down）、`getBlockInfo() -> table | nil`（面向方块信息表 `{x,y,z,id,name,mod,isBlockEntity}`，目标区块未加载返回 nil）、`getBlockEntityData() -> table | nil`（目标为方块实体时返回完整 NBT 数据，类似 `/data get block`，只读；无方块实体/未加载返回 nil）。页面注明探测需访问主线程世界数据，**每次调用有 1 tick 延迟**。
- **内容注入**：`createBook()` 写入 `title="说明书2"`、`author="海鸥的红石"`、`resolved=true`、`generation=0`、`pages`。
- **注册**：`ModItems.INSTRUCTION_BOOK_2`（`instruction_book_2`，堆叠 1）；贴图 `textures/item/instruction_book_2.png`（来源"模型/其他物品/instruction_book_2.png"），模型 `models/item/instruction_book_2.json`；lang 中英"说明书2 / Instruction Book 2"；创造标签发放 `createBook()`。

### 22. 简易长矛（simple_spear）

[SimpleSpearItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/SimpleSpearItem.java) 为铁质剑类武器（继承 `SwordItem`，自带横扫攻击）：

- **Tier**：新增 `ModToolTiers.SIMPLE_SPEAR`——耐久 **130**、攻击伤害加成 **129**（构造修正 0，修改器显示 +129，实际总伤害 = 基础 1 + 129 = 130）、铁锭修复。
- **文字颜色类似附魔金苹果**：注册时 `Rarity.EPIC`（物品名淡紫色）+ 覆写 `isFoil()` 返回 true（物品带附魔微光光泽）。
- **+13 攻击范围**：覆写 `getAttributeModifiers`，主手追加 `ForgeMod.ENTITY_REACH`（`forge:attack_range` 别名）属性修改器 +13.0（ADDITION），攻击距离 = 默认 3.0 + 13.0 = 16.0 格（Forge 1.20.1 攻击判定读取该属性）。
- **悬停描述**：紫色文字"魔女们的秘密武器"（`desc_simple_spear`）。
- **手持模型（特殊设置）**：`models/item/simple_spear.json` 采用 `item/handheld` 姿态（第三人称工具与手臂同平面，rotation `[0,-90,55]`）+ 第一/第三人称 `scale` 2 倍（`1.7`/`1.36`），手持时模型显示二倍大；第三人称 `translation.y` 由 4.0 增至 7.2（沿贴图长度方向前移约 1/5 贴图总长度 ≈ 3.2 单位）。
- **注册**：`ModItems.SIMPLE_SPEAR`（`simple_spear`，堆叠 1）；贴图 `textures/item/simple_spear.png`（来源"模型/其他物品/simple_spear.png"），模型 `models/item/simple_spear.json`（`item/handheld`）；lang 中英"简易长矛 / Simple Spear"；创造标签加入主物品栏。

### 23. /ccrc 指令（CcrcCommand）

[CcrcCommand.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/command/CcrcCommand.java) 注册 `/ccrc` 根指令，用于查看/强制修改 CC: Tweaked 外设编号计数（配合《说明书》中"monitor_0"式命名机制的运维）：

- **设备数量相关指令统一归在 `/ccrc peripheral` 分支下**（读取/处理 CC: Tweaked 设备数量）：
  - **`/ccrc peripheral get_count <设备种类名称>`**：查询该种类当前计数值（无权限要求）。
    - 优先读内存（反射 `ServerContext.idAssigner` 的私有 `ids` Map，已初始化时），否则读文件。
    - 未记录过该种类时显示 0，并提示"下一个新外设将从 0 开始"。
  - **`/ccrc peripheral set_count <设备种类名称> <数字>`**：强制把该种类计数值改为指定值（需 OP 权限 2 级）。
    - **双写保证一致**：① 直接写 CC 计数文件 `<存档>/computercraft/ids.json`（`ServerContext.storageDir()` 定位，Gson 格式 `{"类型": 最大编号}`）；② 反射同步内存 `IDAssigner.ids`（否则 `getNextId` 会按内存旧值继续分配并覆盖文件）。
    - **存在性校验**：仅允许设置 `ids.json` 中已记录的类型（= 曾被有线调制解调器连接/分配过编号）；设置不存在或从未连接过的类型会拒绝执行并提示"可用 /ccrc peripheral list 查看已记录的类型"。
    - 提示"下一个该种类新外设将从 <值> 继续递增分配"。
  - **`/ccrc peripheral list`（OP 权限 2 级）**：**仅查看 CC 计数文件 `ids.json` 的内容**（每行 `类型 → 编号`），不做世界扫描，便于与补全候选对照。
- **自动补全（get_count/set_count）**：`设备种类名称` 参数注册 Brigadier `.suggests()` 建议提供器，候选 = `ids.json` 中已记录的全部类型（下拉菜单，类似原版 `/setblock` 列出方块）。
- **已删除旧顶层别名**（`/ccrc get_count / set_count / list`）——设备数量相关指令仅保留 `/ccrc peripheral` 分支，避免指令重名/歧义。
- **实现要点**：`RegisterCommandsEvent`（FORGE 总线）注册；参数用 Brigadier `StringArgumentType.word()` + `IntegerArgumentType.integer(0)`；`sendSuccess(Supplier<Component>, boolean)` 形式反馈中文消息。
- **音频播放指令 `/ccrc sound <声音ID> <目标> [音量]`**（2026-09-21 新增，无权限要求）：
  - **语法**：`/ccrc sound cc_rc:xxx @a` 或 `/ccrc sound cc_rc:xxx @a 2.0`；`<声音ID>` 用 `ResourceLocationArgument.id()` 解析，支持 Tab 补全（候选 = 全部已注册声音，原版 + 模组）；`<目标>` 用 `EntityArgument.players()` **必选**（支持 @p/@a/@e[type=player] 等选择器）；`<音量>` 用 `FloatArgumentType.floatArg(0.0F)` 可选（默认 1.0，执行时校验须 >0）。
  - **播放机制**：服务端校验声音存在（`ForgeRegistries.SOUND_EVENTS`）后，通过 [PlaySoundPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/PlaySoundPacket.java)（S2C 网络包，ModNetwork **id=5**，纯数据包：soundId + volume）用 `PacketDistributor.PLAYER` **定向发送给每个目标玩家**（其他玩家听不到）；客户端 [PlaySoundClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/PlaySoundClientHandler.java) 收到后播放 [CommandSoundInstance.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/CommandSoundInstance.java)（`AbstractTickableSoundInstance` 子类）——**Attenuation.NONE 无距离衰减 + 无距离限制**，音量 = 指令设置，SoundSource.MASTER（收包玩家可自主调主音量），一次性播放（looping=false）。
  - **服务端加载安全（PR #2 模式）**：PlaySoundPacket 不直接引用任何客户端类，handle() 仅通过无参方法引用 `PlaySoundClientHandler::dispatchPending` 派发（方法引用惰性解析，专用服务器不会加载客户端类），与 EvilGajinLockPacket 等包一致。
  - **lambda 捕获坑**：for-each 循环变量（`for (ServerPlayer target : ...)`）非 effectively final，lambda 捕获前须复制为局部 final 变量（`ServerPlayer receiver = target;`）；发送数量经 `int playerCount = targets.size();` 提前固定后再在成功提示 lambda 中使用。

---

### 24. 密码输入器与破解器（PasswordInputerBlock / PasswordCrackManager）

[PasswordInputerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/password_inputer/PasswordInputerBlock.java) 密码输入器方块，继承 `ConsolePanelBlock` 复用放置逻辑/碰撞箱/文字显示（可贴墙/地板/天花板，14x14x3 面板）：

- **状态**：FACING + FACE（继承）+ POWERED（on/off，默认 off）；blockstate 24 变体（face × facing × powered），on/off 切换 `models/block/password_inputer/password_inputer_on/off`（模型来源"模型/特殊控制台/密码输入器/code_reader.json"，UV 直接沿用 + 顶/底面方向翻转；贴图 2.png / 2-on.png）。
- **破解交互**：手持破解器（`PasswordCrackerItem`）右键本方块 → [PasswordCrackManager.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/password_inputer/PasswordCrackManager.java)（仅服务端，FORGE 总线 `ServerTickEvent`）登记破解会话。
- **破解流程**：默认 400 tick（20 秒，`Config` 的 `password_inputer.crack_ticks` 可配置）；每 tick 校验三个中断条件——玩家距方块 > 5 格、主手不再是破解器（切快捷栏/换手）、方块被破坏/替换；任一成立 → 会话取消、计时重置并提示"破解中断"。计时走完 → `powerOn()`：方块 `POWERED=true`、播放点击音、向连接方向强充能输出 15，20 tick 后自动变回 off。破解中每 10 tick 刷新 action bar 显示剩余秒数。
- **红石**：`isSignalSource` 恒 true；`getSignal` on 时全向 15（弱信号）；`getDirectSignal` on 时连接方向 15（强充能贴附方块，类似按钮/刷卡机）；连接方向真值表 = FLOOR→UP / CEILING→DOWN / WALL→FACING。

[PasswordCrackerItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/PasswordCrackerItem.java) 破解器物品：不可堆叠，**3D 物品模型** `models/item/password_cracker.json`（模型来源"模型/特殊控制台/破解卡/破解卡.json"转换，26 元素 + 源 display + 补 gui 显示，UV 直接沿用），贴图 `textures/item/password_cracker.png`。
  - > **修复记录（破解完成崩溃）**：原实现在服务端 tick 迭代 SESSIONS 时用 `SESSIONS.put()` 推进计时——put 会改变 HashMap 结构计数（modCount），随后迭代器 `remove()` 抛 `ConcurrentModificationException`（crash-reports 实锤，破解完成瞬间崩溃）。已改用 `Map.Entry.setValue()` 推进（不修改结构）。
  - > **修复记录（二次崩溃 ConcurrentModificationException@onServerTick:131）**：修复 put 后仍崩溃——破解成功分支 `powerOn()` 内 `level.setBlock()` 触发 `PasswordInputerBlock.onRemove()`，其无条件调用 `PasswordCrackManager.onBlockRemoved()` 在服务器 tick 正迭代 SESSIONS 的同时移除条目，迭代器 `remove()` 再次抛并发修改异常。双重修复：① `onRemove()` 仅在方块被真正破坏/替换（`!state.is(newState.getBlock())`）时才调用 `onBlockRemoved()`，POWERED 属性切换不再触发；② `SESSIONS` 改用 `ConcurrentHashMap`，对 tick 迭代与 start()/onBlockRemoved() 的交错操作天然免疫（迭代器弱一致）。
  - > **修复记录（提示与倒计时）**：开始/中断/成功提示由聊天栏（sendSystemMessage）改为 **action bar**（displayClientMessage + true，屏幕中间显示）；倒计时节奏改为——开始后前 20 tick 显示"开始破解…"，之后每 10 tick 刷新"破解中…剩余 N 秒"，N 从 19 倒数到 1（总时长仍为配置值）。
  - > **修复记录（破解音效）**：新增声音 `password_crack`（`sounds/password_crack.ogg`，来自"模型/特殊控制台/破解卡/破解.ogg"）。服务端破解开始/中断/成功/方块破坏时通过 `CrackSoundPacket`（S2C 网络包，ModNetwork id=1）通知客户端；客户端 [CrackSoundClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/CrackSoundClientHandler.java)（Dist.CLIENT）在方块位置循环播放，收到停止包或玩家退出/切维度/远离 32 格时停止。
  - > **修复记录（模型被覆盖成 Bedrock 1.21 格式致紫黑）**：`password_inputer_off/on.json` 被 Blockbench **新版导出直接覆盖**（`format_version: 1.21.11`，rotation 用 `{"x":90,"y":0,"z":0}` 欧拉角、origin 平级），Java 模型加载器只认 `axis/angle` → 日志 `Missing axis, expected to find a string` → 方块渲染紫黑。已以用户新版为源重新转换：欧拉角按"取非零轴"转回 `axis/angle`（34 元素全单轴，无复合旋转）、去除 format_version/credit、UV 顶/底面翻转并直接沿用，保留用户全部元素与坐标改动；贴图沿用用户 19:48 更新的版本（64×64）。
  - > **修复记录（Invalid rotation 90 致整体紫黑）**：上述转换后日志改报 `Invalid rotation 90.0 found, only -45/-22.5/0/22.5/45 allowed` —— **Java 模型元素级旋转只允许 -45°~45°**（90° 只允许在 blockstate 层），而用户 1.21 新版模型 34 个元素全带绕 X 轴 90/67.5/112.5°（其中 8 个非轴对齐元素 Java 无法表示）。最终方案：**放弃被覆盖的 1.21 版本，改用素材源"模型/特殊控制台/密码输入器/code_reader.json"（19:40 的 Bedrock 1.9.0 合法版，rotation 全为 0/±22.5°）重新转换**，与用户 19:48 更新的贴图（MD5 与素材一致）配套；验证：坐标全在 0~16、34 元素 0 非法角度，jar 与 build/resources 均已更新。

### 25. 核弹按钮与钥匙（NukeButtonBlock）

[NukeButtonBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/nuke_button/NukeButtonBlock.java) 核弹按钮方块，继承 `ConsolePanelBlock` 复用放置逻辑/碰撞箱/文字显示（放置方式同控制面板）。

- **状态**：FACING + FACE（继承）+ STATE（IntegerProperty 1~6）；blockstate 72 变体（face × facing × state）；6 状态模型 `models/block/nuke_button/nuke_button_1~6`（模型来源"模型/特殊控制台/B"六文件，**保持原尺寸仅 z 平移 +2 对齐 0~16**，x 方向 -4~20 按用户要求保留溢出；UV 直接沿用 + 顶/底面方向翻转；贴图 `nuke_button_1~6.png`）。
- **状态机与信号**：状态1 初始（放置，输出 0）→ 状态2 插钥匙1（输出 5）→ 状态3 插钥匙2（输出 7）→ 状态4 双钥匙（状态3，输出 10）→ 状态5 待发（状态4，输出 10）→ 状态6 发射（状态5，输出 15，60 tick 后自动复位状态1）。
- **钥匙交互**（`use`，仅服务端执行）：手持 `key_1` 右键：状态1→状态2、状态3→状态4；手持 `key_2` 右键：状态1→状态3、状态2→状态4；转移成功即 `held.shrink(1)` 消耗钥匙（不可堆叠 `stacksTo(1)`）。
- **空手交互**：shift+右键在状态4（状态3）与状态5（状态4）间互相切换；状态5 普通右键 → 状态6 并 `scheduleTick(60)`，`tick()` 计时结束复位状态1。
- **红石**：`isSignalSource` 恒 true；`getSignal` 弱信号全方向 = 当前状态信号强度；`getDirectSignal` 信号 > 0 时连接方向强充能同值（类似按钮/刷卡机）；状态切换走 `updateNeighbours`（LeverBlock 金标准双刷新），`onRemove` 在信号 > 0 时刷新。
- **钥匙物品** `key_1` / `key_2`（`ModItems.KEY_1/KEY_2`）：`Item` 直注册，`stacksTo(1)`，贴图 `textures/item/key_1.png`、`key_2.png`（来源"特殊控制台/key_1、key_2"），物品模型 `item/generated`。
- **渲染类型**（[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) `ClientModEvents.onClientSetup`）：B 组贴图含大面积透明区域且被模型 UV 采样，默认 `RenderType.solid()` 会把 alpha=0 像素渲染成黑色 → 在客户端启动时 `ItemBlockRenderTypes.setRenderLayer(NUKE_BUTTON, RenderType.cutout())`（alpha test 剔除透明像素）。

### 26. 钥匙柜与钥匙分发控制器（KeyCabinetBlock / KeyDistributorBlock）

[KeyCabinetBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/key_cabinet/KeyCabinetBlock.java) 钥匙柜方块：水平四向放置（`HORIZONTAL_FACING` = 放置时玩家朝向的反方向，对墙放置柜体贴在远离玩家的墙侧、柜门朝玩家），无方块实体，模型（用户新版，默认贴 z 0~8 侧）blockstate 4 变体（facing 旋转：south→y0 / west→y90 / north→y180 / east→y270）。**碰撞箱为贴墙长方体**：宽 12（x 2~14 左右各空 2）、高 14（y 0~14）、厚 7（贴墙侧，south 基准 z 0~7），四个朝向随 FACING 旋转（`getShape` 四朝向 AABB）；贴图含大面积透明 → 已设 `RenderType.cutout()`（避免 solid 渲染透明处变黑，同核弹按钮修复）。

[KeyDistributorBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/key_distributor/KeyDistributorBlock.java) 钥匙分发控制器：完整方块（六面同贴图 `key_sender.png`，cube_all），带方块实体 [KeyDistributorBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/key_distributor/KeyDistributorBlockEntity.java) 存储已录入的钥匙柜记录列表（[KeyCabinetRecord.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/key_distributor/KeyCabinetRecord.java)：坐标 + 朝向，NBT 列表持久化，新增 `key_distributor_be`）。

- **记录录入**（[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java) `useOn`）：多功能工具右键钥匙柜 → 坐标 + 朝向写入工具 NBT（`KC_X/Y/Z/KC_Facing`）并提示；再右键控制器 → 把记录录入 BE（重复坐标不添加）。也支持指令 `/ccrc keycabinet`（玩家须站在控制器上方）：`list` 查看 / `add <x> <y> <z>`（校验该处是钥匙柜，facing 自动读取）/ `remove <x> <y> <z>`。
- **红石逻辑**（方块 `tick` 心跳持续自调度 + BE `lastSignal` 上升沿检测，仅服务端）：信号 ≤5 → 每 tick 遍历记录，位置不再是钥匙柜则删除该条；信号从 ≤5 跳变到 >5 → **只分发一次**（记录 ≥2 时随机取两条不同记录，在各自钥匙柜**格内柜门侧**（格中心向柜门方向 [FACING 方向本身，新模型柜门朝外一侧] 偏移 0.3 格）生成**钥匙1 / 钥匙2** 掉落物（`setDeltaMovement(0,0,0)` 无初速度静止，`setGlowingTag(true)` 高亮描边 + `lifespan = Integer.MAX_VALUE` 永不自然消失，1.20.1 无 setLifespan 方法故直接改公开字段））。修复：初版用 `neighborChanged` 直接分发，一次红石脉冲会多次触发生成多把钥匙 → 改为上升沿检测，一个脉冲只出一把钥匙1 + 一把钥匙2。

### 27. 错误生物（ErrorMob）

错误生物是本模组**第一个生物实体**（非弹射物），由用户通过 QQ 发送的模型「模型/错误生物/error.obj」（扁平 "ERROR" 字样、5 个部件 object_1~5、32x32 贴图）立体化生成：

- **模型立体化**（[ErrorMobModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobModel.java)）：原模型 z 厚度仅 0.05 方块（平贴字牌），转换时加厚为 0.35 方块（约 5.6 像素）成为立体字块；5 个字母（E/R/R/O/R）横向排列，模型以脚底中心为原点、整体宽约 2.1 方块、高约 1.04 方块。贴图 `textures/entity/error_mob.png`（64x32，深灰半透明底 + 红色 ERROR 字样，由脚本生成）。
- **实体**（[ErrorMob.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMob.java)）：继承原版 `Monster`（敌对）。**AI 完全复制原版蠹虫（Silverfish）的寻路与攻击逻辑**，但不含任何方块交互（无虫蚀方块、无藏匿/钻出、无受击召唤同伴）：
  - `FloatGoal`（游泳防溺水）→ `MeleeAttackGoal(this, 1.0D, false)`（近战，攻击伤害 3）→ `WaterAvoidingRandomStrollGoal(this, 1.0D)`（绕水随机游走）→ `LookAtPlayerGoal(this, Player.class, 8.0F)`（注视玩家）→ `RandomLookAroundGoal(this)`（随机环视）→ `HurtByTargetGoal(this)`（受击反击）→ `NearestAttackableTargetGoal<>(this, Player.class, true)`（主动索敌玩家）。
  - 属性：最大生命 8、移动速度 0.25、攻击伤害 3.0（与蠹虫一致）。
  - **不注册 SpawnPlacements** → 不会在世界中自然生成；只能通过刷怪蛋或原版刷怪笼（`MobCategory.MONSTER` 自动支持）召唤。
- **注册**（[ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java)）：`EntityType.Builder.of(ErrorMob::new, MobCategory.MONSTER).sized(0.6F, 1.5F).clientTrackingRange(8).updateInterval(3)`；实体属性在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 的 `EntityAttributeCreationEvent` 中注册。
- **刷怪蛋**（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java)）：`error_mob_spawn_egg`（主色红 0xDC2828 / 次色深灰蓝 0x1E1E28，贴图 `textures/item/error_mob_spawn_egg.png` 双色点纹），加入主创造标签。
  - **使用 `ForgeSpawnEggItem`**（`net.minecraftforge.common.ForgeSpawnEggItem`，接受 `Supplier<EntityType>` 惰性解析实体类型）：避免在物品注册阶段直接 `ModEntities.ERROR_MOB.get()` 导致 `Registry Object not present` 崩溃。
  - **注册顺序**：[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 构造函数中 `ModEntities.ENTITY_TYPES.register` 必须排在 `ModItems.ITEMS.register` **之前**（实体先于依赖它的物品注册）。
  - 曾因初版直接 `new SpawnEggItem(ModEntities.ERROR_MOB.get(), ...)` + 物品先于实体注册，客户端启动时在 `common_setup` 阶段抛 `NullPointerException: Registry Object not present: cc_rc:error_mob` 崩溃（见 `run/crash-reports/crash-2026-09-10_19.20.03-client.txt`），已按上述两点修复。
- **渲染**（[ErrorMobRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobRenderer.java)）：客户端注册实体渲染器 + 模型层定义（`EntityRenderersEvent.RegisterRenderers` / `RegisterLayerDefinitions`），模型层 `error_mob`；渲染时缩放 0.45 使字牌宽度与碰撞箱相称。

**变种（null / warn）**：用户追加两个模型「模型/错误生物/null.zip」「模型/错误生物/WARN.zip」（同为扁平字牌 OBJ），注册为独立实体 `error_mob_null` / `error_mob_warn`：
- **模型**：[ErrorMobNullModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobNullModel.java)（"null" 字样，4 部件，宽约 0.8 方块）、[ErrorMobWarnModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobWarnModel.java)（"WARN" 字样，4 部件，宽约 2.0 方块），沿用立体化转换（z 厚 0.05→0.35 方块）；贴图 `textures/entity/error_mob_null.png`（暗紫灰 NULL）、`error_mob_warn.png`（琥珀黄 WARN）
- **共用基类**：[ErrorMobModelBase.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobModelBase.java)——三个模型类共用（root 持有 + setupAnim 按 yaw 转身）
- **实体/渲染**：`ModEntities.ERROR_MOB_NULL/WARN`（共用 `ErrorMob` 类与属性，碰撞箱 0.6×0.6 / 0.6×1.2）；`ErrorMobRenderer` 重构为按变种注入模型工厂/贴图/缩放（NULL 0.75 / WARN 0.45）；三个模型层均在 `RegisterLayerDefinitions` 注册
- **刷怪蛋**：`error_mob_null_spawn_egg`（0x8C82B4/0x282338）、`error_mob_warn_spawn_egg`（0xE6AA3C/0x372D14），均用 `ForgeSpawnEggItem`，加入主创造标签；行为与主变种一致（仿蠹虫、不自然生成）
- **【修复】贴图文字错位**（2026-09-14）：用户反馈三个模型正面贴图文字位置不对（y 方向正确），贴图上文字左下角像素 x=2/15/28/41/54、y=18（error_mob.png）；null/warn 贴图为 x=8/21/34/47。根因：三模型的字母方块均使用默认 `texOffs(0,0)`，而 vanilla 盒自动 UV 中正面（+z/SOUTH 面）采样区为 `U∈[u+sz+sx+sz, u+sz+sx+sz+sx]`、`V∈[v+sz, v+sz+sy]`（经反汇编 `ModelPart$Cube` 构造器字节码确认），所有方块正面都采样纹理同一角落区域（x≈15~22），贴图上的字母从未被采样。修复：按公式 `u = 字母x − sz − sx − sz` 给每个字母方块单独设置 texOffs（ERROR 5 块 u=-14/-1/12/22/38、v=0；NULL 4 块 u=-4/9/19/32、v=6/6/7/7；WARN 4 块 u=-10/5/16/26、v=5）；NULL/WARN 方块高度（4.68/6.55/7.95px）小于字母纵向带，v 取 `12 − sz − (sy−6)/2` 使 7px 字母行在采样窗内居中。负 texOffs 合法（BlockBench 惯例，超界采样为透明）；texOffs 为 int，取整误差约 0.5px。三模型已加中文注释说明根因与计算方式；gradlew build BUILD SUCCESSFUL。

### 28. 编辑工具（EditTool）

编辑工具（`edit_tool`，模型「模型/其他物品/edit_tool.png」）用于修改「可显示名称的方块」表面文字——即所有放置时会从物品自定义名称写入文字、并渲染到方块表面的方块实体：

- **统一接口**（[ITextDisplay.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/ITextDisplay.java)）：定义 `setText(Component)` / `getText()`，让编辑工具统一识别可编辑方块，避免逐个 instanceof。已实现于：
  - `ConsolePanelBlockEntity`（基类——其子类 `ConsoleLever*BlockEntity`、`PlotterBlockEntity` 等自动继承）
  - `DigitalDisplayBlockEntity`、`DigitalKnobBlockEntity`、`DigitalPlotterBlockEntity`
- **物品**（[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java)，`stacksTo(1)`）：
  - **主手右键** ITextDisplay 方块 → `NetworkHooks.openScreen` 打开编辑菜单（MenuProvider 携带方块坐标 + 当前文字，经 IForgeMenuType 同步给客户端）；
  - **副手放置**可显示名称方块时 → `BlockEvent.EntityPlaceEvent` 检测放置者是玩家且副手持编辑工具、新方块是 ITextDisplay → 延迟 1 tick（`TickTask`）自动打开编辑界面（确保 setPlacedBy 已把名称写入 BE）。
  - **防御加固**（2026-09-20）：`openEditMenu` 内捕获 `Throwable`——dev 环境若在游戏运行时并发执行 `gradlew build`，被重写的 `build/classes` 可能使匿名类 `EditToolItem$1` 懒加载失败（`NoClassDefFoundError`）导致整服崩溃（crash-2026-09-20_22.32.17：`Exception in server tick loop`）。捕获后记 error 日志 + 向玩家发中文提示"请重启游戏"，服务端不再崩溃（类加载失败后该菜单在本次会话内仍不可用，属正常）。
- **GUI**：
  - 菜单 [EditTextMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/EditTextMenu.java)：无物品槽位的纯文本菜单（携带方块坐标 + 当前文字；`quickMoveStack` 返回空栈），注册于 `ModMenuTypes.EDIT_TEXT`（`IForgeMenuType` 构造从同步数据读坐标与文字）；
  - 屏幕 [EditTextScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/EditTextScreen.java)：**自定义 GUI 背景** `textures/gui/edit_tool_gui.png`（256x256 画布，来源「模型/其他物品/edit_tool_gui.png」），**布局保持原版铁砧原位规格**（imageWidth=176 / imageHeight=166，背景取画布左上 176x166 区域，控件坐标不移动：输入框 12,34 宽 152、完成按钮 12,68）。
    - **自定义标签渲染**（覆写 `renderLabels`）：只画界面标题（"编辑文字"）于左上角原位，**不调用 super.renderLabels 避免出现"物品栏"字样**（无物品槽位界面不需要玩家物品栏标题）；原文字"当前：xxx"显示在标题右侧同行。
    - **修复**：覆写 `keyPressed` 拦截背包键（E）——编辑工具界面为纯输入界面，按 E 不应触发原版背包键的关闭行为（`keyInventory.matches` 拦截），仅 Esc 可关闭。
- **网络**（[EditTextPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/EditTextPacket.java)，C2S，通道 index 2）：客户端提交方块坐标 + 文本 → 服务端权威校验（BE 为 ITextDisplay + 玩家距离 ≤ 8 格）→ `setText(Component)`（去换行 + trim，空文本 = 清除显示）。
- **注册**：`ModItems.EDIT_TOOL`、`ModMenuTypes.EDIT_TEXT`（CcRc 注册 MENU_TYPES + 客户端 `MenuScreens.register`）、物品模型 `item/generated` + 贴图 `textures/item/edit_tool.png`、lang 中英文（含悬停描述）、主创造标签。

### 29. 扩展红石继电器与总线（ExtendedRelayBlock / RelayBusBlock）

两个配套方块：**扩展红石继电器**（远端红石端口）与**扩展红石继电器总线**（CC 外设宿主），实现"总线沿自身朝向在任意距离遥控继电器"的无线红石：

- **扩展红石继电器** `extended_relay`（[ExtendedRelayBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBlock.java) + [ExtendedRelayBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBlockEntity.java)）：
  - 完整方块，水平四向放置（front 贴图朝放置方向）；方块实体存 **outputs[6]**（对外输出强度 0~15）与 **inputs[6]**（从世界读入信号），索引 = CC `ComputerSide.ordinal()`；
  - **side 与朝向**：本地方向 ↔ 世界方向按**继电器自身 FACING** 映射（front=FACING / back=反向 / left/right=逆/顺时针 / top=UP / bottom=DOWN，与 CC `DirectionUtil.toLocal` 一致）；
  - `getSignal/getDirectSignal` 转发实体输出（isSignalSource）；`tick` 每 tick 刷新四周输入；放置后首个 tick 初始化输入读取。
- **扩展红石继电器总线** `relay_bus`（[ExtendedRelayBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBusBlock.java) + [ExtendedRelayBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBusBlockEntity.java)）：
  - 完整方块，六方向放置（FACING 同红石信号发射器），`canConnectRedstone=true`；贴图 bus 六面 + top/bottom；
  - BE 提供 `findRelay(distance)`：沿自身 FACING 在 `distance` 格处（紧贴=1，最大 **Config.relay_bus.max_distance**，默认 16，范围 1~64）查找继电器；`isRelayAt(distance)`。
- **总线 CC 外设**（[ExtendedRelayBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/extended_relay/ExtendedRelayBusPeripheral.java)，类型 `redstone_relay_bus`，经 `ForgeComputerCraftAPI.registerPeripheralProvider` 注册，有线调制解调器可连接）：
  - `isRelay(distance)`：检查距离 distance 处是否为扩展红石继电器；
  - `setOutput(distance, side, on)` / `getOutput(distance, side)`：布尔输出（15/0）；
  - `setAnalogOutput(distance, side, 0-15)` / `getAnalogOutput(distance, side)`：模拟输出（校验范围）；
  - `getInput(distance, side)` / `getAnalogInput(distance, side)`：读取继电器该侧输入；
  - **side 语义**：以**目标继电器自身朝向**为基准的 CC 本地方向（top/bottom/left/right/front/back），与总线朝向无关——不同距离的继电器各自按自身朝向解释 side。

> **修复记录（v0.0.8）总线 FACING 属性读取崩溃**：`ExtendedRelayBusBlockEntity.findRelay` 初版误用 `HorizontalDirectionalBlock.FACING`（仅 north/south/east/west 四值）读取总线朝向，而总线注册的是 `BlockStateProperties.FACING`（六方向）——CC 电脑访问外设时报 `IllegalArgumentException: Cannot get property ... as it does not exist in Block{cc_rc:relay_bus}`。已改为读取 `ExtendedRelayBusBlock.FACING`（六方向）修正。
>
> **修复记录（v0.0.8）总线识别不到继电器**：总线外设所有 Lua 方法（isRelay/setOutput/getOutput/setAnalogOutput/getAnalogOutput/getInput/getAnalogInput）补充 `@LuaFunction(mainThread = true)`。根因：CC 电脑在独立线程运行 Lua，外设方法默认在计算机线程执行；而 `findRelay` 需访问 Minecraft 主线程的 `Level`/方块实体（`level.getBlockEntity`、`level.isLoaded`），非主线程访问不可靠导致 findRelay 返回 null → 总线"识别不到继电器"。与数码显示器外设 `@LuaFunction(mainThread = true)` 的既有写法保持一致。
>
> **修复记录（v0.0.8）继电器红石输出/输入不生效**：
> - **输出无信号**：`setOutput` 只更新 BE 内部 outputs 数组并同步 NBT，但未通知邻居重新查询红石——红石线/机械不会感知 BE 内部状态变化。修复：`setOutput` 变化时调用 `updateNeighborsAt`（含输出方向对侧）刷新四周；同时修正 `getRedstoneOutput` 的方向映射——Minecraft 红石信号方法（getSignal/getDirectSignal）的 direction 参数是**反向语义**（实际输出到 direction.getOpposite() 方向，与原版 LeverBlock/刷卡机修复一致），初版直接用 direction 映射导致方向错位（设置 front 却输出到其它方向）。
> - **输入读不到**：`tick` 仅放置时调度一次，inputs 不持续刷新。修复：`tick` 内持续自调度（`scheduleTick(pos, this, 1)`）每 tick 刷新输入；并新增 `neighborChanged` 在邻居变化时立即刷新输入。
> - **自身传导红石**：用户要求继电器/总线像玻璃一样不传导信号。修复：两个方块的 `canConnectRedstone` 改为 `false`（红石线不连接到方块、不穿过传导），信号完全由总线外设经继电器 BE 手动读写。
> - 附：`setOutput(int level)` 参数名与 BE 的 `Level level` 字段冲突导致编译错误，已改名 `power` 并显式用 `this.level`。
>
> **修复记录（v0.0.8）继电器红石行为对齐 CC RedstoneRelayBlock**：
> - **仍传导红石/充能问题**：参考 CC 官方 `RedstoneRelayBlock` 源码修正——CC 继电器 `getSignal` 与 `getDirectSignal` 均返回**真实定向输出**（`incomingSide.getOpposite()` 反向换算），`canConnectRedstone` 不覆写（默认 false）。此前尝试把 `getDirectSignal` 改为 0 属过度修复（导致继电器无法向目标方向强充能输出）。最终：`canConnectRedstone=false`（红石线不连接/不将其作为导体）、`getSignal=getDirectSignal` 返回定向真实输出、`isRedstoneConductor` 保持完整方块默认 `true`（实心阻断红石线跨格跳跃），与 CC 继电器行为一致。
> - **外设调用约 1 tick 延迟**：这是 CC: Tweaked `@LuaFunction(mainThread = true)` 的**固有机制**——CC 电脑在独立线程运行 Lua，标 `mainThread` 的方法会投递到主线程**下一 tick** 执行（保证世界/方块实体访问线程安全）。因此 `isRelay`/`setOutput`/`getInput` 等每次调用天然有 ≤1 tick 延迟，非 bug；数码显示器等既有外设同样如此。
>
> **修复记录（v0.0.8）玻璃式不传导（isRedstoneConductor）**：用户反馈继电器/总线仍会传导红石。根因：**1.20.1 中 `isRedstoneConductor` 是 `BlockBehaviour.Properties` 的 `StatePredicate` 设置方法**（构造时以 `properties.isRedstoneConductor((state, level, pos) -> bool)` 链式传入），**Block 层已没有可覆写的实例方法**——曾尝试以 `@Override public boolean isRedstoneConductor(BlockState, BlockGetter, BlockPos)` 覆写导致编译失败（"method does not override"），构建中断、jar 未更新，测试 jar 中继电器仍是完整方块默认导体（isRedstoneConductor=true）。修复：两个方块（ExtendedRelayBlock / ExtendedRelayBusBlock）构造器改为 `super(properties.isRedstoneConductor((state, level, pos) -> false))`，配合 `canConnectRedstone=false` 真正实现"红石线不连接、信号不穿透"（同玻璃）；同步修正总线 Javadoc 中过时的 `canConnectRedstone=true` 描述。
>
> **修复记录（v0.0.9）继电器识别不到红石中继器输入**（方向语义更正，**覆盖**上方 v0.0.8 记录中"direction 反向语义"的过时结论）：
> - **现象**：红石中继器对准继电器输出，`getInput`/`getAnalogInput` 读不到信号；红石粉/按钮/拉杆等**方向无关**信号源正常（掩盖了方向错误，仅中继器/比较器等**方向相关**信号源暴露）。
> - **根因（两级取证）**：① 反编译原版 `DiodeBlock`（中继器父类）srg 字节码：`getSignal` 判断 **`direction == FACING`** 才输出——即 `Level.getSignal(pos, direction)` 的 direction 是**查询方指向信号源的方向**（不是"反向语义"）；② 反编译 **CC-Tweaked 1.120.2** 字节码：`RedstoneUtil.getRedstoneInput(level, pos.relative(dir), dir)` 与 `RedstoneRelayBlockEntity.getRedstoneOutput(direction) = state.getExternalOutput(mapSide(direction))` **均不取反**。本实现两处 `getOpposite()` 皆源于 v0.0.8 修复记录的误导性结论。
> - **修复（v0.0.9-1，输入）**：`refreshInputs` 去掉 `getOpposite()` → `level.getSignal(getBlockPos().relative(dir), dir)`（用户实测：中继器输入可正常识别 ✓）；同步更正两个类的 Javadoc。
> - **补充修正（v0.0.9-2，输出）**：首轮修复曾把 `getRedstoneOutput` 也改为不取反，用户实测 **输出前后左右上下全反**，遂恢复为 `toLocalSide(getBlockState(), direction.getOpposite())`——**输出与输入的查询方向语义恰好对称**：输入（本方块查邻居）传"本方块→信号源"方向 `dir`；输出（红石线等查本方块）传入的 `direction` 是"查询方→本方块"方向，与本方块实际输出方向相反，必须取反。最终形态 = 仅改 `refreshInputs` 一处。
> - **验证**：gradlew build BUILD SUCCESSFUL（32s）；需游戏内实测中继器→继电器输入（推荐拓扑：中继器输出端紧贴继电器任意面，Lua `getAnalogInput(distance, side)` 可读 15）与 `setOutput("front")` 打到继电器正面红石线。

### 30. 盖金蜗牛与金鹰（GajinSnail / GoldenEagle）

两个配套内容：**盖金蜗牛**（被动动物实体）与**金鹰**（其食物物品），实现"用金鹰吸引/繁殖蜗牛、右键蜗牛播放音效"的养殖玩法：

- **盖金蜗牛** `gajin`（[GajinSnail.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/GajinSnail.java) + [GajinModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/GajinModel.java) + [GajinRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/GajinRenderer.java)）：
  - **被动动物**（`Animal`，MobCategory.CREATURE），实体尺寸 0.5×0.5，**不自然生成**（不注册 SpawnPlacements），只能刷怪蛋召唤；
  - **AI 参考原版猪**（但**无乘骑机制**，不实现鞍座/骑乘相关方法）：FloatGoal（游泳）→ PanicGoal（恐慌逃跑 2.0）→ BreedGoal（繁殖）→ TemptGoal（手持金鹰吸引跟随 1.25）→ FollowParentGoal（跟随父母）→ WaterAvoidingRandomStrollGoal（绕水游走）→ LookAtPlayerGoal（注视玩家 6 格）→ RandomLookAroundGoal（随机环视）；
  - **右键播放音效**：`mobInteract` 覆写——服务端播放 `gajin` 音效（SoundSource.NEUTRAL）；若手持金鹰则额外进入 `super.mobInteract` 喂食/繁殖逻辑。**平常无 ambient 叫声**（不覆写 getAmbientSound，默认 null）；
  - **食物判定**：`isFood` = 金鹰（Ingredient.of(GOLDEN_EAGLE)）；`getBreedOffspring` 生成新的盖金蜗牛；
  - **属性对齐原版猪**：最大生命 10、移动速度 0.25；
  - **模型**：由「模型/生物/蜗牛/gaijin.bbmodel」（java_block 自由 UV）重排为 Java 实体 Mojang 布局（64×64），4 个 box（身体 8×3×12 / 外壳 6×6×8 / 左右触角 2×4×1），无腿部动画仅随实体 yaw 转身；
  - **贴图** `textures/entity/gajin.png`（重排自 bbmodel 内嵌 64×64 贴图），渲染器 shadow 0.3。

> **修复记录（v0.0.8）模型颠倒/悬空 + 刷怪蛋贴图**：用户反馈蜗牛"上下颠倒、浮在空中、太小"。三个问题及根因：
> 1. **上下颠倒 + 浮空**：根因是**坐标系语义差异**——`gaijin.bbmodel` 是 **java_block 格式（Y 轴向上）**，而 **Java 实体模型（ModelPart/LayerDefinition）的 Y 轴向下为正**（`LivingEntityRenderer` 渲染时 `scale(-1,-1,1)` + `translate(0,-1.501,0)` → 模型 `y=24` 才是脚底贴地，`y=0` 在约 1.5 方块高处）。之前把 bb_y 原样（0~9）填入 `addBox` 的 y，导致整个模型被渲染在离地 0.94~1.5 方块的高空且上下颠倒。修复：坐标整体翻转——body `y 21..24`（底部贴地）、shell `y 15..21`（上方）、触角 `y 17..21`。
> 2. **模型太小**：原 box 宽 4px=0.25 方块，无法与 0.5×0.5 碰撞箱相称。放大至约半方块：body 8×3×12（0.5×0.19×0.75）、shell 6×6×8、触角 2×4×1。
> 3. **贴图需随新 box 尺寸重排**：Mojang 布局中每个 box 的 texOffs 由 box 尺寸决定，box 放大后旧 64×64 布局不再匹配。重新执行重排（PowerShell 脚本按 ModelPart.Cube 面方向映射）：shell `texOffs(0,0)` / tentR `(28,0)` / tentL `(34,0)` / body `(0,16)` 写入 textures/entity/gajin.png。
> 4. **刷怪蛋贴图**：误用了 bbmodel 内嵌实体贴图（64×64），正确应为素材文件夹中的 `gaijin.png`（16×16 刷怪蛋贴图）。已替换 `textures/item/gajin_spawn_egg.png`。
- **金鹰** `golden_eagle`（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java)）：普通物品堆叠 64，贴图 `textures/item/golden_eagle.png`（来源「模型/生物/蜗牛/金鹰.png」），作为蜗牛的食物（吸引/繁殖）。
- **音效** `gajin`（[ModSounds.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModSounds.java)）：sounds.json 注册 `gajin`（sounds/gajin.ogg，由「模型/生物/蜗牛/蜗牛音效.mp4」经 VLC 转码 vorbis），带字幕 `cc_rc.subtitle.gajin`。
- **刷怪蛋** `gajin_spawn_egg`：ForgeSpawnEggItem（Supplier 惰性解析），主色金褐 0xD8B24A、次色深褐 0x5A3A1E。
- **受击能力**（新增）：覆写 `hurt`——蜗牛实际受到伤害时自动获得「抗性提升 II」+「生命回复 II」效果（amplifier 1），持续 30 秒（600 tick），仅服务端施加。
- **音效修复**（v0.0.9 前）：用户反馈右键点击一次音效会"播放完整一遍再放半遍停止"。根因：源素材「模型/生物/蜗牛/蜗牛音效.mp4」本身时长 16.7s，内容即「完整一遍(~11s) + 半遍(~5.5s)」重复录制未剪干净。本沙箱中 VLC 播放/转码被拦截（环境差异），改用 npm 镜像 `registry.npmmirror.com` 下载便携 ffmpeg（`@ffmpeg-installer/win32-x64`，64MB，留存 `run/package/ffmpeg.exe` 供后续音频处理）→ `ffmpeg -t 11 -ac 1 -c:a libvorbis -b:a 160k` 重新转码 `sounds/gajin.ogg` = **11.00 秒 / mono / 44100Hz / 160kbps**（194KB）。另在 `mobInteract` 加 **0.5 秒播放防抖**（服务端 `lastSoundGameTime`，防连点/双触发叠加）。
- **音效修复（续）右键完全无声**：用户复测重转码后的 `gajin.ogg` 右键无任何音效、`/playsound` 也无声。排查结论：**文件本身无问题**——ffmpeg volumedetect 显示音量正常（mean -10.5dB / max -3.5dB）、可正常解码、`src` 与 `build/resources` 两份 MD5 一致（用户确认此前 /playsound 无声为游戏端资源未刷新所致）。真正根因是**防抖 long 整数溢出**：`lastSoundGameTime` 初始为 `Long.MIN_VALUE`，`now - lastSoundGameTime` 在 Java 中整数溢出成约 -9.2e18 的负数，`>= 10` 恒为 false → 首次及后续右键音效永远不播放。修复：比较前先排除哨兵值（`lastSoundGameTime == Long.MIN_VALUE || now - lastSoundGameTime >= 10`），注释同步更新。gradlew build BUILD SUCCESSFUL（34s）。
- **注册**：ModEntities（实体）、CcRc（属性/渲染器/模型层各 1）、ModCreativeTabs（金鹰 + 刷怪蛋入主物品栏）、lang 中英（实体/物品/音效字幕）。

### 31. 唱片音乐（GitHub issue #1 新增 6 张）

处理 GitHub issue [#1](https://github.com/HaiOu-Redstone/CC_RC/issues/1)（用户 Romarku 上传 6 首歌曲 + 封面，请求添加为唱片）：

- **素材**：6 个 mp3 + 6 个 16×16 封面 png 下载至「模型/唱片/」（bit / broken_boy / panic_track / resonance / roller_mobster / sabotage）；6 个 mp3 经 VLC 转码为 **mono OGG**（44.1kHz / vorb / 160kbps / 单声道）存 `sounds/music/<key>.ogg`，验证 ch=1 并实测时长 ticks（3187 / 4847 / 3075 / 4254 / 4286 / 4540）；
- **注册**：ModSounds（6 个 `music_<key>` SoundEvent）、ModItems（6 个 RecordItem，comparator 复用 3/4/7/8/9/10，Rarity.RARE stacksTo 1）、sounds.json（stream:true + subtitle）、models/item 6 个、lang 中英（名称/desc/subtitle）、`minecraft:tags/items/music_discs` 标签、ModCreativeTabs；
- **构建**：gradlew build BUILD SUCCESSFUL。

**补充（2026-09-14）：新增 2 张唱片 friends_wine（朋友的酒）/ air（鸟之诗）**：
- **素材**：`模型/唱片/friends_wine.mp4`、`air.mp4` 为视频文件，经 ffmpeg（run/package/ffmpeg.exe）`-vn` 提取音频轨转码为 **mono OGG**（44.1kHz / vorb / 160kbps），验证 ch=1 且时长完整（friends_wine 4:17 = 5145 tick、air 5:50 = 7003 tick）；封面 png 复制为 `textures/item/music_disc_<key>.png`；
- **注册**：ModSounds（`music_friends_wine` / `music_air`）、ModItems（RecordItem，comparator 复用 11/12，Rarity.RARE stacksTo 1）、sounds.json（stream:true + subtitle）、models/item 2 个、lang 中英（物品名称/desc/subtitle）、`music_discs` 标签、ModCreativeTabs；
- **构建**：gradlew build BUILD SUCCESSFUL。
- 注：ffmpeg 转码 mp4 必须加 `-vn` 忽略视频轨，否则便携版 ffmpeg 会把 1080p 视频流转成 theora 拖慢转码（初次转码即因此被中断，产生残缺 2:56 文件，已重新转码为完整 4:17）。

**补充（2026-09-20）：新增唱片「LEVEL !」（key=level，感叹号不入注册名）**：
- **素材**：`模型/唱片/level !.mp3` + `level!.png`，VLC 转 **mono 44.1k vorbis 160kbps**（`sounds/music/level.ogg`，1.85MB），校验 ch=1 rate=44100、**ticks=2383**（≈1:59）；贴图复制为 `textures/item/music_disc_level.png`；
- **注册**：ModSounds（`music_level`）、ModItems（RecordItem，**比较器输出 13**——1~15 已被全部占用故复用级别 13，同 reimu；Rarity.RARE stacksTo(1)，ticks 2383）、sounds.json（stream:true + subtitle）、models/item、lang 中英「音乐唱片 / Music Disc」「LEVEL !」、subtitle、`minecraft:tags/items/music_discs` 标签、ModCreativeTabs；
- **构建**：gradlew build BUILD SUCCESSFUL（36s）；三、2 物品 152→**153**、三、4 声音 38→**39**（唱片 33 + 非唱片 6）已同步。

**补充（2026-11-22）：全部唱片统一重压为 64kbps 单声道**：
- **转码**：33 张唱片全部以 VLC `transcode{acodec=vorb,ab=64,channels=1,samplerate=44100}` 从原始素材重转（不从旧 160k ogg 二次压缩）。优先用「模型/唱片」源；缺源者取自 `E:\视频\BGM`：`level5`←LEVEL5 -judgelight-.mp3、`railugun`←OnlyMyRailgun.m4a、`never`←Never Gomma Give You Up.m4a、`assumptions`←Assumptions.mp3、`conrnfield_chase`←原野追逐-3.mp3、`move`←動く、動く (チトver.).mp3、`more_one_night`←More One Night (チトver.).mp3、`rain`←雨だれの歌.mp3、`end`←終ワリノ歌.mp3、`underground_river` / `hanezeve_caradhina`←Kevin Penkin 同名 mp3。
- **结果**：`sounds/music` 下 35 个 ogg（33 唱片 + 2 个反应堆音效未改）由 **144.11MB → 61.43MB（约 −57%）**；全部 `ch=1 / 44100Hz`，时长与原文件一致（±1s），RecordItem 的 ticks 无需改动。
- **备份**：原文件完整保留在 `build/music_backup/`（另有 `build/bloom.ogg.bak`）；gradlew build BUILD SUCCESSFUL（25s）。

### 32. CC 外设无延迟改造（风险最小方案实施）

背景：实测证实 CC 的 `mainThread=true` 外设调用**每个调用单独消耗 1 个主线程 tick**（任务执行后 resume Lua 需经 ComputerThread 异步排队，下一个任务只能下一 tick 才执行），连续 N 次调用 ≈ N tick 延迟。按此前定案的风险最小方案改造全部 4 个 CC 外设：

- **digital_knob / digital_display / digital_plotter**（低风险类）：
  - 状态字段 volatile 化（display 的 text/status 为 volatile 引用；plotter 的 data 为 volatile 引用 + **写时复制快照**，写永不修改共享数组，读方拿到不可变快照）；
  - 写方法不再直接调用主线程 API（setChanged/sendBlockUpdated），只置 dirty；三个方块新增 BE 静态 tick（`Block.getTicker` 注册），每 tick 合并广播（至多 1 次同步包）；
  - 外设方法全部去掉 `mainThread=true`（setter/getter 均 0 tick，写后立即读 = 新值）。
- **redstone_relay_bus**（高风险类）：
  - 新增 [RelayState](文件内类)（ReentrantLock 双缓存）：internalOutputs（Lua 读写，0 tick）/ appliedOutputs（主线程应用层，方块 getSignal 读取）/ inputs（主线程写、Lua 读）；
  - 总线 BE 主线程每 tick 重建「distance → RelayState」不可变缓存（volatile 整体替换），**Lua 线程零 Level 访问**（Level 非线程安全）；
  - 继电器输出由主线程方块 tick 合并应用到世界（变化面位掩码决定 updateNeighborsAt 去向），输入沿用 `neighborChanged` 事件 + tick 刷新；
  - **修复**：Inputs 不再落盘（瞬态世界信号），load 后由首个 tick 从世界重刷（原实现持久化 Inputs 导致重启读到过期输入）。
- **构建**：gradlew build BUILD SUCCESSFUL（25s）。
- **修复（总线无法识别继电器）**：总线 BE 的继电器缓存刷新依赖主线程 tick，但 ExtendedRelayBusBlock 漏覆写 `getTicker`（改造前外设是 mainThread 实时 `findRelay` 查 Level，不依赖缓存；改造后读缓存但缓存从不重建 → `relayCache` 恒空、`isRelay` 恒 false、其余方法抛 "No extension relay"）。已补 `getTicker`（手写 lambda，与 knob/display/plotter 同款风格）驱动 `ExtendedRelayBusBlockEntity.tick` 每 tick 重建缓存，并加 `level.isClientSide` 过滤；继电器侧无此问题（ExtendedRelayBlock.tick 方块自调度持续运行，驱动 applyOutputsToWorld/refreshInputs）。gradlew build BUILD SUCCESSFUL（34s）。
- **红石线主动连接继电器**（用户反馈）：ExtendedRelayBlock.canConnectRedstone 由 false 改为 **true**——红石线可主动连接相邻继电器读取定向输出（isSignalSource 原本即 true）；配合 Properties 的 isRedstoneConductor=false 仍保持"非导体"语义（红石线不从继电器穿透传导）。

### 33. 数据单元（DataUnit）

数据单元（`data_unit`）是无方向完整方块，外观模型为**原版书架结构**（`minecraft:block/cube_column` 柱形），贴图已换成**专属贴图**（顶部 `data_unit_top`、四周侧面 `data_unit_side`，模型文件 [data_unit.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/block/data_unit/data_unit.json)，blockstates 与 item model 引用 `cc_rc:block/data_unit/data_unit`）：

- **方块**（[DataUnitBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/data_unit/DataUnitBlock.java)）：无方向完整方块（16×16×16 碰撞箱，木质音效，强度 2.0，`MapColor.WOOD`）；`setPlacedBy` 从物品自定义名称写入名称数据；`getTicker` 注册方块实体 tick。
- **方块实体**（[DataUnitBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/data_unit/DataUnitBlockEntity.java)）实现 `ITextDisplay`（编辑工具右键可改名称）：
  - 存储**名称**（字符串，默认空）与**数据**（整数列表，初始为空，上限 1024）；
  - 线程模型沿用 CC 外设无延迟改造：`name`/`data` 均为 volatile，列表**写时复制**（只替换引用不改共享数组）；写后置 `dirty`，主线程 tick 节流合并「存档 + 客户端同步」（每 tick 至多一次）；
  - `setValue(index, value)`：索引在范围内覆盖、等于长度时追加、越界/超上限返回 false（中间空缺补 0）；`getValue` 越界返回 null；NBT 存 `Name`/`Data`（`IntArrayTag`）。
- **CC 外设**（[DataUnitPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/data_unit/DataUnitPeripheral.java)，type=`data_unit`）：
  - `getName()` / `setName(str)`——读写名称；
  - `getList()` / `setList(tbl)`——读写整个列表（Lua 表索引 1 起，超长返回 false）；
  - `getValue(idx)` / `setValue(idx, v)`——读写列表某一位（Lua 索引 1 起，越界 getValue 返回 nil、setValue 返回 false）；
  - 全部无 `mainThread`（0 tick），与数字调节器/圆盘记录仪一致。
- **资源**：`blockstates/data_unit.json` 与 `models/item/data_unit.json` 引用 `cc_rc:block/data_unit/data_unit`（原版书架 cube_column 结构 + 专属 top/side 贴图）；lang 中英「数据单元 / Data Unit」。
- **构建**：gradlew build BUILD SUCCESSFUL（28s）。

### 34. 方块探测器（BlockDetector）

方块探测器（`block_detector`）是**只读探测设备**，完整方块，可向 x±/y±/z± 六个方向放置，外观模型为**原版观察者结构**（16³ 单元素六面模型），贴图已换成**专属贴图**（front 正面 / back 背面 / side 侧面 / top 顶面，模型文件 [block_detector.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/block/block_detector/block_detector.json)，blockstates 六方向映射 `cc_rc:block/block_detector/block_detector` 加对应旋转）：

- **方块**（[BlockDetectorBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/block_detector/BlockDetectorBlock.java)）：`FACING = BlockStateProperties.FACING`（六方向），放置时面向玩家视线（`getNearestLookingDirection().getOpposite()`，同原版观察者）；完整方块碰撞箱，石质音效强度 2.0。
- **方块实体**（[BlockDetectorBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/block_detector/BlockDetectorBlockEntity.java)）：空实体，仅作 CC 外设承载，不存储数据（探测结果实时读取世界）。
- **CC 外设**（[BlockDetectorPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/block_detector/BlockDetectorPeripheral.java)，type=`block_detector`）**只读**探测 FACING 前方一格的方块：
  - `getFacing()`——方向字符串（north/south/west/east/up/down）；
  - `getBlockInfo()`——表 `{ x, y, z, id="minecraft:stone", name="stone", mod="minecraft", isBlockEntity=布尔 }`（目标区块未加载返回 nil）；
  - `getBlockEntityData()`——目标为方块实体时返回其完整 NBT（`saveWithId()` 递归转换为 Lua 表，含 id/坐标/全部字段，类似 `/data get block`，**不可修改**）；无方块实体/未加载返回 nil；
  - 全部方法标注 `mainThread=true`：探测需实时读取服务端 Level/方块状态/方块实体/区块加载状态，而 Level 非线程安全只能主线程访问（与无延迟改造「Lua 线程严禁访问 Level」约定一致），每次调用消耗 1 tick 属必然代价（参考未改造前的继电器总线）。
- **NBT 转换**：`nbtToObject(Tag)` 静态递归转换——Compound→Map、List→List、数组（byte/int/long）→数值 List、数值→double、字符串→String，供 Lua 直接消费。
- **构建**：首次编译因方法名 `getTarget` 与 `IPeripheral.getTarget()` 接口冲突 + NBT 强转缺失失败；改名 `getBlockInfo` + 强转后 gradlew build BUILD SUCCESSFUL（26s）。

### 35. 贴图/模型资源目录整理

将 `textures/block` 与 `models/block` 顶层**散装**资源全部归入各方块同名文件夹（此前仅部分方块已入夹），达到彻底无散装：

- **多贴图红石类**（本次核心）：
  - `redstone_receiver`：贴图 2 张（`redstone_reciver_off/on.png` → 入夹并**修正拼写 reciver→receiver**，同时模型内 texture 引用、blockstates、item model 同步更新）；模型 `redstone_receiver_off/on.json` 入夹。
  - `redstone_sender`：贴图 6 张（bottom/side/top × off/on）与模型 2 个入夹。
  - `relay_bus`：贴图 3 张（`redstone_relay_bus*.png`）与模型 `relay_bus.json` 入夹。
  - `extended_relay`：贴图 2 张（`redstone_relay_expansion*.png`）与模型 `extended_relay.json` 入夹。
- **贴图已入夹、模型散装**：`fridge`、`potato_crate` 模型入夹。
- **单贴图/单模型一并入夹**：`digital_display`、`digital_knob`、`digital_plotter`、`plotter_clock`、`ccrc_block`、`sink`、`canvas_sign`（32 个 canvas 标志 blockstates 共享该模型引用，全部同步更新）。
- **改路径**：所有 JSON 引用成对更新——blockstates 的 `"model"` 引用、模型内 `"textures"` 引用、item model 的 `"parent"` 引用，批量精确字符串替换共更新 **99 个文件**（UTF-8 无 BOM 写入，保护 lang 中文不乱码）。
- **校验**：临时校验脚本确认全部 blockstates/模型/贴图引用均指向存在的文件（贴图或模型至少其一），且无任何无斜杠旧引用残留。
- **构建**：gradlew build BUILD SUCCESSFUL（31s）。

### 36. 控制面板文字样式（染料染色 + 编辑工具格式按钮）

控制面板类方块（`ConsolePanelBlockEntity` 家族：面板/大号面板/仪表/指示灯/按钮/安全按钮/核弹按钮/密码输入器/拉杆/圆盘记录仪）表面文字此前只能修改纯文本。事实上文字本身一直是 `Component`（含 Style）：NBT 走 `Component.Serializer.toJson` JSON 持久化、渲染层 `font.drawInBatch(text,...)` 会用 style 的颜色/粗体/斜体 —— 用户已实测"指令染色命名面板、放置后颜色保留"（即链路本通），本次补齐的是**修改样式的手段**与**提交保留样式**：

- **染料染色（右键面板染字）**：
  - [ConsolePanelBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlockEntity.java) 新增 `applyDyeColor(DyeColor)`：`Style.withColor(TextColor.fromRgb(dye.getTextColor()))` 仅覆写颜色，保留原文与粗体/斜体等格式；文字允许为空（先染色后写字），样式随空文字一并持久化；
  - [ConsolePanelBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console_panel/ConsolePanelBlock.java) 新增静态三件套：`isDyeTarget`（手持 DyeItem 且目标 BE 是 ConsolePanelBlockEntity，两端判断一致）/ `applyDye`（服务端：染字 + `SoundEvents.DYE_USE` 音效，**不消耗染料**——编辑工具 GUI 已能直接改颜色，染料仅作快捷手段）/ `handleDyeInteraction`（统一入口：客户端仅预测返回成功、服务端实际执行）；
  - **接入点**：基类覆写 `use`（非染料时 `super.use` 保持默认 PASS）覆盖无交互子类（panel/large/meter/point_lamp/plotter）；按钮类 `console_button`/`safe_button`/`nuke_button`/`password_inputer` 与拉杆类 `console_lever`/`console_lever_6/7` 各自 `use()` **开头**调用 `handleDyeInteraction`（染料优先、不影响原交互），`ConsoleLeverBlock` 覆写 use 调 super 保留拉杆切换。
- **编辑工具 GUI 增加样式控件**（[EditTextScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/EditTextScreen.java)，布局仍在 176x166 内，仅向下扩展）：
  - 完成按钮（y=68）下方新增一行格式按钮 y=96：**B(粗体)/I(斜体)/U(下划线)/S(删除线)** 四个 34x16 切换按钮，激活=白色粗体字、未激活=白色（仅以粗细区分，保证暗背景下可读），悬停 tooltip 显示格式名；
  - 再下方 y=118 起 **16 个染料色块按钮**（8 列 x 2 行，16x10，`DyeColor.values()` 顺序取 `getTextColor()`），选中白色边框；GUI 内可直接选色，与染料染色互通；
  - 打开时从 `menu.currentText.getStyle()` 读取当前样式作为按钮/色块初值（含"空文字仅存样式"的场景）；提交时把**完整样式**（颜色 RGB + 四格式布尔）随包写回；`color=-1` = 不设置颜色（恢复默认白）。
- **网络**（[EditTextPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/EditTextPacket.java)，C2S 通道 index 2）：字段扩展为 `text + color(int,-1=无) + bold/italic/underline/strikethrough(boolean)`，encode/decode 同步调整；服务端以 `Component.literal(text).withStyle(style)` 重建写入（空文本仍 = 清除显示）。
- **行为约定**：染料染色**不消耗染料**（编辑工具 GUI 已能直接改颜色，染料仅作快捷手段）；格式按钮未激活态为**白色**（仅以粗细区分，暗背景下可读性更好）。
- **构建**：gradlew build BUILD SUCCESSFUL（28s/26s，微调后 29s）。

### 37. 海报（PosterBlock）

贴墙装饰方块。**全局只注册一个方块 `poster`**（旧版 poster_2 ~ poster_26 多方块方案已废弃并被本方案取代）。方块实体保存「当前图片 + 缩放倍率 + dy/dz 偏移」，客户端 `PosterBER` 直接绘制贴图四边形，**因此尺寸不受原版方块模型元素坐标每轴 [-16, 32] 的限制**（旧版 2 倍竖旗 / 塔塔鲁斯九格宽即因此加载失败变紫黑块）。素材取自 `模型\其他物品\海报\`，双三次缩放并**保留 alpha 透明通道**。

- **方块**（[PosterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlock.java)）extends `HorizontalDirectionalBlock` implements `EntityBlock`：
  - **水平四向放置**：`getStateForPlacement` 仅点击水平墙面时 `FACING = getClickedFace()`，点击顶/底面返回 null（只能贴墙）；
  - **碰撞箱 16x16x1**：`SHAPE_NORTH=box(0,0,15,16,16,16)`（板子贴墙侧），SOUTH=z0~1 / EAST=x0~1 / WEST=x15~16；
  - `canSurvive` 校验支撑墙 `pos.relative(FACING.getOpposite()).isSolid()`；`updateShape` 支撑墙消失即转 AIR；
  - 属性 `noCollission + strength(0.5F) + WOOD`；`getRenderShape = INVISIBLE`（方块本体不渲染）+ `newBlockEntity`；`use` 右键（非潜行）服务端调 `PosterMenu.open` 打开设置界面。
- **方块实体**（[PosterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlockEntity.java)）：字段 `Image`(图片索引) / `Scale`(倍率，1.0=宽 1 格) / `Dy`(垂直偏移，**单位格**) / `Dz`(水平偏移，**单位格**)，`setConfig` 统一夹取（scale 0.05~20、偏移 ±16 格）；NBT 读写 + `getUpdateTag`/`getUpdatePacket` 客户端同步；`getRenderBoundingBox` 扩大为 `inflate(scale+offset+4)` 防止大图被视锥剔除。
- **渲染器**（[PosterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBER.java)）：`RenderType.entityCutoutNoCull(贴图)` 手绘一个四边形。几何：以观者视角锚点=贴墙面左上角，图片向右/向下延伸；宽=scale，高=scale*原图高/原图宽（保持比例）；dz 沿观者向右、dy 沿 +Y，**单位均为格（可直接写 0.5）**。四朝向锚点与「向右」向量（与 FACING=可见面法线一致，不会镜像）：NORTH (1,15/16) r=-X；SOUTH (0,1/16) r=+X；EAST (1/16,1) r=-Z；WEST (15/16,0) r=+Z。**贴墙间隙** `WALL_GAP = 0.5F/16.0F`（半个像素 = 1/32 格），四个朝向的锚点相应取 1/32、31/32，只调整贴图与墙面之间的视觉间隙以避免 Z-fighting；碰撞箱仍为 16×16×1。
- **手持物品标记**：`PosterBER` 检测本地玩家主手/副手持有 poster 物品时，在贴墙面叠加 1x1 半透明（alpha 110、全亮 0xF000F0）物品图标（`RenderType.entityTranslucent`），标记画在**未偏移的锚点**上（标的是方块而非图片），使透明的海报方块位置可见。
- **图片表**（[PosterImages.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterImages.java)）：`PosterImages.LIST` 共 **47 条（46 张可选）**，贴图位于 `textures/block/poster/`，文件名 / 图片 id 为原始图片名对应的英文标识（GUI 悬停显示中文原名），`Image` 字段为 `id / name / texture / width / height / top`。**索引 0 = `no_material`「无材质」为默认材质**（新放置的海报默认显示它，方块模型 particle 与物品贴图也改用它）；索引 1~23 为早期素材：`goggles_mark` 戴护目镜标识、`mixue_sign` 蜜雪冰城招牌、`poster_1~8`（原 1~8.png）、`ask_yourself`/`energy`/`forgetting`/`parking_lot`/`see_more`/`than_just_you`/`we_need_you`、`mystery_mark` 神秘标识、`flag_quantum_energy`/`flag_quantum_science`/`flag_quantum_lighting`/`flag_quantum_atomic` 四张量子竖旗、`tartarus_gate` 塔塔鲁斯上层入口 logo；索引 24~46 见下两条。
- **索引 24~40（17 张）**：CCNR 宣传 8 张 `ccnr_main` / `ccnr_logo_horizontal` / `ccnr_logo_vertical` / `ccnr_admin_region` / `ccnr_character` / `ccnr_canteen` / `ccnr_page_8` / `ccnr_page_9`；其他 6 张 `milk_dragon`（奶龙）、`slippery_floor`（小心地滑）、`wear_helmet`（戴安全帽）、`decor_1` ~ `decor_3`（装饰画）；安全标识 3 张 `danger_fall` / `danger_electric` / `danger_radiation`。
- **索引 41~46（6 张）**：`nuclear_sketch`（核电站简笔画，452×389）、`blue_fish_portrait`（蓝色大肥鱼画像，288×512）、`facility_director_portrait`（设施总监画像，307×512）、`mod_icon`（模组图标，256×256，取自 `src/main/resources/logo.png`）、`water_cycle_diagram`（水循环示意图，512×362，已从选择界面隐藏，改由独立物品 `water_cycle_chart` 提供）、`facility_director_2`（设施总监照片2，512×512）。
- **索引顺序不可变（重要约定）**：图片索引保存在方块实体 NBT 里，**顺序即存档数据**，一旦发布就不能在中间插入或调换；**注册新海报只能在 `LIST` 末尾追加**，否则所有已放置海报都会变成别的图。`PosterImages.get` 对越界索引回退到索引 0「无材质」，避免旧存档的非法索引导致渲染异常。
- **贴图处理**：源图放在 `模型/其他物品/海报/新海报`（部分达 12800×4267，直接当方块贴图会占用巨量显存）；导入时用 HighQualityBicubic 按最长边 512 等比缩小，再用 alpha 包围盒（阈值 8）裁掉四周透明留白，裁切后 `top = 0`、`width / height` 记录实际像素（如 `ccnr_main` 404×296、`ccnr_character` 341×512）；文件放在 `textures/block/poster/`，GUI 缩略图与 BER 都按实际像素取 UV。`water_cycle_diagram` 因高清图（5000×3536）已移到 `textures/gui/water_cycle_diagram_hd.png` 仅供独立物品的高清查看界面按需加载，海报条目回退为 512×362 低清并从选择界面隐藏（`PosterImages.HIDDEN_IDS`）。
- **默认顶端对齐**：`Image.top` 记录内容顶部的透明留白像素，BER 采样 v 从 `v0` 到 1、几何高度按内容高计算，GUI 缩略图同样裁掉留白——`poster_10~16` 为底对齐画布（top=179，`poster_13` 为 150），其余图片 top=0，因此所有图片默认「内容顶端贴方块顶端」。
- **GUI**：`PosterMenu`（无槽位菜单，把坐标+当前配置写进同步数据）/ `PosterScreen`（自绘面板：**7 列 × 固定 4 行**可见缩略图 + 滚轮翻页 + 右侧滚动条，缩放 / dy / dz 三个输入框 + 应用按钮）/ `PosterConfigPacket`（C2S，服务端校验方块实体类型 + 8 格距离后写入并 `sendBlockUpdated` 同步）。缩略图用 11 参 `GuiGraphics.blit(tex, x, y, w, h, u, v, uW, vH, texW, texH)` 采样整图并拉伸到缩略图框（9 参重载只截左上角同尺寸区域、不支持缩放）。滚动实现：`rebuildGrid()` 按 `gridScroll` 只创建可见行的缩略图按钮，`mouseScrolled` 在网格区域内一次翻一行，`gridScroll` 夹在 `[0, 总行数 - 4]`，`init()` 与滚动共用同一套重建逻辑。界面为**白底**：面板白色，标题 / 提示 / 标签文字用 `0xFF202020` / `0xFF707070` / `0xFF303030`，缩略图格背板 `0xFFF2F2F2`，边框为「选中黑 / 悬停深灰 / 普通浅灰」，滚动条轨道与滑块改浅色系，保证 `mod_icon`、`nuclear_sketch` 这类黑白图在白底上可辨认。注册：`ModBlockEntities.POSTER_BE`、`ModMenuTypes.POSTER`、`ModNetwork` id=6、`CcRc` client setup `MenuScreens.register` + `RegisterRenderers` 注册 BER。
- **资源**：`blockstates/poster.json`（facing 四向 y=0/90/180/270）、`models/block/poster.json`（仅 particle，无 elements）、`models/item/poster.json`（item/generated + `textures/item/poster.png`）；旧 poster_2~26 的 blockstate / model / item 贴图共 100 个文件已删除。
- **lang**：zh_cn「海报 / 海报设置 / 缩放倍率 / 垂直 dy（格）/ 水平 dz（格）/ 应用 / 单位：格（可输入 0.5）」及 47 张图片名称，en_us 对应英文。
- **构建**：gradlew build BUILD SUCCESSFUL。

---

### 38. 邪恶盖金（EvilGajin / evil_gajin）

敌对生物，素材 `模型\生物\邪恶盖金`（`evil_gaijin.png` 16x16 刷怪蛋贴图 + 6 个中文语音 mp3）。

- **实体类** [EvilGajin.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/EvilGajin.java)：**继承原版僵尸 Zombie**（复用僵尸完整 AI：寻路/锁定并攻击玩家/近战），三个例外：
  - **不会燃烧**：覆盖 `isSunBurnTick()` 返回 false（白天暴晒不着火）；
  - **不会捡装备**：覆盖 `canPickUpLoot()` 返回 false；
  - **静音原版僵尸音效**：覆盖 `getAmbientSound` / `getHurtSound` / `getDeathSound` 返回 null（不播放僵尸呻吟/受伤/死亡音，全部用自定义语音替代）；
  - **属性**（createAttributes）：生命 **100**、攻击伤害 **20**、基础速度 0.35、**索敌距离 128**（FOLLOW_RANGE，僵尸默认 35→128，约等于服务端实体 tick 半径上限，玩家远在 100+ 格也能被锁定追击）；
  - **距离变速**（customServerAiStep 每 tick 按目标距离设置 MOVEMENT_SPEED，**仅两档**）：
    目标 **>10 格** → **0.45**（用户已调快；约 19.4 m/s，远快于玩家疾跑 ≈5.6 m/s）；**≤10 格** → 0.28（约 12.1 m/s，中等速度——用户实测近身 0.12 过慢，改回原三档方案的中等档）。
  - **自动回血**（2026-09-21 新增，参考盖金蜗牛受击回血）：受到实际伤害时施加「**生命回复 II**」（REGENERATION，amplifier 1）持续 **30 秒**（600 tick），仅服务端。**亡灵兼容说明**：EvilGajin 继承僵尸（亡灵类，`isInvertedHealAndHarm()` 为 true）——**瞬间治疗（INSTANT_HEALTH）会被反转成伤害**，故必须用再生效果（REGENERATION 对亡灵正常按恢复生命处理，不受亡灵反转影响），与盖金蜗牛方案一致。
- **语音音效**（服务端播放，均 VLC 转 mono 44.1k vorbis 160k 存 `sounds/evil_gajin/*.ogg`）：
  - 锁定目标瞬间：**「进攻D点」**（evil_gajin_lock）播放一次（目标从无到有/切换时触发）——**目标为玩家时通过 S2C 包 [EvilGajinLockPacket](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/EvilGajinLockPacket.java)（id 4）通知客户端用 [LockCalloutSound](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/LockCalloutSound.java) 无距离衰减播放**（服务端 playSound 受 SoundEngine 默认 16 格衰减限制，锁定时常相隔 16 格以上完全听不见——用户反馈"进攻D点音量衰减太大"；Attenuation.NONE + 音量 1.0 + 一次性 0.9s，tick 跟随实体位置），**其它目标保持服务端广播**；
  - 攻击造成伤害：**「干得好」/「命中」**随机（evil_gajin_good / evil_gajin_hit，在 doHurtTarget 内播放——1.20.1 Zombie 无 getAttackSound hook）；
  - 击杀目标（伤害致死）：**「摧毁目标」**（evil_gajin_destroy）；
  - 受到攻击：**「局势不太妙」**（evil_gajin_bad），10 秒冷却防持续伤害刷屏。
  - **响度处理**（2026-09-20 微调）：素材响度偏低（lock 峰值约 -8.7dB、good 约 -10.2dB、hit 约 -4.9dB、destroy 约 -6.9dB、bad 约 -7.8dB），播放时明显小于满音量的 RWR 追击音；VLC 的 `scales` 参数实测无效，改用 **VLC 转 wav → PowerShell 采样线性放大（目标峰值 -1dBFS≈29000，上限 4×，逐文件计算）→ 再转 ogg**，5 个语音统一归一化到约 -1dBFS（增益 +3.8dB ~ +9dB）；**播放音量同步 1.0→1.2**（用户要求除 rwr 外其它语音再稍微增大：攻击/击杀/受击/锁定（非玩家目标）服务端 playSound 音量 1.2，客户端 LockCalloutSound 音量 1.2）。
- **RWR 追击循环音**（rwr.mp3 约 3 分钟长音，`stream:true`）：
  - 服务端 **RWR 状态机**（customServerAiStep 内，仅玩家目标）：**刚锁定玩家先播「进攻D点」，等音频播完再加 1 秒（LOCK_GAP_TICKS=18+20=38 tick）才发 RWR 开始包**；追击中**每 RWR_INTERVAL_TICKS=200 tick（10 秒）周期性打断**：发 RWR 停止包 → 播「进攻D点」→ 等待 → 再启动 RWR；目标失去/切换时补发停止包；状态机严格"先停再播、先播完再起"，**保证 RWR 与「进攻D点」不重叠**；
  - 客户端 [EvilGajinRwrSound](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/EvilGajinRwrSound.java) 继承 **AbstractTickableSoundInstance**（模式参考原版蜜蜂音效）：`looping=true` 循环播放，tick() 每帧从客户端世界取实体同步 x/y/z 实现音源跟随，实体死亡/卸载自动 `stop()` 兜底；**衰减采用手动控制**：attenuation = NONE（避开 SoundEngine 默认 16 格陡峭衰减，1.20.1 无 getRange API 无法改衰减范围），在 tick() 内按与玩家的距离计算 `volume = 1 - 距离/32`（**32 格平缓衰减 + 基础音量 1.0**：10 格约 0.69、16 格约 0.5、近处不超 1.0 不爆响——用户要求"衰减较小且基础音量也不大"）；管理器 [EvilGajinRwrClientHandler](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/EvilGajinRwrClientHandler.java) 按实体 ID 维护实例（同一实体不重复创建）。
  - 1.20.1 兼容备注：客户端循环音不能用 `net.minecraft.client.resources.sounds.MovingSound`（不存在），且 `AbstractTickableSoundInstance.stop()` 为 protected，故提供公开 `requestStop()`。
- **服务端加载安全修复（2026-09-21，专用服务器崩溃）**：Mohist 专用服务器启动报 `NoClassDefFoundError: net/minecraft/client/resources/sounds/SoundInstance`（`ModNetwork.init` 注册包 4 时崩溃）。根因：`EvilGajinLockPacket` 的 `handle()` 内联 lambda 直接引用了客户端类 `LockCalloutSound`，其父链 `AbstractTickableSoundInstance → SoundInstance`（`net.minecraft.client.resources.sounds` 包）为客户端专属类、服务器类路径不存在，服务器加载包类时沿父链解析即崩。**修复**：三个 S2C 包（`CrackSoundPacket`/`EvilGajinRwrPacket`/`EvilGajinLockPacket`）统一改为**纯数据包**——`handle()` 仅做 `PENDING` 静态暂存 + `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> XxxClientHandler::dispatchPending)` 无参方法引用派发（方法引用惰性解析，专用服务器不会加载任何客户端类）；播放逻辑移入客户端处理器：`CrackSoundClientHandler`/`EvilGajinRwrClientHandler` 新增 `dispatchPending()`，并新建 [EvilGajinLockClientHandler.java](file:///f:/CCRC/CC_RC/src/main/java/com/cc_rc/client/EvilGajinLockClientHandler.java)。已 javap 验证三个包类常量池均无 `net/minecraft/client` 直接引用。gradlew build BUILD SUCCESSFUL（38s；期间补 `前置/cc-tweaked-1.120.2.jar` 依赖方可解析 CC:Tweaked）。
- **模型/渲染** [EvilGajinModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/EvilGajinModel.java)：**暂时复用盖金蜗牛几何**（`createBodyLayer()` 直接调用 GajinModel 的静态方法）；需独立 Model 类是因为 `MobRenderer<EvilGajin, M>` 要求 `EntityModel<EvilGajin>` 泛型（GajinModel 绑定 GajinSnail 不可直接复用）。渲染器 [EvilGajinRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/EvilGajinRenderer.java) 贴图暂用 `textures/entity/gajin.png`。**（后续已替换为 JSON 直渲模型，见 [二、40](#40-邪恶盖金-json-直渲模型blockbenchjsonmodel)）**
- **刷怪蛋** `evil_gajin_spawn_egg`：ForgeSpawnEggItem（Supplier 惰性解析），主色暗红 0x8B0000 / 次色纯黑 0x000000，贴图 `evil_gaijin.png` 16x16；实体**不自然生成**（不注册 SpawnPlacements），只能刷怪蛋/刷怪笼。
- **构建**：gradlew build BUILD SUCCESSFUL（35s）；开发日志目录 58、三、2 物品 153→**154**、三、4 声音 39→**45**（唱片 33 + 非唱片 12）、三、6 实体 5→**6**（Mob 4→5）已同步。

### 39. 错误生物新模型（ERROR/NULL/WARN 像素字牌）

素材 `模型\生物\错误生物\新模型`（三个 Blockbench zip：`error (1).zip` / `null.zip` / `WARN.zip`，各含 `*.json` + `texture.png`）。

- **json 分析**：Blockbench "每面独立 UV" 布局（`faces.uv` 每面自由摆放），vanilla 展开盒 UV 无法直接表达；贴图为**纯红占位**（error/warn 32x32、null 16x16，uniqueColors=1，透明像素=镂空区，无 `texture_size`）→ 不做 UV 重拼，**几何精确转换 + 贴图直接复制为实体贴图**（`textures/entity/error_mob.png` 等，LayerDefinition tex 尺寸同步 32x32 / 16x16）。
- **转换脚本**（PowerShell ConvertFrom-Json，留存 `run/package/newmodels/`，生成段 `*_java.txt`）：
  - **坐标居中**：Blockbench 原点未居中（error x∈[-14,32] 中心 9、null 中心 8、warn 中心 7；z 均 [7,9] 中心 8）→ x/z 统一减中心偏移，模型以脚底/中心为原点；
  - **旋转元素**：error 3 个、null 1 个、warn 2 个斜腿（z 轴 -22.5°），其中 error 三个 origin=(x,6.5,8) ≠ 元素中心 → **绕 origin 的独立 part**：box 局部坐标 = from-origin..to-origin，PartPose.offsetAndRotation(origin 居中坐标, 0, 0, toRadians(-22.5))，旋转轴点精确；
  - 无旋转直板全部合并进一个 `CubeListBuilder`（`letters` part），旋转件为 `rot0..N` part。
- 三个模型类（ErrorMobModel/ErrorMobNullModel/ErrorMobWarnModel）**仅替换 createBodyLayer 几何与 tex 尺寸**，类名/LAYER_LOCATION/渲染器注册均不变；像素字形：ERROR 23 元素（宽 46px）、NULL 10 元素（宽 42px）、WARN 17 元素（宽 46px），2px 薄板拼字、z 厚 2px。
- **上下颠倒修复**（2026-09-20）：原版 `LivingEntityRenderer` 渲染前执行 `pose.scale(-1,-1,1)`（绕 Z 180°）+ `translate(0,-1.501,0)`，模型必须按"实体语义 y 越大越靠下（脚底 y_px=24）"绘制（见 GajinModel 类注释，蜗牛转换时即做过此翻转）；本次转换脚本**遗漏 y 翻转**，几何仍为 Blockbench 的 Y 向上 → 显示上下颠倒。修复：**不改几何**，在 ErrorMobRenderer.scale() 内再乘 `scale(-s,-s,s)` 抵消原版绕 Z 180°、并 `translate(0,1.501,0)` 反向补偿原版位移（最终矩阵=纯缩放，字牌按 Blockbench 方向贴地显示）；三个变种共用该渲染器一次修复。
- **改为 Blockbench json 直渲**（2026-09-21，用户要求"参考邪恶盖金"）：
  - **原因**：Java addBox 在线转换几何繁琐且上下翻转补偿复杂，错误生物与邪恶盖金同为"无动画字牌/静态模型"，改用与邪恶盖金完全相同的 json 直渲方案，从根源去除转换。
  - **资源**：三个 zip 的 `*.json`（error(1)/null/WARN，format_version 1.9.0，每面独立 UV 布局 uv 值 0~15.75 = 基于 16 像素网格，元素数与 Java 版一致 23/10/17）**原样复制**为 `assets/cc_rc/models/entity/error_mob.json` / `error_mob_null.json` / `error_mob_warn.json`；贴图由 zip 内 `texture.png` **覆盖** `textures/entity/error_mob.png` / `error_mob_null.png` / `error_mob_warn.png`（必需成对更新：json 直渲按 json 内每面 uv 采样贴图，null/warn 原贴图与 zip 哈希不一致，故覆盖为配套贴图；error 本就一致）。
  - **渲染器**：新增 [ErrorMobJsonRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/ErrorMobJsonRenderer.java)（`EntityRenderer<ErrorMob>`，仿 EvilGajinRenderer）：构造时 `BlockbenchJsonModel.load` 加载对应 json（渲染线程一次性构建几何），render 内 `pose.scale(scaleFactor)` 变种缩放 + `mulPose(Axis.YP.rotationDegrees(-entity.getYRot()))` 转向，`RenderType.entityCutoutNoCull` 透明镂空双面可见；三个变种共用本类，注册侧分别传 json/贴图/缩放（ERROR 0.45 / NULL 0.75 / WARN 0.45，与 Java 版 ScaleFactor 一致）。
  - **几何坐标无需预处理**：`BlockbenchJsonModel.load` 自动统计包围盒（centerX/minY/centerZ），渲染时 `translate(-center/16)` 居中贴地——json 原坐标未居中（error x∈[-14,32] 中心 9、null 中心 8、warn 中心 7、z 均中心 8）由加载器统一处理，不再需要转换脚本的"坐标居中"；旋转斜腿由加载器按 `rotation`（origin+轴+角度）逐顶点旋转，无需独立 part。
  - **删除**：ErrorMobRenderer / ErrorMobModelBase / ErrorMobModel / ErrorMobNullModel / ErrorMobWarnModel 五个 Java 类及 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 中三处模型层注册（`onRegisterLayerDefinitions` 仅保留盖金蜗牛；渲染器注册改为 ErrorMobJsonRenderer 三变种）。注：旧 Java 转换脚本、上下颠倒补偿逻辑均不再适用（json 直渲无 LivingEntityRenderer 翻转）。
- 旧转换版构建 BUILD SUCCESSFUL（43s，2026-09-20）；**json 直渲版** gradlew build BUILD SUCCESSFUL（**39s**）；开发日志目录 59、临时日志已同步。若日后用户提供正式贴图，直接覆盖 `textures/entity/error_mob*.png` 即可（json 直渲按 json 中每面 uv 采样，贴图尺寸需与源设计一致，非展开盒布局）。

### 40. 邪恶盖金 JSON 直渲模型（BlockbenchJsonModel）

素材 `模型\生物\邪恶盖金\gaijin_t58.zip`（`gaijin.json` + `gaijin.png`）：Blockbench **format_version 1.9.0**，**17 个元素**（其中 **5 个带非零旋转**：绕 origin 沿 x/y 轴旋转 7°~33°，腿/角/尾），`texture_size` **32x32**，每面独立 UV 布局。

- **方案**（用户要求"生物无动画，模型直接加载 json 不要转化"）：**不再把 Blockbench 几何转成 Java ModelPart**（此前转化过程出现大量问题），改为**运行时直接解析 json 几何并逐元素逐面绘制**：
  - 新增 [BlockbenchJsonModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/BlockbenchJsonModel.java)：`load(ResourceManager, ResourceLocation)` 从资源包读取 `assets/cc_rc/models/entity/evil_gajin.json`（`InputStreamReader` 解析，`texture_size` 归一化 UV），解析 `elements[].from/to`（AABB 角点）、`rotation`（角度/轴/origin，角度换算弧度）、`faces.<方向>.uv`（像素 → 除以 tex 尺寸得 0..1 纹理坐标）；
  - **渲染**：每元素取 8 个角点，旋转元素先 `v' = R·(v - origin) + origin`（jOML `Matrix3f.rotation(弧度, 轴向量)`）；每面 4 顶点顺序"外看左上→右上→右下→左下"（对应 uv 四角，up/down 面 v 反转修正贴图上下），法线随旋转矩阵变换后再经 `pose.normal()`；`VertexConsumer.vertex(Matrix4f,…).color(...).uv(...).uv2(packedLight).overlayCoords(packedOverlay).normal(...)` 绘制；
  - **映射坑记录**（1.20.1 官方映射）：光照/覆盖层方法名是 **`uv2(int)` / `overlayCoords(int)`**（打包值单参版本），**不存在** `light(int,int)` / `setLight(int)` / `overlay(int,int)`（MCP/Yarn 旧名或 1.20.2+ 新名均不可用）；
  - [EvilGajinRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/EvilGajinRenderer.java) 由 `MobRenderer` 改为 **`EntityRenderer<EvilGajin>`**：构造时加载 json（失败抛 IllegalStateException），render 内 `mulPose(Axis.YP.rotationDegrees(-entity.getYRot()))` 转向实体朝向，`RenderType.entityCutoutNoCull(TEXTURE)` 透明镂空双面可见（无背面剔除），贴图 `textures/entity/evil_gajin/evil_gajin.png`；
  - 原 [EvilGajinModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/EvilGajinModel.java) 已删除；[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 移除其 LayerDefinition 注册（JSON 直渲无需 bake 模型层），属性与渲染器注册不变。
- 模型资源：`assets/cc_rc/models/entity/evil_gajin.json`（zip 解包原样）+ `assets/cc_rc/textures/entity/evil_gajin/evil_gajin.png`（32x32）。
- **构建**：gradlew build BUILD SUCCESSFUL（44s）；开发日志目录 60 已同步。
- **透明不显示修复**（2026-09-20）：自绘顶点链两处错误 → 整只实体不可见（仅阴影）：
  1) **元素顺序**：`uv2(light)` 写的是 UV2，而 `overlayCoords(overlay)` 写 UV1；BufferVertexConsumer 按 VertexFormat 元素游标顺序推进，先写 UV2 时游标尚在 UV1 → 元素不匹配静默跳写 → light 缺失、后续数据错位。修正为 **POSITION→COLOR→UV0→UV1(overlay)→UV2(light)→NORMAL**；
  2) **缺失 `.endVertex()`**：顶点从未提交（vertexCount 不增长），GPU 无数据可画。链尾补 `.endVertex()`。
- **大小/UV/位置三连修**（2026-09-20，用户实测：模型高度至少 5 格、UV 错乱、位置不在碰撞箱；模型 json 与贴图确认无问题）：
  1) **过大 16 倍根因**：`renderElement` 提交顶点时**未做像素→格换算**。反编译 `ModelPart$Cube.compile` 字节码证实原版在 `vertex()` 前执行 `vertex.pos /16.0F`（16px=1 格）——本渲染器漏除 16，7.38px 高被渲染成 7.38 格。修复：顶点坐标 `/16.0F`（旋转先行在像素空间完成，均匀缩放后数学等价）；
  2) **UV 左右镜像**：`FACE_CORNERS` 中 east/west 的顶点左右顺序颠倒（从面外侧观察：east 左=南 z2、west 左=北 z1），修正为 east={7,6,5,4}、west={2,3,1,0}；
  3) **位置偏移**：模型包围盒 x∈[5,11] 中心 8px、z∈[-1.75,10] 中心 4.1px，不居中。`load()` 统计 bbox，`render()` 内 `pose.translate(-center/16, -minY/16, -centerZ/16)` 使模型中心落于实体脚底原点并贴地（先平移后旋转，绕原点转不甩动）。
- **up/down 面 UV 翻转修复 + 整体放大 1.5 倍**（2026-09-20，用户反馈大小正常但 UV 仍不对、希望放大 1.5 倍）：
  1) **UV 根因**：反编译原版 `ModelPart$Cube` 构造 + `ModelPart$Polygon` 构造的字节码，取得**黄金标准**：Polygon 顶点 UV 映射为 `v[0]→(u1,v0)、v[1]→(u0,v0)、v[2]→(u0,v1)、v[3]→(u1,v1)`，配合 Cube 6 个面的顶点顺序推导出：**原版实体 Cube 的 up/down 面"贴图上方"朝模型 +Z（南）**，与方块模型 up 面（贴图上方=北）恰好相反。之前对 up/down 加的 flipV 把贴图上下翻转 180° → 俯视即看到贴图颠倒。修复：**去掉 flipV，6 个面统一用标准 UV 映射**（侧面 north/south/east/west 经黄金标准逐角点核对全部正确，east/west 镜像问题已在上一轮修复）；
  2) **整体放大 1.5 倍**：`EvilGajinRenderer.render` 中 `pose.scale(1.5F,1.5F,1.5F)`，作用顺序为 居中平移→1.5 放大→yaw 旋转（以脚下原点为中心缩放，中心不漂移），法线不受 scale 影响。
- **UV 面角点顺序最终修正**（2026-09-20，用户：大小对但贴图仍不对）：放弃"外看视角"推导，改用 **Blockbench 内部面数据结构**（MOPS 插件 `CUBE_FACES_TRIANGLE_FANS` + `CUBE_UV_INDICES`，Blockbench 导出 cube faces 顶点顺序 + 面 UV 角点索引）逐角点核对，得到权威 6 面角点顺序（左上→右上→右下→左下）：
  - north `{2,6,4,0}`、south `{7,3,1,5}`（原已正确）
  - east `{6,7,5,4}`、west `{3,2,0,1}`（上一轮按"外看"推导的镜像修正是**错的**，与 Blockbench 数据相反，已恢复）
  - up `{6,2,3,7}`、down `{5,1,0,4}`（非简单 flipV：up 贴图上方朝 -Z、down 上方朝 +Z，且 u 小端均朝 +X）
  - 结论：之前 up/down 的 flipV 与整体 90°/180° 旋转不匹配，east/west 被改反，均已在最终数据下修正。
- **UV 归一化基准修正（像素颗粒过大）**（2026-09-21，用户：贴图样子有变化但"uv 大小错误、像素点大于正确样子"）：逐面核对 uv 数据与模型几何尺寸发现精确规律——**所有面 uv 区域（x/z 方向）恰好为模型面像素的一半**（element[1] north 面 6px 宽 ↔ uv 宽 3；element[0] up 面 5×7px ↔ uv 2.5×3.5；east 面 7px ↔ uv 3.5）。即 **gaijin.json 的每面 uv 值基于 16 像素网格**（texture_size=32 是其二倍），此前用 `uv/32` 归一化使采样区域减半 → 贴图像素被放大 2 倍（颗粒大，叠加 1.5 倍模型缩放更明显）。修复：UV 归一化统一改为 `/16.0F`（侧面 v 方向作者未严格按比例，与 Blockbench 预览一致）。**待游戏关闭后构建验证。**

### 41. 控制柜系统（ConsoleBlock / ConsoleBusBlock / Controller*）

控制柜是一套多方块结构：用 `controller_block`（另有蓝 / 黑两色变体）拼出长方体，外侧贴一个 `controller_bus` 作为**唯一数据源**——结构状态、表面组件、文字、绘图、变量表、程序行、配置 JSON 全部保存在总线方块实体里。组件物品（仪表、开关、指示灯、按钮、显示器、终端等）贴在结构外表面，由总线统一结算并按 tick 驱动；`controller_aux`（控制柜辅助计算机）右键唯一相邻的总线即可打开程序界面。总线同时提供 CC: Tweaked 外设接口与文本配置的保存 / 加载，整套系统在专用服务器上可直接运行。

**模块与职责**

| 文件 | 职责 |
| --- | --- |
| [ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java) | 总线唯一数据源：结构状态、组件、文字、绘图、变量表、程序行、NBT、VM 执行与 CC 操作队列 |
| [ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) | 服务端 tick、成型校验、外设注册、破坏通知 |
| [ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) / [ConsoleBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlockEntity.java) | 结构成员方块与 `Bus / StructureState` 标记 |
| [ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java) | 辅助计算机：右键找唯一相邻总线并打开程序 GUI（不存数据） |
| [ConsoleStructureHelper.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleStructureHelper.java) | 同色连通泛洪、长方体完整性、每 100 tick 巡检 |
| [ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) / [ConsoleSurface.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSurface.java) | 表面坐标系、组件模型、读数、文字、绘图渲染 |
| [ConsoleFrame.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleFrame.java) | 通用绘制坐标系：普通面与斜面共用同一套 `(u, v)` 换算 |
| [ConsoleSlopeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeBlock.java) / [ConsoleSlopeBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeBlockEntity.java) / [ConsoleSlopeGeometry.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeGeometry.java) / [ConsoleSlopeBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeBER.java) | 斜面控制台：宿主格映射、命中换算、按斜面重画该格内容 |
| [ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) / [ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) | 组件类型与变量能力、命中与点击动作 |
| [ConsoleBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusPeripheral.java) / [ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java) | CC:Tweaked 外设与取数 / 写值排队 |
| [ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java) / [ProgramExprParser.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramExprParser.java) | 程序 IR、校验、NBT / 包序列化；文本表达式 → 树 |
| [ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java) | 字节码编译与栈式执行（运行警告、除零 / 非有限值保护） |
| [ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) / [ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java) / [ConsoleProgramMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramMenu.java) | 变量表与程序行编辑、表达式输入控件、菜单 |
| [ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java) + `ConsoleConfigScreen / ConsoleConfigStore` | 配置 JSON 结构与保存 / 加载界面 |

#### 41.1 结构与成型

- **方块**：`controller_block`（完整立方体、无方向）+ `controller_block_blue` / `controller_block_black`（`MapColor.COLOR_BLUE / COLOR_BLACK`，强度与音效同默认色）；`controller_bus` 水平四向 `HORIZONTAL_FACING`，FACING 即总线背面，同时决定结构顶 / 底面文字的头朝向；总线方块状态另带 `COLOR = default/blue/black`。
- **校验流程**（[ConsoleStructureHelper.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleStructureHelper.java)）：
  1. 收集总线六邻居中的控制柜方块作为种子，无种子返回 `NO_BLOCK`；
  2. 首个种子六向 BFS 泛洪，超过 4096 格返回 `TOO_LARGE`；
  3. 其余种子不在同一连通体返回 `MULTIPLE`（总线同时贴着两个结构）；
  4. 最小外接长方体体积 `sx*sy*sz` 必须等于泛洪格数，否则 `NOT_RECTANGLE`（空洞 / 凸起）。
  - 只记录「最小角 origin + 三边长度」，成员由长方体范围推导，不保存成员列表；`Result` 带 `color`，`stillValid(..., count, color)` 逐格校验颜色，`isConsole(BlockState) / colorOf(BlockState)` 统一判定。
- **同色才连通**：泛洪以种子颜色为准，`colorOf(next) != color` 直接跳过；异色方块可相邻摆放但不并入同一结构，总线同时贴到异色连通体时仍报 `MULTIPLE`。总线成型时把颜色写入 `structureColor` 并用 `flag 3` 写回方块状态，溃散时复位 DEFAULT；`controller_aux` 的 `onPlace / neighborChanged` 先取相邻总线颜色、再退回相邻结构方块颜色（`flag 2` 写入，不连锁刷新）。
- **三态状态机**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`UNFORMED → PARTIAL`（成型，组件可编辑）`→ FULL`（锁定、程序与结算生效），`unlock()` 回 PARTIAL。`formStructure()` 先清旧成员标记再校验，失败回 `UNFORMED` 并记录 `Error`；服务端每 100 tick 巡检 `stillValid`（爆炸 / 指令等外部改动自动失效）。成员方块实体 [ConsoleBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlockEntity.java) 只存 `Bus(long) + StructureState`，由总线 `applyMembers` 写入并 `sendBlockUpdated` 同步。
- **卸载安全**：总线**不重写 `setRemoved()`**（该路径在区块卸载 / 保存退出时也会执行，跨方块实体的 `setChanged()` / `sendBlockUpdated()` 会让保存中的区块重入而卡死「保存世界中」）；成员标记清理交给 `ConsoleBusBlock` 的 `playerWillDestroy / onRemove`。
- **锁定保护**：`ConsoleBlock.onDestroyedByPlayer` 在 FULL 时返回 false 并提示 `message.cc_rc.console.locked`；`playerWillDestroy / onRemove` 通知总线 `invalidateStructure()`；[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 监听 `ExplosionEvent.Detonate`，把锁定的结构方块从受影响列表剔除（总线不受保护）。
- **总线界面**（[ConsoleMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleMenu.java) / [ConsoleScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleScreen.java)）：菜单只同步方块坐标，尺寸 / 状态 / 错误每帧从客户端方块实体读取；三个按钮「检测并成型 / 完全成型 / 解除锁定」按状态启用，另有一条通栏「保存/加载配置」按钮（未成型时禁用）。操作走 C2S [ConsoleStructurePacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleStructurePacket.java)（`registerMessage(7)`，action 0 成型 / 1 锁定 / 2 解锁，服务端校验类型 + 8 格距离）。
- **表面网格**（[ConsoleBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBER.java)）：仅 `PARTIAL` 时为每个外露面（邻居不是控制柜方块）用 `RenderType.lines()` 画 16×16 像素网格 + 外轮廓，便于按像素摆放组件。
- **斜面不参与泛洪**：`controller_slope` 是独立方块，只通过 `SlopeLinks` 登记到相邻结构（见 41.9）。

#### 41.2 表面坐标系与渲染

- **六面坐标系**（[ConsoleSurface.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSurface.java)）：每个外表面一组 U（右）/ V（上）/ N（外法线）单位向量，1 格 = 16 像素。四个侧面 `V=(0,1,0)`，`U` 分别为 north(-1,0,0) / south(1,0,0) / west(0,0,1) / east(0,0,-1)；顶面 `V=-FACING、U=(f.z,0,-f.x)`；底面 `V=-FACING、U=(-f.z,0,f.x)`（仰视时左右相反）。文字 / 组件坐标 `(u, v)` 是**相对该表面矩形中心的像素偏移**，与结构原点、尺寸解耦，结构扩缩不改变内容的世界位置；提供 `pixelU/pixelV/toWorld/faceCenter/halfWidthPx/halfHeightPx/contains`。
- **统一坐标帧**：[ConsoleFrame.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleFrame.java) 提供 `ofFace / toWorld / toLocal`，斜面帧把 origin 平移回「宿主表面像素 (0,0)」，因此普通面与斜面对组件 / 文字 / 绘图使用同一套 `(u, v)`，无需任何数据迁移。
- **文字渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：用 U/V/N 构造仿射矩阵（第 0 列 `U/16`、第 1 列 `-V/16`、第 2 列 `N/16`，平移为文字中心世界坐标），`font.drawInBatch` 以 `(-w/2, -lineHeight/2)` 为中心、`DisplayMode.POLYGON_OFFSET` 沿法线外推 0.005 绘制；JOML `Matrix4f` 字段非公开，只能走 16 参数构造。光照固定 `PACKED_LIGHT = 0xF000F0`（总线常处在无光位置，沿用方块光照会把文字乘成纯黑）。
- **文字条目**：`TextEntry{ id / face / u / v / text / color / bold / italic / underline / strikethrough / anchorH / anchorV / scale }`。锚点 9 种（垂直上/中/下 × 水平左/中/右）表示 (u,v) 对齐文字框哪条边或中心；缩放为「每字体像素 = scale/16 格」，默认取配置 `console_text.default_scale`（默认 0.15，范围 0.05~4.0），旧存档缺字段时按居中锚点 + 配置默认值。命中检测在客户端用 `Font.width` 按 scale 与锚点还原成表面像素矩形（各留 1 像素容差）判断（服务端没有 `Font`，无法算字宽）。
- **组件矩阵与顶点格式**：组件局部矩阵第 0 列 = 外法线 N（模型 x）、第 1 列 = V（模型 y）、第 2 列 = **-U**（模型 z），平移把 `(minX, centerY, centerZ)` 对齐到锚点。模型空间为右手系，其 z 轴 = `N×V = -U`；若用 `+U` 三列构成左手系，整个模型会被水平镜像。`RenderType.entityCutoutNoCull` 是 QUADS 格式，每个三角形必须补一个与第 3 点重合的顶点构成 `(a,b,c,c)` 四边形。锚点沿法线外推 `COMP_OUTSET = 0.002` 格，避免与 `cube_all` 方块表面共面抢深度；光照取组件表面外侧 0.05 格处的 `LevelRenderer.getLightColor`（level 为 null 时回退全亮）。
- **绘制顺序**：绘图图案 → 组件不透明模型 → 组件读数 → 组件附带文字；玻璃面单独占第二遍（所有组件的不透明面先画完，再统一画玻璃），避免同一 `MultiBufferSource` 内切换 RenderType 破坏先创建的批次。指示灯附带文字沿法线外推到模型最厚处之外，按钮 / 仪表在右侧左对齐、其余在下方居中，间距 `COMP_TEXT_GAP = 1` 结构像素。
- **渲染包围盒**：总线方块实体的 `getRenderBoundingBox()` 扩到整个结构长方体，避免表面文字 / 组件被视锥剔除。

#### 41.3 组件通用机制（放置 / 颜色 / 附带文字 / 缩放 / 拖动）

- **类型表**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：每个枚举带 `id`、`itemSuffix`（物品注册后缀，默认代号前缀取其大写，如 `METER_1`）、表面占位尺寸（像素，来自 OBJ 包围盒实测）、`recolorable`、`renderScale`（模型默认缩放）、`modelId / textureId`（默认等于 id，可复用其它模型资源），以及能力判定 `supportsFloatVariable / supportsBoolVariable / variableType / supportsHistory / isThreeState / hasSpeedVariable / hasMuteVariable / hasKeyMode`。组件类型在 NBT 里以 `Type` 字符串（`type.id`）保存、网络包不传枚举序号，枚举末尾追加新成员不影响旧存档。
- **模型加载**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：自行解析 OBJ 的 `v / vt / vn / f`（多边形扇形三角化、支持负数索引、`v` 轴翻转 `1-v`），记录 `o` 分组（`Tri.group`）与 `usemtl` 标记（`tint / prop / glass`），缓存为 `ObjMesh{tris, minX, maxX, centerY, centerZ, texture, tintU, tintV, glassTexture}`；包围盒自动给出「最内侧 x（贴表面）」与 y/z 中心，避免逐模型硬编码偏移。多状态模型用 `get(type, variant)`：`0` 基础 `<modelId>_surface.obj`、`1/2/3` 对应 `_1_ / _2_ / _3_`，双态开关另用 `get(type, closed)` 取 `_close_surface.obj`；贴图在存在 `<textureId><后缀>.png` 时优先，否则回退基础贴图。
- **放置 / 编辑 / 删除**：组件物品右键 `controller_block` → 服务端找总线、校验 PARTIAL、把点击点换算为 `(u,v)`，命中已有组件则打开组件界面，否则在点击处新增；`applyCompAdd` 把坐标夹在表面内、与已有组件矩形相交则拒绝、代号默认「类型前缀_序号」；`applyCompEdit(id, code, u, v, color, decimals, unit, holdTicks, scale, speedVar, lampVar, muteVar, keyMode, coverTicks, code2, links, rgbR, rgbG, rgbB, showOriginal)` 按类型采用对应字段（代号 `[A-Za-z0-9_]{1,32}` 且同结构内不与主代号 / 第二名称冲突，位置移动时排除自身，颜色只接受 `0xRRGGBB` 或 `-1`）；`applyCompDelete` 移除组件。返回码 `COMP_OK / NOT_FORMED / LOCKED / OUTSIDE / LIMIT / DUP_POS / DUP_CODE / BAD_CODE / NOT_FOUND / COMP_INVALID`，上限 256 个组件，仅 PARTIAL 可编辑。所有编辑走 C2S [ConsoleCompRequestPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompRequestPacket.java)（id 10，开界面）与 [ConsoleCompPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompPacket.java)（id 11，提交 / 删除），服务端按保存的组件位置做 8 格距离校验。
- **交互路由**：空手右键 → [ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) `tryUse`（找总线 → 校验成型 → `BlockHitResult` 精确命中点换算表面像素 → `contains` → `findCompAt`），客户端只消费交互、状态变更只在服务端；编辑工具右键控制台 → 表面文字 / 组件附带文字编辑界面；多功能工具右键 → 拖动；组件物品 / 画图工具 / 钥匙走各自 `useOn` 分支；背包手持非空物品时方块 `use` 一律 PASS，避免拦住这些分支。
- **颜色（两种机制）**：
  - **整图重新着色**（`recolorable = true`，[ConsoleCompTextures.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompTextures.java)）：以 `textures/block/console_parts/<textureId>.png` 为基准，只处理「亮部或带彩度」的像素，色相替换为所选 RGB、亮度按 `luma / 该类型最亮可着色像素 luma` 保留；生成 `cc_rc:dynamic/console_comp/<type>_<RRGGBB>` 的 `DynamicTexture` 并缓存（上限 96 张，超限整批释放）。`NativeImage` 像素为 **ABGR** 打包 `(a<<24)|(b<<16)|(g<<8)|r`。gauge 的指针面 UV 全部退化为同一点，因此把「三顶点 UV 相同」的三角形标记为 `solidColor`，另找一块不被任何面采样的像素涂成指针色 `C82525`，渲染时把 `solidColor` 面指向该像素，指针保持红色、表盘正常着色。
  - **逐面染色**（`usemtl tint`）：模型转换时只给背板 / 灯面 / 特定面板写 `usemtl tint`，渲染时这些面的 UV 改指贴图里的一块纯白像素、再用顶点色乘出所选颜色；[ConsoleCompModels.findTintUv](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java) 加载时按当前模型变体的所有 `tint` 面 UV 包围盒统计占用，自右下向左上找 4×4→2×2 未被采样的纯白块；找不到时退化为「只要求不透明白色」再扫一遍——同一组件的不同变体 UV 覆盖范围不同（警报器灯亮模型的灯面包围盒会盖住预留白块），若无此退化扫描，灯亮变体会拿不到采样点、背板染色在灯闪烁时忽有忽无。仍找不到（`tintU < 0`，如安全按钮调色板无纯白块）时保留原 UV、只用顶点色相乘。整图重新着色与逐面染色互斥，避免误染功能部件。
  - 未设置颜色（`NO_COLOR = -1`）时全部走原贴图；组件颜色与显示设置随 NBT 保存，`copyWith*` 系列保证扩展字段在任何状态更新时都不丢。
- **附带文字**：`CompText{ text, color, bold, italic, underline, strike }`，`NONE` 表示无文字；NBT 仅在有文字时写 `CompText / CompTextColor / CompTextBold / CompTextItalic / CompTextUnderline / CompTextStrike`，旧存档缺失 = 无附带文字。`applyCompText` 仅 PARTIAL 可改，空文本 = 清除，不保存坐标 / 缩放。字号 = `console_text.default_scale × comp.scale`，间距 `COMP_TEXT_GAP` 同比例缩放；RGB 控制器配置了附带文字时改为在组件位置按灯色绘制文字（保留粗 / 斜 / 下划线 / 删除线），不再显示模型。编辑入口为 `EditTextMenu` 的 `compId >= 0` 分支，走 C2S `ConsoleCompTextRequestPacket`（id 12）与 `ConsoleCompTextPacket`（id 13）。
- **缩放**：实际大小 = 类型 `renderScale`（模型默认缩放）× `CompEntry.scale`（玩家缩放，0.25~4.0，默认 1.0），`effectiveScale() / halfWidthPx() / halfHeightPx()` 统一供渲染、命中、重叠判定与附带文字定位使用；OBJ 基向量与平移量同时乘缩放，模型才真正以锚点为中心缩放。NBT 仅在非默认时写 `Scale`。
- **拖动**（[ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java)，仅客户端）：多功能工具右键按下抓取点击点下的组件（优先）或表面文字 / 绘图图案（优先级 组件 → 文字 → 图案），记录「锚点 - 点击点」偏移，之后每客户端 tick 按准星命中点换算新锚点，组件按 `Math.round` 吸附整像素（文字保持亚像素），去抖阈值 `SEND_THRESHOLD = 0.25` 像素；组件在客户端预判重叠、重叠不发包；松开右键 / 打开界面 / 准星离开同一表面 / 结构变化时结束，允许跨同一结构的相邻方块继续拖动。文字移动走 `applyTextMove`（只改锚点）与 C2S `ConsoleTextMovePacket`（id 14，服务端按自身保存坐标校验 64 格）。
- **结构变化保持位置**：总线记录 `lastOrigin / lastSizeX/Y/Z`（NBT 同键，失效前也保存），重新成型后用 `rebaseSurfaceEntries` 把新旧表面中心偏移投影到该面的 U/V 并从坐标中扣除，组件 / 文字 / 绘图的世界位置不变；法线方向不补偿（始终贴在新表面）。
- **组件设置界面**（[ConsoleCompMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompMenu.java) / [ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)，菜单类型 `console_comp`）：标准布局显示类型、代号、位置 X/Y、缩放、16 染料色块 + `#RRGGBB` + 「原色」按钮 + 颜色预览；按类型追加「小数位 / 单位」（数字显示屏）、「弹起时间」（按钮 / 旋转按钮 / 安全按钮）、「连接 / R / G / B / 显示原色」（RGB）、「转速变量 / 灯状态变量」（同步指示器）、「静音变量」（警报器）、「钥匙模式」（钥匙开关）、「第二名称」（圆盘记录仪）、「盖子自动关闭」（安全按钮）。背景贴图 `console_comp_gui.png`（176×152，数字显示字段时 176×174，RGB / 带附加变量时 176×212 的 `console_comp_gui_tall.png`），与文字编辑界面同一套 1px 圆角黑描边 + 左上白高光 + 右下深灰阴影 + 内部淡灰。

#### 41.4 组件清单

> 通用规则：所有组件都由总线方块实体保存、只允许 PARTIAL 编辑；`renderScale` 指模型默认缩放，玩家缩放另乘。下表「变量」一栏是程序 / CC 绑定语义。

| 组件 id（物品） | 变量语义 | 模型与贴图 | 交互与特殊规则 |
| --- | --- | --- | --- |
| `gauge`（流量表，`controller_meter`） | FLOAT `value` | 5×5 占位，hbm_ror `gauge_surface.obj`（`o Needle` 指针），`recolorable=true` | 指针枢轴按刻度圆心反推（模型 y=0.4375、z=-0.125），数值 0~100 映射 `-90° + 0.9° × value`，显示夹 -5~105；`renderScale=1.2`；空手右键无动作，只显示 |
| `numitron`（数字显示屏，`controller_display`） | FLOAT `value` | 13×5，`numitron` 模型，`recolorable=false`（颜色用作读数颜色） | 屏幕读数 `String.format("%.<decimals>f", value) + unit`，小数位 0~6（默认 2）、单位最长 8 字符，按 `font.width` 自动缩小上限 0.25；读数贴近凹陷内屏、位于模型批次之后 |
| `graph`（显示器，`controller_graph`） | FLOAT `value` + 50 点历史 | 复用 `numitron` 模型与贴图（`modelId=numitron`） | 以「█」按列画 50 点趋势线，`history[0]` 在最右列，柱高按 0~100 连续映射、列宽 `11.8/50 ≈ 0.236` 结构像素、线高 0.4 像素；默认红色、可用组件颜色；不参与点击状态 |
| `indicator`（指示灯，`controller_lamp`） | BOOL `state` | 7×4，`indicator` 模型（`Base / Light` 分组），`recolorable=true` | 熄灯时颜色 × 0.38 后整图重新着色；未设置颜色时把 `Light` 分组顶点色压到 `INDICATOR_OFF_GRAY = 97`，外壳保持原色 |
| `keypad`（按钮，`controller_button`） | BOOL「是否按下」，只读 | 5×5，`keypad` 模型，`Button` 分组可内推 | 空手右键按下 `state = gameTime + holdTicks`，`holdTicks` 默认取配置 `console_button.hold_ticks`（20 tick），设为 0 时 `state = HOLD_UNTIL_RELEASE(-1)` 表示按住直到松手（松开由 `ConsoleKeypadReleasePacket` id 17 清零）；按下动画 `Button` 沿 -x 内推 `KEYPAD_PRESS_DEPTH = 0.03`；`renderScale=0.5`，程序不可直写 |
| `terminal`（发信终端，`controller_terminal`） | 不参与变量 | 14×14，`terminal` 模型 | 仅外观与占位，未接入变量与程序 |
| `knob_switch`（旋钮开关，`controller_switch`） | BOOL，`state` 0/1 | 14×14，开 / 关两套模型（绿灯箭头左上 / 红灯箭头右下），`o Panel` 可染色 | 空手右键翻转 0↔1，`renderScale=0.3`；背板染色，灯座与手柄保持原色 |
| `lever_switch`（拉杆开关，`controller_lever`） | BOOL，`state` 0/1 | 14×14，开 / 关两套模型（拉杆左倾绿灯 / 右倾红灯），`o Panel` 可染色 | 同旋钮开关，`renderScale=0.3` |
| `gauge_v`（立式仪表，`controller_gauge`） | FLOAT `value` | 14×14，`gauge_v` 模型（中央竖槽 `Needle`），`recolorable=false` | 指针垂直平移，50% 居中，半行程 `GAUGE_V_HALF_TRAVEL = 0.32` 格；显示夹 -5~105；`renderScale=0.45` |
| `gauge_arc`（调节器仪表，`controller_knob`） | FLOAT `value` + `state` 0~100（默认 50） | 14×14，`gauge_arc` 模型（指针烘焙在 35%），`recolorable=false` | 左按钮减、右按钮加（1，Shift 为 10），指针绕枢轴 `(0.4344, 0.5)` 旋转 `1.2°/1%`，0% → -42°、100% → +78°；左红右绿；`renderScale=0.45` |
| `rgb_color`（RGB 颜色控制器，`controller_rgb`） | BOOL「启用」+ R/G/B 三个 FLOAT 通道 + `links` 连接组件 | 14×14，`indicator_orange_on` 换型模型（`Lamp` 灯面可染色），`renderScale=0.25` | 仅 FULL 时按 `R<<16|G<<8|B` 结算灯色并同步给 `links` 中的组件；启用取同名 BOOL 变量，否则取 `!showOriginal`；有附带文字时代替模型绘制彩色文字 |
| `lever_f`（阀门拉杆，`controller_lever_f`） | BOOL，`state` 0/1 | 10.5×14，开 / 关两套把手模型（`Panel` + `Handle`），两者红色面可染色 | 完全等同开关；关态直接切另一套 OBJ，不做运行时旋转；`renderScale=0.3` |
| `button_x`（旋转按钮，`controller_button_x`） | BOOL「是否按下」，只读 | 14×14，`Body` 背板 + `Rotate` 中间件（烘焙按下态），背板黄色面可染色 | 完全等同按钮（含弹起时间、按住直到松手）；未按下时 `Rotate` 绕面板法线顺时针 90° 复位，按下保持烘焙态；`renderScale=0.15` |
| `t_switch`（三状态旋钮开关，`controller_t_switch`） | FLOAT 0/1/2（写入四舍五入夹紧） | 14×14，三套模型（绿 / 橙 / 红三档），`o Panel` 可染色 | 右键 `(state+1) mod 3` 循环并同步 `value`；渲染取 `2 - state` 映射模型档位（state 0 = 右 + 红灯）；`renderScale=0.3` |
| `t_lever`（三状态拉杆，`controller_t_lever`） | FLOAT 0/1/2 | 14×14，三套模型，`o Panel` 可染色 | 同三状态旋钮开关，`renderScale=0.3` |
| `lamp_d`（双色指示灯，`controller_lamp_d`） | BOOL `state` | 14×14，红亮绿灭 / 绿亮红灭两套模型，共用 `t_switch.png` | `state 0 → 红亮模型、1 → 绿亮模型`，`renderScale=0.3`，`o Panel` 可染色 |
| `gauge_f`（扇形仪表，`controller_gauge_f`） | FLOAT `value` | 14×14，`gauge_fan` 模型（`Frame / Face / Hub / Needle`），`recolorable=true` | 指针绕枢轴 `(0.5, 0.5)` 旋转，烘焙角 -17°，0 值 -30°、100 值 +210°，全程 240°；表盘亮部着色、指针与刻度保持原色；`renderScale=0.45` |
| `synchroscope`（同步指示器，`controller_synchroscope`） | FLOAT 角度（自身变量）+ `_s` FLOAT 转速 + `_b` BOOL 灯状态 | 14×14，灯亮 / 灯灭两套模型（`Frame / Face / Hub / Needle / Lamp`），`recolorable=true` | 角度 `wrapAngle180` 后与转速累加并再次环绕；`|速度| < console_synchroscope.lamp_speed`（默认 15 度/tick）且 `|角度| ≤ 15°` 时灯亮；`_b` 为只读输出、每 tick 覆盖；转速 / 灯变量名可自定义；指针 0 位竖直向上（烘焙角 -107°）；`renderScale=0.45` |
| `button_s`（安全按钮，`controller_button_s`） | BOOL「是否按下」 | 14×14，合盖 / 开盖弹起 / 开盖按下三套模型 + 玻璃罩（`usemtl glass`），按钮本体 `o Button` 可染色 | `state`：0 合盖、`OPEN_COVER(-2)` 开盖弹起、`HOLD_UNTIL_RELEASE(-1)` 按住、正数弹起时刻；Shift 开 / 关盖，开盖后右键按下；`coverTicks` 默认 100（0 = 不自动关），到点自动合盖；玻璃用 `entityTranslucentCull` 两遍绘制；`renderScale=0.3` |
| `switch_k`（钥匙开关，`controller_switch_k`） | BOOL 开关状态 | 14×14，四套模型（合盖/开盖 × 无/有钥匙），`o Panel` 可染色 | 无钥匙不能开关；部分成型 Shift 右键可取出钥匙物品 `console_key`，成型后钥匙右键插入（非创造消耗 1 个）；`keyMode`：0 开闭 / 1 常开 / 2 常闭，决定拔钥匙时机；模型 variant = `(hasKey?2:0)+(state?1:0)`；`renderScale=0.3` |
| `graph_l`（圆盘记录仪 L，`controller_plotter_l`） | FLOAT `value` + 50 点历史 + 第二名称 `code2` 触发 BOOL | 16×13，`recorder` 模型（`Body` 静止 + `Pen` 只纵向平移），`recolorable=false`（颜色只作曲线色） | 笔按 `history[0]`（无历史取 `value`）纵向平移；曲线按纸面区域 `-5.78..` 宽 `9.7682`、0/100 对应纵向 `-3.664 / +3.612`、线宽 0.5；触发变量每 tick 清零、程序或 CC 置 true 时补记一笔；`renderScale=1.0` |
| `alarm`（警报器，`controller_alarm`） | BOOL 报警 + `_Q` 只读 BOOL 静音 | 16×16，6 套变体（灯灭/灯亮 × 无/静音按下/重置按下），`lamp_buttons` 模型，红色背板可染色 | 面板从左到右为方形灯 / 静音 / 重置，点击按 `rel < -0.33 / -0.33~0.32 / > 0.32` 分三段；报警中未静音时灯按 `gameTime/10` 闪烁并每 10 tick 播放 `BELL_BLOCK` 占位音效（按 `comp.id` 错开）；静音置 `muted=true` 并镜像 `_Q`，重置把值写回 0；报警解除自动清 `muted`；`renderScale=0.45` |
| `numpad`（数字键盘，`controller_numpad`） | FLOAT `value` | 16×16，`keypad` 模型，屏幕宽 9.6 / 高 1.9 结构像素（中心上移 5.35） | 屏幕显示最多 4 字符（`%.2f` 去尾零 → `%.1f` → 整数 → `9999 / -999`），默认 LED 橙 `0xFFFFA040`；空手右键打开 200×104 输入界面，确认发 `ConsoleNumpadSetPacket`（id 24）写回组件与变量，放弃 / Esc 不发包；`renderScale=0.5` |

#### 41.5 程序系统（变量表 / IR / 字节码 VM / 表达式 / 编辑界面）

**变量表**

- 类型 `VarType{BOOL, FLOAT}`，`VarDef{slot, name, type, compId, trigger}`；**槽位下标即字节码地址**，地址与变量创建后永久绑定，变量只由玩家手动删除。0 号槽位固定为「程序运行间隔」（只读 FLOAT，非负整数 tick，0 = 停止）。
- 槽位上限由配置 `console_program.max_vars` 决定（默认 64，范围 2~256），即使写入更大值也会再夹到 256（字节码地址为 1 字节）；数组按硬上限 256 预分配，NBT 只保存到最后一个已定义槽位。
- 浮点 / 布尔值分别存 `varFloats / varBools`，随总线 NBT 持久化；aux 只展示变量表、不存值；`getVarFloat(0)` 直接返回运行间隔。
- 组件绑定：变量名与设备代号一致即自动绑定（不需要设备下拉），类型由设备决定并锁定（开关 / 指示灯 / 按钮 / 双色灯 / 三状态开关等 → BOOL，仪表 / 显示屏 / 显示器 / 圆盘记录仪 / 数字键盘等 → FLOAT；发信终端与圆盘记录仪以外的历史类不参与）；`compId = 0` 为自定义变量；组件删除后变量保留、值冻结。附加变量按名称约定识别：同步指示器 `_s` / `_b`、警报器 `_Q`、圆盘记录仪 `code2`（触发）。服务端 `applyVarCreate` 按名称重新匹配组件（不信任客户端 compId），不支持变量的设备代号返回 `VAR_NAME_IS_COMP(10)`，被程序引用的槽位删除返回 `VAR_IN_USE(11)`。
- 变量名规则 `[A-Za-z0-9_]{1,32}`、新建时不能以数字开头、不能与 `true / false` 重名（程序常量语法冲突），客户端与服务端双重校验；NBT 读取沿用旧正则，避免旧存档中数字开头的变量被丢弃。
- 组件 ↔ 变量同步：`findVarSlotByComp / writeBackToVar / refreshVarsFromComponents`；空手操作组件后立即回写绑定变量，打开程序界面前统一刷新，新建绑定变量以设备当前值为初值；「保存」时 `applyVarValuesSave` 把所有绑定组件强制上传一次（即使值未变）再 `sync()` 一次。

**程序行与 IR**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java)）

- `MAX_PROGRAM_LINES = 64`，`ProgramIR.Line[] programLines`（下标即行号，`null` 为空行）。`Line{ enabled, cond, condSlot, targetSlot, expr, comment, exprSource, condExpr, condSource }`：`cond` 为旧三态 `ALWAYS / TRUE_VAR / FALSE_VAR`（`condExpr == null` 时使用，兼容旧存档），`comment` 最长 80 字符、不参与编译与校验。
- 表达式树 `Expr`：`FLOAT / BOOL / VAR / NEG / NOT / CAST / ADD / SUB / MUL / DIV / IDIV / MOD / EQ / NE / LT / GT / LE / GE / AND / OR`，以及一元 `FLOOR / FRAC / ABS`（符号 `# ~ @`，与 `! -` 一起以 `ordinal` 追加，网络包只允许追加）；`Expr.type(Scope)` 做类型推导，`depth()` 给嵌套深度。
- `Line.validate(Scope, slots)` 在保存前集中校验：目标必须是已定义且可写的变量、条件必须是布尔、表达式非空且深度 ≤ 16（`MAX_DEPTH`）、表达式类型必须与目标一致，返回 `OK / BAD_TARGET / BAD_COND / BAD_EXPR / TYPE_MISMATCH / TOO_DEEP`。`Expr / Line` 各自实现 NBT 与网络序列化，GUI、网络包、配置 JSON、编译器共用同一份结构；反序列化时递归按剩余深度余量递减，超限返回 null，防止恶意 / 损坏数据触发 `StackOverflowError`。
- NBT：外层 `ProgLines`，每个非空行保存 `Idx` + 行内键 `On / Cond / CondSlot / Target / Expr / Comment / Src / CondExpr / CondSrc`；读取前整表清空、按 `Idx` 还原，重复行号以先读到为准。
- 保存走 `applyProgramSave`（仅服务端、FULL 拒绝、逐行校验，结果码 `PROG_*` 20~25），通过后整表替换并 `sync()` 一次；C2S [ConsoleProgramPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleProgramPacket.java)（id 16，`ACTION_SAVE_LINES` 全量快照，服务端校验 8 格与成型状态）。

**字节码编译与栈式 VM**（[ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java)）

- 一条指令打包进一个 `int`：低 8 位操作码、高 24 位操作数（变量槽位或跳转目标）；浮点常量占两条 int（`OP_PUSH_F` 后紧跟 `Float.floatToRawIntBits`）；跳转操作数用**指令下标**。指令集 `NOP(0x00) / PUSH_F / PUSH_B / LOAD_F / LOAD_B / STORE_F / STORE_B / ADD_F / SUB_F / MUL_F / DIV_F / FLOORDIV_F / MOD_F / NEG_F / NOT_B / AND_B / OR_B / LT_F / LE_F / GT_F / GE_F / EQ_F / NE_F / EQ_B / NE_B / B2F / JMP_IF_FALSE(0x1A) / HALT(0x1B) / FLOOR_F(0x1C) / FRAC_F(0x1D) / ABS_F(0x1E)`，`BYTECODE_VERSION = 2`。字节码不写存档，读档后 `programDirty = true` 重新编译。
- 编译 `compile(lines, scope)`：跳过禁用行与空行；条件非恒真时生成条件代码（`condExpr` 优先，旧三态补 `LOAD_B / NOT_B`）并记录 `JMP_IF_FALSE` 回填位；表达式后按目标类型生成 `STORE_F / STORE_B`；算术 / 比较为布尔操作数自动补 `B2F`，双方均布尔时 `== / !=` 走 `EQ_B / NE_B`，否则浮点按 1e-5 容差；`//` 为 `floor(a/b)`、`%` 余数符号跟随除数。编译期同步记录每条指令所属程序行（`lineOf`）供警告定位；无有效行或超过 `MAX_CODE = 2048` 指令返回 null（不执行）。
- 执行 `execute(compiled, floats, bools)`：浮点 / 布尔双栈 + 共享栈指针，执行期零分配，只读写传入的影子数组；每条指令先按 `requiredStack(op)` 检查栈深（一元 / 写变量 1、二元 2），不足立即结束；除零结果记 0 并记 `DIV_ZERO`，任何非有限结果（NaN / Infinity）置 0 并记 `NON_FINITE`；同一行同一原因去重、最多 `MAX_WARNINGS = 4` 条；非法操作码 / 超步数直接结束。
- 总线侧字段：`programCode`（运行时编译缓存，不持久化）、`programDirty`、`programTimer`、`shadowFloats / shadowBools`、`runWarnings`；保存程序、增删变量、NBT 读取时置脏，保存程序同时清零计数器与旧警告。

**tick 调度**

- 服务端 `tick()` 顺序：`releaseExpiredKeypads()` → `releaseExpiredCovers()` → `clearPlotTriggers()` → `tickProgram()` → `applyCcOps()` → `flushPlotters()` → `applyRgbControllers()` → `applySynchroscopes()` → `applyAlarms()` → `publishCcSnapshot()`；未成型清零计数器，每 100 tick 的结构巡检独立保留，结构失效分支同样先清零再结算。CC 的写入在程序之后结算，组件直接写入最后生效，全部结算完最多同步一次。
- `runProgram()` 严格执行影子数组三步：① 读取——绑定组件的变量取组件实时状态（开关 `state != 0`、按钮类用 `isKeypadPressed(gameTime)`、仪表取 `value`），自定义变量沿用总线值，0 号槽位写入运行间隔，组件来源失效时沿用冻结值并记 `SOURCE_INVALID`；② 执行——只读写影子数组；③ 提交——变量值写回总线，只有值变化的绑定组件才 `uploadVarToComponent`，变量 / 组件 / 警告任一变化才 `sync()` 一次。只有**FULL 且 period > 0** 时按 `programTimer` 累计到 period 执行一次，period = 0 停止但保留上次警告。
- 圆盘记录仪只在触发变量为 true（程序赋值或 CC `pushPoint`）时由 `flushPlotters()` 补记采样，普通变量写入不再无条件记点；`uploadVarToComponent` 对记录仪只镜像当前值、不动历史，触发变量不参与上传与回读。

**运行警告与回显**

- 警告不写存档；`getUpdateTag()` 额外写 `ProgWarn`（行号 + 原因），客户端 `load()` 解析到 `runWarnings`；程序界面在行列表底部显示「⚠ 第一条警告（+N）」，悬停给出全部警告，来源失效无行号、单独提示。文案 `gui.cc_rc.program.warn_div_zero / warn_non_finite / warn_source`。

**表达式文本（[ProgramExprParser.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramExprParser.java)）**

- `parse(source, vars, targetType)` 返回 `Result{expr, error(code + [start,end) + 中文提示), tokens}`，失败时 `expr` 为 null、`tokens` 仍可用于着色。词法：`NAME`（`[A-Za-z0-9_]`，经 `Vars.findSlot` 解析槽位，未定义记 -1）、`NUMBER`（小数 / 科学计数法）、`BOOL`（true/false，忽略大小写）、`OP`、`LPAREN / RPAREN / EOF`；空白与换行只作分隔，双字符运算符优先（`&& || == != <= >= //`），单个 `& / |` 与其它字符记 `bad_char`。
- 语法为 Pratt 优先级爬升：`||` < `&&` < 比较 < `+ -` < `* / // %` < 一元 < 原子，全部左结合；减号在操作数位置按取负、一元可连续叠用（如 `-@A`）。解析时携带期望类型（`&& || !` 的操作数期望布尔，算术 / 比较期望数值，顶层用目标变量类型），布尔位置上的 `0 / 1` 解释为布尔常量；解析完成后用 `Expr.type(vars)` 复核并要求与目标类型一致，另做 `MAX_DEPTH` 检查（超出报 `too_deep`，同时限制递归）。
- 一元只支持符号 `! - # ~ @`（`#` 取整、`~` 取小数部分、`@` 绝对值），**不提供** `floor() / frac() / abs()` 函数写法；仅保留历史数据里的 `float(x)`（显式布尔转浮点），且 `float` 优先按变量名解析，未定义时才当拓宽写法并计入嵌套深度。
- 相等必须写 `==`，单个 `=` 在词法阶段直接报错并提示改写。

**原文与条件表达式**

- `Line.exprSource`（`Src`，最长 1024）保存玩家书写的原文（可含换行），NBT 空串不写、网络包尾部追加、配置 JSON 用 `source` 字段；打开行编辑器优先回显原文，缺失时用 `Expr.toText(scope)` 生成。
- `Line.condExpr`（`CondExpr` 复合标签）+ `condSource`（`CondSrc`）：条件留空表示总是执行；存在 `condExpr` 时 VM 直接编译它再 `JMP_IF_FALSE`，`null` 时保持旧三态路径；`validate` 要求条件类型为 BOOL。配置 JSON 双写 `condition_expr` 与旧 `cond / condition`，导入时 `condition_expr` 优先。

**表达式输入控件**（[ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java)）

- 以内核 `MultilineTextField` 外包绘制与键鼠转发：构造时按控件宽度算 `wrapWidth`，用 `font.getSplitter().splitLines(...)` 自行缓存每个显示行的原文区间（`List<int[]>`，空串补 `{0,0}`、结尾换行补 `{len,len}`），因为原版 `StringView` 是 protected 嵌套 record、外部无法读取区间。逐显示行「Token 分段着色 → 错误区间改红 → 选区底色 → 光标」，`scissor` 裁剪并自绘滚动条；选区区间用「光标 ± `getSelectedText().length()`」反推。
- 交互：`keyPressed` 转发原版（表达式框 Enter 插入真实换行、Ctrl+Enter 提交；条件框用 `singleLine` 模式，Enter 交还界面提交），Esc 在补全展开时只收补全；滚轮仅在鼠标位于控件内时生效。
- 自动补全：光标在开头、空白之后或算符 / `(` 之后时直接列出全部可读变量（`true/false` + 变量），其余位置按前缀过滤；聚焦即自动弹出、失焦收起；自动弹出的列表标记 `autoOpened`，此时回车只收列表，Tab 直接应用候选，上下键导航后回车确认；候选按变量 / 常量 / 算符分别着色，悬停显示变量类型与绑定设备信息。
- 语法着色两套调色板：列表（浅灰底）布尔未绑定 `#4A7FD6` / 绑定 `#1E4FA8`，浮点未绑定 `#3F9A4C` / 绑定 `#1E6B2A`；编辑框 / 悬浮提示（深色底）布尔 `#A8CCFF / #7FB4FF`、浮点 `#A8F0B0 / #7FE08A`；常量黄色、算符（含 `=`、括号、`!`、一元 `-`）品红、注释灰色；`EditBox` 自身文字颜色由 `syncBoxTextColor` 每帧设置，再叠加分段覆盖绘制。

**变量表 / 行编辑 GUI**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）

- 面板 416×260，左栏 162 像素（含 18 像素设备图标列）、右栏 212，背景贴图 `console_program_gui.png` 与界面 1:1；变量表与程序列表为原版凹陷深色槽，按钮统一 `VanillaButton`（浅灰凸起、悬停深红内描边、无阴影文字）与 `ArrowButton`（`page_arrows.png` 四象限切片）。
- 变量表按页显示 8 行（滚轮整页翻、左下 `< / >` 翻页按钮），首行固定 `#00 运行间隔`（只可改值、不可删 / 改名 / 改类型），空槽位点击打开新建表单；名称输入框带仿原版指令的自动补全下拉（空输入即列全部可绑定设备、输入后前缀过滤、滚轮滚动、悬停中文名提示、Esc 先收下拉）；布尔变量点击整行切换、浮点变量就地弹数值输入；底部「保存 / 放弃」，「保存」提交并强制上传一次，「放弃」清空本地待提交值，**Esc 关闭等同保存**。
- 程序区两种视图：行列表（每页 8 行 / 共 64 行，左侧启用开关，行内「条件 目标 = 表达式」+ 下方注释双行、长算式悬停看全文，右侧「移」热区，翻页 / 保存按钮；行编辑期间列表与翻页不响应点击）与行编辑器（启用 / 复制 / 删除 / 条件 / 目标 / 表达式多行框 / 注释 / 确定 / 取消）。
- 行编辑器即时校验：所有输入 `setResponder → onEditorChanged → refreshLineError`，每键解析表达式与条件并调用 `Line.validate`，错误直接显示在底部；载入已有行时 `loadingLine` 抑制中途报错；打开已有表达式时优先原文回显。移动把源行 `snapshot[to] = line; snapshot[from] = null` 后整表提交并跳到目标页；复制把整行（含注释与启用状态）收进 `pendingCopy`、由玩家点空行放置；删除在行编辑器内执行。FULL 时列表与编辑器只读并提示 `prog_locked`。
- 变量表 / 程序列表 / 行编辑器 / 新建变量表单都支持同时操作（各面板只以自身状态为前提，不再互斥）；`keyPressed` 按焦点决定活动面板。

**网络包**

- [ConsoleVarPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleVarPacket.java)（**id 15**）一条包四种动作：创建 / 删除 / 保存数值 / 保存运行间隔（仅 0 号槽位）；服务端校验 8 格与成型状态，结果码翻译为动作栏提示。
- 运行间隔值存 `programPeriod`，上限 `MAX_PERIOD_TICKS = 72000`，手动输入浮点四舍五入、负值按 0（`Math.round(Math.max(0, Math.min(上限, 值)))`），客户端 `sanitizePeriod` 与服务端 `applyProgramPeriod` 双重保证；`applyVarValuesSave` 忽略 0 号槽位，单独通过 `savePeriod` 提交。

#### 41.6 CC: Tweaked 外设接口

- **注册**：`CcRc` 的 `registerPeripheralProvider` 把 `ConsoleBusBlockEntity` 暴露为 `ConsoleBusPeripheral`，外设类型 `controller_bus`；电脑用调制解调器接到总线即可调用。
- **线程模型**（[ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java)）：所有 Lua 方法**不标注 mainThread**——Lua 线程只读写 volatile 快照与加锁的待处理队列，零 `Level` 访问、0 tick；主线程每 tick `drain()` 一次，按调用顺序批量应用并最多同步一次，连续多次读写不各消耗 1 tick。写操作同时记入 overlay，读取优先返回 overlay，overlay 与待处理队列在同一把锁内随 `drain()` 一起清空，既可读己之写又不会永久遮蔽程序 / 玩家的修改。
- **变量接口**（[ConsoleBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusPeripheral.java)）：`listVars()`（name / type / address / component）、`getVar(name)`、`setVar(name, value)`（布尔 / 数值类型校验，0 号运行间隔不可写）、`getPeriod()`（只读）。
- **组件接口**：`listComponents()`（code / type / id / kind）、`getComponent(code)`（value / state / decimals / unit，RGB 额外返回 color / enabled）、`getComponentValue(code)`、`setComponentValue(code, value)`；按钮面板为只读按压状态，写入返回 false；代号不区分大小写。`code` 不是组件代号时回退到变量名，仅放行「已绑定组件的变量」，因此可用 `getComponentValue("SYNCHROSCOPE_1_s")` 读转速、`setComponentValue("SYNCHROSCOPE_1_s", 5)` 写转速、`getComponentValue("SYNCHROSCOPE_1_b")` 读灯状态；组件代号优先，未绑定的普通变量不通过组件接口暴露。
- **圆盘记录仪**（类型 `graph` / `graph_l`）：`pushPoint(code, value)`（0~100 钳制、整体移位、返回写入值）、`setPoint(code, index, value)`（Lua 索引 1~50，1 = 最新）、`getPoint(code, index)`、`getPoints(code)`（长度 50）；历史数组只换引用、不原地修改，快照与 Lua 读取共享安全。
- **RGB 控制器**：`getColor / setColor`（直接 `0xRRGGBB`，与 CC `colors` 表兼容）、`getChannels / setChannels`（0~255 三通道）、`setChannel`（r/g/b/red/green/blue 单改）、`getEnabled / setEnabled`（启用 = 不显示原色）；内部走 `OP_RGB_COLOR` 与 `CompInfo.color / enabled` 快照字段，`applyCcOps` 中 RGB 的 `OP_COMP_BOOL` 特判为启用开关。
- **执行顺序**：程序先跑、CC 队列后结算；变量写入后立即上传绑定组件（记录仪按触发变量决定是否记点），组件直接写入最后生效。结构未成型时仍可读写已有变量 / 组件，只是程序不运行。
- **说明书**：[InstructionBook2Item.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBook2Item.java) 目录含「6. 控制柜总线」，附 4 页 Lua 方法说明与示例。

#### 41.7 配置 JSON 保存与加载

- **入口**：总线界面通栏「保存/加载配置」按钮（未成型禁用）→ [ConsoleConfigScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleConfigScreen.java)（保存 / 加载两个按钮，Esc 返回总线界面）。
- **格式**（[ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java)）：`format = "cc_rc_console_config"`、`version = 1`，Gson `setPrettyPrinting`，可直接用记事本编辑。内容：`size(x/y/z)`、`facing`（总线相对结构朝向，`Direction.getName()`）、`components`（type/face/u/v/code/code2/color/text/decimals/unit/holdTicks/scale，RGB 另有 links/rgbR/rgbG/rgbB/showOriginal）、`texts`（face/u/v/content/color/粗斜下删线/anchor/scale）、`variables`（name/type/trigger/component/value/bool）、`program`（period + lines：row/enabled/cond/condition/condition_expr/condition_source/target/expr/comment/source）。表达式以结构化节点保存（`op` + `number/flag/var/left/right`），变量与组件统一用名称 / 代号引用，不写内部槽位与组件 id。
- **客户端读写**（[ConsoleConfigStore.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleConfigStore.java)）：存放于游戏目录 `cc_rc_configs/<名称>.json`（UTF-8，自动建目录），文件名过滤 `\ / : * ? " < > |`、限制 48 字符、列表按名称排序；导出由 `exportConfigJson()` 用 `getUpdateTag()` 的同步数据组装，不写 state / value / 历史点，程序只导出非空行并记录原行号。
- **服务端校验应用** `applyConfigJson(String)`：方块实体类型与距离（包内 256 格）→ 结构与总线交汇点存在且为 PARTIAL → JSON 解析（`CFG_BAD_JSON`）→ `format` + `version` → 朝向合法性与旋转角（竖直朝向 `CFG_FACING_MISMATCH`）→ `size` 按旋转后（奇数次 90° 时 X/Z 互换）比对（`CFG_SIZE_MISMATCH`）→ 变量数不超上限（`CFG_LIMIT`）→ 逐项构建组件 / 文字 / 变量 / 程序（任何非法 `CFG_INVALID`）。全部通过才整体替换并 `sync()`，不会留下半套配置。重建时按现有规则校验代号格式、主代号与第二名称共用命名空间去重、坐标按该面实际像素范围夹紧、同面重叠检测；变量按名称重新分配槽位、`trigger` 必须绑定圆盘记录仪第二名称且变量名等于该代号、类型强制跟随所绑组件；程序行号 0~63 且不重复、条件 / 目标 / 表达式按变量名解析后 `line.validate()` 必须通过、按钮面板不可作写入目标。
- **网络**：[ConsoleConfigUploadPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleConfigUploadPacket.java)（C2S，上限 1 MiB，按 UTF-8 字节串 + int 长度直写，绕开 `writeUtf` 32 KiB 限制）与 [ConsoleConfigResultPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleConfigResultPacket.java)（S2C 结果码），注册为 id 18 / 19；结果包不在网络包内直接引用客户端 GUI，沿用 `PENDING` + `DistExecutor.unsafeRunWhenOn` + [ConsoleConfigClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleConfigClientHandler.java)。
- **旋转加载**：`rotationSteps()` / `rotateFace()` 按保存朝向与当前总线朝向的顺时针 90° 差值旋转组件与文字的 `face`，奇数次交换 X/Z；局部 `u/v` 随表面坐标系一起旋转而保持不变（顶 / 底面绕 Y 旋转朝向不变）。
- **覆盖确认**：加载时总线已有数据（`hasConfigData()`）先弹「确认覆盖 / 取消」；保存时同名 `.json` 已存在（`ConsoleConfigStore.exists()`）也先确认；确认按钮按待处理动作（保存文件名 / 待上传 JSON）分派。

#### 41.8 画图工具（draw_tool）

- **数据模型**（`block/console/draw/`）：[ConsoleShapeType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleShapeType.java) 枚举 `RECT`（实心矩形）/ `RECT_OUTLINE`（空心矩形）/ `LINE`（线段）/ `ARROW`（箭头）/ `PRESET`（预设图案），`usesWidth()` 标出可设线宽的类型；[ConsoleShape.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleShape.java) 是不可变图形 `id / face / layer / type / color(0xRRGGBB) / width / u1,v1,u2,v2 / preset / rotation / scale`，`MIN_WIDTH=0.1 / MAX_WIDTH=4.0`、`LAYERS=8`、`MIN_SCALE=0.1 / MAX_SCALE=3.0`，`quantize(v)` 四舍五入到 0.1 避免浮点噪声累积；`save/load` 用 `putFloat/getFloat`（旧 int 线宽 / 缩放可被兼容读取，旧 0~3 旋转 int 读入时 ×90 迁移为角度）。
- **预设**（[ConsoleDrawPresets.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleDrawPresets.java)）：共 24 条，登记 `id / 贴图 / 像素尺寸（32 或 16）/ 最大缩放 3.0`；贴图位于 `textures/gui/draw_presets/`（中文文件名改为英文 id），含储罐、泵、汽轮机、反应堆、抑压池、换热器、汽水分离器、给水加热器、过滤器、电池、变压器、整流器、电闸、各类阀门与 `sample`。显示时先乘 `DISPLAY_SCALE = 0.5` 再乘玩家缩放（32 像素贴图默认占 16 表面像素），预设贴图以顶点色相乘染色。
- **存储**：图形按面存总线方块实体的 `Map<Direction, List<ConsoleShape>>`，上限 `MAX_DRAW_SHAPES = 512`，随总线 NBT 保存（`Draws` + `NextDrawId`）并经 `getUpdateTag` 同步，GUI 直接读本地方块实体。服务端 API：`getDrawShapes / countDrawShapes / findDrawShape / findDrawShapeAt / applyDrawAdd / applyDrawEdit / applyDrawSetAnchor / applyDrawDelete`，返回 `DRAW_OK / NOT_FORMED / LOCKED / OUTSIDE / LIMIT / NOT_FOUND / BAD_DATA`；`shiftDrawShape` 按包围盒整体夹紧、`rebaseSurfaceEntries` 同步平移，`hasConfigData()` 计入图形数量。
- **渲染**（[ConsoleDrawRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleDrawRenderer.java)）：按 U/V/N 基向量构造矩阵（列 `U/16、V/16、N/16`），外推 `OUTSET_BASE = 0.001 + layer*0.0001`（最大 0.0017，小于组件 `COMP_OUTSET=0.002`，图案位于组件下方）；实心图形用 1×1 白贴图 `textures/block/console_parts/white.png` + 顶点色（`entitySolid`），预设用 `entityCutoutNoCull` 贴单色 PNG，`rotation` 任意角度绕中心旋转、UV 固定 0..1。箭头头长 `min(len, 4*width)`、头半宽 `1.5*width`，`ConsoleShape.bounds` 留边同步；所有顶点传入固定「视空间向上」法线 `FLAT_NORMAL`，避免实体着色器漫反射把颜色压灰。接入 `ConsoleBusBER.render` 最前面（组件之前）。
- **网络**：[ConsoleDrawPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleDrawPacket.java)（id 21，`OP_EDIT / OP_MOVE / OP_DELETE / OP_CREATE`，服务端 100 格校验），[ConsoleDrawCreatedPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleDrawCreatedPacket.java)（id 22，S2C 回执 `pos + shapeId`，-1 = 失败），[DrawToolSettingsPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/DrawToolSettingsPacket.java)（id 23，C2S 工具设置，服务端校验预设白名单后写 NBT）。
- **属性界面**（[ConsoleDrawMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleDrawMenu.java) / [ConsoleDrawScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleDrawScreen.java)）：单个图案的 386×220 属性界面（不在 GUI 内画布绘图），菜单携带图形全部参数、表面半宽高与 `shapeId / pending`；控件为 5 个类型按钮、16 色块 + `#RRGGBB`、线宽 0.1~4.0、图层 0~7、位置 X/Y（左下角，表面像素）、尺寸（矩形宽高 / 线段箭头长度 + 角度）、右侧 4 列 × 3 行预设缩略图（滚轮滚动、裁剪、滚动条）、选中预设时的旋转 0~359.9 与缩放 0.1~3.0、保存 / 删除 / 关闭；未创建时选线段 / 箭头默认长度 8、角度 0，点类型或贴图才发 `OP_CREATE`，回执轮询在 `containerTick()`（`tick()` 是 final）。绘制与命中统一用绝对坐标。
- **物品与快速绘画**（[DrawToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DrawToolItem.java)）：工具 NBT 记住 `DrawColor / DrawWidth / DrawScale / DrawLayer / DrawType / DrawPreset / DrawQuick / DrawSnap / DrawStraight`；`useOn` 普通模式命中已有图案打开属性界面，否则以点击点为图案左下角、「未创建」状态打开；`use`（右键空气）Shift 打开 [DrawQuickScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/DrawQuickScreen.java)（386×200：类型 / 三个开关 / 16 色 + HEX / 线宽 / 缩放 / 图层 / 4×3 预设），否则取消第一点。快速绘画开启后右键面板不打开属性界面：PRESET 单击生成，矩形 / 线段 / 箭头两次右键取点（按玩家 UUID 记服务端静态 Map，含 busPos + face + u,v），右键非面板或空气取消；吸附整数把点击坐标 `Math.round`，平直线优先在 `|dx| > 2|dy|` 或反向时对齐一轴。
- **拖动**：多功能工具拖动支持 `KIND_SHAPE`，图案以 `u1,v1` 为锚点、吸附整像素、发 `OP_MOVE` 绝对锚点。

#### 41.9 斜面控制台（controller_slope）

- **方块**（[ConsoleSlopeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeBlock.java)）：只有上表面（斜面）是工作面；`FACING` 为水平四向的「下坡方向」，放置取玩家水平朝向反方向。空手右键转交 `ConsoleCompUse.tryUse`；`onPlace / playerWillDestroy / onRemove` 让相邻控制柜或已登记总线 `invalidateStructure()`，放置 / 拆除后需重新「检测并成型」。另有 `controller_slope_blue / controller_slope_black` 复用同类（带 `color` 字段）。
- **方块实体**（[ConsoleSlopeBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeBlockEntity.java)）：只存 `Bus / Host / HostFace` 三项链接信息，`setLink` 有变化时 `setChanged + sendBlockUpdated`；组件与文字仍留在总线。
- **几何映射**（[ConsoleSlopeGeometry.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSlopeGeometry.java)）：坡板模型 `from [0,7,0] to [16,9,17.3]`、`rotation -22.5° axis x origin [0,9,0]`，坡长 17.3px、宽 16px，`SIN 0.38268 / COS 0.92388`，`COVERED_START_PX 1.3`、`COVERED_MID_PX 9.3`。映射取 `U = 宿主面 u 方向`、`V = 上坡方向`、`N = U×V`，宿主 v 上端对齐坡面高端，`v ∈ [中心-8, 中心+8] ↔ t ∈ [1.3, 17.3]`；低边中心取「方块中心沿 FACING 外移半格、高度固定 `LOW_EDGE_Y_PX`」。竖直面要求 `facing == hostFace`；顶面要求 `facing` 与总线 FACING 同轴（与总线背面一致时 U 保持原样、与正面一致时 U 取反 = 内容旋转 180°），与总线前后垂直的朝向明确不映射。`resolve(level, clickedPos, eyePos, hitLocation)` 用「眼睛 → 命中点」射线与坡面平面求交（交点须在视线段内且从外侧射入），再反算宿主 `(u, v)`、用 `inCell` 限制在 16×16 内，返回 `Target(busPos, face, u, v)`。
- **成型登记**：总线静态内部类 `SlopeLink{slopePos, hostPos, hostFace}`（NBT `SlopeLinks`，随 `getUpdateTag` 同步）；`formStructure` 成功后 `applySlopeLinks()` 遍历结构外接长方体内每个 `ConsoleBlock` 的六向邻居（跳过 DOWN），命中朝向匹配的斜面就登记并写入斜面方块实体；重新成型前与 `invalidateStructure()` 时 `clearSlopeLinks()`。`applySlopeLinks` 先比较斜面 `color` 与结构颜色，异色不登记。
- **渲染**：斜面 BER 从自己方块实体取总线与宿主信息，筛出目标格内的组件 / 文字 / 绘图后调用 `ConsoleBusBER.renderMapped(...)`；`ConsoleFrame` 把斜面 origin 平移回宿主表面像素 (0,0)，`ConsoleDrawRenderer.renderInFrame` 按 frame 三轴构造矩阵，因此 `(u,v)` 数据无需迁移，总线 AABB 不扩大。
- **交互**：[ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java)、[ConsoleCompItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ConsoleCompItem.java)、[DrawToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DrawToolItem.java)、[ConsoleKeyItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ConsoleKeyItem.java)、[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java)、[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java)、[ConsoleTextClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleTextClient.java)、[ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java) 全部先调用 `ConsoleSlopeGeometry.resolve`（传玩家眼睛位置 + 命中点）：非空直接用宿主 `face + (u,v)`，为空且点是斜面块则提示「未成型」，否则走原 `findBus + pixelU/V + contains`。服务端包格式不变；拖动额外记录 `slopePos`，每 tick 要求准星仍停在同一斜面并按当前视线重新求交。
- **模型与碰撞**：`controller_slope` 贴图多余面为透明（alpha 0 / 255），方块加 `noOcclusion()` 并在客户端注册 `RenderType.cutout()`，否则默认 solid 会把 alpha=0 写成黑色遮挡斜板；`getShape / getCollisionShape` 按 FACING 缓存四个水平台阶形状（8 级、每级 2 像素进深、每级顶面取该级中点的坡面高度，误差 < 0.42 像素，低端朝 FACING），顶面再下沉 `TOP_OFFSET_PX = 1` 像素；模型根部 `"ambientocclusion": false`，item 模型 `display` 为 gui `[30,225,0]` 缩放 0.625、firstperson 0.32、thirdperson 0.35、ground 0.25、fixed 0.5。

#### 41.10 辅助计算机与界面清单

- **辅助计算机**（[ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java)）：`controller_aux` 装饰性完整方块，无方块实体，`HORIZONTAL_FACING`（默认 NORTH，放置取玩家水平朝向反方向，blockstate 四向变体，模型默认正面 north）。右键扫描六邻：恰好一个 `controller_bus` 时打开程序界面，没有或多于一个用动作栏提示 `message.cc_rc.controller.need_bus / multi_bus`；仍无方块实体，数据全部在总线。朝向只影响模型显示，旧存档缺 `facing` 自动取 NORTH。
- **界面清单**（菜单类型 / 尺寸 / 包）：

| 界面 | 类 | 尺寸 | 网络 |
| --- | --- | --- | --- |
| 总线界面 | [ConsoleScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleScreen.java) | 高 172（含保存 / 加载配置按钮） | `ConsoleStructurePacket` id 7 |
| 配置保存 / 加载 | [ConsoleConfigScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleConfigScreen.java) | 由总线界面进入 | id 18 / 19 |
| 程序界面（变量表 + 程序行） | [ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) / `console_program` | 416×260 | id 15 / 16 |
| 表面文字编辑 | [ConsoleTextScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleTextScreen.java) / `console_text` | 192×234（`console_text_gui.png`） | id 8 / 9、14 |
| 组件设置 | [ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) / `console_comp` | 176×152，数字显示字段 176×174，RGB / 附加变量 176×212 | id 10 / 11、12 / 13 |
| 画图属性 | [ConsoleDrawScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleDrawScreen.java) / `console_draw` | 386×220 | id 21 / 22 / 23 |
| 画图工具快速设置 | [DrawQuickScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/DrawQuickScreen.java) | 386×200（普通 `Screen`） | id 23 |
| 数字键盘输入 | [ConsoleNumpadScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleNumpadScreen.java) | 200×104（无容器菜单） | id 24 |

- 程序界面、组件界面、文字界面、画图界面均以总线为数据源：菜单只同步坐标 / 少量参数，实体数据靠 `sendBlockUpdated` 每帧读取。

#### 41.11 关键数据格式（NBT / 网络包 / 配置 JSON）

- 总线 NBT：`FormState / Error / Origin / SizeX~Z / Count / Color / LastOrigin~LastSizeZ / Texts / NextTextId / Draws / NextDrawId / Comps / NextCompId / SlopeLinks / ProgramPeriod / Vars / VarF / VarB / ProgLines / ProgWarn`。
- 组件 NBT：`Type`（字符串 id）/ `Face / U / V / Code / Code2 / Color / Decimals / Unit / HoldTicks / Scale / State / Value / History / CompText* / SpeedVar / LampVar / MuteVar / HasKey / KeyMode / CoverTicks / Links / RgbR / RgbG / RgbB / ShowOriginal / Muted / PressButton / PressUntil`——仅非默认值才写，旧存档缺失走默认值。
- 表面文字 NBT：`Texts`（ListTag，每项含 `Id / Face / U / V / Text / Color / Bold / Italic / Underline / Strike / AnchorH / AnchorV / Scale`）+ `NextTextId`。
- 斜面方块实体 NBT：`Bus / Host / HostFace`（仅链接信息，组件与文字仍留在总线里）。
- 程序行 NBT：外层 `Idx`，行内 `On / Cond / CondSlot / Target / Expr / Comment / Src / CondExpr / CondSrc`；字节码不持久化，读档后 `programDirty = true` 重新编译。
- 网络包（`ModNetwork` id）：7 结构操作、8 文字请求、9 文字提交、10 组件请求、11 组件提交、12 组件文字请求、13 组件文字提交、14 文字移动、15 变量表、16 程序行、17 键盘释放、18/19 配置上传 / 结果、21 绘图、22 绘图创建回执、23 画图工具设置、24 数字键盘、25 涂鸦文字、26 涂鸦形状、27 便签。
- 配置 JSON：`format / version / size / facing / components / texts / variables / program{period, lines[row, enabled, cond, condition, condition_expr, condition_source, target, expr, comment, source]}`；`condition_expr` 存在时优先于旧三态 `cond / condition`；`format` 缺失即视为非法。

#### 41.12 现状与待办

- 程序系统 P0 ~ P4 已完成（文本表达式、原文回显、`condExpr` 条件表达式）；P5 仅剩手工回归。
- 已知未实现：前向引用提示（方案文档 §4.3 / §16.1）、CC 单变量 `writableByCC` 开关（可选）。
- 系统性审查结论以「网络 / 反序列化健壮性」为主：负行数归零、`Expr.load / read` 深度上限、VM 栈深检查均已落地，不影响现有玩法。

### 42. 涂鸦文字（graffiti）

- **方块**（[GraffitiBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiBlock.java)）：贴墙透明方块，几何与海报一致——`FACING` 四向、16×16×1 轮廓、`noCollission`、支撑墙消失自动掉落、`RenderShape.INVISIBLE`；右键（非潜行）打开涂鸦编辑界面。
- **方块实体**（[GraffitiBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiBlockEntity.java)）：保存 `Text / Color / Scale / Dx / Dy / Bold / Italic / Underline / Strike`；文字单行最长 128 字符，大小 = 一行文字高度（格，0.05~4.0，默认 0.5 = 半格），偏移 ±16 格；`getRenderBoundingBox` 按文字长度与字号动态放大，避免长文本被视锥剔除。
- **渲染**（[GraffitiBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiBER.java)）：复用海报的四向锚点与「观者向右」向量，用 `Matrix4f` 把字体像素映射到墙面（每字体像素 = `scale / font.lineHeight` 格），`font.drawInBatch` 全亮绘制；手持编辑工具（主手 / 副手）时在未偏移锚点绘制 1×1 半透明物品图标（`textures/item/graffiti.png`）标记透明方块位置。
- **放置**：[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java) 的 `useOn` 在控制台 / `ITextDisplay` 分支之后接入涂鸦分支——右键已有涂鸦打开界面；右键任意实心水平墙面（非潜行、目标格可替换）时在墙外侧放置涂鸦方块并立即打开界面。
- **菜单 / 界面 / 网络**：[GraffitiMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/GraffitiMenu.java)（同步文字 / 颜色 / 大小 / 偏移 + 格式位标志）、[GraffitiScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/GraffitiScreen.java)（复用 `console_text_gui.png` 面板：文字输入、完成、B/I/U/S、16 染料色块、dx/dy/大小）、[GraffitiConfigPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/GraffitiConfigPacket.java)（C2S 包 id 25，服务端校验方块实体类型 + 8 格距离后写入并 `sendBlockUpdated`）。
- **注册与资源**：`ModBlocks.GRAFFITI`、`ModItems.GRAFFITI_ITEM`、`ModBlockEntities.GRAFFITI_BE`、`ModMenuTypes.GRAFFITI`、创造物品栏、客户端 cutout 渲染层 / 菜单屏幕 / BER；资源 `blockstates/graffiti.json`、`models/block/graffiti.json`（粒子复用 `block/poster/no_material`）、`models/item/graffiti.json`、`textures/item/graffiti.png`，中英 lang 各一条。
- **注意点**：文字只支持单行（换行符不参与渲染）；放置必须贴着结实的水平墙面，垂直面不放置；透明方块本身不渲染，只有手持编辑工具时才出现幽灵图标。

### 43. 涂鸦形状（graffiti_shape）

- **方块**（[GraffitiShapeBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiShapeBlock.java)）：贴墙透明方块，几何与海报 / 涂鸦文字一致（`FACING` 四向、16×16×1 轮廓、`noCollission`、支撑墙消失自动掉落、`RenderShape.INVISIBLE`）；右键（非潜行）打开图案设置界面。
- **方块实体**（[GraffitiShapeBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiShapeBlockEntity.java)）：保存 `Type / Preset / Color / Size / Width / Rot / Dx / Dy`；类型复用 `ConsoleShapeType`（预设 / 实心矩形 / 空心矩形 / 线段 / 箭头），预设 id 取自 `ConsoleDrawPresets`（24 种）；大小 = 图案边长（格，0.1~16.0，默认 1.0），线宽 0.1~4.0 像素，旋转 0~359.9 度，偏移 ±16 格；`toShape()` 按类型生成以中心为原点的像素坐标图形，`getRenderBoundingBox` 按大小与偏移放大。
- **渲染**（[GraffitiShapeBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/graffiti/GraffitiShapeBER.java)）：锚点取贴墙面中心（如 north 为 `(0.5, 0.5, 15/16)`），像素坐标系 1 像素 = 1/16 格，dx/dy 沿观者向右 / 向上偏移，rotation 在像素平面内绕中心旋转（对所有类型生效）；绘制直接调用 [ConsoleDrawRenderer.renderShape](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleDrawRenderer.java)（公开入口，复用预设染色与矩形 / 线段 / 箭头顶点逻辑）；手持画图工具（主手 / 副手）时在方块本体面绘制半透明物品图标（`textures/item/graffiti_shape.png`）。
- **放置**：[DrawToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DrawToolItem.java) 的 `useOn` 在非控制台分支接入涂鸦形状分支——右键任意实心水平墙面（非潜行、目标格可替换）时在墙外侧放置，用工具当前设置（类型 / 预设 / 颜色 / 缩放 / 线宽）初始化后立即打开设置界面；其它情况仍 PASS。
- **菜单 / 界面 / 网络**：[GraffitiShapeMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/GraffitiShapeMenu.java)、[GraffitiShapeScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/GraffitiShapeScreen.java)（复用 `console_text_gui.png` 面板：类型切换按钮、8×3 预设缩略图网格、16 染料色块、dx/dy/大小/线宽/旋转输入框、完成）、[GraffitiShapeConfigPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/GraffitiShapeConfigPacket.java)（C2S 包 id 26，服务端校验方块实体类型 + 8 格距离后写入并 `sendBlockUpdated`）。
- **注册与资源**：`ModBlocks.GRAFFITI_SHAPE`、`ModItems.GRAFFITI_SHAPE_ITEM`、`ModBlockEntities.GRAFFITI_SHAPE_BE`、`ModMenuTypes.GRAFFITI_SHAPE`、创造物品栏、客户端 cutout 渲染层 / 菜单屏幕 / BER；资源 `blockstates/graffiti_shape.json`、`models/block/graffiti_shape.json`（粒子复用 `block/poster/no_material`）、`models/item/graffiti_shape.json`、`textures/item/graffiti_shape.png`，中英 lang 各一条。
- **注意点**：类型按钮必须按枚举值 `switch` 映射（按下标查数组会与 `RECT / RECT_OUTLINE / LINE / ARROW / PRESET` 错位一位）；在类型按钮上滚动滚轮可循环切换（上滚向后、下滚向前），左键点击为向后切换；预设列表一次显示 24 个（8 列 × 3 行）。

### 44. 便签（note）

- **实体**（[NoteEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/NoteEntity.java)）：自定义非生物实体（`extends Entity`），朝向与四行文字全部放在 `SynchedEntityData`（`DIRECTION` + `COMPOUND_TAG`，文字键 `L0~L3 / C0~C3 / F0~F3`，每行最长 48 字符）随实体自动同步；`makeBoundingBox` 按朝向轴生成 0.5×0.5×0.06 格的薄片（`direction` 为空时兜底 `Direction.NORTH`），纸面外翻后包围盒沿法线改为「靠墙 0.03 + 外翻 0.12」的非对称薄盒以便点选；`isPickable()` 返回 true；右键 `interact`（Shift 取下写物品 NBT，否则打开编辑菜单）、左键 `hurt`（创造模式直接移除，其余 `spawnAtLocation` 掉物品）、`tick` 检查支撑面仍 `isFaceSturdy` 否则自动掉落。
- **物品**（[NoteItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/NoteItem.java)）：`useOn` 校验目标面结实后，用 `getClickLocation` 的命中点 + 面法线半厚生成实体，贴面内两个切向坐标夹到 `[方块+0.25, 方块+0.75]`，因此可贴在方块表面任意浮点位置而不限于格心；先 `setDirection` 再 `setPos` 保证包围盒方向正确；读取物品 NBT `Note` 还原文字；生存 / 冒险 `shrink(1)`，创造不消耗；放置后立即打开编辑界面。
- **冒险模式放置**：Forge 在 `ItemStack.useOn` 内部先调 `ForgeHooks.onPlaceItemIntoWorld`，`mayBuild = false` 时直接返回 PASS，`useOn` 不会被调用；因此放置逻辑由公开方法 `placeNote(level, player, stack, hit, clickedPos, face)` 承担，内部类 `AdventurePlaceHandler`（FORGE 事件总线，监听 `PlayerInteractEvent.RightClickBlock`）只在服务端、非旁观且 `!mayBuild` 时直接调用 `placeNote` 并取消事件；带界面的方块（`getMenuProvider != null`，如箱子、熔炉）仍优先打开界面。
- **已书写的便签**：注册 `written_note`（复用 `NoteItem`，贴图 `textures/item/written_note.png` + 模型 + 中英 lang + 创造栏）；`hasText()` 判断任意一行是否有字，`createNoteStack()` 据此返回 `WRITTEN_NOTE` 或 `NOTE`，实现「带文字取下 → 已书写便签，清空后取下 → 空白便签」。
- **渲染**（[NoteRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/NoteRenderer.java)）：`EntityRenderer` 直绘 0.5 格纸面四边形（`RenderType.entityCutoutNoCull`，贴图 `textures/entity/note.png`），再按六向各自的 `right / upTilt / normalTilt` 基向量用 `Matrix4f` 把字体像素映射到纸面绘制四行文字（字形高约为行高的 80%，长文本按纸面宽度等比缩小），全亮光照。纸面绕上边沿（right 轴）外翻 14°：`upTilt = up·cos − normal·sin`、`normalTilt = up·sin + normal·cos`，文字沿 `normalTilt` 外推 0.01 格、字体 x 沿 `right`、字体 y 沿 `-upTilt`，纸面与文字共用同一组基向量保证倾斜一致、文字位于纸面正面；行距为纸面高度的 82%、字形高为行距的 82%，顶 / 底行不超出纸面。
- **菜单 / 界面 / 网络**：[NoteMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/NoteMenu.java)（实体 id + 四行文字标签）、[NoteScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/NoteScreen.java)（复用 `console_text_gui.png` 面板：四个输入框 + 完成 + B/I/U/S + 16 染料色块，颜色与格式只作用于当前选中行；背景自绘米黄 `F7EF97` + 描边，选中行描边加深）、[NoteConfigPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/NoteConfigPacket.java)（C2S 包 id 27，服务端校验实体类型 + 8 格距离后写入）。
- **注册与资源**：`ModEntities.NOTE`（`MobCategory.MISC`，0.5×0.5，跟踪范围 8、更新间隔 4）、`ModItems.NOTE`、`ModMenuTypes.NOTE`、创造物品栏、`MenuScreens` 与实体渲染器；资源 `models/item/note.json`、`textures/item/note.png`、`textures/entity/note.png`，中英 lang 各两条（`item.cc_rc.note` / `entity.cc_rc.note`）。
- **注意点**：便签文字框使用自绘无阴影 [ShadowlessTextField.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ShadowlessTextField.java)（无阴影、垂直居中、左侧内缩 4px、scissor 裁剪 + 横向滚动、光标按真实时间 500ms 闪烁、点击定位、退格 / 删除 / 左右 / Home / End、`SharedConstants.isAllowedChatCharacter` 过滤）；「正在编辑第 X 行」位于标题栏右侧以避免与按钮重叠；纸面文字不能画在背面（用 `right / upTilt / normalTilt` 显式基向量，text 沿 `normalTilt` 外推 0.01，字体 y 沿 `-upTilt`）；`direction` 为空会 NPE，`makeBoundingBox` 兜底 `Direction.NORTH`；生存 / 冒险消耗 1 个、创造不消耗；空文字取下得到空白便签。

### 45. 激光笔（laser_pointer）

- **物品**：`ModItems.LASER_POINTER`，普通 `Item`、`stacksTo(1)`、无右键逻辑（激光是「手持即生效」的被动效果）。贴图取 `模型/其他物品/激光笔.png`（16×16）直接复制为 `textures/item/laser_pointer.png`；模型 `models/item/laser_pointer.json` 用 `item/handheld`（原版工具手持姿态，保持原版大小）；中英 lang 各一条 + 创造物品栏。无方块 / 方块实体 / 网络包。
- **配置**（[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)）：`laser_pointer` 段——`max_distance`（默认 16，范围 1~64）、`line_width`（默认 0.02 格，范围 0.005~0.2），配 `getLaserPointerMaxDistance()` / `getLaserPointerLineWidth()`。
- **渲染**（[LaserPointerRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/LaserPointerRenderer.java)）：`@EventBusSubscriber(value = Dist.CLIENT, bus = FORGE)` 监听 `RenderLevelStageEvent`，只在 `Stage.AFTER_TRANSLUCENT_BLOCKS` 执行；遍历 `level.players()`，主手或副手持激光笔的玩家各画一条红线（双手各持一支画两条），其他玩家同样可见，全部客户端本地计算，不需要同步包。
- **射线求交**：以 `player.getEyePosition(partialTick)` 为起点、`getViewVector(partialTick)` 为单位方向，最远 `max_distance` 格，用 `level.clip(new ClipContext(..., ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))` 按**碰撞箱**求交；命中则终点取 `hit.getLocation()`（碰撞箱表面交点），未命中则取眼睛 + 视线 × 最大距离，即激光与视线平行。
- **手部起点**：用视线正交基 `right = 视线 × 世界向上`、`up = right × 视线` 构造，起点 = 眼睛 + 视线前 0.30 格 + 左右 0.30 格 − 上 0.62 格（站立时眼睛约 1.62 格高、持物手约 1.0 格高，减去 0.62 后起点落在手部）；右手玩家主手在右、副手在左，`HumanoidArm.LEFT` 玩家整体镜像。
- **绘制方式**：无贴图 `RenderType.debugQuads()`（POSITION_COLOR，红色不透明），沿光束轴取 3 个互成 60° 的方向各画一条细长四边形（半宽 = 配置线宽 / 2），顶点统一减去相机位置；深度测试保持开启，激光会被墙体正确遮挡。不使用单片朝向相机的四边形，因为第一人称时光束几乎与视线重合，单片会退化到看不见。
- **专用服务器兼容性**：物品注册（`ModItems` / `ModCreativeTabs`）、配置（`Config`）全部是公共代码，无任何 `net.minecraft.client` 引用；渲染类 `LaserPointerRenderer` 标注 `value = Dist.CLIENT`，与模组内其他客户端处理器一致，服务端不会加载该类；激光全靠各客户端本地依同步来的玩家位置 / 朝向 / 手持物品计算，无网络包与服务端逻辑。`max_distance` / `line_width` 是 COMMON 配置，多人游戏时每个客户端读自己安装目录的配置值（纯视觉参数，不影响服务端判定）。
- **注意点**：`ClipContext.Block` 中按碰撞箱求交的枚举值是 `COLLIDER`（不是 `COLLISION`）；光束全程按手部位置起算，因此第三人称下同样从手部指向准星交点。

### 46. 音频播放器（audio_player）

- **方块**（[AudioPlayerBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/audio_player/AudioPlayerBlock.java)）：`Block implements EntityBlock`，唯一方块状态 `POWERED`；`neighborChanged` / `onPlace` 在服务端用 `getBestNeighborSignal` 刷新并 `setBlock(..., 3)` 同步给客户端；`canConnectRedstone` 恒 true；右键（非 Shift）服务端 `AudioPlayerMenu.open` 打开设置界面。
- **方块实体**（[AudioPlayerBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/audio_player/AudioPlayerBlockEntity.java)）：保存音效 id / 模式 / 范围 / 衰减 / 起始 / 截至 / 间隔 / 音量（NBT 键 `Sound / Mode / Range / Atten / Start / Stop / Interval / Volume`）；`record Settings` 作为菜单同步与客户端「配置是否变化」的比较快照；`normalizeSoundId` 缺命名空间补 `cc_rc:`、超长截断 128；`setConfig` 统一 `Mth.clamp`；`getUpdateTag` = `saveWithoutMetadata`、`getUpdatePacket` = `ClientboundBlockEntityDataPacket.create`。
- **菜单 / 界面**：[AudioPlayerMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/AudioPlayerMenu.java) 仅承载坐标与配置快照，`NetworkHooks.openScreen` 按 `pos → writeUtf(128) → VarInt → Float → VarInt → Float×3 → VarInt(音量)` 顺序写同步数据；[AudioPlayerScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/AudioPlayerScreen.java) 220×224 白底自绘，含音效名称补全框（候选 `ForgeRegistries.SOUND_EVENTS.getKeys()` 排序，前缀匹配、无结果退回包含，5 行滚轮 + 点击选中，配色同 `ConsoleProgramScreen`）、时长文本、模式循环按钮、范围 / 衰减 / 起始 / 截至 / 间隔 / 音量输入框与保存按钮；保存前校验 id 已注册，通过后发 `AudioPlayerConfigPacket`；鼠标悬浮补全候选时显示该音效的中文翻译（语言键 `cc_rc.sound.<id>`，未命中退回 `cc_rc.subtitle.<path>`，都没有则不显示）；下拉展开时，凡矩形与下拉面板相交的模式按钮 / 输入框 / 标签一律跳过渲染（`isCoveredBySuggest`），避免文字从下拉面板上透出。
- **时长解析**（[SoundDurations.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/SoundDurations.java)）：`SoundManager.getSoundEvent` → `getSound` → `Sound.getLocation` 定位 `sounds/<path>.ogg`，扫描 `01 vorbis` 取采样率（+12，小端 4 字节）、尾部 `OggS` 取 granule（+6，小端 8 字节），`ticks = round(granule / rate * 20)`；按音效事件 id 缓存（失败 -1，调度按 200 tick 兜底）。
- **客户端调度**（[AudioPlayerSoundHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/AudioPlayerSoundHandler.java)）：`Dist.CLIENT` + FORGE 总线，`ClientTickEvent.Phase.END`；每 20 tick 扫描玩家 ±5 区块内 `LevelChunk.getBlockEntities()` 登记 `Track`；每 tick 按配置与 `POWERED` 驱动：触发 = 有红石、反触发 = 无红石、脉冲 = 上升沿播一次（`pulsePending`）、无红石 = 忽略红石常开；配置变化（`Settings.equals`）停旧实例并按新配置重排；`spawn` 受每方块 64 实例上限约束，`nextStartTick = now + max(1, 播放长度 + 间隔)`，负间隔产生重叠；BE 消失 / 距玩家 > 80 格 / 切换维度时停止并清空（含所有实例）。
- **播放实例**（内部 `AudioPlayerSoundInstance extends AbstractTickableSoundInstance`）：`relative=false` + `Attenuation.NONE` + `SoundSource.BLOCKS` + 固定方块中心；用 `looping=true` 保证引擎每 tick 回调 `tick()`（`AbstractSoundInstance` 没有 `isStopped()`），`tick()` 每 tick 按 `clamp(1 - dist/range * 衰减%/100, 0, 1) × 本方音量倍数` 更新音量，`elapsed >= maxTicks` 或超出范围时 `stopNow()`（公开包装 protected `stop()`）。
- **网络包**（[AudioPlayerConfigPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/AudioPlayerConfigPacket.java)）：id 28；服务端校验方块实体类型、8 格距离、音效 id 已注册后 `setConfig` + `sendBlockUpdated(..., 3)`，附近客户端调度器随即按新配置生效。
- **参数语义**：范围 1~64 格（默认 16）；衰减 0~100%（默认 100，0 = 范围内等音量，100 = 到范围边缘线性降到 0）；起始 0~60 秒（开播延迟）；截至 0~3600 秒（0 = 整首，否则播放长度 = min(截至, 曲长)）；间隔 -60~3600 秒；音量 0~500%（默认 100，可超过 100）。
- **音量超过 100% 的实现**：原版 `SoundEngine` 把单个实例音量夹在 0~1，故 `volume = 百分比/100` 超过 1 时拆成 `ceil(volume)` 份同位置实例（最多 5 份），各份音量 = `min(1, volume - i)`，同时开始、混音时振幅相加（如 250% = 1.0 + 1.0 + 0.5）；0% 不创建实例。
- **注意点**：播放完全由客户端驱动，服务端只存配置与红石状态；衰减百分比字段必须避开父类的 `attenuation` 枚举字段（代码用 `attenuationPercent`）；贴图为 AI 生成 1920×1920 后最近邻缩到 16×16 的 `textures/block/audio_player.png`；`轮询 + 每 tick 手动算音量` 的做法与 F.A.A.S 服务器一致。

### 47. 麦卡锡音频素材（96 条声音）

- **新增内容**：仅注册 `SoundEvent` 与 `sounds.json` 条目，无唱片、无物品；另在 `zh_cn.json` 补 96 个中文名语言键 `cc_rc.sound.<id>`（供音频播放器补全列表的悬浮翻译使用，`sounds.json` 仍无 subtitle）。
- **命名与存放**：采用「英文类别_英文名」（`mechanical_` / `explosion_` / `alarm_` / `running_` / `music_`），音频放在 `assets/cc_rc/sounds/mccarthy/<category>/<basename>.ogg`；[ModSounds.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModSounds.java) 新增 96 个显式常量（如 `MECHANICAL_AZ5_TRIGGER`），统一走原有 `register(name)`，`sounds.json` 同步新增 96 条同名条目（总条目 45 → 141）。
- **压缩**：VLC `--sout=#transcode{vcodec=none,acodec=vorb,ab=64,channels=1,samplerate=44100}`，与唱片同规格；96 个输出均校验通道数为 1。
- **裁剪**：只处理文件名带 `^` 的素材——`^N` 按「总时长 / N」取整数个完整循环（警报类目标约 2s、其它 <5s）；`^连续` 取开头（警报类 1.9s、其它 4.8s）；不带 `^` 的一律不裁（注：`控制室警报4.5` 没有 `^`，按无标记处理）。
- **流式**：`music` 类别的 8 条在 `sounds.json` 中带 `"stream": true`（最长 243s），其余非流式。
- **校验**：`sounds.json` 已用 `ConvertFrom-Json` 解析通过，141 个键无重复；`gradlew build` BUILD SUCCESSFUL。

### 48. 水循环图（water_cycle_chart）

- **背景**：水循环图原为海报的其中一张，为保证清晰度改用 5000×3536 原分辨率贴图，常驻显存约 67MB。改为独立物品后，墙面只显示 512×362 低清图，高清图仅在打开查看界面时加载、关闭即释放，闲置时不占大显存（代价是每次打开解码一次大 PNG，有短暂卡顿）。
- **贴图拆分**：低清 `textures/block/poster/water_cycle_diagram.png`（512×362，墙面模型用）、高清 `textures/gui/water_cycle_diagram_hd.png`（5000×3536，1,643,603 字节，查看界面用）、物品图标 `textures/item/water_cycle_chart.png`（128×128，内容 128×90 等比居中）。
- **方块**（[WaterCycleChartBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/WaterCycleChartBlock.java)）：继承 `PosterBlock`（复用贴墙朝向 / 四向放置 / 碰撞箱为无碰撞），但 `newBlockEntity` 返回 null、`getRenderShape` 返回 `MODEL`（海报默认 INVISIBLE 是留给 BER 的）；右键在客户端经 `DistExecutor.unsafeRunWhenOn` 调 `WaterCycleChartClient.open()` 打开界面，服务端不存任何状态、无网络包。
- **界面**（[WaterCycleChartScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/WaterCycleChartScreen.java)）：`init()` 里用 `Minecraft.getInstance().getTextureManager().getTexture(HD_TEXTURE)` 加载高清图并 `setFilter(true, false)`（线性过滤、不开 mipmap）；`render` 用 `renderBackground` + 按窗口**高度**等比 `blit` 整张 5000×3536——图片上下贴满窗口边缘、水平居中（窗口过窄时左右裁掉），标题加深色半透明底画在左上；「完成」按钮缩为 44×14 放右上角（`CommonComponents.GUI_DONE`），尽量减少遮挡；`removed()` 里 `getTextureManager().release(HD_TEXTURE)` 释放显存，下次打开重新加载；`isPauseScreen()` 返回 false。
- **方块模型**（`assets/cc_rc/models/block/water_cycle_chart.json`）：`minecraft:block/block`，单元素 `from [0,-6.625,0.5] to [32,16,1]`——32 模型像素 = 固定 2 格宽，高 22.625 模型像素 ≈ 1.414 格，符合 512:362；只有朝南的 `"south"` 面（canonical 朝南，其余朝向由 blockstate 绕 Y 旋转），`uv [0,0,16,16]`（模型 UV 的 16 即整个 sprite，自动适配非方形贴图），`ambientocclusion:false`，不写 cullface 保证任何角度看都渲染。
- **blockstate**（`assets/cc_rc/blockstates/water_cycle_chart.json`）：`south=0 / west=90 / north=180 / east=270`（按 y=90 映射 north→east 推导，与海报一致）；`models/item/water_cycle_chart.json` 为 `item/generated`；`loot_tables/blocks/water_cycle_chart.json` 自掉落 + `survives_explosion`。
- **注册**：[ModBlocks.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlocks.java) `noCollission().strength(0.5F).sound(Wood)`；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) `WATER_CYCLE_CHART_ITEM`；[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 海报后追加；[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 客户端 `setRenderLayer(..., cutout())`；lang 中英各两条（`block.cc_rc.water_cycle_chart` / `gui.cc_rc.water_cycle_chart.title`）。
- **海报兼容**：[PosterImages.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterImages.java) 把 `water_cycle_diagram` 回退为 512×362，并加入 `HIDDEN_IDS`；旧存档里索引 45 的海报仍能按原索引渲染（不会因删除条目让后面的索引整体前移、把已放置的海报换成别的图），选择界面则通过 `visibleCount()` / `visibleIndexAt(pos)` 跳过隐藏项，不再提供这张海报。列表仍为 47 条，可选 46 张。
- **注意点**：`TextureManager.release` 与 `setFilter` 在 1.20.1 均可用；`release` 只丢弃本 mod 主动加载的这份纹理，不影响原版缓存策略；模型宽度固定在 JSON 里，不再有海报那套缩放 / 偏移设置。

## 三、注册物品

> 约定：方块与物品 ID 一一对应；`BlockItem` 为普通方块物品，特殊物品使用专属类。以下按方块类归组。

### 1. 方块（Blocks）——共 134 个

| 方块 ID | 方块类 | 说明 |
| --- | --- | --- |
| `ccrc_block` | `Block` | 基础装饰方块（石质） |
| `console_lever_1` | `ConsoleLeverBlock` | 控制台拉杆（基础色），继承原版杠杆 |
| `console_lever_2` | `ConsoleLeverBlock` | 控制台拉杆（备用色） |
| `console_lever_6` | `ConsoleLever3StageBlock` | 3 挡位拉杆（0/8/15 输出） |
| `console_lever_7` | `ConsoleLever3StageBlock` | 3 挡位拉杆（同 6） |
| `point_lamp_1` | `PointLampBlock` | 指示灯 1（红石电平点亮） |
| `point_lamp_2` | `PointLampBlock` | 指示灯 2 |
| `point_lamp_3` | `PointLampBlock` | 指示灯 3 |
| `meter` | `MeterBlock` | 仪表（红石强度 0~15，16 种模型） |
| `console_panel` | `ConsolePanelBlock` | 空控制面板（仅文字显示） |
| `console_panel_large` | `ConsolePanelLargeBlock` | 大号空控制面板（16宽×14高×2厚，3倍字号居中文字，碰撞箱随朝向旋转） |
| `console_button_1` | `ConsoleButtonBlock` | 控制台按钮（按下输出 15，20tick 弹起） |
| `console_button_2` | `ConsoleButtonBlock` | 控制台按钮 2 |
| `console_button_3` | `ConsoleButtonBlock` | 控制台按钮 3 |
| `console_button_4` | `ConsoleButtonBlock` | 控制台按钮 4（模型"模型/特殊控制台/A"，on/off 两态） |
| `console_button_5` | `ConsoleButtonBlock` | 控制台按钮 5（模型"模型/特殊控制台/C"，on/off 两态） |
| `password_inputer` | `PasswordInputerBlock` | 密码输入器（放置/碰撞箱同控制面板；on/off 两态，破解成功向后方强充能15，1秒后自动关；模型"模型/特殊控制台/密码输入器"） |
| `nuke_button` | `NukeButtonBlock` | 核弹按钮（放置/碰撞箱同控制面板；6 状态输出 0/5/7/10/10/15，钥匙推进状态，发射态 3 秒自动复位；模型"模型/特殊控制台/B"6 状态，保持原尺寸仅 z 平移对齐） |
| `key_cabinet` | `KeyCabinetBlock` | 钥匙柜（水平四向放置，无方块实体；多功能工具右键记录坐标朝向；模型"模型/特殊控制台/钥匙柜/钥匙柜.json"） |
| `key_distributor` | `KeyDistributorBlock` | 钥匙分发控制器（六面 key_sender 贴图完整方块，方块实体存钥匙柜记录；≤5 信号校验清理、>5 随机分发钥匙1/2 高亮不消失掉落物） |
| `safe_button_1` | `SafeButtonBlock` | 安全按钮（三阶段授权） |
| `plotter` | `PlotterBlock` | 圆盘记录仪（24 点趋势划线） |
| `plotter_clock` | `PlotterClockBlock` | 圆盘记录仪时钟（上升沿触发划线，完整方块） |
| `breaker` | `BreakerBlock` | 断路器（完整方块，on 向后方/上方输出 15） |
| `card_reader_a` | `CardReaderBlock` | 刷卡机 A 级 |
| `card_reader_b` | `CardReaderBlock` | 刷卡机 B 级 |
| `card_reader_c` | `CardReaderBlock` | 刷卡机 C 级 |
| `card_reader_d` | `CardReaderBlock` | 刷卡机 D 级 |
| `card_reader_e` | `CardReaderBlock` | 刷卡机 E 级 |
| `digital_display` | `DigitalDisplayBlock` | 数码显示器（完整方块，双行文字 + CC 外设） |
| `digital_knob` | `DigitalKnobBlock` | 数字调节器（完整方块，四按钮 + CC 外设） |
| `digital_plotter` | `DigitalPlotterBlock` | 数字圆盘记录仪（完整方块，50点0~100线图 + CC 外设，无状态） |
| `data_unit` | `DataUnitBlock` | 数据单元（无方向完整方块，原版书架结构模型 + 专属 top/side 贴图；方块实体存名称+空数据列表，CC 外设读写） |
| `block_detector` | `BlockDetectorBlock` | 方块探测器（完整方块，六方向放置，原版观察者结构模型 + 专属 top/side/front/back 贴图；CC 外设只读探测面向方块信息） |
| `poster` | `PosterBlock` | 海报（**单一方块 + 方块实体**：右键 GUI 选图片/缩放/dy/dz，`PosterBER` 直绘四边形，尺寸不受模型 ±16/32 限制；旧 poster_1~26 方块已全部删除） |
| `graffiti` | `GraffitiBlock` | 涂鸦文字（贴墙透明方块，几何/碰撞箱同海报；手持编辑工具右键实心墙面放置并立即编辑，右键已有涂鸦也可编辑；`GraffitiBER` 在墙面直绘文字，手持编辑工具时显示幽灵图标） |
| `controller_block` | `ConsoleBlock` | 多方块控制柜结构方块（无方向完整立方体，锁定后不可破坏；方块实体只存所属总线与结构状态） |
| `controller_block_blue` | `ConsoleBlock` | 蓝色控制柜结构方块（功能与原控制柜完全一致，仅同色方块之间参与同一结构成型；异色可相邻但不相连） |
| `controller_block_black` | `ConsoleBlock` | 黑色控制柜结构方块（同上，颜色为黑色） |
| `controller_bus` | `ConsoleBusBlock` | 多方块控制柜总线（**水平四向** FACING 面板，朝向同时决定顶/底面文字的头朝向；新增 COLOR 状态：成型后按识别到的结构颜色切换贴图；右键打开结构界面：校验/成型/锁定/解锁；方块实体保存结构元数据 + 表面文字列表并低频巡检） |
| `controller_aux` | `ControllerAuxBlock` | 控制柜辅助计算机（装饰性完整方块，无方块实体；COLOR 状态跟随相邻总线 / 结构方块颜色；右键扫描六邻找唯一总线并打开程序界面：左 1/3 变量表、右 2/3 程序区） |
| `nai_long_toy` | `NaiLongToyBlock` | 奶龙玩偶（半高装饰，水平四方向放置，底面8x8居中高14像素，右键播放声音 nai_long，放置/破坏音效同羊毛） |
| `potato_crate` | `Block` | 箱装土豆（搬运自农夫乐事，无方向完整方块，木板材质 `MapColor.WOOD` + `SoundType.WOOD`，强度 2.0，仅装饰展示） |
| `fridge` | `FridgeBlock` | 冰箱（搬运自 Cooking for Blockheads，水平四方向完整方块，27 格容器 + 原版箱子 GUI，金属音效强度 5.0/10） |
| `sink` | `SinkBlock` | 水槽（搬运自 Cooking for Blockheads，水平四方向完整装饰方块，无方块实体，石头音效强度 5.0/10） |
| `redstone_sender` | `RedstoneSenderBlock` | 红石信号发射器（六方向完整方块，连接红石线；收到外部信号时沿面向方向2~配置距离扫描接收器并同步强度，POWER>0 显示 on 贴图，金属音效） |
| `redstone_receiver` | `RedstoneReceiverBlock` | 红石信号接收器（无方向完整方块，连接红石线；isSignalSource 向相邻方块输出 POWER 强度信号，POWER>0 显示 on 贴图，金属音效） |
| `server_faas_1` | `ServerFaasBlock` | F.A.A.S服务器 1（水平四方向放置朝向玩家，贴近持续播放 server_noise，完整方块金属音效） |
| `server_faas_2` | `ServerFaasBlock` | F.A.A.S服务器 2（同上） |
| `server_faas_3` | `ServerFaasBlock` | F.A.A.S服务器 3（同上） |
| `white`/`orange`/`magenta`/`light_blue`/`yellow`/`lime`/`pink`/`gray`/`light_gray`/`cyan`/`purple`/`blue`/`brown`/`green`/`red`/`black`_canvas_sign | `StandingSignBlock` | 各色粗布告示牌（立式，原版告示牌机制，每种颜色独立 WoodType `cc_rc:canvas_<颜色>`，无碰撞箱，强度 1.0，贴图各色专属 `entity/signs/canvas_<颜色>.png`） |
| 同色 `_canvas_wall_sign` | `WallSignBlock` | 各色粗布告示牌（壁挂式，掉落对应立式块） |
| 同色 `_hanging_canvas_sign` | `CeilingHangingSignBlock` | 各色悬挂式粗布告示牌（天花板悬挂） |
| 同色 `_canvas_wall_hanging_sign` | `WallHangingSignBlock` | 各色悬挂式粗布告示牌（壁挂悬挂，掉落对应天花板悬挂块） |

> 说明：16 色 × 4 类 = 64 个告示牌方块（沿用原版 SignBlock 系列，方块实体复用原版 `BlockEntityType.SIGN` / `HANGING_SIGN`，无需新增方块实体）；`console_lever_1/2` 共用 `ConsoleLeverBlock` 类，`console_lever_6/7` 共用 `ConsoleLever3StageBlock` 类，`point_lamp_1/2/3` 共用 `PointLampBlock` 类，`console_button_1~5` 共用 `ConsoleButtonBlock` 类，`card_reader_a~e` 共用 `CardReaderBlock`（构造参数 grade 'A'~'E'），`server_faas_1/2/3` 共用 `ServerFaasBlock`（仅模型/贴图不同）。

### 2. 物品（Items）——共 172 个

**方块物品（100 个，`BlockItem` / `SignItem`）：**

| 物品 ID | 物品类 | 对应方块 |
| --- | --- | --- |
| `ccrc_block` | `BlockItem` | ccrc_block |
| `console_lever_1` / `console_lever_2` / `console_lever_6` / `console_lever_7` | `BlockItem` | 拉杆 1/2/6/7 |
| `point_lamp_1` / `point_lamp_2` / `point_lamp_3` | `BlockItem` | 指示灯 1/2/3 |
| `meter` | `BlockItem` | 仪表 |
| `empty_console_panel` | `BlockItem` | 空控制面板（对应方块 console_panel） |
| `empty_console_panel_large` | `BlockItem` | 大号空控制面板（对应方块 console_panel_large） |
| `console_button_1` / `console_button_2` / `console_button_3` / `console_button_4` / `console_button_5` | `BlockItem` | 按钮 1/2/3/4/5 |
| `password_inputer` | `BlockItem` | 密码输入器（对应方块 password_inputer） |
| `safe_button_1` | `BlockItem` | 安全按钮 |
| `plotter` | `DescriptionBlockItem` | 圆盘记录仪（悬停显示模式说明） |
| `plotter_clock` | `DescriptionBlockItem` | 圆盘记录仪时钟（悬停显示触发说明） |
| `breaker` | `BlockItem` | 断路器 |
| `card_reader_a` ~ `card_reader_e` | `BlockItem` | 刷卡机 A~E |
| `digital_display` | `BlockItem` | 数码显示器 |
| `digital_knob` | `DescriptionBlockItem` | 数字调节器（悬停显示切换显示模式说明） |
| `digital_plotter` | `BlockItem` | 数字圆盘记录仪 |
| `data_unit` | `BlockItem` | 数据单元（原版书架结构模型 + 专属贴图） |
| `block_detector` | `BlockItem` | 方块探测器（原版观察者结构模型 + 专属贴图） |
| `poster` | `BlockItem` | 海报（单一方块，item 形态 item/generated 平面显示 `textures/item/poster.png`；旧 poster_1~26 物品已全部删除） |
| `graffiti` | `BlockItem` | 涂鸦文字（贴墙透明方块，item 形态 item/generated 平面显示 `textures/item/graffiti.png`） |
| `graffiti_shape` | `BlockItem` | 涂鸦形状（贴墙透明方块，item 形态 item/generated 平面显示 `textures/item/graffiti_shape.png`） |
| `controller_block` | `BlockItem` | 控制柜结构方块 |
| `controller_block_blue` | `BlockItem` | 蓝色控制柜结构方块 |
| `controller_block_black` | `BlockItem` | 黑色控制柜结构方块 |
| `controller_bus` | `BlockItem` | 控制柜总线 |
| `controller_slope` | `BlockItem` | 斜面控制台（gui 30/225/0 缩放 0.625，手持缩小） |
| `controller_slope_blue` | `BlockItem` | 蓝色斜面控制台 |
| `controller_slope_black` | `BlockItem` | 黑色斜面控制台 |
| `controller_aux` | `BlockItem` | 控制柜辅助计算机 |
| `nai_long_toy` | `BlockItem` | 奶龙玩偶 |
| `redstone_sender` | `BlockItem` | 红石信号发射器 |
| `redstone_receiver` | `BlockItem` | 红石信号接收器 |
| `server_faas_1` | `BlockItem` | F.A.A.S服务器 1 |
| `server_faas_2` | `BlockItem` | F.A.A.S服务器 2 |
| `server_faas_3` | `BlockItem` | F.A.A.S服务器 3 |
| `potato_crate` | `DescriptionBlockItem` | 箱装土豆（对应无方向木板材质方块；悬停两行描述：黄斜体"搬运自农夫乐事"＋淡蓝粗体"！？服务器 ？！"；仅入搬运物品栏） |
| `fridge` | `DescriptionBlockItem` | 冰箱（对应 27 格容器方块；黄斜体"搬运自 Cooking for Blockheads"；仅入搬运物品栏） |
| `sink` | `DescriptionBlockItem` | 水槽（对应纯装饰方块；黄斜体"搬运自 Cooking for Blockheads"；仅入搬运物品栏） |
| `white`/`orange`/`magenta`/`light_blue`/`yellow`/`lime`/`pink`/`gray`/`light_gray`/`cyan`/`purple`/`blue`/`brown`/`green`/`red`/`black`_canvas_sign | `DescriptionSignItem` | 各色粗布告示牌（立式/壁挂共用，黄斜体"搬运自农夫乐事"；仅入搬运物品栏） |
| 同色 `_hanging_canvas_sign` | `DescriptionHangingSignItem` | 各色悬挂式粗布告示牌（天花板/壁挂悬挂共用，黄斜体"搬运自农夫乐事"；仅入搬运物品栏） |

> 说明：16 色 × 2 = 32 个告示牌物品（复用 key `item.cc_rc.desc_potato_crate_carried` 显示黄色斜体"搬运自农夫乐事"）。

**特殊物品（31 个）：**

| 物品 ID | 物品类 | 说明 |
| --- | --- | --- |
| `multi_tool` | `MultiToolItem` | 多功能工具（不可堆叠，切换百分比模式 / 圆盘记录仪模式） |
| `instruction_book_1` | `InstructionBookItem` | 说明书1（原版成书 WrittenBookItem 机制，右键打开书籍阅读界面，固定内容：目录 + 控制面板类/编辑工具/圆盘记录仪/断路器/刷卡机/红石信号收发） |
| `instruction_book_2` | `InstructionBook2Item` | 说明书2（原版成书机制，记录 CC 配件外设使用法：数码显示器/数字调节器/数字圆盘记录仪/扩展红石继电器与总线，含每个 Lua 函数名与参数/返回类型） |
| `card_a` ~ `card_e` | `CardItem` | 门禁卡 A~E（不可堆叠，等级 'A'~'E'） |
| `frozen_tilapia` | `DescriptionSwordItem` | 冰冻罗非鱼（旧模组移植，修改器伤害 +255 实际总伤害 256 / 攻速 -2.0 / 耐久 2，蓝冰修复，不可堆叠，Rarity.RARE 蓝色名，金色描述"三体宇宙最强武器（bushi）"） |
| `green_wine` | `DescriptionItem` | 沉船绿酒（旧模组移植，饮用后剧毒 4 + 反胃 3，各 114514 刻，堆叠 16，绿色描述） |
| `green_wine_barrel` | `DescriptionItem` | 沉船绿酒桶（旧模组移植，普通物品，描述"*这是计划的一部分*"） |
| `he_yi_wei` | `DescriptionItem` | 何意味？（旧模组移植，营养 11 / 饱和度 4 / RARE，食用后 10 种效果，堆叠 16，紫色描述） |
| `tastes_food` | `Item` | 合一味（旧模组移植，营养 30 / 饱和度 0.8 / 可随时食用，堆叠 16） |
| `moon_cake` | `Item` | 五仁月饼（营养 8 / 饱和度 4，可随时食用，食用后 200 秒速度 II，堆叠 16） |
| `moon_cake_iron` | `AttackDescriptionItem` | 五金月饼（普通物品、无耐久，主手攻击伤害 10 / 堆叠 16，营养 4 / 饱和度 2，可随时食用，食用后 2 秒凋零 II + 200 秒力量 II 与抗性提升，描述"机加工这一块/."） |
| `ship_biscuit` | `DescriptionItem` | 压缩饼干（营养 12 / 饱和度 6，食用后 60 秒饱和效果 + 20 秒生命恢复，描述"*量大管饱*"） |
| `bao_zi` | `BaoZiItem` | 包子（食物 + 投掷炸弹：营养 4 / 饱和度 2，右键食用；左键投掷 bao_zi 弹射物命中爆炸，仅伤害实体不破坏方块；堆叠 16，红色斜体描述"包子雷？"） |
| `baguette` | `BaguetteItem` | 法棍（食物：营养 2 / 饱和度 2，堆叠 16；主手附加攻击伤害 4（修饰符 3.0 + 空手 1.0）与击退 ATTACK_KNOCKBACK=3.0，粗体棕色描述"坚如磐石"） |
| `crowbar` | `CrowbarItem` | 撬棍（铁质 SwordItem 自带横扫，修改器伤害 +19 实际总伤害 20 / 攻速慢 -3.0 / 暴击 2.0 倍 / 耐久 1024 / 铁锭修复 / 铁砧落地打击音效 / 不可堆叠；浅蓝粗体描述"物理学圣剑"+深蓝"f(x)dx"） |
| `simple_spear` | `SimpleSpearItem` | 简易长矛（铁质 SwordItem 自带横扫，耐久 130 / 修改器伤害 +129 实际总伤害 130 / +13 攻击范围 ForgeMod.ENTITY_REACH / 铁锭修复 / Rarity.EPIC 淡紫名 + isFoil 附魔微光 / 不可堆叠 / 手持模型 2 倍大且 y 前移 3.2；紫色描述"魔女们的秘密武器"） |
| `laser_pointer` | `Item` | 激光笔（不可堆叠，无右键逻辑；手持时客户端 `LaserPointerRenderer` 从手部向「视线与碰撞箱表面的交点」绘制红色激光，最长距离 16 格 / 线宽 0.02 格均可通过 Config 的 `laser_pointer` 段调整，超距无方块时与视线平行；主手、副手各持一支时各画一条；贴图 `模型/其他物品/激光笔.png`，模型 `item/handheld`） |
| `password_cracker` | `PasswordCrackerItem` | 破解器（不可堆叠；手持右键密码输入器开始破解，20 秒可配置，离开 5 格/切快捷栏即中断；**3D 物品模型**，模型来源"模型/特殊控制台/破解卡"；破解时播放 password_crack 音效，中断即停） |
| `key_1` / `key_2` | `Item` | 核弹发射钥匙 1 / 2（不可堆叠；右键核弹按钮消耗并推进状态：状态1+钥匙1→状态2、状态1+钥匙2→状态3、状态2+钥匙2→状态4、状态3+钥匙1→状态4；贴图"特殊控制台/key_1、key_2"） |
| `error_mob_spawn_egg` | `ForgeSpawnEggItem` | 错误生物刷怪蛋（主色红 0xDC2828 / 次色深灰蓝 0x1E1E28，双色点纹贴图；右键生成 `error_mob` 实体，实体不自然生成只能靠刷怪蛋/刷怪笼；用 ForgeSpawnEggItem 惰性解析实体类型避免注册顺序崩溃） |
| `error_mob_null_spawn_egg` | `ForgeSpawnEggItem` | 错误生物（null 变种）刷怪蛋（主色紫灰 0x8C82B4 / 次色深紫褐 0x282338；生成 `error_mob_null`） |
| `error_mob_warn_spawn_egg` | `ForgeSpawnEggItem` | 错误生物（warn 变种）刷怪蛋（主色琥珀黄 0xE6AA3C / 次色深棕褐 0x372D14；生成 `error_mob_warn`） |
| `edit_tool` | `EditToolItem` | 编辑工具（不可堆叠；主手右键可显示名称方块打开文字编辑 GUI，副手放置可显示名称方块自动打开编辑界面；悬停青色粗体"用于编辑可显示名称的方块文字"） |
| `golden_eagle` | `Item` | 金鹰（堆叠 64；盖金蜗牛的食物，手持可吸引（TemptGoal）与繁殖（BreedGoal）；贴图"模型/生物/蜗牛/金鹰.png"） |
| `gajin_spawn_egg` | `ForgeSpawnEggItem` | 盖金蜗牛刷怪蛋（主色金褐 0xD8B24A / 次色深褐 0x5A3A1E；生成 `gajin` 实体，实体不自然生成只能靠刷怪蛋/刷怪笼） |
| `evil_gajin_spawn_egg` | `ForgeSpawnEggItem` | 邪恶盖金刷怪蛋（主色暗红 0x8B0000 / 次色纯黑 0x000000；生成 `evil_gajin` 敌对生物，实体不自然生成只能靠刷怪蛋/刷怪笼；贴图 16x16 素材 `模型/生物/邪恶盖金/evil_gaijin.png`） |
| `draw_tool` | `DrawToolItem` | 画图工具（不可堆叠；右键控制柜表面以点击点为左下角创建图案并打开属性界面，可设类型/颜色/线宽/图层/尺寸/位置或删除；线段与箭头用长度+角度；多功能工具可拖动图案；贴图 `模型/其他物品/画图工具.png`） |
| `note` | `NoteItem` | 便签（贴墙实体物品；右键任意结实方块面按命中点放置并打开四行文字编辑界面，生存/冒险消耗 1 个、创造不消耗；左键移除掉物品，Shift+右键取回并把文字写进 NBT，再放置还原；贴图 `模型/其他物品/便签-物品.png`） |
| `written_note` | `NoteItem` | 已书写的便签（写有任意一行文字后取下 / 破坏便签得到的物品；贴图 `模型/其他物品/已书写的便签-物品.png` → `textures/item/written_note.png`；放置逻辑与便签完全一致，NBT 文字直接还原；文字清空后取下又变回空白便签） |

**控制柜表面组件物品（23 个 `ConsoleCompItem` + 1 个控制柜钥匙）：**

| 物品 ID | 物品类 | 对应组件类型 |
| --- | --- | --- |
| `controller_meter` | `ConsoleCompItem` | GAUGE（流量表） |
| `controller_display` | `ConsoleCompItem` | NUMITRON（数字显示屏） |
| `controller_plotter` | `ConsoleCompItem` | GRAPH（图表面板） |
| `controller_plotter_l` | `ConsoleCompItem` | GRAPH_L（圆盘记录仪 L） |
| `controller_lamp` | `ConsoleCompItem` | INDICATOR（指示灯） |
| `controller_button` | `ConsoleCompItem` | KEYPAD（按钮） |
| `controller_terminal` | `ConsoleCompItem` | TERMINAL（发信终端） |
| `controller_switch` | `ConsoleCompItem` | KNOB_SWITCH（旋钮开关） |
| `controller_lever` | `ConsoleCompItem` | LEVER_SWITCH（拉杆开关） |
| `controller_gauge` | `ConsoleCompItem` | GAUGE_V（立式仪表） |
| `controller_knob` | `ConsoleCompItem` | GAUGE_ARC（弧形仪表） |
| `controller_rgb` | `ConsoleCompItem` | RGB_COLOR（RGB 颜色控制器） |
| `controller_lever_f` | `ConsoleCompItem` | LEVER_F（阀门拉杆） |
| `controller_button_x` | `ConsoleCompItem` | BUTTON_X（旋转按钮） |
| `controller_t_switch` | `ConsoleCompItem` | T_SWITCH（三状态旋钮开关） |
| `controller_t_lever` | `ConsoleCompItem` | T_LEVER（三状态拉杆） |
| `controller_lamp_d` | `ConsoleCompItem` | LAMP_D（双色指示灯） |
| `controller_gauge_f` | `ConsoleCompItem` | GAUGE_F（扇形仪表） |
| `controller_synchroscope` | `ConsoleCompItem` | SYNCHROSCOPE（同步指示器） |
| `controller_button_s` | `ConsoleCompItem` | BUTTON_S（安全按钮） |
| `controller_switch_k` | `ConsoleCompItem` | SWITCH_K（钥匙开关） |
| `controller_alarm` | `ConsoleCompItem` | ALARM（警报器） |
| `controller_numpad` | `ConsoleCompItem` | NUMPAD（数字键盘，屏幕显示浮点变量，空手右键输入数值） |
| `console_key` | `ConsoleKeyItem` | —（控制柜钥匙，NBT 记录所属钥匙开关组件代号） |

**音乐唱片（33 个，`RecordItem`，Rarity.RARE）：**

| 物品 ID | 比较器输出 | 音轨 |
| --- | --- | --- |
| `music_disc_level5` | 5 | level5（5260 tick） |
| `music_disc_railugun` | 1 | railugun（5140 tick） |
| `music_disc_never` | 15 | never（4320 tick） |
| `music_disc_assumptions` | 2 | assumptions（4280 tick） |
| `music_disc_conrnfield_chase` | 1 | conrnfield_chase（5860 tick） |
| `music_disc_move` | 2 | move（5560 tick） |
| `music_disc_more_one_night` | 5 | more_one_night（4400 tick） |
| `music_disc_rain` | 2 | rain（5300 tick） |
| `music_disc_end` | 2 | end（5080 tick） |
| `music_disc_underground_river` | 6 | underground_river（3900 tick） |
| `music_disc_hanezeve_caradhina` | 6 | hanezeve_caradhina（4020 tick） |
| `music_disc_cutie_mew_mew_magic` | 3 | cutie_mew_mew_magic（3680 tick） |
| `music_disc_denise` | 4 | denise（8340 tick） |
| `music_disc_gwangju` | 7 | gwangju（4160 tick） |
| `music_disc_higher` | 8 | higher（4260 tick） |
| `music_disc_king` | 9 | king（4520 tick） |
| `music_disc_marisa` | 10 | marisa（2560 tick） |
| `music_disc_mixue` | 11 | mixue（2060 tick） |
| `music_disc_raw_tell` | 12 | raw_tell（6340 tick） |
| `music_disc_reimu` | 13 | reimu（5700 tick） |
| `music_disc_you_will_be_perfect` | 14 | you_will_be_perfect（3188 tick） |
| `music_disc_bloom` | 5 | bloom（5930 tick） |
| `music_disc_jigoku_shoujo` | 6 | jigoku_shoujo（1891 tick） |
| `music_disc_the_imitation_game` | 1 | the_imitation_game（3160 tick） |
| `music_disc_bit` | 3 | bit（3187 tick） |
| `music_disc_broken_boy` | 4 | broken_boy（4847 tick） |
| `music_disc_panic_track` | 7 | panic_track（3075 tick） |
| `music_disc_resonance` | 8 | resonance（4254 tick） |
| `music_disc_roller_mobster` | 9 | roller_mobster（4286 tick） |
| `music_disc_sabotage` | 10 | sabotage（4540 tick） |
| `music_disc_friends_wine` | 11 | friends_wine（5145 tick） |
| `music_disc_air` | 12 | air（7003 tick） |
| `music_disc_level` | 13 | level（2383 tick）（LEVEL !，注册名不带感叹号） |

> 唱片同时注册进原版 `minecraft:tags/items/music_discs` 标签，可被唱片机播放；比较器输出值在 1~15 之间，其中 1~12 被多张唱片复用（1 = railugun、conrnfield_chase、the_imitation_game；2 = assumptions、move、rain、end；3 = cutie_mew_mew_magic、bit；4 = denise、broken_boy；5 = level5、more_one_night、bloom；6 = underground_river、hanezeve_caradhina、jigoku_shoujo；7 = gwangju、panic_track；8 = higher、resonance；9 = king、roller_mobster；10 = marisa、sabotage；11 = mixue、friends_wine；12 = raw_tell、air；13 = reimu、level），其余 14~15 各一张。

### 3. 方块实体（Block Entity Types）——共 22 个

| 方块实体 ID | 实体类 | 支持的方块 |
| --- | --- | --- |
| `console_lever_be` | `ConsoleLeverBlockEntity` | console_lever_1、console_lever_2 |
| `console_lever_3stage_be` | `ConsoleLever3StageBlockEntity` | console_lever_6、console_lever_7 |
| `console_panel_be` | `ConsolePanelBlockEntity` | point_lamp_1/2/3、meter、console_panel、console_panel_large、console_button_1~5、safe_button_1、password_inputer |
| `plotter_be` | `PlotterBlockEntity` | plotter |
| `plotter_clock_be` | `PlotterClockBlockEntity` | plotter_clock |
| `digital_display_be` | `DigitalDisplayBlockEntity` | digital_display |
| `digital_knob_be` | `DigitalKnobBlockEntity` | digital_knob |
| `digital_plotter_be` | `DigitalPlotterBlockEntity` | digital_plotter |
| `data_unit_be` | `DataUnitBlockEntity` | data_unit |
| `block_detector_be` | `BlockDetectorBlockEntity` | block_detector |
| `canvas_sign_be` | `CanvasSignBlockEntity` | 16 色 × `_canvas_sign`、`_canvas_wall_sign`（立式/壁挂粗布告示牌） |
| `canvas_hanging_sign_be` | `CanvasHangingSignBlockEntity` | 16 色 × `_hanging_canvas_sign`、`_canvas_wall_hanging_sign`（悬挂式粗布告示牌） |
| `fridge_be` | `FridgeBlockEntity` | fridge（冰箱，27 格容器） |
| `key_distributor_be` | `KeyDistributorBlockEntity` | key_distributor（钥匙分发控制器，存储钥匙柜记录列表） |
| `extended_relay_be` | `ExtendedRelayBlockEntity` | extended_relay（扩展红石继电器） |
| `relay_bus_be` | `ExtendedRelayBusBlockEntity` | relay_bus（扩展红石继电器总线，CC 外设宿主） |
| `poster_be` | `PosterBlockEntity` | poster（海报，存图片索引 / 缩放 / 偏移） |
| `graffiti_be` | `GraffitiBlockEntity` | graffiti（涂鸦文字，存文字 / 颜色 / 大小 / dx / dy / 字体格式） |
| `graffiti_shape_be` | `GraffitiShapeBlockEntity` | graffiti_shape（涂鸦形状，存类型 / 预设 / 颜色 / 大小 / 线宽 / 旋转 / dx / dy） |
| `console_block_be` | `ConsoleBlockEntity` | console_block（控制台结构方块，存所属总线 + 结构状态） |
| `console_bus_be` | `ConsoleBusBlockEntity` | console_bus（控制台总线，三态结构 + 最小角/尺寸/格数） |
| `controller_slope_be` | `ConsoleSlopeBlockEntity` | controller_slope、controller_slope_blue、controller_slope_black（斜面控制台，只存 Bus / Host / HostFace 链接信息） |

> 注意：`breaker`（断路器）为纯逻辑方块，**没有**方块实体，状态完全由 `BlockState`（FACING/POWERED）驱动。
>
> **告示牌为何需要自定义方块实体类型**：原版 `BlockEntityType.SIGN` / `HANGING_SIGN` 的 `validBlocks` 只包含原版告示牌方块。`BlockEntityRenderDispatcher` 渲染时会先做 `blockEntity.getType().isValid(blockState)` 校验，若方块不在该方块实体类型的有效方块集合内则直接跳过渲染，导致告示牌完全透明（无模型、无文字、亦非紫黑块）。因此为粗布告示牌注册了专用方块实体类型（工厂复用 `CanvasSignBlockEntity`/`CanvasHangingSignBlockEntity`，`getType()` 返回自定义类型），并在客户端为这两个类型注册原版 `SignRenderer`/`HangingSignRenderer`。模型层与材质仍由原版机制按 `WoodType "cc_rc:canvas"` 自动生成。

### 4. 声音（Sound Events）——共 134 个

32 个音乐声音与 32 张唱片一一对应：`music_level5`、`railugun`、`never`、`assumptions`、`conrnfield_chase`、`move`、`night`（more_one_night）、`rain`、`end`、`underground_river`、`hanezeve_caradhina`、`cutie_mew_mew_magic`、`denise`、`gwangju`、`higher`、`king`、`marisa`、`mixue`、`raw_tell`、`reimu`、`you_will_be_perfect`、`bloom`、`jigoku_shoujo`、`the_imitation_game`、`bit`、`broken_boy`、`panic_track`、`resonance`、`roller_mobster`、`sabotage`（GitHub issue #1 新增 6 首）、`friends_wine`、`air`（2026-09-14 新增 2 首）；另有 6 个非唱片声音：`nai_long`（奶龙玩偶语音，右键奶龙玩偶 `nai_long_toy` 时播放）、`server_noise`（F.A.A.S服务器 `server_faas_1/2/3` 的环境音效，玩家靠近时持续播放，注册于 [sounds.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/sounds.json)）、`password_crack`（破解器破解音效，破解密码输入器期间在方块位置循环播放，中断/成功/方块破坏时由网络包通知客户端停止）、`gajin`（盖金蜗牛右键音效，玩家右键 `gajin` 时播放，平常无 ambient 叫声）、`reactor_start` / `reactor_start_full`（反应堆启动音乐，素材 `模型/音频素材/反应堆正常启动音乐.ogg` 3:51 转 mono 160kbps；`reactor_start` 截取前 56s 且末尾 4s 淡出（52~56s，淡出算在 56s 内，尾部 RMS -28dB），`reactor_start_full` 完整全曲；两者均 `stream: true`、**仅指令播放无唱片**，如 `/playsound cc_rc:reactor_start @p`）。

另有 **麦卡锡音频素材 96 条**（2026-11-22 新增，仅注册声音、无唱片/物品）：素材来自 `模型/音频素材/麦卡锡音频/{器械声,爆炸声,警报声,运行声,音乐}`，按「英文类别_英文名」注册——`mechanical_*` 18 条、`explosion_*` 5 条、`alarm_*` 34 条、`running_*` 31 条、`music_*` 8 条。音频存放在 `assets/cc_rc/sounds/mccarthy/<category>/<basename>.ogg`，`sounds.json` 中对应 `cc_rc:mccarthy/<category>/<basename>`，`music` 类别带 `"stream": true`；全部无 subtitle。压缩统一使用 VLC `acodec=vorb,ab=64,channels=1,samplerate=44100`（与唱片同规格，单声道才能吃到距离衰减）。仅对文件名带 `^` 的素材裁剪：`^N` 表示同一声音重复 N 次，取整数个完整循环（警报类约 2s、其它 <5s）；`^连续` 表示可任意裁剪，取开头 2s（警报类）/4.8s（其它）；不带 `^` 的一律不裁剪。

### 5. 创造标签

方块与物品通过 [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入标签 `cc_rc_tab`（"CC：反应堆控制台"）；另注册第二个标签 `cc_rc_tab_carried`（"CCRC：搬运的物品"），Forge 按注册名对模组创造标签排序，此前缀更长的注册名位于主标签之后，图标为箱装土豆，仅收录搬运类物品（当前为箱装土豆、冰箱、水槽与 16 色粗布告示牌/悬挂式粗布告示牌，均不加入主标签）。

### 6. 实体（Entities）——共 6 个

**弹射物实体（Projectile Entities）——1 个：**

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `bao_zi` | `BaoZi` | 包子（继承原版雪球类 `Snowball`，飞行逻辑与雪球一致；命中实体或方块时触发半径 4 的爆炸，仅伤害实体、不破坏方块，粒子/音效为原版爆炸；客户端复用原版 `ThrownItemRenderer` 渲染） |

**生物实体（Mobs）——5 个：**

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `error_mob` | `ErrorMob` | 错误生物（由「模型/错误生物/error.obj」立体化生成的敌对实体，详见 [二、27 错误生物](#27-错误生物errormob)） |
| `error_mob_null` | `ErrorMob` | 错误生物变种（「模型/错误生物/null.obj」，NULL 字牌，仅模型/贴图不同） |
| `error_mob_warn` | `ErrorMob` | 错误生物变种（「模型/错误生物/WARN/warn.obj」，WARN 字牌，仅模型/贴图不同） |
| `gajin` | `GajinSnail` | 盖金蜗牛（被动动物，AI 参考原版猪无乘骑机制，金鹰食物，右键播放 gajin 音效，不自然生成，详见 [二、30 盖金蜗牛与金鹰](#30-盖金蜗牛与金鹰gajinsnail--goldeneagle)） |
| `evil_gajin` | `EvilGajin` | 邪恶盖金（敌对生物，继承原版僵尸 AI：不会燃烧/不会捡装备，100 血 20 攻，距离变速追击（远快于玩家疾跑、近身减速），锁定/攻击/击杀/受击全套语音 + RWR 追击循环音，不自然生成，详见 [二、38 邪恶盖金](#38-邪恶盖金evilgajin--evil_gajin)） |

**其他实体（Other Entities）——1 个：**

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `note` | `NoteEntity` | 便签（贴墙薄片实体，可贴在方块表面任意浮点位置；右键打开四行文字编辑界面，左键移除，Shift+右键取回物品并保存文字 NBT，详见 [二、44 便签](#44-便签note)） |

> 实体类型注册于 [ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java)（`DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ...)`），在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 构造函数中 `ModEntities.ENTITY_TYPES.register(modEventBus)`。左键投掷由客户端 `InputEvent.InteractionKeyMappingTriggered` 事件 + C2S 数据包实现（见「二、15 包子」）。
