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

import com.inovex.zabbixmobile.activities.BaseSeverityFilterActivity;
import com.inovex.zabbixmobile.adapters.BaseSeverityListPagerAdapter;
import com.inovex.zabbixmobile.adapters.ProblemsListPagerAdapter;
import com.inovex.zabbixmobile.model.Trigger;
import com.inovex.zabbixmobile.model.TriggerSeverity;

import java.util.List;
import java.util.Map;

/**
 * Fragment displaying several lists of problems (one for each severity) using a
 * view pager.
 * 
 */
public class ProblemsListFragment extends
		BaseSeverityFilterListFragment<Trigger> {

	public static final String TAG = ProblemsListFragment.class.getSimpleName();
	private Map<TriggerSeverity, List<Trigger>> content;

	@Override
	protected BaseSeverityListPagerAdapter<Trigger> retrievePagerAdapter() {
		this.mSeverityListPagerAdapter = new ProblemsListPagerAdapter(this.getActivity());
		return mSeverityListPagerAdapter;
	}

	public void setContent(Map<TriggerSeverity, List<Trigger>> content, int mTriggerPosition, boolean hostGroupChanged) {
		this.content = content;
//		this.mSeverityListPagerAdapter.addAll();

		this.mSeverityListPagerAdapter.updateTitle(
				this.mCurrentSeverity.getPosition(),
				content.get(mCurrentSeverity).size());

		if (mCurrentSeverity == TriggerSeverity.ALL && mTriggerPosition != -1) {
			selectItem(mTriggerPosition);
//			showDetailsFragment();
			mTriggerPosition = -1;
			return;
		} else if (mCurrentSeverity == mZabbixDataService.getProblemsListPagerAdapter().getCurrentObject()) {
			((BaseSeverityFilterActivity)getActivity()).selectInitialItem(hostGroupChanged);
		}
	}
}
