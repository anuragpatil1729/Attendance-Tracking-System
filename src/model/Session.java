package model;

import java.time.LocalDateTime;

public class Session {
    private int id;
    private String name;
    private String subject;
    private String sessionType;
    private int lockDurationMinutes;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private boolean open;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public int getLockDurationMinutes() { return lockDurationMinutes; }
    public void setLockDurationMinutes(int lockDurationMinutes) { this.lockDurationMinutes = lockDurationMinutes; }
    public LocalDateTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalDateTime openTime) { this.openTime = openTime; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
}
