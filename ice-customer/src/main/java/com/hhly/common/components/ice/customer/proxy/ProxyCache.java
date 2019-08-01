/*
 * @Project Name: cmp-ice 
 * @File Name: ProxyCache.java 
 * @Package Name:com.hhly.common.components.ice.customer.proxy 
 * @Date: 2016年11月28日下午4:53:24
 * @Creator: shenxiaoping 
 * @line ------------------------------ 
 * @修改人: 
 * @修改时间: 
 * @修改内容:
 */

package com.hhly.common.components.ice.customer.proxy;

import Ice.Communicator;
import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.zk.ZkNode;
import com.hhly.common.components.ice.customer.domain.ICEEndpoint;
import com.hhly.common.components.ice.customer.domain.ICEProxy;
import com.hhly.common.components.ice.customer.domain.MarkProxy;
import com.hhly.common.components.ice.utils.ICECommonUtil;
import org.apache.commons.lang3.StringUtils;
import com.hhly.base.util.SNSLOG;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ice proxy cache
 *
 * @author Allen Shen
 * @date 2016年11月28日下午4:53:24
 * @see
 */
public class ProxyCache {

	private final static SNSLOG LOG = new SNSLOG(ProxyCache.class);
	public final static String KEY_PROXY = "proxy";
	/**
	 * ice_proxy index counter for round robin
	 */
	protected final static ConcurrentHashMap<String, AtomicInteger> ICE_PROXY_INDEXERS = new ConcurrentHashMap<>();
	/**
	 * ICE proxy object cache。 ConcurrentHashMap<系统名称 , Map<服务名称, proxies>>
	 */
	private final static ConcurrentHashMap<String, Map<String, List<Object>>> ICE_PROXY_CACHE = new ConcurrentHashMap<>();
	/**
	 * endpoint cache
	 */
	private final static ConcurrentHashMap<String, List<ICEEndpoint>> ENDPOINTS_CACHE = new ConcurrentHashMap<>();
	/**
	 * save system name
	 */
	private final static Set<String>  CUSTOM_ENDPOINTS_CACHE = new HashSet<>();

	/**
	 * 系统对应的服务名称  sysName---serviceName
	 */
	private final static ConcurrentHashMap<String, List<String>> SYS_SERVICE_CACHE = new ConcurrentHashMap<>();
	/**
	 * ICE provider annotation cache
	 */
	private static ConcurrentHashMap<String, Object> ICE_PROVIDER_CACHE = new ConcurrentHashMap<>();
	/**
	 * test chain :  endpoint, Communicator,
	 */
	private static ConcurrentHashMap<String, Communicator> ENDPOINT_COMMUNICATOR_CACHE = new ConcurrentHashMap<>();
	/**
	 * save the origin ice interface class
	 */
	private static ConcurrentHashMap<String, Map<String,Object>> ICE_INTERFACE_CLZ = new ConcurrentHashMap<>();


	private static Set<String> ICE_STYSTEM_CACHE = new HashSet<>();



	public static void putProviderCache(String serviceName, Object proxy) {
		synchronized (ICE_PROVIDER_CACHE) {
			Object _proxy = getProviderProxy(serviceName);
			if (_proxy != null) {
				LOG.error("find more than one for this service---{},please check", serviceName);
				System.exit(1);
			}
			ICE_PROVIDER_CACHE.put(serviceName, proxy);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getProviderProxy(String serviceName) {
		return (T) ICE_PROVIDER_CACHE.get(serviceName);
	}

	public static void putICEProxyCache(String sysName, String serviceName, List<Object> proxies) {
		synchronized (ICE_PROXY_CACHE) {
			Map<String, List<Object>> sysServiceMap = ICE_PROXY_CACHE.get(sysName);
			if (sysServiceMap == null) {
				sysServiceMap = new HashMap<String, List<Object>>();
				ICE_PROXY_CACHE.put(sysName, sysServiceMap);
			}
			List<Object> singleProxies = sysServiceMap.get(serviceName);
			if (singleProxies == null) {
				LOG.info("\n\n\n#######get [{}] proxies for serviceName [{}-{}]###########\n\n\n", proxies.size(),
						sysName, serviceName);
				Map map = new HashMap<>();
				map.put(serviceName, proxies);
				ICE_PROXY_CACHE.put(sysName, map);
			} else {
				singleProxies.addAll(proxies);
			}
		}
	}

	public static void putICEProxyCacheBySys(String sysName, String endpoint, long czxId) {
		List<String> services = ProxyCache.getServiceCache(sysName);
		if (services == null) {
			services = new ArrayList<>();
		}
		int _size = services.size();
		MarkProxy mp = new MarkProxy(endpoint, czxId);
		Map<String/*服务名*/, List<Object>> _serviceMap = ICE_PROXY_CACHE.get(sysName);
		synchronized (ICE_PROXY_CACHE) {
			if (_serviceMap == null || _serviceMap.isEmpty()) {
				_serviceMap = new HashMap<String, List<Object>>();
				ICE_PROXY_CACHE.put(sysName, _serviceMap);
			}
			if (_size == 0) {
				LOG.warn("none services for {}", sysName);
				services.add(endpoint);
				List<Object> mps = new ArrayList<>();
				mps.add(mp);
				_serviceMap.put(endpoint, mps);
			} else {
				for (int i = 0; i < _size; i++) {
					List<Object> proxies = _serviceMap.get(services.get(i));
					if (proxies == null) {
						proxies = new ArrayList<>();
					}
					//check exist
					boolean isExist = false;
					for (int j = 0; j < proxies.size(); j++) {
						Object p = proxies.get(j);
						if (p instanceof ICEProxy && czxId == (((ICEProxy) p).getCzxId())) {
							isExist = true;
							break;
						} else if (p instanceof MarkProxy && czxId == (((MarkProxy) p).getCzxId())) {
							isExist = true;
							break;
						}
					}
					if (!isExist) {
						proxies.add(mp);
					}
				}
			}
		}
	}

	/**
	 * 获取 proxy 对象
	 *
	 * @param serviceName
	 * @return
	 */
	public static Object getICEProxy(String sysName, String serviceName) {
		Map<String, List<Object>> sysMap = ICE_PROXY_CACHE.get(sysName);
		if (sysMap == null) {
			LOG.warn("The ice server system config [{}] is null, please check the zookeeper node.",sysName);
			return null;
		}
		List<Object> res = sysMap.get(serviceName);
		if (res == null || res.isEmpty()) {
			return null;
		}
		// 根据serviceName的index值获取下一个proxy对象
		AtomicInteger resIndex = ICE_PROXY_INDEXERS.get(serviceName);
		if (resIndex == null) {
			resIndex = new AtomicInteger(0);
			ICE_PROXY_INDEXERS.put(serviceName, resIndex);
		}
		int index = resIndex.incrementAndGet();
		if (index > 100000 || index < 0) {
			resIndex.set(-1);
			index = 0;
		}
		return res.get(index % res.size());
	}

	/**
	 * 删除标记函数
	 *
	 * @param proxy
	 * @param sysName
	 * @param serviceName
	 * @author: shenxiaoping-549
	 * @date: 2017/5/31 11:54
	 */
	public static void removeMarkProxy(MarkProxy proxy, String sysName, String serviceName) {
		if (proxy == null) {
			return;
		}
		String endpoint = proxy.getEndpoint();
		if (StringUtils.isBlank(endpoint)) {
			LOG.info("mark proxy 's endpoint is null,skip");
			return;
		}
		try {
			List<Object> list = ICE_PROXY_CACHE.get(sysName).get(serviceName);
			synchronized (list) {
				if (list != null && list.size() > 0) {
					for (int i = 0; i < list.size(); i++) {
						Object p = list.get(i);
						if (p instanceof MarkProxy && endpoint.equals(((MarkProxy) p).getEndpoint())) {
							list.remove(p);
							LOG.debug("remvoe markProxy[{}] successfully!", endpoint);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("fail to remove markProxy[{}]", proxy.getEndpoint(), e);
		}
	}

	public static ConcurrentHashMap<String, List<ICEEndpoint>> getEndpointsCache() {
		return ENDPOINTS_CACHE;
	}

	public static List<ICEEndpoint> getEndpointsCacheBySys(String sysName) {
		return ENDPOINTS_CACHE.get(sysName);
	}

	public static boolean isExistedEndpoint(String system, String endpoint, long czxId) {
		synchronized (ENDPOINTS_CACHE) {
			List<ICEEndpoint> iceEndpoints = ENDPOINTS_CACHE.get(system);
			if (iceEndpoints == null) {
				return false;
			}
			for (int i = 0; i < iceEndpoints.size(); i++) {
				if (czxId == iceEndpoints.get(i).getCzxId()) {
					return true;
				}
			}
			return false;
		}
	}

	public static boolean putCustomEndpointsCache(String system,String endpoint){
		List<ICEEndpoint> endpoints = ENDPOINTS_CACHE.get(system);
		if (endpoints == null) {
			endpoints = new ArrayList<>();
			ENDPOINTS_CACHE.put(system, endpoints);
		}

		String[] customEndpoints = endpoint.split(",");
		for (int i = 0; i < customEndpoints.length; i++) {
			endpoints.add(new ICEEndpoint(customEndpoints[i]));
		}

		return true;
	}


	public static List<ICEEndpoint> cacheEndpoints(String system, List<ZkNode> endpointList){
		List<ICEEndpoint> res = null;
		synchronized (ENDPOINTS_CACHE) {
			if (endpointList == null || endpointList.isEmpty()) {
				return null;
			}
			res = new ArrayList<>(endpointList.size());
			List<ICEEndpoint> endpoints = ENDPOINTS_CACHE.get(system);
			if (endpoints == null) {
				endpoints = new ArrayList<>();
				ENDPOINTS_CACHE.put(system, endpoints);
			}
			Set<Long> endpointSet = new HashSet<>(((int) (endpoints.size() / .75)) + 1);
			for (int j = 0; j < endpoints.size(); j++) {
				endpointSet.add(endpoints.get(j).getCzxId());
			}
			for (int i = 0; i < endpointList.size(); i++) {
				ZkNode zkNode = endpointList.get(i);
				long czxId = zkNode.getCzxId();
				String e = ICECommonUtil.byte2Str((byte[]) zkNode.getValue());
				if (!endpointSet.contains(czxId)) {
					LOG.warn("add czxId==>{}",czxId);
					ICEEndpoint iee = new ICEEndpoint(e, czxId);
					endpoints.add(iee);
					res.add(iee);
				}
			}
			return res;
		}
	}


	public static boolean putEndpointsCache(String system, List<ZkNode> endpointList) {
		cacheEndpoints(system,endpointList);
		return true;
	}

	public static boolean putEndpointsCache(String system, String endpoint, long czxId) {
		synchronized (ENDPOINTS_CACHE) {
			List<ICEEndpoint> endpoints = ENDPOINTS_CACHE.get(system);
			if (endpoints == null) {
				endpoints = new ArrayList<>();
				ENDPOINTS_CACHE.put(system, endpoints);
			}
			Set<Long> endpointSet = new HashSet<>(((int) (endpoints.size() / .75)) + 1);
			for (int j = 0; j < endpoints.size(); j++) {
				endpointSet.add(endpoints.get(j).getCzxId());
			}
			if (!endpointSet.contains(czxId)) {
				endpoints.add(new ICEEndpoint(endpoint, czxId));
				return true;
			}
			return false;
		}
	}

	public static void removeEndpointsCache(String system, String endpoint, long czxId) {
		synchronized (ENDPOINTS_CACHE) {
			List<ICEEndpoint> es = ENDPOINTS_CACHE.get(system);
			if (es != null && !es.isEmpty()) {
				for (int i = 0; i < es.size(); i++) {
					if (es.get(i).getCzxId() == czxId) {
						es.remove(i);
						LOG.warn("has delete endpoint [{}-{}] from endpoint_cache successfully! ",
								Long.toHexString(czxId), endpoint);
						return;
					}
				}
			}
		}
	}

	public static List<String> getServiceCache(String system) {
		return SYS_SERVICE_CACHE.get(system);
	}

	public static void addServiceCache(String system, String serviceName) {
		List<String> services = SYS_SERVICE_CACHE.get(system);
		synchronized (SYS_SERVICE_CACHE) {
			if (services == null) {
				services = new ArrayList<>();
				SYS_SERVICE_CACHE.put(system, services);
			}
			if (!services.contains(serviceName)) {
				services.add(serviceName);
			}
		}
	}

	public static void putICECommunicatorCache(String endpoint, String serviceName,Communicator cm) {
		if(cm != null && !cm.isShutdown()){
			ENDPOINT_COMMUNICATOR_CACHE.putIfAbsent(endpoint + ":" + serviceName, cm);
		}
	}

	public static Communicator getICECommunicatorCache(String endpoint,String serviceName){
		return ENDPOINT_COMMUNICATOR_CACHE.get(endpoint + ":" + serviceName);
	}

	/**
	 * 链接是否关闭, 如果关闭则将改communicator从缓存中删除
	 * @param endpoint
	 * @return
	 */
	public static boolean isClosedConn(String endpoint) {
		closedConn(endpoint);
		return true;
	}

	/**
	 * 获取还需要serviceName  pendding
	 * @param endpoint
	 */
	public static void closedConn(String endpoint) {
		Iterator<Map.Entry<String,Communicator>> iterator = ENDPOINT_COMMUNICATOR_CACHE.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry<String,Communicator>  me = iterator.next();
			String key = me.getKey();
			if(key.indexOf(endpoint+":")>0){
				Communicator cm = me.getValue();
				if(cm == null || cm.isShutdown()){
					ENDPOINT_COMMUNICATOR_CACHE.remove(key);
					cm.destroy();
				}
			}
		}
	}

	/**
	 * 从缓存中移除断开的链接
	 *
	 * @param sysName
	 * @param endpoint
	 * @param czxId
	 * @author: shenxiaoping-549
	 * @date: 2017/5/16 18:11
	 */
	public static void removeDisconnICEProxy(String sysName, String endpoint, long czxId) {
		try {
			Map<String, List<Object>> serviceMap = ICE_PROXY_CACHE.get(sysName);
			if (serviceMap == null) {
				return;
			}
			synchronized (ICE_PROXY_CACHE) {
				Collection<List<Object>> proxies = serviceMap.values();
				if (proxies != null && !proxies.isEmpty()) {
					Iterator<List<Object>> iterator = proxies.iterator();
					while (iterator.hasNext()) {
						List<Object> list = iterator.next();
						if (list != null && !list.isEmpty()) {
							for (int i = 0; i < list.size(); i++) {
								Object p = list.get(i);
								if (p instanceof ICEProxy) {
									if (czxId == ((ICEProxy) p).getCzxId()) {
										list.remove(i);
										i--;
									}
								} else if (czxId == ((MarkProxy) p).getCzxId()) {
									list.remove(i);
									i--;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("fail to remvoe disconnection proxy=>sys[{}]==endpoint[{}]", sysName, endpoint, e);
		}
	}


	public static void checkProxyCache(Set<Long> czxIds){
		//  <String/*系统名称*/ , Map<String/* serviceName */, List<Object>/* proxies */>>
		 Iterator<Map<String,List<Object>>> iterator = ICE_PROXY_CACHE.values().iterator();
		 while(iterator.hasNext()){
			 Iterator<List<Object>> iceIterators = iterator.next().values().iterator();
			 while(iceIterators.hasNext()){
				 List<Object> iceProxies = iceIterators.next();
				 for (int i = 0; i < iceProxies.size(); i++) {
					 Object proxy = iceProxies.get(i);
					 if(proxy instanceof  ICEProxy){
						if(!czxIds.contains (((ICEProxy)proxy).getCzxId())){
							iceProxies.remove(i);
							i--;
							LOG.warn("remove proxy after zk_reconnect ==[{}}]", JSONObject.toJSONString(proxy));
						}
					 }
					 else{
						 if(!czxIds.contains (((MarkProxy)proxy).getCzxId())){
							 iceProxies.remove(i);
							 i--;
							 LOG.warn("remove proxy after zk_reconnect ==[{}}]", JSONObject.toJSONString(proxy));
						 }
					 }
				 }

			 }

		 }

	}

	public static void checkEndpoints(Set<Long> czxIds){
		Iterator<List<ICEEndpoint>> iterator = ENDPOINTS_CACHE.values().iterator();
		while(iterator.hasNext()){
			List<ICEEndpoint> endpoints = iterator.next();
			for (int i = 0; i < endpoints.size(); i++) {
				ICEEndpoint iceEndpoint = endpoints.get(i);
				if(!czxIds.contains(iceEndpoint.getCzxId())){
					endpoints.remove(i);
					i--;
					LOG.info("remove endpoint==[{}--{}]",iceEndpoint.getEndpoint(),iceEndpoint.getCzxId());
				}
			}
		}

	}


	public static Map<String,Object> getICEOriginalClz(String key){
		return ICE_INTERFACE_CLZ.get(key);
	}

	public static void putICEOriginalClz(String key,  Map<String,Object> data){
		ICE_INTERFACE_CLZ.putIfAbsent(key,data);
	}

	public static void putCustomEndpoint(String endpoint){
		CUSTOM_ENDPOINTS_CACHE.add(endpoint);
	}

	public static Set<String> getCustomEndpoint(){
		return CUSTOM_ENDPOINTS_CACHE;
	}

	public static Set<String> getICESystemCache() {
		return ICE_STYSTEM_CACHE;
	}

	public static void putICESystemCache(String system){
		ICE_STYSTEM_CACHE.add(system);
	}

	/**
	 * ============================================for cache CLI ===================================
	 **/
	public static ConcurrentHashMap<String, AtomicInteger> getIceProxyIndexers() {
		return ICE_PROXY_INDEXERS;
	}

	public static ConcurrentHashMap<String, Map<String, List<Object>>> getIceProxyCache() {
		return ICE_PROXY_CACHE;
	}

	public static ConcurrentHashMap<String, List<String>> getSysServiceCache() {
		return SYS_SERVICE_CACHE;
	}

	public static ConcurrentHashMap<String, Communicator> getEndpointCommunicatorCache() {
		return ENDPOINT_COMMUNICATOR_CACHE;
	}
}
