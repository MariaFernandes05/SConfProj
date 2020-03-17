JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	MequieServer.java \
	Mequie.java \
	User.java \
	Grupo.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
