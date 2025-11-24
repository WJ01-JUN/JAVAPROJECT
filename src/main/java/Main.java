import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        final String host;
        final int port;

        // 클래스패스 리소스에서 server.txt 읽기
        try (
                InputStream is = Main.class.getClassLoader().getResourceAsStream("server.txt");
        ) {
            if (is == null) {
                throw new IllegalStateException("server.txt 를 클래스패스에서 찾을 수 없습니다.");
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line1 = br.readLine();   // 첫 번째 줄: IP
                String line2 = br.readLine();   // 두 번째 줄: 포트

                if (line1 == null || line2 == null) {
                    throw new IllegalStateException("server.txt 형식이 잘못되었습니다. (두 줄 필요)");
                }

                host = line1.trim();
                port = Integer.parseInt(line2.trim());
            }

        } catch (IOException e) {
            throw new RuntimeException("server.txt 를 읽는 중 IO 오류가 발생했습니다.", e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("server.txt 두 번째 줄(포트 번호)이 숫자가 아닙니다.", e);
        }

        // 여기부터 host, port는 확정된 final 값
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);

        // 예: 여기서 소켓 만들기
        // try (Socket socket = new Socket(host, port)) { ... }
    }
}
