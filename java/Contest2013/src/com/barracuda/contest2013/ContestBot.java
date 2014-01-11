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

	public enum HandStatus {
	  ISSUE, RESPONSE, RESULT
	}
	
	public HandStatus myStatus;
	
	public PlayerMessage handleMessage(Message message) {
		if (message.type.equals("request")) {
			MoveMessage m = (MoveMessage)message;
			if (game_id != m.state.game_id) {
				game_id = m.state.game_id;
				System.out.println("new game " + game_id);
			}
			
            System.out.println(m.toString());
            
			if (m.request.equals("request_card")) {
	            if ( m.state.card > 0 ) {
	              myStatus = HandStatus.RESPONSE;
	            }
	            else {
	              myStatus = HandStatus.ISSUE;
	            }
	            System.out.println(m.sigPokerToString(myStatus));
	            
				if (! m.state.can_challenge || Math.random() < 0.8) {
					int i = (int)(Math.random() * m.state.hand.length);
					PlayCardMessage card = new PlayCardMessage(m.request_id, m.state.hand[i]);
//					System.out.println(card.sigPokerToString(myStatus));
					System.out.println(card.toString());
					return card;
				}
				else {
					OfferChallengeMessage challenge = new OfferChallengeMessage(m.request_id);
//					System.out.println(challenge.sigPokerToString(myStatus));
					System.out.println(challenge.toString());
					return challenge;
				}
			}
			else if (m.request.equals("challenge_offered")) {
			  PlayerMessage response;
			  if ( Math.random() < 0.5 ) {
			    response = new AcceptChallengeMessage(m.request_id);
			  }
			  else {
			    response = new RejectChallengeMessage(m.request_id);
			  }
//			  System.out.println(response.sigPokerToString(myStatus));
			  System.out.println(response.toString());
			  return response;
//				return (Math.random() < 0.5)
//						? new AcceptChallengeMessage(m.request_id)
//						: new RejectChallengeMessage(m.request_id);
			}
		}
		else if (message.type.equals("result")) {
		  
			ResultMessage r = (ResultMessage)message;
//	        System.out.println(r.sigPokerToString(myStatus));
			System.out.println(r.toString());
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
}
