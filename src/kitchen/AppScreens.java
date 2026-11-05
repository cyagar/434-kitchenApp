package kitchen;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.*;

// Presentation helper only: app-contained form sheets, confirmations and shared icons. 
public final class AppScreens {
    private AppScreens() { }
    private static final Color INK = new Color(32, 51, 44), GREEN = new Color(35, 116, 81);

    /** Width-tracking form column; the caller still owns all fields and validation. */
    public static JPanel form() {
        JPanel panel = new FormColumn();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        return panel;
    }
    public static void field(JPanel form, String label, JComponent input) {
        if (input instanceof JComboBox) ((JComboBox<?>)input).setLightWeightPopupEnabled(false);
        JLabel heading = new JLabel(label);
        heading.setLabelFor(input);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        heading.setForeground(INK); form.add(heading); form.add(Box.createVerticalStrut(7));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input instanceof JTextArea ? 90 : 44));
        input.setPreferredSize(new Dimension(280, input instanceof JTextArea ? 90 : 44));
        form.add(input); form.add(Box.createVerticalStrut(16));
    }
    /** A small text box inside a scrolling page hands the wheel or trackpad to the page once it has nothing left to scroll. */
    public static void passWheelToPage(JScrollPane box) {
        box.addMouseWheelListener(e -> {
            JScrollBar bar = box.getVerticalScrollBar();
            boolean canScroll = bar.isVisible() && (e.getPreciseWheelRotation() < 0 ? bar.getValue() > bar.getMinimum()
                    : bar.getValue() + bar.getVisibleAmount() < bar.getMaximum());
            Container page = SwingUtilities.getAncestorOfClass(JScrollPane.class, box);
            if (!canScroll && page != null) page.dispatchEvent(SwingUtilities.convertMouseEvent(box, e, page));
        });
    }
    public static JTextArea copy(String text) {
        JTextArea copy = new WrapText(text);
        copy.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        copy.setForeground(INK);
        return copy;
    }
    /** Return null from submit on success, or a validation message to keep the form open. */
    public static void showForm(Component owner, String title, JPanel fields,
                                String action, Supplier<String> submit) {
        JRootPane root = SwingUtilities.getRootPane(owner);
        if (root == null) throw new IllegalStateException("Attach the screen to the app before opening a form.");
        Component previous = root.getGlassPane();
        boolean previousVisible = previous.isVisible();
        Component previousFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setBackground(new Color(247, 249, 247));
        // Intercept clicks outside the sheet, so underlying actions cannot fire.
        overlay.addMouseListener(new MouseAdapter() { });
        overlay.addMouseMotionListener(new MouseMotionAdapter() { });
        overlay.addMouseWheelListener(e -> e.consume());
        JPanel sheet = new JPanel(new BorderLayout(0, 12));
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(new CompoundBorder(new LineBorder(new Color(214, 226, 218)), new EmptyBorder(16, 16, 16, 16)));
        Runnable close = () -> {
            if (root.getGlassPane() == overlay) {
                root.setGlassPane(previous); previous.setVisible(previousVisible);
                if (previousFocus != null) previousFocus.requestFocusInWindow();
            }
        };
        JPanel header = new JPanel(new BorderLayout(8, 4)); header.setOpaque(false);
        JTextArea heading = copy(title); heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        header.add(heading, BorderLayout.CENTER);
        JButton back = new RoundedButton("Cancel"); back.addActionListener(e -> close.run());
        header.add(back, BorderLayout.EAST); sheet.add(header, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(fields);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20); sheet.add(scroll, BorderLayout.CENTER);
        JTextArea error = copy(""); error.setForeground(new Color(160, 43, 43)); error.setVisible(false);
        JPanel footer = new JPanel(new BorderLayout(0, 9)); footer.setOpaque(false);
        footer.add(error, BorderLayout.NORTH);
        JButton save = new RoundedButton(action); save.setBackground(GREEN); save.setForeground(Color.WHITE);
        save.setPreferredSize(new Dimension(300, 46));
        save.addActionListener(e -> {
            String problem = submit.get();
            if (problem == null) close.run();
            else { error.setText(problem); error.setVisible(true); sheet.revalidate(); }
        });
        footer.add(save, BorderLayout.CENTER); sheet.add(footer, BorderLayout.SOUTH);
        configureDropdowns(fields);
        // Keep the sheet inside the existing app window instead of opening a desktop dialog.
        GridBagConstraints constraints = new GridBagConstraints(); constraints.weightx = 1; constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH; constraints.insets = new Insets(8, 8, 8, 8);
        overlay.add(sheet, constraints);
        // The sheet covers the app window's keyboard, so it shows its own below the sheet.
        GridBagConstraints keys = new GridBagConstraints(); keys.gridx = 0; keys.gridy = 1;
        keys.weightx = 1; keys.fill = GridBagConstraints.HORIZONTAL;
        overlay.add(new TabletKeyboard(null), keys);
        overlay.setFocusCycleRoot(true);
        overlay.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        overlay.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        overlay.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { close.run(); }
        });
        root.setGlassPane(overlay); overlay.setVisible(true);
        // Keep navigation inside the sheet until it is submitted or cancelled.
        SwingUtilities.invokeLater(back::requestFocusInWindow);
    }
    public static void confirm(Component owner, String title, String text, Runnable action) {
        JPanel body = form(); body.add(copy(text));
        showForm(owner, title, body, "Confirm", () -> { action.run(); return null; });
    }
    public static void message(Component owner, String title, String text) {
        JPanel body = form(); body.add(copy(text));
        showForm(owner, title, body, "Done", () -> null);
    }
    private static void configureDropdowns(Component component) {
        // Lightweight combo popups are painted below the glass pane containing this sheet.
        // A heavyweight popup stays above it and receives mouse selection normally.
        if (component instanceof JComboBox) ((JComboBox<?>)component).setLightWeightPopupEnabled(false);
        if (component instanceof Container)
            for (Component child : ((Container)component).getComponents()) configureDropdowns(child);
    }
    private static class FormColumn extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        @Override protected void addImpl(Component c, Object constraints, int index) {
            if (c instanceof JComponent) ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);
            super.addImpl(c, constraints, index);
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(20, r.height - 40); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private static final String[] ICON_NAMES = {"pantry", "shopping", "recipes", "profiles", "settings", "add"};
    private static final java.util.Map<String, Icon> ICON_CACHE = new java.util.HashMap<>();

    public static Icon icon(int kind) { return icon(kind, false); }

    // Load real PNG resources copied by Eclipse and bundled into the runnable JAR.
    public static Icon icon(int kind, boolean selected) {
        if (kind < 0 || kind >= ICON_NAMES.length) return null;
        String path = "/kitchen/icons/" + ICON_NAMES[kind] + (selected ? "-active" : "") + ".png";
        return loadIcon(path);
    }
    public static Icon statusIcon(String status) {
        String name = "Empty".equals(status) ? "empty" : "Full".equals(status) ? "full" : "low";
        return loadIcon("/kitchen/icons/status-" + name + ".png");
    }
    private static Icon loadIcon(String path) {
        if (ICON_CACHE.containsKey(path)) return ICON_CACHE.get(path);
        java.net.URL url = AppScreens.class.getResource(path);
        if (url == null) {
            // Text labels remain usable if an asset folder was accidentally omitted.
            System.err.println("Missing icon: " + path + ". Copy the icons folder into src/kitchen.");
            ICON_CACHE.put(path, null);
            return null;
        }
        ImageIcon image = new ImageIcon(url);
        Icon icon = new ImageIcon(image.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        ICON_CACHE.put(path, icon);
        return icon;
    }
}
