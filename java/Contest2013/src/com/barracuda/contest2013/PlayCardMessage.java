package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class PlayCardMessage extends PlayerMessage {
	public PlayCardMessage(int request_id, int card) {
		super(request_id);
		response = new Response("play_card", card);
	}

	@Override
	public String toString() {
		return "Play Card " + response.card + "\n";
	}

  /* (non-Javadoc)
   * @see com.barracuda.contest2013.Message#sigPokerToString(com.barracuda.contest2013.ContestBot.HandStatus)
   */
  @Override
  public String sigPokerToString(HandStatus status) {
    return "\tYou: " + response.card + ">>>>\n";
  }
}
