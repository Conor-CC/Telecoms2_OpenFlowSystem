import tcdIO.Terminal;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Controller extends Node {

    private Listener listener;
    private String controllerName;
    private String controllerAddress;
    private int controllerSrcPort;
    private Terminal terminal;
    private DatagramPacket packet;
    private DatagramPacket originalPacket;
    private DatagramSocket socket;
    private boolean haveNetworkMap = false;
    private InetSocketAddress dstAddress;
    private RouterConnections[][] networkMap = new RouterConnections[GlobalVars.routerHeight][GlobalVars.routerWidth];
    private String[][] connectionsTables;
    Hashtable<String, ArrayList<String>> storedPaths;
    List<Vertex> nodes;
    List<Edge> edges;



    Controller(String controllerName, String controllerAddress, int controllerSrcPort) {
        //Assign Variables
        nodes = new ArrayList<Vertex>();
        edges = new ArrayList<Edge>();
        this.controllerName = controllerName;
        this.controllerAddress = controllerAddress;
        this.controllerSrcPort = controllerSrcPort;
        this.connectionsTables = new String[(GlobalVars.routerHeight * GlobalVars.routerWidth) * 5][3];

        //Initialise socket
        try {
            this.socket = new DatagramSocket(controllerSrcPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        //Optional console output
        if (GlobalVars.beVerbose) {
            System.out.println("Listener for " + controllerName + " initialised...");
        }

        //Initialise Terminal and print Controller Info
        terminal = new Terminal(controllerName);
        terminal.println(controllerName + ":");
        terminal.println("Source Port: " + controllerSrcPort);
        terminal.println("\nController " + controllerName + " online...");
        terminal.println("________________________________________\n");

        //Initialise paths table
        //Format:   <"R0,C2", { "R0", "R2", "R3", "R5", "R10", ...}>
        //          <Dest, ArrayList of Strings>
        storedPaths = new Hashtable<String, ArrayList<String>>();

        //Initialise Listener
        listener = new Listener(controllerName);
        listener.startListener();

    }

    protected synchronized void toSend() {
    }

    protected synchronized void toReceive() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        try {
            socket.receive(packet);
            terminal.println("Received packet from: " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (new String(packet.getData()).contains("DSTQRY")) {
            if (!haveNetworkMap) {      //GETS CALLED ON FIRST CLIENT TRANSMISSION TO CONTROLLER
                originalPacket = packet;
                terminal.println("CNT generating network map...");
                generateNetworkMap();
                packet = originalPacket;
                terminal.println("Map Generated.\n");
                haveNetworkMap = true;
            }
            if (!pathInList()) {
                terminal.println("GENERATING A PATH...");
                System.out.println(controllerName + ": Generating a path through the network...");
                ArrayList<String> newPath = generatePath(connectionsTables, getPacketSrc(), getPacketDestination());
                System.out.println(controllerName + ": Path found.");
                System.out.println(controllerName + ": New Path...");
                for (int i = 0; i < newPath.size(); i++)
                    System.out.println(newPath.get(i));
                storedPaths.put(getPacketDestination(), newPath); //AAAAAAAHHHHH
                System.out.println("\n");
            }
            if (pathInList()) {
                terminal.println("SENDING NEXT DST");
                sendResponse(packet.getPort());
            }
        }
    }

    private void sendResponse(int routerPort) {

        byte[] payload = null;
        byte[] header = null;
        byte[] buffer = null;
        String s = "NEXTDST=" + getNextStationFromList(getPacketSrc()) + ")";
        payload = s.getBytes();
        header = (controllerName + "|" + getPacketDestination() + "|-").getBytes();
        buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstAddress = new InetSocketAddress("localhost", routerPort);
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            terminal.println(controllerName + ": SENT ROUTER DATA PACKET TO " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateNetworkMap() {
        int accum = 4;
        int r = 0;
        for (int i = 0;i < GlobalVars.routerHeight; i++) {
            for (int j = 0; j < GlobalVars.routerWidth; j++) {
                //Assume R0 exists
                sendRouterDataPoll(ROUTERS_START_PORT + accum, r); //Plus four to hit routers gateway port
                String data = receiveRouterData();
                RouterConnections entry = parseRouterData(data, getPacketSrc());
                if (j > 0) {
                    entry.setLeftTime(networkMap[i][j - 1].getRightTime());
                }
                if (i > 0) {
                    entry.setUpTime(networkMap[i - 1][j].getDownTime());
                }
                networkMap[i][j] = entry;

                Vertex location = new Vertex(entry.getRouterName(), entry.getRouterName());
                if (!checkIfNodeAlreadyPresent(entry.getRouterName())) {
                    nodes.add(location); //Maybe have this in ifs?
                }
                if (entry.getLeftConnection().contains("R") || entry.getLeftConnection().contains("c")) {
                    if (!checkIfNodeAlreadyPresent(entry.getLeftConnection())) {
                        nodes.add(new Vertex(entry.getLeftConnection(), entry.getLeftConnection()));
                    }
                }
                if (entry.getUpConnection().contains("R") || entry.getUpConnection().contains("c")) {
                    if (!checkIfNodeAlreadyPresent(entry.getUpConnection())) {
                        nodes.add(new Vertex(entry.getUpConnection(), entry.getUpConnection()));
                    }

                }
                if (entry.getRightConnection().contains("R") || entry.getRightConnection().contains("c")) {
                    if (!checkIfNodeAlreadyPresent(entry.getRightConnection())) {
                        nodes.add(new Vertex(entry.getRightConnection(), entry.getRightConnection()));
                    }

                }
                if (entry.getDownConnection().contains("R") || entry.getDownConnection().contains("c")) {
                    if (!checkIfNodeAlreadyPresent(entry.getDownConnection())) {
                        nodes.add(new Vertex(entry.getDownConnection(), entry.getDownConnection()));
                    }
                }
                accum += 5;
                r++;
            }
        }

        generateEdges();
        if (GlobalVars.beVerbose) {
            printRTables();
        }
    }

    public void generateEdges() {
        Vertex node;
        int k = 0;
        for (int i = 0; i < GlobalVars.routerHeight; i++) {
            for (int j = 0; j < GlobalVars.routerWidth; j++) {
                RouterConnections router = networkMap[i][j];
                node = new Vertex(router.getRouterName(), router.getRouterName());

                if (router.getLeftConnection().contains("c")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getLeftConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getLeftConnection())), router.getLeftTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getLeftConnection())), node, router.getLeftTime()));
                    }
                }
                if (router.getUpConnection().contains("c")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getUpConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getUpConnection())), router.getUpTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getUpConnection())), node,router.getUpTime()));

                    }
                }
                if (router.getRightConnection().contains("c")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getRightConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getRightConnection())), router.getRightTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getRightConnection())), node, router.getRightTime()));

                    }
                }
                if (router.getDownConnection().contains("c")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getDownConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getDownConnection())), router.getDownTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getDownConnection())), node, router.getDownTime()));
                    }

                }


                if (router.getLeftConnection().contains("R")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getLeftConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getLeftConnection())), router.getLeftTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getLeftConnection())), node, router.getLeftTime()));
                    }
                }
                if (router.getUpConnection().contains("R")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getUpConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getUpConnection())), router.getUpTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getUpConnection())), node,router.getUpTime()));
                    }
                }
                if (router.getRightConnection().contains("R")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getRightConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getRightConnection())), router.getRightTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getRightConnection())), node, router.getRightTime()));
                    }
                }
                if (router.getDownConnection().contains("R")) {
                    if (!checkIfEdgeAlreadyPresent(node, nodes.get(indexThang(router.getDownConnection())))) {
                        edges.add(new Edge("Edge " + k, node, nodes.get(indexThang(router.getDownConnection())), router.getDownTime()));
                        edges.add(new Edge("Edge " + k, nodes.get(indexThang(router.getDownConnection())), node, router.getDownTime()));
                    }

                }


                k++;
            }
        }
    }

    private boolean checkIfNodeAlreadyPresent(String name) {
        for (Vertex node : nodes) {
            if (node.getId().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIfEdgeAlreadyPresent(Vertex num1, Vertex num2) {
        for (Edge edge : edges) {
            if (edge.getSourceVertex().equals(num1) && edge.getDestVertex().equals(num2) ||
                    edge.getSourceVertex().equals(num2) && edge.getDestVertex().equals(num1)) {
                return true;
            }
        }
        return false;
    }

    private int indexThang(String name) {
        if (!nodes.isEmpty()) {
            Vertex node;
            for (int i = 0; i < nodes.size(); i++) {
                node = nodes.get(i);
                if (node.getId().equals(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void printRTables() {
        System.out.println("\nNETMAP:\n" + controllerName);
        for (int i = 0; i < GlobalVars.routerHeight; i++) {
            for (int j = 0; j < GlobalVars.routerWidth; j++) {
                if (networkMap[i][j] != null) {
                    System.out.println(networkMap[i][j].routerConnectionsToString() + "\n");
                }
            }
        }
        String mapPic = "";
        for (int i = 0; i < GlobalVars.routerHeight; i++) {
            for (int j = 0; j < GlobalVars.routerWidth; j++) {
                if (networkMap[i][j] != null) {
                    if (j > 0) {
                        mapPic += "---";
                    }
                    mapPic += networkMap[i][j].getRouterName();
                    if (j < GlobalVars.routerWidth - 1) {
                        mapPic += "---";
                    }
                }
            }
            if (i < GlobalVars.routerHeight - 1) {
                mapPic += "\n";
                String vert = "";
                for (int k = 0; k < GlobalVars.routerWidth; k++) {
                    if (k == 0) {
                        mapPic += "|";
                    } else {
                        mapPic += "       |";
                    }
                }
                mapPic += "\n";
            }
        }
        System.out.println(mapPic + "\n");
    }

    private void sendRouterDataPoll(int routerPort, int routerNum) {
        packet = null;
        byte[] payload = null;
        byte[] header = null;
        byte[] buffer = null;
        String s = "DATAREQ";
        payload = s.getBytes();
        header = (controllerName + "|" + "R" + routerNum + "|-").getBytes();
        buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstAddress = new InetSocketAddress("localhost", routerPort);
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            socket.send(packet);
            terminal.println(controllerName + ": SENT ROUTER DATA PACKET TO " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String receiveRouterData() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        try {
            socket.receive(packet);
            terminal.println("Received router data packet from: " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        String s = new String(packet.getData());
        if (s.contains("QRESP_")) {
            return getTableFromPacket(s);
        }
        return "";
    }

    private String getTableFromPacket(String s) {
        int count = 0;
        int delimCount = 0;
        while (delimCount < 2) {
            if (s.charAt(count) == '|') {
                delimCount++;
            }
            count++;
        }
        int start = count + 7;
        String toReturn = "";
        count = 0;
        int dCount = 0;
        while (dCount < 4) {
            if (s.charAt(start + count) == '\n') {
                dCount++;
            }
            toReturn += s.charAt(start + count);
            count++;
        }
        return toReturn;
    }

    private RouterConnections parseRouterData(String routerData, String name){
        String[] dataEntries = routerData.split("\n");
        RouterConnections entry;
        String left = "";
        String right = "";
        String up = "";
        String down = "";

        int leftNum = 0;
        int rightNum = 0;
        int upNum = 0;
        int downNum = 0;

        int leftTime = 0;
        int rightTime = 0;
        int upTime = 0;
        int downTime = 0;



        if (dataEntries[0].contains("R")) {

            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[0].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }

            left = dataEntries[0].substring(0,count - 1);
            leftNum = Integer.parseInt(dataEntries[0].substring(1, count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[0].charAt(countTwo) == '|') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[0].charAt(countThree) == '|') {
                    delimCountThree++;
                }
                countThree++;
            }
            leftTime = Integer.parseInt(dataEntries[0].substring(countTwo, countThree - 1));
        }
        if (dataEntries[1].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[1].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }

            up = dataEntries[1].substring(0,count - 1);
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[1].charAt(countTwo) == '|') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[1].charAt(countThree) == '|') {
                    delimCountThree++;
                }
                countThree++;
            }
            upTime = Integer.parseInt(dataEntries[1].substring(countTwo, countThree - 1));
        }
        if (dataEntries[2].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[2].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }

            right = dataEntries[2].substring(0,count - 1);
            rightNum = Integer.parseInt(dataEntries[2].substring(1,count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[2].charAt(countTwo) == '|') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[2].charAt(countThree) == '|') {
                    delimCountThree++;
                }
                countThree++;
            }
            rightTime = Integer.parseInt(dataEntries[2].substring(countTwo, countThree - 1));
        }
        if (dataEntries[3].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[3].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }

            down = dataEntries[3].substring(0,count - 1);
            downNum = Integer.parseInt(dataEntries[3].substring(1,count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[3].charAt(countTwo) == '|') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[3].charAt(countThree) == '|') {
                    delimCountThree++;
                }
                countThree++;
            }
            downTime = Integer.parseInt(dataEntries[3].substring(countTwo, countThree - 1));
        }
        if (dataEntries[0].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[0].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }
            left = dataEntries[0].substring(0,count - 1);
        }
        if (dataEntries[1].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[1].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }
            up = dataEntries[1].substring(0,count - 1);
            upNum = Integer.parseInt(dataEntries[1].substring(1,count - 1));
        }
        if (dataEntries[2].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[2].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }
            right = dataEntries[2].substring(0,count - 1);
        }
        if (dataEntries[3].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[3].charAt(count) == '|') {
                    delimCount++;
                }
                count++;
            }
            down = dataEntries[3].substring(0,count - 1);
        }
        entry = new RouterConnections(name, left, up, right, down, leftTime, upTime, rightTime, downTime, leftNum, rightNum, upNum, downNum);
        return entry;
    }

    private String getPacketSrc() {

        byte[] packetData = packet.getData();
        String data = new String(packetData);
        int count = 0;
        int firstDelim = 0;
        int secondDelim = 0;
        int delimCount = 0;
        while (delimCount < 1) {
            if (data.charAt(count) == '|') {
                delimCount++;
            }
            count++;
        }
        String srcString = data.substring(0, count - 1);
        return srcString;
    }

    private ArrayList<String> generatePath(String[][] connectionsTables, String currentRouter, String dstClient) {
        int c = 0;
        int currentRouterIndex = -1;
        int dstClientIndex = -1;
        for (Vertex node : nodes) {
            if (node.getId().equals(currentRouter)) {
                currentRouterIndex = c;
            }
            if (node.getId().equals(dstClient)) {
                dstClientIndex = c;
            }
            c++;
        }
        Graph graph = new Graph(nodes, edges);
        DjikstraAlgo dijkstra = new DjikstraAlgo(graph);
        dijkstra.executeAlgorithm(nodes.get(currentRouterIndex), dstClient);
        ArrayList<Vertex> path = dijkstra.generatePath(nodes.get(dstClientIndex));
        ArrayList<String> toRet = new ArrayList<String>();
        for (int i = 0; i < path.size(); i++) {
            toRet.add(path.get(i).getId());
        }
        return toRet;
    }

    private boolean pathInList() {
        for (int i = 0; i < storedPaths.size(); i++) {
            if (storedPaths.containsKey(getPacketDestination())) {
                return true;
            }
        }
        return false;
    }

    private String getPacketDestination() {
        String dst = "";
        byte[] packetData = packet.getData();
        String data = new String(packetData);
        int count = 0;
        int firstDelim = 0;
        int secondDelim = 0;
        int delimCount = 0;
        while (delimCount < 2) {
            if (data.charAt(count) == '|') {
                delimCount++;
                if (firstDelim == 0) {
                    firstDelim = count;
                }
                else if (secondDelim == 0) {
                    secondDelim = count;
                }
            }
            count++;
        }
        String dstString = data.substring((firstDelim + 1), (secondDelim));
        if (GlobalVars.showRouterTerminals) {
            terminal.println(controllerName + ": Destination client is " + dstString);
        }
        return dstString;
    }

    private String getNextStationFromList(String currentStation) {
        String lmao = getPacketDestination();
        ArrayList<String> boiii = null;
        if (storedPaths.containsKey(lmao)) {
            boiii = storedPaths.get(lmao);
        }
        int index = 0;
        for (int i = 0; i < boiii.size(); i++) {
            if (boiii.get(i).equals(currentStation)) {
                index = i;
            }
        }
        String nextStation = boiii.get(index + 1);
        return nextStation;
    }

}
