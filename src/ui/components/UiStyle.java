package ui.components;

import util.Constants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UiStyle {
    private UiStyle() {
    }

    public static Border roundedBorder(int radius) {
        return new RoundedBorder(radius);
    }

    public static Border sectionBorder(int radius) {
        return BorderFactory.createCompoundBorder(
                roundedBorder(radius),
                BorderFactory.createMatteBorder(0, 3, 0, 0, Constants.ACCENT));
    }

    public static JPanel sectionCard(LayoutManager layout, int radius) {
        JPanel card = new JPanel(layout);
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                sectionBorder(radius),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        return card;
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
        installButtonEffects(button, bg);
        return button;
    }

    public static void installButtonEffects(AbstractButton button, Color baseColor) {
        Color hover = Constants.brighten(baseColor, 0.15f);
        Color pressed = Constants.darken(baseColor, 0.10f);
        Border normal = roundedBorder(12);
        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Constants.ACCENT, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.isEnabled()) {
                    return;
                }
                button.setBackground(hover);
                button.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
                button.setBorder(normal);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!button.isEnabled()) {
                    return;
                }
                button.setBackground(pressed);
                button.setBorder(hoverBorder);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) {
                    return;
                }
                button.setBackground(button.contains(e.getPoint()) ? hover : baseColor);
                button.setBorder(button.contains(e.getPoint()) ? hoverBorder : normal);
            }
        });
    }

    public static JPanel createCardLayout() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(
                sectionBorder(16),
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
        table.setSelectionBackground(Constants.darken(Constants.ACCENT, 0.35f));
        table.setSelectionForeground(Constants.TEXT);
        JTableHeader header = table.getTableHeader();
        styleTableHeader(header);
    }

    public static void styleTableHeader(JTableHeader header) {
        header.setReorderingAllowed(false);
        header.setBackground(Constants.SIDEBAR);
        header.setForeground(Constants.ACCENT);
        header.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Constants.ACCENT));
    }

    public static void styleTabbedPane(JTabbedPane tabs) {
        tabs.setBackground(Constants.SIDEBAR);
        tabs.setForeground(Constants.TEXT);
        tabs.setFont(Constants.FONT.deriveFont(Font.BOLD, 13f));
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = Constants.ACCENT;
                lightHighlight = Constants.ACCENT;
                shadow = Constants.SIDEBAR;
                darkShadow = Constants.SIDEBAR;
                focus = Constants.ACCENT;
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? Constants.ACCENT : Constants.SIDEBAR);
                g.fillRect(x, y, w, h);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                // remove default border
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // remove content border
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                               Rectangle[] rects, int tabIndex,
                                               Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // no focus border
            }
        });
        tabs.setOpaque(true);
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
