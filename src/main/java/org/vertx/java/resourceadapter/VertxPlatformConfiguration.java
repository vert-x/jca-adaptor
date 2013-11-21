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
   
   private String haGroup;
   
   private Integer quorumSize;
   
   private String clusterConfiguratoinFile;
   
   
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
      result = prime * result + ((haGroup == null) ? 0 : haGroup.hashCode());
      result = prime * result + ((quorumSize == null) ? 0 : quorumSize.hashCode());
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
      if (haGroup == null)
      {
         if (other.haGroup != null)
            return false;
      }
      else if (!haGroup.equals(other.haGroup))
         return false;
      if (quorumSize == null)
      {
         if (other.quorumSize != null)
            return false;
      }
      else if (!quorumSize.equals(other.quorumSize))
         return false;
      return true;
   }
   /**
    * @return the clusterPort
    */
   public Integer getClusterPort()
   {
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
    * @return the haGroup
    */
   public String getHaGroup()
   {
      return haGroup;
   }
   /**
    * @param haGroup the haGroup to set
    */
   public void setHaGroup(String haGroup)
   {
      this.haGroup = haGroup;
   }
   /**
    * @return the quorumSize
    */
   public Integer getQuorumSize()
   {
      return quorumSize;
   }
   /**
    * @param quorumSize the quorumSize to set
    */
   public void setQuorumSize(Integer quorumSize)
   {
      this.quorumSize = quorumSize;
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
   
}
