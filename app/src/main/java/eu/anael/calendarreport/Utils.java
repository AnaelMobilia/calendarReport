/*
* Copyright 2017-2021 Anael Mobilia
*
* This file is part of calendarReport.
*
* calendarReport is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* calendarReport is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with calendarReport. If not, see <http://www.gnu.org/licenses/>
*/
package eu.anael.calendarreport;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utils {
    /**
     * Enregistrer une préférence
     *
     * @param idOption     id de l'option (Cf R.string.idOption...)
     * @param valeurOption
     */
    public static void setPrefInt(Context unContext, int idOption, int valeurOption) {
        // Initialisation du preferenceManager
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        SharedPreferences.Editor editor = mesPrefs.edit();
        editor.putInt(unContext.getString(idOption), valeurOption);
        editor.apply();
    }

    /**
     * Lire une préférence
     *
     * @param idOption
     * @return
     */
    public static int getPrefInt(Context unContext, int idOption) {
        // Initialisation du preferenceManager
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        return mesPrefs.getInt(unContext.getString(idOption), 0);
    }

    /**
     * Enregistrer une préférence
     *
     * @param idOption     id de l'option (Cf R.string.idOption...)
     * @param valeurOption
     */
    public static void setPrefString(Context unContext, int idOption, String valeurOption) {
        // Initialisation du preferenceManager
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        SharedPreferences.Editor editor = mesPrefs.edit();
        editor.putString(unContext.getString(idOption), valeurOption);
        editor.apply();
    }

    /**
     * Lire une préférence
     *
     * @param idOption
     * @return
     */
    public static String getPrefString(Context unContext, int idOption) {
        // Initialisation du preferenceManager
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        return mesPrefs.getString(unContext.getString(idOption), "");
    }
}
