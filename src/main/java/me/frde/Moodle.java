package me.frde;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.browser.PageConfiguration;
import com.ui4j.api.interceptor.Interceptor;
import com.ui4j.api.interceptor.Request;
import com.ui4j.api.interceptor.Response;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by frede on 2016-12-22.
 */
public class Moodle implements Runnable{

    static Pattern coursePattern = Pattern.compile("(https:\\/\\/moodle.concordia.ca\\/moodle\\/course\\/view.php\\?id=)([0-9]+)");

    private String baseURL;

    private Thread thread;

    private BrowserEngine bEngine;
    private Page page;

    CountDownLatch loadLatch = new CountDownLatch(1);

    private String loadLatchUrl;

    private Interceptor interceptor;

    PageConfiguration pageConfiguration;

    public Moodle(String url){
        baseURL = url;
        interceptor = new Interceptor() {
            public void beforeLoad(Request request) {

            }

            public void afterLoad(Response response) {
                if(response.getUrl().toLowerCase().equalsIgnoreCase(loadLatchUrl.toLowerCase())) {
                    loadLatch.countDown();

                }
            }
        };

        pageConfiguration = new PageConfiguration(interceptor);

        loadLatchUrl = "https://moodle.concordia.ca/moodle/";
        thread = new Thread(this);
        bEngine = BrowserFactory.getWebKit();
        page = bEngine.navigate(url+"/moodle/login",pageConfiguration);
        page.show();
        thread.start();
    }

    public void run() {
        try {
            loadLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        page.close();
        page = bEngine.navigate(baseURL+"/moodle");
        page.show();
        System.out.println("Finding course matches...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Matcher m = coursePattern.matcher(page.getWindow().getDocument().getBody().getOuterHTML());

        page.close();

        HashMap<String, List<String>> studentMap = new HashMap<>();

        List<String> courceUrls = new ArrayList<String>();
        while(m.find()){
            courceUrls.add(m.group(2));
            System.out.println("Found course with id "+m.group(2));
            Course course = new Course(bEngine, m.group(2));
            for (String name : course.getUsers()) {
                if(!studentMap.containsKey(name)){
                    studentMap.put(name, new ArrayList<String>());
                }
                studentMap.get(name).add(course.getName());
            }
        }
        List<String> names = new ArrayList(studentMap.keySet());
        Collections.sort(names);
        String csvString = "";
        for(String name:names){
            csvString += "\""+name+"\"";
            for(String c: studentMap.get(name)){
                csvString += ",\""+c+"\"";
            }
            csvString += "\n";
        }

        File file = new File("output.csv");
        try {
            FileUtils.writeStringToFile(file,csvString, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
    }
}
