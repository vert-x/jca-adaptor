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
   
   private static VertxPlatformFactory instance;
   
   public synchronized static VertxPlatformFactory instance()
   {
      if (instance == null)
      {
         instance = new VertxPlatformFactory();
      }
      return instance;
   }
   
   /**
    * All Vert.x platforms.
    * 
    */
   private ConcurrentHashMap<VertxPlatformConfiguration, Vertx> vertxPlatforms = new ConcurrentHashMap<VertxPlatformConfiguration, Vertx>();
   
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
   public synchronized void createVertxIfNotStart(final VertxPlatformConfiguration config, final VertxLifecycleListener lifecyleListener)
   {
      Vertx vertx = this.vertxPlatforms.get(config);
      if (vertx != null)
      {
         log.log(Level.INFO, "Vert.x platform at address: " + config.getVertxPlatformAddress() + " has been started.");
         lifecyleListener.onGet(vertx);
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
      
      log.log(Level.INFO, "Starts a Vert.x platform at address: " + config.getVertxPlatformAddress());
      
      String clusterFile = config.getClusterConfiguratoinFile();
      System.setProperty("vertx.ra.cluster.file", clusterFile); 
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
               log.log(Level.INFO, "Vert.x Platform at address: " + config.getVertxPlatformAddress() + " Started Successfully.");
               vertxPlatforms.putIfAbsent(config, result.result());
               lifecyleListener.onCreate(result.result());
            }
            else if (result.failed())
            {
               log.log(Level.SEVERE, "Failed to start Vert.x at: " + config.getVertxPlatformAddress());
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
      Vertx vertx = this.vertxPlatforms.get(config);
      if (vertx != null)
      {
         if (isVertxHolded(vertx))
         {
            log.log(Level.WARNING, "Vertx at address: " + config.getVertxPlatformAddress() + " is taken, will not close it.");
            return;
         }
         log.log(Level.INFO, "Stops the Vert.x platform at address: " + config.getVertxPlatformAddress());
         this.vertxPlatforms.remove(config);
         vertx.stop();
      }
      else
      {
         log.log(Level.WARNING, "No Vert.x platform found at address: " + config.getVertxPlatformAddress());
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
   public synchronized void clear()
   {
      for (Map.Entry<VertxPlatformConfiguration, Vertx> entry: this.vertxPlatforms.entrySet())
      {
         log.log(Level.INFO, "Closing Vert.x Platform at address: " + entry.getKey().getVertxPlatformAddress());
         entry.getValue().stop();
         log.log(Level.INFO, "Vert.x Platform at address: " + entry.getKey().getVertxPlatformAddress() + " is Closed.");
      }
      this.vertxPlatforms.clear();
      this.vertxHolders.clear();
   }
   
}