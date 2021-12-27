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

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Paramètrages de l'application
 */
public class SettingsActivity extends AppCompatActivity {


    // Liste des calendriers de l'appareil
    String[] lesCalendriers;
    // ID des calendriers
    Integer[] idCalendriers;
    // ID du calendrier selectionné
    int idCalendrierVoulu;

    // Gestion des dates
    // MOIS en base 0 !
    int debAnnee;
    int debMois;
    int debJour;
    int finAnnee;
    int finMois;
    int finJour;

    // Gestion du séparateur Group By
    String separateur;

    // Objet GUI
    Spinner listeCalendriersSpinner;
    Button buttonDebut;
    Button buttonFin;
    EditText texteSeparateur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bouton back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Définition du listener pour les calendriers
        listeCalendriersSpinner = findViewById(R.id.listeCalendrier);
        listeCalendriersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.w("onItemSelectedListener", lesCalendriers[i] + " -> " + idCalendriers[i]);
                // Mise à jour de l'ID du calendrier choisi
                idCalendrierVoulu = idCalendriers[i];
                // Préférence
                Utils.setPrefInt(getApplicationContext(), R.string.idOptionCalendrier, idCalendrierVoulu);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Définition du listener pour les datePicket
        buttonDebut = findViewById(R.id.buttonDateDebut);
        buttonDebut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(10);
            }
        });
        buttonFin = findViewById(R.id.buttonDateFin);
        buttonFin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(11);
            }
        });

        // Définition du listener pour le séparateur
        texteSeparateur = findViewById(R.id.separateurGroupBy);
        texteSeparateur.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Préférence
                Utils.setPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy, charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Chargement des valeurs si déjà existantes
        initialiserDates();
        initialiserCalendrier();
        initialiserSeparateurGroupBy();

        // Mise à jour de l'affichage
        updateAffichageDates();
        updateAffichageCalendrier();
        updateAffichageSeparateurGroupBy();
    }

    /**
     * Initilisation des valeurs des dates
     */
    private void initialiserDates() {
        // Début - par défaut 01/01/n-1
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutAn) != 0) {
            debAnnee = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutAn);
        } else {
            debAnnee = Calendar.getInstance().get(Calendar.YEAR) - 1;
        }
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutMois) != 0) {
            debMois = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutMois);
        } else {
            debMois = 0;
        }
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutJour) != 0) {
            debJour = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateDebutJour);
        } else {
            debJour = 1;
        }

        // Fin - par défaut : JJ/MM/YY
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinAn) != 0) {
            finAnnee = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinAn);
        } else {
            finAnnee = Calendar.getInstance().get(Calendar.YEAR);
        }
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinMois) != 0) {
            finMois = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinMois);
        } else {
            finMois = Calendar.getInstance().get(Calendar.MONTH);
        }
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinJour) != 0) {
            finJour = Utils.getPrefInt(getApplicationContext(), R.string.idOptionDateFinJour);
        } else {
            finJour = Calendar.getInstance().get(Calendar.DATE);
        }
    }

    private void initialiserCalendrier() {
        // URI
        Uri monURI = CalendarContract.Calendars.CONTENT_URI;
        // Initialisation du contentResolver
        ContentResolver monContentResolver = getContentResolver();

        // Données à récupérer sur les calendriers
        String[] EVENT_PROJECTION = new String[]{ CalendarContract.Calendars._ID, CalendarContract.Calendars
                .CALENDAR_DISPLAY_NAME };

        ArrayList<String> listeCalendriers = new ArrayList<>();
        ArrayList<Integer> listeIDCalendriers = new ArrayList<>();

        // Récupération de la liste des calendriers
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            Cursor monCursor = monContentResolver.query(monURI, EVENT_PROJECTION, null, null, null);
            while (monCursor.moveToNext()) {
                // ID
                listeIDCalendriers.add(monCursor.getInt(0));
                // Nom d'affichage
                listeCalendriers.add(monCursor.getString(1));
            }
            monCursor.close();
        }

        // Retour des valeurs [id - value]
        lesCalendriers = listeCalendriers.toArray(new String[0]);
        idCalendriers = listeIDCalendriers.toArray(new Integer[0]);

        // ID du calendrier choisi
        if (Utils.getPrefInt(getApplicationContext(), R.string.idOptionCalendrier) != 0) {
            idCalendrierVoulu = Utils.getPrefInt(getApplicationContext(), R.string.idOptionCalendrier);
        } else {
            idCalendrierVoulu = 0;
        }
    }

    private void initialiserSeparateurGroupBy() {
        if (!Utils.getPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy).equals("")) {
            separateur = Utils.getPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy);
        } else {
            separateur = ":";
        }
    }

    /**
     * Mise à jour (GUI + préférence) les dates
     */
    private void updateAffichageDates() {
        // Date début - Affichage
        buttonDebut.setText(debJour + "/" + (debMois + (int) 1) + "/" + debAnnee);
        // Date début - Préférence
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateDebutAn, debAnnee);
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateDebutMois, debMois);
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateDebutJour, debJour);

        // Date fin - Affichage
        buttonFin.setText(finJour + "/" + (finMois + (int) 1) + "/" + finAnnee);
        // Date fin - Préférence
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateFinAn, finAnnee);
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateFinMois, finMois);
        Utils.setPrefInt(getApplicationContext(), R.string.idOptionDateFinJour, finJour);
    }

    /**
     * Mise à jour (GUI + préférence) du calendrier
     */
    private void updateAffichageCalendrier() {
        // Injection des valeurs
        ArrayAdapter<String> monAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lesCalendriers);
        // Affichage
        listeCalendriersSpinner.setAdapter(monAdapter);

        // Définition du calendrier sélectionné
        // Récupération de la position dans l'ArrayList
        int monIndice = -1;
        int unIndice = 0;
        for (int unId : idCalendriers) {
            // Si c'est le bon ID
            if (unId == idCalendrierVoulu) {
                // Je sauvegarde l'indice
                monIndice = unIndice;
            }
            unIndice++;
        }
        // Si résultat...
        if (monIndice != -1) {
            // Sélection dans le spinner
            listeCalendriersSpinner.setSelection(monIndice);
        }
    }

    /**
     * Mise à jour (GUI + préférence) du supérateur pour le group by
     */
    private void updateAffichageSeparateurGroupBy() {
        // Injection de la valeur
        texteSeparateur.setText(separateur);

        Utils.setPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy, separateur);
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
            updateAffichageDates();
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
            updateAffichageDates();
        }
    };
}