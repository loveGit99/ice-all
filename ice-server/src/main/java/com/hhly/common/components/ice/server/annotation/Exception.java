/*
 * Project Name: lib File Name: Exception.java Package Name:
 * com.hhly.common.components.ice.exception Date: 2016-12-30上午10:54:34 Creator:
 * bb.h ------------------------------ 修改人: 修改时间: 修改内容:
 */

package com.hhly.common.components.ice.server.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解异常排序
 *
 * @author bb.h
 * @date 2016-12-30上午10:54:34
 * @see
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Exception {

	int order() default 0;
}
