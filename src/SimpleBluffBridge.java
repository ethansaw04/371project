import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;

/**
 * Simple HTTP server that acts as a bridge between the Java client and the web frontend
 * No external dependencies required
 */
public class SimpleBluffBridge {
    private static String GAME_SERVER_ADDRESS;
    private static int GAME_SERVER_PORT;
    private static int HTTP_PORT = 8080;
    private Socket gameServerSocket;
    private BufferedReader serverIn;
    private PrintWriter serverOut;
    private Map<String, Boolean> playerStatus = new HashMap<>(); // Tracks alive/dead status
    private HttpServer httpServer;

    public SimpleBluffBridge() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);

        // Create context for game state API
        httpServer.createContext("/api/gamestate", new GameStateHandler());

        // Create context for handling commands
        httpServer.createContext("/api/command", new CommandHandler());

        // Serve static files from the web directory
        httpServer.createContext("/", new StaticFileHandler());

        httpServer.setExecutor(null); // Use default executor
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java SimpleBluffBridge <game_server_ip> <game_server_port>");
            System.out.println("Example: java SimpleBluffBridge 127.0.0.1 12345");
            return;
        }

        GAME_SERVER_ADDRESS = args[0];
        GAME_SERVER_PORT = Integer.parseInt(args[1]);

        try {
            SimpleBluffBridge bridge = new SimpleBluffBridge();
            bridge.connectToGameServer();
            bridge.start();
            System.out.println("SimpleBluffBridge started on port: " + HTTP_PORT);
        } catch (IOException e) {
            System.out.println("Failed to start bridge: " + e.getMessage());
        }
    }

    private void start() {
        httpServer.start();
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
            }
        } else {
            // Process other types of messages
            System.out.println("Received message from server: " + message);
        }
    }

    // Handler for serving the game state as JSON
    class GameStateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                // Enable CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json");

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

                String response = json.toString();
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }

    // Handler for sending commands to the game server
    class CommandHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Enable CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

                // Read the command from the request body
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }

                // Forward the command to the game server
                String command = requestBody.toString();
                serverOut.println(command);

                String response = "{\"status\":\"success\"}";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                // Handle preflight CORS requests
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
            }
        }
    }

    // Simple handler for static files
    class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            try {
                File file = new File("web" + path);
                if (file.exists() && !file.isDirectory()) {
                    // Set content type
                    String contentType = getContentType(path);
                    exchange.getResponseHeaders().add("Content-Type", contentType);

                    // Send the file
                    exchange.sendResponseHeaders(200, file.length());
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, count);
                        }
                    }
                } else {
                    // File not found
                    String response = "404 Not Found";
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            } catch (IOException e) {
                String response = "500 Internal Server Error";
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            return "text/plain";
        }
    }
}