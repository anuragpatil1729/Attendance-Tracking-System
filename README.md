# Attendance Management System (Java Swing + MySQL)

A Java 17+ desktop application for managing student attendance using Swing and MySQL.

## Features

- Dark themed Swing UI (Catppuccin-inspired palette)
- Mark attendance by date/class with inline status editing
- View/search attendance records by date range, class, and name
- Export current report table to CSV
- DAO + service architecture with validation and stored procedures

## Project Layout

```
attendance-system/
├── lib/
├── sql/
│   ├── schema.sql
│   └── procedures.sql
├── src/
│   ├── db/
│   ├── main/
│   ├── model/
│   ├── service/
│   ├── ui/
│   └── util/
└── README.md
```

## Setup

1. Install Java 17+ and MySQL 8.x.
2. Place `mysql-connector-j-8.x.jar` in `lib/`.
3. Update DB password in `src/util/Constants.java`.

## Database Initialization

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/procedures.sql
```

## Compile

```bash
find src -name "*.java" > sources.txt
javac -cp "lib/*" -d out @sources.txt
```

## Run

```bash
java -cp "out:lib/*" main.App
```

Windows:

```bash
java -cp "out;lib/*" main.App
```

## Usage Flow

1. Launch application.
2. Open **Mark Attendance**, pick date/class, click **Load Students**.
3. Set status/remarks and click **Save All**.
4. Open **View Records**, filter date range/class/name and click **Search**.
5. Click **Export CSV** to create `attendance_export_<timestamp>.csv`.

## Notes

- DB exceptions are displayed as error dialogs.
- Validation issues are shown in panel status labels.
- Startup validates DB connectivity and offers retry.
