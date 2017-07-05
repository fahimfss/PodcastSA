package com.example.fahim.podcastsa;

/**
 * Created by fahim on 03-Jul-17.
 */

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public abstract class DataSource extends AsyncTask<String, Void, String> {

    private final String url = "http://www.scientificamerican.com/podcasts/?page=";
    protected ArrayList <PodcastItem> podcastItemsGET;

    @Override
    protected String doInBackground(String... strings) {
        podcastItemsGET = new ArrayList<>();
        Document document = null;
        try {
            document = Jsoup.connect(url + strings[0]).timeout(25000).get();
            Elements elements1 = document.select("h3.t_small-listing-title.podcasts-listing__title");
            Elements elements2 = document.select("div.podcasts-listing__player.player.player-audio.player-audio-inline");
            Elements elements3 = document.select("div.t_meta.podcasts-listing__meta");

            int i = 0;
            for (Element e: elements1
                    ) {
                podcastItemsGET.add(new PodcastItem());
                podcastItemsGET.get(i++).setTitle(e.text());
                podcastItemsGET.get(i-1).setPosition(i);
            }
            i = 0;
            for (Element e: elements2
                    ) {
                podcastItemsGET.get(i++).setLink(e.select("a").attr("href"));
            }
            i = 0;
            for(Element e: elements3){
                Elements e4 = e.select("span");
                String s = "";
                s += e4.get(0).text();
                s += " | ";
                s += e4.get(1).text();
                podcastItemsGET.get(i).setLenght(e4.get(1).text());
                podcastItemsGET.get(i++).setSecondaryTitle(s);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}