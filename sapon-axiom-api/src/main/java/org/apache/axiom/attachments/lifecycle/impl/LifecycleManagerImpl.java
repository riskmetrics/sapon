/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axiom.attachments.lifecycle.impl;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.axiom.attachments.lifecycle.LifecycleManager;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LifecycleManagerImpl implements LifecycleManager {
    private static final Log log = LogFactory.getLog(LifecycleManagerImpl.class);

    private static final ScheduledExecutorService scheduler
    	= Executors.newScheduledThreadPool(1);

    //Hashtable to store file accessors.
    private static Hashtable<String, FileAccessor> table = new Hashtable<String, FileAccessor>();
    private VMShutdownHook hook = null;
    public LifecycleManagerImpl() {
        super();
    }

    /* (non-Javadoc)
     * @see org.apache.axiom.lifecycle.LifecycleManager#create(java.lang.String)
     */
    public FileAccessor create(String attachmentDir) throws IOException {
        if(log.isDebugEnabled()){
            log.debug("Start Create()");
        }
        File file = null;
        File dir = null;
        if (attachmentDir != null) {
            dir = new File(attachmentDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Given Axis2 Attachment File Cache Location "
                + dir + "  should be a directory.");
        }

        //TODO: is createTempFile not good enough for some reason?
        // Generate unique id.  The UUID generator is used so that we can limit
        // synchronization with the java random number generator.
        String id = UUIDGenerator.getUUID();

        //Replace colons with underscores
        id = id.replaceAll(":", "_");

        String fileString = "Axis2" + id + ".att";
        file = new File(dir, fileString);
        FileAccessor fa = new FileAccessor(this, file);
        //add the fileAccesor to table
        table.put(fileString, fa);
        //Default behaviour
        deleteOnExit(file);
        if(log.isDebugEnabled()){
            log.debug("End Create()");
        }
        return fa;
    }

    public void delete(File file) throws IOException {
        if(log.isDebugEnabled()) {
            log.debug("Deleting " + file);
        }

        if(file != null && file.exists()) {
            table.remove(file);

            if(file.delete()) {
                if(log.isDebugEnabled()) {
                    log.debug("Successfully deleted " + file);
                }
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Cannot delete " + file + ", setting to delete on VM shutdown");
                }
                deleteOnExit(file);
            }
        }
    }

    public void deleteOnExit(File file) throws IOException {
        if(log.isDebugEnabled()){
            log.debug("Setting VM shutdown deletion hook for " + file);
        }
        if(hook == null){
            hook = RegisterVMShutdownHook();
        }

        if(file!=null) {
            hook.add(file);
            table.remove(file);
        }
        if(log.isDebugEnabled()){
            log.debug("VM shutdown deletion hook set for " + file);
        }
    }

    public void deleteOnTimeInterval(int interval, File file) throws IOException {
        if(log.isDebugEnabled()){
            log.debug("Scheduling deletion in " + interval + " seconds of " + file);
        }

        scheduler.schedule(new FileDeletor(file), interval, TimeUnit.SECONDS);
    }

    private VMShutdownHook RegisterVMShutdownHook() throws RuntimeException{
        if(log.isDebugEnabled()){
            log.debug("Start RegisterVMShutdownHook()");
        }
        try{
            hook = AccessController.doPrivileged(new PrivilegedExceptionAction<VMShutdownHook>() {
                public VMShutdownHook run() throws SecurityException, IllegalStateException, IllegalArgumentException {
                    VMShutdownHook hook = VMShutdownHook.hook();
                    if(!hook.isRegistered()){
                        Runtime.getRuntime().addShutdownHook(hook);
                        hook.setRegistered(true);
                    }
                    return hook;
                }
            });
        }catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
                log.debug("VM Shutdown Hook not registered.");
            }
            throw new RuntimeException(e);
        }
        if(log.isDebugEnabled()){
            log.debug("Exit RegisterVMShutdownHook()");
        }
        return hook;
    }

    private class FileDeletor implements Runnable {
        final File _file;

        public FileDeletor(final File file) {
            super();
            this._file = file;
        }

        public void run() {
        	if(log.isDebugEnabled()) {
        		log.debug("Attempting scheduled deletion of " + _file);
        	}
        	if(_file.exists()){
        		table.remove(_file);
        		_file.delete();
        	}
        	if(log.isDebugEnabled()) {
        		log.debug("Successful scheduled deletion of " + _file);
        	}
        }
    }

	public FileAccessor getFileAccessor(String fileName) throws IOException {
		return table.get(fileName);
	}

}


