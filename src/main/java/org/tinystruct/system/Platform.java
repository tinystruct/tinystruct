package org.tinystruct.system;

public class Platform {
    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nux") || os.contains("nix") || os.contains("aix");
    }

    public static boolean isJnaAvailable() {
        try {
            // Check for a core JNA class (e.g., Native)
            Class.forName("com.sun.jna.Native");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
