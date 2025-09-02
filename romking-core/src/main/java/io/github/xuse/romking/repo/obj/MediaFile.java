package io.github.xuse.romking.repo.obj;

import java.sql.Types;

import com.github.xuse.querydsl.annotation.UnsavedValue;
import com.github.xuse.querydsl.annotation.dbdef.ColumnSpec;
import com.github.xuse.querydsl.annotation.dbdef.Key;
import com.github.xuse.querydsl.annotation.dbdef.TableSpec;
import com.github.xuse.querydsl.sql.ddl.ConstraintType;

import io.github.xuse.romking.repo.enums.MediaType;
import lombok.Data;

@TableSpec(name="media_file",primaryKeys = "id",keys= {
		@Key(type=ConstraintType.UNIQUE,path= {"dirId","filepath"})
})
@Data
public class MediaFile {
	/**
	 * 自增
	 */
	@ColumnSpec(nullable = false,type=Types.BIGINT,unsigned = true,autoIncrement = true)
	private long id;
	
	/**
	 * 所属仓库
	 */
	@ColumnSpec(nullable = false,type=Types.INTEGER,unsigned = true)
	@UnsavedValue(UnsavedValue.ZeroAndMinus)
	private int dirId; 
	
	
	/**
	 * 相对仓库根目录路径，如果ROM在ZIP中，此处是ZIP文件名。
	 * 如果是目录作为单个游戏ROM，那么这里是目录名
	 */
	@ColumnSpec(name="file_path",nullable = false,size = 256,type = Types.VARCHAR)
	private String filepath;
	
	/**
	 * 引用次数
	 */
	@ColumnSpec(name="ref_count",nullable = false,size = 256,type = Types.SMALLINT)
	@UnsavedValue(UnsavedValue.ZeroAndMinus)
	private int referCount;
	
	/**
	 * 扩展名
	 */
	@ColumnSpec(name="ext",nullable = false,size = 8,type = Types.VARCHAR)
	private String ext;
	
	/**
	 * 媒体类型
	 */
	@ColumnSpec(nullable = true,type=Types.TINYINT)
	private MediaType type;
	
	/**
	 * MD5
	 */
	@ColumnSpec(type=Types.CHAR,size=32,nullable = false, defaultValue = "''")
	private String md5;
}
