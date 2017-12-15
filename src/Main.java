import tcdIO.Terminal;

import java.util.ArrayList;

public class Main implements SystemConstants {

    public static void main(String[] args) {


        GlobalVars.routerHeight = DEFAULT_ROUTER_COUNT_X;
        GlobalVars.routerWidth = DEFAULT_ROUTER_COUNT_Y;
        int routerPortNum = ROUTERS_START_PORT;
        int clientCount = 0;
        int[] routerPorts = new int[ROUTER_PORTS];
        int[] routerDstPorts = new int[ROUTER_PORTS];
        ArrayList<RoutingTableEntry> routingTableEntries = new ArrayList<RoutingTableEntry>();
        Terminal terminal;



        if (args.length >= 1) {
            for (int k = 0; k < args.length; k++) {
                switch (args[k]) {
                    case "-s":
                        GlobalVars.showRouterTerminals = true;
                        break;
                    case "--showRouterTerminals":
                        GlobalVars.showRouterTerminals = true;
                        break;
                    case "-v":
                        GlobalVars.beVerbose = true;
                        break;
                    case "--verbose":
                        GlobalVars.beVerbose = true;
                        break;
                    case "-r":
                        GlobalVars.displayRoutingTables = true;
                        break;
                    case "--showRoutingTables":
                        GlobalVars.displayRoutingTables = true;
                        break;
                    case "-h":
                        System.out.println("\nUSAGE: Run program with <-s> or <--showRouterTerminals> for a view of all router " +
                                "terminals\nas well as the clients and controller. Use <-v> or <--verbose> " +
                                "for verbose console output.\nUse <-r> or <--showRoutingTables> to print router routing tables" +
                                "to the console.\n  Or, program is run with just client input windows and the controller window by default.\n\n");
                        break;
                    case "--help":
                        System.out.println("\nUSAGE: Run program with <-s> or <--showRouterTerminals> for a view of all router " +
                                "terminals\nas well as the clients and controller. Use <-v> or <--verbose> " +
                                "for verbose console output.\nUse <-r> or <--showRoutingTables> to print router routing tables" +
                                "to the console.\n  Or, program is run with just client input windows and the controller window by default.\n\n");

                }
            }
        }
        else if (args.length == 0) {
            terminal = new Terminal("Setup");
            terminal.println("Show router information windows? \nCan slow down PC with large \nnumbers of routers (Y/N)");
            String ans = terminal.readString();
            if (ans.contains("Y") || ans.contains("y")) {
                GlobalVars.showRouterTerminals = true;
            }
            else if (ans.contains("N") || ans.contains("n")) {
                GlobalVars.showRouterTerminals = false;
            }
            else {
                GlobalVars.showRouterTerminals = false;
            }
            terminal.println("Be verbose with console output? (Y/N)");
            String verb = terminal.readString();
            if (verb.contains("Y") || verb.contains("y")) {
                GlobalVars.beVerbose = true;
            }
            else if (verb.contains("N") || verb.contains("n")) {
                GlobalVars.beVerbose = false;
            }
            else {
                GlobalVars.beVerbose = false;
            }
            terminal.println("Enter in the width of the router grid: ");
            GlobalVars.routerWidth = terminal.readInt();
            terminal.println("Enter in the height of the router grid: ");
            GlobalVars.routerHeight = terminal.readInt();
        }

        Router routers[][] = new Router[GlobalVars.routerHeight][GlobalVars.routerWidth];
        Controller controller = new Controller("CNT", "localhost", CONTROLLER_SRC_PORT);
        Client clientOne;
        Client clientTwo;
        Client clientThree;
        Client clientFour;
        Client clientFive;


        int routerCount = 0;
        for (int i = 0; i < GlobalVars.routerHeight; i++) {
            for (int j = 0; j < GlobalVars.routerWidth; j++) {
                for (int k = 0; k < ROUTER_PORTS; k++) {
                    routerPorts[k] = routerPortNum;
                    if (k == ROUTER_PORTS - 1) {
                        /**
                         *
                         * The next section of code determines the destinationPorts of each router.
                         * The idea is that source ports on the edge of the grid are nullified.
                         * Otherwise, the destination port of a routers right port would be equivalent
                         * to the next router's left port. A router's bottom port corresponds to a
                         * top port and so on...
                         *
                         *                   
                         *
                         *           |   |   |   |   |
                         *        C1-O---O---O---O---O-
                         *           |   |   |   |   |
                         *          -O---O---O---O---O-
                         *           |   |   |   |   |
                         *          -O---O---O---O---O-
                         *           |   |   |   |   |
                         *        C2-O---O---O---O---O-C3
                         *           |   |   |   |   |
                         *                   
                         *
                         *  N.B. In diagram, C represents a client and O a router. An unconnected '-' or '|'
                         *      represents an unused port.
                         *
                         * **/

                        routerDstPorts = determineDestPorts(routerPorts, routerDstPorts);
                        if (j == 0) {
                            routerDstPorts[0] = 0;

                            //put a client on top left corner of grid
                            if (i == 0) {
                                clientOne = new Client("c1", "localhost", "localhost", CLIENTS_START_PORT + 50, routerPorts[0], CLIENTS_START_PORT);
                                routerDstPorts[0] = CLIENTS_START_PORT;
                                clientCount++;
                            }
                            if (i == GlobalVars.routerHeight - 1 && GlobalVars.routerHeight > 1) {
                                clientTwo = new Client("c2", "localhost", "localhost", CLIENTS_START_PORT + 51, routerPorts[0], CLIENTS_START_PORT + 1);
                                routerDstPorts[0] = CLIENTS_START_PORT + 1;
                                clientCount++;
                            }
                        }
                        if (j == GlobalVars.routerWidth - 1) {
                            routerDstPorts[2] = 0;

                            //put a client on bottom right
                            if (i == GlobalVars.routerHeight - 1 && GlobalVars.routerHeight > 1) {
                                clientThree = new Client("c3", "localhost", "localhost", CLIENTS_START_PORT + 52, routerPorts[2], CLIENTS_START_PORT + 2);
                                routerDstPorts[2] = CLIENTS_START_PORT  + 2;
                                clientCount++;
                            }
                        }
                        if (i == 0) {
                            routerDstPorts[1] = 0;
                        }
                        if (i == GlobalVars.routerHeight - 1) {
                            routerDstPorts[3] = 0;
                        }
                        String name = "R" + routerCount;
                        routers[i][j] = new Router(name, "localhost",
                                routerPorts, routerDstPorts, createRoutingTable(routingTableEntries, routerPorts,
                                routerDstPorts, routerCount, clientCount));
                    }
                    routerPortNum++;
                }
                routerCount++;
            }
        }
        System.out.println("________________________________________________________________________________\n" +
                "\nLaunched successfully with " + clientCount + " Clients, " + routerCount + " Routers and a Controller");
        System.out.println("\nRunning System...");
    }


    public static int[] determineDestPorts(int[] routerPorts, int[] routerDstPorts) {
        routerDstPorts[0] = routerPorts[0] - 3;                                                     //LEFT PORT
        routerDstPorts[1] = ((routerPorts[1] - (5 * GlobalVars.routerWidth)) -2) + 4;                           //TOP PORT
        routerDstPorts[2] = routerPorts[2] + 3;                                                     //RIGHT PORT
        routerDstPorts[3] = ((routerPorts[3] + (5 * GlobalVars.routerWidth)) - 2);                          //BOTTOM PORT
        routerDstPorts[4] = CONTROLLER_SRC_PORT;                                                    //CONTROLLER PORT
        return routerDstPorts;
    }

    /**
     *
     * Lots of code to give the routers preconfigured
     * routing tables.
     */
    public static ArrayList<RoutingTableEntry> createRoutingTable(ArrayList<RoutingTableEntry> routingTableEntries,
                                                                  int[] routerSrcPorts, int[] routerDstPorts, int routerCount,
                                                                  int clientCount) {
        routingTableEntries = new ArrayList<RoutingTableEntry>();
        for (int i = 0; i < ROUTER_PORTS; i++) {

            if (routerDstPorts[i] != 0 && routerDstPorts[i] >= ROUTERS_START_PORT) {
                int routerNum = 0;
                if (i == 0) {
                    routerNum = routerCount - 1;
                }
                else if (i == 1) {
                    routerNum = routerCount - GlobalVars.routerWidth;
                }
                else if (i == 2) {
                    routerNum = routerCount + 1;
                }
                else if (i == 3) {
                    routerNum = routerCount + GlobalVars.routerWidth;
                }
                routingTableEntries.add(new RoutingTableEntry(("R" + routerNum), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] >= CLIENTS_START_PORT && routerDstPorts[i] < CLIENTS_START_PORT + 5) {
                routingTableEntries.add(new RoutingTableEntry(("c" + clientCount), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] == CONTROLLER_SRC_PORT) {
                routingTableEntries.add(new RoutingTableEntry(("CNT"), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] == 0) {
                routingTableEntries.add(new RoutingTableEntry(("0"), routerSrcPorts[i], routerDstPorts[i]));
            }
        }
        return routingTableEntries;
    }
}
