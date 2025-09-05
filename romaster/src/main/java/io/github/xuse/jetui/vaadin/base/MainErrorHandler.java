package io.github.xuse.jetui.vaadin.base;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MainErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(MainErrorHandler.class);

    @Bean
    /**
     * 配置一个Vaadin服务初始化监听器，用于全局错误处理
     * 
     * @return 返回一个VaadinServiceInitListener实例，用于在服务初始化时注册会话初始化监听器
     */
    public VaadinServiceInitListener errorHandlerInitializer() {
        // 返回一个Lambda表达式，该表达式定义了服务初始化时的行为
        return (event) -> {
            // 为每个新会话添加一个初始化监听器
            event.getSource().addSessionInitListener(sessionInitEvent -> {
                // 设置自定义的错误处理器
                sessionInitEvent.getSession().setErrorHandler(errorEvent -> {
                    // 记录错误日志
                    log.error("An unexpected error occurred", errorEvent.getThrowable());
                    // 当发生错误时，查找相关的UI组件，并显示一个通知
                    errorEvent.getComponent().flatMap(Component::getUI).ifPresent(ui -> {
                        // 创建并配置通知
                        var notification = new Notification(
                                "An unexpected error has occurred. Please try again later.");
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notification.setPosition(Notification.Position.TOP_CENTER);
                        notification.setDuration(3000);
                        // 在UI线程中打开通知
                        ui.access(notification::open);
                    });
                });
            });
        };
    }
}
