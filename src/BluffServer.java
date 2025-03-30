import java.io.*;
import java.net.*;
import java.util.*;

public class BluffServer {
    private static final int PORT = 12345;
    private static String ip_addr;
    private List<ClientHandler> players = new ArrayList<>();
    private int currentRound = -1;
    private boolean gameRunning = true;
    private ClientHandler lastPlayer = null;
    private List<String> lastPlayedCards = new ArrayList<>();

    public static void main(String[] args) {
        new BluffServer().startServer();
    }

    public String get_ip_addr() {
        return ip_addr;
    }

    public int get_port() {
        return PORT;
    }

    private void startServer() {
        try {
            ip_addr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Error:" + e.getMessage());
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Bluff Server started! \nJoin at ip address: " + ip_addr + "\nPort: " + PORT);
            System.out.println("Waiting for players...");

            while (players.size() < 4) {
                Socket socket = serverSocket.accept();
                ClientHandler player = new ClientHandler(socket, this, players.size() + 1);
                players.add(player);

                new Thread(player).start();
                System.out.println("Player " + players.size() + " connected.");
            }

            System.out.println("All players connected. Starting game!");
            playGame();

        } catch (IOException e) {
            System.out.println("Error Starting the Server");
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
        for (ClientHandler player : players) {
            player.clearCards();
        }

        for (int i = 0; i < cards.size(); i++) {
            players.get(i % playerCount).addCard(cards.get(i));
        }

        for (ClientHandler player : players) {
            player.sendHand();
        }
    }

    private void playGame() {
        while (gameRunning && players.size() > 1) {
            shuffleAndDistributeCards();
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
    
        ClientHandler needToRemove = null;
        for (ClientHandler player : new ArrayList<>(players)) {
            if (!players.contains(player)) continue;
            if (!player.requestPlay(roundCard)) {
                needToRemove = player;
                break;
            }
            
            // Wait for bluff call before proceeding to the next player's turn
            if (waitForBluffCall()) {  // This blocks the next player's turn until the bluff phase is resolved.
                break;
            }
        }
        if (needToRemove != null) {
            players.remove(needToRemove);
        }
    }
    
    public void processMove(ClientHandler player, String move, String roundCard) {
        try {
            String[] parts = move.split(" ");
            int declaredCount = Integer.parseInt(parts[0]);
            int fakeCount = Integer.parseInt(parts[1]);
            List<String> playedCards = player.getSelectedCards(declaredCount, fakeCount);
    
            if (playedCards.isEmpty() || playedCards.size() != declaredCount + fakeCount) {
                player.sendMessage("Invalid move! Try again.");
                for (String str : playedCards) {
                    player.addCard(str);
                }
                player.requestPlay(roundCard);
                return;
            }
    
            lastPlayer = player;
            lastPlayedCards = playedCards;
    
            broadcast("Player " + player.getPlayerID() + " played " + (declaredCount + fakeCount) + " " + roundCard + "(s).");
            player.sendHand();
    
            // waitForBluffCall();  // Wait for a bluff call after the move.
    
        } catch (Exception e) {
            player.sendMessage("Invalid input. Try again.");
            player.requestPlay(roundCard);
        }
    }
    
    private boolean waitForBluffCall() {
        broadcast("Anyone can type 'BLUFF' to call a bluff!");
    
        // Use a synchronized block (Mutex) to make sure only one player can call bluff at a time.
        synchronized (this) {
            long startTime = System.currentTimeMillis();
            boolean bluffCalled = false;
    
            while (System.currentTimeMillis() - startTime < 5000) {
                for (ClientHandler player : players) {
                    if (player.isBluffCalled()) {
                        resolveBluff(player);
                        bluffCalled = true;
                        return true;
                    }
                }
            }
    
            if (!bluffCalled) {
                broadcast("No one called bluff. Round continues.");
                return false;
            }
        }
        
        return false;
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

