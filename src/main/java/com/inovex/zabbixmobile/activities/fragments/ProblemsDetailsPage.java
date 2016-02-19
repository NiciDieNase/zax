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

package com.inovex.zabbixmobile.activities.fragments;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.activities.BaseActivity;
import com.inovex.zabbixmobile.adapters.EventsDetailsPagerAdapter;
import com.inovex.zabbixmobile.data.RemoteAPITask;
import com.inovex.zabbixmobile.model.Item;
import com.inovex.zabbixmobile.model.Trigger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Represents one page of the event details view pager (see
 * {@link EventsDetailsPagerAdapter} ). Shows the details of a specific event.
 * 
 */
public class ProblemsDetailsPage extends BaseDetailsPage {

	private static final String TAG = ProblemsDetailsPage.class.getSimpleName();

	private static final String ARG_TRIGGER_ID = "arg_trigger_id";
	Trigger mTrigger;
	private long mTriggerId;
	private RemoteAPITask mLoadHistoryDetailsTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.page_problems_details, null);
		if (savedInstanceState != null)
			mTriggerId = savedInstanceState.getLong(ARG_TRIGGER_ID, -1);

		return rootView;
	}

	@Override
	protected void fillDetailsText() {
		if (mTrigger != null && getView() != null) {

			((TextView) getView().findViewById(R.id.trigger_details_host))
					.setText(mTrigger.getHostNames());
			((TextView) getView().findViewById(R.id.trigger_details_trigger))
					.setText(mTrigger.getDescription());
			((TextView) getView().findViewById(R.id.trigger_details_severity))
					.setText(mTrigger.getPriority().getNameResourceId());
			((TextView) getView().findViewById(R.id.trigger_details_expression))
					.setText(mTrigger.getExpression());

			Item i = mTrigger.getItem();
			if (i != null) {
				((TextView) getView().findViewById(R.id.trigger_details_item))
						.setText(i.getDescription());
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(i.getLastClock());
				DateFormat dateFormatter = SimpleDateFormat
						.getDateTimeInstance(SimpleDateFormat.SHORT,
								SimpleDateFormat.SHORT, Locale.getDefault());
				cal.setTimeInMillis(i.getLastClock());
				((TextView) getView().findViewById(R.id.latest_data)).setText(i
						.getLastValue()
						+ " "
						+ i.getUnits()
						+ " "
						+ getResources().getString(R.string.at)
						+ " "
						+ dateFormatter.format(cal.getTime()));
			}
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		// if the trigger is not set, this fragment was apparently restored and
		// we
		// need to refresh the event data
		if (mTrigger == null) {
			Log.d(TAG, "trigger was null, loading trigger from database.");
			this.mTrigger = mZabbixDataService.getTriggerById(mTriggerId);
		}
		if (mTrigger != null) {
			fillDetailsText();
			if (!mHistoryDetailsImported && mTrigger.getItem() != null)
				loadHistoryDetails();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(ARG_TRIGGER_ID, mTriggerId);
		super.onSaveInstanceState(outState);
	}

	public void setTrigger(Trigger trigger) {
		this.mTrigger = trigger;
		this.mTriggerId = trigger.getId();
		if (!mHistoryDetailsImported && getView() != null) {
			loadHistoryDetails();
		}
	}

	private void loadHistoryDetails() {
		if(mLoadHistoryDetailsTask!= null)
			mZabbixDataService.cancelTask(mLoadHistoryDetailsTask);
		mLoadHistoryDetailsTask = mZabbixDataService.loadHistoryDetailsByItem(mTrigger.getItem(),
				false, this);
		try{
			((BaseActivity)this.getActivity()).addTask(mLoadHistoryDetailsTask);
		} catch (ClassCastException e){
			e.printStackTrace();
		}
	}

	@Override
	protected void showGraph() {
		showGraph(mTrigger.getItem());
	}

	/**
	 * Refreshes this page's view by reloading the trigger from the database.
	 */
	public void refresh() {
		if (mZabbixDataService != null) {
			this.mTrigger = mZabbixDataService.getTriggerById(mTriggerId);
			fillDetailsText();
		}
	}

}
