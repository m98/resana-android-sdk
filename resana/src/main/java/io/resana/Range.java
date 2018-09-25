package io.resana;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to specify the valid range for a numeric field.
 * <p>
 * This annotation must only be used with numeric fields like {@link Integer} , {@link Float} , {@link Double}
 *
 * @author Hojjat Imani
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Range {
    double from();

    double to();
}