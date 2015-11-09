package fr.lteconsulting.jsr69;

import static javax.tools.Diagnostic.Kind.ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({ "fr.lteconsulting.jsr69.AutoUi" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AutoUiProcessor extends AbstractProcessor {
	private Messager messager;
	private Elements elementsUtils;
	private Types typeUtils;
	private Filer filer;

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// reference often used tools from the processingEnv
		messager = processingEnv.getMessager();
		elementsUtils = processingEnv.getElementUtils();
		typeUtils = processingEnv.getTypeUtils();
		filer = processingEnv.getFiler();

		// generate code for annotated elements
		Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(AutoUi.class);
		for (TypeElement element : ElementFilter.typesIn(annotatedElements)) {
			generateEditingUiClass(element);
		}

		// claim the annotation
		return true;
	}

	/**
	 * Generates an editor UI class corresponding to the given pojo class
	 * 
	 * @param classElement
	 *            Pojo class element
	 */
	void generateEditingUiClass(TypeElement classElement) {
		List<FieldInfo> classFields = getManagedFields(classElement);

		try {
			String simpleName = classElement.getSimpleName().toString();
			String packageName = elementsUtils.getPackageOf(classElement).getQualifiedName().toString();
			String typeName = classElement.getSimpleName().toString() + "AutoUi";

			JavaFileObject javaFile = filer.createSourceFile(packageName + "." + typeName, classElement);
			Writer writer = javaFile.openWriter();
			PrintWriter pw = new PrintWriter(writer);

			pw.println("package " + packageName + ";");
			pw.println("");
			pw.println("import com.google.gwt.user.client.ui.Composite;");
			pw.println("import com.google.gwt.user.client.ui.FlexTable;");
			pw.println("import com.google.gwt.user.client.ui.TextBox;");
			pw.println("");
			pw.println("/**");
			pw.println(" * Generated class creating an editor UI for the");
			pw.println(" * for the class {@link " + classElement.getQualifiedName().toString() + "}");
			pw.println(" * ");
			pw.println(" * @author Arnaud Tournier");
			pw.println(" */");
			pw.println("public class " + typeName + " extends Composite {");

			for (FieldInfo field : classFields)
				pw.println("\tprivate final TextBox " + field.getUiFieldName() + " = new TextBox();");
			pw.println();

			pw.println("\t/**");
			pw.println("\t * Constructs a " + typeName);
			pw.println("\t * ");
			pw.println("\t * <p>");
			pw.println("\t * It is ready to be inserted into a container Widget");
			pw.println("\t */");
			pw.println("\tpublic " + typeName + "() {");
			pw.println("\t\tFlexTable table = new FlexTable();");
			pw.println();
			for (int i = 0; i < classFields.size(); i++) {
				FieldInfo field = classFields.get(i);

				pw.println("\t\ttable.setText(" + i + ", 0, \"" + field.getPrettyName() + "\");");
				pw.println("\t\ttable.setWidget(" + i + ", 1, " + field.getUiFieldName() + ");");
				pw.println();
			}
			pw.println("\t\tinitWidget(table);");
			pw.println("\t}");
			pw.println();

			pw.println("\t/**");
			pw.println("\t * Fills the UI with the given POJO");
			pw.println("\t * ");
			pw.println("\t * @param pojo");
			pw.println("\t *            The object to get the data from");
			pw.println("\t */");
			pw.println("\tpublic void set" + simpleName + "(" + simpleName + " pojo) {");
			for (FieldInfo field : classFields)
				pw.println("\t\t" + field.getUiFieldName() + ".setText(\"\"+pojo." + field.getGetterName() + "());");
			pw.println("\t}");
			pw.println();

			pw.println("\t/**");
			pw.println("\t * Updates a POJO with the values currently present in the UI");
			pw.println("\t * ");
			pw.println("\t * @param pojo");
			pw.println("\t *            The object to which the data is set");
			pw.println("\t */");
			pw.println("\tpublic void update" + simpleName + "(" + simpleName + " pojo) {");
			for (FieldInfo field : classFields)
				pw.println("\t\tpojo." + field.getSetterName() + "(" + field.getToStringAccessor() + ");");
			pw.println("\t}");
			pw.println();

			pw.println("}");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Finds out the list of fields to be managed by the UI
	 * 
	 * @param classElement
	 *            Class element to be inspected
	 * @return
	 */
	private List<FieldInfo> getManagedFields(TypeElement classElement) {
		List<FieldInfo> result = new ArrayList<>();
		
		List<VariableElement> fields = ElementFilter.fieldsIn(classElement.getEnclosedElements());
		for (VariableElement field : fields) {
			boolean isManaged = true;

			// Any @Ignore annotation will skip the field
			List<? extends AnnotationMirror> annotationMirrors = field.getAnnotationMirrors();
			for (AnnotationMirror annotation : annotationMirrors) {
				if ("ignore".equalsIgnoreCase(annotation.getAnnotationType().asElement().getSimpleName().toString())) {
					isManaged = false;
					break;
				}
			}

			if (isManaged) {
				try {
					FieldInfo fieldInfo = new FieldInfo(classElement, field);
					result.add(fieldInfo);
				} catch (FieldException e) {
					messager.printMessage(ERROR, "[AutoUi] " + e.getMessage(), e.getElement());
				}
			}
		}

		return result;
	}

	/**
	 * Information on a managed field
	 * 
	 * @author Arnaud Tournier
	 *
	 */
	private class FieldInfo {
		private final TypeElement classElement;
		private final VariableElement field;
		private final String name;
		private final String prettyName;
		private final String className;

		/**
		 * @param classElement
		 *            Class element containing the field (should be
		 *            field.getEnclosingElement())
		 * @param field
		 *            The field to manage
		 * @throws FieldException
		 *             If the user code is not consistent (getter/setter
		 *             errors), this exception is thrown
		 */
		public FieldInfo(TypeElement classElement, VariableElement field) throws FieldException {
			this.classElement = classElement;
			this.field = field;
			this.name = field.getSimpleName().toString();
			this.prettyName = computePrettyName();
			this.className = field.asType().toString();

			checkFieldConsistency();
		}

		public String getPrettyName() {
			return prettyName;
		}

		/**
		 * Gets the field's UI element's name in the generated code
		 * 
		 * @return
		 */
		public String getUiFieldName() {
			return name + "TextBox";
		}

		/**
		 * Gets the getter name of the field
		 * 
		 * @return
		 */
		public String getGetterName() {
			return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
		}

		/**
		 * Gets the setter name of the field
		 * 
		 * @return
		 */
		public String getSetterName() {
			return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
		}

		/**
		 * Gets the code needed to access to the field and convert it to a
		 * String value.
		 * 
		 * <p>
		 * That's useful for setting the field's value in a TextBox
		 * 
		 * @return
		 */
		public String getToStringAccessor() {
			switch (className) {
			case "int":
			case "java.lang.Integer":
				return "Integer.parseInt(" + getUiFieldName() + ".getText())";

			default:
				return getUiFieldName() + ".getText()";
			}
		}

		/**
		 * Checks the field for error
		 */
		private void checkFieldConsistency() throws FieldException {
			checkGetter();
			checkSetter();
		}

		/**
		 * Checks the getter for error
		 */
		private void checkGetter() throws FieldException {
			String getterName = getGetterName();

			Optional<ExecutableElement> oGetter = ElementFilter.methodsIn(classElement.getEnclosedElements()).stream()
					.filter(m -> m.getSimpleName().toString().equals(getterName)).findFirst();
			if (oGetter == null || !oGetter.isPresent())
				throw new FieldException(field, "No getter found");

			ExecutableElement getter = oGetter.get();

			if (!getter.getReturnType().toString().equals(field.asType().toString()))
				throw new FieldException(getter, "Field type and getter return type are not equal : "
						+ getter.getReturnType() + " / " + field.asType());

			List<? extends VariableElement> p = getter.getParameters();
			if (p != null && !p.isEmpty())
				throw new FieldException(getter, "Getter should not have a parameter");
		}

		/**
		 * Checks the setter for error
		 */
		private void checkSetter() throws FieldException {
			String setterName = getSetterName();

			Optional<ExecutableElement> oSetter = ElementFilter.methodsIn(classElement.getEnclosedElements()).stream()
					.filter(m -> m.getSimpleName().toString().equals(setterName)).findFirst();
			if (oSetter == null || !oSetter.isPresent())
				throw new FieldException(field, "No setter found");

			ExecutableElement setter = oSetter.get();

			List<? extends VariableElement> p = setter.getParameters();
			if (p == null || p.size() != 1)
				throw new FieldException(setter, "Setter must have exactly one parameter");

			if (!typeUtils.isSameType(p.get(0).asType(), field.asType()))
				throw new FieldException(setter, "Setter's parameter is not assignable to the field type");
		}

		/**
		 * The pretty name is given either by :
		 * 
		 * <li>
		 * <ul>
		 * The {@link Label}'s value if the annotation is found on the field,
		 * <ul>
		 * Or the field's name in the Java code if the annotation was not found.
		 * </li>
		 * 
		 * @return
		 */
		private String computePrettyName() {
			Label labelAnnotation = field.getAnnotation(Label.class);
			if (labelAnnotation != null)
				return labelAnnotation.value();
			return name;
		}

		@Override
		public String toString() {
			return "FieldInfo [field=" + field + ", name=" + name + ", clazz=" + className + "]";
		}
	}
}
