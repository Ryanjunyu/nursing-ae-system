package com.his.ae.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 操作审计注解：标注在需要留痕的接口方法上 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
    /** LOGIN / REPORT / VIEW / UPDATE / DELETE / EXPORT */
    String actionType();

    /** 事件 / 患者 / 用户 / 字典 / 系统 */
    String targetType() default "系统";
}
