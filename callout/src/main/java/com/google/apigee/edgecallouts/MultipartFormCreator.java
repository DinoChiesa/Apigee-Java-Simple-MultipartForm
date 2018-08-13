// MultipartFormCreator.java
//
// Copyright (c) 2018 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.jclouds.io.payloads.MultipartForm;
import org.jclouds.io.payloads.Part;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class MultipartFormCreator extends CalloutBase implements Execution {
    private final static String varprefix= "mpf_";
    private final static boolean wantStringDefault = true;

    public MultipartFormCreator (Map properties) {
        super(properties);
    }

    public String getVarnamePrefix() { return varprefix; }

    private boolean getWantDecode(MessageContext msgCtxt) throws Exception {
        String wantDecode = getSimpleOptionalProperty("want-base64-decode", msgCtxt);
        if (wantDecode == null) { return false; }
        return Boolean.parseBoolean(wantDecode.toLowerCase());
    }
    
    private String getDestination(MessageContext msgCtxt) throws Exception {
        String destination = getSimpleOptionalProperty("destination", msgCtxt);
        if (destination == null) { destination = "message"; }
        return destination;
    }

    private String getPartContentVar(MessageContext msgCtxt) throws Exception {
        return getSimpleRequiredProperty("contentVar", msgCtxt);
    }

    private String getPartContentType(MessageContext msgCtxt) throws Exception {
        return getSimpleRequiredProperty("contentType", msgCtxt);
    }

    private String getPartName(MessageContext msgCtxt) throws Exception {
        return getSimpleRequiredProperty("part-name", msgCtxt);
    }

    public ExecutionResult execute (final MessageContext msgCtxt, final ExecutionContext execContext) {
        try {
            String contentVar = getPartContentVar(msgCtxt);
            boolean mustSetDestination = false;
            String content = (String) msgCtxt.getVariable(contentVar);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            boolean wantDecode = getWantDecode(msgCtxt);
            if (wantDecode) {
                InputStream decodingStream = new Base64InputStream(new ByteArrayInputStream(contentBytes), false); // decoding
                byte[] decodedBytes = IOUtils.toByteArray(decodingStream);
                msgCtxt.setVariable(varName("decoded_length"), decodedBytes.length);
                contentBytes = decodedBytes;
            }

            String boundary = "--------------------" + RandomStringUtils.randomAlphanumeric(14);
            String destination = getDestination(msgCtxt);
            Message message = (Message) msgCtxt.getVariable(destination);
            if (message == null) {
                mustSetDestination = true;
                message = msgCtxt.createMessage(msgCtxt
                                                .getClientConnection()
                                                .getMessageFactory()
                                                .createRequest(msgCtxt));
            }
            msgCtxt.setVariable(varName("boundary"), boundary);
            message.setHeader("content-type", "multipart/form-data;boundary=" + boundary);
            Part filepart = Part.create( getPartName(msgCtxt),
                                         new ByteArrayPayload((byte[])contentBytes),
                                         new Part.PartOptions().contentType( getPartContentType(msgCtxt) ));

            MultipartForm mpf = new MultipartForm(boundary, new Part[] {filepart});
            byte[] payload = IOUtils.toByteArray( mpf.openStream() );
            msgCtxt.setVariable(varName("payload_length"), payload.length);
            message.setContent(new ByteArrayInputStream(payload));
            if (mustSetDestination) {
                msgCtxt.setVariable(destination,message);
            }
            return ExecutionResult.SUCCESS;
        }
        catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            String error = e.toString();
            msgCtxt.setVariable(varName("exception"), error);
            int ch = error.lastIndexOf(':');
            if (ch >= 0) {
                msgCtxt.setVariable(varName("error"), error.substring(ch+2).trim());
            }
            else {
                msgCtxt.setVariable(varName("error"), error);
            }
            msgCtxt.setVariable(varName("stacktrace"), ExceptionUtils.getStackTrace(e));
            return ExecutionResult.ABORT;
        }
    }
}
