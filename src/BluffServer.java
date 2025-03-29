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

    public static void main(String[] args) {
        new BluffServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Bluff Server started. Waiting for players...");

            while (players.size() < 4) {
                Socket socket = serverSocket.accept();
                ClientHandler player = new ClientHandler(socket, this, players.size() + 1);
                players.add(player);
                new Thread(player).start();
                System.out.println("Player " + players.size() + " connected.");
            }

            System.out.println("All players connected. Starting game!");
            shuffleAndDistributeCards();
            playGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shuffleAndDistributeCards() {
        List<String> cards = new ArrayList<>(Arrays.asList(
            "A", "A", "A", "A", "A", "A",
            "K", "K", "K", "K", "K", "K",
            "Q", "Q", "Q", "Q", "Q", "Q",
            "J", "J"
        ));
        Collections.shuffle(cards);

        int playerCount = players.size();
        for (int i = 0; i < cards.size(); i++) {
            players.get(i % playerCount).addCard(cards.get(i));
        }

        for (ClientHandler player : players) {
            player.sendHand();
        }
    }

    private void playGame() {
        while (gameRunning && players.size() > 1) {
            playRound();
        }
        broadcast("Game Over! Winner: Player " + players.get(0).getPlayerID());
    }

    private void playRound() {
        currentRound = (currentRound + 1) % 3;
        String roundCard = switch (currentRound) {
            case 0 -> "A";
            case 1 -> "K";
            case 2 -> "Q";
            default -> "";
        };
    
        System.out.println("New round: " + roundCard + "s");
        broadcast("Round: " + roundCard);
    
        for (ClientHandler player : new ArrayList<>(players)) {
            if (!players.contains(player)) continue;
            player.requestPlay(roundCard);
            
            // Wait for bluff call before proceeding to the next player's turn
            waitForBluffCall();  // This blocks the next player's turn until the bluff phase is resolved.
        }
    }
    
    public void processMove(ClientHandler player, String move, String roundCard) {
        try {
            String[] parts = move.split(" ");
            int declaredCount = Integer.parseInt(parts[0]);
            List<String> playedCards = player.getSelectedCards(declaredCount);
    
            if (playedCards.isEmpty() || playedCards.size() != declaredCount) {
                player.sendMessage("Invalid move! Try again.");
                player.requestPlay(roundCard);
                return;
            }
    
            lastPlayer = player;
            lastPlayedCards = playedCards;
    
            broadcast("Player " + player.getPlayerID() + " played " + declaredCount + " " + roundCard + "(s).");
            player.sendHand();
    
            // waitForBluffCall();  // Wait for a bluff call after the move.
    
        } catch (Exception e) {
            player.sendMessage("Invalid input. Try again.");
            player.requestPlay(roundCard);
        }
    }
    
    private void waitForBluffCall() {
        broadcast("Anyone can type 'BLUFF' to call a bluff!");
    
        // Use a synchronized block to make sure only one player can call bluff at a time.
        synchronized (this) {
            long startTime = System.currentTimeMillis();
            boolean bluffCalled = false;
    
            while (System.currentTimeMillis() - startTime < 5000) {
                for (ClientHandler player : players) {
                    if (player.isBluffCalled()) {
                        resolveBluff(player);
                        bluffCalled = true;
                        return;
                    }
                }
            }
    
            if (!bluffCalled) {
                broadcast("No one called bluff. Round continues.");
            }
        }
    }

    private void resolveBluff(ClientHandler accuser) {
        boolean wasLying = lastPlayedCards.stream().anyMatch(card -> !card.equals("J") && !card.equals(lastPlayer.getRoundCard()));

        if (wasLying) {
            broadcast("Bluff successful! Player " + lastPlayer.getPlayerID() + " was lying and is eliminated!");
            players.remove(lastPlayer);
        } else {
            broadcast("Bluff failed! Player " + accuser.getPlayerID() + " is eliminated!");
            players.remove(accuser);
        }

        if (players.size() == 1) {
            gameRunning = false;
        }
    }

    public void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }
}

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

    public void sendHand() {
        sendMessage("Your hand: " + hand);
    }

    public void requestPlay(String roundCard) {
        this.roundCard = roundCard;
        sendMessage("Your turn! Round is: " + roundCard);
        sendMessage("Your hand: " + hand);
        sendMessage("Enter the number of '" + roundCard + "' cards you are playing:");

        try {
            String move = in.readLine();
            server.processMove(this, move, roundCard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getSelectedCards(int count) {
        List<String> selectedCards = new ArrayList<>();
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
            if (!found) {
                selectedCards.add(hand.remove(0));
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
    }

    public String getRoundCard() {
        return roundCard;
    }
}
