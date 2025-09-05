package io.github.xuse.jetui.vaadin.base;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import io.github.xuse.jetui.vaadin.component.ViewToolbar;
import jakarta.annotation.security.PermitAll;

/**
 * This view shows up when a user navigates to the root ('/') of the
 * application.
 */
@Route
@PermitAll
public final class MainView extends Main {

    MainView() {
        addClassName(LumoUtility.Padding.MEDIUM);
        add(new ViewToolbar("Main"));
        add(new Div("请选择左侧的菜单"));
    }

    /**
     * Navigates to the main view.
     */
    public static void showMainView() {
        UI.getCurrent().navigate(MainView.class);
    }
}
