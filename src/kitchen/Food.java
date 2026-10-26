package kitchen;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Member: Ceyhun
public class Food extends JPanel {

    private static final long serialVersionUID = 1L;

    private AppData data;

    private final DefaultListModel<String> model =
        new DefaultListModel<>();

    private final JList<String> list = new JList<>(model);

    private final List<String> visibleItems = new ArrayList<>();

    public Food() {
        setLayout(new BorderLayout(10, 10));
        setBorder(
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        add(new JLabel("Food at home"), BorderLayout.NORTH);

        list.setFixedCellHeight(44);
        list.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(
            new GridLayout(1, 2, 10, 0)
        );

        JButton add = new JButton("Add food");
        JButton status = new JButton("Change status");

        add.addActionListener(event -> addFood());
        status.addActionListener(event -> changeStatus());

        buttons.add(add);
        buttons.add(status);

        add(buttons, BorderLayout.SOUTH);
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    static String[] parts(String item) {
        String[] p = item.split(" \\| ", -1);

        return p.length == 4
            ? p
            : new String[] {
                item, "Uncategorized", "Filled", "Single use"
            };
    }

    public void refreshData() {
        model.clear();
        visibleItems.clear();

        if (data == null) return;

        for (String item : data.foods) {
            String[] p = parts(item);

            if (p[2].equals("Empty")) continue;

            visibleItems.add(item);

            model.addElement(
                p[0] + " - " + p[1] + " - "
                + (p[2].equals("Filled") ? "Full" : "Low")
                + (p[3].equals("Staple") ? " (Staple)" : "")
            );
        }
    }

    private void addFood() {
        if (data == null) return;

        JTextField name = new JTextField();

        JComboBox<String> category = new JComboBox<>(
            new String[] {
                "Uncategorized", "Dairy", "Meat",
                "Fruits", "Vegetables", "Leftovers"
            }
        );

        JComboBox<String> type = new JComboBox<>(
            new String[] {
                "Choose type", "Staple", "Single use"
            }
        );

        Object[] form = {
            "Name:", name,
            "Category:", category,
            "Type:", type
        };

        while (JOptionPane.showConfirmDialog(
                this, form, "Add food",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION) {

            String entered = name.getText().trim();

            boolean duplicate = data.foods.stream()
                .anyMatch(
                    item -> parts(item)[0]
                        .equalsIgnoreCase(entered)
                );

            if (entered.isEmpty()
                    || entered.contains("|")
                    || duplicate
                    || type.getSelectedIndex() == 0) {

                JOptionPane.showMessageDialog(
                    this,
                    "Enter a unique name without | and choose a type."
                );
                continue;
            }

            data.foods.add(
                String.join(
                    " | ",
                    entered,
                    category.getSelectedItem().toString(),
                    "Filled",
                    type.getSelectedItem().toString()
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
            p[2].equals("Filled") ? "Full" : "Low"
        );

        if (choice == null) return;

        if (JOptionPane.showConfirmDialog(
                this,
                "Change " + p[0] + " to " + choice + "?",
                "Confirm",
                JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
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

        if (p[3].equals("Staple")
                && !choice.equals("Full")
                && data.shoppingItems.stream()
                    .noneMatch(p[0]::equalsIgnoreCase)) {

            data.shoppingItems.add(p[0]);
        }

        refreshData();
    }
}