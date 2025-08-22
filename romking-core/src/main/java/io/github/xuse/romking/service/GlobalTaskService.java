package io.github.xuse.romking.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.github.xuse.querydsl.datatype.util.Threads;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.util.DateUtils;

import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.repo.obj.QGlobalTask;
import io.github.xuse.romking.tasks.ProcessResult;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskType;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class GlobalTaskService {
	
	public static final QGlobalTask table=QGlobalTask.globalTask;
	
	@Inject
	private SQLQueryFactory factory; 
	
	private final ExecutorService taskPool=Threads.newFixedThreadPool(2, "GlobalTasks");
	
	private final List<Task> activeTasks=new ArrayList<>();
	
	private static GlobalTaskService globals;
	
	public GlobalTaskService() {
		globals = this;
	}
	
	public static GlobalTaskService getInstance() {
		return globals;
	}
	
	public List<GlobalTask> list(int limit){
		List<GlobalTask> tasks=new ArrayList<>();
		for(Task active:activeTasks ) {
			tasks.add(GlobalTask.fromActive(active));
		}
		int left=limit-tasks.size();
		if(left>0) {
			 List<GlobalTask> dbTasks= factory.selectFrom(table).limit(left).fetch();
			 tasks.addAll(dbTasks);
		}
		return tasks;
	}

	
	public void submit(Task raw) {
		checkTasks(raw);
		activeTasks.add(raw);
		Runnable task=()->{
			ProcessResult result;
			try {
				result= raw.execute();
			}catch(Exception ex){
				log.error("global task {}.{} error.",raw.getType(),raw.getName(),ex);
				result=new ProcessResult(400,ex.getMessage());
			}
			try {
				saveTask(raw, result);
			}catch(Exception ex) {
				log.error("save task {}.{} error",raw.getType(),raw.getName(),ex);
			}finally {
				activeTasks.remove(raw);
			}
		};
		taskPool.submit(task);
	}

	private void saveTask(Task raw, ProcessResult result) {
		long end=System.currentTimeMillis();
		GlobalTask bean=new GlobalTask();
		
		bean.setBegin(new Date(raw.getBegin()));
		bean.setEnd(new Date(end));
		bean.setCost(DateUtils.formatTimePeriod((end-raw.getBegin())/1000L));
		bean.setType(raw.getType());
		bean.setName(raw.getName());
		
		bean.setResult(result.getMessage());
		bean.setCode(result.getCode());
		
		factory.insert(table).populate(bean).execute();
	}

	private void checkTasks(Task raw) {
		if(activeTasks.size()>=2) {
			throw new IllegalStateException("活动任务数已经达到2");
		}
		TaskType type=raw.getType();
		for (Task task : activeTasks) {
			if(task.getType()==type) {
				if(type.singleton) {
					throw new IllegalStateException("同类任务已经在运行");	
				}
				if(task.getName().equals(raw.getName())) {
					throw new IllegalStateException("相同的任务已经在运行");
				}
			}
		}
	}
	
}
