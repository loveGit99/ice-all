/*
 * @Project Name: sns-web-utils
 * @File Name: ConfigLoader
 * @Package Name: com.hhly.base.util
 * @Date: 2017/1/4 14:50
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.hhly.base.util.SNSLOG;

/**
 * 通用配置文件加载器,不依赖spring 容器
 *
 * @author shenxiaoping-549
 * @date 2017/1/4 14:50
 * @see
 */
public class ConfigLoader {

	private final static SNSLOG LOG = new SNSLOG(ConfigLoader.class);
	private final static ConfigLoader instance = new ConfigLoader();

	private ConfigLoader() {
	}

	public static ConfigLoader getInstance() {
		return instance;
	}

	public Properties load(String configPath) {
		InputStream inputStreamConfig = null;
		try {
			inputStreamConfig = getClass().getClassLoader().getResourceAsStream(configPath);
			Properties configProperties = new Properties();
			configProperties.load(inputStreamConfig);
			return configProperties;
		} catch (Exception e) {
			LOG.error("fail to load config properties file==>{}", configPath, e);
		} finally {
			if (isNotNull(inputStreamConfig)) {
				try {
					inputStreamConfig.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static boolean isNull(Object... objects) {
		boolean isNull = true;
		for (Object o : objects) {
			if (o != null) {
				isNull = false;
				break;
			}
		}
		return isNull;
	}

	public static boolean isNotNull(Object... objects) {
		return !isNull(objects);
	}

	/**
	 * <p>
	 * 根据key获取对应的value
	 * </p>
	 */
	public static String getString(Properties configProperties, String key) {
		String tmp = configProperties.getProperty(key);
		try {
			if (tmp == null) {
				LOG.error("###error####  can not find constant--key=" + key);
				return null;
			} else {
				tmp = tmp.trim();
			}
			tmp = new String(tmp.getBytes("iso-8859-1"), "utf8");
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return tmp;
	}

	/**
	 * @param configProperties
	 * @param keys
	 * @param joinSybmol
	 * @return
	 * @description: 获取多个key的值，并链接起来
	 */
	public static String getStrs(Properties configProperties, String[] keys, String joinSybmol) {
		StringBuilder sb = new StringBuilder();
		if (keys == null || keys.length == 0) {
			return null;
		}
		int len = keys.length;
		int index = len - 1;
		for (int i = 0; i < len; i++) {
			String res = getString(configProperties, keys[i]);
			if (res == null) {
				return null;
			}
			sb.append(res);
			if (i < index) {
				sb.append(joinSybmol);
			}
		}
		return sb.toString();
	}

	public static String getStrs(Properties pro, String[] keys, String joinSymbol, String defaultValue) {
		String res = getStrs(pro, keys, joinSymbol);
		if (StringUtils.isBlank(res)) {
			res = defaultValue;
		}
		return res;
	}

	public static String getString(Properties configProperties, String key, String defaultValue) {
		String tmp = configProperties.getProperty(key);
		try {
			if (tmp == null) {
				LOG.warn("###error####  can not find constant--key=" + key);
				return defaultValue;
			} else {
				tmp = tmp.trim();
			}
			tmp = new String(tmp.getBytes("iso-8859-1"), "utf8");
		} catch (Exception e) {
			LOG.warn(e.getMessage(), e);
		}
		return tmp;
	}

	public static Integer getInt(Properties configProperties, String key) {
		return Integer.valueOf(getString(configProperties, key));
	}

	public static int getInt(Properties configProperties, String key, int defaultValue) {
		String str = getString(configProperties, key);
		try {
			return Integer.valueOf(str);
		} catch (NumberFormatException e) {
			LOG.warn(e.getMessage());
			return defaultValue;
		}
	}

	public static Integer getInteger(Properties configProperties, String key, Integer defaultValue) {
		String str = getString(configProperties, key);
		try {
			return Integer.valueOf(str);
		} catch (NumberFormatException e) {
			LOG.warn("key-value={}-{} is not a integer type", key, str);
			return defaultValue;
		}
	}
}
