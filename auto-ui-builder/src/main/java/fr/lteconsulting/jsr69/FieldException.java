package fr.lteconsulting.jsr69;

import javax.lang.model.element.Element;

@SuppressWarnings("serial")
class FieldException extends Exception {
	private Element element;

	public FieldException(Element element, String message) {
		super(message);
		this.element = element;
	}

	public Element getElement() {
		return element;
	}
}