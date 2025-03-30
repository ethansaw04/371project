import java.io.*;
import java.net.*;
import java.util.*;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

/**
 * WebSocket server that acts as a bridge between the Java client and the web frontend
 */
public class BluffBridge extends WebSocketServer {
    private static String GAME_SERVER_ADDRESS;
    private static int GAME_SERVER_PORT;
    private Socket gameServerSocket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private Set<WebSocket> webClients = new HashSet<>();
    private Map<String, Boolean> playerStatus = new HashMap<>(); // Tracks alive/dead status

    public BluffBridge(int port) {
        super(new InetSocketAddress(port));
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java BluffBridge <websocket_port> <game_server_ip> <game_server_port>");
            System.out.println("Example: java BluffBridge 8080 127.0.0.1 12345");
            return;
        }

        int websocketPort = Integer.parseInt(args[0]);
        GAME_SERVER_ADDRESS = args[1];
        GAME_SERVER_PORT = Integer.parseInt(args[2]);

        BluffBridge bridge = new BluffBridge(websocketPort);
        bridge.start();
        System.out.println("BluffBridge started on port: " + websocketPort);

        try {
            bridge.connectToGameServer();
        } catch (IOException e) {
            System.out.println("Failed to connect to game server: " + e.getMessage());
            bridge.stop();
        }
    }

    private void connectToGameServer() throws IOException {
        gameServerSocket = new Socket(GAME_SERVER_ADDRESS, GAME_SERVER_PORT);
        serverIn = new BufferedReader(new InputStreamReader(gameServerSocket.getInputStream()));
        serverOut = new PrintWriter(gameServerSocket.getOutputStream(), true);

        // Start thread to listen for game server messages
        new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = serverIn.readLine()) != null) {
                    processServerMessage(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Game server connection closed: " + e.getMessage());
                try {
                    gameServerSocket.close();
                } catch (IOException ex) {
                    // Ignore close errors
                }
            }
        }).start();
    }

    private void processServerMessage(String message) {
        // Parse the server message to extract game state
        // This will depend on your server's protocol

        // Example parsing assuming server messages are in a specific format
        if (message.contains("PLAYER_STATUS")) {
            // Example: "PLAYER_STATUS:Player1:ALIVE,Player2:DEAD,Player3:ALIVE,Player4:ALIVE"
            String[] parts = message.split(":");
            if (parts.length > 1) {
                String[] playerData = parts[1].split(",");
                for (String playerInfo : playerData) {
                    String[] playerDetails = playerInfo.split(":");
                    if (playerDetails.length == 2) {
                        playerStatus.put(playerDetails[0], "ALIVE".equals(playerDetails[1]));
                    }
                }

                // Send updated status to all web clients
                broadcastPlayerStatus();
            }
        } else {
            // Forward any other message to web clients
            broadcast(message);
        }
    }

    private void broadcastPlayerStatus() {
        // Convert player status to JSON
        StringBuilder json = new StringBuilder("{\"playerStatus\":{");
        Iterator<Map.Entry<String, Boolean>> it = playerStatus.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Boolean> entry = it.next();
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (it.hasNext()) {
                json.append(",");
            }
        }
        json.append("}}");

        broadcast(json.toString());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        webClients.add(conn);
        System.out.println("New web client connected: " + conn.getRemoteSocketAddress());

        // Send current game state to new client
        if (!playerStatus.isEmpty()) {
            broadcastPlayerStatus();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        webClients.remove(conn);
        System.out.println("Web client disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Forward web client commands to game server
        serverOut.println(message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            webClients.remove(conn);
        }
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started");
    }

    private void broadcast(String message) {
        for (WebSocket client : webClients) {
            client.send(message);
        }
    }
}