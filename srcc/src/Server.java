import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
    private static ArrayList<Socket> client_list = new ArrayList<>();  // keeps track of all client ports

    private static ServerSocket server;
    private static Thread listen_thread;

    private static int maxNumPlayers = 4;
    private static int maxNumPotions = 3;
    private static int currentTurn = 0;

    private static ServerListener serverListener = new ServerListener(server, client_list, maxNumPlayers);
    private static ServerSocket start_server(int port_num) throws IOException {
        return new ServerSocket(port_num);
    }



    // GAME LOGIC!!!
    private static void run_game() {
        Scanner scanner = new Scanner(System.in);
        int i = 0;
        while (true) {
            // at the start of each round, give everyone information
            for (int j = 0; j < client_list.size(); j++) {
                Socket sock = client_list.get(i);
                try {
                    OutputStream os = sock.getOutputStream();
                    InputStream is = sock.getInputStream();
                    PrintWriter out = new PrintWriter(os, true);

                    if (i == 0) {
                        out.println("");
                    }

                } catch (IOException e) {
                    System.out.println("Error:" + e.getMessage());
                }
            }


            if (scanner.nextLine().equalsIgnoreCase("exit")) {
                break;
            }

            Socket sock = client_list.get(i);
            try {
                OutputStream os = sock.getOutputStream();
                InputStream is = sock.getInputStream();
                PrintWriter out = new PrintWriter(os, true);

                if (i == 0) {
                    System.out.println("your mom");
                } else {
                    System.out.println("Which cards do you want to play?");
                }

            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }




            i = (i + 1) % client_list.size();
        }
    }

    private static void start_game() {
        System.out.println("Bluff Server started!");
        int size = -1;
        while (client_list.size() < 3) {
            if (size != client_list.size()) {
                System.out.println("Waiting for Sufficient Players... (" + client_list.size() + "/4 players)");
                size = client_list.size();
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.err.println("Error " + e.getMessage());
            }
        }
        System.out.println("Sufficient PLayers! Game Starting...");
        run_game();
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (args.length < 1) {
            System.out.println("Please enter the port number");
            System.out.println("i.e. java Server 8088");
            return;
        }

        try {
            server = start_server(Integer.parseInt(args[0]));
            serverListener = new ServerListener(server, client_list, maxNumPlayers);
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }

        Thread server_thread = new Thread(serverListener);
        server_thread.start();

        // run the game
        start_game();

        try {
            serverListener.shutdown();
            server_thread.join();
        } catch (InterruptedException e) {
            System.err.println("error:" + e.getMessage());
        }
    }
}