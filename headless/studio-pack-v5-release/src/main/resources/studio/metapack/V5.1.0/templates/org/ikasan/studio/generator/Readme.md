Getters or direct access

* Getter example -    ${getMeta().getName()}
* Property example -  ${meta.name}

The getter access is more verbose, and can fall foul of the subtly booleans ( isAdBoolean() as apposed to getABoolean() )

The property example does not conform to familiar Java syntax and can lead to confusion about private/public access (under the covers, property access is converted to getter).

The preference at the moment is to use the java getter style, this has saved noatble time when debugging (easier to find / correct the source of information, with exception to some of the lombok accessors)




