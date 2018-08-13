// MultipartFormParser.java
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
import com.google.apigee.AdapterHttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jclouds.io.payloads.ByteArrayPayload;
import org.jclouds.io.payloads.MultipartForm;
import org.jclouds.io.payloads.Part;
    
public class MultipartFormParser extends CalloutBase implements Execution {
    private final static String varprefix= "mpf_";
    private final static boolean wantStringDefault = true;

    public MultipartFormParser (Map properties) {
        super(properties);
    }

    public String getVarnamePrefix() { return varprefix; }

    private String getSource(MessageContext msgCtxt) throws Exception {
        String source = getSimpleOptionalProperty("source", msgCtxt);
        if (source == null) { source = "message"; }
        return source;
    }

    private static List<FileItem> parseForm(final byte[] data, final String contentType)
        throws Exception {
        final FileItemFactory factory = new DiskFileItemFactory();
        final ServletFileUpload upload = new ServletFileUpload(factory);
        final HttpServletRequest request = new AdapterHttpServletRequest(data, contentType);
        final boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if ((!isMultipart)) {
            throw new IllegalStateException("Illegal request for uploading files. Multipart request expected.");
        }
        final List<FileItem> iter = upload.parseRequest(request);
        return iter;
    }
    
    public ExecutionResult execute (final MessageContext msgCtxt, final ExecutionContext execContext) {
        try {
            String source = getSource(msgCtxt);
            Message message = (Message) msgCtxt.getVariable(source);
            if (message == null) {
                throw new IllegalStateException("source message is null.");
            }
            InputStream input = message.getContentAsStream();
            byte[] inputBytes = IOUtils.toByteArray(input);
            List<FileItem> items = parseForm(inputBytes, message.getHeader("content-type"));
            List<String> names = new ArrayList<String>();
            int n = 0;
            for (FileItem item : items) {
                if (item.isFormField()) {
                    //... ignore any fields in the form
                } else {
                    String fileName = item.getName().replaceAll("[^a-zA-Z0-9_\\. ]", "");
                    names.add(fileName);
                    byte[] itemBytes = IOUtils.toByteArray(item.getInputStream());
                    msgCtxt.setVariable(varName("item_filename_" + n), fileName);
                    msgCtxt.setVariable(varName("item_content_" + n), itemBytes);
                    msgCtxt.setVariable(varName("item_content-type_" + n), item.getContentType());
                    msgCtxt.setVariable(varName("item_size_" + n), item.getSize()+"");
                    n++;
                }
            }
            msgCtxt.setVariable(varName("items"), String.join(", ", names));
            msgCtxt.setVariable(varName("itemcount"), names.size()+"");
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
