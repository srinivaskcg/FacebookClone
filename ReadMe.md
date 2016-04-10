Implementation of Facebook API

How to run:

•	To run the server – run from sbt and select Project4 from the options.
•	To run the Simulator (Client) – run numberOfUsers from sbt and select Simulation from the options. Where numberOfUsers is the user count with which facebook simulation to be performed.

Input: Number of Users

Output: Given number of Users will be registered and then the simulation of facebook activity will be printed.

Maximum Network able to Simulate: Maximum number of users with which the project simulated was 1,00,000. 

Architecture- Created a Server, Client and a Simulator.

Server: Server basically handles the requests received from client and route the requests based on the path to process the requests further. A new actor will be created to process the request and kills the actor after completion of the process. This will allows the spray can receiver to distribute the load and the actors work concurrently. After processing the request, corresponding entity/message will be passed back to client. 

Client: Client receives the simulated activity requests from simulator and passes the request to Server with a future. Once the response was received the output will be printed.

Simulator: Simulator tries to replicate the actual Facebook users and generates activity based on the set parameters. Generated activity will be passed to client and client sends it to server for processing. Based on the stats available on Internet simulated the following activity for 20000 users.

Functionality Implemented: 

Profile API:

Register User – A new user can register by providing the basic details like First Name, Last Name, Gender, Date of Birth, Email Id. In addition server maintains  userId and Creation date for the User.

Get User Info – User can access his details provided during the registration. 

Get User Posts – User can access his posts by providing his user handle. Here the simulator randomly picks a user and fetches the posts for that user.

Friend List API:

Send Friend Request – User can send a friend request to another user. Once the request is sent it will be stored in pending requests list of the other user. Simulator picks two random users and sends a friend request from one to another.

Manage Pending friend requests – User can check the pending requests and accept the request. Once the request is accepted the user will become a friend and the request will be removed from pending list. Simulator for this works in tandem with the previous functionality of send request. With a time delay the received user will accept the request.

Post API:

Post Status/ Post on own Wall – User can update his status or post a message on his own wall. Simulator randomly selects a user and that user can post a status on his wall. 
 
Post on friend’s wall – User can post a message on other user’s wall. Simulator randomly selects two users and tells the first user to post some message on second user’s wall.

Comment on a Post – User can post a comment on a Post. Simulator randomly picks a user and a post and the user can post a comment on the picked post.

Page API:

Create a page – User can create a new page to promote or write about something. Simulator randomly picks a user and that user creates a new page.

Like a Page – User can subscribe to a page by liking the page. Simulator randomly picks a page and a user and that user likes the page.

Post on a page – User can post a message on a page. 

Stats taken as reference:

Number of users – 1.55 billion
New users added every second – 8 
Number of Posts made every second – 54k
Number of Status updates made every second – 500
Number of Friend requests made every second - 166

Scaled activity to 100,000 users:

	Post a status every 1 second
	Posts a post on friends wall every 2 seconds
	Sends a friend request every 5 seconds
	Checks user profile every 1 seconds
	Creates a page every 10 seconds
	Posts on page every 1 seconds
	Comment on an user or page post every 0.5 seconds
	Likes a page every 4 seconds
