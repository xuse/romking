package io.github.xuse.romking.tasks;

import java.util.List;

import lombok.Data;

@Data
public class ProcessResult {
	public ProcessResult(int code, String message) {
		this.code=code;
		this.message=message;
	}
	//200表示正常结束。0表示任务尚未开始
	private int code;
	private String message;
	private List<String> details;

}
