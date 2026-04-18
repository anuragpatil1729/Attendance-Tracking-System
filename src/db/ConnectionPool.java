package db;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import config.ConfigLoader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ConnectionPool {
    private static final int MAX_POOL_SIZE = 5;
    private static final BlockingQueue<Connection> IDLE = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static final Object LOCK = new Object();

    private static MysqlConnectionPoolDataSource dataSource;
    private static int createdConnections;

    private ConnectionPool() {
    }

    public static Connection getConnection() throws SQLException {
        Connection physical = borrowPhysicalConnection();
        return wrap(physical);
    }

    private static Connection borrowPhysicalConnection() throws SQLException {
        synchronized (LOCK) {
            if (dataSource == null) {
                dataSource = buildDataSource();
            }

            Connection idle = IDLE.poll();
            if (isUsable(idle)) {
                return idle;
            }

            if (createdConnections < MAX_POOL_SIZE) {
                createdConnections++;
                try {
                    return dataSource.getConnection();
                } catch (SQLException ex) {
                    createdConnections--;
                    throw ex;
                }
            }

            while (true) {
                try {
                    LOCK.wait();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for pooled connection", ie);
                }

                Connection next = IDLE.poll();
                if (isUsable(next)) {
                    return next;
                }
            }
        }
    }

    private static void releasePhysicalConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        synchronized (LOCK) {
            try {
                if (!isUsable(conn) || !IDLE.offer(conn)) {
                    closeQuietly(conn);
                    createdConnections = Math.max(0, createdConnections - 1);
                }
            } finally {
                LOCK.notifyAll();
            }
        }
    }

    private static Connection wrap(Connection physical) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean released;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("close".equals(name)) {
                    if (!released) {
                        released = true;
                        releasePhysicalConnection(physical);
                    }
                    return null;
                }
                if ("isClosed".equals(name)) {
                    return released || physical.isClosed();
                }
                if ("unwrap".equals(name) && args != null && args.length == 1) {
                    Class<?> type = (Class<?>) args[0];
                    if (type.isInstance(proxy)) {
                        return proxy;
                    }
                    if (type.isInstance(physical)) {
                        return physical;
                    }
                }
                if ("isWrapperFor".equals(name) && args != null && args.length == 1) {
                    Class<?> type = (Class<?>) args[0];
                    return type.isInstance(proxy) || type.isInstance(physical);
                }
                if (released) {
                    throw new SQLException("Connection is closed");
                }
                return method.invoke(physical, args);
            }
        };

        return (Connection) Proxy.newProxyInstance(
                ConnectionPool.class.getClassLoader(),
                new Class[]{Connection.class},
                handler);
    }

    private static MysqlConnectionPoolDataSource buildDataSource() {
        String host = ConfigLoader.get("db.host");
        int port = ConfigLoader.getInt("db.port", 3306);
        String dbName = ConfigLoader.get("db.name");
        String user = ConfigLoader.get("db.user");
        String pass = ConfigLoader.get("db.password");
        String caPath = ConfigLoader.get("db.ssl.ca");

        boolean hasCaPath = caPath != null && !caPath.isBlank();

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?sslMode=" + (hasCaPath ? "VERIFY_CA" : "REQUIRED")
                + (hasCaPath ? "&sslCa=" + caPath : "")
                + "&enabledTLSProtocols=TLSv1.2"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&serverTimezone=UTC"
                + "&connectTimeout=10000"
                + "&socketTimeout=30000";

        MysqlConnectionPoolDataSource ds = new MysqlConnectionPoolDataSource();
        ds.setUrl(url);
        ds.setUser(user);
        ds.setPassword(pass);

        return ds;
    }

    private static boolean isUsable(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            return !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void closeQuietly(Connection conn) {
        try {
            conn.close();
        } catch (SQLException ignored) {
        }
    }
}
