package com.royalroomba.sensor;

import java.io.IOException;

import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

public class ServerMQ{
	
	//Declare constant variables for RabbitMQ server
	//public static final String HOST = "169.254.208.130";
	public static final String EXCHANGE = "amq.topic";
	public static final String ROUTING_KEY = "sensor-in-1";
	public static final String SERVER_KEY = "server";
	public static final int PORT = AMQP.PROTOCOL.PORT;
	//public static final ConnectionFactory FACTORY = new ConnectionFactory();
	public ConnectionFactory factory;
	public static final String PUBLISH_KEY = "sensor-out-1";
	public static Connection conn;
	public static Channel channel;

	//Loop can be terminated by changing loopTermination
	public static boolean loopTermination = true;
	
	//Declare roombacomm variables. Variables are not
	//set to constants in case we want to implement easy reconnection
	//for DCs	
	
	public ServerMQ(String host){
		
		try {
			factory = new ConnectionFactory();
			//Set up RabbitMQ Connection
			Log.i("royalroomba-sensor", "Connecting to Server...");
			factory.setHost(host);
			conn = factory.newConnection();
			Log.i("royalroomba-sensor", "Connection successful");
			channel = conn.createChannel();
			
			//Declare exchange to be used and bind a queue and routing keys
			channel.exchangeDeclare(EXCHANGE, "topic", true);
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, EXCHANGE, ROUTING_KEY);
			channel.queueBind(queueName, EXCHANGE, SERVER_KEY);
			
			//Print out queue name to notify connection
			Log.i("royalroomba-sensor", "Queue Binding Complete");
			Log.i("royalroomba-sensor", "Queue Name is: "+queueName);
			
			//Instantiate a consumer to consume the queues
			boolean noAck = false;
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, noAck, consumer);
			
			//Infinite Loop to listen to deliveries
			//from the rabbitmq server			
			while (loopTermination) {
			    QueueingConsumer.Delivery delivery;
			    try {
			        delivery = consumer.nextDelivery();
			        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);			    		
				    
			        //Debug Statement for the message consumed
			        Log.i("royalroomba-sensor", "Message Consumed: " + delivery.getEnvelope().getRoutingKey()+" "+(new String(delivery.getBody())));
				    
				    //Check the routing key of each delivery's envelope and pass them
				    //To the respective roombas for actions
				    //Check RoombaControl.java for the list of commands
				    if(delivery.getEnvelope().getRoutingKey().equals(ROUTING_KEY)){
					    //delivery.getBody() gives the message
				    	// nothing to do yet
				    }else if(delivery.getEnvelope().getRoutingKey().equals("SERVER_KEY")){
				    	//Terminate the consumption loop
				    	if(delivery.getBody().equals("STOP_CONSUME")){
				    		loopTermination = false;
				    	}
				    }
			
			    } catch (InterruptedException ie) {
			        ie.printStackTrace();
			    }
			}
			
		} catch (Exception ex) {
			Log.i("royalroomba-sensor", "MQ main caught exception");
		    ex.printStackTrace();
		    System.exit(1);
		}
	}
	
	public void disconnect(){
		loopTermination = false;
	}

	//publish function to publish to the server
	public void publish(String key,String message){
		try {
			channel.basicPublish(EXCHANGE, ROUTING_KEY + "-", null, message.getBytes());
		} catch (IOException e) {
			Log.i("royalroomba-sensor", "Error publishing!");
			e.printStackTrace();
		}
	}
}