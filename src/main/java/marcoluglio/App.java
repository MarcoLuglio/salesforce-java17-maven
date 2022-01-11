package marcoluglio;

/**
 * Hello world!
 */
public final class App {

	private App() {
	}

	/**
	 * Says hello to the world.
	 * @param args The arguments of the program.
	 */
	public static void main(String[] args) {
		System.out.println("Hello World!");
	}

}

/*
Saleforce cometD methods
connect 	The client connects to the server.
disconnect 	The client disconnects from the server.
handshake 	The client performs a handshake with the server and establishes a long polling connection.
subscribe 	The client subscribes to a channel defined by a PushTopic. After the client subscribes, it can receive messages from that channel. You must successfully call the handshake method before you can subscribe to a channel.
unsubscribe 	The client unsubscribes from a channel.
*/
