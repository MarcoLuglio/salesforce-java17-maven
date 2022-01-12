package marcoluglio.messagelisteners;

import java.util.concurrent.CompletableFuture;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession.MessageListener;

import marcoluglio.RetryStatus;



public final class OrderChangeSubscribeListener implements MessageListener {

	private CompletableFuture<RetryStatus> isFutureSubscribeSuccessful;

	/**
	 *
	 * @param isFutureSubscribeSuccessful Future that will be set by the subscribe message callback when the subscribe is performed
	 */
	public void setFutureSubscribeStatus(CompletableFuture<RetryStatus> isFutureSubscribeSuccessful) {
		this.isFutureSubscribeSuccessful = isFutureSubscribeSuccessful;
	}

	@Override
	public void onMessage(Message message) {
		if (!message.isSuccessful()) {
			this.isFutureSubscribeSuccessful.complete(RetryStatus.FAILURE);
		}
		this.isFutureSubscribeSuccessful.complete(RetryStatus.SUCCESS);
	}

}
