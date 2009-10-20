/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

import com.sun.lwuit.*;
import com.sun.lwuit.plaf.UIManager;

/**
 * A round button. The current LWUIT has a "rounded" property for buttons, but
 *   I've never used it. Feel free to remove this class if you see fit.
 * Based partly on the blog post: http://lwuit.blogspot.com/2008/06/many-roads-to-round-buttons-advanced.html
 *   ...as usual, though, I've rewritten a great deal of the code myself.
 * @author Seth N. Hetu
 */
public class RoundButton extends Button {
    public RoundButton(String title) {
        super(title);
    }

    protected void paintBorder(Graphics g) {
        g.drawRoundRect(getX(), getY(), getWidth() - 1, getHeight() - 1, 8, 8);
    }

    protected void paintBackground(Graphics g) {
        if (getStyle().getBgTransparency() != 0) {
            if (hasFocus()) {
                g.setColor(getStyle().getBgSelectionColor());
            } else {
                g.setColor(getStyle().getBgColor());
            }
            g.fillRoundRect(getX(), getY(), getWidth() - 1, getHeight() - 1, 8, 8);
        } else {
            super.paintBackground(g);
        }
    }

    public void paint(Graphics g) {
        UIManager.getInstance().getLookAndFeel().drawButton(g, this);
    }
    
}

