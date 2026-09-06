# Barbeque's Delight — Code Wiki

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈与环境](#2-技术栈与环境)
3. [项目结构](#3-项目结构)
4. [模块架构](#4-模块架构)
5. [核心模块详解](#5-核心模块详解)

   * [5.1 主入口与初始化 (init)](#51-主入口与初始化-init)

   * [5.2 方块系统 (content.block)](#52-方块系统-contentblock)

   * [5.3 物品系统 (content.item)](#53-物品系统-contentitem)

   * [5.4 配方系统 (content.recipe)](#54-配方系统-contentrecipe)

   * [5.5 兼容层 (compat)](#55-兼容层-compat)

   * [5.6 事件与混入 (event & mixin)](#56-事件与混入-event--mixin)

   * [5.7 数据生成 (init.data)](#57-数据生成-initdata)

   * [5.8 工具类 (util)](#58-工具类-util)
6. [依赖关系](#6-依赖关系)
7. [项目运行方式](#7-项目运行方式)
8. [数据流与核心流程](#8-数据流与核心流程)
9. [扩展开发指南](#9-扩展开发指南)

***

## 1. 项目概述

**Barbeque's Delight** 是一个 Minecraft Forge 1.20.1 的 Mod，为游戏添加了**烧烤**主题的玩法。它是知名 Mod **Farmer's Delight** 的扩展/附属 Mod。

### 核心玩法

| 功能                  | 描述                                                       |
| ------------------- | -------------------------------------------------------- |
| **烧烤架 (Grill)**     | 放置在营火上方，可烤制生串，需要翻面，烤过头会烧焦                                |
| **食材盆 (Basin)**     | 用于将食材组合成生串                                               |
| **托盘 (Tray)**       | 存储物品的方块                                                  |
| **调味料 (Seasoning)** | 6 种调味料（孜然粉、胡椒粉、辣椒粉、蜂蜜芥末酱、布法罗酱、烧烤酱），可撒在烤串上改变效果            |
| **烤串 (Skewers)**    | 10 种烤串（鳕鱼、鲑鱼、鸡肉、蘑菇、牛肉、羊肉、兔肉、猪肉香肠、土豆、蔬菜），每种有生/熟两种形态       |
| **三明治类食物**          | 烤肉卷饼 (Kebab Wrap)、烤肉三明治 (Kebab Sandwich)、石锅拌饭 (Bibimbap) |

### Mod 信息

| 属性     | 值                     |
| ------ | --------------------- |
| Mod ID | `barbequesdelight`    |
| 版本     | 1.0.6                 |
| 作者     | Mao, lcy0x1           |
| 许可证    | LGPL-2.1              |
| 平台     | Forge / NeoForge      |
| 依赖     | Farmer's Delight (强制) |

***

## 2. 技术栈与环境

### 开发环境

| 项目           | 值                        |
| ------------ | ------------------------ |
| Minecraft 版本 | 1.20.1                   |
| Forge 版本     | 47.1.3                   |
| Java 版本      | 17                       |
| 构建工具         | Gradle (ForgeGradle 6.x) |
| 映射表          | 官方 Mojang 映射 (official)  |

### 核心库

| 库                  | 用途                         | 版本           |
| ------------------ | -------------------------- | ------------ |
| **L2Library**      | 基础框架：注册表、TileEntity、序列化、配方 | 2.4.25       |
| **L2ModularBlock** | 模块化方块系统（组合模式替代继承）          | 1.1.1        |
| **L2Serial**       | 自动序列化/反序列化框架               | 1.2.2        |
| **Registrate**     | 简化注册和数据生成                  | MC1.20-1.3.3 |
| **MixinExtras**    | Mixin 扩展工具                 | 0.2.0-beta.8 |
| **Mixin**          | 运行时字节码注入                   | 0.8.5        |

### 兼容 Mod

| Mod              | 用途                | 必选 |
| ---------------- | ----------------- | -- |
| Farmer's Delight | 核心依赖（声音、效果、方块、标签） | 是  |
| JEI              | 配方查询 GUI          | 否  |
| Jade             | 方块信息提示            | 否  |
| Curios           | 饰品栏支持             | 否  |
| Carry On         | 搬起方块              | 否  |

***

## 3. 项目结构

```
BarbequesDelight/
├── build.gradle                 # Gradle 构建脚本
├── gradle.properties            # 版本和项目配置
├── settings.gradle              # Gradle 设置
├── libs/                        # 本地依赖 JAR
│   ├── l2library-2.4.25-slim.jar
│   ├── l2modularblock-1.1.0.jar
│   ├── l2modularblock-1.1.1.jar
│   └── l2serial-1.2.2.jar
├── src/
│   ├── main/
│   │   ├── java/com/mao/barbequesdelight/
│   │   │   ├── init/                    # [模块] 初始化与注册
│   │   │   │   ├── BarbequesDelight.java       # Mod 主入口
│   │   │   │   ├── BBQDClient.java              # 客户端入口
│   │   │   │   ├── registrate/                  # 注册表
│   │   │   │   │   ├── BBQDBlocks.java          # 方块注册
│   │   │   │   │   ├── BBQDItems.java           # 物品注册
│   │   │   │   │   └── BBQDRecipes.java         # 配方注册
│   │   │   │   ├── food/                        # 食物定义
│   │   │   │   │   ├── BBQSeasoning.java        # 调味料枚举
│   │   │   │   │   ├── BBQSkewers.java          # 烤串枚举
│   │   │   │   │   └── EffectEntry.java         # 效果条目
│   │   │   │   └── data/                        # 数据生成
│   │   │   │       ├── BBQLangData.java         # 语言文件
│   │   │   │       ├── BBQRecipeGen.java        # 配方生成
│   │   │   │       └── BBQTagGen.java           # 标签生成
│   │   │   ├── content/                  # [模块] 核心内容
│   │   │   │   ├── block/                        # 方块逻辑
│   │   │   │   │   ├── GrillBlock.java           # 烧烤架行为
│   │   │   │   │   ├── GrillBlockEntity.java     # 烧烤架实体
│   │   │   │   │   ├── GrillBlockEntityRenderer.java  # 烧烤架渲染
│   │   │   │   │   ├── GrillBlockItem.java       # 烧烤架物品
│   │   │   │   │   ├── GrillPlace.java           # 烧烤架放置/营火
│   │   │   │   │   ├── BasinBlock.java           # 食材盆行为
│   │   │   │   │   ├── BasinBlockEntity.java     # 食材盆实体
│   │   │   │   │   ├── TrayBlock.java            # 托盘行为
│   │   │   │   │   ├── TrayBlockEntity.java      # 托盘实体
│   │   │   │   │   ├── StorageTile.java          # 存储接口
│   │   │   │   │   ├── StorageTileBlockEntity.java   # 存储抽象实体
│   │   │   │   │   ├── StorageTileContainer.java # 存储容器
│   │   │   │   │   ├── StorageTileRenderer.java  # 存储渲染器
│   │   │   │   │   ├── BlockSlot.java            # 槽位命中检测
│   │   │   │   │   ├── ClickStorageMethod.java   # 点击交互
│   │   │   │   │   └── BBQOverlay.java           # HUD 覆盖层
│   │   │   │   ├── item/                         # 物品逻辑
│   │   │   │   │   ├── FoodItem.java             # 食物基类
│   │   │   │   │   ├── BBQSkewerItem.java        # 烤串物品
│   │   │   │   │   ├── SeasoningItem.java        # 调味料物品
│   │   │   │   │   └── BBQSandwichItem.java      # 三明治物品
│   │   │   │   └── recipe/                       # 配方逻辑
│   │   │   │       ├── GrillingRecipe.java       # 烧烤配方抽象
│   │   │   │       ├── SimpleGrillingRecipe.java # 简单烧烤配方
│   │   │   │       ├── GrillingRecipeBuilder.java# 烧烤配方构建器
│   │   │   │       ├── SkeweringRecipe.java      # 串制配方抽象
│   │   │   │       ├── SimpleSkeweringRecipe.java# 简单串制配方
│   │   │   │       ├── SkeweringRecipeBuilder.java# 串制配方构建器
│   │   │   │       ├── CombineItemRecipe.java    # 组合物品配方
│   │   │   │       └── CombineItemRecipeBuilder.java# 组合配方构建器
│   │   │   ├── compat/                   # [模块] 兼容层
│   │   │   │   ├── jei/                          # JEI 集成
│   │   │   │   │   ├── BBQDJeiPlugin.java        # JEI 插件入口
│   │   │   │   │   ├── GrillRecipeCategory.java  # 烧烤配方分类
│   │   │   │   │   └── BasinRecipeCategory.java  # 串制配方分类
│   │   │   │   └── jade/                         # Jade 集成
│   │   │   │       ├── JadeCompat.java           # Jade 插件入口
│   │   │   │       └── GrillInfo.java            # 烧烤架信息
│   │   │   ├── event/                     # [模块] 事件处理
│   │   │   │   └── BBQGeneralEventHandlers.java  # 村民交易事件
│   │   │   ├── mixin/                     # [模块] Mixin 注入
│   │   │   │   └── CuttingBoardBlockMixin.java   # 切菜板混入
│   │   │   └── util/                      # [模块] 工具类
│   │   │       └── FDConfig.java                 # Farmer's Delight 配置桥接
│   │   └── resources/
│   │       ├── META-INF/mods.toml         # Mod 元数据
│   │       ├── assets/barbequesdelight/   # 资源文件（纹理、模型、语言）
│   │       ├── barbequesdelight.mixins.json # Mixin 配置
│   │       └── pack.mcmeta               # 资源包元数据
│   └── generated/resources/              # 自动生成的资源
│       ├── assets/barbequesdelight/       # blockstates, models, items
│       └── data/barbequesdelight/         # advancements, loot_tables, recipes, tags
```

***

## 4. 模块架构

### 架构总览

```
┌─────────────────────────────────────────────────────────────┐
│                    Barbeque's Delight                        │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │  init/   │  │ content/ │  │ compat/  │  │ event/    │  │
│  │ 注册入口  │──│ 核心逻辑  │──│ 兼容层    │  │ 事件处理   │  │
│  │ 数据生成  │  │ 方块/物品 │  │ JEI/Jade │  │ Mixin     │  │
│  └──────────┘  │ 配方系统  │  └──────────┘  └───────────┘  │
│                └──────────┘                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
         │              │              │
         ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                  外部依赖与框架层                              │
│  L2Library · L2ModularBlock · L2Serial · Registrate         │
│  Forge · Farmer's Delight · JEI · Jade · Curios             │
└─────────────────────────────────────────────────────────────┘
```

### 模块职责矩阵

| 模块       | 包路径                | 职责                        |
| -------- | ------------------ | ------------------------- |
| **初始化**  | `init/`            | Mod 入口点、注册表管理、数据生成器注册     |
| **注册表**  | `init/registrate/` | 方块/物品/配方的 Regisrate 注册    |
| **食物定义** | `init/food/`       | 调味料枚举、烤串枚举、效果条目           |
| **数据生成** | `init/data/`       | 语言文件、配方、标签的自动化生成          |
| **方块**   | `content/block/`   | 方块行为、TileEntity、渲染、交互     |
| **物品**   | `content/item/`    | 食物物品、调味料、三明治行为            |
| **配方**   | `content/recipe/`  | 烧烤/串制/组合配方的定义与序列化         |
| **兼容**   | `compat/`          | JEI 配方展示、Jade 信息提示        |
| **事件**   | `event/`           | 村民交易等 Forge 事件            |
| **混入**   | `mixin/`           | 对 Farmer's Delight 切菜板的修改 |
| **工具**   | `util/`            | FD 配置桥接                   |

***

## 5. 核心模块详解

### 5.1 主入口与初始化 (init)

#### `BarbequesDelight.java` — Mod 主入口

* `MODID = "barbequesdelight"` — Mod 唯一标识符

* `REGISTRATE` — `L2Registrate` 实例，用于统一管理所有注册

* `TAB` — 自定义创造模式物品栏，图标为烧烤架

* 构造函数中依次注册：方块 → 调味料 → 烤串 → 物品 → 配方

* 注册三个数据生成器：语言文件、配方、物品/方块标签

* `commonInit()` — 将烤串和烧焦食物注册为堆肥材料

* `communicate()` — 向 Carry On Mod 发送消息，将烧烤架列入黑名单

#### `BBQDClient.java` — 客户端入口

* 注册 `BBQOverlay` 为 HUD 覆盖层，显示在热栏上方

#### `BBQDBlocks.java` — 方块注册

使用 `L2Registrate` 的 Fluent Builder 模式注册方块：

```java
// 注册模式示例
BarbequesDelight.REGISTRATE
    .block("block_id", properties -> ...)  // 创建方块
    .blockstate(...)                        // 方块状态模型
    .item(BlockItem::new).build()           // 物品形式
    .loot(...)                              // 战利品表
    .tag(BlockTags...)                      // 标签
    .register();                            // 最终注册
```

| 方块          | 注册名     | 关键属性            |
| ----------- | ------- | --------------- |
| 烧烤架 (GRILL) | `grill` | 金属材质，镐开采，无碰撞盒遮挡 |
| 食材盆 (BASIN) | `basin` | 木板材质，斧开采        |
| 托盘 (TRAY)   | `tray`  | 木板材质，斧开采，可堆叠    |

每个方块对应一个 `BlockEntityEntry`（带渲染器）。

#### `BBQDItems.java` — 物品注册

| 物品                      | 注册名              | 说明              |
| ----------------------- | ---------------- | --------------- |
| 烧焦食物 (BURNT\_FOOD)      | `burnt_skewer`   | 烧焦产物，中毒+反胃，残留木棍 |
| 烤肉卷饼 (KEBAB\_WRAP)      | `kebab_wrap`     | 营养8，50%几率获得滋养效果 |
| 烤肉三明治 (KEBAB\_SANDWICH) | `kebab_sandwich` | 营养14            |
| 石锅拌饭 (BIBIMBAP)         | `bibimbap`       | 营养16，100%获得舒适效果 |

#### `BBQDRecipes.java` — 配方注册

| 配方类型                | 注册名         | 对应类                     | 说明      |
| ------------------- | ----------- | ----------------------- | ------- |
| `RT_BBQ` / `RS_BBQ` | `grilling`  | `SimpleGrillingRecipe`  | 烧烤架烹饪配方 |
| `RT_SKR` / `RS_SKR` | `skewering` | `SimpleSkeweringRecipe` | 食材盆串制配方 |
| `COMBINE`           | `combine`   | `CombineItemRecipe`     | 工作台组合配方 |

#### `BBQSeasoning.java` — 调味料枚举

| 枚举值            | 注册名                   | 类型        | 特殊效果                   |
| -------------- | --------------------- | --------- | ---------------------- |
| CUMIN          | `cumin_powder`        | 粉末 (耐久64) | 食用后回复1❤️               |
| PEPPER         | `pepper_powder`       | 粉末 (耐久64) | 食物变为快速食用               |
| CHILI          | `chili_powder`        | 粉末 (耐久64) | 食用后受火焰伤害，食物始终可吃        |
| HONEY\_MUSTARD | `honey_mustard_sauce` | 酱料 (耐久16) | 效果持续时间减半，概率翻倍          |
| BUFFALO        | `buffalo_sauce`       | 酱料 (耐久16) | 效果等级+1，持续时间减半，食用后受火焰伤害 |
| BARBEQUE       | `barbeque_sauce`      | 酱料 (耐久16) | 效果持续时间翻倍               |

#### `BBQSkewers.java` — 烤串枚举

| 枚举值           | 生串                        | 熟串                            | 营养 | 饱和度 | 特殊效果               |
| ------------- | ------------------------- | ----------------------------- | -- | --- | ------------------ |
| COD           | `raw_cod_skewer`          | `grilled_cod_skewer`          | 7  | 1.0 | -                  |
| SALMON        | `raw_salmon_skewer`       | `grilled_salmon_skewer`       | 7  | 1.0 | -                  |
| CHICKEN       | `raw_chicken_skewer`      | `grilled_chicken_skewer`      | 7  | 0.7 | -                  |
| MUSHROOM      | `raw_mushroom_skewer`     | `grilled_mushroom_skewer`     | 6  | 0.4 | -                  |
| BEEF          | `raw_beef_skewer`         | `grilled_beef_skewer`         | 8  | 0.7 | 力量 (1800s, 50%)    |
| LAMB          | `raw_lamb_skewer`         | `grilled_lamb_skewer`         | 12 | 0.8 | 生命恢复 (1800s, 50%)  |
| RABBIT        | `raw_rabbit_skewer`       | `grilled_rabbit_skewer`       | 10 | 0.8 | 跳跃提升 (1800s, 100%) |
| PORK\_SAUSAGE | `raw_pork_sausage_skewer` | `grilled_pork_sausage_skewer` | 8  | 0.7 | 抗性提升 (1800s, 50%)  |
| POTATO        | `raw_potato_skewer`       | `grilled_potato_skewer`       | 6  | 0.6 | 滋养 (1800s, 50%)    |
| VEGETABLE     | `raw_vegetable_skewer`    | `grilled_vegetable_skewer`    | 5  | 0.5 | 生命恢复 (1200s, 25%)  |

***

### 5.2 方块系统 (content.block)

#### 架构设计：L2ModularBlock 组合模式

本 Mod 使用 **L2ModularBlock** 库，采用**组合模式**替代传统的方块类继承。每个方块由一个 `DelegateBlock` 和多个 `BlockMethod` 接口实现组成：

```
DelegateBlock (委托方块)
  ├── BlockMethod 1: ShapeBlockMethod       → 碰撞箱形状
  ├── BlockMethod 2: OnClickBlockMethod     → 点击交互
  ├── BlockMethod 3: AnimateTickBlockMethod → 粒子/声音动画
  ├── BlockMethod 4: BlockEntityBlockMethod → 绑定 TileEntity
  └── ... 更多 BlockMethod
```

#### `GrillBlock.java` — 烧烤架行为

| 方法              | 功能                               |
| --------------- | -------------------------------- |
| `getShape()`    | 返回镂空箱体碰撞箱（底部1-15，顶部留空）           |
| `animateTick()` | 烹饪时随机播放滋滋声                       |
| `onClick()`     | 右键交互逻辑：空手取物 / 潜行翻面 / 撒调味料 / 放置生串 |

**右键交互状态机：**

```
玩家右键点击烧烤架
  ├─ 手上物品为空 → 失败
  ├─ 目标槽位为空 → 放置生串（需匹配配方）
  ├─ 目标有物品:
  │   ├─ 潜行 → 翻面（需烹饪时间过半）
  │   ├─ 手持调味料 → 撒调味料
  │   └─ 否则 → 取出物品
  └─ 返回结果
```

#### `GrillBlockEntity.java` — 烧烤架 TileEntity

核心数据结构 — `ItemEntry` 内部类：

| 字段         | 类型        | 说明            |
| ---------- | --------- | ------------- |
| `time`     | int       | 当前烹饪进度 (tick) |
| `duration` | int       | 总烹饪时间 (tick)  |
| `flipped`  | boolean   | 是否已翻面         |
| `burnt`    | boolean   | 是否烧焦          |
| `stack`    | ItemStack | 当前物品          |

**`ItemEntry.tick()`** **— 烹饪逻辑：**

```
每 tick 调用:
  ├─ 物品为空 → 返回
  ├─ 未加热 → time 递减 (不低于0)
  ├─ 已加热:
  │   ├─ time++
  │   ├─ time >= duration*2 → 烧焦 (变为 burnt_skewer)
  │   ├─ time == duration 且已翻面 → 完成烹饪: 查找配方, 输出熟串
  │   └─ 否则 → 继续烹饪
```

**`ItemEntry.addItem()`** **— 放置物品：**

1. 用该物品匹配烧烤配方 (`RT_BBQ`)
2. 匹配成功则记录 `duration`，重置 `time`/`flipped`/`burnt`
3. 物品数量减1

**`ItemEntry.flip()`** **— 翻面：**

* 条件：`time >= duration/2` 且尚未翻面且未烧焦

* 效果：设置 `flipped = true`，`time = duration/2`

**`isHeated()`** **— 加热检测：**

* 检查方块状态的 `CAMPFIRE` 属性（下方是否有营火）

* 或委托 `HeatableBlockEntity.isHeated()`（检测下方热源方块）

**`isBarbecuing()`** **— 是否正在烧烤：**

* 有热源且任意槽位有物品

#### `GrillBlockEntityRenderer.java` — 烧烤架渲染

* 在每个槽位渲染对应的物品

* 物品平放（绕 X 轴旋转90度），根据翻面状态旋转180度

* 使用槽位上方光照计算亮度

#### `GrillPlace.java` — 烧烤架放置与营火集成

管理 `has_campfire` 方块状态属性：

| 方法                       | 功能                  |
| ------------------------ | ------------------- |
| `getStateForPlacement()` | 放置时检测下方是否有点亮的营火     |
| `onClick()`              | 右键放入营火（手持营火右键侧边）    |
| `getLightValue()`        | 有营火时发光等级15          |
| `buildLoot()`            | 有营火时额外掉落营火          |
| `buildModel()`           | 复合模型：有营火时显示营火+烧烤架组合 |

#### `GrillBlockItem.java` — 烧烤架物品

重写 `updatePlacementContext()`：当点击营火时，替换营火而不是放在旁边。

#### `BasinBlock.java` — 食材盆

* 两个碰撞箱变体（X轴/Z轴方向），根据方块朝向切换

* 内部镂空，用于放置食材

#### `BasinBlockEntity.java` — 食材盆逻辑

**`specialClick()`** **— 核心交互：**

1. 取主手物品、槽位物品、副手物品组成3槽容器
2. 匹配串制配方 (`RT_SKR`)
3. 匹配成功后合成输出，放入玩家手中或背包

#### `TrayBlock.java` — 托盘

* 薄板形状（高度3像素）

* `support` 属性：下方无支撑时显示支架

* 可堆叠放置（下层提供支撑）

#### `TrayBlockEntity.java` — 托盘实体

* 3个物品槽位，纯存储，无特殊交互

#### `StorageTile.java` — 存储接口

定义存储方块的通用行为：

| 方法                                 | 功能           |
| ---------------------------------- | ------------ |
| `getStack(i)`                      | 获取槽位物品       |
| `setStack(i, stack)`               | 设置槽位物品       |
| `specialClick(player, i, hand)`    | 特殊交互（子类实现）   |
| `insert(level, i, handStack, all)` | 插入物品（合并同类物品） |

#### `StorageTileBlockEntity.java` — 存储方块抽象实体

* 持有 `StorageTileContainer`（基于 `BaseContainer`）

* 暴露 Forge `IItemHandler` 能力

* 提供 `notifyTile()` 同步数据到客户端

#### `StorageTileContainer.java` — 存储容器

继承 `BaseContainer`，提供基础的物品存储功能。

#### `StorageTileRenderer.java` — 存储方块渲染器

* 根据物品数量渲染不同数量的模型副本（最多12个）

* 随机偏移和旋转，呈现堆叠效果

* 基于物品哈希的伪随机种子，保持一致性

#### `BlockSlot.java` — 槽位命中检测接口

**`getSlotForHitting()`** — 根据射线命中位置计算槽位索引：

* 将方块的面根据朝向映射到一维坐标

* 按比例计算命中哪个槽位

**`getOffset()`** — 计算槽位的渲染偏移量。

#### `ClickStorageMethod.java` — 点击交互方法

通用的存储方块点击交互：

1. 计算命中槽位
2. 手持物品非空 → 尝试插入
3. 尝试 `specialClick()`（子类自定义）
4. 空手且槽位有物品 → 取出

#### `BBQOverlay.java` — HUD 覆盖层

* 当玩家看向烧烤架时，如果某个槽位可翻面，在屏幕中央显示 "Sneak right click to flip skewer" 提示

***

### 5.3 物品系统 (content.item)

#### `FoodItem.java` — 食物基类

* 继承 `Item`，提供食物效果提示工具

* 静态方法 `getFoodEffects()` 用于格式化食物状态效果显示

* 根据 Farmer's Delight 配置决定是否显示工具提示

#### `BBQSkewerItem.java` — 烤串物品

**调味料系统：**

* 通过 NBT 键 `"seasoning"` 存储调味料名称

* `getSeasoning()` — 从 NBT 反序列化调味料枚举

* 动态修改 `FoodProperties`：

  * 读取基础属性

  * 遍历每个效果，调用 `seasoning.appendEffect()` 修改

  * 调用 `seasoning.modify()` 修改元属性

* 食用后调用 `seasoning.onFinish()` 触发额外效果

* 名称渲染：带有调味料前缀和颜色

#### `SeasoningItem.java` — 调味料物品

**`sprinkle()`** **— 撒调味料：**

1. 检查目标物品是否为烤串且尚未调味
2. 在 NBT 中写入调味料名称
3. 播放声音和粒子效果
4. 消耗耐久度

**`canSprinkle()`** **— 判断能否撒调味料：**

* 物品不能为空

* 必须是 `BBQSkewerItem` 实例

* 尚未有调味料

#### `BBQSandwichItem.java` — 三明治物品

**组合系统：**

* 通过 NBT 键 `"Skewers"` 存储包含的烤串列表

* `getFoodProperties()` — 合并所有包含烤串的食物效果（去重）

* `finishUsingItem()` — 食用时触发所有烤串的 `applyEffects()`

* 工具提示显示包含的烤串名称

***

### 5.4 配方系统 (content.recipe)

#### 配方架构

```
BaseRecipe (L2Library)
  ├── GrillingRecipe<T>          # 烧烤配方抽象
  │   └── SimpleGrillingRecipe   # 具体实现: ingredient + output + time
  ├── SkeweringRecipe<T>         # 串制配方抽象
  │   └── SimpleSkeweringRecipe  # 具体实现: tool + ingredient + side + output
  └── AbstractShapelessRecipe    # 无序合成抽象
      └── CombineItemRecipe      # 组合物品配方: 保留烤串 NBT
```

#### `SimpleGrillingRecipe.java` — 烧烤配方

| 字段               | 类型         | 说明          |
| ---------------- | ---------- | ----------- |
| `ingredient`     | Ingredient | 输入食材（生串）    |
| `output`         | ItemStack  | 输出物品（熟串）    |
| `barbecuingTime` | int        | 烧烤时间 (tick) |

* `matches()` — 匹配1槽容器

* `assemble()` — 返回输出的副本

#### `SimpleSkeweringRecipe.java` — 串制配方

| 字段                | 类型         | 说明        |
| ----------------- | ---------- | --------- |
| `tool`            | Ingredient | 主手工具（木棍）  |
| `ingredient`      | Ingredient | 食材盆中的主要食材 |
| `ingredientCount` | int        | 主要食材消耗数量  |
| `side`            | Ingredient | 副手配料      |
| `sideCount`       | int        | 配料消耗数量    |
| `output`          | ItemStack  | 输出物品（生串）  |

* `matches()` — 匹配3槽容器（主手/盆/副手）

* `assemble()` — 消耗材料并输出

#### `CombineItemRecipe.java` — 组合物品配方

* 继承 `AbstractShapelessRecipe`，用于工作台合成

* `assemble()` — 保留输入中所有烤串的 NBT 数据到输出物品的 `"Skewers"` 列表

#### 配方构建器

| 构建器                        | 用途                               |
| -------------------------- | -------------------------------- |
| `GrillingRecipeBuilder`    | 构建烧烤配方，设置 ingredient/output/time |
| `SkeweringRecipeBuilder`   | 构建串制配方，设置 tool/main/side/output  |
| `CombineItemRecipeBuilder` | 构建组合配方                           |

***

### 5.5 兼容层 (compat)

#### JEI 集成 (`compat/jei/`)

| 类                     | 用途                             |
| --------------------- | ------------------------------ |
| `BBQDJeiPlugin`       | JEI 插件入口，注册配方分类和催化剂            |
| `GrillRecipeCategory` | 烧烤配方展示：输入 → 输出，显示烧烤时间          |
| `BasinRecipeCategory` | 串制配方展示：主手工具 / 盆中食材 / 副手配料 → 输出 |

* 催化剂：烧烤架（烧烤配方）、食材盆（串制配方）

* 配方来源：直接从 `RecipeManager` 拉取

#### Jade 集成 (`compat/jade/`)

| 类            | 用途                                          |
| ------------ | ------------------------------------------- |
| `JadeCompat` | Jade 插件入口，注册 `GrillInfo` 到 `DelegateBlock`  |
| `GrillInfo`  | 在 Jade 提示中显示烧烤架每个槽位的物品和烹饪状态（烹饪中/可翻面/已烤熟/烧焦） |

***

### 5.6 事件与混入 (event & mixin)

#### `BBQGeneralEventHandlers.java` — 村民交易事件

* 监听 `VillagerTradesEvent`

* 为屠夫村民 (Butcher) 的 2 级交易添加自定义调味料（`isCustomSeasoning() == true`）

* 价格：12 绿宝石，每次交易 4 个

#### `CuttingBoardBlockMixin.java` — 切菜板混入

* 目标：`CuttingBoardBlock.use()` 方法

* 注入点：`HEAD`，可取消

* 功能：允许在 Farmer's Delight 的切菜板上撒调味料

* 当手持 `SeasoningItem` 且切菜板上的物品可调味时，执行撒调味料并取消原交互

***

### 5.7 数据生成 (init.data)

#### `BBQLangData.java` — 语言文件生成

枚举定义所有本地化键值对：

| 分类      | 数量 | 示例                                                              |
| ------- | -- | --------------------------------------------------------------- |
| 调味料名称   | 6  | `seasoning.chili` → "Chili Flavored"                            |
| 调味料描述   | 6  | `tooltip.cumin` → "Skewers with it will make you feel better\~" |
| JEI 文本  | 6  | `jei.grilling` → "Grilling"                                     |
| Jade 文本 | 4  | `jade.cook` → "Cooking: %ss"                                    |
| 其他      | 2  | 翻面提示、概率效果文本                                                     |

#### `BBQRecipeGen.java` — 配方生成

自动生成以下配方：

* **10 个烧烤配方**：每种烤串的生→熟转换，时间 4-9 秒不等

* **10 个串制配方**：木棍 + 食材 + 可选配料 → 生串

* **1 个合成配方**：烧烤架（铁锭 + 铁活板门）

* **3 个组合配方**：石锅拌饭、烤肉三明治、烤肉卷饼

* **2 个切石机配方**：原木 → 托盘 (4个) / 食材盆 (2个)

#### `BBQTagGen.java` — 标签生成

| 标签                  | 用途           |
| ------------------- | ------------ |
| `grilled_skewers`   | 所有熟串         |
| `raw_skewers`       | 所有生串         |
| `skewer_vegetables` | 可做蔬菜串的蔬菜     |
| `skewer_fruits`     | 可做蔬菜串的水果     |
| Carry On 黑名单        | 烧烤架和食材盆不可被搬起 |

***

### 5.8 工具类 (util)

#### `FDConfig.java` — Farmer's Delight 配置桥接

* 提供 `addTooltip()` 方法

* 尝试读取 Farmer's Delight 的 `ENABLE_FOOD_EFFECT_TOOLTIP` 配置

* 失败时默认返回 `true`

***

## 6. 依赖关系

### 依赖层级

```
Barbeque's Delight
  ├── Forge 47.1.3+ (required)
  │   └── Minecraft 1.20.1 (required)
  ├── Farmer's Delight 1.20.1-1.2.2+ (required)
  │   └── 提供: 声音、效果、标签、方块、配置
  ├── L2Library 2.4.25+ (embedded via JarJar)
  │   ├── L2Serial 1.2.2+ (embedded)
  │   └── L2ModularBlock 1.1.1+ (embedded)
  ├── Registrate MC1.20-1.3.3+ (embedded via JarJar)
  ├── MixinExtras 0.2.0-beta.8+ (embedded via JarJar)
  ├── JEI 15.2.0.23+ (optional)
  ├── Jade (optional)
  ├── Curios 5.2.0+ (optional)
  └── Carry On (optional, runtime)
```

### 依赖类型

| 依赖               | 类型 | 说明           |
| ---------------- | -- | ------------ |
| Forge            | 强制 | Mod 加载器      |
| Farmer's Delight | 强制 | 核心玩法联动       |
| L2Library        | 内嵌 | 序列化/注册表/配方框架 |
| Registrate       | 内嵌 | 简化注册与数据生成    |
| JEI              | 可选 | 配方查询         |
| Jade             | 可选 | 方块信息提示       |
| Curios           | 可选 | 饰品栏          |

***

## 7. 项目运行方式

### 环境要求

* JDK 17+

* 至少 3GB 内存分配 (`org.gradle.jvmargs=-Xmx3G`)

### 常用 Gradle 命令

```bash
# 运行客户端
gradlew runClient

# 运行服务端
gradlew runServer

# 生成数据（配方、模型、语言文件等）
gradlew runData

# 构建 Mod JAR
gradlew build

# 构建 Slim JAR（不含内嵌依赖）
gradlew jar

# 发布到 CurseForge
gradlew publishCurseForge

# 发布到 Modrinth
gradlew modrinth
```

### 运行配置

构建脚本中定义了三种运行配置：

| 配置       | 描述                                                              |
| -------- | --------------------------------------------------------------- |
| `client` | 启动 Minecraft 客户端，工作目录 `run/`                                    |
| `server` | 启动 Minecraft 服务端                                                |
| `data`   | 运行数据生成器，输出到 `src/generated/resources/`，参考 `src/main/resources/` |

### 开发工作流

1. 修改代码后运行 `runData` 生成资源文件
2. 运行 `runClient` 测试修改
3. 构建后 JAR 文件位于 `build/libs/barbequesdelight-{version}.jar`

***

## 8. 数据流与核心流程

### 8.1 烧烤流程

```
玩家放置烧烤架 (GrillBlockItem)
  └─ 检测下方是否有营火 → 设置 has_campfire 状态
      └─ 右键放入生串
          └─ GrillBlockEntity.addItem()
              └─ 匹配 GrillingRecipe → 记录 duration
                  └─ 每 tick:
                      ├─ 加热? → time++ (未加热 → time--)
                      ├─ time >= duration/2 → 可翻面
                      ├─ 玩家翻面 (flip)
                      │   └─ time = duration/2, flipped = true
                      ├─ time == duration → 完成 (查找配方, 输出熟串)
                      └─ time >= duration*2 → 烧焦 (变为 burnt_skewer)
```

### 8.2 串制流程

```
玩家手持木棍 + 副手配料
  └─ 右键点击食材盆
      └─ ClickStorageMethod.onClick()
          ├─ 计算命中槽位
          ├─ 插入主手物品到槽位
          └─ BasinBlockEntity.specialClick()
              └─ 匹配 SkeweringRecipe
                  ├─ 主手: 木棍
                  ├─ 盆中: 主要食材
                  └─ 副手: 配料
                  └─ 成功 → 消耗材料, 输出生串到玩家手中
```

### 8.3 调味料系统

```
玩家手持调味料
  ├─ 右键点击烧烤架上的烤串
  │   └─ SeasoningItem.sprinkle()
  │       └─ 写入 NBT "seasoning" 字段
  ├─ 右键点击切菜板上的物品 (Mixin)
  │   └─ 同上
  └─ 食用调味后的烤串
      └─ BBQSkewerItem.getFoodProperties()
          └─ 读取 NBT → 调用 BBQSeasoning.appendEffect()
              ├─ HONEY_MUSTARD: 概率×2, 持续时间/2
              ├─ BUFFALO: 等级+1, 持续时间/2
              └─ BARBEQUE: 持续时间×2
          └─ 调用 BBQSeasoning.modify()
              ├─ CHILI: alwaysEat
              └─ PEPPER: fast
          └─ 食用后调用 BBQSeasoning.onFinish()
              ├─ CHILI: 受到火焰伤害
              ├─ BUFFALO: 受到火焰伤害
              └─ CUMIN: 回复生命
```

### 8.4 组合物品流程

```
工作台合成
  └─ CombineItemRecipe.assemble()
      └─ 遍历所有输入
          └─ 匹配 grilled_skewers 标签 → 保存 NBT 到 ListTag
              └─ 存入输出物品的 "Skewers" 字段
                  └─ 食用时 BBQSandwichItem.finishUsingItem()
                      └─ 遍历所有记录的烤串 → 逐一触发 applyEffects()
```

***

## 9. 扩展开发指南

### 添加新的烤串

1. 在 `BBQSkewers` 枚举中添加新条目（设置营养值、饱和度、是否为肉、效果）
2. 在 `BBQRecipeGen` 中添加 `grillSkewer()` 和 `craftSkewer()` 调用
3. 添加对应的纹理和模型资源文件

### 添加新的调味料

1. 在 `BBQSeasoning` 枚举中添加新条目
2. 设置语言键、颜色、是否为粉末/酱料
3. 实现 `onFinish()` / `modify()` / `appendEffect()` 方法
4. 在 `BBQLangData` 中添加对应的语言键

### 添加新的方块

1. 实现对应的 `BlockMethod` 接口
2. 在 `BBQDBlocks` 中使用 `REGISTRATE.block()` 注册
3. 添加对应的 `BlockEntity` 和渲染器（如需要）
4. 添加纹理和模型资源

### 添加新的配方类型

1. 继承 `BaseRecipe` 实现自定义配方类
2. 实现对应的 `BaseRecipeBuilder` 构建器
3. 在 `BBQDRecipes` 中注册 `RecipeType` 和 `RecipeSerializer`
4. 在 `BBQRecipeGen` 中添加数据生成
5. 在 JEI 兼容层添加对应的 `RecipeCategory`

