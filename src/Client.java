import tcdIO.Terminal;
import java.io.IOError;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Client {


    private String name;
    private String clientAddress;
    private String destinationIP;
    private int clientSrcPort;
    private int clientDstPort;
    private int clientSrcRecvPort;
    private InetSocketAddress dstAddress;
    protected Terminal terminal;



    Client(String name, String clientAddress, String destinationIP, int clientSrcPort, int clientDstPort, int clientSrcRecvPort) {
        //Assign variables
        this.name = name;
        this.clientAddress = clientAddress;
        this.destinationIP = destinationIP;
        this.clientSrcPort = clientSrcPort;
        this.clientDstPort = clientDstPort;
        this.clientSrcRecvPort = clientSrcRecvPort;

        terminal = new Terminal(name);

        Receiver receiver = new Receiver();
        Sender sender = new Sender();

    }

    private class Sender {

        private DatagramPacket packet;
        private senderListener senListener;
        private DatagramSocket sendSocket;

        Sender() {
            dstAddress = new InetSocketAddress(destinationIP, clientDstPort);
            try {
                sendSocket = new DatagramSocket(clientSrcPort);
                senListener = new senderListener("Send Listener");
                senListener.startListener();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        protected synchronized void toSend() {
            packet = null;
            byte[] payload = null;
            byte[] header = null;
            byte[] buffer = null;

            terminal.println("\nEnter in payload to deliver...");
            String s = terminal.readString() + "-";
            terminal.println("Enter in destination in the following format:\n" +
                    "Cx where 'C' is a client on the network and \n" +
                    "'x'is the positive integer ID of the client. \n" +
                    "Examples: C3, C12, C0 etc.\n");
            String dest = "";


            dest = terminal.readString();

            payload = s.getBytes();
            header = (name + "|" + dest + "|-").getBytes();
            buffer = new byte[header.length + payload.length];
            System.arraycopy(header, 0, buffer, 0, header.length);
            System.arraycopy(payload, 0, buffer, header.length, payload.length);
            packet = new DatagramPacket(buffer, buffer.length, dstAddress);
            try {
                sendSocket.send(packet);
                terminal.println(name + ": SENT PACKET TO " + packet.getSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private class senderListener extends Thread implements Runnable {

            private String name = "";
            senderListener(String name) {
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
                    //loop through send
                    while (true) {
                        toSend();
                    }

            }

        }

    }

    private class Receiver {

        private recvListener recListener;
        private DatagramSocket recvSocket;
        private DatagramPacket packet;

        Receiver () {
            try {
                recvSocket = new DatagramSocket(clientSrcRecvPort);
                terminal.println(String.valueOf(clientSrcRecvPort));
                recListener = new recvListener("Receive Listener");
                recListener.startListener();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        protected synchronized void toReceive() {
            packet = new DatagramPacket(new byte[Node.PACKET_SIZE], Node.PACKET_SIZE);
            try {
                recvSocket.receive(packet);
                terminal.println("\n" + name + ": packet received from " + packet.getAddress() + " " + packet.getPort());
                terminal.println(name + ": packet reads " + getPacketPayload() + "\n");
                terminal.println("\nEnter in payload to deliver...");
            } catch (IOException e) {

            } catch (NullPointerException e1) {

            }
        }

        private String getPacketPayload() {
            int countOne = 0;
            byte[] data = packet.getData();
            while (data[countOne] != '-') {
                countOne++;
            }
            int countTwo = countOne + 1;
            while (data[countTwo] != '-') {
                countTwo++;
            }
            String dataStr = new String(packet.getData());
            return dataStr.substring(countOne + 1, countTwo);
        }

        private class recvListener extends Thread implements Runnable {

            private String name = "";
            recvListener(String name) {
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
                    //loop through receive
                    while (true) {
                        toReceive();
                    }
            }
        }
    }



}
