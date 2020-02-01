# ACTonCompiler
ACTon is an actor-based language with a formal foundation, designed in an effort to bridge the gap between formal verification approaches and real applications.

## How to run? 
#### Step 1:
Generate acton.g4 grammer located in `src/antlr` using Antlr.
#### Step 2: 
Run the whole java project using Acton.java script located in `src/main`.
#### Step 3:
Copy files located in `prerequisites` directory to `output` directory.
#### Step 4: 
After running commands above, All Java bytecode will be generated in the `output` folder. Go to the `output` directory in terminal. Then run commands below:
```
java -jar jasmin.jar *.j
java <MainClass>
```
