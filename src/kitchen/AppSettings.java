package kitchen;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

// Display preferences remain usable in memory if the local preference store is unavailable.
public final class AppSettings {
    private final Preferences prefs;
    private final List<Runnable> listeners = new ArrayList<>();
    private boolean alphabetize, details, compact, larger, confirm;

    public AppSettings() { this(openPreferences()); }
    AppSettings(Preferences preferences) {
        prefs = preferences;
        alphabetize = read("alphabetizeFood", false);
        details = read("showFoodDetails", true);
        compact = read("compactFoodRows", false);
        larger = read("largerListText", false);
        confirm = read("confirmStockChanges", true);
    }
    private static Preferences openPreferences() {
        try { return Preferences.userNodeForPackage(AppSettings.class); }
        catch (SecurityException unavailable) { return null; }
    }
    private boolean read(String key, boolean fallback) {
        try { return prefs == null ? fallback : prefs.getBoolean(key, fallback); }
        catch (SecurityException unavailable) { return fallback; }
    }
    private void write(String key, boolean value) {
        if (prefs != null) try { prefs.putBoolean(key, value); }
        catch (SecurityException unavailable) { /* Current-session preference still works. */ }
    }
    public boolean alphabetizeFood() { return alphabetize; }
    public boolean showFoodDetails() { return details; }
    public boolean compactFoodRows() { return compact; }
    public boolean largerListText() { return larger; }
    public boolean confirmStockChanges() { return confirm; }
    public void setAlphabetizeFood(boolean value) { alphabetize=value; write("alphabetizeFood",value); notifyChanged(); }
    public void setShowFoodDetails(boolean value) { details=value; write("showFoodDetails",value); notifyChanged(); }
    public void setCompactFoodRows(boolean value) { compact=value; write("compactFoodRows",value); notifyChanged(); }
    public void setLargerListText(boolean value) { larger=value; write("largerListText",value); notifyChanged(); }
    public void setConfirmStockChanges(boolean value) { confirm=value; write("confirmStockChanges",value); notifyChanged(); }
    public void onChange(Runnable listener) { listeners.add(listener); }
    public void reset() {
        alphabetize=false; details=true; compact=false; larger=false; confirm=true;
        write("alphabetizeFood",false); write("showFoodDetails",true); write("compactFoodRows",false);
        write("largerListText",false); write("confirmStockChanges",true);
        notifyChanged();
    }
    private void notifyChanged() { for (Runnable listener : new ArrayList<>(listeners)) listener.run(); }
}
