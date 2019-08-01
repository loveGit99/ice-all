/*
 * @Project Name: ice-all
 * @File Name: ICESystemListenerProcessor
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/7/28 9:21
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.util;

import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.base.util.zk.SubNodesChangedProcessorAdapter;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.hhly.base.util.SNSLOG;


import java.util.Map;

/**
 * ICE Service system listener
 *
 * @author shenxiaoping-549
 * @date 2017/7/28 9:21
 * @see
 */
public class ICESystemListenerProcessor extends SubNodesChangedProcessorAdapter {

	private final static SNSLOG LOG = new SNSLOG(ICESystemListenerProcessor.class);

	@Override
	public void childAddCallback(CuratorFramework zkClient, String nodePath, PathChildrenCache cache,
			PathChildrenCacheEvent event, Map<Object, Object> params) {
		String path = event.getData().getPath();
		String[] fullPath = path.split("/");
		String sys = fullPath[fullPath.length - 1];
		LOG.debug("listen system, sys==>{}",sys);
		if(!ProxyCache.getICESystemCache().contains(sys)){
			SNSZKUtil snszkUtil = SNSZKUtil.getInstance();
			try {
				snszkUtil.registerListener4ChildNodesChanged(path,new EndPointListenerProcessor(),null);
				ProxyCache.putICESystemCache(sys);
			} catch (Exception e) {
				LOG.error("fail register sys[{}] watch", sys);
			}
		}
	}

}
