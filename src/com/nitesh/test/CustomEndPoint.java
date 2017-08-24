package com.nitesh.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
@ServerEndpoint("/ratesrv")
public class CustomEndPoint {
 //queue holds the list of connected clients
 private static Queue<Session> queue = new ConcurrentLinkedQueue<Session>();
 private static Thread rateThread ; //rate publisher thread
 static
 {
//rate publisher thread, generates a new value for USD rate every 2 seconds.
  rateThread=new Thread(){
	  	private final File file = new File("/home/mmt5866/Desktop/watch/test.log");
	    private AtomicBoolean stop = new AtomicBoolean(false);
	    private int lineRead = -1;

	    /*public FileWatcher(File file) {
	    	System.out.println("ctor");
	        this.file = file;
	        this.lineRead = -1;
	    }*/

	    public boolean isStopped() { return stop.get(); }
	    public void stopThread() { stop.set(true); }

	    public void doOnChange() {
	    	int curr = -1;
	    	StringBuilder contentBuilder = new StringBuilder();
	        try (BufferedReader br = new BufferedReader(new FileReader(file))) 
	        {
	     
	            String sCurrentLine;
	            while ((sCurrentLine = br.readLine()) != null) 
	            {
	            	if (curr >= lineRead) {
	            		contentBuilder.append(sCurrentLine).append("\n");
	            		lineRead++;
	            	}
	            	curr++;
	            }
	        } 
	        catch (IOException e) 
	        {
	            e.printStackTrace();
	        }
	        
	        System.out.println("changed:\n");
	        System.out.println(contentBuilder.toString());
	        if(queue!=null)
	            sendAll(contentBuilder.toString());
	    }

	    @Override
	    public void run() {
	        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
	        	System.out.println("started");
	            Path path = file.toPath().getParent();
	            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
	            System.out.println("started");
	            while (!isStopped()) {
	                WatchKey key;
	                try { key = watcher.poll(25, TimeUnit.MILLISECONDS); }
	                catch (InterruptedException e) { return; }
	                if (key == null) { Thread.yield(); continue; }

	                for (WatchEvent<?> event : key.pollEvents()) {
	                    WatchEvent.Kind<?> kind = event.kind();

	                    @SuppressWarnings("unchecked")
	                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
	                    Path filename = ev.context();

	                    if (kind == StandardWatchEventKinds.OVERFLOW) {
	                        Thread.yield();
	                        continue;
	                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
	                            && filename.toString().equals(file.getName())) {
	                        doOnChange();
	                    }
	                    boolean valid = key.reset();
	                    if (!valid) { System.out.println("ended");break; }
	                }
	                Thread.yield();
	            }
	        } catch (Throwable e) {
	            // Log or rethrow the error
	        }
	    }
   /*public void run() {
    DecimalFormat df = new DecimalFormat("#.####");
    while(true)
    {
     double d=2+Math.random();     
     if(queue!=null)
      sendAll("USD Rate: "+df.format(d));    
     try {
      sleep(1000);
     } catch (InterruptedException e) {      
     }
    }
   };*/
  } ;
  rateThread.start();
 }
 @OnMessage
 public void onMessage(Session session, String msg) {
//provided for completeness, in out scenario clients don't send any msg.
  try {   
   System.out.println("received msg "+msg+" from "+session.getId());
  } catch (Exception e) {
   e.printStackTrace();
  }
 }
@OnOpen
 public void open(Session session) {
  queue.add(session);
  System.out.println("New session opened: "+session.getId());
 }
  @OnError
 public void error(Session session, Throwable t) {
  queue.remove(session);
  System.err.println("Error on session "+session.getId());  
 }
 @OnClose
 public void closedConnection(Session session) { 
  queue.remove(session);
  System.out.println("session closed: "+session.getId());
 }
 private static void sendAll(String msg) {
  try {
   /* Send the new rate to all open WebSocket sessions */  
   ArrayList<Session > closedSessions= new ArrayList<>();
   for (Session session : queue) {
    if(!session.isOpen())
    {
     System.err.println("Closed session: "+session.getId());
     closedSessions.add(session);
    }
    else
    {
 session.getBasicRemote().sendText(msg);
    }    
   }
   queue.removeAll(closedSessions);
   System.out.println("Sending "+msg+" to "+queue.size()+" clients");
  } catch (Throwable e) {
   e.printStackTrace();
  }
 }
}
