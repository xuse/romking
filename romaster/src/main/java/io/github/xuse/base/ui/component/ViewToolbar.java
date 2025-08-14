package io.github.xuse.base.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

public final class ViewToolbar extends Composite<Header> {
    /**
     * 构造一个带有标题和操作组件的工具栏
     * 
     * @param viewTitle  工具栏的标题
     * @param components 可变参数，表示工具栏中包含的操作组件
     */
    public ViewToolbar(String viewTitle, Component... components) {
        // 设置工具栏的样式，使其内容在垂直方向上以列的形式排列，并在水平方向上居中对齐
        addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, AlignItems.STRETCH, Gap.MEDIUM,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);
        // 创建抽屉切换按钮，并移除其外边距
        var drawerToggle = new DrawerToggle();
        drawerToggle.addClassNames(Margin.NONE);
        // 创建工具栏标题，并设置其字体大小、去除外边距并设置为轻量级字体
        var title = new H1(viewTitle);
        title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.LIGHT);
        // 将抽屉切换按钮和标题组合在一个容器中，并使它们在水平方向上居中对齐
        var toggleAndTitle = new Div(drawerToggle, title);
        toggleAndTitle.addClassNames(Display.FLEX, AlignItems.CENTER);
        // 将组合后的抽屉切换按钮和标题添加到工具栏的内容中
        getContent().add(toggleAndTitle);
        // 如果提供了操作组件，则将它们添加到工具栏中
        if (components.length > 0) {
            // 将所有操作组件组合在一个容器中，并设置相应的样式
            var actions = new Div(components);
            actions.addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, Flex.GROW, Gap.SMALL,
                    FlexDirection.Breakpoint.Medium.ROW);
            // 将操作组件添加到工具栏的内容中
            getContent().add(actions);
        }
    }

    public static Component group(Component... components) {
        var group = new Div(components);
        group.addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.STRETCH, Gap.SMALL,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);
        return group;
    }
}
