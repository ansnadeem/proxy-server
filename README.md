# proxy-server
A simple local proxy server which caches content locally and responds back to the user with server response.

# Pre-requisites
## Java Runtime Evironment
Tested on: 
java 17.0.2 2022-01-18 LTS
Java(TM) SE Runtime Environment (build 17.0.2+8-LTS-86)

## A web browser
Mozilla is recommended for testing.

# Execution

1) Use the command:-
```
java -jar proxy.jar <port>
```
port - The local port number number the proxy will listen to


2) Navigate onto browser settings for proxy
3) Use address 127.0.0.1 as proxy host and the port used above as the command line argument, in the port setting.

# Output
- log.txt - Containing incoming requests
- cached - Folder containing cached content from processed requests
