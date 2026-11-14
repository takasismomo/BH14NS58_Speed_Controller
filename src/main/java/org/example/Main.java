
import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main extends JFrame {

    private final JComboBox<String> driveBox;
    private final JComboBox<String> speedBox;
    private final JLabel statusLabel;
    private final JLabel speedLabel;
    private final JTextArea history;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public Main() {

        setTitle("BH14NS58 Speed Controller");
        setSize(540, 460);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout(10, 10));
        main.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JPanel controls = new JPanel(
                new GridLayout(0, 2, 10, 10)
        );

        driveBox = new JComboBox<>(
                new String[]{"D:", "E:", "F:", "G:"}
        );

        JButton detectButton =
                new JButton("ドライブ自動検出");

        speedBox = new JComboBox<>(
                new String[]{
                        "2倍速",
                        "4倍速",
                        "6倍速",
                        "8倍速",
                        "12倍速",
                        "最大速度"
                }
        );

        JButton applyButton =
                new JButton("速度変更");

        JButton restoreButton =
                new JButton("標準速度に戻す");

        statusLabel = new JLabel("待機中");
        speedLabel = new JLabel("未設定");

        controls.add(new JLabel("ドライブ"));
        controls.add(detectButton);
        controls.add(new JPanel());
        controls.add(driveBox);

        controls.add(new JLabel("読み込み速度"));
        controls.add(speedBox);

        controls.add(applyButton);
        controls.add(restoreButton);

        controls.add(new JLabel("設定速度"));
        controls.add(speedLabel);

        controls.add(new JLabel("状態"));
        controls.add(statusLabel);


        history = new JTextArea();
        history.setEditable(false);
        history.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 12)
        );

        JScrollPane scroll =
                new JScrollPane(history);

        main.add(controls, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);

        add(main);

        applyButton.addActionListener(e ->
                changeSpeed()
        );

        restoreButton.addActionListener(e ->
                restoreSpeed()
        );
        detectButton.addActionListener(e ->
                detectDrive()
        );
        addHistory("アプリケーション起動");
    }

    private String getDrive() {
        return (String) driveBox.getSelectedItem();
    }

    private int getSpeed() {

        String selected =
                (String) speedBox.getSelectedItem();

        if ("最大速度".equals(selected)) {
            return -1;
        }

        return Integer.parseInt(
                selected.replace("倍速", "")
        );
    }

    private void changeSpeed() {

        int speed = getSpeed();

        String drive = getDrive();

        executeAsync(() -> {

            BluRaySpeedController.setStreamingSpeed(
                    drive,
                    speed
            );

            String label = speed == -1
                    ? "最大速度"
                    : speed + "倍速";

            SwingUtilities.invokeLater(() -> {

                speedLabel.setText(label);
                statusLabel.setText("設定要求成功");

                addHistory(
                        drive + " 読み込み速度変更: " + label
                );
            });
        });
    }

    private void restoreSpeed() {

        String drive = getDrive();

        executeAsync(() -> {

            BluRaySpeedController.restoreDefault(
                    drive
            );

            SwingUtilities.invokeLater(() -> {

                speedLabel.setText("ドライブ標準");
                statusLabel.setText("標準速度に復帰");

                addHistory(
                        drive + " 標準速度に復帰"
                );
            });
        });
    }


    private void detectDrive() {

        statusLabel.setText("ドライブ検索中...");

        executeAsync(() -> {

            DriveManager.DriveInfo drive =
                    DriveManager.findTargetDrive();

            SwingUtilities.invokeLater(() -> {

                if (drive == null) {

                    statusLabel.setText(
                            "BH14NS58が見つかりません"
                    );

                    addHistory(
                            "対象ドライブが見つかりません"
                    );

                    return;
                }

                driveBox.removeAllItems();

                driveBox.addItem(
                        drive.getDriveLetter()
                );

                driveBox.setSelectedIndex(0);

                statusLabel.setText("ドライブ検出成功");

                addHistory(
                        "検出: " + drive.toString()
                );
            });
        });
    }

    private void executeAsync(ThrowingRunnable operation) {

        statusLabel.setText("処理中...");

        new SwingWorker<Void, Void>() {

            private Exception error;

            @Override
            protected Void doInBackground() {

                try {
                    operation.run();

                } catch (Exception e) {
                    error = e;
                }

                return null;
            }

            @Override
            protected void done() {

                if (error != null) {

                    statusLabel.setText("エラー");

                    addHistory(
                            "ERROR: " + error.getMessage()
                    );

                    JOptionPane.showMessageDialog(
                            Main.this,
                            error.getMessage(),
                            "速度変更エラー",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        }.execute();
    }


    private void addHistory(String message) {

        String time =
                LocalTime.now().format(formatter);

        history.append(
                "[" + time + "] " + message + "\n"
        );

        history.setCaretPosition(
                history.getDocument().getLength()
        );
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
