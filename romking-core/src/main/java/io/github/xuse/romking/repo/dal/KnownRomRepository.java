package io.github.xuse.romking.repo.dal;

import java.util.List;
import java.util.Map;

import com.github.xuse.querydsl.lambda.LambdaColumn;
import com.github.xuse.querydsl.lambda.LambdaTable;
import com.github.xuse.querydsl.lambda.NumberLambdaColumn;
import com.github.xuse.querydsl.lambda.StringLambdaColumn;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.dsl.Wildcard;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.obj.KnownRom;
import io.github.xuse.romking.repo.obj.KnownRomFilter;
import io.github.xuse.romking.repo.obj.QKnownRom;
import io.github.xuse.simple.context.Service;

@Service
public class KnownRomRepository extends AbstractRepository<KnownRom, Integer, KnownRomFilter> {

	public static final LambdaTable<KnownRom> t = () -> KnownRom.class;

	public static final StringLambdaColumn<KnownRom> md5 = KnownRom::getMd5;
	public static final StringLambdaColumn<KnownRom> crc = KnownRom::getCrc;
	public static final StringLambdaColumn<KnownRom> sha1 = KnownRom::getSha1;
	public static final StringLambdaColumn<KnownRom> gameName = KnownRom::getGameName;
	public static final StringLambdaColumn<KnownRom> parentName = KnownRom::getParentName;
	public static final StringLambdaColumn<KnownRom> romFileName = KnownRom::getRomFileName;
	public static final StringLambdaColumn<KnownRom> datVersion = KnownRom::getDatVersion;

	public static final NumberLambdaColumn<KnownRom, Integer> id = KnownRom::getId;
	public static final NumberLambdaColumn<KnownRom, Long> romSize = KnownRom::getRomSize;

	public static final LambdaColumn<KnownRom, Platform> platform = KnownRom::getPlatform;

	public SQLQueryFactory getFactory() {
		return factory;
	}

	/**
	 * 按平台分组统计记录数
	 */
	public Map<Platform, Long> countByPlatform() {
		QKnownRom q = QKnownRom.knownRom;
		return factory.selectFrom(q)
			.groupBy(q.platform)
			.transform(GroupBy.groupBy(q.platform).as(Wildcard.count));
	}

	/**
	 * 根据parentName和platform查找同组的所有ROM
	 */
	public List<KnownRom> findByParentName(String parentName, Platform platform) {
		QKnownRom q = QKnownRom.knownRom;
		var query = factory.selectFrom(q).where(q.parentName.eq(parentName));
		if (platform != null) {
			query.where(q.platform.eq(platform));
		}
		return query.fetch();
	}
}
