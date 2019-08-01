/*
 * @Project Name: ice-all
 * @File Name: ICEBootstrapAdapter
 * @Package Name: com.hhly.common.components.ice.server.bootstrap
 * @Date: 2017/4/19 14:26
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.bootstrap;

import com.hhly.base.util.SNSLOG;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * ICEBootstrap adapter
 *
 * @author shenxiaoping-549
 * @date 2017/4/19 14:26
 * @see
 */
public class ICEBootstrapAdapter extends ICEBootstrapAbstract {

	private final static SNSLOG LOG = new SNSLOG(ICEBootstrapAdapter.class);

	private final static  ICEBootstrapAdapter instance = new ICEBootstrapAdapter();

	private ICEBootstrapAdapter (){}

	public static ICEBootstrapAdapter getInstance(){
		return instance;
	}

	@Override
	public void callback(ClassPathXmlApplicationContext context) {
		context.stop();
		context.destroy();
		LOG.warn("has destroied spring application context resources");
	}


}
