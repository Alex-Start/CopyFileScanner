package ui;

import ui.table.FileTableModel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static ui.table.FileTableModel.isSkipCheckBoxCondition;

class CenteredCheckboxRenderer extends JCheckBox implements TableCellRenderer {
    public CenteredCheckboxRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER); // Center the checkbox
        setOpaque(true); // Needed for background painting
    }

//    @Override
//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        if (isSkipCheckBoxCondition(table, row)) {
//            return new JLabel(""); // Hide checkbox if file is copied
//        }
//        setSelected(Boolean.TRUE.equals(value)); // Set checkbox state
//        return this;
//    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Set background and foreground based on selection
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        // Get the model row index in case table is sorted
        int modelRow = table.convertRowIndexToModel(row);
        FileTableModel model = (FileTableModel) table.getModel();

        // Check if the cell is editable (i.e., checkbox should be visible and interactive)
        // This logic replaces the old static isSkipCheckBoxCondition
        if (!model.isCellEditable(modelRow, column)) {
            // If not editable, show an empty label (or just don't set the checkbox state)
            return new JLabel(""); // Hides the checkbox visually
        }

        setSelected(Boolean.TRUE.equals(value));
        return this;
    }
}
