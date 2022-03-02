# JLox Interpreter Implementation
## Following A book by Robert Nystrom [Crafting Interpreters](https://craftinginterpreters.com/)

## Language Features

- Statements
    - Simple statements  
        ```
        print "Hello, World!";
        ```
    - Block statements
    ```
    {
       print "Some statement";
       print "Some Second statement"; 
    }
    ```
- Variables
    ```
    var variable = "Some String";
    var pi = 3.14;
    var someValue = 25;
    
    print pi; // 3.14
    ```
- Control flow
    ```
        // if statement
        if (some_condition) {
            // Do stuff...
        } else {
            // Do alternate Stuff...
        }
        
        // while statement
        var i = 0;
        while (i < 20) {
            print i * i;
            i = i + 1;
        }
        
        // for loop statement
        for (var i = 0; i < 20; i = i + 1) {
            print i * i;
        }
        
        ...
    ```
- Functions
    ```
    fun someFunction(parameters) {
        // Do stuff...
    }
    someFunction();
    
    /**
    *   Some two numbers
    */
    fun sum(a, b) {
        return a + b;
    }
    
    print sum(2, 3); // 5
    ```
- Closures
    ```
    /**
    * Lets have our selft some closures
    */
    fun makeCounter() {
        var count = 0;
        
        fun counter() {
            print count;
            count = count + 1;
        }
        
        return counter;
    }
    
    var counter = makeCounter();
    counter(); // 0
    counter(); // 1
    ```
- Classes
    ```
    class SayHello {
        init(name) {
            this.name = name;
        }
        hello() {
            print "Hello, " + this.name + ".";
        }
    }
    
    var hello = SayHello("Patco");
    hello.hello() // "Patco"
    
    ```
- Inheritance
    ```
    class Person {
        init(firstName, lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        
        info() {
            return this.firstName + " " + this.lastName;
        }
    }
    
    class Student < Person { // Ugh... This is the worst example yet...
        init(firstName, lastName, studentId) {
            super.init(firstName, lastName);
            this.studentId = studentId;
        }
        
        info() {
            return super.info() + ", " + this.studentId;
        }
    }
    
    var student = Student("Patco", "Ke...", 20220302);
    print student.info();
    ```

## Installation

```
    # I suppose clone and compile with mvn
    λ mvn package --quiet
    λ java -jar target/jlox-0.01.jar [source]
```

...
