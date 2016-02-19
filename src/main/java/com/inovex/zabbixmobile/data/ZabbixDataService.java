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

package com.inovex.zabbixmobile.data;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;

import com.inovex.zabbixmobile.activities.BaseSeverityFilterActivity;
import com.inovex.zabbixmobile.activities.ChecksActivity;
import com.inovex.zabbixmobile.activities.fragments.ChecksHostsFragment;
import com.inovex.zabbixmobile.activities.fragments.EventsDetailsFragment;
import com.inovex.zabbixmobile.activities.fragments.EventsListPage;
import com.inovex.zabbixmobile.activities.fragments.ProblemsDetailsFragment;
import com.inovex.zabbixmobile.activities.fragments.ProblemsListPage;
import com.inovex.zabbixmobile.activities.fragments.ScreensListFragment;
import com.inovex.zabbixmobile.adapters.BaseServiceAdapter;
import com.inovex.zabbixmobile.adapters.BaseServicePagerAdapter;
import com.inovex.zabbixmobile.adapters.BaseSeverityListPagerAdapter;
import com.inovex.zabbixmobile.adapters.BaseSeverityPagerAdapter;
import com.inovex.zabbixmobile.adapters.ChecksApplicationsPagerAdapter;
import com.inovex.zabbixmobile.adapters.ChecksItemsListAdapter;
import com.inovex.zabbixmobile.adapters.EventsDetailsPagerAdapter;
import com.inovex.zabbixmobile.adapters.EventsListAdapter;
import com.inovex.zabbixmobile.adapters.EventsListPagerAdapter;
import com.inovex.zabbixmobile.adapters.HostGroupsSpinnerAdapter;
import com.inovex.zabbixmobile.adapters.HostsListAdapter;
import com.inovex.zabbixmobile.adapters.ProblemsDetailsPagerAdapter;
import com.inovex.zabbixmobile.adapters.ProblemsListAdapter;
import com.inovex.zabbixmobile.adapters.ProblemsListPagerAdapter;
import com.inovex.zabbixmobile.adapters.ScreensListAdapter;
import com.inovex.zabbixmobile.adapters.ServersListManagementAdapter;
import com.inovex.zabbixmobile.adapters.ServersListSelectionAdapter;
import com.inovex.zabbixmobile.data.APITasks.LoadProblemsTask;
import com.inovex.zabbixmobile.exceptions.FatalException;
import com.inovex.zabbixmobile.exceptions.ZabbixLoginRequiredException;
import com.inovex.zabbixmobile.listeners.OnAcknowledgeEventListener;
import com.inovex.zabbixmobile.listeners.OnApplicationsLoadedListener;
import com.inovex.zabbixmobile.listeners.OnGraphDataLoadedListener;
import com.inovex.zabbixmobile.listeners.OnGraphsLoadedListener;
import com.inovex.zabbixmobile.listeners.OnHostsLoadedListener;
import com.inovex.zabbixmobile.listeners.OnItemsLoadedListener;
import com.inovex.zabbixmobile.listeners.OnScreensLoadedListener;
import com.inovex.zabbixmobile.listeners.OnEventsListsLoadedListener;
import com.inovex.zabbixmobile.listeners.OnProblemListLoadedListener;
import com.inovex.zabbixmobile.model.Application;
import com.inovex.zabbixmobile.model.Event;
import com.inovex.zabbixmobile.model.Graph;
import com.inovex.zabbixmobile.model.GraphItem;
import com.inovex.zabbixmobile.model.HistoryDetail;
import com.inovex.zabbixmobile.model.Host;
import com.inovex.zabbixmobile.model.HostGroup;
import com.inovex.zabbixmobile.model.Item;
import com.inovex.zabbixmobile.model.Screen;
import com.inovex.zabbixmobile.model.Trigger;
import com.inovex.zabbixmobile.model.TriggerSeverity;
import com.inovex.zabbixmobile.model.ZabbixServer;
import com.inovex.zabbixmobile.model.ZaxPreferences;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bound service maintaining the connection with the Zabbix API as well as the
 * local SQLite database. It provides an interface to the data no matter where
 * it is stored,
 *
 * This class also contains all adapters used for the various views throughout
 * the app. These are initialized when the service is created.
 *
 */
public class ZabbixDataService extends Service {

	public interface OnLoginProgressListener {

		public void onLoginStarted();

		public void onLoginFinished(boolean success, boolean showToast);

	}

	private static final String TAG = ZabbixDataService.class.getSimpleName();

	public static final String EXTRA_IS_TESTING = "is_testing";
	boolean mIsTesting = false;
	// Binder given to clients
	private final IBinder mBinder = new ZabbixDataBinder();

	private DatabaseHelper mDatabaseHelper;

	private HostGroupsSpinnerAdapter mHostGroupsSpinnerAdapter;

	/**
	 * Adapters maintained by {@link ZabbixDataService}.
	 */

	// Servers
	private ServersListSelectionAdapter mServersListSelectionAdapter;
	private ServersListManagementAdapter mServersListManagementAdapter;

	// Events
	private EventsListPagerAdapter mEventsListPagerAdapter;
	private HashMap<TriggerSeverity, EventsListAdapter> mEventsListAdapters;
	private HashMap<TriggerSeverity, EventsDetailsPagerAdapter> mEventsDetailsPagerAdapters;

	// Problems
	private ProblemsListPagerAdapter mProblemsListPagerAdapter;
	private ProblemsListAdapter mProblemsMainListAdapter;
	private HashMap<TriggerSeverity, ProblemsListAdapter> mProblemsListAdapters;
	private HashMap<TriggerSeverity, ProblemsDetailsPagerAdapter> mProblemsDetailsPagerAdapters;

	// Checks
	private HostsListAdapter mHostsListAdapter;
	private ChecksApplicationsPagerAdapter mChecksApplicationsPagerAdapter;
	private HashMap<String, ChecksItemsListAdapter> mChecksItemsListAdapters;

	// Screens
	private ScreensListAdapter mScreensListAdapter;

	private Context mActivityContext;
	private LayoutInflater mInflater;
	private ZabbixRemoteAPI mRemoteAPI;
	private int mBindings = 0;
	private long mCurrentZabbixServerId;

	private ZaxPreferences mPreferences;

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class ZabbixDataBinder extends Binder {
		public ZabbixDataService getService() {
			// Return this service instance so clients can call public methods
			return ZabbixDataService.this;
		}
	}

	public boolean isLoggedIn() {
		return mRemoteAPI.isLoggedIn();
	}

	/**
	 * Clears all data in the database and in the adapters.
	 */
	public void clearAllData() {

		Log.d(TAG, "clearing all data");

		mDatabaseHelper.clearAllData();

		mPreferences.refresh(getApplicationContext());
		mCurrentZabbixServerId = mPreferences.getServerSelection();
		Log.d(TAG, "mCurrentZabbixServerId="+mCurrentZabbixServerId);

		mRemoteAPI.logout();
		mRemoteAPI = new ZabbixRemoteAPI(this.getApplicationContext(),
				mDatabaseHelper, mCurrentZabbixServerId, null);
	}

	public ZabbixServer getServerById(long id){
		return mDatabaseHelper.getZabbixServerById(id);
	}

	/**
	 * Returns the Zabbix server list adapter for the server management
	 * activity.
	 *
	 * @return list adapter
	 */
	public BaseServiceAdapter<ZabbixServer> getServersListManagementAdapter() {
		return mServersListManagementAdapter;
	}

	/**
	 * Returns the Zabbix server selection list adapter for the drawer fragment.
	 *
	 * @return list adapter
	 */
	public BaseServiceAdapter<ZabbixServer> getServersSelectionAdapter() {
		return mServersListSelectionAdapter;
	}

	/**
	 * Returns the event list pager adapter.
	 *
	 * @return list pager adapter
	 */
	public BaseSeverityListPagerAdapter<Event> getEventsListPagerAdapter() {
		return mEventsListPagerAdapter;
	}

	/**
	 * Returns an event list adapter.
	 *
	 * See {@link EventsListPage}.
	 *
	 * @param severity
	 *            severity of the adapter
	 * @return list adapter
	 */
	public BaseServiceAdapter<Event> getEventsListAdapter(
			TriggerSeverity severity) {
		return mEventsListAdapters.get(severity);
	}

	/**
	 * Returns an event details adapter to be used by the view pager.
	 *
	 * See {@link EventsDetailsFragment}.
	 *
	 * @param severity
	 *            severity of the adapter
	 * @return details pager adapter
	 */
	public BaseSeverityPagerAdapter<Event> getEventsDetailsPagerAdapter(
			TriggerSeverity severity) {
		return mEventsDetailsPagerAdapters.get(severity);
	}

	/**
	 * Returns the adapter for the host group spinner.
	 *
	 * See {@link BaseSeverityFilterActivity}, {@link ChecksActivity}.
	 *
	 * @return spinner adapter
	 */
	public HostGroupsSpinnerAdapter getHostGroupSpinnerAdapter() {
		return mHostGroupsSpinnerAdapter;
	}

	/**
	 * Returns the problems list pager adapter.
	 *
	 * @return list pager adapter
	 */
	public BaseSeverityListPagerAdapter<Trigger> getProblemsListPagerAdapter() {
		return mProblemsListPagerAdapter;
	}

	/**
	 * Returns the problems list adapter for the main view. This adapter
	 * contains all active problems regardless of severity and hostgroup.
	 *
	 * @return list adapter
	 */
	public BaseServiceAdapter<Trigger> getProblemsMainListAdapter() {
		return mProblemsMainListAdapter;
	}

	/**
	 * Returns a problems list adapter.
	 *
	 * See {@link ProblemsListPage}.
	 *
	 * @param severity
	 *            severity of the adapter
	 * @return list adapter
	 */
	public BaseServiceAdapter<Trigger> getProblemsListAdapter(
			TriggerSeverity severity) {
		return mProblemsListAdapters.get(severity);
	}

	/**
	 * Returns a problems details adapter to be used by the view pager.
	 *
	 * See {@link ProblemsDetailsFragment}.
	 *
	 * @param severity
	 *            severity of the adapter
	 * @return details pager adapter
	 */
	public BaseSeverityPagerAdapter<Trigger> getProblemsDetailsPagerAdapter(
			TriggerSeverity severity) {
		return mProblemsDetailsPagerAdapters.get(severity);
	}

	/**
	 * Returns a host list adapter.
	 *
	 * See {@link ChecksHostsFragment}.
	 *
	 * @return host list adapter
	 */
	public HostsListAdapter getHostsListAdapter() {
		return mHostsListAdapter;
	}

	/**
	 * Returns the screens list adapter.
	 *
	 * See {@link ScreensListFragment}.
	 *
	 * @return screens list adapter
	 */
	public ScreensListAdapter getScreensListAdapter() {
		return mScreensListAdapter;
	}

	/**
	 * Returns the application adapter.
	 *
	 * @return
	 */
	public ChecksApplicationsPagerAdapter getChecksApplicationsPagerAdapter() {
		return mChecksApplicationsPagerAdapter;
	}

	/**
	 * Returns the application items list adapter.
	 *
	 * @return
	 */
	public ChecksItemsListAdapter getChecksItemsListAdapter(long applicationID) {
		return mChecksItemsListAdapters.get(mDatabaseHelper.getApplicationById(applicationID).toString());
	}

	/**
	 * Retrieves the host with the given ID from the database.
	 *
	 * @param hostId
	 * @return
	 */
	public Host getHostById(long hostId) {
		return mDatabaseHelper.getHostById(hostId);
	}

	/**
	 * Retrieves the item with the given ID from the database. No remote api
	 * call
	 *
	 * @param itemId
	 * @return
	 */
	public Item getItemById(long itemId) {
		return mDatabaseHelper.getItemById(itemId);
	}

	/**
	 * Retrieves the event with the given ID from the database.
	 *
	 * @param eventId
	 * @return
	 */
	public Event getEventById(long eventId) {
		return mDatabaseHelper.getEventById(eventId);
	}

	/**
	 * Retrieves the trigger with the given ID from the database.
	 *
	 * @param triggerId
	 * @return
	 */
	public Trigger getTriggerById(long triggerId) {
		return mDatabaseHelper.getTriggerById(triggerId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		mBindings++;
		Log.d(TAG, "onBind: " + mBindings);
		Log.d(TAG,
				"Binder " + this.toString() + ": intent " + intent.toString()
						+ " bound.");

		if (intent.getBooleanExtra(EXTRA_IS_TESTING, false)) {
			mIsTesting = true;
		}

		if (!mIsTesting) {
			onBind(null, null);
		}

		return mBinder;
	}

	public void onBind(DatabaseHelper databasehelperMock,
			ZabbixRemoteAPI remoteAPIMock) {
		if (mDatabaseHelper == null) {
			// set up SQLite connection using OrmLite
			if (databasehelperMock != null) {
				mDatabaseHelper = databasehelperMock;
			} else {
				mDatabaseHelper = OpenHelperManager.getHelper(this,
						DatabaseHelper.class);
				// recreate database
				// mDatabaseHelper.onUpgrade(
				// mDatabaseHelper.getWritableDatabase(), 0, 1);
			}

			Log.d(TAG, "onCreate");
		}
		if (mRemoteAPI == null) {
			if (remoteAPIMock != null) {
				mRemoteAPI = remoteAPIMock;
			} else {
				// in case there was a preferences migration
				mPreferences.refresh(getApplicationContext());
				mCurrentZabbixServerId = mPreferences.getServerSelection();
				mRemoteAPI = new ZabbixRemoteAPI(this.getApplicationContext(),
						mDatabaseHelper, mCurrentZabbixServerId, null);
			}
		}
	}

	/**
	 * Performs the Zabbix login using the server address and credentials from
	 * the preferences. Login is only performed if the user Additionally, this
	 * method loads the host groups and fills the corresponding adapter because
	 * host groups are used at so many spots in the program, so they should be
	 * available all the time.
	 *
	 * @param listener
	 *            listener to be informed about start and end of the login
	 *            process
	 *
	 */
	public void performZabbixLogin(final OnLoginProgressListener listener) {
		final boolean loginNecessary = !mRemoteAPI.isLoggedIn();
		if (loginNecessary && listener != null)
			listener.onLoginStarted();

		// authenticate
		RemoteAPITask loginTask = new RemoteAPITask(mRemoteAPI) {

			private boolean success = false;
			private List<HostGroup> hostGroups;
			private final BaseServiceAdapter<HostGroup> groupsAdapter = mHostGroupsSpinnerAdapter;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				if (loginNecessary)
					mRemoteAPI.authenticate();
				success = true;

				hostGroups = new ArrayList<HostGroup>();
				try {
					mRemoteAPI.importHostsAndGroups();
					// even if the api call is not successful, we can still use
					// the cached events
				} finally {
					hostGroups = mDatabaseHelper.getHostGroups(mCurrentZabbixServerId);
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (groupsAdapter != null && hostGroups != null) {
					// adapter.clear();
					groupsAdapter.addAll(hostGroups);
					groupsAdapter.notifyDataSetChanged();
				}
				if (listener != null)
					listener.onLoginFinished(success, loginNecessary);
			}

		};
		loginTask.execute();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");

		mPreferences = ZaxPreferences.getInstance(getApplicationContext());
		mCurrentZabbixServerId = mPreferences.getServerSelection();

		// set up adapters
		mServersListSelectionAdapter = new ServersListSelectionAdapter(this);
		mServersListManagementAdapter = new ServersListManagementAdapter(this);

		mEventsListPagerAdapter = new EventsListPagerAdapter(this);
		mEventsListAdapters = new HashMap<TriggerSeverity, EventsListAdapter>(
				TriggerSeverity.values().length);
		mEventsDetailsPagerAdapters = new HashMap<TriggerSeverity, EventsDetailsPagerAdapter>(
				TriggerSeverity.values().length);

		for (TriggerSeverity s : TriggerSeverity.values()) {
			mEventsListAdapters.put(s, new EventsListAdapter(this));
			mEventsDetailsPagerAdapters
					.put(s, new EventsDetailsPagerAdapter(s));
		}

		mProblemsListPagerAdapter = new ProblemsListPagerAdapter(this);
		mProblemsListAdapters = new HashMap<TriggerSeverity, ProblemsListAdapter>(
				TriggerSeverity.values().length);
		mProblemsMainListAdapter = new ProblemsListAdapter(this);
		mProblemsDetailsPagerAdapters = new HashMap<TriggerSeverity, ProblemsDetailsPagerAdapter>(
				TriggerSeverity.values().length);

		for (TriggerSeverity s : TriggerSeverity.values()) {
			mProblemsListAdapters.put(s, new ProblemsListAdapter(this));
			mProblemsDetailsPagerAdapters.put(s,
					new ProblemsDetailsPagerAdapter(s));
		}

		mHostGroupsSpinnerAdapter = new HostGroupsSpinnerAdapter(this);

		mHostsListAdapter = new HostsListAdapter(this);
		mChecksApplicationsPagerAdapter = new ChecksApplicationsPagerAdapter();
		mChecksItemsListAdapters = new HashMap<String,ChecksItemsListAdapter>();

		mScreensListAdapter = new ScreensListAdapter(this);
	}

	/**
	 * Loads all events with a given severity and host group from the database
	 * asynchronously. After loading the events, the corresponding adapters are
	 * updated. If necessary, an import from the Zabbix API is triggered.
	 *
	 * @param hostGroupId
	 *            host group id by which the events will be filtered
	 * @param hostGroupChanged
	 *            whether the host group has changed. If this is true, the
	 *            adapters will be cleared before being filled with entries
	 *            matching the selected host group.
	 * @param callback
	 *            listener to be called when the adapters have been updated
	 */
	public RemoteAPITask loadEventsByHostGroup(final long hostGroupId,
			final boolean hostGroupChanged,
			final OnEventsListsLoadedListener callback) {

//		cancelTask(mCurrentLoadEventsTask);

		RemoteAPITask mCurrentLoadEventsTask = new RemoteAPITask(mRemoteAPI) {

			private Map<TriggerSeverity, List<Event>> events;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				events = new HashMap<>();
				try {
					mRemoteAPI.importEvents(this);
				} finally {
					// even if the api call is not successful, we can still use
					// the cached events
					for (TriggerSeverity severity : TriggerSeverity.values()) {
						events.put(severity, mDatabaseHelper
								.getEventsBySeverityAndHostGroupId(severity,
										hostGroupId));
					}
				}
			}

			@Override
			protected void onPostExecute(Void result) {
			   super.onPostExecute(result);
			   for (TriggerSeverity severity : TriggerSeverity.values()) {
					   BaseServiceAdapter<Event> listAdapter = mEventsListAdapters
									   .get(severity);
					   BaseSeverityPagerAdapter<Event> detailsAdapter = mEventsDetailsPagerAdapters
									   .get(severity);
					   if (listAdapter != null) {
							   if (hostGroupChanged)
									   listAdapter.clear();
							   listAdapter.addAll(events.get(severity));
							   listAdapter.notifyDataSetChanged();
					   }

					   if (detailsAdapter != null) {
							   if (hostGroupChanged)
									   detailsAdapter.clear();
							   detailsAdapter.addAll(events.get(severity));
							   detailsAdapter.notifyDataSetChanged();
					   }

					   if (callback != null)
							   callback.onEventsListsLoaded(severity,
									   hostGroupChanged);
			   }
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				if (callback != null)
					callback.onSeverityListAdapterProgressUpdate(values[0]);
			}

		};

		mCurrentLoadEventsTask.execute();
		return mCurrentLoadEventsTask;
	}

	/**
	 * Loads all triggers with a given severity and host group from the database
	 * asynchronously. After loading the events, the corresponding adapters are
	 * updated. If necessary, an import from the Zabbix API is triggered.
	 *
	 * @param hostGroupId
	 *            host group id by which the events will be filtered
	 * @param hostGroupChanged
	 *            whether the host group has changed. If this is true, the
	 *            adapters will be cleared before being filled with entries
	 *            matching the selected host group.
	 * @param callback
	 *            listener to be called when the adapters have been updated
	 */
	public RemoteAPITask loadProblemsByHostGroup(final long hostGroupId,
			final boolean hostGroupChanged,
			final OnProblemListLoadedListener callback) {

		RemoteAPITask currentLoadProblemsTask =
				new LoadProblemsTask(mRemoteAPI,mDatabaseHelper,hostGroupId, callback, hostGroupChanged);

		currentLoadProblemsTask.execute();
		return currentLoadProblemsTask;
	}

	/**
	 * Loads all hosts with a given host group from the database asynchronously.
	 * After loading the hosts, the host list adapter is updated. If necessary,
	 * an import from the Zabbix API is triggered.
	 *
	 * Additionally, this method initializes
	 * {@link ChecksApplicationsPagerAdapter}s (one for each host) if necessary.
	 *
	 * @param hostGroupId
	 *            host group id by which the events will be filtered
	 * @param hostGroupChanged
	 *            whether the host group has changed. If this is true, the
	 *            adapter will be cleared before being filled with entries
	 *            matching the selected host group.
	 * @param callback
	 *            listener to be called when the adapter has been updated
	 */
	public void loadHostsByHostGroup(final long hostGroupId,
			final boolean hostGroupChanged, final OnHostsLoadedListener callback) {
		new RemoteAPITask(mRemoteAPI) {

			private List<Host> hosts;
			private final BaseServiceAdapter<Host> hostsAdapter = mHostsListAdapter;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				hosts = new ArrayList<Host>();
				try {
					mRemoteAPI.importHostsAndGroups();
					// even if the api call is not successful, we can still use
					// the cached events
				} finally {
					hosts = mDatabaseHelper.getHostsByHostGroup(hostGroupId);
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (hostsAdapter != null) {
					if (hostGroupChanged)
						hostsAdapter.clear();
					hostsAdapter.addAll(hosts);
					hostsAdapter.notifyDataSetChanged();
				}
				if (callback != null)
					callback.onHostsLoaded();
				Log.d(TAG, "Hosts imported.");

			}

		}.execute();
	}

	/**
	 * Loads all hosts and host groups from the database asynchronously. After
	 * loading, the host groups spinner adapter is updated. If necessary, an
	 * import from the Zabbix API is triggered.
	 *
	 */
	public void loadHostsAndHostGroups() {
		new RemoteAPITask(mRemoteAPI) {

			private List<HostGroup> hostGroups;
			private final BaseServiceAdapter<HostGroup> hostGroupsAdapter = mHostGroupsSpinnerAdapter;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				hostGroups = new ArrayList<HostGroup>();
				try {
					mRemoteAPI.importHostsAndGroups();
					// even if the api call is not successful, we can still use
					// the cached events
				} finally {
					hostGroups = mDatabaseHelper.getHostGroups(mCurrentZabbixServerId);
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (hostGroupsAdapter != null) {
					hostGroupsAdapter.clear();
					hostGroupsAdapter.addAll(hostGroups);
					hostGroupsAdapter.notifyDataSetChanged();
				}
				Log.d(TAG, "Hosts and host groups imported.");

			}

		}.execute();
	}

	/**
	 * Loads all applications with a given host group from the database
	 * asynchronously. After loading the applications, the corresponding
	 * adapters are updated. If necessary, an import from the Zabbix API is
	 * triggered.
	 *
	 * Attention: This call needs to take care of loading all items inside the
	 * applications as well. Otherwise they will not be available when
	 * {@link ZabbixDataService#loadItemsByApplicationId(long, OnItemsLoadedListener)}
	 * is called.
	 *
	 * @param hostId
	 *            host id
	 * @param callback
	 *            Callback needed to trigger a redraw the view pager indicator
	 * @param resetSelection
	 *            flag indicating if the application selection shall be reset
	 *            when the data is loaded
	 */
	public RemoteAPITask loadApplicationsByHostId(final long hostId,
			final OnApplicationsLoadedListener callback,
			final boolean resetSelection) {

//		cancelTask(currentLoadApplicationsTask);
		RemoteAPITask currentLoadApplicationsTask = new RemoteAPITask(mRemoteAPI) {

			List<Application> applications;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				try {
					// We only import applications with corresponding hosts
					// (this way templates are ignored)
					mRemoteAPI.importApplicationsByHostId(hostId, this);
					mRemoteAPI.importItemsByHostId(hostId, this);
				} finally {
					applications = mDatabaseHelper
							.getApplicationsByHostId(hostId);

				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// fill adapter
				if (mChecksApplicationsPagerAdapter != null) {
					mChecksApplicationsPagerAdapter.clear();
					mChecksApplicationsPagerAdapter.addAll(applications);
					mChecksApplicationsPagerAdapter.notifyDataSetChanged();

					if (callback != null)
						callback.onApplicationsLoaded(resetSelection);
				}

			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				if(values[0] % 10 == 0){
					Log.d(TAG, "progress: " + values[0]);
				}
				if (callback != null)
					callback.onApplicationsProgressUpdate(values[0]);
			}

		};

		currentLoadApplicationsTask.execute();
		return currentLoadApplicationsTask;
	}

	/**
	 * Loads all items in a given application from the database asynchronously.
	 * After loading the items, the corresponding adapters are updated. An
	 * import from Zabbix is not necessary, because the items have already been
	 * loaded together with the applications.
	 *
	 */
	public RemoteAPITask loadItemsByApplicationId(final long applicationId,
			final OnItemsLoadedListener callback) {
//		cancelTask(currentLoadItemsTask);
		RemoteAPITask currentLoadItemsTask = new RemoteAPITask(mRemoteAPI) {

			List<Item> items;
			Application application;

//			@Override
//			protected void onPreExecute() {
//				super.onPreExecute();
				// clear adapters first to avoid display of old items
//				if (mChecksItemsListAdapters != null) {
//					mChecksItemsListAdapters.clear();
//					mChecksItemsListAdapters.notifyDataSetChanged();
//				}
//			}

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				items = mDatabaseHelper.getItemsByApplicationId(applicationId);
				application = mDatabaseHelper.getApplicationById(applicationId);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				// fill adapters
				if(application != null){

					ChecksItemsListAdapter checksItemsListAdapter
							= mChecksItemsListAdapters.get(application);

					if (checksItemsListAdapter == null){
						checksItemsListAdapter = new ChecksItemsListAdapter(ZabbixDataService.this);
					}

					checksItemsListAdapter.clear();
					checksItemsListAdapter.addAll(items);
					checksItemsListAdapter.notifyDataSetChanged();
					mChecksItemsListAdapters.put(application.toString(), checksItemsListAdapter);

					if (callback != null) {
						callback.onItemsLoaded();
					}
				}
			}

		};
		currentLoadItemsTask.execute();
		return currentLoadItemsTask;
	}

	public void loadZabbixServers() {
		List<ZabbixServer> serverSet = mDatabaseHelper.getZabbixServers();

		mServersListSelectionAdapter.clear();
		mServersListSelectionAdapter.addAll(serverSet);

		mServersListManagementAdapter.clear();
		mServersListManagementAdapter.addAll(serverSet);
	}

	/**
	 * Loads all history details for a given item. If necessary, an import from
	 * the Zabbix API is triggered.
	 *
	 * @param item
	 */
	public RemoteAPITask loadHistoryDetailsByItem(final Item item,
			boolean cancelPreviousTasks,
			final OnGraphDataLoadedListener callback) {
		Log.d(TAG, "Loading history for item " + item.toString());

		RemoteAPITask currentTask = new RemoteAPITask(mRemoteAPI) {

			List<HistoryDetail> historyDetails;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				try {
					mRemoteAPI.importHistoryDetails(item.getId(), this);
				} finally {
					historyDetails = mDatabaseHelper
							.getHistoryDetailsByItemId(item.getId());

				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				item.setHistoryDetails(historyDetails);

				if (callback != null)
					callback.onGraphDataLoaded();
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				if (callback != null)
					callback.onGraphDataProgressUpdate(values[0]);
			}

		};

		currentTask.execute();
		return currentTask;
	}

	/**
	 * Loads all screens. If necessary, an import from the Zabbix API is
	 * triggered.
	 *
	 */
	public void loadScreens(final OnScreensLoadedListener callback) {
		new RemoteAPITask(mRemoteAPI) {

			List<Screen> screens;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				try {
					mRemoteAPI.importScreens();
				} finally {
					screens = mDatabaseHelper.getScreens(mCurrentZabbixServerId);
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				if (mScreensListAdapter != null) {
					mScreensListAdapter.clear();
					mScreensListAdapter.addAll(screens);
					mScreensListAdapter.notifyDataSetChanged();
				}

				if (callback != null)
					callback.onScreensLoaded();
			}

		}.execute();
	}

	/**
	 * Loads graphs belonging to a screen. If necessary, an import from the
	 * Zabbix API is triggered.
	 *
	 * @param screen
	 * @param callback
	 */
	public RemoteAPITask loadGraphsByScreen(final Screen screen,
			final OnGraphsLoadedListener callback) {
		RemoteAPITask currentLoadGraphsTask = new RemoteAPITask(mRemoteAPI) {

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				mRemoteAPI.importGraphsByScreen(screen);
				screen.setGraphs(mDatabaseHelper.getGraphsByScreen(screen));
				int numGraphs = screen.getGraphs().size();
				int j = 0;
				for (Graph g : screen.getGraphs()) {
					int numItems = g.getGraphItems().size();
					int k = 0;
					for (GraphItem gi : g.getGraphItems()) {
						Item item = gi.getItem();
						mRemoteAPI.importHistoryDetails(item.getId(), null);
						item.setHistoryDetails(mDatabaseHelper
								.getHistoryDetailsByItemId(item.getId()));
						k++;
						if (numItems > 0 && numGraphs > 0)
							updateProgress((100 * j + ((100 * k) / numItems))
									/ numGraphs);
					}
					j++;
					// updateProgress((100 * j) / numGraphs);
				}
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				if (callback != null)
					callback.onGraphsProgressUpdate(values[0]);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				if (callback != null)
					callback.onGraphsLoaded();
			}

		};
		currentLoadGraphsTask.execute();
		return currentLoadGraphsTask;
	}

	/**
	 * Loads a graph's data from the database and triggers an import from Zabbix
	 * if necessary (i.e. the graph is not cached).
	 *
	 * All graph items in the given graph are filled with the corresponding
	 * history details from the local database / the Zabbix server.
	 *
	 * @param graph
	 *            the graph to be filled with data
	 * @param callback
	 */
	public void loadGraph(final Graph graph,
			final OnGraphsLoadedListener callback) {
		new RemoteAPITask(mRemoteAPI) {

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				for (GraphItem gi : graph.getGraphItems()) {
					Item item = gi.getItem();
					mRemoteAPI.importHistoryDetails(item.getId(), this);
					item.setHistoryDetails(mDatabaseHelper
							.getHistoryDetailsByItemId(item.getId()));
				}
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);

				if (callback != null)
					callback.onGraphsLoaded();
			}

		}.execute();
	}

	public void cancelTask(RemoteAPITask task) {
		if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
			if (task.cancel(true))
				Log.d(TAG, "Cancelled task: " + task);
			else
				Log.d(TAG, "Task was already done: " + task);
		}
	}

	/**
	 * Acknowledges an event.
	 *
	 * @param event
	 *            the event
	 * @param comment
	 *            an optional comment
	 * @param callback
	 *            callback to be notified when the acknowledgement was
	 *            successful
	 */
	public void acknowledgeEvent(final Event event, final String comment,
			final OnAcknowledgeEventListener callback) {
		new RemoteAPITask(mRemoteAPI) {

			private boolean mSuccess = false;

			@Override
			protected void executeTask() throws ZabbixLoginRequiredException,
					FatalException {
				if (event == null)
					return;
				mRemoteAPI.acknowledgeEvent(event.getId(), comment);
				mSuccess = mDatabaseHelper.acknowledgeEvent(event);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (mSuccess && callback != null)
					callback.onEventAcknowledged();
			}

		}.execute();
	}

	/**
	 * Sets the activity context, which is needed to inflate layout elements.
	 * This also initializes the layout inflater
	 * {@link ZabbixDataService#mInflater}.
	 *
	 * @param context
	 *            the context
	 */
	public void setActivityContext(Context context) {
		this.mActivityContext = context;
		this.mInflater = (LayoutInflater) mActivityContext
				.getSystemService(LAYOUT_INFLATER_SERVICE);
	}

	public LayoutInflater getInflater() {
		return mInflater;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigChanged");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mBindings--;
		Log.d(TAG, "onUnbind: " + mBindings);
		return super.onUnbind(intent);
	}

	public Graph getGraphById(long graphId) {
		return mDatabaseHelper.getGraphById(graphId);
	}

	public void performZabbixLogout() {
		mRemoteAPI.logout();
	}

	public void initConnection() {
		mRemoteAPI.initConnection();
	}

	public ZabbixServer createNewZabbixServer(String name) {
		ZabbixServer srv = new ZabbixServer();
		srv.setName(name);
		mDatabaseHelper.insertZabbixServer(srv);

		loadZabbixServers();

		return srv;
	}

	public void updateZabbixServer(ZabbixServer zabbixServer) {
		mDatabaseHelper.updateZabbixServer(zabbixServer);
	}

	public void removeZabbixServer(ZabbixServer item) {
		mDatabaseHelper.deleteZabbixServer(item);
		loadZabbixServers();
		mServersListManagementAdapter.notifyDataSetChanged();
	}

}
