/*
 * @Project Name: ice-all
 * @File Name: ProxyCacheCLI
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/6/3 17:02
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.customer.domain.ICEEndpoint;
import com.hhly.common.components.ice.customer.proxy.ProxyCache;
import com.hhly.common.components.ice.utils.ICECommonUtil;

import Ice.Communicator;

/**
 * proxy cli server
 *
 * @author shenxiaoping-549
 * @date 2017/6/3 17:02
 * @see
 */
@Component
public class ProxyCacheCLI {

	private final static SNSLOG LOG = new SNSLOG(ProxyCacheCLI.class);

	private int cliPort;

	public enum COMMAND {
		PROXY("0"), ENDPOINT("1"), SERVICES("2"), COMMUNICATOR("3"), BYE("4");

		String code;

		COMMAND(String code) {
			this.code = code;
		}
	}

	@PostConstruct
	public void startCLI() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				cli();
			}
		}).start();
	}

	public void cli() {
		ServerSocket serverSocket = null;
		try {
			cliPort = getCLIPort();
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress("127.0.0.1", cliPort));
			LOG.info("\n\n----has start proxy cli[127.0.0.1,{}]---\n\n ", cliPort);
			while (true) {
				Socket socket = serverSocket.accept();
				InputStream in = null;
				PrintStream out = null;
				try {
					while (true) {
						in = socket.getInputStream();
						BufferedReader request = new BufferedReader(new InputStreamReader(in));
						out = new PrintStream(socket.getOutputStream());
						commandInfo(out);
						String reqStr = request.readLine();
						if (!COMMAND.BYE.code.equals(reqStr)) {
							out.flush();
							out.print("\n");
							out.println("==========================================");
							out.println("");
							getInfoByCLI(socket, out, reqStr);
							out.println("");
							out.println("");
							out.println("==========================================");
							out.print("\n\n\n");
						} else {
							socket.close();
						}
					}
				} catch (Exception ex) {
					LOG.error(ex.getMessage(), ex);
				} finally {
					try {
						if (in != null) {
							in.close();
						}
						if (out != null) {
							out.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			LOG.error("fail to build cli socket server[port:{}],SYSTEM EXIT ! ", cliPort, e);
			System.exit(1);
		}
	}

	private static void commandInfo(PrintStream out) {
		out.println(">>query commands ：");
		out.println("\t0: proxyCache");
		out.println("\t1: endpoints");
		out.println("\t2: system");
		out.println("\t3: communicator");
		out.println("\t4: bye");
		out.print("\t>>>please input the command number:");
	}

	private void getInfoByCLI(Socket socket, PrintStream out, String command) throws Exception {

		if (COMMAND.PROXY.code.equals(command)) {
			ConcurrentHashMap<String, Map<String, List<Object>>> proxies = ProxyCache.getIceProxyCache();
			getSystemInfo(out, proxies);
			out.print("\t######please select system: ");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String sys = in.readLine();
			Map<String, List<Object>> services = proxies.get(sys);
			out.println("\t[Result]: ");
			formatJsonStr(out, JSONObject.toJSONString(services));
		} else if (COMMAND.ENDPOINT.code.equals(command)) {
			ConcurrentHashMap<String, List<ICEEndpoint>> es = ProxyCache.getEndpointsCache();
			getSystemInfo(out, es);
			out.print("\t######please select system: ");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String sys = in.readLine();
			out.println("\t[Result]: ");
			formatJsonStr(out, JSONObject.toJSONString(es.get(sys)));

		} else if (COMMAND.SERVICES.code.equals(command)) {
			ConcurrentHashMap<String, List<String>> ss = ProxyCache.getSysServiceCache();
			getSystemInfo(out, ss);
			out.print("\t######please select system: ");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String sys = in.readLine();
			out.println("\t[Result]: ");
			formatJsonStr(out, JSONObject.toJSONString(ss.get(sys)));
		} else if (COMMAND.COMMUNICATOR.code.equals(command)) {
			ConcurrentHashMap<String, Communicator> cs = ProxyCache.getEndpointCommunicatorCache();
			Iterator<Map.Entry<String, Communicator>> it = cs.entrySet().iterator();
			StringBuilder cmms = new StringBuilder();
			while (it.hasNext()) {
				Map.Entry<String, Communicator> me = it.next();
				cmms.append("\t").append(me.getKey()).append("===>").append(me.getValue().getProperties().toString());
			}
			out.print(cmms);
		} else {
			out.print("  unknown command [" + command + "] !  ");
		}
	}

	private void getSystemInfo(PrintStream out, ConcurrentHashMap<String, ?> map) throws Exception {
		// get System
		Enumeration<String> systems = map.keys();
		out.println("\t[SYSTEM]");
		while (systems.hasMoreElements()) {
			out.println("\t   |- " + systems.nextElement());
		}
		out.println("");
	}

	private static void formatJsonStr(PrintStream out, String json) {
		String[] consoleStr = getConsoleFormatJsonStr(3, json).split("\n");
		for (int i = 0; i < consoleStr.length; i++) {
			out.print(consoleStr[i]);
			out.println("");
		}
	}

	private static String getConsoleFormatJsonStr(int initSpaceNum, String json) {
		StringBuilder sb = new StringBuilder(json.length() + 100);
		int spaceCnt = initSpaceNum;
		int quoteCount = 0;
		addTabs(sb, spaceCnt);
		for (int i = 0; i < json.length(); i++) {
			char s = json.charAt(i);
			if (s == '[' || s == '{') {
				spaceCnt++;
				sb.append(s);
				sb.append("\n");
				addTabs(sb, spaceCnt);
			} else if (s == ']' || s == '}') {
				sb.append("\n");
				spaceCnt--;
				addTabs(sb, spaceCnt);
				sb.append(s);
			} else if (s == '"') {
				sb.append('"');
				quoteCount++;
			} else if (s == ',' && quoteCount % 2 == 0) {
				sb.append(s).append("\n");
				addTabs(sb, spaceCnt);
			} else {
				sb.append(s);
			}
		}
		return sb.toString();
	}

	private static void addTabs(StringBuilder sb, int number) {
		for (int j = 0; j < number; j++) {
			sb.append("\t");
		}
	}

	private static int getCLIPort() {
		int cliPort = ICECommonUtil.getRandPort(5000);

		String path = "cliport.txt";
		File file = new File(path);
		LOG.info("the cli port file path is:{}", file.getAbsoluteFile());
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)))) {
			out.write(String.valueOf(cliPort));
		} catch (Exception e) {
			LOG.error("fail to write cli port[{}] into cliport.txt", cliPort, e);
		}
		return cliPort;
	}
}
