package kitchen;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;

// Shows where the tablet's on-screen keyboard appears while a text field has focus. It is only a
// drawing: typing still uses the physical keyboard. It takes layout space instead of floating,
// so the content above it shrinks and the field being edited is never covered.
public final class TabletKeyboard extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(32, 51, 44), GREEN = new Color(35, 116, 81);
    private static final Color TRAY = new Color(222, 228, 224), EDGE = new Color(100, 115, 107);
    private static final Color LETTER = Color.WHITE, SPECIAL = new Color(200, 209, 203), ACTION = new Color(231, 244, 236);
    private static final String[] LETTERS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};

    private static TabletKeyboard active;
    private static Component dismissed, revealed;
    private static boolean mouseDown;

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                mouseDown = true;
                if (event.getSource() instanceof JTextComponent) dismissed = null;
            } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
                mouseDown = false;
                // Change the layout only after the click has finished. A button above the keyboard
                // would otherwise move out from under the pointer and lose the click.
                Timer later = new Timer(40, e -> update());
                later.setRepeats(false);
                later.start();
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner",
                event -> SwingUtilities.invokeLater(TabletKeyboard::update));
    }

    private final Component covered;
    private Rectangle hideKey = new Rectangle();

    /** The covered component, such as the bottom navigation, is hidden while the keyboard is open. It may be null. */
    public TabletKeyboard(Component covered) {
        this.covered = covered;
        setVisible(false);
        setFocusable(false);
        setOpaque(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (hideKey.contains(e.getPoint())) {
                    dismissed = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    update();
                }
            }
        });
    }

    private static void update() {
        if (mouseDown) return;
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (owner == null) return; // Focus is moving between components, or the window is inactive.
        if (owner != dismissed) dismissed = null;
        boolean typing = owner instanceof JTextComponent && ((JTextComponent) owner).isEditable() && owner != dismissed;
        TabletKeyboard next = typing ? keyboardFor(owner) : null;
        if (next != active) {
            if (active != null) active.setOpen(false);
            if (next != null) next.setOpen(true);
            active = next;
        }
        if (typing && owner != revealed) SwingUtilities.invokeLater(() -> reveal((JComponent) owner));
        revealed = typing ? owner : null;
    }

    private void setOpen(boolean open) {
        setVisible(open);
        if (covered != null) covered.setVisible(!open);
    }

    // The closest keyboard wins, so a field in a form sheet uses the sheet's own keyboard.
    private static TabletKeyboard keyboardFor(Component field) {
        for (Container parent = field.getParent(); parent != null; parent = parent.getParent())
            for (Component child : parent.getComponents())
                if (child instanceof TabletKeyboard) return (TabletKeyboard) child;
        return null;
    }

    // Scrolls the field and its label above the keyboard. A text area has its own small scroller,
    // so that whole box is what must be visible, and a JTextField only scrolls its own text.
    private static void reveal(JComponent field) {
        JComponent box = field;
        JScrollPane own = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, field);
        if (own != null && own.getViewport().getView() == field) box = own;
        if (box.getParent() instanceof JComponent) {
            int label = 34;
            ((JComponent) box.getParent()).scrollRectToVisible(new Rectangle(box.getX(), box.getY() - label,
                    box.getWidth(), box.getHeight() + label));
        }
    }

    @Override public Dimension getPreferredSize() {
        int width = getParent() == null ? 440 : getParent().getWidth();
        int height = getParent() == null ? 660 : getParent().getHeight();
        // In a very wide window the keyboard must still leave room for the fields above it.
        return new Dimension(width, Math.min(width * 47 / 100, height * 2 / 5));
    }
    // GridBagLayout falls back to minimum sizes when a long form does not fit, so the keyboard
    // must not be allowed to collapse: the form above it is the part that scrolls.
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), gap = Math.max(3, w / 88);
        g2.setColor(TRAY);
        g2.fillRect(0, 0, w, h);
        g2.setColor(EDGE);
        g2.drawLine(0, 0, w, 0);
        int unit = (w - 11 * gap) / 10, wide = unit * 3 / 2, keyHeight = (h - 5 * gap) / 4;
        Font letter = new Font(Font.SANS_SERIF, Font.PLAIN, keyHeight * 2 / 5);
        Font label = new Font(Font.SANS_SERIF, Font.BOLD, keyHeight * 3 / 10);
        for (int row = 0; row < LETTERS.length; row++) {
            int width = LETTERS[row].length() * (unit + gap) - gap;
            int x = (w - width) / 2, top = gap + row * (keyHeight + gap);
            for (char c : LETTERS[row].toCharArray()) {
                key(g2, x, top, unit, keyHeight, String.valueOf(c), LETTER, INK, letter);
                x += unit + gap;
            }
            if (row == LETTERS.length - 1) {
                int side = (w - width) / 2 - 2 * gap;
                key(g2, gap, top, side, keyHeight, "Shift", SPECIAL, INK, label);
                key(g2, w - gap - side, top, side, keyHeight, "Delete", SPECIAL, INK, label);
            }
        }
        int top = gap + LETTERS.length * (keyHeight + gap);
        int hideX = w - gap - wide, returnX = hideX - gap - wide, periodX = returnX - gap - unit;
        int commaX = gap + wide + gap, spaceX = commaX + unit + gap;
        key(g2, gap, top, wide, keyHeight, "123", SPECIAL, INK, label);
        key(g2, commaX, top, unit, keyHeight, ",", LETTER, INK, letter);
        key(g2, spaceX, top, periodX - gap - spaceX, keyHeight, "Use physical keyboard", LETTER, INK, label.deriveFont(Font.PLAIN));
        key(g2, periodX, top, unit, keyHeight, ".", LETTER, INK, letter);
        key(g2, returnX, top, wide, keyHeight, "Return", SPECIAL, INK, label);
        key(g2, hideX, top, wide, keyHeight, "Hide", ACTION, GREEN, label);
        hideKey = new Rectangle(hideX, top, wide, keyHeight);
        g2.dispose();
    }

    private static void key(Graphics2D g2, int x, int y, int width, int height, String text, Color fill, Color ink, Font font) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, width, height, 9, 9);
        g2.setColor(EDGE);
        g2.drawRoundRect(x, y, width, height, 9, 9);
        g2.setColor(ink);
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, x + (width - metrics.stringWidth(text)) / 2, y + (height - metrics.getHeight()) / 2 + metrics.getAscent());
    }
}
