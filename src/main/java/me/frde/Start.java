package me.frde;

import javafx.application.Platform;

/**
 * Created by frede on 2016-12-22.
 */
public class Start {
    public static void main(String[] args){
        Platform.setImplicitExit(false);
        Moodle moodle = new Moodle("https://moodle.concordia.ca");
    }
}
