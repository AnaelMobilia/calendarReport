/*
* Copyright 2017 Anael Mobilia
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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
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
    // URI
    Uri monURI = CalendarContract.Calendars.CONTENT_URI;
    // Liste des calendriers de l'appareil
    String[] lesCalendriers;
    // ID des calendriers
    String[] idCalendriers;
    // ID du calendrier selectionné
    String[] idCalendrierVoulu;

    // Gestion des dates
    // MOIS en base 0 !
    int debAnnee = 2017;
    int debMois = 1;
    int debJour = 1;
    int finAnnee = 2017;
    int finMois = 11;
    int finJour = 31;

    // Type de tri
    boolean triByDuree = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });


        // Chargement des données sur les calendriers
        monContentResolver = getContentResolver();

        // Vérification du droit de lire le calendrier
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.e("onCreate", "demande des droits accès calendrier");
            // Sinon, demande des droits
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_CALENDAR }, DROIT_LECTURE_CALENDRIER);
        } else {
            Log.e("onCreate", "droits accès calendrier déjà autorisés");
            afficherListeCalendriers();
        }

        // Mise à jour des bornes de dates
        updateDateDeb();
        updateDateFin();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case DROIT_LECTURE_CALENDRIER: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("onRequestPermissionsRes", "OKKKK - DROIT_LECTURE_CALENDRIER");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    afficherListeCalendriers();
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
     * Peuplement de la liste des calendriers
     */
    public void afficherListeCalendriers() {
        // Données à récupérer sur les calendriers
        String[] EVENT_PROJECTION = new String[]{ CalendarContract.Calendars._ID, CalendarContract.Calendars
                .CALENDAR_DISPLAY_NAME };

        // Récupération de la liste des calendriers
        Cursor monCursor = monContentResolver.query(monURI, EVENT_PROJECTION, null, null, null);
        ArrayList<String> listeCalendriers = new ArrayList<>();
        ArrayList<String> listeIDCalendriers = new ArrayList<>();
        while (monCursor.moveToNext()) {
            // ID
            listeIDCalendriers.add(String.valueOf(monCursor.getInt(0)));
            // Nom d'affichage
            listeCalendriers.add(monCursor.getString(1));
        }
        monCursor.close();
        lesCalendriers = listeCalendriers.toArray(new String[0]);
        idCalendriers = listeIDCalendriers.toArray(new String[0]);

        // Récupération de l'objet graphique
        Spinner listeCalendriersSpinner = (Spinner) findViewById(R.id.listeCalendrier);
        // Injection des valeurs
        ArrayAdapter<String> monAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lesCalendriers);
        // Affichage
        listeCalendriersSpinner.setAdapter(monAdapter);

        // Gestion de la sélection
        listeCalendriersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.w("onItemSelectedListener", lesCalendriers[i] + " -> " + idCalendriers[i]);
                // Mise à jour de l'ID du calendrier choisi
                idCalendrierVoulu = new String[]{ idCalendriers[i] };
                // Lance l'affichage des stats
                afficherStats();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * Modification de la date de début - Clic sur boutton
     *
     * @param view
     */
    public void setDateDebut(View view) {
        showDialog(10);
    }

    /**
     * Modification de la date de fin - Clic sur boutton
     *
     * @param view
     */
    public void setDateFin(View view) {
        showDialog(11);
    }

    /**
     * Modification du tri - Clic sur le boutton
     *
     * @param view
     */
    public void setTri(View view) {
        // Inversion du tri
        triByDuree = !triByDuree;
        // MàJ de l'affichage
        afficherStats();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == 10) {
            return new DatePickerDialog(this, dateListenerDebut, debAnnee, debMois, debJour);
        } else if (id == 11) {
            return new DatePickerDialog(this, dateListenerFin, finAnnee, finMois, finJour);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener dateListenerDebut = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            // arg1 = year
            // arg2 = month
            // arg3 = day
            debAnnee = arg1;
            debMois = arg2;
            debJour = arg3;
            // MàJ affichage
            updateDateDeb();
        }
    };
    private DatePickerDialog.OnDateSetListener dateListenerFin = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            // arg1 = year
            // arg2 = month
            // arg3 = day
            finAnnee = arg1;
            finMois = arg2;
            finJour = arg3;
            // MàJ affichage
            updateDateFin();
        }
    };

    /**
     * Mise à jour de la borne de début
     */
    private void updateDateDeb() {
        TextView dateDeb = (TextView) findViewById(R.id.texteDateDebut);
        dateDeb.setText("Du : " + debJour + "/" + (debMois + (int) 1) + "/" + debAnnee);
        // Recalcul des stats
        afficherStats();
    }

    /*
     * Mise à jour de la borne de fin
     */
    private void updateDateFin() {
        TextView dateDeb = (TextView) findViewById(R.id.texteDateFin);
        dateDeb.setText("Au : " + finJour + "/" + (finMois + (int) 1) + "/" + finAnnee);
        // Recalcul des stats
        afficherStats();
    }


    /**
     * Affiche les stats du calendrier selectionné
     * Read : https://www.reddit.com/r/androiddev/comments/2da207/getting_events_from_a_specific_calendar_and/
     */
    public void afficherStats() {
        // Données à récupérer sur les événements du calendrier
        String[] EVENT_PROJECTION = new String[]{ CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END };

        // Filtre sur les dates
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();

        Calendar beginTime = Calendar.getInstance();
        beginTime.set(debAnnee, debMois, debJour, 8, 0);
        long startMills = beginTime.getTimeInMillis();

        Calendar endTime = Calendar.getInstance();
        endTime.set(finAnnee, finMois, finJour, 20, 0);
        long endMills = endTime.getTimeInMillis();

        ContentUris.appendId(builder, startMills);
        ContentUris.appendId(builder, endMills);

        // Récupération de la liste des événements
        Cursor monCursor = monContentResolver.query(builder.build(), EVENT_PROJECTION,
                                                    CalendarContract.Instances.CALENDAR_ID + " = ?", idCalendrierVoulu, null);
        HashMap<String, Long> stats = new HashMap<String, Long>();
        while (monCursor.moveToNext()) {
            // Type de l'événement
            String monType = monCursor.getString(0);

            // Création de la ligne si inexistante
            if (!stats.containsKey(monType)) {
                stats.put(monType, 0L);
            }

            // Durée déjà existante
            Long maDuree = stats.get(monType);

            // Ajout du temps de l'événement (Fin - Début)
            Long laDuree = (monCursor.getLong(2) - monCursor.getLong(1)) / 1000 / 60;
            maDuree += laDuree;

            // Stockage
            stats.remove(monType);
            stats.put(monType, maDuree);

            Log.w("afficherStats",
                  "" + monCursor.getString(0) + " - " + new Date(monCursor.getLong(1)) + " - " + new Date(monCursor.getLong(2))
                  + " => " + laDuree);
        }
        monCursor.close();

        // Gestion des tris
        Map<String, Long> stats2;
        if (triByDuree) {
            // Tri par durée DESC
            stats2 = sortByValue(stats);
        } else {
            // tri par nom ASC
            stats2 = new TreeMap<String, Long>(stats);
        }

        // Statistiques
        TextView mesStats = (TextView) findViewById(R.id.texteStats);
        mesStats.setText("");
        for (HashMap.Entry<String, Long> entry : stats2.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();

            mesStats.append(key + " -> " + value / 60 + "\n");
        }
    }

    /**
     * Sort HashMap by Value
     * https://www.mkyong.com/java/how-to-sort-a-map-in-java/
     *
     * @param unsortMap
     * @return
     */
    private static Map<String, Long> sortByValue(Map<String, Long> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Long>> list = new LinkedList<Map.Entry<String, Long>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Long> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
