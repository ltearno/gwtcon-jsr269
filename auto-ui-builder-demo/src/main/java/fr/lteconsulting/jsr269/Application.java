package fr.lteconsulting.jsr269;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Application implements EntryPoint {
	Person person = new Person("Arnaud", "Tournier", 35);
	
	Label label = new Label();

	PersonAutoUi personEditorUi = new PersonAutoUi();

	@Override
	public void onModuleLoad() {
		personEditorUi.setPerson(person);

		Button updateButton = new Button("Update POJO with UI fields");
		updateButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				personEditorUi.updatePerson(person);

				label.setText("Person pojo updated : " + person);
			}
		});

		VerticalPanel panel = new VerticalPanel();
		panel.add(personEditorUi);
		panel.add(updateButton);
		panel.add(label);

		RootPanel.get().add(panel);
	}
}
