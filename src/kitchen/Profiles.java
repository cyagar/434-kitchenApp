package kitchen;

import java.awt.*;
import javax.swing.*;

/** Household profiles: name, allergies and dietary restrictions. */
public class Profiles extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> members = new JList<>(listModel);
    private final JTextField nameField = new JTextField();
    private final JTextArea allergiesField = new JTextArea(4, 20);
    private final JTextArea dietField = new JTextArea(4, 20);
    private int selectedIndex = -1;

    public Profiles() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(new JLabel("Household Members"), BorderLayout.NORTH);

        members.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        members.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });

        JPanel editor = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(6, 1, 4, 4));
        fields.add(new JLabel("Name:"));
        fields.add(nameField);
        fields.add(new JLabel("Allergies (e.g. peanuts, milk):"));
        allergiesField.setLineWrap(true);
        allergiesField.setWrapStyleWord(true);
        fields.add(new JScrollPane(allergiesField));
        fields.add(new JLabel("Dietary restrictions (e.g. vegetarian, halal):"));
        dietField.setLineWrap(true);
        dietField.setWrapStyleWord(true);
        fields.add(new JScrollPane(dietField));
        editor.add(fields, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(members), editor);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        JButton add = new JButton("Add Person");
        JButton save = new JButton("Save Changes");
        JButton remove = new JButton("Remove Person");
        add.addActionListener(e -> addPerson());
        save.addActionListener(e -> savePerson());
        remove.addActionListener(e -> removePerson());
        buttons.add(add);
        buttons.add(save);
        buttons.add(remove);
        add(buttons, BorderLayout.SOUTH);
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    public void refreshData() {
        int index = members.getSelectedIndex();
        listModel.clear();
        if (data != null) {
            for (String profile : data.profiles) {
                listModel.addElement(unpack(profile)[0]);
            }
        }
        if (index >= 0 && index < listModel.size()) members.setSelectedIndex(index);
        else if (selectedIndex >= listModel.size()) {
            selectedIndex = -1;
            clearFields();
        }
    }

    private static String[] unpack(String profile) {
        String[] parts = profile.split(" \\| ", -1);
        return new String[] {
            parts.length > 0 ? parts[0] : "",
            parts.length > 1 ? parts[1] : "",
            parts.length > 2 ? parts[2] : ""
        };
    }

    private static String pack(String name, String allergies, String diet) {
        return name + " | " + allergies + " | " + diet;
    }

    private void loadSelected() {
        selectedIndex = members.getSelectedIndex();
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) return;
        String[] fields = unpack(data.profiles.get(selectedIndex));
        nameField.setText(fields[0]);
        allergiesField.setText(fields[1]);
        dietField.setText(fields[2]);
    }

    private void clearFields() {
        nameField.setText("");
        allergiesField.setText("");
        dietField.setText("");
    }

    private String validatedProfile(int skipIndex) {
        String name = nameField.getText().trim();
        String allergies = allergiesField.getText().trim();
        String diet = dietField.getText().trim();
        if (name.isEmpty() || name.contains("|") || allergies.contains("|") || diet.contains("|")) {
            JOptionPane.showMessageDialog(this, "Enter a name and do not use the | character.");
            return null;
        }
        for (int i = 0; i < data.profiles.size(); i++) {
            if (i != skipIndex && unpack(data.profiles.get(i))[0].equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this, "This household member already exists.");
                return null;
            }
        }
        return pack(name, allergies, diet);
    }

    private void addPerson() {
        if (data == null) return;
        // Start a new entry rather than overwriting the selected member.
        members.clearSelection();
        selectedIndex = -1;
        clearFields();
        String name = JOptionPane.showInputDialog(this, "New household member's name:");
        if (name == null) return;
        nameField.setText(name.trim());
        String value = validatedProfile(-1);
        if (value == null) return;
        data.profiles.add(value);
        refreshData();
        members.setSelectedIndex(data.profiles.size() - 1);
    }

    private void savePerson() {
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) {
            JOptionPane.showMessageDialog(this, "Select a household member to edit.");
            return;
        }
        String value = validatedProfile(selectedIndex);
        if (value == null) return;
        data.profiles.set(selectedIndex, value);
        refreshData();
        JOptionPane.showMessageDialog(this, "Profile saved.");
    }

    private void removePerson() {
        int index = members.getSelectedIndex();
        if (data == null || index < 0) {
            JOptionPane.showMessageDialog(this, "Select a household member first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Remove " + listModel.get(index) + "?",
                "Confirm removal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        data.profiles.remove(index);
        members.clearSelection();
        selectedIndex = -1;
        clearFields();
        refreshData();
    }
}
