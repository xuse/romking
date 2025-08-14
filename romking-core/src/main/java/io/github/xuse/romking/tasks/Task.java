package io.github.xuse.romking.tasks;

public interface Task{
	TaskType getType();
	
	String getName();
	
	String getProgress();
	
	ProcessResult execute();
	
	long getBegin();
}
