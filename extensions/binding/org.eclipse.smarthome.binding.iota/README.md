# IOTA Binding

This binding allows to bind a IOTA topic to a Thing. A bridge connection needs to be already defined, i.e you first need to create an IOTA bridge thing.
The way this binding works is heavily inspired by the MQTT generic binding that maps JSON data to channels.

## Supported Bridge

* **IOTA BRIDGE**: This bridge represents an instance that connects to an IOTA node. 

## Supported Thing

There is one thing available ("topic"), where you can add the following channels to:

## Supported Channels

* **text**: This channel can show the received text on the given topic.
* **number**: This channel can show the received number on the given topic. It can have a min, max and step values.
* **percentage**: This channel handles numeric values as percentages. It can have a min, max and step values.
* **onoff**: This channel represents a on/off state of a given topic.

## Thing and Channel configuration

The bridge only needs a **protocol**, **host** and **port**. A thing offers a few parameters for the configuration: the frequence at which it refreshes the data, the mode it uses for MAM, and the root address from which to start fetching the data. 

All thing channels support JSON/XML unpacking: Usually a IOTA topic state represents a plain value like a text or a number.

### Common channel configuration parameters

* __stateTopic__: The IOTA topic that represents the state of the thing. For instance, if you're tracking a sensor that sends humidity related data on IOTA's distributed ledger through the io.iota bundle, the stateTopic will be **humidity**. 

### Work In Progress:

* __transformationPattern__: An optional transformation pattern like [JSONPath](http://goessner.net/articles/JsonPath/index.html#e2). Use http://jsonpath.com/ to verify your pattern for the latter case. An example for a received JSON from a IOTA state topic would be a pattern of JSONPATH:$.thingNumber.status.state for a json `[{"NAME": name, "STATUS": { "TOPIC": topic, "STATE": state, "TIME": time }}]` to extract the temperature value.


 
### Channel Type "number"
 
* __min__: A minimum value, necessary if the thing channel is used as a Rollershutter or Dimmer.
* __max__: A maximum value, necessary if the thing channel is used as a Rollershutter or Dimmer.
* __step__: Because Rollershutter and Dimmer can send decrease, increase commands, we need to know the step.
* __isfloat__: If set to true the value is send as a decimal value, otherwise it is send as integer.

If any of the parameters is a float/double (has a decimal point value), then a float value is send to the IOTA topic otherwise an int value is send.

You can connect this channel to a Number item.

### Channel Type "percentage"
 
* __min__: A minimum value, necessary if the thing channel is used as a Rollershutter or Dimmer.
* __max__: A maximum value, necessary if the thing channel is used as a Rollershutter or Dimmer.
* __step__: Because Rollershutter and Dimmer can send decrease, increase commands, we need to know the step.
* __isfloat__: If set to true the value is send as a decimal value, otherwise it is send as integer.

If any of the parameters is a float/double (has a decimal point value), then a float value is send to the IOTA topic otherwise an int value is send.

You can connect this channel to a Rollershutter or Dimmer item.

### Channel Type "onoff"

* __on__: A number (like 1, 10) or a string (like ON) that is recognized as on state.
* __off__: A number (like 0, -10) or a string (like OFF) that is recognized as off state.
* __inverse__: Inverse the meaning. A received "ON" will switch the thing channel off and vice versa.

The thing by default always recognizes `"ON"`,`"1"`, `1` as on state and `"OFF"`, `"0"`, `0` as off state and if **on** and **off** are not configured it sends the integer values `1` for on and `0` for off.

You can connect this channel to a Contact or Switch item.