package ornagai.mobile.filebrowser;

import com.sun.lwuit.Command;
import com.sun.lwuit.Component;
import com.sun.lwuit.Container;
import com.sun.lwuit.Form;
import com.sun.lwuit.Image;
import com.sun.lwuit.Label;
import com.sun.lwuit.List;
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
        FileChooser.folderIcons[1] = folderIcon;
        FileChooser.folderIcons[2] = emptyFolderIcon;

        //Start
        if (FileChooser.chooserForm == null)
            FileChooser.createChooserForm();
        FileChooser.browseToDir(defaultPath, false);
        FileChooser.chooserForm.show();
    }

    private static void createChooserForm() {
        //Top-level form
        chooserForm = new Form();
        chooserForm.setLayout(new BorderLayout());
        chooserForm.setCommandListener(singleton);
        cancelCmd = new Command("Cancel");
        chooserForm.addCommand(cancelCmd);
        okCmd = new Command("Ok");
        chooserForm.addCommand(okCmd);

        //Give it a list component
        fileListData = new DefaultListModel();
        fileList = new List(fileListData);
        fileList.setNumericKeyActions(false);
        fileList.addActionListener(singleton);
        fileList.setListCellRenderer(new FileRenderer());

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
        if (!accept)
            currPath = null;
        if (onClose!=null)
            onClose.actionPerformed(new ActionEvent(currPath));
        previousPage.show();
    }



    private static void browseToDir(String path, boolean selectOnFind) {
        //Set, clear
        currPath = path;
        fileListData.removeAll();

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
             fileListData.addItem(new FileIcon(root, root, folderIcons[0]));
          }
          return;
        }

        //It's a directory or a (real) root; list all sub-folders
        fileListData.addItem(new FileIcon(path, "..", backIcon));
        Vector contents = FileChooser.listContents(path);
        Vector nonFolders = new Vector();
        for (int i=0; i<contents.size(); i++) {
            String name = (String)contents.elementAt(i);
            try {
                FileConnection fc = (FileConnection)Connector.open(path + fs + name);
                if (fc.isDirectory()) {
                    //Is it empty?
                    boolean empty = listContents(path + fs + name).size()==0;

                    //Add it
                    fileListData.addItem(new FileIcon(path + fs + name, name, empty ? folderIcons[1] : folderIcons[2]));
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
            fileListData.addItem(new FileIcon(path + fs + name, name, fileIcons[fileID]));
        }
    }


    private static Vector listContents(String path) {
        Vector res = new Vector();
        try {
            FileConnection fc = (FileConnection) Connector.open(path, Connector.READ);
            Enumeration en = fc.list("*", true);
            while(en.hasMoreElements()) {
                String fileName = (String)en.nextElement();
                int fileID = -1;
                for (int i=0; i<fileSuffixes.length; i++) {
                    if (fileName.endsWith(fileSuffixes[i])) {
                        fileID = i;
                        break;
                    }
                }
                if (fileID>-1)
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


