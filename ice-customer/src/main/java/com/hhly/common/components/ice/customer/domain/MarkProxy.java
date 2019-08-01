/*
 * @Project Name: ice-all
 * @File Name: NULLProxy
 * @Package Name: com.hhly.common.components.ice.customer.util
 * @Date: 2017/5/15 11:06
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.domain;

/**
 *   ice proxy 标记对象
 *
 * @author shenxiaoping-549
 * @date 2017/5/15 11:06
 * @see
 */
public class MarkProxy {

	private String endpoint ;

	private long czxId;

	public MarkProxy(){

	}

	public MarkProxy(String endpoint){
		this.endpoint = endpoint;
	}

	public MarkProxy(String endpoint, long czxid) {
		this.endpoint = endpoint;
		this.czxId = czxid;
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

	public String getCzxIdStr(){
		return Long.toHexString(this.czxId);
	}
}
