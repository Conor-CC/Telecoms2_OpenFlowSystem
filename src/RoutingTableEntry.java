import java.util.Random;

public class RoutingTableEntry {

    private String entry;
    private int receivingPort;
    private int portToSendWith;
    private int linkLatency;
    private Random latencyGenerator;



    RoutingTableEntry (String entry, int receivingPort, int portToSendWith) {
        this.entry = entry;
        this.portToSendWith = portToSendWith;
        this.receivingPort = receivingPort;
        this.linkLatency = 0;
        if (entry.contains("R")) {
            latencyGenerator = new Random();
            this.linkLatency = (latencyGenerator.nextInt(10) + 1);
            this.linkLatency += 1;
        }
    }

    public int getLinkLatency() {
        return linkLatency;
    }

    public String getEntry() {
        return entry;
    }

    public int getReceivingPort() {
        return receivingPort;
    }

    public int getPortToSendWith() {
        return portToSendWith;
    }
}
