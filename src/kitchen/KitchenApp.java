package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

// Desktop Java Swing application styled as a compact mobile interface.
public class KitchenApp {
    private static final Color BG = new Color(247, 249, 247);
    private static final Color INK = new Color(32, 51, 44);
    private static final Color GREEN = new Color(35, 116, 81);
    private static final Color MUTED = new Color(82, 104, 91);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }

            JFrame frame = new JFrame("Kitchen Kompanion");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(createContent(new AppData(), new AppSettings()));
            // At 96 logical DPI, this content area is approximately an 8.3 inch diagonal.
            frame.getContentPane().setPreferredSize(new Dimension(440, 660));
            frame.pack();
            Insets chrome = frame.getInsets();
            frame.setMinimumSize(new Dimension(380 + chrome.left + chrome.right,
                    570 + chrome.top + chrome.bottom));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Screen creation is kept separate only so the UI can be verified without a desktop window.
    static JPanel createContent(AppData data, AppSettings appSettings) {
        configureDefaults();
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
        String[] names = {"Pantry", "Shopping", "Recipes", "Profiles", "Settings"};
        JPanel cards = new JPanel(new CardLayout());
        cards.setBackground(BG);
        for (int i = 0; i < pages.length; i++) {
            styleTree(pages[i]);
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBackground(BG);
            holder.setBorder(new EmptyBorder(0, 6, 0, 6));
            holder.add(pages[i], BorderLayout.CENTER);
            cards.add(holder, names[i]);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        JLabel heading = new JLabel("Pantry");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        heading.setForeground(INK);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(18, 24, 10, 24));
        header.add(heading, BorderLayout.WEST);
        int[] activePage = {0};
        JButton preferences = new RoundedButton("Settings");
        preferences.setIcon(AppScreens.icon(4, true));
        preferences.addActionListener(e -> {
            ((CardLayout) cards.getLayout()).show(cards, "Settings");
            heading.setText("Settings");
        });
        // Settings stays in the top-right corner on every main screen.
        JButton settingsBack = new RoundedButton("← Back");
        settingsBack.addActionListener(e -> {
            ((CardLayout) cards.getLayout()).show(cards, names[activePage[0]]);
            heading.setText(names[activePage[0]]);
        });
        JPanel settingsHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        settingsHeader.setBackground(Color.WHITE);
        settingsHeader.add(settingsBack);
        settings.add(settingsHeader, BorderLayout.NORTH);
        header.add(preferences, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);
        root.add(cards, BorderLayout.CENTER);

        JPanel nav = new JPanel(new GridLayout(1, names.length - 1, 2, 0));
        nav.setBackground(Color.WHITE);
        nav.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(225, 233, 227)), new EmptyBorder(9, 8, 10, 8)));
        JButton[] navButtons = new JButton[names.length - 1];
        for (int i = 0; i < navButtons.length; i++) {
            final int index = i;
            JButton button = new RoundedButton(names[i]);
            button.setIcon(AppScreens.icon(i));
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setIconTextGap(5);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
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
                activePage[0] = index;
                for (int j = 0; j < navButtons.length; j++) {
                    navButtons[j].setIcon(AppScreens.icon(j, j == index));
                    navButtons[j].setBackground(j == index ? new Color(228, 243, 233) : Color.WHITE);
                    navButtons[j].setForeground(j == index ? GREEN : MUTED);
                }
                food.refreshData();
                shopping.refreshData();
                recipes.refreshData();
                profiles.refreshData();
            });
        }
        navButtons[0].setIcon(AppScreens.icon(0, true));
        navButtons[0].setBackground(new Color(228, 243, 233));
        navButtons[0].setForeground(GREEN);
        for (int i = 1; i < navButtons.length; i++) {
            navButtons[i].setBackground(Color.WHITE);
            navButtons[i].setForeground(MUTED);
        }
        // Settings is not a nav tab: clear the highlight while it is open and restore it on Back.
        preferences.addActionListener(e -> {
            for (int j = 0; j < navButtons.length; j++) {
                navButtons[j].setIcon(AppScreens.icon(j, false));
                navButtons[j].setBackground(Color.WHITE);
                navButtons[j].setForeground(MUTED);
            }
        });
        settingsBack.addActionListener(e -> navButtons[activePage[0]].doClick(0));
        root.add(nav, BorderLayout.SOUTH);

        // While typing, the keyboard takes the place of the bottom navigation, as on a real tablet.
        JPanel screen = new JPanel(new BorderLayout());
        screen.add(root, BorderLayout.CENTER);
        screen.add(new TabletKeyboard(nav), BorderLayout.SOUTH);
        return screen;
    }

    private static void configureDefaults() {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
        for (String key : new String[]{"Button.font", "Label.font", "List.font", "TextField.font",
                "TextArea.font", "ComboBox.font", "CheckBox.font", "ToggleButton.font"}) {
            UIManager.put(key, font);
        }
        UIManager.put("Viewport.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", GREEN);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Button.gradient", null);
        UIManager.put("CheckBox.gradient", null);
        UIManager.put("TextField.border", new CompoundBorder(new LineBorder(new Color(102, 117, 110), 1, true), new EmptyBorder(8, 9, 8, 9)));
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextArea.foreground", INK);
        UIManager.put("CheckBox.foreground", INK);
    }

    private static void styleTree(Component component) {
        // Explicit warning colors and primary/destructive button roles must survive styling.
        if (component instanceof JButton) {
            JButton button = (JButton) component;
            if (Boolean.TRUE.equals(button.getClientProperty("primaryAction"))) {
                button.setBackground(GREEN); button.setForeground(Color.WHITE);
            }
        }
        if (component instanceof JList) {
            JList<?> list = (JList<?>) component;
            list.setBackground(Color.WHITE); list.setForeground(INK);
            list.setSelectionBackground(new Color(220, 239, 226)); list.setSelectionForeground(INK);
        }
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents()) styleTree(child);
    }

    private static JPanel createSettings(AppSettings settings) {
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel preferences = AppScreens.form();
        preferences.setBorder(new EmptyBorder(6, 6, 12, 12));

        JLabel heading = new JLabel("Display & behavior");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(heading);
        preferences.add(Box.createVerticalStrut(5));
        addText(preferences, "Changes are applied immediately and saved on this computer.");
        preferences.add(Box.createVerticalStrut(19));

        JLabel display = new JLabel("PANTRY DISPLAY");
        display.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        display.setForeground(GREEN);
        display.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(display);
        preferences.add(Box.createVerticalStrut(8));

        JCheckBox alphabetize = addOption(preferences, "Sort Pantry A–Z",
                "Show the pantry in alphabetical order.", settings.alphabetizeFood(), settings::setAlphabetizeFood);
        JCheckBox details = addOption(preferences, "Show item details",
                "Display quantity, storage location and staple type in Pantry.", settings.showFoodDetails(), settings::setShowFoodDetails);
        JCheckBox compact = addOption(preferences, "Compact Pantry rows",
                "Reduce spacing between pantry entries.", settings.compactFoodRows(), settings::setCompactFoodRows);
        JCheckBox large = addOption(preferences, "Larger list text",
                "Increase text size in Pantry and Shopping lists.", settings.largerListText(), settings::setLargerListText);

        preferences.add(Box.createVerticalStrut(10));
        JLabel behavior = new JLabel("ACTIONS & SAFETY");
        behavior.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        behavior.setForeground(GREEN);
        behavior.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferences.add(behavior);
        preferences.add(Box.createVerticalStrut(8));
        JCheckBox confirm = addOption(preferences, "Confirm stock changes",
                "Ask for confirmation before changing Full, Low or Empty.", settings.confirmStockChanges(), settings::setConfirmStockChanges);
        addText(preferences, "User conflict notices remain visible. Check full ingredient labels before serving.");
        preferences.add(Box.createVerticalStrut(14));

        JButton restore = new RoundedButton("Restore default preferences");
        restore.addActionListener(e -> {
            settings.reset();
            alphabetize.setSelected(settings.alphabetizeFood());
            details.setSelected(settings.showFoodDetails());
            compact.setSelected(settings.compactFoodRows());
            large.setSelected(settings.largerListText());
            confirm.setSelected(settings.confirmStockChanges());
        });
        JPanel restoreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        restoreRow.setOpaque(false);
        restoreRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        restoreRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        restoreRow.setPreferredSize(new Dimension(300, 48));
        restoreRow.add(restore);
        preferences.add(restoreRow);
        preferences.add(Box.createVerticalStrut(14));
        addText(preferences, "Note: Sample data reloads when you restart the app.");

        JScrollPane scroll = new JScrollPane(preferences);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        // Settings always opens at its first option, even after scrolling down on the last visit.
        scroll.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && scroll.isShowing())
                scroll.getVerticalScrollBar().setValue(0);
        });
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

}
