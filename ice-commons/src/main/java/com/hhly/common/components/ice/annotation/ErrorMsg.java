/*
 * @Project Name: ice-all
 * @File Name: ErrorMsg
 * @Package Name: com.hhly.common.components.ice.annotation
 * @Date: 2017/3/29 11:31
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ICE 预定义业务错误提示
 *
 * @author shenxiaoping-549
 * @date 2017/3/29 11:31
 * @see
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErrorMsg {

	String code();

	String msg();

}
