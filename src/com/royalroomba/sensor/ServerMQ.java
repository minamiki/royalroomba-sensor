package com.royalroomba.sensor;

import java.io.IOException;

import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.*;
import com.rabbitmq.client.impl.AMQChannel.SimpleBlockingRpcContinuation;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class ServerMQ
{
	// TAG
	private static final String TAG = "rr-sensor:ServerMQ";
	
	private static final String EXCHANGE = "amq.topic";
	private static final String ROUTING_KEY = "roomba-sensor-1";
	
	private String host;
	private int port;
	ConnectionFactory factory;
	QueueingConsumer consumer;
	
    public boolean listenLoop = false;
    public int mConsumedMessages = 0;
	
	private Connection mConnection;
	private Channel channel;
	
	AudioControl audio;
	
    public ServerMQ(String h, int p, Context context)
    {
    	factory = new ConnectionFactory();
    	this.host = h;
    	this.port = p;
    	audio = new AudioControl(context);
    }
    
    boolean connect()
    {
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
			channel.queueBind(queueName, EXCHANGE, ROUTING_KEY);
			
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
    
    public void disconnect()
    {
    	try{
    	    mConnection.close();
    	    listenLoop = false;
    	}catch (Exception e){
    		Log.d(TAG, "Exception closing connection!");
    		e.printStackTrace();
    		return;
    	}
    }
    
    public void listen(Context context)
    {
	    QueueingConsumer.Delivery delivery;
	    String message;
	    while (listenLoop) {
		    try {
		        delivery = consumer.nextDelivery();
		        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);			    		
			    
		        //Debug Statement for the message consumed
		        Log.i(TAG, delivery.getEnvelope().getRoutingKey()+" "+(new String(delivery.getBody())));
			    
			    // Listen
				message = new String(delivery.getBody());
				Log.i(TAG, "Recieved:" + message);
				audio.raygun.start();
				
		
		    } catch (InterruptedException ie) {
		        ie.printStackTrace();
		    }catch(IOException e){
				e.printStackTrace();
			}
	    }

    	/*
    	try
    	{
    		boolean noAck = true;
    	    AMQConnection mAMQConnection = (AMQConnection) mConnection;    
    	    AMQChannel mChannel = mAMQConnection._channelManager.createChannel(mAMQConnection);
    	    AMQImpl.Queue.Declare queue = new AMQImpl.Queue.Declare(1, queuename, false, false, true, false, false, null);
    	    AMQImpl.Basic.Get getCache = new AMQImpl.Basic.Get(1, queuename, noAck);
            //AMQCommand get_cmd = new AMQCommand(getCache);
            
            Log.d(TAG, "starting to consume....");
            while (listenLoop)
            {
            	SimpleBlockingRpcContinuation k = new SimpleBlockingRpcContinuation();
            	mChannel.rpc(queue, k);
            	k.getReply();
            	k = new SimpleBlockingRpcContinuation();
                mChannel.rpc(getCache, k); //TODO: this allocates in: AMQPChannel #291
            	AMQCommand getCommand = k.getReply();
            	byte[] body = getCommand.getContentBody();
            	
            	if (body == null)
            	{
            		continue;
            	}
            	else
            	{
            		mConsumedMessages++;
            	}
            	
            }
            Log.d(TAG, "stopped consuming.");
    	}
    	catch (Exception e)
    	{
    		Log.d(TAG, "Exception!");
    		e.printStackTrace();
    	}*/
    }
    
    public void publish(String message){
    	byte[] messageBodyBytes = message.getBytes();
    	try{
			channel.basicPublish("amq.topic", "roomba-sensor", null, messageBodyBytes);
			Log.i(TAG, "Message Sent");
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.i(TAG, "Failed to send");
		}
    }
    
    /*
    void test_consume_by_consumer(String queuename)
    {
        Channel channel;
        boolean noAck = true;
        QueueingConsumer consumer;
        try
        {
            channel = mConnection.createChannel();
            consumer = new QueueingConsumer(channel);
            channel.basicConsume(queuename, noAck, consumer);   
        }
        catch (Exception e)
        {
            Log.d(TAG, "Failed to create channel.");
            return;
        }
        
        Log.d(TAG, "Starting to consume messages...");
        mConsumedMessages = 0;
        
        while (mStopTest)
        {
            QueueingConsumer.Delivery delivery;
            try
            {
                delivery = consumer.nextDelivery();
                mConsumedMessages++;
                
                //TODO: do something here...
            }
            catch (Exception e)
            {
                 Log.d(TAG, "Exception on grabbing delivery!");
                 e.printStackTrace();
                 return;
            }
                 
        }
        
        try
        {       
            channel.close();
        }
        catch (Exception e)
        {
        	Log.d(TAG, "Exception closing channel!");
        	e.printStackTrace();
        	return;
        }
    }
    
    void test_consume_by_standard_basic_get(String queuename)
    {
    	Channel channel;
    	try
    	{
    	    channel = mConnection.createChannel();
    	}
    	catch (Exception e)
    	{
    		Log.d(TAG, "Failed to create channel!!!");
    		e.printStackTrace();
    		return;
    	}
    	
    	boolean noAck = true;
    	Log.d(TAG, "Starting to consume messages...");
    	mConsumedMessages = 0;
        while (!mStopTest)
        {
        	GetResponse response;
        	try
        	{
        	    response = channel.basicGet(queuename, noAck);
        	}
        	catch (Exception e)
        	{
        		Log.d(TAG, "Exception executing basic get!");
        		return;
        	}
        	if (response == null)
        	{
        		continue;
        	}
        	
        	//TODO: do something here...
        	mConsumedMessages++;
        }
        Log.d(TAG, "Stopped consuming messages, any gc runs?");
        
        try
        {       
            channel.close();
        }
        catch (Exception e)
        {
        	Log.d(TAG, "Exception closing channel!");
        	e.printStackTrace();
        	return;
        }
    }
    */    
}

