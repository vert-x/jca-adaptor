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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.resourceadapter.VertxPlatformFactory;
import org.vertx.java.resourceadapter.VertxResourceAdapter;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

/**
 * VertxActivation
 *
 * @version $Revision: $
 */
public class VertxActivation
{

   /** The resource adapter */
   private VertxResourceAdapter ra;

   /** Activation spec */
   private VertxActivationSpec spec;

   /** The message endpoint factory */
   private MessageEndpointFactory endpointFactory;
   
   private PlatformManager platformManager;

   /**
    * Whether delivery is active
    */
   private final AtomicBoolean deliveryActive = new AtomicBoolean(false);
   
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
      this.ra.getBootStrapCtx().getWorkManager().scheduleWork(new SetupAction());
   }

   
   private synchronized void setup() throws Exception
   {
      this.platformManager = VertxPlatformFactory.instance().getVertxPlatformManager(this.spec.getVertxPlatformConfig());
      if (this.platformManager == null)
      {
         throw new ResourceException("No Vertx platform started.");
      }
      String address = this.spec.getAddress();
      final MessageEndpoint endPoint = this.endpointFactory.createEndpoint(null);
      platformManager.vertx().eventBus().registerHandler(address, new Handler<Message<?>>()
      {
         public void handle(Message<?> message) {
            ((Handler<Message<?>>)endPoint).handle(message);
         };
      });
   }
   
   /**
    * Stop the activation
    */
   public void stop()
   {
      deliveryActive.set(false);
      tearDown();
   }
   
   private synchronized void tearDown()
   {
      if (this.platformManager != null)
      {
         this.platformManager.stop();
         this.platformManager = null;
      }
   }

   private void handlerThrowable(Throwable t)
   {
      // TODO handle Throwable.
      
   }
   
   private class SetupAction implements Work
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
            handlerThrowable(e);
         }
      }

      @Override
      public void release()
      {
         
      }
      
   }


}
