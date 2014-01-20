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
then gets one <b>org.vertx.java.resourceadapter.VertxConnection</b> instance, then you can get the Vertx <b>EventBus</b> to send messages.

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
                   @ActivationConfigProperty(propertyName = "clusterPort", propertyValue = "0"),
                   })
@ResourceAdapter("vertx-resource-adapter-1.0.1.rar")
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
     * Outbound / Inbound
     * <b>clusterHost</b> specifies which network interface the distributed event bus will be bound to. Default to 'localhost'.
   * <b>clusterPort</b>
     * Type: java.lang.Integer
     * Outbound / Inbound
     * <b>clusterPort</b> specifies which port the distributed event bus will be bound to. Default to 0, means random available port.
   * <b>clusterConfigFile</b>
     * Type: java.lang.String
     * Outbound / Inbound
     * <b>clusterConfigFile</b> specifies which cluster file will be used to join the vertx cluster. 'default-cluster.xml' shipped with the resource adapter will be used if it is not specified. It can be either a file absolute path, or a system property using expression like: '${cluster.config.file}'.
     The resource adapter ships a 'default-cluster.xml' inside the .rar file, which uses tcp-ip network join on '127.0.0.1'
   * <b>timeout</b>
     * Type: java.lang.Long
     * Outbound / Inbound
     * <b>timeout</b> specifies the milliseconds timeout waiting for the Vert.x starts up. Default to 30000, 30 seconds.
   * <b>address</b>
     * Type: java.lang.String
     * Inbound Only
     * Not null
     * <b>address</b> specifies in which vertx event bus address the Endpoint(MDB) listen.


Credits to IronJacamar
-------

[IronJacamar](http://www.ironjacamar.org/) is the top lead JCA implementation in the industry, it supports JCA 1.0/1.5/1.6/1.7, and is adopted by [WildFly](http://www.wildfly.org/) application server.
   
This resource adapter uses IronJacamar as the development and test environment.


Building
-------

It uses gradle for the building, change your current working directory to the codes, then run the command:

> ./gradlew rar 

It will generate the resource adapter file (.rar file) in the *build/libs/* directory.



Deploy to Wildfly
-------
Follow the steps below to deploy the resource adapter to WildFly application server:
   * Build it from source or download from the [Bintary](https://bintray.com/gaol/downloads/vertx-resource-adapter)
   * Starts the WildFly application server.
      >> bin/standalone.sh -c standalone-full.xml
   * Deploy the .rar file
     >> bin/jboss-cli.sh --connect --command="deploy <PATH-TO-VERTX-RESOURCE-ADAPTER.rar>"
   * Change the 'etc/wildfly-ra-sample.cli' according to your own settings
      * Check the .rar file name
      * Check the jndi name 
      * Check the cluster configuration file path
   * Enable the resource adapter
    >> bin/jboss-cli.sh --connect --file=<PATH-TO-YOUR-wildfly-ra-sample.cli>

Downloads
-------
You can download the resouce adapter from: [Bintary](https://bintray.com/gaol/downloads/vertx-resource-adapter)

Examples
-------
TODO


If you get any issues or suggestions, you are appreciated to share the idea by firing an issue [here](https://github.com/gaol/vertx-resource-adapter/issues/new)

Have fun!
