MessagePack-RPC for Java
========================

## Overview

The Java implementation of MessagePack-RPC.

## Implemented Features

Currently, these features are supported:

  - Asynchronous Call API
  - TCP/UDP Transport support
  - Scalable event-driven architecture

## Dependencies

MessagePack-RPC for Java requires the following packages:

  - jackson-dataformat-msgpack - https://github.com/msgpack/msgpack-java
  - Netty - http://netty.io/
  - A SLF4J-compatible logging framework (log4j used by default) - http://slf4j.org/

The gradle build will automatically download and include the maven dependencies.

## Build from the source

Gradle is used to build the project. The gradlew script is a gradle wrapper which
will download/install gradle if necessary.

    ./gradlew build

## License

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
