/*
 * @Project Name: ice-all
 * @File Name: proxyFactory
 * @Package Name: com.hhly.common.components.ice.customer.proxy
 * @Date: 2017/7/3 16:33
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.SNSLOG;
import com.hhly.base.util.ice.ICEThreadLocal;
import com.hhly.base.util.ice.ICETrace;
import com.hhly.common.components.ice.annotation.ErrorMsg;
import com.hhly.common.components.ice.customer.annotation.ICEProvider;
import com.hhly.common.components.ice.customer.constants.ExceptionConstants;
import com.hhly.common.components.ice.customer.constants.ICEConstants;
import com.hhly.common.components.ice.customer.domain.ICEProxy;
import com.hhly.common.components.ice.customer.domain.MarkProxy;
import com.hhly.common.components.ice.customer.util.ICEUtil;
import com.hhly.common.components.ice.customer.util.LoadICEEndpointsUtil;
import com.hhly.common.components.ice.domain.IceRes;
import com.hhly.common.components.ice.utils.DynamicLogUtils;

import Ice.ConnectionRefusedException;
import Ice.ObjectPrx;
import Ice.UnknownException;

/**
 * ICE proxy 对象工厂
 *
 * @author shenxiaoping-549
 * @date 2017/7/3 16:33
 * @see
 */
@Component
public class ICEProxyFactory {

	private final static SNSLOG LOG = new SNSLOG(ICEProxyFactory.class);
	@Autowired
	private ClientHelper helper;

	/**
	 * 通过ICEProvider 修饰的对象获取proxy对象
	 *
	 * @param clz
	 *            ： ICEProvider 注解的对象的类
	 * @return
	 * @author: shenxiaoping-549
	 * @date: 2017/7/3 16:52
	 */
	public Object getProxy(Class<?> clz) {
		ICEProvider iceProvider = clz.getAnnotation(ICEProvider.class);
		if (iceProvider == null) {
			LOG.error("this class [{}] does not have ICEProvider annotation,please check again",
					clz.getCanonicalName());
			throw new RuntimeException("invalid ice proxy object!");
		}
		return getProxy(clz, iceProvider);
	}

	public Object getProxy(Class<?> service, ICEProvider iceProvider) {
		String serviceName = helper.getServiceName(service, iceProvider);
		// 增加服务缓存
		ProxyCache.addServiceCache(iceProvider.system(), serviceName);
		Object proxy = ProxyCache.getProviderProxy(serviceName);
		return proxy != null ? proxy : this.doBuildProxy(service, iceProvider);
	}

	public Object getProxy(final Class<?> service, final String iceServiceName, final String sysName) {
		ICEProvider iceProvider = ICEUtil.instance(iceServiceName, sysName);
		return getProxy(service, iceProvider);
	}

	/**
	 * 构建proxy对象
	 *
	 * @param service
	 * @param iceProvider
	 * @return
	 */
	private Object doBuildProxy(final Class<?> service, final ICEProvider iceProvider) {
		// pending , need modify
		// helper.checkProviderEndpoints(service, iceProvider);
		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(new Class[] { service });
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
				return iceMethodInterceptor(service, iceProvider, obj, method, args);
			}
		});
		Object proxy = enhancer.create();
		ProxyCache.putProviderCache(helper.getServiceName(service, iceProvider), proxy);
		return proxy;
	}

	private Object iceMethodInterceptor(Class<?> service, ICEProvider iceProvider, Object obj, Method method,
			Object[] args) throws Exception {
		String serviceName = helper.getServiceName(service, iceProvider);
		return doIceInvoke(service, iceProvider, method, args, serviceName);
	}

	private Object doIceInvoke(Class<?> service, ICEProvider iceProvider, Method method, Object[] args,
			String serviceName) {
		Object rpcResponse = null;
		int retryCount = 3;
		while (retryCount-- > 0) {
			Object result = doSingleIceInvoke(service, iceProvider, method, args, serviceName);
			if (result instanceof IceRes) {
				IceRes ir = (IceRes) result;
				if (ir.getResult().equals(ExceptionConstants.CONNECTION_REFUSE_CODE)) {
					continue;
				} else if (ir.getResult().equals(ExceptionConstants.CONNECTION_TIMEOUT_CODE)) {
					rpcResponse = ExceptionConstants.SYS_ERROR;
					break;
				} else {
					rpcResponse = result;
					break;
				}

			} else {
				rpcResponse = result;
				break;
			}
		}
		// 做一次转换，防止客户端不识别网络超时code
		if (rpcResponse instanceof IceRes) {
			if (((IceRes) rpcResponse).getResult().equals(ExceptionConstants.CONNECTION_TIMEOUT_CODE)) {
				rpcResponse = ExceptionConstants.SYS_ERROR;
			}
			return JSONObject.toJSONString(rpcResponse);
		}
		return rpcResponse;
	}

	private Object doSingleIceInvoke(Class<?> service, ICEProvider iceProvider, Method method, Object[] args,
			String serviceName) {
		long xs = System.currentTimeMillis();
		int costThreshold = 1000 * LoadICEEndpointsUtil.getInt(ICEConstants.ICE_PROVIDER_COST_THRESHOLD_SECONDES, 10);
		long startMS = System.currentTimeMillis();
		Object result = null;
		Object iceProxy = null;
		Method targetProxyMethod = null;
		String code = ExceptionConstants.SYS_EXCEPTION_CODE;
		String msg = ExceptionConstants.SYS_EXCEPTION_MSG;
		try {
			LOG.console("the request argument==>{}", JSONObject.toJSONString(args));
			LOG.debug("param===>{}", JSONObject.toJSONString(args));
			iceProxy = ((ICEProxy) getICEProxy(service, iceProvider, method, args, serviceName)).getProxy();
			iceProxy = initICEContext(iceProxy);
			LOG.debug("get the ice proxy facade---[{}]----", iceProxy);
			targetProxyMethod = ICEUtil.getMethod(iceProxy, method.getName(), method.getParameterTypes());
			LOG.debug("the proxy [{}] will invoke [{}]", iceProxy, targetProxyMethod);
			long _s = System.currentTimeMillis();
			result = targetProxyMethod.invoke(iceProxy, args);
			long _cost = System.currentTimeMillis() - _s;
			if (_cost > costThreshold) {
				LOG.warn("\n\n [ice_invoke_issue] performance issue: Proxy#Method[{}#{}] cost {} milliseconds during "
						+ "invoked proxy method", iceProxy, targetProxyMethod, _cost);
			}
			LOG.debug("get the ice facade result:--{}--", result);
			DynamicLogUtils.normalLog();
		} catch (Exception e) {
			DynamicLogUtils.errorLog();
			long xe = System.currentTimeMillis();
			LOG.error("fail to invoke the ice server for facade==>[{}], cost=={} ms", iceProxy, (xe - xs), e);
			if (e instanceof InvocationTargetException) {
				Throwable ex = ((InvocationTargetException) e).getTargetException();
				if (ex instanceof ConnectionRefusedException) {
					code = ExceptionConstants.CONNECTION_REFUSE_CODE;
					msg = ExceptionConstants.CONNECTION_REFUSE_MSG;
				} else if (ex instanceof Ice.UnknownException) {
					UnknownException ue = (UnknownException) ex;
					try {
						String[] codeMsg = ue.unknown.split("\\r")[0].split(ExceptionConstants.SPLIT_SYMBOL_CODE_MSG);
						if (codeMsg[0].equals("SNS.BIZException")) {
							code = codeMsg[1];
							msg = codeMsg[2];
						} else {
							LOG.error("Key exception info from sns-services ===>{} ", ue.unknown);
						}
					} catch (Exception e1) {
						LOG.error("fail to parse ice unknownException==>ueMsg=>{}\n, ueUnkown=>{}", ue.toString(),
								ue.unknown);
					}
				}
			} else if (targetProxyMethod != null) {
				ErrorMsg errorMsg = targetProxyMethod.getAnnotation(ErrorMsg.class);
				if (errorMsg != null) {
					msg = errorMsg.msg();
					code = errorMsg.code();
				}
			}
			result = new IceRes(code, msg);
		}
		return result;
	}

	/*
	 * @author: shenxiaoping-549
	 * 
	 * @date: 2017/3/27 14:51 init ice current variable for tracing
	 */
	private Object initICEContext(Object iceProxy) {
		ICETrace trace = ICEThreadLocal.getTrace();
		if (trace == null) {
			LOG.warn("miss the ice trace in the thread local function!");
			return iceProxy;
		} else {
			Map<String, String> ctx = new HashMap<>();
			ctx.put(ICETrace.KEY_TRACE_ID, trace.getTraceId());
			ctx.put(ICETrace.KEY_RPC_ID, incrementAndGetRpcId(trace));
			Map<String, String> traceCtx = trace.getTraceCtx();
			if (!traceCtx.isEmpty()) {
				ctx.put(ICETrace.KEY_CTX_TX_ID, traceCtx.get(ICETrace.KEY_CTX_TX_ID));
				ctx.put(ICETrace.KEY_CTX_SEQ_ID, traceCtx.get(ICETrace.KEY_CTX_SEQ_ID));
			}
			ObjectPrx p = (ObjectPrx) iceProxy;
			p = p.ice_context(ctx);
			return p;
		}
	}

	private String incrementAndGetRpcId(ICETrace trace) {
		int rpcId = trace.getRpcId() + 1;
		trace.setRpcId(rpcId);
		LOG.debug("rpceId increment for ICETrace[{}]", trace.toString());
		return String.valueOf(rpcId);
	}

	/**
	 * @param service
	 * @param iceProvider
	 * @param method
	 * @param args
	 * @param serviceName
	 * @return
	 * @description get ice proxy object
	 * @date 2016年11月30日上午11:40:26
	 * @author shenxiaoping
	 * @since 1.0.0
	 */
	private Object getICEProxy(Class<?> service, ICEProvider iceProvider, Method method, Object[] args,
			String serviceName) {
		Object resProxy = null;
		// get ice proxy from the cache
		String iceSys = iceProvider.system();
		Object proxy = ProxyCache.getICEProxy(iceSys, serviceName);
		if (proxy != null) {
			if (proxy instanceof MarkProxy) {
				MarkProxy mp = (MarkProxy) proxy;
				proxy = helper.initSingleProxy(mp, service, iceProvider, serviceName, iceSys);
				// remove markProxy object when build a new proxy via markProxy.
				if (proxy != null) {
					ProxyCache.removeMarkProxy(mp, iceSys, serviceName);
				}
				return proxy;
			} else {
				return proxy;
			}
		}
		// generate ice proxies and cache them.
		long s = System.currentTimeMillis();
		resProxy = helper.initProxies(service, iceProvider, serviceName, iceSys);
		long e = System.currentTimeMillis();
		LOG.info("creating ice proxy[{}] cost {} milliseconds", iceSys, (e - s));
		return resProxy;
	}
}
