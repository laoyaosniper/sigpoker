package com.barracuda.contest2013;

import java.util.Arrays;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class MoveMessage extends Message {
	public String request;
	public GameState state;
	int request_id;
	float remaining;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Move Message:\n");
		sb.append("\trequest: " + request + "\n");
		sb.append("\trequest_id: " + request_id + "\n");
		sb.append("\tremaining: " + remaining + "\n");
		sb.append(state);
		return sb.toString();
	}
	
	@Override
	public String sigPokerToString(HandStatus status) {
	  StringBuilder sb = new StringBuilder("Move Message: ");
      sb.append("\tGame " + state.game_id
        + ", Hand " + state.hand_id
        + "\n\tTricks " + state.your_tricks + ":" + state.their_tricks
        + "\tPoints " + state.your_points + ":" + state.their_points + "\n");
      sb.append("\tTime Remaining: " + remaining + "\n");
      sb.append("\thand: " + Arrays.toString(state.hand) + "\n");
	  if ( status == HandStatus.RESPONSE ) {
	    sb.append("\t     " + "<<<<<" + state.card + " They\n");
	  }
      return sb.toString();
	}
}
