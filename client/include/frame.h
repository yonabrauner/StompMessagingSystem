#pragma once
#include "../include/event.h"
#include <string>
#include <vector>

using namespace std;

class Frame
{

public:
    static string createLoginFrame(string host, string userName, string password);
    static string createLogoutFrame(int receipt);
    static string createSendFrame(string userName, Event event);
    static string createSubsribeFrame(string game,int subsciptionId);
    static string createUnsubsribeFrame(int subscriptionId, int receipt);
};