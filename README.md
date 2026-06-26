# Amethyst Farm Bot（1.21.1 – 1.21.11）

基于 **Fabric** 与 **Carpet 假人 API** 的紫水晶簇自动农场模组。

本仓库使用 [Stonecutter](https://github.com/kikugie/stonecutter) 多版本构建：**每个 Minecraft 版本对应独立 jar**，不可混用。

## 支持版本

| Minecraft | jar 文件名 |
|-----------|------------|
| 1.21.1 – 1.21.11 | `amethystfarm-1.0.3+{版本}.jar` |

构建产物汇总目录：`build/libs/1.0.3/{版本}/`

## 依赖

各版本依赖见 `stonecutter.properties.toml`。通用要求：

- Fabric Loader 0.16.9+
- 对应版本的 Fabric API
- [Carpet Mod](https://github.com/gnembon/fabric-carpet)（**硬依赖**，服务端与客户端均需安装）

## 版本兼容说明

| 范围 | 客户端线框预览 |
|------|----------------|
| 1.21.1 – 1.21.3 | 无（旧版无 `ShapeRenderer`） |
| **1.21.9** | **无**（Fabric API 暂未提供世界渲染事件） |
| 1.21.4 – 1.21.8、1.21.10 – 1.21.11 | 有 |

其余功能（假人挖掘、命令、GUI、粒子预览等）在所有版本可用。

## 安装方式

| 场景 | 安装位置 | 说明 |
|------|----------|------|
| 完整体验 | 服务端 + 客户端 | 假人挖掘 + 线框预览 + GUI |
| 仅服务端 | 只装服务端 | 假人照常工作；**未装 mod 的玩家也可进服**（用 `/af` 命令） |
| 客户端可选 | 按需安装 | 无 mod 可进服；装 mod 后才有 GUI 与线框预览 |

- 服务端通过注册表同步过滤，未装本模组的 Fabric 玩家**可以进服**（不向客户端同步 `amethystfarm` 菜单项）
- GUI（`/af gui`）与客户端预览**仍需安装**本模组
- 假人逻辑、命令等功能在服务端运行，与玩家是否装 mod 无关
- 服务端与客户端若安装本模组，均须同时安装 Carpet
- **jar 必须与 MC 版本完全一致**（例如 1.21.6 服只能用 `+1.21.6` 的 jar）

## 功能

- 指定 Carpet 假人**自动挖掘附近**（手长范围内）的紫水晶
- **扫描紫水晶**：预览假人周围可挖掘紫水晶（粒子 + 客户端线框）
- **挖掘附近紫水晶**：自动采集手长内的可挖掘紫水晶（小/中/大芽与成熟簇，不破坏母岩）
- **单点锁定挖掘**：对准视线内最近的可挖掘紫水晶进行挖掘
- GUI 控制面板与自制命令（`/amethystfarm` 或简写 `/af`）

## 快速开始

1. 生成 Carpet 假人并站在紫水晶农场附近：
   ```
   /player Bot spawn
   ```
2. 令假人开始挖掘附近紫水晶：
   ```
   /af harvest Bot
   ```
   或：
   ```
   /af mode HARVEST Bot
   ```
3. 停止并关闭该假人的农场控制（不再影响其 Carpet 连续动作）：
   ```
   /af stop Bot
   ```
   或：
   ```
   /af off Bot
   ```
4. 打开 GUI（需客户端安装 mod）：
   ```
   /af gui Bot
   ```

## 命令

主命令：`/amethystfarm`，简写：`/af`

| 命令 | 说明 |
|------|------|
| `/af harvest <假人>` | 启用农场并令假人挖掘附近紫水晶 |
| `/af stop <假人>` / `/af off <假人>` | 关闭该假人农场控制，释放挖掘状态 |
| `/af on <假人>` | 启用该假人农场控制（保留当前模式） |
| `/af gui <假人>` | 打开控制 GUI |
| `/af mode <IDLE\|SCAN\|HARVEST\|LOCK_MINE> <假人>` | 设置工作模式（非 IDLE 时自动启用农场） |
| `/af status <假人>` | 查看状态 |
| `/af list` | 列出所有在线假人及启用状态、模式 |
| `/af batch mode <模式> <假人...>` | 批量设置工作模式 |
| `/af batch harvest <假人...>` | 批量令假人开始挖掘 |
| `/af rule list` | 列出所有模组规则 |
| `/af rule get <规则>` | 查看规则当前值 |
| `/af rule set <规则> <值>` | 设置规则 |

## 模组规则

| 规则 | 说明 | 默认 |
|------|------|------|
| `enabled` | 总开关 | true |
| `autoHarvest` | 允许自动采集 | true |
| `scanPreview` | 扫描模式粒子预览 | true |
| `scanInterval` | 扫描间隔（刻） | 20 |
| `scanRadius` | 附近扫描半径（以假人为中心，方块） | 8 |
| `maxScanVolume` | 最大扫描体积 | 8000 |
| `previewCrystalLimit` | 同步到客户端的预览紫水晶上限 | 96 |
| `maxRenderDistance` | 客户端描边渲染距离（方块） | 32 |
| `maxCrystalOutlines` | 客户端最多绘制描边数 | 48 |
| `miningReach` | 假人挖掘手长（方块） | 4.5 |
| `multiBotMining` | 多假人并行分配（避免抢挖同一簇） | true |

### 与其他假人动作隔离

- **未纳入农场**的假人（从未 `/af on` / `/af harvest`，或已 `/af off`）：mod **不会进入其 tick**，不创建存档、不调用 `ActionPack`，可正常跑 Carpet 连续动作（`forward`、`attack`、`use` 等）。
- **正在挖紫水晶的假人**只控制自己的 `ActionPack`；**不会**影响其他假人。
- 多个农场假人可同时挖掘（`multiBotMining` 认领不同紫水晶，避免抢挖）。
- 关闭农场控制：`/af off <假人>` 或 GUI「农场总开关」/「停止」。

### 多假人并行挖掘

将多个假人分散站在农场不同位置，各自挖掘**手长范围内**的紫水晶，认领系统自动避免抢挖同一簇。

```
/player Bot1 spawn
/player Bot2 spawn
/player Bot3 spawn
/af batch harvest Bot1 Bot2 Bot3
/af list
```

假人配置按**名称**持久化（`data/amethystfarm_profiles.dat`），`/player Bot1 kill` 后同名 respawn 会自动恢复模式与白名单。

建议将假人分散站在农场不同位置，使各自手长覆盖不同区域。各假人的扫描 tick 会自动错开，减少同 tick 争抢。

示例：

```
/af rule set enabled false
/af rule set scanInterval 40
/af rule set scanRadius 10
/af rule set miningReach 4.5
/af rule set multiBotMining true
```

规则会保存至世界目录 `data/amethystfarm_settings.dat`。

## 构建（Stonecutter）

需要 **JDK 21**。

```bash
cd amethystfarm-1.21

# 构建全部 11 个版本
gradle build

# 构建单个版本
gradle :1.21.6:build

# 将 jar 复制到 build/libs/1.0.3/{版本}/
gradle :1.21.6:buildAndCollect

# 汇总全部版本 jar
gradle :1.21.1:buildAndCollect :1.21.2:buildAndCollect ... :1.21.11:buildAndCollect
```

切换 Stonecutter 活动版本：编辑 `stonecutter.gradle.kts` 中的 `kotlin { vcsVersion = ... }`（当前为 `1.21.11`）。

源码使用 Stonecutter 条件编译（`//? if >=x.xx`）处理 API 差异，兼容层位于 `util/ModIds.java`、`util/ModCompat.java`、`util/NbtCompat.java` 及客户端渲染/GUI 类。

## 技术说明

- 通过 Mixin 增强 `EntityPlayerMPFake`（`remap = true` 注入 tick），不覆写 Minecraft 核心类
- 使用 Carpet `EntityPlayerActionPack` 模拟玩家挖掘动作
- 成熟判定：方块为 `small/medium/large_amethyst_bud` 或 `amethyst_cluster`，且附着方块为 `budding_amethyst`
- 自动采集时优先挖掘成熟簇，其次大芽 → 中芽 → 小芽
- 仅挖掘手长（`miningReach`）内的紫水晶，默认 4.5 格（与生存模式一致）
- 多假人模式下每个紫水晶簇同时只会分配给一个假人
- 预览网络包使用增量同步、相对坐标与实体 ID，仅发送给已安装模组的附近玩家
