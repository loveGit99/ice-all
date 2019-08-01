/*
 * @Project Name: ice-all
 * @File Name: iceRes
 * @Package Name: com.hhly.common.components.ice.domain
 * @Date: 2017/3/29 10:39
 * @Creator: shenxiaoping-549
 * @line------------------------------
 * @修改人: 
 * @修改时间: 
 * @修改内容: 
 */

package com.hhly.common.components.ice.domain;

/**
 * ICE response
 *
 * @author shenxiaoping-549
 * @date 2017/3/29 10:39
 * @see
 */
public class IceRes {

	public final static String SUCC_CODE = "0";

	public final static String ERROR_CODE = "2";

	public final static String ERROR_COMMON_MSG = "";

	// 响应码
	private String result;

	//错误信息描述
	private String msg;

	//业务数据
	private Object data;

	public IceRes() {
	}

	public IceRes(String result) {
		this.result = result;
	}

	public IceRes(String result, String msg) {
		this.result = result;
		this.msg = msg;
	}

	public IceRes(String result, String msg, Object data) {
		this.result = result;
		this.msg = msg;
		this.data = data;
	}

	public String getResult() {
		return result;
	}

	public IceRes setResult(String result) {
		this.result = result;
		return this;
	}

	public String getMsg() {
		return msg;
	}

	public IceRes setMsg(String msg) {
		this.msg = msg;
		return this;
	}

	public Object getData() {
		return data;
	}

	public IceRes setData(Object data) {
		this.data = data;
		return this;
	}

	public static IceRes succ(Object data){
		return new IceRes(SUCC_CODE).setData(data);
	}

	public static IceRes error(String msg){
		return new IceRes(ERROR_CODE).setMsg(msg);
	}

	public static IceRes error(String result,String msg){
		return new IceRes(result).setMsg(msg);
	}

}
