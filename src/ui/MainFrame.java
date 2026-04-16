package ui;

import db.DBConnection;
import service.AttendanceService;
import util.Constants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Root frame with sidebar navigation and card content area.
 */
public class MainFrame extends JFrame {
    private static final String CARD_ATTENDANCE = "attendance";
    private static final String CARD_VIEW = "view";
    private static final String CARD_REPORTS = "reports";
    private static final String CARD_SETTINGS = "settings";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    public MainFrame() {
        super(Constants.APP_TITLE);
        setSize(Constants.WINDOW_W, Constants.WINDOW_H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        AttendanceService service = new AttendanceService();

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(service), BorderLayout.CENTER);

        getContentPane().setBackground(Constants.BG_COLOR);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                DBConnection.close();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Constants.SIDEBAR_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel(Constants.APP_TITLE);
        title.setFont(Constants.APP_FONT.deriveFont(Font.BOLD, 18));
        title.setForeground(Constants.ACCENT_COLOR);

        JLabel dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ISO_DATE), SwingConstants.RIGHT);
        dateLabel.setForeground(Constants.TEXT_COLOR);
        dateLabel.setFont(Constants.APP_FONT);

        header.add(title, BorderLayout.WEST);
        header.add(dateLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Constants.SIDEBAR_COLOR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 12, 20, 12));

        sidebar.add(createNavButton("Mark Attendance", CARD_ATTENDANCE));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("View Records", CARD_VIEW));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Reports", CARD_REPORTS));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Settings", CARD_SETTINGS));
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JButton createNavButton(String text, String card) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(Constants.INPUT_BG);
        button.setForeground(Constants.TEXT_COLOR);
        button.setFont(Constants.APP_FONT);
        button.setBorder(new RoundedBorder(10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(LEFT_ALIGNMENT);

        button.addActionListener(e -> cardLayout.show(contentPanel, card));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Constants.ACCENT_COLOR);
                button.setForeground(Color.BLACK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Constants.INPUT_BG);
                button.setForeground(Constants.TEXT_COLOR);
            }
        });

        return button;
    }

    private JPanel buildContent(AttendanceService service) {
        contentPanel.setBackground(Constants.BG_COLOR);

        contentPanel.add(new AttendanceForm(service), CARD_ATTENDANCE);
        contentPanel.add(new ViewAttendance(service), CARD_VIEW);
        contentPanel.add(createPlaceholderPanel("Reports module coming soon."), CARD_REPORTS);
        contentPanel.add(createPlaceholderPanel("Settings module coming soon."), CARD_SETTINGS);

        return contentPanel;
    }

    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(Constants.BG_COLOR);
        JLabel label = new JLabel(message);
        label.setForeground(Constants.TEXT_COLOR);
        label.setFont(Constants.APP_FONT);
        panel.add(label);
        return panel;
    }

    /**
     * Shows a startup retry dialog for DB errors.
     */
    public static boolean showRetryDialog(Exception ex) {
        int option = JOptionPane.showConfirmDialog(
                null,
                "Database connection failed: " + ex.getMessage() + "\nRetry?",
                "DB Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
        );
        return option == JOptionPane.YES_OPTION;
    }
}
