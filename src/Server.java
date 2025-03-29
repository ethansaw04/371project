public class Server {

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
            int deathNum = (int) (Math.random() * maxNumPotions);
            for (int j = 0; j < maxNumPotions; j++) {
                if (j == deathNum) {
                    potionDeath[i][j] = true;
                } else {
                    potionDeath[i][j] = false;
                }
            }
        }

		System.out.println("Hello, World!");
	
	}
}