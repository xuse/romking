package io.github.xuse.romking.repo.obj;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.DateTimePath;
import io.github.xuse.romking.tasks.TaskType;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.core.types.dsl.EnumPath;
import java.util.Date;
import com.github.xuse.querydsl.sql.RelationalPathBaseEx;

public class QGlobalTask extends RelationalPathBaseEx<GlobalTask> {

    public final static QGlobalTask globalTask = new QGlobalTask("glo");

    public final NumberPath<Integer> id = createNumber("id", int.class);

    public final EnumPath<TaskType> type = createEnum("type", TaskType.class);

    public final StringPath name = createString("name");

    public final DateTimePath<Date> begin = createDateTime("begin", Date.class);

    public final DateTimePath<Date> end = createDateTime("end", Date.class);

    public final StringPath cost = createString("cost");

    public final StringPath result = createString("result");

    public final NumberPath<Integer> code = createNumber("code", int.class);

    public QGlobalTask(String variable) {
        super(GlobalTask.class, variable);
        super.scanClassMetadata();
    }
}
