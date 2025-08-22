package io.github.xuse.romking.repo.obj;

import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.NumberPath;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.core.Platform;
import com.querydsl.core.types.dsl.DateTimePath;
import java.time.Instant;
import io.github.xuse.romking.core.GameType;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.EnumPath;
import java.sql.Date;
import io.github.xuse.romking.repo.enums.Region;
import com.querydsl.core.types.dsl.StringPath;
import java.util.Map;
import com.github.xuse.querydsl.sql.RelationalPathBaseEx;

public class QRomFile extends RelationalPathBaseEx<RomFile> {

    public final static QRomFile romFile = new QRomFile("rom");

    public final NumberPath<Integer> dirId = createNumber("dirId", int.class);

    public final NumberPath<Integer> id = createNumber("id", int.class);

    public final StringPath filepath = createString("filepath");

    public final EnumPath<WrapType> wrapType = createEnum("wrapType", WrapType.class);

    public final StringPath gameid = createString("gameid");

    public final StringPath name = createString("name");

    public final EnumPath<GameType> gameType = createEnum("gameType", GameType.class);

    public final EnumPath<Region> region = createEnum("region", Region.class);

    public final NumberPath<Integer> zippedFiles = createNumber("zippedFiles", int.class);

    public final StringPath version = createString("version");

    public final StringPath hackComment = createString("hackComment");

    public final EnumPath<Platform> platform = createEnum("platform", Platform.class);

    public final StringPath romName = createString("romName");

    public final StringPath romExt = createString("romExt");

    public final DatePath<Date> romModified = createDate("romModified", Date.class);

    public final NumberPath<Long> length = createNumber("length", long.class);

    public final StringPath crc = createString("crc");

    public final StringPath md5 = createString("md5");

    public final SimplePath<Map<String, Object>> medias = createSimple("medias", Map.class);

    public final NumberPath<Integer> favorite = createNumber("favorite", int.class);

    public final DateTimePath<Instant> createTime = createDateTime("createTime", Instant.class);

    public QRomFile(String variable) {
        super(RomFile.class, variable);
        super.scanClassMetadata();
    }
}
