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

import android.os.Bundle;
import android.view.View;

import com.inovex.zabbixmobile.R;
import com.inovex.zabbixmobile.activities.BaseActivity;
import com.inovex.zabbixmobile.model.Trigger;

/**
 * Fragment displaying a list of problems for a particular severity.
 * 
 */
public class ProblemsListPage extends BaseSeverityFilterListPage<Trigger> {

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setCustomEmptyText(getResources().getString(
				R.string.empty_list_problems));
	}

	@Override
	protected void setupListAdapter() {
		mListAdapter = mZabbixDataService.getProblemsListAdapter(((BaseActivity)this.getActivity()).getPersistedServerSelection(),mSeverity);
		setListAdapter(mListAdapter);
	}

}
