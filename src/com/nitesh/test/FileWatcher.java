package com.nitesh.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class FileWatcher extends Thread {
    private final File file;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private int lineRead;

    public FileWatcher(File file) {
    	System.out.println("ctor");
        this.file = file;
        this.lineRead = -1;
    }

    public boolean isStopped() { return stop.get(); }
    public void stopThread() { stop.set(true); }

    public void doOnChange() {
    	int curr = -1;
    	StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) 
        {
     
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) 
            {
            	if (curr >= lineRead) {
            		contentBuilder.append(sCurrentLine).append("\n");
            		lineRead++;
            	}
            	curr++;
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        
        System.out.println("changed:\n");
        System.out.println(contentBuilder.toString());
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
        	System.out.println("started");
            Path path = file.toPath().getParent();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            System.out.println("started");
            while (!isStopped()) {
                WatchKey key;
                try { key = watcher.poll(25, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { return; }
                if (key == null) { Thread.yield(); continue; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(file.getName())) {
                        doOnChange();
                    }
                    boolean valid = key.reset();
                    if (!valid) { System.out.println("ended");break; }
                }
                Thread.yield();
            }
        } catch (Throwable e) {
            // Log or rethrow the error
        }
    }
}