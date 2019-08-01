/*
 * @Project Name: cmp-ice
 * @File Name: PropertiesUtil.java
 * @Package Name: com.hhly.common.components.ice.util
 * @Date: 2016年11月28日下午4:11:20
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人:
 * @修改时间:
 * @修改内容:
 */

package com.hhly.common.components.ice.customer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.stereotype.Component;

import com.hhly.base.util.ApplicationPropertiesLoader;
import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.constants.ICECommonConstant;
import com.hhly.common.components.ice.customer.annotation.ICEProvider;
import com.hhly.common.components.ice.customer.constants.ICEConstants;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import com.hhly.common.components.ice.utils.DynamicLogUtils;

/**
 * 仅仅加载ICE客户端配置到内存中
 *
 * @author Allen Shen
 * @date 2016年11月28日下午4:11:20
 * @see
 */
@Component
public class LoadICEEndpointsUtil {
	private final static SNSLOG LOG = new SNSLOG(LoadICEEndpointsUtil.class);
	private final static String ICE_CONFIG_PATH = "env/ice-registry.properties;env/ice-client.properties";
	private static Properties propMap = null;
	private static Map<String, String> CUSTOMER_SERVICE_ENDPOINTS = Collections.EMPTY_MAP;

	public void loadIceConfig() {
		if (propMap != null) {
			return;
		}
		try {
			initIceProperties();
			getCustomerEndpoints();
			initLogUtils();
		} catch (IOException e) {
			LOG.error("fail to load ice properties from application.properties or {}", ICE_CONFIG_PATH);
			System.exit(1);
		}
		LOG.info("======ice configuration detail:\n\n\n{}\n\n\n", propMap.toString());

	}

	private void initIceProperties() throws IOException {
		propMap = new Properties();
		LOG.console("\n\n\n load spring boot properties file\n\n\n");
		Properties ps = ApplicationPropertiesLoader
				.getApplicationProperties4ICE(ICECommonConstant.ICE_CLIENT_SBOOT_PREFIX);
		LOG.console("\n\n\n ***************load spring boot properties file End !**************\n\n\n");
		if (ps != null) {
			Properties localPs = ApplicationPropertiesLoader
					.getApplicationProperties(ICECommonConstant.ICE_CLIENT_SBOOT_LOCAL_PREFIX);
			if (localPs != null) {
				ps.putAll(localPs);
			}
			propMap = ps;
		} else {
			LOG.console("============spring boot properties is null==================");
			initIcePropertiesFromFile();
		}
	}

	private void initIcePropertiesFromFile() throws IOException {
		String[] resources = ICE_CONFIG_PATH.split(";");
		InputStream read = null;
		for (int i = 0; i < resources.length; i++) {
			String res = resources[i];
			try {
				read = getClass().getClassLoader().getResourceAsStream(res);
				if (read == null) {
					LOG.error("fail to find the ice configuration file[  {}  ], please check.", res);
					System.exit(1);
				}
				propMap.load(read);
			} finally {
				if (read != null) {
					try {
						read.close();
					} catch (IOException e) {
						LOG.warn("fail to close inputstream", e);
					}
				}
			}
		}
		LOG.info(".............loaded {} successfully..............!", ICE_CONFIG_PATH);
	}

	/**
	 * @param key
	 *            属性key值
	 * @param provider
	 *            注解器
	 * @return
	 * @description 用于适配endpoints 来支持 针对同一个系统在没有自定义的情况下可以使用通配系统endpoints来构建每个代理
	 *              （前提是注解中指定了endpointsSystem的值）
	 * @date 2016-12-10下午3:22:44
	 * @author bb.h
	 * @since 1.0.0
	 */
	public static String getStr(String key, ICEProvider provider) {
		String endpoints = (String) propMap.get(key);
		if (endpoints == null) {
			if (provider.system().length() > 0) {
				String defaultKey = ICEConstants.ICE_PROVIDER_ENDPOINTS_PREFIX + "." + provider.system() + ".default";
				LOG.info("Attempt to automatically [" + key + "] configure default system properties  new is ["
						+ defaultKey + "]");
				endpoints = (String) propMap.get(defaultKey);
			}
		}
		return endpoints;
	}

	/**
	 * 获取配置的endpoint
	 * 
	 * @author: shenxiaoping-549
	 * @date: 2017/7/13 15:05
	 * @return
	 */
	public static void getCustomerEndpoints() {
		Map<String, String> res = new HashMap<>();
		Iterator<Map.Entry<Object, Object>> it = propMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> me = it.next();
			String key = me.getKey().toString();
			if (key.startsWith(ICEConstants.ICE_PROVIDER_ENDPOINTS_PREFIX + ".")) {
				key = key.substring(ICEConstants.ICE_PROVIDER_ENDPOINTS_PREFIX.length() + 1).replace(".default", "");
				String value = me.getValue().toString();
				res.put(key, value);
				LOG.info("[custom] put custom endpoints==>{}-{} into cache", key, value);
				ProxyCache.putCustomEndpoint(key);
			}
		}
		CUSTOMER_SERVICE_ENDPOINTS = res;
	}

	public static void initLogUtils() {
		if ("false".equals(getStr(ICEConstants.DYNAMIC_LOG_ENABLE))) {
			DynamicLogUtils.setEnabled(false);
		}
		DynamicLogUtils.setDynamicLogTime(getInt(ICEConstants.DYNAMIC_LOG_TIME, 10) * 60 * 1000);
		DynamicLogUtils.setOriginLogLevel(SNSLOG.getLogLevel());
	}

	public static String getStr(String key) {
		return (String) propMap.get(key);
	}

	public static String getStr(String key, String defaultStr) {
		Object res = propMap.get(key);
		return res != null ? (String) res : defaultStr;
	}

	public static Integer getInt(String key) {
		return getInt(key, null);
	}

	public static Integer getInt(String key, Integer def) {
		String value = getStr(key);
		return value != null ? Integer.valueOf(getStr(key)) : def;
	}

	public static Map<String, String> getCustomerServiceEndpoints() {
		return CUSTOMER_SERVICE_ENDPOINTS;
	}
}
