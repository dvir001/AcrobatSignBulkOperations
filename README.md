## Acrobat Sign Bulk Operation 
The Acrobat Sign Bulk Operations Tool is a comprehensive application designed to facilitate the efficient handling of bulk operations for Account/Group admins. With this tool, users can effortlessly perform tasks such as deleting agreements, downloading agreements and form fields, and hiding all agreements. The tool is specifically tailored for Account/Group admins, offering streamlined processes for managing agreements at scale.

## Key Features:
#### 1. Bulk Operations:
  <ul>
    <li>
      Delete: Seamlessly remove documents associated with agreements in bulk.
    </li>
      <li>
        Download: Easily retrieve agreements and their associated form fields in bulk.
      </li>
      <li>
        Hide: Quickly conceal all agreements for enhanced organization and management.
      </li>
    <li>
        Cancel Agreements: Cancel In-Progress agreements in bulk.
      </li>
     <li>
        Cancel reminders: Cancel reminders in bulk.
      </li>
  </ul>
  
#### 2. Advanced filtering:
  <ul>
  <li>
    Date Range Filtering: Fetch agreements based on specific date ranges, allowing for targeted retrieval of desired records.
  </li>
  <li>
    Acrobat Sign Group: Fetch agreements based on specific groups, allowing for targeted retrieval of desired records.
  </li>
  <li>
    Agreement Status Filtering: Filter agreements based on their status, enabling quick access to agreements in specific states (e.g., completed, pending).
  </li>
  <li>
    Role-Based Fetching: Fetch agreements based on assigned roles, simplifying the process of retrieving agreements associated with specific users or groups.
  </li>
  </ul>
  
#### 3. Workflow Agreement Fetch:
  <ul>
  
  <li>
    Streamlined Retrieval: Effortlessly fetch agreements associated with workflows directly from the application, ensuring easy access to relevant records.
  </li>
</ul>

#### 4. Download library templates form fields:
  <ul>
  <li>
    With this capability, users can retrieve and download library templates and their associated form fields directly from within the bulk operations tool. Users can also hide the library templates in bulk.
  </li>
</ul>

#### 5. Webform-Associated Agreements Retrieval:
  <ul>
  <li>
    Users can now access a list of created webforms and subsequently retrieve associated agreements to perform bulk operations (download agreements/form fields etc.).
  </li>
</ul>

### Delete Operation
The delete operation is available to delete the documents associated with agreements. To enable the delete operation, please raise the support ticket and sign the retention policy with enable "agreement_retention" flag.
<br>

![image](https://github.com/abhishekdixitadobe/AcrobatSignBulkOperations/assets/93244386/b0cf89cd-0b3f-43c5-ab65-51f81badf6c3)


## Current vs Proposed solution

### Current process
 There is no OOTB solution available for bulk operations and this application is a platform for all the bulk operations.

### Proposed solution

<ul>
  <li>
    This application provides users to perform different bulk operations based on role. This will be a single platform to perform different bulk operations.
  </li>
  </ul>

## Technology stack
  <ul>
     <li>Java 17 or newer</li>
     <li>Spring Boot 4 (exact dependency versions are pinned in pom.xml)</li>
  </ul>

# Instructions to run the application
 <ul>
     <li>Please ensure that JDK 17 or newer is installed on the machine.</li>
     <li>Download the users list to run the tool for All users in the account. From Acrobat Sign Account -> Users -> Export all users. Remove users except for active users and remove columns except for email addresses.</li>
     <li>Create integration key - https://helpx.adobe.com/sign/kb/how-to-create-an-integration-key.html </li>
     <li>Copy src/main/resources/application.yml to your working folder and update it with:
       <ul>
        <li>correct integration-key</li>
        <li>Update baseUrl</li>
        <li>Update agreement_status to include/exclude agreements based on status</li>
       </ul>
      </li>
     <li>Build from source as described below, then run application.bat or <code>java -jar target/acrobatsignbulkoperationtool-0.0.1-SNAPSHOT.jar</code> from your configuration folder.</li>
        <li> 
     The application will run as http://localhost:8090/
  </li>
  </ul>

## Windows EXE releases

Maintainers can open **Actions → Windows EXE release → Run workflow**, select the
branch to build, and enter a new version such as `1.0.0` (without a `v` prefix).
First configure the repository secret `NVD_API_KEY` with an NVD API key; the
workflow refuses to release without a successful dependency vulnerability scan.
The workflow runs the Maven tests, rebuilds from source on Windows x64, and publishes a GitHub
release tagged `v1.0.0` at the selected commit with
`AcrobatSignBulkOperations-1.0.0-windows-x64.zip` containing the `.exe` installer and a
credential-free `application.yml` template, SBOM, dependency inventory, and scan
reports. A separate `.sha256` asset records the ZIP checksum. Release installers are packaged as
a ZIP, never as a standalone `.exe`. Use a new version for each release;
existing releases are not overwritten. Windows version limits are `255.255.65535`.
The workflow must be on the default branch for the **Run workflow** button to appear.

To run a release:

1. Download the ZIP from **Releases**, extract it into a writable working folder,
   and run the extracted `.exe` installer. It includes Java, so
   a separate Java installation is not required. The installer is unsigned and
   Windows may display a security warning.
2. Open the extracted `application.yml` in that working folder.
   Update `integration-key`, `baseUrl`, and any filters as described above.
   Keep this file private; do not upload your integration key.
3. Set `APP_PASSWORD` to a strong, unique local application password (not your Adobe
   integration key). Optionally set `APP_USERNAME` (default: `operator`).
   If no password is configured, a random password is printed to the console at
   startup and changes on each restart. Keep the console output private.
4. Open Command Prompt in that working folder and run the installed launcher,
   using its full path: `"<installation-folder>\AcrobatSignBulkOperations.exe"`.
   The application reads `application.yml` from the working folder and writes
   relative output paths there. Do not launch from a protected system directory.
5. Keep the console open and visit http://localhost:8090/. Sign in using the local
   application credentials. The packaged application listens only on the local
   machine by default. Press Ctrl+C in the console to stop it.

# Instructions on how to run the code (For developers)
## Prerequisites
For the building of this project, the client machine should have the following software installed:
<ul>
  <li>
    OS: Windows/Mac/Linux
  </li>
  <li>
    Java JDK: version 17 or newer
  </li>
  <li>
    Maven: 3.6.3 or newer. Download URL: https://maven.apache.org/download.cgi
  </li>
</ul>
  
## Installation
To install the application to your local repository:
<ul>
  <li>
    Please add JAVA_HOME as an environment variable and set the path.
  </li>
  <li>
    Ensure Java and Maven are on PATH and JAVA_HOME points to your JDK.
  </li>
  <li>
    Run <code>mvn --batch-mode --no-transfer-progress clean verify</code>, or run <code>run.bat</code> on Windows.
  </li>
  <li> 
    The build runs the tests and creates an executable JAR, dependency inventory, and CycloneDX SBOM under <code>target/</code>.
  </li>
  <li> 
     Start the built JAR with <code>application.bat</code> or <code>java -jar target/acrobatsignbulkoperationtool-0.0.1-SNAPSHOT.jar</code>, then visit http://localhost:8090/.
  </li>
</ul>

## Documentation for API Endpoints
Authenticated API documentation is available at http://localhost:8090/swagger-ui/index.html.

## Security and release verification

- The server binds to `127.0.0.1` by default, including when an external configuration file is used. Do not expose it to a network without a separate deployment security review and HTTPS.
- Authentication and operator authorization are required for application operations. Browser mutations require a CSRF token; API clients must authenticate and supply a token rather than disabling CSRF.
- For all launch modes, set `APP_USERNAME` (default `operator`) and `APP_PASSWORD`, or use `spring.security.user.name` and `spring.security.user.password` in your private external configuration. With no password configured, the console prints a fresh random password for that run. Do not reuse the Adobe integration key as the local login password.
- Uploads use a fresh private temporary directory and a server-selected filename, never a client-supplied filesystem path. Temporary documents are removed after the Adobe request, including failures.
- Configuration backups, generated binaries, and downloaded agreement data are no longer tracked. Local configuration backups and downloaded agreements are preserved, but **previously committed integration keys must be revoked and replaced**: deleting a file does not remove it from Git history. Clean builds replace the contents of `target/`. Review historical output data and old release assets separately; history has not been rewritten.
- Build release artifacts from reviewed source instead of using old checked-in JARs. `target/dependency-tree.txt` contains the resolved dependency inventory; `target/bom.json` and `target/bom.xml` contain the CycloneDX SBOM, including test dependencies.
- Browser scripts are separately pinned to jQuery 3.5.1 and FileSaver.js 2.0.5 with SHA-384 subresource-integrity checks. Unused Bootstrap JavaScript has been removed. These CDN scripts and external stylesheets are not included in the Maven SBOM and require separate review when updated.
- Run `mvn --batch-mode --no-transfer-progress -Psecurity-scan clean verify` to build and scan resolved dependencies with OWASP Dependency-Check. Set `NVD_API_KEY` for reliable NVD access. The scan also needs access to its advisory databases; a database download failure is not a clean scan. The scan fails on reported vulnerabilities or scanner errors; investigate findings and upgrade dependencies rather than bypassing the gate. The Maven inventory does not cover externally hosted browser assets. Advisory scanning is not malware detection and does not prove an artifact is safe.
- The Windows release workflow caches Maven dependencies and the Dependency-Check database separately. Its **Update vulnerability database** step checks NVD for updates on every run, then saves the successfully updated database before scanning so vulnerability findings do not discard the download. Compilation and tests are not the source of the initial long wait: a cold database download contains hundreds of thousands of records and can take an hour or more. Let that first update finish; subsequent releases restore the database and normally fetch only changes. Cache eviction or a changed Dependency-Check version requires a fresh database download. The database is outside `target/`, so `clean` does not delete it; cache hits never skip the security scan or bypass update failures.

# Future automation opportunities
<ul>
   <li>
      OAuth 2.0 setup
   </li>
  </ul>
