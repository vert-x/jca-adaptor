/**
 * 
 */
package org.vertx.java.resourceadapter;

import java.io.Serializable;

/**
 * 
 * VertxPlatformConfiguration is used to create an embedded Vertx Platform.
 * 
 * @author Lin Gao <lgao@redhat.com>
 *
 */
public class VertxPlatformConfiguration implements Serializable
{
   
   /**
    * generated serial version UUID
    */
   private static final long serialVersionUID = -2647099599010357452L;
   
   private Integer clusterPort;
   
   private String clusterHost;
   
   private String clusterConfiguratoinFile;
   
   /** Timeout in milliseconds waiting for the Vert.x starts up. Default to 30000, 30 seconds **/
   private Long timeout;
   
   /**
    * @return the timeout
    */
   public Long getTimeout()
   {
      return timeout;
   }

   /**
    * @param timeout the timeout to set
    */
   public void setTimeout(Long timeout)
   {
      this.timeout = timeout;
   }

   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder(getVertxPlatformIdentifier());
      if (timeout != null)
      {
         sb.append(":timeout[");
         sb.append(timeout);
         sb.append("]");
      }
      return sb.toString();
   }
      
   public String getVertxPlatformIdentifier()
   {
      StringBuilder sb = new StringBuilder();
      sb.append(getClusterHost());
      sb.append(":");
      sb.append(getClusterPort());
      if (this.clusterConfiguratoinFile != null && this.clusterConfiguratoinFile.length() > 0)
      {
         sb.append(":clusterFile[");
         sb.append(this.clusterConfiguratoinFile);
         sb.append("]");
      }
      return sb.toString();
   }
  
   /**
    * @return the clusterPort
    */
   public Integer getClusterPort()
   {
      if (clusterPort == null)
      {
         return Integer.valueOf(0);
      }
      return clusterPort;
   }
   /**
    * @param clusterPort the clusterPort to set
    */
   public void setClusterPort(Integer clusterPort)
   {
      this.clusterPort = clusterPort;
   }
   /**
    * @return the clusterHost
    */
   public String getClusterHost()
   {
      if (clusterHost == null || clusterHost.trim().length() == 0)
      {
         return "localhost";
      }
      return clusterHost;
   }
   /**
    * @param clusterHost the clusterHost to set
    */
   public void setClusterHost(String clusterHost)
   {
      this.clusterHost = clusterHost;
   }
   /**
    * @return the clusterConfiguratoinFile
    */
   public String getClusterConfiguratoinFile()
   {
      return clusterConfiguratoinFile;
   }
   /**
    * @param clusterConfiguratoinFile the clusterConfiguratoinFile to set
    */
   public void setClusterConfiguratoinFile(String clusterConfiguratoinFile)
   {
      this.clusterConfiguratoinFile = clusterConfiguratoinFile;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clusterConfiguratoinFile == null) ? 0 : clusterConfiguratoinFile.hashCode());
      result = prime * result + ((clusterHost == null) ? 0 : clusterHost.hashCode());
      result = prime * result + ((clusterPort == null) ? 0 : clusterPort.hashCode());
      result = prime * result + ((timeout == null) ? 0 : timeout.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      VertxPlatformConfiguration other = (VertxPlatformConfiguration) obj;
      if (clusterConfiguratoinFile == null)
      {
         if (other.clusterConfiguratoinFile != null)
            return false;
      }
      else if (!clusterConfiguratoinFile.equals(other.clusterConfiguratoinFile))
         return false;
      if (clusterHost == null)
      {
         if (other.clusterHost != null)
            return false;
      }
      else if (!clusterHost.equals(other.clusterHost))
         return false;
      if (clusterPort == null)
      {
         if (other.clusterPort != null)
            return false;
      }
      else if (!clusterPort.equals(other.clusterPort))
         return false;
      if (timeout == null)
      {
         if (other.timeout != null)
            return false;
      }
      else if (!timeout.equals(other.timeout))
         return false;
      return true;
   }
   
}
