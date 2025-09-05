package io.github.xuse.romaster.ui.sample.content;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.theme.lumo.LumoIcon;

public class ShowIcons extends Div {
	public ShowIcons() {
		for (VaadinIcon i : VaadinIcon.values()) {
			Icon icon = i.create();
			icon.getStyle().set("padding", "0.5em");
			icon.setSize("64px");
			icon.getElement().setAttribute("title", "VaadinIcon." + i.name());
			icon.getElement().addEventListener("contextmenu", (e) -> {
				Notification.show("VaadinIcon." + i.name());
			}).preventDefault();
			icon.getElement().executeJs("""
					this.addEventListener('click', function(e) {
					        navigator.clipboard.writeText('$0');
					});""".replace("$0", "VaadinIcon." + i.name()));
			icon.addClickListener((e)->{
				Notification.show("Icon name copies to clipboard.");
			});
			add(icon);
		}

		add(new Hr());
		for (LumoIcon i : LumoIcon.values()) {
			Icon icon = i.create();
			icon.getStyle().set("padding", "0.5em");
			icon.setSize("64px");
			icon.getElement().setAttribute("title", "LumoIcon." + i.name());
			icon.getElement().executeJs("""
					this.addEventListener('click', function(e) {
					        navigator.clipboard.writeText('$0');
					});""".replace("$0", "LumoIcon." + i.name()));
			icon.addClickListener((e)->{
				Notification.show("Icon name copies to clipboard.");
			});
			add(icon);
		}
	}
}
