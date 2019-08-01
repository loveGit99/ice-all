/*
 * @Project Name: ice-all
 * @File Name: EndPointListenerProccor
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/5/6 18:57
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.util;

import com.hhly.base.util.zk.SubNodesChangedProcessorAdapter;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.hhly.base.util.SNSLOG;


import java.util.Map;
import java.util.Set;

/**
 * 子节点变化监听处理器
 *
 * @author shenxiaoping-549
 * @date 2017/5/6 18:57
 * @see
 */
public class EndPointListenerProcessor extends SubNodesChangedProcessorAdapter {

	private final static SNSLOG LOG = new SNSLOG(EndPointListenerProcessor.class);

	private final static Set<String> CUSTOM_ENDPOINTS =ProxyCache.getCustomEndpoint();

	@Override
	public void childAddCallback(CuratorFramework zkClient, String nodePath, PathChildrenCache cache,
			PathChildrenCacheEvent event, Map<Object, Object> params) {
		try {
			String path = event.getData().getPath();
			String[] fullPath = nodePath.split("/");
			String sys = fullPath[fullPath.length - 1];
			if(CUSTOM_ENDPOINTS.contains(sys)){
				LOG.info("[custom_watch] custom endpoints contain this system [{}], so ignore it",sys);
				return;
			}

			byte[] child = zkClient.getData().forPath(path);
			if(child == null){
				LOG.debug("the node[{}] value  is null",path);
				return;
			}
			long czxId = event.getData().getStat().getCzxid();
			String v = new String(child, "UTF-8");
			int cnt = 0;
			boolean isValid = false;
			while (!isValid && ++cnt < 15 ) {
				try {
					Thread.sleep(100);
					v = new String(zkClient.getData().forPath(path),"UTF-8");
					System.err.println("path===" + path + ", v====>"+v);
					isValid = ICEUtil.isValidICEEndpoint(v);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if(!isValid){
				LOG.error("it is not a validate endpoints==>[{}]", v);
				return;
			}

			boolean isAdded = ProxyCache.putEndpointsCache(sys, v,czxId);
			if(isAdded){
				//ICE PROXY  增加 MarkProxy
				ProxyCache.putICEProxyCacheBySys(sys,v,czxId);
				LOG.warn("load for sys[{}]---endpoint[{}]--czxId[{}]",sys,v,Long.toHexString(czxId));
			}
			else{
				LOG.warn("sys[{}]-endpoint[{}]--cxzId[{}] existed ", sys,v,Long.toHexString(czxId));
				return;
			}
		} catch (Exception e) {
			LOG.error("fail to get added child data", e);
		}
	}

	/**
	 * 删除endpoint节点
	 * @param zkClient
	 * @param nodePath
	 * @param cache
	 * @param event
	 * @param params
	 */
	@Override
	public void childRemoveCallback(CuratorFramework zkClient, String nodePath, PathChildrenCache cache,
			PathChildrenCacheEvent event, Map<Object, Object> params) {
		try {
			byte[] child = event.getData().getData();
			Long czxId = event.getData().getStat().getCzxid();
			String v = new String(child, "UTF-8");
			String[] fullPath = nodePath.split("/");
			String sys = fullPath[fullPath.length - 1];

			//
			ProxyCache.getICECommunicatorCache(sys,v);


			if(CUSTOM_ENDPOINTS.contains(sys)){
				LOG.info("[remove watch]custom endpoints contain this system[{}], so ignore it",sys);
				return;
			}
			LOG.info("\n\n\n\n ##xxxxxxxxx remove data ====>{}-{}-{}\n\n\n",sys,v,czxId);
			//先判断是否是已经存在的，如果不存在，就直接skip
			ProxyCache.removeEndpointsCache(sys,v,czxId);
			ProxyCache.removeDisconnICEProxy(sys,v,czxId);

			//下面关闭的代码逻辑上有问题，待改正  pending
			ProxyCache.closedConn(v);
		} catch (Exception e) {
			LOG.error("fail to remove child data", e);
		}
	}

	@Override
	public void childUpdateCallback(CuratorFramework zkClient, String nodePath, PathChildrenCache cache,
			PathChildrenCacheEvent event, Map<Object, Object> params) {
	}
}
