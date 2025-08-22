package io.github.xuse.jetui.convert;


/**
 * 界面的枚举选项解析器。
 * 本系统中，对于枚举选项采用以下语法来描述 @{code 协议://枚举数据描述}。<br>
 * 举例如下
 * <ol>
 * <li><strong>enum://male:男性;female:女性;unknown:未知</strong><br>最基本的枚举数据方法，常量枚举。</li>
 * <li><strong>properties://hae.properties</strong><br>将classpath下的hae.properties文件读入，作为字典的枚举项</li>
 * <li><strong>sql://select name as text, keyword as code from tablename where type='test'</strong><br>用一个SQL语句查询结果作为枚举项</li>
 * <li><strong>dict://payment</strong><br>用类型为payment的数据字典作为枚举项</li>
 * </ol>
 * 上述四种是基本要求的协议，今后可以扩展。
 * 实现类需要能够解析协议描述，并将其转换为Map对象，其中包含枚举的键值对。
 * @author jiyi
 */
public interface SelectMapResolver {
	
	/**
	 * @param url 选择项字符串
	 * @param params 解析参数，可以传入null
	 * @return 解析配置的选择项字符串，转换为Map<String,String>
	 */
	OptionResolver parse(String url);
}
