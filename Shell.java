import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

class Shell {

    public static RuntimeCatcher catcher;

    public static void main(String[] args) throws Exception {
        // AdbPairService.aliasAdbLib(getApplicationInfo().nativeLibraryDir); In the Android , We need to alias libadb.so for use adb
        catcher = new RuntimeCatcher(AdbPairService.pair("0.0.0.0", "123", "123456"));
        catcher.start();
        final int result = process.waitFor();
        catcher.stop();
        System.out.println(isPairSuccess(catcher.inputText));
    }

}

class AdbPairService {
    public static void aliasAdbLib(String libPath){
        Runtime.getRuntime().exec("alias adb=\'"+ libPath +"/libadb.so\'");
    }

    public static Process pair(String ip, String port, String code) {
        return Runtime.getRuntime().exec("adb pair " + ip + ":" + port + " " + code);
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
