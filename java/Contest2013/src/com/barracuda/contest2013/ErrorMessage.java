package com.barracuda.contest2013;

import com.barracuda.contest2013.ContestBot.HandStatus;

public class ErrorMessage extends Message {
	String message;
	String seen_host;

	@Override
	public String toString() {
		return "Error: " + message + "\n";
	}

	@Override
	public String sigPokerToString(HandStatus status) {
		// TODO Auto-generated method stub
		return null;
	}
}
