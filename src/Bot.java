import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList; 
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class Bot {	
	/* The pause time will be between min and max wait times */ 
	private long minTimeBetweenCycles;  
	private long maxTimeBetweenCycles; 
	private long maxTweetsPerCycle; 
	
	private ArrayList<String> queries; 
	private ArrayList<String> responses; 
	private HashSet<String> responseIDs; 
	private OAuthSettings settings; 
	
	/**
	 * @param pathToConfigFile
	 */
	public Bot(String pathToConfigFile) {
		queries = new ArrayList<String>(); 
		responses = new ArrayList<String>(); 
		responseIDs = new HashSet<String>(); 
		
		loadConfigFile(pathToConfigFile); 
	}

	/**
	 * This method handles running the bot, which will schedule
	 * the Twitter queries and replies and allow the user to 
	 * shutdown the bot at any time. Although, if the program is 
	 * currently in a cycle it will finish before shutting down. 
	 */
	public synchronized void run() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
		Scanner input = new Scanner(System.in); 
		Executer ex = new Executer(); 
		Thread t = new Thread(ex); 
		
		System.out.println("Instructions: \n" +
		           "[1] Enter \"kill\", \"stop\", or \"shutdown\" at any time to stop the bot.\n");
		
		System.out.printf("[%s] Starting Bot\n", dateFormatter.format(Calendar.getInstance().getTime()));
		t.start(); 
		
		while(ex.running) { 
			String cm = input.nextLine().toLowerCase(); 
			if(cm.equals("kill") || cm.equals("stop") || cm.equals("shutdown")) { 
				ex.running = false; 
				
				if(ex.processingQuery) { 
					System.out.printf("[%s] Currently processing query; Bot will commence shutdown when finished.\n", 
							          dateFormatter.format(Calendar.getInstance().getTime()));
				} else { 
					synchronized(ex) { 
						ex.notify(); 
					}
				}
				System.out.printf("[%s] Shutting Down Bot.\n", dateFormatter.format(Calendar.getInstance().getTime()));	
			}
		}
	}
	
	/** 
	 * This method will pick a random query string, analyzes the results
	 * and respond to them if they match the query. This method will never post 
	 * more than maxTweetsPerCycle, but it could post less depending on
	 * the results. 
	 */
	
	private void queryAnalyzePost() { 
		
		Random r = new Random(); 
		
		/* Choose random query */ 
		String query = cleanupString(queries.get(r.nextInt(queries.size()))); 
		
		try {
			ArrayList<TweetData> data = parseJSON(TwitterInteraction.twitterQuery(query));
			
			int status = 0, count = 0; 
			for(TweetData d : data) {
				if(cleanupString(d.getText()).contains(query)) { 
					
					/* Check if this tweet was already responded to */ 
					if(!responseIDs.contains(d.getReplyID())) { 
						d.setText("@" + d.getUserID() + " " + responses.get(r.nextInt(responses.size())));
						status = TwitterInteraction.twitterPost(d, settings);
						
						if(status == 200) {
							System.out.println("Posted Tweet: " + d.getText());
							responseIDs.add(d.getReplyID()); 
							
							if(++count >= maxTweetsPerCycle) return; 
						} else { 
							System.out.println("Twitter Post Failed: Status = " + status);
						}
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println("Error: Query/Response string could not be encoded with UTF-8");
		} 
	}

	/**
	 * This method parses JSON with JSON-simple framework.
	 *  
	 * @param json: Full JSON object in string form. 
	 * @return Returns a list of TweetData. 
	 */
	private static ArrayList<TweetData> parseJSON(String json) { 
		JSONObject source = (JSONObject) JSONValue.parse(json);
		JSONArray array = (JSONArray) source.get("results"); 
		
		ArrayList<TweetData> tweets = new ArrayList<TweetData>(); 
		if(array != null) { 
			for(Object o : array) { 
				JSONObject j = (JSONObject) o; 
				tweets.add(new TweetData((String)j.get("text"), (String)j.get("from_user"), (String)j.get("id_str"))); 
			}
		}
		
		return tweets; 
	}
	
	/* Helper method to remove unnecessary portions of a Tweet */ 
	private String cleanupString(String s) {
		s = s.replaceAll("#(\\w*)(\\s+)", ""); //HashTag
		s = s.replaceAll("@(\\w*)(\\s+)", ""); //UserID
		s = s.replaceAll("[^\\w\\s]", "");     //Punctuation
		s = s.replaceAll("\\s+", " ");         //Convert all white space to a single space
		s = s.toLowerCase(); 
		
		return s;  
	}

	/**
	 * Config file must contain the following variables in JSON format. Example: 
	 * { 
	 * 		"min_time_between_cycles":<LONG>,
	 * 		"max_time_between_cycles":<LONG>, 
	 * 
	 * 		"queries": [<Query Strings>],
	 * 		"responses:[<Response Strings>],
	 * 
	 * 		"consumer_key":      "<OAuth Consumer Key>",
	 * 		"consumer_secret":   "<OAuth Consumer Secret>",
	 * 		"access_token":      "<OAuth Access Token>",
	 * 		"access_token_secret:"<OAuth Access Secret>" 
	 * }
	 * 
	 * @param pathToConfigFile
	 */
	private void loadConfigFile(String pathToConfigFile) { 
		try {
			FileReader reader = new FileReader(new File(pathToConfigFile));
			JSONObject source = (JSONObject) JSONValue.parse(reader); 
			
			if(source == null) { 
				System.err.println("Error in Config: No JSON object found.");
				System.exit(1); 
			}
			
			/* OAuth Settings */ 
			String cKey = (String) source.get("consumer_key"); 
			String cSecret = (String) source.get("consumer_secret"); 
			String aToken = (String) source.get("access_token"); 
			String aSecret = (String) source.get("access_token_secret"); 
			
			if(cKey == null || cSecret == null || aToken == null || aSecret == null) { 
				System.err.println("Error in Config: Missing OAuth information.");
				System.exit(1); 
			}
			settings = new OAuthSettings(cKey, cSecret, aToken, aSecret); 
		
			/* Timer settings */ 
			Long minTBC = (Long) source.get("min_time_between_cycles"); 
			Long maxTBC = (Long) source.get("max_time_between_cycles");
			
			if(minTBC == null || maxTBC == null) { 
				System.err.println("Error in Config: Missing variable(s): min_time_between_cycles/max_time_between_cylces.");
				System.exit(1); 
			}
			
			minTimeBetweenCycles = minTBC; 
			maxTimeBetweenCycles = maxTBC;
			
			if(maxTimeBetweenCycles - minTimeBetweenCycles < 1) { 
				System.err.println("Error in Config: max_time_between_cylces must be larger than min_time_between_cycles.");
				System.exit(1); 
			}
			
			/* Cycle count setting */ 
			Long mtpc = (Long) source.get("max_tweets_per_cycle");
			
			if(mtpc == null) { 
				System.err.println("Error in Config: Missing Variable: max_tweets_per_cycle");
				System.exit(1); 
			}
			
			maxTweetsPerCycle = mtpc; 
			if(maxTweetsPerCycle < 1) { 
				System.err.println("Error in Config: max_tweets_per_cycle must be greater than 0");
				System.exit(1); 
			}
			
			/* Get queries and responses and convert to string arrays */
			JSONArray queries = (JSONArray) source.get("queries"); 
			JSONArray responses = (JSONArray) source.get("responses"); 
			
			
			if(queries == null || responses == null) { 
				System.err.println("Error in Config: Missing query and/or response array");
				System.exit(1); 
			}
			
			for(Object o : queries) { 
				this.queries.add((String) o); 
			}
			
			for(Object o : responses) { 
				this.responses.add((String) o); 
			}
			
			if(this.queries.size() < 1 || this.responses.size() < 1) { 
				System.err.println("Error in Config: Queries and Response arrays must contain atleast one string. ");
				System.exit(1); 
			}
		} catch (FileNotFoundException e) {
			System.err.println("Error: Could not find settings file: " + pathToConfigFile);
			System.exit(1); 
		}
	}
	
	/* The bot will be executed off the main thread */ 
	class Executer implements Runnable {
		
		private boolean running = true; 
		private boolean processingQuery = false;
		
		@Override
		public void run() {
			Random r = new Random(); 
			
			while(running) { 
				processingQuery = true; 
				queryAnalyzePost();
				processingQuery = false; 
			
				if(running) { /* Check again in case of shutdown request during queryAnalyzePost */ 
					synchronized(this) { 
						try {
							this.wait(r.nextInt((int)(maxTimeBetweenCycles - minTimeBetweenCycles)) + minTimeBetweenCycles);
						} catch (InterruptedException ignored) {} 
					}
				}
			}
		} 
	}
	
	/* Example test run */ 
	public static void main(String[] args) {
		Bot b = new Bot("./config.json"); 
		b.run(); 
	}
}
