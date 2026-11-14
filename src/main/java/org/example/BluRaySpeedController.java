
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

public class BluRaySpeedController {

    private static final int IOCTL_CDROM_SET_SPEED =
            0x00024060;

    private static final int BD_1X = 4495;

    private static final int STRUCT_SIZE = 36;

    /**
     * 読み込み速度を変更する
     *
     * speed:
     * 2, 4, 6, 8, 12 = 倍速
     * -1 = ドライブの最適速度
     */
    public static void setStreamingSpeed(
            String driveLetter,
            int speed
    ) throws Exception {

        int readSize;

        if (speed == -1) {
            readSize = 0xFFFF;
        } else {
            if (speed < 1 || speed > 12) {
                throw new IllegalArgumentException(
                        "速度は1～12倍速で指定してください"
                );
            }

            readSize = BD_1X * speed;
        }

        sendStreamingCommand(
                driveLetter,
                readSize,
                false
        );
    }

    /**
     * ドライブの標準速度に戻す
     */
    public static void restoreDefault(
            String driveLetter
    ) throws Exception {

        sendStreamingCommand(
                driveLetter,
                0,
                true
        );
    }

    private static void sendStreamingCommand(
            String driveLetter,
            int readSize,
            boolean restore
    ) throws Exception {

        String path = "\\\\.\\" +
                driveLetter.replace(":", "") + ":";

        WinNT.HANDLE handle =
                Kernel32.INSTANCE.CreateFile(
                        path,
                        0x80000000,
                        3,
                        null,
                        3,
                        0,
                        null
                );

        if (handle == null ||
                Pointer.nativeValue(handle.getPointer()) == -1L) {

            throw new Exception(
                    "ドライブを開けません。Windowsエラー: "
                            + Kernel32.INSTANCE.GetLastError()
            );
        }

        try {

            Memory input = new Memory(STRUCT_SIZE);
            input.clear();

            input.setInt(0, 1);       // CdromSetStreaming

            input.setInt(4, readSize);
            input.setInt(8, 1000);    // 1000ms

            input.setInt(12, 0);      // WriteSize
            input.setInt(16, 0);      // WriteTime

            input.setInt(20, 0);      // StartLba
            input.setInt(24, -1);     // EndLba

            input.setInt(28, 0);      // RotationControl

            input.setByte(32,
                    (byte) (restore ? 1 : 0));

            input.setByte(33, (byte) 0); // SetExact
            input.setByte(34, (byte) 0); // RandomAccess
            input.setByte(35, (byte) 0); // Persistent

            IntByReference returned =
                    new IntByReference();

            boolean success =
                    Kernel32.INSTANCE.DeviceIoControl(
                            handle,
                            IOCTL_CDROM_SET_SPEED,
                            input,
                            STRUCT_SIZE,
                            null,
                            0,
                            returned,
                            null
                    );

            if (!success) {
                int error =
                        Kernel32.INSTANCE.GetLastError();

                throw new Exception(
                        "速度変更失敗。Windowsエラー: " + error
                );
            }

        } finally {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }
}
