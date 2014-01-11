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
//	private boolean isMyTurn;
	public ContestBot(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	private int winTime = 0;
	private int loseTime = 0;
	private int tiedTime = 0;
	private int totalTime = 0;
//	private int strategy = 1;
	private int myLastCard = -1;
	private int theirLastCard = -1;
	public class DecisionMaker {
		void onReceiveResult(Status status, ResultMessage r){
			if ( r.result.type.equals("trick_won")) {
				if ( r.result.by == r.your_player_num ) winTime++;
				else loseTime++;
				theirLastCard = r.result.card;
				totalTime++;
			}
			if ( r.result.type.equals("trick_tied")) {
				tiedTime++;
				theirLastCard = myLastCard;
				totalTime++;
			}
			if ( r.result.type.equals("hand_done") ) {
				winTime = 0;
				loseTime = 0;
				totalTime = 0;
				tiedTime = 0;
//				strategy = 0;
				myLastCard = -1;
				theirLastCard = -1;
			}
		}
		int onReceiveRequest(Status status, MoveMessage m){
			if(m.state.total_tricks==0&&m.state.in_challenge==false&&m.state.card>0){
				return 9999;
			}
			int index = -1;
			int hand[] = m.state.hand;
			int their_card = m.state.card;
			sort(hand);
			if(their_card<=0){
				//in this round, I play first.
				if (totalTime == 0 ) {
					int idx=-1;
					for(int i=0;i<hand.length;i++){
						if(hand[i]<10){
							idx = i;
							break;
						}
					}
					index = 2>idx?2:idx;
				}
				else if(totalTime == 1){
					index = 2;
				}
				else {
					index = secondBigger(hand);
				}
				
				if ( tiedTime == 0 ) {
					if ( winTime == 1 && loseTime == 2)	index = 0;
				}
				else {
					if ( loseTime >= 1 ) index = 0;
				}
				
			}
			else{
				theirLastCard = their_card;
				if((m.state.card>hand[hand.length-1])&&((m.state.card-hand[hand.length-1])>6)){
					index = hand.length-1;
				}
				else if((findCard(hand,m.state.card)==hand.length-1||
						findCard(hand,m.state.card)==hand.length-2)
						&&(m.state.card<=4)){
					index = findCard(hand,m.state.card);
				}
				else{
				index = minBigger(hand,m.state.card);
				}
				//in this round,they play first.
			}
			
//			if ( m.state.can_challenge && strategy == 1 ) {
//				if (totalTime == 1 || totalTime == 0) {
//					index = hand.length - 1;
//				}
//			}
			if ( index < 0 || index >= hand.length ) {
				System.err.println("W:" + winTime + " L:" + loseTime + " T:" + tiedTime);
				System.err.println("Hand: ");
				for ( int x : hand ) {
					System.err.print(x + " ");
				}
				
				System.err.println("\nIndex:" + index + "\n");
			}
			myLastCard = hand[index];
			//return hand[0];
			return index;
		}
	}
	private int findCard(int[] hand, int card){
		int index = -3;
		for(int i=0;i<hand.length;i++){
			if(hand[i]==card){
				index = i;
				break;	
			}
		}
		return index;
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
				e.printStackTrace();
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
				boolean shouldPlay = (! m.state.can_challenge || isChanllenge(m) == false);
//				shouldPlay = shouldPlay || (strategy == 1 && totalTime <=1);
				if ( shouldPlay ) {
					//int i = (int)(Math.random() * m.state.hand.length);
					int i = dm.onReceiveRequest(status, m);
					//////////////
					if(i==9999){
					OfferChallengeMessage challenge = new OfferChallengeMessage(m.request_id);
					return challenge;
					}
					//////////////
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
		
		return activeChallenge(m.state.hand, winTime, loseTime, m.state.their_points, m.state.your_points);
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
	public boolean activeChallenge(int[] hand, int win, int lose, int theirPoint, int ourPoint){
//		System.out.println("Active: Win:" + winTime + " Lose:" + loseTime + " Tied:" + tiedTime);

		double p = 0.0;
		double base = 0.4;
		if ( theirPoint >= 9 ) {
			return true;
		}
		if ( ourPoint >= 9 ) {
			return false;
		}

		if ( theirPoint >= 7 && ourPoint <=5 ) {
			return true;
		}
		if ( theirPoint >= 8 && ourPoint <=7 ) {
			return true;
		}
		
		sort(hand);
		if ( tiedTime == 0 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 33;
				if ( hand[0] >= 10 && hand[1] >= 10 && hand[2] >= 10
						&& sum >= boulder) {
					base = 0.4;
					p = base + (sum - boulder) * 0.20; 

//					if ( p >= 1.0 ) strategy = 1;
				}
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0] + hand[1];
				int boulder = 20;
				int decrement = 0;
				if ( theirLastCard > 0 && theirLastCard < 6 ) {
					decrement  = 6 - theirLastCard;
				}
				if ( sum - decrement >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				if ( hand.length != 4) {
					System.err.println("W:" + winTime + " L:" + loseTime + " T:" + tiedTime + "\n");
					System.err.println("Hand: ");
					for ( int x : hand ) {
						System.err.print(x + " ");
					}
				}
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 27;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 2 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				int boulder = 5;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.2;
				}
			}
			else if ( win == 0 && lose == 2 ) {
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 33;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.2;

				}
			}
			else if ( win == 1 && lose == 1) {
				int sum = hand[0] + hand[1];
				int boulder = 22;
				if ( sum >= boulder ) {
					base = 0.4;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 2 && lose == 1) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				else {
					return false;
				}
			}
			else if ( win == 1 && lose == 2) {
				if ( hand.length != 2) {
					System.err.println("W:" + winTime + " L:" + loseTime + " T:" + tiedTime + "\n");
					System.err.println("Hand: ");
					for ( int x : hand ) {
						System.err.print(x + " ");
					}
				}
				int sum = hand[0] + hand[1];
				int boulder = 24;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.2;
				}
			}
			else if ( win == 2 && lose == 2) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				int boulder = 11;
				if ( sum >= boulder ) {
					base = 0.6;
					p = base + (sum - boulder) * 0.4;
				}
			}
		}
		else if ( tiedTime == 1 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 32;
				if ( sum >= boulder) {
					base = 0.4;
					p = base + (sum - boulder) * 0.20; 

//					if ( p >= 1.0 ) strategy = 1;
				}
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				int boulder = 11;
				int decrement = 0;
				if ( theirLastCard > 0 && theirLastCard < 6 ) {
					decrement  = 6 - theirLastCard;
				}
				if ( sum - decrement >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 30;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 1 && lose == 1) {
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 2) {
				return true;
			}
			else if ( lose == 2 ) {
				return false;
			}
		}
		else if ( tiedTime == 2 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder) {
					base = 0.8;
					p = base + (sum - boulder) * 0.10; 
				}
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				int boulder = 11;
				int decrement = 0;
				if ( theirLastCard > 0 && theirLastCard < 6 ) {
					decrement  = 6 - theirLastCard;
				}
				if ( sum - decrement >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				int sum = hand[0] + hand[1];
				int boulder = 34;
				if ( sum >= boulder ) {
					return true;
				}
			}
			else if ( win == 1 && lose == 1) {
				return false;
			}
		}
		else if ( tiedTime == 3 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder) {
					base = 0.8;
					p = base + (sum - boulder) * 0.10; 
				}
			}
			else if ( win == 1 && lose == 0 ) {
				return true;
			}
			else if ( win == 0 && lose == 1 ) {
				return false;
			}
		}
		else if ( tiedTime == 4 ) {
			if( win == 0 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) return true;
				else return false;
			}
		}
		
		return (p > Math.random()) ? true : false;
		
	}

	public boolean passiveChallenge(int[] hand, int win, int lose, int theirPoint, int ourPoint){
//		System.out.println("Passive: Win:" + winTime + " Lose:" + loseTime + " Tied:" + tiedTime);
		double p = 0.0;
		double base = 0.4;
		
//		return true;
		if ( theirPoint >= 9 ) {
			return true;
		}
		sort(hand);
		if ( tiedTime == 0 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1] + hand[2];
				if ( ourPoint < 9 ) {
					int boulder = 33;
					if ( hand[0] >= 10 && hand[1] >= 10 && hand[2] >= 10
							&& sum >= boulder) {
						base = 0.4;
						p = base + (sum - boulder) * 0.1; 
						p = p * 0.8;
					}
				}
				else if ( ourPoint == 9 ) {
					int boulder = 30;

					if ( hand[0] >= 10 && hand[1] >= 10 && hand[2] >= 10 ) {
						base = 0.4;
						p = base + (sum - boulder) * 0.1;
					}
					if ( hand[0] >= 13 && hand[1] >= 13 && hand[3] >= 7 ) {
						boulder = 33;
						base = 0.4;
						p = base + (sum - boulder) * 0.2;
					}
				}
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;

					p = p*1.1;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 35;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.1;

					p = p*0.9;
				}
			}
			else if ( win == 1 && lose == 1) {
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder ) {
					base = 0.7;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 2 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				int boulder = 5;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.2;
				}
			}
			else if ( win == 0 && lose == 2 ) {
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 36;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.1;

				}
			}
			else if ( win == 2 && lose == 1) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				
				int boulder = 11;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.25;
				}
			}
			else if ( win == 1 && lose == 2) {
				int sum = hand[0] + hand[1];
				int boulder = 23;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.2;

					p = p*0.9;
				}
			}
			else if ( win == 2 && lose == 2) {
				int sum = hand[0];
				if ( sum == 13 ) {	// must win
					return true;
				}
				else {
					return false;
				}
			}
		} 
		else if ( tiedTime == 1 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1] + hand[2];
				if ( ourPoint < 9 ) {
					int boulder = 30;
					if ( sum >= boulder) {
						base = 0.4;
						p = base + (sum - boulder) * 0.1; 
						p = p * 0.8;
					}
				}

				else if ( ourPoint == 9 ) {
					int boulder = 30;

					if ( hand[0] >= 10 && hand[1] >= 10 && hand[2] >= 10 ) {
						base = 0.4;
						p = base + (sum - boulder) * 0.1;
					}
					if ( hand[0] >= 13 && hand[1] >= 13 && hand[3] >= 7 ) {
						boulder = 33;
						base = 0.4;
						p = base + (sum - boulder) * 0.2;
					}
				}
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0] + hand[1];
				int boulder = 22;
				if ( sum >= boulder ) {
					base = 0.5;
					p = base + (sum - boulder) * 0.1;

					p = p*1.1;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				int sum = hand[0] + hand[1] + hand[2];
				int boulder = 33;
				if ( sum >= boulder ) {
					base = 0.7;
					p = base + (sum - boulder) * 0.1;

					p = p*0.9;
				}
			}
			else if ( win == 1 && lose == 1) {
				int sum = hand[0] + hand[1];
				int boulder = 24;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.1;
				}
			}
			else if ( win == 2 ) {
				return true;
			}
			else if ( lose == 2 ) {
				return false;
			}
		}
		else if ( tiedTime == 2 ) {
			if(win==0 && lose==0){
				int sum = hand[0] + hand[1];
				int boulder = 23;
				
				if (ourPoint >= 9) {
					if ( hand[0] >= 10 && hand[1] >= 10 ) {
						base = 0.5;
						p = base + (sum - boulder) * 0.1;
					}
				}
				else {
					if ( sum >= boulder) {
						base = 0.7;
						p = base + (sum - boulder) * 0.1; 
						p = p * 0.8;
					}
				}

				if ( ourPoint < 9 ) {
					if ( sum >= boulder) {
						base = 0.7;
						p = base + (sum - boulder) * 0.1; 
						p = p * 0.8;
					}
				}
				else if ( ourPoint == 9 ) {
					if ( hand[0] >= 13 && hand[1] >= 10 ) {
						boulder = 33;
						base = 0.4;
						p = base + (sum - boulder) * 0.2;
					}
				}
				
			}
			else if ( win == 1 && lose == 0 ) {
				int sum = hand[0];
				if ( sum == 13 ) {
					return true;
				}
				else {
					return false;
				}
			}
			else if ( win == 0 && lose == 1 ) {
				int sum = hand[0] + hand[1];
				int boulder = 36;
				if ( sum >= boulder ) {
					base = 0.8;
					p = base + (sum - boulder) * 0.1;

					p = p*0.9;
				}
			}
			else if ( win == 1 && lose == 1) {
				int sum = hand[0];
				if ( sum == 13 ) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		else if ( tiedTime == 3 ) {
			int sum = hand[0];
			if ( sum == 13 ) return true;
			else return false;
		}
		return (p > Math.random()) ? true : false; 
	}
}
