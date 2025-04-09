import java.io.*;
import java.net.*;
import java.util.*;

public class BluffServer {
    private static final int PORT = 12345;
    private List<ClientHandler> players = new ArrayList<>();
    private int currentRound = -1;
    private boolean gameRunning = true;
    private ClientHandler lastPlayer = null;
    private List<String> lastPlayedCards = new ArrayList<>();
    private int lastActualCount, lastClaimedCount;

    public static void main(String[] args) {
        new BluffServer().startServer();
    }

    private void startServer() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Bluff Server started on " + ip + ":" + PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for players...");
            // accept exactly just 4 players 
            while (players.size() < 4) {
                Socket sock = serverSocket.accept();
                ClientHandler h = new ClientHandler(sock, this, players.size() + 1);
                players.add(h);
                new Thread(h).start();
                broadcast("Player " + h.getPlayerID() + " has joined (" + players.size() + "/4).");
            }
            broadcast("All players connected. Starting game!");
            playGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playGame() {
        // call playRound
        while (gameRunning && players.size() > 1) {
            shuffleAndDistributeCards();
            playRound();
        }
        if (players.size() == 1) {
            broadcast("Game Over! Winner: Player " + players.get(0).getPlayerID());
        } else {
            broadcast("Game Over! No winner.");
        }
    }

    // play 3 rounds 
    private void playRound() {
        currentRound = (currentRound + 1) % 3;
        String rc = getRoundCard();
        broadcast("Round: " + rc);

        // snapshot at start of round to kep track of the whos in whos out yfm 
        List<ClientHandler> roundPlayers = new ArrayList<>(players);

        // each player takes a turn 
        for (ClientHandler p : roundPlayers) {
            if (!players.contains(p)) continue;  // skip if eliminated earlier
            lastPlayer = null;
            p.sendMessage("Your turn!");
            p.sendMessage("Enter the number of ACTUAL " + rc + " and FAKE " + rc + " cards you are playing:");

            // wait for MOVE
            synchronized (this) {
                try { wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            // give everyone 5s to call bluff
            waitForBluffCall();

            if (players.size() <= 1) break;
        }
    }

    private void shuffleAndDistributeCards() {
        List<String> deck = new ArrayList<>();
        Collections.addAll(deck,
            "A","A","A","A","A","A",
            "K","K","K","K","K","K",
            "Q","Q","Q","Q","Q","Q",
            "J","J"
        );
        Collections.shuffle(deck);

        for (ClientHandler p : players) p.clearCards();
        int pc = players.size();
        for (int i = 0; i < deck.size(); i++) {
            players.get(i % pc).addCard(deck.get(i));
        }
        for (ClientHandler p : players) p.sendHand();
    }

    // called by ClientHandler when a MOVE arrives
    public void processMove(ClientHandler player, int actual, int claimed) {
        String rc = getRoundCard();
        lastPlayer = player;
        lastActualCount  = actual;
        lastClaimedCount = claimed;
        lastPlayedCards  = player.getSelectedCards(actual);  // remove only actual cards

        broadcast("Player " + player.getPlayerID() + "  claimed " + rc + "(s)");
        player.sendHand();

        // wake up playRound
        synchronized (this) {
            notify();
        }
    }

    private boolean waitForBluffCall() {
        broadcast("Anyone may type BLUFF in the next 5 seconds to call it!");
        long start = System.currentTimeMillis();
        boolean called = false;

        while (System.currentTimeMillis() - start < 5000) {
            for (ClientHandler p : players) {
                if (p.isBluffCalled()) {
                    called = true;
                    resolveBluff(p);
                    break;
                }
            }
            if (called) break;
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        if (!called) {
            broadcast("No one called bluff. Round continues.");
        }

        // reset bluff flags
        for (ClientHandler p : players) p.setBluffCalled(false);
        return called;
    }

    private void resolveBluff(ClientHandler accuser) {
        boolean wasLying = (lastActualCount != lastClaimedCount);

        if (wasLying) {
            broadcast("Bluff successful! Player " + lastPlayer.getPlayerID() + " was lying and is eliminated!");
            players.remove(lastPlayer);
        } else {
            broadcast("Bluff failed! Player " + accuser.getPlayerID() + " is eliminated!");
            players.remove(accuser);
        }
        if (players.size() == 1) gameRunning = false;
    }

    public String getRoundCard() {
        return switch (currentRound) {
            case 0 -> "A";
            case 1 -> "K";
            case 2 -> "Q";
            default -> "?";
        };
    }

    // broacast a text line to all the connected players 
    private void broadcast(String msg) {
        System.out.println("[SERVER] " + msg);
        for (ClientHandler p : players) p.sendMessage(msg);
    }
}
