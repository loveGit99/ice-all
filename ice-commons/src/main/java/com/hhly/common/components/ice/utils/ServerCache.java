/*
 * @Project Name: ice-all
 * @File Name: ServerCache
 * @Package Name: com.hhly.common.components.ice.utils
 * @Date: 2017/5/18 15:45
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * server cache
 *
 * @author shenxiaoping-549
 * @date 2017/5/18 15:45
 * @see
 */
public class ServerCache {

	public static ApplicationContext SPRING_CTX ;

	public static void setSpringCtx(ApplicationContext ctx){
		SPRING_CTX = ctx;
	}

}
