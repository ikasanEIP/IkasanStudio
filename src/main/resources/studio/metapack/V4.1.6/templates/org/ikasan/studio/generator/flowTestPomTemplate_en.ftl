<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>${groupId?xml}</groupId><artifactId>${artifactId?xml}</artifactId><version>${version?xml}</version></parent>
  <artifactId>user-flow-tests</artifactId>
  <dependencies>
    <dependency><groupId>${applicationGroupId?xml}</groupId><artifactId>${applicationArtifactId?xml}</artifactId><version>${applicationVersion?xml}</version><scope>test</scope></dependency>
    <dependency><groupId>org.ikasan</groupId><artifactId>ikasan-test</artifactId><version>${r"${version.ikasan}"}</version><scope>test</scope></dependency>
    <dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency>
    <dependency><groupId>org.apache.ftpserver</groupId><artifactId>ftpserver-core</artifactId><version>1.2.1</version><scope>test</scope></dependency>
    <dependency><groupId>org.apache.mina</groupId><artifactId>mina-core</artifactId><version>2.2.9</version><scope>test</scope></dependency>
  </dependencies>
  <build><plugins><plugin>
    <groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.2.5</version>
    <dependencies><dependency><groupId>org.apache.maven.surefire</groupId><artifactId>surefire-junit4</artifactId><version>3.2.5</version></dependency></dependencies>
  </plugin></plugins></build>
</project>
