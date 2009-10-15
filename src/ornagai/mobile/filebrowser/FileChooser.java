package ornagai.mobile.filebrowser;

import com.sun.lwuit.Button;
import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.list.DefaultListModel;
import com.sun.lwuit.list.ListCellRenderer;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 *
 * @author Seth N. Hetu
 */
public class FileChooser implements ActionListener {
    //State
    private static Form previousPage;
    private static ActionListener onClose;
    private static String[] fileSuffixes;
    private static Image[] fileIcons;
    private static Image backIcon;
    private static Image[] folderIcons = new Image[3]; //root, empty folder, full folder

    //Show
    private static Form chooserForm;
    private static Command cancelCmd;
    private static Command okCmd;
    private static List fileList;
    private static Label fileTypesLbl;
    private static DefaultListModel fileListData;
    private static String currPath;

    //Useful
    private static FileChooser singleton = new FileChooser();
    private static char fs = '/';

    //Keep it internal
    protected FileChooser(){}


    //Simple call and return, with cancel and ok commands
    //  Returns null if nothing was selected. Else, returns the path of the selected item.
    //  Null or empty default path for root
    public static void browseForFile(Form previousPage, String defaultPath, String[] fileSuffixes, Image[] fileIcons, Image folderIcon, Image emptyFolderIcon, Image rootIcon, Image backIcon, ActionListener onClose) {
        //Check
        if (fileSuffixes.length != fileIcons.length)
            throw new IllegalArgumentException("File suffixes and file icons arrays must be the same length.");
        if (defaultPath == null)
            defaultPath = "";

        //Save
        FileChooser.previousPage = previousPage;
        FileChooser.onClose = onClose;
        FileChooser.fileSuffixes = fileSuffixes;
        FileChooser.fileIcons = fileIcons;
        FileChooser.backIcon = backIcon;
        FileChooser.folderIcons[0] = rootIcon;
        FileChooser.folderIcons[1] = emptyFolderIcon;
        FileChooser.folderIcons[2] = folderIcon;

        //Init
        if (FileChooser.chooserForm == null)
            FileChooser.createChooserForm();
        StringBuffer sb = new StringBuffer("Type: ");
        String comma = "";
        for (int i=0; i<fileSuffixes.length; i++) {
            sb.append(comma + "*." + fileSuffixes[i]);
            comma = " , ";
        }
        fileTypesLbl.setText(sb.toString());

        //Show
        FileChooser.browseToDir(defaultPath, false);
        FileChooser.fileList.requestFocus();
        FileChooser.chooserForm.show();
    }

    private static void createChooserForm() {
        //Top-level form
        chooserForm = new Form();
        chooserForm.setLayout(new BorderLayout());
        chooserForm.setCommandListener(singleton);
        chooserForm.getStyle().setBgColor(0x333333);
        cancelCmd = new Command("Cancel");
        chooserForm.addCommand(cancelCmd);
        okCmd = new Command("Ok");
        chooserForm.addCommand(okCmd);

        //Give it a list component
        FileRenderer renderer = new FileRenderer();
        renderer.getStyle().setBorder(Border.createEmpty());
        fileListData = new DefaultListModel();
        fileList = new List(fileListData);
        fileList.setNumericKeyActions(false);
        fileList.addActionListener(singleton);
        fileList.setListCellRenderer(renderer);
        fileList.getStyle().setBorder(Border.createLineBorder(1, 0xFFFFFF));
        fileList.getStyle().setBgColor(0xFFFFFF);
        chooserForm.addComponent(BorderLayout.CENTER, fileList);

        //Add some more useful bits
        Label titleLbl = new Label("Select a file:");
        titleLbl.getStyle().setBgTransparency(0);
        titleLbl.getStyle().setFgColor(0xBBBBBB);
        chooserForm.addComponent(BorderLayout.NORTH, titleLbl);

        //Add some more useful bits
        fileTypesLbl = new Label("Select a file:");
        fileTypesLbl.getStyle().setBgTransparency(0);
        fileTypesLbl.getStyle().setFgColor(0xBBBBBB);
        chooserForm.addComponent(BorderLayout.SOUTH, fileTypesLbl);

        //File separator
        String fsStr = System.getProperty("file.separator");
        if (fsStr!=null) {
            if (fsStr.length()>1)
                throw new IllegalArgumentException("File separator is too long: " + fsStr);
            fs = fsStr.charAt(0);
        }
    }

    public void actionPerformed(ActionEvent args) {
        if (args.getCommand() == okCmd)
            FileChooser.closeForm(true);
        else if (args.getCommand() == cancelCmd)
            FileChooser.closeForm(false);
        else if (args.getSource() == (Object) fileList) {
            FileIcon item = (FileIcon)fileList.getSelectedItem();
            FileChooser.browseToDir(item.fullPath, true);
        }
    }

    private static void closeForm(boolean accept) {
        //Remove all memory-intensive storage
        FileChooser.fileSuffixes = null;
        FileChooser.fileIcons = null;
        FileChooser.backIcon = null;
        FileChooser.folderIcons[0] = null;
        FileChooser.folderIcons[1] = null;
        FileChooser.folderIcons[2] = null;
        fileListData.removeAll();

        //Close the form
        if (!accept)
            currPath = null;
        if (onClose!=null)
            onClose.actionPerformed(new ActionEvent(currPath));
        previousPage.show();

        //Remove a few more stored instances; this should prevent memory leaks.
        FileChooser.previousPage = null;
        FileChooser.onClose = null;
    }



    private static void browseToDir(String path, boolean selectOnFind) {
        //Set, clear
        currPath = path;
        fileListData.removeAll();

        //System.out.println("Browsing to path: " + path);

        //Does this path exist? Is it a file?
        if (path!=null && path.length()!=0) {
            try {
                FileConnection fc = (FileConnection) Connector.open(path, Connector.READ);
                if (fc.exists()) {
                    if (!fc.isDirectory()) {
                        if (selectOnFind) {
                            FileChooser.closeForm(true);
                            return;
                        }
                        int parent = path.lastIndexOf(fs);
                        path = path.substring(0, parent);
                    }
                } else
                    path = null;
                fc.close();
            } catch (IOException ex)  {
                path = null;
            } catch (SecurityException ex) {
                path = null;
            }
        }

        //Is it root?
        if (path==null || path.length()==0) {
          Enumeration drives = FileSystemRegistry.listRoots();
          while(drives.hasMoreElements()) {
             String root = (String)drives.nextElement();
             fileListData.addItem(new FileIcon("file:///"+root, root, folderIcons[0]));
          }
          return;
        }

        //It's a directory or a (real) root; list all sub-folders
        String parentPath = path.endsWith(""+fs) ? path.substring(0, path.length()-1) : path;
        int slashID = parentPath.lastIndexOf(fs);
        parentPath = parentPath.substring(0, slashID);
        if (parentPath.equals("file://"))
            parentPath = "";
        else
            parentPath += fs;
        fileListData.addItem(new FileIcon(parentPath, "..", backIcon));
        Vector contents = FileChooser.listContents(path);
        Vector nonFolders = new Vector();
        for (int i=0; i<contents.size(); i++) {
            String name = (String)contents.elementAt(i);
            try {
                FileConnection fc = (FileConnection)Connector.open(appendPath(path, fs, name));
                if (fc.isDirectory()) {
                    //Is it empty?
                    boolean empty = listContents(appendPath(path, fs, name)).size()==0;

                    //Add it
                    fileListData.addItem(new FileIcon(appendPath(path, fs, name), name, empty ? folderIcons[1] : folderIcons[2]));
                } else
                    nonFolders.addElement(name);
            } catch (IOException ex) {} catch (SecurityException ex) {}
        }

        //Now, add all single files
        for (int i=0; i<nonFolders.size(); i++) {
            //Get its icon
            String name = (String)nonFolders.elementAt(i);
            int fileID = -1;
            for (int x=0; x<fileSuffixes.length; x++) {
                if (name.endsWith(fileSuffixes[x])) {
                    fileID = x;
                    break;
                }
            }
            fileListData.addItem(new FileIcon(appendPath(path, fs, name), name, fileIcons[fileID]));
        }
    }

    
    private static String appendPath(String prefix, char fs, String suffix) {
        if (prefix.endsWith(fs+""))
            return prefix + suffix;
        else
            return prefix + fs + suffix;
    }


    private static Vector listContents(String path) {
        Vector res = new Vector();
        try {
            FileConnection fc = (FileConnection) Connector.open(path, Connector.READ);
            Enumeration en = fc.list("*", true);
            while(en.hasMoreElements()) {
                String fileName = (String)en.nextElement();
                fc = (FileConnection)Connector.open(appendPath(path, fs, fileName), Connector.READ);

                boolean ok = fc.isDirectory();
                for (int i=0; i<fileSuffixes.length && !ok; i++) {
                    if (fileName.endsWith(fileSuffixes[i]) || fileSuffixes[i].equals("*"))
                        ok = true;
                }
                if (ok)
                    res.addElement(fileName);
            }
            fc.close();
        } catch (IOException ex) {
            res.removeAllElements();
        } catch (SecurityException ex) {
            res.removeAllElements();
        }

        return res;
    }



    static class FileIcon {
        public String fullPath;
        public String name;
        public Image icon;
        public FileIcon(String fullPath, String name, Image icon) {
            this.fullPath = fullPath;
            this.name = name;
            this.icon = icon;
        }
    }


    static class FileRenderer extends Container implements ListCellRenderer {
      private Label filePath = new Label("");
      private Label fileIcon = new Label("");
      private Label focus = new Label("");

      public FileRenderer() {
          setLayout(new BorderLayout());
          addComponent(BorderLayout.WEST, fileIcon);
          addComponent(BorderLayout.CENTER, filePath);

          filePath.getStyle().setBorder(Border.createEmpty());
          filePath.getStyle().setBgTransparency(0);
          fileIcon.getStyle().setBorder(Border.createEmpty());
          fileIcon.getStyle().setBgTransparency(0);

          focus.getStyle().setFgColor(0x0000FF);
          focus.getStyle().setBorder(Border.createLineBorder(2, 0x0000FF));
          focus.getStyle().setBgTransparency(100);
      }

      public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
          FileIcon item = (FileIcon)value;
          filePath.setText(item.name);
          fileIcon.setText("");
          fileIcon.setIcon(item.icon);
          return this;
      }

      public Component getListFocusComponent(List list) {
          return focus;
      }
    }
}


