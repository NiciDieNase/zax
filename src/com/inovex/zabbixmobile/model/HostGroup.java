package com.inovex.zabbixmobile.model;

import com.j256.ormlite.field.DatabaseField;

public class HostGroup {

	/** Host ID */
	public static final String COLUMN_GROUPID = "groupid";
	@DatabaseField(id = true, columnName = COLUMN_GROUPID)
	private long groupId;
	/** Host name */
	public static final String COLUMN_NAME = "name";
	@DatabaseField(columnName = COLUMN_NAME)
	private String name;
	
	public long getGroupId() {
		return groupId;
	}
	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
