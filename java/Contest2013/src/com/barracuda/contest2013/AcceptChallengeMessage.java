package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class AcceptChallengeMessage extends PlayerMessage {

	public AcceptChallengeMessage(int request_id) {
		super(request_id);
		response = new Response("accept_challenge");
	}

	@Override
	public String toString() {
		return "Accept Challenge\n";
	}

  /* (non-Javadoc)
   * @see com.barracuda.contest2013.Message#sigPokerToString(com.barracuda.contest2013.ContestBot.HandStatus)
   */
  @Override
  public String sigPokerToString(HandStatus status) {
    return "Accept Challenge\n";
  }
}
