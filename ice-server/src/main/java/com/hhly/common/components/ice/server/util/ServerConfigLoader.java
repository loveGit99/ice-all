/*
 * @Project Name: ice-all
 * @File Name: ServerCnfigLoader
 * @Package Name: com.hhly.common.components.ice.server.util
 * @Date: 2017/9/14 15:47
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.server.util;

import com.hhly.base.util.ApplicationPropertiesLoader;
import com.hhly.base.util.SNSLOG;
import com.hhly.common.components.ice.constants.ICECommonConstant;
import com.hhly.common.components.ice.utils.ConfigLoader;

import java.util.Properties;

/**
 * todo
 *
 * @author shenxiaoping-549
 * @date 2017/9/14 15:47
 * @see
 */
public class ServerConfigLoader {

	private final static SNSLOG LOG = new SNSLOG(ServerConfigLoader.class);

	public static Properties loadServerConfig() {
		Properties ps = ApplicationPropertiesLoader.getApplicationProperties4ICE(ICECommonConstant.ICE_SERVER_SBOOT_PREFIX);
		if (ps == null) {
			ps = ConfigLoader.getInstance().load("env/ice-server.properties");
			if (ps == null) {
				LOG.error("fail to load env/ice-server.properties, SYSTEM EXIT NOW !");
				System.exit(1);
			}
		}
		return ps;
	}
}
