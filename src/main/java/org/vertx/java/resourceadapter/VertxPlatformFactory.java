/**
 * 
 */
package org.vertx.java.resourceadapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

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
   private ConcurrentHashMap<String, Vertx> vertxPlatforms = new ConcurrentHashMap<String, Vertx>();
   
   /**
    * Default private constructor
    */
   private VertxPlatformFactory(){
      
   }

   /**
    * Gets or creates a Vert.x according to configuration.
    * 
    * @param config
    * @return a Vertx embedded environment
    */
   public synchronized Vertx getOrCreateVertx(VertxPlatformConfiguration config)
   {
      Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformKey());
      if (vertx != null)
      {
         log.log(Level.FINEST, "Vert.x platform at address: " + config.getVertxPlatformKey() + " has been started already.");
         return vertx;
      }
      Integer clusterPort = config.getClusterPort();
      String clusterHost = config.getClusterHost();
      if (clusterHost == null || clusterHost.length() == 0)
      {
         log.log(Level.FINEST, "Cluster Host is not set, use '127.0.0.1' by default.");
         clusterHost = "127.0.0.1";
      }
      if (clusterPort == null)
      {
         log.log(Level.FINEST, "Cluster Port is not set, use 0 to random choose available port.");
         clusterPort = Integer.valueOf(0);
      }
      
      log.log(Level.FINEST, "Starts a Vert.x platform at address: " + config.getVertxPlatformKey());
      String clusterFile = config.getClusterConfiguratoinFile();
      
      System.setProperty("vertx.ra.cluster.file", clusterFile); 
      vertx = VertxFactory.newVertx(clusterPort, clusterHost);
      
      this.vertxPlatforms.putIfAbsent(config.getVertxPlatformKey(), vertx);
      return vertx;
   }
   
   /**
    * Stops the Vert.x Platform Manager and removes it from cache.
    * 
    * @param config
    */
   public synchronized void stopPlatformManager(VertxPlatformConfiguration config)
   {
      Vertx vertx = this.vertxPlatforms.remove(config.getVertxPlatformKey());
      if (vertx != null)
      {
         log.log(Level.FINEST, "Stops the Vert.x platform at address: " + config.getVertxPlatformKey());
         vertx.stop();
      }
      else
      {
         log.log(Level.WARNING, "No Vert.x platform found at address: " + config.getVertxPlatformKey());
      }
   }
   
   /**
    * stops all started Vert.x platforms.
    */
   public synchronized void clear()
   {
      for (Map.Entry<String, Vertx> entry: this.vertxPlatforms.entrySet())
      {
         log.log(Level.INFO, "Closing Vert.x Platform at address: " + entry.getKey());
         entry.getValue().stop();
      }
      this.vertxPlatforms.clear();
   }
   
}