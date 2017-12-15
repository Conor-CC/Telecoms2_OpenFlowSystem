import java.io.IOError;

public abstract class Node implements SystemConstants {

    Listener listener;

    protected synchronized void toSend() {}
    protected synchronized void toReceive() {}

    public class Listener extends Thread implements Runnable {

        private String name = "";
        Listener(String name) {
            this.name = name;
        }


        public void startListener() {
            try {
                new Thread(this).start();
            } catch (IOError exception) {
                exception.printStackTrace();
            }
        }

        public void run() {
            while (true) {
                toSend();
                toReceive();
            }
        }
    }
}
