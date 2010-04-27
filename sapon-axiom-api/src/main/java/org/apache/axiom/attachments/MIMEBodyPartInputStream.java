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

package org.apache.axiom.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * An InputStream for a single MIME Body Part.
 */
public class MIMEBodyPartInputStream extends InputStream 
{
    BoundaryPushbackInputStream bpis;
    PushbackInputStream inStream;
    boolean done = false;

    /**
     * @param inStream
     * @param boundary
     */
    public MIMEBodyPartInputStream(	PushbackInputStream inStream, 
    		                       	byte[] boundary ) 
    {
        this (inStream, boundary, boundary.length + 2);
    }

    /**
     * @param inStream
     * @param boundary
     * @param parent
     * @param pushbacksize <= size of pushback buffer on inStream
     */
    public MIMEBodyPartInputStream(	PushbackInputStream inStream,
            						byte[] boundary, 
            						int pushbacksize ) 
    {
        bpis = new BoundaryPushbackInputStream(inStream, boundary, pushbacksize);
        this.inStream = inStream;
    }

    @Override
    public int read() throws IOException {
        if (done) {
            return -1;
        }
        int rc = bpis.read();
        if (getBoundaryStatus()) {
            finish();
        }
        return rc;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (done) {
            return -1;
        } 
        int rc = bpis.read(b, off, len);
        if (getBoundaryStatus()) {
            finish();
        }
        return rc;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (done) {
            return -1;
        } 
        int rc = bpis.read(b);
        if (getBoundaryStatus()) {
            finish();
        }
        return rc;
    }

    /**
     * Called when done reading to detect and consumes trailing -- 
     * @throws IOException
     */
    private void finish() throws IOException {
        if (!done) {
            int one = inStream.read();
            
            // Accept --
            if (one != -1) {
                int two = inStream.read();
                if (two != -1) {
                    if (one == 45 && two == 45) {
                        // Accept --
                    } else {
                        inStream.unread(two);
                        inStream.unread(one);
                    }
                } else {
                    inStream.unread(one);
                }
            }
            
            one = inStream.read();
            
            // Accept /r/n
            if (one != -1) {
                int two = inStream.read();
                if (two != -1) {
                    if (one == 13 && two == 10) {
                        // Accept /r/n and continue
                    } else {
                        inStream.unread(two);
                        inStream.unread(one);
                    }
                } else {
                    inStream.unread(one);
                }
            }
        }
        done = true;
    }
    
    public boolean getBoundaryStatus()
    {
        return bpis.getBoundaryStatus();
    }
}