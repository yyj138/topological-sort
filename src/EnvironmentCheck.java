import javax.swing.JFrame;

/** Checks the shared Java environment without opening a desktop window. */
public final class EnvironmentCheck {
    private EnvironmentCheck() {
    }

    public static void main(String[] args) {
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Swing class: " + JFrame.class.getName());
        System.out.println("Environment check passed. Application features are not implemented yet.");
    }
}
