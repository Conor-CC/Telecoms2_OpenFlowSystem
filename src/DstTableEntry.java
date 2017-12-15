public class DstTableEntry {

    private String dstName;
    private String nextRouter;
    private int socketNum;

    DstTableEntry(String dstName, String nextRouter, int socketNum) {
        this.dstName = dstName;
        this.nextRouter = nextRouter;
        this.socketNum = socketNum;
    }

    public String getDstName() {
        return dstName;
    }

    public int getSocketNum() {
        return socketNum;
    }

    public String getNextRouter() {
        return nextRouter;
    }
}
