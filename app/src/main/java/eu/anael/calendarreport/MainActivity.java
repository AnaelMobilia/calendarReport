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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
        fabTri.setOnClickListener(view -> setTri());

        FloatingActionButton groupBy = findViewById(R.id.iconeGroupBy);
        groupBy.setOnClickListener(view -> setGroupby());
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
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == DROIT_LECTURE_CALENDRIER) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("onRequestPermissionsRes", "OK - DROIT_LECTURE_CALENDRIER");
                // permission was granted, yay! Do the contacts-related task you need to do.
                afficherStats();
            } else {
                Log.e("onRequestPermissionsRes", "Echec - DROIT_LECTURE_CALENDRIER");
                // permission denied, boo! Disable the functionality that depends on this permission.
            }
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
     * Calcul de la moyenne d'une série
     *
     * @param listeValeurs liste des valeurs
     * @param nbValeurs    nombre de valeurs (ne pas se baser sur la taille de valeurs)
     * @return moyenne
     */
    private float calculerMoyenne(ArrayList<Integer> listeValeurs, int nbValeurs) {
        // Gestion du cas de la division par zéro
        if (nbValeurs == 0) {
            return (float) 0;
        }

        int total = 0;
        for (int uneValeur : listeValeurs) {
            // On additionne...
            total += uneValeur;
        }

        return (float) (total / nbValeurs);
    }

    /**
     * Calcul de la médiane d'une série
     *
     * @param listeValeurs liste des valeurs
     * @param nbValeurs    nombre de valeurs (ne pas se baser sur la taille de valeurs)
     * @return médiane
     */
    private float calculerMediane(ArrayList<Integer> listeValeurs, int nbValeurs) {
        Collections.sort(listeValeurs);
        Log.e("xxx", listeValeurs.toString());
        int middle = nbValeurs / 2;
        if (nbValeurs % 2 == 1) {
            return listeValeurs.get(middle);
        } else {
            return (float) ((listeValeurs.get(middle - 1) + listeValeurs.get(middle)) / 2);
        }
    }

    /**
     * Affiche les stats du calendrier selectionné
     * Read : <a href="https://www.reddit.com/r/androiddev/comments/2da207/getting_events_from_a_specific_calendar_and/">...</a>
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

        // Formatteur de dates
        DateTimeFormatter dateFormater = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        // Nb de jours travaillés
        HashMap<String, Integer> nbJoursTravailles = new HashMap<>();
        // Amplitude horaire quotidienne
        HashMap<String, Integer> debutJournee = new HashMap<>();
        HashMap<String, Integer> finJournee = new HashMap<>();
        // Durée des événements par type d'événéments
        HashMap<String, Integer> stats = new HashMap<>();
        // Temps de travail par jour
        HashMap<String, Integer> dureeJournee = new HashMap<>();

        // Récupération de la liste des événements
        Cursor monCursor = monContentResolver.query(builder.build(), EVENT_PROJECTION,
                CalendarContract.Instances.CALENDAR_ID + " = ?", new String[]{String.valueOf(
                        Utils.getPrefInt(getApplicationContext(), R.string.idOptionCalendrier))}, null);

        while (monCursor.moveToNext()) {
            // Si besoin d'un debug
            //Log.e("xxx", "."+ DatabaseUtils.dumpCursorToString(monCursor));
            //     35 {
            //       title=Cat : Subcat
            //       begin=1642593600000
            //       end=1642613400000
            //       organizer=xxx@example.com
            //    }

            // Type de l'événement
            String monType = monCursor.getString(0);
            // Début et fin
            LocalDateTime dateDeb = LocalDateTime.ofInstant(Instant.ofEpochMilli(monCursor.getLong(1)), ZoneId.systemDefault());
            LocalDateTime dateFin = LocalDateTime.ofInstant(Instant.ofEpochMilli(monCursor.getLong(2)), ZoneId.systemDefault());
            // Propriétaire
            String owner = monCursor.getString(3);
            Log.w("Owner : ", owner.toLowerCase().trim());
            // Filtrage sur l'organisateur
            // if (! owner.toLowerCase().trim().contains("xxx@example.com")) {
            //    continue;
            // }

            // Gestion du groupBy
            if (groupBy) {
                // Réduction du type de l'événement à sa partie avant ":"
                int positionSeparateur = monType.indexOf(Utils.getPrefString(getApplicationContext(), R.string.idOptionSeparateurGroupBy));
                // Si sous élément
                if (positionSeparateur > 0) {
                    // Je ne prends que le début + trim pour éviter les effets de bord...
                    monType = monType.substring(0, positionSeparateur).trim();
                }
            }

            // Calcul de la durée de l'événement
            Integer maDuree = 0;
            // Type d'événement déjà connu ?
            if (stats.containsKey(monType)) {
                // Récupération de la durée
                maDuree = stats.get(monType);
                // Suppression de la liste
                stats.remove(monType);
            }
            // Ajout du temps de l'événement (Fin - Début) + millisecondes -> secondes + secondes -> minutes
            Duration duration = Duration.between(dateDeb, dateFin);
            maDuree += (int) duration.toMinutes();
            // Stockage
            stats.put(monType, maDuree);
            Log.w("afficherStats", monType + " - " + dateDeb + " - " + dateFin + " => " + duration.toMinutes());

            // Calcul du nombre de jours travaillés
            // TODO :: ne pas gérer le multi-journées
            LocalDateTime dateTmp = dateDeb;
            for (int i = 0; i <= duration.toDays(); i++) {
                // Formattage de la date
                String dateString = dateTmp.format(dateFormater);
                // Enregistrement comme date travaillée
                nbJoursTravailles.put(dateString, 1);
                Log.d("dateString", dateDeb + " + " + i + " -> " + dateTmp);
                // Passage au jour suivant (si sur plusieurs jours)
                dateTmp = dateTmp.plusDays(1);
            }

            // Calcul de l'amplitude horaire
            // TODO :: prendre en charge les événements sur plusieurs jours
            String dateDebString = dateDeb.format(dateFormater);
            int minutesDebut = dateDeb.getHour() * 60 + dateDeb.getMinute();
            boolean update = true;
            // Déjà un enregistrement pour ce jour ?
            if (debutJournee.containsKey(dateDebString)) {
                // Récupération de la durée
                int debutActuel = debutJournee.get(dateDebString);
                // Le début est-il plus tôt que celui actuellement stocké ?
                if (minutesDebut < debutActuel) {
                    debutJournee.remove(dateDebString);
                } else {
                    update = false;
                }
            }
            // Mise à jour
            if (update) {
                debutJournee.put(dateDebString, minutesDebut);
            }

            String dateFinString = dateFin.format(dateFormater);
            int minutesFin = dateFin.getHour() * 60 + dateFin.getMinute();
            update = true;
            // Déjà un enregistrement pour ce jour ?
            if (finJournee.containsKey(dateFinString)) {
                // Récupération de la durée
                int finActuelle = finJournee.get(dateFinString);
                // La fin est-elle plus tard que celle actuellement stockée ?
                if (minutesFin > finActuelle) {
                    finJournee.remove(dateFinString);
                } else {
                    update = false;
                }
            }
            // Mise à jour
            if (update) {
                finJournee.put(dateFinString, minutesFin);
            }

            // Calcul du temps de travail journalier
            int dureeAAjouter = (int) duration.toMinutes();
            // Déjà un enregistrement pour ce jour ?
            if (dureeJournee.containsKey(dateDebString)) {
                // Récupération de la durée
                dureeAAjouter += dureeJournee.get(dateDebString);
                dureeJournee.remove(dateDebString);
            }
            // On enregistre...
            dureeJournee.put(dateDebString, dureeAAjouter);
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

        // Calcul des amplitudes
        ArrayList<Integer> amplitudeJournaliere = new ArrayList<>();
        // Pour chaque journée...
        for (String uneDate : debutJournee.keySet()) {
            if (!finJournee.containsKey(uneDate)) {
                Log.e("calcul des amplitudes", uneDate + " -> présente uniquement dans debutJournee !!" + debutJournee + " - " + finJournee);
                continue;
            }
            // Amplitude de la journée
            int amplitudeDuJour = finJournee.get(uneDate) - debutJournee.get(uneDate);
            amplitudeJournaliere.add(amplitudeDuJour);
        }

        // Statistiques
        TextView mesStats = findViewById(R.id.texteStats);
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
        // Affichage du total
        mesStats.append("**Total** : " + Math.round(dureeTotale / 60.0f) + "h\n");
        // Affichage du nombre de jours
        mesStats.append("**Nb jours travaillés** : " + nbJoursTravailles.size() + "\n");
        // Temps de travail moyen
        mesStats.append("**Temps de travail moyen** : " + String.format(Locale.FRANCE, "%.2f", (calculerMoyenne(new ArrayList<>(dureeJournee.values()), nbJoursTravailles.size()) / 60.0f)) + "h/j\n");
        // Temps de travail médian
        mesStats.append("**Temps de travail médian** : " + String.format(Locale.FRANCE, "%.2f", (calculerMediane(new ArrayList<>(dureeJournee.values()), nbJoursTravailles.size()) / 60.0f)) + "h/j\n");
        // Amplitude horaire moyenne
        mesStats.append("**Amplitude horaire moyenne** : " + String.format(Locale.FRANCE, "%.2f", (calculerMoyenne(amplitudeJournaliere, nbJoursTravailles.size()) / 60.0f)) + "h/j\n");
        // Amplitude horaire médianne
        mesStats.append("**Amplitude horaire médiane** : " + String.format(Locale.FRANCE, "%.2f", (calculerMediane(amplitudeJournaliere, nbJoursTravailles.size()) / 60.0f)) + "h/j\n");
        // Date du build
        mesStats.append(" ---  Compilation : " + new Date(BuildConfig.TIMESTAMP) + "  --- \n");
    }

    /**
     * Sort HashMap by Value
     * https://www.mkyong.com/java/how-to-sort-a-map-in-java/
     */
    private static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
