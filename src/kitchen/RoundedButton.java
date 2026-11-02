package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Antialiased rounded filled button; uses only built-in Java Swing. */
public class RoundedButton extends JButton {
    private static final long serialVersionUID = 1L;
    public RoundedButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBackground(new Color(231, 244, 236));
        setForeground(new Color(35, 116, 81));
        setFocusPainted(false);
        setBorder(new EmptyBorder(10, 14, 10, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D graphics = (Graphics2D) g.create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = getBackground();
        if (!isEnabled()) fill = new Color(235, 237, 235);
        if (getModel().isPressed()) fill = fill.darker();
        graphics.setColor(fill);
        graphics.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 17, 17);
        graphics.dispose();
        super.paintComponent(g);
    }
}
