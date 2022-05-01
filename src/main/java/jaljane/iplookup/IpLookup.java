package jaljane.iplookup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IpLookup {

    private static String SEP = ";";

    private static String EMPTY = "";

    private static int RATE = 40;

    public static void main(String[] args) {
        String infile = args[0];
        String outfile = args[1];
        List<String> ips = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(infile);
                FileWriter fw = new FileWriter(outfile, false)) {
            fw.append("\"status\""
                    + SEP
                    + "\"message\""
                    + SEP
                    + "\"ip\""
                    + SEP
                    + "\"country\""
                    + SEP
                    + "\"city\""
                    + SEP
                    + "\"isp\""
                    + SEP
                    + "\"org\""
                    + "\n");

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            int counter = 0;
            long rateLimitCheckpoint = System.currentTimeMillis() + 60 * 1000;
            while ((strLine = br.readLine()) != null) {
                counter++;
                if (!StringUtils.isEmpty(strLine) && !ips.contains(strLine)) {
                    System.out.println("lookup " + strLine + " ...");
                    HttpGet get = new HttpGet("http://ip-api.com/json/" + strLine);
                    try (CloseableHttpClient httpClient = HttpClients.createDefault();
                            CloseableHttpResponse response = httpClient.execute(get)) {
                        String out = EntityUtils.toString(response.getEntity());
                        ObjectMapper om = new ObjectMapper();
                        IpLookupResult result = om.readValue(out, IpLookupResult.class);
                        String outLine = result.getStatus() + SEP;
                        outLine += Objects.toString(result.getMessage(), EMPTY) + SEP;
                        outLine += Objects.toString(result.getQuery(), EMPTY) + SEP;
                        outLine += Objects.toString(result.getCountry(), EMPTY) + SEP;
                        outLine += Objects.toString(result.getCity(), EMPTY) + SEP;
                        outLine += Objects.toString(result.getIsp(), EMPTY) + SEP;
                        outLine += Objects.toString(result.getOrg(), EMPTY) + "\n";
                        fw.append(outLine);

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                    ips.add(strLine);
                    if (counter >= RATE - 1) {
                        long diff = System.currentTimeMillis() - rateLimitCheckpoint;
                        if (diff < 0) {
                            try {
                                System.out.println("reached rate limit. blocking for some while...");
                                Thread.sleep(Math.abs(diff));
                            } catch (InterruptedException e) {
                            }
                        }
                        counter = 0;
                        rateLimitCheckpoint = System.currentTimeMillis() + 60 * 1000;
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
