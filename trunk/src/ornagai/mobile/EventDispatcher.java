/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

import com.sun.lwuit.events.*;
import java.util.Vector;

/**
 * Handle events fired from a LWUIT List. Originally, this code was a stand-in
 *  for the then-unreleased-LWUIT equivalent. I have since rewritten it to
 *  match only the necessary functionality of the ornagai-mobile project, to
 *  avoid a possible licensing conflict (although the binary distribution of
 *  LWUIT's source makes this far from clear-cut). It is released under the
 *  terms of the MIT license.
 *
 * This class is not really type-safe, but simply upgrading LWUIT to the latest
 *  version will remove the need for this class. 
 *
 * @author Seth N. Hetu
 */
public class EventDispatcher {
    //Main class data
    private Vector listeners = new Vector(); //Vector<SelectionListener>

    //Add a selection listener
    public void addListener(SelectionListener listener) {
        listeners.addElement(listener);
    }

    //Remove a selection listener
    public void removeListener(SelectionListener listener) {
        listeners.removeElement(listener);
    }

    //Fire selection events
    public void fireSelectionEvent(int oldSelection, int newSelection) {
        //Avoid meaningless processing
        if(listeners.isEmpty())
            return;

        //Fire the selection event for each element in the listeners array
        for (int i=0; i<listeners.size(); i++) {
            ((SelectionListener)listeners.elementAt(i)).selectionChanged(oldSelection, newSelection);
        }
    }
}
