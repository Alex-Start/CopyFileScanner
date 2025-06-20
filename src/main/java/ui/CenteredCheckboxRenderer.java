package ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static ui.table.FileTableModel.isSkipCheckBoxCondition;

class CenteredCheckboxRenderer extends JCheckBox implements TableCellRenderer {
    public CenteredCheckboxRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER); // Center the checkbox
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSkipCheckBoxCondition(table, row)) {
            return new JLabel(""); // Hide checkbox if file is copied
        }
        setSelected(Boolean.TRUE.equals(value)); // Set checkbox state
        return this;
    }
}
