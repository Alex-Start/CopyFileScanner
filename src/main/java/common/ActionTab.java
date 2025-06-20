package common;

public class ActionTab {

    public enum Tab {
        COPY("Copied"),// active copy panel tab
        DUPLICATE("Duplicate");// active dupl. panel tab

        private final String value;

        Tab(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private Tab actionName;

    public ActionTab(Tab name) {
        actionName = name;
    }

    public Tab getActionName() {
        return actionName;
    }

    public ActionTab setActionName(Tab name) {
        actionName = name;
        return this;
    }

    public boolean isEqual(Tab tab) {
        if(tab == null && actionName == null) return true;
        if(actionName == null) return false;
        return actionName.equals(tab);
    }
}
