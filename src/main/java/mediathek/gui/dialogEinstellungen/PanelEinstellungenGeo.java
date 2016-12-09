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

import mSearch.daten.DatenFilm;
import mSearch.tool.Listener;
import mediathek.config.Daten;
import mediathek.config.Icons;
import mediathek.config.MVConfig;
import mediathek.file.GetFile;
import mediathek.gui.PanelVorlage;
import mediathek.gui.dialog.DialogHilfe;

import javax.swing.*;

@SuppressWarnings("serial")
public class PanelEinstellungenGeo extends PanelVorlage {
    public PanelEinstellungenGeo(Daten d, JFrame pparentComponent) {
        super(d, pparentComponent);
        initComponents();
        daten = d;
        init();
    }

    private void init() {
        switch (MVConfig.get(MVConfig.Configs.SYSTEM_GEO_STANDORT)) {
            case DatenFilm.GEO_CH:
                jRadioButtonCH.setSelected(true);
                break;
            case DatenFilm.GEO_AT:
                jRadioButtonAt.setSelected(true);
                break;
            case DatenFilm.GEO_EU:
                jRadioButtonEu.setSelected(true);
                break;
            case DatenFilm.GEO_WELT:
                jRadioButtonSonst.setSelected(true);
                break;
            default:
                jRadioButtonDe.setSelected(true);
        }
        jRadioButtonDe.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_STANDORT, DatenFilm.GEO_DE);
            melden();
        });
        jRadioButtonCH.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_STANDORT, DatenFilm.GEO_CH);
            melden();
        });
        jRadioButtonAt.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_STANDORT, DatenFilm.GEO_AT);
            melden();
        });
        jRadioButtonEu.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_STANDORT, DatenFilm.GEO_EU);
            melden();
        });
        jRadioButtonSonst.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_STANDORT, DatenFilm.GEO_WELT);
            melden();
        });
        jCheckBoxMarkieren.setSelected(Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_GEO_MELDEN)));
        jCheckBoxMarkieren.addActionListener(e -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_GEO_MELDEN, String.valueOf(jCheckBoxMarkieren.isSelected()));
            melden();
        });
        jButtonHilfe.setIcon(Icons.ICON_BUTTON_HELP);
        jButtonHilfe.addActionListener(e -> new DialogHilfe(parentComponent, true, new GetFile().getHilfeSuchen(GetFile.PFAD_HILFETEXT_GEO)).setVisible(true));
    }

    private void melden() {
        daten.getListeBlacklist().filterListe();
        Listener.notify(Listener.EREIGNIS_GEO, PanelEinstellungenGeo.class.getName());
        Listener.notify(Listener.EREIGNIS_BLACKLIST_GEAENDERT, PanelEinstellungenGeo.class.getSimpleName());

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.ButtonGroup buttonGroup1 = new javax.swing.ButtonGroup();
        javax.swing.JPanel jPanel6 = new javax.swing.JPanel();
        jCheckBoxMarkieren = new javax.swing.JCheckBox();
        jRadioButtonDe = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jRadioButtonCH = new javax.swing.JRadioButton();
        jRadioButtonAt = new javax.swing.JRadioButton();
        jRadioButtonEu = new javax.swing.JRadioButton();
        jRadioButtonSonst = new javax.swing.JRadioButton();
        jButtonHilfe = new javax.swing.JButton();

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Geogeblockte Filme"));

        jCheckBoxMarkieren.setText("geblockte Sendungen gelb markieren");

        buttonGroup1.add(jRadioButtonDe);
        jRadioButtonDe.setSelected(true);
        jRadioButtonDe.setText("DE - Deutschland");

        jLabel1.setText("Mein Standort:");

        buttonGroup1.add(jRadioButtonCH);
        jRadioButtonCH.setText("CH - Schweiz");

        buttonGroup1.add(jRadioButtonAt);
        jRadioButtonAt.setText("AT - Österreich");

        buttonGroup1.add(jRadioButtonEu);
        jRadioButtonEu.setText("EU (EBU - European Broadcasting Union)");

        buttonGroup1.add(jRadioButtonSonst);
        jRadioButtonSonst.setText("sonst");

        jButtonHilfe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/muster/button-help.png"))); // NOI18N
        jButtonHilfe.setToolTipText("Hilfe anzeigen");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jRadioButtonSonst)
                    .addComponent(jRadioButtonEu)
                    .addComponent(jCheckBoxMarkieren)
                    .addComponent(jLabel1)
                    .addComponent(jRadioButtonDe)
                    .addComponent(jRadioButtonCH)
                    .addComponent(jRadioButtonAt))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButtonHilfe)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCheckBoxMarkieren)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonDe)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonCH)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonAt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonEu)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRadioButtonSonst)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                .addComponent(jButtonHilfe)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonHilfe;
    private javax.swing.JCheckBox jCheckBoxMarkieren;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JRadioButton jRadioButtonAt;
    private javax.swing.JRadioButton jRadioButtonCH;
    private javax.swing.JRadioButton jRadioButtonDe;
    private javax.swing.JRadioButton jRadioButtonEu;
    private javax.swing.JRadioButton jRadioButtonSonst;
    // End of variables declaration//GEN-END:variables

}
