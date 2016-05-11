TruffleMate - A Reflective Execution Environment implemented using Oracle's Truffle Framework
=============================================================================================

Introduction
------------

Mate is an approach for building Virtual Machines that expose their whole 
structure and behavior to the language level [Mate].
A MOP[MOP] then enables to adapt reflectively the Virtual Machine behavior at run time.
TruffleMate is an implementation of the Mate approach in Truffle that started as a branch from the [TruffleSOM] VM.

This repository contains an extension to the [Truffle][T]-based implementation of SOM, including
SOM's standard library, a number of examples and the Mate v1 Metaobject protocol[M]. 

Obtaining and Running TruffleMate
--------------------------------

To checkout the code:

    git clone https://github.com/charig/TruffleMate.git

Then, TruffleMate can be build with Ant:

    ant jar

Afterwards, the tests can be executed with:

    ./som.sh -cp Smalltalk TestSuite/TestHarness.som
   
A simple Hello World program is executed with:

    ./som.sh -cp Smalltalk Examples/Hello.som

The --mate -activateMate optiosn runs the system with the Mate MOP enabled.

#When working on TruffleMate, for instance in Eclipse, it is helpful to download
#the source files for Truffle as well:

#    ant develop

Information on previous authors are included in the AUTHORS file. This code is
distributed under the MIT License. Please see the LICENSE file for details.

TruffleMate Implementation
-------------------------

TruffleSOM implements a file-based Smalltalk with most of the language features
common to other Smalltalks. This includes support for objects, classes,
methods, closures/blocks/lambdas, non-local returns, and typical reflective
operations, e.g., method invocation or object field access.

The implementation of TruffleSOM is about 3500 lines of code in size and is a
concise but comprehensive example for how to use the Truffle framework to
implement standard language features.

Its parser creates a custom AST that is geared towards representing the
executable semantics. Thus, we did not include AST nodes that have structural
purpose only. Instead, we concentrated on the AST nodes that are relevant to
express Smalltalk language semantics.

Currently TruffleSOM demonstrates for instance:

 - method invocation: [som.interpreter.nodes.MessageSendNode](hhttps://github.com/SOM-st/TruffleSOM/blob/master/src/som/interpreter/nodes/MessageSendNode.java#L626)
 - usage of Truffle frames
 - argument passing [som.interpreter.nodes.ArgumentInitializationNode](https://github.com/SOM-st/TruffleSOM/blob/master/src/som/interpreter/nodes/ArgumentInitializationNode.java#L24)
 - associating AST tree nodes with source code, e.g., [som.compiler.Parser.unaryMessage(..)](https://github.com/smarr/TruffleSOM/blob/master/src/som/compiler/Parser.java#L652)
 - support for lexical scoping and access from nested blocks, cf.
   ContextualNode subclasses and [som.interpreter.ContextualNode.determineContext(..)](https://github.com/smarr/TruffleSOM/blob/master/src/som/interpreter/nodes/ContextualNode.java#L59)
 - usage of control-flow exception for
     - non-local returns, cf. [som.interpreter.nodes.ReturnNonLocalNode.executeGeneric(..)](https://github.com/smarr/TruffleSOM/blob/master/src/som/interpreter/nodes/ReturnNonLocalNode.java#L68)
       as well as [som.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode.executeGeneric(..)](https://github.com/SOM-st/TruffleSOM/blob/master/src/som/interpreter/nodes/ReturnNonLocalNode.java#L124)
     - looping: [som.interpreter.nodes.specialized.AbstractWhileNode](https://github.com/SOM-st/TruffleSOM/blob/master/src/som/interpreter/nodes/specialized/AbstractWhileNode.java#L62)
       as well as [som.interpreter.nodes.specialized.IntToDoMessageNode](https://github.com/SOM-st/TruffleSOM/blob/master/src/som/interpreter/nodes/specialized/IntToDoMessageNode.java#L52)


Build Status
------------

Thanks to Travis CI, all commits of this repository are tested.
The current build status is: [![Build Status](
https://travis-ci.org/charig/TruffleMATE.png)](https://travis-ci.org/charig/TruffleMATE)

 [SOM]: http://www.hpi.uni-potsdam.de/hirschfeld/projects/som/
 [SOMst]: https://travis-ci.org/SOM-st/
 [Mate]: http://dl.acm.org/citation.cfm?id=2814241
 [TruffleMate]: https://travis-ci.org/charig/TruffleMate
 [TruffleSOM]: https://github.com/SOM-st/TruffleSOM
 [RS]:  http://dx.doi.org/10.1016/j.cl.2005.02.003
 [T]:   http://www.christianwimmer.at/Publications/Wuerthinger12a/
 [M]:   http://dl.acm.org/citation.cfm?id=2814241
