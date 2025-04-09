import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BluffServer server;
    private final PrintWriter out;
    private final BufferedReader in;
    private final List<String> hand = new ArrayList<>();
    private final int playerID;
    private volatile boolean bluffCalled = false;

    public ClientHandler(Socket socket, BluffServer server, int playerID) throws IOException {
        this.socket = socket;
        this.server = server;
        this.playerID = playerID;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public int getPlayerID() { return playerID; }
    public void clearCards()   { hand.clear(); }
    public void addCard(String c) { hand.add(c); }
    public void sendHand()      { sendMessage("Your hand: " + hand); }

    public boolean isBluffCalled() { return bluffCalled; }
    public void setBluffCalled(boolean b) { bluffCalled = b; }

    /** Remove exactly `actual` cards of the round card from the hand */
    public List<String> getSelectedCards(int actual) {
        List<String> sel = new ArrayList<>();
        String rc = server.getRoundCard();
        for (int i = 0; i < actual; i++) {
            if (hand.remove(rc)) sel.add(rc);
        }
        return sel;
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            sendMessage("Connected as Player " + playerID);
            sendMessage("Welcome to Bluff! Waiting for other players...");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MOVE")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 3) {
                        int actual  = Integer.parseInt(parts[1]);
                        int claimed = Integer.parseInt(parts[2]);
                        server.processMove(this, actual, claimed);
                    } else {
                        sendMessage("Invalid MOVE. Use: MOVE <actual> <claimed>");
                    }
                }
                else if (line.equalsIgnoreCase("BLUFF")) {
                    bluffCalled = true;
                }
                else {
                    sendMessage("Unknown command: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Player " + playerID + " disconnected.");
        }
    }
}
