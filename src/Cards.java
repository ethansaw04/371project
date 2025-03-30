/**
 * Represents a deck of cards for the Liar's Bar game.
 * A standard Liar's Bar deck includes:
 * 6 Kings, 6 Queens, 6 Aces and 2 Jokers
 */
public class Cards {
    // Card types
    public static final String KING = "King";
    public static final String QUEEN = "Queen";
    public static final String ACE = "Ace";
    public static final String JOKER = "Joker";

    // Deck composition
    private static final int NUM_KINGS = 6;
    private static final int NUM_QUEENS = 6;
    private static final int NUM_ACES = 6;
    private static final int NUM_JOKERS = 2;

    // The full deck of cards
    private String[] deck;
    // Current position in the deck when dealing
    private int currentPosition;

    /**
     * Creates a new deck of cards for Liar's Bar and shuffles them.
     */
    public Cards() {
        // Calculate total number of cards
        int totalCards = NUM_KINGS + NUM_QUEENS + NUM_ACES + NUM_JOKERS;
        deck = new String[totalCards];

        // Initialize the deck with the correct number of each card
        int index = 0;

        // Add Kings
        for (int i = 0; i < NUM_KINGS; i++) {
            deck[index++] = KING;
        }

        // Add Queens
        for (int i = 0; i < NUM_QUEENS; i++) {
            deck[index++] = QUEEN;
        }

        // Add Aces
        for (int i = 0; i < NUM_ACES; i++) {
            deck[index++] = ACE;
        }

        // Add Jokers
        for (int i = 0; i < NUM_JOKERS; i++) {
            deck[index++] = JOKER;
        }

        // Shuffle the deck
        shuffle();

        // Reset current position
        currentPosition = 0;
    }

    /**
     * Shuffles the deck of cards.
     */
    public void shuffle() {
        for (int i = deck.length - 1; i > 0; i--) {
            // Generate a random index between 0 and i (inclusive)
            int j = (int) (Math.random() * (i + 1));

            // Swap cards at positions i and j
            String temp = deck[i];
            deck[i] = deck[j];
            deck[j] = temp;
        }
        // Reset current position after shuffling
        currentPosition = 0;
    }

    /**
     * Deals the specified number of cards from the deck.
     *
     * @param numCards Number of cards to deal
     * @return Array of dealt cards
     * @throws IllegalArgumentException if not enough cards are left in the deck
     */
    public String[] dealCards(int numCards) {
        if (currentPosition + numCards > deck.length) {
            throw new IllegalArgumentException("Not enough cards left in the deck");
        }

        String[] dealtCards = new String[numCards];

        for (int i = 0; i < numCards; i++) {
            dealtCards[i] = deck[currentPosition++];
        }

        return dealtCards;
    }

    /**
     * Returns the number of cards remaining in the deck.
     *
     * @return Number of cards remaining
     */
    public int cardsRemaining() {
        return deck.length - currentPosition;
    }

    /**
     * Prints the current deck state (for debugging).
     */
    public void printDeck() {
        System.out.println("Current deck state:");
        for (int i = 0; i < deck.length; i++) {
            if (i == currentPosition) {
                System.out.print(" | "); // Mark the current position
            }
            System.out.print(deck[i] + " ");
        }
        System.out.println();
        System.out.println("Cards remaining: " + cardsRemaining());
    }

    /**
     * Simple test of the Cards class functionality.
     */
    public static void main(String[] args) {
        Cards cards = new Cards();
        System.out.println("Created a new deck of Liar's Bar cards");

        // Print initial deck
        cards.printDeck();

        // Deal some cards
        int cardsToDeal = 5;
        System.out.println("\nDealing " + cardsToDeal + " cards:");
        String[] hand = cards.dealCards(cardsToDeal);

        // Print the dealt hand
        for (String card : hand) {
            System.out.print(card + " ");
        }
        System.out.println();

        // Print remaining deck
        System.out.println("\nAfter dealing:");
        cards.printDeck();

        // Shuffle and start over
        System.out.println("\nShuffling the deck...");
        cards.shuffle();
        cards.printDeck();
    }
}