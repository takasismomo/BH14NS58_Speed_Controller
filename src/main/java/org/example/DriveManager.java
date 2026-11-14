
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DriveManager {

    private static final String TARGET_MODEL = "BH14NS58";

    public static class DriveInfo {

        private final String driveLetter;
        private final String model;

        public DriveInfo(String driveLetter, String model) {
            this.driveLetter = driveLetter;
            this.model = model;
        }

        public String getDriveLetter() {
            return driveLetter;
        }

        public String getModel() {
            return model;
        }

        @Override
        public String toString() {
            return driveLetter + "  [" + model + "]";
        }
    }

    /**
     * Windowsの光学ドライブをすべて取得
     */
    public static List<DriveInfo> detectDrives()
            throws Exception {

        List<DriveInfo> drives = new ArrayList<>();

        String ps =
                "Get-CimInstance Win32_CDROMDrive | " +
                        "ForEach-Object { " +
                        "$letter = $_.Drive; " +
                        "$model = $_.Name; " +
                        "if ($letter) { " +
                        "Write-Output ($letter + '|' + $model) " +
                        "} }";

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                ps
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream(),
                                     StandardCharsets.UTF_8
                             ))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts = line.split("\\|", 2);

                if (parts.length != 2) {
                    continue;
                }

                String letter = parts[0].trim();
                String model = parts[1].trim();

                if (letter.matches("(?i)[A-Z]:")) {
                    drives.add(
                            new DriveInfo(letter, model)
                    );
                }
            }
        }

        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new Exception("ドライブ検出がタイムアウトしました");
        }

        if (process.exitValue() != 0) {
            throw new Exception("ドライブ情報の取得に失敗しました");
        }

        return drives;
    }

    /**
     * BH14NS58を自動検索
     */
    public static DriveInfo findTargetDrive()
            throws Exception {

        List<DriveInfo> drives = detectDrives();

        for (DriveInfo drive : drives) {

            if (drive.getModel()
                    .toUpperCase()
                    .contains(TARGET_MODEL)) {

                return drive;
            }
        }

        return null;
    }
}
