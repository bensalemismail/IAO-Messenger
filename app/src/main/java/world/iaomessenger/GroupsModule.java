package world.iaomessenger;

public class GroupsModule {

    String name;
    long time;
    String message;
    String from;

    public GroupsModule(String name, long time, String message, String from) {
        this.name = name;
        this.time = time;
        this.message = message;
        this.from = from;
    }

    public GroupsModule() {
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
