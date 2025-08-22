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
        addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, AlignItems.STRETCH, Gap.MEDIUM,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);
        var drawerToggle = new DrawerToggle();
        drawerToggle.addClassNames(Margin.NONE);
        var title = new H1(viewTitle);
        title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.LIGHT);
        var toggleAndTitle = new Div(drawerToggle, title);
        toggleAndTitle.addClassNames(Display.FLEX, AlignItems.CENTER);
        getContent().add(toggleAndTitle);
        // 如果提供了操作组件，则将它们添加到工具栏中
        if (components.length > 0) {
            var actions = new Div(components);
            actions.addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, Flex.GROW, Gap.SMALL,
                    FlexDirection.Breakpoint.Medium.ROW);
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
