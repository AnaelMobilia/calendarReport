/*
 * Copyright 2017-2022 Anael Mobilia
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

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    // ID demande de droit lecture calendrier
    private final int DROIT_LECTURE_CALENDRIER = 1;

    // Content Resolver
    ContentResolver monContentResolver;

    // Type de tri
    boolean triByDuree = true;
    FloatingActionButton fabTri;

    // Group By
    boolean groupBy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Chargement des données sur les calendriers
        monContentResolver = getContentResolver();

        // Vérification du droit de lire le calendrier
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.e("onCreate", "demande des droits accès calendrier");
            // Sinon, demande des droits
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, DROIT_LECTURE_CALENDRIER);
        } else {
            Log.e("onCreate", "droits accès calendrier déjà autorisés");
            afficherStats();
        }

        // Action du bouton de tri
        fabTri = findViewById(R.id.iconeTri);
        fabTri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setTri();
            }
        });

        FloatingActionButton groupBy = findViewById(R.id.iconeGroupBy);
        groupBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setGroupby();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Lancement de la configuration
            Intent monIntent = new Intent(this, SettingsActivity.class);
            startActivity(monIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Réponse à une demande de droits de l'application
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case DROIT_LECTURE_CALENDRIER: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("onRequestPermissionsRes", "OK - DROIT_LECTURE_CALENDRIER");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    afficherStats();
                } else {

                    Log.e("onRequestPermissionsRes", "Echec - DROIT_LECTURE_CALENDRIER");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Modification du tri - Clic sur le boutton
     */
    private void setTri() {
        // Inversion du tri
        triByDuree = !triByDuree;
        // MàJ de l'icône
        if (triByDuree) {
            fabTri.setImageResource(android.R.drawable.ic_menu_sort_alphabetically);
        } else {
            fabTri.setImageResource(android.R.drawable.ic_menu_sort_by_size);
        }
        // MàJ de l'affichage
        afficherStats();
    }

    /**
     * Modification de l'affichage - Group By
     */
    private void setGroupby() {
        // Inversion du Group By
        groupBy = !groupBy;

        // MàJ de l'affichage
        afficherStats();
    }

    /**
     * Affiche les stats du calendrier selectionné
     * Read : https://www.reddit.com/r/androiddev/comments/2da207/getting_events_from_a_specific_calendar_and/
     */
    private void afficherStats() {
        // Données à récupérer sur les événements du calendrier
        String[] EVENT_PROJECTION = new String[]{CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.ORGANIZER};

        // Filtre sur les dates
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();

        Calendar beginTime = Calendar.getInstance();
        beginTime.set(Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutAn),
                Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutMois),
                Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutJour), 0, 0);
        long startMills = beginTime.getTimeInMillis();

        Calendar endTime = Calendar.getInstance();
        endTime.set(Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinAn),
                Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinMois),
                Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinJour), 23, 59);
        long endMills = endTime.getTimeInMillis();

        ContentUris.appendId(builder, startMills);
        ContentUris.appendId(builder, endMills);

        // Récupération de la liste des événements
        Cursor monCursor = monContentResolver.query(builder.build(), EVENT_PROJECTION,
                CalendarContract.Instances.CALENDAR_ID + " = ?", new String[]{String.valueOf(
                        Utils.getPrefInt(getApplicationContext(), R.string.idOptionCalendrier))}, null);
        HashMap<String, Integer> stats = new HashMap<>();
        while (monCursor.moveToNext()) {
            // Type de l'événement
            String monType = monCursor.getString(0);

            String owner = monCursor.getString(3);
            Log.w("Owner : ", owner.toLowerCase().trim());
            // je ne prends que les rdv que j'ai créé => GTA
            //if (owner.toLowerCase().trim().contains("xxx@example.com")) {
            if(true) {
                // Gestion du groupBy
                if (groupBy) {
                    // Réduction du type de l'événement à sa partie avant ":"
                    int positionSeparateur = monType.indexOf(
                            Utils.getPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy));
                    // Si sous élément
                    if (positionSeparateur > 0) {
                        // Je ne prends que le début + trim pour éviter les effets de bord...
                        monType = monType.substring(0, positionSeparateur).trim();
                    }
                }

                // Création de la ligne si inexistante
                if (!stats.containsKey(monType)) {
                    stats.put(monType, 0);
                }

                // Durée déjà existante
                int maDuree = stats.get(monType);

                // Ajout du temps de l'événement (Fin - Début) + millisecondes -> secondes + secondes -> minutes
                int laDuree = (monCursor.getInt(2) - monCursor.getInt(1)) / 1000 / 60;
                maDuree += laDuree;

                // Stockage
                stats.remove(monType);
                stats.put(monType, maDuree);

                Log.w("afficherStats",
                        "" + monCursor.getString(0) + " - " + new Date(monCursor.getInt(1)) + " - " + new Date(monCursor.getInt(2))
                                + " => " + laDuree);
            }
        }

        monCursor.close();

        // Gestion des tris
        Map<String, Integer> stats2;
        if (triByDuree) {
            // Tri par durée DESC
            stats2 = sortByValue(stats);
        } else {
            // tri par nom ASC
            stats2 = new TreeMap<>(stats);
        }

        // Calcul du temps total
        int dureeTotale = 0;
        for (int uneDuree : stats2.values()) {
            // On additionne...
            dureeTotale += uneDuree;
        }

        // Statistiques
        TextView mesStats = (TextView) findViewById(R.id.texteStats);
        mesStats.setText("");
        for (HashMap.Entry<String, Integer> entry : stats2.entrySet()) {
            // Nom de l'item
            String key = entry.getKey();
            // Durée totale
            int value = entry.getValue();
            // Calcul du %age
            float percent = (float) value / (float) dureeTotale * 100.0f;

            mesStats.append(key + " -> " + Math.round(value / 60.0f) + " (" + Math.round(percent) + "%)\n");
        }
    }

    /**
     * Sort HashMap by Value
     * https://www.mkyong.com/java/how-to-sort-a-map-in-java/
     *
     * @param unsortMap
     * @return
     */
    private static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
