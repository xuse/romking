package io.github.xuse.romking.repo.dal;

import java.util.List;
import java.util.stream.Stream;

import com.github.xuse.querydsl.lambda.LambdaColumn;
import com.github.xuse.querydsl.lambda.LambdaTable;
import com.github.xuse.querydsl.lambda.NumberLambdaColumn;
import com.github.xuse.querydsl.lambda.StringLambdaColumn;
import com.github.xuse.querydsl.sql.SQLQueryAlter;
import com.github.xuse.querydsl.sql.SQLQueryFactory;

import io.github.xuse.romking.core.GameType;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.FileStatus;
import io.github.xuse.romking.repo.enums.Region;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.QRomDir;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;
import io.github.xuse.simple.context.Service;

@Service
public class RomFileRepository extends AbstractRepository<RomFile, Integer, RomFileFilter>{
	
	public static final LambdaTable<RomFile> t = ()-> RomFile.class;
	
	
	public static final StringLambdaColumn<RomFile> filepath = RomFile::getFilepath;
	public static final StringLambdaColumn<RomFile> name = RomFile::getName;
	public static final StringLambdaColumn<RomFile> displayName = RomFile::getDisplayName;
	public static final StringLambdaColumn<RomFile> romName = RomFile::getRomName;
	public static final StringLambdaColumn<RomFile> romExt = RomFile::getRomExt;
	public static final StringLambdaColumn<RomFile> md5 = RomFile::getMd5;
	public static final StringLambdaColumn<RomFile> crc = RomFile::getCrc;

	public static final NumberLambdaColumn<RomFile,Integer> id = RomFile::getId;
	public static final NumberLambdaColumn<RomFile,Integer> dirId = RomFile::getDirId;
	public static final NumberLambdaColumn<RomFile,Integer> favorite = RomFile::getFavorite;
	
	public static final NumberLambdaColumn<RomFile,Long> length = RomFile::getLength;
	
	public static final LambdaColumn<RomFile,WrapType> wrapType = RomFile::getWrapType;
	public static final LambdaColumn<RomFile, Platform> platform = RomFile::getPlatform;
	public static final LambdaColumn<RomFile, GameType> gameType = RomFile::getGameType;
	public static final LambdaColumn<RomFile, Region> region = RomFile::getRegion;
	public static final LambdaColumn<RomFile, FileStatus> fileStatus = RomFile::getFileStatus;
	
	
	
	public SQLQueryFactory getFactory() {
		return factory;
	}

	/**
	 * 按仓库label和平台查询ROM文件（分页）
	 */
	public Stream<RomFile> listByRepo(String label, Platform platform, String name, int offset, int limit) {
		QRomFile rf = QRomFile.romFile;
		QRomDir rd = QRomDir.romDir;
		SQLQueryAlter<RomFile> query = factory.selectFrom(rf)
				.where(rf.dirId.in(
						factory.select(rd.id).from(rd).where(rd.label.eq(label))
				));
		if (platform != null) {
			query.where(rf.platform.eq(platform));
		}
		if (name != null && !name.isBlank()) {
			query.where(rf.name.containsIgnoreCase(name));
		}
		return query.offset(offset).limit(limit).fetch().stream();
	}

	/**
	 * 按仓库label和平台统计ROM文件数
	 */
	public int countByRepo(String label, Platform platform, String name) {
		QRomFile rf = QRomFile.romFile;
		QRomDir rd = QRomDir.romDir;
		SQLQueryAlter<RomFile> query = factory.selectFrom(rf)
				.where(rf.dirId.in(
						factory.select(rd.id).from(rd).where(rd.label.eq(label))
				));
		if (platform != null) {
			query.where(rf.platform.eq(platform));
		}
		if (name != null && !name.isBlank()) {
			query.where(rf.name.containsIgnoreCase(name));
		}
		return (int) query.fetchCount();
	}

	/**
	 * 获取仓库下所有平台列表
	 */
	public List<Platform> findPlatformsByRepo(String label) {
		QRomFile rf = QRomFile.romFile;
		QRomDir rd = QRomDir.romDir;
		return factory.select(rf.platform).distinct().from(rf)
				.where(rf.dirId.in(
						factory.select(rd.id).from(rd).where(rd.label.eq(label))
				))
				.fetch();
	}
}
