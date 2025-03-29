import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    static int maxNumPlayers = 4;
    static int maxNumPotions = 3;

    static int currentConnectedPlayers = 0;
    static int currentTurn = 0;

    static boolean[][] potionsAvailable; //true if potion available to drink, false if not
    static boolean[][] potionDeath; //true if this potion will kill the player, 1->1 correspondence with available potions

    private static final int PORT = 12345;
    private static List<Socket> clients = new ArrayList<>();
    // private static int currentTurn = 0;

	public static void main(String[] args){
        potionsAvailable = new boolean[maxNumPlayers][maxNumPotions];
        for (int i = 0; i < maxNumPlayers; i++) {
            for (int j = 0; j < maxNumPotions; j++) {
                potionsAvailable[i][j] = true;
            }
        }

        potionDeath = new boolean[maxNumPlayers][maxNumPotions];

        for (int i = 0; i < maxNumPlayers; i++) {
            int deathNum = (int) (Math.random() * maxNumPotions);
            for (int j = 0; j < maxNumPotions; j++) {
                if (j == deathNum) {
                    potionDeath[i][j] = true;
                } else {
                    potionDeath[i][j] = false;
                }
            }
        }

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Waiting for players...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (clients) {
                    clients.add(clientSocket);
                }
                System.out.println("New player connected! Total players: " + clients.size());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            int clientId;
            synchronized (clients) {
                clientId = clients.indexOf(clientSocket);
            }
            out.println("Welcome Player " + clientId + "! Waiting for your turn...");

            while (true) {
                synchronized (Server.class) {
                    while (clientId != currentTurn) {
                        Server.class.wait(); // Wait for turn
                    }
                }

                out.println("It's your turn! Type a message:");
                String message = "";
                try {
                    message = in.readLine();
                } catch (SocketException e) {
                    synchronized (clients) {
                        clients.remove(clientSocket);
                    }
                    System.out.println("Player " + clientId + " left.");
                    out.println("You left the game.");
                    clientSocket.close();
                    nextTurn();
                    break;
                }

                if (message == null || message.equalsIgnoreCase("exit")) {
                    synchronized (clients) {
                        clients.remove(clientSocket);
                    }
                    System.out.println("Player " + clientId + " left.");
                    out.println("You left the game.");
                    clientSocket.close();
                    nextTurn();
                    break;
                }

                System.out.println("Player " + clientId + ": " + message);
                broadcast("Player " + clientId + ": " + message);
                nextTurn();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void nextTurn() {
        if (!clients.isEmpty()) {
            currentTurn = (currentTurn + 1) % clients.size();
            Server.class.notifyAll();
        }
    }

    private static void broadcast(String message) {
        synchronized (clients) {
            for (Socket client : clients) {
                try {
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                    out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}