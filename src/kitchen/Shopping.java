package kitchen;

import java.awt.*;
import javax.swing.*;

// Member: Ceyhun
public class Shopping extends JPanel {

    private static final long serialVersionUID = 1L;

    private AppData data;

    private final DefaultListModel<String> model =
        new DefaultListModel<>();

    private final JList<String> list = new JList<>(model);

    public Shopping() {
        setLayout(new BorderLayout(10, 10));
        setBorder(
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        add(
            new JLabel(
                "Shopping list - select an item before sending or removing"
            ),
            BorderLayout.NORTH
        );

        list.setFixedCellHeight(44);
        list.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(
            new GridLayout(1, 3, 10, 0)
        );

        JButton add = new JButton("Add item");
        JButton bought = new JButton("Send to Food");
        JButton remove = new JButton("Remove");

        add.addActionListener(event -> addItem());
        bought.addActionListener(event -> sendToFood());
        remove.addActionListener(event -> removeItem());

        buttons.add(add);
        buttons.add(bought);
        buttons.add(remove);

        add(buttons, BorderLayout.SOUTH);
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    public void refreshData() {
        model.clear();

        if (data != null) {
            for (String item : data.shoppingItems) {
                model.addElement(item);
            }
        }
    }

    private void addItem() {
        if (data == null) return;

        String name = JOptionPane.showInputDialog(
            this, "Item name:"
        );

        if (name == null) return;

        name = name.trim();

        if (name.isEmpty() || name.contains("|")) {
            JOptionPane.showMessageDialog(
                this, "Enter a name without |."
            );
            return;
        }

        if (data.shoppingItems.stream()
                .anyMatch(name::equalsIgnoreCase)) {

            JOptionPane.showMessageDialog(
                this, "That item is already on the list."
            );
            return;
        }

        data.shoppingItems.add(name);
        refreshData();
    }

    private String selectedItem() {
        String name = list.getSelectedValue();

        if (name == null) {
            JOptionPane.showMessageDialog(
                this, "Select an item first."
            );
        }

        return name;
    }

    private void sendToFood() {
        String name = selectedItem();

        if (data == null || name == null) return;

        if (JOptionPane.showConfirmDialog(
                this,
                "Bought " + name + "? Send it to Food as Full?",
                "Confirm purchase",
                JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }

        boolean found = false;

        // Refill existing food while preserving its details.
        for (int i = 0; i < data.foods.size(); i++) {
            String[] p = Food.parts(data.foods.get(i));

            if (p[0].equalsIgnoreCase(name)) {
                p[2] = "Filled";
                data.foods.set(i, String.join(" | ", p));
                found = true;
                break;
            }
        }

        // New purchases use basic defaults.
        if (!found) {
            data.foods.add(
                name + " | Uncategorized | Filled | Single use"
            );
        }

        data.shoppingItems.remove(name);
        refreshData();
    }

    private void removeItem() {
        String name = selectedItem();

        if (data == null || name == null) return;

        if (JOptionPane.showConfirmDialog(
                this,
                "Remove " + name + "?",
                "Confirm removal",
                JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {

            data.shoppingItems.remove(name);
            refreshData();
        }
    }
}