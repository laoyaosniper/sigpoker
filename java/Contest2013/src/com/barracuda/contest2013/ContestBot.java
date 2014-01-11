/**
 * sample Java implementation for 2013 Barracuda Networks Programming Contest
 *
 */
package com.barracuda.contest2013;

import java.io.IOException;

public class ContestBot {
	private static final int RECONNECT_TIMEOUT = 15; // seconds

	private final String host;
	private final int port;
	private int game_id = -1;
	private int current_hand=-1;
	public ContestBot(String host, int port) {
		this.host = host;
		this.port = port;
	}

	private void run() {
		while (true) {
			// just reconnect upon any failure
			try {
				JsonSocket sock = new JsonSocket(host, port);
				try {
					sock.connect();
				}
				catch (IOException e) {
					throw new Exception("Error establishing connection to server: " + e.toString());
				}

				while (true) {
					Message message = sock.getMessage();
		
					PlayerMessage response = handleMessage(message);
		
					if (response != null) {
						sock.sendMessage(response);
					}
				}
			}
			catch (Exception e) {
				System.err.println("Error: " + e.toString());
				System.err.println("Reconnecting in " + RECONNECT_TIMEOUT + "s");
				try {
					Thread.sleep(RECONNECT_TIMEOUT * 1000);
				}
				catch (InterruptedException ex) {}
			}
		}
	}

	public PlayerMessage handleMessage(Message message) {
		if (message.type.equals("request")) {
			MoveMessage m = (MoveMessage)message;
			if (game_id != m.state.game_id) {
				game_id = m.state.game_id;
				System.out.println("new game " + game_id);
			}

			if (m.request.equals("request_card")) {
				if (! m.state.can_challenge || isChanllenge(m) == false) {
					int i = (int)(Math.random() * m.state.hand.length);
					return new PlayCardMessage(m.request_id, m.state.hand[i]);
				}
				else {
					return new OfferChallengeMessage(m.request_id);
				}
			}
			else if (m.request.equals("challenge_offered")) {
				/*return (Math.random() < 0.5)
						? new AcceptChallengeMessage(m.request_id)
						: new RejectChallengeMessage(m.request_id);
						*/
				if(acceptChallenge(m)){
					return new AcceptChallengeMessage(m.request_id);
				}
				else{
					return new RejectChallengeMessage(m.request_id);
				}
			}
		}
		else if (message.type.equals("result")) {
			ResultMessage r = (ResultMessage)message;
		}
		else if (message.type.equals("error")) {
			ErrorMessage e = (ErrorMessage)message;
			System.err.println("Error: " + e.message);

			// need to register IP address on the contest server
			if (e.seen_host != null) {
				System.exit(1);
			}
		}
		return null;
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java -jar ContestBot.jar <HOST> <PORT>");
			System.exit(1);
		}

		String host = args[0];
		Integer port = Integer.parseInt(args[1]);

		ContestBot cb = new ContestBot(host, port);
		cb.run();
	}
	public boolean isChanllenge(MoveMessage m){
		if(myHandQuality(m.state.hand,m.state.hand_id)>0){
			return true;
		}
		else if(m.state.their_points>8){
			return true;
		}
		return false;
	}
	public boolean haveLostHand(MoveMessage m){
		if((m.state.your_tricks<m.state.their_tricks)
				&&(Math.abs(m.state.their_tricks-m.state.your_tricks)>(5-m.state.total_tricks) )){
			return true;
		}
		return false;
	}
	public boolean haveWinHand(MoveMessage m){
		if((m.state.your_tricks>m.state.their_tricks)
				&&(Math.abs(m.state.their_tricks-m.state.your_tricks)>(5-m.state.total_tricks) )){
			return true;
		}
		return false;
	}
	public boolean acceptChallenge(MoveMessage m){
		if(haveWinHand(m)){
			return true;
		}
		else if(haveLostHand(m)){
			return false;
		}
		return false;
	}
	public int myHandQuality(int[] hand,int hid){
		for (int i=0;i<hand.length;i++){
			if(hand[i]==13){
				return 1;
			}
		}
		return 0;
	}
	public static int[] sort(int[] hand){
		int tmp;
		for(int i=0;i<hand.length;i++){
			for(int j=hand.length-1;j>i;j--){
				if(hand[j]>hand[j-1]){
					tmp = hand[j];
					hand[j] = hand[j-1];
					hand[j-1] = tmp;
				}
			}
		}
		return hand;
	}
	//called after sort();
	public int minBigger(int [] hand, int card){
		int index = -1;
		for(int i=0;i<hand.length;i++){
			if(hand[i]>card){
				index = i;
			}
		}
		return index;
	}
}
