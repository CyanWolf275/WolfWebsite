package top.cyanwolf.main;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a chat user in the system, which implements the ChatInstance interface.
 * ChatUser maintains a list of ChatMessages, a history, a status flag indicating if it's ready for chat,
 * and other relevant information about the chat. It interacts with external scripts and communicates
 * with the user through a Friend interface provided by the Mirai library.
 *
 * @author CyanWolf
 * @version 1.0
 * @since 2023-03-25
 */
public class ChatUser {
    /**
     * List of ChatMessage objects representing the history of messages exchanged with this user.
     */
    private List<ChatMessage> msgList;
    /**
     * The unique identifier of the friend associated with this ChatUser.
     */
    private long id;
    /**
     * Represents the file that stores the historical chats of this user.
     */
    private File history;
    /**
     * Flag that indicates if the chat service is ready for this user. True if ready, false otherwise.
     */
    public boolean chatReady;
    /**
     * Static SimpleDateFormat used to format date and time as: "yyyy-MM-dd HH:mm:ss".
     */
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * Static SimpleDateFormat used to format date and time as: "MM-dd-HH-mm-ss".
     */
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("MM-dd-HH-mm-ss");
    /**
     * Represents the instructions associated with the user chat.
     */
    private String instruction;
    /**
     * Represents the directory path where the chat files are stored.
     */
    private String chatdir;
    /**
     * Flag that indicates if the GPT-4 model should be used for generating responses. True if GPT-4 should be used, false otherwise.
     */
    private boolean gpt4;

    /**
     * Constructs a new ChatUser with the provided Friend, chat directory and boolean for GPT4.
     * It reads in chat history if it exists, and creates new files for history, responses, and tokens
     * if they don't exist. The ChatUser is set as ready to chat.
     *
     * @param id  the id this ChatUser uses to communicate.
     * @param chatdir the directory where the chat history and related files are stored.
     * @param gpt4    a boolean flag indicating if the GPT4 model is used for this ChatUser.
     * @throws IOException if an I/O error occurs.
     */
    public ChatUser(long id, String chatdir, boolean gpt4) throws IOException {
        this.chatdir = chatdir;
        this.gpt4 = gpt4;
        this.id = id;
        msgList = new ArrayList<ChatMessage>();
        history = new File(chatdir + "web" + File.separator + id + ".txt");
        instruction = "";
        if(history.exists()){
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(history), "UTF-8"));
            StringBuilder tempstr = new StringBuilder();
            while(reader.ready())
                tempstr.append((char)reader.read());
            reader.close();
            String[] historylst = tempstr.toString().split("\\\\\\|\\n");
            for(String msg : historylst){
                if(msg.length() != 0){
                    int idx = msg.indexOf(":");
                    msgList.add(new ChatMessage(msg.substring(0, idx), msg.substring(idx + 1)));
                }
            }
            if(msgList.size() != 0)
                instruction = msgList.get(0).message;
        }else{
            history.createNewFile();
            File responsefile = new File(chatdir + "web" + File.separator + id + "response.txt");
            File tokenfile = new File(chatdir + "web" + File.separator + id + "tokens.txt");
            responsefile.createNewFile();
            tokenfile.createNewFile();
        }
        chatReady = true;
    }
    /**
     * Clears the chat history of the user.
     * Removes all messages and sets the first message as an instruction.
     */
    public void clearHistory(){
        msgList.clear();
        msgList.add(new ChatMessage("system", instruction));
        save();
    }

    /**
     * Sends a chat message and receives a response from an external script.
     * It adds the sent message and received response to the chat message list.
     * If the external script doesn't respond in 90 seconds, it aborts the process and removes the message from the list.
     * The chat status is updated before and after sending a message.
     *
     * @param msg the message to be sent.
     * @return a string representing the SQL insert command to log this chat into a database.
     */
    public ChatResponse chat(String msg){
        chatReady = false;
        msgList.add(new ChatMessage("user", msg));
        ChatResponse result = null;
        try{
            save();
            String response = "";
            Process process = null;
            if(gpt4)
                process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + "web" + File.separator + " && python privatechatgpt4.py " + id);
            else
                process = Runtime.getRuntime().exec("cmd /c cd " + chatdir + "web" + File.separator + " && python privatechat.py " + id);
            if(process.waitFor(90, TimeUnit.SECONDS)){
                if(process.exitValue() == 0){
                    File file = new File(chatdir + "web" + File.separator + id + "response.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
                    while(reader.ready())
                        response += (char)reader.read();
                    String responsetime = SDF.format(new Date());
                    File tokens = new File(chatdir + "web" + File.separator + id + "tokens.txt");
                    BufferedReader tokenreader = new BufferedReader(new InputStreamReader(new FileInputStream(tokens)));
                    int numtoken = Integer.parseInt(tokenreader.readLine());
                    msgList.add(new ChatMessage("assistant", response));
                    save();
                    result = new ChatResponse(response, responsetime, numtoken);
                    reader.close();
                    tokenreader.close();
                }else{
                    msgList.remove(msgList.size() - 1);
                    save();
                    chatReady = true;
                    return new ChatResponse("apierror", SDF.format(new Date()), -1);
                }
            }else{
                msgList.remove(msgList.size() - 1);
                process.destroy();
                save();
                chatReady = true;
                return new ChatResponse("timeout", SDF.format(new Date()), -1);
            }
        }catch (Exception e){
            msgList.remove(msgList.size() - 1);
            save();
            chatReady = true;
            throw new RuntimeException(e);
        }
        chatReady = true;
        return result;
    }
    /**
     * Attempts to regenerate the last response from the chatbot.
     * Removes the last user message and the assistant response from the history and initiates a chat with the message prior to the last.
     *
     * @return The response from the chatbot to the message prior to the last.
     */
    public String regenerate(){
        String msg = msgList.get(msgList.size() - 2).message;
        msgList.remove(msgList.size() - 1);
        msgList.remove(msgList.size() - 1);
        return chat(msg).response;
    }
    /**
     * Reverts the last conversation turn.
     * Removes the last user message and the assistant response from the history.
     *
     * @return true if the operation was successful, false if there were not enough messages in the history to perform the operation.
     */
    public boolean pushback(){
        if(msgList.size() <= 1){
            return false;
        }else{
            msgList.remove(msgList.size() - 1);
            msgList.remove(msgList.size() - 1);
            return true;
        }
    }
    /**
     * Clears the chat history and sets the initial instruction message for the chat session.
     * This message appears at the start of the chat history when it is cleared.
     *
     * @param instr The message to be set as the initial instruction.
     */
    public void setInstruction(String instr){
        msgList.clear();
        instruction = instr;
        msgList.add(new ChatMessage("system", instruction));
        save();
    }
    /**
     * Returns the current instruction of the ChatUser. The instruction is initially set to the first
     * message of the chat history (if it exists), or a default instruction if the chat history is empty.
     * It can be changed using the setInstruction methods.
     *
     * @return a String representing the current instruction
     */
    public String getInstruction(){
        return instruction;
    }
    /**
     * Saves the current chat history to a history file. The history file is overwritten each time
     * this method is called. If the file is successfully written, the method returns true.
     *
     * @return true if the chat history was successfully written to the history file
     * @throws RuntimeException if there was an error writing to the history file
     */
    public boolean save(){
        try {
            FileOutputStream fs = new FileOutputStream(history);
            fs.write(chatListString(msgList).getBytes("UTF-8"));
            fs.close();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public List<String> listSave(){
        List<String> result = new ArrayList<String>();
        //TODO
        return result;
    }
    public boolean saveToFile(String name){
        try{
            //TODO
            return true;
        }catch (Exception e){
            return false;
        }
    }
    public List<ChatMessage> readFromFile(String name){
        //TODO
        return getMsgList();
    }
    public long getId(){
        return id;
    }
    public List<ChatMessage> getMsgList(){
        return msgList;
    }
    public boolean equals(Object chatUser){
        return id == ((ChatUser) chatUser).getId();
    }
    /**
     * Converts the provided list of ChatMessages into a String representation.
     * Each ChatMessage in the list is translated into a string in the format: "role:message\|\n".
     * Note that the resulting string uses the "role" and "message" properties of each ChatMessage.
     * The messages are separated by a "\|\n" delimiter.
     *
     * @param list The list of ChatMessages to be converted to a String representation.
     * @return A String representation of the ChatMessages list.
     */
    private static String chatListString(List<ChatMessage> list){
        String result = "";
        for(ChatMessage chatMessage : list){
            result += chatMessage.role + ":" + chatMessage.message + "\\|\n";
        }
        return result;
    }
    public String chatListSave(){
        String result = "";
        for(ChatMessage chatMessage : msgList){
            String rolename = "";
            if(chatMessage.role.equals("system"))
                rolename = "系统";
            else if(chatMessage.role.equals("user"))
                rolename = "你";
            else if(chatMessage.role.equals("assistant"))
                rolename = "大狼";
            result += rolename + ": " + chatMessage.message + "\n";
        }
        return result;
    }
}
