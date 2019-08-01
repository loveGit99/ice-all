/*
 * Project Name: cmp-ice
 * File Name: Constants.java
 * Package Name: com.hhly.common.components.ice.constant
 * Date: 2016年11月26日下午3:43:28
 * Creator: shenxiaoping
 * ------------------------------
 * 修改人: 
 * 修改时间: 
 * 修改内容: 
 */

package com.hhly.common.components.ice.customer.constants;

/**
 * ICE 组件常量
 *
 * @author Allen Shen
 * @date 2016年11月26日下午3:43:28
 * @see
 */
public class ICEConstants {

	/*
	 * ============================ICE SETTING CONSTANTS ======================
	 */

	/**
	 * 客户端配置对应service ICE 的 endpoints 地址.
	 */
	public final static String ICE_PROVIDER_ENDPOINTS_PREFIX = "ice.provider.endpoints";
	/**
	 * ICE 客户端配置 前缀
	 */
	public final static String ICE_PROVIDER_PREFIX = "ice.provider";
	/**
	 * ice 请求超时默认值
	 */
	public final static Integer ICE_TIEMOUT_MS_DEFAULT = 5000;
	/**
	 * ICE 调用告警阈值，毫秒为单位
	 */
	public final static String ICE_PROVIDER_COST_THRESHOLD_SECONDES = "ice.provider.cost.max.seconds";
	/**
	 * 最大线程数
	 */
	public final static String ICE_PROVIDER_THREAD_MAX = "Ice.ThreadPool.Client.SizeMax";
	public final static String ICE_THREAD_MAX = "30";
	/**
	 * 最小线程数
	 */
	public final static String ICE_PROVIDER_THREAD_CORE = "Ice.ThreadPool.Client.Size";
	public final static String ICE_THREAD_CORE = "10";
	/**
	 * 最大传输大小，单位是kB, 默认4M
	 */
	public final static String ICE_MSG_MAX_SIZE = "4096";
	public final static String ICE_PROVIDER_MSG_MAX_SIZE = "Ice.MessageSizeMax";

	public final static int ICE_ENDPOINT_TYPE_ZK = 0;

	public final static int ICE_ENDPOINT_TYPE_CUSTOM = 1;

	/*
	 * ==============================================
	 */

	public static String ICE_ROOT_PATH = null;

	public final static String DYNAMIC_LOG_ENABLE = "dynamic.log.enable";
	public final static String DYNAMIC_LOG_TIME = "dynamic.log.time";

}
