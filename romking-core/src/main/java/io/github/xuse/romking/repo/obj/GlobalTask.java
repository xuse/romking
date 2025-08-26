package io.github.xuse.romking.repo.obj;

import java.sql.Types;
import java.util.Date;

import com.github.xuse.querydsl.annotation.UnsavedValue;
import com.github.xuse.querydsl.annotation.dbdef.ColumnSpec;
import com.github.xuse.querydsl.annotation.dbdef.TableSpec;

import io.github.xuse.jetui.annotation.ViewColumn;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskType;
import lombok.Data;

@TableSpec(name="tasks",primaryKeys = "id")
@Data
public class GlobalTask {
	@ColumnSpec(type=Types.INTEGER,nullable = false,autoIncrement = true)
	private int id;
	
	@ViewColumn(caption="任务")
	@ColumnSpec(type=Types.INTEGER)
	private TaskType type;
	
	@ViewColumn(caption="名称")
	@ColumnSpec(nullable = false, size=128)
	private String name;
	
	@ViewColumn(caption="开始时间")
	@ColumnSpec(name="task_begin",nullable = false)
	private Date begin;
	
	@ViewColumn(caption="结束时间")
	@ColumnSpec(name="task_end",nullable = false)
	private Date end;
	
	@ViewColumn(caption="耗时")
	@ColumnSpec(name="task_cost",size=16)
	private String cost;
	
	@ViewColumn(caption="结果")
	@ColumnSpec(name="task_result",size=5000)
	private String result;
	
	@ColumnSpec(name="task_code",type=Types.INTEGER,nullable = false)
	@UnsavedValue(UnsavedValue.MinusNumber)
	private int code;
	
	
	
	public static GlobalTask fromActive(Task active) {
		GlobalTask task=new GlobalTask();
		task.setBegin(new Date(active.getBegin()));
		task.setCode(0);
		task.setCost("");
		task.setEnd(null);
		task.setType(active.getType());
		task.setName(active.getName());
		task.setResult(active.getProgress());
		return task;
	}
}
