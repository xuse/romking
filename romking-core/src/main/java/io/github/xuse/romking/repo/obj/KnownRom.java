package io.github.xuse.romking.repo.obj;

import java.sql.Types;

import com.github.xuse.querydsl.annotation.CustomType;
import com.github.xuse.querydsl.annotation.dbdef.ColumnSpec;
import com.github.xuse.querydsl.annotation.dbdef.Key;
import com.github.xuse.querydsl.annotation.dbdef.TableSpec;
import com.github.xuse.querydsl.sql.ddl.ConstraintType;
import com.github.xuse.querydsl.types.EnumByCodeType;

import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.Region;
import lombok.Data;

/**
 * 已知ROM数据库（从No-Intro DAT文件导入）。
 * 用于识别ROM版本、去重、校验。
 */
@Data
@TableSpec(name = "known_rom", primaryKeys = "id", keys = {
		@Key(type = ConstraintType.UNIQUE, path = {"md5"}),
		@Key(type = ConstraintType.KEY, path = {"crc", "romSize"}),
		@Key(type = ConstraintType.KEY, path = {"platform", "parentName"}),
		@Key(type = ConstraintType.KEY, path = {"sha1"}),
})
public class KnownRom {

	@ColumnSpec(nullable = false, autoIncrement = true, type = Types.INTEGER)
	private int id;

	/**
	 * MD5校验值
	 */
	@ColumnSpec(nullable = true, type = Types.CHAR, size = 32)
	@ViewColumn(caption = "MD5", order = 6)
	private String md5;

	/**
	 * CRC32校验值
	 */
	@ColumnSpec(nullable = true, type = Types.CHAR, size = 8)
	@ViewColumn(caption = "CRC", order = 7)
	private String crc;

	/**
	 * SHA1校验值
	 */
	@ColumnSpec(nullable = true, type = Types.CHAR, size = 40)
	@ViewColumn(caption = "SHA1", order = 8)
	private String sha1;

	/**
	 * ROM文件大小（字节）
	 */
	@ColumnSpec(nullable = false, type = Types.BIGINT, defaultValue = "0")
	@ViewColumn(caption = "文件大小", order = 4, converter = "fileSize")
	private long romSize;

	/**
	 * No-Intro标准游戏名（含区域标签），如 "Super Mario Bros. (Japan)"
	 */
	@ColumnSpec(nullable = false, type = Types.VARCHAR, size = 256)
	@ViewColumn(caption = "游戏名", order = 1, sortable = true)
	private String gameName;

	/**
	 * ROM文件名（No-Intro标准命名），如 "Super Mario Bros. (Japan).nes"
	 */
	@ColumnSpec(nullable = false, type = Types.VARCHAR, size = 256)
	@ViewColumn(caption = "ROM文件名", order = 5)
	private String romFileName;

	/**
	 * Parent游戏名（来自P/C DAT），同一游戏家族共享同一个parentName。
	 * 如果没有P/C信息，则等于gameName。
	 */
	@ColumnSpec(nullable = true, type = Types.VARCHAR, size = 256)
	private String parentName;

	/**
	 * 区域（从游戏名中解析）
	 */
	@ColumnSpec(nullable = true, type = Types.TINYINT, unsigned = true)
	@CustomType(EnumByCodeType.class)
	@ViewColumn(caption = "区域", order = 3)
	private Region region;

	/**
	 * 平台
	 */
	@ColumnSpec(nullable = false, type = Types.VARCHAR, size = 14)
	@ViewColumn(caption = "平台", order = 2, sortable = true)
	private Platform platform;

	/**
	 * DAT来源标识，如 "No-Intro"、"TOSEC"、"Redump"
	 */
	@ColumnSpec(nullable = false, type = Types.VARCHAR, size = 32, defaultValue = "'No-Intro'")
	private String source;

	/**
	 * DAT版本（日期字符串），如 "20250520-091234"
	 */
	@ColumnSpec(nullable = true, type = Types.VARCHAR, size = 32)
	private String datVersion;
}
