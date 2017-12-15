public class RouterConnections {

    private String routerName;
    private String leftConnection;
    private String upConnection;
    private String rightConnection;
    private String downConnection;
    private int leftNum;
    private int rightNum;
    private int upNum;
    private int downNum;
    private int leftTime;
    private int upTime;
    private int rightTime;

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    private int downTime;

    public int getLeftNum() {
        return leftNum;
    }

    public int getRightNum() {
        return rightNum;
    }

    public int getUpNum() {
        return upNum;
    }

    public int getDownNum() {
        return downNum;
    }

    RouterConnections (String routerName, String leftConnection, String upConnection, String rightConnection,
                       String downConnection, int leftTime, int upTime, int rightTime, int downTime, int leftNum,
                       int rightNum, int upNum, int downNum) {

        this.routerName = routerName;


        this.leftConnection = leftConnection;
        this.upConnection = upConnection;
        this.rightConnection = rightConnection;
        this.downConnection = downConnection;

        this.leftNum = leftNum;
        this.rightNum = rightNum;
        this.upNum = upNum;
        this.downNum = downNum;


        this.leftTime = leftTime;
        this.upTime = upTime;
        this.rightTime = rightTime;
        this.downTime = downTime;
    }

    public String getRouterName() {
        return routerName;
    }

    public String getLeftConnection() {
        return leftConnection;
    }

    public String getUpConnection() {
        return upConnection;
    }

    public String getRightConnection() {
        return rightConnection;
    }

    public String getDownConnection() {
        return downConnection;
    }

    public int getLeftTime() {

        return leftTime;
    }

    public void setLeftTime(int leftTime) {
        this.leftTime = leftTime;
    }

    public void setUpTime(int upTime) {
        this.upTime = upTime;
    }

    public void setRightTime(int rightTime) {
        this.rightTime = rightTime;
    }

    public void setDownTime(int downTime) {
        this.downTime = downTime;
    }

    public int getUpTime() {

        return upTime;
    }

    public int getRightTime() {
        return rightTime;
    }

    public int getDownTime() {
        return downTime;
    }

    public String routerConnectionsToString() {
        return (routerName + ": \nLeft Connection = " + leftConnection + " " + leftTime + "\nupConnection = " + upConnection + " " + upTime +
                "\nrightConnection = " + rightConnection + " " + rightTime + "\ndownConnection = " + downConnection + " " + downTime);

    }
}
