/*
 * @Project Name: ice-server
 * @File Name: ICEServerConst.java
 * @Package Name: com.hhly.common.components.ice.server.constants
 * @Date: 2017年1月18日下午4:39:34
 * @Creator: shengxiaping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.constants;

import java.util.Properties;

import com.hhly.common.components.ice.server.util.ServerConfigLoader;
import com.hhly.common.components.ice.utils.ConfigLoader;

/**
 * 服务端常量
 *
 * @author shengxiaping-549
 * @date 2017年1月18日下午4:39:34
 * @see
 */
public class ICEServerConst {

	private final static Properties SETTING = ServerConfigLoader.loadServerConfig();
	public final static String ICE_SERVER_THREADS_CORE = ConfigLoader.getString(SETTING, "Ice.ThreadPool.Server.Size",
			"50");
	public final static String ICE_SERVER_THREADS_MAX = ConfigLoader.getString(SETTING, "Ice.ThreadPool.Server.SizeMax",
			"100");
	public final static String ICE_SERVER_MSG_MAX_SIZE = ConfigLoader.getString(SETTING, "Ice.MessageSizeMax", "4096");

	/**********************
	 * ===========ICE key parameter setting
	 **************************/
	public static String ICE_IP = null;

	// port 的起始值
	public final static int ICE_PORT = ConfigLoader.getInt(SETTING, "ice.server.port.base", 4000);
	public final static String ICE_IP_PRIVATE = ConfigLoader.getString(SETTING, "ice.server.ip.private", "true");
	public final static String ICE_PROTOCOL = ConfigLoader.getString(SETTING, "ice.server.protocol", "tcp");
	// 自定义ip
	public final static String ICE_IP_CUSTOM = ConfigLoader.getString(SETTING, "ice.server.ip", null);

	// 自定义端口
	public final static Integer ICE_PORT_CUSTOM = ConfigLoader.getInteger(SETTING, "ice.server.port", null);

	/**************************
	 * =====================ice registry setting======================
	 ************/
	public final static String ICE_ENDPOINT_NODE = ConfigLoader.getString(SETTING, "ice.endpoints.node",
			"/ice-services-endpoints");
	public final static String ICE_RELEASE_VERSION = ConfigLoader.getString(SETTING, "service.release.version");
	public final static String ICE_SYSTEM_NAME = ConfigLoader.getString(SETTING, "ice.system.name");
	/**
	 * 服务注册端点
	 **/
	public final static String ICE_SERVERS_ENDPOINTS = ConfigLoader.getString(SETTING, "ice.servers.endpoints");
	public final static int ICE_EXPORT_SERVER = ConfigLoader.getInt(SETTING, "ice.servers.exposed", 1);

	/**
	 * DYNAMIC_LOG_ENABLE: 是否开启动态日志，默认打开
	 */
	public final static String DYNAMIC_LOG_ENABLE = ConfigLoader.getString(SETTING, "dynamic.log.enable", "true");
	/**
	 * DYNAMIC_LOG_TIME: 动态日志持续时间，默认10分钟
	 */
	public final static int DYNAMIC_LOG_TIME = ConfigLoader.getInt(SETTING, "dynamic.log.time", 10);

}
