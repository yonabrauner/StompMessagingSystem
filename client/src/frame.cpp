#include "../include/frame.h"

    string Frame::createLoginFrame(string host, string userName, string password){
        string output = "CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:" 
        + userName + "\npasscode:" + password + "\n";
        return output;
    }

    string Frame::createLogoutFrame(int recipt){
        string output =  "DISCONNECT\nreceipt:" + to_string(recipt);
        output += "\n";
        return output;
    }

    string Frame::createSendFrame(string userName, Event event){
        string game = event.get_team_a_name() + "_" + event.get_team_b_name();
        string output = "SEND\ndestination: " + game + "\n\n" + "user: " + userName + "\nteam a: " + event.get_team_a_name() + "\nteam b: " + event.get_team_b_name()
        + "\nevent name: " + event.get_name() + "\ntime: " + std::to_string(event.get_time()) + "\ngeneral game updates:\n";
        for (std::pair<string, string> update : event.get_game_updates()){
            output = output + "    " + update.first + ": " + update.second + "\n";
        }
        output = output + "team a updates:\n";
        for (std::pair<string, string> update : event.get_team_a_updates()){
            output = output + "    " + update.first + ": " + update.second + "\n";
        }
        output = output + "team b updates:\n";
        for (std::pair<string, string> update : event.get_team_b_updates()){
            output = output + "    " + update.first + ": " + update.second + "\n";
        }
        output = output + "description:\n" + event.get_discription() + "\n~";
        return output;
    }

    string Frame::createSubsribeFrame(string game, int subscriptionId){
        string output = "SUBSCRIBE\ndestination:"+ game + "\nid:" + to_string(subscriptionId) + "\n";
        return output;
    }
    
    string Frame::createUnsubsribeFrame(int subscriptionId, int recipt){
        string output = "UNSUBSCRIBE\nid:"+ to_string(subscriptionId);
        output += "\nrecipt:" + to_string(recipt);
        output += "\n"; 
        return output;
    }
