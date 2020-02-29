package mediathek.gui.abo;

import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import jiconfont.icons.FontAwesome;
import jiconfont.swing.IconFontSwing;
import mediathek.config.Daten;
import mediathek.config.MVConfig;
import mediathek.daten.DatenAbo;
import mediathek.gui.actions.CreateNewAboAction;
import mediathek.gui.dialog.DialogEditAbo;
import mediathek.gui.messages.AboListChangedEvent;
import mediathek.javafx.tool.JavaFxUtils;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.Datum;
import mediathek.tool.NoSelectionErrorDialog;
import mediathek.tool.cellrenderer.CellRendererAbo;
import mediathek.tool.listener.BeobTableHeader;
import mediathek.tool.models.TModelAbo;
import mediathek.tool.table.MVAbosTable;
import mediathek.tool.table.MVTable;
import net.engio.mbassy.listener.Handler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

@SuppressWarnings("serial")
public class ManageAboPanel extends JPanel {
    private static final String ACTION_MAP_KEY_EDIT_ABO = "edit_abo";
    private static final String ACTION_MAP_KEY_DELETE_ABO = "delete_abo";
    private final MVTable tabelle = new MVAbosTable();
    private final Daten daten;
    private final CreateNewAboAction createAboAction = new CreateNewAboAction(Daten.getInstance().getListeAbo());
    private final JFXPanel toolBarPanel = new JFXPanel();
    private final JFXPanel infoPanel = new JFXPanel();
    private FXAboToolBar toolBar;
    private JScrollPane jScrollPane1;
    /*
     * controller must be kept in variable for strong ref, otherwise GC will erase controller and therefore
     * update of abos in dialog will stop working...
     */
    private AboInformationController infoController;


    public ManageAboPanel() {
        super();
        daten = Daten.getInstance();

        initComponents();
        jScrollPane1.setViewportView(tabelle);

        setupToolBar();
        setupInfoPanel();

        daten.getMessageBus().subscribe(this);

        initListeners();

        initializeTable();
    }

    private void setupInfoPanel() {
        JavaFxUtils.invokeInFxThreadAndWait(() -> {
            try {
                URL url = getClass().getResource("/mediathek/res/programm/fxml/abo/abo_information_panel.fxml");

                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(url);

                HBox infoPane = loader.load();
                infoPanel.setScene(new Scene(infoPane));

                infoController = loader.getController();
                infoController.startListener();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void initializeTable() {
        tabelleLaden();
        tabelle.initTabelle();
        if (tabelle.getRowCount() > 0) {
            tabelle.setRowSelectionInterval(0, 0);
        }
    }

    private void setupToolBar() {
      CreateNewAboAction newAboAction = new CreateNewAboAction(Daten.getInstance().getListeAbo());  
        JavaFxUtils.invokeInFxThreadAndWait(() -> {
            toolBar = new FXAboToolBar();
            toolBar.btnOn.setOnAction(e -> SwingUtilities.invokeLater(() -> aboEinAus(true)));
            toolBar.btnOff.setOnAction(e -> SwingUtilities.invokeLater(() -> aboEinAus(false)));
            toolBar.btnDelete.setOnAction(e -> SwingUtilities.invokeLater(this::aboLoeschen));
            toolBar.btnEdit.setOnAction(e -> SwingUtilities.invokeLater(this::editAbo));

            toolBar.btnNewAbo.setOnAction(e -> SwingUtilities.invokeLater(() -> newAboAction.actionPerformed(null)));

            toolBar.cbSender.setOnAction(e -> SwingUtilities.invokeLater(this::tabelleLaden));

            toolBarPanel.setScene(new Scene(toolBar));
        });
    }

    public void tabelleSpeichern() {
        if (tabelle != null) {
            tabelle.tabelleNachDatenSchreiben();
        }
    }

    private void setCellRenderer() {
        final CellRendererAbo cellRenderer = new CellRendererAbo(daten.getSenderIconCache());
        tabelle.setDefaultRenderer(Object.class, cellRenderer);
        tabelle.setDefaultRenderer(Datum.class, cellRenderer);
        tabelle.setDefaultRenderer(Integer.class, cellRenderer);
    }

    @Handler
    private void handleAboListChanged(AboListChangedEvent e) {
        SwingUtilities.invokeLater(this::tabelleLaden);
    }

    private void setupKeyMap() {
        final InputMap im = tabelle.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_MAP_KEY_EDIT_ABO);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), ACTION_MAP_KEY_DELETE_ABO);

        final ActionMap am = tabelle.getActionMap();
        am.put(ACTION_MAP_KEY_EDIT_ABO, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editAbo();
            }
        });
        am.put(ACTION_MAP_KEY_DELETE_ABO, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aboLoeschen();
            }
        });
    }

    private JPopupMenu createContextMenu() {
        JMenuItem itemEinschalten = new JMenuItem("Abo einschalten");
        itemEinschalten.setIcon(IconFontSwing.buildIcon(FontAwesome.CHECK, 16));
        itemEinschalten.addActionListener(e -> aboEinAus(true));

        JMenuItem itemDeaktivieren = new JMenuItem("Abo ausschalten");
        itemDeaktivieren.setIcon(IconFontSwing.buildIcon(FontAwesome.TIMES, 16));
        itemDeaktivieren.addActionListener(e -> aboEinAus(false));

        JMenuItem itemLoeschen = new JMenuItem("Abo löschen");
        itemLoeschen.setIcon(IconFontSwing.buildIcon(FontAwesome.MINUS, 16));
        itemLoeschen.addActionListener(e -> aboLoeschen());

        JMenuItem itemAendern = new JMenuItem("Abo ändern");
        itemAendern.setIcon(IconFontSwing.buildIcon(FontAwesome.PENCIL_SQUARE_O, 16));
        itemAendern.addActionListener(e -> editAbo());

        JMenuItem itemNeu = new JMenuItem();
        itemNeu.setAction(createAboAction);

        JPopupMenu jPopupMenu = new JPopupMenu();
        jPopupMenu.add(itemEinschalten);
        jPopupMenu.add(itemDeaktivieren);
        jPopupMenu.addSeparator();
        jPopupMenu.add(itemNeu);
        jPopupMenu.add(itemLoeschen);
        jPopupMenu.add(itemAendern);

        return jPopupMenu;
    }

    private void initListeners() {
        tabelle.setComponentPopupMenu(createContextMenu());

        setCellRenderer();

        tabelle.setModel(new TModelAbo(new Object[][]{}, DatenAbo.COLUMN_NAMES));
        tabelle.setLineBreak(MVConfig.getBool(MVConfig.Configs.SYSTEM_TAB_ABO_LINEBREAK));
        tabelle.getTableHeader().addMouseListener(new BeobTableHeader(tabelle,
                DatenAbo.spaltenAnzeigen,
                new int[]{DatenAbo.ABO_EINGESCHALTET},
                new int[]{},
                true,
                MVConfig.Configs.SYSTEM_TAB_ABO_LINEBREAK));

        setupKeyMap();
    }

    private void tabelleLaden() {
        tabelle.getSpalten();

        JavaFxUtils.invokeInFxThreadAndWait(() -> {
            final String selectedItem = toolBar.cbSender.getValue();
            if (selectedItem != null) {
                SwingUtilities.invokeLater(() -> {
                    daten.getListeAbo().addObjectData((TModelAbo) tabelle.getModel(), selectedItem);
                    tabelle.setSpalten();
                });
            }
        });
    }

    private void aboLoeschen() {
        int[] rows = tabelle.getSelectedRows();
        if (rows.length > 0) {
            String text;
            if (rows.length == 1) {
                int delRow = tabelle.convertRowIndexToModel(rows[0]);
                text = '"' + tabelle.getModel().getValueAt(delRow, DatenAbo.ABO_NAME).toString() + "\" löschen?";
            } else {
                text = rows.length + " Abos löschen?";
            }
            final int ret = JOptionPane.showConfirmDialog(this, text, "Löschen?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.OK_OPTION) {
                for (int i = rows.length - 1; i >= 0; --i) {
                    int delRow = tabelle.convertRowIndexToModel(rows[i]);
                    ((TModelAbo) tabelle.getModel()).removeRow(delRow);
                    daten.getListeAbo().remove(delRow);
                }
            }
            tabelleLaden();

            selectFirstRow();

            daten.getListeAbo().aenderungMelden();
        } else {
            NoSelectionErrorDialog.show();
        }
    }

    private void selectFirstRow() {
        if (tabelle.getRowCount() > 0) {
            // sonst ist schon eine Zeile markiert
            if (tabelle.getSelectedRow() == -1) {
                tabelle.requestFocus();
                tabelle.setRowSelectionInterval(0, 0);
            }
        }
    }

    public void editAbo() {
        // nichts selektiert
        if (tabelle.getSelectedRowCount() == 0) {
            NoSelectionErrorDialog.show();
            return;
        }

        final int[] rows = tabelle.getSelectedRows();
        int modelRow = tabelle.convertRowIndexToModel(tabelle.getSelectedRow());

        DatenAbo akt = daten.getListeAbo().getAboNr(modelRow);
        DialogEditAbo dialog = new DialogEditAbo(MediathekGui.ui(), true, daten, akt, tabelle.getSelectedRowCount() > 1);
        dialog.setTitle("Abo ändern");
        dialog.setVisible(true);
        if (!dialog.ok) {
            return;
        }

        if (tabelle.getSelectedRowCount() > 1) {
            // bei mehreren selektierten Zeilen
            for (int row : rows) {
                for (int b = 0; b < dialog.ch.length; ++b) {
                    if (!dialog.ch[b]) {
                        continue;
                    }
                    modelRow = tabelle.convertRowIndexToModel(row);
                    DatenAbo sel = daten.getListeAbo().getAboNr(modelRow);
                    sel.arr[b] = akt.arr[b];
                    if (b == DatenAbo.ABO_MINDESTDAUER) {
                        sel.setMindestDauerMinuten();
                    }
                    if (b == DatenAbo.ABO_MIN) {
                        sel.min = Boolean.parseBoolean(sel.arr[DatenAbo.ABO_MIN]);
                    }
                }
            }

        }

        tabelleLaden();
        daten.getListeAbo().aenderungMelden();
    }

    private void aboEinAus(boolean ein) {
        final int[] rows = tabelle.getSelectedRows();
        if (rows.length > 0) {
            for (int row : rows) {
                int modelRow = tabelle.convertRowIndexToModel(row);
                DatenAbo akt = daten.getListeAbo().getAboNr(modelRow);
                akt.arr[DatenAbo.ABO_EINGESCHALTET] = String.valueOf(ein);
            }
            tabelleLaden();
            tabelle.clearSelection();
            tabelle.requestFocus();
            for (int row : rows) {
                tabelle.addRowSelectionInterval(row, row);
            }

            daten.getListeAbo().aenderungMelden();
        } else {
            NoSelectionErrorDialog.show();
        }
    }

    private void initComponents() {
        jScrollPane1 = new JScrollPane();
        var jTable1 = new JTable();

        setLayout(new BorderLayout());

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(jTable1);

        add(toolBarPanel, BorderLayout.NORTH);
        add(jScrollPane1, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);
    }
}
