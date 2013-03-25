Entropy to Btrplace
===============================

This module allows to convert configurations in the entropy protobuf format
to models and constraints that can be used by btrplace.

It can be used through an API or a standalone application

## Conversion rules ##

 * VM and node templates are declared as an attribute named "template".
 * Node IP and Mac are declared as as attributes named "ip" and "mac".
 * For each VM and node, the original name of the element inside entropy is
   stored in the "entropy_id" attribute.
 * The number of CPUs for the nodes and the VMs is converted to a
   ShareableResource view having a "nbCpus" resource identifier. No mapping
   is performed for this resource by default as Entropy ignores it.
 * The memory capacity (usage) of the nodes (VMs) is converted to a
   `ShareableResource` view having a "memory" resource identifier. The mapping
   between the VM consumption and the node capacity is performed with an
   Overbook constraint having an overloading factor of 1.
 * The uCPU capacity (consumption) of the nodes (VMs) is converted to a
   `ShareableResource` view having a "uCpu" resource identifier. The
   mapping between the VM consumption and the node capacity is performed with an
   Overbook constraint having an overloading factor of 1.
 * When the next state for the elements is provided through another
   configuration, `Running`, `Sleeping`, `Ready`, `Killed`, `Online`, or
   `Offline` constraints are inserted to indicate the state changes.

## Usage as a standalone application ##

Download the last release of the application, and uncompress it.
The `entroPlace` script can then be used to convert entropy configuration
into btrplace instances:

  $ ./entroPlace

## Embedding ##

A maven artifact is available through a private repository
so you have first to edit your `pom.xml` to declare it:

<repositories>
    <repository>
        <id>btrp-releases</id>
        <url>http://btrp.inria.fr:8080/repos/releases</url>
    </repository>
    <repository>
        <id>btrp-snapshots</id>
        <url>http://btrp.inria.fr:8080/repos/snapshot-releases</url>
    </repository>
</repositories>

Next, just declare the dependency:

<dependency>
   <groupId>btrplace</groupId>
   <artifactId>from-entropy</artifactId>
   <version>1.1</version>
</dependency>

The API documentation is directly available online:

* Last snapshot version: http://btrp.inria.fr:8080/apidocs/snapshots/from-entropy
* Released versions: http://btrp.inria.fr:8080/apidocs/releases/btrplace/from-entropy

## Building from sources ##

Requirements:
* JDK 6+
* maven 3+

The source of the released versions are directly available in the `Tag` section.
You can also download them using github features.
Once downloaded, move to the source directory then execute the following command
to make the jar:

    $ mvn clean install

If the build succeeded, the resulting jar will be automatically
installed in your local maven repository and available in the `target` sub-folder.

Copyright
-------------------------------
Copyright (c) 2013 University of Nice-Sophia Antipolis. See `LICENSE.txt` for details
