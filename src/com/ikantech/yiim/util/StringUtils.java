package com.ikantech.yiim.util;

import com.ikantech.support.util.YiUtils;

public class StringUtils {
	private StringUtils() {

	}

	/**
	 * 输入10000@192.168.1.104/Smack
	 * 
	 * @param user
	 * @return 10000@192.168.1.104
	 */
	public static String escapeUserResource(String user) {
		return user.replaceAll("/.+$", "");
	}

	/**
	 * 输入10000@192.168.1.104/Smack
	 * 
	 * @param user
	 * @return 10000
	 */
	public static String escapeUserHost(String user) {
		return user.replaceAll("@.+$", "");
	}

	public static String getJidResouce(String user) {
		if (YiUtils.isStringInvalid(user)) {
			return "";
		}
		return user.substring(user.indexOf('/') + 1, user.length());
	}
}
