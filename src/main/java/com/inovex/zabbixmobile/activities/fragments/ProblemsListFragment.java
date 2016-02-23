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

import com.inovex.zabbixmobile.activities.BaseActivity;
import com.inovex.zabbixmobile.adapters.BaseSeverityListPagerAdapter;
import com.inovex.zabbixmobile.model.Trigger;

/**
 * Fragment displaying several lists of problems (one for each severity) using a
 * view pager.
 * 
 */
public class ProblemsListFragment extends
		BaseSeverityFilterListFragment<Trigger> {

	public static final String TAG = ProblemsListFragment.class.getSimpleName();

	@Override
	protected BaseSeverityListPagerAdapter<Trigger> retrievePagerAdapter() {
		return mZabbixDataService.getProblemsListPagerAdapter(((BaseActivity)getActivity()).getPersistedServerSelection());
	}

}
