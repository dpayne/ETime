package com.etime;

/*
 *  This file is part of ETime.
 *
 *  ETime is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ETime is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with ETime.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * User: dpayne2
 * Date: 1/3/12
 * Time: 9:07 PM
 */
public class ETimePreferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
