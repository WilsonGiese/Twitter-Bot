Twitter-Bot
===========

Author: Wilson Giese - giese.wilson@gmail.com
### About

A Twitter bot that will query for specified terms and respond the the tweets using the Twitter REST API. 

### Config

To run this application several things need to be defined in a config file. Example config.json:
```json
{
	"min_time_between_cycles":180000,
	"max_time_between_cycles":3000000,
	"max_tweets_per_cycle":1,

	"consumer_key":"YOUR CONSUMER KEY",
	"consumer_secret":"YOUR CONSUMER SECRET",
	"access_token":"YOUR ACCESS TOKEN",
	"access_token_secret":"YOUR ACCESS TOKEN SECRET",

	"queries":   ["I like rain", "I like thunder"], 
	"responses": ["You're welcome.", "I'm glad you like it.",  
					      "Just doing my job, no need to thank me."]
}
```

The above example is a config file for my test bot; The Zeus Bot(OAuth keys excluded). 
- **min/max_min_time_between_cycles**: This variable is the time in milliseconds that the bot will wait to run another cycle. The time is chosen randomly for each cycle. 
- **max_tweets_per_cycle**: This variable is the maximum number of tweets posted per cycle. 
- **consumer_key, consumer_secret, access_token, & access_token_secret**: These are the OAuth keys used to post to twitter using the REST API. These keys are given by Twitter and should remain a secret. Consumer keys are for the application itself, and access keys are for the twitter account that will post the "tweets". 
- **queries**: An array of strings that will be used to query Twitter. 
- **responses**: An array of string that will be used to respond to matching queries. 

### Run

	java -cp TwitterBot.jar Bot
You can run the program with the normal class files, but you'll need to link all the libraries. 
### Required Libraries

[json-simple-1.1.1](http://code.google.com/p/json-simple/downloads/list), [signpost-core-1.2.1.2](http://code.google.com/p/oauth-signpost/downloads/list), [commons-codec-1.7](http://commons.apache.org/codec/download_codec.cgi)

### Notes
This bot was not created as a spam or advertisement bot, just as a way to explore the Twitter, OAuth, and JSON API's. That said, you can use or modify the code as much as you want. 
