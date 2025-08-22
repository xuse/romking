package io.github.xuse.romking.repo.obj;

import com.querydsl.core.types.dsl.NumberPath;
import io.github.xuse.romking.repo.enums.MediaType;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.github.xuse.querydsl.sql.RelationalPathBaseEx;

public class QMediaFile extends RelationalPathBaseEx<MediaFile> {

    public final static QMediaFile mediaFile = new QMediaFile("med");

    public final NumberPath<Long> id = createNumber("id", long.class);

    public final NumberPath<Integer> dirId = createNumber("dirId", int.class);

    public final StringPath filepath = createString("filepath");

    public final NumberPath<Integer> referCount = createNumber("referCount", int.class);

    public final StringPath ext = createString("ext");

    public final EnumPath<MediaType> type = createEnum("type", MediaType.class);

    public final StringPath md5 = createString("md5");

    public QMediaFile(String variable) {
        super(MediaFile.class, variable);
        super.scanClassMetadata();
    }
}
