package org.tensorflow.lite.examples.detection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.util.ArrayList;

public class GetImageInfo extends android.os.AsyncTask<String,Void,ArrayList>{
    @Override
    protected ArrayList doInBackground(String... strings) {
        ArrayList<String> res = new ArrayList<String>();
        Document doc = null;
        try{
            doc = Jsoup.connect(strings[0]).get();
            Element elements = doc.select("#__NEXT_DATA__").first();

            Node n = elements.childNodes().get(0);
            String getStr = n.toString();


            int loca = getStr.indexOf("title");
            getStr = getStr.substring(loca + 5,getStr.length());
            int loct = getStr.indexOf("title");
            getStr = getStr.substring(loct,getStr.length());
            int locd = getStr.indexOf("desc");
            String title = "제목은 "+ getStr.substring(8,locd - 3) + " 입니다.";
            int loce = getStr.indexOf("}")-1;
            String desc = getStr.substring(locd+7,loce);
            desc = desc.replace("\\u003e","");
            desc = desc.replace("\\u003c","");
            res.add(title);
            res.add(desc);
        }catch(IOException e){
            e.printStackTrace();
        }
        return res;
    }
}
