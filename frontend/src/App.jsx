import React, { useState, useEffect, useRef } from 'react';
import './style.css';

const App = () => {
  const [msg, setMsg] = useState("Need to play a: PLACEHOLDER BALLS");
  const [actualCount, setActualCount] = useState("");
  const [fakeCount, setFakeCount] = useState("");
  const wsRef = useRef(null);
  const playerId = 1; // Hardcoded player ID for now

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!wsRef.current) {
        wsRef.current = new WebSocket("ws://localhost:8082");
        wsRef.current.onopen = () => console.log("Connected to WebSocket");
        wsRef.current.onmessage = (event) => {
          const data = JSON.parse(event.data);
          console.log("Received:", data);
          if (data.type === "TURN") {
            setMsg(`Player turn: ${data.playerId}`);
          } else if (data.type === "GAME_STATE") {
            setMsg(data.message);
          } else if (data.type === "DEAD") {
            setMsg(`Player eliminated: ${data.playerId}`);
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
      setMsg("Move submitted!");
    }
  };

  const callBluff = () => {
    if (wsRef.current) {
      const bluffMsg = {
        type: "BLUFF",
        playerId
      };
      wsRef.current.send(JSON.stringify(bluffMsg));
      setMsg("Bluff called!");
    }
  };

  return (
    <div className="game-container">
      <div className="player top" id="player1">Player {playerId}</div>
      <div className="message">{msg}</div>
      <div className="table">
        <div className="deck">
          <img src="/images/ace.png" alt="Ace Card" width="50" />
          <img src="/images/king.png" alt="King Card" width="50" />
          <img src="/images/queen.png" alt="Queen Card" width="50" />
          <img src="/images/joker.png" alt="Joker Card" width="50" />
        </div>
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
        </div>
        <button className="start-btn" onClick={callBluff}>Call Bluff</button>
      </div>
      <div className="players-row">
        <div className="player left" id="player2">Player 2</div>
        <div className="player right" id="player3">Player 3</div>
      </div>
      <div className="player bottom" id="player4">Player 4</div>
    </div>
  );
};

export default App;
