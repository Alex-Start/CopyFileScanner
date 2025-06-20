package ui;

import javax.swing.*;
import java.awt.*;

public class MessageDialog extends JFrame {
    private final Component parent;

    public MessageDialog(Component parent) {
        this.parent = parent;
    }

    public void showMessageDialog(String message) {
        JOptionPane.showMessageDialog(parent, message);
    }
}
