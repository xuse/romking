package io.github.xuse.romking.repo.obj;

import com.querydsl.core.types.dsl.NumberPath;
import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.Region;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.github.xuse.querydsl.sql.RelationalPathBaseEx;

public class QKnownRom extends RelationalPathBaseEx<KnownRom> {

    public final static QKnownRom knownRom = new QKnownRom("kno");

    public final NumberPath<Integer> id = createNumber("id", int.class);

    public final StringPath md5 = createString("md5");

    public final StringPath crc = createString("crc");

    public final StringPath sha1 = createString("sha1");

    public final NumberPath<Long> romSize = createNumber("romSize", long.class);

    public final StringPath gameName = createString("gameName");

    public final StringPath romFileName = createString("romFileName");

    public final StringPath parentName = createString("parentName");

    public final EnumPath<Region> region = createEnum("region", Region.class);

    public final EnumPath<Platform> platform = createEnum("platform", Platform.class);

    public final StringPath source = createString("source");

    public final StringPath datVersion = createString("datVersion");

    public QKnownRom(String variable) {
        super(KnownRom.class, variable);
        super.scanClassMetadata();
    }
}
