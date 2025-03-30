// game.js - Frontend logic for Liars Table game with WebSocket integration

// Player class to maintain state
class Player {
  constructor(name, position) {
    this.name = name;
    this.position = position; // 'top', 'right', 'bottom', 'left'
    this.isAlive = true;
    this.cards = [];
  }
}

// Game state
const gameState = {
  players: [],
  currentPlayer: 0,
  cardsInPlay: [],
  requiredCard: null,
  isGameStarted: false,
  socket: null
};

// Initialize the game
function initGame() {
  // Create players (you can modify the names as needed)
  gameState.players = [
    new Player("Player 1", "top"),
    new Player("Player 2", "right"),
    new Player("Player 3", "bottom"),
    new Player("Player 4", "left")
  ];

  // Update player display initially
  updatePlayersDisplay();

  // Add event listeners
  document.querySelector('.start-btn').addEventListener('click', startGame);
  document.querySelector('.submit-btn').addEventListener('click', submitMove);

  // Add card selection functionality
  const cards = document.querySelectorAll('.deck img');
  cards.forEach(card => {
    card.addEventListener('click', selectCard);
  });

  // Connect to WebSocket server
  connectWebSocket();
}

// Connect to the WebSocket bridge
function connectWebSocket() {
  // Change the URL to your WebSocket bridge address
  const socketUrl = `ws://${window.location.hostname}:8080`;
  gameState.socket = new WebSocket(socketUrl);

  gameState.socket.onopen = function(event) {
    console.log("Connected to game server");
    // You might want to request initial game state here
    gameState.socket.send("GET_GAME_STATE");
  };

  gameState.socket.onmessage = function(event) {
    console.log("Message from server:", event.data);
    processServerMessage(event.data);
  };

  gameState.socket.onclose = function(event) {
    console.log("Disconnected from game server");
    // Maybe try to reconnect after a delay
    setTimeout(connectWebSocket, 5000);
  };

  gameState.socket.onerror = function(error) {
    console.error("WebSocket error:", error);
  };
}

// Process messages from the server
function processServerMessage(message) {
  try {
    // Try to parse as JSON
    const data = JSON.parse(message);

    // Handle player status updates
    if (data.playerStatus) {
      // Update player alive/dead status
      for (let i = 0; i < gameState.players.length; i++) {
        const player = gameState.players[i];
        if (data.playerStatus.hasOwnProperty(player.name)) {
          player.isAlive = data.playerStatus[player.name];
        }
      }

      // Update display
      updatePlayersDisplay();
    }

    // Handle other game state updates
    if (data.currentPlayer !== undefined) {
      gameState.currentPlayer = data.currentPlayer;
    }

    if (data.requiredCard) {
      gameState.requiredCard = data.requiredCard;
      document.querySelector('.message').innerHTML = `Need to play a: <b>${gameState.requiredCard}</b>`;
    }

    if (data.isGameStarted !== undefined) {
      gameState.isGameStarted = data.isGameStarted;
      document.querySelector('.start-btn').disabled = gameState.isGameStarted;
      document.querySelector('.submit-btn').disabled = !gameState.isGameStarted;
      document.querySelector('.message').style.visibility = gameState.isGameStarted ? 'visible' : 'hidden';
    }

  } catch (e) {
    // Not JSON, handle as text message
    console.log("Text message from server:", message);
    // You might want to display this in a message area
  }
}

// Update the display for all players
function updatePlayersDisplay() {
  gameState.players.forEach(player => {
    const playerElement = document.querySelector(`.player.${player.position}`);
    if (playerElement) {
      // Update player name
      playerElement.textContent = player.name;

      // Update appearance based on alive/dead status
      if (player.isAlive) {
        playerElement.classList.remove('dead');
        playerElement.style.opacity = '1';
        playerElement.style.background = 'rgba(255, 255, 255, 0.8)';
        playerElement.style.border = '2px solid red';

        // Remove dead indicator if it exists
        const deadIndicator = playerElement.querySelector('.dead-indicator');
        if (deadIndicator) {
          playerElement.removeChild(deadIndicator);
        }
      } else {
        playerElement.classList.add('dead');
        playerElement.style.opacity = '0.5';
        playerElement.style.background = 'rgba(100, 100, 100, 0.8)';
        playerElement.style.border = '2px solid black';

        // Add a visual indicator for dead players
        if (!playerElement.querySelector('.dead-indicator')) {
          const deadIndicator = document.createElement('img');
          deadIndicator.src = '/images/skull.png';
          deadIndicator.alt = 'Dead';
          deadIndicator.classList.add('dead-indicator');
          deadIndicator.style.width = '20px';
          deadIndicator.style.height = '20px';
          deadIndicator.style.position = 'absolute';
          deadIndicator.style.top = '5px';
          deadIndicator.style.right = '5px';
          playerElement.appendChild(deadIndicator);
        }
      }

      // Highlight current player
      if (gameState.isGameStarted && gameState.players.indexOf(player) === gameState.currentPlayer) {
        playerElement.style.boxShadow = '0 0 10px 5px yellow';
      } else {
        playerElement.style.boxShadow = 'none';
      }
    }
  });
}

// Start the game
function startGame() {
  if (gameState.isGameStarted) return;

  // Send start game command to server
  if (gameState.socket && gameState.socket.readyState === WebSocket.OPEN) {
    gameState.socket.send("START_GAME");
  } else {
    alert("Not connected to game server");
  }
}

// Handle card selection
function selectCard(event) {
  if (!gameState.isGameStarted) return;

  // Remove selection from all cards
  document.querySelectorAll('.deck img').forEach(card => {
    card.style.transform = '';
    card.style.boxShadow = '';
  });

  // Highlight the selected card
  event.target.style.transform = 'translateY(-10px)';
  event.target.style.boxShadow = '0 0 10px 2px gold';

  // Store the selection
  gameState.selectedCard = event.target.alt.split(' ')[0]; // Extract card type from alt text
}

// Handle move submission
function submitMove() {
  if (!gameState.isGameStarted || !gameState.selectedCard) return;

  // Send move to server
  if (gameState.socket && gameState.socket.readyState === WebSocket.OPEN) {
    gameState.socket.send(`PLAY_CARD:${gameState.selectedCard}`);

    // Clear selection
    gameState.selectedCard = null;
    document.querySelectorAll('.deck img').forEach(card => {
      card.style.transform = '';
      card.style.boxShadow = '';
    });
  } else {
    alert("Not connected to game server");
  }
}

// Initialize the game when the page loads
document.addEventListener('DOMContentLoaded', initGame);