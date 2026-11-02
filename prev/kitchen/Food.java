package kitchen;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Food inventory, name-based allergy notices and a three-color availability dot. */
public class Food extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color FULL = new Color(34, 153, 91);
    private static final Color LOW = new Color(227, 163, 34);
    private static final Color EMPTY = new Color(211, 68, 69);

    private AppData data;
    private AppSettings settings;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<String>(model) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final List<String> visibleItems = new ArrayList<>();
    private final WrapText safetySummary = new WrapText("Warnings use food names, not full ingredient labels.");

    public Food() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Food at home");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.add(legendItem(FULL, "Full"));
        legend.add(legendItem(LOW, "Low"));
        legend.add(legendItem(EMPTY, "Empty"));
        heading.add(legend);
        heading.add(Box.createVerticalStrut(7));
        safetySummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(safetySummary);
        add(heading, BorderLayout.NORTH);

        list.setFixedCellHeight(-1); // Variable height allows names and warnings to wrap.
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((items, value, index, selected, focus) -> createRow(value, index, selected));
        list.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                // Swing caches list-row heights. Force fresh wrapping after resizing.
                list.setCellRenderer(list.getCellRenderer());
                list.revalidate();
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        JButton add = new JButton("Add food");
        JButton status = new JButton("Change status");
        JButton warnings = new JButton("Warnings");
        add.addActionListener(event -> addFood());
        status.addActionListener(event -> changeStatus());
        warnings.addActionListener(event -> showWarnings());
        buttons.add(add);
        buttons.add(status);
        buttons.add(warnings);
        add(buttons, BorderLayout.SOUTH);
    }

    private static JComponent legendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.add(new StatusDot(color, text, 12));
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        item.add(label);
        return item;
    }

    private JPanel createRow(String value, int index, boolean selected) {
        String[] p = parts(value);
        List<String> warnings = warningsAt(index);
        Set<String> affected = new LinkedHashSet<>();
        for (String warning : warnings) affected.add(warning.split(":", 2)[0]);
        String details = displayStatus(p[2]);
        if (settings == null || settings.showFoodDetails()) {
            if (!p[4].trim().isEmpty())
                details += "  •  " + p[4] + (p[5].trim().isEmpty() ? "" : " " + p[5]);
            details += "  •  " + p[6];
            if (p[3].equals("Staple")) details += "  •  Staple";
        }
        boolean compact = settings != null && settings.compactFoodRows();
        int fontSize = settings != null && settings.largerListText() ? 17 : 15;
        String caution = warnings.isEmpty() ? null
                : "⚠ Possible allergy / diet conflict: " + String.join(", ", affected);
        FoodRow row = new FoodRow(p[0], details, caution, p[2], fontSize, compact,
                selected, list.getWidth());
        row.setToolTipText(warnings.isEmpty() ? details : String.join("; ", warnings));
        return row;
    }

    /** Draws status and wrapped copy directly, so Swing never clips a wrapped sublabel. */
    private static class FoodRow extends JPanel {
        private final String name, details, caution, state;
        private final int fontSize, suppliedWidth;
        private final boolean compact;
        private final Font titleFont, bodyFont;
        private final Color background;
        FoodRow(String name, String details, String caution, String state,
                int fontSize, boolean compact, boolean selected, int suppliedWidth) {
            this.name = name; this.details = details; this.caution = caution;
            this.state = state; this.fontSize = fontSize; this.compact = compact;
            this.suppliedWidth = suppliedWidth;
            this.titleFont = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            this.bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(12, fontSize - 2));
            this.background = selected ? new Color(219, 240, 225)
                    : caution == null ? Color.WHITE : new Color(255, 248, 230);
            setBackground(background);
            setOpaque(true);
        }
        private int availableWidth(int rowWidth) { return Math.max(110, rowWidth - 64); }
        private int padding() { return compact ? 8 : 12; }
        @Override public Dimension getPreferredSize() {
            int width = Math.max(190, suppliedWidth > 0 ? suppliedWidth - 4 : 310);
            // The viewport may still be resizing when Swing caches row heights.
            // Use a conservative measure so long text cannot be cut off.
            int textWidth = Math.min(215, availableWidth(width));
            FontMetrics t = getFontMetrics(titleFont);
            FontMetrics b = getFontMetrics(bodyFont);
            int titleH = wrap(name, t, textWidth).size() * t.getHeight();
            int detailH = wrap(details, b, textWidth).size() * b.getHeight();
            int cautionH = caution == null ? 0 : 5 + wrap(caution, b, textWidth).size() * b.getHeight();
            return new Dimension(width, 2 * padding() + titleH + 4 + detailH + cautionH);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int left = 11;
            int textWidth = availableWidth(getWidth());
            int y = padding();
            y = drawLines(g2, name, titleFont, new Color(34, 54, 45), left, y, textWidth);
            y += 4;
            y = drawLines(g2, details, bodyFont, new Color(88, 111, 99), left, y, textWidth);
            if (caution != null) {
                y += 5;
                drawLines(g2, caution, bodyFont, new Color(151, 82, 13), left, y, textWidth);
            }
            int diameter = 18;
            int x = getWidth() - 32;
            int middle = getHeight() / 2;
            g2.setColor(statusColor(state));
            g2.fillOval(x, middle - diameter / 2, diameter, diameter);
            g2.setColor(statusColor(state).darker());
            g2.drawOval(x, middle - diameter / 2, diameter, diameter);
            g2.dispose();
        }
        private static int drawLines(Graphics2D g2, String text, Font font, Color color,
                                     int x, int top, int width) {
            g2.setFont(font);
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            for (String line : wrap(text, fm, width)) {
                g2.drawString(line, x, top + fm.getAscent());
                top += fm.getHeight();
            }
            return top;
        }
        private static List<String> wrap(String text, FontMetrics fm, int width) {
            List<String> lines = new ArrayList<>();
            String current = "";
            for (String word : text.trim().split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (fm.stringWidth(candidate) <= width) { current = candidate; continue; }
                if (!current.isEmpty()) { lines.add(current); current = ""; }
                while (fm.stringWidth(word) > width && word.length() > 1) {
                    int n = 1;
                    while (n < word.length() && fm.stringWidth(word.substring(0, n + 1)) <= width) n++;
                    lines.add(word.substring(0, n));
                    word = word.substring(n);
                }
                current = word;
            }
            if (!current.isEmpty()) lines.add(current);
            if (lines.isEmpty()) lines.add("");
            return lines;
        }
    }

    static String displayStatus(String state) {
        return state.equalsIgnoreCase("Empty") ? "Empty" :
                state.equalsIgnoreCase("Filled") || state.equalsIgnoreCase("Full") ? "Full" : "Low";
    }
    static Color statusColor(String state) {
        return state.equalsIgnoreCase("Empty") ? EMPTY :
                state.equalsIgnoreCase("Filled") || state.equalsIgnoreCase("Full") ? FULL : LOW;
    }

    /** Painted vector dot instead of a font-specific emoji. */
    private static class StatusDot extends JComponent {
        private final Color color;
        private final int diameter;
        StatusDot(Color color, String status, int diameter) {
            this.color = color;
            this.diameter = diameter;
            setToolTipText(status);
            if (getAccessibleContext() != null)
                getAccessibleContext().setAccessibleName("Availability: " + status);
            setPreferredSize(new Dimension(diameter + 4, diameter + 4));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval((getWidth() - diameter)/2, (getHeight() - diameter)/2, diameter, diameter);
            g2.setColor(color.darker());
            g2.drawOval((getWidth() - diameter)/2, (getHeight() - diameter)/2, diameter, diameter);
            g2.dispose();
        }
    }

    public void setAppData(AppData data) { this.data = data; refreshData(); }
    public void setAppSettings(AppSettings settings) {
        this.settings = settings;
        settings.onChange(this::refreshData);
        refreshData();
    }

    // Existing format: name | unused category | status | type | quantity | unit | storage
    static String[] parts(String item) {
        String[] old = item.split(" \\| ", -1);
        String[] p = {old[0], "Uncategorized", "Filled", "Single use", "", "", "Cabinet"};
        if (old.length >= 4) System.arraycopy(old, 0, p, 0, Math.min(old.length, p.length));
        return p;
    }
    static String shoppingName(String item) { return item.split(" \\| ", -1)[0]; }

    public void refreshData() {
        String selected = list.getSelectedValue();
        model.clear();
        visibleItems.clear();
        if (data == null) return;
        List<String> foods = new ArrayList<>(data.foods);
        if (settings != null && settings.alphabetizeFood())
            foods.sort(Comparator.comparing(f -> parts(f)[0].toLowerCase(java.util.Locale.ROOT)));
        int warningCount = 0;
        for (String item : foods) {
            String[] p = parts(item);
            // Keep Empty staples visible (red dot) until restocked; single-use
            // items are removed when emptied using the existing behavior.
            if (p[2].equalsIgnoreCase("Empty") && !p[3].equals("Staple")) continue;
            visibleItems.add(item);
            if (!FoodAlerts.warningsFor(p[0], data.profiles).isEmpty()) warningCount++;
            model.addElement(item);
        }
        if (selected != null) list.setSelectedValue(selected, true);
        // safetySummary.setText(warningCount == 0
        //         ? "No name-based conflict detected. Always check actual ingredient labels."
        //         : "⚠ " + warningCount + " possible food conflict(s). Select an item to view details.");
        list.revalidate();
        list.repaint();
    }

    private List<String> warningsAt(int index) {
        if (data == null || index < 0 || index >= visibleItems.size())
            return java.util.Collections.emptyList();
        return FoodAlerts.warningsFor(parts(visibleItems.get(index))[0], data.profiles);
    }

    private void showWarnings() {
        int index = list.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Select a food first.");
            return;
        }
        List<String> warnings = warningsAt(index);
        String food = parts(visibleItems.get(index))[0];
        String message = warnings.isEmpty()
                ? "No name-based conflicts found for " + food
                    + ".\nAlways check full ingredient and allergen labels."
                : "⚠ Possible conflicts for " + food + ":\n\n • "
                    + String.join("\n • ", warnings)
                    + "\n\nThese are name-based hints only. Check ingredients,"
                    + " allergen labels and cross-contact before serving.";
        JTextArea wrapped = new JTextArea(message, 9, 38);
        wrapped.setEditable(false);
        wrapped.setLineWrap(true);
        wrapped.setWrapStyleWord(true);
        wrapped.setOpaque(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(wrapped), "Food warnings",
                warnings.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void addFood() {
        if (data == null) return;

        JTextField name = new JTextField();
        JTextField quantity = new JTextField();
        JTextField unit = new JTextField();

        JComboBox<String> type = new JComboBox<>(
            new String[] {
                "Choose type", "Staple", "Single use"
            }
        );

        JComboBox<String> storage = new JComboBox<>(
            new String[] {"Fridge", "Freezer", "Cabinet"}
        );

        Object[] form = {
            "Name:", name,
            "Type:", type,
            "Quantity (optional):", quantity,
            "Unit (e.g. gallons):", unit,
            "Storage:", storage
        };

        while (JOptionPane.showConfirmDialog(
                this,
                form,
                "Add food",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION) {

            String entered = name.getText().trim();
            String amount = quantity.getText().trim();
            String units = unit.getText().trim();

            boolean validAmount = amount.isEmpty();

            try {
                if (!amount.isEmpty()) {
                    double number = Double.parseDouble(amount);

                    validAmount =
                        Double.isFinite(number) && number > 0;
                }
            } catch (NumberFormatException ignored) {
                // Show the validation message below.
            }

            boolean duplicate = data.foods.stream()
                .anyMatch(
                    item -> parts(item)[0]
                        .equalsIgnoreCase(entered)
                );

            if (entered.isEmpty()
                    || duplicate
                    || !validAmount
                    || type.getSelectedIndex() == 0
                    || (entered + amount + units).contains("|")) {

                JOptionPane.showMessageDialog(
                    this,
                    "Use a unique name, choose a type, and use "
                        + "a positive quantity or leave it blank. "
                        + "Do not use |."
                );

                continue;
            }

            data.foods.add(
                String.join(
                    " | ",
                    entered,
                    "Uncategorized",
                    "Filled",
                    type.getSelectedItem().toString(),
                    amount,
                    units,
                    storage.getSelectedItem().toString()
                )
            );

            refreshData();
            return;
        }
    }

    private void changeStatus() {
        int selected = list.getSelectedIndex();

        if (data == null || selected < 0) {
            JOptionPane.showMessageDialog(
                this, "Select a food first."
            );
            return;
        }

        String original = visibleItems.get(selected);
        String[] p = parts(original);

        String choice = (String) JOptionPane.showInputDialog(
            this,
            "Status for " + p[0] + ":",
            "Change status",
            JOptionPane.PLAIN_MESSAGE,
            null,
            new String[] {"Full", "Low", "Empty"},
            displayStatus(p[2])
        );

        if (choice == null) return;

        if (settings == null || settings.confirmStockChanges()) {
            if (JOptionPane.showConfirmDialog(this,
                    "Change " + p[0] + " to " + choice + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        }

        int index = data.foods.indexOf(original);

        if (index < 0) return;

        p[2] = choice.equals("Full")
            ? "Filled"
            : choice.equals("Low") ? "Running low" : "Empty";

        if (choice.equals("Empty")
                && p[3].equals("Single use")) {

            data.foods.remove(index);
        } else {
            data.foods.set(index, String.join(" | ", p));
        }

        boolean alreadyInShopping =
            data.shoppingItems.stream().anyMatch(
                item -> shoppingName(item)
                    .equalsIgnoreCase(p[0])
            );

        if (p[3].equals("Staple")
                && !choice.equals("Full")
                && !alreadyInShopping) {

            data.shoppingItems.add(p[0]);
        }

        refreshData();
    }
}