# Requirements Document

## Introduction

为 DAT 导入的标准库（known_rom 表）创建一个浏览界面。用户通过 DatImportView 导入 No-Intro DAT 文件后，需要一个独立页面来浏览、查询和检视已导入的 ROM 标准数据记录。该页面提供按平台、游戏名、区域等维度的筛选能力，支持分页浏览大量数据，并展示每条记录的完整信息（包括校验值、文件大小、Parent/Clone 关系等）。同时提供统计概览（总记录数、各平台记录数）。

## Glossary

- **Known_Rom_Browser**: 已知 ROM 标准数据浏览视图，是一个 Vaadin Flow 页面组件（路由 `/known-roms`）
- **KnownRom**: known_rom 表对应的实体，存储从 No-Intro DAT 文件导入的 ROM 标准数据
- **KnownRomFilter**: 筛选条件对象，封装平台、区域、游戏名等查询参数，供 KnownRomRepository 使用
- **Platform**: 游戏平台枚举（NES、SNES、N64、NGC、Wii、Switch、VirtualBoy、GB、GBC、GBA、NDS、n3DS、MD、SS、DC、PS、PS2、PSP、PSV、PORTS、DOS）
- **Region**: 发行区域枚举（JPN、EUR、USA、ASA、CHN、KOR、OTHER）
- **Parent_Name**: 同一游戏家族共享的标识名，用于表示 Parent/Clone 关系
- **Filter_Panel**: 筛选面板，包含平台选择、区域选择、游戏名搜索等筛选控件
- **Data_Grid**: 数据表格组件，以虚拟滚动分页方式展示 KnownRom 记录列表
- **Statistics_Panel**: 统计信息面板，展示总记录数和各平台记录数

## Requirements

### Requirement 1: 页面路由与导航入口

**User Story:** As a 用户, I want 通过侧边栏菜单访问 ROM 标准数据浏览页面, so that 我可以方便地查看已导入的 ROM 数据库内容。

#### Acceptance Criteria

1. THE Known_Rom_Browser SHALL 注册路由路径 "/known-roms"，菜单顺序为 4（位于 ROM Search 和 DAT Import 之间），菜单标题为"标准ROM库"
2. THE Known_Rom_Browser SHALL 使用 @PermitAll 注解允许所有已认证用户访问
3. THE Known_Rom_Browser SHALL 继承 Main 组件并遵循现有视图的布局模式（Flex 列布局、内边距、间距）

### Requirement 2: 统计信息展示

**User Story:** As a 用户, I want 在页面顶部看到 ROM 标准数据库的统计概览, so that 我可以了解数据库的整体规模和各平台数据分布。

#### Acceptance Criteria

1. THE Statistics_Panel SHALL 在页面顶部显示已导入 KnownRom 的总记录数
2. THE Statistics_Panel SHALL 显示各 Platform 的记录数分布信息
3. WHEN 数据库中无任何 KnownRom 记录, THE Statistics_Panel SHALL 显示总数为 0 并提示用户前往 DAT Import 页面导入数据

### Requirement 3: 平台筛选

**User Story:** As a 用户, I want 按游戏平台筛选 ROM 记录, so that 我可以只查看特定平台的 ROM 标准数据。

#### Acceptance Criteria

1. THE Filter_Panel SHALL 提供一个平台下拉选择控件，列出所有 Platform 枚举值
2. WHEN 用户选择一个平台, THE Data_Grid SHALL 仅显示该平台对应的 KnownRom 记录
3. WHEN 用户清除平台选择, THE Data_Grid SHALL 显示所有平台的 KnownRom 记录

### Requirement 4: 区域筛选

**User Story:** As a 用户, I want 按发行区域筛选 ROM 记录, so that 我可以查看特定区域版本的游戏。

#### Acceptance Criteria

1. THE Filter_Panel SHALL 提供一个区域下拉选择控件，列出所有 Region 枚举值
2. WHEN 用户选择一个区域, THE Data_Grid SHALL 仅显示该区域对应的 KnownRom 记录
3. WHEN 用户清除区域选择, THE Data_Grid SHALL 显示所有区域的 KnownRom 记录

### Requirement 5: 游戏名搜索

**User Story:** As a 用户, I want 通过关键字搜索游戏名, so that 我可以快速定位特定游戏的 ROM 标准数据。

#### Acceptance Criteria

1. THE Filter_Panel SHALL 提供一个文本输入框用于输入游戏名搜索关键字
2. WHEN 用户输入搜索关键字并触发查询, THE Data_Grid SHALL 仅显示 gameName 包含该关键字的 KnownRom 记录（不区分大小写）
3. WHEN 用户清空搜索关键字, THE Data_Grid SHALL 恢复显示所有符合其他筛选条件的记录

### Requirement 6: 组合筛选

**User Story:** As a 用户, I want 同时使用多个筛选条件, so that 我可以精确定位目标 ROM 记录。

#### Acceptance Criteria

1. WHEN 用户同时设置平台、区域和游戏名筛选条件, THE Data_Grid SHALL 仅显示同时满足所有条件的 KnownRom 记录
2. THE Filter_Panel SHALL 提供一个"查询"按钮触发筛选，以及一个"重置"按钮清除所有筛选条件恢复默认状态
3. THE KnownRomFilter SHALL 封装 platform（等值匹配）、region（等值匹配）、gameName（包含匹配）三个筛选字段

### Requirement 7: 分页数据展示

**User Story:** As a 用户, I want 以分页方式浏览 ROM 标准数据, so that 页面在数据量大时仍能快速响应。

#### Acceptance Criteria

1. THE Data_Grid SHALL 使用 CallbackDataProvider 实现后端分页（lazy loading），通过 KnownRomRepository 的 ListDataProvider 接口获取数据
2. THE Data_Grid SHALL 支持通过 Vaadin Grid 虚拟滚动自动加载下一页数据
3. WHEN 筛选条件变更, THE Data_Grid SHALL 重置滚动位置并从第一条记录开始显示

### Requirement 8: 数据列展示

**User Story:** As a 用户, I want 在表格中查看 ROM 记录的关键信息, so that 我可以了解每条 ROM 的详细属性。

#### Acceptance Criteria

1. THE Data_Grid SHALL 显示以下列：平台(platform)、游戏名(gameName)、ROM 文件名(romFileName)、区域(region)、文件大小(romSize)、CRC、MD5、SHA1
2. THE Data_Grid SHALL 将 romSize 以人类可读格式显示（如 KB、MB）
3. THE Data_Grid SHALL 支持按 gameName 列和 platform 列排序
4. THE Data_Grid SHALL 显示 parentName 列，用于展示游戏家族归属

### Requirement 9: Parent/Clone 家族快速筛选

**User Story:** As a 用户, I want 快速查看同一游戏家族的所有版本, so that 我可以了解某个游戏有哪些区域版本和变体。

#### Acceptance Criteria

1. WHEN 用户点击某条记录的 parentName 值, THE Known_Rom_Browser SHALL 将游戏名搜索条件设置为该 parentName 值，以展示同一家族的所有 ROM 记录
2. WHEN 通过 parentName 筛选后, THE Filter_Panel SHALL 反映当前的筛选状态，用户可通过重置按钮恢复

### Requirement 10: Repository 筛选支持

**User Story:** As a 开发者, I want KnownRomRepository 支持 KnownRomFilter 参数化查询, so that 视图层可以通过 Filter 对象驱动数据加载。

#### Acceptance Criteria

1. THE KnownRomRepository SHALL 将泛型参数 F 从 Void 变更为 KnownRomFilter
2. WHEN KnownRomFilter 的 platform 字段非空, THE KnownRomRepository SHALL 在查询中添加 platform 等值条件
3. WHEN KnownRomFilter 的 region 字段非空, THE KnownRomRepository SHALL 在查询中添加 region 等值条件
4. WHEN KnownRomFilter 的 gameName 字段非空, THE KnownRomRepository SHALL 在查询中添加 gameName 包含匹配条件（不区分大小写）
5. WHEN KnownRomFilter 所有字段均为空, THE KnownRomRepository SHALL 返回全部记录（无额外过滤条件）
