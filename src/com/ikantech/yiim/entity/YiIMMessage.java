package com.ikantech.yiim.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

public class YiIMMessage {
	public enum MsgType {
		PLANTEXT("plantext"), IMAGE("image"), VIDEO("video"), AUDIO("audio"), FILE(
				"file"), SYSTEM("system"), BIG_EMOTION("big_emotion");

		private String type;

		private MsgType(String type) {
			this.type = type;
		}

		public static MsgType eval(String type) {
			MsgType[] types = MsgType.values();
			for (MsgType msgType : types) {
				if (msgType.toString().equals(type)) {
					return msgType;
				}
			}
			return MsgType.PLANTEXT;
		}

		public String toString() {
			return type;
		}
	}

	private MsgType type;
	private String body;
	private Map<String, String> params;

	public YiIMMessage() {
		type = MsgType.PLANTEXT;
		body = "";
		params = new HashMap<String, String>();
	}

	public String toString() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("type", type.toString());
			obj.put("body", body);

			Set<String> keys = params.keySet();
			for (String key : keys) {
				obj.put(key, params.get(key));
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return obj.toString();
	}

	public static YiIMMessage fromString(String string) {
		YiIMMessage ret = new YiIMMessage();
		try {
			JSONObject obj = new JSONObject(string);
			ret.type = MsgType.eval(obj.optString("type",
					MsgType.PLANTEXT.toString()));
			ret.body = obj.optString("body", "");
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if ("type".equals(key) || "body".equals(key))
					continue;
				String s = obj.optString(key, null);
				if (s != null) {
					ret.params.put(key, s);
				}
			}
		} catch (Exception e) {
//			e.printStackTrace();
			ret.type = MsgType.PLANTEXT;
			ret.body = string;
		}
		return ret;
	}

	public MsgType getType() {
		return type;
	}

	public void setType(MsgType type) {
		this.type = type;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void addParam(String key, String value) {
		params.put(key, value);
	}
	
	public void putParam(String key, String value) {
		params.put(key, value);
	}
}
