/*
 * @Project Name: ice-all
 * @File Name: JdkIceServerProxy
 * @Package Name: com.hhly.common.components.ice.server.proxy
 * @Date: 2017/3/25 16:19
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.SNSLOG;
import com.hhly.base.util.ice.ICEThreadLocal;
import com.hhly.base.util.ice.ICETrace;
import com.hhly.common.components.ice.domain.IceRes;
import com.hhly.common.components.ice.server.util.ICEUtil;
import com.hhly.common.components.ice.utils.DynamicLogUtils;
import com.hhly.common.components.redis.RedisTemplateDriver;

import Ice.Current;

/**
 * Ice Server proxy
 *
 * @author shenxiaoping-549
 * @date 2017/3/25 16:19
 * @see
 */
public class JdkIceServerProxy implements InvocationHandler {

	private final static SNSLOG LOG = new SNSLOG(JdkIceServerProxy.class);
	private Object target;
	private RedisTemplateDriver redis;

	public JdkIceServerProxy(RedisTemplateDriver redis) {
		this.redis = redis;
	}

	public Object getProxy(Object target) {
		this.target = target;
		Class<?>[] interfaces = ICEUtil.getAllInterface(target.getClass());
		Object obj = null;
		try {
			obj = Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces, this);
		} catch (Exception e) {
			LOG.error("fail to proxy ice server[{}] ", target.getClass().getCanonicalName(), e);
		}
		return obj;
	}

	/*
	 * @author: shenxiaoping-549
	 * 
	 * @date: 2017/3/29 12:19 main target : proxy ice methods.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
		Map<String, String> resMap = new HashMap<>();
		boolean isDTModule = initTraceLinkCtx(params, resMap);
		LOG.debug("\n\n##############invoke {}-{}\n\n\n", target.getClass().getCanonicalName(), method.getName());
		try {
			Object res = method.invoke(target, params);
			recordDTx(isDTModule, method, res, resMap);
			DynamicLogUtils.normalLog();
			return res;
		} catch (Exception e) {
			DynamicLogUtils.errorLog();
			LOG.error(e.getMessage(), e);
			StringBuilder serverInfo = new StringBuilder();
			if (e instanceof InvocationTargetException) {
				InvocationTargetException ite = (InvocationTargetException) e;
				StackTraceElement[] info = ite.getTargetException().getStackTrace();
				if (info != null && info.length > 0) {
					int len = Math.min(info.length, 2);
					for (int i = 0; i < len; i++) {
						serverInfo.append(info[i]);
					}
				}
			}
			throw new Ice.UnknownException(e.getCause().toString() + "\n\n" + serverInfo.toString());
		}
	}

	private boolean initTraceLinkCtx(Object[] params, Map<String, String> resMap) {
		boolean isDTModule = false;
		try {
			if (params != null && params.length > 0 && params[params.length - 1] instanceof Current) {
				Current env = (Current) params[params.length - 1];
				Map<String, String> map = env.ctx;
				String rpc = map.get(ICETrace.KEY_RPC_ID);
				rpc = rpc == null ? "-100" : rpc;

				Map<String, String> ctxMap = new HashMap<>();
				String txId = map.get(ICETrace.KEY_CTX_TX_ID);
				String seqId = map.get(ICETrace.KEY_CTX_SEQ_ID);
				if (txId != null && seqId != null) {
					isDTModule = true;
					ctxMap.put(ICETrace.KEY_CTX_TX_ID, txId);
					ctxMap.put(ICETrace.KEY_CTX_SEQ_ID, seqId);
					resMap.put(ICETrace.KEY_CTX_TX_ID, txId);
					resMap.put(ICETrace.KEY_CTX_SEQ_ID, seqId);
				}

				ICEThreadLocal.set(new ICETrace(map.get(ICETrace.KEY_TRACE_ID), Integer.valueOf(rpc), ctxMap));
			}
		} catch (Exception e) {
			LOG.error("fail to set trace flag", e);
		}

		return isDTModule;
	}

	private void recordDTx(boolean isDTModule, Method method, Object res, Map<String, String> resMap) {
		if (!isDTModule) {
			return;
		}
		if (res instanceof String) {
			IceRes iceRes = getIceRes((String) res);
			if (iceRes == null) {
				LOG.error("fail to get IceRes from res json[{}]", String.valueOf(res));
				return;
			} else if (IceRes.SUCC_CODE.equals(iceRes.getResult())) {
				// record redis, expire after 3 days
				redis.set("SNS_DTX_PHASED_RES-" + resMap.get(ICETrace.KEY_CTX_TX_ID) + "_"
						+ resMap.get(ICETrace.KEY_CTX_SEQ_ID), 1, 259200);
			}
		} else {
			LOG.warn("the result is not a String type", res.getClass().getCanonicalName());
		}
	}

	private IceRes getIceRes(String res) {
		try {
			return JSONObject.parseObject(res, IceRes.class);
		} catch (Exception e) {
			LOG.error("It is not an IceRes json string==>{}", res);
		}
		return null;
	}
}
