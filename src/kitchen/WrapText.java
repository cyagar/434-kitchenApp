package kitchen;

import java.awt.*;
import java.awt.font.*;
import java.text.AttributedString;
import javax.swing.JTextArea;

// Read only wrapped text. Measuring never resizes the live component.
public final class WrapText extends JTextArea {
    private static final long serialVersionUID = 1L;
    public WrapText(String text) {
        super(text);
        setEditable(false); setFocusable(false); setOpaque(false);
        setLineWrap(true); setWrapStyleWord(true); setBorder(null);
        setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        setForeground(new Color(102, 117, 110));
    }
    // The hidden caret must not scroll the page that contains this read-only text.
    @Override public void scrollRectToVisible(Rectangle r) { }
    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
    @Override public Dimension getPreferredSize() {
        Container parent = getParent();
        int width = parent == null ? 300 : parent.getWidth() - parent.getInsets().left - parent.getInsets().right;
        if (parent != null && parent.getLayout() instanceof BorderLayout) {
            BorderLayout layout = (BorderLayout) parent.getLayout();
            if (layout.getLayoutComponent(BorderLayout.CENTER) == this) {
                for (String side : new String[]{BorderLayout.WEST, BorderLayout.EAST}) {
                    Component neighbor = layout.getLayoutComponent(side);
                    if (neighbor != null && neighbor.isVisible()) width -= neighbor.getPreferredSize().width + layout.getHgap();
                }
            }
        }
        if (width <= 0) width = 300;
        Insets inset = getInsets();
        int available = Math.max(1, width - inset.left - inset.right);
        FontMetrics metrics = getFontMetrics(getFont());
        int lines = 0;
        for (String paragraph : getText().split("\\n", -1)) {
            if (paragraph.isEmpty()) { lines++; continue; }
            AttributedString text = new AttributedString(paragraph);
            text.addAttribute(TextAttribute.FONT, getFont());
            LineBreakMeasurer measure = new LineBreakMeasurer(text.getIterator(), metrics.getFontRenderContext());
            while (measure.getPosition() < paragraph.length()) { measure.nextLayout(available); lines++; }
        }
        return new Dimension(width, Math.max(1, lines) * metrics.getHeight() + inset.top + inset.bottom + 4);
    }
}
