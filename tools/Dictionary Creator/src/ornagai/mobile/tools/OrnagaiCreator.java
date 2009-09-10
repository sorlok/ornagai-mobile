/*
 * OrnagaiCreator.java
 *
 * Created on Sep 10, 2009, 1:10:13 AM
 */

package ornagai.mobile.tools;


import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Seth N. Hetu
 */
public class OrnagaiCreator extends javax.swing.JApplet {
    //State
    private File dictionaryFile = null;
    private int numColumns = 0;
    private int colidWord = 0;
    private int colidPOS = 0;
    private int colidDefinition = 0;
    private int sampleTextID = -1;

    //For display and reordering.
    private ArrayList<String[]> allDictEntries = new ArrayList<String[]>();

    //For general usage
    private Random rand = new Random();


    //Read a file, load up some random entries in the hash table, etc.
    private void readNewDictionaryFile(File newFile) {
        //Set form elements
        dictionaryFile = newFile;
        txtPathToDictionary1.setText(dictionaryFile.getAbsolutePath());

        //Don't update
        sampleTextID = -1;

        //Read each entry, make sure all lines have the same number of tabs, etc.
        allDictEntries.clear();
        String errorMsg = "";
        int[] totalEntryLengths = null;
        int demoWord = 0;
        try {
            BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream(dictionaryFile), Charset.forName("UTF-8")));
            int lineID = 0;
            for (String line = inFile.readLine(); line!=null; line=inFile.readLine()) {
                //Skip empty lines
                line = line.trim();
                lineID++;
                if (line.length()==0)
                    continue;

                //Break
                String[] entries = line.split("\\t");
                if (allDictEntries.size()==0) {
                    numColumns = entries.length;
                    totalEntryLengths = new int[numColumns];
                } else if (numColumns != entries.length) {
                    errorMsg = "Line ["+lineID+"] contains " + entries.length + " entries when " + numColumns + " was expected.";
                    break;
                }

                //Store
                allDictEntries.add(entries);
                for (int i=0; i<numColumns; i++)
                    totalEntryLengths[i] += entries[i].length();

                //Pick a good demo word
                if (demoWord==0) {
                    if (line.contains("scion"))
                        demoWord = allDictEntries.size()-1;
                }
            }
            inFile.close();
        } catch (FileNotFoundException ex) {
            errorMsg = "File not found: " + ex.toString();
        } catch (UnsupportedEncodingException ex) {
            errorMsg = "Unsupported encoding: " + ex.toString();
        } catch (IOException ex) {
            errorMsg = "IO exception: " + ex.toString();
        }

        //Problem?
        if (errorMsg.length()>0) {
            JOptionPane.showMessageDialog(this, errorMsg, "Error Reading Dictionary File", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Add entries to the combo boxes.
        cmbStructWord1.removeAllItems();
        cmbStructPOS1.removeAllItems();
        cmbStructDef1.removeAllItems();
        for (int i=0; i<=numColumns; i++) {
            String newRow = (i<numColumns) ? "Column " + (i+1) : "(Don't include)";
            cmbStructWord1.addItem(newRow);
            cmbStructPOS1.addItem(newRow);
            cmbStructDef1.addItem(newRow);
        }

        //Determine which column is which.
        colidWord = numColumns;
        colidPOS = numColumns;
        colidDefinition = numColumns;
        if (numColumns==1)
            colidWord = 0;
        else if (numColumns==2) {
            colidWord = 0;
            colidDefinition = 1;
        } else {
            //First: the longest column is the definition list
            colidDefinition = 0;
            for (int i=1; i<numColumns; i++) {
                if (totalEntryLengths[i] > totalEntryLengths[colidDefinition])
                    colidDefinition = i;
            }

            //Next: the first non-definition row is the word
            for (colidWord=0; colidWord<numColumns; colidWord++) {
                if (colidWord != colidDefinition)
                    break;
            }

            //Finally: the first non-word, non-definition row is the pos
            for (colidPOS=0; colidPOS<numColumns; colidPOS++) {
                if ((colidPOS!=colidWord) && (colidPOS!=colidDefinition))
                    break;
            }
        }

        //Update our combo boxes.
        cmbStructWord1.setSelectedIndex(colidWord);
        cmbStructPOS1.setSelectedIndex(colidPOS);
        cmbStructDef1.setSelectedIndex(colidDefinition);

        //Finally, pick a word and show it in our Sample label
        sampleTextID = demoWord;
        reloadSampleText();

        //And now that that's all done....
        recheckEnabledComponents();
    }


    //Reload our "sample" panel
    private void reloadSampleText() {
        //Clear, and possibly do nothing
        lblSampleEntry1.setText("");
        if (sampleTextID==-1)
            return;

        //Load our entry by ID
        String[] entry = allDictEntries.get(sampleTextID);
        String word = (colidWord<numColumns) ? entry[colidWord] : "";
        String pos = (colidPOS<numColumns) ? entry[colidPOS] : "";
        String defRaw = (colidDefinition<numColumns) ? entry[colidDefinition] : "";

        //Add some optional hyphens to myanmar words, in obvious places
        StringBuffer def = new StringBuffer();
        for (int i=0; i<defRaw.length(); i++) {
            char c = defRaw.charAt(i);
            def.append(c);
            ZawgyiSegment.SEMANTICS sem = ZawgyiSegment.getSemantics(c);
            if (sem == ZawgyiSegment.SEMANTICS.MY_VIRAMA) {
                //Generally obvious cases
                if (i+1<defRaw.length()) {
                    char nextC = defRaw.charAt(i+1);
                    ZawgyiSegment.SEMANTICS nextSem = ZawgyiSegment.getSemantics(nextC);
                    if (nextSem == ZawgyiSegment.SEMANTICS.MY_BASE || nextSem==ZawgyiSegment.SEMANTICS.MY_LEADING) {
                        def.append("&shy;");
                    }
                }
            }
        }

        //Now, get our entries and build an HTML string
        StringBuffer sb = new StringBuffer();
        sb.append("<html><div style=\"background:#DDDDDD; width:142px; height:87px;\">");
        sb.append("<b>"+word+"</b>");
        sb.append("<br>");
        sb.append("<i>"+pos+"</i>");
        sb.append("<br>");
        sb.append(def.toString());
        sb.append("</div></html>");
        lblSampleEntry1.setText(sb.toString());
    }

    //Which components should and should not be shown?
    private void recheckEnabledComponents() {
        //Show stage 2 dictionary components?
        Component[] dictComponents = new Component[]{cmbStructWord1, cmbStructPOS1, cmbStructDef1, btnRandomDictEntry1, btnNext1};
        for (Component cmp : dictComponents) {
            cmp.setEnabled(dictionaryFile!=null);
        }

    }


    /** Initializes the applet OrnagaiCreator */
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    initComponents();
                    getContentPane().setBackground(getBackground()); //Weird...

                    //Now, remove all formatted text.
                    txtPathToDictionary1.setText("");
                    cmbStructWord1.removeAllItems();
                    cmbStructPOS1.removeAllItems();
                    cmbStructDef1.removeAllItems();
                    lblSampleEntry1.setText("");

                    //For consistency
                    recheckEnabledComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        firstPnl1 = new javax.swing.JPanel();
        btnNext1 = new javax.swing.JButton();
        lblSampleEntry1 = new javax.swing.JLabel();
        lblSample1 = new javax.swing.JLabel();
        cmbStructPOS1 = new javax.swing.JComboBox();
        cmbStructDef1 = new javax.swing.JComboBox();
        lblDefinition1 = new javax.swing.JLabel();
        cmbStructWord1 = new javax.swing.JComboBox();
        lblStructPOS1 = new javax.swing.JLabel();
        btnBrowseForDict1 = new javax.swing.JButton();
        txtPathToDictionary1 = new javax.swing.JTextField();
        lblChooseFile1 = new javax.swing.JLabel();
        lblCurrentPage1 = new javax.swing.JLabel();
        lblStructWord1 = new javax.swing.JLabel();
        lblStructure1 = new javax.swing.JLabel();
        btnRandomDictEntry1 = new javax.swing.JButton();

        setBackground(new java.awt.Color(153, 255, 204));
        setName("firstPnl"); // NOI18N

        jLayeredPane1.setPreferredSize(new java.awt.Dimension(463, 421));

        firstPnl1.setOpaque(false);
        firstPnl1.setPreferredSize(new java.awt.Dimension(463, 421));

        btnNext1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        btnNext1.setText("Next");
        btnNext1.setEnabled(false);

        lblSampleEntry1.setBackground(new java.awt.Color(255, 255, 255));
        lblSampleEntry1.setFont(new java.awt.Font("Zawgyi-One", 0, 12)); // NOI18N
        lblSampleEntry1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSampleEntry1.setText("<html><div style=\"background:#DDDDDD; width:142px; height:87px;\"><b>scion</b><br><i>n</i><br>\u1021\u1019\u103A\u102D\u102F\u1038\u1031\u1000\u102C\u1004\u1039\u1038\u101E\u102C\u1038\u104B \u1019\u103A\u102D\u102F\u1038\u1006\u1000\u1039\u104B \u1021\u1015\u1004\u1039\u1015\u103C\u102C\u1038\u101A\u1030\u101B\u1014\u1039\u103B\u1016\u1010\u1039\u1011\u102F\u1010\u1039\u101E\u100A\u1039&shy;\u1037\u1015\u1004\u1039\u1005\u100A\u1039\u1021\u1015\u102D\u102F\u1004\u1039\u1038\u1021\u1005\u104B \u1015\u1004\u1039\u1015\u103C\u102C\u1038\u1019\u103A\u102D\u102F\u1038\u1006\u1000\u1039\u104B</div></html>");
        lblSampleEntry1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblSampleEntry1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblSampleEntry1.setOpaque(true);

        lblSample1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblSample1.setText("Sample:");

        cmbStructPOS1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructPOS1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructPOS1.setSelectedIndex(1);
        cmbStructPOS1.setEnabled(false);
        cmbStructPOS1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructPOS1ActionPerformed(evt);
            }
        });

        cmbStructDef1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructDef1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructDef1.setSelectedIndex(2);
        cmbStructDef1.setEnabled(false);
        cmbStructDef1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructDef1ActionPerformed(evt);
            }
        });

        lblDefinition1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblDefinition1.setText("Definition:");

        cmbStructWord1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructWord1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructWord1.setEnabled(false);
        cmbStructWord1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructWord1ActionPerformed(evt);
            }
        });

        lblStructPOS1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblStructPOS1.setText("P.O.S.:");

        btnBrowseForDict1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        btnBrowseForDict1.setText("...");
        btnBrowseForDict1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseForDict1ActionPerformed(evt);
            }
        });

        txtPathToDictionary1.setFont(new java.awt.Font("Courier New", 0, 12));
        txtPathToDictionary1.setText("D:\\Open Source Projects\\ornagai_dict\\NEW! dictionary (tsv) 2008\\convert_entab.tsv");
        txtPathToDictionary1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPathToDictionary1ActionPerformed(evt);
            }
        });

        lblChooseFile1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblChooseFile1.setText("Choose your dictionary:");

        lblCurrentPage1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblCurrentPage1.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage1.setText("<html><span style=\"font-weight: bold; color:black; font-size:26pt;\">1</span>..2..3</html>");

        lblStructWord1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblStructWord1.setText("Word:");

        lblStructure1.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblStructure1.setText("Dictionary Structure:");

        btnRandomDictEntry1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        btnRandomDictEntry1.setText("(Random Entry)");
        btnRandomDictEntry1.setEnabled(false);
        btnRandomDictEntry1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRandomDictEntry1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout firstPnl1Layout = new javax.swing.GroupLayout(firstPnl1);
        firstPnl1.setLayout(firstPnl1Layout);
        firstPnl1Layout.setHorizontalGroup(
            firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(firstPnl1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(firstPnl1Layout.createSequentialGroup()
                                .addComponent(lblStructure1)
                                .addGap(81, 81, 81)
                                .addComponent(lblSample1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRandomDictEntry1))
                            .addComponent(lblChooseFile1)
                            .addGroup(firstPnl1Layout.createSequentialGroup()
                                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblStructWord1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructWord1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(txtPathToDictionary1, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblStructPOS1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructPOS1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblDefinition1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructDef1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(btnBrowseForDict1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addComponent(lblSampleEntry1, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)))))
                        .addGap(272, 272, 272))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnNext1, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblCurrentPage1))
                        .addGap(250, 250, 250))))
        );
        firstPnl1Layout.setVerticalGroup(
            firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(firstPnl1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCurrentPage1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblChooseFile1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnBrowseForDict1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtPathToDictionary1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStructure1)
                    .addComponent(lblSample1)
                    .addComponent(btnRandomDictEntry1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructWord1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStructWord1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructPOS1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStructPOS1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructDef1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblDefinition1)))
                    .addComponent(lblSampleEntry1, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(btnNext1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        firstPnl1.setBounds(0, 0, 520, 380);
        jLayeredPane1.add(firstPnl1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 518, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 374, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnBrowseForDict1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseForDict1ActionPerformed
        //Open a file chooser
        String path = ".";
        if (dictionaryFile!=null)
            path = dictionaryFile.getParent();
        JFileChooser dictChooser = new JFileChooser(new File("."));
        dictChooser.setFileFilter(new FileNameExtensionFilter("Tab-separated dictionaries", "tsv", "txt"));
        dictChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        dictChooser.setMultiSelectionEnabled(false);
        if (dictChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        //They approved; now, get the file.
        File newDictFile = dictChooser.getSelectedFile();
        if (newDictFile.exists())
            readNewDictionaryFile(newDictFile);
    }//GEN-LAST:event_btnBrowseForDict1ActionPerformed

    private void txtPathToDictionary1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPathToDictionary1ActionPerformed
        String filePath = txtPathToDictionary1.getText();
        File newDictFile = new File(filePath);
        if (!newDictFile.exists()) {
            JOptionPane.showMessageDialog(this, "Dictionary file cannot be found at: \n\n"+filePath, "File Not Found", JOptionPane.ERROR_MESSAGE);
            txtPathToDictionary1.setText("");
            if (dictionaryFile!=null)
                txtPathToDictionary1.setText(dictionaryFile.getAbsolutePath());
            return;
        }

        //Found it
        readNewDictionaryFile(newDictFile);
    }//GEN-LAST:event_txtPathToDictionary1ActionPerformed

    private void btnRandomDictEntry1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRandomDictEntry1ActionPerformed
        sampleTextID = rand.nextInt(allDictEntries.size());
        reloadSampleText();
    }//GEN-LAST:event_btnRandomDictEntry1ActionPerformed

    private void cmbStructWord1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructWord1ActionPerformed
        colidWord = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructWord1ActionPerformed

    private void cmbStructPOS1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructPOS1ActionPerformed
        colidPOS = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructPOS1ActionPerformed

    private void cmbStructDef1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructDef1ActionPerformed
        colidDefinition = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructDef1ActionPerformed





    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowseForDict;
    private javax.swing.JButton btnBrowseForDict1;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnNext1;
    private javax.swing.JButton btnRandomDictEntry1;
    private javax.swing.JComboBox cmbStructDef;
    private javax.swing.JComboBox cmbStructDef1;
    private javax.swing.JComboBox cmbStructPOS;
    private javax.swing.JComboBox cmbStructPOS1;
    private javax.swing.JComboBox cmbStructWord;
    private javax.swing.JComboBox cmbStructWord1;
    private javax.swing.JPanel firstPnl;
    private javax.swing.JPanel firstPnl1;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLabel lblChooseFile;
    private javax.swing.JLabel lblChooseFile1;
    private javax.swing.JLabel lblCurrentPage;
    private javax.swing.JLabel lblCurrentPage1;
    private javax.swing.JLabel lblDefinition;
    private javax.swing.JLabel lblDefinition1;
    private javax.swing.JLabel lblSample;
    private javax.swing.JLabel lblSample1;
    private javax.swing.JLabel lblSampleEntry;
    private javax.swing.JLabel lblSampleEntry1;
    private javax.swing.JLabel lblStructPOS;
    private javax.swing.JLabel lblStructPOS1;
    private javax.swing.JLabel lblStructWord;
    private javax.swing.JLabel lblStructWord1;
    private javax.swing.JLabel lblStructure;
    private javax.swing.JLabel lblStructure1;
    private javax.swing.JTextField txtPathToDictionary;
    private javax.swing.JTextField txtPathToDictionary1;
    // End of variables declaration//GEN-END:variables

}
