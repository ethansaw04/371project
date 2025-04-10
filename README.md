# Bluff Game Setup and Execution

This document outlines the steps to run the Bluff card game server and client applications.

## Running the Server

1.  **Navigate to the `src` directory.**
2.  **Compile the Java source files:** Use the command `javac *.java` in your terminal.
3.  **Start the Bluff Server:** Execute the command `java BluffServer` in your terminal.
    * The server will start and output its IP address and port number to the console.

## Running the Client

1.  You need four client connections to start the game. This can be achieved in two ways:

* **Single Machine:** Open multiple terminal windows and run the client application once in each.
* **Multiple Devices:** Have up to four different players run the client application from their own computers.

2.  **Execute the Bluff Client application:** Use the command `java BluffClientSwing <IP Address> <Port number>` in your terminal.
    * Replace `<IP Address>` with the IP address of the server (obtained from the server's output).
    * Replace `<Port number>` with the port number of the server (obtained from the server's output).

    **Example:**
    Execute `java BluffClientSwing 172.29.146.3 12345` in a terminal.

3.  **Repeat step 2 three more times** in separate terminal windows, using the same IP address and port number.

## Game Start

Once four client applications have successfully connected to the server, the Bluff game will begin. Follow the on-screen instructions in each client window to play the game.
