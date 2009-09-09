package ornagai.mobile;


import com.sun.lwuit.Button;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.plaf.UIManager;

/**
 *
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
