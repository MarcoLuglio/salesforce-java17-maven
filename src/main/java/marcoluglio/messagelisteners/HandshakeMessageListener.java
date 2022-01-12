package marcoluglio.messagelisteners;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;

import marcoluglio.Retry;
import marcoluglio.RetryStatus;



/**
 * Subscribes to the order change channel for processing order change messages after handshake
 */
public final class HandshakeMessageListener implements MessageListener {

	private final BayeuxClient bayeuxClient;
	private CompletableFuture<RetryStatus> isFutureHandshakeSuccessful;
	private final Gson gson;

	/**
	 * Subscribes to the order change channel for processing order change messages after handshake
	 * @param bayeuxClient CometD (bayeux) client that will send order change events
	 * @param gson Google fast json deserialiser instance
	 */
	public HandshakeMessageListener(BayeuxClient bayeuxClient, Gson gson) {
		this.bayeuxClient = bayeuxClient;
		this.gson = gson;
	}

	/**
	 *
	 * @param isFutureHandshakeSuccessful Future that will be set by the handshake message callback when the handshake is performed
	 */
	public void setFutureHandshakeStatus(CompletableFuture<RetryStatus> isFutureHandshakeSuccessful) {
		this.isFutureHandshakeSuccessful = isFutureHandshakeSuccessful;
	}

	/**
	 * Subscribes to the order change channel for processing order change messages after handshake
	 */
	@Override
	public void onMessage(ClientSessionChannel channel, Message message) {

		if (!message.isSuccessful()) {
			this.isFutureHandshakeSuccessful.complete(RetryStatus.FAILURE);
			return;
		}

		this.isFutureHandshakeSuccessful.complete(RetryStatus.SUCCESS);

		this.subscribeToOrderChange(channel, message);

	}

	private void subscribeToOrderChange(ClientSessionChannel channel, Message message) {

		System.err.printf("Received message on %s: %s%n", channel, message);

		var orderChangeChannel = bayeuxClient.getChannel("/event/Order_Change__e");
		var orderChangeMessageListener = new OrderChangeMessageListener(this.gson);
		var orderChangeSubscribeListener = new OrderChangeSubscribeListener();

		var isSubscribeSuccessful = new Retry(futureSubscribeStatus -> {
			try {
				// futureSubscribeStatus will be set by the orderChangeChannel subscribe listener callback asynchronously
				orderChangeSubscribeListener.setFutureSubscribeStatus(futureSubscribeStatus);
				orderChangeChannel.subscribe(
					orderChangeMessageListener,
					orderChangeSubscribeListener
				);
				return futureSubscribeStatus.get();
			} catch (InterruptedException | ExecutionException ex) {
				ex.printStackTrace();
				return RetryStatus.FAILURE;
			}
		})
		.run();

		if (isSubscribeSuccessful == RetryStatus.FAILURE) {
			throw new RuntimeException("Unable to initialize an http client");
		}

	}

}
