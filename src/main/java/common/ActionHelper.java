package common;

public class ActionHelper {

    public enum ActionEnum {
        SCAN("Scan"),
        COPY("Copied"),
        DELETE("Deleted");

        private final String value;

        ActionEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private ActionEnum actionName;

    public ActionHelper(ActionEnum name) {
        actionName = name;
    }

    public ActionEnum getActionName() {
        return actionName;
    }

    public ActionHelper setActionName(ActionEnum name) {
        actionName = name;
        return this;
    }

    public boolean isEqual(ActionEnum actionEnum) {
        if(actionEnum == null && actionName == null) return true;
        if(actionName == null) return false;
        return actionName.equals(actionEnum);
    }
}
