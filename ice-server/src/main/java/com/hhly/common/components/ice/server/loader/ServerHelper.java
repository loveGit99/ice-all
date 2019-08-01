/*
 * Project Name: cmp-ice
 * File Name: ServerHelper.java
 * Package Name: com.hhly.common.components.ice.provider.loader
 * Date: 2016年11月26日下午6:51:52
 * Creator: shenxiaoping
 * ------------------------------
 * 修改人: 
 * 修改时间: 
 * 修改内容: 
 */

package com.hhly.common.components.ice.server.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Component;

import com.hhly.base.util.SNSLOG;
import com.hhly.base.util.SystemUtil;
import com.hhly.base.util.zk.SNSZKUtil;
import com.hhly.common.components.ice.server.constants.ICEServerConst;
import com.hhly.common.components.ice.server.util.CallbackReconnectedDefault;
import com.hhly.common.components.ice.server.util.ICEUtil;
import com.hhly.common.components.ice.utils.ICECommonUtil;

import Ice.Communicator;
import Ice.InitializationData;
import Ice.ObjectAdapter;
import Ice.Util;

/**
 * @author Allen Shen
 * @description
 * @date 2016年11月26日下午6:51:52
 * @see
 */
@Component("serverHelper")
public class ServerHelper {

	private final static SNSLOG LOG = new SNSLOG(ServerHelper.class);

	private static boolean isLoadedConstants = false;

	// @Value("${ice.endpoints.node:/ice-services-endpoints}")
	private String iceEndpointsNode;
	// @Value("${service.release.version:NA}")
	private String releaseVersion;
	// @Value("${ice.system.name:NA}")
	private String sysName;
	/**
	 * 服务注册端点
	 */
	// @Value("${ice.servers.endpoints:NA}")
	private String endpoints;
	private String _iceEndpoint;
	private ObjectAdapter adapter;
	private Communicator ic;
	public String serverNodePath;
	public String preServerNodePath;
	// @Value("${ice.servers.exposed:1}")
	private int exposedServer;

	/**
	 * 发布ice server
	 *
	 * @param serviceName
	 * @param serverBean
	 * @return
	 * @date 2016年11月28日上午11:19:07
	 * @author shenxiaoping
	 * @since 1.0.0
	 */
	public boolean registryServer(String serviceName, Object serverBean) {
		try {
			initAdapter();
			this.adapter.add((Ice.Object) serverBean, ic.stringToIdentity(serviceName));
			LOG.warn("has registered server----{}------", serviceName);
		} catch (Exception e) {
			LOG.error("fail to register ice server[{}]", serviceName, e);
			System.exit(1);
		}
		return true;
	}

	public synchronized void initAdapter() {
		initSeverConstant();
		if (sysName == null) {
			LOG.error("please check the ice.system.name in the ice-server.properties! EXIT SYSTEM NOW!");
			System.exit(1);
		}
		if (releaseVersion == null) {
			LOG.error("please check the service.release.version config in the ice-registry.properties, EXIT SYSTEM "
					+ "NOW!");
			System.exit(1);
		}
		if (adapter == null) {
			_iceEndpoint = ICEUtil.getICEEndpoint(endpoints);
			if (exposedServer == 1 && SystemUtil.isLinux()) {
				// 限定注册系统为linux系统，避免开发机误注册进去
				register2centerDuringStartup(_iceEndpoint);
			} else {
				LOG.info("system[{}] is local debug model, not registry zookeeper.", sysName);
			}
			ic = getCommunicator();
			adapter = ic.createObjectAdapterWithEndpoints(ICEUtil.getAdapterRandomName(), _iceEndpoint);
		}
	}

	private void initSeverConstant() {
		// init constants
		if (!isLoadedConstants) {
			iceEndpointsNode = ICEServerConst.ICE_ENDPOINT_NODE;
			sysName = ICEServerConst.ICE_SYSTEM_NAME;
			releaseVersion = ICEServerConst.ICE_RELEASE_VERSION;
			endpoints = ICEServerConst.ICE_SERVERS_ENDPOINTS;
			exposedServer = ICEServerConst.ICE_EXPORT_SERVER;
			isLoadedConstants = true;
		}
	}

	public void startAdapter() {
		if (adapter == null) {
			LOG.error("\n\n\n No ICEServer bean, please check!\n\n\n");
			return;
		}
		LOG.warn("ready to start adapter--{}", adapter.getName());
		adapter.activate();
		new ICEThread().start();
		LOG.info("=============has start adapter===============");
	}

	public boolean doRegister2Center(SNSZKUtil snszkUtil, String iceEndPoints) {
		initSeverConstant();
		String sysNodePath = iceEndpointsNode + '-' + releaseVersion + '/' + sysName + "/server";
		boolean isRegisterOk = false;
		int cnt = 0;
		while (++cnt < 20) {
			try {
				checkIceEndpoints(iceEndPoints);
				serverNodePath = snszkUtil.setDataForciblyReturnNodePath(sysNodePath, iceEndPoints,
						CreateMode.EPHEMERAL_SEQUENTIAL);
				LOG.warn("register service[endpoint:{}] to registerCenter successfully!", iceEndPoints);
				isRegisterOk = true;
				break;
			} catch (Exception e) {
				LOG.error("fail to init services[{}] for endpoint[{}],retry {} times", serverNodePath, iceEndPoints,
						cnt);
				ICECommonUtil.sleep(5, TimeUnit.SECONDS);
			}
		}
		if (!isRegisterOk) {
			LOG.error("fail to register service into registerCenter.EXIT SYSTEM NOW!");
		}
		return isRegisterOk;
	}

	private void register2centerDuringStartup(String iceEndPoints) {
		SNSZKUtil snszkUtil = SNSZKUtil.getInstance();
		boolean isOk = doRegister2Center(snszkUtil, iceEndPoints);
		if (isOk) {
			String rootPath = iceEndpointsNode + '-' + releaseVersion + '/' + sysName;
			Map<String, Object> map = new HashMap<>();
			map.put("nodePath", rootPath);
			map.put("serverHelper", this);
			map.put("subNodePath", serverNodePath);
			snszkUtil.addNotifier(new CallbackReconnectedDefault(map));
		} else {
			LOG.error("fail to register server to zk , EXIT SYSTEM NOW!");
			System.exit(1);
		}
	}

	/**
	 * 重连成功时回调
	 *
	 * @param snszkUtil
	 */
	public void register2CenterCallback(final SNSZKUtil snszkUtil) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < 1440; i++) {
					boolean isRegistered = doRegister2Center(snszkUtil, _iceEndpoint);
					if (isRegistered) {
						break;
					} else {
						try {
							Thread.sleep(1000 * 60);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}).start();
	}

	public Communicator getCommunicator() {
		InitializationData initData = new InitializationData();
		initData.properties = Util.createProperties();
		initData.properties.setProperty("Ice.ThreadPool.Server.Size", ICEServerConst.ICE_SERVER_THREADS_CORE);
		initData.properties.setProperty("Ice.ThreadPool.Server.SizeMax", ICEServerConst.ICE_SERVER_THREADS_MAX);
		initData.properties.setProperty("Ice.MessageSizeMax", ICEServerConst.ICE_SERVER_MSG_MAX_SIZE);
		return Ice.Util.initialize(initData);
	}

	class ICEThread extends Thread {

		@Override
		public void run() {
			try {
				LOG.warn("========== Adapter will accept requests ............ ==============");
				ic.waitForShutdown();
				LOG.warn("===========> has shutdown ice adapter, the application will stop! <===========");
			} catch (Exception e) {
				LOG.error("********* has shutdwon ice adapter ************", e);
			}
		}
	}

	public ObjectAdapter getAdapter() {
		return adapter;
	}

	public void checkIceEndpoints(String endpoints) {
		if (endpoints.indexOf("127.0.0.1") >= 0 || endpoints.indexOf("localhost") >= 0) {
			LOG.error("loopback address [the current setting ip: {}] is invalid to expose service,please check your "
					+ "ice-server.properties ,SHUTDOWN !", endpoints);
			System.exit(1);
		}
	}
}
