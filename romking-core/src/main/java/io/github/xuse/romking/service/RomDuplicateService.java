package io.github.xuse.romking.service;

import java.util.ArrayList;
import java.util.List;

import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.querydsl.core.Tuple;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.dal.RomFileRepository;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.repo.vo.DuplicateGroup;
import io.github.xuse.romking.repo.vo.DuplicateGroup.DuplicateType;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;

/**
 * ROM重复检测服务。
 * 
 * 支持两种重复检测：
 * 1. 完全重复（SAME_MD5）：同一仓库内MD5完全相同的文件，是冗余副本
 * 2. 同游戏多版本（SAME_GAME）：gameid相同但MD5不同，是同一游戏的不同版本
 */
@Service
public class RomDuplicateService {

	@Inject
	private RomFileRepository romFileRepo;

	private static final QRomFile t = QRomFile.romFile;

	/**
	 * 查找完全重复的ROM（同一仓库内MD5相同的条目）。
	 * 
	 * @param dirId 目录ID（0表示跨所有仓库检测）
	 * @return 重复分组列表
	 */
	public List<DuplicateGroup> findExactDuplicates(int dirId) {
		SQLQueryFactory factory = romFileRepo.getFactory();

		var query = factory.select(t.md5, t.name, t.platform, t.md5.count())
				.from(t)
				.where(t.md5.isNotNull(), t.md5.ne(""));

		if (dirId > 0) {
			query.where(t.dirId.eq(dirId));
		}

		List<Tuple> tuples = query
				.groupBy(t.md5)
				.having(t.md5.count().gt(1))
				.orderBy(t.md5.count().desc())
				.limit(200)
				.fetch();

		List<DuplicateGroup> groups = new ArrayList<>();
		for (Tuple tuple : tuples) {
			DuplicateGroup group = new DuplicateGroup();
			group.setGroupKey(tuple.get(t.md5));
			group.setGameName(tuple.get(t.name));
			group.setPlatform(tuple.get(t.platform));
			group.setCount(tuple.get(3, Long.class).intValue());
			group.setType(DuplicateType.SAME_MD5);
			groups.add(group);
		}
		return groups;
	}

	/**
	 * 查找同一游戏的多个版本（gameid相同但MD5不同的条目）。
	 * 
	 * @param dirId 目录ID（0表示跨所有仓库检测）
	 * @return 重复分组列表
	 */
	public List<DuplicateGroup> findGameVersions(int dirId) {
		SQLQueryFactory factory = romFileRepo.getFactory();

		var query = factory.select(t.gameid, t.name, t.platform, t.gameid.count())
				.from(t)
				.where(t.gameid.isNotNull(), t.gameid.ne(""));

		if (dirId > 0) {
			query.where(t.dirId.eq(dirId));
		}

		List<Tuple> tuples = query
				.groupBy(t.gameid)
				.having(t.gameid.count().gt(1))
				.orderBy(t.gameid.count().desc())
				.limit(200)
				.fetch();

		List<DuplicateGroup> groups = new ArrayList<>();
		for (Tuple tuple : tuples) {
			DuplicateGroup group = new DuplicateGroup();
			group.setGroupKey(tuple.get(t.gameid));
			group.setGameName(tuple.get(t.name));
			group.setPlatform(tuple.get(t.platform));
			group.setCount(tuple.get(3, Long.class).intValue());
			group.setType(DuplicateType.SAME_GAME);
			groups.add(group);
		}
		return groups;
	}

	/**
	 * 获取指定分组内的所有ROM条目
	 * 
	 * @param groupKey 分组标识
	 * @param type 重复类型
	 * @return ROM列表
	 */
	public List<RomFile> getGroupMembers(String groupKey, DuplicateType type) {
		if (groupKey == null || groupKey.isEmpty()) return List.of();

		if (type == DuplicateType.SAME_MD5) {
			return romFileRepo.find(q -> q.where(t.md5.eq(groupKey)));
		} else {
			return romFileRepo.find(q -> q.where(t.gameid.eq(groupKey)));
		}
	}

	/**
	 * 获取同一gameid下的所有ROM条目
	 */
	public List<RomFile> getVersionsByGameId(String gameid) {
		if (gameid == null || gameid.isEmpty()) return List.of();
		return romFileRepo.find(q ->
				q.where(t.gameid.eq(gameid)));
	}

	/**
	 * 删除指定ROM记录（仅删除数据库记录，不删物理文件）
	 */
	public void deleteRecord(int romFileId) {
		romFileRepo.getFactory().delete(t)
				.where(t.id.eq(romFileId))
				.execute();
	}

	/**
	 * 批量删除ROM记录（仅删除数据库记录，不删物理文件）
	 */
	public void deleteRecords(List<Integer> romFileIds) {
		if (romFileIds == null || romFileIds.isEmpty()) return;
		romFileRepo.getFactory().delete(t)
				.where(t.id.in(romFileIds))
				.execute();
	}
}
