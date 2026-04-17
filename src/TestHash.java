public class TestHash {
    public static void main(String[] args) {
        String password = "admin123";

        String hash = util.PasswordUtil.hash(password);
        System.out.println("HASH: " + hash);

        boolean match = util.PasswordUtil.verify(password, hash);
        System.out.println("MATCH: " + match);
    }
}
