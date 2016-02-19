/*
This file is part of ZAX.

	ZAX is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	ZAX is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with ZAX.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.inovex.zabbixmobile.data.APITasks;

import com.inovex.zabbixmobile.data.DatabaseHelper;
import com.inovex.zabbixmobile.data.RemoteAPITask;
import com.inovex.zabbixmobile.data.ZabbixRemoteAPI;
import com.inovex.zabbixmobile.exceptions.FatalException;
import com.inovex.zabbixmobile.exceptions.ZabbixLoginRequiredException;
import com.inovex.zabbixmobile.listeners.OnProblemListLoadedListener;
import com.inovex.zabbixmobile.model.Trigger;
import com.inovex.zabbixmobile.model.TriggerSeverity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by felix on 19/02/16.
 */
public class LoadProblemsTask extends RemoteAPITask {

	private final long hostGroupId;
	private final OnProblemListLoadedListener callback;
	private final boolean hostGroupChanged;
	private Map<TriggerSeverity, List<Trigger>> triggers;

	public LoadProblemsTask(ZabbixRemoteAPI api, DatabaseHelper dbHelper, long hostGroupId, OnProblemListLoadedListener callback, boolean hostGroupChanged) {
		super(api);
		this.zabbixAPI = api;
		this.mDatabaseHelper = dbHelper;
		this.hostGroupId = hostGroupId;
		this.callback = callback;
		this.hostGroupChanged = hostGroupChanged;
	}

	@Override
	protected void executeTask() throws ZabbixLoginRequiredException,
			FatalException {
		triggers = new HashMap<TriggerSeverity, List<Trigger>>();
		try {
			zabbixAPI.importActiveTriggers(this);
			// even if the api call is not successful, we can still use
			// the cached events
		} finally {
			for (TriggerSeverity severity : TriggerSeverity.values()) {
				triggers.put(severity, this.mDatabaseHelper
						.getProblemsBySeverityAndHostGroupId(severity,
								hostGroupId));
			}
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);

		if (callback != null)
			callback.onProblemListLoaded(triggers, hostGroupChanged);

	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		if (callback != null)
			callback.onSeverityListAdapterProgressUpdate(values[0]);
	}

}
