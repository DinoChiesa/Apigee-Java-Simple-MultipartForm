# Apigee Edge Multipart Form Creator

This directory contains the Java source code and pom.xml file required to build a Java callout that
creates a multipart form payload, from a single blob, or parses an inbound multipart form parload.

For creating the multipart form, it relies on code lifted from [Apache jclouds](https://github.com/jclouds/jclouds).
For parsing, it relies on code lifted from [javadelight](https://github.com/javadelight/delight-fileupload). I didn't use the entire libraries for either of these things, because they drag in too many un-desired dependencies.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Using this policy

You do not need to build the source code in order to use the policy in Apigee Edge.
All you need is the built JAR, and the appropriate configuration for the policy.
If you want to build it, feel free.  The instructions are at the bottom of this readme.


1. copy the jar file, available in  target/edge-custom-multipart-form-1.0.2.jar , if you have built the jar, or in [the repo](bundle/apiproxy/resources/java/edge-custom-multipart-form-1.0.2.jar) if you have not, to your apiproxy/resources/java directory. You can do this offline, or using the graphical Proxy Editor in the Apigee Edge Admin Portal.

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:

   ```xml
    <JavaCallout name='Java-Multipart-Form-1'>
        ...
      <ClassName>com.google.apigee.edgecallouts.MultipartFormCreator</ClassName>
      <ResourceURL>java://edge-custom-multipart-form-1.0.2.jar</ResourceURL>
    </JavaCallout>
   ```

3. use the Edge UI, or a command-line tool like
   [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js) or
   [pushapi](https://github.com/carloseberhardt/apiploy) or
   [apigeetool](https://github.com/apigee/apigeetool-node)
   or similar to
   import your proxy into an Edge organization, and then deploy the proxy .
   Eg, `./importAndDeploy.js -n -v -o ${ORG} -e ${ENV} -d bundle/`

4. Use a client to generate and send http requests to the proxy you just deployed . Eg,
   ```
   curl -i https://$ORG-$ENV.apigee.net/myproxy/foo
   ```


## Notes on Usage

This repo includes two callout classes,

* com.google.apigee.edgecallouts.MultipartFormCreator - create a form payload

* com.google.apigee.edgecallouts.MultipartFormParser - parse a form payload

## MultipartFormCreator

This callout will create a form, using a specific string as input. Optionally, the callout will base64-decode the string before placing it into the form. For base64-decoding, it uses the [Base64InputStream](https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/binary/Base64InputStream.html) from Apache commons-codec.

It accepts several data items as input

| property name   | status   | description                                                                |
| ----------------| -------- | -------------------------------------------------------------------------- |
| **contentVar**  | required | name of a variable containing a string which represents a base64-encoded byte array |
| **contentType** | required | a string, something like image/jpeg or image/png etc                       |
| **part-name**   | required | a string, the name of the part within the form                             |
| **want-base64-decode**   | optional | true or false. If not present, assumed false.                     |
| **destination** | optional | a string, the name of a  message. If it does not exist, it will be created. Defaults to 'message'.          |


An example for creating a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="contentVar">base64EncodedImageData</Property>
    <Property name="contentType">image/png</Property>
    <Property name="want-base64-decode">true</Property>
    <Property name="part-name">image</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.MultipartFormCreator</ClassName>
  <ResourceURL>java://edge-custom-multipart-form-1.0.2.jar</ResourceURL>
</JavaCallout>
```

The variable named in contentVar must hold a string in base64-encoded format.
How you get the string there, is up to you.

The result will be a form, that looks like so:

```
--9WTvUeO4O5
Content-Disposition: form-data; name="image"
Content-Type: image/png

PNG

IHDREXtSoftwareAdobe ImageReadyqe<:GIDATxZr@F8Kuzko-'[$\@...more image data here...
```


## MultipartFormParser

This callout will parse a form, using the content of the specified message as input.

It accepts a single parameter as input

| property name   | status   | description                                                                |
| ----------------| -------- | -------------------------------------------------------------------------- |
| **source**      | optional | name of a variable containing a message, containing a form. defaults to "message". |

An example for parsing a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="source">message</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.MultipartFormParser</ClassName>
  <ResourceURL>java://edge-custom-multipart-form-1.0.2.jar</ResourceURL>
</JavaCallout>
```

The callout sets variables in the context containing information about the parts of the inbound form.


| property name     | description                                                                |
| ------------------| -------------------------------------------------------------------------- |
| **items**         | String, a comma-separated list of file items from the form.  |
| **itemcount**     | String, a number indicating the number of  file items found in the form.  |
| **item_filename_X** | name of item X.  |
| **item_content_X**  | content for item X.  This is a byte array. May need to decode it.  |
| **item_content-type_X**  | String, the content-type for item X. |
| **item_size_X**  | String, the size in bytes of the content for item X. |

Subsequent policies can then read these variables and operate on them.


## Example API Proxy

You can find an example proxy bundle that uses the policy, [here in this repo](bundle/apiproxy).
The example proxy accepts a post.

You must deploy the proxy in order to invoke it.

Invoke it like this:

* Create a form, using "message":
  ```
    curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form-creator/t1
  ```

  Internally, the example proxy assigns a static, fixed string value to
  a variable, and uses THAT as the contentVar for the policy.  It then
  invokes the policy, which creates the form payload.  The proxy then
  sends the form to a backend system.

  NB: The backend system as of this moment does not correctly handle the
  form.  This is because the backend doesn't handle forms; it's not
  because the form is invalid.

* Create a form, using a new message
  ```
    curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form-creator/t2
  ```

* Parse a form

  ```
    curl -i -F person=anonymous -F readme=@../README.md https://$ORG-$ENV.apigee.net/multipart-form-creator/t3

  ```



## Building

Building from source requires Java 1.8, and Maven.

1. unpack (if you can read this, you've already done that).

2. Before building _the first time_, configure the build on your machine by loading the Apigee jars into your local cache:
  ```
  ./buildsetup.sh
  ```

3. Build with maven.
  ```
  mvn clean package
  ```
  This will build the jar and also run all the tests, and copy the jar to the resource directory in the sample apiproxy bundle.


## License

This material is Copyright 2018 Google LLC.
and is licensed under the [Apache 2.0 License](LICENSE). This includes the Java code as well as the API Proxy configuration.

## Bugs

* The automated tests are pretty thin.
