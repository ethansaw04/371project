import java.io.*;
import java.net.*;

public class BluffClient {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java BluffClient <server_ip> <port>");
            return;
        }
        String serverIp = args[0];
        int port       = Integer.parseInt(args[1]);
        Socket sock    = new Socket(serverIp, port);
        BufferedReader in      = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        PrintWriter    out     = new PrintWriter(sock.getOutputStream(), true);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        // thread to print server messages
        new Thread(() -> {
            try {
                String l;
                while ((l = in.readLine()) != null) {
                    System.out.println(l);
                }
            } catch (IOException e) { }
        }).start();

        // main loop to send user commands
        String cmd;
        while ((cmd = console.readLine()) != null) {
            out.println(cmd);
        }
    }
}
