package com.nitesh.test;

import java.io.File;

public class WatchTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("ended");
		
		new FileWatcher(new File("/home/mmt5866/Desktop/watch/test.log")).start();

	}

}
