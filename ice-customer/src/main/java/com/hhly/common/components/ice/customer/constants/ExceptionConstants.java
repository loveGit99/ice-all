/*
 * @Project Name: ice-all
 * @File Name: ExceptionConstants
 * @Package Name: com.hhly.common.components.ice.customer.constants
 * @Date: 2017/4/12 16:36
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.constants;

import com.hhly.common.components.ice.domain.IceRes;

/**
 * ICE exception constants
 *
 * @author shenxiaoping-549
 * @date 2017/4/12 16:36
 * @see
 */
public class ExceptionConstants {

	public final static String CONNECTION_TIMEOUT_MSG = "网络链接超时";
	public final static String CONNECTION_TIMEOUT_CODE = "0001";
	public final static String CONNECTION_REFUSE_MSG = "网络连接被拒绝";
	public final static String CONNECTION_REFUSE_CODE = "0002";
	public final static String SYS_EXCEPTION_MSG = "系统异常";
	public final static String SYS_EXCEPTION_CODE = "1000";
	public final static String SPLIT_SYMBOL_CODE_MSG = String.valueOf((char) 30);
	public final static IceRes SYS_ERROR = new IceRes(ExceptionConstants.SYS_EXCEPTION_CODE,
			ExceptionConstants.SYS_EXCEPTION_MSG);
}
