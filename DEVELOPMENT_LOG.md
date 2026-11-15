# CC：反应堆控制台（cc_rc）开发日志

> 模组 ID：`cc_rc` ｜ Minecraft Forge 1.20.1 ｜ 前置：CC: Tweaked
> 本文档按「目录 / 代码 / 注册物品」三部分组织，目录为索引，方便快速定位源码与实现。
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
| 26 | 注册 | 所有注册方块（按方块类归组） | [三、1 方块](#1-方块blocks) |
| 27 | 注册 | 所有注册物品（含唱片） | [三、2 物品](#2-物品items) |
| 28 | 注册 | 方块实体 / 声音 / 创造标签 / 弹射物实体 | [三、3~6 方块实体·声音·创造标签·弹射物实体](#3-方块实体block-entity-types) |
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
| 45 | 生物/物品 | 错误生物 error_mob（由 QQ 发送的 error.obj 立体化生成的敌对生物实体，仿蠹虫寻路/攻击，无方块交互，不自然生成）+ 刷怪蛋 error_mob_spawn_egg + 变种 error_mob_null/error_mob_warn（null.zip/WARN.zip，仅模型贴图不同）+ 各自刷怪蛋 | [二、27 错误生物](#27-错误生物error_mob) |
| 46 | 物品/GUI | 编辑工具 edit_tool（主手右键可显示名称方块打开文字编辑 GUI；副手放置自动打开编辑界面）+ 统一接口 ITextDisplay | [二、28 编辑工具](#28-编辑工具edit_tool) |
| 47 | 红石/CC外设 | 扩展红石继电器 extended_relay（总线远端红石端口）+ 扩展红石继电器总线 relay_bus（CC 外设 redstone_relay_bus，沿朝向距离搜索继电器读写信号） | [二、29 扩展红石继电器与总线](#29-扩展红石继电器与总线extendedrelayblock--relaybusblock) |
| 48 | 生物/物品 | 盖金蜗牛 gajin（AI 参考原版猪无乘骑机制，金鹰作为食物吸引/繁殖，右键播放 gajin 音效，平常无叫声）+ 刷怪蛋 gajin_spawn_egg + 金鹰 golden_eagle（堆叠 64，蜗牛食物） | [二、30 盖金蜗牛与金鹰](#30-盖金蜗牛与金鹰gajinsnail--golden_eagle) |
| 49 | 物品/音乐 | 唱片音乐 6 张（GitHub issue #1：bit / broken_boy / panic_track / resonance / roller_mobster / sabotage，VLC mono OGG + RecordItem 注册 + 双语 lang） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 50 | CC 外设 | CC 外设无延迟改造（风险最小方案）：knob/display/plotter volatile+dirty+tick 节流广播，relay_bus RelayState 双缓存 + 总线缓存 + Lua 零 Level 访问，修复 Inputs 落盘 | [二、32 CC 外设无延迟改造](#32-cc-外设无延迟改造风险最小方案实施) |
| 51 | 物品/音乐 | 唱片音乐 2 张（friends_wine 朋友的酒 / air 鸟之诗，mp4 源文件经 ffmpeg 提取音频转 mono OGG + RecordItem 注册 + 双语 lang） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 52 | 方块/CC外设 | 数据单元 data_unit（无方向完整方块，专属贴图 top/side；方块实体存名称+空数据列表，编辑工具可改名称，CC 外设读写名称/整个列表/某一位） | [二、33 数据单元](#33-数据单元dataunit) |
| 53 | 方块/CC外设 | 方块探测器 block_detector（六方向放置，专属贴图 top/side/front/back；CC 外设只读探测面向方块的坐标/注册名/模组来源/方块实体数据） | [二、34 方块探测器](#34-方块探测器blockdetector) |
| 54 | 资源整理 | 贴图/模型目录整理：textures/block 与 models/block 顶层散装文件全部归入各方块同名文件夹（含 redstone_receiver/sender、relay_bus、extended_relay 等多贴图方块），所有 JSON 引用同步更新（99 文件） | [二、35 贴图/模型资源目录整理](#35-贴图模型资源目录整理) |
| 55 | 方块/GUI | 控制面板文字样式：染料右键面板染字（16 色，不消耗染料）+ 编辑工具 GUI 新增 B/I/U/S 格式按钮与 16 色块（打开时读取当前样式），EditTextPacket 携带完整样式（颜色/粗体/斜体/下划线/删除线） | [二、36 控制面板文字样式](#36-控制面板文字样式染料染色--编辑工具格式按钮) |
| 56 | 方块 | ~~海报1 poster_1~~（已删除：与 poster_2 贴图完全相同，属第一张重复注册） | [二、37 海报](#37-海报posterblock) |
| 57 | 方块/GUI | 海报 poster（**重构：只注册一个方块**，右键打开 GUI 选图片 + 缩放倍率 + dy/dz 偏移；渲染改 BlockEntityRenderer 直绘四边形，彻底摆脱原版模型元素坐标 ±16/32 限制；旧 poster_2~26 方块/物品/模型/blockstate 全部删除，25 张贴图保留为可选图片） | [二、37 海报](#37-海报posterblock) |
| 58 | 生物/物品 | 邪恶盖金 evil_gajin（敌对生物，僵尸 AI 不燃烧/不捡装备，100 血 20 攻，距离变速追击，锁定/攻击/击杀/受击全套语音 + RWR 追击循环音，模型暂用盖金蜗牛）+ 刷怪蛋 evil_gajin_spawn_egg | [二、38 邪恶盖金](#38-邪恶盖金evilgajin--evil_gajin) |
| 59 | 生物/渲染 | 错误生物新模型（ERROR/NULL/WARN 像素字牌，来源模型/生物/错误生物/新模型三个 Blockbench zip：纯红占位贴图 + 每面独立 UV json）——先离线转 Java addBox，后改 **Blockbench json 直渲**（ErrorMobJsonRenderer 直接加载 json 渲染，复用 BlockbenchJsonModel，同邪恶盖金方案；删除全部 Java Model 类与模型层注册） | [二、39 错误生物新模型](#39-错误生物新模型errornullwarn-像素字牌) |
| 60 | 生物/渲染 | 邪恶盖金换 Blockbench JSON 直渲模型（gaijin_t58.zip：17 元素 5 旋转，texture_size 32x32）——不再转 Java ModelPart，新增运行时加载器 BlockbenchJsonModel 逐元素逐面绘制（每面独立 UV + 旋转元素绕 origin 旋转），渲染器改为 EntityRenderer 直渲 | [二、40 邪恶盖金 JSON 直渲模型](#40-邪恶盖金-json-直渲模型blockbenchjsonmodel) |
| 61 | 物品/音乐 | 唱片「LEVEL !」music_disc_level（感叹号不入注册名，VLC mono OGG 160k ≈1:59，比较器输出 13 复用 reimu，music_discs 标签兼容唱片机） | [二、31 唱片音乐](#31-唱片音乐github-issue-1-新增-6-张) |
| 62 | 指令 | /ccrc sound 音频播放指令（`<声音ID> <目标> [音量]`，**目标必选**、音量可选，定向播放给 @p/@a/@e 等，无距离限制无衰减；S2C 纯数据包 PlaySoundPacket id5 + 客户端 CommandSoundInstance 无衰减播放，遵循 PR#2 服务端加载安全模式） | [二、23 /ccrc 指令](#23-ccrc-指令ccrccommand) |
| 63 | 方块/GUI | 多方块控制台·第一期：控制台结构方块 console_block + 控制台总线 console_bus（六向面板）；总线 GUI 一键校验并成型（BFS 泛洪 + 最小外接长方体体积一致性 + 4096 格上限 + 5 种失败原因），三态 未成型/部分成型/完全成型；完全成型禁止挖掘并免疫爆炸（需总线解锁）；部分成型时结构外露表面绘制一像素网格 | [二、41 多方块控制台](#41-多方块控制台console_block--console_bus) |
| 64 | 方块/GUI | 控制柜程序界面·第二阶段：辅助计算机 controller_aux 右键绑定唯一相邻总线并打开程序界面（左 1/3 变量表滚轮滚动，右 2/3 程序区留空）；变量表支持按槽位创建 / 删除变量、绑定现有设备（变量名 = 设备代号）、布尔点击切换、浮点就地输入、保存数值并强制上传绑定组件；新增 ConsoleVarPacket id15 与服务端名称 / 类型 / 槽位校验 | [二、42 控制柜程序界面](#42-控制柜程序界面第二阶段变量表-gui) |
| 65 | 方块/GUI | 控制柜组件 RGB 颜色控制器 controller_rgb（主名称布尔启用 + R/G/B 三个浮点通道变量驱动灯面染色、连接组件同步颜色、命名后隐藏模型只显示彩色名称、显示原色开关；配置 JSON 支持） | [二、41 多方块控制台](#41-多方块控制台consoleblock--consolebusblock) |
| 66 | 方块/渲染 | 控制柜组件 阀门拉杆 controller_lever_f（开关语义，开关两套 OBJ 把手角度 + 红色背板/把手染色）与 旋转按钮 controller_button_x（按钮语义，未按下时 Rotate 分组顺时针 90° 复位 + 黄色背板染色）；染色采样点改为按贴图动态扫描白块（找不到回退旧 512 常量） | [二、41 多方块控制台](#41-多方块控制台consoleblock--consolebusblock) |
| 67 | 方块/渲染 | 控制柜组件 三状态旋钮开关 controller_t_switch / 三状态拉杆 controller_t_lever（浮点变量 0/1/2，右键 0→1→2→0 循环，各三套模型）+ 双色指示灯 controller_lamp_d（布尔量，红亮 / 绿亮两套模型切换）；RGB 控制器模型换型为 indicator_orange_on（只染灯面）；新组件仅染背板 `o Panel` | [二、41 多方块控制台](#41-多方块控制台consoleblock--consolebusblock) |
| 68 | 物品/GUI | 画图工具 draw_tool（右键控制柜表面打开属性界面，默认未创建，点选类型或右侧贴图样式后才创建图案，直接关闭不留图案；可设置类型/颜色/线宽/图层/尺寸/位置或删除；线宽 0.1~4.0、缩放 0.1~3.0、预设旋转为任意角度；线段与箭头默认长度 8、角度 0；工具 NBT 记住颜色/线宽/缩放/图层/类型/预设；shift+右键空气打开设置界面：快速绘画 / 吸附整数 / 平直线优先；快速绘画下图案单击生成、矩形线段箭头两次右键取点；多功能工具可像拖动组件一样拖动图案；图形存总线方块实体并随 NBT 同步；网络包 id 21/22/23、菜单 console_draw） | [二、41 画图工具](#第四十一期画图工具draw_tool) |
| 69 | 方块/渲染 | 控制柜组件 扇形仪表 controller_gauge_f（gauge_f）：功能与流量表相同（绑定浮点变量、可整图着色），指针 0 = 左下 30°、顺时针扫 240° 到右下 30° = 100；`o Needle` 绕模型 X 轴旋转，烘焙角 -17° | [二、42 扇形仪表](#第四十二期扇形仪表gauge_f) |
| 70 | 方块/渲染 | 控制柜组件 同步指示器 controller_synchroscope（synchroscope）：自身浮点变量为指针角度（-180~180，正上方 = 0），附加浮点变量 `<代号>_s` 为转速（度/tick，正 = 顺时针）；成型后按转速持续旋转并在 ±180 环绕，转速绝对值小于配置阈值（默认 15）且角度在 ±15° 内时切换为灯亮模型 | [二、43 同步指示器](#第四十三期同步指示器synchroscope) |
| 71 | 方块/渲染 | 控制柜组件 安全按钮 controller_button_s（button_s）：带玻璃罩三套模型（合盖 / 开盖弹起 / 开盖按下），Shift+右键开/关盖、开盖后右键按下（按下期间不可关盖）；盖子自动关闭时间可设置（0 = 不自动关，默认 100t）；玻璃面 `usemtl glass` 用 `entityTranslucentCull` 半透明批次两遍绘制（真正透明）；按钮本体 `o Button` 标记 `usemtl tint` 可染色（面板 / 金属框 / 玻璃罩保持原色，无纯白块时保留原 UV 只乘顶点色） | [二、44 安全按钮](#第四十四期安全按钮button_s) |
| 72 | 方块/物品 | 控制柜组件 钥匙开关 controller_switch_k（switch_k）+ 控制柜钥匙 console_key：四套模型（合盖/开盖 × 有无钥匙），部分成型 Shift+右键取钥匙（NBT 记组件代号），成型后默认无钥匙不能开关，钥匙右键插入（消耗）后才能开关，空手 Shift+右键拔钥匙（开闭 / 常开 / 常闭三种模式限制），布尔变量只看开关状态 | [二、45 钥匙开关](#第四十五期钥匙开关switch_k) |
| 73 | 方块/渲染 | 控制柜组件 圆盘记录仪 L controller_plotter_l（graph_l）：独立更高的模型（16×13，带右侧 `o Pen` 笔），浮点变量 / 历史采样 / 推送触发名称与圆盘记录仪相同；笔只按最新采样值（history[0]）上下平移，曲线按新纸面区域绘制，组件颜色只染曲线 | [二、46 圆盘记录仪 L](#第四十六期圆盘记录仪-lgraph_l) |
| 74 | 方块/渲染 | 控制柜组件 警报器 controller_alarm（alarm）：面板从左到右为方形灯 / 静音按钮 / 重置按钮，主布尔变量 true = 报警；报警中未静音时灯闪烁并从组件位置周期播放占位音效，按静音按钮静音（只读复制变量 `<代号>_Q` 置 true、灯常亮、停止发声），按重置按钮把主变量写回 false 并熄灭；6 套模型变体（灯灭/灯亮 × 中键按下 / 右键按下） | [二、47 警报器](#第四十七期警报器alarm) |

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
- **微调**（用户反馈后）：①染料染色**不再消耗染料**（编辑工具 GUI 已能直接改颜色，染料仅作快捷手段）；②格式按钮未激活态由灰色改**白色**（仅以粗细区分，暗背景下可读性更好）。
- **构建**：gradlew build BUILD SUCCESSFUL（28s/26s，微调后 29s）。

### 37. 海报（PosterBlock）
贴墙装饰方块。**当前实现：全局只注册一个方块 `poster`**（旧版 poster_2 ~ poster_26 多方块方案已废弃，见本节末「旧实现」）。方块实体保存「当前图片 + 缩放倍率 + dy/dz 偏移」，客户端 `PosterBER` 直接绘制贴图四边形，**因此尺寸不受原版方块模型元素坐标每轴 [-16, 32] 的限制**（旧版 2 倍竖旗 / 塔塔鲁斯九格宽就是因此加载失败变紫黑块）。素材取自 `模型\其他物品\海报\`，双三次缩放并**保留 alpha 透明通道**。

- **方块**（[PosterBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlock.java)）extends `HorizontalDirectionalBlock` implements `EntityBlock`：
  - **水平四向放置**：`getStateForPlacement` 仅点击水平墙面时 `FACING = getClickedFace()`，点击顶/底面返回 null（只能贴墙）；
  - **碰撞箱 16x16x1**：`SHAPE_NORTH=box(0,0,15,16,16,16)`（板子贴墙侧），SOUTH=z0~1 / EAST=x0~1 / WEST=x15~16；
  - `canSurvive` 校验支撑墙 `pos.relative(FACING.getOpposite()).isSolid()`；`updateShape` 支撑墙消失即转 AIR；
  - 属性 `noCollission + strength(0.5F) + WOOD`；
  - `getRenderShape = INVISIBLE`（方块本体不渲染）+ `newBlockEntity`；`use` 右键（非潜行）服务端调 `PosterMenu.open` 打开设置界面。
- **方块实体**（[PosterBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBlockEntity.java)）：字段 `Image`(图片索引) / `Scale`(倍率，1.0=宽 1 格) / `Dy`(垂直偏移，**单位格**) / `Dz`(水平偏移，**单位格**)，`setConfig` 统一夹取（scale 0.05~20、偏移 ±16 格）；NBT 读写 + `getUpdateTag`/`getUpdatePacket` 客户端同步；`getRenderBoundingBox` 扩大为 `inflate(scale+offset+4)` 防止大图被视锥剔除。
- **渲染器**（[PosterBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterBER.java)）：`RenderType.entityCutoutNoCull(贴图)` 手绘一个四边形。几何：以观者视角锚点=贴墙面左上角（离墙 1 像素），图片向右/向下延伸；宽=scale，高=scale*原图高/原图宽（保持比例）；dz 沿观者向右、dy 沿 +Y，**单位均为格（可直接写 0.5）**。四朝向锚点与「向右」向量（与 FACING=可见面法线一致，不会镜像）：NORTH (1,15/16) r=-X；SOUTH (0,1/16) r=+X；EAST (1/16,1) r=-Z；WEST (15/16,0) r=+Z。
- **图片表**（[PosterImages.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/poster/PosterImages.java)）：**共 24 张**，贴图位于 `textures/block/poster/`，**文件名/图片 id 按原始图片名称命名**（GUI 悬停显示的 name 即素材原名）。`Image` 字段：`id`(英文贴图名) / `name`(原始图片名) / `texture` / `width` / `height` / `top`(顶部透明留白)。**索引 0 = `no_material`「无材质」为默认材质**（新放置的海报默认显示它，方块模型 particle 与物品贴图也改用它），其余依次：`goggles_mark` 戴护目镜标识、`mixue_sign` 蜜雪冰城招牌、`poster_1~8`（原 1~8.png）、`ask_yourself`/`energy`/`forgetting`/`parking_lot`/`see_more`/`than_just_you`/`we_need_you`、`mystery_mark` 神秘标识、`flag_quantum_energy`/`flag_quantum_science`/`flag_quantum_lighting`/`flag_quantum_atomic` 四张量子竖旗、`tartarus_gate` 塔塔鲁斯上层入口 logo。
- **GUI**：`PosterMenu`（无槽位菜单，把坐标+当前配置写进同步数据）/ `PosterScreen`（自绘 212x190 深色面板：7 列 x 4 行缩略图 + 缩放/dy/dz 三个输入框 + 应用按钮）/ `PosterConfigPacket`（C2S，服务端校验方块实体类型 + 8 格距离后写入并 `sendBlockUpdated` 同步）。注册：`ModBlockEntities.POSTER_BE`、`ModMenuTypes.POSTER`、`ModNetwork` id=6、`CcRc` client setup `MenuScreens.register` + `RegisterRenderers` 注册 BER。
- **微调（用户反馈后）**：①缩略图几乎空白——原用 9 参 `GuiGraphics.blit`（只截取贴图左上角同尺寸区域、**不支持缩放**），改用 11 参重载 `blit(tex, x, y, w, h, u, v, uW, vH, texW, texH)` 采样整图并拉伸到缩略图框；②dy/dz 由「像素（16=1格）」改为**直接输入格数**（方便填 0.5），BER 不再除 16，偏移上限 ±16 格，界面标签/提示同步改为「垂直 dy（格）/ 水平 dz（格）/ 单位：格（可输入 0.5）」。
- **微调 2（用户反馈后）**：①**默认定位 = 图片顶端对齐方块顶端**：实测 25 张贴图的 alpha 包围盒，只有 poster_10~16（256x512 画布）内容底对齐、顶部有透明留白（top=179，poster_13 为 150），其余图片 top=0；`Image` 增加 `top` 字段（`contentHeight()/aspect()/v0()`），BER 采样 v 从 `v0` 到 1、几何高度按内容高计算，GUI 缩略图也裁掉透明留白——因此图片 9~15（缩略图第 9~15 格 = poster_10~16）默认即「图片顶端贴方块顶端」；②**手持海报物品时标记方块本体**：`PosterBER` 检测本地玩家主手/副手持有 poster 物品，在贴墙面叠加一张 1x1 半透明（alpha 110、全亮 0xF000F0）物品图标（`RenderType.entityTranslucent`），使透明的海报方块位置可见；标记画在**未偏移的锚点**上（标的是方块而非图片）。
- **微调 3（用户反馈后）**：①**删除重复图片**：原 4 组量子竖旗各有「1 倍 / 2 倍」两份**完全相同**的贴图（MD5 逐一核对一致：poster_18=19、20=21、22=23、24=25），缩放倍率可调后各删掉 2 倍那份（poster_19/21/23/25）；②**按图片名称命名**：用「源图等比缩放后与贴图逐像素比对」反查 25 张贴图的来源（方形 8 张、竖版 7 张、旗帜 4 张、神秘标识、塔塔鲁斯均为 diff≈0），贴图文件名与图片 id 全部改为图片原名对应的英文标识，GUI 悬停提示由「图片 N」改为显示原始图片名称；③**新增 3 张海报**（素材同路径 `模型\其他物品\海报\新海报`）：`no_material` 无材质（16x16 原尺寸，**索引 0 = 默认材质**）、`goggles_mark` 戴护目镜标识（384x512 → 256x341）、`mixue_sign` 蜜雪冰城招牌（1087x363 → 768x256）；④方块模型 particle 与物品贴图同步改为 `no_material`。图片总数 25 - 4 + 3 = **24 张**。
- **微调 4（索引秩序修正，重要约定）**：图片索引保存在方块实体 NBT 里，**顺序即存档数据**，一旦发布就不能在中间插入或调换。本次先把新增的「戴护目镜标识」「蜜雪冰城招牌」移到列表末尾，恢复「无材质 = 索引 0、其余按之前注册顺序」的排布（旧图片索引相对第一次创建时 +1）。**后续注册新海报只能 `LIST.add`/在末尾追加，禁止插入中间**，否则所有已放置海报都会变成别的图。
- **资源**：新增 `blockstates/poster.json`（facing 四向 y=0/90/180/270）、`models/block/poster.json`（仅 particle，无 elements）、`models/item/poster.json`（item/generated + `textures/item/poster.png`）；**删除**旧 poster_2~26 的 blockstate/model/item 贴图共 100 个文件，`textures/block/poster/` 现有 24 张贴图（重命名+去重后）。
- **lang**：zh_cn「海报 / 海报设置 / 缩放倍率 / 垂直 dy（格）/ 水平 dz（格）/ 应用 / 单位：格（可输入 0.5）」，en_us 对应英文。
- **构建**：gradlew build BUILD SUCCESSFUL。

#### 旧实现（已废弃，仅存档说明）

旧版把每张海报各注册成一个方块 `poster_2 ~ poster_26`（poster_1 因与 poster_2 贴图 MD5 相同已删除），每个方块用独立方块模型元素把图片「顶出」/「拉宽」出格子，未超出 ±16/32 的能正常显示，超出者（2 倍竖旗 `to.y≈59.29`、塔塔鲁斯 `to.x=144`）模型解析失败 → 紫黑块（日志 `JsonParseException: 'from' specifier exceeds the allowed boundaries`）。该方案整体替换为上面的单方块 + BER 方案。以下为旧实现的内容存档。

#### 海报 2~16（批量注册，旧实现）

素材 `模型\其他物品\海报\1~8.png + ask_yourself/energy/forgetting/parking_lot/see_more/than_just_you/we_need_you.png` 共 **15 张**（已确认 `1.png` 与海报1 原图不同）：

- **命名**：按文件名排序 `poster_2` ~ `poster_16`（poster_2~9 方形、poster_10~16 竖版）。
- **贴图**（保留 alpha，与海报1 相同的双三次缩放 + 透明处理）：
  - 方形 8 张 → `textures/block/poster/*.png` 256x256 全图；
  - 竖版（787x1024 六张；1654x2339 的 parking_lot 一张）→ **256x512 画布、内容按比例 256x333 / 256x362 底对齐**（`256*1024/787=333`；`256*2339/1654=362`），保证"底边贴格子底部、顶边超出"且**图片比例不变**；
  - item 贴图：方形直接复制；竖版单独生成 **256x256 方形画布内容居中**（避免 item/generated 16x16 图标压扁）。
- **模型**（`models/block/poster/poster_N.json`）：方形同海报1（高 16，UV 0~16）；竖版元素 `to.y = 16*内容高/256`（787x1024 → **20.8125**、1654x2339 → **22.625**），UV `v` 起点 = 内容顶部在画布中的位置（`(512-333)/32 = 5.59375`、`(512-362)/32 = 4.6875`），顶边超出碰撞箱但**碰撞箱保持 16x16x1 不变**（用户明确要求不改变碰撞箱，仅渲染超出）。
- **注册**：ModBlocks 用循环 `POSTERS` 列表（15 个注册名 poster_2~16，属性同海报1）、ModItems 用 `POSTER_ITEMS` 循环（注册名取方块名）、创造栏 `forEach` 批量加入；CcRc client setup 用 `ModBlocks.POSTERS.forEach` 批量 `setRenderLayer(cutout())`（全部保留透明）。
- **lang**：zh_cn「海报2~16」、en_us「Poster 2~16」。
- **构建**：gradlew build BUILD SUCCESSFUL（30s）；开发日志目录 57、三、1 方块 110→**125**、三、2 物品 137→**152**（方块物品 76→**91**）已同步。

#### 海报 17~26（新海报批次 + 重复注册修复，旧实现）

用户要求：①修复此前海报第一张重复注册；②注册 `模型\其他物品\海报\新海报` 下 6 张图。

- **重复修复**：核对 MD5 发现 `poster_1.png` 与 `poster_2.png` 相同（方块与物品贴图均一致），确认第一张被重复注册；**删除 poster_1**（方块/物品/blockstate/两个模型/两张贴图/lang 条目/创造栏条目全部移除），保留 poster_2。
- **poster_17 神秘标识**（418x631）：沿用竖版方法——宽 1 格、底边贴格子底部、模型按比例向上超出：元素 `from[0,0,15] to[16,24.1531,16]`（16*631/418），方块贴图 256x386 等比缩放，UV 全图 0~16。
- **poster_18~25 四竖旗**（442x819，比例 0.540）：采用**左右与顶端贴合格子边缘、贴图向下超出格子**的新方法——
  - 1 倍版（18/20/22/24）：元素 `from[0,-13.6471,15] to[16,16,16]`（高 16*819/442=29.6471，顶端 y=16 对齐、向下超出 13.6471px），贴图 256x474；
  - 2 倍版（19/21/23/25）：宽高各翻倍，元素 `from[0,-43.2941,15] to[32,16,16]`（宽 32=2 格、高 59.2941），**左上角对齐格子边缘、右下超出**，复用 1 倍版方块贴图（item 图标各自 256x256）；
  - 对应关系：量子能源公司 18/19、量子科学 20/21、量子灯具 22/23、量子原子能源公司 24/25。
- **poster_26 塔塔鲁斯上层入口标志**（1530x342 ≈ 9:2）：按用户要求直接 **9 格宽 × 2 格高**——元素 `from[0,-16,15] to[144,16,16]`（左上角对齐格子顶端、向下超出 1 格），方块贴图 512x114。
- **资源生成**：PowerShell + System.Drawing 双三次缩放（保留 alpha、统一写无 BOM UTF-8），自动生成 blockstate（四向 y=0/90/180/270）、方块模型、item 模型、方块/物品贴图；JSON 全部通过 ConvertFrom-Json 校验。
- **lang**：zh_cn「神秘标识 / 竖旗·量子能源公司（大）… / 塔塔鲁斯上层入口标志」、en_us 对应英文。
- **构建**：gradlew build BUILD SUCCESSFUL（34s）。

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

### 41. 多方块控制台（ConsoleBlock / ConsoleBusBlock）

第一期目标：结构成型 + 尺寸 GUI（组件/装饰编辑、CC 外设留待后续期）。

- **方块**：`console_block`（完整立方体，无方向，`ConsoleBlock`）+ `console_bus`（水平四向 `FACING`，`ConsoleBusBlock`，右键打开总线界面）。
- **结构定义与校验**（[ConsoleStructureHelper.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleStructureHelper.java)）：
  1. 收集总线六邻居中的控制台方块作为种子（无 → `NO_BLOCK`）；
  2. 首个种子六向 BFS 泛洪（超过 4096 格 → `TOO_LARGE`）；
  3. 其余种子不在同一连通体 → `MULTIPLE`（总线同时贴着两个结构，无法判断归属）；
  4. 最小外接长方体体积 `sx*sy*sz` 必须等于泛洪格数，否则 `NOT_RECTANGLE`（有空洞/凸起）。
  - **关键简化**：不保存成员列表，只记录「最小角 origin + 三边长度」，成员由长方体范围推导。
- **三态状态机**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`UNFORMED` → `PARTIAL`（成型，组件可编辑）→ `FULL`（锁定、交互生效）；`unlock()` 可回 `PARTIAL`。
  - `formStructure()` 先清旧成员标记再校验；失败回到 `UNFORMED` 并记录 `Error`；
  - 服务端主线程每 100 tick 巡检 `stillValid`（爆炸/指令等外部改动自动失效）；
  - **不在 `setRemoved()` 中做任何事**（见下方「修复（保存世界卡死）」）。
  - NBT：`FormState / Error / Origin(long) / SizeX / SizeY / SizeZ / Count / Texts / NextTextId`。
- **成员方块实体**（[ConsoleBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlockEntity.java)）：只存 `Bus(long) + StructureState`，由总线 `applyMembers` 写入并 `sendBlockUpdated` 同步。
- **锁定保护**：
  - [ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) `onDestroyedByPlayer` 在 `FULL` 时返回 false 并提示 `message.cc_rc.console.locked`（需先在总线解锁）；
  - `playerWillDestroy` / `onRemove` 通知总线 `invalidateStructure()`（兜底活塞/指令等途径）；
  - [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 监听 `ExplosionEvent.Detonate`，把锁定的结构方块从受影响列表剔除（总线不保护）。
- **总线界面**（[ConsoleMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleMenu.java) / [ConsoleScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleScreen.java)）：
  - 菜单只同步方块坐标；尺寸/状态/错误每帧从客户端方块实体读取（靠 `sendBlockUpdated` 同步），点完按钮即时刷新；
  - 三个按钮：检测并成型 / 完全成型 / 解除锁定，`containerTick()` 按状态设置 `active`；
  - 操作走 C2S [ConsoleStructurePacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleStructurePacket.java)（`registerMessage(7, ...)`，action 0 成型 / 1 锁定 / 2 解锁，服务端校验类型 + 8 格距离）。
- **表面网格**（[ConsoleBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBER.java)）：仅 `structureState == 1`（部分成型）时为每个外露面（邻居不是控制台方块）用 `RenderType.lines()` 画 16x16 像素网格 + 外轮廓，辅助按一像素精度摆放组件。
- **资源**：`blockstates/console_block.json`（单变体）、`blockstates/console_bus.json`（水平四向旋转，第二期由六向改为四向）、`models/block/console_block.json`（cube_all）、`models/block/console_bus.json`（cube，north=正面屏幕）、`models/item/*.json`、贴图 `textures/block/console/console_block.png` / `console_bus_front.png` / `console_bus_side.png`（16x16，PowerShell + System.Drawing 生成）、中英 lang 键。
- **构建**：gradlew build BUILD SUCCESSFUL。

- **修复（保存世界卡死）**：原实现在 `ConsoleBusBlockEntity.setRemoved()` 中遍历成员并调用 `applyMembers(null, 0)`，而 `setRemoved()` 在区块卸载 / 保存退出时也会被调用，其中的 `setChanged()` / `sendBlockUpdated()` 会让正在保存的区块重新变脏并触发区块系统重入，导致「保存世界中」永久卡死（半成型/完全成型才有成员关联，所以未成型不复现）。现改为**不重写 `setRemoved()`**（不在卸载路径做任何跨方块实体操作），成员标记清理交给 [ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) 新增的 `playerWillDestroy` / `onRemove`（世界正常运行时执行，可安全同步）。

**第二期：总线水平四向 + 结构表面文字**

- **总线改为水平四向**：`FACING` 由 `BlockStateProperties.FACING`（六向）改为 `HORIZONTAL_FACING`，放置取 `context.getHorizontalDirection().getOpposite()`；`blockstates/console_bus.json` 只保留 north/south(y=180)/east(y=90)/west(y=270) 四个变体。FACING 即「总线背面」方向，同时决定结构顶面/底面文字的头朝向。
- **表面坐标系**（[ConsoleSurface.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSurface.java)）：六个外表面各一组 U（右）/V（上）/N（外法线）单位向量（1 格 = 16 像素）。
  - 四个侧面：`V=(0,1,0)`，`U` 分别为 north(-1,0,0) / south(1,0,0) / west(0,0,1) / east(0,0,-1)；
  - 顶面：`V=-FACING`、`U=(f.z,0,-f.x)`；底面：`V=-FACING`、`U=(-f.z,0,f.x)`（仰视时左右相反）；
  - 文字坐标 `(u,v)` 是**相对该表面矩形中心的像素偏移**，可与结构原点/尺寸解耦；提供 `pixelU/pixelV/toWorld/faceCenter/halfWidthPx/halfHeightPx/contains`。
- **文字存储**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`List<TextEntry> texts` + 自增 `nextTextId`（上限 256 条）。`TextEntry` 保存 `id / face / u / v / text / color / bold / italic / underline / strikethrough`，NBT 键 `Texts`（`ListTag`）+ `NextTextId`，随 `getUpdateTag/getUpdatePacket` 同步到客户端。
  - `applyTextEdit(id, face, u, v, text, ...)`：`id>=0` 时修改原文（位置不变，文本为空 = 删除）；`id=-1` 时校验点击点在表面矩形内后新建；仅 `PARTIAL` 可编辑，`FULL` 锁定，`UNFORMED` 提示先成型；返回 `TEXT_OK / TEXT_NOT_FORMED / TEXT_LOCKED / TEXT_OUTSIDE / TEXT_LIMIT`。
  - `getRenderBoundingBox()` 扩到整个结构长方体，避免表面文字被视锥剔除。
- **文字渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：在总线方块实体注册的 BER 中遍历全部文字，用 `ConsoleSurface` 的 U/V/N 构造仿射矩阵（第 0 列 `U/16`、第 1 列 `-V/16`（原版字体 y 轴向下）、第 2 列 `N/16`、平移为文字中心世界坐标），再 `font.drawInBatch` 以 `(-w/2, -lineHeight/2)` 为中心绘制（`DisplayMode.POLYGON_OFFSET`，沿外法线外推 0.005 防深度冲突）。注意 JOML 的 `Matrix4f` 字段非公开，必须用 16 参数构造函数。
- **交互链路**（编辑工具右键控制台方块）：
  1. 客户端 [ConsoleTextClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleTextClient.java) 通过 `ConsoleStructureHelper.findBus` 找总线、读结构范围与朝向，把点击点换算为 `(u,v)`；
  2. 用 `Font.width` 做命中检测：点击点落在某条文字中心矩形（宽 = 字宽、高 = `font.lineHeight`，各留 1 像素容差）内 → 带该文字 id 请求编辑，否则在该点新建；
  3. C2S [ConsoleTextRequestPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleTextRequestPacket.java)（`registerMessage(8)`）→ 服务端校验状态/表面/8 格距离后打开复用的 `EditTextMenu`（`target = TARGET_CONSOLE`，附带 textId/face/u/v）；
  4. 提交走 C2S [ConsoleTextPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleTextPacket.java)（`registerMessage(9)`）→ `applyTextEdit` 写入并同步。
  - 命中检测必须放在客户端：服务端没有 `Font`，无法计算文字宽度。
- **修复（文字全黑）**：表面文字一开始把方块实体传入的 `packedLight` 直接传给 `font.drawInBatch`，总线常贴着结构、处在无光位置，光照值为 0 时文字被乘成纯黑（与所选颜色无关）。现固定 `PACKED_LIGHT = 0xF000F0`（全亮），与本模组数字显示器/记录仪的做法一致。
- **界面复用**：[EditTextMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/EditTextMenu.java) 新增 `TARGET_BLOCK=0 / TARGET_CONSOLE=1` 与 `target/textId/faceOrd/u/v` 字段，并统一为 `write/read` 同步数据格式（先 `target`，控制台目标续读 4 个字段）；`EditTextScreen.submitAndClose()` 按 target 分流发包；`EditToolItem` 新增 `openConsoleEditMenu(...)`。
- **提示文案**：`message.cc_rc.console.need_form / text_locked / text_outside / text_limit`（中英）。
- **第三期：表面文字专用 GUI（锚点 + 位置 + 缩放）**
  - 交互分流：编辑工具右键**普通可显示名称方块**仍打开旧 GUI（`EditTextMenu` + `EditTextScreen`，仅文字/颜色/字体），右键**控制台方块**改走新 GUI；上一版临时塞进 `EditTextMenu` 的 `target` 机制已全部回退清理。
  - 新增 [ConsoleTextMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleTextMenu.java) + [ConsoleTextScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleTextScreen.java)（菜单类型 `console_text`）：上半部分沿用旧界面布局（输入框 / 完成 / B I U S / 16 染料色块），下半部分为原贴图之外用纯色补齐的扩展区，新增：
    - 锚点坐标 `X` / `Y`（结构像素，1 格 = 16 像素，点击表面中心为 0，可负值）；
    - 9 种锚点组合：垂直 `上/中/下` × 水平 `左/中/右`，表示 (X,Y) 对齐文字框的哪条边/中心；
    - 缩放倍率（默认 `0.25`，即旧版字号的四分之一；范围 0.05~4）。
  - `TextEntry` 新增 `anchorH / anchorV / scale` 字段与 NBT `AnchorH / AnchorV / Scale`（旧存档缺字段时默认居中锚点 + 0.25）；`applyTextEdit` 增加这三个参数并把锚点坐标夹在表面矩形内。
  - 渲染：每字体像素 = `scale / 16` 格；按锚点把文字框左上角相对锚点偏移（水平 0 / -w/2 / -w，垂直 0 / -h/2 / -h）后再 `drawInBatch`。
  - 命中检测：客户端按 `scale` 与锚点把每条文字还原成结构像素矩形再做点选，保证「右键文字覆盖区域任意一点」仍可再次编辑。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期：结构表面组件（仅有模型与代号，暂无功能）**

- **组件类型**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：5 种 `GAUGE / NUMITRON / INDICATOR / KEYPAD / TERMINAL`，各带 `id`、默认代号前缀（`GAUGE_1` 之类）与表面占位尺寸（像素）：gauge 5×5、numitron 13×5、indicator 7×4、keypad 5×5、terminal 14×14；尺寸由 OBJ 包围盒实测得出，用于重叠判定与命中检测。
- **模型与贴图资源**：从「模型/控制台类/HBM_RoR_panel_models/hbm_ror_single_block」各目录复制**单件** `*_surface.obj`（不用 `face_layout.obj` 排布模型）到 `models/console_parts/<id>_surface.obj`，贴图复制到 `textures/block/console_parts/<id>.png`（gauge 14x7 / numitron 15x11 / indicator 21x6 / keypad 13x7 / terminal 20x20）。
- **客户端 OBJ 加载器**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：项目无 OBJ 模型加载 API，故自行解析 `v / vt / vn / f`（支持多边形扇形三角化与负数索引，`v` 轴翻转 `1-v`），缓存为 `ObjMesh{tris, minX, centerY, centerZ, texture}`；包围盒自动求出「最内侧 x（贴表面）」与「y/z 中心（对齐锚点）」，避免逐模型硬编码偏移。
- **组件数据**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`List<CompEntry> comps` + 自增 `nextCompId`（上限 256）；`CompEntry` 保存 `id / type / face / u / v / code / color`，NBT `Comps`（`ListTag`）+ `NextCompId`（旧存档无字段时为空列表）；`color` 仅在真正设置过时写入 `Color`，默认 `NO_COLOR = -1`（保持贴图原样，兼容旧存档）。
  - `applyCompAdd(type, face, u, v)`：坐标夹在表面内、与已有组件矩形相交则拒绝、代号默认 `类型前缀_序号`、颜色默认未设置；`applyCompEdit(id, code, u, v, color)`：代号格式 `[A-Za-z0-9_]{1,32}`、重复校验、移动时排除自身、颜色只接受 `0xRRGGBB` 或 `-1`；`applyCompDelete(id)`。
  - 返回码：`COMP_OK / NOT_FORMED / LOCKED / OUTSIDE / LIMIT / DUP_POS / DUP_CODE / BAD_CODE / NOT_FOUND`，仅 `PARTIAL` 可编辑。
  - `findCompAt(face, u, v)` 用轴对齐矩形（各留 0.5 像素容差）做点选。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：先画组件再画文字；每个组件用 `ConsoleSurface` 的基向量构造矩阵（第 0 列 = 法线 / 模型 x、第 1 列 = V / 模型 y、第 2 列 = **-U** / 模型 z，平移把 `(minX, centerY, centerZ)` 对齐到锚点），逐三角形用 `RenderType.entityCutoutNoCull(贴图)` 绘制；光照取组件表面外侧 0.05 格处的 `LevelRenderer.getLightColor`（level 为 null 时回退全亮）。1.20.1 的 `VertexConsumer` 需用 `overlayCoords(int)` 与 `normal(Matrix3f, ...)`。
  - 第 2 列取 `-U` 的原因：模型空间 `x = 外法线、y = 向上` 是右手系，其 z 轴 = `N × V` = `-U`；若直接用 `+U`，三列构成左手系（行列式 -1），整个模型会被水平镜像（表盘红色指示跑到错误一侧）。
- **组件物品**（[ConsoleCompItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ConsoleCompItem.java)，注册 `console_gauge / console_numitron / console_indicator / console_keypad / console_terminal`，`ModItems` + 创造标签 + `models/item/*.json` 使用 `block/console_parts/<id>` 贴图）：
  - 手持右键 `console_block` → 服务端找总线、校验 `PARTIAL`、把点击点换算为 `(u,v)`；命中已有组件则打开组件界面，否则在点击处新增；
  - 编辑工具右键控制台方块时，`ConsoleTextClient` 先做组件命中检测，命中则发 `ConsoleCompRequestPacket` 打开组件界面，未命中才走文字编辑。
- **组件界面**（[ConsoleCompMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompMenu.java) / [ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)，菜单类型 `console_comp`）：显示组件类型；可编辑**组件代号**（英文/数字/下划线）、**位置 X/Y**（结构像素）、**组件颜色**（16 个染料色块 + 手动 `#RRGGBB` 输入 + 「原色」恢复，带颜色预览块）；底部「完成」提交、「删除组件」移除。
  - 网络：C2S [ConsoleCompRequestPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompRequestPacket.java)（id 10，开界面）与 [ConsoleCompPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompPacket.java)（id 11，提交/删除），均用服务端保存的组件位置做 8 格距离校验。
- **提示文案**：`message.cc_rc.console.comp_locked / comp_outside / comp_limit / comp_dup_pos / comp_dup_code / comp_bad_code / comp_added`，以及 `gui.cc_rc.console.comp.<id>` 五种类型名（中英）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期补充：组件颜色（贴图逐像素重新着色）**

- **为什么不用顶点色**：直接用 `consumer.color()` 相乘会把原贴图的色相也乘进去。改为**逐像素重算贴图**：只处理「亮部或带彩度」的像素（白色/奶白底色、表盘红色区域、按键区、灯面），把色相替换为所选 RGB，亮度按 `luma / 该类型最亮可着色像素 luma` 的比例保留（于是红色区域自然成为所选颜色的加深版）。
- **着色器**（新建 [ConsoleCompTextures.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompTextures.java)）：读取 `textures/block/console_parts/<id>.png` 作为基准（缓存原始 `NativeImage` 与归一化亮度 `REF_LUMA`），按 `(类型, 颜色)` 生成 `DynamicTexture` 并注册到 `cc_rc:dynamic/console_comp/<type>_<RRGGBB>` 后缓存（上限 96 张，超限整批释放）。
  - 像素判定 `isTintable`：**亮部或带彩度**才换色 → 深灰外壳（luma≈80）、黑色刻度（luma≈0）、透明像素保持原样；表盘奶白底色与右上角红色区域都会换色。
  - **指针保持红色**：`gauge_surface.obj` 里指针（`o Needle`）的 11 个面 UV 全部退化为同一点（-0.002146, -0.002232），采样红色区域边角的同一个像素 —— 颜色上与表盘右上角红色区域无法区分。做法：`ConsoleCompModels` 解析时把「三个顶点 UV 完全相同」的三角形标记为 `solidColor`；`ConsoleCompTextures` 在着色贴图里另找一个**不被任何面采样**的像素（用每个三角形 UV 包围盒做保守覆盖判断，优先选透明像素；gauge 得到 (11,5)），把它涂成原指针色（C82525，`NEEDLE` 缓存），渲染时 `ConsoleBusBER` 再把所有 `solidColor` 面的 UV 换成该像素中心 —— 指针保持原红、表盘（含右上角红色区域）整块正常着色，不会在任何位置残留与指针同色的小方块，组件整体仍只用一张贴图。
  - 归一化：`factor = min(1, luma / REF_LUMA)`，于是同一区域里较暗的像素自动得到「所选颜色加深」的效果。
  - `NativeImage` 像素为 **ABGR** 打包（`(a<<24)|(b<<16)|(g<<8)|r`），写入时必须按此顺序拼装。
- **类型开关**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：新增 `recolorable` 字段，`GAUGE / INDICATOR / KEYPAD = true`，`NUMITRON / TERMINAL = false`（颜色只保存，后续用于显示文字颜色，暂不改外观）。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：`comp.color != NO_COLOR` 时用 `ConsoleCompTextures.get(type, color, mesh)` 得到的贴图替代 `mesh.texture`，取不到则退回原贴图；**整个组件只使用一张 RenderType**（不要在同一个组件的绘制里切换第二张原贴图，实测会导致着色整体不生效）。
- **实际效果**：gauge 表盘底色 = 所选颜色、右上红色区域 ≈ 0.38 倍变暗、红色指针保持原色；keypad 中间按键区、indicator 灯面 = 所选颜色；外壳与刻度不变。indicator 的「熄灯色 = 亮灯色加深」在灯状态逻辑实现时按同一 `factor` 规则推导。
- **界面与网络**：`ConsoleCompMenu` / `ConsoleCompPacket` 增加 `color` 字段；`ConsoleCompScreen` 增加 16 个染料色块（选中白框）、`#RRGGBB` 输入框、「原色」按钮与颜色预览块。
- **构建**：gradlew build BUILD SUCCESSFUL。

**修复：组件模型渲染错乱（条纹花屏 / 镜像）**

> 原则：只改渲染代码，不动 `*_surface.obj` 资源，保证后续新增 OBJ 组件同样生效。

- **左右镜像**：局部矩阵第 2 列由 `+U` 改为 `-U`，平移同步改为 `anchor - N·minX - V·centerY + U·centerZ`。模型空间（x=外法线、y=向上）为右手系，z 轴应等于 `N×V = -U`；原写法三列构成左手系（行列式 -1），导致所有组件被水平镜像（gauge 红色指示偏移到错误一侧）。
- **共面深度冲突**：新增常量 `COMP_OUTSET = 0.002`（格），组件锚点沿外法线外推后再放置。`_surface` 模型的最内侧 x 恒为 0，而 `console_block` 是 `cube_all` 完整立方体，两者外表面完全共面，插值深度仅差几个 ULP，会互相抢深度 → 密集条纹 / 撕裂花屏。外推量对所有 OBJ 通用，不依赖具体模型尺寸（模型自带的 `minX` 仍由平移抵消）。
- **顶点模式不匹配（真正的主因）**：`RenderType.entityCutoutNoCull` 的顶点格式是 `VertexFormat.Mode.QUADS`（每个图元 4 个顶点），而组件渲染原来按三角形只提交 3 个顶点。缓冲区于是把「下一个三角形的首顶点」当成当前四边形的第 4 点，相邻三角形被错位缝合成虚假斜面 → 密集斜纹花屏、撕裂折片、悬空平面、X 形风车。修复：每个三角形补一个与第 3 点重合的顶点，构成 `(a,b,c,c)` 四边形（上传时拆成 `(0,1,2)` + `(0,2,3)`，第二个退化不可见）。参照本模组已正常工作的 [BlockbenchJsonModel.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/entity/BlockbenchJsonModel.java#L230-L247)，它对 `entityCutoutNoCull` 同样每面提交 4 个顶点。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期补充：组件附带文字**

- **数据**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增嵌套类 `CompText`（内容 + 颜色 + B/I/U/S，`NONE` = 无附带文字）；`CompEntry` 增加 `text` 字段，NBT 仅在有文字时写 `CompText / CompTextColor / CompTextBold / CompTextItalic / CompTextUnderline / CompTextStrike`（旧存档无字段 = 无附带文字）；新增 `applyCompText(id, text, color, bold, italic, underline, strike)`：仅部分成型可改、空文本 = 清除、不保存坐标/缩放。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：新增 `renderCompTexts(...)`，在所有组件模型画完后统一绘制（文字用字体着色器，集中放在模型批次之后，避免打断组件贴图批次）；`renderCompText(...)` 按类型固定位置与锚点 —— 指示灯在灯板表面居中（沿外法线外推到模型最厚处 `maxX - minX` 之外）、按钮/仪表在组件右侧垂直居中并左对齐、其他在组件下方水平居中并上对齐；间距常量 `COMP_TEXT_GAP = 1` 结构像素，字号固定 `TextEntry.DEFAULT_SCALE = 0.25`。
- **模型**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：`ObjMesh` 新增 `maxX`（模型最外侧），供指示灯把文字放到灯板外表面之前。
- **界面与网络**：`EditTextMenu` 增加 `compId`（-1 = 普通方块文字，>= 0 = 组件附带文字），打开界面同步数据追加 `writeVarInt(compId)`；[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java) 新增 `openCompTextMenu(...)`；`EditTextScreen.submitAndClose()` 按 `compId` 分流发送 `EditTextPacket` / `ConsoleCompTextPacket`；新增 C2S `ConsoleCompTextRequestPacket`（id 12，开界面）与 `ConsoleCompTextPacket`（id 13，提交），均校验成型状态 / 组件存在 / 8 格距离；[ConsoleTextClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleTextClient.java) 编辑工具命中组件时改发 `ConsoleCompTextRequestPacket`（组件设置界面仍由组件物品右键打开）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期补充：默认字号配置化 + 文字编辑界面加宽**

- **配置**（[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)）：新增 `console_text.default_scale`（Double，默认 `0.15`，范围 0.05~4.0）与 `getConsoleTextDefaultScale()`；删除 `TextEntry.DEFAULT_SCALE` 常量，改为从配置读取。
- **默认值生效点**：新建表面文字（[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java)）、旧存档缺 `Scale` 字段的兜底（`TextEntry.load`）、组件附带文字渲染（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）、界面输入非法时的回退（[ConsoleTextScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleTextScreen.java)）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期补充：组件尺寸调整 + 多功能工具拖动**

- **尺寸**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：枚举新增 `renderScale` 字段 —— `KEYPAD = 0.5`（按钮缩小到原来一半）、`GAUGE = 1.2`（流量表放大到 1.2 倍）、其余 `1.0`。`halfWidthPx()/halfHeightPx()` 同步乘 `renderScale`，命中检测、重叠校验与附带文字定位自动跟随。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：组件局部矩阵三个基向量同乘 `renderScale`，**同时平移量里的 `N·minX / V·centerY / U·centerZ` 也乘 `renderScale`**，使缩放真正以锚点为中心（`anchor + s·(模型点 - 包围盒中心/最内侧面)`）；指示灯附带文字的沿法线外推量改为按缩放后的模型厚度计算。附带文字位置：`case GAUGE, KEYPAD` 拆开，仅 KEYPAD 保持「右侧、左对齐」，GAUGE 落入默认分支「下方、水平居中、上对齐」。
- **修复：缩放后模型定位偏移**：此前只缩放基向量、平移量仍按 `s=1` 计算，模型等于绕局部原点缩放，额外偏移 `(1-s)·(N·minX + V·centerY - U·centerZ)`（按钮向下偏约 4.5 像素、流量表向上偏约 1.8 像素），而点击判定矩形仍以锚点为中心，表现为「点得到、模型却不在点击处」。平移量同步乘 `s` 后模型包围盒中心与锚点重合；`gradlew build` BUILD SUCCESSFUL。
- **结构变化后组件/文字位置保持**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：表面坐标以「表面矩形中心」为原点，扩展或缩减控制台后中心会移动，旧坐标会让组件整体平移。新增 `lastOrigin / lastSizeX/Y/Z` 记录「上一次成型范围」（`invalidateStructure()` 失效前也保存，NBT `LastOrigin / LastSizeX/Y/Z` 持久化，旧存档缺失时用当前范围兜底）；`formStructure()` 成型成功后调用 `rebaseSurfaceEntries(...)`，把新旧表面中心的偏移投影到该面的 U/V 轴并从坐标中扣除，组件与表面文字的世界位置保持不变；法线方向不补偿（始终贴在新表面，避免扩展后嵌进方块内部）。参考系本身仍为表面中心相对像素坐标。
- **拖动（多功能工具）**：新增 [ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java)（仅客户端）：右键按下时抓取点击点下的组件（优先）或表面文字，记录「目标锚点 - 点击点」偏移；之后每客户端 tick 用准星命中点换算表面像素坐标 + 偏移得到新锚点，按 `SEND_THRESHOLD = 0.25` 像素去抖后发包（组件 → `ConsoleCompPacket`，文字 → `ConsoleTextMovePacket`）；松开右键 / 打开界面 / 准星离开同一表面 / 结构变化时结束；位置先夹在表面内，组件再在客户端预判重叠（重叠时不发包，避免服务端提示刷屏）。允许跨同一结构的相邻方块继续拖动。[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java) 的 `useOn` 新增控制台方块分支（客户端调用 `begin`，服务端仅消费交互）。
- **文字拖动**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `applyTextMove(id, u, v)` —— 只改锚点位置，内容/样式/锚点方向/缩放保持不变，校验与 `applyTextEdit` 一致；新增 C2S `ConsoleTextMovePacket`（网络 id 14，服务端按自己保存的坐标校验距离 64 格后再写回）。
- **复用**（[ConsoleTextClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleTextClient.java)）：把文字命中检测抽为公共方法 `findTextAt(bus, face, u, v)`，右键编辑与拖动共用同一套「锚点 + 缩放」矩形判定。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第四期补充：新增旋钮开关 / 拉杆开关 / 立式仪表 / 弧形仪表**

> 模型来源：`模型/控制台类/panel_props_obj/panel_props`（该模型包 1.0 = 1 方块，+Y 向上，正面朝 −Z）；旋钮开关与立式仪表后来改用修订版 `模型/控制台类/panel_props_obj (2)/panel_props` 中同名模型。

- **坐标转换**：源 OBJ 采用 `x∈[0,1] / y∈[0,1] / z∈[-0.44,0]`、正面朝 −Z 的约定，与控制台组件的「x = 外法线、y = 向上、z = 横向（= −U）」不同。转换式 `(x, y, z) → (-z, y, x)`（与源文件顶点一一对应，行列式 +1，纯旋转不改朝向/UV），法线同式转换；转换后包围盒 x 最小为 0（贴表面）、y/z 约 0.034~0.938（≈14 像素），故四种类型统一取 `14.0 × 14.0` 像素占位。
- **资源**（无 BOM，`mtllib` 行替换为注释）：
  - `models/console_parts/knob_switch_surface.obj`（源 `(2)/knob_switch_open.obj`：绿灯 + 箭头指向左上）
  - `models/console_parts/lever_switch_surface.obj`（源 `lever_switch_open.obj`：拉杆左倾 + 绿灯）
  - `models/console_parts/gauge_v_surface.obj`（源 `(2)/gauge_50.obj`：立式条形仪表，指针取 50，指针在修订版中改为单个三角棱柱 10 个面；同包另有 0 / 100 两种）
  - `models/console_parts/gauge_arc_surface.obj`（源 `gauge_buttons.obj`：弧形表盘 + 双按钮，指针取 35）
  - 贴图：`textures/block/console_parts/{knob_switch,lever_switch,gauge_v,gauge_arc}.png`
  - 物品图标：由 `previews/sheet_front.png` 裁切并按蓝色背景判定抠图，输出到 `textures/item/console_{knob_switch,lever_switch,gauge_v,gauge_arc}.png`。
- **类型**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：新增 `KNOB_SWITCH / LEVER_SWITCH / GAUGE_V / GAUGE_ARC`（代号前缀 `KNOB / LEVER / GAUGE_V / GAUGE_ARC`），追加在枚举末尾以保持既有 ordinal 不变（存档/网络包兼容）；`recolorable = false` —— 避免「纯色面当指针重定向 UV」的着色逻辑误伤新模型里的多个纯色材质面，颜色暂只记录。
- **注册**：`ModItems` 新增 `console_knob_switch / console_lever_switch / console_gauge_v / console_gauge_arc`（`ConsoleCompItem`）；`ModCreativeTabs` 追加 4 项；`models/item/console_*.json` 使用 `item/console_*` 图标；`lang` 增补 `item.cc_rc.console_*` 与 `gui.cc_rc.console.comp.*`（中英）。
- **渲染复用**：与既有组件完全相同的加载/渲染路径（`<id>_surface.obj` + `<id>.png`），无需改渲染代码；命中检测、重叠判定、附带文字定位随类型尺寸自动生效。
- **尺寸**：四种新组件 `renderScale` 统一为 `0.3`（缩到原大小的 30%），命中矩形、重叠判定与附带文字定位随缩放自动跟随。
- **染色（只染底板）**：
  - **标记**：模型转换脚本在每个面之前写入 `usemtl tint` / `usemtl prop`，规则为「只有 `Panel` 分组（贴墙的两层长方体）可染色」；[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java) 新增解析 `usemtl`，把面标记为 `Tri.tintable`。
  - **白块**：四张贴图右上角（480..511, 40..71）预留 32×32 纯白像素（已验证四种模型的面 UV 包围盒均不采样该区域）。
  - **渲染**：设置颜色后，`tintable` 面的 UV 改为白块中心并用顶点色乘出所选颜色（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)），未设置颜色时仍用原贴图 UV、外观不变；整图重着色（`recolorable=true`）不参与，避免误染功能部件。
  - **染色范围**：四种组件均只染 `Panel` 分组（贴墙底板的两层长方体）——`knob_switch / gauge_v / lever_switch / gauge_arc` 一致；灯座与灯面、旋钮底座与手柄、拉杆底座与杆体、表框、表盘、指针、按钮外壳全部保持原贴图颜色。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五期：组件运行功能（开关 / 按钮 / 弧形仪表数值调节）**

> 交互手势：**空手右键**组件即操作（编辑工具右键仍打开文字编辑界面、多功能工具右键仍拖动，原有分支不变）。

- **交互入口**（新建 [ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java)）：`tryUse(level, player, clickedPos, hit)` 统一处理空手右键 —— 找所属总线 → 校验已成型 → 用 `BlockHitResult` 的精确命中点换算表面像素坐标 → `ConsoleSurface.contains` 校验 → `findCompAt` 命中组件。客户端只消费交互（返回 true 即不再触发别的行为），真正的状态变更只在服务端执行。弧形仪表额外分流：点在组件矩形**上半区**（表盘）直接消费但无动作，**下半区**左半 `u < comp.u` 为 `-`（`action = 0`）、右半为 `+`（`action = 1`）。
- **方块接入**：[ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) 新增 `use(...)` —— 手持非空物品一律 `PASS`（避免拦住编辑工具 / 多功能工具 / 组件物品的 `useOn`），空手才交给 `ConsoleCompUse`；[ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) 的 `use(...)` 先跑同一套空手组件判定，未命中时保持原逻辑（Shift 直通，否则开总线界面）。
- **状态数据**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `public final long state`（含义按类型而定），NBT 仅在「与类型默认值不同」时写 `State`（旧存档缺字段即取默认值）；`defaultState(type)` = `GAUGE_ARC ? 50 : 0`。新增 `applyCompUse(id, action, bigStep)`：未成型拒绝，`PARTIAL / FULL` 均可操作，变化后重建 `CompEntry` 并 `sync()` 同步客户端。
  - `KNOB_SWITCH / LEVER_SWITCH`：`state` 0 ↔ 1 切换；
  - `KEYPAD`：`state = level.getGameTime() + Config.getConsoleButtonHoldTicks()`（存「应弹起的游戏时刻」，渲染时比较即可）；`tick` 每 tick 先跑 `releaseExpiredKeypads()`，把到点按钮的 `state` 清回 0 并 `writeBackToVar` 同步绑定变量，实现「按下为 true、弹起后为 false」；
  - `GAUGE_ARC`：`state ± (bigStep ? 10 : 1)`，夹在 `0..100`；
  - 其余类型（含立式仪表 `GAUGE_V`）直接返回 `COMP_OK`，不产生无意义同步（功能预留）。
- **配置**（[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)）：新增 `console_button.hold_ticks`（Int，默认 20 tick = 1 秒，范围 1~200）与 `getConsoleButtonHoldTicks()`。
- **开关两态模型**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：缓存改为 `Map<String, ObjMesh>`，新增 `get(type, closed)`（键为 `类型名 + "_close"`，`get(type)` 委托 `closed = false`）；文件名 `<id>_close_surface.obj`。解析新增 `o` 分组记录（`Tri.group`），供按钮/指针的按组动画使用。
- **新增资源**（由源模型转换、临时脚本已删除，无 BOM）：
  - `models/console_parts/knob_switch_close_surface.obj`（源 `(2)/knob_switch_close.obj`：红灯 + 箭头指向右下）
  - `models/console_parts/lever_switch_close_surface.obj`（源 `lever_switch_close.obj`：拉杆右倾 + 红灯）
  - 转换规则与开启态一致 `(x, y, z) → (-z, y, x)`，`vt` 原样；`o Panel` 写 `usemtl tint`，其余写 `usemtl prop`；贴图仍共用 `<id>.png`（灯色 / 箭头已烘在模型 UV 里）。
- **渲染动画**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：`meshFor(comp)` 统一取模型 —— 开关类 `state == 0` 时改用关闭态变体，其余用开启态。
  - **按钮按下**：`KEYPAD` 且 `level.getGameTime() < comp.state` 视为按下，`Button` 分组沿模型 `-x`（外法线向内）平移 `KEYPAD_PRESS_DEPTH = 0.03`；到点后服务端 `releaseExpiredKeypads()` 把 `state` 清 0，按钮复原且绑定变量回到 false。
  - **弧形仪表指针**：`Needle` 分组绕枢轴 `(ARC_PIVOT_Y = 0.4344, ARC_PIVOT_Z = 0.5)` 旋转，转角 `= ARC_DEG_PER_PERCENT(1.2) × (state − ARC_BAKED_VALUE(35))` 度。模型烘焙在 35%（−18°），故追加角即相对 35% 的差值；源空间绕 +Z 的旋转换算到转换后的模型空间，等价于 `my = dz·sin + dy·cos`、`mz = dz·cos − dy·sin`（法线同式），`mx` 不变。于是 0% → −42°、100% → +78°，与模型自带刻度范围一致。
  - 顶点提交改用变换后的 `px/py/pz / nx/ny/nz`；附带文字也走 `meshFor`，开关关闭态下文字位置随同一包围盒。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五期补充：弧形仪表改名「调节器仪表」+ 左右按钮对调**

- **改名**：显示名 `gui.cc_rc.console.comp.gauge_arc` / `item.cc_rc.console_gauge_arc` 由「弧形仪表 / Arc Gauge」改为「调节器仪表 / Regulator Gauge」（[zh_cn.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/lang/zh_cn.json) / [en_us.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/lang/en_us.json)）。注册名 `gauge_arc`、物品 id、贴图与模型文件名**均保持不变**，避免破坏存档与网络包兼容。
- **左右语义对调**：左按钮 = 减、右按钮 = 加（`applyCompUse` 中 `action 0` 为 `-step`、`action 1` 为 `+step`；`ConsoleCompUse` 仍按 `u < comp.u ? 0 : 1` 判定左右，即「0 = 左 = 减」）。
- **颜色对调（左红右绿）**：`gauge_arc_surface.obj` 中 `BtnOpen`（模型 z≈0.64，经右手系渲染后位于**观察者左侧**）与 `BtnClose`（z≈0.29，右侧）原本分别为绿、红。两者面结构完全一一对应（各 24 面，顶点 / UV / 法线索引分别相差 32 / 96 / 24），故直接**交换两组 96 条 `vt` 定义块**（`vt 145..240` ↔ `vt 241..336`）：几何、面索引、贴图均不变，左侧按钮变红、右侧按钮变绿。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五期补充：控制台文字编辑界面改用完整背景贴图**

- **问题**：`ConsoleTextScreen` 原先复用 `edit_tool_gui.png`（面板实际只有 176x158，右侧/底部靠 `graphics.fill` 拼纯色扩展），再在其上叠加 192x228 的扩展区。拼出来的面板会露出原贴图的右边框竖线、上下边框粗细不一致、右下角缺少圆角，视觉上像两块面板叠在一起。
- **新贴图**：新增 [console_text_gui.png](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/textures/gui/console_text_gui.png)（192x234），样式完全对齐原 `edit_tool_gui.png`：1 像素圆角黑色描边、上/左 2 像素白色高光、下/右 2 像素深灰（0x555555）阴影、内部统一淡灰 198,198,198；四个圆角像素块直接从原贴图复制，保证圆角形状一致。
- **代码**：`ConsoleTextScreen` 的 `GUI_BG` 指向新贴图，删除 `TEXTURE_WIDTH / TEXTURE_HEIGHT` 常量与 `renderBg` 里的多段 `fill` 拼贴，改为整张 `blit`；`BG_WIDTH / BG_HEIGHT` 与贴图尺寸一致（192x234），控件坐标不变（底部锚点按钮下沿 y=224，留 5 像素底边距）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五期补充：GUI 边框裁切修复 + 组件设置界面同风格贴图**

- **右边框/下边框消失的根因**：`GuiGraphics.blit` 的 7 参数重载（不带贴图尺寸）默认按 **256×256** 计算 UV。新贴图只有 192×234，于是右侧约 48 像素、底部约 20 像素的边框被当作贴图外区域裁掉。改用 11 参数重载（显式传 `textureWidth = 192`、`textureHeight = 234`，即 `graphics.blit(tex, x, y, w, h, u, v, uw, vh, texW, texH)`）后恢复正常。
- **组件设置界面**：[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) 原来用 5 条 `graphics.fill` 手绘灰色面板 + 单像素描边，边框没有圆角与高光/阴影。新增同风格贴图 [console_comp_gui.png](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/textures/gui/console_comp_gui.png)（176×152，与文字编辑界面同一套边框：1px 圆角黑描边 + 上/左 2px 白高光 + 下/右 2px 深灰阴影 + 内部淡灰），`renderBg` 改为整张 blit；标题分隔线与颜色预览块仍用纯色绘制，控件坐标未变。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五期补充：新增「显示器（graph）」组件 + 组件物品图标改用 RoR 图标**

- **新增类型**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 追加 `GRAPH("graph", "GRAPH", 13×5, recolorable=false, 1.0F, modelId="numitron")`。新增 `modelId` 字段（默认等于 `id`，带重载构造器），[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java) 的 OBJ 路径与贴图路径改用 `type.modelId`，于是显示器直接复用数字显示屏的模型与贴图，不产生重复资源文件。显示功能暂未实现（`applyCompUse` 对其返回成功无事发生）。
- **注册**：[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `console_graph`（`ConsoleCompItem(GRAPH)`），[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 排在 `console_numitron` 之后；新增 item 模型 `models/item/console_graph.json`；中英语言补 `gui.cc_rc.console.comp.graph`（显示器 / Graph Display）与 `item.cc_rc.console_graph`。
- **组件类型存储兼容**：组件类型在 NBT 中以 `Type` 字符串（`type.id`）保存、网络包不传枚举序号，因此枚举中间插入新成员不影响旧存档。
- **物品图标替换**：从 `模型/控制台类/HBM_RoR_item_icons/hbm_ror_icons/icons_16` 复制 6 张 16×16 图标到 `textures/item/`，并把这 6 个 item 模型的 `layer0` 由方块贴图（`block/console_parts/*`）改为物品图标（`item/console_*`）：`console_gauge`←rbmk_gauge、`console_graph`←rbmk_graph、`console_indicator`←rbmk_indicator、`console_keypad`←rbmk_key_pad、`console_numitron`←rbmk_numitron、`console_terminal`←rbmk_terminal。方块表面上的模型贴图不变。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第六期：系列改名为「控制柜」（`controller_*`）+ 辅助计算机方块**

- **背景**：旧版已有 `console_lever_* / console_panel* / console_button_*` 等「控制台」方块，多方块系列继续用 `console_*` 会混淆。现将**多方块系列**的注册名统一改为 `controller_` 前缀，中文译名统一为「控制柜」；旧系列注册名与译名保持不变。
- **方块 / 物品改名**：`console_block → controller_block`、`console_bus → controller_bus`；组件事物 `console_gauge / numitron / graph / indicator / keypad / terminal / knob_switch / lever_switch / gauge_v / gauge_arc → controller_*`。Java 类名（`ConsoleBlock`、`ConsoleBusBlock`、`ConsoleCompType` 等）与内部字段名不变，只改注册 id。
- **组件后缀统一**：同功能组件在不同系列使用相同后缀 —— `controller_meter`（仪表）、`controller_display`（数字显示屏）、`controller_plotter`（圆盘记录仪）、`controller_lamp`（指示灯）、`controller_button`（按钮）、`controller_terminal`（发信终端）、`controller_switch`（旋钮开关）、`controller_lever`（拉杆开关）、`controller_gauge`（立式仪表）、`controller_knob`（调节器仪表）。表面模型/贴图资源仍按类型 id（gauge / numitron / …）存放，`ConsoleCompType` 新增 `itemSuffix` 字段区分物品注册后缀；物品模型与图标文件同步改名。
- **资源同步**：`blockstates/`、`models/block/`、`models/item/`、`textures/item/` 下对应文件全部改名；方块贴图目录 `textures/block/console/` 改为 `textures/block/controller/`（`controller_block.png / controller_bus_front.png / controller_bus_side.png`），模型内引用同步。中英文 lang 键 `block.cc_rc.controller_*`、`item.cc_rc.controller_*` 同步，`gui.cc_rc.console.*` / `message.cc_rc.console.*` 键名不变、文案中的「控制台」改为「控制柜」。
- **方块实体类型 id**：`console_block_be / console_bus_be → controller_block_be / controller_bus_be`。
- **新增 `controller_aux`（控制柜辅助计算机）**：装饰性完整方块，无方块实体，邻接总线放置；本期仅完成注册与占位贴图（`textures/block/controller/controller_aux.png` 暂由总线侧面图复制，后续替换），程序界面在后续阶段接入总线数据。已加入创造标签。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第七期：程序变量表数据层（第一阶段）**

- **变量表模型**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `VarType{BOOL, FLOAT}`、`VarDef{slot, name, type, compId}` 与固定槽位，**槽位下标即地址**，0 号槽位固定为「程序运行间隔」（只读，float，非负整数 tick，0 = 停止）。地址与变量在创建后永久绑定，变量只由玩家手动删除。
- **槽位数量配置**：`config/cc_rc-common.toml` 新增 `console_program.max_vars`，默认 64，范围 2~256（字节码地址为 1 字节，0~255）；即使手动把配置改到 256 以上，`Config.getConsoleProgramMaxVars()` 也会再次夹紧到 256。数组按硬上限 256 预分配，NBT 只保存到最后一个已定义槽位。
- **值的归属**：浮点 / 布尔值分别存 `varFloats`、`varBools`（数组按硬上限 256 预分配，实际可用以配置为准），随总线 NBT 持久化；aux 后续只保存变量定义（表），不保存值。`getVarFloat(0)` 直接返回运行间隔，不重复存储。
- **组件绑定**：`VarDef.compId = 0` 为自定义变量；绑定组件时按类型限制（开关 / 指示灯 / 按钮 → 布尔，仪表 / 显示屏 → 浮点，发信终端与显示器数组不参与），组件删除后变量保留、值冻结，不自动删除。
- **保存上传**：`applyVarValuesSave(float[], boolean[])` 写入本次编辑值，并对所有绑定组件**强制上传一次**（即使值没变），最后只 `sync()` 一次，供玩家测试「变量 ↔ 组件」对应关系；运算过程不写组件。
- **组件数值字段**：`CompEntry` 新增 `float value`（旧存档缺省：弧形仪表取 `state`，其余 0）；`Value` 字段仅在非默认值时写入 NBT。弧形仪表写回时原始值保留在 `value`，指针 `state` 夹在 `-5 ~ 105` 的显示范围内，允许看出轻微超量程又不跑出基座；手动左右按钮调节时同步 `value`。
- **NBT**：`ProgramPeriod`（float）、`Vars`（VarDef 列表）、`VarF`（Float 列表）、`VarB`（int 数组，0/1）。1.20.1 的 `CompoundTag` 无 `FloatArrayTag` 辅助方法，故浮点用 `ListTag<TAG_FLOAT>` 保存。
- **接口**：`applyProgramPeriod`、`applyVarCreate`、`applyVarDelete`、`applyVarValuesSave`、`getVarDef / getVarFloat / getVarBool / findVarSlot`。GUI、网络包、字节码与执行器留待后续阶段。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第八期：控制柜程序界面（第二阶段：变量表 GUI）**

- **辅助计算机入口**（[ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java)）：把 `controller_aux` 由普通 `Block` 改为专用方块。右键扫描六邻：恰好一个 `controller_bus` 时打开程序界面；没有总线或多于一个时用动作栏提示（`message.cc_rc.controller.need_bus / multi_bus`）。仍无方块实体，所有数据放在总线。
- **程序界面框架**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) + [ConsoleProgramMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramMenu.java)）：360×196，左 140 像素为变量表、右侧 220 像素为程序区（本阶段仅占位文字），中间一条分隔线；菜单只同步总线坐标，界面直接读取总线方块实体，变量增删 / 数值同步后自动刷新。新增菜单类型 `console_program`，客户端注册屏幕。背景先用空白贴图 [console_program_gui.png](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/textures/gui/console_program_gui.png)（360×196，纯淡灰 + 1px 描边），后续再替换美化。
- **变量表（左栏）**：按槽位显示全部地址（`#00`…），**鼠标滚轮上下滚动、不分页**，右侧带滚动条；每行两行文本（地址 + 名称 / 类型 + 当前值），行右侧「×」删除。
  - **首行固定为 `#00 运行间隔`**（`ConsoleBusBlockEntity.VAR_SLOT_PERIOD`）：只可点击改值（玩家指定计算机每几个 tick 运行一次），不能删除 / 改名 / 改类型，行尾不显示「×」；
  - 空槽位点击 → 在该地址打开新建表单（名称输入框、类型按钮、创建 / 取消）；
  - **名称输入框带仿原版指令的自动补全下拉**：深色底板 + 黑色描边，悬停行黄色文字 + 深蓝选中底色；**什么也没输入时点击输入框即展开全部可绑定设备**，输入后按前缀过滤（排除与输入完全一致的代号），候选按代号排序；**鼠标滚轮可在下拉内滚动翻看**（同屏最多 5 行，内容超出时右侧画滚动条），滚轮不在下拉内时仍滚动变量表；**悬停某行时显示 tooltip，用中文名说明这是哪种设备**（`ConsoleCompType.translationKey()`）；点击候选直接填入并收起下拉，Esc 先收起下拉、再按才关闭表单；
  - 布尔变量点击整行切换开 / 关；浮点变量点击整行就地弹出数值输入框（Enter 或点击别处提交）；
  - 底部「保存 / 放弃」两个按钮：「保存」把整张变量表提交服务端并由 `applyVarValuesSave` 强制上传一次到绑定组件；「放弃」清空本地待提交值后关闭；
  - **Esc 关闭界面等同保存**（`onClose` 里默认调用保存，只有点「放弃」时跳过）。
- **命名与绑定规则**：变量名与设备代号规则一致（`[A-Za-z0-9_]{1,32}`）。**名称与设备代号对上即自动绑定该设备**（不需要设备下拉），绑定后类型由设备决定并锁定（开关 / 指示灯 / 按钮 → 布尔，仪表 / 显示屏 → 浮点，发信终端与圆盘记录仪不参与）；名称属于不支持变量的设备时禁止创建。服务端 `applyVarCreate` 按名称重新匹配组件（不信任客户端 compId），并对不支持变量的设备代号返回 `VAR_NAME_IS_COMP`（10）。
- **组件 ↔ 变量同步**：新增 `findVarSlotByComp / writeBackToVar / refreshVarsFromComponents`。玩家空手右键开关、调节器仪表后，`applyCompUse` 立即把组件新状态回写到绑定变量；打开程序界面（`ConsoleProgramMenu.open`）前再统一刷新一次，保证 aux 显示的就是设备当前值；新建绑定变量时也以设备当前值作为初值。
- **网络**：[ConsoleVarPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleVarPacket.java)（id 15）一条包四种动作：创建 / 删除 / 保存数值 / 保存运行间隔（`ACTION_SAVE_PERIOD`，仅 0 号槽位）；服务端校验距离（≤8 格）与成型状态（完全成型时变量表锁定），并把结果码翻译成动作栏提示。新增 `ConsoleCompType.variableType()` 统一给出组件对应的变量类型。
- **运行间隔（0 号槽位）**：值存在 `programPeriod`，上限 `MAX_PERIOD_TICKS = 72000`；**手动输入浮点自动四舍五入取整、负值按 0 处理**，客户端 `sanitizePeriod` 与服务端 `applyProgramPeriod` 双重保证（`Math.round(Math.max(0, Math.min(上限, 值)))`）；点「保存 / Esc」时单独通过 `ConsoleVarPacket.savePeriod` 提交，`applyVarValuesSave` 明确忽略 0 号槽位。
- **表单提示区调整**：补全下拉移到 `render()` 最末尾绘制并改为不透明底板；删除底部常驻的名称规则提示（原来会横向超出界面），改为名称框下方 / 类型按钮上方的提示区（`FORM_MSG_Y`），**平常留空，只有报错或已绑定设备时显示**，按宽度自动换行（最多 3 行）；点「创建 / Enter」时先收起下拉，重新输入即清除上次报错。
- **下拉遮挡文字的处理**：MC 的 GUI 文字与色块分批渲染（文字批次晚于色块），单纯把下拉画在后面只能盖住按钮背景、盖不住按钮文字。改为**下拉展开期间直接隐藏类型按钮（`syncTypeButtonVisibility`）并跳过提示区绘制**，收起后恢复；下拉展开状态在输入、点击、Esc、提交等处统一同步按钮可见性。
- **变量表加宽 + 名称悬浮提示**：界面由 300×196 加宽到 **360×196**，变量表由 100 加宽到 **140** 像素（右侧程序区仍有 220），背景贴图同步重设为 360×196；变量名仍按可用宽度截断显示，但**鼠标悬停在名称文字上时弹出 tooltip**：第一行完整变量名，第二行「已绑定设备：代号（组件类型中文名）」或「未绑定设备」（新增 `gui.cc_rc.program.unbound`）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第九期：控制柜程序区（第三阶段 A：程序行 IR + 右侧行编辑 GUI）**

- **程序行数据模型**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java)，新建）：定义不可变表达式树 `Expr`（`FLOAT / BOOL / VAR / NEG / NOT / CAST / ADD / SUB / MUL / DIV / IDIV / MOD / EQ / NE / LT / GT / LE / GE / AND / OR`）、条件 `Cond{ALWAYS, TRUE_VAR, FALSE_VAR}` 与程序行 `Line{enabled, cond, condSlot, targetSlot, expr}`。`Expr.type(Scope)` 做类型推导，只允许变量已定义的类型参与运算；`depth()` 给出嵌套深度。`Line.validate(Scope, slots)` 集中做保存前校验：目标必须是已定义且可写的变量、条件必须是布尔变量、表达式非空且深度 ≤ 16、表达式类型必须与目标类型一致，返回 `OK / BAD_TARGET / BAD_COND / BAD_EXPR / TYPE_MISMATCH / TOO_DEEP`。`Expr` 与 `Line` 各自实现 NBT 与网络序列化，GUI、网络包、NBT、后续编译器共用同一份数据结构。
- **总线程序存储**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `MAX_PROGRAM_LINES = 64` 与 `ProgramIR.Line[] programLines`（**下标即行号**，`null` 为空行）。新增 `getProgramLines / getProgramLine / getProgramScope`（匿名 `Scope`，按 `varDefs` 返回类型与名称）、`applyProgramSave`、`isVarReferenced`。`applyProgramSave` 只在服务端接受，完全成型时拒绝；逐行调用 `validate`，把行错误映射为 `PROG_*` 结果码（20~25），并额外拒绝写入按钮类只读组件；校验通过后整表替换并 `sync()` 一次。
- **变量删除保护**：`applyVarDelete` 在原有判空之后新增引用检查，只要该槽位被任意程序行的目标、条件或表达式引用即返回新增的 `VAR_IN_USE`（11），避免程序静默失效；[ConsoleVarPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleVarPacket.java) 增加对应提示文案。
- **NBT**：新增 `ProgLines`（ListTag，每个非空行保存 `Idx` + `Line.save`），读取时按 `Idx` 还原，重复行号以先读到为准；读取前先整表清空，保证旧存档与损坏数据都不会残留脏行。
- **网络**：[ConsoleProgramPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleProgramPacket.java)（**id 16**，15 已被变量包占用）单动作 `ACTION_SAVE_LINES`，全量快照提交：服务端校验距离（≤8 格）与成型状态，调用 `applyProgramSave`，结果码翻译为 `message.cc_rc.controller.prog_*` 提示。
- **右侧程序区**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：右侧 220×196 划为**行列表**与**行编辑器**两种视图。
  - **行列表**：每页 8 行、共 8 页（64 行），行左侧 12 像素为启用开关（点击切换并立即提交），中间显示「条件 目标 = 表达式」，右侧「×」删除该行；空行显示「空行（点击新建）」；底部页码 `x / 8` 与「上一页 / 下一页 / 保存」按钮；行编辑期间列表与翻页按钮不响应点击，避免误触。
  - **行编辑器**：**不做文本语法解析**（IR 是唯一程序源），依次为「启用 / 条件 / 目标 / 算式 / 追加 / 确定 / 取消」。**除启用按钮、条件三态按钮与「追加」按钮外，其余控件全部改为与变量名称框同样式的输入框**（`EditBox` + `setHint` 占位提示）：
    - **条件**：左侧三态按钮在 `true → 变量 → !变量` 之间轮换；选后两项时按钮右侧出现变量名输入框，选 `true` 时自动隐藏。
    - **算式**：改为**纵向多行「算符 + 操作数」**结构（最多 5 行）。第一行左侧固定显示 `=` 并禁止编辑，右侧填起始值；后续每行左侧输入算符、右侧输入操作数，点「追加」增加一行（到 5 行后按钮置灰）。表达式按**书写顺序从左到右**合并，因此 `a+b-c` 即 `(a+b)-c`，无需括号。**「追加 / 确定 / 取消」三个按钮纵向排列在左侧「算式」标签下方**（`EDIT_BTN_Y=88`、行距 20），表达式输入框整体右缩进 48 像素让出按钮列，避免补全下拉展开时压住按钮。
    - **目标 / 操作数**：直接输入文本。`true` / `false` 解析为布尔常量，数字解析为浮点常量（若目标为布尔变量，`0` / `1` 也按布尔常量）；其余按变量名查找，名称不存在或绑定只读组件时即时提示错误。操作数支持前缀 `!` 取反（如 `A && !B`，也可直接写第一行 `= !A`）。
    - **算符**：输入框键入 `+ - * / // % == != < > <= >= && || !`，无法识别的符号提示 `err_op`；追加行整行留空时忽略该行。`&&` / `||` 为布尔逻辑与 / 或（操作数必须是布尔，结果布尔）；`!` 为**一元非**，表示把本行以上的累计结果取反，该行右侧操作数框自动禁用。
  - **即时校验**：所有输入框 `setResponder` → `onEditorChanged` → `refreshLineError`，逐行校验目标/条件/算符/操作数后调用 `Line.validate`，错误直接显示在编辑器底部，无需提交即可发现；打开已有行时用 `loadingLine` 抑制中途报错。载入已有表达式时沿表达式树的左结合链拆回各行（`flattenExpr`），保证打开-保存不改变语义。
  - **自动补全下拉**：目标、条件、各行算符与操作数输入框都支持仿原版指令的候选下拉（`bindSuggest` 绑定候选来源）：点击输入框即展开全部候选，输入后按前缀过滤（完全一致的项不再列出），支持鼠标滚轮滚动、点击候选填入、Esc 先收下拉；候选来源分别为「可写入变量名」「布尔变量名」「算符表」「true / false + 全部变量名」。GUI 文字批次总是晚于色块提交（`graphics.flush()` 无效），下拉底板压不住已绘制的文字，因此下拉展开期间**直接跳过被遮挡的输入框、标签、报错文字与按钮**（`coveredBySuggest` / `isCoveredBySuggest` / `syncEditorButtons`），底板内没有候选的点击一并吞掉。下拉优先放输入框下方，放不下则翻到上方，两边都放不下才夹取；行数按上下可用空间收缩。操作数框的下拉对齐整行左边界并铺满右侧面板，被遮挡的行整行隐藏，不出现半截输入框。
  - **行列表悬浮提示**：鼠标悬停在某行的「条件 目标 = 表达式」文字区域（不含右侧删除区）时，弹出 tooltip 显示完整表达式，避免长算式被行宽截断后读不全。
  - **交互**：`Enter` 确认、`Esc` 关闭行编辑器并返回列表（不关闭整个界面）；完全成型时行列表与编辑器均只读并提示 `prog_locked`。
- **变量命名与常量语法冲突**：程序行操作数用 `true` / `false` / 数字表示常量，因此新建变量名前缀必须是字母或下划线、且不能与 `true` / `false` 重名（`VAR_NAME_PATTERN` + `equalsIgnoreCase` 判断）。客户端 `confirmCreate` 与服务端 `applyVarCreate` 双重校验；`ConsoleBusBlockEntity` 的 NBT 读取仍沿用旧正则，避免旧存档中数字开头的变量被丢弃。
- **语言资源**：[zh_cn.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/lang/zh_cn.json) 与 [en_us.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/lang/en_us.json) 新增约 22 个 `gui.cc_rc.program.*`（行标题、空行、启用、条件、目标、左右值、运算符、常量、错误提示等）与 9 个 `message.cc_rc.controller.*`（`var_in_use`、`prog_locked / saved / invalid / limit / bad_target / bad_cond / bad_expr / type / deep`）键，并删除已无用的 `gui.cc_rc.program.code_hint`；文本框化改造后再删除 9 个轮换式旧键（`cond / always / target / no_var / operand_a / operand_b / operator / const / op_none`），新增 `cond_true / cond_var / cond_not / label_cond / label_target / label_expr / add_op / ok / hint_var / hint_operand / hint_operator / err_op`，`name_rule` 文案同步改为「不能数字开头、不能为 true/false」；两个 JSON 均无 BOM。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。字节码编译、VM 执行与 tick 调度留待下一阶段（P3b）。

**第十期：控制柜程序区（第三阶段 B：字节码编译 + 栈式 VM + tick 调度 + 运行警告）**

- **字节码与 VM**（[ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java)，新建）：
  - **指令编码**：一条指令打包进一个 `int`——低 8 位操作码、高 24 位操作数（变量槽位或跳转目标）；浮点常量占两条 int（`OP_PUSH_F` 后紧跟 `Float.floatToRawIntBits`）；跳转操作数使用**指令下标**而非字节偏移。指令集为 `NOP / PUSH_F / PUSH_B / LOAD_F / LOAD_B / STORE_F / STORE_B / ADD_F / SUB_F / MUL_F / DIV_F / FLOORDIV_F / MOD_F / NEG_F / NOT_B / AND_B / OR_B / LT_F / LE_F / GT_F / GE_F / EQ_F / NE_F / EQ_B / NE_B / B2F / JMP / JMP_IF_FALSE / HALT`（0x00~0x1C），`BYTECODE_VERSION = 1`。
  - **编译**（`compile(lines, scope)`）：逐行生成代码，禁用行与空行跳过；条件非「真」时生成 `LOAD_B 条件槽`（`!变量` 再补 `NOT_B`）并记录 `JMP_IF_FALSE` 待回填位置，表达式后按目标类型生成 `STORE_F / STORE_B`，最后回填跳转到「本行之后」并补 `HALT`。算术 / 比较自动为布尔操作数补 `B2F`；`== / !=` 双方均为布尔时走 `EQ_B / NE_B`，否则按浮点 1e-5 容差比较；`//` 为 `floor(a/b)`，`%` 余数符号跟随除数。编译期同步产出**每条指令所属程序行**（`lineOf`），供运行警告定位行号；无有效行或指令数超过 `MAX_CODE = 2048` 时返回 `null`（不执行）。
  - **执行**（`execute(compiled, floats, bools)`）：浮点 / 布尔双栈 + 共享栈指针，执行期**零分配**；只读写传入的影子数组。除零结果记 0 并记 `DIV_ZERO`；任何非有限结果（NaN / Infinity）置 0 并记 `NON_FINITE`；同一行同一原因去重，最多 `MAX_WARNINGS = 4` 条；非法操作码或超步数直接结束，避免死循环。
  - **修复（一元运算失效）**：`NEG_F / NOT_B / B2F` 原先写成 `pushB(stackB, sp++, !popB(stackB, --sp))` 这类形式，同一表达式内混用 `sp++` 与 `--sp`，Java 参数从左到右求值使 `sp++` 先自增、`--sp` 只减回自增后的值，读到的是空槽而不是栈顶操作数——结果 `!true` 恒算出 `true`、一元取负恒为 0、布尔转浮点恒为 0.0，且每次取反都让 `sp` 多涨一格污染后续指令。改为先 `float / boolean a = pop(...(--sp))` 取出操作数，再用 `sp++` 压栈；比较与其余二元指令此前已是该写法。
- **总线调度**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：
  - 新增 `programCode`（运行时编译缓存，不持久化）、`programDirty`、`programTimer`、`shadowFloats / shadowBools` 影子数组与 `runWarnings` 列表。
  - `tick()` 改为服务端统一入口：未成型清零计数器；每 100 tick 的结构巡检保持不变；其余情况调用 `tickProgram()`。只有**完全成型且 period > 0** 时按 `programTimer` 累计到 period 执行一次，period = 0 停止但保留上次警告。
  - `runProgram()` 严格执行**影子数组三步**：① 读取——绑定组件的变量取组件实时状态（开关取 `state != 0`、仪表取 `value`），自定义变量沿用总线值，0 号槽位写入运行间隔；组件来源失效时沿用冻结值并记 `SOURCE_INVALID` 警告；② 执行——只读写影子数组；③ 提交——变量值写回总线，只有值变化的绑定组件才 `uploadVarToComponent`，变量 / 组件 / 警告任一变化才 `sync()` 一次。
  - `programDirty` 在保存程序、新建变量、删除变量、NBT 读取时置位，下次执行前重新编译；保存程序时同时清零计数器与旧警告。
- **警告同步与回显**：警告不写存档，`getUpdateTag()` 额外写入 `ProgWarn`（行号 + 原因），客户端 `load()` 解析到 `runWarnings`；[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) 在行列表底部显示「⚠ 第一条警告（+N）」，鼠标悬停弹出全部警告 tooltip；来源失效没有行号，单独提示。
- **语言资源**：新增 `gui.cc_rc.program.warn_div_zero / warn_non_finite / warn_source`（中英各 3 条），JSON 无 BOM。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十一期：组件动态显示（流量表 / 立式仪表 / 指示灯）**

- **流量表（GAUGE）**：`value` 驱动物理指针。按刻度圆弧反推出指针枢轴（模型坐标 y = 0.4375、z = -0.125，即刻度圆心），指针在 OBJ 中垂直向上烘焙，对应刻度弧的竖直端（100%）；数值 0~100 映射为 `GAUGE_START_DEG(-90°) + 0.9° × value`，0% 指向刻度弧的水平端（模型 +z，即表面左侧），100% 回到烘焙的竖直端，数值增大时指针顺时针扫动。显示范围夹在 -5~105，轻微超量程可见但不跑出表盘。
- **修复：流量表指针方向反了**：原实现用 `-0.9° × value`，0% 落在烘焙的竖直端、100% 落到水平端，表现为「数值增大时指针往 0 的方向走」，自加自减程序看上去像算错了。改为 `GAUGE_START_DEG + 0.9° × value`（删除原 `GAUGE_BAKED_VALUE`），并用中文注释写明 0% / 100% 的端点与扫动方向。
- **立式仪表（GAUGE_V）**：`value` 驱动中央竖槽内的箭头（`Needle` 分组）垂直平移；50% 居中，行程半程 0.26 格（槽体 0.18125~0.81875，箭头高约 0.12），同样夹在 -5~105。
- **指示灯（INDICATOR）**：熄灯（`state == 0`）时把所选颜色乘以暗化系数 0.38 后走 `ConsoleCompTextures` 整图重新着色，得到同色相下更暗的灯面；亮灯仍使用原色。未设置颜色时保持原贴图。暗化系数与流量表右上角暗部比例接近。
- **渲染细节**：`effectiveColor` 统一供着色贴图与可染色面顶点色使用；新增 `dimColor`、`clampDisplay` 辅助方法。所有改动只涉及 [ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)，未改动模型与贴图资源。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十二期：程序编辑体验修复（操作数取负 / 目标留空 / 前缀补全 / 行启用点击 / 候选提示）**

- **操作数取负**：`ConsoleProgramScreen.parseOperand` 由「只识别开头一个 `!`」改为循环收集开头的一元前缀 `!`（逻辑非）与 `-`（取负），再从最内侧前缀向外套用 IR 一元节点，因此 `-A`、`A + -B` 均可正确表示，且取负只作用于该操作数本身（先取负再参与二元运算）；`-!A` 解析为对 A 取反后再取负。载入已有行时 `flattenExpr` 会把这些一元节点还原为输入框文本。
- **目标默认留空**：打开空行编辑器时 `targetBox` 不再预填第一个已定义变量，改为空串，避免误写目标。
- **前缀补全不再消失**：`refreshFieldSuggest` 对操作数框先剥离开头的 `!` / `-` 前缀再匹配名称，候选文本仍保留前缀；`!` 只列布尔变量（含 `!true` / `!false`），`-` 列全部数值变量（IR 中布尔按 0/1 参与数值运算），点击候选写入带前缀的完整文本。
- **行启用点击**：新增 `checkMarkX(index, rowX)` 统一绘制与命中坐标，点击行首「✓ / ·」即可切换 `Line.enabled`（原先命中区偏左，点到的是行号）。
- **候选悬浮提示**：新增 `renderFieldSuggestTooltip`，鼠标悬停在变量候选上时显示完整变量名 + 「已绑定设备：代号（类型中文名）」或「未绑定设备」，与左侧变量表一致；`true / false` 常量不显示。
- **语言资源**：`gui.cc_rc.program.hint_operand` 中英文案补充 `- 取负`。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十三期：布尔常量显示 / 双栏同时编辑 / 默认设备名 / 整页滚动**

- **布尔常量显示为 1 / 0**：`leafText` 与 `ProgramIR.Expr.toText` 的 BOOL 分支改为输出 `1` / `0`，操作数候选也改为 `1`、`0`（`!` 前缀下为 `!1`、`!0`）；`parseOperand` 仍兼容旧文本 `true` / `false`，布尔目标行输入 `1` / `0` 仍按布尔常量解析。
- **变量栏与程序栏可同时编辑**：`mouseClicked` 中行编辑器只以 `editingLine >= 0` 为前提、变量行只以 `createSlot < 0` 为前提、程序行只以 `editingLine < 0` 为前提，去掉互相排斥条件；`keyPressed` 改为按焦点判断活动面板（`lineEditorFocused` / `nameBox.isFocused` / `valueBox.isFocused`），`openLineEditor` 结束时把焦点交给目标框，保证 Enter / Esc 作用于刚操作的面板。
- **设备默认名称同步注册名**：`ConsoleCompType` 移除写死的 `codePrefix` 字段，改由 `codePrefix()` 直接取 `itemSuffix` 大写（与 `controller_` 注册名一致）；新放置设备自动生成的代号变为 `METER_1`、`DISPLAY_1`、`PLOTTER_1`、`LAMP_1`、`BUTTON_1`、`SWITCH_1`、`LEVER_1`、`GAUGE_1`、`KNOB_1` 等，旧存档显式保存的 `Code` 不受影响。
- **变量表整页翻页**：新增 `VAR_ROWS_PER_PAGE`、`varPage`、`changeVarPage`，`clampScroll` 改为按页对齐像素偏移；滚轮在变量列表内每次翻一整页，`beginValueEdit` 自动跳到目标行所在页；移除像素滚动条。
- **程序列表滚轮翻页**：`mouseScrolled` 在程序列表区域按整页切换 `page`；变量表左上角新增 `<` / `>` 翻页按钮，与程序列表底部翻页按钮并存。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十四期：程序注释 / 双行列表 / 面板纵向拉伸**

- **注释字段**：[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java) 的 `Line` 新增 `public final String comment` 与 `MAX_COMMENT = 80`；构造函数改为 6 参数（末位 `comment`，`null` 归一为空串），`withEnabled` 复制注释；NBT 仅在注释非空时写 `Comment` 字段、读取时缺失补空串，旧存档可直接加载；网络序列化 `writeUtf(comment, 80)` / `readUtf(80)`。`ProgramVM.compile` 与执行逻辑完全不读 `comment`，注释不参与编译、校验与运行，仅在客户端与服务端之间随程序行同步。
- **注释输入框**：[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) 行编辑器在「目标」之后新增 `commentBox`（`EDIT_COMMENT_Y = 152`，样式与其余输入框一致，`setMaxLength(80)`，占位提示 `hint_comment`）；打开空行时清空、打开已有行时载入 `line.comment`，`buildLine` 提交时 `trim()` 后写入。无格式要求，任何文本都合法，也不参与 `refreshLineError` 校验。
- **列表双行显示**：行列表每行改为「上方表达式、下方注释」两行（`y + 4` / `y + 14`，注释为空时不绘制），行高 `LINE_H` 由 16 提高到 24，每页仍 8 行；禁用行的表达式与注释均用暗色。悬停 tooltip 由单行表达式改为多行 `List<Component>`：第一行表达式全文、第二行注释（非空时）。
- **面板纵向拉伸**：`GUI_H` 由 196 提高到 246，`LIST_H` 改为 192，左侧变量表每页 8 行（`VAR_ROWS_PER_PAGE = LIST_H / ROW_H`），右侧程序区 `ROWS_PER_PAGE = 8` × `LINE_H = 24` 与左侧逐行对齐；底部按钮条、运行间隔栏、行编辑器各控件纵坐标随之重排（`BTN_Y`、`PROG_BAR_Y`、`EDIT_*_Y`）。背景贴图仍为 360×196，按新高度整体纵向拉伸绘制，未新增贴图资源。
- **语言资源**：中英文各新增 `gui.cc_rc.program.label_comment`（注释 / Note）与 `hint_comment`（可选，不参与运行 / Optional, not executed），JSON 无 BOM。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十五期：程序文字语法着色**

- **配色规则**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：固定 GUI 标签（「变量表」「程序」「条件 / 目标 / 算式 / 注释」等）保持原色；语义文字按下表着色：
  - 布尔变量：蓝色，绑定设备的变量用更深一档；浮点变量：绿色，绑定设备更深一档。
  - 常量：黄色；算符（含 `=`、括号、`!`、一元 `-`、`float`）：品红；注释：灰色。
- **两套调色板**：程序列表行是浅灰底（`COLOR_LIST_*` 用深色），编辑框与悬浮提示是深色底（`COLOR_EDIT_*` 用亮色），保证两种背景下都清晰可辨；绑定判定使用 `VarDef.isBound()`。
- **列表界面**：`renderProgramRows` 不再绘制 `Line.toText` 的整串文字，改为 `lineSegments` 递归生成「条件 + 目标 + 表达式」的彩色分段，再由 `drawSegments` 逐段绘制并在行宽处截断；注释行固定用灰色。
- **编辑界面**：表达式「算符 / 操作数」框、条件框、目标框在 `EditBox.render` 之后调用 `renderBoxTokenColors` 覆盖绘制彩色文字（位置与 `EditBox` 一致：`x+4`、`y+(h-8)/2`，文字超出可视宽度时不覆盖以避免与横向滚动错位）；操作数开头的 `!` / `-` 按算符着色，其余按常量 / 变量着色；注释框直接 `setTextColor` 灰色。
- **悬浮提示**：程序行 tooltip 用 `styledLine` 把分段转成带 `TextColor` 的组件，表达式彩色、注释灰色；变量表与补全候选的 tooltip 变量名同样按类型着色；补全下拉候选项按「变量 / 常量 / 算符」分别着色（悬停仍为黄色高亮）。
- **变量表**：左侧列表的变量名按类型着色（绑定设备更深一档），固定标签与类型文字保持原色。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十六期：着色亮度调整 / 编辑框内置着色**

- **配色调整**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java#L149-L169)）：原来的蓝 / 绿整体偏深，改为「未绑定更浅、绑定设备保留原较浅色」的两档：
  - 列表（浅灰底）：布尔未绑定 `#4A7FD6`、绑定 `#1E4FA8`；浮点未绑定 `#3F9A4C`、绑定 `#1E6B2A`。
  - 编辑框 / 悬浮提示（深色底）：布尔未绑定 `#A8CCFF`、绑定 `#7FB4FF`；浮点未绑定 `#A8F0B0`、绑定 `#7FE08A`。
  - 常量、算符、注释配色不变。
- **编辑界面着色**：新增 `syncBoxTextColor`，在每帧绘制输入框前根据内容设置 `EditBox` 自身文字颜色（算符框品红并同时设置只读颜色、条件 / 目标按变量类型、操作数按常量 / 变量），使编辑界面即使覆盖绘制失效也能显示语法色；随后 `renderBoxTokenColors` 仍在同位置覆盖绘制，把操作数开头的 `!` / `-` 单独标成品红，并避免与输入框底色出现白边。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十七期：变量表左扩 / 设备图标 / 悬浮提示修正**

- **面板左扩**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java#L51-L57)）：`GUI_W` 360 → 382、`LEFT_W` 140 → 162，新增 `ICON_COL_W = 18` 设备图标列；右侧程序区宽度保持不变（`RIGHT_W` 仍 212），背景贴图按新宽度整体拉伸绘制。
- **设备图标**：变量行左侧预留 16×16 图标区（`listX + 3`、`top + 4`），绑定设备的变量用 `GuiGraphics.renderItem` 绘制 `CompEntry.type.item()` 的物品贴图，只画图标、不画物品栏底板；行内文字（槽位号 / 名称 / 类型 / 数值）统一右移 `ICON_COL_W`，数值输入框同步右移并收窄。
- **设备映射**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java#L90-L109) 新增 `item()`（枚举 → 对应 `ModItems` 组件物品）与 `registryName()`（返回 `cc_rc:controller_xxx`）。
- **悬浮提示**：变量名 tooltip 第一行仍为完整变量名（按类型着色），第二行不再重复变量名，改为「设备中文名（设备注册名）」，例如 `立式仪表（cc_rc:controller_gauge）`；未绑定时仍显示「未绑定设备」。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十八期：GUI 原版风格重做（列表 / 编辑 / 变量注册 + 按钮）**

- **参考素材**：`模型/控制台类/gui` 下的 `recipe_book.png`、`server_selection.png`、`slider.png`，取其「浅灰面板 + 黑白斜面 + 深色凹槽内容区 + 浅灰凸起按钮」的原版界面语言。
- **斜面绘制工具**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java#L1080-L1145)）：新增 `drawRaised`（外圈黑边 + 左上白高光 + 右下深灰阴影）与 `drawSunken`（外圈黑边 + 左上深灰 + 右下亮边），整块面板补上原版窗口斜面，中间分隔线改为「暗线 + 亮线」。
- **列表界面**：变量表与程序列表区域改为原版凹陷深色槽（`COLOR_LIST_BG #141414`），行底色同步改为深色系（`#232323` / 悬停 `#2F2F2F` / 空行 `#1B1B1B`），行内文字统一换用亮色（`COLOR_TEXT_LIGHT` / `COLOR_DIM_LIGHT` / `COLOR_ERROR_LIGHT`），删除按钮悬浮时显示亮红；程序行表达式与注释同步改为亮色语法着色（`lineSegments(..., true)`）。
- **编辑界面**：行编辑时右侧区域改画浅色凸起子面板，所有输入框（条件 / 目标 / 算符 / 操作数 / 注释）统一先画深色凹陷输入框底（`drawFieldBg`），保证亮色语法着色在浅灰面板上可读；标签与报错仍使用浅底深色文字。
- **变量注册界面**：新建变量时左侧列表区域改画浅色凸起子面板，名称输入框同样使用深色凹陷底，类型 / 创建 / 取消按钮统一为原版样式。
- **按钮样式**：新增内部类 `VanillaButton`，重写 `renderWidget` 直接绘制原版按钮——浅灰凸起面、禁用时压暗、悬停时内侧一圈深红描边（对应参考图选中态）、白色带阴影文字；全部 15 个按钮（保存 / 放弃 / 变量翻页 `<` `>` / 类型 / 创建 / 取消 / 程序翻页 / 保存程序 / 启用 / 条件模式 / 追加 / 确定 / 取消）改由 `vanillaButton(...)` 工厂创建。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第十九期：GUI 尺寸放宽 + 按钮样式微调**

- **尺寸与留白**：界面由 `382×246` 放大到 `416×260`，左右外边距统一为 14（`PAD_X`），与上下留白接近；变量表与程序栏的间距由 4 拉大到 14，分隔线移到两栏正中。
- **列表区高度**：左右列表区域高度由 192 增到 206，列表底部形成 14px 内边距，行数仍为每页 8 行。
- **变量翻页按钮**：从右上角标题栏移到下方按钮栏，位于程序翻页箭头左侧（`VAR_PREV_X` / `VAR_NEXT_X`）；程序翻页箭头相应右移（`PROG_PREV_X` / `PROG_NEXT_X`），页码显示在两枚程序箭头之间。
- **翻页按钮样式**：新增 `ArrowButton`（透明底 + `drawArrow` 双三角 `« / »`），平时深灰、悬停绿色、不可用时浅灰，对应参考素材 `server_selection.png`。
- **按钮文字**：`VanillaButton` 文字改为深色（常态 `#303030`、悬停 `#000000`、禁用 `#707070`）。
- **新建变量表单**：整体放宽（标题 36、标签 58、名称 70、提示 94、类型 140、按钮 186），输入控件左右各留 8px；创建 / 取消按钮居中排列并留 10px 间距。
- **行编辑界面**：启用 / 条件 / 目标 / 算式 / 注释各行间距拉开（24 / 46 / 68 / 92 起，行距 17），三个操作按钮改到算式行左侧的空白带（112 起、行距 22、间距 4），避开「算式」「注释」标签；底部报错区保留 2 行。
- **错位修正**：数值输入框宽度补上设备图标列偏移（`LIST_W - 4 - ICON_COL_W`），避免覆盖到右侧边框；补全下拉的锚点与宽度改为跟随新表单位置。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十期：按钮文字去重影 + 翻页箭头贴图化 + 变量翻页归位**

- **按钮文字去阴影**：`VanillaButton.renderWidget` 不再调用 `drawCenteredString`（该方法默认带阴影，深色文字会出现黑色重影），改为 `font.width(...)` 手工居中后用 `graphics.drawString(font, msg, tx, ty, color, false)` 无阴影绘制。
- **翻页箭头贴图化**：新增 `page_arrows.png`（由素材 `模型/控制台类/gui/按钮.png` 复制而来，64×64，四象限各 32×32：左上 / 左下 = 向前翻页常态 / 悬停，右上 / 右下 = 向后翻页常态 / 悬停）；`ArrowButton` 改为按 `u = 左/右（0 / 32）`、`v = 常态/悬停（0 / 32）` 取象限，再按象限内箭头实际包围盒（14×22，左箭头 x=9 起、右箭头 x=10 起、y=6 起）1:1 裁剪绘制，保持像素清晰；删除自绘 `drawArrow`；禁用时改用 `graphics.setColor(0.42)` 压暗后再 blit（仅影响不透明像素，不会把三角形以外的透明区域染成灰方块），绘制后立即把颜色复位为全白。
- **变量翻页归位**：`VAR_PREV_X` / `VAR_NEXT_X` 改为跟在左下「保存」「放弃」按钮右侧（14..54、62..102，箭头 118..138 / 142..162），不再位于右侧程序栏下方；程序翻页箭头仍留在右侧底部（190..210 / 244..264），页码显示在其间。
- **布局回归整行**：左右列表高度由 206 改回 192（8 × 24 正好整行，消除底部半行被裁切），底部按钮栏统一到 `BTN_Y = PROG_BAR_Y = 224`。
- **翻页可用态**：`containerTick` 中按变量总页数刷新 `varPrevButton` / `varNextButton` 的可用状态（首页禁用向前、末页禁用向后）；新建变量表单打开时隐藏两枚变量箭头，避免误触换页。
- **行编辑器横向留白**：新增 `EDIT_PAD_X = 7`，行编辑器内所有控件（启用按钮、条件模式按钮 / 变量框、目标框、注释框、算符 / 操作数框、三个操作按钮）左右各内缩 7px，不再贴住右侧面板凹槽边框；标签、报错换行宽度与补全下拉的左右边界同步内缩。
- **静态背景烘焙进贴图**：`console_program_gui.png` 重制为 `416×260`（与界面 1:1，不再拉伸采样）：灰色底面 `#C6C6C6`、窗口左上白色高光 / 右下 `#555555` 阴影、中间装饰分隔线（`x=176` 暗线 + `x=177` 亮线，`y=4..256`）。`renderBg` 删除原先的 6 条斜面 / 分隔线 `fill`，只保留底板 `blit` 与状态相关的列表凹槽；变量 / 程序列表的深色凹槽及浅色表单子面板因会随状态切换，仍由代码绘制（贴图可直接精修）。
- **保存即关闭**：左下「保存」按钮改为直接调用 `onClose()`（`onClose` 内部会保存变量表，避免重复提交）；右侧「保存」按钮的 `saveProgram()` 改为返回 `boolean`，保存成功后调用 `onClose()` 关闭界面，被锁定或总线缺失时保持打开。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十一期：圆盘记录仪历史采样 / 数字显示屏小数位与单位**

- **变量绑定能力**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：`supportsFloatVariable()` 加入 `GRAPH`（圆盘记录仪），使其与仪表 / 数字显示屏一样按单个浮点值绑定；新增 `supportsHistory()`，仅 `GRAPH` 返回 true，用于区分「每次写入都记录一笔历史」的组件。
- **CompEntry 扩展**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `float[] history`（`HISTORY_LEN = 12`，索引 0 为最新，其它类型为 null）、`int decimals`（`DEFAULT_DECIMALS = 2`、`MAX_DECIMALS = 6`）、`String unit`（`MAX_UNIT_LEN = 8`）；构造函数扩为 13 参，原 10 参版本委托为默认值，避免改动全部旧调用点。
- **不可变复制工具**：新增 `copyWith(base,state,value)` / `copyWithPos(base,u,v)`，替换 `withFloatValue` / `withBoolValue` / `applyCompUse` / `releaseExpiredKeypads` / `rebaseSurfaceEntries` / `applyCompText` 中 6 处手工 new，确保扩展字段（历史、显示设置）在任何状态下都不会被清空。
- **历史移位采样**：新增 `withHistorySample(comp, value)`，新建等长数组后 `System.arraycopy(history, 0, copy, 1, len-1)` 整体后移一位、把新值写到索引 0，与独立方块 `DigitalPlotterBlockEntity` 的移位规则一致；非有限值归一为 0。
- **每轮都记录**：`runProgram` 提交段保留「变量值变化才写回」，但新增 `bindsPlotter(VarDef)` 判定——绑定组件为圆盘记录仪时，无论返回值是否变化，每轮程序执行结束都调用 `uploadVarToComponent`，因此每个计算周期都会产生一个新的历史采样点（先移位再填入返回值）。
- **显示设置同步**：`applyCompEdit` 签名扩展为 `(id, code, u, v, color, decimals, unit)`，仅 `NUMITRON` 采用新的小数位 / 单位（夹取 0~6、`trim`、截断 8 字符），其它类型沿用原值并保留 `history`；`ConsoleCompMenu`、`ConsoleCompItem`、`ConsoleCompPacket`、`ConsoleDragClient` 全线补传 `decimals` / `unit`。
- **NBT 持久化**：`CompEntry.save()` 用 `ListTag + FloatTag` 保存 `History`（1.20.1 无 `putFloatArray`），非默认时才写 `Decimals`、非空时才写 `Unit`；`load()` 中 `GRAPH` 固定新建 12 长度数组并按 `Tag.TAG_FLOAT` 读回，缺字段走默认值，兼容旧存档。
- **组件设置界面**（[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)）：背景高度 152 → 174、按钮行下移到 148，仅当类型为 `NUMITRON` 时新增「小数位」（1 字符）与「单位」（8 字符，可留空）两个输入框；提交时解析并随包发送，删除组件时也带上原值。
- **读数渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleBusBER.java)）：渲染顺序改为「组件模型 → 组件读数 → 附带文字」，读数在模型批次之后统一绘制以免打断贴图批次导致染色失效；`drawText` 末尾新增颜色参数。
  - 数字显示屏：`String.format("%.<decimals>f", value) + unit`，按 `font.width` 自动缩小（上限 0.25、可用宽 11.5 结构像素），颜色取组件染色，未设置时白色。
  - 圆盘记录仪：以「█」字符按列画柱状趋势，`history[0]` 放最右列，柱高按 0~100 映射 4 结构像素、每列宽 1 像素，默认红色，最新值自然从右侧滚入。
- **GUI 贴图**：`console_comp_gui.png` 由 176×152 纵向延长到 176×174（复制原边框行，保持 1px 描边、左上百高光、右下 `#555555` 阴影与圆角透明），不重绘整图。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十二期：圆盘记录仪曲线细化（50 点 / 亚像素）**

- **采样点数**：`CompEntry.HISTORY_LEN` 由 12 改为 50，与单独实现的数字圆盘记录仪（`DigitalPlotterBlockEntity.DATA_LENGTH = 50`）的宽度方向点数一致；旧存档的 12 点历史读取时自动补齐到 50 点（缺失位置为 0）。
- **像素变细**：`renderPlotHistory` 不再按整像素堆叠方块，改为一个采样点画一条细柱——列宽 `PLOT_WIDTH(11.8) / 50 ≈ 0.236` 结构像素，整条曲线铺满屏幕内宽（12 像素）。
- **高度连续**：柱高按 `value / 100 * PLOT_MAX_HEIGHT(3.8)` 连续计算（亚像素），不再用 `Math.round` 分档成 0~4 像素，因此数值变化能被平滑表现出来；最大高度 3.8 像素比单独记录仪的约 7.7 像素略压缩，仍完整落在 4 像素高的屏幕内。
- **非等比缩放**：`ConsoleBusBER` 新增 `drawTextScaled(...)`（水平 / 垂直可分别给缩放），`drawText` 改为等比例委托；圆盘记录仪用 `scaleU = 列宽 / 8`、`scaleV = 柱高 / 8` 把「█」字形拉伸成细柱。
- **基线对齐**：垂直下对齐时原版按 `lineHeight(9)` 而非字形高(8) 偏移，字形底边会比锚点高 1 个字体像素，绘制时把锚点下移同样的 `scaleV`，使柱底正好压在 `PLOT_BASE_V` 基线上、柱顶不越过屏幕上沿。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十三期：记录仪曲线位置 / 单线样式 / 读数贴合凹面**

- **位置修正**：第二十二期改写时漏加组件锚点，导致曲线画到了控制台结构中心；`renderPlotHistory` 的 `u` / `v` 恢复为 `comp.u + ...` / `comp.v + ...`，曲线重新落在组件屏幕上。
- **单线趋势**：不再从基线堆满到数值的实心柱，改为每列只画一个点（`PLOT_LINE_THICK = 0.4` 结构像素高），50 个点横向相连成一条细趋势线，线下方保持屏幕底色。
- **读数外推减半**：显示器 / 记录仪的屏幕在模型中间是凹进去的（内屏约在模型厚度一半处），`screenOutset` 由「最外框厚度」改为「一半厚度 + 极小偏移」，读数与数码管文字不再悬浮在组件最外侧之前，而是贴近凹陷的内屏表面。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十四期：立式仪表刻度标定 / 指示灯默认熄灯 / 仪表放大 / 按钮弹起时间**

- **立式仪表刻度对齐**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：按截图逐刻度测量（100→0 在模型中约占 0.64 格），`GAUGE_V_HALF_TRAVEL` 由 0.26 改为 0.32，数值 0 / 100 时箭头正好落在刻度弧两端；居中点仍为 50%，无需额外偏移。
- **指示灯默认模型熄灯**：未设置颜色时直接用原贴图，无法像着色贴图那样通过 `dimColor` 变暗，导致看起来永远开灯；新增 `INDICATOR_OFF_GRAY = 97`（255 × `INDICATOR_OFF_FACTOR`），`state == 0` 且未设置颜色时只把灯面（OBJ 的 `Light` 分组）的顶点色压暗，外壳 `Base` 保持原样。
- **仪表放大**（[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：`GAUGE_V` 与 `GAUGE_ARC` 的 `renderScale` 0.3 → 0.45（约 1.5 倍）；命中检测、重叠判定与附带文字定位随 `halfWidthPx / halfHeightPx` 自动跟随。
- **按钮弹起时间可配置**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `holdTicks`（`DEFAULT_HOLD_TICKS = -1` 跟随配置文件、`MAX_HOLD_TICKS = 200`），构造函数扩为 14 参、13 参版本委托默认值；`copyWith` / `copyWithPos` / `withHistorySample` / `applyCompText` 均保留该字段；NBT 仅在非默认时写 `HoldTicks`，`load()` 夹取 0~200 并兼容旧存档。
- **按下逻辑**：`applyCompUse` 的 `KEYPAD` 分支改为「组件单独设置优先、-1 时取 `Config.getConsoleButtonHoldTicks()`」，并用 `Math.max(1, hold)` 保证设为 0 时也能产生一次可见的按下（下一 tick 由 `releaseExpiredKeypads` 清零，即松手立刻弹起）；`applyCompEdit` 签名扩展为 `(id, code, u, v, color, decimals, unit, holdTicks)`，仅 `KEYPAD` 采用新值。
- **同步链路**：`ConsoleCompMenu`（写入 / 读取）、`ConsoleCompPacket`（编解码）、`ConsoleCompItem.openCompMenu`、`ConsoleDragClient` 全线补传 `holdTicks`。
- **组件设置界面**（[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)）：仅 `KEYPAD` 在原有「小数位 / 单位」那一行显示「弹起时间」标签与输入框（复用 `DISPLAY_Y`），默认展示有效值（`-1` 时取配置文件值），输入 0 表示松手立刻弹起，提交时夹取 0~200。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十五期：按钮「按住直到松手」语义**

- **问题**：弹起时间设为 0 时，按住右键原版每 4 tick 重复触发一次 use，每次按下只维持 1 tick，表现为「不断弹起又按下」。
- **服务端状态**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `CompEntry.HOLD_UNTIL_RELEASE = -1L`，`applyCompUse` 的 `KEYPAD` 分支在 `effectiveHoldTicks() <= 0` 时把 state 置为该标记（不再记录弹起时刻），重复 use 因 `newState == existing.state` 直接返回，天然幂等、不再抖动；定时模式仍记录 `gameTime + hold`。
- **按下判定集中**：新增 `CompEntry.effectiveHoldTicks()`（-1 跟随配置）与 `isKeypadPressed(gameTime)`（state 0 = 未按下、-1 = 按住直到松手、其它为弹起时刻），`writeBackToVar` 与 `ConsoleBusBER` 的按下动画统一改用它。
- **到点弹起**：`releaseExpiredKeypads` 跳过 `HOLD_UNTIL_RELEASE`（不计时），只处理定时模式；新增 `releaseKeypad(id)` 供客户端松手时清零 state 并回写变量。
- **客户端释放**：新增 [ConsoleKeypadClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleKeypadClient.java)（CLIENT 事件订阅）——`ConsoleCompUse` 在客户端命中「弹起时间为 0」的按钮时经 `DistExecutor` 记录该按钮；每 tick 检测右键是否松开 / 打开界面 / 离开世界，满足即发释放包并清本地状态；切换到另一个按钮时先释放上一个。
- **网络**：新增 [ConsoleKeypadReleasePacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleKeypadReleasePacket.java)（busPos + compId，服务端做 64 格距离校验），在 `ModNetwork` 注册为 id 17。
- **界面提示**：组件设置界面的「弹起时间」tooltip 改为「0 = 按住期间一直按下、松开右键才弹起」。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十六期：组件可变大小（组件缩放）**

- **语义**：实际大小 = 「组件缩放」×「模型默认缩放」。`ConsoleCompType.renderScale` 从此定位为**模型默认缩放**（以后调「默认大小」只改它）；玩家可编辑的 `CompEntry.scale` 所有组件放置时默认 `1.0`。
- **数据**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增常量 `DEFAULT_SCALE = 1.0F` / `MIN_SCALE = 0.25F` / `MAX_SCALE = 4.0F` 与 `public final float scale`；构造函数扩为 15 参（14 参版本委托默认 1.0）；`copyWith` / `copyWithPos` / `withHistorySample` / `applyCompText` 全部保留该字段（修掉了历史采样时缩放被重置为 1.0 的问题）。
- **辅助方法**：新增 `effectiveScale()`（= `type.renderScale × scale`）、`halfWidthPx()` / `halfHeightPx()`（结构像素半宽/半高），交互与渲染统一走这里，保证「看到多大就能点多大」。
- **交互判定**：`findCompAt` 用 `halfWidthPx / halfHeightPx` 判定命中；`overlapsOtherComp` 增加 `float scale` 参数，按双方实际半宽/半高判重叠（`applyCompAdd` 传默认 1.0，`applyCompEdit` 传新值）；拖动端的 `ConsoleDragClient.overlapsOtherComp` 同步改为组件实际半宽/半高。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：OBJ 矩阵缩放由 `type.renderScale` 改为 `comp.effectiveScale()`；附带文字定位（按钮右侧 / 默认下方 / 指示灯外推）改用 `halfWidthPx / halfHeightPx / effectiveScale`；`screenOutset` 随实际缩放；数码管读数与圆盘记录仪趋势线额外乘 `comp.scale`（横向步长、线宽、高度、字号同步缩放）。
- **服务端校验**：`applyCompEdit` 签名扩展 `float scale`，非有限值回退 1.0 并夹取 0.25~4.0，用 safeScale 做重叠判定；NBT 仅在非默认时写 `Scale`，`load()` 读取后夹取，旧存档缺失按 1.0 处理。
- **同步链路**：`ConsoleCompMenu`（写入 / 读取）、`ConsoleCompPacket`（编解码）、`ConsoleCompItem.openCompMenu`、`ConsoleDragClient` 全线补传 `scale`。
- **组件设置界面**（[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)）：位置行收窄（X 22..58 / Y 68..104），右侧新增「缩放」标签与输入框（124..160），默认 `1.0`，范围 0.25~4.0，tooltip 说明「实际大小 = 该值 × 模型默认缩放」；提交与删除均带上该值。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十七期：拖动组件吸附完整像素**

- **吸附取整**（[ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java)）：多功能工具拖动组件时，目标锚点 `nu / nv` 先按 `Math.round` 取整，组件只会落在整数结构像素坐标上；表面文字拖动保持原有的亚像素跟随，不受影响。
- **边界同步取整**：组件取整后再按 `ceil(-halfW) ~ floor(halfW)`（垂直同理）做范围夹取，避免夹取把取值又拉回小数、出现半像素位置。
- **首发判定**：`begin()` 抓取时把 `sentU / sentV` 记录为目标当前锚点（而非点击点），吸附取整后位置一有变化就会发包，不会因为点击点与锚点存在抓取偏移而漏发第一次更新。
- **精细调节**：仍通过组件设置界面直接输入 X / Y 坐标（支持小数）。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十八期：组件附带文字随组件缩放**

- **字号跟随**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：`renderCompText` 传入的字体缩放由「配置默认文字大小」改为「配置默认文字大小 × `comp.scale`」，组件放大 / 缩小时附带文字一起变大变小。
- **间距跟随**：按钮右侧、默认下方的 `COMP_TEXT_GAP` 同样乘以 `comp.scale`，避免组件放大后文字紧贴、缩小后文字离得过远。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第二十九期：控制柜辅助计算机支持水平四向放置**

- **状态属性**（[ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java)）：由普通 `Block` 增加 `FACING = BlockStateProperties.HORIZONTAL_FACING`（默认 NORTH），`createBlockStateDefinition` 注册该属性，`getStateForPlacement` 取 `context.getHorizontalDirection().getOpposite()`，即正面朝向玩家、只允许 x± / z± 四个水平朝向。
- **模型旋转**（[controller_aux.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/blockstates/controller_aux.json)）：blockstate 由单一 variant 改为 `facing=north / south(180) / east(90) / west(270)` 四个变体，与 controller_bus 的写法一致；模型默认正面为 north，因此旋转后屏幕面始终朝外。
- **兼容性**：辅助计算机没有方块实体，朝向只影响模型显示；总线六邻扫描与程序界面逻辑完全不变。旧存档缺少 `facing` 时自动取默认 NORTH。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十期：控制柜总线 CC 接口（变量 / 组件值读写，批量结算）**

- **外设注册**（[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java)）：在 `ForgeComputerCraftAPI.registerPeripheralProvider` 中新增 `ConsoleBusBlockEntity → ConsoleBusPeripheral`，外设类型 `controller_bus`；电脑用调制解调器接到控制柜总线即可调用。
- **线程模型**（[ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java)）：所有 Lua 方法均**不标注 mainThread**。Lua 线程只读写 volatile 快照与加锁的待处理队列，零 Level 访问、0 tick；主线程每 tick `drain()` 一次，按调用顺序批量应用并最多同步一次，因此连续多次读写不会每个调用消耗 1 tick。
- **读己之写**：写操作同时记录到 overlay，读取优先返回 overlay；overlay 与待处理队列在同一把锁内随 `drain()` 一起清空，既能看到刚写入的值，又不会永久遮蔽程序或玩家的修改。
- **执行顺序**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`tick` 中依次执行「按钮弹起 → 辅助计算机 `tickProgram()` → `applyCcOps()` → `publishCcSnapshot()`」。程序与 CC 同时写同一变量时，CC 的写入在程序之后结算；变量写入后立即上传绑定组件（圆盘记录仪记一笔），组件直接写入最后生效，全部操作结算完最多同步一次。结构未成型时仍可 CC 读写已有变量 / 组件，只是程序不运行。
- **变量接口**（[ConsoleBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusPeripheral.java)）：`listVars()`（name / type / address / component）、`getVar(name)`、`setVar(name, value)`（bool / number 类型校验，0 号运行间隔不可写）、`getPeriod()`（只读）。
- **组件接口**：`listComponents()`（code / type / id / kind）、`getComponent(code)`（value / state / decimals / unit）、`getComponentValue(code)`、`setComponentValue(code, value)`；按钮面板为只读按压状态，写入返回 false。代号不区分大小写。
- **组件接口支持附带变量名**（[ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java#L263-L310)）：`getComponentValue` / `setComponentValue` 的 `code` 不是组件代号时，回退到变量名（`findAttachedVar` 仅放行快照 `compCode` 非空的「已绑定组件变量」），因此可直接用 `getComponentValue("SYNCHROSCOPE_1_s")` 读转速、`setComponentValue("SYNCHROSCOPE_1_s", 5)` 写转速、`getComponentValue("SYNCHROSCOPE_1_b")` 读灯状态；组件代号优先，灯状态等只读输出仍会在下一 tick 被实际判断覆盖；未绑定的普通变量不会被组件接口暴露（仍需 `getVar` / `setVar`）。
- **圆盘记录仪**（组件类型 `graph`，参考数字圆盘记录仪）：`pushPoint(code, value)`（0~100 钳制、整体移位、返回写入值）、`setPoint(code, index, value)`（Lua 索引 1~50，1 = 最新）、`getPoint(code, index)`、`getPoints(code)`（长度 50）；历史数组只换引用不原地修改，快照与 Lua 读取共享安全。
- **说明书**（[InstructionBook2Item.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/InstructionBook2Item.java)）：目录新增「6. 控制柜总线」，补充 4 页 Lua 方法说明与示例。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十一期：圆盘记录仪双名称与条件推送**

- **组件第二名称**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `code2`，仅圆盘记录仪（`supportsHistory()`）使用；新建组件时自动生成 `<主代号>_T`（重名时追加序号），可清空。NBT 仅在非空时写 `Code2`，旧存档按空处理；所有复制 / 编辑路径（`copyWith` / `copyWithPos` / `withHistorySample` / `withPoint` / `applyCompText`）均保留该字段。
- **触发变量**：`VarDef` 新增 `trigger`。玩家注册变量时若名称等于某记录仪的第二名称，则强制绑定该组件的 `trigger` 布尔变量（NBT `Trigger`）；主代号仍绑定原有浮点变量。代号判重（`codeUsed`）同时比较主代号与第二名称，新增 `nameTakenByOtherVar` 防止与其它设备的绑定变量重名。
- **tick 顺序**（`tick`）：`releaseExpiredKeypads()` → **`clearPlotTriggers()`** → `tickProgram()` → `applyCcOps()` → **`flushPlotters()`** → `publishCcSnapshot()`；未成型 / 结构失效分支同样先清零再结算，避免残留 true 误推送。
- **推送语义**：触发变量每 tick 开始清零；程序本 tick 赋 true 或 CC 显式 `pushPoint` 时，`flushPlotters()` 才按该记录仪的数值变量补记一笔。程序 / CC 的普通变量写入不再无条件给记录仪记点（`runProgram`、`applyCcOps` 去掉 `bindsPlotter` 分支）。
- **角色隔离**：`uploadVarToComponent` 跳过触发变量；`findVarSlotByComp(compId, trigger)` 按角色过滤，组件状态回写（`writeBackToVar`）只作用于数值变量。
- **组件编辑界面**（[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) / [ConsoleCompMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompMenu.java) / [ConsoleCompPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompPacket.java)）：记录仪新增「第二名称」输入框，菜单与网络包同步 `code2`；拖动组件时原样回传，不会清空。
- **变量注册界面**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：补全候选用 `DeviceOption(设备, 名称, 类型, 是否触发)` 表示，记录仪同时列出主代号（浮点）与第二名称（布尔），悬停提示标注「推送触发」。
- **删除组件后的名称回收**：删除组件时 `unbindVarsFromComp` 将其变量解除绑定（变量名 / 类型保留、`trigger` 清除）；再次注册同名设备时，`nameBlocked` 只把「仍绑定在其它现存组件」或「同名变量类型不符」视为冲突，随后 `relinkVarToComp` 把同名同类型变量自动重连到新组件。`applyCompAdd`（自动代号）与 `applyCompEdit`（改名）都走同一套回收逻辑，类型不同则返回 `COMP_DUP_CODE` 阻止创建。
- **修正记录仪误推送**：`uploadVarToComponent` 对圆盘记录仪只镜像「当前值」（`withFloatValue`），不再移位历史；历史采样只由 `flushPlotters`（触发变量为 true）或 CC `pushPoint` 写入。`runProgram` 读取阶段也不再从记录仪回读组件值（数值变量为唯一数据源），避免变量与组件值来回覆盖触发假推送。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十二期：控制柜配置保存 / 加载（JSON 文本文件）**

- **入口**（[ConsoleScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleScreen.java)）：总线界面由 150 加高到 172，新增一个通栏「保存/加载配置」按钮（未成型时禁用）；打开 [ConsoleConfigScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleConfigScreen.java) 后，界面内再分「保存配置」「加载配置」两个按钮（Esc 返回总线界面）。
- **文件格式**（[ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java)）：`format = "cc_rc_console_config"`、`version = 1`，Gson `setPrettyPrinting` 输出可直接用记事本编辑的 JSON。内容包含：`size`（x/y/z）、`facing`（总线相对结构的朝向，`Direction.getName()`）、`components`（type/face/u/v/code/code2/color/text/decimals/unit/holdTicks/scale）、`texts`（face/u/v/content/color/粗斜下删线/anchor/scale）、`variables`（name/type/trigger/component/value/bool）、`program`（period + lines：row/enabled/cond/condition/target/expr/comment）。表达式以结构化节点保存（`op` + `number`/`flag`/`var`/`left`/`right`），变量与组件统一用**名称/代号**引用，不写内部槽位与组件 id，因此可由人手改文件。
- **客户端读写**（[ConsoleConfigStore.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleConfigStore.java)）：保存到游戏目录 `cc_rc_configs/<名称>.json`（UTF-8，自动建目录）；文件名过滤 `\ / : * ? " < > |`，限制 48 字符，列表按名称排序。
- **导出**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `hasConfigData()` / `exportConfigJson()`，客户端依赖 `getUpdateTag()` 的完整同步数据直接组装 JSON；不写 state/value/历史点，程序只导出非空行并记录原行号。
- **服务端校验应用**：新增 `applyConfigJson(String)`，校验顺序为——方块实体类型与距离（包内 256 格）→ 结构与总线交汇点存在且为「部分成型」→ JSON 解析（异常返回 `CFG_BAD_JSON`）→ `format` + `version`（缺 format 视为非法，避免误加载任意 JSON）→ 朝向合法性 + 旋转角计算（竖直朝向返回 `CFG_FACING_MISMATCH`）→ `size` 按旋转后（奇数次 90° 时 X/Z 互换）与当前结构比对（`CFG_SIZE_MISMATCH`）→ 变量数不超过槽位上限（`CFG_LIMIT`）→ 逐项构建组件/文字/变量/程序（任何非法返回 `CFG_INVALID`）。全部通过后才整体替换现有数据并 `sync()`，避免「改到一半失败」留下半套配置。组件按现有规则校验代号格式、代号/第二名称共用命名空间去重、坐标按该面实际像素范围夹紧、同面重叠检测；变量按名称重新分配槽位、`trigger` 必须绑定圆盘记录仪第二名称且变量名必须等于该代号、类型强制跟随所绑组件、未绑定变量不得占用设备名称；程序行号 0~63 且不重复、条件/目标/表达式按变量名解析后 `line.validate()` 必须通过、按钮面板不可作为写入目标。
- **网络**（[ConsoleConfigUploadPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleConfigUploadPacket.java) / [ConsoleConfigResultPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleConfigResultPacket.java)）：C2S 上传 JSON（上限 1 MiB，按 UTF-8 字节串 + int 长度直写，绕开 `writeUtf` 编码后 32 KiB 的限制），服务端处理后用 S2C 回传结果码；结果包不在网络包内直接引用客户端 GUI，沿用 `PENDING` + `DistExecutor.unsafeRunWhenOn` + [ConsoleConfigClientHandler.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleConfigClientHandler.java) 的模式。两者在 [ModNetwork.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ModNetwork.java) 注册为 id 18 / 19。
- **旋转加载**：新增 `rotationSteps()` / `rotateFace()`。保存朝向与当前总线朝向的差值按顺时针 90° 步数计算；结构尺寸互换由该差值决定（奇数次交换 X/Z），组件与文字的 `face` 随之旋转，局部 `u/v` 因为随表面坐标系一起旋转而保持不变（顶/底面在绕 Y 旋转时朝向不变）。
- **覆盖确认**：加载时若总线已有组件 / 文字 / 变量 / 程序（`hasConfigData()`），界面先切到「确认覆盖 / 取消」；保存时若同名 `.json` 已存在（`ConsoleConfigStore.exists()`）也先确认，避免误覆盖手工编辑过的文件。确认按钮按待处理动作（保存文件名 / 待上传 JSON）分派。
- **文案**：两份 lang 新增 `gui.cc_rc.console.config`（保存/加载配置）以及 `gui.cc_rc.console.config.save` / `gui.cc_rc.console.config.load`。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十三期：RGB 颜色控制器（控制柜组件）**

- **注册**（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) / [ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java)）：新增 `controller_rgb`（`ConsoleCompItem(RGB_COLOR)`）与枚举 `RGB_COLOR("rgb_color", "rgb", 14×14, recolorable=false, renderScale=0.5)`（默认模型缩到线性 1/2、面积约 1/4，命中框与附带文字定位随缩放同步缩小）；中英 lang 补 `gui.cc_rc.console.comp.rgb_color` / `item.cc_rc.controller_rgb`；新增物品模型 `models/item/controller_rgb.json`、表面模型 `console_parts/rgb_color_surface.obj`（源自 `模型/控制台类/RGB颜色控制器/RGB颜色控制器.obj`，坐标按 `(x,y,z)→(-z,y,x)` 变换保持绕序，`Lamp` 分组标 `usemtl tint`，因此**只有灯面随颜色变化，背板保持原色**）与贴图 `console_parts/rgb_color.png`。
- **数据字段**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `links`（连接的组件代号，`;` 分隔）、`rgbR/rgbG/rgbB`（三个通道变量名）、`showOriginal`（显示原色 = 停用）；NBT 仅在非空 / true 时写 `Links/RgbR/RgbG/RgbB/ShowOriginal`，复制 / 编辑 / 历史采样等路径全部保留新字段。
- **变量绑定**：放置时自动生成默认代号（`RGB_1`、`RGB_2`…，与其它组件一致，代号只用于注册与变量绑定，不参与显示）；主名称对应 **BOOL**「启用」变量（`supportsBoolVariable` 已含 RGB），三个通道名各对应一个 **FLOAT** 变量。`applyCompEdit` 规范化连接串（按 `;` / `；` 拆分，去空 / 自身 / 重复，最多 32 个）与通道名（格式合法、互不相同、不与主代号冲突），随后 `syncRgbVars` 解除不再使用的绑定并为通道名自动占用空槽创建 FLOAT 变量；`applyVarCreate` 按名称识别 RGB 通道名并强制浮点类型。
- **结算**（`tick` 成型路径，`flushPlotters` 之后）：**仅完全成型（FULL）时**执行 `applyRgbControllers()`——部分成型（PARTIAL，可编辑）保留手动设置的颜色 / 显示原色开关，不再被 tick 覆盖。逐个判定启用：存在同名 BOOL 变量时以变量值为准，否则用 `!showOriginal`；启用且至少一个通道变量有值时按 `R<<16|G<<8|B` 计算颜色写入自身 `color`（仅灯面染色），并把同色写入 `links` 中能按代号找到的组件；停用（显示原色）时 `color` 写为 `NO_COLOR`，连接组件保持当前颜色（**不会**强制变回原色）；启用但没有任何通道变量时保留当前颜色（手动设置的灯色）。有变化时 `sync()`。
- **渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：RGB 控制器**配置了附带文字**（文本编辑器点击组件设置）时跳过模型渲染，改在组件位置按当前灯色绘制该附带文字（只保留粗/斜/下划线/删除线样式，颜色统一取灯色；未着色时白色）；**没有附带文字时正常显示模型**（与是否填写代号无关）。`recolorable=false` 使整图不重新着色，仅 `usemtl tint` 的灯面按顶点颜色着色。成型前（PARTIAL）「显示原色」开关直接决定是否着色，成型后只看服务端结算写入的 `color`。命中检测仍按组件矩形。
- **界面**（[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) / [ConsoleCompMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompMenu.java) / [ConsoleCompPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleCompPacket.java)）：RGB 使用专用加高布局（176×212，背景 `textures/gui/console_comp_gui_tall.png`，由标准背景纵向拉伸中部得到）——保留「代号（组件注册名 = 默认 BOOL 变量名）/ X / Y / 缩放 / 染料色块 / HEX / 预览块」这些标准选项（用于设置组件名称与当前灯色），下方追加「连接」输入框、R / G / B 三个输入框、「显示原色：是/否」切换按钮；RGB **不提供单独的「原色」重置按钮**（颜色值需保留，只由「显示原色」决定是否着色）。完成 / 删除按钮随界面下移。菜单与网络包增加 `links/rgbR/rgbG/rgbB/showOriginal` 字段并全线传递。
- **容错**：通道名格式非法 / 与主代号相同 / 三通道重名时**只丢弃该通道**（不再拒绝整次编辑，颜色与「显示原色」照常保存）；客户端提交前会先校验并在聊天栏提示（避免填错名字后"变量没出现"却无任何反馈）。`ensureChannelVar` 会把未绑定 / 本组件名下的同名变量重新登记为 FLOAT（可把此前误建成布尔的同名变量改成浮点）；完全成型 tick 时若发现通道名对应的浮点变量缺失会自动补建。
- **变量候选**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：AUX 程序界面的「新建变量」补全下拉增加 RGB 控制器的通道名候选（FLOAT），`matchedDevice` 同样识别通道名；主名称仍按 BOOL 候选显示，未命名的控制器不产生空名候选。
- **创造栏 / 物品贴图**（[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java)）：把 `controller_rgb` 加入本模组创造物品栏（弧形仪表之后）；物品贴图 `textures/item/controller_rgb.png` 换用 `模型/控制台类/RGB颜色控制器/sheet_front.png`（原贴图误用了旋钮开关的截图）。
- **配置 JSON**（[ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java) 与 `exportConfigJson` / `applyConfigJson`）：Component 增加 `links/rgbR/rgbG/rgbB/showOriginal`；导出 / 加载支持 RGB 空代号，加载时校验通道名互不相同、不与组件代号冲突，并按名称把变量补绑到控制器（控制器无名称时也能接上通道变量）。
- **手动保存回写变量**：`applyCompEdit` 在 RGB 分支内 `syncRgbVars` 之后调用新增的 `writeRgbCompToVars`——把设置界面选定的灯色按 `(color>>16)&0xFF / (color>>8)&0xFF / color&0xFF` 写入 R/G/B 三个已注册 FLOAT 变量，并把 `!showOriginal` 写入启用 BOOL 变量。因此部分成型时手动配置颜色 / 开关并点「完成」，变量表里立刻就是该颜色（未着色时不覆盖通道变量），计算机读到的与界面显示完全一致。
- **CC 接口**（[ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java) / [ConsoleBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusPeripheral.java)）：新增 `OP_RGB_COLOR`（key = 组件代号，number = 0xRRGGBB）与 `CompInfo.color/enabled` 快照字段、`rgbColorOverlay`（读己之写）；外设新增 `getColor / setColor`（直接 0xRRGGBB，与 CC `colors` 表兼容）、`getChannels / setChannels`（0~255 三通道）、`setChannel`（单独改 r/g/b/red/green/blue）、`getEnabled / setEnabled`（启用 = 不显示原色，复用 BOOL 通道）；`applyCcOps` 中 RGB 的 `OP_COMP_BOOL` 特判为启用开关（`setRgbEnabled`），`OP_RGB_COLOR` 经 `applyRgbColor` 同时写通道变量与组件灯色；`getComponent(code)` 对 RGB 额外返回 `color` 与 `enabled`。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十四期：阀门拉杆（lever_f）与旋转按钮（button_x）**

- **注册**（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) / [ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) / [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java)）：新增 `controller_lever_f`（`LEVER_F("lever_f", 10.5×14, recolorable=false, renderScale=0.3)`）与 `controller_button_x`（`BUTTON_X("button_x", 14×14, recolorable=false, renderScale=0.3)`），均加入本模组创造栏；中英 lang 补 `gui.cc_rc.console.comp.lever_f`（阀门拉杆 / Valve Lever）、`gui.cc_rc.console.comp.button_x`（旋转按钮 / Rotary Button）与 `item.cc_rc.controller_lever_f` / `item.cc_rc.controller_button_x`；物品模型 `models/item/controller_lever_f.json`、`controller_button_x.json`，物品贴图 `textures/item/controller_lever_f.png`、`controller_button_x.png`（取自 `模型/控制台类/…/物品贴图.png`）。
- **模型资源**（`models/console_parts/`）：`lever_f_surface.obj`（开）/ `lever_f_close_surface.obj`（关）——两组 `o Panel`（背板）+ `o Handle`（把手），把手的 y/z 区间不同即两档角度，`Panel`/`Handle` 上的红色面标 `usemtl tint`；`button_x_surface.obj`——`o Body`（背板）+ `o Rotate`（可旋转中间件，烘焙为「按下」态），背板黄色面标 `usemtl tint`。三张贴图 `textures/block/console_parts/lever_f.png` / `lever_f_close.png` / `button_x.png` 为 64×64，右下预留 4×4 纯白块供染色采样。
- **动态染色采样点**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：`ObjMesh` 新增 `tintU/tintV`，由 `findTintUv()` 在加载时用 `NativeImage` 读贴图，按全部 `usemtl tint` 面的 UV 包围盒建占用表，自右下向左上扫描 4×4→2×2 的全白未占用块，取块中心 UV；找不到（或读图异常）回退旧常量 `495.5/512, 55.5/512`。因此旧的 512×512 贴图（旋钮开关 / 拉杆开关 / 立式仪表 / 弧形仪表）行为完全不变，新 64×64 贴图也能正确定位白块。[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) 删掉写死的 `TINT_U/TINT_V`，改用 `mesh.tintU/tintV`。
- **关态模型 / 贴图回退**：`ConsoleCompModels.load()` 对 `closed` 状态优先尝试 `<id>_close_surface.obj` 与 `<id>_close.png`（存在才用，否则退回默认），`meshFor()` 的关态判定加入 `LEVER_F`（`state == 0` → 关）。阀门拉杆因此直接切换两套把手角度，不需要运行时旋转。
- **旋转按钮复位动画**：`Rotate` 分组在**未按下**时绕面板法线轴（枢轴 `y = z = 0.5`）顺时针旋转 90°，顶点 `(y, z) → 枢轴 + (z_rel, -y_rel)`、法线同步 `(ny, nz) → (nz, -ny)`（渲染矩阵第 2 列取 `-U`，观察者视角的「右」为 `-z`，故该式即视角顺时针）；按下期间保持模型烘焙的「按下」态，松手 / 定时弹起后自动复位。只在模型坐标系内变换，与缩放、外推、光照矩阵无关。
- **逻辑接入**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java) / [ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) / [ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java)）：`LEVER_F` 完全等同开关（`state` 0/1 右键翻转、可写变量、`supportsBoolVariable`）；`BUTTON_X` 完全等同按钮（`state` 为弹起时刻或 `HOLD_UNTIL_RELEASE`，`effectiveHoldTicks()` / `isKeypadPressed()` / `releaseExpiredKeypads()` / `releaseKeypad()` / CC 快照 / 组件编辑弹起时间 / GUI 弹起输入框全部按 `KEYPAD` 同路径处理），但 `supportsVariableWrite()` 排除（与按钮一致，程序不可直写）。附带文字定位复用按钮分支（`renderCompText` 的 `case KEYPAD, BUTTON_X`）。
- **修正（指示灯误染色 / 按钮默认尺寸）**：拉杆背板上的红色指示灯（源贴图中偏亮的 `169,25,17` 与高光 `244,134,134`）此前按「红色」一起标了 `usemtl tint`，改为背板（`object_1`）只认纯深红 `136,0,21`（`IsPanelRed`：R 110~160、G ≤ 20、B ≤ 40）才染色，把手（`object_2`）仍用宽松红色判定，于是背板 19 面 + 把手 15 面染色、指示灯与绿色指示灯保留原色；两套模型与贴图已按新规则重新生成。`BUTTON_X` 默认 `renderScale` 由 0.3 改为 0.15（再缩小一半），已放置的旧组件保留各自存档里的缩放。
- **构建**：gradlew build BUILD SUCCESSFUL（仅保留既有过时 API 警告）。

**第三十五期：三状态开关（t_switch / t_lever）、双色指示灯（lamp_d）与 RGB 控制器换型**

- **注册**（[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) / [ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) / [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java)）：新增 `controller_t_switch`（`T_SWITCH("t_switch", 14×14, renderScale=0.3)`）、`controller_t_lever`（`T_LEVER("t_lever", 14×14, 0.3)`）、`controller_lamp_d`（`LAMP_D("lamp_d", 14×14, 0.3)`），均加入本模组创造栏；中英 lang 补组件名与物品名；物品模型 `controller_t_switch.json` / `controller_t_lever.json` / `controller_lamp_d.json` 使用专属图标 `textures/item/controller_t_switch.png`（三档旋钮）/ `controller_t_lever.png`（三档拉杆）/ `controller_lamp_d.png`（双色指示灯），取自 `模型/控制台类/panel_props/物品贴图-三档旋钮.png`、`物品贴图-三档拉杆.png`、`物品贴图-双色指示灯.png`（均 16×16）。
- **模型资源**（`models/console_parts/`，由 `build/tmp/convert_panel_props.ps1` 从 `模型/控制台类/panel_props` 转换，坐标变换 `(x,y,z)→(-z,y,x)`、法线 `(-nz,ny,nx)`，与既有旋钮开关资源同一约定、不做缩放）：`t_switch_surface.obj` / `_1_` / `_2_` ← `knob_switch3_green/orange/red.obj`（对应档位 0/1/2）；`t_lever_surface.obj` / `_1_` / `_2_` ← `lever_switch3_green/orange/red.obj`；`lamp_d_surface.obj`（红亮绿灭）← `indicator2_red.obj`、`lamp_d_1_surface.obj`（绿亮红灭）← `indicator2_green.obj`。所有新模型只把 `o Panel`（背板）标 `usemtl tint`，其余分组（Knob / Base / Lever / LampGreen / LampOrange / LampRed）保持原贴图，因此**染色只作用在背板**。
- **RGB 颜色控制器换型**：`rgb_color_surface.obj` 改用 panel_props 的 `indicator_orange_on.obj`（单灯居中的「点亮」态），分组 `Panel` 保持 `prop`、`Lamp` 标 `usemtl tint`（沿用 RGB「只给灯面上色」的语义）。
- **贴图共用与修正**：新模型全部来自 panel_props，而 UV 是按该目录的调色板贴图排布的（所有 indicator* / knob_switch* 共用 `knob_switch.mtl → knob_switch.png`，lever_switch* 共用 `lever_switch.mtl → lever_switch.png`）。此前误用了本模组旧版 `knob_switch.png`（调色板顺序不同、且大面积透明），导致 `entityCutout` 把旋钮 / 拉杆主体整片裁掉（表现为「中间部分缺失」、灯色发黑）。现将 `t_switch.png` / `t_lever.png` 改为 panel_props 源图的副本，并在右下角 (508,64) 补 4×4 不透明白块供 tint 采样；`ConsoleCompType.textureId()` 让 `LAMP_D` / `RGB_COLOR` 与 `T_SWITCH` 共用 `t_switch.png`（三者的源模型共用同一 mtl），删除重复的 `lamp_d.png` / `rgb_color.png`；[ConsoleCompTextures.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompTextures.java) 的 `base()` 也改用 `textureId()` 取图，避免按已删除的 id 贴图名告警「贴图缺失」。
- **多状态模型加载**（[ConsoleCompModels.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleCompModels.java)）：新增 `get(type, int variant)`——`0` 基础模型、`1/2` 对应 `<modelId>_1_surface.obj` / `<modelId>_2_surface.obj`；内部统一为 `getInternal(type, suffix)` / `load(type, suffix)`，后缀同时用于缓存 key、OBJ 文件名，贴图按 `textureId()` 取名并在存在 `<textureId><后缀>.png` 时优先使用（三档开关 / 双色灯 / RGB 共用基础贴图，自动回退）。原 `get(type, boolean closed)` 行为不变。
- **渲染选择**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：`meshFor()` 对三状态开关按 `state`（夹紧 0~2）取三个档位模型，对 `LAMP_D` 按 `state` 0/1 取红亮 / 绿亮模型；两态开关的 `_close` 逻辑保持原样。
- **逻辑**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`applyCompUse` 新增 `T_SWITCH / T_LEVER` 分支，右键按 `floorMod(state+1, 3)` 循环 0→1→2→0，并同步 `value = state`；`writeBackToVar` 把三状态开关写回 **FLOAT**（0/1/2），`LAMP_D` 并入 BOOL 分支；`withFloatValue` 把写入值四舍五入并夹紧到 0~2 后同时更新 `state`（程序 / CC 写浮点即选档）。`ConsoleCompType` 中 `supportsFloatVariable()` 加入 T_SWITCH / T_LEVER、`supportsBoolVariable()` 加入 LAMP_D，并新增 `isThreeState()`。
- **RGB 控制器尺寸**：`RGB_COLOR` 默认 `renderScale` 由 0.5 改为 0.25（面积缩到原来的 1/4，14×14 模型显示约 3.5×3.5 结构像素），命中框按 `renderScale` 计算因而同步缩小；已放置的旧组件保留各自存档里的缩放。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第三十六期：程序列表「移动」与行编辑器「复制」**

- **移动**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：程序行右侧新增 14px 宽的「移」热区（删除行按钮已从列表中移除，见第三十八期），点击后进入移动模式：源行底色改为暗黄高亮、所有空行显示「空行（点击移动到此）」；此时点击任一空行即执行 `moveLine(from, to)`（`snapshot[to] = line; snapshot[from] = null`）并整表提交，随后自动翻到目标行所在页并用动作栏提示「已移动到程序行 #xx」。点其它行的「移」可换源行，点别处或按 Esc 取消；已完全成型（`prog_locked`）时不可移动。行的表达式文字可用宽度相应减去 `LINE_MOVE_W`。
- **复制**：行编辑器顶部「启用」开关右侧新增「复制」按钮（宽度收窄给按钮让位），点击执行 `copyLineForward()`——把当前行整行（条件 / 目标 / 算式 / 注释 / 启用状态）收进 `pendingCopy` 并关闭编辑器，进入与「移动」相同的放置模式（空行显示「空行（点击放置副本）」），由玩家点击任意空行放置，点别处或 Esc 取消；空行则提示无可复制内容。
- **文案**：`gui.cc_rc.program.move_short / move_target / copy / copy_empty / paste_target / delete` 与 `message.cc_rc.controller.prog_moved / prog_copy_ready / prog_pasted` 中英双语补齐；`localHint` 支持格式化参数。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第三十七期：组件附带文字缩放基准修正**

- **文字大小**：[ConsoleBusBER.renderCompText](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java#L549-L586) 与 [renderRgbText](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java#L511-L536) 的文字缩放系数取 `comp.scale`（玩家组件缩放），间距 `COMP_TEXT_GAP` 同样按它的比例缩放：缩放比 1 时就是以前各组件默认的附带文字大小，不再乘类型的 `renderScale`（否则旋钮 / RGB 等本来就渲染得小的组件文字会小到看不清）。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第三十八期：程序列表删除按钮移入行编辑器**

- **删除**：程序行右侧的「×」删除热区整体移除（`LINE_DELETE_W` 随之删除），表达式可用宽度只扣除「移」热区。
- **编辑器新增删除**：行编辑器顶部「启用」右侧并排「复制」「删除」两个按钮（启用按钮宽度相应收窄）；`deleteEditingLine()` 提交 `snapshot[editingLine] = null` 后关闭编辑器，完全成型锁定状态下提示不可修改。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第三十九期：三状态组件档位与模型对齐**

- **问题**：`t_lever_*_surface.obj` / `t_switch_*_surface.obj` 的模型档位顺序是「0 = 左 + 绿灯、1 = 中 + 橙灯、2 = 右 + 红灯」（分别由 `lever_switch3_*` / `knob_switch3_*` 转换；模型 z 轴 = -U，故绿灯在屏幕左侧、红灯在右侧），与逻辑约定（state 0 = 右 + 红灯、2 = 左 + 绿灯）相反。
- **修正**（[ConsoleBusBER.meshFor](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java#L463-L471)）：`T_LEVER` 与 `T_SWITCH` 统一取 `2 - state` 作为模型档位，反转首尾档位；中间档位（竖直 / 竖直指针、橙灯）不变，`state` 数值与程序 / CC 写入口径不变。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第四十期：控制柜颜色变体（蓝 / 黑）**

- **颜色枚举**（[ConsoleColor.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleColor.java)）：新增 `DEFAULT / BLUE / BLACK`，实现 `StringRepresentable`（`EnumProperty` 必需），提供 `textureSuffix()`（默认色返回空串，其余返回 `_blue` / `_black`）用于贴图名后缀。
- **新增结构方块**（[ModBlocks.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlocks.java) / [ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) / [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java)）：`controller_block_blue`（`MapColor.COLOR_BLUE`）、`controller_block_black`（`MapColor.COLOR_BLACK`），强度 / 音效 / 工具要求与原 `controller_block` 一致，均带 `BlockItem` 并入创造栏；[ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) 增加 `public final ConsoleColor color` 字段与携带颜色的构造器（旧构造器保留，默认色）。[ModBlockEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModBlockEntities.java) 的 `controller_block_be` 合法方块列表补上两者。
- **同色才连通**（[ConsoleStructureHelper.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleStructureHelper.java)）：泛洪以种子方块的颜色为准，遇 `colorOf(nextState) != color` 直接跳过（不连通、不记入成员）；第 3 步「相邻控制柜方块但未访问」仍判 MULTIPLE，因此总线同时贴到异色连通体时照常报错。`Result` 增加 `color` 字段，`stillValid(..., count, color)` 逐格校验颜色；新增 `isConsole(BlockState)` / `colorOf(BlockState)` 工具方法（移除不再需要的 `ModBlocks` 依赖）。异色方块因此可以相邻摆放，但不会并入同一结构、也不会互相影响功能。
- **总线换贴图（不新增方块）**（[ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) / [ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：总线新增 `COLOR` 方块状态（默认 DEFAULT），成型时把识别到的颜色写入方块实体字段 `structureColor`，再由 `applyBusColor()` 用 `flag 3` 写回方块状态（触发邻居更新，让相邻 aux 跟随）；失效 / 溃散时复位为 DEFAULT。`stillValid` 巡检带颜色，颜色不符即视为结构破损。NBT 新增 `Color`（enum ordinal，缺省 DEFAULT，旧存档保持原样）。
- **aux 跟随**（[ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java)）：新增 `COLOR` 方块状态，`onPlace` / `neighborChanged` 时扫描六邻——先取相邻总线颜色，再退回相邻结构方块颜色，都没有则默认色；写入用 `flag 2`（不触发邻居更新，避免连锁刷新）。
- **方块状态与模型**：`blockstates/controller_bus.json`、`controller_aux.json` 重写为 `color=default/blue/black × facing=north/east/south/west` 共 12 个变体；新增 `blockstates/controller_block_blue|black.json`、`models/block/controller_block_blue|black.json`（cube_all）、`models/item/controller_block_blue|black.json`，以及 `models/block/controller_bus_blue|black.json`、`models/block/controller_aux_blue|black.json`（正面 north 用 front / aux 变体，其余四面用 side 变体）。
- **贴图**：`textures/block/controller/` 新增 `controller_block_blue.png`、`controller_block_black.png`、`controller_aux_blue.png`、`controller_aux_black.png`、`controller_bus_front_blue.png`、`controller_bus_side_blue.png`、`controller_bus_front_black.png`、`controller_bus_side_black.png`（源自 `模型/控制台类/贴图/蓝色|黑色`；黑色源文件名误拼 `controller_bus_frount.png` 已按规范名复制）。
- **判定替换**：[CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java)（爆炸保护）、[MultiToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/MultiToolItem.java)、[ConsoleCompItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ConsoleCompItem.java)、[EditToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/EditToolItem.java)、[ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java)、[ConsoleBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBER.java) 中原先写死的 `state.is(ModBlocks.CONSOLE_BLOCK)` 全部改为 `ConsoleStructureHelper.isConsole(state)`，使新色方块与原方块行为完全一致。
- **文案**：中英 lang 补 `block.cc_rc.controller_block_blue` / `controller_block_black`。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第四十一期：画图工具（draw_tool）**

- **数据模型**（`block/console/draw/`）：[ConsoleShapeType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleShapeType.java) 枚举 `RECT`（实心矩形）/ `RECT_OUTLINE`（空心矩形）/ `LINE`（线段）/ `ARROW`（箭头）/ `PRESET`（预设图案），`usesWidth()` 标出可设线宽的类型；[ConsoleShape.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleShape.java) 为不可变图形（`id / face / layer / type / color(0xRRGGBB) / width / u1,v1,u2,v2 / preset / rotation / scale`），`width`、`rotation`（角度 0~359.9）、`scale` 均为 `float`，常量 `MIN_WIDTH=0.1F`、`MAX_WIDTH=4.0F`、`LAYERS=8`、`MIN_SCALE=0.1F`、`MAX_SCALE=3.0F`，并提供 `quantize(v)`（四舍五入到 0.1，避免浮点噪声在界面 / 网络 / 存档之间累积），`save()/load()` 用 `putFloat/getFloat`（线宽 / 缩放的旧 int 值可被 `getFloat` 兼容读取；旋转旧存档是 0~3 的 int 枚举，`load()` 检测到 int 时 ×90 迁移为角度）；[ConsoleDrawPresets.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleDrawPresets.java) 登记预设图案（`id / 贴图 / 像素尺寸 / 最大缩放倍数(float)`），默认 `sample`（`textures/gui/draw_presets/sample.png`，32×32，缩放 1.0~3.0）；贴图显示时先乘 `DISPLAY_SCALE=0.5` 再乘玩家缩放（32 像素贴图默认占 16 表面像素，与旧 16×16 观感一致）；预设贴图以顶点色相乘染色，灰度值越深显示越暗。
- **存储与同步**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：图形按面存在总线方块实体的 `Map<Direction, List<ConsoleShape>> drawShapes`（上限 `MAX_DRAW_SHAPES=512`），随总线 NBT 一起保存（`Draws` 列表 + `NextDrawId`）并通过 `getUpdateTag` 同步客户端，因此 GUI 直接读本地方块实体即可，无需把图形塞进菜单包；新增服务端 API `getDrawShapes(face) / countDrawShapes() / findDrawShape(id) / findDrawShapeAt(face,u,v) / applyDrawAdd(...) / applyDrawEdit(...) / applyDrawSetAnchor(id,u,v) / applyDrawDelete(id)`，返回 `DRAW_OK / NOT_FORMED / LOCKED / OUTSIDE / LIMIT / NOT_FOUND / BAD_DATA` 状态码；整体平移 `shiftDrawShape` 按包围盒夹紧（避免逐点 clamp 变形），`rebaseSurfaceEntries` 中结构基准变动时同步平移图形；`hasConfigData()` 计入图形数量。
- **渲染**（[ConsoleDrawRenderer.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleDrawRenderer.java)）：按 `ConsoleSurface` 的 U/V/N 基向量构造矩阵（列 = U/16、V/16、N/16），外推 `OUTSET_BASE=0.001 + layer*0.0001`（最大 0.0017，小于组件 `COMP_OUTSET=0.002`，保证图案位于组件下方且减少与方块表面的深度冲突）；实心图形统一用 1×1 白贴图 `textures/block/console_parts/white.png` + 顶点色（`RenderType.entitySolid`），预设用 `entityCutoutNoCull` 直接贴单色 PNG；预设按 `rotation` 角度（任意角度，不限 90° 倍数）以中心旋转四角、UV 固定为 0..1（`rotateOffset` 辅助方法）。箭头头部长度 `min(len, 4*width)`、头半宽 `1.5*width`（完全随线宽等比缩放，不再有 6 / 3 的固定下限），[ConsoleShape.bounds](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/draw/ConsoleShape.java) 的箭头留边同步为 `1.5*width`。所有顶点统一传入固定的「视空间向上」法线 `FLAT_NORMAL`（单位矩阵 + `(0,1,0)`）：实体类着色器会对顶点做 `minecraft_mix_light` 漫反射，平面图案法线正对玩家时系数仅约 0.74，会把白色压成浅灰、所有颜色整体发灰；固定法线后系数恒为 1，图案始终显示为所选颜色。接入 [ConsoleBusBER.render](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) 的最前面（组件之前绘制）。
- **网络**（[ConsoleDrawPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleDrawPacket.java)）：id 21，`OP_EDIT`（整体改写类型 / 颜色 / 线宽 / 图层 / 两端点 / 预设）、`OP_MOVE`（按绝对锚点整体移动）、`OP_DELETE`、`OP_CREATE`（首次创建，成功后回 `ConsoleDrawCreatedPacket`）；服务端距离 100 校验后调用总线方法并按状态码提示。新增 [ConsoleDrawCreatedPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleDrawCreatedPacket.java)（id 22，S2C 回执 `pos + shapeId`，-1 = 创建失败），界面据此从「未创建」切换到编辑状态。
- **界面**（[ConsoleDrawMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleDrawMenu.java) / [ConsoleDrawScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleDrawScreen.java)）：**单个图案的属性界面**（320×220），不再在 GUI 内画布绘图。菜单携带图形全部参数、表面半宽高以及 `shapeId / pending`（-1 + pending = 尚未创建）；界面控件：5 个类型按钮（矩形 / 空心矩形 / 线段 / 箭头 / 预设）、16 色色块 + `#RRGGBB`、线宽输入框（0.1~4.0，支持一位小数）、图层 0~7、位置 X/Y（图案左下角，表面像素）、尺寸（矩形宽/高；线段箭头长度/角度）、右侧**贴图样式列表**（2 列 24×24 缩略图，直接点选预设图案）、选中预设时的**旋转角度输入框**（任意角度 0~359.9）与缩放输入框（0.1~3.0，支持一位小数）、保存 / 删除 / 关闭；未创建状态下选线段或箭头时默认长度 8、角度 0。切换类型时按当前几何换算端点并重建该行控件，绘制与命中统一使用绝对坐标（`leftPos/topPos`），避免错位。`pending` 时保存 / 删除禁用，点击任一类型或贴图才发 `OP_CREATE`；回执轮询放在 `containerTick()`（`AbstractContainerScreen.tick()` 是 final 不能覆盖）。
- **物品**（[DrawToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DrawToolItem.java)）：工具用 NBT 记住玩家最近选择的颜色 / 线宽 / 缩放 / 图层，另外记录类型、预设、快速绘画、吸附整数、平直线优先（`DrawColor / DrawWidth / DrawScale / DrawLayer / DrawType / DrawPreset / DrawQuick / DrawSnap / DrawStraight`），属性界面保存或创建成功后由 `ConsoleDrawPacket` 回写 `remember(...)`。`useOn`——普通模式：命中已有图案打开其属性界面，否则以点击点为**图案左下角**、以「未创建」状态打开界面（点选类型或贴图才创建，直接关闭不留图案）；非面板方块 / 空气会取消快速绘画已记录的第一点。`use`（右键空气）：shift 打开设置界面 `DrawQuickScreen`，否则取消第一点。悬停青色粗体说明并附带当前设置摘要。
- **快速绘画**（[DrawToolItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/DrawToolItem.java)）：设置界面里开启后右键面板不再打开属性界面。shift+右键只用于编辑命中的已有图案（未命中提示 shift_edit_only）；当前类型为 PRESET 时右键直接以点击点为中心生成图案；为矩形 / 线段 / 箭头时，第一次右键记录第一点（`PENDING`，按玩家 UUID 存服务端静态 Map，含 `busPos + face + u,v`），第二次右键同一总线同一面生成图形（以第一点为原点、第二点为目标），右键非面板位置或空气取消第一点。`吸附整数` 开启时点击坐标 `Math.round` 到整像素；`平直线优先` 开启且类型为线段 / 箭头时，若 `|dx| > 2|dy|` 或 `|dy| > 2|dx|` 则把第二点对应轴对齐第一点，直接画出水平或垂直线 / 箭头。
- **设置界面**（[DrawQuickScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/DrawQuickScreen.java)）：shift+右键空气打开（340×200 普通 `Screen`），可切换 5 种类型、快速绘画 / 吸附整数 / 平直线优先三个开关、16 色色块与 `#RRGGBB`、线宽与缩放输入框、图层 0~7、右侧 3 列预设贴图缩略图、关闭；任何改动立即写回工具 NBT 并同步服务端。
- **工具设置同步**（[DrawToolSettingsPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/DrawToolSettingsPacket.java)）：id 23，C2S 携带颜色 / 线宽 / 缩放 / 图层 / 类型 / 预设 / 三个开关；服务端找到手持工具后校验预设白名单再写入 NBT（[ModNetwork.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ModNetwork.java) 注册）。
- **拖动**（[ConsoleDragClient.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/client/ConsoleDragClient.java)）：多功能工具右键拖动新增 `KIND_SHAPE`，抓取优先级 组件 → 文字 → 图案；图案以 `u1,v1` 为锚点、吸附整数像素跟随准星，发 `OP_MOVE` 绝对锚点，服务端 `applyDrawSetAnchor` 按包围盒夹紧。
- **服务端 API**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：新增 `findDrawShapeAt(face,u,v)`（图层高优先、同层后加优先；线段按点到线段距离）、`applyDrawEdit(...)`（夹紧图层 / 线宽 / 端点，预设白名单）、`applyDrawSetAnchor(id,u,v)`；`applyDrawAdd` 改为返回新建的 `ConsoleShape`（含 id）；移除 `applyDrawMove` 与 `applyDrawClearLayer`。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第四十二期：扇形仪表（gauge_f）**

- **模型转换**：源 `模型/控制台类/panel_props/11_gauge_f/gauge_fan.obj` 按既有约定 `(x,y,z) → (-z,y,x)`（法线同式）一次性转为 [gauge_f_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/gauge_f_surface.obj)，保留 `vt/o/f`，`mtllib` 换成注释头，全部面写 `usemtl prop`（`recolorable=true` 走整图着色，不需要 tint 白块）；转换后包围盒 x 0.090625~0.2075（贴表面）、y/z 0.1~0.9，分组 `o Frame / o Face / o Hub / o Needle`。
- **贴图与图标**：`11_gauge_f/gauge_fan.png`（512×512）复制为 `textures/block/console_parts/gauge_f.png`；`物品贴图.png`（16×16）复制为 `textures/item/controller_gauge_f.png`，物品模型 `models/item/controller_gauge_f.json` 用 `item/generated` + `cc_rc:item/controller_gauge_f`。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 末尾追加 `GAUGE_F("gauge_f", "gauge_f", 14×14, recolorable=true, renderScale=0.45F)`，`item()` 返回新物品、`supportsFloatVariable()` 加入 `GAUGE_F`；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_gauge_f`（`ConsoleCompItem`）；[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补 `gui.cc_rc.console.comp.gauge_f` / `item.cc_rc.controller_gauge_f`。
- **逻辑**：[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java) 的 `writeBackToVar` 浮点分支加入 `GAUGE_F`，与流量表完全同路径（`value` 默认 0、范围夹 -5~105、程序 / CC 写浮点即驱动指针），无需新增字段。
- **指针动画**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：新增常量 `GAUGE_F_BAKED_DEG = -17`（实测模型指针在 (z,y) 平面指向右下约 17°，源 README 的 197° 不可用）、`GAUGE_F_START_DEG = -30`（0 值 = 左下 30°）、`GAUGE_F_DEG_PER_PERCENT = 2.4`（全程 240°）、枢轴 `(y=0.5, z=0.5)`（源 0.5, 0.5, -0.1875 转换而来）；渲染时 `Needle` 分组绕模型 X 轴旋转，角度 = `(-30 + 2.4×value) - (-17)` 转弧度，数值 0 指向左下 30°、100 指向右下 30°、顺时针扫 240°，指针与法线同步旋转。
- **着色**：沿用流量表 `recolorable=true` 的整图重着色，`ConsoleCompTextures` 跳过深色/透明像素，黑色指针与刻度保持原色，颜色只作用在表盘亮部。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。

**第四十三期：同步指示器（synchroscope）**

- **模型转换**：源 `模型/控制台类/panel_props_models/panel_props/12_synchroscope/gauge_fan_lamp.obj`（灯亮）与 `gauge_fan_lamp_off.obj`（灯灭）按 `(x,y,z) → (-z,y,x)`（法线同式）转为 [synchroscope_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/synchroscope_surface.obj)（variant 0，灯亮）与 [synchroscope_1_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/synchroscope_1_surface.obj)（variant 1，灯灭），保留 `vt/o/f`，`mtllib` 换成注释头，全部面写 `usemtl prop`；分组 `o Frame / o Face / o Hub / o Needle / o Lamp`，两个模型共用贴图。
- **贴图与图标**：`12_synchroscope/gauge_fan_lamp.png` 复制为 `textures/block/console_parts/synchroscope.png`；`物品贴图-synchroscope-同步指示器.png`（16×16）复制为 `textures/item/controller_synchroscope.png`，物品模型 `models/item/controller_synchroscope.json` 用 `item/generated` + `cc_rc:item/controller_synchroscope`。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 末尾追加 `SYNCHROSCOPE("synchroscope", "synchroscope", 14×14, recolorable=false, renderScale=0.45F)`，`item()` 返回新物品、`supportsFloatVariable()` 加入 `SYNCHROSCOPE`，新增 `hasSpeedVariable()`（仅同步指示器为 true）；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_synchroscope`（`ConsoleCompItem`）；[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补 `gui.cc_rc.console.comp.synchroscope` / `item.cc_rc.controller_synchroscope`。
- **配置**（[Config.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/Config.java)）：新增 `console_synchroscope.lamp_speed`（默认 15，范围 0~180，单位 度/tick）与 `getSynchroscopeLampSpeed()`。
- **变量绑定**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：附加变量名 = 组件代号 + `_s`，绑定到该组件（`syncSynchroVars / ensureSynchroSpeedVar / isSynchroSpeedName`）；放置组件时只重连已存在的变量，创建发生在设置界面点「完成」；改名 / 设置时重新确保绑定，改名时解绑旧速度变量。速度变量只作为输入：`findVarSlotByComp` 跳过、`uploadVarToComponent` 不写回、`runProgram` 读取阶段跳过、`buildVars`（配置 JSON）按名称补绑为 `FLOAT`，类型或组件不符则 `CFG_INVALID`。
- **服务端 tick**（`applySynchroscopes()`，FULL 状态每 tick）：读取 `<代号>_s` 作为 `speed`，角度先 `wrapAngle180(comp.value)` 再叠加 `speed` 并再次环绕（185° → -175°）；`|speed| < lamp_speed` 且 `|angle| ≤ 15°` 时 state=1（灯亮），否则 state=0（灯灭）；值或状态变化才 `copyWith` 写回变量并 `sync()`。`writeBackToVar` 浮点分支加入 `SYNCHROSCOPE`（程序 / CC 写浮点即设定指针位置）。
- **指针渲染**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：新增常量 `SYNCHRO_BAKED_DEG`（烘焙角，0 位校准值见下方修复）、枢轴 `(y=0.5, z=0.5)`；`o Needle` 分组绕模型 X 轴绕枢轴旋转，角度 = `wrapAngle180(value) - SYNCHRO_BAKED_DEG`，value=0 时指针指向正上方，正角度在屏幕上为顺时针，法线同步旋转。`meshFor` 中 state 1 用基础模型（灯亮），state 0 用 variant 1（灯灭，`_1_surface`）。
- **构建**：gradlew build BUILD SUCCESSFUL（EXIT=0，仅既有过时 API 警告）。
- **修复（转速变量名可编辑）**：`CompEntry` 新增 `speedVar`（空 = 自动 `代号 + "_s"`，`speedVarName()` 统一取名），构造 / 四个 `copyWith*` / NBT（`SpeedVar`，空时不写）/ `applyCompEdit`（新参数 + `[A-Za-z0-9_]{1,32}`、不得等于主代号、重名检查）全部贯通；`isSynchroSpeedName / ensureSynchroSpeedVar / applySynchroscopes / buildVars` 改读 `speedVarName()`；`ConsoleCompPacket`、`ConsoleCompMenu`、`ConsoleCompItem.openCompMenu` 同步增加 `speedVar` 字段与编解码；[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) 为同步指示器在 `DISPLAY_Y` 行新增「转速变量」输入框（初值 = 自定义名，未自定义时显示 `代号_s`），提交时若等于 `代号_s` 或留空则发送空串（改代号后自动跟随），并补 render / keyPressed / 回车提交。
- **修复（背板染色）**：同步指示器原先全部面 `usemtl prop` 且贴图无纯白像素，染色链路拿不到 `tintable` 面、`findTintUv` 回退到透明像素导致完全无效。两个 OBJ 的 `o Frame`（背面大板，即背板）12 个面改为 `usemtl tint`，`o Face` 前恢复 `usemtl prop`；贴图 `synchroscope.png` 在未被任何面采样的区域 (500,196)-(507,203) 写入 8×8 纯白块供 `findTintUv` 采样。现在与其它 panel_props 组件一致：背板随所选颜色染色，表盘 / 指针 / 灯面保持原色。
- **修复（转速变量不再在放置时自动创建）**：`applyCompAdd` 不再调用 `syncSynchroSpeedVar`，改为只调用 `ensureSynchroSpeedVar(comp, false)` 重连已存在的同名浮点变量，不会凭空多出 `<代号>_s`；变量改在设置界面点「完成」（`applyCompEdit → syncSynchroVars → ensureSynchroSpeedVar(comp, true)`）时创建，与 RGB 控制器三通道变量的行为一致。同时 `isSynchroSpeedName` 改用 `comp.speedVarName()`，自定义转速变量名也能被识别为转速输入量（此前只认 `代号_s`，改自定义名会残留旧绑定）。
- **修复（0 位指针偏左）**：按指针几何反算烘焙角——针根中点 (y=0.5, z=0.5)、针尖中点 (y≈0.4251, z≈0.7451)，指针实际指向 +Z 偏下约 107°，`SYNCHRO_BAKED_DEG` 由 -102.5 改为 -107.0，0 位时指针正好竖直向上（居中）。
- **新增（灯状态布尔附加变量）**：同步指示器增加 `<代号>_b`（如 `SYNCHROSCOPE_1_b`），布尔，只读输出：`applySynchroscopes` 每 tick 用 `lamp = |速度| < 阈值 && |角度| ≤ 15°` 的实际判断值覆盖；放置组件时只重连、不创建，设置界面点「完成」时由 `ensureSynchroLampVar` 创建；`applyVarCreate` / 配置 JSON 导入按名称绑为 BOOL；`findVarSlotByComp`、`uploadVarToComponent` 均跳过该变量，避免抢占主变量槽位或被变量反向写入。CC 侧经 `getVar("代号_b")` 读取。
- **修复（灯状态变量名可编辑 + 出现在变量创建候选）**：`CompEntry` 新增 `lampVar`（空 = 自动 `代号 + "_b"`，`lampVarName()` 自定义优先），构造链 / 四个 `copyWith*` / `withValue` / `withPoint` / `applyCompText` / NBT（`LampVar`，空时不写）/ `applyCompEdit`（新参数 + 格式校验 + 不得等于主代号与转速名 + 重名检查）全部贯通；`ConsoleCompPacket`、`ConsoleCompMenu`、`ConsoleCompItem.openCompMenu` 增加 `lampVar` 编解码；[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) 为同步指示器新增 `LAMP_Y` 行的「灯状态变量」输入框，同步指示器改用加高背景（`tallLayout()`：RGB 控制器或带灯变量时用 `GUI_BG_TALL` / `RGB_BG_HEIGHT` / `RGB_BUTTON_Y`）；[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) 的 `devices()` / `matchedDevice()` 补上同步指示器的 `speedVarName()`（FLOAT）与 `lampVarName()`（BOOL）候选与匹配，创建变量下拉里可直接选到两个附加变量。
- **修复（染色位置错误）**：原先用 `usemtl tint` 标记 `o Frame`（外框）导致颜色只染外框。改为与扇形仪表一致的整图重新着色：`SYNCHROSCOPE` 的 `recolorable` 改为 `true`，两个模型的 `o Frame` 恢复 `usemtl prop`。表盘中间背景（亮米色）被染色，外框 / 刻度 / 深色指针的贴图像素为深色，重新着色时被跳过。

**第四十四期：安全按钮（button_s）**

- **模型转换**：源 `模型/控制台类/panel_props_models/panel_props/09_cover_button` 的三个 OBJ 按 `(x,y,z) → (-z,y,x)`（法线同式）转为 [button_s_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/button_s_surface.obj)（variant 0，合盖）、[button_s_1_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/button_s_1_surface.obj)（variant 1，开盖弹起）、[button_s_2_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/button_s_2_surface.obj)（variant 2，开盖按下）；分组 `Panel / Base / Button / Cover`，玻璃罩面写 `usemtl glass`，其余 `usemtl prop`。
- **贴图与图标**：`button_s.png`（模型贴图）与 `button_s_glass.png`（玻璃罩，供半透明批次使用）放入 `textures/block/console_parts/`，物品图标 `textures/item/controller_button_s.png` + 物品模型 `models/item/controller_button_s.json`（`item/generated`）。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 追加 `BUTTON_S("button_s", "button_s", 14×14, recolorable=false, renderScale=0.3F)`，`item()` 返回新物品、`supportsBoolVariable()` 加入 `BUTTON_S`（布尔语义与按钮面板一致，表示是否按下）；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_button_s`，[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补 `gui.cc_rc.console.comp.button_s` / `item.cc_rc.controller_button_s`。
- **状态编码**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：复用 `state` long —— `0` 合盖、`OPEN_COVER(-2)` 开盖弹起、`HOLD_UNTIL_RELEASE(-1)` 按住到松手、`>0` 弹起时刻；`CompEntry` 新增 `coverTicks`（默认 `DEFAULT_COVER_TICKS = 100`，`0` = 不自动关，上限 `MAX_COVER_TICKS`），构造链 / 四个 `copyWith*` / `withValue` / `withPoint` / `applyCompText` / NBT（`CoverTicks`，等于默认值时不写，兼容旧存档）/ `applyCompEdit`（新参数 + 仅对 BUTTON_S 生效并夹紧）全部贯通，并新增 `effectiveCoverTicks()`。
- **盖子计时**：新增运行期映射 `Map<Integer,Long> coverDeadlines`（组件 id → 自动关盖时刻，不写 NBT）：`openCover` / `closeCover` 播放开 / 关盖音效（`WOODEN_TRAPDOOR_OPEN` / `_CLOSE`），`refreshCoverDeadline` 按 `now + coverTicks` 记录（0 则移除）；`releaseExpiredCovers()` 在 `tick` 中于 `releaseExpiredKeypads()` 后调用，到点把 `OPEN_COVER` 合回 `0` 并 `sync()`；删除组件时清理映射；读档时对 `state == OPEN_COVER && coverTicks > 0` 的组件按 `当前时间 + coverTicks` 重建，避免存档关闭期间时间流逝导致重进世界立刻关盖。
- **交互**：`applyCompUse` 新增 `case BUTTON_S` —— `bigStep`（Shift）在 `0 ↔ OPEN_COVER` 间切换，按钮按下期间（`-1` 或 `>0`）Shift 无效；非 Shift 仅在 `OPEN_COVER` 时按下（`holdTicks<=0` 为按住到松手，否则 `now + holdTicks`），按下期间暂停自动关盖计时，播放 `STONE_BUTTON_CLICK_ON`；`releaseExpiredKeypads` / `releaseKeypad` 到点或松手时回到 `OPEN_COVER` 并重新计时；[ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) 的客户端按住判断加入 BUTTON_S，且非开盖状态不进入按住流程。
- **渲染**：`ConsoleCompModels` 识别 `usemtl glass`，标记 `Tri.glass` 并在 `ObjMesh` 记录 `glassTexture`（`<modelId>_glass.png`）；[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) 为玻璃面单独用一个 `entityTranslucentCull(button_s_glass.png)` 半透明批次绘制（见下方修复）；`meshFor` 对 BUTTON_S 按状态取模型（`OPEN_COVER` → variant 1，按下 → variant 2，否则 variant 0），按下状态已烘焙在 `_2` 模型里，故 `pressGroup` 位移排除 BUTTON_S。
- **设置界面**：[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) 为安全按钮在第二行新增「盖子自动关闭」输入框（0~`MAX_COVER_TICKS`，默认 100，`0` = 不自动关），并改用加高背景（`tallLayout()` 加入 BUTTON_S）；`ConsoleCompPacket` / `ConsoleCompMenu` / `ConsoleCompItem` 全链路增加 `coverTicks` 字段与编解码。
- **修复（玻璃罩不透明 / 整个按钮发蓝）**：玻璃面一度改成 `RenderType.entityCutoutNoCull(button_s_glass.png)`，但裁切批次只丢弃 alpha < 0.1 的像素，`button_s_glass.png` 全部像素的 alpha 都约 0.23，于是整片玻璃被当成不透明绘制——盖子变成一块浅蓝实体，把后面的面板 / 底座 / 按钮全部挡住。现改为 `RenderType.entityTranslucentCull(button_s_glass.png)`：半透明批次按贴图 alpha 真正混色，罩子透明可见内部按钮；批次创建顺序在不透明批次之后，先画不透明再画玻璃。
- **修复（玻璃罩整体发雾 / 全部半透明）**：玻璃罩由 5 块「薄板盒」（每块 = 两面 0.015 间隔的板）拼成，若用无剔除的 `entityTranslucent`，每块板正反两面各叠一层 alpha（约 0.23×2），整个罩子累积到 40% 以上的雾状遮盖，表现为「整个按钮半透明」。改用带背面剔除的 `entityTranslucentCull`：每块板只画朝向观察者的一面，只贡献一层 alpha。
- **修复（整个组件被玻璃贴图/半透明状态污染）**：`renderComponents` 原先是「一个组件一次遍历、内部交替写入裁切批次与半透明批次」。同一个 `MultiBufferSource` 里先创建裁切批次、再创建半透明批次后，先创建的批次贴图/状态会被破坏（与「组件绘制中切换 RenderType 导致贴图整体失效」同一根因），表现为整个模型都套上 `button_s_glass.png`、整体半透明。现改为**两遍绘制**：第一遍只画所有组件的不透明面（只创建 `entityCutoutNoCull` 批次），第二遍只画所有玻璃面（只创建 `entityTranslucentCull` 批次），两遍互不干扰；批次创建顺序保证玻璃在不透明之后绘制。
- **修复（盖子永不自动关闭）**：`releaseExpiredKeypads()` 原先的跳过条件只排除 `state == 0` / `HOLD_UNTIL_RELEASE`，而合盖外的 `OPEN_COVER(-2)` 会落到「`now >= state` 已到点」分支，于是每个 tick 都被当成一次弹起、并调用 `refreshCoverDeadline` 把自动关盖时刻重设为 `now + coverTicks`，截止时刻永远推后、`releaseExpiredCovers()` 永远等不到。现跳过条件改为 `state <= 0L`（只有正数弹起时刻才参与计时弹起），开盖后 `coverTicks` 到点即自动合盖。
- **染色（按钮本体可着色）**：三个 OBJ 的 `o Button` 分组（按钮本体 + 薄板共 12 面）在分组后加 `usemtl tint`，即按钮本体为逐面染色面（`Tri.tintable`）；面板 / 底座 / 金属框仍是 `usemtl prop`，玻璃罩 `usemtl glass`，均不参与染色。`button_s.png` 是调色板贴图、没有可挪用的纯白采样块，故 `ConsoleCompModels.findTintUv` 找不到白块时返回 `null`，`ObjMesh.tintU/tintV` 记为 `-1`；[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java#L532-L537) 渲染时用 `useTintUv = tinted && mesh.tintU >= 0` 判断，为 `-1` 时**保留原 UV、只用顶点色乘出所选颜色**，避免采样到透明像素被裁切丢弃。效果：设置颜色后只有中间按钮的白色本体变色，其余部分保持原色。
- **修复（开盖后变量直接变 true）**：`runProgram()` 读组件时对布尔变量一律用 `comp.state != 0L`，而安全按钮的 `state` 在开盖时是 `OPEN_COVER(-2)`、计时按下时是弹起时刻，都会被判成 true，于是程序运行时变量表/程序读数显示「按下了」。现对 `KEYPAD / BUTTON_X / BUTTON_S` 改用 `comp.isKeypadPressed(gameTime)` 判断：合盖与开盖都是 false，按住到松手（-1）或计时未到点才是 true，松手/到点后回到 false。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第四十五期：钥匙开关（switch_k）**

- **模型转换**：源 `模型/控制台类/panel_props_models/panel_props/10_key_switch` 的四个 OBJ 按 `(x,y,z) → (-z,y,x)`（法线同式）转为 [switch_k_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/switch_k_surface.obj)（variant 0，合盖无钥匙）、[switch_k_1_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/switch_k_1_surface.obj)（variant 1，开盖无钥匙）、[switch_k_2_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/switch_k_2_surface.obj)（variant 2，合盖有钥匙）、[switch_k_3_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/switch_k_3_surface.obj)（variant 3，开盖有钥匙）；分组 `o Panel` 写 `usemtl tint`（背板可染色），`o Knob` / `o Key` 写 `usemtl prop`。
- **贴图与图标**：`knob_switch.png` 复制为 `textures/block/console_parts/switch_k.png`；物品图标 `textures/item/controller_switch_k.png`（钥匙开关）与 `textures/item/console_key.png`（钥匙）；物品模型 `models/item/controller_switch_k.json`、`models/item/console_key.json`（`item/generated`）。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 追加 `SWITCH_K("switch_k", "switch_k", 14×14, recolorable=false, renderScale=0.3F)`，`item()` 返回新物品、`supportsBoolVariable()` 加入 `SWITCH_K`，新增 `hasKeyMode()`；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_switch_k`（`ConsoleCompItem`）与 `console_key`（`ConsoleKeyItem`，`stacksTo(1)`）；[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补组件名 / 物品名 / `item.cc_rc.console_key.bound` / 钥匙提示文案。
- **钥匙物品**（[ConsoleKeyItem.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/item/ConsoleKeyItem.java)）：NBT 键 `CompCode` 记录组件代号；`createKey(code, label)` 生成物品并设置名字「控制柜钥匙：<组件附带文字，空则用代号>」；`keyCode(stack)` 读取代号；`useOn` 定位总线与点击面上的组件，仅对钥匙开关调用 `applyKeyInsert`。
- **组件字段**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `hasKey`（是否有钥匙）与 `keyMode`（0 开闭 / 1 常开 / 2 常闭，常量 `KEY_MODE_*`）；完整构造器末尾追加两参（旧 24 参构造委托「无钥匙 + 开闭」），`copyWith / copyWithPos / copyWithColor / copyWithShowOriginal / copyWithKey / withHistorySample / withPoint / applyCompText / applyCompEdit` 与 NBT（`HasKey` 为 true 才写、`KeyMode` 非默认才写，读档夹紧 0~2）全部贯通；`writeBackToVar` 布尔分支加入 `SWITCH_K`（变量只取 `state != 0`）。
- **交互**：`applyCompUse` 新增 `Player` 参数（[ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) 传入），`case SWITCH_K`：无钥匙时部分成型 + Shift 右键取出一把钥匙（`giveKeyItem`，进入背包或掉落）；有钥匙时 Shift 右键拔钥匙（常开仅开时可拔、常闭仅关时可拔、开闭都可），拔下后 `hasKey=false` 并给玩家钥匙；普通右键在有钥匙时切换 0/1。新增 `applyKeyInsert(id, stack, player)`：校验组件类型 / 尚无钥匙 / 钥匙代号 `equalsIgnoreCase` 组件代号，通过后非创造模式消耗 1 个并把 `hasKey` 置 true。新增 `COMP_INVALID(9)` 返回码。
- **交互拦截**：[ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) 的 `use` 在手持 `ConsoleKeyItem` 时返回 `PASS`，避免打开总线界面拦截物品的 `useOn`（结构方块走 [ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) 的「手持物品即 PASS」）。
- **渲染**：[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) 的 `meshFor` 新增 SWITCH_K 分支：`variant = (hasKey ? 2 : 0) + (state != 0 ? 1 : 0)`。
- **设置界面**：`ConsoleCompScreen` 的 `tallLayout()` 加入 `hasKeyMode()`（加高背景），在 `SHOW_ORIGINAL_Y` 行新增「钥匙模式」循环按钮（开闭 → 常开 → 常闭）；`ConsoleCompMenu` / `ConsoleCompPacket` / `ConsoleCompItem.openCompMenu` 全链路增加 `keyMode` 字段与编解码，提交时随编辑包发送。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第四十六期：圆盘记录仪 L（graph_l）**

- **模型转换**：源 `模型/控制台类/圆盘记录仪/recorder/recorder.obj` 按 `(x,y,z) → (-z,y,x)`（法线同式）转为 [plotter_l_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/plotter_l_surface.obj)（包围盒 16×13×3 设计单位 = 1×0.8125×0.1875 格），保留两个分组 `o Body`（框 + 纸 + 刻度板，静止）与 `o Pen`（右侧的笔，只纵向平移）；材质统一 `usemtl prop`，配套 [plotter_l.mtl](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/plotter_l.mtl)。
- **贴图与图标**：`recorder.png` 复制为 `textures/block/console_parts/plotter_l.png`；物品图标 `textures/item/controller_plotter_l.png`（源 `物品贴图.png`）；物品模型 `models/item/controller_plotter_l.json`（`item/generated`）。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 追加 `GRAPH_L("graph_l", "plotter_l", 16×13, recolorable=false, renderScale=1.0F)`，`item()` 返回新物品，`supportsFloatVariable()` / `supportsHistory()` 加入 `GRAPH_L`（变量、历史采样、推送触发名称 `Code2` 全部与圆盘记录仪一致）；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_plotter_l`，[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补 `gui.cc_rc.console.comp.graph_l` / `item.cc_rc.controller_plotter_l`。
- **笔的动画**（[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java)）：新增 `penTravel(comp)`，取历史序列最前面的采样 `history[0]`（最近一次 push 进入的值；无历史时退回 `value`），按模型设计 `y = 2.836 + 0.07276 × 值`（设计单位，0~100）映射，并减去模型中烘焙的位置 `6.4743`（源模型按 50 建模）得到纵向偏移，再除以 16 换算成格；渲染时对 `Pen` 分组的顶点只做 `py += penOffsetY`（横向、法线不变）。
- **曲线**：`renderPlotHistory` 按 `GRAPH_L` 切换一套新几何——绘制区左边界 `-5.78`、宽度 `9.7682`（纸面设计单位 4.0118…13.78，右端正好接笔尖）、数值 0 / 100 对应纵向 `-3.664` / `+3.612`（与笔行程 7.276 一致）、线宽 `0.5`，50 个采样点仍按「最新在最右列」排列；曲线用 `penPlaneOutset`（模型 x = 2.815 设计单位）外推，位于纸面（2.800）之前、笔尖背面（2.828）之后，不穿模。
- **染色**：`recolorable = false`，模型保持原贴图；组件颜色只作为曲线颜色（与圆盘记录仪相同，未设置颜色时默认红色）。
- **踩坑修复（组件完全不渲染）**：`modelId` 默认取 `id`，若只写 6 参构造，加载器会去找 `graph_l_surface.obj / graph_l.png` 而资源实际叫 `plotter_l`，`ConsoleCompModels.load` 返回 null → 模型与曲线一起被跳过。改为显式传第 7 参 `modelId = "plotter_l"`；同时把 `GRAPH_L` 常量追加到枚举末尾，避免改变已有类型序号（NBT 按 `id` 字符串存类型，不影响旧存档）。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第四十七期：警报器（alarm）**

- **模型转换**：源 `模型/控制台类/警报器/lamp_buttons` 的 OBJ 按 `(x,y,z) → (-z,y,x)`（法线同式）转为 6 个变体 [alarm_surface.obj](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/alarm_surface.obj)（灯灭）与 `alarm_1..5_surface.obj`（`_1` 灯亮、`_2` 静音按钮按下、`_3` 灯亮 + 静音按下、`_4` 重置按下、`_5` 灯亮 + 重置按下），配套 [alarm.mtl](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/models/console_parts/alarm.mtl)；面板从左到右为方形灯 / 静音按钮 / 重置按钮。
- **贴图与图标**：`lamp_buttons.png` 复制为 `textures/block/console_parts/alarm.png`；物品图标 `textures/item/controller_alarm.png`；物品模型 `models/item/controller_alarm.json`（`item/generated`）。
- **注册**：[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 追加 `ALARM("alarm", "alarm", 16×16, recolorable=false, renderScale=0.45F)`，`item()` 返回新物品，`supportsBoolVariable()` 加入 `ALARM`，新增 `hasMuteVariable()`（仅 ALARM 为 true）；[ModItems.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModItems.java) 新增 `controller_alarm`，[ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入创造栏；中英 lang 补 `gui.cc_rc.console.comp.alarm` / `item.cc_rc.controller_alarm`。
- **组件字段**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`CompEntry` 新增 `muted`（静音）、`pressButton`（0 无 / 1 静音 / 2 重置）与 `pressUntil`（按下反馈结束的游戏时间）；原完整构造器改为委托 `false / 0 / 0L`，新增 29 参完整构造器，仅 ALARM 保留这三个值；`copyWith / copyWithPos / copyWithColor / copyWithShowOriginal / copyWithKey`、`withValue`、`withPoint`、`withHistorySample`、`applyCompText / applyCompEdit` 全部贯通，新增 `copyWithMuted / copyWithPress` 与 `muteVarName()`（`<代号>_Q`）；NBT 写 `Muted`（仅 true 时）与 `PressButton` / `PressUntil`（非 0 时）。
- **`_Q` 附加变量**：`applyVarAdd` 按 `isAlarmMuteName` 绑定为只读 `BOOL`（初值取 `muted`）；`uploadVarToComponent` / `findVarSlotByComp` 跳过该名称，避免抢占主变量槽位或被变量反向写入；`ensureAlarmMuteVar(comp, create)` 只重连已存在的同名变量（不自动创建），设置界面改名时由 `syncAlarmVars` 解绑旧名并重绑新名（模仿同步指示器灯变量的做法）；`writeBackToVar` 布尔分支加入 `ALARM`（只写 `state != 0`）。
- **报警结算**（新增 `applyAlarms()`，在 `tick` 的 `State.FULL` 分支 `applySynchroscopes()` 之后调用）：清除到期的 `pressUntil` 按下反馈；报警解除（`state == 0`）时自动把 `muted` 清为 false —— 保证下一次「false → true」上升沿一定是未静音状态；静音状态每 tick 镜像到 `<代号>_Q`；报警中且未静音时每 `ALARM_SOUND_INTERVAL`（10 tick）用 `ConsoleSurface.toWorld(...)` 算出组件所在位置并 `playSound` 播放占位音效 `SoundEvents.BELL_BLOCK`（多个警报器按 `comp.id` 错开），之后替换为专用音频。
- **交互**：`applyCompUse` 新增 `case ALARM` —— `action 2`（静音）把 `muted` 置 true、记录 `pressButton = 1`，灯常亮且停止发声（`_Q` 由下一 tick 的 `applyAlarms` 镜像）；`action 3`（重置）把组件值置 0 并 `writeBackToVar` 写回主变量、记录 `pressButton = 2`，灯熄灭（静音随报警解除一并清除）；灯区域不处理。[ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) 按模型实测比例把点击位置分为三段：`rel < -0.33`（灯，只消费不响应）、`-0.33~0.32`（静音，action 2）、`> 0.32`（重置，action 3）。
- **渲染**：[ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) 的 `meshFor` 新增 ALARM 分支：`lampOn = 报警中 && (已静音 || gameTime/10 偶数)`（闪烁只在客户端按游戏时间计算，不逐 tick 同步），`variant = (lampOn ? 1 : 0) + (静音按下 ? 2 : 0) + (重置按下 ? 4 : 0)`，覆盖 0~5 六个模型。
- **修复（变量表里自动多出变量 + 界面缺少改名栏）**：放置组件时不再自动创建 `<代号>_Q`（`applyCompAdd` 改为 `ensureAlarmMuteVar(added, false)`，只重连已存在的同名变量）；随后又发现「移动组件（设置界面改坐标 → `applyCompEdit → syncAlarmVars`）」仍会自动创建，于是把 `syncAlarmVars` 也改为只重连（`create = false`），同步指示器的 `syncSynchroVars`（`_s` / `_b`）一并改为不创建——现在任何组件放置 / 移动 / 改名都不会修改变量表，附加变量只能由玩家在变量表里按同名新增后自动绑定。`CompEntry` 新增 `muteVar`（空 = 自动 `代号 + "_Q"`）并贯通完整构造器（30 参）、全部 `copyWith*`、`applyCompText`、NBT（`MuteVar`，空时不写）；`applyCompEdit` 增加 `muteVar` 参数（格式校验 / 不得等于代号 / 重名检查 / 等于自动名时按空保存）；`ConsoleCompMenu` / `ConsoleCompPacket` / `ConsoleCompItem` 全链路增加 `muteVar` 编解码；[ConsoleCompScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleCompScreen.java) 为警报器新增「静音变量」输入框（加高界面），[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) 的 `devices()` / `matchedDevice()` 补上 `muteVarName()`（BOOL）候选与匹配；配置 JSON 导入按名称补绑静音变量。此外 `runProgram()` 读布尔变量时跳过 `_Q`（只读变量自身，不从「是否报警」回读），避免程序轮询把它覆盖成报警状态。
- **染色（只染背板红色面板）**：`lamp_buttons.png` 的背板由三个盒子组成——奶油色边框 `#e2d7bd`（整块底板）、红色面板 `#b23a2b`、顶部黑条 `#202024`；六个变体的 `o Panel` 分组中每 6 个面为一盒，因此只在第 7~12 面（红色面板）插入 `usemtl tint`，第 13 面前恢复 `usemtl prop`（6 个 OBJ 各 1 处，已 grep 校验）。`ConsoleCompModels` 解析时把这些面标记为 `Tri.tintable`，`findTintUv` 在本贴图中找到未被任何变体三角形 UV 覆盖的纯白 4×4 块（位于 18,18），渲染时染色面的 UV 指到该白块、再乘顶点色，于是显示为准确的所选颜色（无需改 PNG）；未设置颜色时仍用原 UV 显示原红色面板。[ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) 的 `ALARM` 保持 `recolorable = false` 并注释原因：`recolorable = true` 会走 `ConsoleCompTextures` 整张贴图重新着色，把灯、红色重置按钮等有色像素一并改色，违背「只染红色背板」。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第四十八期：程序表达式一元运算（取整 / 取小数部分 / 绝对值）**

- **IR**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java)）：`Op` 末尾追加 `FLOOR` / `FRAC` / `ABS`（网络包按 `ordinal` 传输，只能追加）；`isUnary()` 纳入三者；`type()` 为「数值（布尔可隐式拓宽）→ FLOAT」；`symbol()` 返回 `#` / `~` / `@`；`toText()` 输出前缀形式（子表达式为二元时自动加括号，与 `NEG` 规则一致）；NBT 的 `save / load` 与网络包的 `write / read` 四个一元分支同步补全。
- **VM**（[ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java)）：新增 `OP_FLOOR_F(0x1D)` / `OP_FRAC_F(0x1E)` / `OP_ABS_F(0x1F)`，`BYTECODE_VERSION` 由 1 升为 2；`compileValue` 对三者先用 `compileAsFloat` 编译操作数（布尔自动 `B2F`），执行时分别为 `Math.floor`、`x - Math.floor(x)`、`Math.abs`。
- **行编辑器**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：`parseOperand` 允许任意组合的一元前缀 `! - # ~ @`（新增 `isUnaryPrefix` / `unaryOpOf`，离操作数最近的最先生效，如 `-@A` 为 `-(|A|)`）；`appendExprSegments` 与 `appendWrappedSegments` 补三个显示分支；操作数着色、候选下拉的前缀扫描统一改为调用 `isUnaryPrefix`。
- **配置导入**（[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java)）：`buildConfigExpr` 的一元分支补 `FLOOR / FRAC / ABS`（JSON 的 `op` 用枚举名，导出 / 导入天然一致）。
- **方案文档**：新增 [表达式文本化改造方案.md](file:///e:/trae/program/CC_RC/表达式文本化改造方案.md)，记录 P0~P5 分阶段计划、原版 `MultilineTextField` 复用结论、算符优先级表与一元糖对照表。
- **构建**：gradlew build BUILD SUCCESSFUL（附加 `-Dnet.minecraftforge.gradle.check.certs=false` 关闭 ForgeGradle 的联网证书检查，否则离线环境下会卡在 `EnvironmentChecks.testServerConnection`；仅既有过时 API 警告）。

**第四十九期：表达式文本解析器 ProgramExprParser（P1）**

- **新类**（[ProgramExprParser.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramExprParser.java)）：P1 阶段的纯后端解析器，不接界面。`parse(source, vars, targetType)` 返回 `Result`（`expr` / `error` / `tokens`），失败时 `expr` 为 null 且 `error` 带 `code`、`[start, end)` 与中文提示，`tokens` 始终可用供后续着色。
- **词法**：`NAME`（变量名，`[A-Za-z0-9_]`，经 `Vars.findSlot` 解析槽位，未定义记 -1）、`NUMBER`（支持小数与科学计数法）、`BOOL`（true/false，忽略大小写）、`OP`、`LPAREN`、`RPAREN`、`EOF`；空白与换行只作分隔符，换行不产生任何语义。双字符运算符优先识别（`&& || == != <= >= //`），单 `&`、`|` 与其它字符记为 `bad_char`。
- **语法**：Pratt 优先级爬升，优先级 `||` < `&&` < 比较 < `+ -` < `* / // %` < 一元 < 原子，全部左结合；减号在操作数位置按取负处理，一元可连续叠用（`-@A`）。
- **类型**：解析时携带「期望类型」上下文——`&&`、`||`、`!` 的操作数期望布尔，算术与比较的操作数期望数值，顶层用目标变量类型；布尔位置上的 `0 / 1` 解释为布尔常量（与旧行编辑器 `isBoolTarget` 行为一致）。解析完成后用 `Expr.type(vars)` 复核，并要求结果类型与目标变量完全一致（沿用 `Line.validate` 的严格规则），另做 `MAX_DEPTH` 深度检查，超过即报 `too_deep`（同时限制递归，避免恶意长括号串爆栈）。
- **保留写法**：一元只用符号 `!  -  #  ~  @`，**不提供** `floor() / frac() / abs()` 之类函数写法；仅保留历史数据里的 `float(x)`（显式布尔转浮点），因为 `Expr.toText` 仍这样输出，需要能读回。
- **自检**：临时程序 `build/parsercheck/ParserCheck.java`（不属于源码，检查后删除）对 28 个用例全部通过——含优先级、左结合、一元叠用、`0/1` 布尔上下文、多行输入、`toText` 往返同构，以及空表达式 / 未知变量 / 缺右括号 / 多余符号 / 非法字符 / 类型不符的错误码与区间定位。
- **构建**：gradlew compileJava BUILD SUCCESSFUL。

**第五十期：表达式原文 exprSource 的存储与同步（P2）**

- **IR**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java#L441-L490)）：`Line` 新增 `exprSource`（玩家书写的原文，可含换行）与 `MAX_SOURCE = 1024`；新增 7 参构造器，保留 6 参构造器（原文默认空串），`withEnabled` 一并保留原文。
- **NBT**：`save` / `load` 增加 `Src` 键，空串不写入，旧存档读不到即视为无原文（界面继续按 `Expr.toText` 回显）。
- **网络包**：`write` / `read` 末尾追加 `writeUtf/readUtf(..., MAX_SOURCE)`，客户端与服务端包结构同步变更（同版本内自洽）。
- **配置 JSON**（[ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java#L151-L167)）：`Line` 增加 `source` 字段；导出写入 `line.exprSource`，导入经 `clipSource`（空转空串 + 截断到上限，不 trim 以保留缩进换行）。
- **说明**：本阶段只做存储与同步，不改任何界面行为；原文与 `expr` 树暂时可能不一致（由后续 P3 文本编辑器负责保证一致）。
- **构建**：gradlew build BUILD SUCCESSFUL。

**第五十一期：表达式文本框编辑器 ExprEditBox（P3）**

- **新控件**（[ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java)）：以公共类 `MultilineTextField` 为内核，外包一层绘制与键鼠转发。构造时按控件宽度算出可用文本宽度 `wrapWidth = width - PAD * 2 - SCROLL_W`，并通过 `font.getSplitter().splitLines(value, wrapWidth, Style.EMPTY, false, consumer)` 自行缓存每个显示行的原文区间 `lineRanges`（`List<int[]>`）；空串补 `{0,0}`、结尾换行补 `{len,len}`，与原版 `reflowDisplayLines` 行为一致。原因：`MultilineTextField.StringView` 是 protected 嵌套 record，外部既不能引用类型也不能调用其 `beginIndex()/endIndex()`（`var` 同样报错），只能自算一份区间。
- **绘制**：逐显示行「先按 Token 区间分段着色 → 错误区间整段改红 → 画选区底色 → 画光标」，`scissor` 裁剪，右侧自绘滚动条（滑块位置按 `scrollOffset / 总行数` 计算）；行数统计改用 `lineRanges.size()`，滚动与 `scrollToCursor` 同步替换（`getLineAtCursor()` 仍用原版）。选区区间用「光标 ± `getSelectedText().length()`」反推 `[begin,end)`：先试光标在选区末尾（`value.startsWith(text, cursor - length)`），再试开头，兜底 `[max(0, cursor-length), min(len, cursor)]`。
- **交互**：`keyPressed` 直接转发 `MultilineTextField.keyPressed`（Enter 插入真实 `\n`，Esc 在补全展开时只收补全）；鼠标点击 `seekCursorToPoint` 定位并收起补全，候选下拉的点击由屏幕在 `mouseClicked` 最前处理；滚轮仅在鼠标位于控件内时生效。
- **界面替换**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：删除旧 5 行「算符 + 操作数」控件及其全部辅助方法（`resetExprRows`、`addExprRow`、`loadExpr`、`flattenExpr`、`operandText`、`leafText`、`buildExpr`、`parseOperand`、`isUnaryPrefix`、`unaryOpOf`、`parseOperator`、`isBoolTarget`、`operandNames`、`operatorNames`、`operandBaseColor`、`operandSegments`）、`exprOpBoxes / exprValueBoxes / exprRows / addOpButton` 及 `EDIT_EXPR_ROWS / EDIT_EXPR_STEP / EDIT_OP_W` 常量；行编辑器表达式区改为单个多行框（`EDIT_EXPR_Y = 88`、`EDIT_EXPR_H = 76`），注释框、按钮列、错误提示行随之下移。
- **校验联动**：新增 `analyzeExpr(bus)`（调用 `ProgramExprParser.parse(原文, exprVars(bus), targetType(bus))`）、`exprVars`（匿名 `Vars`：`findSlot → bus.findVarSlot`、`typeOf → VarDef.type`、`nameOf → def.name` 或 `#槽位`）、`targetType`（行目标变量的类型，只读或未定义返回 null）、`exprNameColor`、`exprCandidates`（`true/false` + 全部可读变量）、`exprErrorText`（错误码 → `err_name / err_type / err_deep / err_expr`）。`refreshLineError` 先解析再给 `exprBox.setDiagnostics(result)`，目标与条件错误次之，解析失败立即显示表达式错误并禁止提交；`buildLine` 失败返回 null，保存时把原文写入 `Line.exprSource`，`openLineEditor` 回显优先原文、缺失时用 `Expr.toText(scope)` 生成。
- **提交与焦点**：表达式框内 Enter 换行、Ctrl+Enter 提交（`Screen.hasControlDown()` + 257/335），无候选时 Esc 关闭行编辑器；焦点与拖拽交给 `AbstractContainerScreen` 的默认派发，`lineEditorFocused()` 纳入 `exprBox.isFocused()`。补全下拉绘制在框外，由屏幕 `render` 末尾调用 `renderSuggestions`、`mouseClicked` 最前调用 `mouseClickedSuggestion`。
- **文案**：中英 lang 的 `hint_operand` 改为表达式示例与新一元符号说明，新增 `err_name`（未定义的变量），删除已无引用的 `add_op / hint_operator / err_op`。
- **方案文档**：[表达式文本化改造方案.md](file:///e:/trae/program/CC_RC/表达式文本化改造方案.md) 的「六、编辑器控件」按实际实现重写（自算行区间、选区反推、自绘滚动条），阶段表 P3 标为已完成。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第五十二期：条件表达式化 condExpr（P4）**

- **IR**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java)）：`Line` 新增 `Expr condExpr`（`null` = 沿用旧三态，兼容旧存档）与 `String condSource`（条件原文）；新增 9 参完整构造器，7 参 / 6 参构造器委托 `null / ""`；`withEnabled`、`validate`、`toText`、NBT（`CondExpr` 复合标签 + `CondSrc`）、网络包（存在标记 + 树 + 原文）全部贯通。`validate` 对新条件要求 `condExpr.type(scope) == BOOL` 且深度不超 `MAX_DEPTH`。
- **VM**（[ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java)）：`compile` 优先编译 `condExpr` 再 `OP_JMP_IF_FALSE`；`condExpr == null` 时保持旧的 `LOAD_B / NOT_B` 三态路径，新旧字节码语义一致。
- **配置 JSON**（[ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java)）：新增 `condition_expr` 与 `condition_source`；[ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java) 导出时双写（旧 `cond / condition` 保留），导入时 `condition_expr` 优先，并用 `clipSource` 截断原文；`isVarReferenced` 同时检查 `condExpr`，避免删除仅被条件引用的变量。
- **控件**（[ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java)）：新增 `singleLine` 模式，`keyPressed` 遇到 Enter（257/335）直接返回 `false`，把提交交还界面。
- **行编辑器**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：删除条件三态按钮与 `cycleCondMode`、`editCond`、`COND_MODE_W`；`condBox` 改为单行 `ExprEditBox`，接入 `setNameColor / setCandidates / setOnChanged`，共享着色、标红、补全。新增 `analyzeCond`（`ProgramExprParser.parse(条件原文, vars, BOOL)`）与 `condErrorText`；`refreshLineError` / `confirmLine` / `buildLine` 都会解析条件（留空视为总是执行），失败即禁止提交；`openLineEditor` 回显优先 `condSource`，其次 `condExpr.toText`，最后旧三态；`lineSegments`、`setLineEditorVisible`、`mouseClicked`、`keyPressed`、`renderSuggestions`、`lineEditorFocused` 均已纳入条件框。
- **文案**：中英 lang 删除 `cond_true / cond_var / cond_not`，新增 `hint_cond`（「条件，留空表示总是执行；例如 A > 3 && B」），`err_cond` 改为「条件必须是布尔表达式」。
- **方案文档**：[表达式文本化改造方案.md](file:///e:/trae/program/CC_RC/表达式文本化改造方案.md) 阶段表 P4 标为已完成，§四补 `condSource` 与网络包字段，§六补单行模式，§八补旧三态回落说明。
- **过时文档修正**：[控制柜程序系统实现方案.md](file:///e:/trae/program/CC_RC/控制柜程序系统实现方案.md) 的状态、§4.1 条件、§4.2 `&& ||`、§6.1 IR、§6.2 行编辑器、§9 标题、§13 阶段状态（P2~P6 全部已完成）、§14 文件路径（改为 `block/console` 实际类，删不存在的 `program/*` 与 aux 方块实体）、§16 待定项同步为现状。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第五十三期：表达式框空白 / 算符后自动列出变量候选**

- **控件**（[ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java)）：`refreshSuggestions` 增加空前缀分支——光标位于文本开头、空白之后，或算符 / `(` 之后（`isSuggestPosition` 跳过空格向前看）时，直接列出全部候选（`true` / `false` + 全部可读变量），替换区间为光标处的空区间。
- **不打扰已有文字**：聚焦或点击时只走「自动」分支（`refreshSuggestions(true)`），已有名字中间不弹过滤列表；新增 `setFocused` 覆盖，聚焦即按空白 / 算符位置自动弹出，失焦收起。
- **回车不抢占**：自动弹出的列表标记 `autoOpened`，此时回车（257/335）只收起列表，继续换行（表达式框）或提交（单行条件框）；Tab 仍直接应用候选，上下键导航后视为主动选择，回车可确认。
- **构建**：gradlew build BUILD SUCCESSFUL（仅既有过时 API 警告）。

**第五十四期：控制柜子系统系统性审查（健壮性 + 注释 + 文档）**

- **网络包**（[ConsoleProgramPacket.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/network/ConsoleProgramPacket.java)）：`decode` 遇到负行数先归零，避免 `Math.min` 结果为负导致 `new ProgramIR.Line[负数]` 抛 `NegativeArraySizeException`。
- **IR 反序列化深度上限**（[ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java)）：`Expr.load` / `Expr.read` 拆出带剩余深度余量的私有重载，递归每层减一，余量耗尽即返回 `null`（上限取 `Line.MAX_DEPTH`）。异常存档或恶意包不再能构造超深树触发 `StackOverflowError`；`Line.load` 的 `condExpr` 也因此同样受控。
- **过时注释**：`ProgramIR` 类头由「不做文本语法解析 / 后续阶段实现」改为实际的「文本入口 + 旧数据入口」两条来源；`ExprEditBox` 键盘分支注释「上下键换行」改为「上下键在候选间导航」。
- **方案文档**：[控制柜程序系统实现方案.md](file:///e:/trae/program/CC_RC/控制柜程序系统实现方案.md) 的 §1.2 数据流、§1.3 权威源、§2.2 `controller_aux` 去掉「aux 有方块实体 / 保存 IR 快照 / 双向 version+hash 比对 / 未成型拒绝打开」的旧设计；§3.5 NBT 表按 `saveAdditional` / `load` 的实际键重写（补结构、文字、图形、组件、`ProgLines` 与行内键），并注明字节码与 `ProgWarn` 不写盘。
- **审查未改（第五十五期已全部修复）**：`ProgramVM` 栈下溢无显式检查（仅损坏字节码可达）、`OP_JMP` 为死指令、解析器单 `=` 等同 `==` 且名为 `float` 的变量后接 `(` 会被当作 CAST、`refreshLineError` 经 `buildLine` 重复解析条件（轻微性能）。
- **构建**：gradlew build BUILD SUCCESSFUL（28s，11 个既有过时 API 警告）。

**第五十五期：审查遗留问题修复（VM 栈防御 / 解析器歧义 / 界面重复解析）**

- **VM 栈下溢防御**（[ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java)）：执行循环取出指令后先按 `requiredStack(op)` 检查栈深（一元 / 写变量 1，二元 2），不足立即结束本轮，避免 `--sp` 变负后 pop / push 落到错误下标、静默写坏影子数组。
- **删除死指令 `OP_JMP`**：编译期从不生成，删掉常量与执行分支；其后操作码顺延（`JMP_IF_FALSE` 0x1A、`HALT` 0x1B、`FLOOR_F/FRAC_F/ABS_F` 0x1C~0x1E）。字节码不写存档，无需版本迁移。
- **解析器歧义**（[ProgramExprParser.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramExprParser.java)）：单个 `=` 不再是相等运算符，词法阶段直接报错并提示改写成 `==`；`float` 优先按变量解析，只有 `float` 未定义时 `float(` 才当作历史拓宽写法，且该分支计入 `nesting`，不能再绕过 `MAX_DEPTH`。
- **界面解析次数**（[ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java)）：`buildLine` 改为接收调用方已解析的 `result / condResult`，`refreshLineError` 与 `confirmLine` 不再重复解析表达式与条件（每次按键少解析两次）。
- **文档**：[控制柜程序系统实现方案.md](file:///e:/trae/program/CC_RC/控制柜程序系统实现方案.md) §4.2 补「相等必须写 `==`」「`float` 变量优先」两条规则，§7.1 改为字节码不写盘，§7.2 指令表删 `JMP`、顺延编号并补 `FLOOR_F / FRAC_F / ABS_F`；[表达式文本化改造方案.md](file:///e:/trae/program/CC_RC/表达式文本化改造方案.md) §五同步。
- **构建**：gradlew build BUILD SUCCESSFUL（28s，10 个既有过时 API 警告）。

**控制柜系统总览（历期索引与现状）**

> 第二期 ~ 第五十五期中与控制柜（`controller_*` / `Console*`）有关的条目按模块归拢如下，便于查阅；详细实现仍见对应期号。

**1. 模块与职责**

| 文件 | 职责 |
| --- | --- |
| [ConsoleBusBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlockEntity.java) | 总线唯一数据源：结构状态、组件、文字、绘图、变量表、程序行、NBT、VM 执行与 CC 操作队列 |
| [ConsoleBusBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBlock.java) | 服务端 tick、成型校验、外设注册、破坏通知 |
| [ConsoleBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlock.java) / [ConsoleBlockEntity.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBlockEntity.java) | 结构成员方块与 `Bus / StructureState` 标记 |
| [ControllerAuxBlock.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ControllerAuxBlock.java) | 辅助计算机：右键找唯一相邻总线并打开程序 GUI（不存数据） |
| [ConsoleStructureHelper.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleStructureHelper.java) | 同色连通泛洪、长方体完整性、每 100 tick 巡检 |
| [ConsoleBusBER.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusBER.java) / [ConsoleSurface.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleSurface.java) | 表面坐标系、组件模型、读数、文字、绘图渲染 |
| [ConsoleCompType.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompType.java) / [ConsoleCompUse.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCompUse.java) | 组件类型与变量能力、命中与点击动作 |
| [ConsoleBusPeripheral.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleBusPeripheral.java) / [ConsoleCcBridge.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleCcBridge.java) | CC:Tweaked 外设与取数 / 写值排队 |
| [ProgramIR.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramIR.java) / [ProgramExprParser.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramExprParser.java) | 程序 IR、校验、NBT / 包序列化；文本表达式 → 树 |
| [ProgramVM.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ProgramVM.java) | 字节码编译与栈式执行（运行警告、除零 / 非有限值保护） |
| [ConsoleProgramScreen.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramScreen.java) / [ExprEditBox.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ExprEditBox.java) / [ConsoleProgramMenu.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/gui/ConsoleProgramMenu.java) | 变量表与程序行编辑、表达式输入控件、菜单 |
| [ConsoleConfigJson.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/block/console/ConsoleConfigJson.java) + `ConsoleConfigScreen / ConsoleConfigStore` | 配置 JSON 结构与保存 / 加载界面 |

**2. 历期索引**

| 分组 | 期号 |
| --- | --- |
| 结构与方块 | 第二期（四向总线 + 表面文字）、第六期（改名 `controller_*` + 辅助计算机）、第二十九期（辅助计算机四向）、第四十期（蓝 / 黑变体） |
| 组件模型与交互 | 第四期及补充（组件 / 颜色 / 文字 / 尺寸）、第五期及补充（开关 / 按钮 / 调节器仪表）、第二十五期（按住语义）、第二十六 ~ 二十八期（缩放 / 吸附 / 文字随缩放）、第三十三 ~ 三十五期（RGB / 阀门 / 三状态 / 双色灯）、第三十九期（三状态档位）、第四十二 ~ 四十七期（扇形仪表 / 同步指示器 / 安全按钮 / 钥匙开关 / 圆盘记录仪 L / 警报器） |
| 显示与渲染 | 第十一期（动态读数）、第二十一 ~ 二十四期（历史采样 / 曲线 / 刻度 / 按钮弹起）、第四十六期（记录仪 L） |
| 程序系统 | 第七 ~ 十期（变量表 / GUI / IR / 字节码 VM）、第十二 ~ 二十期（编辑体验 / 着色 / 原版风格 GUI）、第三十 ~ 三十二期（CC 接口 / 配置 JSON）、第三十六 / 三十八期（移动 / 复制 / 删除）、第四十八 ~ 五十三期（一元算符 / 文本解析 / 原文 / 文本框 / 条件表达式 / 自动补全） |
| 工具 | 第四十一期（画图工具） |

**3. 关键数据格式**

- 总线 NBT：`FormState / Error / Origin / SizeX~Z / Count / Color / LastOrigin~LastSizeZ / Texts / NextTextId / Draws / NextDrawId / Comps / NextCompId / ProgramPeriod / Vars / VarF / VarB / ProgLines / ProgWarn`。
- 程序行 NBT：外层 `Idx`，行内 `On / Cond / CondSlot / Target / Expr / Comment / Src / CondExpr / CondSrc`；字节码不持久化，读档后 `programDirty = true` 重新编译。
- 网络包：`ConsoleProgramPacket`（整表保存）、`ConsoleVarPacket`、`ConsoleCompPacket`、`ConsoleConfigUpload/Result`、`ConsoleStructurePacket`、`ConsoleText*/ConsoleCompText*`、`ConsoleDraw*`、`ConsoleKeypadRelease`。
- 配置 JSON：`format / version / size / facing / components / texts / variables / program{period, lines[row, enabled, cond, condition, condition_expr, condition_source, target, expr, comment, source]}`；`condition_expr` 存在时优先于旧三态 `cond / condition`。

**4. 现状与待办**

- 程序系统 P0 ~ P4 已完成（文本表达式、原文回显、`condExpr` 条件）；P5 仅剩手工回归。
- 已知未实现：前向引用提示（§4.3 / §16.1）、CC 单变量 `writableByCC` 开关（可选）。
- 2026-11-15 系统性审查的结论见临时日志；确认项以「网络 / 反序列化健壮性」为主，不影响现有玩法。

## 三、注册物品

> 约定：方块与物品 ID 一一对应；`BlockItem` 为普通方块物品，特殊物品使用专属类。以下按方块类归组。

### 1. 方块（Blocks）——共 130 个

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

### 2. 物品（Items）——共 165 个

**方块物品（96 个，`BlockItem` / `SignItem`）：**

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
| `controller_block` | `BlockItem` | 控制柜结构方块 |
| `controller_block_blue` | `BlockItem` | 蓝色控制柜结构方块 |
| `controller_block_black` | `BlockItem` | 黑色控制柜结构方块 |
| `controller_bus` | `BlockItem` | 控制柜总线 |
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

**特殊物品（30 个）：**

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

### 3. 方块实体（Block Entity Types）——共 18 个

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
| `console_block_be` | `ConsoleBlockEntity` | console_block（控制台结构方块，存所属总线 + 结构状态） |
| `console_bus_be` | `ConsoleBusBlockEntity` | console_bus（控制台总线，三态结构 + 最小角/尺寸/格数） |

> 注意：`breaker`（断路器）为纯逻辑方块，**没有**方块实体，状态完全由 `BlockState`（FACING/POWERED）驱动。
>
> **告示牌为何需要自定义方块实体类型**：原版 `BlockEntityType.SIGN` / `HANGING_SIGN` 的 `validBlocks` 只包含原版告示牌方块。`BlockEntityRenderDispatcher` 渲染时会先做 `blockEntity.getType().isValid(blockState)` 校验，若方块不在该方块实体类型的有效方块集合内则直接跳过渲染，导致告示牌完全透明（无模型、无文字、亦非紫黑块）。因此为粗布告示牌注册了专用方块实体类型（工厂复用 `CanvasSignBlockEntity`/`CanvasHangingSignBlockEntity`，`getType()` 返回自定义类型），并在客户端为这两个类型注册原版 `SignRenderer`/`HangingSignRenderer`。模型层与材质仍由原版机制按 `WoodType "cc_rc:canvas"` 自动生成。

### 4. 声音（Sound Events）——共 38 个

32 个音乐声音与 32 张唱片一一对应：`music_level5`、`railugun`、`never`、`assumptions`、`conrnfield_chase`、`move`、`night`（more_one_night）、`rain`、`end`、`underground_river`、`hanezeve_caradhina`、`cutie_mew_mew_magic`、`denise`、`gwangju`、`higher`、`king`、`marisa`、`mixue`、`raw_tell`、`reimu`、`you_will_be_perfect`、`bloom`、`jigoku_shoujo`、`the_imitation_game`、`bit`、`broken_boy`、`panic_track`、`resonance`、`roller_mobster`、`sabotage`（GitHub issue #1 新增 6 首）、`friends_wine`、`air`（2026-09-14 新增 2 首）；另有 6 个非唱片声音：`nai_long`（奶龙玩偶语音，右键奶龙玩偶 `nai_long_toy` 时播放）、`server_noise`（F.A.A.S服务器 `server_faas_1/2/3` 的环境音效，玩家靠近时持续播放，注册于 [sounds.json](file:///e:/trae/program/CC_RC/src/main/resources/assets/cc_rc/sounds.json)）、`password_crack`（破解器破解音效，破解密码输入器期间在方块位置循环播放，中断/成功/方块破坏时由网络包通知客户端停止）、`gajin`（盖金蜗牛右键音效，玩家右键 `gajin` 时播放，平常无 ambient 叫声）、`reactor_start` / `reactor_start_full`（反应堆启动音乐，素材 `模型/音频素材/反应堆正常启动音乐.ogg` 3:51 转 mono 160kbps；`reactor_start` 截取前 56s 且末尾 4s 淡出（52~56s，淡出算在 56s 内，尾部 RMS -28dB），`reactor_start_full` 完整全曲；两者均 `stream: true`、**仅指令播放无唱片**，如 `/playsound cc_rc:reactor_start @p`）。

### 5. 创造标签

方块与物品通过 [ModCreativeTabs.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModCreativeTabs.java) 加入标签 `cc_rc_tab`（"CC：反应堆控制台"）；另注册第二个标签 `cc_rc_tab_carried`（"CCRC：搬运的物品"），Forge 按注册名对模组创造标签排序，此前缀更长的注册名位于主标签之后，图标为箱装土豆，仅收录搬运类物品（当前为箱装土豆、冰箱、水槽与 16 色粗布告示牌/悬挂式粗布告示牌，均不加入主标签）。

### 6. 实体（Entities）——共 5 个

**弹射物实体（Projectile Entities）——1 个：**

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `bao_zi` | `BaoZi` | 包子（继承原版雪球类 `Snowball`，飞行逻辑与雪球一致；命中实体或方块时触发半径 4 的爆炸，仅伤害实体、不破坏方块，粒子/音效为原版爆炸；客户端复用原版 `ThrownItemRenderer` 渲染） |

**生物实体（Mobs）——5 个：**

| 实体 ID | 实体类 | 说明 |
| --- | --- | --- |
| `error_mob` | `ErrorMob` | 错误生物（由「模型/错误生物/error.obj」立体化生成的敌对实体，详见 [二、27 错误生物](#27-错误生物error_mob)） |
| `error_mob_null` | `ErrorMob` | 错误生物变种（「模型/错误生物/null.obj」，NULL 字牌，仅模型/贴图不同） |
| `error_mob_warn` | `ErrorMob` | 错误生物变种（「模型/错误生物/WARN/warn.obj」，WARN 字牌，仅模型/贴图不同） |
| `gajin` | `GajinSnail` | 盖金蜗牛（被动动物，AI 参考原版猪无乘骑机制，金鹰食物，右键播放 gajin 音效，不自然生成，详见 [二、30 盖金蜗牛与金鹰](#30-盖金蜗牛与金鹰gajinsnail--golden_eagle)） |
| `evil_gajin` | `EvilGajin` | 邪恶盖金（敌对生物，继承原版僵尸 AI：不会燃烧/不会捡装备，100 血 20 攻，距离变速追击（远快于玩家疾跑、近身减速），锁定/攻击/击杀/受击全套语音 + RWR 追击循环音，不自然生成，详见 [二、38 邪恶盖金](#38-邪恶盖金evilgajin--evil_gajin)） |

> 实体类型注册于 [ModEntities.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/ModEntities.java)（`DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ...)`），在 [CcRc.java](file:///e:/trae/program/CC_RC/src/main/java/com/cc_rc/CcRc.java) 构造函数中 `ModEntities.ENTITY_TYPES.register(modEventBus)`。左键投掷由客户端 `InputEvent.InteractionKeyMappingTriggered` 事件 + C2S 数据包实现（见「二、15 包子」）。
