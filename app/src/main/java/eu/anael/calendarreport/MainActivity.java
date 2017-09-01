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
import android.content.ContentResolver;
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
import android.widget.Spinner;

import java.util.ArrayList;

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
    Integer[] idCalendriers;
    // ID du calendrier selectionné
    Integer idCalendrierVoulu;

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
        String[] EVENT_PROJECTION = new String[]{ CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME };

        // Récupération de la liste des calendriers
        Cursor monCursor = monContentResolver.query(monURI, EVENT_PROJECTION, null, null, null);
        ArrayList<String> listeCalendriers = new ArrayList<>();
        ArrayList<Integer> listeIDCalendriers = new ArrayList<>();
        while (monCursor.moveToNext()) {
            // ID
            listeIDCalendriers.add(monCursor.getInt(0));
            // Nom d'affichage
            listeCalendriers.add(monCursor.getString(1));
        }
        lesCalendriers = listeCalendriers.toArray(new String[0]);
        idCalendriers = listeIDCalendriers.toArray(new Integer[0]);

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
                Log.d("onItemSelectedListener", lesCalendriers[i] + " -> " + idCalendriers[i]);
                // Mise à jour de l'ID du calendrier choisi
                idCalendrierVoulu = idCalendriers[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

}
