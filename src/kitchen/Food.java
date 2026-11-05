package kitchen;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Pantry inventory with image-based availability icons and household notices.
public class Food extends JPanel {
    private static final long serialVersionUID = 1L;

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
    private final JLabel inventoryHint = new JLabel("Add your first item with Add item below.");


    public Food() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel overview = new JPanel(new BorderLayout(9, 12));
        inventoryHint.setForeground(new Color(88, 111, 99));
        overview.add(inventoryHint, BorderLayout.NORTH);
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
        JPanel plusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JButton plus = new RoundedButton("Add item");
        plus.setToolTipText("Add pantry item");
        plus.getAccessibleContext().setAccessibleName("Add pantry item");
        plus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        plus.setIcon(AppScreens.icon(5, true));
        plus.setPreferredSize(new Dimension(160, 46));
        plus.addActionListener(e -> addFood());
        plusRow.add(plus);
        bottom.add(plusRow, BorderLayout.NORTH);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        legend.add(legendItem("Full"));
        legend.add(legendItem("Low"));
        legend.add(legendItem("Empty"));
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
        JTextArea title = AppScreens.copy(p[0]);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        top.add(title, BorderLayout.CENTER);
        details.add(top, BorderLayout.NORTH);

        JPanel fields = AppScreens.form();
        fields.setBorder(new EmptyBorder(12, 5, 14, 8));
        addSection(fields, "Availability: " + displayStatus(p[2]));
        addInfo(fields, "Type: " + p[3] + "      Storage: " + p[6]);
        JTextField remaining = new JTextField(p[4]);
        remaining.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        addSection(fields, "Quantity remaining (optional)");
        addInfo(fields, p[5].isEmpty() ? "Quantity is separate from stock status." : "Unit: " + p[5]);
        fields.add(remaining);
        fields.add(Box.createVerticalStrut(11));
        addSection(fields, "Stock status");
        JPanel manual = statusButtons(displayStatus(p[2]));
        fields.add(manual);
        fields.add(Box.createVerticalStrut(8));
        addInfo(fields, "Choose a status; quantity does not change it automatically.");
        fields.add(Box.createVerticalStrut(15));
        List<String> alerts = FoodAlerts.warningsFor(p[0], data.profiles);
        addSection(fields, alerts.isEmpty() ? "User notices" : "User conflict");
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
        save.addActionListener(e -> updateFood(remaining.getText(), chosenStatus(manual)));
        save.setBackground(new Color(35, 116, 81)); save.setForeground(Color.WHITE);
        JButton delete = new RoundedButton("Delete item");
        delete.setForeground(new Color(160, 43, 43));
        delete.setBackground(new Color(253, 236, 234));
        delete.addActionListener(e -> deleteFood());
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.add(save); actions.add(delete);
        details.add(actions, BorderLayout.SOUTH);
        details.revalidate(); details.repaint();
        screens.show(content, "details");
    }

    private void deleteFood() {
        if (data == null || editingItem == null) return;
        String original = editingItem;
        String name = parts(original)[0];
        AppScreens.confirm(this, "Delete pantry item", "Delete " + name + " from Pantry?", () -> {
            data.foods.remove(original);
            editingItem = null;
            refreshData();
            screens.show(content, "overview");
        });
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

    private void updateFood(String quantityText, String manualChoice) {
        if (editingItem == null || data == null) return;
        String original = editingItem;
        int index = data.foods.indexOf(original);
        if (index < 0) { screens.show(content, "overview"); return; }
        String[] p = parts(editingItem);
        String text = quantityText.trim();
        Double quantity = Stock.parse(text);
        if (!text.isEmpty() && quantity == null) {
            AppScreens.message(this, "Check quantity", "Enter a quantity of 0 or more."); return;
        }
        p[4] = quantity == null ? "" : Stock.format(quantity);
        p[7] = ""; // Kept in the record format for compatibility; no baseline is needed.
        p[2] = storedStatus(manualChoice);
        Runnable save = () -> {
            int target = data.foods.indexOf(original);
            if (target < 0) {
                editingItem = null; refreshData(); screens.show(content, "overview"); return;
            }
            if (p[2].equals("Empty") && p[3].equals("Single use")) {
                data.foods.remove(target);
            } else {
                data.foods.set(target, AppData.join(p));
            }
            if (p[3].equals("Staple") && !p[2].equals("Filled")
                    && data.shoppingItems.stream().noneMatch(x -> shoppingName(x).equalsIgnoreCase(p[0]))) {
                data.shoppingItems.add(Shopping.shoppingRequest(p));
            }
            editingItem = null;
            refreshData();
            screens.show(content, "overview");
        };
        if (!p[2].equals(parts(original)[2]) && (settings == null || settings.confirmStockChanges())) {
            AppScreens.confirm(this, "Save stock change",
                    "Save " + p[0] + " as " + displayStatus(p[2]) + "?", save);
        } else save.run();
    }

    private static JPanel statusButtons(String selected) {
        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(300, 46));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup group = new ButtonGroup();
        java.util.List<JToggleButton> buttons = new ArrayList<>();
        Runnable restyle = () -> {
            for (JToggleButton button : buttons) {
                button.setBackground(button.isSelected() ? new Color(35, 116, 81) : new Color(231, 244, 236));
                button.setForeground(button.isSelected() ? Color.WHITE : new Color(35, 116, 81));
            }
        };
        for (String status : new String[]{"Full", "Low", "Empty"}) {
            JToggleButton button = new JToggleButton(status);
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            button.setActionCommand(status); button.setFocusPainted(false);
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            button.setBorder(BorderFactory.createLineBorder(new Color(102, 117, 110)));
            group.add(button); buttons.add(button); row.add(button);
            button.setSelected(status.equals(selected));
            button.addActionListener(e -> restyle.run());
        }
        restyle.run(); row.putClientProperty("stockGroup", group);
        return row;
    }
    private static String chosenStatus(JPanel row) {
        ButtonModel selected = ((ButtonGroup)row.getClientProperty("stockGroup")).getSelection();
        return selected == null ? "Full" : selected.getActionCommand();
    }
    private static String storedStatus(String label) {
        return "Empty".equals(label) ? "Empty" : "Low".equals(label) ? "Running low" : "Filled";
    }

    private static JComponent legendItem(String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.add(statusLabel(text));
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        item.add(label);
        return item;
    }

    private JPanel createRow(String value, int index, boolean selected) {
        String[] p = parts(value);
        List<String> warnings = warningsAt(index);
        String details = displayStatus(p[2]);
        if (settings == null || settings.showFoodDetails()) {
            if (!p[4].trim().isEmpty())
                details += "  •  " + Stock.withUnit(p[4], p[5]);
            details += "  •  " + p[6];
            if (p[3].equals("Staple")) details += "  •  Staple";
        }
        boolean compact = settings != null && settings.compactFoodRows();
        int fontSize = settings != null && settings.largerListText() ? 17 : 15;
        // A tablet has no hover, so the reason is shown on the row itself.
        String caution = warnings.isEmpty() ? null : String.join("; ", warnings);
        FoodRow row = new FoodRow(p[0], details, caution, p[2], fontSize, compact,
                selected, list.getWidth());
        row.setToolTipText(warnings.isEmpty() ? details : String.join("; ", warnings));
        return row;
    }

    // Standard Swing text components wrap the copy; status artwork comes from PNG resources.
    private static class FoodRow extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JPanel copy = new JPanel();
        private final int suppliedWidth, padding;
        FoodRow(String name, String details, String caution, String state,
                int fontSize, boolean compact, boolean selected, int suppliedWidth) {
            super(new BorderLayout(12, 0));
            this.suppliedWidth = suppliedWidth;
            padding = compact ? 8 : 12;
            setBorder(new EmptyBorder(padding, 11, padding, 12));
            setBackground(selected ? new Color(219, 240, 225)
                    : caution == null ? Color.WHITE : new Color(255, 248, 230));
            copy.setOpaque(false);
            copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
            copy.add(rowText(name, new Font(Font.SANS_SERIF, Font.BOLD, fontSize), new Color(34, 54, 45)));
            copy.add(Box.createVerticalStrut(4));
            Font body = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(13, fontSize - 1));
            copy.add(rowText(details, body, new Color(88, 111, 99)));
            if (caution != null) {
                copy.add(Box.createVerticalStrut(5));
                copy.add(rowText(caution, body, new Color(151, 82, 13)));
            }
            add(copy, BorderLayout.CENTER);
            add(statusLabel(state), BorderLayout.EAST);
            getAccessibleContext().setAccessibleName(name + ", " + details + (caution == null ? "" : ", " + caution));
            // List renderers are not live containers; lay out their nested text column explicitly.
            setSize(getPreferredSize());
            doLayout();
            copy.doLayout();
        }
        private static WrapText rowText(String text, Font font, Color color) {
            WrapText label = new WrapText(text);
            label.setFont(font); label.setForeground(color);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }
        @Override public Dimension getPreferredSize() {
            int width = Math.max(190, suppliedWidth > 0 ? suppliedWidth - 4 : 310);
            copy.setSize(Math.max(110, width - 59), 10000);
            int height = 0;
            for (Component child : copy.getComponents()) height += child.getPreferredSize().height;
            return new Dimension(width, 2 * padding + Math.max(24, height));
        }
    }

    static String displayStatus(String state) {
        return state.equalsIgnoreCase("Empty") ? "Empty" :
                state.equalsIgnoreCase("Filled") || state.equalsIgnoreCase("Full") ? "Full" : "Low";
    }
    private static JLabel statusLabel(String state) {
        String status = displayStatus(state);
        JLabel label = new JLabel(AppScreens.statusIcon(status));
        label.setToolTipText(status);
        label.getAccessibleContext().setAccessibleName("Availability: " + status);
        label.setPreferredSize(new Dimension(24, 24));
        return label;
    }

    public void setAppData(AppData data) { this.data = data; refreshData(); }
    public void setAppSettings(AppSettings settings) {
        this.settings = settings;
        settings.onChange(this::refreshData);
        refreshData();
    }

    // Existing format: name | unused category | status | type | quantity | unit | storage
    static String[] parts(String item) {
        String[] old = AppData.fields(item);
        String[] p = {old[0], "Uncategorized", "Filled", "Single use", "", "", "Cabinet", ""};
        if (old.length >= 4) System.arraycopy(old, 0, p, 0, Math.min(old.length, p.length));
        return p;
    }
    static String shoppingName(String item) { return AppData.fields(item)[0]; }

    public void refreshData() {
        String selected = list.getSelectedValue();
        model.clear();
        visibleItems.clear();
        if (data == null) {
            editingItem = null; inventoryHint.setText("Add your first item with Add item below.");
            screens.show(content, "overview"); return;
        }
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
        inventoryHint.setText(model.isEmpty() ? "Add your first item with Add item below." : model.size() + (model.size() == 1 ? " item" : " items") + "  /  Tap an item for details");
        if (selected != null) list.setSelectedValue(selected, true);
        // A purchase can update this item while another destination is active.
        if (editingItem != null && !data.foods.contains(editingItem)) {
            String name = parts(editingItem)[0];
            editingItem = data.foods.stream().filter(raw -> parts(raw)[0].equalsIgnoreCase(name)).findFirst().orElse(null);
            if (editingItem == null) screens.show(content, "overview"); else showDetail();
        }
        list.revalidate();
        list.repaint();
    }

    private List<String> warningsAt(int index) {
        if (data == null || index < 0 || index >= visibleItems.size())
            return java.util.Collections.emptyList();
        return FoodAlerts.warningsFor(parts(visibleItems.get(index))[0], data.profiles);
    }

    private void addFood() {
        if (data == null) return;
        JTextField name = new JTextField();
        JTextField quantity = new JTextField();
        JComboBox<String> unit = new JComboBox<>(Stock.units());
        JComboBox<String> type = new JComboBox<>(new String[]{"Choose type", "Staple", "Single use"});
        JComboBox<String> storage = new JComboBox<>(new String[]{"Fridge", "Freezer", "Cabinet"});
        JPanel manual = statusButtons("Full");
        JPanel form = AppScreens.form();
        AppScreens.field(form, "Name", name);
        AppScreens.field(form, "Type", type);
        AppScreens.field(form, "Quantity (optional)", quantity);
        AppScreens.field(form, "Unit", unit);
        AppScreens.field(form, "Storage", storage);
        form.add(AppScreens.copy("Stock status"));
        form.add(Box.createVerticalStrut(7));
        form.add(manual);
        form.add(Box.createVerticalStrut(12));
        form.add(AppScreens.copy("Note: Staples stay in Pantry when empty. Low or empty staples are added to Shopping automatically."));
        AppScreens.showForm(this, "Add pantry item", form, "Add to Pantry", () -> {
            String entered = name.getText().trim();
            String amount = quantity.getText().trim();
            Double numeric = Stock.parse(amount);
            boolean duplicate = data.foods.stream().anyMatch(item -> parts(item)[0].equalsIgnoreCase(entered));
            if (entered.isEmpty()) return "Enter a food name.";
            if (duplicate) return "That food is already in your pantry.";
            if (type.getSelectedIndex() == 0) return "Choose Staple or Single use.";
            if (!amount.isEmpty() && numeric == null) return "Enter a quantity of 0 or more.";
            String status = storedStatus(chosenStatus(manual));
            if (status.equals("Empty") && "Single use".equals(type.getSelectedItem()))
                return "Add empty single-use foods to Shopping.";
            String baseline = "";
            String amountText = numeric == null ? "" : Stock.format(numeric);
            String units = Stock.unitLabel(unit.getSelectedItem().toString());
            data.foods.add(AppData.join(entered, "Uncategorized", status,
                    type.getSelectedItem().toString(), amountText, units, storage.getSelectedItem().toString(), baseline));
            if (type.getSelectedItem().equals("Staple") && !status.equals("Filled")
                    && data.shoppingItems.stream().noneMatch(x -> shoppingName(x).equalsIgnoreCase(entered)))
                data.shoppingItems.add(Shopping.shoppingRequest(parts(data.foods.get(data.foods.size() - 1))));
            refreshData();
            return null;
        });
    }
}
