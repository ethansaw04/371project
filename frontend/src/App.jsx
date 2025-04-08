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
    selectedCard: null,
    round: 0
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
          try {
            const data = JSON.parse(event.data);
            console.log("Received:", data);

            if (data.type === "TURN") {
              // Update current player turn
              const currentId = parseInt(data.playerId.replace("player", ""), 10) - 1; // Convert to 0-based index
              console.log(`Setting current player to index: ${currentId}`);

              // Make the message more prominent for turns
              setMessage(
                <div className="turn-message">
                  <span className="turn-highlight">➡️ It's {gameState.players[currentId].name}'s turn! ⬅️</span>
                  {currentId === playerId - 1 &&
                    <div className="your-turn-alert">YOUR TURN TO PLAY!</div>
                  }
                </div>
              );

              setGameState(prevState => ({
                ...prevState,
                currentPlayer: currentId
              }));
            }
            else if (data.type === "GAME_STATE") {
              // Update game state and message
              setMessage(data.message || "Game has started!");
              setGameState(prevState => ({
                ...prevState,
                requiredCard: data.currentRound,
                round: prevState.round + 1,
                isGameStarted: true
              }));
            }
            else if (data.type === "DEAD") {
              // Mark player as dead
              const deadId = parseInt(data.playerId.replace("player", ""), 10) - 1; // Convert to 0-based index
              setMessage(`${gameState.players[deadId].name} was eliminated!`);

              setGameState(prevState => {
                const updatedPlayers = [...prevState.players];
                updatedPlayers[deadId].isAlive = false;
                return {
                  ...prevState,
                  players: updatedPlayers
                };
              });
            }
            else if (data.type === "HAND") {
              // Update player's hand
              if (data.cards && Array.isArray(data.cards)) {
                setPlayerHand(data.cards);
              }
            }
          } catch (error) {
            console.error("Error processing WebSocket message:", error);
          }
        };

        wsRef.current.onerror = (err) => console.error("WebSocket error:", err);
        wsRef.current.onclose = () => console.log("WebSocket connection closed");
      }
    }, 1000);

    return () => {
      clearTimeout(timer);
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, []);

  // Add handler for console messages about hand changes
  useEffect(() => {
    // Function to extract card information from console messages
    const extractCardsFromMessage = (msg) => {
      if (typeof msg !== 'string') return null;

      // Look for hand information in the message
      if (msg.includes('Your hand:')) {
        const handMatch = msg.match(/Your hand: \[(.*?)\]/);
        if (handMatch && handMatch[1]) {
          return handMatch[1].split(', ').map(card => card.trim());
        }
      }

      // Look for round information in the message
      if (msg.includes('Round:') || msg.includes('Round is:')) {
        const roundMatch = msg.match(/Round:? (?:is:)? ?([A-Z])/);
        if (roundMatch && roundMatch[1]) {
          console.log("Detected round card:", roundMatch[1]);
          setGameState(prevState => ({
            ...prevState,
            requiredCard: roundMatch[1],
            isGameStarted: true
          }));
        }
      }

      return null;
    };

    // Create console.log override to catch hand information
    const originalConsoleLog = console.log;
    console.log = function() {
      // Call the original console.log
      originalConsoleLog.apply(console, arguments);

      // Check for hand information in the message
      const message = arguments[0];
      const cards = extractCardsFromMessage(message);
      if (cards) {
        setPlayerHand(cards);
        console.info("Hand updated from console message:", cards);
      }
    };

    // Process initial welcome messages that might be in the console already
    // This helps with initial game state setup
    const consoleLog = console.log;
    setTimeout(() => {
      // Look for existing messages in the DOM that might contain game info
      const consoleElements = document.querySelectorAll('div.turn-message, div.console-message');
      consoleElements.forEach(element => {
        const text = element.textContent;
        extractCardsFromMessage(text);
      });

      // Set initial test data if needed
      if (playerHand.length === 0) {
        const initialTestHand = window.initialHand || ['A', 'Q', 'A', 'Q', 'K'];
        setPlayerHand(initialTestHand);
        console.info("Set initial test hand:", initialTestHand);
      }
    }, 500);

    return () => {
      // Restore original console.log
      console.log = originalConsoleLog;
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

  // Enhanced player rendering with clearer turn indication
  const renderPlayers = () => {
    return gameState.players.map((player, index) => {
      const isCurrentPlayer = gameState.currentPlayer === index;

      // More prominent styling for the current player
      const playerStyle = {
        opacity: player.isAlive ? 1 : 0.5,
        background: player.isAlive
          ? (isCurrentPlayer ? 'rgba(255, 215, 0, 0.8)' : 'rgba(255, 255, 255, 0.8)')
          : 'rgba(100, 100, 100, 0.8)',
        border: player.isAlive
          ? (isCurrentPlayer ? '3px solid gold' : '2px solid red')
          : '2px solid black',
        boxShadow: isCurrentPlayer ? '0 0 20px 10px rgba(255, 215, 0, 0.7)' : 'none',
        color: isCurrentPlayer ? '#000' : (player.isAlive ? '#000' : '#fff'),
        fontWeight: isCurrentPlayer ? 'bold' : 'normal',
        position: 'absolute',
        padding: '15px',
        borderRadius: '10px',
        zIndex: 10,
        transition: 'all 0.3s ease'
      };

      // Position the players
      switch(player.position) {
        case 'top':
          playerStyle.top = '20px';
          playerStyle.left = '50%';
          playerStyle.transform = 'translateX(-50%)';
          break;
        case 'bottom':
          playerStyle.bottom = '20px';
          playerStyle.left = '50%';
          playerStyle.transform = 'translateX(-50%)';
          break;
        case 'left':
          playerStyle.left = '20px';
          playerStyle.top = '50%';
          playerStyle.transform = 'translateY(-50%)';
          break;
        case 'right':
          playerStyle.right = '20px';
          playerStyle.top = '50%';
          playerStyle.transform = 'translateY(-50%)';
          break;
        default:
          break;
      }

      return (
        <div
          key={index}
          style={playerStyle}
        >
          {player.name}
          {isCurrentPlayer && player.isAlive && (
            <div style={{
              position: 'absolute',
              top: '-25px',
              left: '50%',
              transform: 'translateX(-50%)',
              background: 'gold',
              color: 'black',
              fontWeight: 'bold',
              padding: '3px 8px',
              borderRadius: '5px',
              fontSize: '12px',
              whiteSpace: 'nowrap',
              animation: 'pulse 1.5s infinite'
            }}>
              CURRENT TURN
            </div>
          )}
          {!player.isAlive && (
            <span style={{
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
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        marginBottom: '20px',
        background: 'rgba(255, 255, 255, 0.8)',
        padding: '15px',
        borderRadius: '10px',
        boxShadow: '0 5px 15px rgba(0, 0, 0, 0.2)'
      }}>
        <h3 style={{ marginTop: 0, marginBottom: '10px', color: '#333' }}>Your Hand</h3>
        <div style={{ display: 'flex', gap: '15px' }}>
          {playerHand.map((card, index) => (
            <div key={index} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <img
                src={`/images/${getCardImage(card)}.png`}
                alt={card}
                width="70"
                onClick={() => selectCard(card)}
                style={{
                  transform: gameState.selectedCard === card ? 'translateY(-10px)' : '',
                  boxShadow: gameState.selectedCard === card ? '0 0 10px 2px gold' : '',
                  cursor: 'pointer',
                  transition: 'transform 0.2s, box-shadow 0.2s',
                  borderRadius: '5px'
                }}
              />
              <div style={{ marginTop: '5px', fontWeight: 'bold', color: '#333' }}>{card}</div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  // Helper to get card image name - corrected to use actual card values
  const getCardImage = (card) => {
    switch(card) {
      case 'A': return 'ace';
      case 'K': return 'king';
      case 'Q': return 'queen';
      case 'J': return 'joker';
      default: return 'card-back';
    }
  };

  // Enhanced game status message with prominent round information
  const renderStatusMessage = () => {
    // Don't show the waiting message if we already have a required card
    const displayMessage = gameState.requiredCard && message === "Waiting for game to start..."
      ? `Current Round: ${gameState.requiredCard}`
      : message;

    return (
      <div style={{
        position: 'absolute',
        top: '120px',
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 20,
        background: 'rgba(255, 255, 255, 0.95)',
        padding: '15px 25px',
        borderRadius: '10px',
        boxShadow: '0 0 15px rgba(0, 0, 0, 0.3)',
        textAlign: 'center',
        minWidth: '300px',
        border: gameState.isGameStarted ? '2px solid gold' : '2px solid #ccc'
      }}>
        <div style={{ fontSize: '18px', fontWeight: 'bold' }}>
          {displayMessage}
        </div>
        {gameState.requiredCard && (
          <div style={{
            marginTop: '15px',
            fontSize: '1.5em',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            padding: '10px',
            background: 'rgba(255, 223, 0, 0.2)',
            borderRadius: '8px',
            border: '2px dashed gold'
          }}>
            <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>CURRENT ROUND</div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '10px'
            }}>
              <span>Card to play:</span>
              <span style={{
                fontWeight: 'bold',
                fontSize: '1.8em',
                color: '#d4af37',
                textShadow: '1px 1px 3px rgba(0,0,0,0.3)'
              }}>
                {gameState.requiredCard}
              </span>
              <img
                src={`/images/${getCardImage(gameState.requiredCard)}.png`}
                alt={gameState.requiredCard}
                style={{ width: '40px', height: 'auto' }}
              />
            </div>
            <div style={{ fontSize: '0.8em', marginTop: '10px' }}>
              Round: {gameState.round}
            </div>
          </div>
        )}
      </div>
    );
  };

  // Enhanced input section with clearer instructions
  const renderInputSection = () => {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        width: '100%',
        maxWidth: '300px',
        alignItems: 'center',
        padding: '15px',
        background: 'rgba(255, 255, 255, 0.9)',
        borderRadius: '10px',
        boxShadow: '0 5px 15px rgba(0, 0, 0, 0.2)'
      }}>
        <div style={{
          width: '100%',
          textAlign: 'center',
          marginBottom: '10px',
          fontWeight: 'bold',
          color: '#333'
        }}>
          {gameState.requiredCard && (
            <div style={{
              marginBottom: '8px',
              fontSize: '16px',
              padding: '5px',
              background: 'rgba(0, 0, 255, 0.1)',
              borderRadius: '5px'
            }}>
              Enter the number of <span style={{
                color: 'red',
                fontWeight: 'bold',
                fontSize: '18px'
              }}>{gameState.requiredCard}</span> cards:
            </div>
          )}
        </div>

        <div style={{
          display: 'flex',
          flexDirection: 'column',
          width: '100%'
        }}>
          <label style={{
            fontSize: '0.9em',
            marginBottom: '3px',
            color: '#006400',
            fontWeight: 'bold'
          }}>
            ACTUAL count (truth):
          </label>
          <input
            type="number"
            placeholder="Actual Count"
            value={actualCount}
            onChange={(e) => setActualCount(e.target.value)}
            style={{
              padding: '8px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              width: '100%',
              marginBottom: '10px'
            }}
          />
        </div>

        <div style={{
          display: 'flex',
          flexDirection: 'column',
          width: '100%'
        }}>
          <label style={{
            fontSize: '0.9em',
            marginBottom: '3px',
            color: '#8B0000',
            fontWeight: 'bold'
          }}>
            FAKE count (what others see):
          </label>
          <input
            type="number"
            placeholder="Fake Count"
            value={fakeCount}
            onChange={(e) => setFakeCount(e.target.value)}
            style={{
              padding: '8px',
              borderRadius: '4px',
              border: '1px solid #ccc',
              width: '100%',
              marginBottom: '10px'
            }}
          />
        </div>

        <button
          onClick={submitMove}
          style={{
            border: 'none',
            padding: '10px 15px',
            borderRadius: '5px',
            marginTop: '10px',
            background: '#4CAF50',
            color: 'white',
            cursor: 'pointer',
            fontWeight: 'bold',
            width: '100%'
          }}
        >
          Submit Move
        </button>
        <button
          onClick={callBluff}
          style={{
            background: '#f44336',
            color: 'white',
            border: 'none',
            padding: '10px 15px',
            marginTop: '10px',
            borderRadius: '5px',
            cursor: 'pointer',
            fontWeight: 'bold',
            width: '100%'
          }}
        >
          Call Bluff
        </button>
      </div>
    );
  };

  return (
    <div style={{
      width: '1300px',
      height: '650px',
      borderRadius: '15px',
      position: 'relative',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '10px',
      background: 'rgba(0, 0, 0, 0.3)',
      border: '1px solid #444'
    }}>
      {renderPlayers()}
      {renderStatusMessage()}

      <div style={{
        width: '500px',
        height: '500px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '10px',
        position: 'relative',
        marginTop: '80px',
        zIndex: 1
      }}>
        {renderPlayerHand()}
        {renderInputSection()}
      </div>
    </div>
  );
};

export default App;