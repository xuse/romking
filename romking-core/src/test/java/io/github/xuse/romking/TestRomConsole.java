package io.github.xuse.romking;

import javax.sql.DataSource;

import com.github.xuse.querydsl.config.ConfigurationEx;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.sql.log.QueryDSLSQLListener;
import com.github.xuse.querydsl.sql.support.SimpleDataSource;
import com.github.xuse.querydsl.sql.support.UpdateDeleteProtectListener;
import com.querydsl.sql.SQLTemplates;
import com.zaxxer.hikari.HikariDataSource;

import io.github.xuse.simple.context.ApplicationContext;

/**
 * 测试用的 RomConsole，使用内存 H2 数据库。
 * 每次创建都是全新的数据库实例，测试之间互不干扰。
 */
public class TestRomConsole {

	private final ApplicationContext context;
	private final SQLQueryFactory factory;
	private static int dbCounter = 0;

	public TestRomConsole() {
		String dbName = "mem:testdb_" + (++dbCounter);
		SimpleDataSource datasource = new SimpleDataSource();
		datasource.setDriverClass("org.h2.Driver");
		datasource.setUrl("jdbc:h2:" + dbName + ";DB_CLOSE_DELAY=-1");

		this.factory = getSqlFactory(datasource, datasource.getUrl());
		this.context = ApplicationContext.builder()
				.addBean("romConsole", this)
				.addBean("datasource", datasource)
				.addBean("factory", factory)
				.processResourceAnnotation(true)
				.scan("io.github.xuse.romking").build();
	}

	private static SQLQueryFactory getSqlFactory(DataSource datasource, String url) {
		return new SQLQueryFactory(querydslConfiguration(SQLQueryFactory.calcSQLTemplate(url)),
				wrapAsPool(datasource), true);
	}

	private static DataSource wrapAsPool(DataSource ds) {
		HikariDataSource pool = new HikariDataSource();
		pool.setDataSource(ds);
		return pool;
	}

	private static ConfigurationEx querydslConfiguration(SQLTemplates templates) {
		ConfigurationEx configuration = new ConfigurationEx(templates);
		configuration.setSlowSqlWarnMillis(5000);
		configuration.addListener(new QueryDSLSQLListener(QueryDSLSQLListener.FORMAT_DEBUG));
		configuration.addListener(new UpdateDeleteProtectListener());
		configuration.getScanOptions()
				.setAlterExistTable(true)
				.setAllowDropIndex(true)
				.setAllowDropConstraint(true);
		configuration.scanPackages("io.github.xuse.romking.repo.obj");
		return configuration;
	}

	public <T> T getBean(Class<T> clz) {
		return context.getBean(clz);
	}

	public SQLQueryFactory getFactory() {
		return factory;
	}
}
