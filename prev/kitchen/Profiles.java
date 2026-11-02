package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Person-first Profiles: select a household member, then edit their details. */
public class Profiles extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private int selectedIndex = -1;

    private final CardLayout screens = new CardLayout();
    private final JPanel content = new JPanel(screens);
    private final ViewportColumn memberCards = new ViewportColumn();
    private final JTextField nameField = new JTextField();
    private final JTextArea allergiesField = new JTextArea(4, 16);
    private final JTextArea dietField = new JTextArea(4, 16);
    private final JLabel editingTitle = new JLabel("Person details");

    public Profiles() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel landing = new JPanel(new BorderLayout(10, 12));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Household members");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(6));
        WrapText hint = new WrapText("Tap a person's name to add or change their allergies and dietary restrictions.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(hint);
        landing.add(top, BorderLayout.NORTH);
        memberCards.setBorder(new EmptyBorder(4, 2, 4, 2));
        JScrollPane memberScroll = new JScrollPane(memberCards);
        memberScroll.setBorder(BorderFactory.createEmptyBorder());
        memberScroll.getVerticalScrollBar().setUnitIncrement(16);
        landing.add(memberScroll, BorderLayout.CENTER);

        JButton addPerson = new JButton("+ Add a person");
        addPerson.addActionListener(e -> addPerson());
        landing.add(addPerson, BorderLayout.SOUTH);

        JPanel editor = new JPanel(new BorderLayout(0, 12));
        JPanel header = new JPanel(new BorderLayout(10, 4));
        JButton back = new JButton("\u2190 Back");
        back.addActionListener(e -> goBack());
        header.add(back, BorderLayout.WEST);
        editingTitle.setFont(editingTitle.getFont().deriveFont(Font.BOLD, 17f));
        header.add(editingTitle, BorderLayout.CENTER);
        editor.add(header, BorderLayout.NORTH);

        ViewportColumn fields = new ViewportColumn();
        fields.setBorder(new EmptyBorder(4, 2, 10, 6));
        addHeading(fields, "Name");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nameField.setPreferredSize(new Dimension(220, 40));
        fields.add(nameField);
        fields.add(Box.createVerticalStrut(20));

        addHeading(fields, "Allergies");
        addDescription(fields, "Enter allergens separated by commas, such as peanuts, milk, shellfish.");
        configureArea(allergiesField);
        fields.add(new JScrollPane(allergiesField));
        fields.add(Box.createVerticalStrut(20));

        addHeading(fields, "Dietary restrictions");
        addDescription(fields, "Enter restrictions separated by commas, such as vegetarian, vegan, gluten-free.");
        configureArea(dietField);
        fields.add(new JScrollPane(dietField));
        fields.add(Box.createVerticalStrut(15));
        addDescription(fields, "\u26A0 Food alerts are possible matches based on names. Always check full ingredient labels.");

        JScrollPane editScroll = new JScrollPane(fields);
        editScroll.setBorder(BorderFactory.createEmptyBorder());
        editScroll.getVerticalScrollBar().setUnitIncrement(16);
        editor.add(editScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 9, 0));
        JButton save = new JButton("Save changes");
        JButton remove = new JButton("Remove person");
        save.addActionListener(e -> savePerson());
        remove.addActionListener(e -> removePerson());
        actions.add(save);
        actions.add(remove);
        editor.add(actions, BorderLayout.SOUTH);

        content.add(landing, "members");
        content.add(editor, "details");
        add(content, BorderLayout.CENTER);
        screens.show(content, "members");
    }

    private static void configureArea(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        area.setMargin(new Insets(8, 9, 8, 9));
        area.setRows(4);
    }

    private static void addHeading(JPanel column, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(label);
        column.add(Box.createVerticalStrut(7));
    }

    private static void addDescription(JPanel column, String text) {
        WrapText description = new WrapText(text);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(description);
        column.add(Box.createVerticalStrut(8));
    }

    public void setAppData(AppData data) { this.data = data; refreshData(); }

    public void refreshData() {
        memberCards.removeAll();
        if (data == null || data.profiles.isEmpty()) {
            addDescription(memberCards, "No people yet. Add someone to set their food preferences.");
        } else {
            for (int i = 0; i < data.profiles.size(); i++) {
                final int index = i;
                String[] values = unpack(data.profiles.get(i));
                JButton button = new JButton(values[0] + "   \u203a");
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
                button.setPreferredSize(new Dimension(260, 54));
                button.setAlignmentX(Component.LEFT_ALIGNMENT);
                button.setToolTipText("Open preferences for " + values[0]);
                button.addActionListener(e -> openPerson(index));
                memberCards.add(button);
                memberCards.add(Box.createVerticalStrut(9));
            }
        }
        memberCards.revalidate();
        memberCards.repaint();
    }

    private static String[] unpack(String profile) {
        String[] parts = profile.split(" \\| ", -1);
        return new String[]{parts.length > 0 ? parts[0] : "",
                parts.length > 1 ? parts[1] : "",
                parts.length > 2 ? parts[2] : ""};
    }
    private static String pack(String name, String allergies, String diet) {
        return name + " | " + allergies + " | " + diet;
    }

    private void openPerson(int index) {
        if (data == null || index < 0 || index >= data.profiles.size()) return;
        selectedIndex = index;
        String[] values = unpack(data.profiles.get(index));
        nameField.setText(values[0]);
        allergiesField.setText(values[1]);
        dietField.setText(values[2]);
        allergiesField.setCaretPosition(0);
        dietField.setCaretPosition(0);
        editingTitle.setText("Edit " + values[0]);
        screens.show(content, "details");
    }

    private void goBack() {
        if (selectedIndex >= 0 && isDirty() &&
                JOptionPane.showConfirmDialog(this, "Discard unsaved changes?",
                        "Go back", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        selectedIndex = -1;
        refreshData();
        screens.show(content, "members");
    }

    private boolean isDirty() {
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) return false;
        String[] values = unpack(data.profiles.get(selectedIndex));
        return !values[0].equals(nameField.getText().trim())
                || !values[1].equals(allergiesField.getText().trim())
                || !values[2].equals(dietField.getText().trim());
    }

    private void addPerson() {
        if (data == null) return;
        String name = JOptionPane.showInputDialog(this, "Person's name:", "Add a person",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        name = name.trim();
        if (!validName(name, -1)) return;
        data.profiles.add(pack(name, "", ""));
        refreshData();
        openPerson(data.profiles.size() - 1); // Go straight to allergy + diet fields.
    }

    private boolean validName(String name, int skipIndex) {
        if (name.isEmpty() || name.contains("|")) {
            JOptionPane.showMessageDialog(this, "Enter a name without the | character.");
            return false;
        }
        for (int i = 0; data != null && i < data.profiles.size(); i++) {
            if (i != skipIndex && unpack(data.profiles.get(i))[0].equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this, "This household member already exists.");
                return false;
            }
        }
        return true;
    }

    private void savePerson() {
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) return;
        String name = nameField.getText().trim();
        String allergies = allergiesField.getText().trim();
        String diet = dietField.getText().trim();
        if (!validName(name, selectedIndex)) return;
        if (allergies.contains("|") || diet.contains("|")) {
            JOptionPane.showMessageDialog(this, "Please do not use the | character in preferences.");
            return;
        }
        data.profiles.set(selectedIndex, pack(name, allergies, diet));
        refreshData();
        editingTitle.setText("Edit " + name);
        JOptionPane.showMessageDialog(this, "Saved preferences for " + name + ".");
    }

    private void removePerson() {
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) return;
        String name = unpack(data.profiles.get(selectedIndex))[0];
        if (JOptionPane.showConfirmDialog(this, "Remove " + name + " from the household?",
                "Confirm removal", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        data.profiles.remove(selectedIndex);
        selectedIndex = -1;
        refreshData();
        screens.show(content, "members");
    }

    /** A BoxLayout column whose width follows the scrolling viewport. */
    private static class ViewportColumn extends JPanel implements Scrollable {
        ViewportColumn() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 18; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return visible.height - 25; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
