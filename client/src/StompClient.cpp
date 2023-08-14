
#include "../include/StompClient.h"
#include "../include/ConnectionHandler.h"

bool loggedIn = false;
bool shouldTerminate = false;

void tryLogin();
void listenToKeyboard();
ConnectionHandler handler;    

int main (int argc, char *argv[]) {
    // loop that runs forever
    while (true) {
        tryLogin();
        shouldTerminate = false;
        thread t1(listenToKeyboard);
        while(loggedIn){
            string message = "";
            if (handler.getLine(message)) {
                handler.handleMessage(message);
            }
        }       // thread automatically closes itself
        handler.close();
        shouldTerminate = true;
        t1.join();
    }
    return 0;

}

void tryLogin() {
    while(!loggedIn){
        handler.close();
        cout<<"not logged in. enter login command" << endl;
        const short bufsize = 1024;
        char buf[bufsize];
        cin.getline(buf, bufsize);
        string input(buf);
        if (input.find("login") != string::npos){
            tuple<string,short> hostAndPort = StompProtocol::getHostAndPort(input);
            handler = ConnectionHandler(get<0>(hostAndPort),get<1>(hostAndPort));
            if (handler.connect()){
                handler.handleCommand(input);
                string message;
                if (handler.getLine(message)) {
                    handler.handleMessage(message);
                }
                else{
                // if(!loggedIn){
                    cout << "could not connect to server." << endl;
                    handler.close();
                }
            }
        }
    }
}

void listenToKeyboard (){
    string input = "";
    while (!shouldTerminate){                       // shutdown = closing the client, only if logged out!!!!
        if(input == "") {
            const short bufsize = 1024;
            char buf[bufsize];
            cin.getline(buf, bufsize);
            if (!loggedIn) break;
            string input(buf);                 //reading from keyboard
            if (input == "logout")
                shouldTerminate = true;
            handler.handleCommand(input);           // the send process
        }                                           //read line if we didn't save any input before
        else {                                      //we saved input - use it instead of a new one
            input = ""; 
        }       
    }
}