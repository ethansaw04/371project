import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BluffServer server;
    private PrintWriter out;
    private BufferedReader in;
    private List<String> hand = new ArrayList<>();
    private int playerID;
    private boolean bluffCalled = false;
    private String roundCard = "";

    public ClientHandler(Socket socket, BluffServer server, int playerID) {
        this.socket = socket;
        this.server = server;
        this.playerID = playerID;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPlayerID() {
        return playerID;
    }

    public void addCard(String card) {
        hand.add(card);
    }

    public void clearCards() {
        hand.clear();
    }

    public void sendHand() {
        sendMessage("Your hand: " + hand);
    }

    public boolean requestPlay(String roundCard) {
        this.roundCard = roundCard;
        sendMessage("Your turn! Round is: " + roundCard);
        sendMessage("Your hand: " + hand);

        try {
            sendMessage("Enter the number of ACTUAL '" + roundCard + "' cards you are playing:");
            String actual = in.readLine();
    
            sendMessage("Enter the number of FAKE '" + roundCard + "' cards you are playing:");
            String fake = in.readLine();
    
            String move = actual + " " + fake;
            server.processMove(this, move, roundCard);
        } catch (SocketException e) {
            System.out.println("Player connection disconnected");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<String> getSelectedCards(int count, int fakeCount) {
        List<String> selectedCards = new ArrayList<>();
        for (int i = 0; i < count && !hand.isEmpty(); i++) {
            for (int j = 0; j < hand.size(); j++) {
                if (hand.get(j).equals(roundCard)) {
                    selectedCards.add(roundCard);
                    hand.remove(j);
                    break;
                }
            }
        }
        for (int i = 0; i < fakeCount && !hand.isEmpty(); i++) {
            for (int j = 0; j < hand.size(); j++) {
                if (!hand.get(j).equals(roundCard)) {
                    selectedCards.add(hand.remove(j));
                    break;
                }
            }
        }
        return selectedCards;
    }

    public boolean isBluffCalled() {
        try {
            if (in.ready()) {
                String response = in.readLine();
                if (response.equalsIgnoreCase("BLUFF")) {
                    bluffCalled = true;
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bluffCalled;
    }
    
    public void setBluffCalled(boolean flag) {
        bluffCalled = flag;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        sendMessage("Welcome to Bluff! Waiting for other players...");

        try {
            while (true) {
                // Wait for a message from the client (like "BLUFF" calls)
                if (in.ready()) {
                    String input = in.readLine();
                    if (input.equalsIgnoreCase("BLUFF")) {
                        bluffCalled = true;
                    }
                }

                // Brief pause to prevent CPU hogging
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            System.out.println("Player " + playerID + " disconnected.");
        }
    }

    public void notifyTurn() {
        requestPlay(roundCard);
    }

    public String getRoundCard() {
        return roundCard;
    }
}
