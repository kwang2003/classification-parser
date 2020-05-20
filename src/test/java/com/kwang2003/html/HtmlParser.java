package com.kwang2003.html;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlParser {
	String folder = "d:/c/";
	static String[] USER_AGENTS = { "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:76.0) Gecko/20100101 Firefox/76.0",
			"User-Agent:Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)",
			"User-Agent:Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv 11.0) like Gecko",
			"User-Agent:Mozilla/5.0 (Android; Mobile; rv:14.0) Gecko/14.0 Firefox/14.0",
			"User-Agent:Mozilla/5.0 (compatible; WOW64; MSIE 10.0; Windows NT 6.2)",
			"User-Agent:Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; en) Presto/2.9.168 Version/11.52",
			"User-Agent:Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.229 Version/11.62"};

	@Test
	void test() throws IOException {
		String url = "http://www.stats.gov.cn/tjsj/tjbz/tjypflml/index%s.html";
		List<String> pages = Lists.newArrayList("", "_1", "_2", "_3", "_4");
		for (String p : pages) {
			hanle(String.format(url, p));
		}
	}

	void hanle(String url) throws IOException {
		Document document = document(url);
		handle1(document);
	}

	void handle1(Document document) {
		document.select(".cont_tit03").stream().filter(e -> !Strings.isNullOrEmpty(e.text())).forEach(e -> {
			Entity entity = new Entity();
			String text = e.text();
			String[] arr = text.split("-");
			entity.id = arr[0];
			entity.name = arr[1];
			entity.level = 1;
			File doneFile = new File(folder, String.format("%s.done", entity.id));
			if (doneFile.exists()) {
				return;
			}
			String file = "%s.sql";
			File f = new File(folder, String.format(file, entity.id));
			f.delete();
			log(entity, f);
			handle2(entity, f);
			try {
				doneFile.createNewFile();
			} catch (IOException e1) {
			}
		});
	}

	@SneakyThrows
	void handle2(Entity parent, File f) {
		String url = "http://www.stats.gov.cn/tjsj/tjbz/tjypflml/2010/%s.html";
		Document doc = document(String.format(url, parent.id));
		Optional.ofNullable(doc).ifPresent(document ->{
			document.select("tr[class='citytr']").forEach(tr -> {
				Entity entity = new Entity();
				entity.level = 2;
				entity.parentId = parent.id;

				Elements tds = tr.children();
				Element id = tds.get(0);
				Element name = tds.get(1);
				if (hasChildTag(id, "a")) {
					Element ida = id.getElementsByTag("a").get(0);
					Element namea = name.getElementsByTag("a").get(0);
					entity.id = ida.text();
					entity.name = namea.text();
					log(entity, f);
					handle3(entity, f);
				} else {
					entity.id = id.text();
					entity.name = name.text();
					log(entity, f);
				}
			});
		});
	}

	boolean hasChildTag(Element element, String tag) {
		return element.getElementsByTag(tag).size() > 0;
	}

	@SneakyThrows
	void handle3(Entity parent, File f) {
		String url = "http://www.stats.gov.cn/tjsj/tjbz/tjypflml/2010/%s/%s.html";
		Document doc = document(String.format(url, parent.id.subSequence(0, 2), parent.id));
		Optional.ofNullable(doc).ifPresent(document ->{
			document.select("tr[class='countytr']").forEach(tr -> {
				Entity entity = new Entity();
				entity.level = 3;
				entity.parentId = parent.id;

				Elements tds = tr.children();
				Element id = tds.get(0);
				Element name = tds.get(1);
				if (hasChildTag(id, "a")) {
					Element ida = id.getElementsByTag("a").get(0);
					Element namea = name.getElementsByTag("a").get(0);
					String href = ida.attributes().get("href");
					entity.id = href.split("/")[1].split("[.]")[0];
					entity.name = namea.text();
					log(entity, f);
					handle4(entity, f);
				} else {
					entity.id = id.text();
					entity.name = name.text();
					log(entity, f);
				}
			});
		});
	}

	@SneakyThrows
	void handle4(Entity parent, File f) {
		String url = "http://www.stats.gov.cn/tjsj/tjbz/tjypflml/2010/%s/%s/%s.html";
		Document doc = document(
				String.format(url, parent.id.subSequence(0, 2), parent.id.subSequence(2, 4), parent.id));
		Optional.ofNullable(doc).ifPresent(document ->{
			document.select("tr[class='towntr']").forEach(tr -> {
				Entity entity = new Entity();
				entity.level = 4;
				entity.parentId = parent.id;

				Elements tds = tr.children();
				Element id = tds.get(0);
				Element name = tds.get(1);
				if (hasChildTag(id, "a")) {
					Element ida = id.getElementsByTag("a").get(0);
					Element namea = name.getElementsByTag("a").get(0);
					String href = ida.attributes().get("href");
					entity.id = href.split("/")[1].split("[.]")[0];
					entity.name = namea.text();
					log(entity, f);
					Set<String> ignors = Sets.newHashSet();
					ignors.add("32080110");// 铁道用钢材按品种分
					ignors.add("32080130");// 铁道用钢材按化学成分分
					ignors.add("32080210");// 大型型钢按品种分
					ignors.add("32080230");//大型型钢按化学成分分
					ignors.add("32080240");
					ignors.add("32080310");
					ignors.add("32080330");
					ignors.add("32080340");
					if (ignors.contains(entity.getId())) {
						return;
					}
					handle5(entity, f);
				} else {
					entity.id = id.text();
					entity.name = name.text();
					log(entity, f);
				}
			});
		});
	}

	@SneakyThrows
	void handle5(Entity parent, File f) {
		String url = "http://www.stats.gov.cn/tjsj/tjbz/tjypflml/2010/%s/%s/%s/%s.html";
		Document doc = document(String.format(url, parent.id.subSequence(0, 2), parent.id.subSequence(2, 4),
				parent.id.subSequence(4, 6), parent.id));
		Optional.ofNullable(doc).ifPresent(document ->{
			document.select("tr[class='villagetr']").forEach(tr -> {
				Entity entity = new Entity();
				entity.level = 5;
				entity.parentId = parent.id;

				Elements tds = tr.children();
				Element id = tds.get(0);
				Element name = tds.get(1);
				if (hasChildTag(id, "a")) {
					Element ida = id.getElementsByTag("a").get(0);
					Element namea = name.getElementsByTag("a").get(0);
					entity.id = ida.text();
					entity.name = namea.text();
					log(entity, f);
				} else {
					entity.id = id.text();
					entity.name = name.text();
					log(entity, f);
				}
			});
		});
	}

	@SneakyThrows
	void log(Entity entity, File f) {
		log.info("{}", entity);
		String template = "insert into classification(id,name,level,parent_id,status) values('%s','%s',%s,'%s',%s);\n";
		String sql = String.format(template, entity.id, entity.name, entity.level, entity.parentId, 1);
		Files.asCharSink(f, Charsets.UTF_8, FileWriteMode.APPEND).write(sql);
	}

	@SneakyThrows
	void sleep10() {
		sleep(10000L);
	}

	@SneakyThrows
	void sleep5() {
		sleep(5000L);
	}

	@SneakyThrows
	void sleep(long n) {
		Thread.sleep(n);
	}

	Document document(String url) throws IOException {
		Map<String, String> headers = Maps.newHashMap();
		headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
		headers.put("Accept-Encoding", "gzip, deflate");
		try {
			Random random = new Random();
			int index = Math.abs(random.nextInt()%USER_AGENTS.length);
			Document document = Jsoup.connect(url).timeout(5000).userAgent(USER_AGENTS[index]).get();
			if (Strings.isNullOrEmpty(document.title())) {
				sleep10();
				return document(url);
			}
			return document;
		}catch(org.jsoup.HttpStatusException e) {
			log.error(Throwables.getStackTraceAsString(e));
			return null;
		}catch (Exception e) {
			log.error(Throwables.getStackTraceAsString(e));
			return document(url);
		}
	}

	@Data
	private static class Entity {
		String id;
		String parentId;
		String name;
		int level;
	}
}
