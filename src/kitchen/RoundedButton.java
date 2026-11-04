package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Shared standard Swing button styling. Class name retained for existing callers.
public class RoundedButton extends JButton {
    private static final long serialVersionUID = 1L;
    public RoundedButton(String text) {
        super();
        putClientProperty("html.disable", Boolean.TRUE);
        setText(text);
        setOpaque(true);
        setContentAreaFilled(true);
        setBackground(new Color(231, 244, 236));
        setForeground(new Color(35, 116, 81));
        setFocusPainted(false);
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(44, size.width), Math.max(44, size.height));
    }
}
