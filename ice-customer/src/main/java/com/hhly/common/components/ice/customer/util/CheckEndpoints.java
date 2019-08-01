/*
 * @Project Name: ice-all
 * @File Name: checkEndpoints
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/6/11 22:47
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.util;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.zk.CallbackReconnected;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.base.util.zk.ZkNode;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import com.hhly.common.components.ice.utils.ICECommonUtil;
import com.hhly.base.util.SNSLOG;


import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * remove the invalid endpoints and close the relative channels.
 *
 * @author shenxiaoping-549
 * @date 2017/6/11 22:47
 * @see
 */
public class CheckEndpoints implements CallbackReconnected {

	private final static SNSLOG LOG = new SNSLOG(CheckEndpoints.class);
	private Map<String, Object> params;

	public CheckEndpoints(Map<String, Object> params) {
		this.params = params;
	}

	@Override
	public Object doPostReconnected(SNSZKUtil snszkUtil) {
		//防止zk启动后，自动重建宕机时的临时节点，没来及删除造成数据不对
		ICECommonUtil.sleep(3, TimeUnit.MINUTES);
		String rootNodePath = (String) params.get("nodePath");
		if (rootNodePath == null) {
			LOG.error("node rootNodePath is null");
			return null;
		}
		try {
			int len = rootNodePath.length();
			Set<Long> czxIds = new HashSet<>();
			StringBuilder fullPath = new StringBuilder(rootNodePath);
			List<String> serviceNames = snszkUtil.getChildrenList(rootNodePath);
			for (int i = 0; i < serviceNames.size(); i++) {
				String fullChildPath = fullPath.append('/').append(serviceNames.get(i)).toString();
				List<ZkNode> nodes = snszkUtil.getChildrenData(fullChildPath);
				for (int j = 0; j < nodes.size(); j++) {
					czxIds.add(nodes.get(j).getCzxId());
				}
				fullPath.delete(len,fullPath.length());
			}
			LOG.info("current czxIds==>{}",JSONObject.toJSONString(czxIds));

			//check proxy cache
			ProxyCache.checkProxyCache(czxIds);

			//check endpoints
			ProxyCache.checkEndpoints(czxIds);

		} catch (Exception e) {
			LOG.error("fail to get children data from rootNode [{}]",rootNodePath,e);
		}
		return null;
	}
}
