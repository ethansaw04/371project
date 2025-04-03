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
    private GameWebSocketServer wsServer;

    public static void main(String[] args) {
        new BluffServer().startServer();
    }

    public String get_ip_addr() {
        return ip_addr;
    }

    public int get_port() {
        return PORT;
    }
    
    // Expose players list for WebSocket message handling
    public List<ClientHandler> getPlayers() {
        return players;
    }

    private void startServer() {
        try {
            ip_addr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Error:" + e.getMessage());
        }
        
        // Start the WebSocket server on port 8082.
        wsServer = new GameWebSocketServer(8082);
        wsServer.start();
        // Let GameWebSocketServer know about this instance.
        GameWebSocketServer.setBluffServer(this);

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

    // Helper to send a turn update to the frontend.
    public void sendTurnUpdate(int playerID) {
        String json = "{\"playerId\": \"player" + playerID + "\", \"type\": \"TURN\"}";
        GameWebSocketServer.broadcastToClients(json);
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
            
            // Send turn update to frontend.
            sendTurnUpdate(player.getPlayerID());
            
            if (!player.requestPlay(roundCard)) {
                needToRemove = player;
                break;
            }
            
            // Wait for bluff call before proceeding to the next player's turn
            if (waitForBluffCall()) {
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
    
        } catch (Exception e) {
            player.sendMessage("Invalid input. Try again.");
            player.requestPlay(roundCard);
        }
    }
    
    private boolean waitForBluffCall() {
        broadcast("Anyone can type 'BLUFF' to call a bluff!");
    
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
            GameWebSocketServer.broadcastToClients("{\"playerId\": \"player" + lastPlayer.getPlayerID() + "\", \"type\": \"DEAD\"}");
            players.remove(lastPlayer);
        } else {
            broadcast("Bluff failed! Player " + accuser.getPlayerID() + " is eliminated!");
            GameWebSocketServer.broadcastToClients("{\"playerId\": \"player" + accuser.getPlayerID() + "\", \"type\": \"DEAD\"}");
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
