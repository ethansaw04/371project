import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    private String ip;
	private int port;
	private Socket socket;
	private boolean running = true;

	public Client(String ip, int port) {
		this.ip = ip;
		this.port = port;
		try {
			this.socket = new Socket(ip, port);
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

    public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Please enter the port number");
			System.out.println("i.e. java Client localhost 8088");
			return;
		}

		String server_ip = args[0];
		int port = Integer.parseInt(args[1]);

		try {
			Client client = new Client(server_ip, port);
			System.out.println("TYPE SHI");
		} catch (Exception e) {
			System.out.println("Error:" + e.getMessage());
		}

		while (true) {

		}

	}
}
