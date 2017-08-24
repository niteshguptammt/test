package com.nitesh.test2;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;



public final class HttpClientPool {

  // Single-element enum to implement Singleton.
  private enum HttpClientSingleton {
    // Just one of me so constructor will be called once.
    Client;
	// The thread-safe client.
    private final CloseableHttpClient threadSafeClient;
    // The pool monitor.
    private final IdleConnectionMonitorThread monitor;

    // The constructor creates it - thus late
    private HttpClientSingleton() {;
      PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
      // Increase max total connection to MAX_TOTAL_CONNECTIONS
      cm.setMaxTotal(10);
      // Increase default max connection per route to DEFAULT_MAX_PER_ROUTE
      cm.setDefaultMaxPerRoute(10);
      // Build the client.
      threadSafeClient = HttpClients.custom()
              .setConnectionManager(cm)
              .build();
      // Start up an eviction thread.
      monitor = new IdleConnectionMonitorThread(cm);
      // Don't stop quitting.
      monitor.setDaemon(true);
      monitor.start();
    }

    public CloseableHttpClient get() {
      return threadSafeClient;
    }

  }

  public static CloseableHttpClient getClient() {
    // The thread safe client is held by the singleton.
    return HttpClientSingleton.Client.get();
  }

  // Watches for stale connections and evicts them.
  private static class IdleConnectionMonitorThread extends Thread {
    private static final int CLOSED_EXPIRED_CONNECTION_TIME = 120;
	// The manager to watch.
    private final PoolingHttpClientConnectionManager cm;
    // Use a BlockingQueue to stop everything.
    private final BlockingQueue<Stop> stopSignal = new ArrayBlockingQueue<Stop>(1);

    // Pushed up the queue.
    private static class Stop {
      // The return queue.
      private final BlockingQueue<Stop> blockingQueueStop = new ArrayBlockingQueue<Stop>(1);

      // Called by the process that is being told to stop.
      public void stopped() {
        // Push me back up the queue to indicate we are now stopped.
        blockingQueueStop.add(this);
      }

      // Called by the process requesting the stop.
      public void waitForStopped() throws InterruptedException {
        // Wait until the callee acknowledges that it has stopped.
        blockingQueueStop.take();
      }

    }

    IdleConnectionMonitorThread(PoolingHttpClientConnectionManager cm) {
      super();
      this.cm = cm;
    }

    @Override
    public void run() {
      try {
        // Holds the stop request that stopped the process.
        Stop stopRequest;
        // Every 5 seconds.
        while ((stopRequest = stopSignal.poll(CLOSED_EXPIRED_CONNECTION_TIME, TimeUnit.SECONDS)) == null) {
          // Close expired connections
          cm.closeExpiredConnections();
          // Optionally, close connections that have been idle too long.
          cm.closeIdleConnections(CLOSED_EXPIRED_CONNECTION_TIME, TimeUnit.SECONDS);
        }
        // Acknowledge the stop request.
        stopRequest.stopped();
      } catch (InterruptedException ex) {
        // terminate
      }
    }

    public void shutdown() throws InterruptedException, IOException {
      // Signal the stop to the thread.
      Stop stop = new Stop();
      stopSignal.add(stop);
      // Wait for the stop to complete.
      stop.waitForStopped();
      // Close the pool - Added
      HttpClientSingleton.Client.get().close();
      // Close the connection manager.
      cm.close();

    }

  }

  public static void shutdown() throws InterruptedException, IOException {
    // Shutdown the monitor.
    HttpClientSingleton.Client.monitor.shutdown();
  }

}
