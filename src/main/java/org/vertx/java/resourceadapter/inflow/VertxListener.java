/**
 * 
 */
package org.vertx.java.resourceadapter.inflow;

import org.vertx.java.core.eventbus.Message;

/**
 * 
 * Listener on the Vert.x message.
 * 
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public interface VertxListener
{

   /**
    * On Vertx Message.
    * 
    * @param message the message sent from vertx platform.
    */
   <T> void onMessage(Message<T> message);
   
}
