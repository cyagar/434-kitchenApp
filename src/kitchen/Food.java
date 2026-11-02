package kitchen;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final CardLayout screens = new CardLayout();
    private final JPanel content = new JPanel(screens);
    private final JPanel details = new JPanel(new BorderLayout(9, 12));
    private String editingItem;


    public Food() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel overview = new JPanel(new BorderLayout(9, 12));
        list.setFixedCellHeight(-1);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((items, value, index, selected, focus) -> createRow(value, index, selected));
        list.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                list.setCellRenderer(list.getCellRenderer());
                list.revalidate();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) openFood(index);
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
        list.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (list.getSelectedIndex() >= 0) openFood(list.getSelectedIndex());
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        overview.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 7));
        JPanel plusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton plus = new RoundedButton("+");
        plus.setToolTipText("Add food");
        plus.getAccessibleContext().setAccessibleName("Add food");
        plus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        plus.setPreferredSize(new Dimension(58, 47));
        plus.addActionListener(e -> addFood());
        plusRow.add(plus);
        bottom.add(plusRow, BorderLayout.NORTH);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        legend.add(legendItem(FULL, "Full"));
        legend.add(legendItem(LOW, "Low"));
        legend.add(legendItem(EMPTY, "Empty"));
        bottom.add(legend, BorderLayout.SOUTH);
        overview.add(bottom, BorderLayout.SOUTH);

        content.add(overview, "overview");
        content.add(details, "details");
        add(content, BorderLayout.CENTER);
        screens.show(content, "overview");
    }

    private void openFood(int index) {
        if (data == null || index < 0 || index >= visibleItems.size()) return;
        editingItem = visibleItems.get(index);
        showDetail();
    }

    private void showDetail() {
        if (editingItem == null || data == null) return;
        String[] p = parts(editingItem);
        details.removeAll();
        details.setBorder(new EmptyBorder(0, 0, 0, 0));
        JPanel top = new JPanel(new BorderLayout(10, 0));
        JButton back = new RoundedButton("← Back");
        back.addActionListener(e -> screens.show(content, "overview"));
        top.add(back, BorderLayout.WEST);
        JLabel title = new JLabel("<html><body style='width:190px'>" + escapeHtml(p[0]) + "</body></html>");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        top.add(title, BorderLayout.CENTER);
        details.add(top, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBorder(new EmptyBorder(12, 5, 14, 8));
        addSection(fields, "Availability: " + displayStatus(p[2]));
        addInfo(fields, "Type: " + p[3] + "      Storage: " + p[6]);
        JTextField remaining = new JTextField(p[4]);
        remaining.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        addSection(fields, "Quantity remaining");
        addInfo(fields, p[5].isEmpty() ? "Enter a number; zero means empty." : "Unit: " + p[5]);
        fields.add(remaining);
        fields.add(Box.createVerticalStrut(11));
        Double baseline = Stock.parse(p[7]);
        if (baseline != null && baseline > 0) addInfo(fields, "Full amount: " + Stock.format(baseline) + " " + p[5]);
        JCheckBox resetBaseline = new JCheckBox("Set this quantity as the new full amount");
        resetBaseline.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetBaseline.setOpaque(false);
        fields.add(resetBaseline);
        addInfo(fields, "1 = Low  •  0 = Empty  •  Half or less of full = Low");

        JComboBox<String> manual = new JComboBox<>(new String[]{"Full", "Low", "Empty"});
        manual.setSelectedItem(displayStatus(p[2]));
        addSection(fields, "Status (used if quantity is blank)");
        manual.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        fields.add(manual);
        fields.add(Box.createVerticalStrut(15));
        List<String> alerts = FoodAlerts.warningsFor(p[0], data.profiles);
        addSection(fields, alerts.isEmpty() ? "Allergy / diet notices" : "⚠ Possible allergy / diet conflict");
        WrapText warning = new WrapText(alerts.isEmpty()
                ? "No name-based warnings found. Always verify ingredient and allergy labels."
                : String.join("\n\n", alerts) + "\n\nCheck actual labels and cross-contact before serving.");
        warning.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(warning);
        JScrollPane scroller = new JScrollPane(fields);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getVerticalScrollBar().setUnitIncrement(16);
        details.add(scroller, BorderLayout.CENTER);
        JButton save = new RoundedButton("Save changes");
        save.addActionListener(e -> updateFood(remaining.getText(), (String) manual.getSelectedItem(), resetBaseline.isSelected()));
        details.add(save, BorderLayout.SOUTH);
        details.revalidate(); details.repaint();
        screens.show(content, "details");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
    private static void addSection(JPanel fields, String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        fields.add(label);
        fields.add(Box.createVerticalStrut(7));
    }
    private static void addInfo(JPanel fields, String text) {
        WrapText label = new WrapText(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(label);
        fields.add(Box.createVerticalStrut(8));
    }

    private void updateFood(String quantityText, String manualChoice, boolean reset) {
        if (editingItem == null || data == null) return;
        int index = data.foods.indexOf(editingItem);
        if (index < 0) { screens.show(content, "overview"); return; }
        String[] p = parts(editingItem);
        String text = quantityText.trim();
        Double quantity = Stock.parse(text);
        if (!text.isEmpty() && quantity == null) {
            JOptionPane.showMessageDialog(this, "Enter a nonnegative number for quantity."); return;
        }
        if (quantity != null) {
            Double baseline = Stock.parse(p[7]);
            if (baseline == null || baseline <= 0 || reset) baseline = Math.max(1.0, quantity);
            p[4] = Stock.format(quantity);
            p[7] = Stock.format(baseline);
            p[2] = Stock.status(quantity, baseline);
        } else {
            p[4] = ""; p[7] = "";
            p[2] = "Full".equals(manualChoice) ? "Filled" : "Low".equals(manualChoice) ? "Running low" : "Empty";
        }
        if (settings == null || settings.confirmStockChanges()) {
            if (JOptionPane.showConfirmDialog(this, "Save " + p[0] + " as " + displayStatus(p[2]) + "?",
                    "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        }
        if (p[2].equals("Empty") && p[3].equals("Single use")) {
            data.foods.remove(index);
        } else {
            data.foods.set(index, String.join(" | ", p));
        }
        if (p[3].equals("Staple") && !p[2].equals("Filled")
                && data.shoppingItems.stream().noneMatch(x -> shoppingName(x).equalsIgnoreCase(p[0]))) {
            data.shoppingItems.add(p[0]);
        }
        editingItem = null;
        refreshData();
        screens.show(content, "overview");
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
            setOpaque(false);
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
            g2.setColor(background);
            g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
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
        String[] p = {old[0], "Uncategorized", "Filled", "Single use", "", "", "Cabinet", ""};
        if (old.length >= 4) System.arraycopy(old, 0, p, 0, Math.min(old.length, p.length));
        if (old.length == 7 && Stock.parse(p[4]) != null && Stock.parse(p[4]) > 0) p[7] = p[4];
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
        for (String item : foods) {
            String[] p = parts(item);
            // Keep Empty staples visible (red dot) until restocked; single-use
            // items are removed when emptied using the existing behavior.
            if (p[2].equalsIgnoreCase("Empty") && !p[3].equals("Staple")) continue;
            visibleItems.add(item);
            model.addElement(item);
        }
        if (selected != null) list.setSelectedValue(selected, true);
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
        JComboBox<String> unit = new JComboBox<>(new String[]{
                "pieces", "g", "kg", "oz", "lb", "mL", "L", "cups", "tbsp", "tsp", "packs", "bottles", "cans", "gallons", "other / none"});
        JComboBox<String> type = new JComboBox<>(new String[]{"Choose type", "Staple", "Single use"});
        JComboBox<String> storage = new JComboBox<>(new String[]{"Fridge", "Freezer", "Cabinet"});
        Object[] form = {"Name:", name, "Type:", type, "Full quantity (optional):", quantity,
                "Unit:", unit, "Storage:", storage};
        while (JOptionPane.showConfirmDialog(this, form, "Add food",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String entered = name.getText().trim();
            String amount = quantity.getText().trim();
            Double numeric = Stock.parse(amount);
            boolean duplicate = data.foods.stream().anyMatch(item -> parts(item)[0].equalsIgnoreCase(entered));
            if (entered.isEmpty() || entered.contains("|") || duplicate || type.getSelectedIndex() == 0
                    || (!amount.isEmpty() && numeric == null)) {
                JOptionPane.showMessageDialog(this, "Enter a unique name, a type, and a nonnegative quantity (or leave it blank). Do not use |.");
                continue;
            }
            if (numeric != null && numeric == 0 && "Single use".equals(type.getSelectedItem())) {
                JOptionPane.showMessageDialog(this, "A single-use food with zero quantity has no remaining stock. Add it to Shopping instead.");
                continue;
            }
            String status = numeric == null ? "Filled" : Stock.status(numeric, Math.max(1, numeric));
            String baseline = numeric == null ? "" : Stock.format(Math.max(1, numeric));
            String amountText = numeric == null ? "" : Stock.format(numeric);
            String units = numeric == null || unit.getSelectedItem().equals("other / none") ? "" : unit.getSelectedItem().toString();
            data.foods.add(String.join(" | ", entered, "Uncategorized", status,
                    type.getSelectedItem().toString(), amountText, units, storage.getSelectedItem().toString(), baseline));
            if (type.getSelectedItem().equals("Staple") && !status.equals("Filled")
                    && data.shoppingItems.stream().noneMatch(x -> shoppingName(x).equalsIgnoreCase(entered)))
                data.shoppingItems.add(entered);
            refreshData();
            return;
        }
    }
}
