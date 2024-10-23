package velocity.me.francies.ReportDiscordPlus;

import java.util.Base64;

public class a {


    public static String encodeUrl(String url) {
        return Base64.getEncoder().encodeToString(url.getBytes());
    }


    public static String decodeUrl(String encodedUrl) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedUrl);
        return new String(decodedBytes);
    }

    public static void main(String[] args) {
        String url = "https://www.francescoferrara.it/api/keys.json";


        String encodedUrl = encodeUrl(url);
        String decodedUrl = decodeUrl(encodedUrl);
    }
}
