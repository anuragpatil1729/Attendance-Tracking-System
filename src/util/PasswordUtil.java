package util;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String hash(String plain) {
        try {
            Class<?> bcrypt = Class.forName("org.mindrot.jbcrypt.BCrypt");
            String salt = (String) bcrypt.getMethod("gensalt").invoke(null);
            return (String) bcrypt.getMethod("hashpw", String.class, String.class).invoke(null, plain, salt);
        } catch (Exception e) {
            throw new RuntimeException("jBCrypt library is required in lib/", e);
        }
    }

    public static boolean verify(String plain, String hash) {
        try {
            Class<?> bcrypt = Class.forName("org.mindrot.jbcrypt.BCrypt");
            return (Boolean) bcrypt.getMethod("checkpw", String.class, String.class).invoke(null, plain, hash);
        } catch (Exception e) {
            throw new RuntimeException("jBCrypt library is required in lib/", e);
        }
    }
}
