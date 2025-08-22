package io.github.xuse.romking.repo.dal;

import com.github.xuse.querydsl.lambda.LambdaColumn;
import com.github.xuse.querydsl.lambda.LambdaTable;
import com.github.xuse.querydsl.lambda.NumberLambdaColumn;
import com.github.xuse.querydsl.lambda.StringLambdaColumn;
import com.github.xuse.querydsl.sql.SQLQueryFactory;

import io.github.xuse.romking.core.GameType;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.Region;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.obj.RomFileFilter;
import io.github.xuse.simple.context.Service;

@Service
public class RomFileRepository extends AbstractRepository<RomFile, Integer, RomFileFilter>{
	
	public static final LambdaTable<RomFile> t = ()-> RomFile.class;
	
	
	public static final StringLambdaColumn<RomFile> filepath = RomFile::getFilepath;
	public static final StringLambdaColumn<RomFile> name = RomFile::getName;
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
	
	
	
	public SQLQueryFactory getFactory() {
		return factory;
	}
}
