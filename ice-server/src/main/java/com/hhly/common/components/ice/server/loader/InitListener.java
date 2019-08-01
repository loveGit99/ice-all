package com.hhly.common.components.ice.server.loader;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.server.constants.ICEServerConst;
import com.hhly.common.components.ice.utils.DynamicLogUtils;

/**
 * ICE SERVER组件初始化
 * 
 * @author xuyuji-374
 * @date 2017年11月2日下午2:50:38
 */
@Component
public class InitListener implements ApplicationListener<ContextRefreshedEvent> {

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if ("false".equals(ICEServerConst.DYNAMIC_LOG_ENABLE)) {
			DynamicLogUtils.setEnabled(false);
		}
		DynamicLogUtils.setDynamicLogTime(ICEServerConst.DYNAMIC_LOG_TIME * 60 * 1000);
		DynamicLogUtils.setOriginLogLevel(SNSLOG.getLogLevel());
	}
}
