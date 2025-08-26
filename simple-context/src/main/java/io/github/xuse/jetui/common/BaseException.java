package io.github.xuse.jetui.common;


public class BaseException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	/**
	 * 错误编码。无异常默认为0
	 */
	protected int errCode = 0;

	/**
	 * 消息参数
	 */
	protected Object[] args = null;

	/**
	 * 构造（转封装时）
	 * 
	 * @param cause
	 *            上一个异常
	 * @param errCode
	 *            错误码
	 * @param params
	 *            错误消息和错误消息有关的参数
	 * @see #errCode
	 */
	public BaseException(Throwable cause, int errCode, Object... params) {
		super(cause);
		this.errCode = errCode;
		this.args = params;
		setStackTrace(cause.getStackTrace());
	}

	/**
	 * 构造(新构造时)
	 * 
	 * @param errCode
	 *            错误码
	 * @param args
	 *            错误消息
	 */
	public BaseException(int errCode, Object... args) {
		this.errCode = errCode;
		this.args = args;
		super.fillInStackTrace();
	}

	/**
	 * @see #errCode
	 */
	public int getErrCode() {
		return errCode;
	}

	/**
	 * 阻止填充异常堆栈以优化性能。
	 */
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}

	public BaseException() {
		super();
	}

	public BaseException(Throwable cause) {
		super(cause);
		this.args = new Object[] { cause.getMessage() };
	}

	public BaseException(String message) {
		super(message);
		this.args = new Object[] { message };
	}

	public Object[] getFormatObjects() {
		return args;
	}

	public void setFormatObjects(Object... formatObjects) {
		this.args = formatObjects;
	}
}
