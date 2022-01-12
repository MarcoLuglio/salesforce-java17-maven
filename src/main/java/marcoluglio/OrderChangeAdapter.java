package marcoluglio;

import com.google.gson.Gson;

import org.cometd.bayeux.Channel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.http.jetty.JettyHttpClientTransport;
import org.cometd.client.transport.ClientTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;

import marcoluglio.messagelisteners.ConnectMessageListener;
import marcoluglio.messagelisteners.DisconnectMessageListener;
import marcoluglio.messagelisteners.HandshakeMessageListener;
import marcoluglio.messagelisteners.SubscribeMessageListener;
import marcoluglio.messagelisteners.UnsubscribeMessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;



/**
 * Transfers Salesforce cometD (bayeux) order change messages to something better supported by .Net
 */
public final class OrderChangeAdapter {

	private final ConnectMessageListener connectMessageListener = new ConnectMessageListener();
	private HandshakeMessageListener handshakeMessageListener;
	private final SubscribeMessageListener subscribeMessageListener = new SubscribeMessageListener();
	private final UnsubscribeMessageListener unsubscribeMessageListener = new UnsubscribeMessageListener();
	private DisconnectMessageListener disconnectMessageListener;

	/**
	 * Transfers Salesforce cometD (bayeux) order change messages to something better supported by .Net
	 * @param salesforceSubdomain Salesforce org custom subdomain (the part between https:// and .salesforce.com)
	 * @param salesforceApiVersion Salesforce org api version. You can check the latest version your org supports when creating any Apex class.
	 * @param bearerToken The connected app bearer token for authentication. Must include the "Bearer " at the beginning.
	 */
	OrderChangeAdapter(String salesforceSubdomain, String salesforceApiVersion, String bearerToken) {
		this.connect(salesforceSubdomain, salesforceApiVersion, bearerToken);
	}

	/**
	 * Connects to the cometD (bayeux) endpoint in Salesforce and starts to listen for order change messages
	 * @param salesforceSubdomain Salesforce org custom subdomain (the part between https:// and .salesforce.com)
	 * @param salesforceApiVersion Salesforce org api version. You can check the latest version your org supports when creating any Apex class.
	 * @param bearerToken The connected app bearer token for authentication. Must include the "Bearer " at the beginning.
	 */
	private void connect(String salesforceSubdomain, String salesforceApiVersion, String bearerToken) {

		var bayeuxClient = this.initBayeuxClient(salesforceSubdomain, salesforceApiVersion, bearerToken);
		this.addClientListeners(bayeuxClient);

		var handshakeStatus = new Retry(futureHandshakeStatus -> {
			try {
				// futureHandshakeStatus will be set by the bayeuxClient handshake listener callback asynchronously
				this.resetFutureHandshakeStatus(futureHandshakeStatus);
				bayeuxClient.handshake();
				return futureHandshakeStatus.get();
			} catch (InterruptedException | ExecutionException ex) {
				ex.printStackTrace();
				return RetryStatus.FAILURE;
			}
		})
		.run();

		if (handshakeStatus == RetryStatus.EXCEEDED_ATTEMPTS) {
			throw new RuntimeException("Unable to initialize an http client");
		}

	}

	/**
	 * Creates an authenticated cometD (bayeux) client ready for handshakes
	 * @param salesforceSubdomain Salesforce org custom subdomain (the part between https:// and .salesforce.com)
	 * @param salesforceApiVersion Salesforce org api version. You can check the latest version your org supports when creating any Apex class.
	 * @param bearerToken The connected app bearer token for authentication. Must include the "Bearer " at the beginning.
	 * @return An authenticated BayeuxClient ready for handshakes
	 */
	private BayeuxClient initBayeuxClient(String salesforceSubdomain, String salesforceApiVersion, String bearerToken) {

		// TODO add a proxy at some point

		var httpClient = new HttpClient();
		// httpClient.setMaxConnectionsPerDestination(2);

		var startStatus = new Retry(() -> {
			try {
				httpClient.start();
				return RetryStatus.SUCCESS;
			} catch (Exception ex) {
				ex.printStackTrace();
				return RetryStatus.FAILURE;
			}
		})
		.run();

		if (startStatus == RetryStatus.EXCEEDED_ATTEMPTS) {
			throw new RuntimeException("Unable to initialize an http client");
		}

		var saleforceApiUrl = String.format(
			"https://%1$s.my.salesforce.com/cometd/%2$s/",
			salesforceSubdomain, // 1
			salesforceApiVersion // 2
		);

		// cometD transport can be http or web socket, but Salesforce only supports long polling
		Map<String, Object> transportOptions = new HashMap<>();
		ClientTransport httpTransport = new JettyHttpClientTransport(transportOptions, httpClient) {
			@Override
			protected void customize(Request request) {
				super.customize(request);
				request.headers(httpFields -> httpFields.add("Authorization", bearerToken));
			}
		};

		var bayeuxClient = new BayeuxClient(saleforceApiUrl, httpTransport);
		// bayeuxClient.addExtension(new ReplayExtension(replay)); // TODO

		return bayeuxClient;

	}

	/**
	 * Adds listeners to standard cometD (bayeux) channels (handshake, disconnect, etc.)
	 * @param bayeuxClient
	 * @param isFutureHandshakeSuccessful
	 */
	private void addClientListeners(BayeuxClient bayeuxClient) {

		var clientConnectChannel = bayeuxClient.getChannel(Channel.META_CONNECT);
		clientConnectChannel.addListener(this.connectMessageListener);

		this.handshakeMessageListener = new HandshakeMessageListener(
			bayeuxClient,
			new Gson()
		);
		var clientHandshakeChannel = bayeuxClient.getChannel(Channel.META_HANDSHAKE);
		clientHandshakeChannel.addListener(this.handshakeMessageListener);

		var clientSubscribeChannel = bayeuxClient.getChannel(Channel.META_SUBSCRIBE);
		clientSubscribeChannel.addListener(this.subscribeMessageListener);

		var clientUnsubscribeChannel = bayeuxClient.getChannel(Channel.META_UNSUBSCRIBE);
		clientUnsubscribeChannel.addListener(this.unsubscribeMessageListener);

		this.disconnectMessageListener = new DisconnectMessageListener(
			bayeuxClient,
			this.handshakeMessageListener
		);
		var clientDisconnectChannel = bayeuxClient.getChannel(Channel.META_DISCONNECT);
		clientDisconnectChannel.addListener(this.disconnectMessageListener);

		// additional listeners are added after handshake

	}

	/**
	 *
	 * @param futureHandshakeStatus Future that will be set by the bayeuxClient handshake listener callback when the handshake is performed
	 */
	private void resetFutureHandshakeStatus(CompletableFuture<RetryStatus> futureHandshakeStatus) {
		this.handshakeMessageListener.setFutureHandshakeStatus(futureHandshakeStatus);
	}

}
