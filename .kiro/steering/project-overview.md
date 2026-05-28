# Romking 项目概览

## 项目简介
Romking 是一个游戏 ROM 文件扫描与管理应用程序，用于管理复古游戏 ROM 集合。支持扫描 TF 卡或游戏集成包目录，解析元数据，归档管理，以及导出制作游戏集成包。

## 项目架构

### 多模块 Maven 工程
```
romking/                    (parent pom, Java 17)
├── romking-core/           核心领域逻辑，ROM 扫描/管理服务
├── simple-context/         轻量级 IoC 容器（自研，桥接到 Spring）
└── romaster/               Vaadin UI Web 应用（Spring Boot）
```

### 技术栈
| 组件 | 技术 | 版本 |
|------|------|------|
| UI 框架 | Vaadin (Flow) | 24.8.5 |
| 后端框架 | Spring Boot | 3.5.4 |
| 安全 | Spring Security + Vaadin Control Center | 6.5.2 |
| 数据库 | H2 (开发) / PostgreSQL (生产) | 2.3.232 |
| ORM/查询 | QueryDSL (querydsl-sql-extension) | - |
| IoC 桥接 | simple-context (自研) | 1.0.0 |
| 构建 | Maven | - |
| 其他 | Lombok, Jackson 2.19.2, JNA | - |

### 核心设计模式
- **RomConsole**: 核心入口，初始化 H2 数据库和 simple-context IoC 容器，在 Spring Boot 启动时将内部 Bean 注册到 Spring 上下文
- **simple-context**: 自研轻量 IoC，使用 `@Service`、`@Inject` 注解，支持包扫描和 `InitializingBean`
- **QueryDSL**: 使用 `querydsl-sql-extension` 进行数据库操作，实体类通过注解定义表结构（自动建表）
- **jetui**: 自研 Vaadin UI 辅助框架，提供 AutoForm、Grid 自动生成、ViewToolbar 等

## 数据模型

### 核心实体
- **RomDir** (`rom_repo` 表): ROM 仓库目录，包含 label、platform、rootpath、type(INSTANCE/ARCHIVE)
- **RomFile** (`rom_file` 表): ROM 文件记录，包含路径、MD5、CRC、平台、游戏名、区域等
- **MediaFile** (`media_file` 表): 媒体文件（图片、视频、音频）
- **GlobalTask** (`tasks` 表): 后台任务记录

### 枚举
- **Platform**: 游戏平台（NES, SNES, N64, GB, GBA, MD, PS, PSP 等）
- **RepoType**: 仓库类型（INSTANCE 实例仓库 / ARCHIVE 归档仓库）
- **WrapType**: 文件包装形式（ROM / ZIPPED_ROM / ZIPPED_DIRECTORY）
- **MediaType**: 媒体类型（IMAGE / VIDEO / SOUND / OTHER）
- **Region**: 发行区域

## 功能模块（design.md）

### 一、信息收集与建档
1. 扫描 TF 卡/ROM 集成包目录 → 创建 INSTANCE 仓库
2. 扫描 ROM 仓库目录 → 创建 ARCHIVE 仓库
3. 元数据补全（跨仓库 MD5 匹配）

### 二、ROM 维护
1. 重复 ROM 查找与合并
2. ROM 格式转换
3. ROM 归档（INSTANCE → ARCHIVE）
4. 界面维护元数据

### 三、仓库导出
1. 全量导出（ARCHIVE → 新 INSTANCE）
2. 增量导出

### UI 视图
1. 扫描任务管理
2. 仓库管理（ROM 文件列表 + 媒体文件列表）
3. ROM 归档操作
4. ROM 导出操作

## 元数据解析
- **EE (EmulationStation)**: 解析 `gamelist.xml` 格式
- **Pegasus**: 解析 Pegasus 前端格式（待实现）
- 通用 `MetadataParser` 接口

## 构建与运行
```bash
# 开发模式
cd romaster && ./mvnw

# 生产构建
cd romaster && ./mvnw -Pproduction package

# Docker
docker build -t romaster:latest .
```

## 性能与设计约束

### Hash 计算策略（重要）
仓库规模约 70TB，MD5 计算开销极大，必须谨慎使用：

- **ZIP 文件的 CRC**：从 ZIP Central Directory 直接读取，零解压开销，优先使用
- **MD5**：需要完整读取文件内容（ZIP 还需解压），仅在必要时计算
- **文件元数据**：大小(size) + 修改日期(lastModified) 作为快速变更检测依据

**扫描/校验时的跳过规则**：
- 如果文件的 大小 + 修改日期 与数据库记录一致 → 跳过 MD5 计算，视为未变更
- 如果是 ZIP 文件且 CRC 与记录一致 → 跳过 MD5 计算
- 仅当上述快速检测发现不一致时，才计算 MD5 确认

**MD5 计算时机**：
- 首次扫描入库时（可选，由 computeMd5 选项控制）
- 归档时（确保归档记录的准确性）
- 用户手动触发"完整校验"时
- 与 known_rom 表匹配识别版本时（如果 CRC+size 已能唯一匹配则不需要）

### CRC 的定位
- ZIP 文件：CRC 存储在 ZIP 文件头中，读取开销极低（仅解析 Central Directory）
- 非 ZIP 文件：CRC32 计算需要读取全文件，但比 MD5 快（无加密运算）
- 用途：快速去重预筛、快速校验、与 No-Intro DAT 匹配

### RomFile 记录中的修改日期字段
`romModified` 字段记录文件的 lastModified 时间戳，用于：
- 增量扫描时判断文件是否变更
- 配合 size 做快速变更检测，避免不必要的 hash 计算

