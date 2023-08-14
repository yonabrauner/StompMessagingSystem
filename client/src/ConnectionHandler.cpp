#include "../include/ConnectionHandler.h"

ConnectionHandler::ConnectionHandler(string host, short port):host_("127.0.0.1"), port_(7777),
 protocol_(), io_service_(), socket_(io_service_) {}

ConnectionHandler::ConnectionHandler():host_(), port_(),protocol_(),io_service_(), socket_(io_service_){}

ConnectionHandler& ConnectionHandler::operator=(const ConnectionHandler& h){
	this->host_ = h.host_;
	this->port_ = h.port_;
	this->protocol_ = h.protocol_;
	return *this;
}

ConnectionHandler::~ConnectionHandler() {
	close();
}

bool ConnectionHandler::connect() {
	cout << "Starting connect to "
	          << host_ << ":" << port_ << endl;
	try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
	}
	catch (exception &e) {
		cerr << "Connection failed (Error: " << e.what() << ')' << endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
	size_t tmp = 0;
	boost::system::error_code error;
	try {
		while (!error && bytesToRead > tmp) {
			tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
		}
		if (error)
			throw boost::system::system_error(error);
	} catch (exception &e) {
		cerr << "recv failed (Error: " << e.what() << ')' << endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
	int tmp = 0;
	boost::system::error_code error;
	try {
		while (!error && bytesToWrite > tmp) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
		}
		if (error)
			throw boost::system::system_error(error);
	} catch (exception &e) {
		cerr << "recv failed (Error: " << e.what() << ')' << endl;
		return false;
	}
	return true;
}

bool ConnectionHandler::getLine(string &line) {
	return getFrameAscii(line, '\0');
}

bool ConnectionHandler::sendLine(string &line) {
	return sendFrameAscii(line, '\0');
}


bool ConnectionHandler::getFrameAscii(string &frame, char delimiter) {
	char ch;
	// Stop when we encounter the null character.
	// Notice that the null character is not appended to the frame string.
	try {
		do {
			if (!getBytes(&ch, 1)) {
				return false;
			}
			if (ch != '\0')
				frame.append(1, ch);
		} while (delimiter != ch);
	} catch (exception &e) {
		cerr << "recv failed2 (Error: " << e.what() << ')' << endl;
		return false;
	}
	if (frame == "")
		return false;
	return true;
}

bool ConnectionHandler::sendFrameAscii(const string &frame, char delimiter) {
	bool result = sendBytes(frame.c_str(), frame.length());
	if (!result) return false;
	return sendBytes(&delimiter, 1);
}

void ConnectionHandler::handleCommand(string &command){
	string wholeFrame = protocol_.process(command,true);
	if (wholeFrame == "") return;
	vector<string> frames = StompProtocol::splitByDelim(wholeFrame,'~');
	for (string frame : frames){
		sendFrameAscii(frame, '\0');
	}
};

void ConnectionHandler::handleMessage(string &frame){
	string result = protocol_.process(frame,false);
	// if (result.find("Error!") != string::npos)
	// 	close();
	cout << result << endl;
	// if(result == "logged out successfuly") delete this;
};

// Close down the connection properly.
void ConnectionHandler::close() {
	try {
		socket_.close();
	} catch (...) {
		cout << "closing failed: connection already closed" << endl;
	}
}

