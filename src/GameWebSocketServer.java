import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

public class GameWebSocketServer extends WebSocketServer {

    private static final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());
    private static BluffServer bluffServer;
    private static final Map<WebSocket, Integer> connectionToPlayer = new HashMap<>();
    private static int nextPlayerId = 1;

    public GameWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }
    
    public static void setBluffServer(BluffServer server) {
        bluffServer = server;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("New WebSocket connection: " + conn.getRemoteSocketAddress());
        // If 4 clients are connected, auto-start the game by broadcasting a GAME_STATE message.
        if (clients.size() == 4) {
            String roundCard = "A"; // starting round card; adjust as needed
            String startMessage = "Game started! It's Player 1's turn.";
            JSONObject json = new JSONObject();
            json.put("type", "GAME_STATE");
            json.put("message", startMessage);
            json.put("currentRound", roundCard);
            broadcastToClients(json.toString());
            System.out.println("Auto-start game: " + json.toString());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("Closed WebSocket: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from client: " + message);
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            if ("REGISTER".equals(type)) {
                // Assign a unique player ID to this connection.
                int assignedId = nextPlayerId++;
                connectionToPlayer.put(conn, assignedId);
                JSONObject response = new JSONObject();
                response.put("type", "REGISTERED");
                response.put("playerId", assignedId);
                conn.send(response.toString());
                System.out.println("WebSocket registered for Player " + assignedId);
            } else if ("MOVE".equals(type)) {
                int playerId = json.getInt("playerId");
                int actual = json.getInt("actual");
                int fake = json.getInt("fake");
                String move = actual + " " + fake;
                for (var player : bluffServer.getPlayers()) {
                    if (player.getPlayerID() == playerId) {
                        bluffServer.processMove(player, move, bluffServer.getRoundCard());
                        break;
                    }
                }
            } else if ("BLUFF".equals(type)) {
                int playerId = json.getInt("playerId");
                for (var player : bluffServer.getPlayers()) {
                    if (player.getPlayerID() == playerId) {
                        player.setBluffCalled(true);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started!");
    }

    public static void broadcastToClients(String message) {
        synchronized (clients) {
            for (WebSocket client : clients) {
                client.send(message);
            }
        }
    }
}
