/*
 * Project Name: cmp-ice File Name: ICEUtil.java Package Name:
 * com.hhly.common.components.ice.util Date: 2016年11月26日下午5:06:04 Creator:
 * shenxiaoping ------------------------------ 修改人: 修改时间: 修改内容:
 */

package com.hhly.common.components.ice.customer.util;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.SNSLOG;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.base.util.zk.SubNodesChangedProcessorAdapter;
import com.hhly.common.components.ice.customer.annotation.ICEProvider;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * ICE 工具类
 *
 * @author Allen Shen
 * @date 2016年11月26日下午5:06:04
 * @see
 */
public class ICEUtil {

	private final static SNSLOG LOG = new SNSLOG(ICEUtil.class);
	private final static String IP_REG = "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";
	private final static Pattern ICE_PATTERN = Pattern.compile("tcp\\s+-h\\s+" + IP_REG + "\\s+-p\\s+\\d+");

	public static String getICEServiceName(String serviceName, Class<?> clz) {
		if (serviceName.length() == 0) {
			String orgName = clz.getSimpleName();
			return Character.toLowerCase(orgName.charAt(0)) + orgName.substring(1);
		}
		return serviceName;
	}

	public static Method getMethod(Object obj, String methodName, Class<?>[] types) {
		try {
			return obj.getClass().getMethod(methodName, types);
		} catch (NoSuchMethodException | SecurityException e) {
			LOG.error("fail to get method instance--clz:[{}]==method:[{}]==params:[{}]",
					obj.getClass().getCanonicalName(), methodName, JSONObject.toJSONString(types));
		}
		return null;
	}

	/**
	 * @param endpoints
	 * @return
	 * @description 分散压力, 重新排序
	 * @date 2016年12月1日下午5:47:27
	 * @author shenxiaoping
	 * @since 1.0.0
	 */
	public static String[] getOptimalEndpoints(List<String> endpoints) {
		if (endpoints.size() < 2) {
			return new String[] { endpoints.get(0) };
		} else {
			return getSortedEndpoints(endpoints);
		}
	}

	private static String[] getSortedEndpoints(List<String> endpoints) {
		int size = endpoints.size();
		String[] sortEndpoints = new String[size];
		for (int i = 0; i < size; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = i; j < i + size; j++) {
				sb.append(":").append(endpoints.get(j % size));
			}
			if (sb.length() > 0) {
				sortEndpoints[i] = sb.deleteCharAt(0).toString();
			}
		}
		return sortEndpoints;
	}

	/**
	 * @param bean
	 * @return
	 * @description 获取该类所有的属性，包括所有父类的属性
	 * @date 2017年2月16日下午3:07:24
	 * @author shengxiaping-549
	 * @since 1.0.0
	 */
	public static Field[] getAllFields(Object bean) {
		List<Field> list = new ArrayList<>();
		Class<?> clz = bean.getClass();
		while (clz != Object.class) {
			Field[] fields = clz.getDeclaredFields();
			if (fields != null && fields.length > 0) {
				list.addAll(Arrays.asList(fields));
			}
			clz = clz.getSuperclass();
		}
		return list.toArray(new Field[list.size()]);
	}

	public static boolean isValidICEEndpoint(String endpoint) {
		return ICE_PATTERN.matcher(endpoint).matches();
	}

	public static void registerEndPoint(SNSZKUtil snszkUtil, String endpointsNode, String iceSys,
			SubNodesChangedProcessorAdapter processor) throws Exception {
		Set<String> AllSys = ProxyCache.getICESystemCache();
		if (!AllSys.contains(iceSys)) {
			ProxyCache.putICESystemCache(iceSys);
			snszkUtil.registerListener4ChildNodesChanged(endpointsNode, processor, null);
		}
	}

	public static ICEProvider instance(final String iceServiceName, final String sysName) {
		return new ICEProvider() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return ICEProvider.class;
			}

			/**
			 * 系统默认ice endpoints 通配
			 * 当配置文件缺失endpoints单一配置之后 将使用此属性构造出
			 * ice.provider.endpoints.${endpointsSystem}.default
			 * 作为key 重新获取endpoints
			 * 作用于 减少一个系统相同的值只是不同的key需要配置多条数据的问题
			 *
			 * @return
			 * @date 2016-12-10下午3:44:54
			 * @author bb.h
			 * @since 1.0.0
			 */
			@Override
			public String system() {
				return sysName;
			}

			/**
			 * 服务名称
			 *
			 * @return
			 * @date 2016年11月28日下午6:58:29
			 * @author shenxiaoping
			 * @since 1.0.0
			 */
			@Override
			public String name() {
				return iceServiceName;
			}

			/**
			 * ice针对每个服务生成的proxyHelper类, 默认可以不指定
			 *
			 * @return
			 * @date 2016年11月28日下午6:57:48
			 * @author shenxiaoping
			 * @since 1.0.0
			 */
			@Override
			public Class<?> proxy() {
				return null;
			}
		};
	}


}
