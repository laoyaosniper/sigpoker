package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class ResultMessage extends Message {
	public Result result;
	public int your_player_num;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Result Message:\n");
		sb.append(result.toString());
		sb.append("\tyour_player_num: " + your_player_num + "\n");
		return sb.toString();
	}
	
	@Override
	public String sigPokerToString(HandStatus status) {
	  if ( result.type.equals("accepted") ) {
	    return "";
	  }
      StringBuilder sb = new StringBuilder(result.type + "\n");
      if ( status == HandStatus.ISSUE ) {
        if ( result.card != null )
          sb.append("\t     " + "<<<<<" + result.card + " They\n");
      }
      if ( result.by != null )
        if ( result.by == your_player_num ) {
          sb.append("\tYou Wins!\n");
        }
        else {
          sb.append("\tYou Lose!\n");
        }
      return sb.toString();
	}
}
