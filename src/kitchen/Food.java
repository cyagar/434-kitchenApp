package kitchen;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

// Member: ?
public class Food extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public Food() {
        // Add food entry/editing, status updates, search, and categories.
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    // Refresh shared data when returning to this tab.
    public void refreshData() {
        listModel.clear();
        if (data != null) {
            for (String item : data.foods) {
                listModel.addElement(item);
            }
        }
    }
}
