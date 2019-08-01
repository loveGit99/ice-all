/*
 * @Project Name: ice-all
 * @File Name: DiscoveryICEServicesUtil
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/5/6 14:56
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.util;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.base.util.zk.ZkNode;
import com.hhly.common.components.ice.customer.constants.ICEConstants;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import com.hhly.common.components.ice.utils.ICECommonUtil;
import com.hhly.base.util.SNSLOG;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 动态发现ICE服务
 *
 * @author shenxiaoping-549
 * @date 2017/5/6 14:56
 * @see
 */
@Component
public class DiscoveryICEServicesUtil {

	private final static SNSLOG LOG = new SNSLOG(DiscoveryICEServicesUtil.class);
	private String iceEndpointsNode;
	private String releaseVersion;
	// 加载标识， 0 ： 未加载， 1： 加载中   2：加载成功　-1: 加载失败
	private volatile int isLoadedFlag = 0;

	public void initEndpoints() {
		if (isInitialized()) {
			return;
		}
		isLoadedFlag = 1;
		String iceRootNode = getMixIceEndpoints();
		SNSZKUtil snszkUtil = SNSZKUtil.getInstance();
		loadCustomEndpoint();
		loadZkEndpoint(snszkUtil, iceRootNode);
		listenICESystem(snszkUtil, iceRootNode);
		//注册回调，防止注册节点失效
		registerNotifier(snszkUtil, iceRootNode);
	}

	/**
	 * 加载本地指定的ip和端口的ice服务
	 */
	private void loadCustomEndpoint() {
		Map<String, String> customerServiceEndpoints = LoadICEEndpointsUtil.getCustomerServiceEndpoints();
		Iterator<Map.Entry<String, String>> it = customerServiceEndpoints.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> me = it.next();
			String sys = me.getKey();
			String endpoint = me.getValue();
			ProxyCache.putICESystemCache(sys);
			ProxyCache.putCustomEndpointsCache(sys, endpoint);
			LOG.info("\n\n\n[load custom] load  system --custom endpoints ==>[{}----{}] successfully!", sys, endpoint);
		}
	}

	private void loadZkEndpoint(SNSZKUtil snszkUtil, String iceRootNode) {
		new Thread(new LoadZKICEEndpointsThread(snszkUtil, iceRootNode)).start();
	}

	class LoadZKICEEndpointsThread implements Runnable {
		private SNSZKUtil snszkUtil;
		private String iceRootNode;
		public LoadZKICEEndpointsThread(SNSZKUtil snszkUtil, String iceRootNode) {
			this.iceRootNode = iceRootNode;
			this.snszkUtil = snszkUtil;
		}
		@Override
		public void run() {
			int cnt = 0;
			Set<String> localCustomerEndpoints = ProxyCache.getCustomEndpoint();
			while (true) {
				boolean isLoaded = false;
				cnt++;
				try {
					snszkUtil.setDataForcibly(iceRootNode, "");
					List<ZkNode> sysNodes = snszkUtil.getChildrenDataWithoutNodes(iceRootNode, localCustomerEndpoints);
					if(cnt < 20) {
						LOG.info("[from_zk] ==>[{}]", JSONObject.toJSONString(sysNodes));
					}
					if (sysNodes == null || sysNodes.isEmpty()) {
						if (cnt < 20) {
							LOG.info("get none iceNode from zk--{}, try again!", iceRootNode);
							ICECommonUtil.sleep(3, TimeUnit.SECONDS);
						}
						continue;
					}
					for (int i = 0; i < sysNodes.size(); i++) {
						String systemName = sysNodes.get(i).getNodeId();
						String endpointsNode = iceRootNode + '/' + systemName;
						//register node
						ICEUtil.registerEndPoint(snszkUtil,endpointsNode,systemName,new EndPointListenerProcessor());
						List<ZkNode> endpoints = snszkUtil.getChildrenData(endpointsNode);
						if (endpoints == null || endpoints.isEmpty()) {
							isLoaded = false;
						} else {
							isLoaded = true;
							ProxyCache.putICESystemCache(systemName);
							ProxyCache.putEndpointsCache(systemName, endpoints);
							LOG.info("\n\n\nload system {} endpoints ==> [{}] successfully!", systemName, endpoints);
						}
					}
					if (isLoaded) {
						isLoadedFlag = 2;
						LOG.info("load ice server endpoints successfully, after retry {} times", cnt);
						break;
					}
				} catch (Exception e) {
					isLoadedFlag = -1;
					if (cnt < 2) {
						LOG.info("fail to get ice service endpoints from zk, retry {} times", (cnt - 1), e);
					}
				}
				ICECommonUtil.sleep(3, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * listen the ice service system
	 */
	private void listenICESystem(final SNSZKUtil snszkUtil, final String iceRootNode) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int cnt = 0;
				boolean isRegister = false;
				while (cnt < 200) {
					try {
						snszkUtil.registerListener4ChildNodesChanged(iceRootNode, new ICESystemListenerProcessor(), null);
						isRegister = true;
						break;
					} catch (Exception e) {
						cnt++;
						if (cnt < 30) {
							LOG.error("fail to register ice system root node [{}], try again ", iceRootNode, e);
						}
						ICECommonUtil.sleep(3,TimeUnit.MINUTES);
					}
				}
				if(isRegister){
					LOG.info("listening ice system node!");
				}
				else{
					LOG.error("fail to register ice system root node");
				}
			}
		}).start();
	}

	private void registerNotifier(SNSZKUtil snszkUtil, String iceRootNode) {
		Map<String, Object> notifyMap = new HashMap<>(4);
		notifyMap.put("nodePath", iceRootNode);
		snszkUtil.addNotifier(new CheckEndpoints(notifyMap));
	}

	private String getMixIceEndpoints() {
		iceEndpointsNode = LoadICEEndpointsUtil.getStr("ice.endpoints.node", "/ice-services-endpoints");
		releaseVersion = LoadICEEndpointsUtil.getStr("service.release.version");
		if (releaseVersion == null) {
			LOG.error("not setting service.release.version , please check it in ice-registry.properties");
			System.exit(1);
		}
		String iceRoot = iceEndpointsNode + '-' + releaseVersion;
		ICEConstants.ICE_ROOT_PATH = iceRoot;
		return iceRoot;
	}

	/**
	 * 是否完成初始化
	 *
	 * @return
	 * @author: shenxiaoping-549
	 * @date: 2017/5/15 10:40
	 */
	private boolean isInitialized() {
		return isLoadedFlag == 2 || isLoadedFlag == 1;
	}
}
