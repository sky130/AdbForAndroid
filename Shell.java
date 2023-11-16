import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

/**
 * @author Sky233
 * @from https://github.com/sky130/AdbForAndroid
 */
class Shell {

    public static RuntimeHelper helper;
    public static Process process;
    public static Scanner scanner;

    public static void main(String[] args) throws Exception {
        // AdbService.aliasAdbLib(getApplicationInfo().nativeLibraryDir); In the
        // Android , We need to alias libadb.so for use adb
        // process = AdbService.pair("0.0.0.0", "123", "123456");
        // catcher = new RuntimeCatcher(process);
        // catcher.start();
        // final int result = process.waitFor();
        // catcher.stop();
        // System.out.println(AdbService.isPairSuccess(catcher.inputText));

        // process = AdbService.shell(AdbService.ADB_SHELL, "ls", "/sdcard/");
        // catcher = new RuntimeCatcher(process);
        // catcher.start();
        // process.waitFor();
        // catcher.stop();
        // System.out.println(catcher.inputText.toString());
        scanner = new Scanner(System.in);
        process = AdbService.shell(AdbService.CMD);
        helper = new RuntimeHelper(process);
        helper.liveData.observe((value) -> {
            System.out.println(value);
        });
        helper.start();
        while (scanner.hasNextLine() && !helper.isFinish()) {
            // 这里还不够完善
            helper.run(scanner.nextLine());
        }
        scanner.close();

        helper.stop();
        for (String string : helper.inputText) {
            System.out.println(string);
        }
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

class RuntimeHelper {
    public ArrayList<String> inputText = new ArrayList<>();
    public SimpleLiveData<String> liveData = new SimpleLiveData<>();
    private Process mProcess;
    private static final String DEFAULT_CHARSET_NAME = "GBK";
    private BufferedWriter mBufferedWriter;
    private BufferedReader mBufferedReader;
    private boolean canRun = true;
    private boolean isFinish = false;
    private Runnable mRunnable = new Runnable() {
        public void run() {
            try {
                String input;
                while ((input = mBufferedReader.readLine()) != null) {
                    liveData.setValue(input);
                    inputText.add(input);
                    mBufferedWriter.write("");
                    mBufferedWriter.flush();
                    if (!canRun) {
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private Thread mThread = new Thread(mRunnable);
    private Runnable mRunnableRuntime = new Runnable() {
        public void run() {
            try {
                mProcess.waitFor();
            } catch (InterruptedException e) {
            }
            isFinish = true;
        }
    };
    private Thread mThreadRuntime = new Thread(mRunnableRuntime);

    public RuntimeHelper(Process process, String charsetName) throws UnsupportedEncodingException {
        this.mProcess = process;
        this.mBufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream(), charsetName));
        this.mBufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charsetName));
    }

    public RuntimeHelper(Process process) throws UnsupportedEncodingException {
        this.mProcess = process;
        this.mBufferedReader = new BufferedReader(
                new InputStreamReader(mProcess.getInputStream(), DEFAULT_CHARSET_NAME));
        this.mBufferedWriter = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), DEFAULT_CHARSET_NAME));
    }

    public void write(String command) throws IOException {
        this.mBufferedWriter.write(command);
    }

    public void newLine() throws IOException {
        this.mBufferedWriter.newLine();
    }

    public void flush() throws IOException {
        this.mBufferedWriter.flush();
    }

    public boolean isFinish() {
        return this.isFinish;
    }

    public void run(String command) throws IOException {
        this.mBufferedWriter.write(command);
        this.mBufferedWriter.newLine();
        this.mBufferedWriter.flush();
    }

    public void start() {
        this.mThread.start();
        this.mThreadRuntime.start();
    }

    public void stop() {
        canRun = false;
    }

}

interface DataChangeListener<T> {
    void onDataChangeListener(T type);
}

// 稍微学了一点LiveData就来尝试手搓一个了,但是很明显是不够用的,比不上咕噜咕噜官方的
class SimpleLiveData<T> {
    private T value;

    private ArrayList<DataChangeListener<T>> mList = new ArrayList<>();

    public void addOnDataChangerListener(DataChangeListener<T> listener) {
        mList.add(listener);
    }

    public void observe(DataChangeListener<T> listener) {
        mList.add(listener);
    }

    public void setValue(T value) {
        this.value = value;
        for (DataChangeListener<T> listener : mList) {
            listener.onDataChangeListener(value);
        }
    }

    public T getValue() {
        return value;
    }
}
