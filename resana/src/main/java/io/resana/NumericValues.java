package io.resana;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to Specify a valid set of values for a numeric field
 * <p>
 * This annotation must only be used on numerics like {@link Integer}, {@link Double}
 *
 * @author Hojjat Imani
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface NumericValues {
    double[] value();
}
