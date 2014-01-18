/**
 * 
 */
package org.vertx.java.resourceadapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.impl.ConcurrentHashSet;

/**
 * A singleton factory to start a clustered Vert.x platform.
 * 
 * One clusterPort/clusterHost pair matches one Vert.x platform.
 * 
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxPlatformFactory
{
   
   private static Logger log = Logger.getLogger(VertxPlatformFactory.class.getName());
   
   private static VertxPlatformFactory INSTANCE = new VertxPlatformFactory();
   
   public static VertxPlatformFactory instance()
   {
      return INSTANCE;
   }
   
   /**
    * All Vert.x platforms.
    * 
    */
   private ConcurrentHashMap<String, Vertx> vertxPlatforms = new ConcurrentHashMap<String, Vertx>();
   
   /**
    * All Vert.x holders
    */
   private ConcurrentHashSet<VertxHolder> vertxHolders = new ConcurrentHashSet<VertxHolder>();
   
   /**
    * Default private constructor
    */
   private VertxPlatformFactory(){
      
   }

   /**
    * Creates a Vertx if one is not started yet.
    * 
    * @param config the configuration to start a vertx
    * @param lifecyleListener the vertx lifecycle listener
    */
   public synchronized void createVertxIfNotStart(final VertxPlatformConfiguration config, final VertxListener lifecyleListener)
   {
      Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());
      if (vertx != null)
      {
         log.log(Level.INFO, "Vert.x platform at: " + config.getVertxPlatformIdentifier() + " has been started.");
         lifecyleListener.whenReady(vertx);
         return;
      }
      Integer clusterPort = config.getClusterPort();
      String clusterHost = config.getClusterHost();
      if (clusterHost == null || clusterHost.length() == 0)
      {
         log.log(Level.INFO, "Cluster Host is not set, use '127.0.0.1' by default.");
         clusterHost = "localhost";
      }
      if (clusterPort == null)
      {
         log.log(Level.INFO, "Cluster Port is not set, use 0 to random choose available port.");
         clusterPort = Integer.valueOf(0);
      }
      
      log.log(Level.INFO, "Starts a Vert.x platform at: " + config.getVertxPlatformIdentifier());
      
      String clusterFile = config.getClusterConfigFile();
      if (clusterFile != null)
      {
         System.setProperty("vertx.ra.cluster.file", clusterFile);
      }
      if (clusterFile != null && clusterFile.length() > 0)
      {
         log.log(Level.INFO, "Using Cluster file: " + clusterFile);
      }
      VertxFactory.newVertx(clusterPort, clusterHost, new Handler<AsyncResult<Vertx>>()
      {
         @Override
         public void handle(final AsyncResult<Vertx> result)
         {
            if (result.succeeded())
            {
               log.log(Level.INFO, "Vert.x Platform at: " + config.getVertxPlatformIdentifier() + " Started Successfully.");
               vertxPlatforms.putIfAbsent(config.getVertxPlatformIdentifier(), result.result());
               lifecyleListener.whenReady(result.result());
            }
            else if (result.failed())
            {
               log.log(Level.SEVERE, "Failed to start Vert.x at: " + config.getVertxPlatformIdentifier());
               Throwable cause = result.cause();
               if (cause != null)
               {
                  throw new RuntimeException(cause);
               }
            }
         }
      });
   }
   
   /**
    * Adds VertxHolder to be recorded.
    * 
    * @param holder the VertxHolder
    */
   public void addVertxHolder(VertxHolder holder)
   {
      Vertx vertx = holder.getVertx();
      if (vertxPlatforms.containsValue(vertx))
      {
         if (!this.vertxHolders.contains(holder))
         {
            log.log(Level.INFO, "Adding Vertx Holder: " + holder.toString());
            this.vertxHolders.add(holder);
         }
         else
         {
            log.log(Level.WARNING, "Vertx Holder: " + holder.toString() + " has been added already.");
         }
      }
      else
      {
         log.log(Level.SEVERE, "Vertx Holder: " + holder.toString() + " is out of management.");
      }
   }
   
   /**
    * Removes the VertxHolder from recorded.
    * 
    * @param holder the VertxHolder
    */
   public void removeVertxHolder(VertxHolder holder)
   {
      if (this.vertxHolders.contains(holder))
      {
         log.log(Level.INFO, "Removing Vertx Holder: " + holder.toString());
         this.vertxHolders.remove(holder);
      }
      else
      {
         log.log(Level.SEVERE, "Vertx Holder: " + holder.toString() + " is out of management.");
      }
   }
   
   /**
    * Stops the Vert.x Platform Manager and removes it from cache.
    * 
    * @param config
    */
   public synchronized void stopPlatformManager(VertxPlatformConfiguration config)
   {
      Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());
      if (vertx != null)
      {
         if (isVertxHolded(vertx))
         {
            log.log(Level.WARNING, "Vertx at: " + config.getVertxPlatformIdentifier() + " is taken, will not close it.");
            return;
         }
         log.log(Level.INFO, "Stops the Vert.x platform at: " + config.getVertxPlatformIdentifier());
         this.vertxPlatforms.remove(config);
         vertx.stop();
      }
      else
      {
         log.log(Level.WARNING, "No Vert.x platform found at: " + config.getVertxPlatformIdentifier());
      }
   }
   
   private boolean isVertxHolded(Vertx vertx)
   {
      for (VertxHolder holder: this.vertxHolders)
      {
         if (vertx.equals(holder.getVertx()))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * Stops all started Vert.x platforms.
    * 
    * Clears all vertx holders.
    */
   synchronized void clear()
   {
      for (Map.Entry<String, Vertx> entry: this.vertxPlatforms.entrySet())
      {
         log.log(Level.INFO, "Closing Vert.x Platform at address: " + entry.getKey());
         entry.getValue().stop();
         log.log(Level.INFO, "Vert.x Platform at address: " + entry.getKey() + " is Closed.");
      }
      this.vertxPlatforms.clear();
      this.vertxHolders.clear();
   }
   
   /**
    * The Listener to monitor whether the embedded vert.x runtime is ready.
    *
    */
   public interface VertxListener
   {

      /**
       * When vertx is ready, maybe just started, or have been started already.
       * 
       * @param vertx the Vert.x
       */
      void whenReady(Vertx vertx);
   }
   
}