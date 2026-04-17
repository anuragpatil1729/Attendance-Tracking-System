package ui.officer;

import model.User;
import service.SessionService;
import ui.components.RoundedBorder;
import ui.components.ToastNotification;
import util.Constants;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

public class SessionManagerPanel extends JPanel {
    private final SessionService sessionService = new SessionService();
    private final User user;
    private final JTextField name = new JTextField();
    private final JTextField subject = new JTextField();
    private final JSpinner lockSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 240, 1));
    private final JRadioButton practical = new JRadioButton("Practical", true);
    private final JRadioButton lecture = new JRadioButton("Lecture");

    public SessionManagerPanel(User user) {
        this.user = user;
        setLayout(new GridBagLayout());
        setBackground(Constants.BG);
        JPanel card = new JPanel(new GridLayout(0, 2, 8, 8));
        card.setBackground(Constants.SIDEBAR);
        card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(16), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        style(name); style(subject); style(lockSpinner);
        practical.setOpaque(false); practical.setForeground(Constants.TEXT);
        lecture.setOpaque(false); lecture.setForeground(Constants.TEXT);
        ButtonGroup g = new ButtonGroup(); g.add(practical); g.add(lecture);
        practical.addActionListener(e -> lockSpinner.setEnabled(true));
        lecture.addActionListener(e -> lockSpinner.setEnabled(false));

        JButton create = new JButton("Create & Open");
        create.setBorder(new RoundedBorder(10));
        create.addActionListener(e -> createSession());

        card.add(label("Session Name")); card.add(name);
        card.add(label("Subject")); card.add(subject);
        card.add(label("Session Type"));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT)); radios.setOpaque(false); radios.add(practical); radios.add(lecture); card.add(radios);
        card.add(label("Lock Duration (min)")); card.add(lockSpinner);
        card.add(new JLabel()); card.add(create);
        add(card);
    }

    private void createSession() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                sessionService.createAndOpen(name.getText().trim(), subject.getText().trim(), practical.isSelected() ? "practical" : "lecture", user.getId(), (Integer) lockSpinner.getValue(), LocalDateTime.now());
                return null;
            }
            @Override protected void done() {
                try { get(); ToastNotification.showSuccess(SessionManagerPanel.this, "Session opened"); }
                catch (Exception ex) { ToastNotification.showError(SessionManagerPanel.this, ex.getCause()==null?ex.getMessage():ex.getCause().getMessage()); }
            }
        }.execute();
    }

    private JLabel label(String t){ JLabel l=new JLabel(t); l.setForeground(Constants.TEXT); return l; }
    private void style(JComponent c){ c.setBorder(new RoundedBorder(10)); c.setForeground(Constants.TEXT); c.setBackground(Constants.INPUT); }
}
