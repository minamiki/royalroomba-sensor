package com.royalroomba.sensor;

import java.io.IOException;
import java.util.Random;

import com.rabbitmq.client.*;
import android.content.Context;
import android.util.Log;

public class ServerMQ
{
	// TAG
	private static final String TAG = "rr-sensor:ServerMQ";
	
	// Server variables
	private static final String EXCHANGE = "amq.topic";
	private static final String ROUTING_KEY_OUT = "roomba-sensorout-";
	private static final String ROUTING_KEY_IN = "roomba-sensorin-";
	
	private ConnectionFactory factory;
	private Connection mConnection;
	private Channel channel;	
	private String host;
	private int port;
	private int deviceID = 1;
	
	private QueueingConsumer consumer;
    public boolean listenLoop = false;
    
    int switchControl;
	
	AudioControl audio;
	// Hit sounds
	private Random generator = new Random();
	int i;
	
    public ServerMQ(String h, int p, Context context, int deviceID){
    	factory = new ConnectionFactory();
    	this.host = h;
    	this.port = p;
    	this.deviceID = deviceID;
    	audio = new AudioControl(context);
    }
    
    boolean connect(){
    	factory.setUsername("guest");
    	factory.setPassword("guest");
    	factory.setVirtualHost("/");
    	factory.setHost(host);
    	factory.setPort(port);
    	try{
    		// connect to server
    		mConnection = factory.newConnection();
    		
    		// init the channel
    		channel = mConnection.createChannel();
    		
			channel.exchangeDeclare(EXCHANGE, "topic", true);
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, EXCHANGE, ROUTING_KEY_IN + deviceID);
			
			//Print out queue name to notify connection
			Log.i(TAG, "Queue Binding Complete");
			System.out.println("Queue Name is: "+queueName);
			
			//Instantiate a consumer to consume the queues
			boolean noAck = false;
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, noAck, consumer);

    		listenLoop = true;
    	}catch (Exception e){
    		Log.d(TAG, "Failed to connect to server!");
    		e.printStackTrace();
    		return false;
    	}
    	Log.d(TAG, "Connected Successfully");
    	return true;
    }
    
    public void disconnect(){
    	try{
    	    mConnection.close();
    	    listenLoop = false;
    	}catch (Exception e){
    		Log.d(TAG, "Exception closing connection!");
    		e.printStackTrace();
    	}
    }
    
    public void listen(Context context){
	    QueueingConsumer.Delivery delivery;
	    String message;
	    Log.i(TAG, "Start listening...");
	    while (listenLoop) {
		    try {
		        delivery = consumer.nextDelivery();
		        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);			    		
			    
		        //Debug Statement for the message consumed
		        Log.i(TAG, delivery.getEnvelope().getRoutingKey()+" "+(new String(delivery.getBody())));
			    // Listen
				message = new String(delivery.getBody());
				
				// Execute functions
				if(message.contentEquals("HORN")){
					audio.horn.start();					
				}else if(message.contentEquals("COLLIDE")){
					i = generator.nextInt(audio.getNumHits());
					audio.hit[i].start();					
				}
		    }catch(InterruptedException ie){
		        ie.printStackTrace();
		    }catch(IOException e){
				e.printStackTrace();
			}
	    }
    }
    
    public void publish(String message){
    	byte[] messageBodyBytes = message.getBytes();
    	try{
			channel.basicPublish(EXCHANGE, ROUTING_KEY_OUT + deviceID, null, messageBodyBytes);
			Log.i(TAG, "Message Sent");
		}catch(IOException e){
			e.printStackTrace();
			Log.i(TAG, "Failed to send");
		}
    }
}

