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

    private final List<String> visibleItems =
        new ArrayList<>();

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

    // Keep the previous data format compatible.
    // name | unused category | status | type | quantity | unit | storage
    static String[] parts(String item) {
        String[] old = item.split(" \\| ", -1);

        String[] p = {
            old[0], "Uncategorized", "Filled",
            "Single use", "", "", "Cabinet"
        };

        if (old.length >= 4) {
            System.arraycopy(
                old, 0, p, 0, Math.min(old.length, p.length)
            );
        }

        return p;
    }

    static String shoppingName(String item) {
        return item.split(" \\| ", -1)[0];
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
                p[0]
                    + " - "
                    + (p[2].equals("Filled") ? "Full" : "Low")
                    + (p[4].isEmpty()
                        ? ""
                        : " - " + p[4] + " " + p[5])
                    + " - " + p[6]
                    + (p[3].equals("Staple") ? " (Staple)" : "")
            );
        }
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