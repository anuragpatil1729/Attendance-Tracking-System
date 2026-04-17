# Smart Attendance Management System (Java 17+, Swing, MySQL)

A polished desktop attendance app with two roles:
- **Attendee**: login, self-mark attendance for practical sessions, view own history/percentage.
- **Attendance Officer**: manage sessions/accounts/reports/device logs, lecture manual CRUD, audit trail.

## Stack
- Java 17+
- Swing UI + FlatLaf (Nimbus fallback)
- Aiven MySQL (remote)
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

## Aiven Setup
1. Go to https://aiven.io and sign up for free.
2. Create a new MySQL service (free tier).
3. Once running, click the service → "Connection Information".
4. Copy: Host, Port, User, Password, Database name.
5. Paste into config.properties.
6. (Optional) Download the CA certificate and set db.ssl.ca to its path for full SSL verification.
7. Run schema.sql and procedures.sql:
   ```bash
   mysql --ssl-ca=<ca-path> -h <host> -P <port> -u <user> -p <dbname> < sql/schema.sql
   mysql --ssl-ca=<ca-path> -h <host> -P <port> -u <user> -p <dbname> < sql/procedures.sql
   ```
   If skipping CA cert:
   ```bash
   mysql -h <host> -P <port> -u <user> -p <dbname> < sql/schema.sql
   mysql -h <host> -P <port> -u <user> -p <dbname> < sql/procedures.sql
   ```

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
