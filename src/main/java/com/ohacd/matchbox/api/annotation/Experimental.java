package com.ohacd.matchbox.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks APIs that are experimental and may change in future releases.
 * Use with caution; experimental APIs may be promoted to stable or removed.
 *
 * @since 0.9.5
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface Experimental {
}
