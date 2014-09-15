package com.ikantech.yiim.provider;

public enum UriType {
	CONVERSATION(0x01), CONVERSATION_ID(0x02), LIVE_FOLDER_CONVERSATION(0x03), ROSTER_GROUP(
			0x04), ROSTER_GROUP_ID(0x05), LIVE_FOLDER_ROSTER_GROUP(0x06), ROSTER(
			0x07), ROSTER_ID(0x08), LIVE_FOLDER_ROSTER(0x09), MSG(0x0A), MSG_ID(
			0x0B), LIVE_FOLDER_MSG(0x0C), VCARD(0x0D), VCARD_ID(0x0E), LIVE_FOLDER_VCARD(
			0x0F), MULTI_ROOM(0x10), MULTI_ROOM_ID(0x11), LIVE_FOLDER_MULTI_ROOM(0x12);

	private int code;

	private UriType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public String toString() {
		return String.valueOf(code);
	}
}
