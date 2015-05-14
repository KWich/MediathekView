/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.gui.dialogEinstellungen;

import com.jidesoft.utils.SystemInfo;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mediathek.controller.Log;
import mediathek.daten.Daten;
import mediathek.file.GetFile;
import mediathek.gui.PanelVorlage;
import mediathek.gui.dialog.DialogHilfe;
import mediathek.res.GetIcon;
import mediathek.tool.Duration;
import mediathek.tool.HinweisKeineAuswahl;
import mediathek.tool.ListenerMediathekView;
import mediathek.tool.MVConfig;
import mediathek.tool.MVMessageDialog;
import mediathek.tool.TModel;
import msearch.filmlisten.MSFilmlisteSchreiben;

public class PanelMediaDB extends PanelVorlage {

    private final TModel modelPath = new TModel(new Object[][]{}, new String[]{"Pfad"});

    public PanelMediaDB(Daten d, JFrame parent) {
        super(d, parent);
        initComponents();
        daten = d;

        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_MEDIA_DB_START, PanelMediaDB.class.getSimpleName()) {
            @Override
            public void ping() {
                // neue DB suchen
                setIndex(false);
            }
        });
        ListenerMediathekView.addListener(new ListenerMediathekView(ListenerMediathekView.EREIGNIS_MEDIA_DB_STOP, PanelMediaDB.class.getSimpleName()) {
            @Override
            public void ping() {
                // neue DB liegt vor
                jLabelSizeIndex.setText(Daten.mVMediaDB.getSizeFileArray() + "");
                setIndex(true);
            }
        });

        progress.setVisible(false);
        progress.setIndeterminate(true);
        progress.setMaximum(0);
        progress.setMinimum(0);
        progress.setValue(0);
        jTablePath.setModel(modelPath);
        jTextFieldSuffix.setText(Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_SUFFIX));
        jTextFieldSuffix.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_SUFFIX, jTextFieldSuffix.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_SUFFIX, jTextFieldSuffix.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_SUFFIX, jTextFieldSuffix.getText());
            }
        });
        jRadioButtonOhneSuffix.setSelected(Boolean.parseBoolean(Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_SUFFIX_OHNE)));
        jRadioButtonMitSuffix.setSelected(!Boolean.parseBoolean(Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_SUFFIX_OHNE)));
        jRadioButtonOhneSuffix.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_SUFFIX_OHNE, Boolean.toString(jRadioButtonOhneSuffix.isSelected()));
            }
        });
        jRadioButtonMitSuffix.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_SUFFIX_OHNE, Boolean.toString(jRadioButtonOhneSuffix.isSelected()));
            }
        });
        jCheckBoxMediaDB.setSelected(Boolean.parseBoolean(Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_ECHTZEITSUCHE)));
        jCheckBoxMediaDB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_ECHTZEITSUCHE, Boolean.toString(jCheckBoxMediaDB.isSelected()));
            }
        });

        jButtonMakeIndex.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                jLabelSizeIndex.setText("0");
                Daten.mVMediaDB.makeIndex();
            }
        });
        jButtonPath.setIcon(GetIcon.getProgramIcon("fileopen_16.png"));
        jButtonAdd.setIcon(GetIcon.getProgramIcon("add_16.png"));
        jButtonRemove.setIcon(GetIcon.getProgramIcon("remove_16.png"));
        jButtonPath.addActionListener(new BeobPath());
        jButtonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addPath();
            }
        });
        jButtonRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removePath();
            }
        }
        );
        jButtonHelp.setIcon(GetIcon.getProgramIcon("help_16.png"));
        jButtonHelp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DialogHilfe(daten.mediathekGui, true, new GetFile().getHilfeSuchen(GetFile.PFAD_HILFETEXT_PANEL_MEDIA_DB)).setVisible(true);
            }
        });

        jButtonExportPath.setIcon(GetIcon.getProgramIcon("fileopen_16.png"));
        jTextFieldExportPath.setText(Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_EXPORT_DATEI));
        jTextFieldExportPath.getDocument().addDocumentListener(new BeobTextFeld());
        jButtonExport.addActionListener(new BeobExport());
        jButtonExportPath.addActionListener(new BeobPfad());

        setTablePath();
    }

    private void filmeExportieren() {
        int ret;
        String exporDatei = jTextFieldExportPath.getText();
        if (exporDatei.equals("")) {
            MVMessageDialog.showMessageDialog(parentComponent, "Keine Datei angegeben", "Pfad", JOptionPane.INFORMATION_MESSAGE);
        } else {
            try {
                if (new File(exporDatei).exists()) {
                    ret = JOptionPane.showConfirmDialog(parentComponent, "Datei:  " + "\"" + exporDatei + "\"" + "  existiert bereits", "Überschreiben?",
                            JOptionPane.YES_NO_OPTION);
                } else {
                    ret = JOptionPane.OK_OPTION;
                }
                if (ret == JOptionPane.OK_OPTION) {
                    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    updateUI();
                    Daten.mVMediaDB.writeFileArray(exporDatei);
                    if (!new File(exporDatei).exists()) {
                        MVMessageDialog.showMessageDialog(parentComponent, "Datei:  " + "\"" + exporDatei + "\"" + "  Konnte nicht erstellt werden!", "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception ex) {
                Log.fehlerMeldung(732165489, ex);
            }
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void setIndex(boolean noIndex) {
        progress.setVisible(!noIndex);
        jTextFieldPath.setEnabled(noIndex);
        jButtonMakeIndex.setEnabled(noIndex);
        jButtonAdd.setEnabled(noIndex);
        jButtonPath.setEnabled(noIndex);
        jButtonRemove.setEnabled(noIndex);
    }

    private void addPath() {
        String db = Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_PATH_MEDIA);
        String add = jTextFieldPath.getText();
        if (add.isEmpty()) {
            return;
        }
        for (String s : db.split(Daten.mVMediaDB.FILE_TRENNER)) {
            if (s.equals(add)) {
                return; // dann gibts den schon
            }
        }
        if (db.isEmpty()) {
            db = add;
        } else {
            db += Daten.mVMediaDB.FILE_TRENNER + add;
        }
        Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_PATH_MEDIA, db);
        setTablePath(); //neu aufbauen
    }

    private void removePath() {
        int row = jTablePath.getSelectedRow();
        if (row >= 0) {
            String p = jTablePath.getModel().getValueAt(jTablePath.convertRowIndexToModel(row), 0).toString();
            String db = Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_PATH_MEDIA);
            String dbNew = "";
            if (db.isEmpty()) {
                return;
            }
            for (String s : db.split(Daten.mVMediaDB.FILE_TRENNER)) {
                if (s.equals(p)) {
                    continue;
                }
                dbNew += dbNew.isEmpty() ? s : Daten.mVMediaDB.FILE_TRENNER + s;
            }
            Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_PATH_MEDIA, dbNew);
            setTablePath(); //neu aufbauen
        } else {
            new HinweisKeineAuswahl().zeigen(daten.mediathekGui);
        }
    }

    private synchronized void setTablePath() {
        // Tabelle mit den Pfaden bauen
        String db = Daten.mVConfig.get(MVConfig.SYSTEM_MEDIA_DB_PATH_MEDIA);
        modelPath.setRowCount(0);
        if (!db.isEmpty()) {
            for (String s : db.split(Daten.mVMediaDB.FILE_TRENNER)) {
                modelPath.addRow(new Object[]{s});
            }
        }
    }


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jButtonHelp = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldPath = new javax.swing.JTextField();
        jButtonPath = new javax.swing.JButton();
        jButtonAdd = new javax.swing.JButton();
        jButtonRemove = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jCheckBoxMediaDB = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jButtonExportPath = new javax.swing.JButton();
        jTextFieldExportPath = new javax.swing.JTextField();
        jButtonExport = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablePath = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        jLabel2 = new javax.swing.JLabel();
        jLabelSizeIndex = new javax.swing.JLabel();
        jButtonMakeIndex = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jRadioButtonOhneSuffix = new javax.swing.JRadioButton();
        jRadioButtonMitSuffix = new javax.swing.JRadioButton();
        jTextFieldSuffix = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        jButton1.setText("jButton1");

        setMinimumSize(new java.awt.Dimension(2, 483));

        jButtonHelp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/programm/help_16.png"))); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)), "Pfad hinzufügen"));

        jLabel4.setText("Pfad:");

        jButtonPath.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/programm/fileopen_16.png"))); // NOI18N

        jButtonAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/programm/add_16.png"))); // NOI18N

        jButtonRemove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/programm/remove_16.png"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldPath)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonPath)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonAdd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRemove)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonRemove)
                    .addComponent(jButtonAdd)
                    .addComponent(jButtonPath)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(jTextFieldPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonAdd, jButtonPath, jButtonRemove, jTextFieldPath});

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        jCheckBoxMediaDB.setText("Echtzeitsuche in der Mediensammlung");

        jLabel1.setText("Export Index:");

        jButtonExportPath.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/programm/fileopen_16.png"))); // NOI18N

        jButtonExport.setText("Exportieren");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldExportPath)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonExportPath))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonExport))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jCheckBoxMediaDB)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxMediaDB)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldExportPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonExportPath))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonExport)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonExportPath, jTextFieldExportPath});

        jTablePath.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(jTablePath);

        jLabel5.setText("In den Pfaden suchen");

        jLabel2.setText("Dateien im Index:");

        jLabelSizeIndex.setText("0");

        jButtonMakeIndex.setText("Index neu aufbauen");
        jButtonMakeIndex.setToolTipText("");

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        buttonGroup1.add(jRadioButtonOhneSuffix);
        jRadioButtonOhneSuffix.setSelected(true);
        jRadioButtonOhneSuffix.setText("Keine Dateien mit diesem Suffix");

        buttonGroup1.add(jRadioButtonMitSuffix);
        jRadioButtonMitSuffix.setText("Nur Dateien mit diesem Suffix");

        jTextFieldSuffix.setText("txt,xml");

        jLabel6.setText("(z.B.: txt,xm,jpg)");

        jLabel7.setText("(z.B.: mp4,flv,m4v)");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jRadioButtonMitSuffix)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7))
                    .addComponent(jTextFieldSuffix)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jRadioButtonOhneSuffix)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel6)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonOhneSuffix)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonMitSuffix)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextFieldSuffix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonMakeIndex)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonHelp))
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelSizeIndex)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 103, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(jLabelSizeIndex))
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonMakeIndex)
                    .addComponent(jButtonHelp))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonExportPath;
    private javax.swing.JButton jButtonHelp;
    private javax.swing.JButton jButtonMakeIndex;
    private javax.swing.JButton jButtonPath;
    private javax.swing.JButton jButtonRemove;
    private javax.swing.JCheckBox jCheckBoxMediaDB;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabelSizeIndex;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JRadioButton jRadioButtonMitSuffix;
    private javax.swing.JRadioButton jRadioButtonOhneSuffix;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablePath;
    private javax.swing.JTextField jTextFieldExportPath;
    private javax.swing.JTextField jTextFieldPath;
    private javax.swing.JTextField jTextFieldSuffix;
    private javax.swing.JProgressBar progress;
    // End of variables declaration//GEN-END:variables

    private class BeobPath implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            //we can use native directory chooser on Mac...
            if (SystemInfo.isMacOSX()) {
                //we want to select a directory only, so temporarily change properties
                System.setProperty("apple.awt.fileDialogForDirectories", "true");
                FileDialog chooser = new FileDialog(daten.mediathekGui, "Pfad zu den Filmen wählen");
                chooser.setVisible(true);
                if (chooser.getFile() != null) {
                    //A directory was selected, that means Cancel was not pressed
                    try {
                        jTextFieldPath.setText(new File(chooser.getDirectory() + chooser.getFile()).getAbsolutePath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(951024789, ex);
                    }
                }
                System.setProperty("apple.awt.fileDialogForDirectories", "false");
            } else {
                //use the cross-platform swing chooser
                int returnVal;
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (!jTextFieldPath.getText().equals("")) {
                    chooser.setCurrentDirectory(new File(jTextFieldPath.getText()));
                }
                returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        jTextFieldPath.setText(chooser.getSelectedFile().getPath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(765212369, ex);
                    }
                }
            }
        }
    }

    private class BeobTextFeld implements DocumentListener {

        @Override
        public void changedUpdate(DocumentEvent e) {
            tusEinfach(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            tusEinfach(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            tusEinfach(e);
        }

        void tusEinfach(DocumentEvent e) {
            Daten.mVConfig.add(MVConfig.SYSTEM_MEDIA_DB_EXPORT_DATEI, jTextFieldExportPath.getText());
        }
    }

    private class BeobPfad implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            //we can use native chooser on Mac...
            if (SystemInfo.isMacOSX()) {
                FileDialog chooser = new FileDialog(daten.mediathekGui, "Filme exportieren");
                chooser.setMode(FileDialog.SAVE);
                chooser.setVisible(true);
                if (chooser.getFile() != null) {
                    try {
                        File destination = new File(chooser.getDirectory() + chooser.getFile());
                        jTextFieldExportPath.setText(destination.getAbsolutePath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(679890147, ex);
                    }
                }
            } else {
                int returnVal;
                JFileChooser chooser = new JFileChooser();
                if (!jTextFieldExportPath.getText().equals("")) {
                    chooser.setCurrentDirectory(new File(jTextFieldExportPath.getText()));
                }
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileHidingEnabled(false);
                returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        jTextFieldExportPath.setText(chooser.getSelectedFile().getAbsolutePath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(911025463, ex);
                    }
                }
            }
        }
    }

    private class BeobExport implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            filmeExportieren();
        }
    }

}