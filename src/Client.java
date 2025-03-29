import java.io.*;
import java.net.*;

public class Client {
    
    public static void main(String[] args){
	
        String serverAddress = "127.0.0.1";
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connected to server!");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                String message = consoleInput.readLine();
                out.println(message);
                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Leaving the game...");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
