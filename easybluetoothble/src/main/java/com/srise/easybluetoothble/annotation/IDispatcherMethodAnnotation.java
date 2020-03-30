package com.srise.easybluetoothble.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IDispatcherMethodAnnotation {
    String[] CharacteristicUUID();

    int requestId() default -1;
}
