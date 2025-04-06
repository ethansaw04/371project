import React, { useState, useEffect, useRef } from 'react';
import './style.css';

const App = () => {
  const [gameState, setGameState] = useState({
    players: [
      { name: "Player 1", position: "top", isAlive: true },
      { name: "Player 2", position: "left", isAlive: true },
      { name: "Player 3", position: "right", isAlive: true },
      { name: "Player 4", position: "bottom", isAlive: true }
    ],
    currentPlayer: null,
    requiredCard: "",
    isGameStarted: false,
    selectedCard: null
  });

  const [playerHand, setPlayerHand] = useState([]);
  const [actualCount, setActualCount] = useState("");
  const [fakeCount, setFakeCount] = useState("");
  const [message, setMessage] = useState("Waiting for game to start...");
  const wsRef = useRef(null);
  const playerId = 1; // Hardcoded player ID for now

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!wsRef.current) {
        wsRef.current = new WebSocket("ws://localhost:8082");

        wsRef.current.onopen = () => {
          console.log("Connected to WebSocket");
          // Register player with WebSocket server
          const registerMsg = {
            type: "REGISTER",
            playerId
          };
          wsRef.current.send(JSON.stringify(registerMsg));
        };

        wsRef.current.onmessage = (event) => {
          const data = JSON.parse(event.data);
          console.log("Received:", data);

            if (data.type === "TURN") {
              // Update current player turn
              const currentId = parseInt(data.playerId.replace("player", ""), 10);
              // Make the message more prominent for turns
              setMessage(
                <div className="turn-message">
                  <span className="turn-highlight">➡️ It's Player {currentId}'s turn! ⬅️</span>
                  {currentId === playerId &&
                    <div className="your-turn-alert">YOUR TURN TO PLAY!</div>
                  }
                </div>
              );
              setGameState(prevState => ({
                ...prevState,
                currentPlayer: currentId - 1
              }));
            }
          else if (data.type === "GAME_STATE") {
            // Update game state and message
            setMessage(data.message);
            setGameState(prevState => ({
              ...prevState,
              requiredCard: data.currentRound,
              isGameStarted: true
            }));
          }
          else if (data.type === "DEAD") {
            // Mark player as dead
            const deadId = parseInt(data.playerId.replace("player", ""), 10);
            setMessage(`Player ${deadId} was eliminated!`);

            setGameState(prevState => {
              const updatedPlayers = [...prevState.players];
              updatedPlayers[deadId - 1].isAlive = false;
              return {
                ...prevState,
                players: updatedPlayers
              };
            });
          }
        };

        wsRef.current.onerror = (err) => console.error("WebSocket error:", err);
      }
    }, 1000);

    return () => {
      clearTimeout(timer);
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [playerId]);

  // Add listener for socket messages to parse hand information
  useEffect(() => {
    const handleSocketMessage = (message) => {
      // Parse hand information from server messages
      if (message.startsWith("Your hand:")) {
        try {
          // Extract cards from message like "Your hand: [Q, Q, K, K, J]"
          const handMatch = message.match(/\[(.*?)\]/);
          if (handMatch && handMatch[1]) {
            const cards = handMatch[1].split(', ').map(card => card.trim());
            setPlayerHand(cards);
          }
        } catch (err) {
          console.error("Error parsing hand:", err);
        }
      }
    };

    // Add event listener
    window.addEventListener('message', (event) => {
      if (event.data && typeof event.data === 'string') {
        handleSocketMessage(event.data);
      }
    });

    // For testing, manually set the hand that was shown in your console
    setPlayerHand(['Q', 'Q', 'K', 'K', 'J']);

    return () => {
      window.removeEventListener('message', handleSocketMessage);
    };
  }, []);

  const submitMove = () => {
    if (wsRef.current && actualCount && fakeCount) {
      const moveMsg = {
        type: "MOVE",
        playerId,
        actual: parseInt(actualCount, 10),
        fake: parseInt(fakeCount, 10)
      };
      wsRef.current.send(JSON.stringify(moveMsg));
      setMessage("Move submitted!");
      setActualCount("");
      setFakeCount("");
    } else {
      setMessage("Please enter both actual and fake card counts");
    }
  };

  const callBluff = () => {
    if (wsRef.current) {
      const bluffMsg = {
        type: "BLUFF",
        playerId
      };
      wsRef.current.send(JSON.stringify(bluffMsg));
      setMessage("Bluff called!");
    }
  };

  const selectCard = (card) => {
    setGameState(prevState => ({
      ...prevState,
      selectedCard: card
    }));
  };

  // Generate player elements with proper styling
// In your App.jsx, modify the renderPlayers function:

    const renderPlayers = () => {
      return gameState.players.map((player, index) => {
        const isCurrentPlayer = gameState.currentPlayer === index;

        const playerStyle = {
          opacity: player.isAlive ? 1 : 0.5,
          background: player.isAlive ? 'rgba(255, 255, 255, 0.8)' : 'rgba(100, 100, 100, 0.8)',
          border: player.isAlive ? '2px solid red' : '2px solid black',
          boxShadow: isCurrentPlayer ? '0 0 15px 8px gold' : 'none'
        };

        return (
          <div
            key={index}
            className={`player ${player.position} ${player.isAlive ? '' : 'dead'} ${isCurrentPlayer ? 'active-player' : ''}`}
            style={playerStyle}
          >
            {player.name}
            {isCurrentPlayer && player.isAlive && (
              <div className="turn-indicator">CURRENT TURN</div>
            )}
            {!player.isAlive && (
              <span className="dead-indicator" style={{
                position: 'absolute',
                top: '5px',
                right: '5px',
                fontSize: '16px'
              }}>💀</span>
            )}
          </div>
        );
      });
    };

  // Render the player's hand
  const renderPlayerHand = () => {
    return (
      <div className="player-hand">
        <h3>Your Hand</h3>
        <div className="hand-cards">
          {playerHand.map((card, index) => (
            <div key={index} className="hand-card">
              <img
                src={`/images/${getCardImage(card)}.png`}
                alt={card}
                width="70"
                onClick={() => selectCard(card)}
                style={{
                  transform: gameState.selectedCard === card ? 'translateY(-10px)' : '',
                  boxShadow: gameState.selectedCard === card ? '0 0 10px 2px gold' : '',
                  cursor: 'pointer'
                }}
              />
              <div className="card-label">{card}</div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  // Helper to get card image name
  const getCardImage = (card) => {
    switch(card) {
      case 'A': return 'ace';
      case 'K': return 'king';
      case 'Q': return 'queen';
      case 'J': return 'joker';
      default: return 'card-back';
    }
  };

  return (
    <div className="game-container">
      {renderPlayers()}

      <div className="message-container">
        <div className="message">
          {message}
          {gameState.requiredCard && (
            <div className="current-round">
              Current Round: <strong>{gameState.requiredCard}</strong>
            </div>
          )}
        </div>
      </div>

      <div className="table">
        {renderPlayerHand()}

        <div className="move-input">
          <input
            type="number"
            placeholder="Actual Count"
            value={actualCount}
            onChange={(e) => setActualCount(e.target.value)}
          />
          <input
            type="number"
            placeholder="Fake Count"
            value={fakeCount}
            onChange={(e) => setFakeCount(e.target.value)}
          />
          <button className="submit-btn" onClick={submitMove}>Submit Move</button>
          <button className="bluff-btn" onClick={callBluff}>Call Bluff</button>
        </div>
      </div>
    </div>
  );
};

export default App;