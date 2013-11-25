vertx-resource-adapter
======================

JCA Adapter for Vertx to interaction between JavaEE application server and Vertx cluster.

Overview
------

The idea of the resource adapter is try to start an embedded Vertx within the JavaEE application server, then expose the Vertx
distributed event bus and shared data as JCA components.

It supports both outbound and inbound vertx communication. 

Outbound communication
------

An application component like a web application(a .war), an ejb instance can send message to the Vertx cluster using outbound communication.

Typical usage is try to get the <b>org.vertx.java.resourceadapter.VertxConnectionFactory</b> using a JNDI lookup, or inject the resource using CDI, 
then gets one <b>org.vertx.java.resourceadapter.VertxConnection</b> instance, then you can get the <b>EventBus</b> or the <b>SharedData</b> which Vertx provides.

<pre>

javax.naming.InitialContext ctx = null;
org.vertx.java.resourceadapter.VertxConnection conn = null;
try
{
   ctx = new javax.naming.InitialContext();
   org.vertx.java.resourceadapter.VertxConnectionFactory connFactory = 
   (org.vertx.java.resourceadapter.VertxConnectionFactory)ctx.lookup("java:/eis/VertxConnectionFactory");
   conn = connFactory.getVertxConnection();
   conn.eventBus().send("outbound-address", "Hello from JCA");
}
catch (Exception e)
{
   e.printStackTrace();
}
finally
{
   if (ctx != null)
   {
      ctx.close();  
   }
   if (conn != null)
   {
      conn.close();  
   }
}
</pre>

   * NOTE: always call *org.vertx.java.resourceadapter.VertxConnection.close()* when you does not need the connection anymore, otherwise the connection pool will be full very soon.

Inbound communication
------

Usually a MDB is the client which receives inbound communication from a Vert.x cluster.

The end point of the MDB implements interface: *org.vertx.java.resourceadapter.inflow.VertxListener*.

<pre>

package org.vertx.java.ra.examples.mdb;

import org.vertx.java.resourceadapter.inflow.VertxListener;
import org.vertx.java.core.eventbus.Message;

import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

import org.jboss.ejb3.annotation.ResourceAdapter;


@MessageDriven(name = "VertxMonitor", 
       messageListenerInterface = VertxListener.class,
       activationConfig = {
                   @ActivationConfigProperty(propertyName = "address", propertyValue = "inbound-address"),
                   @ActivationConfigProperty(propertyName = "clusterHost", propertyValue = "localhost"),
                   @ActivationConfigProperty(propertyName = "clusterPort", propertyValue = "4041"),
                   })
@ResourceAdapter("vertx-resource-adapter-0.0.1.rar")
public class VertxMonitor implements VertxListener {

   private Logger logger = Logger.getLogger(VertxMonitor.class.getName());
   
    /**
     * Default constructor. 
     */
    public VertxMonitor() {
        logger.info("VertxMonitor started.");
    }

   @Override
   public <T> void onMessage(Message<T> message)
   {
      logger.info("Get a message from Vert.x at address: " + message.address());
      T body = message.body();
      if (body != null)
      {
         logger.info("Body of the message: " + body.toString());
      }
   }
}

</pre>


Now, you can send a message in your Vert.x runtime to address: 'inbound-address', and the MDB will get notified.

Configuration
-------

The configuration of outbound and inbound are same, they are:

   * <b>clusterHost</b>
     * Type: java.lang.String
     * <b>clusterHost</b> specifies which network interface the distributed event bus will be bound to. Default to 'localhost'.
   * <b>clusterPort</b>
     * Type: java.lang.Integer
     * <b>clusterPort</b> specifies which port the distributed event bus will be bound to. Default to 0, means random available port.
   * <b>clusterConfiguratoinFile</b>
     * Type: java.lang.String
     * <b>clusterConfiguratoinFile</b> specifies which cluster file will be used to join the vertx cluster. Default to 'cluster.xml'. 
     The resource adapter ships a 'cluster.xml' inside the .rar file, which uses tcp-ip network join on '127.0.0.1'


IronJacamar
-------

[IronJacamar](http://www.ironjacamar.org/) is the top lead JCA implementation in the industry, it supports JCA 1.0/1.5/1.6/1.7, and is adopted by [WildFly](http://www.wildfly.org/) application server.
   
The shipped .rar file contains an ironjacamar.xml file in 'META-INF/ironjacamar.xml', which will active by default if you deploy it to a WildFly application server.


			<ironjacamar xmlns="http://www.ironjacamar.org/doc/schema"
			             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			             xsi:schemaLocation="http://www.ironjacamar.org/doc/schema 
			             http://www.ironjacamar.org/doc/schema/ironjacamar_1_1.xsd">
			             
			  <transaction-support>NoTransaction</transaction-support>
			  
			  <connection-definitions>
			    <connection-definition class-name="org.vertx.java.resourceadapter.VertxManagedConnectionFactory" jndi-name="java:/eis/VertxConnectionFactory" pool-name="VertxConnectionFactory">
			      <config-property name="clusterHost">localhost</config-property>
			      <config-property name="clusterPort">4041</config-property>
			    </connection-definition>
			  </connection-definitions>
			
			</ironjacamar>


Build
-------

It uses gradle for the building, change your current working directory to the codes, then run the command:

> ./gradlew 

It will generate the resource adapter file (.rar file) in the *build/libs/* directory.


Where to get the binarary
-------
TODO

Examples
-------
TODO

Known Issues
-------

   * The packages of this resource adapter start with *org.vertx*, because it should be a main feature of Vert.x, but it does not get included yet, before that,
    it is a problem to use that package names.

   * This resource adapter uses it's own ClusterManagerFactory to be able to use different cluster files than 'cluster.xml' or/and 'default-cluster.xml',
 so you need to copy the vertx-resource-adapter.jar to &lt;Your Vertx Home&gt;/lib/ directory.


So Have fun!