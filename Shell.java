import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

class Shell {

    public static RuntimeCatcher catcher;
    public static Process process;

    public static void main(String[] args) throws Exception {
        // AdbService.aliasAdbLib(getApplicationInfo().nativeLibraryDir); In the
        // Android , We need to alias libadb.so for use adb
        // process = AdbService.pair("0.0.0.0", "123", "123456");
        // catcher = new RuntimeCatcher(process);
        // catcher.start();
        // final int result = process.waitFor();
        // catcher.stop();
        // System.out.println(AdbService.isPairSuccess(catcher.inputText));

        process = AdbService.shell(AdbService.ADB_SHELL, "ls", "/sdcard/");
        catcher = new RuntimeCatcher(process);
        catcher.start();
        process.waitFor();
        catcher.stop();
        System.out.println(catcher.inputText.toString());
    }

}

class AdbService {
    public static final String CMD = "cmd";
    public static final String SH = "sh";
    public static final String ADB = "adb";
    public static final String ADB_SHELL = "adb_shell";
    public static final String SU = "su";

    public static Process shell(String mode, String... args) throws IOException {
        String[] cache = { mode };
        if (args.length == 0)
            return Runtime.getRuntime().exec(cache);
        if (mode.equals(ADB_SHELL)) {
            cache = new String[2 + args.length];
            cache[0] = ADB;
            cache[1] = "shell";
            System.arraycopy(args, 0, cache, 2, cache.length - 2);
        } else {
            cache = new String[1 + args.length];
            cache[0] = mode;
            System.arraycopy(args, 0, cache, 1, cache.length - 1);
        }
        return Runtime.getRuntime().exec(cache);
    }

    public static void aliasAdbLib(String libPath) throws IOException, InterruptedException {
        String[] commands = { "alias adb=\'", libPath, "/libadb.so\'" };
        Runtime.getRuntime().exec(commands).waitFor();
    }

    public static Process pair(String ip, String port, String code) throws IOException {
        String[] commands = { "adb", "pair", ip + ":" + port, code };
        return Runtime.getRuntime().exec(commands);
    }

    public static boolean isPairSuccess(ArrayList<String> list) {
        for (String text : list) {
            if (text.startsWith("Successfully paired to ")) {
                return true;
            }
        }
        return false;
    }
}

class RuntimeCatcher {
    public ArrayList<String> inputText = new ArrayList<>();
    private Process mProcess;
    private BufferedReader mBufferedReader;
    private boolean canRun = true;
    private Runnable mRunnable = new Runnable() {
        public void run() {
            try {
                String input;
                while ((input = mBufferedReader.readLine()) != null) {
                    if (!canRun) {
                        break;
                    }
                    inputText.add(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private Thread mThread = new Thread(mRunnable);

    public RuntimeCatcher(Process process) {
        this.mProcess = process;
        this.mBufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
    }

    public void start() {
        this.mThread.start();
    }

    public void stop() {
        canRun = false;
    }

}
