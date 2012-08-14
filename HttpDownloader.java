/*
  Source: HttpDownloader.java
  Author: Jay Elrod
  Description: This program takes two arguments,
      one string defining a target download URL
      and one int denoting the number of separate
      HTTP download requests to send to the host.
      A new thread is spawned for each request,
      then each thread downloads a piece of the
      target, stores it in separate 'PART_X' files
      and the file is reconstructed upon download
      completion.
*/

import java.net.*;
import java.io.*;

public class HttpDownloader {

    //inner class defines parallel download threads
    static class DownloadThread extends Thread {

	Socket sock = null;
	PrintWriter writer = null;
	DataInputStream dataIn = null;
	DataOutputStream dataOut = null;
	String targetHost = "";
	String target = "";
	byte[] rangeData = null;
	int numthreads = 0;
	int threadNo = 0;
	int start = 0;
	int end = 0;
	
	//DownloadThread constructor
	DownloadThread(String targetHost, String target, int numthreads, int threadNo, int start, int end) {
	    super();
	    this.targetHost = targetHost;
	    this.target = target;
	    this.rangeData = new byte[(end-start)+1];
	    this.numthreads = numthreads;
	    this.threadNo = threadNo;
	    this.start = start;
	    this.end = end;
	    try {
		//establish threaded socket connection and instantiate I/O streams
		this.sock = new Socket(targetHost, 80);
		this.writer = new PrintWriter(sock.getOutputStream(), true);
		this.dataIn = new DataInputStream(sock.getInputStream());
		this.dataOut = new DataOutputStream(new FileOutputStream(new File("PART_" + threadNo)));
	    }
	    catch (Exception e) {
		System.out.println("Unhandled exception encountered during threaded socket establishment.");
		e.printStackTrace();
		System.exit(-1);
	    }
	}
	
	//thread activity
	public void run() {
	    try {
		writer.println("GET " + target + " HTTP/1.1");      //query HTTP server
		writer.println("Range: bytes=" + start + "-" + end);
		writer.println("Host: " + targetHost);
		writer.println("");
		while ( dataIn.readLine().length() != 0 ) {}        //skip HTTP reasponse headers
		dataIn.readFully(rangeData);                        //read response data
		dataOut.write(rangeData, 0, rangeData.length);      //write data to PART file
	    }	    
	    catch (Exception e) {
		System.out.println("Unhandled exception encountered during threaded HTTP exchange.");
		e.printStackTrace();
		System.exit(-1);
	    }
	}
    }
    //end inner class

    //master thread main function
    public static void main(String[] args) {

	DownloadThread[] threads = null;
	Socket sock = null;
	PrintWriter writer = null;
	DataInputStream dataIn = null;
	DataOutputStream dataOut = null;
	BufferedReader reader = null;
	String buffer = "";
	String targetHost = "";
	String target = "";
	int bytesRead = 0;
	int numthreads = 0;
	int filesize = 0;
	byte[] bytes = null;

	//usage error checking
	if (args.length != 2) {
	    System.out.println("Usage: java HttpDownloader [target URL] [number of threads]");
	    System.exit(-1);
	}
	
	//parse target URL string
	targetHost = args[0].substring(7, args[0].substring(7).indexOf('/')+7);
	target = args[0].substring(args[0].substring(7).indexOf('/')+7);
	numthreads = Integer.parseInt(args[1]);

	//establish master socket connection, retrieve filesize, and print HTTP response headers for user
	try {
	    sock = new Socket(targetHost, 80);
	    writer = new PrintWriter(sock.getOutputStream(), true);
	    reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	    writer.println("HEAD " + target + " HTTP/1.1");
	    writer.println("Host: " + targetHost);
	    writer.println("");
	    while ( (buffer = reader.readLine()) != null ) {
		System.out.println(buffer);
		if (buffer.indexOf("Content-Length") != -1 ) {
		    filesize = Integer.parseInt(buffer.substring(buffer.lastIndexOf(": ")+2));
		}
	    }
	    System.out.println();
	}
	catch (UnknownHostException e) {
	    System.out.println("Don't know about host "+ targetHost);
	    System.exit(-1);
	}
	catch (Exception e){
	    e.printStackTrace();
	    System.exit(-1);
	}

	//initialize DownloadThread and byte arrays
	threads = new DownloadThread[numthreads];
	bytes = new byte[filesize];

	//instantiate and start() threads
	for (int i=0; i<numthreads; i++) {
	    threads[i] = new DownloadThread(targetHost, target, numthreads, i, ((filesize*i)/numthreads), (((filesize*(i+1))/numthreads)-1));
	    threads[i].start();
	}

	System.out.println("Downloading...\n");

	//wait for threads to complete
	for (int i=0; i<numthreads; i++) {
	    while (threads[i].isAlive()) {}
	}

	//write thread data to output file and report
	try {
	    dataOut = new DataOutputStream(new FileOutputStream(new File(target.substring(target.lastIndexOf('/')+1))));
	    for (int i=0; i<numthreads; i++) {
		dataIn = new DataInputStream(new FileInputStream(new File("PART_" + i)));
		while ( (bytesRead = dataIn.read(bytes, 0, filesize)) != -1 )
		    dataOut.write(bytes, 0, bytesRead);
	    }
	    System.out.println("File download and recombination successful!\n");
	}
	catch (Exception e) {
	    System.out.println("Unhandled exception encountered during file recombination.");
	    e.printStackTrace();
	    System.exit(-1);
	}

    }
}