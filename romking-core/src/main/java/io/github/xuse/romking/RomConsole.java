package io.github.xuse.romking;

import java.io.File;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter.Feature;
import com.github.xuse.querydsl.config.ConfigurationEx;
import com.github.xuse.querydsl.lambda.LambdaTable;
import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.github.xuse.querydsl.sql.log.QueryDSLSQLListener;
import com.github.xuse.querydsl.sql.support.SimpleDataSource;
import com.github.xuse.querydsl.sql.support.UpdateDeleteProtectListener;
import com.github.xuse.querydsl.util.IOUtils;
import com.querydsl.sql.SQLTemplates;
import com.zaxxer.hikari.HikariDataSource;

import io.github.xuse.romking.metadata.ee.GameListService;
import io.github.xuse.romking.metadata.ee.Gamelist;
import io.github.xuse.romking.repo.obj.QRomDir;
import io.github.xuse.romking.repo.obj.QRomFile;
import io.github.xuse.romking.repo.obj.RomDir;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.service.RomImportService;
import io.github.xuse.romking.util.RandomData;
import io.github.xuse.simple.context.ApplicationContext;
import io.github.xuse.simple.context.Inject;
import lombok.Getter;

@Getter
public class RomConsole {

	private ApplicationContext context;
	
	private SQLQueryFactory factory;

	@Inject
	private RomImportService repoService;

	@Inject
	private GameListService gameListService;

	@Test
	public void ScanTest() {
		File root = new File("./Card");
		File target = new File("./Card2");

		for (File xml : IOUtils.listFiles(root, "xml")) {
			Gamelist list = gameListService.loadXml(xml);
			System.out.println(JSON.toJSONString(list, Feature.PrettyFormat));
		}
	}
	
	@Test
	public void testDatabase() {
		LambdaTable<RomDir> t=()->RomDir.class;
		
		List<RomDir> roms=factory.selectFrom(t).fetch();
		System.out.println(roms);
	}
	
	@Test
	public void testRomFile() {
		RomFile o=RandomData.newInstance(RomFile.class);
		System.out.println(o.getCreateTime());
		System.out.println(o.getRomModified());
		factory.insert(QRomFile.romFile).populate(o).execute();
		
		System.out.println(factory.select(QRomFile.romFile).fetchCount());
	}

	public RomConsole() {
		SimpleDataSource datasource = new SimpleDataSource();
		datasource.setDriverClass("org.h2.Driver");
		datasource.setUrl("jdbc:h2:~/romking");
		SQLQueryFactory db = getSqlFactory(datasource, datasource.getUrl());
		this.factory=db;
		this.context = ApplicationContext.builder()
				.addBean("romConsole", this)
				.addBean("datasource", datasource)
				.addBean("factory", db)
				.processResourceAnnotation(true)
				.scan("io.github.xuse.romking").build();
	}

	protected static SQLQueryFactory getSqlFactory(DataSource datasource, String url) {
		return new SQLQueryFactory(querydslConfiguration(SQLQueryFactory.calcSQLTemplate(url)),
				wrapAsPool(datasource), true);
	}

	private static DataSource wrapAsPool(DataSource ds) {
		HikariDataSource pool = new HikariDataSource();
		pool.setDataSource(ds);
		return pool;
	}

	public static ConfigurationEx querydslConfiguration(SQLTemplates templates) {
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
	
	public <T> T getBean(Class<T> clz){
		return context.getBean(clz);
	}

	public SQLQueryFactory getFactory() {
		return factory;
	}

}
