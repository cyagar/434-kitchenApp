package kitchen;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

// Member: Ezra
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

    public Shopping() {
        setLayout(new BorderLayout(10, 10));
        setBorder(
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        add(new WrapText("Check purchased items, then choose Send to Food."), BorderLayout.NORTH);

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
            JTextArea name = new JTextArea(Food.shoppingName(value));
            name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN,
                    settings != null && settings.largerListText() ? 18 : 15));
            name.setLineWrap(true);
            name.setWrapStyleWord(true);
            name.setOpaque(false);
            name.setEditable(false);
            int width = Math.max(100, list.getWidth() - 74);
            name.setSize(width, 10000);
            int height = name.getPreferredSize().height;
            row.add(name, BorderLayout.CENTER);
            row.setPreferredSize(new Dimension(Math.max(180, list.getWidth() - 4), Math.max(45, height + 19)));
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

        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(
            new GridLayout(1, 3, 8, 0)
        );

        JButton add = new JButton("Add");
        JButton remove = new JButton("Remove");
        JButton send = new JButton("Send to Food");

        add.addActionListener(event -> addItem());
        remove.addActionListener(event -> removeItem());
        send.addActionListener(event -> sendToFood());

        buttons.add(add);
        buttons.add(remove);
        buttons.add(send);

        add(buttons, BorderLayout.SOUTH);
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
        }
    }

    private void toggle(String item) {
        if (!checked.add(item)) {
            checked.remove(item);
        }

        list.repaint();
    }

    private String selected() {
        String item = list.getSelectedValue();

        if (item == null) {
            JOptionPane.showMessageDialog(
                this, "Select a shopping item first."
            );
        }

        return item;
    }

    private void addItem() {
        if (data == null) return;

        String entered = JOptionPane.showInputDialog(
            this, "Item name:"
        );

        if (entered == null) return;

        String name = entered.trim();

        boolean duplicate = data.shoppingItems.stream()
            .anyMatch(
                item -> Food.shoppingName(item)
                    .equalsIgnoreCase(name)
            );

        if (name.isEmpty()
                || name.contains("|")
                || duplicate) {

            JOptionPane.showMessageDialog(
                this, "Enter a unique name without |."
            );
            return;
        }

        data.shoppingItems.add(name);
        refreshData();
    }

    private void removeItem() {
        if (data == null) return;

        String item = selected();

        if (item == null) return;

        if (JOptionPane.showConfirmDialog(
                this,
                "Remove " + Food.shoppingName(item) + "?",
                "Confirm removal",
                JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {

            data.shoppingItems.remove(item);
            refreshData();
        }
    }

    private void sendToFood() {
        if (data == null) return;

        if (checked.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Check purchased items first."
            );
            return;
        }

        if (JOptionPane.showConfirmDialog(
                this,
                "Send " + checked.size()
                    + " checked item(s) to Food as Full?",
                "Confirm purchase",
                JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {

            return;
        }

        for (String item : new ArrayList<>(checked)) {
            String name = Food.shoppingName(item);
            boolean found = false;

            for (int i = 0; i < data.foods.size(); i++) {
                String[] p = Food.parts(data.foods.get(i));

                if (p[0].equalsIgnoreCase(name)) {
                    p[2] = "Filled";

                    data.foods.set(
                        i, String.join(" | ", p)
                    );

                    found = true;
                    break;
                }
            }

            if (!found) {
                data.foods.add(
                    String.join(
                        " | ",
                        name,
                        "Uncategorized",
                        "Filled",
                        "Single use",
                        "",
                        "",
                        "Cabinet"
                    )
                );
            }

            data.shoppingItems.remove(item);
        }

        checked.clear();
        refreshData();
    }
}