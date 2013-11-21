/**
 * 
 */
package org.vertx.java.resourceadapter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

/**
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxPlatformFactory
{
   
   private static Logger log = Logger.getLogger(VertxPlatformFactory.class.getName());
   
   public static VertxPlatformFactory instance()
   {
      return new VertxPlatformFactory();
   }
   
   /**
    * Default private constructor
    */
   private VertxPlatformFactory(){
      
   }

   public PlatformManager getVertxPlatformManager(VertxPlatformConfiguration config)
   {
      PlatformManager platformManager = null;
      Integer clusterPort = config.getClusterPort();
      String clusterHost = config.getClusterHost();
      String haGroup = config.getHaGroup();
      Integer quorumSize = config.getQuorumSize();
      if (clusterHost == null || clusterHost.length() == 0)
      {
         log.log(Level.INFO, "Cluster Host is not set, use 'localhost' by default.");
         clusterHost = "localhost";
      }
      if (clusterPort == null)
      {
         log.log(Level.INFO, "Cluster Port is not set, use 0 to random choose available port.");
         clusterPort = Integer.valueOf(0);
      }
      if (haGroup != null && haGroup.length() > 0)
      {
         // cluster with ha
         platformManager = PlatformLocator.factory.createPlatformManager(clusterPort, clusterHost, quorumSize, haGroup);
      }
      else
      {
         // cluster without ha
         platformManager = PlatformLocator.factory.createPlatformManager(clusterPort, clusterHost);
      }
      return platformManager;
   }
}
