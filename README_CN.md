# Romaster 阅读指南

Romaster是Romking的界面工程。

要在开发模式下启动应用程序，请将其导入到您的IDE并运行 `Application` 类。

您也可以通过命令行运行以下命令来启动应用程序：

```bash
./mvnw
```

要在生产模式下构建应用程序，请运行：
```bash
./mvnw -Pproduction package
```

要另外构建Docker镜像，请继续运行：

```bash
docker build -t my-application:latest .
```


禁用登录屏幕
默认情况下，项目中的所有视图都限制为经过身份验证的用户。这意味着如果您尝试在未登录的情况下访问任何视图，您将被重定向到登录屏幕。
要使视图公开访问，请将 @PermitAll 注解替换为来自 com.vaadin.flow.server.auth 包中的 @AnonymousAllowed。
例如，将此更改应用于 MainView 允许您在不登录的情况下打开 http://localhost:8080。

如果您允许匿名访问 TaskListView，您还需要更新 TaskService 中的方法级安全注解。
没有这些更改，视图可能会加载，但由于后端访问限制，数据获取或保存将失败。

移除安全性
此项目包括一个基本的、预先配置的安全设置。如果您希望从头开始实现自己的安全配置， 删除 security Java 包。
在此更改之后，您可能会遇到一些小的编译错误。这些错误通常发生在引用当前安全设置的地方 - 例如，以特定用户身份运行的集成测试，或显示当前用户名和头像的UI组件（如主布局）。

要解决这些问题，您可以删除受影响的代码或重构它以适应您的自定义安全设置。
