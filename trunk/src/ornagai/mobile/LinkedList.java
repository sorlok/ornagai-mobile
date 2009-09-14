package ornagai.mobile;

/**
 *
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
