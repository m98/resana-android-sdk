package io.resana;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to indicate that a field's value must be base64 decoded on deserialization and
 * base64 encoded on serialization.
 * <p>
 * The annotation must be only used on {@link String} fields.
 *
 * @author Hojjat Imani
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Base64 {
}
