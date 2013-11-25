/**
 * 
 */
package org.vertx.java.resourceadapter;

import org.vertx.java.core.Vertx;

/**
 *
 *Vertx life cycle listener.
 *
 *Used to be noticed when vertx is created or just get from factory.
 *
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public interface VertxLifecycleListener
{

   /**
    * When vertx is created.
    * 
    * @param vertx the Vert.x
    */
   void onCreate(Vertx vertx);
   
   /**
    * When vertx is just got from the factory.
    * 
    * @param vertx the vertx
    */
   void onGet(Vertx vertx);
   
}
