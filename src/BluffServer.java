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

    public void notifyPlayerTurn(int playerID) {
        for (ClientHandler player : players) {
            if (player.getPlayerID() == playerID) {
                String roundCard = getRoundCard();
                player.requestPlay(roundCard);
                break;
            }
        }
    }

    // Broadcast a TURN message to clients
    public void sendTurnUpdate(int playerID) {
        String json = "{\"playerId\": \"player" + playerID + "\", \"type\": \"TURN\"}";
        GameWebSocketServer.broadcastToClients(json);

        notifyPlayerTurn(playerID);
    }
    
    // Broadcast a full game state message.
    public void broadcastGameState(String message, String currentRound) {
        String json = "{\"type\":\"GAME_STATE\", \"message\":\"" + message + "\", \"currentRound\":\"" + currentRound + "\"}";
        GameWebSocketServer.broadcastToClients(json);
    }
    
    // After a move is processed, determine and broadcast next player's turn.
    private void advanceTurn() {
        if (players.size() > 0 && lastPlayer != null) {
            int currentIndex = players.indexOf(lastPlayer);
            int nextIndex = (currentIndex + 1) % players.size();
            int nextPlayerID = players.get(nextIndex).getPlayerID();

            // Check if everyone has played once (round is complete)
            if (nextIndex == 0) {  // Back to the first player
                completeRound();
                return;
            }

            sendTurnUpdate(nextPlayerID);
            broadcastGameState("It's Player " + nextPlayerID + "'s turn", getRoundCard());
        }
    }
    
    // Helper to retrieve current round card for game state message.
    public String getRoundCard() {
        return switch (currentRound) {
            case 0 -> "A";
            case 1 -> "K";
            case 2 -> "Q";
            default -> "";
        };
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

    public void completeRound() {
        synchronized (this) {
            notify(); // Notify that the round is complete
        }
    }

    private void playRound() {
        currentRound = (currentRound + 1) % 3;
        String roundCard = getRoundCard();
    
        System.out.println("New round: " + roundCard + "s");
        broadcast("Round: " + roundCard);
    
        // For simplicity, assume that turns advance via WebSocket MOVE messages.
        // The initial turn can be set to the first player:
//        if (!players.isEmpty()) {
//            sendTurnUpdate(players.get(0).getPlayerID());
//            broadcastGameState("It's Player " + players.get(0).getPlayerID() + "'s turn", roundCard);
//        }

        if (!players.isEmpty()) {
            sendTurnUpdate(players.get(0).getPlayerID());
            broadcastGameState("It's Player " + players.get(0).getPlayerID() + "'s turn", roundCard);

            // Wait for this round to complete before starting a new one
            synchronized (this) {
                try {
                    wait(); // Will be notified when round is complete
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Further turn advancement will be handled in processMove().
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
            
            // After processing the move, advance the turn.
            advanceTurn();
    
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

        completeRound();
    }

    public void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }

}
