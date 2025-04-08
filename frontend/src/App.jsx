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
  const [playerId, setPlayerId] = useState(null); // Will be set dynamically from server
  const wsRef = useRef(null);

  // Create the WebSocket connection once on component mount.
  useEffect(() => {
    const timer = setTimeout(() => {
      if (!wsRef.current) {
        wsRef.current = new WebSocket("ws://localhost:8082");

        wsRef.current.onopen = () => {
          console.log("Connected to WebSocket");
          // Send registration without a preset playerId.
          const registerMsg = { type: "REGISTER" };
          wsRef.current.send(JSON.stringify(registerMsg));
        };

        wsRef.current.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            console.log("Received:", data);

            if (data.type === "REGISTERED") {
              setPlayerId(data.playerId);
              console.log("Assigned playerId:", data.playerId);
            } else if (data.type === "TURN") {
              const currentIndex = parseInt(data.playerId.replace("player", ""), 10) - 1;
              setMessage(
                <div className="turn-message">
                  <span className="turn-highlight">
                    ➡️ It's {gameState.players[currentIndex].name}'s turn! ⬅️
                  </span>
                  {currentIndex === (playerId !== null ? playerId - 1 : -1) &&
                    <div className="your-turn-alert">YOUR TURN TO PLAY!</div>
                  }
                </div>
              );
              setGameState(prevState => ({
                ...prevState,
                currentPlayer: currentIndex
              }));
            } else if (data.type === "GAME_STATE") {
              // When game starts, update state and remove waiting message.
              setMessage(data.message || "Game has started!");
              setGameState(prevState => ({
                ...prevState,
                requiredCard: data.currentRound,
                round: prevState.round + 1,
                isGameStarted: true
              }));
            } else if (data.type === "DEAD") {
              const deadId = parseInt(data.playerId.replace("player", ""), 10) - 1;
              setMessage(`${gameState.players[deadId].name} was eliminated!`);
              setGameState(prevState => {
                const updatedPlayers = [...prevState.players];
                updatedPlayers[deadId].isAlive = false;
                return {
                  ...prevState,
                  players: updatedPlayers
                };
              });
            } else if (data.type === "HAND") {
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

    return () => clearTimeout(timer);
  }, []); // Empty dependency array: set up connection once on mount.

  const submitMove = () => {
    if (wsRef.current && playerId !== null && actualCount && fakeCount) {
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
    if (wsRef.current && playerId !== null) {
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

  const renderPlayers = () => {
    return gameState.players.map((player, index) => {
      const isCurrentPlayer = gameState.currentPlayer === index;
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
        <div key={index} style={playerStyle}>
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

  const renderPlayerHand = () => {
    return (
      <div className="player-hand">
        <h3>Your Hand</h3>
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
              <div style={{ marginTop: '5px', fontWeight: 'bold' }}>{card}</div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const getCardImage = (card) => {
    switch(card) {
      case 'A': return 'ace';
      case 'K': return 'king';
      case 'Q': return 'queen';
      case 'J': return 'joker';
      default: return 'card-back';
    }
  };

  const renderStatusMessage = () => {
    const displayMessage = gameState.requiredCard && message === "Waiting for game to start..."
      ? `Current Round: ${gameState.requiredCard}`
      : message;

    return (
      <div className="message-container">
        <div style={{ fontSize: '18px', fontWeight: 'bold' }}>
          {displayMessage}
        </div>
        {gameState.requiredCard && (
          <div className="current-round">
            <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>CURRENT ROUND</div>
            <div>
              <span>Card to play: </span>
              <span style={{ fontWeight: 'bold', fontSize: '1.8em', color: '#d4af37' }}>
                {gameState.requiredCard}
              </span>
              <img src={`/images/${getCardImage(gameState.requiredCard)}.png`} alt={gameState.requiredCard} style={{ width: '40px' }} />
            </div>
            <div>Round: {gameState.round}</div>
          </div>
        )}
      </div>
    );
  };

  const renderInputSection = () => {
    return (
      <div className="move-input">
        <div>
          {gameState.requiredCard && (
            <div style={{ marginBottom: '8px', fontSize: '16px', padding: '5px', background: 'rgba(0, 0, 255, 0.1)', borderRadius: '5px' }}>
              Enter the number of <span style={{ color: 'red', fontWeight: 'bold', fontSize: '18px' }}>{gameState.requiredCard}</span> cards:
            </div>
          )}
        </div>
        <div>
          <label style={{ fontSize: '0.9em', marginBottom: '3px', color: '#006400', fontWeight: 'bold' }}>
            ACTUAL count (truth):
          </label>
          <input
            type="number"
            placeholder="Actual Count"
            value={actualCount}
            onChange={(e) => setActualCount(e.target.value)}
          />
        </div>
        <div>
          <label style={{ fontSize: '0.9em', marginBottom: '3px', color: '#8B0000', fontWeight: 'bold' }}>
            FAKE count (what others see):
          </label>
          <input
            type="number"
            placeholder="Fake Count"
            value={fakeCount}
            onChange={(e) => setFakeCount(e.target.value)}
          />
        </div>
        <button onClick={submitMove}>Submit Move</button>
        <button onClick={callBluff}>Call Bluff</button>
      </div>
    );
  };

  return (
    <div className="game-container">
      {renderPlayers()}
      {renderStatusMessage()}
      <div className="table">
        {renderPlayerHand()}
        {renderInputSection()}
      </div>
    </div>
  );
};

export default App;
