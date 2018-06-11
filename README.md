# A Large-Scale Study on Source Code Reviewer Recommendation
This project deals with optimal recommendations of source code reviewers for open source projects. It contains the implementation of RevFinder algorithm (http://ieeexplore.ieee.org/document/7081824/), ReviewBot algorithm (https://labs.vmware.com/download/198/) and of a novel Naive Bayes-based <i>Code Reviewers Recommendation Algorithm</i>.

#### If you use the provided source code and data set, please cite:

> Lipcak, J., Rossi, B. (2018) A Large-Scale Study on Source Code Reviewer Recommendation, in 44th Euromicro Conference on Software Engineering and Advanced Applications (SEAA) 2018, IEEE.

## Build and Run:
#### The aplication has the following requirements:
* [Apache Maven](https://maven.apache.org/download.cgi) has to be installed.
* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (at least version 8) has to be installed and <i>JAVA_HOME</i> environment variable has to be set and point to the JDK installation.
#### Deployment:
After the aforementioned steps are done, the application can be compiled and launched from its root folder using the following command:
* `mvn spring-boot:run`
