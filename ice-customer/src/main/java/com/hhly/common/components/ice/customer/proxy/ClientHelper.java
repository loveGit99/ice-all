/*
 * Project Name: cmp-ice File Name: ClientHelper.java Package Name:
 * com.hhly.common.components.ice.customer.proxy Date: 2016年11月28日下午5:15:43
 * Creator: shenxiaoping ------------------------------ 修改人: 修改时间: 修改内容:
 */

package com.hhly.common.components.ice.customer.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.hhly.base.util.SNSLOG;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.base.util.zk.ZkNode;
import com.hhly.common.components.ice.customer.annotation.ICEProvider;
import com.hhly.common.components.ice.customer.constants.ICEConstants;
import com.hhly.common.components.ice.customer.domain.ICECommunicator;
import com.hhly.common.components.ice.customer.domain.ICEEndpoint;
import com.hhly.common.components.ice.customer.domain.ICEProxy;
import com.hhly.common.components.ice.customer.domain.MarkProxy;
import com.hhly.common.components.ice.customer.util.EndPointListenerProcessor;
import com.hhly.common.components.ice.customer.util.LoadICEEndpointsUtil;

import Ice.Communicator;
import Ice.InitializationData;
import Ice.ObjectPrx;
import Ice.Util;

/**
 * ice helper
 *
 * @author Allen Shen
 * @date 2016年11月28日下午5:15:43
 * @see
 */
@Component
public class ClientHelper {

	private final static SNSLOG LOG = new SNSLOG(ClientHelper.class);

	public Communicator getCommunicator(String serviceName, String endpoints) {
		// crate a new communicator & cache it.
		InitializationData initData = new InitializationData();
		initData.properties = Util.createProperties();
		StringBuilder setting = new StringBuilder();
		setting.append(serviceName).append(":").append(endpoints);
		initData.properties.setProperty(serviceName + ".Proxy", setting.toString());
		initData.properties.setProperty("Ice.ThreadPool.Client.Size",
				LoadICEEndpointsUtil.getStr(ICEConstants.ICE_PROVIDER_THREAD_CORE, ICEConstants.ICE_THREAD_CORE));
		initData.properties.setProperty("Ice.ThreadPool.Client.SizeMax",
				LoadICEEndpointsUtil.getStr(ICEConstants.ICE_PROVIDER_THREAD_MAX, ICEConstants.ICE_THREAD_MAX));
		initData.properties.setProperty("Ice.MessageSizeMax",
				LoadICEEndpointsUtil.getStr(ICEConstants.ICE_PROVIDER_MSG_MAX_SIZE, ICEConstants.ICE_MSG_MAX_SIZE));
		Communicator cm = Ice.Util.initialize(initData);
		return cm;
	}

	/**
	 * get endpoint by system and serviceName
	 * 
	 * @param serviceName
	 * @param iceSys
	 * @return
	 */
	private List<ICEEndpoint> getEndpointList(String serviceName, String iceSys, List<ICEEndpoint> endpointList) {
		if (endpointList != null && endpointList.size() > 0) {
			return endpointList;
		}
		List<ICEEndpoint> endpoints = null;
		try {
			if (ICEConstants.ICE_ROOT_PATH == null) {
				String iceEndpointsNode = LoadICEEndpointsUtil.getStr("ice.endpoints.node");
				String releaseVersion = LoadICEEndpointsUtil.getStr("service.release.version");
				if (iceEndpointsNode == null || releaseVersion == null) {
					LOG.error("fail to get ice.endpoints.node , service.release.version setting,please check! ");
					return null;
				}
				ICEConstants.ICE_ROOT_PATH = iceEndpointsNode + '-' + releaseVersion;
			}
			String endpointsNode = ICEConstants.ICE_ROOT_PATH + '/' + iceSys;
			SNSZKUtil snszkUtil = SNSZKUtil.getInstance();

			Set<String> AllSys = ProxyCache.getICESystemCache();
			if (!AllSys.contains(iceSys)) {
				ProxyCache.putICESystemCache(iceSys);
				snszkUtil.registerListener4ChildNodesChanged(endpointsNode, new EndPointListenerProcessor(), null);
			}
			List<ZkNode> zkNodeValues = snszkUtil.getChildrenData(endpointsNode);
			endpoints = ProxyCache.cacheEndpoints(iceSys, zkNodeValues);
			LOG.info("\n\n\n getEndpointList() load system {} service {} endpoints ==> [{}] successfully!", iceSys,
					serviceName, endpoints);
		} catch (Exception e) {
			LOG.error("fail to get endpoint from {}-{}", iceSys, serviceName, e);
		}
		return endpoints;
	}

	public List<ICECommunicator> doGetCommunicators(String serviceName, String iceSys, List<ICEEndpoint> endpointList) {
		List<ICECommunicator> cms = new ArrayList<>();
		endpointList = getEndpointList(serviceName, iceSys, endpointList);
		if (endpointList == null || endpointList.isEmpty()) {
			LOG.error("none endpoints for ice system [{}]", iceSys);
			return null;
		}
		for (int i = 0; i < endpointList.size(); i++) {
			ICEEndpoint iceEndpoint = endpointList.get(i);
			String endpointStr = iceEndpoint.getEndpoint();
			Communicator cm = ProxyCache.getICECommunicatorCache(endpointStr, serviceName);
			if (cm != null && cm.isShutdown()) {
				try {
					cm.destroy();
					cm = null;
					LOG.warn("destroy communicator==>{}-{}", endpointStr, serviceName);
				} catch (Exception e) {
					LOG.warn("fail to close communicator==>{}-{}", endpointStr, serviceName);
				}
			}
			if (cm == null || cm.isShutdown()) {
				InitializationData initData = new InitializationData();
				initData.properties = Util.createProperties();
				StringBuilder setting = new StringBuilder();
				setting.append(serviceName).append(":").append(endpointStr);
				initData.properties.setProperty(serviceName + ".Proxy", setting.toString());
				initData.properties.setProperty("Ice.ThreadPool.Client.Size", LoadICEEndpointsUtil
						.getStr(ICEConstants.ICE_PROVIDER_THREAD_CORE, ICEConstants.ICE_THREAD_CORE));
				initData.properties.setProperty("Ice.ThreadPool.Client.SizeMax",
						LoadICEEndpointsUtil.getStr(ICEConstants.ICE_PROVIDER_THREAD_MAX, ICEConstants.ICE_THREAD_MAX));
				initData.properties.setProperty("Ice.MessageSizeMax", LoadICEEndpointsUtil
						.getStr(ICEConstants.ICE_PROVIDER_MSG_MAX_SIZE, ICEConstants.ICE_MSG_MAX_SIZE));
				cm = Ice.Util.initialize(initData);

				LOG.info("sns_tag create communicator for [{}-{}-{}]", endpointStr, iceSys, serviceName);
			}
			ICECommunicator comm = new ICECommunicator(endpointStr, cm, iceEndpoint.getCzxId());
			ProxyCache.putICECommunicatorCache(endpointStr, serviceName, cm);
			cms.add(comm);
		}
		return cms;
	}

	/**
	 * 批量获取 communicator
	 *
	 * @param serviceName
	 * @return
	 */
	public List<ICECommunicator> getCommunicators(String serviceName, String iceSys) {
		List<ICEEndpoint> endpointList = ProxyCache.getEndpointsCacheBySys(iceSys);
		return doGetCommunicators(serviceName, iceSys, endpointList);
	}

	/**
	 * 根据endpoint构建iceCommunicator
	 * 
	 * @author: shenxiaoping-549
	 * @date: 2017/5/15 11:21
	 * @param serviceName
	 * @param iceSys
	 * @param endpoint
	 * @return
	 */
	public ICECommunicator getSingleCommunicator(String serviceName, String iceSys, String endpoint, long czxId) {
		List<ICEEndpoint> endpointList = new ArrayList<>(1);
		endpointList.add(new ICEEndpoint(endpoint, czxId));
		List<ICECommunicator> resList = doGetCommunicators(serviceName, iceSys, endpointList);
		return !CollectionUtils.isEmpty(resList) ? resList.get(0) : null;
	}

	public Class<?> getPrxHelper(Class<?> service, ICEProvider iceProvider) {
		Class<?> iceProxy = iceProvider.proxy();
		if (iceProxy != ICEProvider.class) {
			return iceProxy;
		}
		String path = service.getPackage().getName() + "." + service.getSimpleName() + "PrxHelper";
		try {
			return Class.forName(path);
		} catch (ClassNotFoundException e) {
			LOG.error("fail to get class:{}", path, e);
		}
		return null;
	}

	public String getServiceName(Class<?> service, ICEProvider provider) {
		String customerName = provider.name();
		return StringUtils.isBlank(customerName) ? StringUtils.uncapitalize(service.getSimpleName()) : customerName;
	}

	public String getServiceName(Class<?> service, String name) {
		return StringUtils.isBlank(name) ? StringUtils.uncapitalize(service.getSimpleName()) : name;
	}

	public void setupPrx(ObjectPrx prx) {
		prx.ice_timeout(LoadICEEndpointsUtil.getInt(ICEConstants.ICE_PROVIDER_PREFIX + ".timeout.ms",
				ICEConstants.ICE_TIEMOUT_MS_DEFAULT));
	}

	public int getTimeout() {
		return LoadICEEndpointsUtil.getInt(ICEConstants.ICE_PROVIDER_PREFIX + ".timeout.ms",
				ICEConstants.ICE_TIEMOUT_MS_DEFAULT);
	}

	public void checkProviderEndpoints(Class<?> service, ICEProvider provider) {
		String serviceName = this.getServiceName(service, provider);
		String endpointsKey = ICEConstants.ICE_PROVIDER_ENDPOINTS_PREFIX + "." + serviceName;
		String endpoints = LoadICEEndpointsUtil.getStr(endpointsKey, provider);
		if (endpoints == null) {
			LOG.error("fail to get the endpoints of {},please check your configuration for {}.", serviceName,
					endpointsKey);
			System.exit(1);
		}
	}

	public Object initSingleProxy(MarkProxy proxy, Class<?> service, ICEProvider iceProvider, String serviceName,
			String iceSys) {
		List<ICECommunicator> cms = new ArrayList<>(1);
		String endpoint = proxy.getEndpoint();
		ICECommunicator singleCommunicator = getSingleCommunicator(serviceName, iceSys, endpoint, proxy.getCzxId());
		cms.add(singleCommunicator);
		ProxyCache.putICECommunicatorCache(endpoint, serviceName, singleCommunicator.getCommunicator());
		return doBuildProxies(cms, service, iceProvider, serviceName, iceSys);
	}

	//
	public synchronized Object initProxies(Class<?> service, ICEProvider iceProvider, String serviceName,
			String iceSys) {
		// 再次验证，防止并发导致多次初始化。
		Object proxy = ProxyCache.getICEProxy(iceSys, serviceName);
		if (proxy == null) {
			Object resProxy = null;
			List<ICECommunicator> cms = null;
			int cnt = 10;
			while (--cnt > 0) {
				cms = getCommunicators(serviceName, iceSys);
				if (cms == null) {
					LOG.info("\n\nfail to get communicator for [{}-{}]\n\n", iceSys, serviceName);
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				} else {
					LOG.info("\n\nget communicator for [{}-{}] successfully!\n\n", iceSys, serviceName);
					break;
				}
			}
			if (cms == null) {
				LOG.error("communicator is null for iceSys-serviceName==>{}-{}", iceSys, serviceName);
				throw new RuntimeException("please check whether the server " + iceSys + " register into zk");
			}
			resProxy = doBuildProxies(cms, service, iceProvider, serviceName, iceSys);
			return resProxy;
		}

		return proxy;
	}

	private Object doBuildProxies(List<ICECommunicator> cms, Class<?> service, ICEProvider iceProvider,
			String serviceName, String iceSys) {
		Object resProxy = null;
		List<Object> proxies = new ArrayList<>();
		for (int i = 0; i < cms.size(); i++) {
			ICECommunicator iceCommunicator = cms.get(i);
			Communicator cm = iceCommunicator.getCommunicator();
			ObjectPrx base = cm.propertyToProxy(serviceName + ".Proxy").ice_twoway().ice_timeout(getTimeout());
			Class<?> proxyHelperClz = getPrxHelper(service, iceProvider);
			try {
				Method cast = proxyHelperClz.getDeclaredMethod("uncheckedCast", ObjectPrx.class);
				Object _proxy = cast.invoke(proxyHelperClz.newInstance(), base);
				if (_proxy != null) {
					ICEProxy wrapProxy = new ICEProxy(iceCommunicator.getEndpoint(), _proxy,
							iceCommunicator.getCzxId());
					proxies.add(wrapProxy);
					// 给第一次初始化调用时的proxy赋值
					if (resProxy == null) {
						resProxy = wrapProxy;
					}
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | InstantiationException e) {
				LOG.error("fail to invoke uncheckedCast", e);
			}
		}
		// cache proxies for special service name
		if (!proxies.isEmpty()) {
			ProxyCache.putICEProxyCache(iceSys, serviceName, proxies);
		}
		return resProxy;
	}
}
