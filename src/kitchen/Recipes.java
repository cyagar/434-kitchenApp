package kitchen;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;

// Member: ?
public class Recipes extends JPanel {
    private static final long serialVersionUID = 1L;
    private AppData data;
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    public Recipes() {
        // Add recipe details and send missing ingredients to Shopping.
    }

    public void setAppData(AppData data) {
        this.data = data;
        refreshData();
    }

    // Refresh shared data when returning to this tab.
    public void refreshData() {
        listModel.clear();
        if (data != null) {
            for (String item : data.recipes) {
                listModel.addElement(item);
            }
        }
    }
}
