package fr.lteconsulting.jsr69;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation signifying we want to generate an editing UI for the POJO on which
 * it is placed
 * 
 * @author Arnaud Tournier
 */
@Target(TYPE)
@Retention(SOURCE)
public @interface AutoUi {
}
