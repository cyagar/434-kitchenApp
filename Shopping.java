package kitchen;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class Shopping extends JPanel {

    private static final long serialVersionUID = 1L;

    private AppData data;
    private AppSettings settings;

    private final DefaultListModel<String> model =
        new DefaultListModel<>();

    private final JList<String> list = new JList<String>(model) {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };

    private final Set<String> checked = new HashSet<>();
    private final JLabel listSummary = new JLabel("Add your first shopping item below.");
    private final JButton movePurchased = new RoundedButton("Move purchased to Pantry");
    private final JButton removeSelected = new RoundedButton("Remove checked items");
    private final JCheckBox selectAll = new JCheckBox("Select all");

    public Shopping() {
        setLayout(new BorderLayout(10, 10));
        setBorder(
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        JPanel top = new JPanel(new BorderLayout(8, 0));
        JPanel titleRow = new JPanel(new BorderLayout(8, 0));
        titleRow.add(listSummary, BorderLayout.CENTER);
        JButton add = new RoundedButton("+ New item");
        add.addActionListener(event -> addItem());
        titleRow.add(add, BorderLayout.EAST);
        top.add(titleRow, BorderLayout.NORTH);
        top.add(new JLabel("Check items after buying them."), BorderLayout.CENTER);
        selectAll.setOpaque(false);
        selectAll.addActionListener(e -> {
            checked.clear();
            if (selectAll.isSelected()) for (int i = 0; i < model.size(); i++) checked.add(model.get(i));
            list.repaint();
            updateSelectAll();
        });
        top.add(selectAll, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        list.setFixedCellHeight(-1);
        list.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        list.setCellRenderer((items, value, index, selected, focus) -> {
            JPanel row = new JPanel(new BorderLayout(9, 0));
            Color background = selected ? new Color(220, 235, 225) : Color.WHITE;
            row.setBackground(background);
            row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            JCheckBox box = new JCheckBox("", checked.contains(value));
            box.setOpaque(false);
            box.setFocusable(false);
            row.add(box, BorderLayout.WEST);
            String[] details = Food.parts(value);
            boolean detailed = AppData.fields(value).length >= 7;
            String description = detailed
                    ? (details[4].isEmpty() ? "Quantity not set" : Stock.withUnit(details[4], details[5])) + "  /  " + details[6] + "  /  " + details[3]
                    : "Choose quantity and storage when moving to Pantry";
            JTextArea name = new JTextArea(Food.shoppingName(value) + "\n" + description);
            name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN,
                    settings != null && settings.largerListText() ? 18 : 15));
            name.setLineWrap(true);
            name.setWrapStyleWord(true);
            name.setOpaque(false);
            name.setEditable(false); name.setFocusable(false);
            int width = Math.max(100, list.getWidth() - 74);
            name.setSize(width, 10000);
            int height = name.getPreferredSize().height;
            row.add(name, BorderLayout.CENTER);
            row.setPreferredSize(new Dimension(Math.max(180, list.getWidth() - 4), Math.max(45, height + 19)));
            row.getAccessibleContext().setAccessibleName(Food.shoppingName(value) + ", " + description
                    + (checked.contains(value) ? ", purchased" : ", not purchased"));
            row.setSize(row.getPreferredSize());
            row.doLayout();
            return row;
        });
        list.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                list.setCellRenderer(list.getCellRenderer());
                list.revalidate();
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int index = list.locationToIndex(
                    event.getPoint()
                );

                if (index >= 0
                        && list.getCellBounds(index, index)
                            .contains(event.getPoint())) {

                    toggle(model.get(index));
                }
            }
        });

        list.getInputMap().put(
            KeyStroke.getKeyStroke("SPACE"), "toggle"
        );

        list.getActionMap().put(
            "toggle",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (list.getSelectedValue() != null) {
                        toggle(list.getSelectedValue());
                    }
                }
            }
        );

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(listScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        movePurchased.putClientProperty("primaryAction", true);
        movePurchased.setBackground(new Color(35, 116, 81));
        movePurchased.setForeground(Color.WHITE);
        movePurchased.addActionListener(event -> sendToFood());
        removeSelected.addActionListener(event -> removeItem());
        buttons.add(movePurchased);
        buttons.add(removeSelected);
        add(buttons, BorderLayout.SOUTH);
        updateSelectAll();
    }

    public void setAppSettings(AppSettings settings) {
        this.settings = settings;
        settings.onChange(() -> {
            list.setCellRenderer(list.getCellRenderer());
            list.revalidate();
            list.repaint();
        });
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    public void refreshData() {
        model.clear();

        if (data != null) {
            checked.retainAll(data.shoppingItems);

            for (String item : data.shoppingItems) {
                model.addElement(item);
            }
        } else checked.clear();
        updateSelectAll();
    }

    private void updateSelectAll() {
        selectAll.setSelected(model.size() > 0 && checked.size() == model.size());
        movePurchased.setEnabled(!checked.isEmpty());
        removeSelected.setEnabled(!checked.isEmpty());
        movePurchased.setText("Move purchased to Pantry" + (checked.isEmpty() ? "" : " (" + checked.size() + ")"));
        listSummary.setText(model.isEmpty() ? "Your shopping list is empty." : model.size() + (model.size() == 1 ? " item / " : " items / ") + checked.size() + " purchased");
    }

    private void toggle(String item) {
        if (!checked.add(item)) {
            checked.remove(item);
        }

        list.repaint();
        updateSelectAll();
    }

    private void addItem() {
        if (data == null) return;

        JTextField entry = new JTextField();
        JTextField quantity = new JTextField();
        JComboBox<String> type = new JComboBox<>(new String[]{"Choose type", "Staple", "Single use"});
        JComboBox<String> unit = new JComboBox<>(Stock.units());
        JComboBox<String> storage = new JComboBox<>(new String[]{"Fridge", "Freezer", "Cabinet"});
        JPanel form = AppScreens.form();
        AppScreens.field(form, "Item name", entry);
        AppScreens.field(form, "Type", type);
        AppScreens.field(form, "Quantity to buy (optional)", quantity);
        AppScreens.field(form, "Unit", unit);
        AppScreens.field(form, "Store in", storage);
        form.add(AppScreens.copy("Note: These details carry over to Pantry after you purchase the item."));
        AppScreens.showForm(this, "New shopping item", form, "Save to Shopping", () -> {
            String name = entry.getText().trim(), amount = quantity.getText().trim();
            Double numeric = Stock.parse(amount);
            if (name.isEmpty()) return "Enter an item name.";
            if (data.shoppingItems.stream().anyMatch(item -> Food.shoppingName(item).equalsIgnoreCase(name)))
                return "That item is already on your list.";
            if (type.getSelectedIndex() == 0) return "Choose Staple or Single use.";
            if (!amount.isEmpty() && (numeric == null || numeric <= 0)) return "Enter a quantity greater than 0.";
            String normalized = numeric == null ? "" : Stock.format(numeric);
            data.shoppingItems.add(AppData.item(name, type.getSelectedItem().toString(), normalized,
                    Stock.unitLabel(unit.getSelectedItem().toString()), storage.getSelectedItem().toString(), normalized));
            refreshData();
            return null;
        });
    }

    private void removeItem() {
        if (data == null || checked.isEmpty()) return;
        java.util.List<String> toRemove = new ArrayList<>(checked);
        AppScreens.confirm(this, "Remove shopping items",
                "Remove " + toRemove.size() + " item(s) from the list?", () -> {
            data.shoppingItems.removeAll(toRemove);
            checked.removeAll(toRemove);
            refreshData();
        });
    }

    private void sendToFood() {
        if (data == null || checked.isEmpty()) return;
        java.util.List<String> originals = new ArrayList<>();
        // Keep review order consistent with the visible list.
        for (String item : data.shoppingItems) if (checked.contains(item)) originals.add(item);
        java.util.List<JTextField> quantities = new ArrayList<>();
        java.util.List<JComboBox<String>> types = new ArrayList<>(), units = new ArrayList<>(), storage = new ArrayList<>();
        JPanel form = AppScreens.form();
        for (String raw : originals) {
            String[] p = Food.parts(raw);
            if (AppData.fields(raw).length < 7) {
                for (String food : data.foods) if (Food.parts(food)[0].equalsIgnoreCase(p[0])) {
                    p = Food.parts(food); p[4] = ""; break;
                }
            }
            JLabel title = new JLabel(); title.putClientProperty("html.disable", Boolean.TRUE); title.setText(p[0]); title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
            form.add(title); form.add(Box.createVerticalStrut(12));
            JTextField quantity = new JTextField(p[4]);
            JComboBox<String> type = new JComboBox<>(new String[]{"Staple", "Single use"});
            JComboBox<String> unit = new JComboBox<>(Stock.units());
            JComboBox<String> place = new JComboBox<>(new String[]{"Fridge", "Freezer", "Cabinet"});
            type.setSelectedItem(p[3]); unit.setSelectedItem(p[5].isEmpty() ? "other / none" : p[5]); place.setSelectedItem(p[6]);
            quantities.add(quantity); types.add(type); units.add(unit); storage.add(place);
            AppScreens.field(form, "Purchased quantity (optional)", quantity);
            AppScreens.field(form, "Unit", unit); AppScreens.field(form, "Type", type); AppScreens.field(form, "Store in", place);
        }
        form.add(AppScreens.copy("Note: Purchased items are marked Full. Review their quantity and storage before moving."));
        AppScreens.showForm(this, "Move purchases to Pantry", form, "Move to Pantry", () -> {
            java.util.List<String> reviewed = new ArrayList<>();
            for (int i = 0; i < originals.size(); i++) {
                String name = Food.shoppingName(originals.get(i)), amount = quantities.get(i).getText().trim();
                Double number = Stock.parse(amount);
                if (!amount.isEmpty() && (number == null || number <= 0)) return name + ": enter a quantity greater than 0.";
                String normalized = number == null ? "" : Stock.format(number);
                reviewed.add(AppData.item(name, types.get(i).getSelectedItem().toString(), normalized,
                        Stock.unitLabel(units.get(i).getSelectedItem().toString()), storage.get(i).getSelectedItem().toString(), normalized));
            }
            String error = finishSendToFood(originals, reviewed);
            if (error != null) return error;
            refreshData();
            return null;
        });
    }

    // Apply a reviewed batch only after every purchase has been validated.
    private String finishSendToFood(java.util.List<String> originals, java.util.List<String> reviewed) {
        if (originals.size() != reviewed.size() || !data.shoppingItems.containsAll(originals))
            return "Your list changed. Reopen purchase review.";
        java.util.List<String> foods = new ArrayList<>(data.foods);
        for (String replacement : reviewed) {
            String name = Food.parts(replacement)[0];
            int index = -1;
            for (int i = 0; i < foods.size(); i++) if (Food.parts(foods.get(i))[0].equalsIgnoreCase(name)) { index = i; break; }
            if (index >= 0) foods.set(index, replacement); else foods.add(replacement);
        }
        data.foods.clear(); data.foods.addAll(foods);
        data.shoppingItems.removeAll(originals); checked.removeAll(originals);
        return null;
    }

    static String shoppingRequest(String[] food) {
        // A low/empty status does not tell us how much the user intends to buy.
        return AppData.item(food[0], food[3], "", food[5], food[6], "");
    }
}
