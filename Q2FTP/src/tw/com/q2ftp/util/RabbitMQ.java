package tw.com.q2ftp.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class RabbitMQ {

	private static final Logger logger = LogManager.getLogger(RabbitMQ.class);

	public static void ErrorPush(String message, String configPath) throws Exception {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}
		File file = new File(configPath);

		Document configDoc = null;
		try {
			configDoc = dombuilder.parse(file);
		} catch (SAXException | IOException e) {
			logger.error(e);
		}
		Element configRoot = configDoc.getDocumentElement();

		NodeList connectionFactory = configRoot.getElementsByTagName("errorQueueConnectionFactory");
		NodeList connectionInfo = connectionFactory.item(0).getChildNodes();

		String host = null, queue_name = null, routing_key = null, exchange = null, username = null, password = null;
		int port = 0;

		for (int i = 0; i < connectionInfo.getLength(); i++) {
			Node node = (Node) connectionInfo.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {

				String nodeName = node.getNodeName();
				String value = node.getTextContent();

				host = "host".equals(nodeName) ? value : host;
				port = "port".equals(nodeName) ? Integer.valueOf(value) : port;
				username = "username".equals(nodeName) ? value : username;
				password = "password".equals(nodeName) ? value : password;
				queue_name = "queueName".equals(nodeName) ? value : queue_name;
				routing_key = "routingKey".equals(nodeName) ? value : routing_key;
				exchange = "exchangeName".equals(nodeName) ? value : exchange;
			}
		}
		logger.debug(
				"host: {} \\ port: {} \\ username: {} \\ password: {} \\ queue_name: {} \\ routing_key: {} \\ exchange: {}",
				host, port, username, password, queue_name, routing_key, exchange);

		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost(host);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);

		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(queue_name, true, false, false, null);

		logger.debug("寄送: {}", message);
		byte[] b = message.getBytes(StandardCharsets.UTF_8);
		channel.basicPublish(exchange, routing_key, MessageProperties.BASIC, b);

		channel.close();
		connection.close();
	}

	public static String Pull(String configPath) throws IOException, TimeoutException {
		DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder dombuilder = null;
		try {
			dombuilder = domfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error(e);
		}
		File file = new File(configPath);

		Document configDoc = null;
		try {
			configDoc = dombuilder.parse(file);
		} catch (SAXException | IOException e) {
			logger.error(e);
		}
		Element configRoot = configDoc.getDocumentElement();

		NodeList connectionFactory = configRoot.getElementsByTagName("queueConnectionFactory");
		NodeList connectionInfo = connectionFactory.item(0).getChildNodes();

		String host = null, virtualHost = null, username = null, password = null, queue_name = null;
		int port = 0;

		for (int i = 0; i < connectionInfo.getLength(); i++) {
			Node node = (Node) connectionInfo.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {

				String nodeName = node.getNodeName();
				String value = node.getTextContent();

				host = nodeName.equals("host") ? value : host;
				port = nodeName.equals("port") ? Integer.valueOf(value) : port;
				username = nodeName.equals("username") ? value : username;
				password = nodeName.equals("password") ? value : password;
				virtualHost = nodeName.equals("virtualHost") ? value : virtualHost;
				queue_name = "queueName".equals(nodeName) ? value : queue_name;

			}
		}
		logger.debug("host: {} \\ port: {} \\ username: {} \\ password: {} \\ virtualHost: {} \\ queue_name: {}", host,
				port, username, password, virtualHost, queue_name);

		String message = null;
		boolean autoAck = false;

		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost(host);
		factory.setPort(port);
		factory.setUsername(username);
		factory.setPassword(password);
		factory.setVirtualHost(virtualHost);

		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		GetResponse response = channel.basicGet(queue_name, autoAck);
		if (response == null) {
			// No message retrieved.
		} else {
			AMQP.BasicProperties props = response.getProps();
			byte[] body = response.getBody();
			long deliveryTag = response.getEnvelope().getDeliveryTag();

			message = new String(body, "UTF-8");
			channel.basicAck(deliveryTag, false);
		}

		return message;

	}
}
