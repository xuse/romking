package io.github.xuse.jetui.vaadin.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;

import io.github.xuse.security.AppUserInfo;
import io.github.xuse.security.CurrentUser;

@Layout
@AnonymousAllowed // Allow all users, including anonymous ones. If you want only authenticated
                  // users, change to @PermitAll.
public final class MainLayout extends AppLayout {

    private final transient AuthenticationContext authenticationContext;

    /**
     * 构造函数：初始化MainLayout布局
     * 该构造函数主要用于设置主界面的主要组成部分，包括认证上下文、主导航栏以及用户相关的菜单
     *
     * @param currentUser           当前登录的用户对象，用于获取当前用户信息并根据用户状态调整界面布局
     * @param authenticationContext 认证上下文对象，用于管理用户的认证状态和相关信息
     */
    MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext) {
        // 设置认证上下文，以便在需要时可以访问用户认证相关的信息
        this.authenticationContext = authenticationContext;

        // 设置主布局的首要部分为主导航栏（DRAWER），表明主导航是界面的重要组成部分
        setPrimarySection(Section.DRAWER);

        // 向主导航栏中添加头部组件和侧边导航栏，使用Scroller包裹以支持滚动功能
        addToDrawer(createHeader(), new Scroller(createSideNav()));

        // 如果当前有用户登录，则向主导航栏中添加用户菜单，以便用户可以进行相关操作，如登出等
        currentUser.get().ifPresent(user -> addToDrawer(createUserMenu(user)));
    }

    /**
     * 创建应用头部组件
     * 
     * 此方法负责构建并返回一个包含应用图标和名称的头部Div组件该头部是用户界面的一部分，
     * 用于展示应用的标识信息头部组件具有特定的样式，以确保在界面上的视觉一致性
     *
     * @return 返回一个Div对象，其中包含了应用图标和名称，且应用了特定的样式
     */
    private Div createHeader() {
        // 创建应用图标，并设置其颜色和大小
        var appLogo = VaadinIcon.GAMEPAD.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        // 创建应用名称文本，并设置其字体粗细和大小
        var appName = new Span("Rom King");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);
        List<MenuEntry> entries = new ArrayList<>(MenuConfiguration.getMenuEntries());
        boolean testDiv=false;
        for (int i = 0; i < entries.size(); i++) {
            MenuEntry entry = entries.get(i);
            Class<?> clz=entry.menuClass();
            SideNavItem item= createSideNavItem(entry);
            if(!testDiv && clz.getSimpleName().startsWith("Test")) {
            	nav.getElement().appendChild(new Hr().getElement());
            	testDiv=true;
            }
            nav.addItem(item);
        }
        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        if (menuEntry.icon() != null) {
            return new SideNavItem(menuEntry.title(), menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            return new SideNavItem(menuEntry.title(), menuEntry.path());
        }
    }

    /**
     * 创建用户菜单组件
     * 
     * @param user 用户信息对象，包含用户全名、头像URL和配置URL等信息
     * @return 配置好的用户菜单栏组件
     */
    private Component createUserMenu(AppUserInfo user) {
        // 创建并配置用户头像组件，设置大小、边距和颜色
        var avatar = new Avatar(user.getFullName(), user.getPictureUrl());
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        avatar.addClassNames(Margin.Right.SMALL);
        avatar.setColorIndex(5);

        // 创建并配置用户菜单栏，应用主题和边距样式
        var userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassNames(Margin.MEDIUM);

        // 添加主菜单项，使用头像作为图标并显示用户全名
        var userMenuItem = userMenu.addItem(avatar);
        userMenuItem.add(user.getFullName());

        // 如果存在个人资料URL，则添加"查看个人资料"子菜单项
        if (user.getProfileUrl() != null) {
            userMenuItem.getSubMenu().addItem("View Profile",
                    event -> UI.getCurrent().getPage().open(user.getProfileUrl()));
        }
        // 添加"退出登录"子菜单项，触发登出操作
        userMenuItem.getSubMenu().addItem("Logout", event -> authenticationContext.logout());

        return userMenu;
    }

}
