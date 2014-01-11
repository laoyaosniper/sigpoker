package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public abstract class Message {
	public String type;

	@Override
	public abstract String toString();
	
	public abstract String sigPokerToString(HandStatus status);
}
