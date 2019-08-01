/*
 * @Project Name: ice-all
 * @File Name: ICEBootstrap
 * @Package Name: com.hhly.common.components.ice.server.util
 * @Date: 2017/4/19 11:56
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人:
 * @修改时间:
 * @修改内容:
 */

package com.hhly.common.components.ice.server.bootstrap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.server.loader.ServerHelper;
import com.hhly.common.components.ice.utils.ServerCache;

/**
 * 启动bootstrap
 *
 * @author shenxiaoping-549
 * @date 2017/4/19 11:56
 * @see
 */
public abstract class ICEBootstrapAbstract {

	private final static SNSLOG LOG = new SNSLOG(ICEBootstrapAbstract.class);

	/*
	 * @author: shenxiaoping-549
	 * 
	 * @date: 2017/4/19 14:12 启动服务，并启动Adapter， 默认的spring 入口文件是
	 * xml/applicationContext.xml
	 */
	public ApplicationContext run(String systemName) {
		return run("xml/applicationContext.xml", systemName);
	}

	public void start(ApplicationContext ctx, String systemName) {
		ServerCache.setSpringCtx(ctx);
		// 启动adapters
		ServerHelper helper = ctx.getBean("serverHelper", ServerHelper.class);
		helper.startAdapter();
		LOG.info("====================has start {} server===================", systemName);
	}

	/*
	 * @author: shenxiaoping-549
	 * 
	 * @date: 2017/4/19 14:12 启动服务，并启动Adapter
	 */
	public ApplicationContext run(String configuration, String systemName) {
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configuration);
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						callback(context);
					} catch (Exception e) {
						LOG.error("fail to run callback method", e);
					}
				}
			}));
			context.start();
			// cache spring context
			ServerCache.setSpringCtx(context);
			// 启动adapter
			ServerHelper helper = context.getBean("serverHelper", ServerHelper.class);
			helper.startAdapter();
			LOG.info("====================has start {} server===================", systemName);
			return context;
		} catch (Exception e) {
			LOG.error("fail to start {} server", systemName, e);
			System.exit(1);
		}
		return null;
	}

	/*
	 * @author: shenxiaoping-549
	 * 
	 * @date: 2017/4/19 14:20 自定义回调钩子，在子类中实现
	 */
	public abstract void callback(ClassPathXmlApplicationContext context);

}
