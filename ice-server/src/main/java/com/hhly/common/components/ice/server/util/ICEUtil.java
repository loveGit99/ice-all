/*
 * Project Name: cmp-ice
 * File Name: ICEUtil.java
 * Package Name: com.hhly.common.components.ice.util
 * Date: 2016年11月26日下午5:06:04
 * Creator: shenxiaoping
 * ------------------------------
 * 修改人: 
 * 修改时间: 
 * 修改内容: 
 */

package com.hhly.common.components.ice.server.util;

import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.server.constants.ICEServerConst;
import com.hhly.common.components.ice.utils.ICECommonUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ICE UTIL
 *
 * @author Allen Shen
 * @date 2016年11月26日下午5:06:04
 * @see
 */
public class ICEUtil {

	private final static SNSLOG LOG = new SNSLOG(ICEUtil.class);

	public static String getICEServiceName(String serviceName, Class<?> clz) {
		if (serviceName.length() == 0) {
			String orgName = clz.getSimpleName();
			return Character.toLowerCase(orgName.charAt(0)) + orgName.substring(1);
		}
		return serviceName;
	}

	public static String getAdapterRandomName() {
		return System.currentTimeMillis() + "_Adapter";
	}

	/*
	* @author: shenxiaoping-549
	* @date: 2017/3/28 16:08
	* get the all interface for the special class
	*/
	public static Class<?>[] getAllInterface(Class<?> clz) {
		List<Class<?>> list = new ArrayList<>();
		while (clz != Object.class) {
			list.addAll(Arrays.asList(clz.getInterfaces()));
			clz = clz.getSuperclass();
		}
		LOG.debug("all interfaces == > {} ", list.toString());
		return list.toArray(new Class<?>[list.size()]);
	}

	public static String getICEEndpoint(String endpoint){
		return endpoint == null || "NA".equals(endpoint)  ? getEndPointByConf():endpoint ;
	}

	public static String getEndPointByConf(){
		String protocol = ICEServerConst.ICE_PROTOCOL;
		String ip = ICEServerConst.ICE_IP_CUSTOM;
		Integer port = ICEServerConst.ICE_PORT_CUSTOM;
		if(ip == null){
			ip = getIPByConf(ICEServerConst.ICE_IP_PRIVATE);
		}
		if(port == null){
			port = ICECommonUtil.getRandPort(ICEServerConst.ICE_PORT);
		}
		String iceEndpoint = protocol + " -h " + ip +" -p " + port;
		LOG.warn(" \n\n ice server endpoint is [{}]",iceEndpoint);
		return iceEndpoint;
	}

	private static String getIPByConf(String isPrivateService) {
		String ip = null;
		InetAddress address = null;
		if("true".equalsIgnoreCase(isPrivateService)){
			address = ICECommonUtil.getLocalAddress();
			if(address == null){
				LOG.error("fail to get private ip , SYSTEM EXIST NOW!");
				System.exit(1);
			}
		}
		else{
			address = ICECommonUtil.getInternetIP();
			if(address == null){
				LOG.error("fail to get internet ip , SYSTEM EXIST NOW!");
				System.exit(1);
			}
		}
		ip = address.getHostAddress();
		ICEServerConst.ICE_IP = ip;
		return ip;
	}
}
