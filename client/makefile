CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lpthread

all: clean bin/StompWCIClient

EchoClient: bin/ConnectionHandler.o bin/echoClient.o
	g++ -o bin/EchoClient bin/ConnectionHandler.o bin/echoClient.o $(LDFLAGS)

bin/StompWCIClient: bin/StompClient.o bin/ConnectionHandler.o bin/event.o bin/frame.o bin/StompProtocol.o
	g++ -o bin/StompWCIClient bin/StompClient.o bin/ConnectionHandler.o bin/event.o bin/frame.o bin/StompProtocol.o $(LDFLAGS)

bin/StompClient.o: src/StompClient.cpp
	g++ $(CFLAGS) -o bin/StompClient.o src/StompClient.cpp	

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/event.o: src/event.cpp
	g++ $(CFLAGS) -o bin/event.o src/event.cpp

bin/frame.o: src/frame.cpp
	g++ $(CFLAGS) -o bin/frame.o src/frame.cpp

bin/StompProtocol.o: src/StompProtocol.cpp
	g++ $(CFLAGS) -o bin/StompProtocol.o src/StompProtocol.cpp		

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp

.PHONY: clean
clean:
	rm -f bin/*
	