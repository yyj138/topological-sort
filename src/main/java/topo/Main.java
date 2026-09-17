package topo;

import topo.controller.MainController;
import topo.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// 程序入口：启动主窗口并装配控制器
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new MainController(frame);
            frame.setVisible(true);
        });
    }
}
