/*
 * @Project Name: ice-all
 * @File Name: ICECommunicator
 * @Package Name: com.hhly.common.components.ice.customer.domain
 * @Date: 2017/5/15 16:56
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.customer.domain;

import Ice.Communicator;

/**
 * todo
 *
 * @author shenxiaoping-549
 * @date 2017/5/15 16:56
 * @see
 */
public class ICECommunicator {

	private String endpoint;

	private long czxId;

	private Communicator  communicator;

	public ICECommunicator() {
	}

	public ICECommunicator(String endpoint, Communicator communicator) {
		this.endpoint = endpoint;
		this.communicator = communicator;
	}

	public ICECommunicator(String endpoint, Communicator communicator,long czxId) {
		this.endpoint = endpoint;
		this.communicator = communicator;
		this.czxId = czxId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Communicator getCommunicator() {
		return communicator;
	}

	public void setCommunicator(Communicator communicator) {
		this.communicator = communicator;
	}

	public long getCzxId() {
		return czxId;
	}

	public void setCzxId(long czxId) {
		this.czxId = czxId;
	}
}
