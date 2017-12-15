import tcdIO.Terminal;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class Router extends Node implements SystemConstants {
    private ArrayList<RoutingTableEntry> routingTable = new ArrayList<RoutingTableEntry>();
    private ArrayList<DstTableEntry> dstTableEntries = new ArrayList<DstTableEntry>();
    private int[] ports;
    private int[] dstPorts;
    private DatagramSocket[] sockets = new DatagramSocket[ROUTER_PORTS];
    private DatagramPacket packet;
    private DatagramPacket queryPacket;
    private boolean needToSendQuery;
    private String payload;


    private String name;
    private String routerAddress;
    private InetSocketAddress dstAddress;
    private Terminal terminal;

    Router (String name, String routerAddress, int[] ports, int[] dstPorts, ArrayList<RoutingTableEntry> routingTable) {
        //Assignment of Variables
        this.routingTable = routingTable;
        this.name = name;
        this.routerAddress = routerAddress;
        this.ports = ports;
        this.dstPorts = dstPorts;
        this.needToSendQuery = false;
        this.payload = "";
        //Set up of all sockets present in Router
        try {
            for (int i = 0; i < sockets.length; i++) {
                sockets[i] = new DatagramSocket(ports[i]);
                sockets[i].setSoTimeout(10);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //Optional console output
        if (GlobalVars.beVerbose) {
            System.out.println("\nListener for " + name + " initialised...\n");
            printRoutingTable();            //PRINTS ROUTING TABLE TO CONSOLE. ONLY WHEN IN verbose MODE.
        }
        //If showTerminal is true, print routing table to console, initialise terminal and use it to print router info
        if (GlobalVars.showRouterTerminals) {
            terminal = new Terminal(name);
            terminal.println(name + "\nSource Ports: ");
            printSocketPorts();
            terminal.println("\nDstPorts");
            printDstPorts();
            terminal.println("\nRouter " + name + " online...");
            terminal.println("________________________________________\n");
        }
        //Initialise listener
        listener = new Listener(name);
        listener.startListener();
    }

    //toSend() only called when router needs to query controller
    @Override
    protected synchronized void toSend() {
        if (needToSendQuery) {
            payload = getPacketPayload();
            String packetDestination = "";
            packetDestination = getPacketDestination();
            if (!dstTableContainsDst(packetDestination)) {
                //Send Query to Controller
                assembleQueryPacket(packetDestination);
                try {
                    sockets[4].send(queryPacket);
                    if (GlobalVars.showRouterTerminals) {
                        terminal.println("No route for Client " + packetDestination + ". Querying Controller...\n");
                        terminal.println(name + ": SENT QUERY TO " + queryPacket.getSocketAddress());
                    }
                } catch (IOException e) {}
            }
            needToSendQuery = false;
        }
    }

    @Override
    protected synchronized void toReceive() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        for (int i = 0; i < sockets.length; i++) {
            trySocket(sockets[i]);
        }
    }

    private int findIndexInRoutingTable(String entry) {
        for (int i = 0; i < routingTable.size(); i++) {
            if (routingTable.get(i).getEntry().equals(entry)) {
                return i;
            }
        }
        return 0;
    }

    private synchronized void trySocket(DatagramSocket sock) {
        String packetDestination = "";
        try {
            sock.receive(packet);
            packetDestination = getPacketDestination();
            if (GlobalVars.showRouterTerminals) {
                terminal.println("Received packet from: " + packet.getSocketAddress());
            }
            if (dstTableContainsDst(packetDestination)) {
                int index = 0;
                for (int i = 0; i < dstTableEntries.size(); i++) {
                    if (dstTableEntries.get(i).getDstName().equals(packetDestination)) {
                        index = i;
                    }
                }
                DstTableEntry entry = dstTableEntries.get(index);
                sendPacketOn(entry.getDstName(), getPacketPayload(), index, routingTable.get(entry.getSocketNum()).getPortToSendWith());
                //terminal.println("\nHave destination. Sending packet on to " + entry.getNextRouter() + "\n");

            }
            else if ((new String(packet.getData())).contains("DATAREQ")) {
                sendControllerRouterData();
            }
            else if ((new String(packet.getData())).contains("NEXTDST")) { //WHERE NEXTDST IS STORED INTO DST TABLE
                String nextDest = parseNextDest();
                String finalDest = getPacketDestination();
                if (GlobalVars.showRouterTerminals) {
                    terminal.println("\nNEXTDST from CNT reading: " + nextDest);
                }
                for (int i = 0; i < routingTable.size(); i++) {
                    if (routingTable.get(i).getEntry().equals(nextDest) && !dstTableContainsDst(nextDest)) {
                        dstTableEntries.add(new DstTableEntry(packetDestination, nextDest, findIndexInRoutingTable(nextDest)));
                        sendPacketOn(finalDest, payload, i, routingTable.get(i).getPortToSendWith());
                        packet = null;
                        i = routingTable.size();
                    }
                }
            }
            else {
                needToSendQuery = true;
            }
        } catch (IOException e) {}
    }

    private void sendPacketOn(String finalDest, String packetPayload, int portToSendThrough, int portToSendTo) {
        if (GlobalVars.beVerbose) {
            printRoutingTable();
        }
        DatagramPacket newPacket = null;
        byte[] payload = null;
        byte[] header = null;
        byte[] buffer = null;
        String s = packetPayload + "-";
        payload = s.getBytes();
        header = (name + "|" + finalDest + "|-").getBytes();
        buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstAddress = new InetSocketAddress(routerAddress, portToSendTo);
        newPacket = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            sockets[portToSendThrough].send(newPacket);
            if (GlobalVars.showRouterTerminals) {
                terminal.println(name + ": SENT TO " + newPacket.getSocketAddress());
            }
        } catch (IOException e) {}
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

    private void printDstPorts() {
        for (int i = 0; i < ports.length; i++) {
            if (i == 0)
                terminal.println("L="+String.valueOf(dstPorts[i]));
            else if (i == 1)
                terminal.println("T="+String.valueOf(dstPorts[i]));
            else if (i == 2)
                terminal.println("R="+String.valueOf(dstPorts[i]));
            else if (i == 3)
                terminal.println("D="+String.valueOf(dstPorts[i]));
            else if (i == 4)
                terminal.println("CNT="+String.valueOf(dstPorts[i]));
        }
    }

    private void printSocketPorts() {
        for (int i = 0; i < ports.length; i++) {
            if (i == 0)
                terminal.println("L="+String.valueOf(sockets[i].getLocalPort()));
            else if (i == 1)
                terminal.println("T="+String.valueOf(sockets[i].getLocalPort()));
            else if (i == 2)
                terminal.println("R="+String.valueOf(sockets[i].getLocalPort()));
            else if (i == 3)
                terminal.println("D="+String.valueOf(sockets[i].getLocalPort()));
            else if (i == 4)
                terminal.println("CNT="+String.valueOf(sockets[i].getLocalPort()));
        }
    }

    private void printRoutingTable() {
        System.out.println("________________________________________\n" + "Routing table for " + name + ":");
        for (int i = 0; i < routingTable.size(); i++) {
            System.out.println(routingTable.get(i).getEntry() + "|" + routingTable.get(i).getReceivingPort() + "|"
             + routingTable.get(i).getPortToSendWith() + "|" + routingTable.get(i).getLinkLatency() + "|");
        }
        System.out.println("\n");
    }

    private String generateRoutingTableString () {
        String table = "";
        for (int i = 0; i < routingTable.size(); i++) {
            table += (routingTable.get(i).getEntry() + "|" + routingTable.get(i).getReceivingPort() + "|"
                    + routingTable.get(i).getPortToSendWith() + "|" + routingTable.get(i).getLinkLatency() + "|\n");
        }
        return (table);
    }

    private String getPacketDestination() {

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
        String dstString = data.substring(firstDelim + 1, secondDelim);
        if (GlobalVars.showRouterTerminals) {
            terminal.println(name + ": Destination client is " + dstString);
        }
        return dstString;
    }

    private boolean dstTableContainsDst(String toCompare) {
        if (dstTableEntries.size() == 0) {
            return false;
        }
        DstTableEntry entry;
        for (int i = 0; i < dstTableEntries.size(); i++) {
            entry = dstTableEntries.get(i);
            if (entry.getDstName().contains(toCompare)) {
                return true;
            }
        }
        return false;
    }

    private void assembleQueryPacket(String packetDstToQuery) {
        queryPacket = null;
        byte[] payload = null;
        byte[] header = null;
        byte[] buffer = null;
        String s = "DSTQRY";
        payload = s.getBytes();
        header = (name + "|" + packetDstToQuery + "|-").getBytes();
        buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstAddress = new InetSocketAddress(routerAddress, dstPorts[4]);
        queryPacket = new DatagramPacket(buffer, buffer.length, dstAddress);
    }

    private void sendControllerRouterData() {
        queryPacket = null;
        byte[] payload = null;
        byte[] header = null;
        byte[] buffer = null;
        String s = "QRESP_" + generateRoutingTableString();
        payload = s.getBytes();
        header = (name + "|" + "CNT" + "|-").getBytes();
        buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstAddress = new InetSocketAddress(routerAddress, dstPorts[4]);
        queryPacket = new DatagramPacket(buffer, buffer.length, dstAddress);
        try {
            sockets[4].send(queryPacket);
            if (GlobalVars.showRouterTerminals) {
                terminal.println("Sending CNT routing table for..." + name + "\n");
                terminal.println(name + ": SENT ROUTER DATA TO " + queryPacket.getSocketAddress());
            }
        } catch (IOException e) {}
    }

    private String parseNextDest() {
        byte[] data = packet.getData();
        String pData = new String(data);
        int count = 0;
        while (pData.charAt(count) != '=') {
            count++;
        }
        int countTwo = 0;
        while (pData.charAt(countTwo) != ')') {
            countTwo++;
        }
        return pData.substring(count+1, countTwo);
    }
}
