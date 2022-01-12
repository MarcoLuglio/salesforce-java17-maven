package marcoluglio.messagelisteners;

import com.google.gson.Gson;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;

import marcoluglio.OrderChangeMessage;



public final class OrderChangeMessageListener implements MessageListener {

	private final Gson gson;

	public OrderChangeMessageListener(Gson gson) {
		this.gson = new Gson();
	}

	@Override
	public void onMessage(ClientSessionChannel channel, Message message) {

		var stringMessage = (String)message.getData();
		var orderChangeMessage = gson.fromJson(stringMessage, OrderChangeMessage.class);
		// query salesforce for additional data all fill the object
		// or just save it to a kafka or service bus queue or mongodb

	}

}
