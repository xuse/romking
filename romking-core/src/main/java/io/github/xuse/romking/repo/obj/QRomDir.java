package io.github.xuse.romking.repo.obj;

import com.querydsl.core.types.dsl.NumberPath;
import io.github.xuse.romking.core.Platform;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.core.types.dsl.EnumPath;
import java.util.Date;
import io.github.xuse.romking.repo.enums.RepoType;
import com.querydsl.core.types.dsl.StringPath;
import java.util.List;
import com.github.xuse.querydsl.sql.RelationalPathBaseEx;

public class QRomDir extends RelationalPathBaseEx<RomDir> {

    public final static QRomDir romDir = new QRomDir("rom");

    public final NumberPath<Integer> id = createNumber("id", int.class);

    public final StringPath label = createString("label");

    public final EnumPath<Platform> platform = createEnum("platform", Platform.class);

    public final StringPath rootpath = createString("rootpath");

    public final StringPath description = createString("description");

    public final EnumPath<RepoType> type = createEnum("type", RepoType.class);

    public final DateTimePath<Date> createTime = createDateTime("createTime", Date.class);

    public final DateTimePath<Date> lastScan = createDateTime("lastScan", Date.class);

    public final SimplePath<List<String>> tags = createSimple("tags", List.class);

    public QRomDir(String variable) {
        super(RomDir.class, variable);
        super.scanClassMetadata();
    }
}
