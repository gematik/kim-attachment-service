# KIM-KAS

## Docker

### Build with Spring-Boot

#### Commands

To run the application locally (HTTP only) by its own, run:

    $ mvn spring-boot:run

To build the Kas-Dockerimage, run:

    $ mvn spring-boot:build-image

To build the image with a proxy, set your proxy in pom.xml under plugins → `spring-boot-maven-plugin`.

#### Need

-   Docker installed

-   Maven 3.5+ or higher

#### How to use parameters

Parameters can be set by using the `-e <PARAMETER_NAME>=<VALUE>`

Example:

    $ docker run --name kas -p 81:8080 -d gematik.kim.kas.maxMailSize=30776 -e kim-kas

## Parameters

**List of parameters:.**

All Parameters start with `gematik.kim.kas.`

<table>
<tbody>
<tr class="odd">
<td><p>PARAMATER_NAME</p></td>
<td><p>Description</p></td>
<td><p>Default</p></td>
</tr>
<tr class="even">
<td><p>maxMailSize</p></td>
<td><p>Maximal allowed size of an e-mail.
If size is greater than 4G the nginx parameter <code>client_max_body_size</code> must be increased as well.</p></td>
<td><p>524288000</p></td>
</tr>
<tr class="odd">
<td><p>storagePath</p></td>
<td><p>Path to volume where attachments got stored</p></td>
<td><p>./target/storage</p></td>
</tr>
<tr class="even">
<td><p>logDir</p></td>
<td><p>Path to logfile</p></td>
<td><p>./target/logs</p></td>
</tr>
<tr class="odd">
<td><p>data_base_location</p></td>
<td><p>Path to database storage</p></td>
<td><p>./target/db/demo</p></td>
</tr>
<tr class="even">
<td><p>http-port</p></td>
<td><p>Port used by the application</p></td>
<td><p>8080</p></td>
</tr>
<tr class="odd">
<td><p>swagger-ui-base-addr</p></td>
<td><p>Base URLs used by the swagger ui, separated by a comma</p></td>
<td><p><a href="https://localhost:8443,http://localhost:8080">https://localhost:8443,http://localhost:8080</a></p></td>
</tr>
<tr class="even">
<td><p>use-first-swagger-base-ui-addr-for-add-attachment</p></td>
<td><p>If set to true, the <code>addAttachment</code> method will use the first entry of <code>swagger-ui-base-addr</code> in its answer, otherwise the requesting address or <strong>X-Forward</strong> header us used</p></td>
<td><p>false</p></td>
</tr>
<tr class="odd">
<td><p>cleantime</p></td>
<td><p>Scheduled time for cronjob.
The fields read from left to right are interpreted as follows:</p>
<ul>
<li><p>Second</p></li>
<li><p>Minute</p></li>
<li><p>Hour</p></li>
<li><p>Day of Month</p></li>
<li><p>Month</p></li>
<li><p>Day of week</p></li>
</ul>
<p>Full documentation <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Scheduled.html">here</a>.</p></td>
<td><p>0 0 3 * * * (daily at 3 am)</p></td>
</tr>
<tr class="even">
<td><p>keepIdTime</p></td>
<td><p>Time UUI is unique in milliseconds</p></td>
<td><p>31536000000 (1 year)</p></td>
</tr>
<tr class="odd">
<td><p>endpoints</p></td>
<td><p>All these parameters can be used separately.
To use more than one, provide them comma separated.
The resource can be entered by <code>&lt;host&gt;&lt;port&gt;/attachments/v2.2/actuator/&lt;resource_name&gt;</code></p>
<p><strong>Please do not use this options in productive environment!</strong></p>
<ul>
<li><p>httptrace → Showing the http requests and corresponding answers (Just in memory and limited by the capacity)</p></li>
<li><p>logfile → Shows the regular logfile</p></li>
<li><p>env → Shows all environment variables</p></li>
<li><p>scheduledtasks → Shows all configured tasks, and their configuration</p></li>
<li><p>mappings → Shows all accessible endpoints</p></li>
<li><p>health → Shows status of the server</p></li>
</ul></td>
<td><p>NONE</p></td>
</tr>
<tr class="even">
<td><p>http_log_capacity</p></td>
<td><p>Amount of saved http requests and responses in memory and displayed by <code>httptrace</code> - endpoint</p></td>
<td><p>500</p></td>
</tr>
<tr class="odd">
<td><p>version</p></td>
<td><p>Version of the KAS.
This have an effect of the url!</p></td>
<td><p>v2.2</p></td>
</tr>
<tr class="even">
<td><p>kim-am-url</p></td>
<td><p>The location of the accountmanager for the basic auth authorization</p></td>
<td><p><a href="http://localhost:8082">http://localhost:8082</a></p></td>
</tr>
<tr class="odd">
<td><p>use-auth-initial</p></td>
<td><p>Switch if basicAuth should be used.
This can be switched of via the <code>/switchAuth</code> api for developing reasons</p></td>
<td><p>true</p></td>
</tr>
</tbody>
</table>

## cURL examples

To upload a file **data.file** use the POST method:

    $ curl -k -X POST https://localhost:8443/attachments/v2.2/attachment -H "Content-Type: application/octet-stream" --data-binary @data.file
    {"Shared-Link":"https://localhost:8443/attachments/v2.2/attachment/469bf002-701f-4362-a9bc-6585c1871250"}

The result of this call can be used to download into the file **download.file**:

    $ curl -k -X GET https://localhost:8443/attachments/v2.2/attachmenthttps://localhost:8443/attachments/v2.2/attachment/469bf002-701f-4362-a9bc-6585c1871250 -o download.file

## TLS - Hints

KAS delivers a HTTPS connection with TLS 1.2 and an RSA and brainpool ECC key that are compliment to Gematik specs.

To use modern OpenSSL with the TLS-ECC brainpool, you have explicit use brainpool curve, e.g.:

    $ openssl s_client -connect localhost:8443 \
       -curves brainpoolP256r1 \
       -CAfile GEM.RCA3-TEST-ONLY.pem \
       -cert mailuser-ecc.pem \
       -key mailuser-ecc.prv.pem

Without a parameter RSA is used, e.g.,

    $ openssl s_client -connect localhost:8443 \
       -CAfile GEM.RCA2-TEST-ONLY.pem \
       -cert mailuser-rsa1.pem \
       -key mailuser-rsa1.prv.pem
