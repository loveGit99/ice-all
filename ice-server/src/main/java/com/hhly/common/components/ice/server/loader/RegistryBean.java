/*
 * @Project Name: ice-all
 * @File Name: RegistryBean
 * @Package Name: com.hhly.common.components.ice.server.loader
 * @Date: 2017/3/28 14:06
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.loader;

import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.server.annotation.ICEServer;
import com.hhly.common.components.ice.server.proxy.JdkIceServerProxy;
import com.hhly.common.components.ice.server.util.ICEUtil;
import com.hhly.common.components.redis.RedisTemplateDriver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * register ice server into adapter
 *
 * @author shenxiaoping-549
 * @date 2017/3/28 14:06
 * @see
 */
@Component
public class RegistryBean implements BeanPostProcessor {

	private final static SNSLOG LOG = new SNSLOG(RegistryBean.class);
	@Autowired
	private ServerHelper serverHelper;

	@Autowired
	private RedisTemplateDriver redis;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clz = bean.getClass();
		ICEServer iceServer = clz.getAnnotation(ICEServer.class);
		if (iceServer != null) {
			String servicesKey = ICEUtil.getICEServiceName(iceServer.name(), clz);
			Object proxy = new JdkIceServerProxy(redis).getProxy(bean);
			serverHelper.registryServer(servicesKey, proxy);
			LOG.warn("############## has published ICE service---{}------  #################",servicesKey);
		}
		return bean;
	}

//	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
