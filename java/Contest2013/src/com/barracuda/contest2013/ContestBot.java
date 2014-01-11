/**
 * sample Java implementation for 2013 Barracuda Networks Programming Contest
 *
 */
package com.barracuda.contest2013;

import java.io.IOException;

public class ContestBot {
	private static final int RECONNECT_TIMEOUT = 15; // seconds

	private boolean ENABLE_BOOST = true;
	private final String host;
	private final int port;
	private int game_id = -1;
	private DecisionMaker dm;
	private Status status;
	private boolean isMyTurn;
	public ContestBot(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	private int winTime = 0;
	private int loseTime = 0;
	private int tiedTime = 0;
	private int totalTime = 0;
	private int strategy = 1;
	public class DecisionMaker {
		void onReceiveResult(Status status, ResultMessage r){
			if ( r.result.type.equals("trick_won")) {
				if ( r.result.by == r.your_player_num ) winTime++;
				else loseTime++;
				
				totalTime++;
			}
			if ( r.result.type.equals("trick_tied")) {
				tiedTime++;
				totalTime++;
			}
			if ( r.result.type.equals("hand_done") ) {
				winTime = 0;
				loseTime = 0;
				totalTime = 0;
				tiedTime = 0;
				strategy = 0;
			}
		}
		int onReceiveRequest(Status status, MoveMessage m){
			int index = -1;
			int hand[] = m.state.hand;
			int their_card = m.state.card;
			sort(hand);
			if(their_card<=0){
				//in this round, I play first.
				index = secondBigger(hand);
				if ( tiedTime == 0 ) {
					if ( winTime == 1 && loseTime == 2)	index = 0;
				}
				else {
					if ( loseTime >= 1 ) index = 0;
				}
				
			}
			else{
				if((m.state.card>hand[hand.length-1])&&((m.state.card-hand[hand.length-1])>7)){
					index = hand.length-1;
				}
				else{
				index = minBigger(hand,m.state.card);
				}
				//in this round,they play first.
			}
			
			if ( strategy == 1 ) {
				if (totalTime == 1 ) {
					index = hand.length - 1;
				}
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

	public int wonGames = 0;
	public int totalGames = 0;

	public PlayerMessage handleMessage(Message message) {
		if (message.type.equals("request")) {
			MoveMessage m = (MoveMessage)message;
			if (game_id != m.state.game_id) {
				game_id = m.state.game_id;
//				System.out.println("new game " + game_id);
			}

//			System.out.println(m.toString());

			if (m.request.equals("request_card")) {
				if ( ! m.state.can_challenge || isChanllenge(m) == false || strategy == 1 && totalTime <=1) {
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
//				System.out.println(response.toString());
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
//				System.out.println(r.toString());
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
		
		if(haveLostHand(m)){
			return false;
		}
		
		if (haveWinHand(m)) {
			return true;
		}
//		if(isHandBig(m.state.hand)>0){
//			return true;
//		}
//		else if(m.state.their_points>8){
//			return true;
//		}
		
		return activeChallenge(m.state.hand, winTime, loseTime, m.state.their_points);
//		return true;
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
		return passiveChallenge(m.state.hand, winTime, loseTime, m.state.their_points, m.state.your_points);
//		return true;
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
	public boolean activeChallenge(int[] hand, int win, int lose, int theirPoint){
		// Check tie
		if ( hand.length + win + lose < 5) {
			System.out.println("Active: Found Tie!");
			return false;
		}
//		System.out.println("Active: Win:" + winTime + " Lose:" + loseTime + " Tied:" + tiedTime);

		double p = 0.0;
		double base = 0.4;
		if ( theirPoint >= 9 ) {
			base = 1.0;
		}
		sort(hand);
		if(win==0 && lose==0){
			int sum = hand[0] + hand[1] + hand[2];
			int boulder = 30;
			if ( hand[0] >= 9 && hand[1] >= 9 && hand[2] >= 9
				&& sum >= boulder) {
				base = 0.4;
				p = base + (sum - boulder) * 0.20; 
				
				if ( p >= 1.0 ) strategy = 1;
			}
		}
		else if ( win == 1 && lose == 0 ) {
			int sum = hand[0] + hand[1];
			int boulder = 20;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.1;
			}
		}
		else if ( win == 0 && lose == 1 ) {
			int sum = hand[0] + hand[1] + hand[2];
			int boulder = 27;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.1;
			}
		}
		else if ( win == 2 && lose == 0 ) {
			int sum = hand[0];
			int boulder = 5;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.2;
			}
		}
		else if ( win == 0 && lose == 2 ) {
			int sum = hand[0] + hand[1] + hand[2];
			int boulder = 30;
			if ( sum >= boulder ) {
				base = 0.8;
				p = base + (sum - boulder) * 0.2;
				
			}
		}
		else if ( win == 1 && lose == 1) {
			int sum = hand[0] + hand[1];
			int boulder = 18;
			if ( sum >= boulder ) {
				base = 0.4;
				p = base + (sum - boulder) * 0.1;
			}
		}
		else if ( win == 2 && lose == 1) {
			int sum = hand[0];
			int boulder = 9;
			if ( sum >= boulder ) {
				base = 0.6;
				p = base + (sum - boulder) * 0.15;
			}
		}
		else if ( win == 1 && lose == 2) {
			int sum = hand[0] + hand[1];
			int boulder = 20;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.2;
			}
		}
		else if ( win == 2 && lose == 2) {
			int sum = hand[0];
			int boulder = 11;
			if ( sum >= boulder ) {
				base = 0.6;
				p = base + (sum - boulder) * 0.4;
			}
		}
		
		return (p > Math.random()) ? true : false;
		
	}

	public boolean passiveChallenge(int[] hand, int win, int lose, int theirPoint, int ourPoint){
		// Check tie
		if ( hand.length + win + lose < 5) {
			System.out.println("Passive: Found Tie!");
			return false;
		}

//		System.out.println("Passive: Win:" + winTime + " Lose:" + loseTime + " Tied:" + tiedTime);
		double p = 0.0;
		double base = 0.4;
		if ( theirPoint >= 9 ) {
			base = 1.0;
		}
		sort(hand);
		if(win==0 && lose==0){
			int sum = hand[0] + hand[1] + hand[2];
			if ( ourPoint < 9 ) {
				int boulder = 30;
				if ( hand[0] >= 9 && hand[1] >= 9 && hand[2] >= 9
						&& sum >= boulder) {
					base = 0.4;
					p = base + (sum - boulder) * 0.1; 
					p = p * 0.9;
				}
			}
			else if ( ourPoint == 9 ) {
				int boulder = 27;

				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
		}
		else if ( win == 1 && lose == 0 ) {
			int sum = hand[0] + hand[1];
			int boulder = 20;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.1;
				
				p = p*1.1;
			}
		}
		else if ( win == 0 && lose == 1 ) {
			int sum = hand[0] + hand[1] + hand[2];
			int boulder = 27;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.1;
				
				p = p*0.9;
			}
		}
		else if ( win == 2 && lose == 0 ) {
			int sum = hand[0];
			int boulder = 5;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.2;
			}
		}
		else if ( win == 0 && lose == 2 ) {
			int sum = hand[0] + hand[1] + hand[2];
			int boulder = 30;
			if ( sum >= boulder ) {
				base = 0.8;
				p = base + (sum - boulder) * 0.2;
				
			}
		}
		else if ( win == 1 && lose == 1) {
			int sum = hand[0] + hand[1];
			int boulder = 18;
			if ( sum >= boulder ) {
				base = 0.4;
				p = base + (sum - boulder) * 0.1;
			}
		}
		else if ( win == 2 && lose == 1) {
			int sum = hand[0];
			int boulder = 9;
			if ( sum >= boulder ) {
				base = 0.6;
				p = base + (sum - boulder) * 0.15;
				
				p = p*1.1;
			}
		}
		else if ( win == 1 && lose == 2) {
			int sum = hand[0] + hand[1];
			int boulder = 20;
			if ( sum >= boulder ) {
				base = 0.5;
				p = base + (sum - boulder) * 0.2;
				
				p = p*0.9;
			}
		}
		else if ( win == 2 && lose == 2) {
			int sum = hand[0];
			int boulder = 11;
			if ( sum >= boulder ) {
				base = 0.6;
				p = base + (sum - boulder) * 0.4;
			}
		}
		
		return (p > Math.random()) ? true : false; 
	}
}
