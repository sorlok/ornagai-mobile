/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile.gui;

/**
 * General hook to the main MIDlet. Centralizes control.
 * 
 * @author Seth N. Hetu
 */
public interface FormController {
    public abstract void switchToSplashForm();
    public abstract void switchToOptionsForm();
    public abstract void switchToDictionaryForm();
    public abstract boolean reloadDictionary();
    public abstract void waitForDictionaryToLoad();
    public abstract void closeProgram();
}

