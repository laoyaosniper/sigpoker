package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class RejectChallengeMessage extends PlayerMessage {

	public RejectChallengeMessage(int request_id) {
		super(request_id);
		response = new Response("reject_challenge");
	}

	@Override
	public String toString() {
		return "Reject Challenge\n";
	}

  /* (non-Javadoc)
   * @see com.barracuda.contest2013.Message#sigPokerToString(com.barracuda.contest2013.ContestBot.HandStatus)
   */
  @Override
  public String sigPokerToString(HandStatus status) {
    return "Reject Challenge\n";
  }
}
