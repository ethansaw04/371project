import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BluffServer server;
    private PrintWriter out;
    private BufferedReader in;
    public List<String> hand = new ArrayList<>();
    private int playerID;
    private boolean bluffCalled = false;
    private String roundCard = "";

    public ClientHandler(Socket socket, BluffServer server, int playerID) {
        this.socket = socket;
        this.server = server;
        this.playerID = playerID;
        try {
            //setup sockets
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
        //info
        this.roundCard = roundCard;
        sendMessage("Your turn! Round is: " + roundCard);
        sendMessage("Your hand: " + hand);

        try {
            //get the player's move
            sendMessage("Enter the number of ACTUAL '" + roundCard + "' and FAKE '" + roundCard + "' cards you are playing:");
            String actual = in.readLine();
            
            //parse move
            String[] parts = actual.split(" ");

            String move = parts[1] + " " + parts[2];
            //get server to process move
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
        //grab REAL cards in this for loop, e.g. if it's a K round, grab specified # of Ks
        for (int i = 0; i < count && !hand.isEmpty(); i++) {
            boolean found = false;
            for (int j = 0; j < hand.size(); j++) {
                if (hand.get(j).equals(roundCard)) {
                    selectedCards.add(roundCard);
                    hand.remove(j);
                    found = true;
                    break;
                }
            }
        }

        //grab FAKE cards in this for loop, e.g. if it's a K round, grab specified # of non Ks
        for (int i = 0; i < fakeCount && !hand.isEmpty(); i++) {
            boolean found = false;
            for (int j = 0; j < hand.size(); j++) {
                if (!hand.get(j).equals(roundCard)) {
                    //selectedCards.add(roundCard);
                    selectedCards.add(hand.remove(j));
                    // hand.remove(j);
                    found = true;
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
        return false;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        sendMessage("Welcome to Bluff! Waiting for other players...");
        sendMessage("You are Player " + playerID);
    }

    public String getRoundCard() {
        return roundCard;
    }
}