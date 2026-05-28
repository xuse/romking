# Implementation Plan: Known ROM Browser

## Overview

为 `known_rom` 表创建 Vaadin Flow 浏览视图，实现按平台、区域、游戏名的组合筛选和分页浏览。实现顺序：Filter 对象 → Repository 修改 → 实体注解 → 视图组件（统计面板 + 筛选工具栏 + Grid + parentName 交互）。

## Tasks

- [x] 1. Create KnownRomFilter and modify KnownRomRepository
  - [x] 1.1 Create `KnownRomFilter` class in `romking-core`
    - Create `io.github.xuse.romking.repo.obj.KnownRomFilter`
    - Annotate class with `@Data` and `@ConditionBean`
    - Add field `platform` (Platform) with `@Condition(Ops.EQ)` and `@FormField(caption = "平台", type = InputType.COMBO)`
    - Add field `region` (Region) with `@Condition(Ops.EQ)` and `@FormField(caption = "区域", type = InputType.COMBO)`
    - Add field `gameName` (String) with `@Condition(Ops.STRING_CONTAINS)` and `@FormField(caption = "游戏名", placeHolder = "输入游戏名关键字", type = InputType.TEXT)`
    - All fields nullable, null means no filtering
    - _Requirements: 6.3, 10.2, 10.3, 10.4, 10.5_

  - [x] 1.2 Modify `KnownRomRepository` generic parameter from `Void` to `KnownRomFilter`
    - Change class declaration to `extends AbstractRepository<KnownRom, Integer, KnownRomFilter>`
    - Add `countByPlatform()` method returning `Map<Platform, Long>` using QueryDSL `groupBy` + `transform`
    - Existing Lambda column definitions remain unchanged
    - _Requirements: 10.1, 2.2_

  - [ ] 1.3 Write property test for combined filter correctness
    - **Property 1: Combined filter correctness**
    - **Validates: Requirements 3.2, 4.2, 5.2, 6.1, 6.3, 10.2, 10.3, 10.4**
    - Generate random KnownRom records and random KnownRomFilter, insert into H2 in-memory database
    - Verify every returned record satisfies all active filter conditions
    - Verify no matching record is excluded

  - [ ] 1.4 Write property test for empty filter returns all records
    - **Property 2: Empty filter returns all records**
    - **Validates: Requirements 3.3, 4.3, 5.3, 10.5**
    - Generate random records, use filter with all fields null
    - Verify count equals total inserted records

  - [ ] 1.5 Write property test for platform count distribution consistency
    - **Property 3: Platform count distribution consistency**
    - **Validates: Requirements 2.1, 2.2**
    - Generate random records, verify sum of `countByPlatform()` values equals `count(Optional.empty())`

- [x] 2. Checkpoint - Ensure romking-core compiles and filter tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Add @ViewColumn annotations to KnownRom entity
  - [x] 3.1 Add `@ViewColumn` annotations to `KnownRom` fields
    - Add `@ViewColumn(caption = "平台", order = 1, sortable = true)` to `platform`
    - Add `@ViewColumn(caption = "游戏名", order = 2, sortable = true)` to `gameName`
    - Add `@ViewColumn(caption = "ROM文件名", order = 3)` to `romFileName`
    - Add `@ViewColumn(caption = "Parent", order = 4)` to `parentName`
    - Add `@ViewColumn(caption = "区域", order = 5)` to `region`
    - Add `@ViewColumn(caption = "文件大小", order = 6, converter = "fileSize")` to `romSize`
    - Add `@ViewColumn(caption = "CRC", order = 7)` to `crc`
    - Add `@ViewColumn(caption = "MD5", order = 8)` to `md5`
    - Add `@ViewColumn(caption = "SHA1", order = 9)` to `sha1`
    - Import `io.github.xuse.jetui.annotation.ViewColumn`
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 4. Create file size formatting utility
  - [x] 4.1 Create `formatFileSize` utility method
    - Add static method `formatFileSize(long bytes)` in an appropriate utility class (or within the view)
    - Return "B" for < 1024, "KB" for < 1MB, "MB" for < 1GB, "GB" otherwise
    - Ensure the `converter = "fileSize"` in `@ViewColumn` is wired to this formatter (implement in `FieldAccessor` if needed)
    - _Requirements: 8.2_

  - [ ] 4.2 Write property test for file size formatting correctness
    - **Property 4: File size formatting correctness**
    - **Validates: Requirements 8.2**
    - Generate random non-negative long values
    - Verify output contains correct unit suffix (B, KB, MB, GB)
    - Verify unit boundaries: KB for ≥ 1024, MB for ≥ 1048576, GB for ≥ 1073741824

- [x] 5. Create KnownRomBrowserView with statistics panel and filter toolbar
  - [x] 5.1 Create `KnownRomBrowserView` class in `romaster` module
    - Create `io.github.xuse.romaster.ui.manage.KnownRomBrowserView`
    - Annotate with `@Route("known-roms")`, `@PageTitle("标准ROM库")`, `@Menu(order = 4, icon = "vaadin:database", title = "标准ROM库")`, `@PermitAll`
    - Extend `Main`, add Flex column layout class names (matching `RomSearchView` pattern)
    - Inject `KnownRomRepository` via `RomConsole.getBean()`
    - Create `KnownRomFilter filter` instance field
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 5.2 Implement statistics panel in `KnownRomBrowserView`
    - Create `createStatisticsPanel()` method
    - Query total count via `knownRomRepo.count(Optional.empty())`
    - If total == 0, display hint text "暂无数据，请前往 DAT Import 页面导入标准ROM数据"
    - If total > 0, query `countByPlatform()` and display platform distribution
    - Use `HorizontalLayout` with `Span` components
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 5.3 Implement filter toolbar using `VaadinHelper.viewToolbarBuilder`
    - Call `VaadinHelper.viewToolbarBuilder(KnownRomFilter.class)` to auto-generate filter controls
    - Add "查询" button triggering `grid.getDataProvider().refreshAll()`
    - Add "重置" button clearing all filter fields and refreshing grid
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 6.1, 6.2_

  - [x] 5.4 Create data Grid with `VaadinViews.createGrid`
    - Call `VaadinViews.createGrid(KnownRom.class, knownRomRepo)` to create Grid with CallbackDataProvider
    - Set grid to `setSizeFull()`
    - Grid uses virtual scrolling (Vaadin default lazy loading)
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 6. Implement parentName click interaction
  - [x] 6.1 Configure parentName column with click handler
    - Replace default parentName column renderer with `ComponentRenderer`
    - Render parentName as clickable `Anchor` (link style)
    - On click: set `filter.setGameName(parentName)`, update filter UI text field, call `grid.getDataProvider().refreshAll()`
    - Handle null parentName by rendering empty `Span`
    - _Requirements: 9.1, 9.2_

- [x] 7. Final checkpoint - Full build verification
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The implementation language is Java (matching the existing codebase)
- `KnownRomFilter` follows the same `@ConditionBean` pattern as `RomFileFilter`
- `KnownRomBrowserView` follows the same layout pattern as `RomSearchView`
- The `VaadinHelper.viewToolbarBuilder` auto-generates form controls from `@FormField` annotations on the filter class
- `countByPlatform()` uses QueryDSL's `GroupBy.groupBy().as(Wildcard.count)` transform pattern

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "3.1"] },
    { "id": 2, "tasks": ["1.3", "1.4", "1.5", "4.1"] },
    { "id": 3, "tasks": ["4.2", "5.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.4"] },
    { "id": 5, "tasks": ["6.1"] }
  ]
}
```
