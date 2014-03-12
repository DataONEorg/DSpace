# Installation instructions #

Installation of a DataONE member node requires the following steps to be completed.

1.) Adjustment of DSpace maven poms with the appropriate settings
2.) Addition of dataone configuration files to the dspace config directory
3.) Copying of the dataone "log" solr configuration into the dspace solr configuration
4.) Alteration of the dspace discovery solr schema.xml to support additional DSpace Object types.
5.) Runing DataONE intialization scripts to populate DSpace version history, discovery and dataone logging core.

## Maven Artifact Locations ##

For convenience sake, dspace-dataone maven artifacts reside in a git repository on github:

```
[GIT](https://github.com/TBD/mvn-repo.git)
 ```
## Build Configuration ##

Build configuration require that you supply dependencies and addon module pom.xml file within your build source.

Changes are neccessary to the following files

* [dspace-parent/pom.xml](../pom.xml)
* [dspace/modules/pom.xml](../dspace/modules/pom.xml)
* [dspace/modules/additions/pom.xml](../dspace/modules/additions/pom.xml)
* [dspace/modules/xmlui/pom.xml](../dspace/modules/xmlui/pom.xml)

One new pom will be needed to be added to complete support for DataONE member node services:

* [dspace/modules/dataone/pom.xml](../dspace/modules/dataone/pom.xml) 

### dspace-parent/pom.xml Changes ###

Add the following dependency management versions to the [dspace-parent.pom](../pom.xml). These changes provide version numbers to the rest of the dependencies added below.

```
          <dependency>
             <groupId>org.dspace</groupId>
             <artifactId>dspace-dataone-api</artifactId>
             <version>4.2</version>
             <type>jar</type>
          </dependency>
          <dependency>
             <groupId>org.dspace</groupId>
             <artifactId>dspace-dataone-xmlui</artifactId>
             <version>4.2</version>
             <type>jar</type>
             <classifier>classes</classifier>
          </dependency>
          <dependency>
             <groupId>org.dspace</groupId>
             <artifactId>dspace-dataone-xmlui</artifactId>
             <version>4.2</version>
             <type>war</type>
          </dependency>
```

### dspace/modules/pom.xml POM Changes ###

Additional build module sections should be added to support the new sesame and workbench webapplications.

```
        <profile>
            <id>dspace-dataone</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <modules>
                <module>dataone</module>
            </modules>
        </profile>
```
        
### dspace/modules/additions/pom.xml POM Changes ###

Add the following in [additions.pom](../dspace/modules/additions/pom.xml)

Under the repositories section 
```
   <repositories>
        <repository>
            <id>dataone-release-repository</id>
            <name>DSpace DataONE Release Repository</name>
            <url>https://raw.githubusercontent.com/TBD/mvn-repo/master/releases</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
   </repositories>
```   
Add the following under the dependencies section

```
  <dependency>
     <groupId>org.dspace</groupId>
     <artifactId>dspace-dataone-api</artifactId>
  </dependency>
```

### dspace/modules/xmlui/pom.xml POM Changes ###

Add the following under the dependency section in [xmlui.pom](../dspace/modules/xmlui/pom.xml)

Under the repositories section 
```
   <repositories>
        <repository>
            <id>dataone-release-repository</id>
            <name>DSpace DataONE Release Repository</name>
            <url>https://raw.githubusercontent.com/TBD/mvn-repo/master/releases</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
   </repositories>
```

Add the following under the dependencies section

```
  <dependency>
     <groupId>org.dspace</groupId>
     <artifactId>dspace-dataone-xmlui</artifactId>
     <type>war</type>
  </dependency>

  <dependency>
     <groupId>org.dspace</groupId>
     <artifactId>dspace-dataone-xmlui</artifactId>
     <type>jar</type>
     <classifier>classes</classifier>
  </dependency>
```

### dspace/modules/dataone/pom.xml POM Changes ###

Create the dataone module directory inside dspace/modules

Add the following dataone maven [dspace/modules/dataone/pom.xml](../dspace/modules/dataone/pom.xml) to dspace/modules/dataone

## Configuration in dspace/config/modules/dataone.cfg ##

To properly configure the Member Node the following properties need to be acquired from DataONE through registration and provided in the following file [dspace/config/modules/dataone.cfg](../dspace/config/modules/dataone.cfg)

```
### DataONE Member Node support ###

node.identifier=${dspace.url}
node.name=${dspace.name}
node.description=${dspace.name}
node.subject=CN=urn:node:MyNode, DC=dataone, DC=org
node.contact=CN=User Principle,O=My University,C=US,DC=cilogon,DC=org

# Location of Solr Core used for DataOne Logging Service
server = ${solr.server}/dataone
```
## Configuration in dspace.cfg ##

Enabling versioning consumer in dspace.cfg:

The consumer should be enabled by adding versioning to the list of consumers in dspace.cfg if it is not present already.
```
eg: event.dispatcher.default.consumers = versioning, search, browse, eperson, harvester
```

## Adustments to DSpace Discovery Solr Schema ##

"WiP"

## Adjustments for Solr DataONE log core ##

"WiP"

## Commandline Support for reindexing DataONE Logs ##

Commandline routine to generate initial versions of Items and populate solr log core

From the bin directory of deployment folder, run the following
```
./dspace dsrun org.dspace.versioning.ImportVersion2Bitstream

```

## Commandline Support for reindexing Discovery to populate Bitstream records ##

DataONE leverages indexing of Bitstreams as individual records to allow for search and retrieval of previous versions of Items. Discovery must be fully reindexed using the following command.
From the bin directory of deployment folder, run the following
```
./dspace index-discovery -f

```