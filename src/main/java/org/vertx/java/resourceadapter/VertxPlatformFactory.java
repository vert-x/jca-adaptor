/**
 * 
 */
package org.vertx.java.resourceadapter;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.impl.ConcurrentHashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

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
   
   
   private File currentCtxLoaderDir;
   
   
   private Lock lock = new ReentrantLock();
   
   /**
    * Default private constructor
    */
   private VertxPlatformFactory(){
      
   }

   /**
    * @param currentCtxLoaderDir the currentCtxLoaderDir to set
    */
   public void setCurrentCtxLoaderDir(File currentCtxLoaderDir)
   {
      lock.lock();
      try
      {
         this.currentCtxLoaderDir = currentCtxLoaderDir;
      }
      finally
      {
         lock.unlock();
      }
      
   }

   /**
    * Creates a Vertx if one is not started yet.
    * 
    * @param config the configuration to start a vertx
    * @param lifecyleListener the vertx lifecycle listener
    */
   public void createVertxIfNotStart(final VertxPlatformConfiguration config, final VertxListener lifecyleListener)
   {
      lock.lock();
      Vertx vertx = this.vertxPlatforms.get(config.getVertxPlatformIdentifier());
      if (vertx != null)
      {
         lock.unlock();
         log.log(Level.INFO, "Vert.x platform at: " + config.getVertxPlatformIdentifier() + " has been started.");
         lifecyleListener.whenReady(vertx);
         return;
      }
      try
      {
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
         
         ClassLoader currentCtxLoader = SecurityActions.getContextClassLoader();
         URLClassLoader clusterClassLoader = null;
         
         String clusterFile = config.getClusterConfigFile();
         File tmpClusterFile = null;
         if (clusterFile != null && clusterFile.length() > 0)
         {
            if (SecurityActions.isExpression(clusterFile))
            {
               clusterFile = SecurityActions.getExpressValue(clusterFile);
            }
            log.log(Level.INFO, "Using Cluster file: " + clusterFile);
            
            // make sure the tmp context loader directory is ready
            if (currentCtxLoaderDir == null)
            {
               log.log(Level.FINEST, "Using system temporary directory.");
               currentCtxLoaderDir = new File(SecurityActions.getSystemProperty("java.io.tmpdir"));
            }
            if (!currentCtxLoaderDir.exists())
            {
               log.log(Level.FINEST, currentCtxLoaderDir.getAbsolutePath() + " does not exist, create it.");
               if (!currentCtxLoaderDir.mkdirs())
               {
                  throw new IllegalStateException("Can't create the directory: " + currentCtxLoaderDir.getAbsolutePath());
               }
            }
            else
            {
               if (!currentCtxLoaderDir.isDirectory())
               {
                  throw new IllegalArgumentException(currentCtxLoaderDir.getAbsoluteFile() + " is not a valid directory");
               }
               if (!currentCtxLoaderDir.canRead())
               {
                  throw new IllegalStateException("Can't read the directory: " + currentCtxLoaderDir.getAbsolutePath());
               }
            }
            
            try
            {
               tmpClusterFile = new File(currentCtxLoaderDir, "cluster.xml");
               copyFile(new File(clusterFile), tmpClusterFile);
               
               URL clusterConfigDir = this.currentCtxLoaderDir.toURI().toURL();
               clusterClassLoader = new URLClassLoader(new URL[]{clusterConfigDir}, currentCtxLoader);
            }
            catch (IOException e)
            {
               throw new IllegalArgumentException("Cluster configuration directory is not valid.", e);
            }
         }
         
         try
         {
            if (clusterFile != null && clusterFile.length() > 0)
            {
               SecurityActions.setCurrentContextClassLoader(clusterClassLoader);
            }
            final CountDownLatch vertxStartCount = new CountDownLatch(1);
            VertxFactory.newVertx(clusterPort, clusterHost, new Handler<AsyncResult<Vertx>>()
                  {
                     @Override
                     public void handle(final AsyncResult<Vertx> result)
                     {
                        try
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
                        finally
                        {
                           vertxStartCount.countDown();
                        }
                     }
                  });
            vertxStartCount.await(); // waiting for the vertx starts up.
         }
         finally
         {
            lock.unlock();
            if (clusterFile != null && clusterFile.length() > 0)
            {
               SecurityActions.setCurrentContextClassLoader(currentCtxLoader);
               if (tmpClusterFile != null)
               {
                  try
                  {
                     tmpClusterFile.delete();
                  }
                  catch (Exception delE)
                  {
                     log.log(Level.WARNING, "Can't remove the temporary cluster file: " + tmpClusterFile.getAbsolutePath(), delE);
                  }
               }
            }
         }
      }
      catch(Exception exp)
      {
         lock.unlock();
         throw new RuntimeException(exp);
      }
   }
   
   private void copyFile(File in, File out) throws IOException
   {
      InputStream input = new FileInputStream(in);
      OutputStream output = new FileOutputStream(out);
      byte[] buffer = new byte[4096];
      int readLength;
      try
      {
         while((readLength = input.read(buffer, 0, buffer.length)) != -1) 
         {
            output.write(buffer, 0, readLength);
         }
         output.flush();
      }
      finally
      {
         input.close();
         output.close();
      }
   }



   /**
    * Adds VertxHolder to be recorded.
    * 
    * @param holder the VertxHolder
    */
   public void addVertxHolder(VertxHolder holder)
   {
      lock.lock();
      try
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
      finally
      {
         lock.unlock();
      }
   }
   
   /**
    * Removes the VertxHolder from recorded.
    * 
    * @param holder the VertxHolder
    */
   public void removeVertxHolder(VertxHolder holder)
   {
      lock.lock();
      try
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
      finally
      {
         lock.unlock();
      }
   }
   
   /**
    * Stops the Vert.x Platform Manager and removes it from cache.
    * 
    * @param config
    */
   public void stopPlatformManager(VertxPlatformConfiguration config)
   {
      lock.lock();
      try
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
      finally
      {
         lock.unlock();
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
   void clear()
   {
      lock.lock();
      try
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
      finally
      {
         lock.unlock();
      }
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