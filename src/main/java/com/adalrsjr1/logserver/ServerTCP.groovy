package com.adalrsjr1.logserver

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors;

import groovy.util.logging.Slf4j;

@Slf4j
class ServerTCP {
	static ExecutorService tPool = Executors.newCachedThreadPool()
	
	static void main(String[] args) {
		
		ServerSocket welcomeSocket = new ServerSocket(9999);

		while(true)
		{
			Socket connectionSocket = welcomeSocket.accept();
			tPool.execute(new MyWorker(connectionSocket))
		}
	}
}

@Slf4j
class MyWorker implements Runnable {
	Socket socket
	MyWorker(Socket socket) {
		this.socket = socket
	}
	
	void run() {
		String clientSentence;
		String capitalizedSentence;
		BufferedReader inFromClient =
		new BufferedReader(new InputStreamReader(socket.getInputStream()));
		DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
		clientSentence = inFromClient.readLine();
		log.info("Received: " + clientSentence);
		capitalizedSentence = clientSentence.toUpperCase() + '\n';
		outToClient.writeBytes(capitalizedSentence);
	}
}
