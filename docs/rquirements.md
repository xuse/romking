# Romking 功能需求与进度

## 概述

Romking 是一个游戏 ROM 文件扫描与管理应用程序，用于管理复古游戏 ROM 集合（约 70TB）。核心场景：
- 扫描 TF 卡或 ROM 集成包目录，建立数据库
- 管理和浏览 ROM 仓库
- 导入 No-Intro 标准 ROM 数据库，识别版本、去重
- 将归档仓库导出到 TF 卡，生成模拟器前端可识别的游戏列表

---

## 一、信息收集与建档

### 1.1 扫描 TF 卡/ROM 集成包目录 → 创建 INSTANCE 仓库

| 子项 | 状态 | 说明 |
|------|------|------|
| 递归扫描根目录下的平台子目录 | ✅ 已完成 | 自动识别平台名（含别名如 FC→NES） |
| 区分 ROM 文件、媒体文件 | ✅ 已完成 | 按扩展名分类，媒体文件单独入 media_file 表 |
| 解析 EE (EmulationStation) gamelist.xml | ✅ 已完成 | 提取 name/image/video/desc 等元数据 |
| 解析天马G格式 | ⬜ 未开始 | 待提供样本文件后实现 |
| ZIP 文件处理（读取内部 ROM 信息） | ✅ 已完成 | 单文件/多文件 ZIP 均支持 |
| 计算 MD5（取 ZIP 内主体 ROM） | ✅ 已完成 | 可通过选项开关 |
| 计算 CRC（ZIP 文件直接从头获取） | ✅ 已完成 | 可通过选项开关，零解压开销 |
| 过滤说明文件、空白文件、BIOS 文件 | ✅ 已完成 | 按扩展名忽略 txt/md/xml/ini/cfg 等 |
| 扫描无 gamelist 的目录（可选） | ✅ 已完成 | scanWithoutGamelist 选项控制 |
| 从文件名猜测区域（J/U/E/中文） | ✅ 已完成 | |
| 扫描时自动匹配 No-Intro ROM 数据库 | ✅ 已完成 | MD5/CRC+size 匹配，补充 gameid 和 region |
| 元数据补全（跨仓库 MD5 匹配） | ⬜ 未开始 | |

### 1.2 扫描 ROM 仓库目录 → 创建 ARCHIVE 仓库

| 子项 | 状态 | 说明 |
|------|------|------|
| 扫描逻辑同上，建立 Archive 仓库 | ✅ 已完成 | 通过 RomScanOptions.repoType 指定 |

### 1.3 元数据补全

| 子项 | 状态 | 说明 |
|------|------|------|
| 利用 No-Intro 数据库自动补全 gameid/region | ✅ 已完成 | 扫描时自动匹配 |
| 利用 INSTANCE 仓库元数据补全归档仓库 | ⬜ 未开始 | |
| 空白数据入库后待人工更新 | ✅ 已完成 | 默认值填充，可通过 UI 编辑 |

### 1.4 增量扫描与 Hash 计算优化

**背景**：仓库规模约 70TB，MD5 全量计算不现实。

**快速变更检测规则**（重扫描时）：
- 文件 大小 + 修改日期 与数据库记录一致 → 视为未变更，跳过 hash 计算
- ZIP 文件且 CRC（从 ZIP 头读取）与记录一致 → 跳过 MD5 计算
- 仅当快速检测发现不一致时，才计算 MD5

**MD5 计算时机**：
- 首次扫描入库（由 computeMd5 选项控制，大文件平台建议关闭）
- 归档时（确保归档准确性）
- 用户手动触发"完整校验"
- 与 known_rom 匹配时（CRC+size 能唯一匹配则不需要 MD5）

| 子项 | 状态 | 说明 |
|------|------|------|
| 增量扫描选项（incremental） | ✅ 已完成 | RomScanOptions.incremental 字段 |
| 增量扫描逻辑（跳过未变更文件） | ⬜ 未开始 | 基于 size+lastModified 判断 |
| ZIP 文件 CRC 快速检测 | ✅ 已完成 | 从 ZIP 头直接读取，零解压 |
| 大文件 MD5 延迟计算 | ⬜ 未开始 | 首次扫描可选跳过，归档时补算 |

---

## 二、ROM 维护

### 2.1 ROM 归档（INSTANCE → ARCHIVE）

**业务规则：**
- 归档 = 复制物理文件 + 创建数据库条目，文件复制成功才创建记录
- 基于 MD5 去重：目标 ARCHIVE 中已有相同 MD5 的 ROM 则跳过
- 源设备（游戏卡）必须在线，归档时源文件必须可达
- 支持按 label 批量归档：源 INSTANCE 的所有平台目录自动匹配目标 ARCHIVE 中对应 platform 的目录
- 如果目标 ARCHIVE 中没有对应 platform 的目录，自动创建

**数据模型：**
- `RomFile.fileStatus` 字段：FileStatus 枚举 OK(0) / MISSING(1) / CORRUPTED(2)
- 归档成功的条目 fileStatus = OK
- INSTANCE 仓库条目 fileStatus 默认 OK，仅在用户触发校验时更新

| 子项 | 状态 | 说明 |
|------|------|------|
| FileStatus 枚举 | ✅ 已完成 | OK / MISSING / CORRUPTED |
| RomFile 增加 fileStatus 字段 | ✅ 已完成 | 默认 OK |
| ArchiveRomTask 加文件复制逻辑 | ✅ 已完成 | 复制成功才建条目 |
| 按 label 批量归档 | ✅ 已完成 | RomArchiveService.archiveByLabel() |
| 自动匹配目标平台目录 | ✅ 已完成 | 按 platform 匹配，不存在则创建 |
| 归档 UI 改为 label 选择 | ✅ 已完成 | ArchiveView + ArchiveForm |

### 2.2 校验 ROM 文件

**业务规则：**
- 对 INSTANCE 和 ARCHIVE 仓库都适用
- 用户手动触发（点击"校验ROM文件"按钮）
- 支持两种模式：快速校验（CRC）和完整校验（MD5）
- 校验只更新 fileStatus，不删除记录，不删除文件
- 之前标记为 MISSING/CORRUPTED 的，校验时如果恢复正常则回到 OK

**校验流程：**
1. **预检阶段**：快速扫描目录，统计文件匹配率
2. 如果缺失率 > 70%，暂停任务，提示用户"可能插错卡或目录不对"
3. 用户确认后进入正式校验
4. **快速校验**：ZIP 文件从头读 CRC 比对（零解压），非 ZIP 仅检查存在性
5. **完整校验**：MD5 优先，无 MD5 回退到 CRC
6. 更新 fileStatus 字段

| 子项 | 状态 | 说明 |
|------|------|------|
| VerifyRepoTask 实现 | ✅ 已完成 | 含预检 + 快速/完整两种模式 |
| 预检阶段（匹配率检测） | ✅ 已完成 | >70% 缺失时返回 code=300 |
| 快速校验模式（CRC） | ✅ 已完成 | ZIP 零解压，非 ZIP 用 CRC32 |
| 完整校验模式（MD5） | ✅ 已完成 | 需解压 ZIP 计算 |
| 校验 UI（触发按钮 + 模式选择） | ✅ 已完成 | RomFileListView 中 |
| RomVerifyService | ✅ 已完成 | |
| TaskType.VERIFY | ✅ 已完成 | |
| 导出时跳过异常状态条目 | ⬜ 未开始 | ExportRomTask 改造 |

### 2.3 ROM 标准数据库（No-Intro DAT）

**业务规则：**
- 导入 No-Intro DAT 文件到 `known_rom` 表，用于 ROM 版本识别
- 支持标准 DAT（hash 信息）和 P/C DAT（parent/clone 游戏家族关系）
- 支持直接读取 ZIP 压缩的 DAT 文件
- 按配置过滤平台：默认仅导入 SEGA 全系 + NES/SNES/GBA/3DS/Wii + PS/PS2/PSP
- DAT 文件存放在项目 `no-intro/` 目录（已 gitignore）

**数据模型：**
- `KnownRom`（`known_rom` 表）：md5, crc, sha1, romSize, gameName, romFileName, parentName, region, platform, source, datVersion
- 索引：MD5 唯一索引，CRC+size 索引，platform+parentName 索引

**扫描时自动匹配：**
- 优先 MD5 精确匹配
- 其次 CRC+size 匹配（仅唯一匹配时采用）
- 匹配成功：设 `gameid = parentName`，补充 `region`，`name` 设为 No-Intro 标准名
- `displayName` 保持 gamelist.xml 的中文名不受影响

| 子项 | 状态 | 说明 |
|------|------|------|
| KnownRom 实体 + Repository | ✅ 已完成 | known_rom 表 |
| NoIntroDatParser（XML 解析） | ✅ 已完成 | 支持标准 DAT 和 P/C DAT |
| DatImportService（导入逻辑） | ✅ 已完成 | 支持 ZIP、目录批量、平台过滤 |
| DatImportConfig（平台过滤配置） | ✅ 已完成 | 默认 SEGA+Nintendo+Sony 选定平台 |
| DatManageService（管理服务） | ✅ 已完成 | 提交任务、按 MD5/CRC 查找 |
| ImportDatTask（后台任务） | ✅ 已完成 | TaskType.IMPORT_DAT |
| DAT 导入 UI（/dat-import） | ✅ 已完成 | 菜单"ROM数据库"，统计+导入 |
| 扫描时自动匹配 known_rom | ✅ 已完成 | ScanRomTask.applyKnownRomInfo() |
| P/C 关系导入（parentName） | ✅ 已完成 | 用于游戏家族分组 |

### 2.4 重复检测与版本管理

**业务规则：**
- 通过 `gameid`（= No-Intro parentName）识别同一游戏的不同版本
- 两种重复类型：
  - 完全重复（SAME_MD5）：文件内容完全相同的冗余副本
  - 同游戏多版本（SAME_GAME）：gameid 相同但 MD5 不同的不同版本
- 删除操作仅删数据库记录，不删物理文件

| 子项 | 状态 | 说明 |
|------|------|------|
| RomDuplicateService | ✅ 已完成 | 完全重复 + 同游戏多版本检测 |
| 重复检测 UI（/rom-duplicates） | ✅ 已完成 | 两个 Tab + 详情对话框 + 删除操作 |
| 按 gameid 分组查看版本 | ✅ 已完成 | |
| 删除冗余记录 | ✅ 已完成 | 仅删记录，不删文件 |
| 批量清理（保留一个，删除其余） | ⬜ 未开始 | |
| ROM 格式转换后统一维护 | ⬜ 未开始 | 记录转换前 MD5，指向转换后文件 |

### 2.5 其他维护功能

| 子项 | 状态 | 说明 |
|------|------|------|
| 通过界面维护元数据 | ✅ 已完成 | RomEditForm + 编辑对话框 |
| 标注 Favorite | ✅ 已完成 | 列表中直接点击切换 |
| ROM 文件状态展示 | ✅ 已完成 | Grid 中彩色徽章（正常/缺失/损坏） |

---

## 三、仓库导出

### 3.1 导出流程

| 子项 | 状态 | 说明 |
|------|------|------|
| 从 ARCHIVE 仓库导出到 TF 卡目标目录 | ✅ 已完成 | ExportRomTask |
| 按平台目录组织文件 | ✅ 已完成 | |
| 复制 ROM 文件到目标 | ✅ 已完成 | |
| 复制媒体文件（封面、视频） | ✅ 已完成 | |
| 生成 EE 格式 gamelist.xml | ✅ 已完成 | GameListService.saveXml |
| 生成天马G格式游戏列表 | ⬜ 未开始 | 待样本文件 |

### 3.2 导出模式

| 子项 | 状态 | 说明 |
|------|------|------|
| 覆盖模式（删除目标多余 ROM） | ✅ 已完成 | |
| 增量模式（保留目标现有 ROM） | ✅ 已完成 | |

### 3.3 MD5 校验（导出时目标已有同名文件）

| 子项 | 状态 | 说明 |
|------|------|------|
| MD5 一致 → 跳过不复制 | ✅ 已完成 | |
| MD5 不一致，主仓库有该版本 → 提示不同版本，跳过 | ✅ 已完成 | |
| MD5 不一致，主仓库无该版本 → 发现新版本，不覆盖，留记录 | ✅ 已完成 | 待人工核实 |
| 快速导出（跳过 MD5 校验） | ✅ 已完成 | quickExport 选项 |

### 3.4 导出任务管理

| 子项 | 状态 | 说明 |
|------|------|------|
| 后台异步执行 | ✅ 已完成 | GlobalTaskService 线程池 |
| 进度展示 | ✅ 已完成 | Task.getProgress() |
| 任务结果记录（含失败明细） | ✅ 已完成 | ProcessResult.details |
| 失败后可继续导出 | ✅ 已完成 | 增量模式下重新执行即可 |

---

## 四、UI 视图

### 4.1 仪表盘首页（/）

| 子项 | 状态 | 说明 |
|------|------|------|
| 平台分布统计 | ⬜ 未开始 | |
| 仓库概览 | ⬜ 未开始 | |
| 媒体覆盖率 | ⬜ 未开始 | |
| 最近活动 | ⬜ 未开始 | |
| 健康状态 | ⬜ 未开始 | |

### 4.2 扫描任务管理（/task-list）

| 子项 | 状态 | 说明 |
|------|------|------|
| 任务列表 Grid | ✅ 已完成 | |
| 扫描对话框 | ✅ 已完成 | |
| 提交扫描任务 | ✅ 已完成 | |

### 4.3 仓库管理（/rom-mng, /rom-dirs, /rom-files）

| 子项 | 状态 | 说明 |
|------|------|------|
| 仓库列表 | ✅ 已完成 | |
| 目录列表 | ✅ 已完成 | |
| ROM 文件列表 + 媒体文件 Tab | ✅ 已完成 | |
| 文件状态徽章列 | ✅ 已完成 | 正常/缺失/损坏 |
| Favorite 操作列 | ✅ 已完成 | |
| ROM 编辑对话框 | ✅ 已完成 | |
| 校验 ROM 文件按钮 | ✅ 已完成 | 快速/完整两种模式 |
| ROM 搜索与筛选 | ⬜ 未开始 | |

### 4.4 ROM 归档（/rom-archive）

| 子项 | 状态 | 说明 |
|------|------|------|
| 归档对话框（按 label 选择） | ✅ 已完成 | |
| 提交归档任务 | ✅ 已完成 | |
| 任务历史展示 | ✅ 已完成 | |

### 4.5 ROM 导出（/rom-export）

| 子项 | 状态 | 说明 |
|------|------|------|
| 导出对话框 | ✅ 已完成 | |
| 提交导出任务 | ✅ 已完成 | |
| 任务历史展示 | ✅ 已完成 | |
| 导出模板/预设 | ⬜ 未开始 | |

### 4.6 ROM 数据库导入（/dat-import）

| 子项 | 状态 | 说明 |
|------|------|------|
| 已导入条目统计 | ✅ 已完成 | |
| 导入对话框（目录路径 + 平台过滤开关） | ✅ 已完成 | |
| 提交导入任务 | ✅ 已完成 | |
| 支持 ZIP 格式 DAT 文件 | ✅ 已完成 | |
| 默认平台过滤 | ✅ 已完成 | SEGA全系+NES/SNES/GBA/3DS/Wii+PS/PS2/PSP |

### 4.7 重复检测（/rom-duplicates）

| 子项 | 状态 | 说明 |
|------|------|------|
| 完全重复 Tab（同 MD5） | ✅ 已完成 | |
| 同游戏多版本 Tab（同 gameid） | ✅ 已完成 | |
| 点击查看分组详情 | ✅ 已完成 | 对话框展示组内所有条目 |
| 删除冗余记录 | ✅ 已完成 | 仅删记录，不删文件，需确认 |

---

## 五、模拟器前端格式支持

| 格式 | 导入（扫描） | 导出 | 说明 |
|------|:---:|:---:|------|
| EE (EmulationStation) gamelist.xml | ✅ | ✅ | 天马G以外的大部分掌机/盒子 |
| 天马G (txt 格式) | ⬜ | ⬜ | 待提供样本文件 |
| Pegasus (metadata.pegasus.txt) | ⬜ | ⬜ | 优先级低 |

---

## 六、已知问题 / TODO

- [x] ~~工具栏输入框/选框 placeholder 文字不显示~~ → 已修复
- [ ] 导出视图中源仓库选择目前是手动输入 label，后续改为下拉选择
- [ ] 任务详情查看页面
- [ ] 仓库目录列表按 label 过滤（当前 Grid 未传入 filter）
- [ ] 预检失败后 UI 层确认"继续"的交互（当前 code=300 需要前端处理）

---

## 七、数据模型

### 核心表

| 表名 | 实体 | 说明 |
|------|------|------|
| rom_repo | RomDir | 仓库目录（label + platform + rootpath + type） |
| rom_file | RomFile | ROM 文件记录（路径、MD5、CRC、平台、游戏名、区域、文件状态等） |
| media_file | MediaFile | 媒体文件（图片、视频、音频） |
| tasks | GlobalTask | 后台任务记录 |
| known_rom | KnownRom | No-Intro 标准 ROM 数据库（MD5/CRC/SHA1 + 游戏名 + 家族关系） |

### RomFile 核心字段

| 字段 | 说明 |
|------|------|
| name | 标准名（No-Intro 英文名，或文件名推断） |
| displayName | 显示名（中文，来自 gamelist.xml 或用户设置，优先展示） |
| gameid | 游戏家族 ID（No-Intro parentName，同一游戏不同版本共享） |
| fileStatus | 文件状态：OK / MISSING / CORRUPTED |
| md5, crc | 校验值（CRC 对 ZIP 文件零开销获取） |

### 核心枚举

| 枚举 | 说明 |
|------|------|
| Platform | 游戏平台（NES, SNES, N64, GB, GBA, MD, SS, DC, PS, PS2, PSP 等） |
| RepoType | INSTANCE（实例仓库/TF卡）/ ARCHIVE（归档仓库） |
| WrapType | ROM / DIRECTORY / ZIPPED_ROM / ZIPPED_DIRECTORY |
| MediaType | IMAGE / VIDEO / SOUND / OTHER |
| Region | JPN / EUR / USA / ASA / CHN / KOR / OTHER |
| FileStatus | OK / MISSING / CORRUPTED |
| TaskType | SCAN_DIR / EXPORT / ARCHIVE / VERIFY / IMPORT_DAT |

---

## 八、高优先级事项（下一步计划）

### P0 — 基础可用性

| 事项 | 状态 |
|------|------|
| ~~修复工具栏输入框/选框无文字问题~~ | ✅ 已修复 |
| 仓库目录列表按 label 过滤 | ⬜ |

### P1 — 核心流程验证

| 事项 | 状态 |
|------|------|
| 导入 No-Intro DAT → 扫描仓库 → 验证自动匹配效果 | ⬜ 待验证 |
| 归档流程端到端验证（扫描→归档→导出） | ⬜ 待验证 |
| 增量扫描逻辑实现 | ⬜ |

### P2 — 效率提升

| 事项 | 状态 |
|------|------|
| ROM 搜索与筛选（全局跨仓库） | ⬜ |
| 批量操作（修改字段、归档、删除） | ⬜ |
| 导出模板/预设 | ⬜ |
| 批量清理重复（保留一个删其余） | ⬜ |

### P3 — 体验优化

| 事项 | 状态 |
|------|------|
| 仪表盘首页 | ⬜ |
| ROM 封面缩略图预览 | ⬜ |
| 任务结果详情页 | ⬜ |
| 天马G格式支持 | ⬜ |
| 中文名映射表（No-Intro英文名→中文名） | ⬜ |
