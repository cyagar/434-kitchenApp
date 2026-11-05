package kitchen;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Profiles, select a household member, then edit their details.
public class Profiles extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private int selectedIndex = -1;
    private JButton removeButton;

    private final CardLayout screens = new CardLayout();
    private final JPanel content = new JPanel(screens);
    private final JPanel memberCards = AppScreens.form();
    private final JTextField nameField = new JTextField();
    private final JTextArea allergiesField = new JTextArea(4, 16);
    private final JTextArea dietField = new JTextArea(4, 16);
    private final JLabel editingTitle = new JLabel("Person details");
    private JScrollPane editScroll;

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
        memberScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        memberScroll.setBorder(BorderFactory.createEmptyBorder());
        memberScroll.getVerticalScrollBar().setUnitIncrement(16);
        landing.add(memberScroll, BorderLayout.CENTER);

        JButton addPerson = new RoundedButton("+ Add a person");
        addPerson.addActionListener(e -> addPerson());
        landing.add(addPerson, BorderLayout.SOUTH);

        JPanel editor = new JPanel(new BorderLayout(0, 12));
        JPanel header = new JPanel(new BorderLayout(10, 4));
        JButton back = new RoundedButton("\u2190 Back");
        back.addActionListener(e -> goBack());
        header.add(back, BorderLayout.WEST);
        editingTitle.setFont(editingTitle.getFont().deriveFont(Font.BOLD, 17f));
        header.add(editingTitle, BorderLayout.CENTER);
        editor.add(header, BorderLayout.NORTH);

        JPanel fields = AppScreens.form();
        fields.setBorder(new EmptyBorder(4, 2, 10, 6));
        addHeading(fields, "Name");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nameField.setPreferredSize(new Dimension(220, 40));
        fields.add(nameField);
        fields.add(Box.createVerticalStrut(20));

        addHeading(fields, "Allergies");
        addDescription(fields, "Enter allergens separated by commas, such as peanuts, milk, shellfish.");
        configureArea(allergiesField);
        JScrollPane allergiesScroll = new JScrollPane(allergiesField);
        allergiesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        AppScreens.passWheelToPage(allergiesScroll);
        fields.add(allergiesScroll);
        fields.add(Box.createVerticalStrut(20));

        addHeading(fields, "Dietary restrictions");
        addDescription(fields, "Enter restrictions separated by commas, such as vegetarian, vegan, gluten-free.");
        configureArea(dietField);
        JScrollPane dietScroll = new JScrollPane(dietField);
        dietScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        AppScreens.passWheelToPage(dietScroll);
        fields.add(dietScroll);
        fields.add(Box.createVerticalStrut(15));
        addDescription(fields, "\u26A0 Pantry alerts are possible matches based on names. Always check full ingredient labels.");

        editScroll = new JScrollPane(fields);
        editScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        editScroll.setBorder(BorderFactory.createEmptyBorder());
        editScroll.getVerticalScrollBar().setUnitIncrement(16);
        editor.add(editScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 9, 0));
        JButton save = new RoundedButton("Save changes");
        JButton remove = new RoundedButton("Remove person");
        removeButton = remove;
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
                JButton button = new RoundedButton(values[0] + "   \u203a");
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
        String[] parts = AppData.fields(profile);
        return new String[]{parts.length > 0 ? parts[0] : "",
                parts.length > 1 ? parts[1] : "",
                parts.length > 2 ? parts[2] : ""};
    }
    private static String pack(String name, String allergies, String diet) {
        return AppData.join(name, allergies, diet);
    }

    private void openPerson(int index) {
        if (data == null || index < 0 || index >= data.profiles.size()) return;
        selectedIndex = index;
        removeButton.setEnabled(true);
        String[] values = unpack(data.profiles.get(index));
        nameField.setText(values[0]);
        allergiesField.setText(values[1]);
        dietField.setText(values[2]);
        allergiesField.setCaretPosition(0);
        dietField.setCaretPosition(0);
        editingTitle.setText("Edit " + values[0]);
        screens.show(content, "details");
        editScroll.getVerticalScrollBar().setValue(0);
    }

    private void goBack() {
        Runnable back = () -> {
            selectedIndex = -1;
            refreshData();
            screens.show(content, "members");
        };
        if (isDirty()) AppScreens.confirm(this, "Discard changes", "Discard unsaved changes?", back);
        else back.run();
    }

    private boolean isDirty() {
        if (data == null) return false;
        if (selectedIndex < 0) return !nameField.getText().trim().isEmpty()
                || !allergiesField.getText().trim().isEmpty() || !dietField.getText().trim().isEmpty();
        if (selectedIndex >= data.profiles.size()) return false;
        String[] values = unpack(data.profiles.get(selectedIndex));
        return !values[0].equals(nameField.getText().trim())
                || !values[1].equals(allergiesField.getText().trim())
                || !values[2].equals(dietField.getText().trim());
    }

    private void addPerson() {
        if (data == null) return;
        selectedIndex = -1;
        nameField.setText("");
        allergiesField.setText("");
        dietField.setText("");
        editingTitle.setText("Add a person");
        removeButton.setEnabled(false);
        screens.show(content, "details");
        editScroll.getVerticalScrollBar().setValue(0);
    }

    private boolean validName(String name, int skipIndex) {
        if (name.isEmpty()) {
            AppScreens.message(this, "Check name", "Enter a name.");
            return false;
        }
        for (int i = 0; data != null && i < data.profiles.size(); i++) {
            if (i != skipIndex && unpack(data.profiles.get(i))[0].equalsIgnoreCase(name)) {
                AppScreens.message(this, "Check name", "That person is already in your household.");
                return false;
            }
        }
        return true;
    }

    private void savePerson() {
        if (data == null || selectedIndex >= data.profiles.size()) return;
        String name = nameField.getText().trim();
        String allergies = allergiesField.getText().trim();
        String diet = dietField.getText().trim();
        if (!validName(name, selectedIndex)) return;
        if (selectedIndex < 0) {
            data.profiles.add(pack(name, allergies, diet));
            selectedIndex = data.profiles.size() - 1;
        } else data.profiles.set(selectedIndex, pack(name, allergies, diet));
        refreshData();
        editingTitle.setText("Edit " + name);
        removeButton.setEnabled(true);
        AppScreens.message(this, "Preferences saved", "Saved preferences for " + name + ".");
    }

    private void removePerson() {
        if (data == null || selectedIndex < 0 || selectedIndex >= data.profiles.size()) return;
        String original = data.profiles.get(selectedIndex);
        String name = unpack(original)[0];
        AppScreens.confirm(this, "Remove person", "Remove " + name + " from the household?", () -> {
            data.profiles.remove(original);
            selectedIndex = -1;
            refreshData();
            screens.show(content, "members");
        });
    }

}
