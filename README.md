# File-Transfer-Program

This is a file sharing application which includes a server program and a client program.
Users can access file from another computer. A user can have server program and client
program at the same time. For example, user A can share some of his/her computer files
with her friends by using server program, by putting certain files in a specific directory
so that user B can download the file(s) or even the whole directory to his/her local
computer when user B is using client program to remotely connect to user A’s server
program. On the other hand, if user B also want to share his/her files to user A, user B
can do the same way as above by using server program. For security reason, server
program’s owner can set a password to control the access to these files. In the
meanwhile, client program users need to provide the correct password to remotely
connect to server program and access these shared files remotely. Other people are not
allowed to do so. Moreover, this application support of multiple-user downloading and
multithread downloading.

This file sharing application will use File transfer protocol (FTP). For multiple-user
downloading and multithread downloading implementation, multithread programming
is needed. It achieves concurrent usage by multiple client users and download several
files concurrently.
For server side, First, the server program requires user to set a password to control
access to the shared files. Once user set a password, it would not ask anymore. Second,
the user need to set a specific path for sharing the files and put them into the path
folder. After this, server program can wait and receive client request and respond
corresponding function to client program.
For client side, First, the client program requires user to type in the IP address and the
port number of the server program. Second, user need to input the correct password in
order to remotely connect to the specific server program. If user types the wrong

password, the client program requires the user types again until the password is correct.
After authentication, user can perform the following a set of “User Commands”.
