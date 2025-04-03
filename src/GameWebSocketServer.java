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
            int playerId = json.getInt("playerId");
            
            if ("MOVE".equals(type)) {
                int actual = json.getInt("actual");
                int fake = json.getInt("fake");
                String move = actual + " " + fake;
                // Forward the move to the corresponding ClientHandler in BluffServer.
                for (var player : bluffServer.getPlayers()) {
                    if (player.getPlayerID() == playerId) {
                        // Pass a placeholder for current round; backend logic uses its own currentRound.
                        bluffServer.processMove(player, move, "currentRound");
                        break;
                    }
                }
            } else if ("BLUFF".equals(type)) {
                // Forward the bluff call to the corresponding ClientHandler.
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
