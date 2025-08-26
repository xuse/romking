package io.github.xuse.jetui.common;

/**
 * 错误处理常量定义
 * 
 * @author jiyi
 * 
 */
public class ErrorCodes {

	public static final int UNKNOWN_EXCEPTION = 9998;

	public static final int LOGIN_PASSWORD_ERROR = 1001;

	public static final int ACCOUNT_INVALID = 1002;

	public static final int ACCOUNT_LOCKED = 1003;

	public static final int NOT_DELETE_DUETO_APPAPACE = 1010;

	public static final int NOT_DELETE_DUETO_ORDERED = 1011;

	public static final int CLOUD_OPERATE_TIMEOUT = 1101;
	
	public static final int CLOUD_STORAGE_ERROR = 1102;


	/*
	 * 20000---29999段
	 */

	/**
	 * 导入配置单的版本ID不存在
	 */
	public static final int APP_VERSION_NOT_EXIST = 20001;
	/**
	 * 装配单格式异常，非XML或JSON
	 */
	public static final int DEPLOY_LIST_FORMAT_ERROR = 20002;
	/**
	 * 版本ID与appID不匹配
	 */
	public static final int APP_VERSION_NOT_MATCH_APP_ID = 20003;
	/**
	 * 配置单的版本ID与依赖服务部署文件的版本ID不匹配
	 */
	public static final int APP_VERSION_NOT_MATCH_FILE_ID = 20004;
	/**
	 * 同一应用存在多个已部署版本
	 */
	public static final int APP_MULTI_DEPLOYED = 20005;

	/**
	 * 同一版本已经部署
	 */
	public static final int APP_VERSION_HAD_DEPLOYED = 20006;

	/**
	 * 配置单中依赖服务名不存在
	 */
	public static final int DEPEND_SERVICE_NAME_NOT_EXIST = 20007;

	/**
	 * 装配过程异常
	 */
	public static final int DEPLOY_ERROR = 20008;

	/**
	 * 部署信息必填项为空
	 */
	public static final int DEPLOY_CFG_MISS = 20009;

	/**
	 * 应用ID不匹配
	 */
	public static final int APP_ID_NOT_MATCH = 20010;

	/**
	 * 同一App类型存在多个部署镜像
	 */
	public static final int MULTI_APP_IMAGE = 20011;

	/**
	 * App类型无对应部署镜像
	 */
	public static final int NO_APP_IMAGE = 20012;

	/**
	 * 依赖服务名字重复
	 */
	public static final int DEPENDENCY_NAME_REPEAT = 20013;

	/**
	 * 数据库读取错误
	 */
	public static final int DATABASE_READ_ERROR = 20014;

	/**
	 * 数据库写入错误
	 */
	public static final int DATABASE_WRITE_ERROR = 20015;
	
	/**
	 * 应用尚未部署
	 */
	public static final int NOT_DEPLOYED = 20016;
	
	/**
	 * 应用无需扩展
	 */
	public static final int NEED_NOT_SCALE = 20017;
}
