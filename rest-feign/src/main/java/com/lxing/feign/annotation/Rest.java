package com.lxing.feign.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Rest {

  /**
   * The name of feign in ApplicationContext
   */
  String feignName() default "feign";

  Class<?> interfaceClass() default void.class;

  String interfaceName() default "";

  String version() default "";

  String url() default "";
}
