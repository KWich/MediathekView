package mediathek.gui;

import javafx.collections.ObservableList;
import mSearch.daten.DatenFilm;
import mSearch.daten.ListeFilme;
import mediathek.config.Daten;
import mediathek.javafx.filterpanel.FilmActionPanel;
import mediathek.tool.Filter;
import mediathek.tool.TModel;
import mediathek.tool.TModelFilm;
import mediathek.tool.TrailerTeaserChecker;
import mediathek.tool.table.MVTable;

import java.util.concurrent.TimeUnit;

public class GuiFilmeModelHelper {
    private final FilmActionPanel fap;
    private final Daten daten;
    private final MVTable tabelle;
    private final TModel tModel;
    private final ListeFilme listeFilme;

    public GuiFilmeModelHelper(FilmActionPanel fap, Daten daten, MVTable tabelle) {
        this.fap = fap;
        this.daten = daten;
        this.tabelle = tabelle;

        tModel = new TModelFilm(new Object[][]{}, DatenFilm.COLUMN_NAMES);
        listeFilme = daten.getListeFilmeNachBlackList();

    }

    private String getFilterThema() {
        String filterThema = fap.themaBox.getSelectionModel().getSelectedItem();
        if (filterThema == null) {
            filterThema = "";
        }

        return filterThema;
    }

    private String getFilterSender() {
        String filterSender = fap.senderBox.getSelectionModel().getSelectedItem();
        if (filterSender == null)
            filterSender = "";

        return filterSender;
    }

    private String[] evaluateThemaTitel() {
        String[] arrThemaTitel;

        final String filterThemaTitel = fap.roSearchStringProperty.getValueSafe();
        if (Filter.isPattern(filterThemaTitel)) {
            arrThemaTitel = new String[]{filterThemaTitel};
        } else {
            arrThemaTitel = filterThemaTitel.split(",");
            for (int i = 0; i < arrThemaTitel.length; ++i) {
                arrThemaTitel[i] = arrThemaTitel[i].trim().toLowerCase();
            }
        }

        return arrThemaTitel;
    }

    private boolean noFiltersAreSet() {
        boolean ret = false;

        if (fap.senderList.getCheckModel().isEmpty()
                && getFilterSender().isEmpty()
                && getFilterThema().isEmpty()
                && fap.roSearchStringProperty.getValueSafe().isEmpty()
                && ((int) fap.filmLengthSlider.getLowValue() == 0)
                && ((int) fap.filmLengthSlider.getHighValue() == FilmActionPanel.UNLIMITED_VALUE)
                && !fap.dontShowAbos.getValue()
                && !fap.showUnseenOnly.getValue()
                && !fap.showOnlyHd.getValue()
                && !fap.showSubtitlesOnly.getValue()
                && !fap.showLivestreamsOnly.getValue()
                && !fap.showNewOnly.getValue()
                && !fap.dontShowTrailers.getValue()
                && !fap.dontShowSignLanguage.getValue()
                && !fap.dontShowAudioVersions.getValue())
            ret = true;

        return ret;
    }

    private void performTableFiltering() {
        final boolean nurNeue = fap.showNewOnly.getValue();
        final boolean nurUt = fap.showSubtitlesOnly.getValue();
        final boolean showOnlyHd = fap.showOnlyHd.getValue();
        final boolean kGesehen = fap.showUnseenOnly.getValue();
        final boolean keineAbos = fap.dontShowAbos.getValue();
        final boolean showOnlyLivestreams = fap.showLivestreamsOnly.getValue();
        final boolean dontShowTrailers = fap.dontShowTrailers.getValue();
        final boolean dontShowGebaerdensprache = fap.dontShowSignLanguage.getValue();
        final boolean dontShowAudioVersions = fap.dontShowAudioVersions.getValue();

        final long minLength = (long) fap.filmLengthSlider.getLowValue();
        final long maxLength = (long) fap.filmLengthSlider.getHighValue();

        final String filterSender = getFilterSender();
        final String filterThema = getFilterThema();

        // ThemaTitel
        String[] arrThemaTitel = evaluateThemaTitel();

        final long minLengthInSeconds = TimeUnit.SECONDS.convert(minLength, TimeUnit.MINUTES);
        final long maxLengthInSeconds = TimeUnit.SECONDS.convert(maxLength, TimeUnit.MINUTES);
        final TrailerTeaserChecker ttc = new TrailerTeaserChecker();
        final ObservableList<String> selectedSenders = fap.senderList.getCheckModel().getCheckedItems();

        for (DatenFilm film : listeFilme) {
            if (!selectedSenders.isEmpty()) {
                if (!selectedSenders.contains(film.getSender()))
                    continue;
            }

            final long filmLength = film.getFilmLength();
            if (filmLength < minLengthInSeconds)
                continue;

            if (maxLength < FilmActionPanel.UNLIMITED_VALUE) {
                if (filmLength > maxLengthInSeconds)
                    continue;

            }
            if (nurNeue) {
                if (!film.isNew()) {
                    continue;
                }
            }
            if (showOnlyLivestreams) {
                if (!film.getThema().equals(ListeFilme.THEMA_LIVE)) {
                    continue;
                }
            }
            if (showOnlyHd) {
                if (!film.isHD()) {
                    continue;
                }
            }
            if (nurUt) {
                if (!film.hasSubtitle()) {
                    continue;
                }
            }
            if (keineAbos) {
                if (!film.arr[DatenFilm.FILM_ABO_NAME].isEmpty()) {
                    continue;
                }
            }
            if (kGesehen) {
                if (daten.history.urlPruefen(film.getUrlHistory())) {
                    continue;
                }
            }

            if (dontShowTrailers) {
                if (ttc.check(film.getTitle()))
                    continue;
            }

            if (dontShowGebaerdensprache) {
                String titel = film.getTitle();
                if (titel.contains("Gebärden"))
                    continue;
            }

            if (dontShowAudioVersions) {
                if (checkForAudioVersions(film.getTitle()))
                    continue;
            }

            //TODO add sender filtering here, remove from below

            //filter mitLaenge false dann aufrufen
            //je nachdem dann das ganze herausoperieren
            String[] arrIrgendwo = {};
            String[] arrTitel = {};
            if (Filter.filterAufFilmPruefen(filterSender, filterThema, arrTitel, arrThemaTitel, arrIrgendwo, 0, true, film, false)) {
                addObjectDataTabFilme(film);
            }
        }
    }

    private void fillTableModel() {
        // dann ein neues Model anlegen
        if (noFiltersAreSet()) {
            // dann ganze Liste laden
            addObjectDataTabFilme();
        } else {
            performTableFiltering();
        }
        tabelle.setModel(tModel);
    }

    /**
     * Check if string contains specific keywords.
     */
    private boolean checkForAudioVersions(String titel) {
        return titel.contains("Hörfassung") || titel.contains("Audiodeskription");
    }

    public void prepareTableModel() {
        if (!listeFilme.isEmpty())
            fillTableModel();

        //use empty model
        tabelle.setModel(tModel);
    }

    private void addObjectDataTabFilme() {
        if (!listeFilme.isEmpty()) {
            for (DatenFilm film : listeFilme) {
                addObjectDataTabFilme(film);
            }
        }
    }

    private void addObjectDataTabFilme(DatenFilm film) {
        Object[] object = new Object[DatenFilm.MAX_ELEM];
        for (int m = 0; m < DatenFilm.MAX_ELEM; ++m) {
            switch (m) {
                case DatenFilm.FILM_NR:
                    object[m] = film.getFilmNr();
                    break;
                case DatenFilm.FILM_DATUM:
                    object[m] = film.datumFilm;
                    break;
                case DatenFilm.FILM_GROESSE:
                    object[m] = film.getFilmSize();
                    break;
                case DatenFilm.FILM_REF:
                    object[m] = film;
                    break;
                case DatenFilm.FILM_HD:
                    object[m] = film.isHD() ? "1" : "0";
                    break;
                case DatenFilm.FILM_UT:
                    object[m] = film.hasSubtitle() ? "1" : "0";
                    break;
                default:
                    object[m] = film.arr[m];
                    break;
            }
        }

        tModel.addRow(object);
    }
}
