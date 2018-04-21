import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class FileClient {
//    String[] filelist = new String[]{
//            "1.txt", "2.txt", "3.txt"
//    };
    String[] filelist;

    public FileClient(String ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
        System.out.println("Please input the filename(s)/directory(s): ");
        Scanner scanner = new Scanner(System.in);
        String str = scanner.nextLine();
        filelist= str.split(" ");
        out.writeInt(str.length());
        out.write(str.getBytes());
        for (String file : filelist
                ) {
            receiveFile(file, in);
        }
    }

    public void receiveFile(String filename, DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        long count = 0, size = 0, len = 0;
        File file;
        FileOutputStream fout;
        String filePath = "/Users/ShInGSon/test/" + filename;
        file = new File(filePath);
        fout = new FileOutputStream(file);
        size = in.readLong();
        while (count < size) {
            len = in.read(buffer, 0, (int) Math.min(buffer.length, size - count));
            count += len;
            fout.write(buffer, 0, (int) len);
            System.out.println(filePath + " is downloading.");
        }
        fout.close();
    }

    public static void main(String[] args) throws IOException {
        FileClient client = new FileClient("127.0.0.1", 9000);
    }
}
