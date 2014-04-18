ImportanceSampling
==================
 1. Algorithm 1 is in file ImportanceSampling.java
 2. Algorithm 2 is in file wCutSet.java 

#1. How to compile the project?
    1. In Linux/Unix command terminal, enter to the directory of the project, then you can see a bunch of *.java source file
    2. Execute the command below to compile all the source files
  
javac *.java

#2. Run the project
   We specify the input file in command line
   For example, to compute on the input file `17_4_s.binary.uai`
 
java ImportanceSampling `17_4_s.binary.uai` 1 1000 adaptive

   where ImportanceSampling is the main class. `17_4_s.binary.uai` specify the input file. In this case, input is `17_4_s.binary.uai`, which located in the project directory. You can replace it with Unix-style file path. But ImportanceSampling is unchangable.
So the general format of command is:

java ImportanceSampling {InputFilePath} {w} {N} {isAdaptive}

#3. The output
   The name of the output is {InputFilePath} + ".output". In the case above, the output file is named `17_4_s.binary.uai`, which locates in the same directory of `17_4_s.binary.uai`

