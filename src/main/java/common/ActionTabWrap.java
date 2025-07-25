package common;

import javax.swing.*;

public class ActionTabWrap {

    public ActionTab FIND_COPY = ActionTab.COPY;
    public ActionTab FIND_DUPLICATES = ActionTab.DUPLICATE;

    public enum ActionTab {
        COPY("Copied"),// active copy panel tab
        DUPLICATE("Duplicate");// active dupl. panel tab

        private final String value;

        ActionTab(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final JTabbedPane tabbedPane;

    public ActionTabWrap(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    public ActionTab getActionName() {
        return tabbedPane.getSelectedIndex() == 0 ? ActionTab.COPY : ActionTab.DUPLICATE;
    }

//    public ActionTabWrap setActionName(ActionTab name) {
//        actionName = name;
//        return this;
//    }

    public boolean isEqual(ActionTab actionTab) {
        if(actionTab == null && getActionName() == null) return true;
        if(getActionName() == null) return false;
        return getActionName().equals(actionTab);
    }
}
