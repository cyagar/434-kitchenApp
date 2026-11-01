package kitchen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Read-only, word-wrapping replacement for labels with long descriptions. */
public final class WrapText extends JTextArea {
    private static final long serialVersionUID = 1L;

    public WrapText(String text) {
        super(text);
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(null);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        setForeground(new Color(102, 117, 110));
    }

    @Override public Dimension getPreferredSize() {
        // Swing's JTextArea normally reports the preferred width of one long
        // line when it has not yet been sized. Measure at the available width.
        int width = getParent() == null ? 300 : getParent().getWidth();
        if (width <= 0) width = 300;
        width = Math.max(90, width - 4);
        if (getWidth() != width) super.setSize(width, 10000);
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.min(d.width, width), Math.max(d.height, getFontMetrics(getFont()).getHeight() + 4));
    }
}
