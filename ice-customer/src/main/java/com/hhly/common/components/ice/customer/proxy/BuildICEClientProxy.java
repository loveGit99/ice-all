/*
 * @Project Name: cmp-ice
 * @File Name: BuildICEClientProxy.java
 * @Package Name: com.hhly.common.components.ice.customer.proxy
 * @Date: 2016年11月28日下午12:17:47
 * @Creator: shenxiaoping
 * @line------------------------------
 * @修改人:
 * @修改时间:
 * @修改内容:
 */

package com.hhly.common.components.ice.customer.proxy;

import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.customer.annotation.ICEProvider;
import com.hhly.common.components.ice.customer.util.DiscoveryICEServicesUtil;
import com.hhly.common.components.ice.customer.util.ICEUtil;
import com.hhly.common.components.ice.customer.util.LoadICEEndpointsUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * ICE client proxy
 *
 * @author Allen Shen
 * @date 2016年11月28日下午12:17:47
 * @see
 */
@Component
public class BuildICEClientProxy implements BeanPostProcessor {

	private final static SNSLOG LOG = new SNSLOG(BuildICEClientProxy.class);
	@Autowired
	private ClientHelper helper;
	@Autowired
	private LoadICEEndpointsUtil propertyUtil;
	@Autowired
	private DiscoveryICEServicesUtil discovery;

	@Autowired
	private ICEProxyFactory proxyFactory;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		propertyUtil.loadIceConfig();
		discovery.initEndpoints();
		buildProxy(bean);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * @param bean
	 * @description build proxy object
	 * @date 2016年11月28日下午4:48:46
	 * @author shenxiaoping
	 * @since 1.0.0
	 */
	private void buildProxy(Object bean) {
		Field[] fields = ICEUtil.getAllFields(bean);
		if (fields == null || fields.length == 0) {
			return;
		}
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			final ICEProvider iceProvider = field.getAnnotation(ICEProvider.class);
			if (iceProvider != null) {
				field.setAccessible(true);
				Class<?> serviceType = field.getType();
				try {
					Map<String, Object> d = new HashMap<>();
					d.put("service", serviceType);
					d.put("iceProvider", iceProvider);
					ProxyCache.putICEOriginalClz(serviceType.getName(), d);
					field.set(bean, proxyFactory.getProxy(serviceType, iceProvider));
					LOG.info("%%% inject proxy into {}---{}%%", bean.getClass().getName(), field.getName());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					LOG.error("fail to set field with proxy object[{}], SYSTEM EXIT NOW!",
							helper.getServiceName(serviceType, iceProvider), e);
					System.exit(1);
				}
			}
		}
	}



}
