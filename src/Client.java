import java.io.*;
import java.net.*;
import java.util.*;
public class Client {

	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private Scanner scanner;
	private String playerName;
	private boolean gameRunning = true;

	public Client(String serverAddress, int port) {
		try {
			// Connect to server
			socket = new Socket(serverAddress, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			scanner = new Scanner(System.in);

			System.out.println("Connected to Liar's Table server at " + serverAddress + ":" + port);

			// Get player name
			System.out.print("Enter your name: ");
			playerName = scanner.nextLine();
			out.println(playerName); // Send name to server

			// Start threads to handle server messages and user input
			startMessageReceiver();
			handleUserInput();

		} catch (IOException e) {
			System.err.println("Error connecting to server: " + e.getMessage());
		} finally {
			closeConnection();
		}
	}

	private void startMessageReceiver() {
		// Create a separate thread to continuously read messages from the server
		new Thread(() -> {
			try {
				String serverMessage;
				while ((serverMessage = in.readLine()) != null) {
					if (serverMessage.equals("GAME_OVER")) {
						gameRunning = false;
						System.out.println("Game has ended");
						break;
					} else if (serverMessage.startsWith("YOUR_CARDS:")) {
						String[] cards = serverMessage.substring(11).split(",");
						System.out.println("Your cards: " + String.join(", ", cards));
					} else if (serverMessage.equals("YOUR_TURN")) {
						System.out.println("\n--- YOUR TURN ---");
						System.out.println("Options: [claim <dice_value> <count>] or [challenge]");
					} else {
						System.out.println(serverMessage);
					}
				}
			} catch (IOException e) {
				if (gameRunning) {
					System.err.println("Connection to server lost: " + e.getMessage());
				}
			}
		}).start();
	}

	private void handleUserInput() {
		try {
			while (gameRunning) {
				String input = scanner.nextLine();
				if (input.equals("quit")) {
					out.println("QUIT");
					gameRunning = false;
					break;
				} else {
					out.println(input);
				}
			}
		} catch (Exception e) {
			System.err.println("Error processing input: " + e.getMessage());
		}
	}

	private void closeConnection() {
		try {
			if (in != null) in.close();
			if (out != null) out.close();
			if (socket != null && !socket.isClosed()) socket.close();
			if (scanner != null) scanner.close();
		} catch (IOException e) {
			System.err.println("Error closing connection: " + e.getMessage());
		}
	}
    
    public static void main(String[] args){

		String serverAddress = "localhost";
		int port = 8080;

		// Allow command-line arguments to specify server and port
		if (args.length >= 1) {
			serverAddress = args[0];
		}
		if (args.length >= 2) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port number. Using default: 8080");
			}
		}

		new Client(serverAddress, port);
	}
}
