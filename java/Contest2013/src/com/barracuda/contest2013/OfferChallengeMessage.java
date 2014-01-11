package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class OfferChallengeMessage extends PlayerMessage {

	public OfferChallengeMessage(int request_id) {
		super(request_id);
		response = new Response("offer_challenge");
	}

	@Override
	public String toString() {
		return "Offer Challenge\n";
	}

  /* (non-Javadoc)
   * @see com.barracuda.contest2013.Message#sigPokerToString(com.barracuda.contest2013.ContestBot.HandStatus)
   */
  @Override
  public String sigPokerToString(HandStatus status) {
    return "\tThey Offer Challenge!\n";
  }
}
