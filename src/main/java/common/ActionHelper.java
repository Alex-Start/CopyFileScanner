package common;

public class ActionHelper {
    public enum Action {
        SCAN("Scan"),// scan files in threads
        COPY("Copy"),// click copy button
        DELETE_DEST("Delete Destination"),// click Delete button
        DELETE_SOURCE("Delete Source");// click Delete button

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private Action actionName;

    public ActionHelper(Action name) {
        actionName = name;
    }

    public Action getActionName() {
        return actionName;
    }

    public ActionHelper setActionName(Action name) {
        actionName = name;
        return this;
    }

    public boolean isEqual(Action action) {
        if(action == null && actionName == null) return true;
        if(actionName == null) return false;
        return actionName.equals(action);
    }
}
