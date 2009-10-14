package ornagai.mobile.gui;

/**
 *
 * @author Seth N. Hetu
 */
public interface FormController {
    public abstract void switchToSplashForm();
    public abstract void switchToOptionsForm();
    public abstract void switchToDictionaryForm();
    public abstract void reloadDictionary();
    public abstract void waitForDictionaryToLoad();
    public abstract void closeProgram();
}
