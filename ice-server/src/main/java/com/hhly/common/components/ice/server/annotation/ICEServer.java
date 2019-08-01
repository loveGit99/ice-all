/*
 * Project Name: cmp-ice
 * File Name: ICEServer.java
 * Package Name: com.hhly.common.components.ice
 * Date: 2016年11月26日下午3:38:51
 * Creator: shenxiaoping
 * ------------------------------
 * 修改人: 
 * 修改时间: 
 * 修改内容: 
 */

package com.hhly.common.components.ice.server.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ICE server 注解
 *
 * @author Allen Shen
 * @date 2016年11月26日下午3:38:51
 * @see
 */
@Inherited
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ICEServer {

	String name() default "";
}
