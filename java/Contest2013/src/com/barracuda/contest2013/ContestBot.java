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
	private DecisionMaker dm;
	private Status status;
	private boolean isMyTurn;
	private boolean chanllengeNextTime=false;
	public ContestBot(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	
	public class DecisionMaker {
		void onReceiveResult(Status status, ResultMessage r){
			if(isMyTurn&&(r.result.by==r.your_player_num)){
				chanllengeNextTime = true;
			}
		}
		int onReceiveRequest(Status status, MoveMessage m){
			int index = -1;
			int hand[] = m.state.hand;
			int their_card = m.state.card;
			sort(hand);
			if(their_card<=0){
				//in this round, I play first.
				isMyTurn = true;
				index = secondBigger(hand);
			}
			else{
				isMyTurn = false;
				index = minBigger(hand,m.state.card);
				//in this round,they play first.
			}
			return index;
		}
	}
	
	
	private void run() {
		dm = new DecisionMaker();
		status = new Status();
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

	public enum HandStatus {
		ISSUE, RESPONSE, RESULT
	}

	public HandStatus myStatus;

	public int wonGames = 0;
	public int totalGames = 0;

	public PlayerMessage handleMessage(Message message) {
		if (message.type.equals("request")) {
			MoveMessage m = (MoveMessage)message;
			if (game_id != m.state.game_id) {
				game_id = m.state.game_id;
				System.out.println("new game " + game_id);
			}

//			System.out.println(m.toString());

			if (m.request.equals("request_card")) {
				if (! m.state.can_challenge || isChanllenge(m) == false) {
					//int i = (int)(Math.random() * m.state.hand.length);
					int i = dm.onReceiveRequest(status, m);
					int[] hand = m.state.hand;
					sort(hand);
					PlayCardMessage card = new PlayCardMessage(m.request_id,hand[i]);
//					System.out.println(card.toString());
					return card;
				}
				else {
					OfferChallengeMessage challenge = new OfferChallengeMessage(m.request_id);
//					System.out.println(challenge.toString());
					return challenge;
				}
			}
			else if (m.request.equals("challenge_offered")) {
				PlayerMessage response;
				if(acceptChallenge(m)){
					response = new AcceptChallengeMessage(m.request_id);
				}
				else{
					response = new RejectChallengeMessage(m.request_id);
				}
				//System.out.println(response.toString());
				return response;
			}
		}
		else if (message.type.equals("result")) {
			ResultMessage r = (ResultMessage)message;
			if ( r.result.type.equals("game_won") ) {
				if ( r.result.by == r.your_player_num ) wonGames++;
				totalGames++;
				System.out.println("Won ratio: " + (double)wonGames/totalGames);
			}
				//System.out.println(r.toString());
			dm.onReceiveResult(status, r);
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
		if(chanllengeNextTime){
			chanllengeNextTime=false;
			return true;
		}
		if(haveLostHand(m)){
			return false;
		}
		if(isHandBig(m.state.hand)>0){
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
		else if(isHandBig(m.state.hand)==1){
			return true;
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
	public int isHandBig(int[] hand){
		int sum = 0;
		for(int i=0;i<hand.length;i++){
			sum+=hand[i];
		}
		if((sum/hand.length)>=8){
			return 1;
		}
		return 0;
	}
	public  int[] sort(int[] hand){
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
		if(index>=0){
			return index;
		}
		return hand.length-1;
	}
	//called after sort();
	public int secondBigger(int [] hand){
		if(hand.length==0){
			return -1;
		}
		if(hand.length<=2){
			return 0;
		}
		else{
			return 1;
		}
	}
}
