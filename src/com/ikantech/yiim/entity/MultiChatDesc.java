package com.ikantech.yiim.entity;

import org.json.JSONObject;

import com.ikantech.support.util.YiUtils;
import com.ikantech.yiim.R;

public class MultiChatDesc {
	public static enum MultiChatIcon {
		DEFAULIT_1("default1", R.drawable.troop_default_head_1), DEFAULIT_2(
				"default2", R.drawable.troop_default_head_2), DEFAULIT_3(
				"default3", R.drawable.troop_default_head_3), DEFAULIT_4(
				"default4", R.drawable.troop_default_head_4);

		int mId;
		String mDesc;

		private MultiChatIcon(String desc, int id) {
			// TODO Auto-generated constructor stub
			mId = id;
			mDesc = desc;
		}

		public int getResId() {
			return mId;
		}

		public String toString() {
			return mDesc;
		}

		public static MultiChatIcon eval(String str) {
			MultiChatIcon[] types = MultiChatIcon.values();
			for (MultiChatIcon msgType : types) {
				if (msgType.toString().equals(str)) {
					return msgType;
				}
			}
			return MultiChatIcon.DEFAULIT_4;
		}
	}

	private MultiChatIcon icon;
	private String desc;
	private String name;

	public MultiChatIcon getIcon() {
		return icon;
	}

	public void setIcon(MultiChatIcon icon) {
		this.icon = icon;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static MultiChatDesc fromString(String str) {
		MultiChatDesc ret = new MultiChatDesc();
		try {
			JSONObject obj = new JSONObject(str);

			ret.icon = MultiChatIcon.eval(obj.optString("icon",
					MultiChatIcon.DEFAULIT_4.toString()));
			ret.desc = obj.optString("desc", "");
			ret.name = obj.optString("name", "");
		} catch (Exception e) {
			// TODO: handle exception
			ret.icon = MultiChatIcon.DEFAULIT_4;
			ret.desc = str;
		}
		return ret;
	}

	public String toString() {
		JSONObject obj = new JSONObject();
		try {
			obj.put("icon", icon.toString());
			if (YiUtils.isStringInvalid(desc)) {
				obj.put("desc", "");
			} else {
				obj.put("desc", desc);
			}
			if (YiUtils.isStringInvalid(name)) {
				obj.put("name", "");
			} else {
				obj.put("name", name);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return obj.toString();
	}
}
