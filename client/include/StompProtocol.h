#pragma once
#include "../include/frame.h"
#include "../include/event.h"

#include <map>
#include <fstream>
#include <tuple>

using namespace std;

struct gameReport{
    gameReport ();
    map<string,string> generalGameUpdates;
    map<string,string> teamAStats;
    map<string,string> teamBStats;
    map<string,string> eventInfoToDescription;
};

enum commands {login, logout, join, EXIT, report, summary};

enum responses {CONNECTED, MESSAGE, RECEIPT, ERROR}; 

class StompProtocol{

private:

    int _receiptId;
    int _subId;
    string userName;
    string _command;
    vector<string> _parsedCommand;
    map<string,string> _response;
    // vector<int> _expectedReceipt;
    map<int,bool> _expectedReceipt;
    map<string,int> _mysubscriptions; 
    map<string,map<string,gameReport>> _gameToReports;
    map<string,commands> commandsMap;
    map<string,responses> responsesMap;



public:

    StompProtocol();
    string process(string &msg, bool msgSource);
    string handleLogin();
    string handleLogout();
    string handleJoin();
    string handleExit();
    string handleReport();
    string handleSummary();
    string handleConnected();
    string handleMessage();
    string handleReceipt();
    string handleError();
    void parseCommand(string &msg);
    void parseResponse(string &msg);
    void resetProccess();
    string reportToString(gameReport report);
    void printMessage();
    void saveEvent();
    static vector<string> splitByDelim(const string &s, char celim);
    static tuple<string,short> getHostAndPort(string &command);
    void initEnumMap();
    
};

#include "../include/ConnectionHandler.h"

