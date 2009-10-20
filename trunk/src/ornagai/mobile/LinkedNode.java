/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

/**
 * Main node class for a linked list. Contains a "next" node and some data.
 *   Not two-way traversible.
 * @author Seth N. Hetu
 */
public class LinkedNode {
    public LinkedNode next;
    public Object data;

    public LinkedNode(Object data) {
        this.data = data;
    }
}
