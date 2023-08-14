#include "../include/StompProtocol.h"


    extern bool loggedIn;
    

    StompProtocol::StompProtocol(): _receiptId(1), _subId(1),userName(),_command(),
    _parsedCommand(),_response(),_expectedReceipt(),_mysubscriptions(),_gameToReports(),commandsMap(),responsesMap()
    {
        initEnumMap();
    }

    gameReport::gameReport(): generalGameUpdates(),teamAStats(),teamBStats(),eventInfoToDescription() {}
    // can implement without boolean msgSource (true = keyboard, false = server), it's a design choice.
     string StompProtocol::process(string &msg, bool msgSource){
        if (msgSource){
            parseCommand(msg);
            switch(commandsMap[_command]){
                case (login): {
                    return handleLogin();
                }
                case (logout): {
                    return handleLogout();
                }
                case (join): {
                    return handleJoin();
                }   
                case (EXIT): {
                    return handleExit();
                }
                case (report): {
                    return handleReport();
                }
                case (summary): {
                    return handleSummary();
                }
            }
        }
        else{
            parseResponse(msg);
            switch (responsesMap[_response["frameType"]]){
                case (CONNECTED): {
                    return handleConnected();
                }
                case (MESSAGE): {
                    return handleMessage();
                }
                case (RECEIPT): {
                    return handleReceipt();
                }
                case (ERROR): {
                    return handleError();
                }
            }
        }
        resetProccess();
        return "";
    }



    // ------------------ Commands (received from user) -------------------

    void StompProtocol::parseCommand(string &msg){
        size_t pos = 0;
        string temp;
        _parsedCommand.clear();
        while ((pos = msg.find(" ")) != string::npos){
            temp = msg.substr(0, pos);
            _parsedCommand.push_back(temp);
            msg.erase(0,pos+1);
        }
        _parsedCommand.push_back(msg);
        _command = _parsedCommand[0];
    }

    string StompProtocol::handleLogin(){
        if (loggedIn){
            cout<<"please log out first"<<endl;
            return "";
        }
        string port_host = _parsedCommand[1];
        size_t pos = port_host.find(":");
        string host = port_host.substr(0, pos);
        string userName = _parsedCommand[2];
        this->userName = userName;
        string password = _parsedCommand[3];
        string frame = Frame::createLoginFrame(host,userName,password);
        return frame;
    }

    string StompProtocol::handleLogout(){
        string frame = Frame::createLogoutFrame(_receiptId);
        _expectedReceipt[_receiptId] = true;
        _receiptId++;
        loggedIn = false;
        return frame;
    }

    string StompProtocol::handleJoin(){
        string frame = Frame::createSubsribeFrame(_parsedCommand[1],_subId);
        _mysubscriptions.insert({_parsedCommand[1],_subId});
        _subId++;
        _expectedReceipt[_receiptId] = false;
        _receiptId++;
        return frame;
    }

    string StompProtocol::handleExit(){
        int subID = _mysubscriptions.find(_parsedCommand[1])->second;
        string frame = Frame::createUnsubsribeFrame(subID,_receiptId);
        _expectedReceipt[_receiptId] = false;
        _receiptId++;
        return frame;
    }

    string StompProtocol::handleReport(){
        names_and_events report = parseEventsFile(_parsedCommand[1]);
        string sendFrame = "";
        for (Event e : report.events){
            sendFrame = sendFrame + Frame::createSendFrame(userName,e);
        }
        return sendFrame;
    }

    string StompProtocol::handleSummary(){
        gameReport toSum = _gameToReports[_parsedCommand[1]][_parsedCommand[2]];
        ofstream summaryFile(_parsedCommand[3]);
        summaryFile<<reportToString(toSum);
        summaryFile.close();
        return "";   
    };

    // ------------------ Responses (received from server) -------------------

    void StompProtocol::parseResponse(string &msg){
        vector<string> lines = splitByDelim(msg,'\n');
        _response.clear();
        _response["frameType"] = lines[0];
        int i = 1; 
        int numOfLines = lines.size();
        while (i < numOfLines){ //info about message loop
            vector<string> toInsert = splitByDelim(lines[i],':');
            if(toInsert.size() == 0) break; //case of \n
            _response[toInsert[0]] = toInsert[1];
            i++; 
        }
        i++; // getting to event
        _response["event"] = "";
        while(i < numOfLines){ //saving event loop and avoiding the null char by -1
            _response["event"] = _response["event"] + "\n" +  lines[i]; 
            i++;
        }
        //next part checks if user and destination "headers" exist, but user will never exist.
        // if (_response.find("user") != _response.end()){
        //     if (_response.find("destination") != _response.end())
        //         _gameToReports[_response["destination"]][_response["user"]]
        // }
            
    }

    string StompProtocol::handleMessage(){
        printMessage();
        saveEvent();
        return "";
    };

    string StompProtocol::handleConnected(){
        string response = "Successfuly Logged In";
        loggedIn = true;
        return response;
    }

    string StompProtocol::handleReceipt(){
        int receipt = std::stoi(_response["receipt-id"]);
        bool type = _expectedReceipt[receipt];
        _expectedReceipt.erase(receipt);
        if(type ==true){
            loggedIn = false;
            return "logged out successfuly";
        } 
        return "";
    }

    string StompProtocol::handleError(){
        string response = "Error!\nLogged Out of Server.\n" + _response["message"] + "\n press enter to re-login.";
        loggedIn = false;
        return response;
    }

    //prints out a message to client
    void StompProtocol::printMessage(){
        std::cout<< _response["event"]<<std::endl;

    };

    void StompProtocol::saveEvent(){
        string gameName = _response["destination"];
        // _gameToReports[gameName].   _response["user"];
        _response["event"] = _response["event"].substr(2);              // event is only frame received with two /n/n in a row
        map<string,gameReport> reporters =_gameToReports[gameName];
        vector<string> lines = splitByDelim(_response["event"],'\n');
        string user = lines[0].substr(lines[0].find(":")+2);
        gameReport gameToUpdate = reporters[user];
        int i = 1; int numOfLines = lines.size();
        string eventTime,eventName;
        while(i < numOfLines){
            vector<string> stat = splitByDelim(lines[i],':');
            if(lines[i].find("general game") != string::npos) break;
            if (stat[0] == "event name") eventName = stat[1];
            if (stat[0] == "time") eventTime = stat[1];
            i++;
        }
        i++;
        while(i < numOfLines){
            if(lines[i].find("team a updates") == 0) break;
            vector<string> stat = splitByDelim(lines[i],':');
            stat[0] = stat[0].substr(stat[0].find_first_not_of(' ')); // delete the spaces
            gameToUpdate.generalGameUpdates[stat[0]] = stat[1];
            i++;
        }
        i++;
        while(i < numOfLines){
            if(lines[i].find("team b updates") == 0) break;
            vector<string> stat = splitByDelim(lines[i],':');
            stat[0] = stat[0].substr(stat[0].find_first_not_of(' ')); // delete the spaces
            gameToUpdate.teamAStats[stat[0]] = stat[1];
            i++;
        }
        i++;
        while(i < numOfLines){
            if(lines[i].find("description") == 0) break;
            vector<string> stat = splitByDelim(lines[i],':');
            stat[0] = stat[0].substr(stat[0].find_first_not_of(' ')); // delete the spaces
            gameToUpdate.teamBStats[stat[0]] = stat[1];
            i++;
        }
        i++;
        string info = eventTime + ':' + eventName;
        string description = "";
        while (i < numOfLines){
            description = description + lines[i] + '\n';
            i++;
        }
        gameToUpdate.eventInfoToDescription[info] = description;  
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
        reporters[user] = gameToUpdate;
        _gameToReports[gameName] = reporters;
    };

    string StompProtocol::reportToString(gameReport report){
        vector<string> team_names = splitByDelim(_parsedCommand[1], '_');
        string output = team_names[0] + "VS" + team_names[1] + "\nGame Stats:\nGeneral stats:\n";
        for (pair<string, string> stat : report.generalGameUpdates){
            output = output + stat.first + ": " + stat.second + "\n";
        }
        output = output + "\n" + team_names[0] + " stats:\n";
        for (pair<string, string> stat : report.teamAStats){
            output = output + stat.first + ": " + stat.second + "\n";
        }
        output = output + "\n" + team_names[1] + " stats:\n";
        for (pair<string, string> stat : report.teamBStats){
            output = output + stat.first + ": " + stat.second + "\n";
        }
        output = output + "\n" + "Game event reports:\n";
        for (pair<string, string> stat : report.eventInfoToDescription){
            output = output + stat.first + "\n\n" + stat.second + "\n\n\n";
        }
        output = output + "\nend of summary";
        return output;
    }

    tuple<string,short> StompProtocol::getHostAndPort(string &command){
        vector<string> split = splitByDelim(command,' ');
        vector<string> hostAndPort = splitByDelim(split.at(1),':');
        short port = std::stoi(hostAndPort.at(1),nullptr,10);
        tuple<string,short> output = make_tuple(hostAndPort[0],port);
        return output;
    }

    //recives a string and a delimeter
    //splits text by delimeter into vector
    //returns vector
    vector<string> StompProtocol::splitByDelim(const string &s,char delim) {
        vector<std::string> words;
        string word = "";
        for (auto x : s){
            if (x == delim){
                words.push_back(word);	
                word = "";
            }
            else {
                word = word + x;
            }
        }
        if (word != ""){
            words.push_back(word);	
        }
        return words;
    }

    void StompProtocol::resetProccess(){
        _parsedCommand.clear();
        _response.clear();
    }

    void StompProtocol::initEnumMap(){
        commandsMap["login"] = login;
        commandsMap["logout"] = logout;
        commandsMap["join"] = join;
        commandsMap["exit"] = EXIT;
        commandsMap["report"] = report;
        commandsMap["summary"] = summary;
        responsesMap["CONNECTED"] = CONNECTED;
        responsesMap["MESSAGE"] = MESSAGE;
        responsesMap["RECEIPT"] = RECEIPT;
        responsesMap["ERROR"] = ERROR;



    }

