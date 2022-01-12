package marcoluglio.messagelisteners;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;

import marcoluglio.Retry;
import marcoluglio.RetryStatus;



public final class DisconnectMessageListener implements MessageListener {

	private final BayeuxClient bayeuxClient;
	private HandshakeMessageListener handshakeMessageListener;

	public DisconnectMessageListener(BayeuxClient bayeuxClient, HandshakeMessageListener handshakeMessageListener) {
		this.bayeuxClient = bayeuxClient;
		this.handshakeMessageListener = handshakeMessageListener;
	}

	@Override
	public void onMessage(ClientSessionChannel channel, Message message) {

		// TODO should I try to remove and readd the old listeners?

		var isHandshakeSuccessful = new Retry(isFutureHandshakeSuccessful -> {
			try {
				// isFutureHandshakeSuccessful will be set by the bayeuxClient handshake listener callback asynchronously
				this.resetIsFutureHandshakeSuccessful(isFutureHandshakeSuccessful);
				bayeuxClient.handshake();
				return isFutureHandshakeSuccessful.get();
			} catch (InterruptedException | ExecutionException ex) {
				ex.printStackTrace();
				return RetryStatus.FAILURE;
			}
		})
		.run();

		if (isHandshakeSuccessful == RetryStatus.EXCEEDED_ATTEMPTS) {
			// TODO try to fallback to another proxy and restart pod network
			throw new RuntimeException("Unable to initialize an http client");
		}

	}

	/**
	 *
	 * @param isFutureHandshakeSuccessful Future that will be set by the bayeuxClient handshake listener callback when the handshake is performed
	 */
	private void resetIsFutureHandshakeSuccessful(CompletableFuture<RetryStatus> isFutureHandshakeSuccessful) {
		this.handshakeMessageListener.setFutureHandshakeStatus(isFutureHandshakeSuccessful);
	}

}
