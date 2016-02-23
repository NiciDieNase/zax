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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.activities.BaseActivity;
import com.inovex.zabbixmobile.adapters.EventsDetailsPagerAdapter;
import com.inovex.zabbixmobile.listeners.OnAcknowledgeEventListener;
import com.inovex.zabbixmobile.model.Event;

/**
 * Fragment which displays event details using a ViewPager (adapter:
 * {@link EventsDetailsPagerAdapter}).
 * 
 */
public class EventsDetailsFragment extends
		BaseSeverityFilterDetailsFragment<Event> {

	public static final String TAG = EventsDetailsFragment.class
			.getSimpleName();

	MenuItem mMenuItemAcknowledge;

	@Override
	protected void retrievePagerAdapter() {
		mDetailsPagerAdapter = mZabbixDataService
				.getEventsDetailsPagerAdapter(((BaseActivity)this.getActivity()).getPersistedServerSelection(),mSeverity);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.events_details, menu);
		mMenuItemAcknowledge = menu.findItem(R.id.menuitem_acknowledge_event);

		mMenuItemShare = menu.findItem(R.id.menuitem_share);
		mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(mMenuItemShare);
		updateShareIntent();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuitem_acknowledge_event:
			Event e = mDetailsPagerAdapter.getCurrentObject();
			Log.d(TAG, "Acknowledge event: " + e);
			if (e == null)
				return false;
			// show dialog
			AcknowledgeDialogFragment dialog = AcknowledgeDialogFragment
					.getInstance();
			dialog.setEvent(e);
			dialog.show(getChildFragmentManager(),
					AcknowledgeDialogFragment.TAG);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Enables/disables the menu button for event acknowledgement.
	 * 
	 * @param enabled
	 *            true: enable; false: disable the button
	 */
	private void setAcknowledgeButtonEnabled(boolean enabled) {
		if (mMenuItemAcknowledge != null)
			mMenuItemAcknowledge.setVisible(enabled);
	}

	/**
	 * Updates the app's menu. If necessary (event is not acknowledged yet), the
	 * acknowledge button is displayed in the action bar.
	 */
	@Override
	protected void updateMenu() {
		super.updateMenu();
		if (mDetailsPagerAdapter == null
				|| mDetailsPagerAdapter.getCount() == 0) {
			this.setAcknowledgeButtonEnabled(false);
			return;
		}
		Event e = mDetailsPagerAdapter.getCurrentObject();
		if (e != null && e.isAcknowledged())
			this.setAcknowledgeButtonEnabled(false);
		else
			this.setAcknowledgeButtonEnabled(true);
	}

	@Override
	public void refreshCurrentItem() {
		EventsDetailsPage currentPage = (EventsDetailsPage) mDetailsPagerAdapter
				.instantiateItem(mDetailsPager,
						mDetailsPagerAdapter.getCurrentPosition());
		if (currentPage != null)
			currentPage.refresh();
	}

	// TODO: orientation change when acknowledge dialog is open
	/**
	 * The dialog displayed when an event shall be acknowledged.
	 * 
	 */
	public static class AcknowledgeDialogFragment extends DialogFragment {

		public static final String TAG = AcknowledgeDialogFragment.class
				.getSimpleName();

		private OnAcknowledgeEventListener mCallback;
		private Event mEvent;

		public static AcknowledgeDialogFragment getInstance() {
			return new AcknowledgeDialogFragment();
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);

			try {
				mCallback = (OnAcknowledgeEventListener) activity;
			} catch (ClassCastException e) {
				throw new ClassCastException(activity.toString()
						+ " must implement OnAcknowledgeEventListener.");
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();

			View view = inflater.inflate(R.layout.dialog_acknowledge, null);
			builder.setView(view);

			final EditText comment = (EditText) view
					.findViewById(R.id.acknowledge_comment);
			// add title
			builder.setTitle(R.string.acknowledge_event);
			// Add action buttons
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							mCallback.acknowledgeEvent(mEvent, comment
									.getText().toString());
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							AcknowledgeDialogFragment.this.getDialog().cancel();
						}
					});
			return builder.create();
		}

		public void setEvent(Event event) {
			this.mEvent = event;
		}

	}

}
