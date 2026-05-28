# Design Document: Known ROM Browser

## Overview

为 `known_rom` 表创建一个 Vaadin Flow 浏览视图（路由 `/known-roms`），允许用户浏览、筛选和检视从 No-Intro DAT 文件导入的 ROM 标准数据。该视图遵循项目现有的 UI 模式（继承 `Main`、使用 `VaadinViews.createGrid`、`VaadinHelper.viewToolbarBuilder`），并通过 `@ConditionBean` 注解的 `KnownRomFilter` 类实现参数化查询。

核心变更：
1. 新增 `KnownRomFilter` 类（`@ConditionBean`），封装 platform、region、gameName 筛选条件
2. 修改 `KnownRomRepository` 泛型参数从 `Void` 改为 `KnownRomFilter`
3. 在 `KnownRom` 实体上添加 `@ViewColumn` 注解定义 Grid 列
4. 新增 `KnownRomBrowserView` Vaadin 视图组件

## Architecture

```mermaid
graph TD
    A[KnownRomBrowserView] --> B[VaadinHelper.viewToolbarBuilder]
    A --> C[VaadinViews.createGrid]
    A --> D[Statistics Panel]
    
    B --> E[KnownRomFilter]
    C --> F[CallbackDataProvider]
    F --> G[KnownRomRepository]
    G --> H[AbstractRepository]
    H --> I[GenericRepository.countByCondition/findByCondition]
    
    E -->|@ConditionBean| I
    D --> G
```

**数据流：**
1. 用户在 Filter_Panel 中设置筛选条件 → 构建 `KnownRomFilter` 对象
2. 点击"查询"按钮 → `CallbackDataProvider` 携带 `KnownRomFilter` 调用 `KnownRomRepository.count()` / `list()`
3. `AbstractRepository` 检测到 filter 非空 → 委托给 `GenericRepository.countByCondition()` / `findByCondition()`
4. `querydsl-sql-extension` 根据 `@Condition` 注解自动生成 WHERE 子句
5. 结果返回给 Vaadin Grid 渲染

## Components and Interfaces

### 1. KnownRomFilter（新增）

```java
package io.github.xuse.romking.repo.obj;

@Data
@ConditionBean
public class KnownRomFilter {
    @FormField(caption = "平台", type = InputType.COMBO)
    @Condition(Ops.EQ)
    private Platform platform;

    @FormField(caption = "区域", type = InputType.COMBO)
    @Condition(Ops.EQ)
    private Region region;

    @FormField(caption = "游戏名", placeHolder = "输入游戏名关键字", type = InputType.TEXT)
    @Condition(Ops.STRING_CONTAINS_IC)
    private String gameName;
}
```

**设计决策：**
- 使用 `Ops.STRING_CONTAINS_IC` 实现游戏名的不区分大小写包含匹配（如果 querydsl-sql-extension 不支持 `STRING_CONTAINS_IC`，则使用 `Ops.STRING_CONTAINS` 并依赖数据库 collation 实现大小写不敏感）
- `@FormField` 注解使 `VaadinHelper.viewToolbarBuilder(KnownRomFilter.class)` 能自动生成对应的输入控件
- 所有字段均为 nullable，null 表示不参与筛选

### 2. KnownRomRepository（修改）

```java
@Service
public class KnownRomRepository extends AbstractRepository<KnownRom, Integer, KnownRomFilter> {
    // ... 现有 Lambda 列定义不变 ...

    /**
     * 按平台分组统计记录数
     */
    public Map<Platform, Long> countByPlatform() {
        QKnownRom q = QKnownRom.knownRom;
        return factory.selectFrom(q)
            .groupBy(q.platform)
            .transform(GroupBy.groupBy(q.platform).as(Wildcard.count));
    }
}
```

**变更说明：**
- 泛型参数 `Void` → `KnownRomFilter`
- 继承的 `count(Optional<F>)` 和 `list(Optional<F>, int, int)` 方法自动适配新的 filter 类型
- 新增 `countByPlatform()` 方法用于统计面板

### 3. KnownRom 实体（修改 - 添加 @ViewColumn）

在 `KnownRom` 类的字段上添加 `@ViewColumn` 注解：

```java
@ViewColumn(caption = "平台", order = 1, sortable = true)
private Platform platform;

@ViewColumn(caption = "游戏名", order = 2, sortable = true)
private String gameName;

@ViewColumn(caption = "ROM文件名", order = 3)
private String romFileName;

@ViewColumn(caption = "Parent", order = 4)
private String parentName;

@ViewColumn(caption = "区域", order = 5)
private Region region;

@ViewColumn(caption = "文件大小", order = 6, converter = "fileSize")
private long romSize;

@ViewColumn(caption = "CRC", order = 7)
private String crc;

@ViewColumn(caption = "MD5", order = 8)
private String md5;

@ViewColumn(caption = "SHA1", order = 9)
private String sha1;
```

**设计决策：**
- `romSize` 使用 converter 实现人类可读格式（KB/MB）。如果现有 converter 机制不支持 `fileSize`，则在 `FieldAccessor` 中通过自定义 `ValueProvider` 实现格式化
- `gameName` 和 `platform` 设置 `sortable = true`
- `parentName` 列需要支持点击交互（通过 ComponentRenderer 实现超链接样式）

### 4. KnownRomBrowserView（新增）

```java
package io.github.xuse.romaster.ui.manage;

@Route("known-roms")
@PageTitle("标准ROM库")
@Menu(order = 4, icon = "vaadin:database", title = "标准ROM库")
@PermitAll
public class KnownRomBrowserView extends Main {
    private final KnownRomRepository knownRomRepo;
    private final KnownRomFilter filter = new KnownRomFilter();
    private Grid<KnownRom> grid;

    public KnownRomBrowserView(RomConsole console) {
        this.knownRomRepo = console.getBean(KnownRomRepository.class);
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
            LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(createStatisticsPanel());
        add(createFilterToolbar());
        grid = VaadinViews.createGrid(KnownRom.class, knownRomRepo);
        grid.setSizeFull();
        configureParentNameColumn();
        add(grid);
    }
}
```

**组件结构：**
```
KnownRomBrowserView (Main, Flex Column)
├── StatisticsPanel (HorizontalLayout)
│   ├── Span: "总记录数: {total}"
│   └── Span: "NES: {n} | SNES: {n} | ..."  (或空数据提示)
├── ViewToolbar (Filter Panel)
│   ├── ComboBox<Platform>
│   ├── ComboBox<Region>
│   ├── TextField (gameName)
│   ├── Button "查询"
│   └── Button "重置"
└── Grid<KnownRom> (CallbackDataProvider, 虚拟滚动)
    ├── Column: platform
    ├── Column: gameName
    ├── Column: romFileName
    ├── Column: parentName (可点击)
    ├── Column: region
    ├── Column: romSize (格式化)
    ├── Column: crc
    ├── Column: md5
    └── Column: sha1
```

### 5. 统计查询方法

统计面板在视图构造时一次性查询：

```java
private Component createStatisticsPanel() {
    long total = knownRomRepo.count(Optional.empty());
    if (total == 0) {
        Span hint = new Span("暂无数据，请前往 DAT Import 页面导入标准ROM数据");
        // 可添加 RouterLink 到 DatImportView
        return hint;
    }
    Map<Platform, Long> platformCounts = knownRomRepo.countByPlatform();
    // 构建统计展示组件
    HorizontalLayout layout = new HorizontalLayout();
    layout.add(new Span("总记录数: " + total));
    StringBuilder sb = new StringBuilder();
    platformCounts.forEach((p, c) -> sb.append(p.name()).append(": ").append(c).append("  "));
    layout.add(new Span(sb.toString()));
    return layout;
}
```

### 6. ParentName 点击交互

通过替换 `parentName` 列的默认渲染器实现点击筛选：

```java
private void configureParentNameColumn() {
    grid.getColumnByKey("parentName").setRenderer(new ComponentRenderer<>(rom -> {
        if (rom.getParentName() == null) return new Span("");
        Anchor link = new Anchor("#", rom.getParentName());
        link.getElement().addEventListener("click", e -> {
            filter.setGameName(rom.getParentName());
            // 同步更新工具栏中的 gameName 输入框
            updateFilterUI();
            grid.getDataProvider().refreshAll();
        }).addEventData("event.preventDefault()");
        return link;
    }));
}
```

## Data Models

### KnownRomFilter

| 字段 | 类型 | 匹配方式 | 说明 |
|------|------|----------|------|
| platform | Platform (enum) | EQ (等值) | 为 null 时不参与筛选 |
| region | Region (enum) | EQ (等值) | 为 null 时不参与筛选 |
| gameName | String | STRING_CONTAINS (包含, 不区分大小写) | 为 null/空 时不参与筛选 |

### KnownRom 列展示映射

| 实体字段 | Grid 列标题 | 排序 | 特殊处理 |
|----------|------------|------|----------|
| platform | 平台 | ✓ | 枚举 name() 显示 |
| gameName | 游戏名 | ✓ | - |
| romFileName | ROM文件名 | ✗ | - |
| parentName | Parent | ✗ | 可点击，触发家族筛选 |
| region | 区域 | ✗ | 枚举 name() 显示 |
| romSize | 文件大小 | ✗ | 格式化为 KB/MB |
| crc | CRC | ✗ | - |
| md5 | MD5 | ✗ | - |
| sha1 | SHA1 | ✗ | - |

### 文件大小格式化逻辑

```java
public static String formatFileSize(long bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
    if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
    return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Combined filter correctness

*For any* set of KnownRom records and *for any* combination of filter values (platform, region, gameName — each independently null or non-null), every record returned by `KnownRomRepository.list(filter)` SHALL satisfy ALL active filter conditions simultaneously: platform equals the filter platform (if set), region equals the filter region (if set), and gameName contains the filter gameName substring case-insensitively (if set). Furthermore, no record satisfying all conditions shall be excluded from the results.

**Validates: Requirements 3.2, 4.2, 5.2, 6.1, 6.3, 10.2, 10.3, 10.4**

### Property 2: Empty filter returns all records

*For any* set of KnownRom records in the database, when `KnownRomFilter` has all fields set to null, `KnownRomRepository.count(Optional.of(emptyFilter))` SHALL equal the total number of records, and `KnownRomRepository.list(Optional.of(emptyFilter), 0, Integer.MAX_VALUE)` SHALL return all records.

**Validates: Requirements 3.3, 4.3, 5.3, 10.5**

### Property 3: Platform count distribution consistency

*For any* set of KnownRom records, the sum of all per-platform counts returned by `countByPlatform()` SHALL equal the total record count returned by `count(Optional.empty())`.

**Validates: Requirements 2.1, 2.2**

### Property 4: File size formatting correctness

*For any* non-negative long value representing bytes, the `formatFileSize` function SHALL produce a string that: (a) contains the correct unit suffix (B, KB, MB, or GB), (b) when parsed back to bytes (within rounding tolerance), approximates the original value, and (c) uses KB for values ≥ 1024, MB for values ≥ 1048576, GB for values ≥ 1073741824.

**Validates: Requirements 8.2**

## Error Handling

| 场景 | 处理方式 |
|------|----------|
| 数据库连接失败 | Vaadin 全局错误处理，显示错误通知 |
| 空数据库 | Statistics_Panel 显示 0 并提示导入 |
| 筛选无结果 | Grid 显示空状态（Vaadin 默认行为） |
| 非法筛选输入 | `@ConditionBean` 自动忽略 null 字段，无需额外处理 |
| parentName 为 null | 点击列渲染为空 Span，不触发交互 |

## Testing Strategy

### Property-Based Tests (jqwik)

项目已使用 jqwik（见 `.jqwik-database` 文件），property-based tests 使用 jqwik 框架。

**配置：**
- 最少 100 次迭代
- 每个 property test 标注对应的设计属性

```java
// Tag format example:
// Feature: known-rom-browser, Property 1: Combined filter correctness
@Property(tries = 100)
void combinedFilterReturnsOnlyMatchingRecords(@ForAll KnownRomFilter filter, @ForAll List<KnownRom> records) { ... }
```

**Property Tests:**
1. **Property 1**: 生成随机 KnownRom 记录集和随机 KnownRomFilter，插入 H2 内存数据库，验证 repository 返回的记录全部满足所有活跃筛选条件
2. **Property 2**: 生成随机记录集，使用空 filter 查询，验证返回数量等于插入数量
3. **Property 3**: 生成随机记录集，验证 `countByPlatform()` 各值之和等于 `count(empty)`
4. **Property 4**: 生成随机 long 值（0 ~ Long.MAX_VALUE），验证 `formatFileSize` 输出格式正确

### Unit Tests (JUnit 5)

- 验证 `KnownRomBrowserView` 路由注解正确（`@Route("known-roms")`、`@Menu(order=4)`、`@PermitAll`）
- 验证 `KnownRomFilter` 的 `@Condition` 注解配置正确
- 验证 `KnownRom` 的 `@ViewColumn` 注解覆盖所有必需列
- 验证空数据库时统计面板显示提示信息
- 验证 parentName 点击交互更新 filter 状态

### Integration Tests

- 端到端验证：导入 DAT 文件 → 浏览视图显示正确数据
- 验证 Vaadin Grid 的 CallbackDataProvider 正确调用 repository 方法
