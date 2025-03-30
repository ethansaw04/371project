import java.io.*;
import java.net.*;

public class BluffClient {
    private static String SERVER_ADDRESS;
    private static int PORT;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please enter server ip address and port number");
            System.out.println("For example, java Client 127.0.0.1 12345");
            return;
        }

        SERVER_ADDRESS = args[0];
        PORT = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to Bluff Server!");

            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (SocketException e) {
                    System.out.println("Connection terminated.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listener.start();

            while (true) {
                String userInput = console.readLine();
                out.println(userInput);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
