/*
 * @Project Name: ice-commons
 * @File Name: LogUtils.java
 * @Package Name: com.hhly.common.components.ice.utils
 * @Date: 2017年10月25日下午4:56:20
 * @Creator: xuyuji-374
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.hhly.base.util.SNSLOG;

import ch.qos.logback.classic.Level;

/**
 * 动态日志工具类
 * 
 * @author xuyuji-374
 * @date 2017年10月25日下午4:56:20
 * @see
 */
public class DynamicLogUtils {

	private static boolean isEnabled = true; // 是否开启动态日志
	private static AtomicBoolean upLogLevel = new AtomicBoolean(false); // 是否提升了日志级别
	private static AtomicLong errorTime = new AtomicLong(); // 错误发生时间点
	private static Level originLogLevel = Level.INFO; // 原始日志级别
	private static long DYNAMIC_LOG_TIME = 10 * 60 * 1000; // 动态日志持续时间(单位：毫秒)

	private DynamicLogUtils() {
	}

	public static void setOriginLogLevel(Level level) {
		originLogLevel = level;
	}

	public static void setDynamicLogTime(long time) {
		DYNAMIC_LOG_TIME = time;
	}

	public static void setEnabled(boolean isEnabled) {
		DynamicLogUtils.isEnabled = isEnabled;
	}

	/**
	 * 在捕获异常时调用，提升日志级别到DEBUG。
	 * 
	 * @date 2017年11月2日下午2:48:25
	 * @author xuyuji-374
	 * @since 1.0.0
	 */
	public static synchronized void errorLog() {
		if (isEnabled) {
			upLogLevel.set(true);
			errorTime.set(System.currentTimeMillis());
			SNSLOG.setLogLevel(Level.DEBUG);
		}
	}

	/**
	 * 无异常时调用，如果动态日志持续时间内无异常发生还原日志级别。
	 * 
	 * @date 2017年11月2日下午2:49:19
	 * @author xuyuji-374
	 * @since 1.0.0
	 */
	public static synchronized void normalLog() {
		if (isEnabled) {
			if (upLogLevel.get() && (System.currentTimeMillis() - errorTime.get() > DYNAMIC_LOG_TIME)) {
				upLogLevel.set(false);
				SNSLOG.setLogLevel(originLogLevel);
			}
		}
	}
}
