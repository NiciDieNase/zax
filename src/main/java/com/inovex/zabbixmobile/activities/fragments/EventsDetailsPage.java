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

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.adapters.EventsDetailsPagerAdapter;
import com.inovex.zabbixmobile.data.RemoteAPITask;
import com.inovex.zabbixmobile.model.Event;
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
public class EventsDetailsPage extends BaseDetailsPage {

	private static final String TAG = EventsDetailsPage.class.getSimpleName();

	private static final String ARG_EVENT_ID = "arg_event_id";
	private Event mEvent;
	private long mEventId = -1;

	private boolean mHistoryDetailsImported = false;
	private RemoteAPITask mLoadHistoryDetailsTask;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
			mEventId = savedInstanceState.getLong(ARG_EVENT_ID, -1);
		Log.d(TAG, "onCreate: " + this.toString());
		Log.d(TAG, "mEventId: " + mEventId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.page_events_details, null);

		return rootView;
	}

	@Override
	protected void fillDetailsText() {

		if (mEvent != null && getView() != null) {

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(mEvent.getClock());
			DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance(
					SimpleDateFormat.SHORT, SimpleDateFormat.SHORT,
					Locale.getDefault());
			((TextView) getView().findViewById(R.id.event_details_time))
					.setText(dateFormatter.format(cal.getTime()));
			TextView status = ((TextView) getView().findViewById(
					R.id.event_details_status));
			status.setText((mEvent.getValue() == Event.VALUE_OK) ? R.string.ok
					: R.string.problem);
			status.setCompoundDrawablesWithIntrinsicBounds(
					mEvent.getValue() == Event.VALUE_OK ? R.drawable.ok
							: R.drawable.problem, 0, 0, 0);
			TextView acknowledged = ((TextView) getView().findViewById(
					R.id.event_details_acknowledged));
			acknowledged.setText((mEvent.isAcknowledged()) ? R.string.yes
					: R.string.no);
			acknowledged.setCompoundDrawablesWithIntrinsicBounds(mEvent
					.isAcknowledged() ? R.drawable.ok : R.drawable.problem, 0,
					0, 0);

			Trigger t = mEvent.getTrigger();
			if (t != null) {

				((TextView) getView().findViewById(R.id.trigger_details_host))
						.setText(t.getHostNames());
				((TextView) getView()
						.findViewById(R.id.trigger_details_trigger)).setText(t
						.getDescription());
				((TextView) getView().findViewById(
						R.id.trigger_details_severity)).setText(t.getPriority()
						.getNameResourceId());
				((TextView) getView().findViewById(
						R.id.trigger_details_expression)).setText(t
						.getExpression());

				Item i = t.getItem();
				if (i != null) {
					((TextView) getView().findViewById(
							R.id.trigger_details_item)).setText(i
							.getDescription());
					cal.setTimeInMillis(i.getLastClock());
					((TextView) getView().findViewById(R.id.latest_data))
							.setText(i.getLastValue() + " " + i.getUnits()
									+ " "
									+ getResources().getString(R.string.at)
									+ " " + dateFormatter.format(cal.getTime()));
				}
			}

		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		super.onServiceConnected(name, service);
		// if the event is not set, this fragment was apparently restored and we
		// need to refresh the event data
		if (mEvent == null) {
			Log.d(TAG, "event was null, loading event from database.");
			mEvent = mZabbixDataService.getEventById(mEventId);
		}

		if (mEvent != null) {
			fillDetailsText();
			if (!mHistoryDetailsImported && mEvent.getTrigger() != null
					&& mEvent.getTrigger().getItem() != null){
				loadHistoryDetails();
			}
		}
	}

	private void loadHistoryDetails() {
		if(mLoadHistoryDetailsTask != null && mZabbixDataService != null) {
			mZabbixDataService.cancelTask(mLoadHistoryDetailsTask);
		}
		mLoadHistoryDetailsTask = mZabbixDataService.loadHistoryDetailsByItem(mEvent.getTrigger()
				.getItem(), false, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(ARG_EVENT_ID, mEventId);
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState: " + this.toString());
	}

	public void setEvent(Event event) {
		this.mEvent = event;
		this.mEventId = event.getId();
		if (!mHistoryDetailsImported && getView() != null){
			loadHistoryDetails();
		}
	}

	@Override
	protected void showGraph() {
		showGraph(mEvent.getTrigger().getItem());
	}

	/**
	 * Refreshes the current view by loading the event from the database.
	 */
	public void refresh() {
		if (mZabbixDataService != null) {
			this.mEvent = mZabbixDataService.getEventById(mEventId);
			fillDetailsText();
		}
	}
}
