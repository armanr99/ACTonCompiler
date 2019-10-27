export CLASSPATH=".:/usr/local/lib/antlr-4.7.2-complete.jar:$CLASSPATH" # your ANTLR jar file path
rm *.class
rm *.tokens
rm *.interp
rm ACTon*.java 
java -jar /usr/local/lib/antlr-4.7.2-complete.jar ACTon.g4 
javac *.java
java org.antlr.v4.gui.TestRig ACTon acton -gui < test1.act
