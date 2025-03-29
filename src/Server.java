package player;
import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
	private ArrayList<Player> player_list = new ArrayList<>();	// keeps track of all players
	private int port_num;
	private int num_connections = 0;

    static int maxNumPlayers = 4;
    static int maxNumPotions = 3;

    static int currentConnectedPlayers = 0;
    static int currentTurn = 0;

    static boolean[][] potionsAvailable; //true if potion available to drink, false if not
    static boolean[][] potionDeath; //true if this potion will kill the player, 1->1 correspondence with available potions

	public static void main(String[] args){
        potionsAvailable = new boolean[maxNumPlayers][maxNumPotions];
        for (int i = 0; i < maxNumPlayers; i++) {
            for (int j = 0; j < maxNumPotions; j++) {
                potionsAvailable[i][j] = true;
            }
        }

        potionDeath = new boolean[maxNumPlayers][maxNumPotions];

        for (int i = 0; i < maxNumPlayers; i++) {
            boolean setDeathPotion = false;
            for (int j = 0; j < maxNumPotions; j++) {
                if (((int) (Math.random() * maxNumPotions) + 1 == 1) && !setDeathPotion) {
                    potionDeath[i][j] = true;
                    setDeathPotion = true;
                } else {
                    potionDeath[i][j] = false;
                }
            }
        }
}