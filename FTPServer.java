import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class FTPServer {
    public static int connectCount = 0;
    String password = null;
    String path = null;
    String clientList = "";
    String clientInfo = "";
    ServerSocket sSocket;
    int port = -1;
    int internal = -1;
    File rootDir;

    public FTPServer(int port) throws IOException {
        this.port = port;
        sSocket = new ServerSocket(port);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please set password: ");
        password = scanner.nextLine();
        System.out.println("Please set specific shared path: ");
        path = scanner.nextLine();
        rootDir = new File(path);
        System.out.printf("password: %s and shared path: %s\n", password, rootDir.getCanonicalPath());
        scanner.close();
        while (true) {
            Socket cSocket = sSocket.accept();
            connectCount++;
            System.out.println("Connected client:" + connectCount);
            String clientName = receiveName(cSocket);
            clientInfo = "connected from " + clientName + ": " +
                    cSocket.getInetAddress().getHostAddress() + "\n";
            clientList += clientInfo;
            System.out.println(clientList);
            System.out.println("----------------------------------\n");
            new Thread(() -> {
                try {
                    serve(cSocket, clientName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void serve(Socket socket, String clientName) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        checkPassword(in, out);
        sendCurrentDir(out, rootDir);
        while (true) {
            if (internal == 006) break;
            handleCommand(in, out, clientName);
            System.out.println();
        }
    }

    private void handleCommand(DataInputStream in, DataOutputStream out, String clientName) throws IOException {
        String clientPath = receivePath(in); // Get the client current directory at every command.
        internal = in.readInt(); // Command code to internal code for communication.
        String option = receiveMsg(in);
        System.out.println("Handling command by " + clientName + "... internal: " + internal + " option: " + option);
        switch (internal) {
            case 001:
                listFiles(out, clientPath);
                break;
            case 002:
                changeDir(option, out, clientPath);
                break;
            case 003:
                pwd(out, clientPath);
                break;
            case 004:
                String[] optionList = option.split(" ");
                int fileNum = optionList.length;
                System.out.println("fileNum: " + fileNum);
                out.writeInt(fileNum);
                if (fileNum == 1) {
                    File f = new File(optionList[0]);
                    if (checkOption(f.getName(), out, clientPath)) {
                        if (f.isFile()) {
                            sendFile(optionList[0], out, clientPath);
                        } else {
                            String zip = zipFolder(clientPath, optionList[0]);
                            sendFile(zip, out, clientPath);
                        }
                    }
                } else {
                    multiThreadSend(optionList, clientPath);
                }
                break;
            case 005:
                int dirFileNum = fileNum(out, clientPath);
                out.writeInt(dirFileNum);
                String dirFile = "";
                File curDir = new File(clientPath);
                File[] curDirFile = curDir.listFiles();
                for (File f : curDirFile
                        ) {
                    dirFile += f.getName() + " ";
                }
                String[] dirFilelist = dirFile.split(" ");
                multiThreadSend(dirFilelist, clientPath);
                break;
            case 006:
                break;
        }
    }

    private String receiveName(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] buffer = new byte[1024];
        String name = "";
        int size = in.readInt();
        int count = 0, len = 0;
        while (count < size) {
            len = in.read(buffer, 0, buffer.length);
            count += len;
            name += new String(buffer, 0, len);
        }
        return name;
    }

    private void checkPassword(DataInputStream in, DataOutputStream out) throws IOException {
        boolean checkPW = false;
        while (true) {
            byte[] buffer = new byte[1024];
            String str = "";
            int size = in.readInt();
            System.out.println("getting client input...");
            System.out.println("size:" + size);
            int count = 0, len = 0;
            while (count < size) {
                len = in.read(buffer, 0, Math.min(buffer.length, size - count));
                count += len;
                str += new String(buffer, 0, len);
            }
            System.out.println("client input: " + str);

            // correct password
            if (str.equals(password)) {
                checkPW = true;
                System.out.println("client type the correct password.");
            }

            // wrong password
            else {
                checkPW = false;
                System.out.println("client type the wrong password.");
            }
            out.writeBoolean(checkPW);

            // break the loop, and go to handleCommand loop
            if (checkPW == true) {
                System.out.println("Going to handle client command.\n");
                System.out.println("----------------------------------\n");
                break;
            }
        }
    }

    private void sendCurrentDir(DataOutputStream out, File curDir) throws IOException {
        out.writeInt(curDir.getCanonicalPath().length());
        out.write(curDir.getCanonicalPath().getBytes());
        out.flush();
    }

    private String receivePath(DataInputStream in) throws IOException {
        int size = in.readInt();
        String clientPath = "";
        byte[] buffer = new byte[1024];
        int len = 0, count = 0;
        while (count < size) {
            len += in.read(buffer, 0, buffer.length);
            clientPath = new String(buffer, 0, len);
            count += len;
        }
        return clientPath;
    }

    public String receiveMsg(DataInputStream in) throws IOException {
        String msg = "";
        byte[] buffer = new byte[1024];
        int count = 0, len = 0;
        int size = in.readInt();
        while (count < size) {
            len = in.read(buffer, 0, Math.min(buffer.length, size - count));
            count += len;
            msg += new String(buffer, 0, len);
        }
        return msg;
    }

    private void pwd(DataOutputStream out, String clientPath) throws IOException {
        String msg = clientPath + "\n";
        String str = "The current working directory: " + msg;
        out.writeInt(str.length());
        out.write(str.getBytes());
        out.flush();
        System.out.println(str);
    }

    private void listFiles(DataOutputStream out, String clientPath) throws IOException {
        String info = "";
        File dir = new File(clientPath);
        File[] fileList = dir.listFiles();
        for (File file : fileList
                ) {
            info += getInfo(file) + "\n";
        }
        out.writeInt(info.length());
        out.write(info.getBytes());
        System.out.println(info + "info length: " + info.length());
    }

    private String getInfo(File f) {
        Date date = new Date(f.lastModified());
//        String ld = new SimpleDateFormat("MMM dd, yyyy").format(date);
        if (f.isFile()) {
            return String.format("%dKB\t%s",
                    (int) Math.ceil((float) f.length() / 1024),
                    f.getName());
        } else
            return String.format("<DIR>\t%s", f.getName());
    }

    private void changeDir(String path, DataOutputStream out, String clientPath) throws IOException {
        boolean checkPath = false;
        String msg = "";
        String currentP = clientPath + "\\";
        String newP = clientPath + "\\" + path;
        File dir = new File(currentP);
        File newDir = new File(newP);
        if (newDir.isDirectory() && !path.equals("..")) { // correct next path
            checkPath = true;
            out.writeBoolean(checkPath);
            System.out.println(newDir.getName() + " is a folder.");
            clientPath = newP;
            out.writeInt(clientPath.length());
            out.write(clientPath.getBytes());

        } else if (path.equals("..")) {

            // At root dir and cd ..
            if (dir.getCanonicalPath().equals(rootDir.getCanonicalPath())) {
                checkPath = false;
                out.writeBoolean(checkPath);
                msg = "This is home dir. You cannot go to Parent Dir anymore.";
                out.writeInt(msg.length());
                out.write(msg.getBytes());
                System.out.println(msg);
            }

            // Not at root dir and cd ..
            else {
                checkPath = true;
                out.writeBoolean(checkPath);
                clientPath = dir.getParent();
                out.writeInt(clientPath.length());
                out.write(clientPath.getBytes());
            }

            // Go to root Dir
        } else if (path.equals("~")) {
            checkPath = true;
            out.writeBoolean(checkPath);
            clientPath = rootDir.getCanonicalPath();
            out.writeInt(clientPath.length());
            out.write(clientPath.getBytes());
        }

        // Wrong next path
        else {
            checkPath = false;
            out.writeBoolean(checkPath);
            msg = newDir.getName() + " is not a folder. Please input again.";
            out.writeInt(msg.length());
            out.write(msg.getBytes());
            System.out.println(msg);
        }
    }

    private boolean checkOption(String option, DataOutputStream out, String clientPath) throws IOException {
        boolean checkOption = false;
        String filePath = clientPath + "/" + option;
        File file = new File(filePath);
        if (file.exists()) {
            checkOption = true;
        } else {
            checkOption = false;
        }
        out.writeBoolean(checkOption);
        return checkOption;
    }

    private void sendFile(String filename, DataOutputStream out, String clientPath) throws IOException {
        out.writeInt(filename.length());
        out.write(filename.getBytes());
        String filepath = clientPath + "/" + filename;
        String msg = "Start downloadning: " + filename;
        out.writeInt(msg.length());
        out.write(msg.getBytes());
        File file = new File(filepath);
        byte[] buffer = new byte[1024];
        System.out.println(file.getCanonicalPath());
        FileInputStream fin = new FileInputStream(file);
        long size = file.length();
        long count = 0;
        int len;
        out.writeLong(size);
        while (count < size) {
            len = fin.read(buffer, 0, buffer.length);
            count += len;
            out.write(buffer, 0, len);
            System.out.println(file.getName() + " sends out...");
        }
        msg = "Finish download: " + filename;
        out.writeInt(msg.length());
        out.write(msg.getBytes());
        fin.close();
        if (file.getName().contains(".zip")) {
            if (file.delete()) {
                System.out.println(file.getName() + " is deleted");
            }
        }
    }

    private void multiThreadSend(String[] filename, String clientPath) throws IOException {
        int mPort = 9002;
        ServerSocket multiServerSocket = new ServerSocket(mPort);
        for (int i = 0; i < filename.length; i++) {
            int fileID = i;
            Socket cSocket = multiServerSocket.accept();
            DataOutputStream out = new DataOutputStream(cSocket.getOutputStream());
            new Thread(() -> {
                try {
                    if (checkOption(filename[fileID], out, clientPath)) {
                        String filepath = clientPath + "/" + filename[fileID];
                        File f = new File(filepath);
                        if (f.isFile()) {
                            sendFile(filename[fileID], out, clientPath);
                        } else {
                            String zip = zipFolder(clientPath, filename[fileID]);
                            sendFile(zip, out, clientPath);
                        }
                    } else {
                        System.out.println((fileID + 1) + " option is wrong");
                    }
                } catch (IOException e) {
                }
            }).start();
        }
        multiServerSocket.close();
    }

    private int fileNum(DataOutputStream out, String clientPath) throws IOException {
        File curDir = new File(clientPath);
        File[] filelist = curDir.listFiles();
        int fileNum = filelist.length;
        return fileNum;
    }

    private String zipFolder(String srcDir, String folder) throws IOException {
        String zipFile = srcDir + "/" + folder + ".zip";
        new CreateZipFile(srcDir + "/" + folder, zipFile);
        return folder + ".zip";
    }

    public static void main(String[] args) throws IOException {
        FTPServer server = new FTPServer(9001);
    }
}
