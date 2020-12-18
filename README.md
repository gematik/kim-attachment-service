# KIM-KAS

## Docker

### Build with Spring-Boot

#### Commands

With the `spring-boot-maven-plugin` it is possible to generate a docker image out of the box.

    mvn spring-boot:build-image

The image will be available with name kim-kas.
To run it use this command:

    docker run --name kas -p 81:8080 -d kim-kas

#### Need

-   Docker installed

-   Maven 3.5+ or higher

#### How to use parameters

Parameters can be set by using the `-e <PARAMETER_NAME>=<VALUE>`

Example:

    docker run --name kas -p 81:8080 -d gematik.kim.kas.maxMailSize=30776 -e kim-kas

## Parameters

**List of parameters:**

All Parameters start with `gematik.kim.kas.`

<table>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>PARAMATER_NAME</p></td>
<td style="text-align: left;"><p>Description</p></td>
<td style="text-align: left;"><p>Default</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>maxMailSize</p></td>
<td style="text-align: left;"><p>Maximal allowed size of an e-mail</p></td>
<td style="text-align: left;"><p>524288000</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>storagePath</p></td>
<td style="text-align: left;"><p>Path to volume where attachments got stored</p></td>
<td style="text-align: left;"><p>./target/storage</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>logDir</p></td>
<td style="text-align: left;"><p>Path to logfile</p></td>
<td style="text-align: left;"><p>./target/logs</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>data_base_location</p></td>
<td style="text-align: left;"><p>Path to database storage</p></td>
<td style="text-align: left;"><p>./target/db/demo</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>hostUrl</p></td>
<td style="text-align: left;"><p>The main URL to be returned by storage calls</p></td>
<td style="text-align: left;"><p>localhost</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>protocol</p></td>
<td style="text-align: left;"><p>The used protocol (Slashes needed)</p></td>
<td style="text-align: left;"><p>https://</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>cleantime</p></td>
<td style="text-align: left;"><p>Scheduled time for cronjob. The fields read from left to right are interpreted as follows:</p>
<ul>
<li><p>Second</p></li>
<li><p>Minute</p></li>
<li><p>Hour</p></li>
<li><p>Day of Month</p></li>
<li><p>Month</p></li>
<li><p>Day of week</p></li>
</ul>
<p>Full documentation <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Scheduled.html">here</a>.</p></td>
<td style="text-align: left;"><p>0 0 3 * * *</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>keepFileTime</p></td>
<td style="text-align: left;"><p>Time a file remains in system in milliseconds</p></td>
<td style="text-align: left;"><p>7776000000 (90 days)</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>keepIdTime</p></td>
<td style="text-align: left;"><p>Time UUI is unique in milliseconds</p></td>
<td style="text-align: left;"><p>31536000000 (1 year)</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>storeType</p></td>
<td style="text-align: left;"><p>Name of the key-store-type</p></td>
<td style="text-align: left;"><p>PKCS12</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>certPath</p></td>
<td style="text-align: left;"><p>Path to certificate. Shows to self-signed example certificate. Default is set to RSA-Certificate. If KAS should run on ECC enter
<code>classpath:keystore/ecc/kas.gem.kim.telematik-test-ECC.p12</code></p></td>
<td style="text-align: left;"><p>classpath:keystore/rsa/kas.gem.kim.telematik-test.p12</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>pw</p></td>
<td style="text-align: left;"><p>Password of the certificate</p></td>
<td style="text-align: left;"><p>00</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>tsl</p></td>
<td style="text-align: left;"><p>Should server start with https</p></td>
<td style="text-align: left;"><p>true</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>endpoints</p></td>
<td style="text-align: left;"><p>All these parameters can be used separately. To use more than one, provide them comma separated. The resource can be entered by &lt;host&gt;&lt;port&gt;/v1.1/actuator/&lt;resource_name&gt;</p>
<p><strong>Please do not use this options in productive environment!</strong></p>
<ul>
<li><p>httptrace → Showing the http requests and corresponding answers (Just in memory and limited by the capacity)</p></li>
<li><p>logfile → Shows the regular logfile</p></li>
<li><p>env → Shows all environment variables</p></li>
<li><p>scheduledtasks → Shows all configured tasks, and their configuration</p></li>
<li><p>mappings → Shows all accessible endpoints</p></li>
<li><p>health → Shows status of the server</p></li>
</ul></td>
<td style="text-align: left;"><p>NONE</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>http_log_capacity</p></td>
<td style="text-align: left;"><p>Amount of saved http requests and responses in memory and displayed by <code>httptrace</code> - endpoint</p></td>
<td style="text-align: left;"><p>500</p></td>
</tr>
</tbody>
</table>
