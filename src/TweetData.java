
public class TweetData {

	private String text; 
	private String userID;
	private String replyID; 

	public TweetData(String text, String userID, String replyID) { 
		this.text = text; 
		this.userID = userID; 
		this.replyID = replyID; 
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}
	
	public String getReplyID() {
		return replyID;
	}

	public void setReplyID(String replyID) {
		this.replyID = replyID;
	}
}
