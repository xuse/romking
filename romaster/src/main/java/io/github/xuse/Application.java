package io.github.xuse;

import java.time.Clock;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.github.xuse.querydsl.sql.SQLQueryFactory;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import io.github.xuse.romking.RomConsole;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Theme("default")
@Slf4j
public class Application implements AppShellConfigurator {

	@Bean
	public RomConsole romConsole(ConfigurableApplicationContext context) {
		RomConsole console=new RomConsole();
		BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) context.getBeanFactory();
		for(Map.Entry<String,Object> innerBeans:console.getContext().beans()) {
			String name=innerBeans.getKey();
			Object bean=innerBeans.getValue();
			@SuppressWarnings({ "unchecked", "rawtypes" })
			BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition((Class)bean.getClass(),()->bean);
			beanFactory.registerBeanDefinition(name, bdb.getBeanDefinition());
			log.info("Bean from simpleContext registe to Spring [{}]->{}",name,bean);
		}
		return console;
	}

	@Bean
	public SQLQueryFactory sqlFactory(RomConsole console) {
		return console.getFactory();
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone(); // You can also use Clock.systemUTC()
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
