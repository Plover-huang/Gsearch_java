import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.DynamicMBean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class SearchResult{
	private String url, title,content;

	/**
	 * @return the url
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setURL(String url) {
		this.url = url;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	public void printIt(){
		System.out.println("url\t->"+url);
		System.out.println("title\t->"+title);
		System.out.println("content\t->"+content);
	}
	public void writeFile(String filename){
		try {
			FileWriter file=new FileWriter(filename);
			file.write("url:"+url+"\n");
			file.write("title:"+title+"\n");
			file.write("content:"+content+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class GoogleAPI{
    static List<String> user_agents=new ArrayList<>(); 
    static int len_user_agent=0;
    //load user agents
	static{
    	try (
    		    InputStream fis = new FileInputStream("resources/user_agents");
    		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
    		    BufferedReader br = new BufferedReader(isr);
    		) {
    			String line="";
    		    while ((line = br.readLine()) != null) {
    		        user_agents.add(line);
    		    }
    		    System.out.println("success load user_agents");
    		} catch (IOException e) {
				e.printStackTrace();
			}
    	len_user_agent=user_agents.size();
    }
	
	private int timeout=40;
	private final int default_results_num=10;
	private final String base_url="https://www.google.com/";
	
	//generate random num between min and max
	private static int randInt(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
	
	public void randomSleep() {
		try {
			Thread.sleep(randInt(60, 120));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public String extractDomain(String url){
		String domain="";
		Pattern pattern=Pattern.compile("http[s]?://([^/]+)/");
		Matcher url_match=pattern.matcher(url);
		if(url_match.find()){
			domain=url_match.group(1);
		}
		return domain;
	}
	
	public String extractUrl(String href){
		String url="";
		Pattern pattern=Pattern.compile("(http[s]?://[^&]+)&");
		Matcher url_match = pattern.matcher(href);
		if(url_match.find()){
			url=url_match.group(1);
		}
		return url;
	}
	
	public List<SearchResult> extractSearchResults(Document html){
		List<SearchResult> results=new ArrayList<SearchResult>();
		Element div=html.getElementById("search");
		if(div!=null){
			Elements lis=div.select("div[class=g]");
			if(!lis.isEmpty()){
				for(Element li:lis){
					SearchResult result=new SearchResult();
					Element h3=li.select("h3[class=r]").first();
					if(h3==null)
						continue;
					Element link=h3.getElementsByTag("a").first();
					if(link==null)
						continue;
					String url=link.attr("href");
					url=extractUrl(url);
					if(url.isEmpty())
						continue;
					String title=link.text();
					result.setURL(url);
					result.setTitle(title);
					
					Element span=li.select("span[class=st]").first();
					if(span!=null){
						result.setContent(span.text());
					}
					results.add(result);
				}
			}
		}
		return results;
	}
	
	public List<SearchResult> search(String query, String lang, int num){
		List<SearchResult> search_results=new ArrayList<>();
		try {
			query=URLEncoder.encode(query,"utf-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int pages,start,retry=3;
		String url,user_agent;
		Document content;
		if(num%default_results_num==0){
			pages=num/default_results_num;
		}
		else pages=num/default_results_num+1;
		
		for(int p=0;p<pages;p++){
			start=p*default_results_num;
			url=base_url+"/search?hl="+lang+"&num="+default_results_num+"&start="+start+"&q="+query;
			while(retry>0){
				try{
					user_agent=user_agents.get(randInt(0, len_user_agent));
//					content=Jsoup.connect(url).timeout(timeout).userAgent(user_agent).get();
					//for test:
					content=Jsoup.parse(new File("resources/localhtml"), "UTF-8");
					
					List<SearchResult> results=extractSearchResults(content);
					search_results.addAll(0, results);
					break;
				}
				catch(Exception e){
					System.out.println(e);
					randomSleep();
					retry--;
					continue;
				}
			}
		}
		return search_results;
	}
}

public class Gsearch {
	public static void crawler(String keyword, boolean isFile){
		GoogleAPI googleAPI=new GoogleAPI();
		int expect_num=10;
		if(isFile){
			try (
	    		    InputStream fis = new FileInputStream("resources/keywords");
	    		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
	    		    BufferedReader br = new BufferedReader(isr);
	    		) {
	    			String line="";
	    		    while ((line = br.readLine()) != null) {
	    		    	List<SearchResult> results=googleAPI.search(line, "en", expect_num);
	    		    	for(SearchResult result:results){
	    		    		result.printIt();
	    		    		System.out.println();
	    		    	}
	    		    	System.out.println();
	    		    }
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			List<SearchResult> results=googleAPI.search(keyword, "en", expect_num);
			for(SearchResult result:results){
	    		result.printIt();
	    		System.out.println();
	    	}
		}
	}
	
	public static void main(String[] argv){
		Scanner scanner=new Scanner(System.in);
//		String keyword=scanner.next();
		crawler("", true);
	}
}
