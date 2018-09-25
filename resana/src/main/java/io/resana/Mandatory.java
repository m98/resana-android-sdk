package io.resana;

import java.lang.annotation.*;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation to mark mandatory fields of a DTO.
 * <p>
 * This annotation must only be used on Object types (not primitives)
 *
 * @author Hojjat Imani
 */
@Retention(RetentionPolicy.RUNTIME)
@interface Mandatory {
}