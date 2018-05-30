# Apigee Edge Multipart Form Creator

This directory contains the Java source code and pom.xml file required to build a Java callout that
creates a multipart form payload, from a single blob that is a base64-encoded string.

For base64-decoding, it uses the [Base64InputStream](https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/binary/Base64InputStream.html) from Apache commons-codec.

For creating the multipart form, it relies on code lifted from [Apache jclouds](https://github.com/jclouds/jclouds).


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Using this policy

You do not need to build the source code in order to use the policy in Apigee Edge.
All you need is the built JAR, and the appropriate configuration for the policy.
If you want to build it, feel free.  The instructions are at the bottom of this readme.


1. copy the jar file, available in  target/edge-custom-multipart-form-1.0.1.jar , if you have built the jar, or in [the repo](bundle/apiproxy/resources/java/edge-custom-multipart-form-1.0.1.jar) if you have not, to your apiproxy/resources/java directory. You can do this offline, or using the graphical Proxy Editor in the Apigee Edge Admin Portal.

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:

   ```xml
    <JavaCallout name='Java-Multipart-Form-1'>
        ...
      <ClassName>com.google.apigee.edgecallouts.MultipartFormCallout</ClassName>
      <ResourceURL>java://edge-custom-multipart-form-1.0.1.jar</ResourceURL>
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

There is one callout class, com.google.apigee.edgecallouts.MultipartFormCallout

It accepts several data items as input

| property name   | status   | description                                                                |
| ----------------| -------- | -------------------------------------------------------------------------- |
| **contentVar**  | required | name of a variable containing a string which represents a base64-encoded byte array |
| **contentType** | required | a string, something like image/jpeg or image/png etc                       |
| **part-name**   | required | a string, the name of the part within the form                             |
| **destination** | optional | a string, the name of an existing message. Defaults to 'request'.          |

This callout operates on the message content.
If you place it in the request flow, it will operate on the request content.
If you attach the policy to the response flow, it will operate on the response content.
You probably want it in the request flow!


## Configuring the Callout

An example for encoding:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="contentVar">base64EncodedImageData</Property>
    <Property name="contentType">image/png</Property>
    <Property name="part-name">image</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.MultipartFormCallout</ClassName>
  <ResourceURL>java://edge-custom-multipart-form-1.0.1.jar</ResourceURL>
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


## Example API Proxy

You can find an example proxy bundle that uses the policy, [here in this repo](bundle/apiproxy).
The example proxy accepts a post.

You must deploy the proxy in order to invoke it.

Invoke it like this:

```
  curl -i -X POST -d '' https://${ORG}-${ENV}.apigee.net/multipart-form-creator/t1
```

Internally, the example proxy assigns a static, fixed string value to a variable, and uses THAT as the contentVar for the policy.
It then invokes the policy, which creates the form payload.
The proxy then sends the form to a backend system.

NB: The backend system as of this moment does not correctly handle the form.  This is because the backend doesn't handle forms; it's not because the form is invalid.


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


## Runtime Dependencies

All of these are runtime dependencies. These JARs must be available in the proxy or organization into which you deploy.

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0
- Apache commons IO 2.3
- Apache commons Codec 1.11
- Apache commons lang3 3.7
- Google Guava 24.1

## License

This material is Copyright 2018 Google LLC.
and is licensed under the [Apache 2.0 License](LICENSE). This includes the Java code as well as the API Proxy configuration.

## Bugs

* The tests are incomplete.
