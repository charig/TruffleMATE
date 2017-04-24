Fully Reflective Execution Environments: Towards a New Generation of Virtual Machines
=============================================================================================

Introduction
------------

This branch contains the version of TruffleMate used for executing the experiments in the paper submitted to the [12th Workshop on Implementation, Compilation, Optimization of Object-Oriented Languages, Programs and Systems (ICOOOLPS 2017)][ICOOOLPS]. 

Obtaining and Running TruffleMate
--------------------------------

To checkout the code:

    git clone -b papers/ICOOOLPS17 https://github.com/charig/TruffleMate.git
    cd ICOOOLPS17

Then, to build TruffleMate with Ant:

    ant jar or ant compile

Afterwards, the tests can be executed with:

    ./som -G -cp Smalltalk:TestSuite TestHarness (Mate disabled mode)
    ./som -G --mate -activateMate -cp Smalltalk:TestSuite:Smalltalk/Mate:Smalltalk/Mate/MOP TestHarness (Mate enabled)
   
A simple Hello World program is executed with:

    ./som -G --mate -activateMate -cp Smalltalk:TestSuite:Smalltalk/Mate:Smalltalk/Mate/MOP:Smalltalk/Mate/Compiler:Examples Hello

The --mate -activateMate options make the system run with the Mate Metaobject Protocol (MOP) enabled. Note that the classpath must be also extended with the MOP-related classes found under Smalltalk/MATE. The -G runs TruffleMATE in interpreter mode. For running in combination with GRAAL, the [Graal][GRAAL] compiler must be installed in the system. Compiled binaries can be gathered from Oracle's [OTM][OTM]. Then the -G option can be removed.

When working on TruffleMate, for instance in Eclipse, it is helpful to download
the source files for Truffle as well:

    ant develop

Information on previous authors are included in the AUTHORS file. This code is
distributed under the MIT License. Please see the LICENSE file for details.

Running the experiments
-------------------------

[Click here](Documentation/Experiments.md) for a detailed description of all the benchmarks and examples described in the paper. The documentation also contains precise instructions on how to run them.

Build Status
------------

Thanks to Travis CI, all commits of this repository are tested.
The current build status is: [![Build Status](
https://travis-ci.org/charig/TruffleMATE.png)](https://travis-ci.org/charig/TruffleMATE.svg?branch=TSE2017)

 [ICOOOLPS]: http://2017.ecoop.org/track/ICOOOLPS-2017-papers
 [GRAAL]: https://github.com/graalvm/graal-core
 [OTM]: http://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html
