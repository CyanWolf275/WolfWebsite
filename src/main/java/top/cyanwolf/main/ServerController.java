package top.cyanwolf.main;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Controller
public class ServerController {

    @Autowired
    public JdbcTemplate jt;
    private final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat SDF2 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public ModelAndView indexPage(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("index");
        String ip = request.getRemoteAddr();
        modelAndView.addObject("ip", ip);
        URL url = null;
        try {
            url = new URL("http://ip-api.com/csv/" + ip/* + "?lang=zh-CN" */);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(5000);
            con.connect();
            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                String content = new String(in.readLine().getBytes(), StandardCharsets.UTF_8);
                String[] msg = content.split(",");
                modelAndView.addObject("from", msg[1] + "/" + msg[4]/*.substring(0, msg[4].length() - 2)*/ + "/" + msg[5]);
                in.close();
            }
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return modelAndView;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView loginPage(){
        return new ModelAndView("login");
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String defaultPage(){
        return "redirect:/index";
    }

    @RequestMapping(value = "dashboard")
    public ModelAndView dataPage(HttpServletRequest request, @RequestParam(value = "type", required = false) int[] type){
        ModelAndView modelAndView = new ModelAndView("dashboard");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        long id = user.getId();
        final String group = request.getParameter("groupid");
        final String start = request.getParameter("starttime") + " 00:00:00";
        final String end = request.getParameter("endtime") + " 23:59:59";
        String txt = request.getParameter("filtertxt");
        List<Map<String, Object>> authlst = null;
        String[] groups = null;
        if(user.getRoles().contains("ADMIN")){
            authlst = jt.queryForList("select id from Groups");
            groups = new String[authlst.size()];
            for(int i=0;i<authlst.size();i++)
                groups[i] = authlst.get(i).get("id").toString();
        } else{
            authlst = jt.queryForList("select groups from all_members_groups where id=" + id);
            groups = ((String) authlst.get(0).get("groups")).split(",");
        }
        List<Group> groupchoice = new ArrayList<Group>();
        for(String groupid : groups){
            List<Map<String, Object>> groupnamelst = jt.queryForList("select name from Groups where id=" + Long.parseLong(groupid));
            groupchoice.add(new Group(groupid, (String) (groupnamelst.get(0).get("name"))));
        }
        modelAndView.addObject("groupchoice", groupchoice);
        if(group != null && start != null && end != null && type != null){
            if(user.getRoles().contains("ADMIN") || in(groups, group)){
                List<Message> lst = null;
                List<GroupMember> msgcount = null;
                String typestr = "";
                for(int num : type)
                    typestr += "type = " + num + " or ";
                typestr = typestr.substring(0, typestr.length() - 4);
                if(Objects.isNull(txt)){
                    lst = jt.query("select * from `" + group + "` where time >= ? and time <= ? and (" + typestr + ")", new PreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, start);
                            ps.setString(2, end);
                        }
                    }, new MessageMapper());
                    msgcount = jt.query("select name, count(*) from `" + group + "` where time >= ? and time <= ? and (" + typestr + ") group by name order by count(*) desc", new PreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, start);
                            ps.setString(2, end);
                        }
                    }, new GroupMemberMapper());
                }else{
                    final String txtdecode = txt.replaceAll("/", "//").replaceAll("%", "/%").replaceAll("_", "/_");
                    lst = jt.query("select * from `" + group + "` where time >= ? and time <= ? and (" + typestr + ") and content like concat(\'%\', ?, \'%\') escape \'/\'", new PreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, start);
                            ps.setString(2, end);
                            ps.setString(3, txtdecode);
                        }
                    }, new MessageMapper());
                    msgcount = jt.query("select name, count(*) from `" + group + "` where time >= ? and time <= ? and (" + typestr + ") and content like concat(\'%\', ?, \'%\') escape \'/\' group by name order by count(*) desc", new PreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, start);
                            ps.setString(2, end);
                            ps.setString(3, txtdecode);
                        }
                    }, new GroupMemberMapper());
                }
                int sum = 0;
                for(GroupMember member : msgcount)
                    sum += member.msgcount;
                modelAndView.addObject("totalmsg", sum);
                if(msgcount.size() > 30){
                    modelAndView.addObject("msgcountlst", msgcount.subList(0, 30));
                }else{
                    modelAndView.addObject("msgcountlst", msgcount);
                }
                modelAndView.addObject("index", lst);
                modelAndView.addObject("groupid", group);
            }else{
                return new ModelAndView("Error");
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "member")
    public ModelAndView memberPage(HttpServletRequest request, @RequestParam(value = "group", required = false) Long group) throws ParseException {
        ModelAndView modelAndView = new ModelAndView("member");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        List<Map<String, Object>> authlst = jt.queryForList("select groups from all_members_groups where id=" + user.getId());
        String[] groups = ((String) authlst.get(0).get("groups")).split(",");
        if(group != null){
            if(user.getRoles().contains("ADMIN") || in(groups, group.toString())){
                modelAndView.addObject("selectedgroup", new Group(group.toString(), (String) (jt.queryForList("select name from Groups where id=" + Long.parseLong(group.toString())).get(0).get("name"))));
                List<GeneralMember> lst = null;
                lst = jt.query("select id,name,jointime,age,level from `" + group + "Members`", new GeneralMemberMapper());
                for(GeneralMember member : lst){
                    List<Map<String, Object>> info = jt.queryForList("select time from `" + group + "` where sender=" + member.id + " order by `time` desc limit 1");
                    if(info.size() > 0){
                        long lastSeen = new Date().getTime() / 1000L - ((LocalDateTime)(info.get(0).get("time"))).toEpochSecond(ZoneOffset.ofHours(8));
                        if(lastSeen < 60)
                            member.setLastSeen(lastSeen + "秒前");
                        else if(lastSeen < 3600)
                            member.setLastSeen(lastSeen / 60 + "分钟前");
                        else if(lastSeen < 86400L)
                            member.setLastSeen(lastSeen / 3600 + "小时前");
                        else
                            member.setLastSeen(lastSeen / 86400L + "天" + (lastSeen % 86400L) / 3600L + "小时前");
                    }else
                        member.setLastSeen("无");
                }
                modelAndView.addObject("members", lst);
            }else
                return new ModelAndView("Error");
        }else{
            return new ModelAndView("Error");
        }
        return modelAndView;
    }

    @RequestMapping(value = "clock")
    public ModelAndView clockPage(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("clock");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping(value = "/api/test")
    public Map<String, Object> authentication(@RequestHeader(value = "Auth") String auth, @RequestHeader(value = "Authname") String authname){
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        List<Map<String, Object>> authlst = jt.queryForList("select token from web where name=\'" + authname + "\'");
        if(authlst.size() > 0){
            String crypt = authlst.get(0).get("token").toString();
            if(bCryptPasswordEncoder.matches(auth, crypt)){
                return authlst.get(0);
            }else{
                return new HashMap<String, Object>();
            }
        }
        else
            return new HashMap<String, Object>();
    }
    @ResponseBody
    @RequestMapping(value = "/api/location", method = RequestMethod.GET)
    public int location(@RequestHeader(value = "Auth") String auth, @RequestHeader(value = "Authname") String authname, @RequestParam(value = "latitude") double latitude, @RequestParam(value = "longitude") double longitude, @RequestParam(value = "altitude") double altitude, @RequestParam(value = "battery") int battery){
        try{
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            List<Map<String, Object>> authlst = jt.queryForList("select token from web where name=\'" + authname + "\'");
            if(authlst.size() > 0){
                String crypt = authlst.get(0).get("token").toString();
                if(bCryptPasswordEncoder.matches(auth, crypt)){
                    String time = SDF.format(new Date());
                    String name = authname;
                    jt.execute("insert into `position`(time, name, latitude, longitude, altitude, battery) values (\'" + time + "\',\'" + name + "\'," + latitude + "," + longitude + "," + altitude + "," + battery + ")");
                    return 0;
                }else{
                    return -1;
                }
            }
            else
                return -1;
        }catch (Exception e){
            System.out.println(e);
            return -1;
        }
    }
    @ResponseBody
    @RequestMapping(value = "/chat/api", method = RequestMethod.GET)
    public Object chatresponse(@RequestParam(value = "method") String method, @RequestParam(value = "msg") String msg) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        long id = user.getId();
        ChatUser chat = user.getChatUser();
        Map<String, Object> result = new HashMap<String, Object>();
        if(method.equals("init")){
            return chat.getMsgList();
        }else if(method.equals("instru")) {
            chat.setInstruction(msg);
            return chat.getMsgList();
        }else if(method.equals("clear")){
            chat.clearHistory();
            return chat.getMsgList();
        }else{
            if(!chat.chatReady){
                result.put("status", -3);
                return result;
            }
                ChatResponse response = chat.chat(msg);
                if(!Objects.isNull(response)){
                    if(response.tokens == -1){
                        if(response.response.equals("timeout")){
                            result.put("status", -1);
                            result.put("time", response.time);
                        }else if(response.response.equals("apierror")){
                            result.put("status", -2);
                            result.put("time", response.time);
                        }
                    }else{
                        jt.execute("INSERT INTO PrivateChat(user,time,tokens) VALUES(" + id + ",\'" + response.time + "\'," + response.tokens + ")");
                        result.put("status", 0);
                        result.put("time", response.time);
                        result.put("response", response.response);
                    }
                }
        }
        return result;
    }

    @RequestMapping(value = "chat/download", method = RequestMethod.GET)
    public void chatdownload(HttpServletResponse response) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        long id = user.getId();
        ChatUser chat = user.getChatUser();
        String content = chat.chatListSave();
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + SDF2.format(new Date()) + ".txt");
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Length", "" + buffer.length);
        OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
        outputStream.write(buffer);
        outputStream.flush();
    }
    @ResponseBody
    @RequestMapping(value = "chat/setinstruction", method = RequestMethod.GET)
    public int instruction(@RequestParam(value = "title") String title, @RequestParam(value = "content") String content){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(Objects.isNull(title) || Objects.isNull(content) || title.length() == 0 || content.length() == 0)
            return -1; //invalid input
        List<Map<String, Object>> result = jt.queryForList("select content from `instructions` where username=" + user.getId() + " and title=\'" + title + "\'");
        if(result.isEmpty()){
            jt.update("insert into `instructions`(username,title,content) values(?,?,?)", new PreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setLong(1, user.getId());
                    ps.setString(2, title);
                    ps.setString(3, content);
                }
            });
            return 0; //success
        }else
            return -2; //duplicate entry
    }
    @ResponseBody
    @RequestMapping(value = "chat/getinstruction", method = RequestMethod.GET)
    public List<Map<String, Object>> getInstruction(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        return jt.queryForList("select title,content from `instructions` where username=" + user.getId());
    }
    @ResponseBody
    @RequestMapping(value = "chat/deleteinstruction", method = RequestMethod.GET)
    public int deleteInstruction(@RequestParam(value = "title") String title){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(Objects.isNull(title) || title.length() == 0)
            return -1; //invalid input
        try{
            jt.update("delete from `instructions` where username=? and title=?", new PreparedStatementSetter() {

                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setLong(1, user.getId());
                    ps.setString(2, title);
                }
            });
            return 0; //success
        }catch (DataAccessException e){
            return -2; //no such title
        }
    }
    @RequestMapping(value = "user", method = RequestMethod.GET)
    public ModelAndView userSettings(@RequestParam(value = "secret", required = false) String secret) throws QrGenerationException {
        ModelAndView modelAndView = new ModelAndView("user");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(!user.getRoles().contains("ADMIN"))
            return new ModelAndView("Error");
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        if(Objects.isNull(secret)){
            if(Objects.isNull(user.getSecret()) || user.getSecret().length() == 0){
                modelAndView.addObject("secretbool", false);
            }else{
                modelAndView.addObject("secretbool", true);
            }
            modelAndView.addObject("imggen", false);
        }else{
            SecretGenerator secretGenerator = new DefaultSecretGenerator(64);
            String generate = secretGenerator.generate();
            jt.execute("update Users set secret=\'" + generate + "\' where id=" + user.getId());
            user.setSecret(generate);
            QrData data = new QrData.Builder().label(user.getName()).secret(generate).issuer("cyanwolf.furmijk.cn").algorithm(HashingAlgorithm.SHA256).digits(6).period(30).build();
            QrGenerator generator = new ZxingPngQrGenerator();
            modelAndView.addObject("imggen", true);
            modelAndView.addObject("secretimg", getDataUriForImage(generator.generate(data), generator.getImageMimeType()));
            if(Objects.isNull(user.getSecret()) || user.getSecret().length() == 0){
                modelAndView.addObject("secretbool", false);
            }else{
                modelAndView.addObject("secretbool", true);
            }
        }
        return modelAndView;
    }
    @ResponseBody
    @RequestMapping(value = "user/verify", method = RequestMethod.GET)
    public Map<String, Boolean> verify(@RequestParam(name = "code") String code) throws UnknownHostException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(!user.getRoles().contains("ADMIN"))
            return null;
        String secret = user.getSecret();
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        codeVerifier.setTimePeriod(30);
        codeVerifier.setAllowedTimePeriodDiscrepancy(0);
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        result.put("result", codeVerifier.isValidCode(secret, code));
        return result;
    }

    @RequestMapping(value = "chat", method = RequestMethod.GET)
    public ModelAndView chat(){
        ModelAndView modelAndView = new ModelAndView("chat");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        modelAndView.addObject("gpt4", user.getGpt4());
        return modelAndView;
    }
    @RequestMapping(value = "webchat", method = RequestMethod.GET)
    public ModelAndView webchat(){
        ModelAndView modelAndView = new ModelAndView("webchat");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        modelAndView.addObject("name", user.getName());
        modelAndView.addObject("userid", user.getId());
        modelAndView.addObject("key", user.getTempkey());
        return modelAndView;
    }
    @ResponseBody
    @RequestMapping(value = "webchat/history", method = RequestMethod.GET)
    public List<Map<String, Object>> webchatHistory(@RequestParam(name = "startTime") String time, @RequestParam(name = "session") Long session){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> authlst = jt.queryForList("select groups from Users where id=" + user.getId());
        if(authlst.isEmpty())
            return result;
        String[] groups = authlst.get(0).get("groups").toString().split(",");
        boolean inGroup = false;
        for(String groupid : groups)
            if(groupid.equals(session.toString()))
                inGroup = true;
        if(!inGroup)
            return result;
        result = jt.queryForList("select time,sender,content,encrypted from chatmessages where groupid=" + session + " limit 50");
        return result;
    }
    private static boolean in(String[] lst, String value){
        for(String lststring : lst)
            if(lststring.equals(value))
                return true;
        return false;
    }

    @RequestMapping(value = "map")
    public ModelAndView mapPage(HttpServletRequest request){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(!user.getRoles().contains("ADMIN"))
            return new ModelAndView("Error");

        //List<Map<String, Object>> location = jt.queryForList("select * from location where id=2715720177 order by time desc");
        return new ModelAndView("map");
    }
    @ResponseBody
    @RequestMapping(value = "map/location", method = RequestMethod.GET)
    public Map<String, Object> location(@RequestParam(name = "code") String code) throws UnknownHostException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MyUser user = ((MyUser) authentication.getPrincipal());
        if(!user.getRoles().contains("ADMIN"))
            return null;
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        codeVerifier.setTimePeriod(30);
        codeVerifier.setAllowedTimePeriodDiscrepancy(0);
        Map<String, Object> result = new HashMap<String, Object>();
        if(codeVerifier.isValidCode(user.getSecret(), code)){
            result = jt.queryForList("select * from position where name=\'cyanwolf\' order by `time` desc limit 1").get(0);
            result.put("status", 0);
        }else{
            result.put("status", -1);
        }
        return result;
    }

}
