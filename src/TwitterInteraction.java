import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException; 

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class TwitterInteraction {
	public static final int PORT = 80; 
	
	/** 
	 * This method queries twitter for a single query string. 
	 * 
	 * @param query
	 * @return Full JSON object as a single string. 
	 * @throws UnsupportedEncodingException: Thrown from encoding query to UTF-8. 
	 */
	public static String twitterQuery(String query) throws UnsupportedEncodingException {
		/* Format query */ 
		query = URLEncoder.encode(query, "UTF-8"); 
		String urlQuery = "http://search.twitter.com/search.json?q=" + query;
		
		try {			
			URL url = new URL(urlQuery);
			Socket socket = new Socket(url.getHost(), PORT); 
			OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream()); 
			
			/* Write request */ 
			String request = "GET " + url.getPath() + "?" + url.getQuery() + " HTTP/1.1\n" +
					         "Host: " + url.getHost() + "\n\n";
			output.write(request);
			output.flush(); 
			
			/* Get response */ 
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
			
			/* Log and remove header info */
			String line; 
			while(!(line = input.readLine()).contains("{")) { /* TODO Log header info */ }
			
			/* JSON */ 
			String json = line;
			while((line = input.readLine()) != null) { 
				json += line + "\n"; 
			}
			
			input.close(); 
			output.close();
			
			return json; 
			
		} catch (MalformedURLException mfurle) {
			System.err.println("Query URL was malformed: " + urlQuery);  
		} catch (UnknownHostException uhe) {
			System.err.println("Unknown Host: " + urlQuery);
		} catch (IOException ioe) {
			System.err.println("IOException from Socket. URL: " + urlQuery + " Port: " + PORT);
		}
		
		return null; 
	}
	
	/**
	 * Twitter API POST: https://api.twitter.com/1/statuses/update.json
	 * Required Param: status=<STRING>
	 * 
	 * @param data
	 * @param settings
	 * @throws UnsupportedEncodingException
	 */
	public static int twitterPost(TweetData data, OAuthSettings settings) throws UnsupportedEncodingException { 
		
		/* OAuth-Signpost will be used to authenticate the twitter POST */ 
		OAuthConsumer consumer = new DefaultOAuthConsumer(settings.getConsumerKey(), 
				                                          settings.getConsumerSecret()); 
		consumer.setTokenWithSecret(settings.getAccessToken(), 
				                    settings.getAccessTokenSecret());
	
		
		/* Format status message */ 
		String status = URLEncoder.encode(data.getText(), "UTF-8");
		String urlStatus = "https://api.twitter.com/1.1/statuses/update.json?status=" + status + "&in_reply_to_status_id=" + data.getReplyID(); 
		
		try {
			URL url = new URL(urlStatus);
			
			/* sign() wants org.apache.http.HttpRequest, but 
			 * java.net.HttpURLConnection works, and we can get one
			 * from the URL. */ 
			HttpURLConnection request = (HttpURLConnection) url.openConnection();  
			request.setRequestMethod("POST"); 
			
			/* Sign request with OAuthConsumer */ 
			consumer.sign(request);
			
			/* Connect Request */ 
			request.connect(); 
			
			return request.getResponseCode();
			
		} catch (MalformedURLException e) {
			System.err.println("Status URL was malformed: " + urlStatus);
		} catch (IOException e) {
			System.err.println("Problem with request; Possible problems: \n"  + 
		                       "[1] Issue opening connecting. \n" + 
					           "[2] Issue connecting. \n" + 
		                       "[3] Issue getting response code. Should not happen if 1 & 2 were successful.");
		} catch (OAuthMessageSignerException e) {
			System.err.println("Problem Signing request with OAuthConsumer. ");
		} catch (OAuthExpectationFailedException e) {
			System.err.println("Problem Signing request with OAuthConsumer. ");
		} catch (OAuthCommunicationException e) {
			System.err.println("Problem Signing request with OAuthConsumer. ");
		}
		return 0; 
	}
}
