package mediathek.daten;

import com.google.common.base.Stopwatch;
import mediathek.config.Daten;
import mediathek.config.MVConfig;
import mediathek.javafx.filterpanel.ZeitraumSpinner;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.Filter;
import mediathek.tool.Listener;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("serial")
public class ListeBlacklist extends LinkedList<DatenBlacklist> {

    private static final Logger logger = LogManager.getLogger(ListeBlacklist.class);
    private static final String[] EMPTY_STRING = new String[]{""};
    private long days = 0;
    private boolean doNotShowFutureFilms, doNotShowGeoBlockedFilms;
    private boolean blacklistIsActive;
    private long filmlaengeSoll = 0;
    private int nr = 0;

    public ListeBlacklist() {
    }

    /**
     * Add item without notifying registered listeners.
     *
     * @param b {@link DatenBlacklist} item.
     */
    public synchronized void addWithoutNotification(DatenBlacklist b) {
        b.arr[DatenBlacklist.BLACKLIST_NR] = Integer.toString(nr++);
        super.add(b);
    }

    @Override
    public synchronized boolean add(DatenBlacklist b) {
        b.arr[DatenBlacklist.BLACKLIST_NR] = Integer.toString(nr++);
        boolean ret = super.add(b);
        filterListAndNotifyListeners();
        return ret;
    }

    @Override
    public synchronized boolean remove(Object b) {
        boolean ret = super.remove(b);
        filterListAndNotifyListeners();
        return ret;
    }

    @Override
    public synchronized DatenBlacklist remove(int idx) {
        DatenBlacklist ret = super.remove(idx);
        filterListAndNotifyListeners();
        return ret;
    }

    public synchronized DatenBlacklist remove(String idx) {
        DatenBlacklist bl;
        if ((bl = get(idx)) != null) {
            remove(bl);
        }
        filterListAndNotifyListeners();
        return bl;
    }

    @Override
    public synchronized DatenBlacklist get(int idx) {
        return super.get(idx);
    }

    /**
     * Return the element at the specified {@link String} position.
     *
     * @param strIndex Index string of the specified element
     * @return the specified element in the list
     */
    public synchronized DatenBlacklist get(final String strIndex) {
        return stream()
                .filter(e -> e.arr[DatenBlacklist.BLACKLIST_NR].equals(strIndex))
                .findFirst()
                .orElse(null);

    }

    @Override
    public synchronized void clear() {
        super.clear();
        filterListAndNotifyListeners();
    }

    public synchronized Object[][] getObjectData() {
        Object[][] object = new Object[size()][DatenBlacklist.MAX_ELEM];

        int i = 0;
        for (DatenBlacklist blacklist : this) {
            object[i] = blacklist.arr;
            ++i;
        }
        return object;
    }

    /**
     * Main filtering routine
     */
    public synchronized void filterListe() {
        final Daten daten = Daten.getInstance();
        final ListeFilme listeFilme = daten.getListeFilme();
        final ListeFilme listeRet = daten.getListeFilmeNachBlackList();

        loadCurrentFilterSettings();

        Stopwatch stopwatch = Stopwatch.createStarted();
        listeRet.clear();

        if (listeFilme != null && listeFilme.size() > 0) { // Check if there are any movies
            listeRet.setMetaData(listeFilme.metaData());

            this.parallelStream().forEach(entry -> {
                entry.toLower();
                entry.hasPattern();
            });

            listeRet.neueFilme = false;

            final Predicate<DatenFilm> pred = createPredicate();

            Stopwatch stopwatch2 = Stopwatch.createStarted();
            listeFilme.parallelStream().filter(pred).forEachOrdered(listeRet::add);
            stopwatch2.stop();
            logger.debug("FILTERING and ADDING() took: {}", stopwatch2);

            setupNewEntries();

            // Array mit Sendernamen/Themen füllen
            listeRet.fillSenderList();
        }
        stopwatch.stop();
        logger.debug("filterListe(): {}", stopwatch);
    }

    /**
     * Setup dynamically the list of filter to be applied to blacklist film list
     *
     * @return The reduced filter predicates.
     */
    private Predicate<DatenFilm> createPredicate() {
        final List<Predicate<DatenFilm>> filterList = new ArrayList<>();
        if (days != 0)
            filterList.add(this::checkDate);

        if (blacklistIsActive) {
            //add the filter predicates to the list
            if (!isEmpty()) {
                filterList.add(this::applyBlacklistFilters);
            }

            if (doNotShowGeoBlockedFilms) {
                filterList.add(this::checkGeoBlockedFilm);
            }
            if (doNotShowFutureFilms) {
                filterList.add(this::checkIfFilmIsInFuture);
            }
            filterList.add(this::checkFilmLength);
        }

        final Predicate<DatenFilm> pred = filterList.stream().reduce(Predicate::and).orElse(x -> true);
        filterList.clear();

        return pred;
    }

    /**
     * Detect if there are new entried in the blacklist filtered film list.
     */
    private void setupNewEntries() {
        //are there new film entries?
        final Daten daten = Daten.getInstance();
        daten.getListeFilmeNachBlackList().stream()
                .filter(DatenFilm::isNew)
                .findAny()
                .ifPresent(ignored -> daten.getListeFilmeNachBlackList().neueFilme = true);
    }

    /**
     * Filterfunction for Abos dialog.
     *
     * @param film item to te tested
     * @return true if item should be displayed.
     */
    public synchronized boolean checkBlackOkFilme_Downloads(DatenFilm film) {
        // hier werden die Filme für Downloads gesucht, Zeit ist "0"
        // ob die Blackliste dafür verwendet werden soll, ist schon geklärt
        loadCurrentFilterSettings();
        days = 0; // soll nur im TabFilme ausgewertet werden (Filter: Tage)
        blacklistIsActive = true; // Blacklist nur wenn "auch für Abos" geklickt, egal ob ein- oder ausgeschaltet

        return applyFiltersForAbos(film);
    }

    /**
     * Filter the list and notify all registered listeners.
     */
    public synchronized void filterListAndNotifyListeners() {
        filterListe();
        Listener.notify(Listener.EREIGNIS_BLACKLIST_GEAENDERT, ListeBlacklist.class.getSimpleName());
    }

    /**
     * Load current filter settings from Config
     */
    private void loadCurrentFilterSettings() {
        try {
            final String val = MediathekGui.ui().tabFilme.fap.zeitraumProperty.getValue();
            if (val.equals(ZeitraumSpinner.UNLIMITED_VALUE))
                days = 0;
            else {
                final long max = 1000L * 60L * 60L * 24L * Integer.parseInt(val);
                days = System.currentTimeMillis() - max;
            }
        } catch (Exception ex) {
            days = 0;
        }
        try {
            filmlaengeSoll = Long.parseLong(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE)) * 60; // Minuten
        } catch (Exception ex) {
            filmlaengeSoll = 0;
        }
        blacklistIsActive = Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_ON));
        doNotShowFutureFilms = Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_ZUKUNFT_NICHT_ANZEIGEN));
        doNotShowGeoBlockedFilms = Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_GEO_NICHT_ANZEIGEN));
    }

    /**
     * Apply filters for ABOS if check box is active
     *
     * @param film item to be filtered
     * @return true if film shall be displayed
     */
    private boolean applyFiltersForAbos(DatenFilm film) {
        // erst mal den Filter Tage, kommt aus dem Filter und deswegen immer
        if (!checkDate(film)) {
            return false;
        }

        //===========================================
        // dann die Blacklist, nur wenn eingeschaltet
        if (!blacklistIsActive) {
            return true;
        }
        if (doNotShowGeoBlockedFilms && !checkGeoBlockedFilm(film)) {
            return false;
        }
        if (doNotShowFutureFilms && !checkIfFilmIsInFuture(film)) {
            return false;
        }
        if (!checkFilmLength(film)) {
            // wegen der Möglichkeit "Whiteliste" muss das extra geprüft werden
            return false;
        }
        if (this.isEmpty()) {
            return true;
        }
        for (DatenBlacklist blacklistEntry : this) {
            if (Filter.filterAufFilmPruefen(blacklistEntry.arr[DatenBlacklist.BLACKLIST_SENDER], blacklistEntry.arr[DatenBlacklist.BLACKLIST_THEMA],
                    Filter.isPattern(blacklistEntry.arr[DatenBlacklist.BLACKLIST_TITEL])
                            ? new String[]{blacklistEntry.arr[DatenBlacklist.BLACKLIST_TITEL]} : blacklistEntry.arr[DatenBlacklist.BLACKLIST_TITEL].toLowerCase().split(","),
                    Filter.isPattern(blacklistEntry.arr[DatenBlacklist.BLACKLIST_THEMA_TITEL])
                            ? new String[]{blacklistEntry.arr[DatenBlacklist.BLACKLIST_THEMA_TITEL]} : blacklistEntry.arr[DatenBlacklist.BLACKLIST_THEMA_TITEL].toLowerCase().split(","),
                    new String[]{""}, 0, true /*min*/, film, true /*auch die Länge prüfen*/
            )) {
                return Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST));
            }
        }
        return !Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST));
    }

    /**
     * Check if film would be geoblocked for user
     *
     * @param film item to be checked
     * @return true if it is NOT blocked, false if it IS blocked
     */
    private boolean checkGeoBlockedFilm(DatenFilm film) {
        var geoOpt = film.getGeo();
        if (geoOpt.isEmpty())
            return true;

        final String geoLocation = ApplicationConfiguration.getConfiguration().getString(ApplicationConfiguration.GEO_LOCATION);
        return geoOpt.orElse("").contains(geoLocation);
    }

    private String[] mySplit(final String inputString) {
        final String[] pTitle = StringUtils.split(inputString, ',');
        if (pTitle.length == 0)
            return EMPTY_STRING;
        else
            return pTitle;
    }

    private String[] createPattern(final boolean isPattern, final String inputString) {
        if (isPattern)
            return new String[]{inputString};
        else
            return mySplit(inputString);
    }

    /**
     * Apply filters to film.
     *
     * @param film item to be filtered
     * @return true if film can be displayed
     */
    private boolean applyBlacklistFilters(DatenFilm film) {
        final boolean isWhitelist = Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST));

        for (DatenBlacklist entry : this) {
            final String[] pTitel = createPattern(entry.patternTitle, entry.arr[DatenBlacklist.BLACKLIST_TITEL]);
            final String[] pThema = createPattern(entry.patternThema, entry.arr[DatenBlacklist.BLACKLIST_THEMA_TITEL]);

            if (performFiltering(
                    entry,
                    pTitel,
                    pThema,
                    film)) {
                return isWhitelist;
            }
        }
        return !isWhitelist;
    }

    private boolean performFiltering(final DatenBlacklist entry,
                                     final String[] titelSuchen, final String[] themaTitelSuchen,
                                     final DatenFilm film) {
        // prüfen ob xxxSuchen im String imXxx enthalten ist, themaTitelSuchen wird mit Thema u. Titel verglichen
        // senderSuchen exakt mit sender
        // themaSuchen exakt mit thema
        // titelSuchen muss im Titel nur enthalten sein

        boolean result = false;
        final String thema = film.getThema();
        final String title = film.getTitle();


        final String senderSuchen = entry.arr[DatenBlacklist.BLACKLIST_SENDER];
        final String themaSuchen = entry.arr[DatenBlacklist.BLACKLIST_THEMA];

        if (senderSuchen.isEmpty() || film.getSender().compareTo(senderSuchen) == 0) {
            if (themaSuchen.isEmpty() || thema.equalsIgnoreCase(themaSuchen)) {
                if (titelSuchen.length == 0 || Filter.pruefen(titelSuchen, title)) {
                    if (themaTitelSuchen.length == 0
                            || Filter.pruefen(themaTitelSuchen, thema)
                            || Filter.pruefen(themaTitelSuchen, title)) {
                        // die Länge soll mit geprüft werden
                        if (checkLengthWithMin(film.getFilmLength())) {
                            result = true;
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean checkLengthWithMin(long filmLaenge) {
        return Filter.lengthCheck(0, filmLaenge) || filmLaenge > 0;
    }

    /**
     * Check film based on date
     *
     * @param film item to be checked
     * @return true if film can be displayed
     */
    private boolean checkDate(@NotNull DatenFilm film) {
        if (days != 0) {
            final long filmTime = film.getDatumFilm().getTime();
            return filmTime == 0 || filmTime >= days;
        }

        return true;
    }

    /**
     * Check if a future film should be displayed.
     *
     * @param film item to be checked.
     * @return true if it should be displayed.
     */
    private boolean checkIfFilmIsInFuture(@NotNull DatenFilm film) {
        return film.getDatumFilm().getTime() <= System.currentTimeMillis();
    }

    /**
     * Filter based on film length.
     *
     * @param film item to check
     * @return true if film should be displayed
     */
    private boolean checkFilmLength(@NotNull DatenFilm film) {
        final long filmLength = film.getFilmLength();
        return !(filmlaengeSoll != 0 && filmLength != 0 && filmlaengeSoll > filmLength);

    }
}
