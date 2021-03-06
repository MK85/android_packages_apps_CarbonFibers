/*
 * Copyright (C) 2012 Slimroms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carbon.settings.fragments.inter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.carbon.settings.R;
import com.carbon.settings.SettingsPreferenceFragment;
import com.carbon.settings.Utils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Date;

public class InterfaceRecents extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Recents";

    private static final String KEY_RECENTS_ASSIST = "recents_target_assist";
    private static final String RAM_BAR_MODE = "ram_bar_mode";
    private static final String RAM_BAR_COLOR_APP_MEM = "ram_bar_color_app_mem";
    private static final String RAM_BAR_COLOR_CACHE_MEM = "ram_bar_color_cache_mem";
    private static final String RAM_BAR_COLOR_TOTAL_MEM = "ram_bar_color_total_mem";
    private static final String KEY_CLEAR_RECENTS_POSITION = "clear_recents_position";

    private static final String EXPLANATION_URL = "http://www.slimroms.net/index.php/faq/slimbean/238-why-do-i-have-less-memory-free-on-my-device";

    static final int DEFAULT_MEM_COLOR = 0xff8d8d8d;
    static final int DEFAULT_CACHE_COLOR = 0xff00aa00;
    static final int DEFAULT_ACTIVE_APPS_COLOR = 0xff33b5e5;

    private CheckBoxPreference mShowAssistButton;
    private ListPreference mRamBarMode;
    private ColorPickerPreference mRamBarAppMemColor;
    private ColorPickerPreference mRamBarCacheMemColor;
    private ColorPickerPreference mRamBarTotalMemColor;
    private ListPreference mClearPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int intColor;
        String hexColor;

        addPreferencesFromResource(R.xml.interface_recents);

        PreferenceScreen prefSet = getPreferenceScreen();

        mShowAssistButton = (CheckBoxPreference) findPreference(KEY_RECENTS_ASSIST);
        mShowAssistButton.setChecked(Settings.System.getInt(mContentRes,
                Settings.System.RECENTS_TARGET_ASSIST, 0) == 1);

        mRamBarMode = (ListPreference) prefSet.findPreference(RAM_BAR_MODE);
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        mRamBarMode.setValue(String.valueOf(ramBarMode));
        mRamBarMode.setSummary(mRamBarMode.getEntry());
        mRamBarMode.setOnPreferenceChangeListener(this);

        mRamBarAppMemColor = (ColorPickerPreference) findPreference(RAM_BAR_COLOR_APP_MEM);
        mRamBarAppMemColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_ACTIVE_APPS_COLOR, DEFAULT_ACTIVE_APPS_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mRamBarAppMemColor.setSummary(hexColor);
        mRamBarAppMemColor.setNewPreviewColor(intColor);

        mRamBarCacheMemColor = (ColorPickerPreference) findPreference(RAM_BAR_COLOR_CACHE_MEM);
        mRamBarCacheMemColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_CACHE_COLOR, DEFAULT_CACHE_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mRamBarCacheMemColor.setSummary(hexColor);
        mRamBarCacheMemColor.setNewPreviewColor(intColor);

        mRamBarTotalMemColor = (ColorPickerPreference) findPreference(RAM_BAR_COLOR_TOTAL_MEM);
        mRamBarTotalMemColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_MEM_COLOR, DEFAULT_MEM_COLOR);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mRamBarTotalMemColor.setSummary(hexColor);
        mRamBarTotalMemColor.setNewPreviewColor(intColor);

        mClearPosition = (ListPreference) findPreference(KEY_CLEAR_RECENTS_POSITION);
        int ClearSide = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.CLEAR_RECENTS_POSITION, 1);
        mClearPosition.setValue(String.valueOf(ClearSide));
        mClearPosition.setSummary(mClearPosition.getEntry());
        mClearPosition.setOnPreferenceChangeListener(this);

        updateRamBarOptions();
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.ram_bar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(EXPLANATION_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return true;
            case R.id.reset:
                ramBarColorReset();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mRamBarMode) {
            int ramBarMode = Integer.valueOf((String) newValue);
            int index = mRamBarMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.RECENTS_RAM_BAR_MODE, ramBarMode);
            mRamBarMode.setSummary(mRamBarMode.getEntries()[index]);
            updateRamBarOptions();
            return true;
        } else if (preference == mRamBarAppMemColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_ACTIVE_APPS_COLOR, intHex);
            return true;
        } else if (preference == mRamBarCacheMemColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_CACHE_COLOR, intHex);
            return true;
        } else if (preference == mRamBarTotalMemColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentAppRes,
                    Settings.System.RECENTS_RAM_BAR_MEM_COLOR, intHex);
            return true;
        } else if (preference == mClearPosition) {
            int position = Integer.valueOf((String) newValue);
            int index = mClearPosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CLEAR_RECENTS_POSITION, position);
            mClearPosition.setSummary(mClearPosition.getEntries()[index]);
            return true;
         }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mShowAssistButton) {
            Settings.System.putInt(mContentAppRes,
                    Settings.System.RECENTS_TARGET_ASSIST,
            mShowAssistButton.isChecked() ? 1 : 0 );
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void ramBarColorReset() {
        Settings.System.putInt(mContentAppRes,
                Settings.System.RECENTS_RAM_BAR_ACTIVE_APPS_COLOR, DEFAULT_ACTIVE_APPS_COLOR);
        Settings.System.putInt(mContentAppRes,
                Settings.System.RECENTS_RAM_BAR_CACHE_COLOR, DEFAULT_CACHE_COLOR);
        Settings.System.putInt(mContentAppRes,
                Settings.System.RECENTS_RAM_BAR_MEM_COLOR, DEFAULT_MEM_COLOR);

        mRamBarAppMemColor.setNewPreviewColor(DEFAULT_ACTIVE_APPS_COLOR);
        mRamBarCacheMemColor.setNewPreviewColor(DEFAULT_CACHE_COLOR);
        mRamBarTotalMemColor.setNewPreviewColor(DEFAULT_MEM_COLOR);
        String hexColor = String.format("#%08x", (0xffffffff & DEFAULT_ACTIVE_APPS_COLOR));
        mRamBarAppMemColor.setSummary(hexColor);
        hexColor = String.format("#%08x", (0xffffffff & DEFAULT_ACTIVE_APPS_COLOR));
        mRamBarCacheMemColor.setSummary(hexColor);
        hexColor = String.format("#%08x", (0xffffffff & DEFAULT_MEM_COLOR));
        mRamBarTotalMemColor.setSummary(hexColor);
    }


    private void updateRamBarOptions() {
        int ramBarMode = Settings.System.getInt(mContentAppRes,
               Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode == 0) {
            mRamBarAppMemColor.setEnabled(false);
            mRamBarCacheMemColor.setEnabled(false);
            mRamBarTotalMemColor.setEnabled(false);
        } else if (ramBarMode == 1) {
            mRamBarAppMemColor.setEnabled(true);
            mRamBarCacheMemColor.setEnabled(false);
            mRamBarTotalMemColor.setEnabled(false);
        } else if (ramBarMode == 2) {
            mRamBarAppMemColor.setEnabled(true);
            mRamBarCacheMemColor.setEnabled(true);
            mRamBarTotalMemColor.setEnabled(false);
        } else {
            mRamBarAppMemColor.setEnabled(true);
            mRamBarCacheMemColor.setEnabled(true);
            mRamBarTotalMemColor.setEnabled(true);
        }
    }

}
