package mediathek.tool.models;

import mediathek.daten.DatenAbo;
import mediathek.tool.Datum;

@SuppressWarnings("serial")
public class TModelAbo extends TModel {
    public TModelAbo(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case DatenAbo.ABO_NR, DatenAbo.ABO_MINDESTDAUER -> Integer.class;
            case DatenAbo.ABO_DOWN_DATUM -> Datum.class;
            default -> String.class;
        };
    }

    @Override
    public String getColumnName(int column) {

        return switch (column) {
            case DatenAbo.ABO_NR -> "Nr";
            case DatenAbo.ABO_EINGESCHALTET -> "aktiv";
            case DatenAbo.ABO_NAME -> "Name";
            case DatenAbo.ABO_SENDER -> "Sender";
            case DatenAbo.ABO_THEMA -> "Thema";
            case DatenAbo.ABO_TITEL -> "Titel";
            case DatenAbo.ABO_THEMA_TITEL -> "Thema-Titel";
            case DatenAbo.ABO_IRGENDWO -> "Irgendwo";
            case DatenAbo.ABO_MINDESTDAUER -> "Dauer";
            case DatenAbo.ABO_MIN -> "min/max";
            case DatenAbo.ABO_ZIELPFAD -> "Zielpfad";
            case DatenAbo.ABO_DOWN_DATUM -> "letztes Abo";
            case DatenAbo.ABO_PSET -> "Programmset";
            case DatenAbo.ABO_CATEGORY -> "Kategorie";
            default -> throw new IndexOutOfBoundsException("UNKNOWN COLUMN NAME: " + column);
        };
    }
}
