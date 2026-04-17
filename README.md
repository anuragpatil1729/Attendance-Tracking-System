# Smart Attendance Management System (Java 17+, Swing, MySQL)

A polished desktop attendance app with two roles:
- **Attendee**: login, self-mark attendance for practical sessions, view own history/percentage.
- **Attendance Officer**: manage sessions/accounts/reports/device logs, lecture manual CRUD, audit trail.

## Stack
- Java 17+
- Swing UI + FlatLaf (Nimbus fallback)
- Clever Cloud MySQL (remote)
- SQLite fallback (`attendance_local.db`) for offline marking

## Setup
1. Install Java 17+.
2. Put required jars in `lib/`:
   - mysql-connector-j
   - sqlite-jdbc
   - flatlaf
   - jbcrypt
3. Copy config template:
   ```bash
   cp config.properties.example config.properties
   ```
4. Fill DB credentials in `config.properties`.
5. Initialize database:
   ```bash
   mysql -h <host> -P <port> -u <user> -p < sql/schema.sql
   mysql -h <host> -P <port> -u <user> -p < sql/procedures.sql
   ```

## Clever Cloud notes
- Use your Clever Cloud MySQL host/port/database/user/password.
- Ensure incoming IP/network permissions allow your machine.
- Keep `config.properties` local only (already gitignored).

## Compile
```bash
find src -name "*.java" > sources.txt
javac -cp "lib/*" -d out @sources.txt
```

## Run
Linux/macOS:
```bash
java -cp "out:lib/*" main.App
```

Windows:
```bash
java -cp "out;lib/*" main.App
```

## Implemented behavior highlights
- Session types: practical vs lecture.
- Practical: anti-proxy lock (IP + device fingerprint + lock window).
- Lecture: officer-driven manual CRUD + audit logging.
- Offline writes to SQLite with `sync_status='pending'` and UUID `local_id`.
- Background sync every 30 seconds via `ScheduledExecutorService`.
- DB errors are surfaced as UI toasts/dialogs.
