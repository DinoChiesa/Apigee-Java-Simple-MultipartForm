// Copyright 2016 Apigee Corp, 2017-2018 Google Inc.
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
// ------------------------------------------------------------------

package com.google.apigee.testng.tests;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.apigee.edgecallouts.MultipartFormCallout;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestMultipartFormCallout {
    private final static String testDataDir = "src/test/resources/test-data";

    MessageContext msgCtxt;
    InputStream messageContentStream;
    Message message;
    ExecutionContext exeCtxt;

    @BeforeMethod()
    public void beforeMethod() {

        msgCtxt = new MockUp<MessageContext>() {
            private Map variables;
            public void $init() {
                variables = new HashMap();
            }

            @Mock()
            public <T> T getVariable(final String name){
                if (variables == null) {
                    variables = new HashMap();
                }
                if (name.equals("message.content")) {
                    try {
                        return (T) IOUtils.toByteArray(messageContentStream);
                    }
                    catch(IOException ioexc1) {
                        return (T) null;
                    }
                }
                return (T) variables.get(name);
            }

            @Mock()
            public boolean setVariable(final String name, final Object value) {
                if (variables == null) {
                    variables = new HashMap();
                }
                if (name.equals("message.content")) {
                    if (value instanceof String){
                        messageContentStream = new ByteArrayInputStream( ((String)value).getBytes( StandardCharsets.UTF_8 ) );
                    }
                    else if (value instanceof InputStream) {
                        messageContentStream = (InputStream)value;
                    }
                }
                variables.put(name, value);
                return true;
            }

            @Mock()
            public boolean removeVariable(final String name) {
                if (variables == null) {
                    variables = new HashMap();
                }
                if (variables.containsKey(name)) {
                    variables.remove(name);
                }
                return true;
            }

            @Mock()
            public Message getMessage() {
                return message;
            }
        }.getMockInstance();

        exeCtxt = new MockUp<ExecutionContext>(){ }.getMockInstance();

        message = new MockUp<Message>(){
            @Mock()
            public InputStream getContentAsStream() {
                return messageContentStream;
            }
            @Mock()
            public void setContent(InputStream is) {
                messageContentStream = is;
            }
            @Mock()
            public void setContent(String content) {
                messageContentStream = new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) );
            }
            @Mock()
            public String getContent() {
                try {
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(messageContentStream, writer, StandardCharsets.UTF_8);
                    return writer.toString();
                }
                catch (Exception ex1) {
                    return null;
                }
            }
        }.getMockInstance();
    }


    @Test
    public void test_MessageCreation() throws Exception {
        String filename = "Logs_512px.png.b64";
        String outputVar = "output_payload";
        Path path = Paths.get(testDataDir, filename);
        if (!Files.exists(path)) {
            throw new IOException("file(" + path.toString() + ") not found");
        }
        InputStream imageInputStream = Files.newInputStream(path);
        byte[] imageBytes = IOUtils.toByteArray(imageInputStream);
        msgCtxt.setVariable("base64EncodedImageData", new String(imageBytes, StandardCharsets.UTF_8));

        Properties props = new Properties();
        props.put("contentVar", "base64EncodedImageData");
        props.put("contentType", "image/png");
        props.put("part-name", "image");
        props.put("outputVar", outputVar);

        MultipartFormCallout callout = new MultipartFormCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        ExecutionResult expectedResult = ExecutionResult.SUCCESS;

        // check result and output
        // Object output = msgCtxt.getVariable("request.content");
        // Assert.assertNotNull(output, "no output");
        Object error = msgCtxt.getVariable("mpf_error");
        Assert.assertNull(error, "error");
        Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
        Assert.assertNull(stacktrace, "stacktrace");
        Assert.assertEquals(actualResult, expectedResult, "result not as expected");
    }


    // @Test
    // public void testDataProviders() throws IOException {
    //     Assert.assertTrue(getDataForBatch1().length > 0);
    // }
    //
    // @Test(dataProvider = "batch1")
    // public void test2_Configs(TestCase tc) throws Exception {
    //     if (tc.getDescription()!= null)
    //         System.out.printf("  %10s - %s\n", tc.getTestName(), tc.getDescription() );
    //     else
    //         System.out.printf("  %10s\n", tc.getTestName() );
    //
    //     Path path = Paths.get(testDataDir, tc.getInput());
    //     if (!Files.exists(path)) {
    //         throw new IOException("file("+tc.getInput()+") not found");
    //     }
    //
    //     messageContentStream = Files.newInputStream(path);
    //
    //     Base64 callout = new Base64(tc.getProperties());
    //
    //     // execute and retrieve output
    //     ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    //
    //     String s = tc.getExpected().get("success");
    //     ExecutionResult expectedResult = (s!=null && s.toLowerCase().equals("true")) ?
    //                                        ExecutionResult.SUCCESS : ExecutionResult.ABORT;
    //     // check result and output
    //     if (expectedResult == actualResult) {
    //         if (expectedResult == ExecutionResult.SUCCESS) {
    //             String fname = tc.getExpected().get("output");
    //             path = Paths.get(testDataDir, fname);
    //             if (!Files.exists(path)) {
    //                 throw new IOException("expected output file("+fname+") not found");
    //             }
    //             byte[] expectedOutputBytes = IOUtils.toByteArray(Files.newInputStream(path));
    //             boolean stringOutput =
    //                 (((String)(msgCtxt.getVariable("b64_action"))).equals("encode")) &&
    //                 (boolean)(msgCtxt.getVariable("b64_wantString"));
    //
    //             //byte[] actualOutputBytes = IOUtils.toByteArray(msgCtxt.getMessage().getContentAsStream());
    //             Object messageContent = msgCtxt.getVariable("message.content");
    //             byte[] actualOutputBytes = null;
    //             if (messageContent instanceof String){
    //                 actualOutputBytes = ((String)messageContent).getBytes(StandardCharsets.UTF_8);
    //             }
    //             else if (messageContent instanceof byte[]){
    //                 actualOutputBytes = (byte[])messageContent;
    //             }
    //             else if (messageContent instanceof InputStream){
    //                 actualOutputBytes = IOUtils.toByteArray((InputStream)messageContent);
    //             }
    //
    //             if (!expectedOutputBytes.equals(actualOutputBytes)) {
    //
    //                 // String digest1 = DigestUtils.sha256Hex(actualOutputBytes);
    //                 // String digest2 = DigestUtils.sha256Hex(expectedOutputBytes);
    //                 // System.err.printf("    digest got: %s\n", digest1);
    //                 // System.err.printf("    expected  : %s\n", digest2);
    //
    //                 System.err.printf("    got: %s\n", Hex.encodeHexString( actualOutputBytes ) ) ;
    //                 System.err.printf("    expected  : %s\n", Hex.encodeHexString( expectedOutputBytes ) ) ;
    //
    //                 // the following will throw
    //                 Assert.assertEquals(actualOutputBytes, expectedOutputBytes, tc.getTestName() + ": result not as expected");
    //             }
    //         }
    //         else {
    //             String expectedError = tc.getExpected().get("error");
    //             Assert.assertNotNull(expectedError, tc.getTestName() + ": broken test: no expected error specified");
    //
    //             String actualError = msgCtxt.getVariable("b64_error");
    //             Assert.assertEquals(actualError, expectedError, tc.getTestName() + ": error not as expected");
    //         }
    //     }
    //     else {
    //         String observedError = msgCtxt.getVariable("b64_error");
    //         System.err.printf("    observed error: %s\n", observedError);
    //
    //         Assert.assertEquals(actualResult, expectedResult, tc.getTestName() + ": result not as expected");
    //     }
    //     System.out.println("=========================================================");
    // }

}
