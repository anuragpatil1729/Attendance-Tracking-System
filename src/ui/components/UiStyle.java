package ui.components;

import util.Constants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public final class UiStyle {
    private UiStyle() {
    }

    public static Border roundedBorder(int radius) {
        return new RoundedBorder(radius);
    }

    public static void styleField(JTextField field, String tooltip) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setBorder(roundedBorder(12));
        field.setBackground(Constants.INPUT);
        field.setForeground(Constants.TEXT);
        field.setCaretColor(Constants.ACCENT);
        field.setToolTipText(tooltip);
    }

    public static void styleComponent(JComponent component, int radius) {
        component.setBorder(roundedBorder(radius));
        component.setForeground(Constants.TEXT);
        component.setBackground(Constants.INPUT);
    }

    public static JButton createButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(Constants.FONT.deriveFont(Font.BOLD, 14f));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorder(roundedBorder(12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JPanel createCardLayout() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                roundedBorder(16),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        return card;
    }

    public static void styleTable(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(32);
        table.setFocusable(false);
        table.setBorder(null);
        table.setBackground(Constants.INPUT);
        table.setForeground(Constants.TEXT);
        table.setSelectionBackground(Constants.ACCENT.darker());
        table.setSelectionForeground(Constants.TEXT);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBorder(null);
    }

    public static JScrollPane wrapScroll(JComponent content, Color bg) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(bg);
        scroll.setBackground(bg);
        return scroll;
    }

    public static void installPlaceholder(JTextField field, String placeholder) {
        Color dimmed = Constants.TEXT.darker();
        field.setForeground(dimmed);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (placeholder.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(Constants.TEXT);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isBlank()) {
                    field.setForeground(dimmed);
                    field.setText(placeholder);
                }
            }
        });
    }

    public static boolean isShowingPlaceholder(JTextField field, String placeholder) {
        return placeholder.equals(field.getText()) && !field.hasFocus();
    }

    public static void onType(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }
}
