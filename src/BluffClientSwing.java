import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class BluffClientSwing extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> handModel = new DefaultListModel<>();
    private final JList<String> handList = new JList<>(handModel);
    private final JTextField actualField = new JTextField(3);
    private final JTextField fakeField   = new JTextField(3);

    public BluffClientSwing(String serverIp, int port) {
        super("Bluff Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLayout(new BorderLayout());

        // log area
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // right: hand (desk of cards)
        handList.setBorder(BorderFactory.createTitledBorder("Your Hand"));
        add(new JScrollPane(handList), BorderLayout.EAST);

        // bottom: input and buttons
        JPanel ctrl = new JPanel();
        // enter number of actual
        ctrl.add(new JLabel("Actual:"));
        ctrl.add(actualField);
        // enter number of fakes
        ctrl.add(new JLabel("Fake:"));
        ctrl.add(fakeField);
        // buttons for ending turn or calling someone out 
        JButton submit = new JButton("Submit Move");
        JButton bluff  = new JButton("Call Bluff");
        ctrl.add(submit);
        ctrl.add(bluff);
        add(ctrl, BorderLayout.SOUTH);

        submit.addActionListener(e -> {
            String a = actualField.getText().trim();
            String f = fakeField.getText().trim();
            if (a.isEmpty()||f.isEmpty()) {
                appendLog("Enter both counts.");
                return;
            }
            out.println("MOVE " + a + " " + f);
            appendLog(">> MOVE " + a + " " + f);
            actualField.setText("");
            fakeField.setText("");
        });
        bluff.addActionListener(e -> {
            out.println("BLUFF");
            appendLog(">> BLUFF");
        });

        // connect in background
        new Thread(() -> connect(serverIp, port)).start();
    }

    private void connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out    = new PrintWriter(socket.getOutputStream(), true);
            appendLog("Connected to server " + ip + ":" + port);

            // reader thread
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> processServer(msg));
            }
        } catch (IOException e) {
            appendLog("Connection failed: " + e.getMessage());
        }
    }

    private void processServer(String msg) {
        appendLog(msg);
        if (msg.startsWith("Your hand:")) {
            int b = msg.indexOf('['), e = msg.indexOf(']');
            if (b>=0 && e>b) {
                String[] cards = msg.substring(b+1,e).split(",\\s*");
                handModel.clear();
                for (String c : cards) if (!c.isBlank()) handModel.addElement(c);
            }
        }
    }

    private void appendLog(String txt) {
        logArea.append(txt + "\n");
    }

    public static void main(String[] args) {
        if (args.length!=2) {
            System.out.println("Usage: java BluffClientSwing <server_ip> <port>");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            BluffClientSwing f = new BluffClientSwing(args[0], Integer.parseInt(args[1]));
            f.setVisible(true);
        });
    }
}
