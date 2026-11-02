package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/** Desktop Java Swing application styled as a compact mobile interface. */
public class KitchenApp {
    private static final Color BG = new Color(247, 249, 247);
    private static final Color INK = new Color(32, 51, 44);
    private static final Color GREEN = new Color(35, 116, 81);
    private static final Color MUTED = new Color(106, 124, 114);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            configureDefaults();

            AppData data = new AppData();
            AppSettings appSettings = new AppSettings();
            Food food = new Food();
            Shopping shopping = new Shopping();
            Recipes recipes = new Recipes();
            Profiles profiles = new Profiles();
            food.setAppSettings(appSettings);
            shopping.setAppSettings(appSettings);
            food.setAppData(data);
            shopping.setAppData(data);
            recipes.setAppData(data);
            profiles.setAppData(data);

            JPanel settings = createSettings(appSettings);
            JPanel[] pages = {food, shopping, recipes, profiles, settings};
            String[] names = {"Food", "Shopping", "Recipes", "Profiles", "Settings"};
            String[] symbols = {"\u2302", "\u2611", "\u2630", "\u263a", "\u2699"};
            JPanel cards = new JPanel(new CardLayout());
            cards.setBackground(BG);
            for (int i = 0; i < pages.length; i++) {
                styleTree(pages[i]);
                JPanel holder = new JPanel(new BorderLayout());
                holder.setBackground(BG);
                holder.setBorder(new EmptyBorder(12, 17, 12, 17));
                JPanel surface = new RoundedPanel(Color.WHITE, 22);
                surface.setLayout(new BorderLayout());
                surface.setBorder(new EmptyBorder(10, 10, 10, 10));
                surface.add(pages[i], BorderLayout.CENTER);
                holder.add(surface, BorderLayout.CENTER);
                cards.add(holder, names[i]);
            }

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(BG);
            JLabel heading = new JLabel("Food");
            heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
            heading.setForeground(INK);
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(BG);
            header.setBorder(new EmptyBorder(18, 24, 10, 24));
            header.add(heading, BorderLayout.WEST);
            root.add(header, BorderLayout.NORTH);
            root.add(cards, BorderLayout.CENTER);

            JPanel nav = new JPanel(new GridLayout(1, names.length, 2, 0));
            nav.setBackground(Color.WHITE);
            nav.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(225, 233, 227)), new EmptyBorder(9, 8, 10, 8)));
            JButton[] navButtons = new JButton[names.length];
            for (int i = 0; i < names.length; i++) {
                final int index = i;
                JButton button = new RoundedButton("<html><center><span style='font-size:20px'>" + symbols[i] + "</span><br>" + names[i] + "</center></html>");
                button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                button.setFocusPainted(false);
                button.setBorder(new EmptyBorder(7, 1, 7, 1));
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                navButtons[i] = button;
                nav.add(button);
                button.addActionListener(e -> {
                    ((CardLayout) cards.getLayout()).show(cards, names[index]);
                    heading.setText(names[index]);
                    for (int j = 0; j < navButtons.length; j++) {
                        navButtons[j].setBackground(j == index ? new Color(228, 243, 233) : Color.WHITE);
                        navButtons[j].setForeground(j == index ? GREEN : MUTED);
                    }
                    food.refreshData();
                    shopping.refreshData();
                    recipes.refreshData();
                    profiles.refreshData();
                });
            }
            navButtons[0].setBackground(new Color(228, 243, 233));
            navButtons[0].setForeground(GREEN);
            for (int i = 1; i < navButtons.length; i++) {
                navButtons[i].setBackground(Color.WHITE);
                navButtons[i].setForeground(MUTED);
            }
            root.add(nav, BorderLayout.SOUTH);

            JFrame frame = new JFrame("Kitchen Kompanion");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(root);
            frame.setMinimumSize(new Dimension(370, 560));
            frame.setSize(520, 780);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void configureDefaults() {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        for (String key : new String[]{"Button.font", "Label.font", "List.font", "TextField.font",
                "TextArea.font", "ComboBox.font", "OptionPane.messageFont", "OptionPane.buttonFont"}) {
            UIManager.put(key, font);
        }
        UIManager.put("OptionPane.background", BG);
        UIManager.put("Panel.background", Color.WHITE);
    }

    private static void styleTree(Component component) {
        if (component instanceof JPanel) {
            JPanel p = (JPanel) component;
            p.setBackground(Color.WHITE);
        }
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            label.setForeground(INK);
            if (label.getFont() instanceof javax.swing.plaf.FontUIResource)
                label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        }
        if (component instanceof JButton) {
            JButton b = (JButton) component;
            b.setBackground(new Color(231, 244, 236));
            b.setForeground(GREEN);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (!(b instanceof RoundedButton))
                b.setBorder(new CompoundBorder(new LineBorder(new Color(208, 229, 215), 1, true), new EmptyBorder(9, 8, 9, 8)));
        }
        if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(new Color(250, 252, 250));
            component.setForeground(INK);
        }
        if (component instanceof JList) {
            JList<?> list = (JList<?>) component;
            list.setBackground(Color.WHITE);
            list.setForeground(INK);
            list.setSelectionBackground(new Color(220, 239, 226));
            list.setSelectionForeground(INK);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) styleTree(child);
        }
    }

    private static JPanel createSettings(AppSettings settings) {
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel preferences = new ViewportColumn();
        preferences.setLayout(new BoxLayout(preferences, BoxLayout.Y_AXIS));
        preferences.setBorder(new EmptyBorder(6, 6, 12, 12));

        JLabel heading = new JLabel("Display & behavior");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(heading);
        preferences.add(Box.createVerticalStrut(5));
        addText(preferences, "Changes are applied immediately and saved on this computer.");
        preferences.add(Box.createVerticalStrut(19));

        JLabel display = new JLabel("FOOD DISPLAY");
        display.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        display.setForeground(GREEN);
        display.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(display);
        preferences.add(Box.createVerticalStrut(8));

        JCheckBox alphabetize = addOption(preferences, "Sort foods A–Z",
                "Show the pantry in alphabetical order.", settings.alphabetizeFood(), settings::setAlphabetizeFood);
        JCheckBox details = addOption(preferences, "Show item details",
                "Display quantity, storage location and staple type in Food.", settings.showFoodDetails(), settings::setShowFoodDetails);
        JCheckBox compact = addOption(preferences, "Compact food rows",
                "Reduce spacing between pantry entries.", settings.compactFoodRows(), settings::setCompactFoodRows);
        JCheckBox large = addOption(preferences, "Larger list text",
                "Increase text size in Food and Shopping lists.", settings.largerListText(), settings::setLargerListText);

        preferences.add(Box.createVerticalStrut(10));
        JLabel behavior = new JLabel("ACTIONS & SAFETY");
        behavior.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        behavior.setForeground(GREEN);
        behavior.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(behavior);
        preferences.add(Box.createVerticalStrut(8));
        JCheckBox confirm = addOption(preferences, "Confirm stock changes",
                "Ask for confirmation before changing Full, Low or Empty.", settings.confirmStockChanges(), settings::setConfirmStockChanges);
        addText(preferences, "Allergy and dietary warning hints always remain visible in Food. They are not a substitute for checking ingredient labels.");
        preferences.add(Box.createVerticalStrut(14));

        JButton restore = new RoundedButton("Restore default preferences");
        restore.setAlignmentX(Component.LEFT_ALIGNMENT);
        restore.addActionListener(e -> {
            settings.reset();
            alphabetize.setSelected(settings.alphabetizeFood());
            details.setSelected(settings.showFoodDetails());
            compact.setSelected(settings.compactFoodRows());
            large.setSelected(settings.largerListText());
            confirm.setSelected(settings.confirmStockChanges());
        });
        preferences.add(restore);
        preferences.add(Box.createVerticalStrut(14));
        addText(preferences, "Version 1.3 • Food, shopping items and profiles reset when the app closes. Display settings are saved locally.");

        JScrollPane scroll = new JScrollPane(preferences);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    private static void addText(JPanel container, String text) {
        WrapText label = new WrapText(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
    }

    private static JCheckBox addOption(JPanel container, String name, String explanation,
                                       boolean initial, java.util.function.Consumer<Boolean> action) {
        JCheckBox box = new JCheckBox(name, initial);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setOpaque(false);
        box.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        box.addActionListener(e -> action.accept(box.isSelected()));
        container.add(box);
        WrapText description = new WrapText(explanation);
        description.setBorder(new EmptyBorder(0, 24, 0, 0));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(description);
        container.add(Box.createVerticalStrut(13));
        return box;
    }

    private static class ViewportColumn extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return visible.height - 24; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final int arc;
        RoundedPanel(Color fill, int arc) { this.fill = fill; this.arc = arc; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
