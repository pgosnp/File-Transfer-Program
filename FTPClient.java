import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class FTPClient {
    boolean checkPW = false;
    String rootDir;
    String currentDir;
    String ip;
    int port;

    public FTPClient(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        try {
            Socket socket = new Socket(ip, port);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            serve(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void serve(DataInputStream in, DataOutputStream out) throws IOException {
        sendName(out);
        sendPassword(in, out);
        System.out.println("check password: " + checkPW);
        receivePath(in);
        handleCommand(in, out);
    }

    public void handleCommand(DataInputStream in, DataOutputStream out) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("You can use the following command:\n" +
                "(1) ls: This command allows the users to browse the shared files with the corresponding file name and file size.\n" +
                "(2) cd ‘directory’: This command allows the users to change to current directory to the ‘directory’. For example, cd folder1\n" +
                "(3) pwd: This command prints working directory to the users.\n" +
                "(4) dl ‘filename(s)’: This command allows the users to download or multithread download the selected" +
                "files by input “dl ‘filename(s)’” such as:\n" +
                "Example 1: “dl 1.txt”. The example only download 1.txt.\n" +
                "Example 2:“dl 1.txt 2.txt 3.txt”. The example will download 1.txt,2.txt and 3.txt.\n" +
                "(5) dlall: This command allows the users to download the whole directory by multithread" +
                "downloading.\n" +
                "(6) exit: This command allows the users to quit the program and then close or disconnect the" +
                "connection from the network.\n");
        while (true) {
            sendPath(out);
            String command = null;
            String option = "";
            String statement;
            int internal = -1;
            System.out.print(">> ");
            statement = scanner.nextLine();
            int endIdx = statement.trim().indexOf(' ');
            if (endIdx > 0) {
                command = statement.substring(0, endIdx).trim();
                option = statement.substring(endIdx + 1).trim();
            } else
                command = statement;

            switch (command.toLowerCase()) {
                case "ls":
                    internal = 001;
                    break;
                case "cd":
                    internal = 002;
                    break;
                case "pwd":
                    internal = 003;
                    break;
                case "dl":
                    internal = 004;
                    break;
                case "dlall":
                    internal = 005;
                    break;
                case "exit":
                    internal = 006;
                    break;
            }

            out.writeInt(internal);
            out.writeInt(option.length());
            out.write(option.getBytes());
            System.out.println("internal: " + internal + " option: " + option + " option len: " + option.length());

            if (internal == 001 || internal == 003) {
                System.out.println(receiveMsg(in));
            } else if (internal == 002) {
                boolean checkPath = in.readBoolean();
                System.out.println("checkPath: " + checkPath);
                String str = receiveMsg(in);
                if (checkPath) {
                    currentDir = str;
                    System.out.println("Your current Dir: " + str);
                } else {
                    System.out.println(str);
                }
                System.out.println();
            } else if (internal == 004) {
                int fileNum = in.readInt();
                if (fileNum <= 1) {
                    receiveFile(in);
                } else {
                    mulitThreadReceive(fileNum);
                    continue;
                }
            } else if (internal == 006) {
                break;
            } else if (internal == -1) {
                System.out.println("You type the wrong command! Please type it again!");
                continue;
            }
        }
    }

    public void receivePath(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        int size = in.readInt();
        int count = 0, len = 0;
        while (count < size) {
            len = in.read(buffer, 0, buffer.length);
            currentDir = new String(buffer, 0, len);
            count += len;
        }
        rootDir = currentDir;
        System.out.println("Root Path: " + rootDir + "\n");
    }

    public void sendPath(DataOutputStream out) throws IOException {
        out.writeInt(currentDir.length());
        out.write(currentDir.getBytes());
    }


    public void sendName(DataOutputStream out) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input your name:");
        String name = scanner.nextLine();
        out.writeInt(name.length());
        out.write(name.getBytes());
        out.flush();
    }

    public void sendPassword(DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            try {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Please input the password to connect server:");
                String pw = scanner.nextLine();
                out.writeInt(pw.length());
                out.write(pw.getBytes());
                out.flush();
                System.out.println("pw: " + pw);
                checkPW = in.readBoolean();
                if (checkPW == true) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public String[] receiveDirFilelist(DataInputStream in) throws IOException {
        String name = "";
        String filelist[] = null;
        // is Dir
        name = getFilename(in);
        filelist = getDirfile(in);
        for (String file : filelist
                ) {
            System.out.println(file);
        }
        return filelist;
    }

    String[] getDirfile(DataInputStream in) throws IOException {
        String filelist[] = null;
        String str = "";
        byte[] buffer = new byte[1024];
        int size = in.readInt();
        int count = 0, len = 0;
        while (count < size) {
            len = in.read(buffer, 0, buffer.length);
            str += new String(buffer, 0, len);
            count += len;
        }
        filelist = str.split(" ");
        return filelist;
    }

    String getFilename(DataInputStream in) throws IOException {
        String name = "";
        byte[] buffer = new byte[1024];
        int size = in.readInt();
        int count = 0, len = 0;
        while (count < size) {
            len = in.read(buffer, 0, buffer.length);
            name += new String(buffer, 0, len);
            count += len;
        }
        return name;
    }

//    void receiveFile(String filename, DataInputStream in) throws IOException {
//        boolean checkfile = in.readBoolean();
//        boolean isFile = in.readBoolean();
//        byte[] buffer = new byte[1024];
//        File file;
//        FileOutputStream fout;
//        String filePath = "/Users/ShInGSon/test/" + filename;
//
//        if (checkfile && isFile) {
//            long count = 0, size = 0, len = 0;
//            file = new File(filePath);
//            fout = new FileOutputStream(file);
//            System.out.println("filePath: " + filePath);
//            size = in.readLong();
//            System.out.println("size: " + size);
//            while (count < size) {
//                len = in.read(buffer, 0, (int) Math.min(buffer.length, size - count));
//                count += len;
//                fout.write(buffer, 0, (int) len);
//            }
//            System.out.println(filePath + " is downloaded.");
//            fout.close();
//        } else if (checkfile && !isFile) {
//            int fileNum = in.readInt();
//            System.out.println("File Num: " + fileNum);
//            file = new File(filePath);
//            file.mkdirs();
//            System.out.println(file.getName() + " is made.");
//            while (fileNum != 0) {
//                String str = "";
//                int size = in.readInt();
//                int count = 0, len = 0;
//                while (count < size) {
//                    len = in.read(buffer, 0, buffer.length);
//                    str += new String(buffer, 0, len);
//                    count += len;
//                }
//                System.out.println("Inside file: " + str);
//                int count2 = 0, len2 = 0;
//                long size2 = 0;
//                File file2 = new File(str);
//                FileOutputStream fout2 = new FileOutputStream(file2);
//                System.out.println("filePath: " + filePath + "/" + str);
//                size2 = in.readLong();
//                System.out.println("size: " + size2);
//                while (count < size) {
//                    len2 = in.read(buffer, 0, (int) Math.min(buffer.length, size2 - count2));
//                    count2 += len2;
//                    fout2.write(buffer, 0, len2);
//                }
//                System.out.println(str + " is downloaded.");
//                fout2.close();
//                fileNum--;
//            }
//        } else if (!checkfile) {
//            String str = "";
//            int size = in.readInt();
//            int count = 0, len = 0;
//            while (count < size) {
//                len = in.read(buffer, 0, buffer.length);
//                str += new String(buffer, 0, len);
//                count += len;
//            }
//            System.out.println(str);
//        }
//    }

    private void receiveFile(DataInputStream in) throws IOException {
        String filename = receiveMsg(in);
        System.out.println(receiveMsg(in));
        byte[] buffer = new byte[1024];
        File file;
        FileOutputStream fout;
        String filePath = "/Users/ShInGSon/test/" + filename;
        long count = 0, size = 0, len = 0;
        file = new File(filePath);
        fout = new FileOutputStream(file);
        System.out.println("filePath: " + filePath);
        size = in.readLong();
        System.out.println(filename + " size: " + size);
        while (count < size) {
            len = in.read(buffer, 0, (int) Math.min(buffer.length, size - count));
            count += len;
            fout.write(buffer, 0, (int) len);
        }
        System.out.println(receiveMsg(in));
        fout.close();
    }

    private void mulitThreadReceive(int fileNum) throws IOException {
        int mPort = 9002;
        for (int i = 0; i < fileNum; i++) {
            new Thread(() -> {
                try {
                    Socket cSocket = new Socket(ip, mPort);
                    DataInputStream in = new DataInputStream(cSocket.getInputStream());
                    receiveFile(in);
                } catch (UnknownHostException e) {
                } catch (IOException e) {
                }
            }).start();
        }
    }

    public static void main(String[] args) throws IOException {
        FTPClient client = new FTPClient("127.0.0.1", 9001);
    }
}


