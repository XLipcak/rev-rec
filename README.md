# Optimal Recommendations for Source Code Reviews
This project deals with optimal recommendations of source code reviewers for open source projects. It contains the implementation of RevFinder algorithm (http://ieeexplore.ieee.org/document/7081824/), ReviewBot algorithm (https://labs.vmware.com/download/198/) and of a novel Naive Bayes-based <i>Code Reviewers Recommendation Algorithm</i>.

## Configure Database and import data:
#### You can either:
* Use the configuration defined in the <i>application.properties</i>. The connection is configured against the remote DB provided by Amazon Web Services. This database contains all the data which were used for testing the algorithms. However, this option is much slower than the local database and it should only be used to demonstrate the functionality of the application. Running all the test configurations against this DB is not recommended.

#### or

* * Download and install the [MySql Server](https://dev.mysql.com/downloads/mysql/).
  * Import the data from our [import scripts](https://github.com/XLipcak/rev-rec/tree/master/data/sql).
  * Set the database settings in the <i>application.properties</i> 

## Build and Run:
#### The aplication has the following requirements:
* [Apache Maven](https://maven.apache.org/download.cgi) has to be installed.
* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (at least version 8) has to be installed and <i>JAVA_HOME</i> environment variable has to be set and point to the JDK installation.
#### Deployment:
After the aforementioned steps are done, the application can be compiled and launched using the following command:
* `mvn spring-boot:run`
