/*
 * @Project Name: ice-all
 * @File Name: ICECommonUtil
 * @Package Name: com.hhly.common.components.ice.utils
 * @Date: 2017/4/10 11:23
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import com.hhly.base.util.SNSLOG;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * ice common util
 *
 * @author shenxiaoping-549
 * @date 2017/4/10 11:23
 * @see
 */
public class ICECommonUtil {

	private final static SNSLOG logger = new SNSLOG(ICECommonUtil.class);
	private final static String UTF8 = "UTF-8";
	public final static String LOCAL_HOST = "127.0.0.1";
	public final static String ANY_HOST = "0.0.0.0";
	//
	private static final int MAX_PORT = 65535;
	private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
	private int len;

	/*
	* @author: shenxiaoping-549
	* @date: 2017/4/10 11:38
	*  Object to json
	*/
	public static String toJson(Object obj) {
		return JSONObject.toJSONString(obj);
	}

	public static String byte2Str(byte[] bytes, String encoding) {
		try {
			return new String(bytes, encoding);
		} catch (UnsupportedEncodingException e) {
			logger.error("un support encoding [{}]", encoding, e);
		}
		return null;
	}

	public static String byte2Str(byte[] bytes) {
		return byte2Str(bytes, UTF8);
	}

	public static InetAddress getLocalAddress() {
		return getLocalIP();
	}

	private static boolean isValidAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress()) {
			logger.error("error ip[{}], please check your OS ip-host mapping config!",address.getHostAddress());
			return false;
		}
		String name = address.getHostAddress();
		return (name != null && !ANY_HOST.equals(name) && !LOCAL_HOST.equals(name) && IP_PATTERN.matcher(name)
				.matches());
	}

	public static long str2IPNumber(String ip) {
		if (StringUtils.isBlank(ip)) {
			return -1L;
		}
		String[] bits = ip.split("\\.");
		int len = bits.length;
		if (len != 4) {
			logger.error("is not a valid ip[{}]", ip);
			return -2L;
		}
		long ipNum = 0L;
		for (int i = 0; i < len; i++) {
			ipNum += Long.valueOf(bits[i]) << (8 * (3 - i));
		}
		return ipNum;
	}

	/**
	 * 内网IP地址
	 * 10.0.0.0/8：10.0.0.0～10.255.255.255
	 * 172.16.0.0/12：172.16.0.0～172.31.255.255
	 * 192.168.0.0/16：192.168.0.0～192.168.255.255
	 *
	 * @param ip
	 * @return
	 */
	public static boolean isInnerIP(long ip) {
		return (ip >> 24 == 0xa) || (ip >> 16 == 0xc0a8) || (ip >> 22 == 0x2b0);
	}

	public static boolean isInnerIP(InetAddress address){
		if(address == null){
			return false;
		}
		return isInnerIP(str2IPNumber(address.getHostAddress()));
	}

	/**
	 * 是否是外网地址
	 *
	 * @param address
	 * @return
	 * @author: shenxiaoping-549
	 * @date: 2017/6/26 10:38
	 */
	public static boolean isInternetAddress(InetAddress address) {
		if (address == null || address.isLoopbackAddress()) {
			return false;
		}
		String ip = address.getHostAddress();
		long ipNum = str2IPNumber(ip);
		if (ipNum < 0) {
			return false;
		}
		return !isInnerIP(ipNum);
	}

	private static InetAddress getLocalIP() {
		InetAddress localAddress = null;
		try {
			//为获取本地地址，则返回回环地址
			localAddress = InetAddress.getLocalHost();
			if (isValidAddress(localAddress)) {
				logger.info("get right local ip=>{}", localAddress.getHostAddress());
			} else {
				localAddress = getIP(false);
			}
		} catch (Throwable e) {
			logger.warn("fail to get ip address", e);
		}
		return localAddress;
	}

	private static InetAddress getIP(boolean isPublicIP){
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			if (interfaces != null) {
				while (interfaces.hasMoreElements()) {
					try {
						NetworkInterface network = interfaces.nextElement();
						Enumeration<InetAddress> addresses = network.getInetAddresses();
						if (addresses != null) {
							while (addresses.hasMoreElements()) {
								try {
									InetAddress address = addresses.nextElement();
									if(isValidAddress(address)){
										if (isPublicIP && isInternetAddress(address)) {
											return address;
										}
										else if(!isPublicIP && isInnerIP(address)){
											return  address;
										}
									}
								} catch (Throwable e) {
									logger.warn("fail to get ip address", e);
								}
							}
						}
					} catch (Throwable e) {
						logger.warn("fail to get ip address", e);
					}
				}
			}
		} catch (Throwable e) {
			logger.warn("fail to get ip address", e);
		}
		return null;
	}

	public static InetAddress getInternetIP() {
		InetAddress address = getIP(true);
		if(address == null){
			logger.warn("fail to get internet ip address");
		}
		return address;
	}

	public static Integer getRandPort(int basePort) {
		for (int i = basePort; i < MAX_PORT; i++) {
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(i);
				return i;
			} catch (Exception e) {
			} finally {
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (Exception e) {
						logger.error("fail to close server socket[{}]", serverSocket.getLocalPort(), e);
					}
				}
			}
		}
		return null;
	}

	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	public static void sleep(int time, TimeUnit unit) {
		try {
			Thread.sleep(unit.toMillis(time));
		} catch (InterruptedException e) {
		}
	}


	public static List<String> toSingleValues(Collection<String> values, String separator){
		if(values == null || values.isEmpty()){
			return Collections.EMPTY_LIST;
		}

		List<String> res = new ArrayList<>();
		Iterator<String> mv = values.iterator();
		while(mv.hasNext()){
			String[] vs = mv.next().split(separator);
			for (int i = 0; i < vs.length; i++) {
				res.add(vs[i]);
			}
		}
		return res;
	}


	public static boolean isExistClz(String classFullPath){
		try {
			Class.forName(classFullPath);
			return true;
		} catch (ClassNotFoundException e) {
			logger.console(e.getMessage(),e);
		}
		return false;
	}

}
