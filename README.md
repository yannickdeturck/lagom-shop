# Lagom Shop
This Lagom project contains two services
* *Item service* that serves as an API for creating and looking up items
* *Order service* that served as an API for creating and looking up orders for certain items

The project also contains a frontend written in Play with multiple screens for working with items and orders.

## Setup
Install sbt
```
brew install sbt
```

Navigate to the project and run `$ sbt`

Start up the project by executing `$ runAll`

## Importing the project in an IDE
Import the project as an sbt project in your IDE.

This project uses the [Immutables](https://immutables.github.io) library, be sure to consult [Set up Immutables in your IDE](http://www.lagomframework.com/documentation/1.0.x/ImmutablesInIDEs.html).