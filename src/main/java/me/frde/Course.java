package me.frde;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.Page;
import com.ui4j.api.browser.PageConfiguration;
import com.ui4j.api.dom.Element;
import com.ui4j.api.interceptor.Interceptor;
import com.ui4j.api.interceptor.Request;
import com.ui4j.api.interceptor.Response;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * A moodle course, scrapes all the names from it
 */
public class Course {
    /**
     * Course name
     */
    private String name;
    /**
     * Course id
     */
    private String id;
    /**
     * Browser engine to open page
     */
    private BrowserEngine engine;
    /**
     * Current page
     */
    private Page page;

    //Latch / interceptors are used to make sure page is loaded before querying the names
    private CountDownLatch loadLatch = new CountDownLatch(1);

    private Interceptor interceptor;

    private PageConfiguration configuration;

    public Course(BrowserEngine engine, String id){
        this.id = id;
        this.engine = engine;

        interceptor = new Interceptor() {
            @Override
            public void beforeLoad(Request request) {

            }

            @Override
            public void afterLoad(Response response) {
                /**
                 * If it's a name page, countdown latch
                 */
                if(response.getUrl().contains("spage=")){
                    loadLatch.countDown();
                }
            }
        };

        configuration = new PageConfiguration(interceptor);
        //Go to initial page
        page = engine.navigate("https://moodle.concordia.ca/moodle/user/index.php?id="+id, configuration);
        page.show();
        //wait for it to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Get course name
        name = page.getDocument().getTitle().get().split(":")[0];

    }

    /**
     * Get a list of all student names for the course
     * @return
     */
    public List<String> getUsers(){
        List<String> names = new ArrayList<String>();
        while(getpageNames(names)){
            try {
                loadLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        page.close();
        return names;
    }

    /**
     * Get names from current page and go to next page
     * @param names
     * @return if a new page is found
     */
    private boolean getpageNames(List<String> names){

        //Find all student names from current page
        List<Element> nameEls = page.getDocument().queryAll("#participants strong a");
        for(Element e:nameEls){
            names.add(e.getInnerHTML());
        }
        //Go to next page if possible
        Optional<Element> next = page.getDocument().query(".next");
        if(next.isPresent()){
            next.get().click();
            System.out.println("Going to page");
            return true;
        }
        //No next page has been found
        return false;
    }

    public String getName(){
        return name;
    }
}
