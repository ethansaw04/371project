import java.io.*;
import java.net.*;
import java.util.*;
public class ServerListener implements Runnable{
    final private ServerSocket server;
    private ArrayList<Socket> client_list;
    final private int maxNumPlayers;
    private int currentConnectedPlayers = 0;
    private volatile boolean running = true;

    public ServerListener(ServerSocket server, ArrayList<Socket> client_list, int maxNumPlayers) {
        this.server = server;
        this.client_list = client_list;
        this.maxNumPlayers = maxNumPlayers;
    }

    public void shutdown() {
        running = false;
        try {
            if (!server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server" + e.getMessage());
        }
    }

    public void run() {
        System.out.println("lsetening:....");

        while (running) {
            if (currentConnectedPlayers >= maxNumPlayers) {
                try {
                    System.out.println("Too many players connected. Please try later");
                    Thread.sleep(100); // Prevent CPU overload
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
                continue;
            }

            try {
                if (!running) break;
                Socket clientSocket = server.accept();
                String ip = clientSocket.getInetAddress().toString();
                int port = clientSocket.getPort();

                try {
                    OutputStream os = clientSocket.getOutputStream();
                    InputStream is = clientSocket.getInputStream();
                    PrintWriter out = new PrintWriter(os, true);
                    out.println("Welcome to Bluff! Please wait for your turn...");

                } catch (IOException e) {
                    System.out.println("Error:" + e.getMessage());
                }

                client_list.add(clientSocket);
                currentConnectedPlayers++;

            } catch (IOException e) {
                if (server.isClosed()) {
                    System.out.println("server clsoed");
                } else {
                    System.err.println("Errorr: " + e.getMessage());
                }
            }
        }
        System.out.println("shitting down...");
    }
}

