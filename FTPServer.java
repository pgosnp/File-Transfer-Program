import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
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
            System.out.println("Connected client：" + connectCount);
            clientInfo = "connected from " + receiveName(cSocket) + ": " +
                    cSocket.getInetAddress().getHostAddress() + "\n";
            clientList += clientInfo;
            System.out.println(clientList);
            System.out.println("----------------------------------\n");
            new Thread(() -> {
                try {
                    serve(cSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void serve(Socket socket) throws IOException {
        boolean checkPW = false;
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        checkPassword(in, out, checkPW);
        sendCurrentDir(out, rootDir);
        while (true) {
            if (internal == 006) break;
            handleCommand(in, out);
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

    private void checkPassword(DataInputStream in, DataOutputStream out, boolean checkPW) throws IOException {
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
            if (str.equals(password)) {
                checkPW = true;
                System.out.println("client type the correct password.");
            } else {
                checkPW = false;
                System.out.println("client type the wrong password.");
            }
            out.writeBoolean(checkPW);
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

    private void handleCommand(DataInputStream in, DataOutputStream out) throws IOException {
        String clientPath = receivePath(in);
        internal = in.readInt();
        String option = receiveMsg(in);
        System.out.println("Handling command... internal: " + internal + " option: " + option);
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
                    sendFile(optionList[0], out, clientPath);
                } else {
                    multiThreadSend(optionList, clientPath);
                }
                break;
            case 005:
                break;
            case 006:
                break;
        }
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
        String clientCurrentDir = clientPath + "/";
        File dir = new File(clientCurrentDir);
        File[] fileList = dir.listFiles();

        for (File file : fileList
                ) {
            info += getInfo(file) + "\n";
        }
        System.out.println(info + "info length: " + info.length());

        out.writeInt(info.length());
        out.write(info.getBytes());
    }

    private String getInfo(File f) {
        Date date = new Date(f.lastModified());
        String ld = new SimpleDateFormat("MMM dd, yyyy").format(date);
        if (f.isFile()) {
            return String.format("%dKB\t%s\t%s",
                    (int) Math.ceil((float) f.length() / 1024),
                    ld, f.getName());
        } else
            return String.format("<DIR>\t%s\t%s", ld, f.getName());
    }

    private void changeDir(String path, DataOutputStream out, String clientPath) throws IOException {
        boolean checkPath = false;
        String msg = "";
        String currentP = clientPath + "/";
        String newP = clientPath + "/" + path;
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

            if (dir.getCanonicalPath().equals(rootDir.getCanonicalPath())) { // At root dir and cd ..

                checkPath = false;
                out.writeBoolean(checkPath);
                msg = "This is home dir. You cannot go to Parent Dir anymore.";
                out.writeInt(msg.length());
                out.write(msg.getBytes());
                System.out.println(msg);

            } else { // Not at root dir and cd ..

                checkPath = true;
                out.writeBoolean(checkPath);
                clientPath = dir.getParent();
                out.writeInt(clientPath.length());
                out.write(clientPath.getBytes());

            }
        } else if (path.equals("~")) {
            checkPath = true;
            out.writeBoolean(checkPath);
            clientPath = rootDir.getCanonicalPath();
            out.writeInt(clientPath.length());
            out.write(clientPath.getBytes());
        } else { // wrong next path

            checkPath = false;
            out.writeBoolean(checkPath);
            msg = newDir.getName() + " is not a folder. Please input again.";
            out.writeInt(msg.length());
            out.write(msg.getBytes());
            System.out.println(msg);

        }

    }

    private void checkFileAndDir(String filename, DataOutputStream out, String clientPath) throws IOException {
        boolean checkfilename = false;
        boolean isFile = true;
        String filePath = clientPath + "/" + filename;
        File file = new File(filePath);

        if (file.exists() && file.isDirectory()) { // is Dir
            checkfilename = true;
            out.writeBoolean(checkfilename);
            isFile = false;
            out.writeBoolean(isFile);
            out.writeInt(file.getName().length());
            out.write(file.getName().getBytes());
            File[] filelist_f = file.listFiles();
            String filelist = "";
            for (File f : filelist_f
                    ) {
                filelist += f.getName() + " ";
            }
            out.writeInt(filelist.length());
            out.write(filelist.getBytes());
        } else if (file.exists() && file.isFile()) { // is file
            checkfilename = true;
            out.writeBoolean(checkfilename);
            isFile = true;
            out.writeBoolean(isFile);
            out.writeInt(file.getName().length());
            out.write(file.getName().getBytes());
        } else if (!file.exists()) { // wrong name , file not exist
            checkfilename = false;
            out.writeBoolean(checkfilename);
            isFile = false;
            out.writeBoolean(isFile);
        }

    }

//    private void sendFile(String filename, DataOutputStream out, String clientPath) throws IOException {
//        String msg = "";
//        boolean checkfile = false;
//        boolean isFile = true;
//        byte[] buffer = new byte[1024];
//        String filePath = clientPath + "/" + filename;
//        File curDir = new File(clientPath);
//        File file = new File(filePath);
//        if (file.exists() && file.isDirectory()) {
//            checkfile = true;
//            out.writeBoolean(checkfile);
//            isFile = false;
//            out.writeBoolean(isFile);
//            out.writeInt(file.listFiles().length);
//            if (file.listFiles() != null) {
//                for (File f : file.listFiles()
//                        ) {
//                    out.writeInt(f.getName().length());
//                    out.write(f.getName().getBytes());
//                    System.out.println("filename: " + f.getName());
//                    downFile(f.getName(), out, filePath);
//                }
//            }
//        } else if (file.exists() && file.isFile()) {
//            checkfile = true;
//            out.writeBoolean(checkfile);
//            isFile = true;
//            out.writeBoolean(isFile);
//            System.out.println(file.getCanonicalPath());
//            FileInputStream fin = new FileInputStream(file);
//            long size = file.length();
//            long count = 0;
//            int len;
//            out.writeLong(size);
//            while (count < size) {
//                len = fin.read(buffer, 0, buffer.length);
//                count += len;
//                out.write(buffer, 0, len);
//                System.out.println(file.getName() + " sends out...");
//            }
////        fin.close();
////            msg = filename + " is downloaded.";
////            out.writeInt(msg.length());
////            out.write(msg.getBytes());
////            out.flush();
//        } else if (!file.exists()) {
//            checkfile = false;
//            out.writeBoolean(checkfile);
//            isFile = false;
//            out.writeBoolean(isFile);
//            msg = filename + " is not correct. Please input the correct download filename/folder.";
//            out.writeInt(msg.length());
//            out.write(msg.getBytes());
//            out.flush();
//        }
//
//    }

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
                    sendFile(filename[fileID], out, clientPath);
                } catch (IOException e) {
                }
            }).start();
        }
        multiServerSocket.close();
    }

    public static void main(String[] args) throws IOException {
        FTPServer server = new FTPServer(9001);
    }
}
