package io.github.xuse.romking.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import com.github.xuse.querydsl.datatype.util.Threads;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.util.DateUtils;

import io.github.xuse.jetui.repository.ListDataProvider;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.repo.obj.QGlobalTask;
import io.github.xuse.romking.tasks.ProcessResult;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskProgress;
import io.github.xuse.romking.tasks.TaskProgressListener;
import io.github.xuse.romking.tasks.TaskType;
import io.github.xuse.simple.context.Inject;
import io.github.xuse.simple.context.Service;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class GlobalTaskService implements ListDataProvider<GlobalTask, Void> {
	
	public static final QGlobalTask table=QGlobalTask.globalTask;
	
	@Inject
	private SQLQueryFactory factory; 
	
	private final ExecutorService taskPool=Threads.newFixedThreadPool(2, "GlobalTasks");
	
	private final List<Task> activeTasks=new ArrayList<>();
	
	private final List<TaskProgressListener> listeners = new CopyOnWriteArrayList<>();
	
	private static GlobalTaskService globals;
	
	public GlobalTaskService() {
		globals = this;
	}
	
	public static GlobalTaskService getInstance() {
		return globals;
	}
	
	@Override
	public int count(Optional<Void> f) {
		long dbCount=factory.selectFrom(table).fetchCount();
		return (int)dbCount+activeTasks.size();
	}

	@Override
	public Stream<GlobalTask> list(Optional<Void> f, int offset, int limit) {
		if(offset>0) {
			log.error("不支持翻页");
		}
		List<GlobalTask> tasks=new ArrayList<>();
		for(Task active:activeTasks ) {
			tasks.add(GlobalTask.fromActive(active));
		}
		int left=limit-tasks.size();
		if(left>0) {
			 List<GlobalTask> dbTasks= factory.selectFrom(table).limit(left).fetch();
			 tasks.addAll(dbTasks);
		}
		return tasks.stream();
	}
	
	public void submit(Task raw) {
		checkTasks(raw);
		activeTasks.add(raw);
		Runnable task=()->{
			ProcessResult result;
			try {
				ProgressMonitor monitor = new ProgressMonitor(raw);
				monitor.start();
				try {
					result= raw.execute();
				} finally {
					monitor.stop();
				}
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
				notifyTaskCompleted(raw, result);
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

	/**
	 * 是否有活动任务
	 */
	public boolean hasActiveTasks() {
		return !activeTasks.isEmpty();
	}

	/**
	 * 获取当前活动任务列表的快照
	 */
	public List<Task> getActiveTasks() {
		return new ArrayList<>(activeTasks);
	}

	public void addListener(TaskProgressListener listener) {
		listeners.add(listener);
	}

	public void removeListener(TaskProgressListener listener) {
		listeners.remove(listener);
	}

	private void notifyProgressChanged(Task task, TaskProgress progress) {
		for (TaskProgressListener listener : listeners) {
			try {
				listener.onProgressChanged(task, progress);
			} catch (Exception ex) {
				log.error("Listener error on progress change", ex);
			}
		}
	}

	private void notifyTaskCompleted(Task task, ProcessResult result) {
		for (TaskProgressListener listener : listeners) {
			try {
				listener.onTaskCompleted(task, result);
			} catch (Exception ex) {
				log.error("Listener error on task completed", ex);
			}
		}
	}

	private class ProgressMonitor {
		private final Task task;
		private volatile boolean running = true;
		private TaskProgress lastProgress;

		ProgressMonitor(Task task) {
			this.task = task;
		}

		void start() {
			Thread monitorThread = new Thread(() -> {
				while (running) {
					TaskProgress current = task.getTaskProgress();
					if (current != null && !current.equals(lastProgress)) {
						lastProgress = current;
						notifyProgressChanged(task, current);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}, "ProgressMonitor-" + task.getName());
			monitorThread.setDaemon(true);
			monitorThread.start();
		}

		void stop() {
			running = false;
			TaskProgress finalProgress = task.getTaskProgress();
			if (finalProgress != null && !finalProgress.equals(lastProgress)) {
				notifyProgressChanged(task, finalProgress);
			}
		}
	}
}
