/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2013, Red Hat Inc, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.vertx.java.resourceadapter.inflow;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.resourceadapter.VertxLifecycleListener;
import org.vertx.java.resourceadapter.VertxPlatformFactory;
import org.vertx.java.resourceadapter.VertxResourceAdapter;

/**
 * VertxActivation
 *
 * @version $Revision: $
 */
public class VertxActivation implements VertxLifecycleListener
{

   private static Logger log = Logger.getLogger(VertxActivation.class.getName());
   
   /** The resource adapter */
   private VertxResourceAdapter ra;

   /** Activation spec */
   private VertxActivationSpec spec;

   /** The message endpoint factory */
   private MessageEndpointFactory endpointFactory;
   
   /**
    * Whether delivery is active
    */
   private final AtomicBoolean deliveryActive = new AtomicBoolean(false);
   
   
   static 
   {
      try
      {
         VertxListener.class.getMethod("onMessage", new Class[] { Message.class });
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * Default constructor
    * @exception ResourceException Thrown if an error occurs
    */
   public VertxActivation() throws ResourceException
   {
      this(null, null, null);
   }

   /**
    * Constructor
    * @param ra VertxResourceAdapter
    * @param endpointFactory MessageEndpointFactory
    * @param spec VertxActivationSpec
    * @exception ResourceException Thrown if an error occurs
    */
   public VertxActivation(VertxResourceAdapter ra, 
      MessageEndpointFactory endpointFactory,
      VertxActivationSpec spec) throws ResourceException

   {
      this.ra = ra;
      this.endpointFactory = endpointFactory;
      this.spec = spec;
   }

   /**
    * Get activation spec class
    * @return Activation spec
    */
   public VertxActivationSpec getActivationSpec()
   {
      return spec;
   }

   /**
    * Get message endpoint factory
    * @return Message endpoint factory
    */
   public MessageEndpointFactory getMessageEndpointFactory()
   {
      return endpointFactory;
   }

   /**
    * Start the activation
    * @throws ResourceException Thrown if an error occurs
    */
   public void start() throws ResourceException
   {
      deliveryActive.set(true);
      this.ra.getWorkManager().scheduleWork(new SetupActivation());
   }

   
   private synchronized void setup() throws Exception
   {
      
      VertxPlatformFactory.instance().createVertxIfNotStart(this.spec.getVertxPlatformConfig(), this);
   }
   
   @Override
   public synchronized void onCreate(Vertx vertx)
   {
      String address = this.spec.getAddress();
      try
      {
         final MessageEndpoint endPoint = this.endpointFactory.createEndpoint(null);
         log.log(Level.INFO, "Endpoint created, register Vertx handler on address: " + address);
         vertx.eventBus().registerHandler(address, new Handler<Message<?>>()
         {
            public void handle(Message<?> message) {
               ((VertxListener)endPoint).onMessage(message);
            };
         });
      }
      catch (Exception e)
      {
         throw new RuntimeException("Can't create the endpoint.", e);
      }
      
   }
   
   @Override
   public synchronized void onGet(Vertx vertx)
   {
      log.log(Level.FINEST, "Nothing to do.");
   }
   
   /**
    * Stop the activation
    */
   public void stop()
   {
      deliveryActive.set(false);
      try
      {
         this.ra.getWorkManager().scheduleWork(new StopActivation());
      }
      catch (WorkException e)
      {
         throw new RuntimeException("Can't stop the Vert.x platform.", e);
      }
   }
   
   private synchronized void tearDown()
   {
      VertxPlatformFactory.instance().stopPlatformManager(this.spec.getVertxPlatformConfig());
   }
   
   private class StopActivation implements Work
   {

      @Override
      public void run()
      {
         try
         {
            tearDown();
         }
         catch (Exception e)
         {
            throw new RuntimeException("Can't stop the Vert.x platform.", e);
         }
      }

      @Override
      public void release()
      {
      }
      
   }
   
   private class SetupActivation implements Work
   {

      @Override
      public void run()
      {
         try
         {
            setup();
         }
         catch (Exception e)
         {
            throw new RuntimeException("Can't start the Vert.x platform.", e);
         }
      }

      @Override
      public void release()
      {
         
      }
      
   }


}
