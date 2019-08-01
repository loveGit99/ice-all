/*
 * @Project Name: ice-all
 * @File Name: ICEEndpoint
 * @Package Name: com.hhly.common.components.ice.customer.domain
 * @Date: 2017/6/8 10:37
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.domain;

import com.hhly.common.components.ice.customer.constants.ICEConstants;

/**
 * ICE endpoint 封装类
 *
 * @author shenxiaoping-549
 * @date 2017/6/8 10:37
 * @see
 */
public class ICEEndpoint {

	private String endpoint;
	private long czxId;
	/**
	 * 0  是 注册zk，
	 * 1 是custom endpoint
	 */
	private int type;

	public ICEEndpoint(String endpoint){
		this.endpoint = endpoint;
		this.type = ICEConstants.ICE_ENDPOINT_TYPE_CUSTOM;
	}

	public ICEEndpoint(String endpoint, long czxId) {
		this.endpoint = endpoint;
		this.czxId = czxId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public long getCzxId() {
		return czxId;
	}

	public void setCzxId(long czxId) {
		this.czxId = czxId;
	}

	public String getCzxIdStr() {
		return Long.toHexString(this.czxId);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
