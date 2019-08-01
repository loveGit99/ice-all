/*
 * @Project Name: sns-base
 * @File Name: CallbackReconnectedDefault
 * @Package Name: com.hhly.base.util.zk
 * @Date: 2017/6/7 10:47
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.util;

import com.hhly.base.util.zk.CallbackReconnected;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.common.components.ice.server.loader.ServerHelper;
import com.hhly.common.components.ice.utils.ICECommonUtil;
import com.hhly.base.util.SNSLOG;


import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缺省处理函数
 *
 * @author shenxiaoping-549
 * @date 2017/6/7 10:47
 * @see
 */
public class CallbackReconnectedDefault implements CallbackReconnected {

	private final static SNSLOG LOG = new SNSLOG(CallbackReconnectedDefault.class);
	private Map<String, Object> params;

	public CallbackReconnectedDefault(Map<String, Object> params) {
		this.params = params;
	}

	@Override
	public Object doPostReconnected(SNSZKUtil snszkUtil) {
		String rootNodePath = (String) params.get("nodePath");
		String subNodePath = (String) params.get("subNodePath");
		if (rootNodePath == null) {
			LOG.error("node rootNodePath is null");
			return null;
		}
		try {
			int cnt = 0;
			ServerHelper helper = (ServerHelper) params.get("serverHelper");
			helper.preServerNodePath = helper.serverNodePath;
			helper.register2CenterCallback(snszkUtil);
			LOG.warn("register zk starting ...............");

			//delete pre subNode
			ICECommonUtil.sleep(30, TimeUnit.SECONDS);
			while (++cnt < 10) {
				if(snszkUtil.isExistNode(helper.preServerNodePath)) {
					boolean isDel = snszkUtil.delNode(helper.preServerNodePath);
					if(isDel){
						LOG.info("delete [{}] node successfully!",helper.preServerNodePath);
						break;
					}
					else{
						LOG.info("fail to delete [{}] node", helper.preServerNodePath);
					}
				}
				ICECommonUtil.sleep(1, TimeUnit.SECONDS);
			}

		} catch (Exception e) {
			LOG.error("fail to register server to zk ", e);
		}
		return null;
	}



}
