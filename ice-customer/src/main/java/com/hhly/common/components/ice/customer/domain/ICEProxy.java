/*
 * @Project Name: ice-all
 * @File Name: ICEProxy
 * @Package Name: com.hhly.common.components.ice.customer.domain
 * @Date: 2017/5/15 15:40
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.domain;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * ICE 代理对象
 *
 * @author shenxiaoping-549
 * @date 2017/5/15 15:40
 * @see
 */
public class ICEProxy {

	private String endpoint;

	private long  czxId;
	/**
	 * ice 链接代理  ObjectPrx
	 */
	@JSONField(serialize=false)
	private Object proxy;

	public ICEProxy() {
	}

	public ICEProxy(String endpoint, Object proxy) {
		this.endpoint = endpoint;
		this.proxy = proxy;
	}

	public ICEProxy(String endpoint, Object proxy,long czxId) {
		this.endpoint = endpoint;
		this.proxy = proxy;
		this.czxId = czxId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Object getProxy() {
		return proxy;
	}

	public void setProxy(Object proxy) {
		this.proxy = proxy;
	}

	public long getCzxId() {
		return czxId;
	}

	public void setCzxId(long czxId) {
		this.czxId = czxId;
	}

	public String getCzxIdStr(){
		return Long.toHexString(this.czxId);
	}
}
