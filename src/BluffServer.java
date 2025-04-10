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

            //wait for all players to connect
            while (players.size() < 4) {
                Socket socket = serverSocket.accept();
                ClientHandler player = new ClientHandler(socket, this, players.size() + 1);
                players.add(player);

                //each player has a thread
                new Thread(player).start();
                System.out.println("Player " + players.size() + " connected.");
            }

            //start
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
            //reset player hands
            player.clearCards();
        }

        for (int i = 0; i < cards.size(); i++) {
            //add cards to their hands
            players.get(i % playerCount).addCard(cards.get(i));
        }

        //adjust player's hands
        for (ClientHandler player : players) {
            player.sendHand();
        }
    }

    private void playGame() {
        ClientHandler winner = null;
        while (gameRunning && players.size() > 1) {
            //start rounds
            shuffleAndDistributeCards();
            winner = playRound();
            if (winner != null) {
                break;
            }
        }
        if (winner != null) {
            //winner being player who emptied their hand
            broadcast("Player " + winner.getPlayerID() + " emptied their hand and has won!");
            broadcast("Game Over! Winner: Player " + winner.getPlayerID());
        } else {
            //winner being last player
            broadcast("Game Over! Winner: Player " + players.get(0).getPlayerID());
        }
    }

    private ClientHandler playRound() {
        //initialize round card
        currentRound = (currentRound + 1) % 3;
        String roundCard = switch (currentRound) {
            case 0 -> "A";
            case 1 -> "K";
            case 2 -> "Q";
            default -> "";
        };
    
        //info
        System.out.println("New round: " + roundCard + "s");
        broadcast("New Round: " + roundCard + "'s round");
        broadcast("There are 6 Aces, 6 Queens, 6 Kings, 2 Jacks, distributed amongst you.");
        broadcast("Jacks can disguise as any card. Find who's lying.");
    
        ClientHandler needToRemove = null;
        //choose random starting player
        int random = (int)(Math.random() * players.size());
        while (true) {
            ClientHandler player = players.get(random);
            broadcast("It is now Player " + player.getPlayerID() + "'s turn");

            if (!players.contains(player)) continue;
            if (!player.requestPlay(roundCard)) {
                needToRemove = player;
                break;
            }

            //info
            broadcast("Player " + player.getPlayerID() + "'s hand now has only " + player.hand.size() + " cards left!");
            
            // Wait for bluff call before proceeding to the next player's turn
            if (waitForBluffCall()) {  // This blocks the next player's turn until the bluff phase is resolved.
                break;
            }

            //if player's hand is empty, they win since no one can call bluff on them anymore and they cant play cards
            if (player.hand.isEmpty()) {
                return player;
            }

            //next turn
            random++;
            random %= players.size();
        }

        //remove player if needed
        if (needToRemove != null) {
            players.remove(needToRemove);
        }

        return null;
    }
    
    public void processMove(ClientHandler player, String move, String roundCard) {
        try {
            String[] parts = move.split(" ");
            int declaredCount = Integer.parseInt(parts[0]);
            int fakeCount = Integer.parseInt(parts[1]);
            List<String> playedCards = player.getSelectedCards(declaredCount, fakeCount);
    
            //if it's an invalid move
            if (playedCards.size() != declaredCount + fakeCount) {
                player.sendMessage("Invalid move! Try again.");
                for (String str : playedCards) {
                    player.addCard(str);
                }
                player.requestPlay(roundCard);
                return;
            }
    
            //store last player so we can check bluff
            lastPlayer = player;
            lastPlayedCards = playedCards;
    
            //information
            broadcast("Player " + player.getPlayerID() + " played " + (declaredCount + fakeCount) + " " + roundCard + "(s).");

            //adjusts player's hand
            player.sendHand();
        } catch (Exception e) {
            //ask them to resend cards
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
    
            //wait for 5 secs
            while (System.currentTimeMillis() - startTime < 5000) {
                for (ClientHandler player : players) {
                    if (player.isBluffCalled()) {
                        resolveBluff(player);
                        bluffCalled = true;
                        //return true if someone called bluff
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
        //check if player was lying i.e. playing a 'fake' card
        boolean wasLying = lastPlayedCards.stream().anyMatch(card -> !card.equals("J") && !card.equals(lastPlayer.getRoundCard()));

        if (wasLying) {
            broadcast("Bluff successful! Player " + lastPlayer.getPlayerID() + " was lying and is eliminated!");

            //remove liar
            players.remove(lastPlayer);
            lastPlayer.sendMessage("You have been eliminated!");
        } else {
            broadcast("Bluff failed! Player " + accuser.getPlayerID() + " is eliminated!");
            //remove accuser
            players.remove(accuser);
            accuser.sendMessage("You have been eliminated!");
        }

        //end game if only one player left
        if (players.size() == 1) {
            gameRunning = false;
        }
    }

    public void broadcast(String message) {
        //send a message to all of the players
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }
}