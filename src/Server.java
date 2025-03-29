package player;
import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
	private ArrayList<Player> player_list = new ArrayList<>();	// keeps track of all players
	private int port_num;
	private int num_connections = 0;

	private create_connection() throws IOException {	// creates the server on the port
		ServerSocket = new ServerSocket(this.port_num);
		System.out.println("server is running on ", this.port_num);

	}

	public Server(int port) {
		this.port_num = port;
		create_connection();
		System.out.println("successfully created connection");
	}



}