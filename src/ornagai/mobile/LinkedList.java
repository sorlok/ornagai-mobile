/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

/**
 * Simple linked list. Use it like any other. Limited functionality, since our
 *  program doesn't need that much. Definitely not thread-safe.
 * @author Seth N. Hetu
 */
public class LinkedList {
    public LinkedNode head;
    public LinkedNode tail;

    public void addToEnd(LinkedNode ln) {
        //First?
        if (head==null)
            head = ln;

        //Increment
        if (tail!=null)
            tail.next = ln;
        ln.next = null;
        tail = ln;
    }
}
